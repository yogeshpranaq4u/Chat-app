package com.chatit.chat.sealed

sealed class Screen(val route: String) {
    object Register : Screen("register")
    object ChatList : Screen("chat_list")
    object UserList : Screen("user_list")  // To pick new chat user
    object ChatPage : Screen("chat_page/{chatId}/{otherUserId}") {
        fun createRoute(chatId: String, otherUserId: String) = "chat_page/$chatId/$otherUserId"
    }
}
