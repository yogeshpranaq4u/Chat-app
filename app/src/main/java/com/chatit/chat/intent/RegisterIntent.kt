package com.chatit.chat.intent

sealed class RegisterIntent {
    data class RegisterUser(val email: String, val imageUriString: String?) : RegisterIntent()
}