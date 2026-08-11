package com.involvex.localdreamchat.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,

    val name: String,

    val description: String,

    val personality: String,

    @ColumnInfo(defaultValue = "")
    val avatarEmoji: String = "",

    val systemPrompt: String,

    @ColumnInfo(defaultValue = "")
    val imageTriggerKeywords: String = "",

    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
)
