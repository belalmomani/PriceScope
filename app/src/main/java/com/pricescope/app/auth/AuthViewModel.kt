package com.pricescope.app.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricescope.app.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class AuthUiState(
    val ready: Boolean = false,
    val loading: Boolean = false,
    val signedInEmail: String? = null,
    @StringRes val error: Int? = null,
    @StringRes val info: Int? = null
) {
    val signedIn: Boolean get() = signedInEmail != null
}

class AuthViewModel : ViewModel() {
    var state by mutableStateOf(AuthUiState())
        private set

    private val auth get() = SupabaseProvider.client.auth

    init {
        viewModelScope.launch {
            runCatching { auth.awaitInitialization() }
            refresh()
        }
    }

    fun clearMessages() {
        state = state.copy(error = null, info = null)
    }

    fun signIn(emailInput: String, passwordInput: String) {
        if (!validate(emailInput, passwordInput)) return
        run {
            auth.signInWith(Email) {
                email = emailInput.trim()
                password = passwordInput
            }
            refresh()
        }
    }

    fun signUp(emailInput: String, passwordInput: String) {
        if (!validate(emailInput, passwordInput)) return
        run {
            auth.signUpWith(Email) {
                email = emailInput.trim()
                password = passwordInput
            }
            refresh()
            // With email confirmation on, there is no session until the link is opened.
            if (!state.signedIn) state = state.copy(info = R.string.auth_confirm_email)
        }
    }

    fun signOut() {
        run {
            auth.signOut()
            refresh()
        }
    }

    private fun validate(email: String, password: String): Boolean {
        val ok = AuthValidator.isValidEmail(email) && AuthValidator.isValidPassword(password)
        if (!ok) state = state.copy(error = R.string.auth_invalid_input, info = null)
        return ok
    }

    private fun refresh() {
        val user = runCatching { auth.currentUserOrNull() }.getOrNull()
        state = state.copy(ready = true, loading = false, signedInEmail = user?.email)
    }

    private fun run(block: suspend () -> Unit) {
        state = state.copy(loading = true, error = null, info = null)
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(loading = false, error = map(e))
            }
        }
    }

    private fun map(e: Exception): Int {
        val msg = e.message.orEmpty()
        return when {
            e is IOException -> R.string.auth_network_error
            msg.contains("Invalid login credentials", ignoreCase = true) -> R.string.auth_invalid_credentials
            msg.contains("already", ignoreCase = true) && msg.contains("regist", ignoreCase = true) ->
                R.string.auth_already_registered
            else -> R.string.auth_generic_error
        }
    }
}
