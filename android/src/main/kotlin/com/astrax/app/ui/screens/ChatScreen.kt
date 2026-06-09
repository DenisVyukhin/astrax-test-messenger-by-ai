package com.astrax.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrax.app.data.AstraxState
import com.astrax.app.data.AstraxViewModel
import com.astrax.app.data.MessageDto
import com.astrax.app.ui.components.Avatar
import com.astrax.app.ui.components.MarkdownText
import com.astrax.app.ui.components.MessageStatusIcon
import com.astrax.app.ui.components.TimeText
import com.astrax.app.ui.theme.AstraxAccent
import com.astrax.app.ui.theme.AstraxRed
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(state: AstraxState, viewModel: AstraxViewModel) {
    val chat = state.activeChat
    var menuExpanded by remember { mutableStateOf(false) }
    var knownMessageIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var newMessagesCount by remember { mutableIntStateOf(0) }
    var wasAtBottomBeforeMessages by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        }
    }

    BackHandler {
        viewModel.backToChats()
    }

    LaunchedEffect(state.activeChat?.id) {
        knownMessageIds = state.messages.map { it.id }.toSet()
        newMessagesCount = 0
    }

    LaunchedEffect(state.messages, isAtBottom) {
        val currentIds = state.messages.map { it.id }.toSet()
        val shouldAutoScrollIncoming = wasAtBottomBeforeMessages
        val newOutgoing = state.messages.count { message ->
            message.id !in knownMessageIds && message.senderId == state.userId
        }
        val newIncoming = state.messages.count { message ->
            message.id !in knownMessageIds && message.senderId != state.userId
        }
        if (knownMessageIds.isNotEmpty() && newOutgoing > 0) {
            listState.animateScrollToItem(0)
            newMessagesCount = 0
        }
        if (knownMessageIds.isNotEmpty() && newIncoming > 0) {
            if (shouldAutoScrollIncoming) {
                listState.animateScrollToItem(0)
                newMessagesCount = 0
            } else {
                newMessagesCount += newIncoming
            }
        }
        if (isAtBottom) {
            newMessagesCount = 0
        }
        knownMessageIds = currentIds
        wasAtBottomBeforeMessages = isAtBottom
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::backToChats) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                if (chat != null) {
                    Avatar(chat.peer.login, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = chat.peer.login,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.width(260.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (chat.muted) "Unmute" else "Mute", style = MaterialTheme.typography.titleMedium) },
                                leadingIcon = { Icon(if (chat.muted) Icons.Default.Notifications else Icons.Default.NotificationsOff, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.toggleMute()
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { Text(if (chat.blocked) "Unblock" else "Block user", style = MaterialTheme.typography.titleMedium) },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.toggleBlock()
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Chat", color = AstraxRed, style = MaterialTheme.typography.titleMedium) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AstraxRed) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.deleteChat()
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            MessageInput(
                value = state.draft,
                onValueChange = viewModel::setDraft,
                onSend = viewModel::sendMessage,
                enabled = chat?.blocked != true,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                items(state.messages.reversed(), key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isMine = message.senderId == state.userId,
                        isDark = state.isDarkTheme
                    )
                }
                item {
                    AnimatedVisibility(state.error != null) {
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            NewMessagesButton(
                count = newMessagesCount,
                visible = newMessagesCount > 0,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        newMessagesCount = 0
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun NewMessagesButton(
    count: Int,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = count.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto, isMine: Boolean, isDark: Boolean) {
    val bubbleColor = when {
        isMine -> AstraxAccent
        isDark -> Color(0xFF252C34)
        else -> Color.White
    }
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 6.dp,
                bottomEnd = if (isMine) 6.dp else 18.dp
            ),
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                MarkdownText(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeText(message.createdAt)
                    if (isMine) {
                        Spacer(Modifier.width(4.dp))
                        MessageStatusIcon(message.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(if (enabled) "Сообщение" else "Чат заблокирован") },
            enabled = enabled,
            maxLines = 4,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp))
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
