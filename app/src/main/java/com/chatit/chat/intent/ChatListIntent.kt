package com.chatit.chat.intent

sealed class ChatListIntent {
    object LoadChats : ChatListIntent()
}