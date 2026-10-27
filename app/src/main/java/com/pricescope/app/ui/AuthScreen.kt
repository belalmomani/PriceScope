package com.pricescope.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pricescope.app.R
import com.pricescope.app.auth.AuthViewModel
import com.pricescope.app.designsystem.Dimens
import com.pricescope.app.designsystem.PsPrimaryButton

@Composable
fun AuthScreen(vm: AuthViewModel, onBack: () -> Unit) {
    val state = vm.state
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var signUpMode by rememberSaveable { mutableStateOf(false) }

    SubScreen(stringResource(R.string.setting_account), onBack) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding)
        ) {
            if (state.signedIn) {
                Text(
                    stringResource(R.string.signed_in_as, state.signedInEmail.orEmpty()),
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(
                    onClick = { vm.signOut() },
                    enabled = !state.loading,
                    shape = RoundedCornerShape(Dimens.ControlRadius),
                    modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouch)
                ) { Text(stringResource(R.string.sign_out)) }
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; vm.clearMessages() },
                    label = { Text(stringResource(R.string.auth_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; vm.clearMessages() },
                    label = { Text(stringResource(R.string.auth_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                state.error?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                state.info?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                }
                PsPrimaryButton(
                    text = stringResource(if (signUpMode) R.string.sign_up else R.string.sign_in),
                    onClick = { if (signUpMode) vm.signUp(email, password) else vm.signIn(email, password) },
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { signUpMode = !signUpMode; vm.clearMessages() },
                    modifier = Modifier.heightIn(min = Dimens.MinTouch)
                ) {
                    Text(stringResource(if (signUpMode) R.string.switch_to_sign_in else R.string.switch_to_sign_up))
                }
            }
        }
    }
}
