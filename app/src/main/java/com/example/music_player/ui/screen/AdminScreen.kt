package com.example.music_player.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.runtime.rememberCoroutineScope
import com.example.music_player.data.DataStoreManager
import com.example.music_player.data.User
import com.example.music_player.data.UserService
import com.example.music_player.navigation.Screen
import com.example.music_player.ui.theme.ActiveColor
import com.example.music_player.ui.viewmodel.AdminUiState
import com.example.music_player.ui.viewmodel.AdminViewModel
import com.example.music_player.ui.viewmodel.UserListUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController, viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adminState by viewModel.adminState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<User?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf<User?>(null) }
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    
    // 获取当前登录用户信息
    var currentUsername by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        val userService = UserService(context)
        currentUsername = userService.currentUsername.first()
    }

    // Handle logout navigation
    LaunchedEffect(adminState) {
        if (adminState is AdminUiState.LoggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true } // Clear back stack
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "User Management",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Cursive
                        ),
                        fontWeight = FontWeight.Bold,
                        color = ActiveColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is UserListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is UserListUiState.Success -> {
                    if (state.users.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "暂无用户",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // 退出登录按钮
                            Button(
                                onClick = { viewModel.logout() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("退出登录", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.users) { user ->
                                    UserItem(
                                        user = user,
                                        dataStoreManager = dataStoreManager,
                                        onDeleteClick = { showDeleteDialog = user },
                                        onChangePasswordClick = { showChangePasswordDialog = user }
                                    )
                                }
                            }
                            
                            // 退出登录按钮
                            Button(
                                onClick = { viewModel.logout() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("退出登录", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                is UserListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    showDeleteDialog?.let {
        DeleteConfirmationDialog(
            user = it,
            onConfirm = { 
                viewModel.deleteUser(it)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
    
    showChangePasswordDialog?.let {
        AdminChangePasswordDialog(
            user = it,
            viewModel = viewModel,
            onDismiss = { showChangePasswordDialog = null }
        )
    }
}

@Composable
fun UserItem(
    user: User, 
    dataStoreManager: DataStoreManager, 
    onDeleteClick: () -> Unit,
    onChangePasswordClick: () -> Unit
) {
    val context = LocalContext.current
    val avatarUri = remember(user.username) {
        dataStoreManager.getAvatarPath(user.username)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 用户头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(java.io.File(avatarUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "用户头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 根据性别显示默认头像
                    val defaultAvatarRes = if (user.gender == com.example.music_player.data.Gender.MALE) {
                        com.example.music_player.R.drawable.default_man
                    } else {
                        com.example.music_player.R.drawable.default_woman
                    }
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = defaultAvatarRes),
                        contentDescription = "默认头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // 用户信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(
                            id = if (user.gender == com.example.music_player.data.Gender.MALE) {
                                com.example.music_player.R.drawable.male
                            } else {
                                com.example.music_player.R.drawable.female
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (user.lastLoginTime != null) {
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        "最后登录: ${dateFormat.format(user.lastLoginTime)}"
                    } else {
                        "从未登录"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // 操作按钮
            Row {
                IconButton(
                    onClick = onChangePasswordClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "修改密码",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除用户",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AdminChangePasswordDialog(
    user: User,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column {
                Text(
                    text = "用户: ${user.username} (${user.nickname})",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.isNotBlank() && newPassword.length >= 6) {
                        isLoading = true
                        scope.launch {
                            val result = viewModel.adminUpdatePassword(user.username, newPassword)
                            isLoading = false
                            result.onSuccess {
                                android.widget.Toast.makeText(context, "密码修改成功", android.widget.Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }.onFailure {
                                android.widget.Toast.makeText(context, it.message ?: "修改失败", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        android.widget.Toast.makeText(context, "密码至少需要6位", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isLoading && newPassword.isNotBlank()
            ) {
                if (isLoading) {
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

@Composable
fun DeleteConfirmationDialog(user: User, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { 
            Text(
                "确认删除",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = { 
            Text(
                "确定要删除用户 '${user.username}' (${user.nickname}) 吗？\n此操作无法撤销。",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
