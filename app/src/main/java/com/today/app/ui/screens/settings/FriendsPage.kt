package com.today.app.ui.screens.settings

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.today.app.data.ContactEntry
import com.today.app.data.ContactsReader
import com.today.app.data.Friend
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.FriendAvatar
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FriendsPage(vm: TodayViewModel, onBack: () -> Unit, onOpenChat: (String) -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contactsAllowed by remember { mutableStateOf(ContactsReader.hasPermission(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { contactsAllowed = ContactsReader.hasPermission(context) }

    var pickerContacts by remember { mutableStateOf<List<ContactEntry>?>(null) }
    var showContactsDenied by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf<Friend?>(null) }
    var confirmDelete by remember { mutableStateOf<Friend?>(null) }

    fun loadContactsAndPick() {
        scope.launch {
            val list = withContext(Dispatchers.IO) { ContactsReader.read(context) }
            if (list.isEmpty()) vm.showMessage(s.contactsEmpty) else pickerContacts = list
        }
    }

    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsAllowed = granted
        if (granted) {
            loadContactsAndPick()
        } else {
            vm.finishContactsPrompt()
            // "Don't ask again" (or denied twice): the system won't show the dialog anymore.
            val canAskAgain = (context as? Activity)?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CONTACTS)
            } ?: false
            if (canAskAgain) vm.showMessage(s.contactsDenied) else showContactsDenied = true
        }
    }

    fun startImport() {
        if (ContactsReader.hasPermission(context)) loadContactsAndPick() else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
    }

    val friends = vm.visibleFriends
    val blocked = vm.blockedFriends

    SettingsSubPage(s.rowFriends, onBack) {
        // ---- First-time question: use contacts or not ----
        if (!vm.settings.contactsPromptDone) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Mint.copy(alpha = 0.1f))
                    .border(1.dp, Mint.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Contacts, Mint)
                    Spacer(Modifier.width(12.dp))
                    Text(s.contactsCardTitle, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.padding(top = 8.dp))
                Text(s.contactsCardBody, color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = vm::finishContactsPrompt, modifier = Modifier.weight(1f)) { Text(s.contactsLater) }
                    Button(onClick = ::startImport, modifier = Modifier.weight(1f)) { Text(s.contactsImport) }
                }
            }
        }

        SettingsGroup(s.groupContacts) {
            SettingsRow(
                title = s.importFromContacts,
                subtitle = if (contactsAllowed) s.contactsAllowed else s.contactsNotAllowed,
                icon = Icons.Filled.Contacts,
                iconColor = Mint,
                onClick = ::startImport,
            )
        }

        SettingsGroup(s.groupFriendList(friends.size)) {
            SettingsRow(
                title = s.addFriend,
                icon = Icons.Filled.PersonAdd,
                iconColor = Violet,
                onClick = { showAddDialog = true },
            )
            if (friends.isEmpty()) {
                SettingsDivider()
                Text(s.noFriends, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            friends.forEach { f ->
                SettingsDivider()
                FriendRow(
                    friend = f,
                    onMessage = { onOpenChat(f.id) },
                    onBlock = { confirmBlock = f },
                    onDelete = { confirmDelete = f },
                )
            }
        }

        SettingsGroup(s.groupBlocked(blocked.size)) {
            if (blocked.isEmpty()) {
                Text(s.noBlocked, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            blocked.forEachIndexed { i, f ->
                if (i > 0) SettingsDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FriendAvatar(f, 40.dp, dimmed = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.friendName(f))
                        Text("@${f.handle}", color = TextMuted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { vm.setBlocked(f.id, false) }) { Text(s.unblock) }
                }
            }
        }
    }

    pickerContacts?.let { contacts ->
        ContactPickerDialog(
            contacts = contacts,
            alreadyAdded = vm.friends.mapNotNull { it.contactId }.toSet(),
            onDismiss = { pickerContacts = null },
            onAdd = { picked ->
                vm.importContacts(picked)
                pickerContacts = null
            },
        )
    }

    if (showAddDialog) {
        AddFriendDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, handle ->
                val error = vm.addFriend(name, handle)
                if (error == null) showAddDialog = false else vm.showMessage(error)
            },
        )
    }

    confirmBlock?.let { f ->
        val name = s.friendName(f)
        AlertDialog(
            onDismissRequest = { confirmBlock = null },
            title = { Text(s.blockConfirmTitle(name)) },
            text = { Text(s.blockConfirmBody) },
            confirmButton = { TextButton(onClick = { vm.setBlocked(f.id, true); confirmBlock = null }) { Text(s.block, color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmBlock = null }) { Text(s.cancel) } },
        )
    }

    confirmDelete?.let { f ->
        val name = s.friendName(f)
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(s.deleteFriendTitle(name)) },
            text = { Text(s.deleteFriendBody) },
            confirmButton = { TextButton(onClick = { vm.removeFriend(f.id); confirmDelete = null }) { Text(s.delete, color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(s.cancel) } },
        )
    }

    if (showContactsDenied) {
        AlertDialog(
            onDismissRequest = { showContactsDenied = false },
            title = { Text(s.groupContacts) },
            text = { Text(s.contactsDenied) },
            confirmButton = { TextButton(onClick = { showContactsDenied = false; openAppDetailsSettings(context) }) { Text(s.openSystemSettings) } },
            dismissButton = { TextButton(onClick = { showContactsDenied = false }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun FriendRow(friend: Friend, onMessage: () -> Unit, onBlock: () -> Unit, onDelete: () -> Unit) {
    val s = LocalStrings.current
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onMessage)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FriendAvatar(friend, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.friendName(friend), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${friend.handle} · ${s.friendSource(friend.source)}", color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = s.friendMenu, tint = TextMuted) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(s.sendMessageAction) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
                    onClick = { menu = false; onMessage() },
                )
                DropdownMenuItem(
                    text = { Text(s.block) },
                    leadingIcon = { Icon(Icons.Filled.Block, null) },
                    onClick = { menu = false; onBlock() },
                )
                DropdownMenuItem(
                    text = { Text(s.deleteFriend, color = Coral) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Coral) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.addFriend) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(TodayViewModel.MAX_FRIEND_NAME) },
                    label = { Text(s.fieldFriendName) }, singleLine = true,
                )
                OutlinedTextField(
                    value = handle, onValueChange = { handle = it.take(21) },
                    label = { Text(s.fieldFriendHandle) }, singleLine = true, prefix = { Text("@") },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(name, handle) }, enabled = name.isNotBlank()) { Text(s.add) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
private fun ContactPickerDialog(
    contacts: List<ContactEntry>,
    alreadyAdded: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (List<ContactEntry>) -> Unit,
) {
    val s = LocalStrings.current
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<Long>() }
    val shown = contacts.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.contactsPickerTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text(s.searchName) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it.id }) { c ->
                        val added = c.id in alreadyAdded
                        val checked = added || c.id in selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !added) { if (c.id in selected) selected.remove(c.id) else selected.add(c.id) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, enabled = !added)
                            Spacer(Modifier.width(8.dp))
                            Text(c.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (added) Text(s.alreadyFriend, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(contacts.filter { it.id in selected }) }, enabled = selected.isNotEmpty()) {
                Text(s.addSelected(selected.size))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
