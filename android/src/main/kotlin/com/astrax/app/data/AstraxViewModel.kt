package com.astrax.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AstraxViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = TokenStore(application)
    private val api = ApiClient(tokenStore)
    private var socketJob: Job? = null
    private var pollingJob: Job? = null
    private var chatsPollingJob: Job? = null
    private var userSearchJob: Job? = null
    private var socket: DefaultClientWebSocketSession? = null

    private val _state = MutableStateFlow(AstraxState(isAuthorized = tokenStore.token() != null, userId = tokenStore.userId()))
    val state: StateFlow<AstraxState> = _state

    init {
        if (_state.value.isAuthorized) loadChats()
    }

    fun setDarkTheme(enabled: Boolean) {
        _state.update { it.copy(isDarkTheme = enabled) }
    }

    fun switchAuthMode() {
        _state.update { it.copy(authMode = if (it.authMode == AuthMode.Login) AuthMode.Register else AuthMode.Login, error = null) }
    }

    fun login(login: String, password: String) = auth(AuthMode.Login, login, password)

    fun register(login: String, password: String) = auth(AuthMode.Register, login, password)

    fun logout() {
        viewModelScope.launch {
            userSearchJob?.cancel()
            stopChatsPolling()
            closeSocket()
            tokenStore.clear()
            _state.value = AstraxState()
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            runRequest {
                val chats = api.chats()
                _state.update { it.copy(chats = chats, screen = Screen.Chats, error = null) }
                startChatsPolling()
            }
        }
    }

    fun createChat(login: String) {
        if (login.isBlank()) return
        if (!_state.value.isAuthorized) {
            _state.update { it.copy(searchError = "Не авторизованы") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, searchError = null) }
            runCatching { api.createChat(login.trim()) }
                .onSuccess { chat ->
                    userSearchJob?.cancel()
                    _state.update { it.copy(isLoading = false, chats = (listOf(chat) + it.chats.filterNot { item -> item.id == chat.id }), newChatLogin = "", userSuggestions = emptyList()) }
                    openChat(chat)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            searchError = if (error.message?.contains("not found", ignoreCase = true) == true) "Такого пользователя не существует" else error.message ?: "Ошибка поиска"
                        )
                    }
                }
        }
    }

    fun clearSearchError() {
        _state.update { it.copy(searchError = null) }
    }

    fun openChat(chat: ChatDto) {
        viewModelScope.launch {
            stopChatsPolling()
            closeSocket()
            _state.update { it.copy(screen = Screen.Chat, activeChat = chat, messages = emptyList(), error = null) }
            runRequest {
                val messages = api.messages(chat.id)
                _state.update { it.copy(messages = messages) }
                connectSocket(chat.id)
                startPolling(chat.id)
            }
        }
    }

    fun backToChats() {
        viewModelScope.launch {
            closeSocket()
            loadChats()
        }
    }

    fun setDraft(value: String) {
        _state.update { it.copy(draft = value.take(2000)) }
    }

    fun setNewChatLogin(value: String) {
        if (!_state.value.isAuthorized) {
            _state.update { it.copy(searchError = "Не авторизованы") }
            return
        }
        val query = cleanLoginInput(value)
        _state.update { it.copy(newChatLogin = query, searchError = null) }
        userSearchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(userSuggestions = emptyList()) }
            return
        }
        userSearchJob = viewModelScope.launch {
            delay(180)
            val users = runCatching { api.searchUsers(query) }.getOrNull().orEmpty()
            _state.update { state ->
                if (state.newChatLogin == query) state.copy(userSuggestions = users) else state
            }
        }
    }

    fun sendMessage() {
        val chat = _state.value.activeChat ?: return
        if (!_state.value.isAuthorized || _state.value.userId == 0L) {
            _state.update { it.copy(error = "Не авторизованы") }
            return
        }
        val text = _state.value.draft.trim()
        if (text.isBlank()) return
        val local = MessageDto(
            id = -System.currentTimeMillis(),
            chatId = chat.id,
            senderId = _state.value.userId,
            text = text,
            createdAt = System.currentTimeMillis(),
            status = MessageStatus.sending
        )
        _state.update { it.copy(draft = "", messages = it.messages + local) }
        viewModelScope.launch {
            runCatching {
                val message = api.sendMessage(chat.id, text)
                applyIncomingMessage(message)
                _state.update { it.copy(error = null) }
            }.onFailure { error ->
                _state.update { state ->
                    state.copy(
                        messages = state.messages.filterNot { it.id == local.id },
                        draft = text,
                        error = error.message ?: "Message was not sent"
                    )
                }
            }
        }
    }

    fun toggleMute() {
        val chat = _state.value.activeChat ?: return
        viewModelScope.launch {
            runRequest {
                val updated = api.setMuted(chat.id, !chat.muted)
                replaceActiveChat(updated)
            }
        }
    }

    fun toggleBlock() {
        val chat = _state.value.activeChat ?: return
        viewModelScope.launch {
            runRequest {
                val updated = api.setBlocked(chat.id, !chat.blocked)
                replaceActiveChat(updated)
            }
        }
    }

    fun deleteChat() {
        val chat = _state.value.activeChat ?: return
        viewModelScope.launch {
            runRequest {
                api.deleteChat(chat.id)
                closeSocket()
                _state.update { it.copy(screen = Screen.Chats, activeChat = null, messages = emptyList(), chats = it.chats.filterNot { item -> item.id == chat.id }) }
                startChatsPolling()
            }
        }
    }

    private fun auth(mode: AuthMode, login: String, password: String) {
        viewModelScope.launch {
            runRequest {
                val response = if (mode == AuthMode.Login) api.login(login.trim(), password) else api.register(login.trim(), password)
                tokenStore.save(response.token, response.user)
                _state.update { it.copy(isAuthorized = true, userId = response.user.id, error = null) }
                loadChats()
            }
        }
    }

    private suspend fun connectSocket(chatId: Long) {
        socket = runCatching { api.chatSocket(chatId) }.getOrNull()
        socketJob = viewModelScope.launch {
            val activeSocket = socket ?: return@launch
            for (frame in activeSocket.incoming) {
                if (frame !is Frame.Text) continue
                val event = runCatching { api.json.decodeFromString(MessageEvent.serializer(), frame.readText()) }.getOrNull() ?: continue
                when (event.type) {
                    "message" -> event.message?.let { applyIncomingMessage(it) }
                    "read" -> applyRead(event.messageIds)
                }
            }
        }
    }

    private fun startPolling(chatId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(2_000)
                val messages = runCatching { api.messages(chatId) }.getOrNull() ?: continue
                _state.update { state ->
                    if (state.activeChat?.id != chatId) state else state.copy(messages = messages.sortedBy { it.createdAt })
                }
            }
        }
    }

    private fun startChatsPolling() {
        chatsPollingJob?.cancel()
        chatsPollingJob = viewModelScope.launch {
            while (true) {
                delay(2_000)
                val chats = runCatching { api.chats() }.getOrNull() ?: continue
                _state.update { state ->
                    if (state.screen != Screen.Chats) state else state.copy(chats = chats)
                }
            }
        }
    }

    private fun stopChatsPolling() {
        chatsPollingJob?.cancel()
        chatsPollingJob = null
    }

    private fun applyIncomingMessage(message: MessageDto) {
        _state.update { state ->
            val filtered = state.messages.filterNot { it.id < 0 && it.text == message.text && it.senderId == message.senderId }
            state.copy(
                messages = (filtered + message).distinctBy { it.id }.sortedBy { it.createdAt },
                chats = state.chats.map { chat ->
                    if (chat.id == message.chatId) chat.copy(lastMessage = message) else chat
                }
            )
        }
    }

    private fun applyRead(ids: List<Long>) {
        if (ids.isEmpty()) return
        _state.update { state ->
            state.copy(messages = state.messages.map { if (it.id in ids) it.copy(status = MessageStatus.read) else it })
        }
    }

    private fun replaceActiveChat(chat: ChatDto) {
        _state.update {
            it.copy(
                activeChat = chat,
                chats = it.chats.map { item -> if (item.id == chat.id) chat else item }
            )
        }
    }

    private suspend fun closeSocket() {
        pollingJob?.cancel()
        socketJob?.cancel()
        socket?.close(CloseReason(CloseReason.Codes.NORMAL, "screen changed"))
        pollingJob = null
        socketJob = null
        socket = null
    }

    private suspend fun runRequest(block: suspend () -> Unit) {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { block() }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Request failed") } }
        _state.update { it.copy(isLoading = false) }
    }
}

data class AstraxState(
    val isAuthorized: Boolean = false,
    val userId: Long = 0L,
    val isDarkTheme: Boolean = false,
    val authMode: AuthMode = AuthMode.Login,
    val screen: Screen = Screen.Chats,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchError: String? = null,
    val userSuggestions: List<UserDto> = emptyList(),
    val chats: List<ChatDto> = emptyList(),
    val activeChat: ChatDto? = null,
    val messages: List<MessageDto> = emptyList(),
    val draft: String = "",
    val newChatLogin: String = ""
)

enum class AuthMode {
    Login,
    Register
}

enum class Screen {
    Chats,
    Chat
}

private fun cleanLoginInput(value: String): String =
    value.trim().filter { it.isLetterOrDigit() || it == '_' }.filter { it.code < 128 }.lowercase().take(32)
