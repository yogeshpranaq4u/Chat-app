package com.chatit.chat.intent

sealed class ChatPageIntent {
    data class SendMessage(val chatId: String, val content: String, val type: String, val mediaUriString: String?) : ChatPageIntent()
    data class LoadMessages(val chatId: String) : ChatPageIntent()
}
