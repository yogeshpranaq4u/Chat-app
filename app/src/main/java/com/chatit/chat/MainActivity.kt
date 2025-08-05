package com.chatit.chat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chatit.chat.sealed.Screen
import com.chatit.chat.ui.screens.ChatListScreen
import com.chatit.chat.ui.screens.ChatPageScreen
import com.chatit.chat.ui.screens.RegisterScreen
import com.chatit.chat.ui.screens.UserListScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = Screen.Register.route) {
                composable(Screen.Register.route) {
                    RegisterScreen(onRegistered = {
                        navController.navigate(Screen.ChatList.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    })
                }
                composable(Screen.ChatList.route) {
                    ChatListScreen(navController)
                }
                composable(Screen.UserList.route) {
                    UserListScreen(navController)
                }
                composable(
                    Screen.ChatPage.route,
                    arguments = listOf(
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("otherUserId") { type = NavType.StringType }
                    )) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                    ChatPageScreen(navController, chatId, otherUserId)
                }
            }
        }
    }
}
