package com.today.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.today.app.data.TodayViewModel
import com.today.app.ui.screens.CommunityScreen
import com.today.app.ui.screens.HomeScreen
import com.today.app.ui.screens.OutfitScreen
import com.today.app.ui.screens.SettingsScreen
import com.today.app.ui.theme.NightSurface
import com.today.app.ui.theme.TodayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodayTheme {
                TodayApp()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Quests("퀘스트", Icons.Filled.TaskAlt),
    Outfit("옷장", Icons.Filled.Checkroom),
    Messages("메시지", Icons.Filled.Forum),
    Settings("설정", Icons.Filled.Settings),
}

@Composable
fun TodayApp(vm: TodayViewModel = viewModel()) {
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
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = {
                                if (t == Tab.Messages && unreadCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$unreadCount") } }) { Icon(t.icon, t.label) }
                                } else {
                                    Icon(t.icon, t.label)
                                }
                            },
                            label = { Text(t.label) },
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
