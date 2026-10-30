package com.scooter.control

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scooter.control.data.DeepLink
import com.scooter.control.data.ProfileApi
import com.scooter.control.data.ScooterRepository
import com.scooter.control.data.SessionStore
import com.scooter.control.data.SettingsStore
import com.scooter.control.ui.GameScreen
import com.scooter.control.ui.HomeScreen
import com.scooter.control.ui.LoginScreen
import com.scooter.control.ui.ProfileScreen
import com.scooter.control.ui.SettingsScreen
import com.scooter.control.ui.theme.LastBotTheme
import com.scooter.control.ui.theme.LocalPalette
import com.scooter.control.ui.theme.paletteById
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val sessionStore by lazy { SessionStore(applicationContext) }
    private val settingsStore by lazy { SettingsStore(applicationContext) }
    private val repository = ScooterRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // После входа (и при каждом новом токене) подтягиваем профиль и аватарку с сервера бота.
        lifecycleScope.launch {
            sessionStore.tokenFlow.distinctUntilChanged().collect { token ->
                if (!token.isNullOrBlank()) ProfileApi.refresh(applicationContext, sessionStore)
            }
        }

        setContent {
            val themeId by settingsStore.themeFlow.collectAsState(initial = "orange")
            val animations by settingsStore.animationsFlow.collectAsState(initial = true)
            val kawaiiUnlocked by settingsStore.kawaiiFlow.collectAsState(initial = false)
            val activePalette = if (kawaiiUnlocked && themeId == "kawaii") {
                com.scooter.control.ui.theme.KawaiiPalette
            } else {
                paletteById(themeId)
            }

            LastBotTheme(palette = activePalette, animations = animations) {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                var deepLink by remember { mutableStateOf(extractDeepLink(intent)) }

                val logout: () -> Unit = {
                    scope.launch {
                        sessionStore.clear()
                        deepLink = null
                        setIntent(Intent())
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                }

                Box(Modifier.fillMaxSize().background(LocalPalette.current.background)) {
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        enterTransition = {
                            if (animations) fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 8 }
                            else EnterTransition.None
                        },
                        exitTransition = {
                            if (animations) fadeOut(tween(200)) else ExitTransition.None
                        },
                        popEnterTransition = {
                            if (animations) fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 8 }
                            else EnterTransition.None
                        },
                        popExitTransition = {
                            if (animations) fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 8 }
                            else ExitTransition.None
                        },
                    ) {
                        composable("login") {
                            LoginScreen(
                                deepLink = deepLink,
                                sessionStore = sessionStore,
                                onLoggedIn = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                repository = repository,
                                session = sessionStore,
                                settings = settingsStore,
                                onProfile = { navController.navigate("profile") },
                                onSettings = { navController.navigate("settings") },
                                onGame = { navController.navigate("game") },
                            )
                        }
                        composable("game") {
                            GameScreen(onBack = { navController.popBackStack() })
                        }
                        composable("profile") {
                            ProfileScreen(
                                session = sessionStore,
                                onBack = { navController.popBackStack() },
                                onLogout = logout,
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                settings = settingsStore,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Если пользователь перешёл по ссылке из Telegram-бота, пока приложение
        // уже открыто, данные подхватятся при пересоздании экрана.
        recreate()
    }

    private fun extractDeepLink(intent: Intent?): DeepLink? {
        val data = intent?.data ?: return null
        if (data.scheme != "scooterapp" || data.host != "auth") return null
        val token = data.getQueryParameter("token") ?: return null
        return DeepLink(
            token = token,
            name = data.getQueryParameter("name").orEmpty(),
            username = data.getQueryParameter("username").orEmpty(),
            uid = data.getQueryParameter("uid").orEmpty(),
            server = data.getQueryParameter("server").orEmpty(),
        )
    }
}
