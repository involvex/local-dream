package com.involvex.localdreamchat.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["characterId"]),
        Index(value = ["lastMessageTime"]),
    ],
)
data class ConversationEntity(
    @PrimaryKey val id: String,

    val characterId: String,

    val title: String,

    @ColumnInfo(defaultValue = "")
    val lastMessage: String = "",

    @ColumnInfo(defaultValue = "0")
    val lastMessageTime: Long = 0,

    @ColumnInfo(defaultValue = "0")
    val messageCount: Int = 0,
)
