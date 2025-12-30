package com.example.music_player.ui.screen

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.music_player.ui.theme.ActiveColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.music_player.navigation.Screen
import com.example.music_player.ui.viewmodel.AuthUiState
import com.example.music_player.ui.viewmodel.LoginViewModel
import com.example.music_player.ui.viewmodel.LoginViewModelFactory

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel) {
    val context = LocalContext.current

    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Handle side effects of login state changes
    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is AuthUiState.Success -> {
                context.getSharedPreferences("UserPreferences", ComponentActivity.MODE_PRIVATE).edit().putString("currentUsername", username).apply()
                val route = if (state.isAdmin) Screen.AdminHome.route else Screen.Main.route
                navController.navigate(route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                viewModel.resetLoginState() // Reset state after navigation
            }
            is AuthUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetLoginState()
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF9B6ED3),
                        androidx.compose.ui.graphics.Color(0xFFC4B5FD),
                        androidx.compose.ui.graphics.Color(0xFFD8BFF0),
                        androidx.compose.ui.graphics.Color(0xFFFCC8E8),
                        androidx.compose.ui.graphics.Color(0xFFF4E5F9)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Music Player", 
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Cursive
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("登录以继续", style = MaterialTheme.typography.bodyMedium, color = ActiveColor)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = loginState is AuthUiState.Error
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = loginState is AuthUiState.Error
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loginState !is AuthUiState.Loading
                ) {
                    if (loginState is AuthUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("登录")
                    }
                }

                TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                    Text("没有账号？去注册")
                }

                TextButton(onClick = { showForgotPasswordDialog = true }) {
                    Text("忘记密码？")
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(viewModel = viewModel, onDismiss = { showForgotPasswordDialog = false })
    }
}

@Composable
fun ForgotPasswordDialog(viewModel: LoginViewModel, onDismiss: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val resetPasswordState by viewModel.resetPasswordState.collectAsStateWithLifecycle()

    LaunchedEffect(resetPasswordState) {
        when (val state = resetPasswordState) {
            is AuthUiState.Success -> {
                Toast.makeText(context, "密码重置成功", Toast.LENGTH_SHORT).show()
                viewModel.resetResetPasswordState()
                onDismiss()
            }
            is AuthUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetResetPasswordState()
            }
            else -> Unit
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重置密码") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("请输入您的用户名") },
                    isError = resetPasswordState is AuthUiState.Error,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("请输入旧密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = resetPasswordState is AuthUiState.Error,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("请输入新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = resetPasswordState is AuthUiState.Error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.resetPassword(username, oldPassword, newPassword) },
                enabled = resetPasswordState !is AuthUiState.Loading && 
                    username.isNotBlank() && oldPassword.isNotBlank() && newPassword.isNotBlank()
            ) {
                if (resetPasswordState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}