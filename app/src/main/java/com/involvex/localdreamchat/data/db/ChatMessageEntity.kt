package com.involvex.localdreamchat.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,

    val conversationId: String,

    val role: String,

    val content: String,

    val imagePath: String? = null,

    val imagePrompt: String? = null,

    @ColumnInfo(defaultValue = "0")
    val isGenerating: Boolean = false,

    val timestamp: Long,

    @ColumnInfo(defaultValue = "0")
    val tokenCount: Int = 0,
)
