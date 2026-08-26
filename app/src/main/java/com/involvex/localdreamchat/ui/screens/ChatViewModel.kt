package com.involvex.localdreamchat.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.involvex.localdreamchat.data.db.AppDatabase
import com.involvex.localdreamchat.data.model.ChatCharacter
import com.involvex.localdreamchat.data.model.ChatMessage
import com.involvex.localdreamchat.data.model.Conversation
import com.involvex.localdreamchat.data.repository.ChatRepository
import com.involvex.localdreamchat.service.ImageGenerationBridge
import com.involvex.localdreamchat.service.LlmChatMessage
import com.involvex.localdreamchat.service.LlmService
import com.involvex.localdreamchat.service.LlmState
import com.involvex.localdreamchat.service.TokenCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ChatViewModel(
    application: Application,
    private val characterId: String,
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val LLM_LOAD_TIMEOUT_MS = 120_000L
    }

    private val repository = ChatRepository.get(AppDatabase.get(application))
    private val llmService = LlmService.get(application)
    private val imageBridge = ImageGenerationBridge(application, repository)

    private val _character = MutableStateFlow<ChatCharacter?>(null)
    val character: StateFlow<ChatCharacter?> = _character.asStateFlow()

    private val _conversation = MutableStateFlow<Conversation?>(null)
    val conversation: StateFlow<Conversation?> = _conversation.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _imageGenerationError = MutableStateFlow<String?>(null)
    val imageGenerationError: StateFlow<String?> = _imageGenerationError.asStateFlow()

    private val _loadingLLM = MutableStateFlow(false)
    val loadingLLM: StateFlow<Boolean> = _loadingLLM.asStateFlow()

    private val _loadingImage = MutableStateFlow(false)
    val loadingImage: StateFlow<Boolean> = _loadingImage.asStateFlow()

    private val _hasImage = MutableStateFlow(false)
    val hasImage: StateFlow<Boolean> = _hasImage.asStateFlow()

    private val _selectedImage = MutableStateFlow<Boolean>(false)
    val selectedImage: StateFlow<Boolean> = _selectedImage.asStateFlow()

    private val _isModelFound = MutableStateFlow(false)
    val isModelFound: StateFlow<Boolean> = _isModelFound.asStateFlow()

    private val _isModelDownloaded = MutableStateFlow(false)
    val isModelDownloaded: StateFlow<Boolean> = _isModelDownloaded.asStateFlow()

    private val _characterName = MutableStateFlow<String>("")
    val characterName: StateFlow<String> = _characterName.asStateFlow()

    private val _conversationId = MutableStateFlow<String>("")
    val conversationId: StateFlow<String> = _conversationId.asStateFlow()

    private val _isImageGenerationSupported = MutableStateFlow(false)
    val isImageGenerationSupported: StateFlow<Boolean> = _isImageGenerationSupported.asStateFlow()

    private val _showImageGenerationButton = MutableStateFlow(true)
    val showImageGenerationButton: StateFlow<Boolean> = _showImageGenerationButton.asStateFlow()

    private val _imageGenerationErrorReason = MutableStateFlow<String?>(null)
    val imageGenerationErrorReason: StateFlow<String?> = _imageGenerationErrorReason.asStateFlow()

    // Observe LLM state to update loadingLLM
    init {
        viewModelScope.launch {
            llmService.state.collect { state ->
                _loadingLLM.value = state is LlmState.Loading
            }
        }
    }

    fun onNewMessage(message: String, characterId: String) {
        val conv = _conversation.value
        if (conv == null) return

        viewModelScope.launch {
            val messageMsg = repository.addUserMessage(conv.id, message)
            _messages.value = _messages.value + messageMsg
        }
    }

    fun onMessageReceived(message: String) {
        val conv = _conversation.value
        if (conv == null) return

        viewModelScope.launch {
            val messageMsg = repository.addUserMessage(conv.id, message)
            _messages.value = _messages.value + messageMsg
        }
    }

    fun onConversationInitialized(conv: Conversation) {
        _conversation.value = conv
    }

    private fun onCharacterLoaded(char: ChatCharacter) {
        _character.value = char
        _characterName.value = char.name
        _conversationId.value = _conversation.value?.id ?: ""
        _showImageGenerationButton.value = true
    }

    fun loadCharacter(characterId: String) {
        viewModelScope.launch {
            val char = repository.getCharacter(characterId)
            if (char != null) {
                onCharacterLoaded(char)
                val conv = repository.getOrCreateConversation(characterId)
                onConversationInitialized(conv)
                val msgs = repository.getMessages(conv.id)
                _messages.value = msgs

                // Load LLM model for chat
                if (!llmService.isModelDownloaded()) {
                    _loadingLLM.value = true
                    // Model not downloaded - user needs to download it first
                } else {
                    llmService.loadModel()
                }
            }
        }
    }

    // loadConversations is no longer needed; messages are updated by onNewMessage/onMessageReceived
    // and the conversation list is loaded separately if needed.

    fun sendMessage(content: String) {
        if (content.isBlank() || _isGenerating.value) return

        val conv = _conversation.value ?: return
        val char = _character.value ?: return

        viewModelScope.launch {
            // Ensure LLM is loaded
            if (llmService.state.value !is LlmState.Ready) {
                if (!llmService.isModelDownloaded()) {
                    _imageGenerationError.value = "LLM model not downloaded. Please download the LLM model first."
                    return@launch
                }
                _loadingLLM.value = true
                llmService.loadModel()
                // Wait for LLM to become Ready or Error; an unbounded wait here
                // used to leave the chat spinner running forever on load failure.
                withTimeoutOrNull(LLM_LOAD_TIMEOUT_MS) {
                    llmService.state.first { it is LlmState.Ready || it is LlmState.Error }
                }
                _loadingLLM.value = false
                val state = llmService.state.value
                if (state !is LlmState.Ready) {
                    _imageGenerationError.value =
                        (state as? LlmState.Error)?.message ?: "LLM took too long to load"
                    return@launch
                }
            }

            _isGenerating.value = true
            _isGeneratingImage.value = false
            _imageGenerationError.value = null
            _loadingLLM.value = false

            try {
                // Snapshot history BEFORE persisting the new user message so
                // the current turn appears exactly once in the transcript.
                val history = _messages.value
                    .filter { it.content.isNotBlank() && it.imagePath == null }
                    .takeLast(12)
                    .map { message ->
                        LlmChatMessage(
                            role = if (message.role == "user") "user" else "assistant",
                            content = message.content,
                        )
                    }
                val userMsg = repository.addUserMessage(conv.id, content)
                _messages.value = _messages.value + userMsg

                // Stateless multi-turn generation: system persona + recent
                // history + the new user message go through the model's chat
                // template on every send (KV cache is reset per call).
                val systemPrompt = buildString {
                    append("You are ${char.name}, chatting casually with the user. ")
                    if (char.personality.isNotBlank()) {
                        append("Personality: ${char.personality}. ")
                    }
                    append("Description: ${char.description}")
                }
                val chatMessages = listOf(LlmChatMessage(role = "system", content = systemPrompt)) +
                    history +
                    listOf(LlmChatMessage(role = "user", content = content))

                val llmMessage = llmService.generateChat(chatMessages).trim()
                    .ifBlank {
                        _isGenerating.value = false
                        _imageGenerationError.value = "LLM returned an empty response"
                        return@launch
                    }

                val messageMsg = repository.addAssistantMessage(
                    conversationId = conv.id,
                    content = llmMessage,
                )
                _messages.value = _messages.value + messageMsg
                _isGenerating.value = false
                _loadingImage.value = false
                _imageGenerationError.value = null

                val imagePath = imageBridge.checkAndGenerate(
                    conversationId = conv.id,
                    userMessage = content,
                    imageTriggerKeywords = char.imageTriggerKeywords,
                )

                if (imagePath != null) {
                    val imageMsg = repository.addAssistantMessage(
                        conversationId = conv.id,
                        content = "Here's an image of me:",
                    )
                    repository.updateMessageImage(imageMsg.id, imagePath)
                    _hasImage.value = true
                } else if (imageBridge.lastError != null) {
                    // checkAndGenerate also returns null when no trigger keyword
                    // matched - that is the normal text-only path, not an error.
                    _imageGenerationError.value = imageBridge.lastError
                }
            } catch (e: Exception) {
                Log.e(TAG, "LLM send message failed: ${e.message}", e)
                _isGenerating.value = false
                _loadingLLM.value = false
                _imageGenerationError.value = e.message
            }
        }
    }

    fun generateCharacterImage(explicitPrompt: String? = null) {
        if (_isGeneratingImage.value) return
        val conv = _conversation.value
        if (conv == null) {
            _imageGenerationError.value =
                "No active conversation — open a chat before generating an image"
            return
        }
        val char = _character.value
        if (char == null) {
            _imageGenerationError.value = "No character selected for this conversation"
            return
        }

        viewModelScope.launch {
            _isGeneratingImage.value = true
            _imageGenerationError.value = null

            try {
                val path = imageBridge.generateCharacterImage(
                    conversationId = conv.id,
                    characterName = char.name,
                    characterDescription = char.description,
                    explicitPrompt = explicitPrompt,
                )

                if (path != null) {
                    val imageMsg = repository.addAssistantMessage(
                        conversationId = conv.id,
                        content = "Here's an image of me:",
                    )
                    repository.updateMessageImage(imageMsg.id, path)
                    _hasImage.value = true
                } else {
                    _imageGenerationError.value =
                        imageBridge.lastError ?: "Image generation returned no result"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Character image generation failed", e)
                _imageGenerationError.value = e.message ?: "Image generation failed"
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }

    fun onImageGenerationError(error: String?) {
        _imageGenerationError.value = error?.takeIf { it.isNotBlank() }
    }

    fun onBackPressed() {
        // no-op
    }

    override fun onCleared() {
        super.onCleared()
        llmService.unload()
    }
}
