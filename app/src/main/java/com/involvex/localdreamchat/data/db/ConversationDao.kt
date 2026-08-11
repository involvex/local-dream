package com.involvex.localdreamchat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE characterId = :characterId ORDER BY lastMessageTime DESC LIMIT 1")
    suspend fun getByCharacterId(characterId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE characterId = :characterId ORDER BY lastMessageTime DESC LIMIT 1")
    fun observeByCharacterId(characterId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: String): Flow<ConversationEntity?>

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE conversations SET messageCount = messageCount + 1, lastMessageTime = :time, lastMessage = :preview WHERE id = :id")
    suspend fun incrementMessageCount(id: String, time: Long, preview: String)
}
