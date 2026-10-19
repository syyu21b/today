package com.today.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.i18n.Strings
import com.today.app.ui.components.IntroSplash
import com.today.app.ui.screens.CommunityScreen
import com.today.app.ui.screens.HomeScreen
import com.today.app.ui.screens.OutfitScreen
import com.today.app.ui.screens.SettingsScreen
import com.today.app.ui.theme.NightSurface
import com.today.app.ui.theme.TodayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash (logo) -> IntroSplash draws the identical logo in the same spot, so drop the
        // system one instantly instead of letting its own fade-out dim the logo for a frame.
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val freshLaunch = savedInstanceState == null
        setContent {
            TodayTheme {
                // Intro only on a real cold start, not after rotation / process recreation.
                var showIntro by rememberSaveable { mutableStateOf(freshLaunch) }
                Box(Modifier.fillMaxSize()) {
                    TodayApp() // composed underneath so it's ready when the intro fades out
                    AnimatedVisibility(visible = showIntro, enter = EnterTransition.None, exit = fadeOut(tween(400))) {
                        IntroSplash(onFinished = { showIntro = false })
                    }
                }
            }
        }
    }
}

private enum class Tab(val icon: ImageVector) {
    Quests(Icons.Filled.TaskAlt),
    Outfit(Icons.Filled.Checkroom),
    Messages(Icons.Filled.Forum),
    Settings(Icons.Filled.Settings);

    fun label(s: Strings) = when (this) {
        Quests -> s.tabQuests
        Outfit -> s.tabOutfit
        Messages -> s.tabMessages
        Settings -> s.tabSettings
    }
}

@Composable
fun TodayApp(vm: TodayViewModel = viewModel()) {
    // Every screen reads its text from LocalStrings, so switching language re-renders all of it.
    CompositionLocalProvider(LocalStrings provides vm.strings) {
        TodayScaffold(vm)
    }
}

@Composable
private fun TodayScaffold(vm: TodayViewModel) {
    val strings = LocalStrings.current
    var tab by rememberSaveable { mutableStateOf(Tab.Quests) }
    val inChat = tab == Tab.Messages && vm.openChatId != null
    val unreadCount = vm.unread.count { it.value }
    val snackbar = remember { SnackbarHostState() }

    // App left open past midnight -> move "today" forward when it comes back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDay() }

    vm.message?.let { msg ->
        LaunchedEffect(msg.id) {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(msg.text)
            vm.consumeMessage(msg.id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(!inChat, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NavigationBar(containerColor = NightSurface) {
                    Tab.entries.forEach { t ->
                        val label = t.label(strings)
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = {
                                if (t == Tab.Messages && unreadCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$unreadCount") } }) { Icon(t.icon, label) }
                                } else {
                                    Icon(t.icon, label)
                                }
                            },
                            label = { Text(label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
        },
    ) { inner ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(inner)
            .consumeWindowInsets(inner)
        Crossfade(targetState = tab, label = "tab") { current ->
            when (current) {
                Tab.Quests -> HomeScreen(vm, contentModifier)
                Tab.Outfit -> OutfitScreen(vm, contentModifier)
                Tab.Messages -> CommunityScreen(vm, contentModifier)
                Tab.Settings -> SettingsScreen(vm, onOpenOutfit = { tab = Tab.Outfit }, modifier = contentModifier)
            }
        }
    }
}
