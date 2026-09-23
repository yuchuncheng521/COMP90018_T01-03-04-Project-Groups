package com.knot.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState = viewModel.uiState
    var email by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearPasswordResetState() }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.isPasswordResetEmailSent) {
                Text(text = "Check your email", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "We've sent a password reset link to $email. Follow the link to set a new password.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                TextButton(onClick = onNavigateBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Back to login")
                }
            } else {
                Text(text = "Reset your password", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Enter the email you signed up with and we'll send you a reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                Button(
                    onClick = { viewModel.sendPasswordResetEmail(email) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Send reset link")
                    }
                }
                TextButton(onClick = onNavigateBack, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Back to login")
                }
            }
        }
    }
}