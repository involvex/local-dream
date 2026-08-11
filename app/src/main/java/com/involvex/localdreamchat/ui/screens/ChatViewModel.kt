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
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application,
    private val characterId: String,
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
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

    private val _llmState = MutableStateFlow<LlmState>(LlmState.Unloaded)
    val llmState: StateFlow<LlmState> = _llmState.asStateFlow()

    private var currentMessageId: String? = null

    init {
        viewModelScope.launch {
            _character.value = repository.getCharacter(characterId)
            val conv = repository.getOrCreateConversation(characterId)
            _conversation.value = conv
            repository.observeMessages(conv.id).collect { msgs ->
                _messages.value = msgs
            }
        }

        // Observe LLM state
        viewModelScope.launch {
            llmService.state.collect { state ->
                _llmState.value = state
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isGenerating.value) return

        val conv = _conversation.value ?: return
        val char = _character.value ?: return

        viewModelScope.launch {
            // Add user message
            repository.addUserMessage(conv.id, content)

            // Check for image trigger
            val imagePath = imageBridge.checkAndGenerate(
                conversationId = conv.id,
                userMessage = content,
                imageTriggerKeywords = char.imageTriggerKeywords,
            )

            if (imagePath != null) {
                // Add a companion message with the image
                val imageMsg = repository.addAssistantMessage(
                    conversationId = conv.id,
                    content = "Here's what I created for you:",
                )
                repository.updateMessageImage(imageMsg.id, imagePath)
            }

            // Generate LLM response
            _isGenerating.value = true
            val assistantMsg = repository.addAssistantMessage(
                conversationId = conv.id,
                content = "",
                isGenerating = true,
            )
            currentMessageId = assistantMsg.id

            try {
                val responseBuilder = StringBuilder()
                llmService.generateStream(
                    systemPrompt = char.systemPrompt,
                    messages = repository.getMessages(conv.id).map {
                        LlmChatMessage(
                            role = it.role,
                            content = it.content,
                        )
                    },
                    callback = object : TokenCallback {
                        override fun onToken(token: String) {
                            responseBuilder.append(token)
                        }

                        override fun onComplete() {
                            viewModelScope.launch {
                                repository.addAssistantMessage(
                                    conversationId = conv.id,
                                    content = responseBuilder.toString(),
                                )
                                repository.markGenerationComplete(assistantMsg.id)
                            }
                        }

                        override fun onError(error: String) {
                            Log.e(TAG, "LLM error: $error")
                            viewModelScope.launch {
                                repository.addAssistantMessage(
                                    conversationId = conv.id,
                                    content = "Sorry, I encountered an error: $error",
                                )
                                repository.markGenerationComplete(assistantMsg.id)
                            }
                        }
                    },
                )
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                repository.addAssistantMessage(
                    conversationId = conv.id,
                    content = "Sorry, I couldn't generate a response. Please try again.",
                )
                repository.markGenerationComplete(assistantMsg.id)
            } finally {
                _isGenerating.value = false
                currentMessageId = null
            }
        }
    }

    fun loadModel() {
        viewModelScope.launch {
            llmService.load()
        }
    }

    fun isModelDownloaded(): Boolean = llmService.isModelDownloaded()

    fun unloadModel() {
        viewModelScope.launch {
            llmService.unload()
        }
    }

    fun generateCharacterImage(explicitPrompt: String? = null) {
        if (_isGeneratingImage.value) return
        val conv = _conversation.value ?: return
        val char = _character.value ?: return

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
                } else {
                    _imageGenerationError.value = "Image generation returned no result"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Character image generation failed", e)
                _imageGenerationError.value = e.message
            } finally {
                _isGeneratingImage.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmService.unload()
    }
}
