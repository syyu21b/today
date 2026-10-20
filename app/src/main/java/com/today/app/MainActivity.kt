package com.today.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.today.app.data.AppIconManager
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.i18n.Strings
import com.today.app.notify.Notifier
import com.today.app.ui.components.IntroSplash
import com.today.app.ui.screens.CommunityScreen
import com.today.app.ui.screens.HomeScreen
import com.today.app.ui.screens.OutfitScreen
import com.today.app.ui.screens.StatsScreen
import com.today.app.ui.screens.settings.SettingsScreen
import com.today.app.ui.theme.NightSurface
import com.today.app.ui.theme.TodayTheme

class MainActivity : ComponentActivity() {
    private val vm: TodayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash is a plain background; IntroSplash then shows the user's own character.
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val freshLaunch = savedInstanceState == null
        if (freshLaunch) handleNotificationIntent(intent)
        setContent {
            TodayTheme {
                // Intro only on a real cold start, not after rotation / process recreation.
                var showIntro by rememberSaveable { mutableStateOf(freshLaunch) }
                Box(Modifier.fillMaxSize()) {
                    TodayApp(vm) // composed underneath so it's ready when the intro fades out
                    AnimatedVisibility(visible = showIntro, enter = EnterTransition.None, exit = fadeOut(tween(400))) {
                        IntroSplash(vm.character.outfit, onFinished = { showIntro = false })
                    }
                }
            }
        }
    }

    /** Leaving the app is the safe moment to swap the launcher icon to the current outfit. */
    override fun onStop() {
        super.onStop()
        AppIconManager.sync(applicationContext, vm.character.outfit)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    /** Tapping a message notification opens that chat; the streak alert's button spends a shield. */
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(Notifier.EXTRA_USE_SHIELD, false) == true) {
            intent.removeExtra(Notifier.EXTRA_USE_SHIELD)
            // Our notification targets MainActivity itself; other apps can only reach the exported
            // launcher aliases, so they can't spend shields.
            if (intent.component?.className == MainActivity::class.java.name) vm.useShieldFromNotification()
        }
        val friendId = intent?.getStringExtra(Notifier.EXTRA_OPEN_CHAT) ?: return
        intent.removeExtra(Notifier.EXTRA_OPEN_CHAT)
        vm.openChatFromNotification(friendId)
    }
}

private enum class Tab(val icon: ImageVector) {
    // Declaration order = bottom bar order.
    Quests(Icons.Filled.TaskAlt),
    Outfit(Icons.Filled.Checkroom),
    Messages(Icons.Filled.Forum),
    Stats(Icons.Filled.BarChart),
    Settings(Icons.Filled.Settings);

    fun label(s: Strings) = when (this) {
        Quests -> s.tabQuests
        Stats -> s.tabStats
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
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.appInForeground = true }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.appInForeground = false }

    // A notification asked for a chat: show the Messages tab (the VM already opened the chat).
    LaunchedEffect(vm.pendingChatNavigation) {
        if (vm.pendingChatNavigation != null) {
            tab = Tab.Messages
            vm.consumeChatNavigation()
        }
    }

    vm.message?.let { msg ->
        LaunchedEffect(msg.id) {
            snackbar.currentSnackbarData?.dismiss()
            // With a button (Undo) it stays a bit longer; without an explicit duration it would never close.
            val result = snackbar.showSnackbar(
                msg.text,
                actionLabel = msg.action,
                duration = if (msg.action != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.onMessageAction(msg.id)
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
                Tab.Stats -> StatsScreen(vm, contentModifier)
                Tab.Outfit -> OutfitScreen(vm, contentModifier)
                Tab.Messages -> CommunityScreen(vm, contentModifier)
                Tab.Settings -> SettingsScreen(
                    vm,
                    onOpenOutfit = { tab = Tab.Outfit },
                    onOpenChat = { id -> vm.openChat(id); tab = Tab.Messages },
                    modifier = contentModifier,
                )
            }
        }
    }
}
