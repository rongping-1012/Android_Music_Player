package com.example.music_player.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.music_player.ui.theme.ActiveColor
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.music_player.navigation.Screen
import com.example.music_player.ui.viewmodel.AuthUiState
import com.example.music_player.ui.viewmodel.LoginViewModel
import com.example.music_player.data.Gender

@Composable
fun RegisterScreen(navController: NavController, viewModel: LoginViewModel) {
    var nickname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf<Gender>(Gender.MALE) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()
    
    // 验证用户名格式：只能包含字母、数字、下划线
    fun isValidUsername(username: String): Boolean {
        return username.matches(Regex("^[a-zA-Z0-9_]+$"))
    }
    
    // 过滤用户名输入，只允许字母、数字、下划线
    fun filterUsername(input: String): String {
        return input.filter { it.isLetterOrDigit() || it == '_' }
    }

    // Handle side effects of register state changes
    LaunchedEffect(registerState) {
        when (val state = registerState) {
            is AuthUiState.Success -> {
                Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
                viewModel.resetRegisterState()
            }
            is AuthUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetRegisterState()
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
                Text("创建新账号", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("加入我们，开始音乐之旅", style = MaterialTheme.typography.bodyMedium, color = ActiveColor)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = registerState is AuthUiState.Error
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        val filtered = filterUsername(it)
                        username = filtered
                        // 实时验证用户名格式
                        usernameError = if (filtered.isNotEmpty() && !isValidUsername(filtered)) {
                            "用户名只能包含字母、数字和下划线"
                        } else {
                            null
                        }
                    },
                    label = { Text("用户名 (字母、数字、下划线)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = registerState is AuthUiState.Error || usernameError != null,
                    supportingText = {
                        if (usernameError != null) {
                            Text(
                                text = usernameError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码 (至少6位)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = registerState is AuthUiState.Error
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 性别选择
                Text(
                    text = "性别",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedGender = Gender.MALE }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGender == Gender.MALE,
                            onClick = { selectedGender = Gender.MALE }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("男生")
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedGender = Gender.FEMALE }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGender == Gender.FEMALE,
                            onClick = { selectedGender = Gender.FEMALE }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("女生")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { 
                        // 验证用户名格式
                        if (!isValidUsername(username)) {
                            usernameError = "用户名只能包含字母、数字和下划线"
                            return@Button
                        }
                        usernameError = null
                        viewModel.register(nickname, username, password, selectedGender) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = registerState !is AuthUiState.Loading && username.isNotBlank() && password.isNotBlank() && nickname.isNotBlank() && usernameError == null
                ) {
                    if (registerState is AuthUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("注册")
                    }
                }

                TextButton(onClick = { navController.popBackStack() }) {
                    Text("已有账号？返回登录")
                }
            }
        }
    }
}