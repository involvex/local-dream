package com.involvex.localdreamchat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeByConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getByConversation(conversationId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :id")
    suspend fun getById(id: String): ChatMessageEntity?

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)

    @Query("UPDATE chat_messages SET imagePath = :imagePath WHERE id = :id")
    suspend fun updateImagePath(id: String, imagePath: String)

    @Query("UPDATE chat_messages SET isGenerating = 0 WHERE id = :id")
    suspend fun markGenerationComplete(id: String)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId AND isGenerating = 1 ORDER BY timestamp ASC")
    suspend fun getGeneratingMessages(conversationId: String): List<ChatMessageEntity>
}
