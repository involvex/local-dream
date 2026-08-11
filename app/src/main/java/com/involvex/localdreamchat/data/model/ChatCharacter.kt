package com.involvex.localdreamchat.data.model

data class ChatCharacter(
    val id: String,
    val name: String,
    val description: String,
    val personality: String,
    val avatarEmoji: String,
    val systemPrompt: String,
    val imageTriggerKeywords: List<String>,
    val isFavorite: Boolean,
)
