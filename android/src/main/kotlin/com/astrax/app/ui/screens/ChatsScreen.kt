package com.astrax.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.astrax.app.data.AstraxState
import com.astrax.app.data.AstraxViewModel
import com.astrax.app.data.ChatDto
import com.astrax.app.data.UserDto
import com.astrax.app.ui.components.Avatar
import com.astrax.app.ui.components.TimeText
import com.astrax.app.ui.theme.AstraxGreen

@Composable
fun ChatsScreen(state: AstraxState, viewModel: AstraxViewModel) {
    val context = LocalContext.current
    LaunchedEffect(state.searchError) {
        state.searchError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSearchError()
        }
    }

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Astrax",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.setDarkTheme(!state.isDarkTheme) }) {
                        Icon(if (state.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null)
                    }
                    IconButton(onClick = viewModel::logout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.newChatLogin,
                        onValueChange = viewModel::setNewChatLogin,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Логин собеседника") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.createChat(state.newChatLogin) }),
                        shape = RoundedCornerShape(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.createChat(state.newChatLogin) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                AnimatedVisibility(state.userSuggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                            .padding(vertical = 6.dp)
                    ) {
                        state.userSuggestions.forEach { user ->
                            UserSuggestionRow(
                                user = user,
                                onClick = { viewModel.createChat(user.login) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state.chats, key = { it.id }) { chat ->
                ChatRow(chat = chat, onClick = { viewModel.openChat(chat) })
            }
        }
    }
}

@Composable
private fun UserSuggestionRow(user: UserDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(user.login, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = user.login,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatRow(chat: ChatDto, onClick: () -> Unit) {
    val unreadSize by animateDpAsState(if (chat.unreadCount > 0) 24.dp else 0.dp, label = "unread")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.peer.login)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.peer.login,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                chat.lastMessage?.let { TimeText(it.createdAt) }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = chat.lastMessage?.text?.plainMessagePreview() ?: "Нет сообщений",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedVisibility(chat.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(unreadSize)
                    .clip(CircleShape)
                    .background(AstraxGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.unreadCount.coerceAtMost(99).toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun String.plainMessagePreview(): String =
    replace("**", "")
        .replace("__", "")
        .replace("~~", "")
        .replace("*", "")
        .trim()
