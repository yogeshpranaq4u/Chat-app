package com.chatit.chat.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import com.chatit.chat.R
import com.chatit.chat.data.Message
import com.chatit.chat.intent.ChatPageIntent
import com.chatit.chat.state.ChatPageState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ChatPageViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _state = mutableStateOf<ChatPageState>(ChatPageState.Loading)
    val state: State<ChatPageState> = _state

    private var messagesListener: ListenerRegistration? = null
    private val messages = mutableListOf<Message>()

    fun process(intent: ChatPageIntent, uid: String, context: Context) {
        when (intent) {
            is ChatPageIntent.LoadMessages -> listenMessages(intent.chatId,context)
            is ChatPageIntent.SendMessage -> sendMessage(intent, uid)
        }
    }

    private fun listenMessages(chatId: String, context: Context) {
        messagesListener?.remove()
        val messagesRef = firestore.collection("chats").document(chatId).collection("messages").orderBy("timestamp")
        messagesListener = messagesRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _state.value = ChatPageState.Error(e.message ?: "Unknown error")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val receivedMessages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                _state.value = ChatPageState.Loaded(receivedMessages)
            }
        }
    }

    fun showLocalNotification(message: Message, context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("New Message")
            .setContentText(
                if (message.type == "image") "Sent you an image" else message.content
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

    private fun sendMessage(intent: ChatPageIntent.SendMessage, receiverId: String) {
        val sender = auth.currentUser?.uid ?: return
        val chatId = intent.chatId
        val messagesRef = firestore.collection("chats").document(chatId).collection("messages")
        val now = System.currentTimeMillis()
        val msgId = messagesRef.document().id

        fun postMessage(content: String, type: String, mediaUrl: String?) {
            println("ChatPageViewModel.postMessage ... $receiverId .. $sender")
            val newMsg = Message(msgId, sender, content, type, mediaUrl, now)
            messagesRef.document(msgId).set(newMsg)
                .addOnSuccessListener {
                    val chatRef = firestore.collection("chats").document(chatId)
                    val chatUpdates = mapOf(
                        "lastMessage" to content.take(50),
                        "members" to listOf(receiverId, sender),
                        "timestamp" to now
                    )
                    chatRef.set(chatUpdates, SetOptions.merge())
                        .addOnFailureListener { e -> println("ChatPageViewModel: chat update failed: ${e.message}") }
                }
                .addOnFailureListener { e ->
                    println("ChatPageViewModel: failed to send message: ${e.message}")
                }
        }

        if (intent.type == "image" && !intent.mediaUriString.isNullOrEmpty()) {
            val uri = Uri.parse(intent.mediaUriString)
            val storageRef = storage.reference.child("chatImages/$msgId.jpg")
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { url ->
                        postMessage("[Image]", "image", url.toString())
                    }.addOnFailureListener {
                        _state.value = ChatPageState.Error("Failed to get image download URL")
                    }
                }
                .addOnFailureListener {
                    _state.value = ChatPageState.Error(it.message ?: "Image upload failed")
                }
        } else {
            postMessage(intent.content, "text", null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}
