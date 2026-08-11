package com.involvex.localdreamchat.data.repository

import com.involvex.localdreamchat.data.db.AppDatabase
import com.involvex.localdreamchat.data.db.CharacterEntity
import com.involvex.localdreamchat.data.db.ChatMessageEntity
import com.involvex.localdreamchat.data.db.ConversationEntity
import com.involvex.localdreamchat.data.model.ChatCharacter
import com.involvex.localdreamchat.data.model.ChatMessage
import com.involvex.localdreamchat.data.model.Conversation
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository private constructor(
    private val db: AppDatabase,
) {

    private val characterDao = db.characterDao()
    private val conversationDao = db.conversationDao()
    private val chatMessageDao = db.chatMessageDao()

    // ── Characters ───────────────────────────────────────────────

    fun observeCharacters(): Flow<List<ChatCharacter>> = characterDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getCharacters(): List<ChatCharacter> = characterDao.getAll().map { it.toDomain() }

    suspend fun getCharacter(id: String): ChatCharacter? = characterDao.getById(id)?.toDomain()

    fun observeCharacter(id: String): Flow<ChatCharacter?> = characterDao.observeById(id).map { it?.toDomain() }

    suspend fun setCharacterFavorite(id: String, favorite: Boolean) = characterDao.setFavorite(id, favorite)

    // ── Conversations ───────────────────────────────────────────

    fun observeConversations(): Flow<List<Conversation>> = conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getOrCreateConversation(characterId: String): Conversation {
        val existing = conversationDao.getByCharacterId(characterId)
        if (existing != null) return existing.toDomain()

        val character = characterDao.getById(characterId)
        val conversation = ConversationEntity(
            id = UUID.randomUUID().toString(),
            characterId = characterId,
            title = character?.name ?: "Chat",
            lastMessage = "",
            lastMessageTime = System.currentTimeMillis(),
            messageCount = 0,
        )
        conversationDao.insert(conversation)
        return conversation.toDomain()
    }

    fun observeConversation(id: String): Flow<Conversation?> = conversationDao.observeById(id).map { it?.toDomain() }

    suspend fun deleteConversation(id: String) {
        chatMessageDao.deleteByConversation(id)
        conversationDao.deleteById(id)
    }

    // ── Messages ────────────────────────────────────────────────

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = chatMessageDao.observeByConversation(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun getMessages(conversationId: String): List<ChatMessage> = chatMessageDao.getByConversation(conversationId).map { it.toDomain() }

    suspend fun addUserMessage(conversationId: String, content: String): ChatMessage {
        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = content,
            timestamp = System.currentTimeMillis(),
        )
        chatMessageDao.insert(message)
        conversationDao.incrementMessageCount(
            id = conversationId,
            time = System.currentTimeMillis(),
            preview = content.take(100),
        )
        return message.toDomain()
    }

    suspend fun addAssistantMessage(
        conversationId: String,
        content: String,
        isGenerating: Boolean = false,
    ): ChatMessage {
        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "assistant",
            content = content,
            timestamp = System.currentTimeMillis(),
            isGenerating = isGenerating,
        )
        chatMessageDao.insert(message)
        conversationDao.incrementMessageCount(
            id = conversationId,
            time = System.currentTimeMillis(),
            preview = content.take(100),
        )
        return message.toDomain()
    }

    suspend fun updateMessageImage(messageId: String, imagePath: String) = chatMessageDao.updateImagePath(messageId, imagePath)

    suspend fun markGenerationComplete(messageId: String) = chatMessageDao.markGenerationComplete(messageId)

    // ── Seeding ─────────────────────────────────────────────────

    suspend fun seedIfEmpty() {
        if (characterDao.count() > 0) return
        characterDao.insertAll(DEFAULT_CHARACTERS.map { it.toEntity() })
    }

    // ── Mapping helpers ─────────────────────────────────────────

    private fun CharacterEntity.toDomain() = ChatCharacter(
        id = id,
        name = name,
        description = description,
        personality = personality,
        avatarEmoji = avatarEmoji,
        systemPrompt = systemPrompt,
        imageTriggerKeywords = imageTriggerKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        isFavorite = isFavorite,
    )

    private fun ChatCharacter.toEntity() = CharacterEntity(
        id = id,
        name = name,
        description = description,
        personality = personality,
        avatarEmoji = avatarEmoji,
        systemPrompt = systemPrompt,
        imageTriggerKeywords = imageTriggerKeywords.joinToString(","),
        isFavorite = isFavorite,
    )

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        characterId = characterId,
        title = title,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        messageCount = messageCount,
    )

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        imagePath = imagePath,
        isGenerating = isGenerating,
    )

    companion object {
        @Volatile
        private var INSTANCE: ChatRepository? = null

        fun get(db: AppDatabase): ChatRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: ChatRepository(db).also { INSTANCE = it }
        }

        private val DEFAULT_CHARACTERS = listOf(
            ChatCharacter(
                id = "luna",
                name = "Luna",
                description = "A creative and imaginative artist who sees beauty in everything.",
                personality = "Dreamy, poetic, enthusiastic about art and nature.",
                avatarEmoji = "\uD83C\uDF19",
                systemPrompt = "You are Luna, a dreamy and creative artist. You speak with poetic warmth and often find metaphors in everyday things. You love discussing art, nature, dreams, and the beauty of small moments. Keep responses concise and evocative.",
                imageTriggerKeywords = listOf("draw", "paint", "create", "imagine", "sketch", "art", "picture of", "show me"),
                isFavorite = false,
            ),
            ChatCharacter(
                id = "kai",
                name = "Kai",
                description = "A tech-savvy adventurer who loves exploring the digital frontier.",
                personality = "Curious, energetic, witty, loves gadgets and sci-fi.",
                avatarEmoji = "\uD83D\uDE80",
                systemPrompt = "You are Kai, a tech-savvy adventurer. You're enthusiastic about technology, science fiction, and exploring new ideas. You speak with energy and wit, often making pop culture references. Keep responses concise and engaging.",
                imageTriggerKeywords = listOf("generate", "create", "build", "make", "design", "show me", "picture of"),
                isFavorite = false,
            ),
            ChatCharacter(
                id = "aria",
                name = "Aria",
                description = "A wise and calming presence who offers thoughtful advice.",
                personality = "Calm, empathetic, philosophical, great listener.",
                avatarEmoji = "\uD83C\uDF3F",
                systemPrompt = "You are Aria, a wise and calming presence. You listen deeply and offer thoughtful, empathetic advice. You speak gently and often share philosophical insights or wisdom from nature. Keep responses concise and soothing.",
                imageTriggerKeywords = listOf("visualize", "envision", "create", "imagine", "show me", "picture of", "scene of"),
                isFavorite = false,
            ),
            ChatCharacter(
                id = "sage",
                name = "Sage",
                description = "A witty storyteller who brings humor to every conversation.",
                personality = "Humorous, clever, storytelling, loves trivia and jokes.",
                avatarEmoji = "\uD83E\uDDD0",
                systemPrompt = "You are Sage, a witty storyteller. You bring humor and clever observations to every conversation. You love sharing interesting facts, telling short stories, and making people laugh. Keep responses concise and entertaining.",
                imageTriggerKeywords = listOf("illustrate", "draw", "create", "show me", "picture of", "scene of", "depict"),
                isFavorite = false,
            ),
        )
    }
}
