package com.barter.core.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.AuthViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
) {
    val vm: AuthViewModel = remember { AppDI.get() }
    val form by vm.loginForm.collectAsState()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        // Gradient header
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BarterTeal, BarterTealLight),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "\uD83D\uDD04",
                    fontSize = 48.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Barter",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Exchange goods & services",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // Login form card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(6.dp),
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Welcome back",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BarterDark,
                )

                OutlinedTextField(
                    value = form.email,
                    onValueChange = vm::onLoginEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    placeholder = { Text("ion@barter.md") },
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
                    onValueChange = vm::onLoginPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
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

                Button(
                    onClick = vm::login,
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
                        Text("Sign In", Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Register link
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onNavigateToRegister) {
                Text(
                    "Register",
                    color = BarterTeal,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
