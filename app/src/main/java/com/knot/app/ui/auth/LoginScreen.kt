package com.knot.app.ui.auth

import com.knot.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knot.app.model.UserAccount
import com.knot.app.ui.theme.JudsonFontFamily
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotTheme


@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    KnotTheme {
        LoginScreen(
            viewModel = AuthViewModel(), // see note below if this line doesn't compile/crashes the preview
            onLoginSuccess = {},
            onNavigateToSignUp = {}
        )
    }
}


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginScreen(
        viewModel: AuthViewModel,
        onLoginSuccess: (UserAccount) -> Unit,
        onNavigateToSignUp: () -> Unit
    ) {
        val uiState = viewModel.uiState
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        LaunchedEffect(uiState.loggedInUser) {
            uiState.loggedInUser?.let { onLoginSuccess(it) }
        }

        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 50.dp),
                verticalArrangement = Arrangement.Center
            ) {

                Image(
                    painter = painterResource(R.drawable.knot_blue),
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "Welcome\nback!",
                    fontFamily = JudsonFontFamily,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    color = KnotDarkBrown,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(60.dp))



                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Button(
                    onClick = { viewModel.login(email, password) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Log in")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Don't have an account yet?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateToSignUp) {
                        Text("Create one")
                    }
                }
            }
        }
    }

