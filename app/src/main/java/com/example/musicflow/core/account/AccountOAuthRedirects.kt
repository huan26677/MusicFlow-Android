package com.example.musicflow.core.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-local callback inbox. Navigation may not have created Account VM yet. */
object AccountOAuthRedirects {
    private val pending = MutableStateFlow<String?>(null)
    val latest = pending.asStateFlow()

    fun offer(url: String) {
        if (GoogleOAuthFlow.isCallback(url)) pending.value = url
    }

    fun consume(url: String) {
        if (pending.value == url) pending.value = null
    }
}
