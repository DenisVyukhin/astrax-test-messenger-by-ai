package com.astrax.app.data

import android.content.Context

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("astrax", Context.MODE_PRIVATE)

    fun token(): String? = prefs.getString("token", null)

    fun userId(): Long = prefs.getLong("userId", 0L)

    fun save(token: String, user: UserDto) {
        prefs.edit()
            .putString("token", token)
            .putLong("userId", user.id)
            .putString("login", user.login)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
