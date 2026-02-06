package com.barter.core.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.barter.core.di.AppDI
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
) {
    val vm: AuthViewModel = remember { AppDI.get() }
    val form by vm.registerForm.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Join Barter",
                style = MaterialTheme.typography.headlineMedium,
                color = BarterTeal,
            )
            Text(
                "Start exchanging goods & services today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = form.displayName,
                onValueChange = vm::onRegisterNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display Name") },
                placeholder = { Text("Your name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterTeal,
                    focusedLabelColor = BarterTeal,
                    cursorColor = BarterTeal,
                ),
            )

            OutlinedTextField(
                value = form.location,
                onValueChange = vm::onRegisterLocationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Location") },
                placeholder = { Text("City, Country") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterTeal,
                    focusedLabelColor = BarterTeal,
                    cursorColor = BarterTeal,
                ),
            )

            OutlinedTextField(
                value = form.email,
                onValueChange = vm::onRegisterEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                placeholder = { Text("you@example.com") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterTeal,
                    focusedLabelColor = BarterTeal,
                    cursorColor = BarterTeal,
                ),
            )

            OutlinedTextField(
                value = form.password,
                onValueChange = vm::onRegisterPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterTeal,
                    focusedLabelColor = BarterTeal,
                    cursorColor = BarterTeal,
                ),
            )

            OutlinedTextField(
                value = form.confirmPassword,
                onValueChange = vm::onRegisterConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterTeal,
                    focusedLabelColor = BarterTeal,
                    cursorColor = BarterTeal,
                ),
            )

            form.error?.let { error ->
                Text(
                    error,
                    color = BarterCoral,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = vm::register,
                modifier = Modifier.fillMaxWidth(),
                enabled = !form.loading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
            ) {
                if (form.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Create Account", Modifier.padding(vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
