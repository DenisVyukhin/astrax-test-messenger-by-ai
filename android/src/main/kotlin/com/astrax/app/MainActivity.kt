package com.astrax.app

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.astrax.app.data.AstraxViewModel
import com.astrax.app.data.Screen
import com.astrax.app.ui.screens.AuthScreen
import com.astrax.app.ui.screens.ChatScreen
import com.astrax.app.ui.screens.ChatsScreen
import com.astrax.app.ui.theme.AstraxTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AstraxViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val appWindow = window
        setContent {
            val state by viewModel.state.collectAsState()
            AstraxTheme(darkTheme = state.isDarkTheme) {
                val statusBarColor = MaterialTheme.colorScheme.surface
                val navigationBarColor = MaterialTheme.colorScheme.background
                SideEffect {
                    appWindow.statusBarColor = statusBarColor.toArgb()
                    appWindow.navigationBarColor = navigationBarColor.toArgb()
                    val lightFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
                    appWindow.decorView.systemUiVisibility = if (state.isDarkTheme) 0 else lightFlags
                }
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            visible = !state.isAuthorized,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AuthScreen(state = state, viewModel = viewModel)
                        }
                        AnimatedVisibility(
                            visible = state.isAuthorized,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AnimatedContent(
                                targetState = state.screen,
                                label = "screen",
                                transitionSpec = {
                                    if (targetState == Screen.Chat) {
                                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                                    } else {
                                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                                    }
                                }
                            ) { screen ->
                                when (screen) {
                                    Screen.Chats -> ChatsScreen(state = state, viewModel = viewModel)
                                    Screen.Chat -> ChatScreen(state = state, viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
