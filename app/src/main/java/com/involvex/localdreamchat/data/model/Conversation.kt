package com.involvex.localdreamchat.data.model

data class Conversation(
    val id: String,
    val characterId: String,
    val title: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val messageCount: Int,
)
