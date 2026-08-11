package com.involvex.localdreamchat.data.model

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val imagePath: String? = null,
    val isGenerating: Boolean = false,
)
