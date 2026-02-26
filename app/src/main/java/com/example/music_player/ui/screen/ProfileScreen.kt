package com.example.music_player.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource
import com.example.music_player.data.local.entity.PlayHistory
import com.example.music_player.navigation.Screen
import com.example.music_player.ui.viewmodel.ProfileUiState
import com.example.music_player.ui.viewmodel.ProfileViewModel
import com.example.music_player.ui.viewmodel.UpdateResultState
import androidx.compose.ui.text.font.FontFamily
import com.example.music_player.ui.theme.ActiveColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: ProfileViewModel, darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showEditNicknameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // Handle logout navigation
    LaunchedEffect(profileState) {
        if (profileState is ProfileUiState.LoggedOut) {
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
                        "My Profile", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Cursive,
                            color = ActiveColor
                        )
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "退出登录",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
        ) {
            when (val state = profileState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Success -> {
                    val user = state.user
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 头像
                        val avatarUri by viewModel.avatarUri.collectAsStateWithLifecycle()
                        val imagePicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: android.net.Uri? ->
                            uri?.let {
                                viewModel.setAvatarUri(it)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        // 直接使用 ViewModel 提供的带时间戳的 URI 字符串
                                        .data(avatarUri!!)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "头像（点击上传）",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // 根据性别显示默认头像
                                val defaultAvatarRes = if (user.gender == com.example.music_player.data.model.Gender.MALE) {
                                    com.example.music_player.R.drawable.default_man
                                } else {
                                    com.example.music_player.R.drawable.default_woman
                                }
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = defaultAvatarRes),
                                    contentDescription = "默认头像（点击上传）",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 用户名和昵称
                        Text(
                            text = user.nickname,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.foundation.Image(
                                painter = painterResource(
                                    id = if (user.gender == com.example.music_player.data.model.Gender.MALE) {
                                        com.example.music_player.R.drawable.male
                                    } else {
                                        com.example.music_player.R.drawable.female
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // 功能卡片
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // 修改资料
                                ProfileMenuItem(
                                    icon = Icons.Filled.Edit,
                                    title = "修改资料",
                                    onClick = { showEditNicknameDialog = true }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // 修改密码
                                ProfileMenuItem(
                                    icon = Icons.Filled.Lock,
                                    title = "修改密码",
                                    onClick = { showChangePasswordDialog = true }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // 选择音乐文件夹
                                SelectMusicFolderItem(
                                    onFolderSelected = { uri ->
                                        viewModel.loadMusicFromFolder(uri)
                                    }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // 我的喜欢
                                ProfileMenuItem(
                                    icon = Icons.Filled.Favorite,
                                    title = "我的喜欢",
                                    onClick = { navController.navigate(Screen.Favorite.route) }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // 播放历史
                                ProfileMenuItem(
                                    icon = Icons.Filled.History,
                                    title = "播放历史",
                                    onClick = { navController.navigate(Screen.PlayHistory.route) }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // 暗色模式
                                val isLoggedIn = profileState is ProfileUiState.Success
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clickable(enabled = isLoggedIn) { 
                                            if (isLoggedIn) {
                                                onToggleTheme()
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                            contentDescription = null,
                                            tint = if (isLoggedIn) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "暗色模式",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isLoggedIn) 
                                                MaterialTheme.colorScheme.onSurface 
                                            else 
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                    Switch(
                                        checked = darkTheme,
                                        onCheckedChange = { 
                                            // 只有在登录状态下才允许切换主题
                                            if (isLoggedIn) {
                                                onToggleTheme()
                                            }
                                        },
                                        enabled = isLoggedIn
                                    )
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // 关于
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clickable { 
                                            navController.navigate(Screen.About.route)
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "关于",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        
                    }
                }
                is ProfileUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ProfileUiState.LoggedOut -> { /* Handled by LaunchedEffect */ }
            }
        }
    }

    if (showEditNicknameDialog) {
        EditNicknameDialog(
            viewModel = viewModel,
            onDismiss = { showEditNicknameDialog = false }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            viewModel = viewModel,
            onDismiss = { showChangePasswordDialog = false }
        )
    }
}

@Composable
fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SelectMusicFolderItem(onFolderSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onFolderSelected(it)
            Toast.makeText(context, "已选择音乐文件夹", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                folderLauncher.launch(null)
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "选择音乐文件夹",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EditNicknameDialog(viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val currentUser = (profileState as? ProfileUiState.Success)?.user
    
    var newNickname by remember { mutableStateOf(currentUser?.nickname ?: "") }
    var selectedGender by remember { mutableStateOf(currentUser?.gender ?: com.example.music_player.data.model.Gender.MALE) }
    var pendingGenderUpdate by remember { mutableStateOf<com.example.music_player.data.model.Gender?>(null) }
    val updateState by viewModel.updateNicknameState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(currentUser) {
        currentUser?.let {
            newNickname = it.nickname
            selectedGender = it.gender
        }
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateResultState.Success -> {
                // 如果还有待更新的性别，先更新性别
                if (pendingGenderUpdate != null) {
                    val genderToUpdate = pendingGenderUpdate
                    pendingGenderUpdate = null
                    viewModel.updateGender(genderToUpdate!!)
                } else {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetUpdateStates()
                    onDismiss()
                }
            }
            is UpdateResultState.Error -> {
                pendingGenderUpdate = null
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateStates()
            }
            else -> Unit
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改资料") },
        text = {
            Column {
                OutlinedTextField(
                    value = newNickname,
                    onValueChange = { newNickname = it },
                    label = { Text("昵称") },
                    isError = updateState is UpdateResultState.Error,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
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
                            .clickable { selectedGender = com.example.music_player.data.model.Gender.MALE }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGender == com.example.music_player.data.model.Gender.MALE,
                            onClick = { selectedGender = com.example.music_player.data.model.Gender.MALE }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("男生")
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedGender = com.example.music_player.data.model.Gender.FEMALE }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGender == com.example.music_player.data.model.Gender.FEMALE,
                            onClick = { selectedGender = com.example.music_player.data.model.Gender.FEMALE }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("女生")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nicknameChanged = newNickname != currentUser?.nickname
                    val genderChanged = selectedGender != currentUser?.gender
                    
                    if (!nicknameChanged && !genderChanged) {
                        onDismiss()
                        return@Button
                    }
                    
                    if (nicknameChanged && genderChanged) {
                        // 如果两个都变了，先更新昵称，然后在成功回调中更新性别
                        pendingGenderUpdate = selectedGender
                        viewModel.updateNickname(newNickname)
                    } else if (nicknameChanged) {
                        viewModel.updateNickname(newNickname)
                    } else if (genderChanged) {
                        viewModel.updateGender(selectedGender)
                    }
                },
                enabled = updateState !is UpdateResultState.Loading && newNickname.isNotBlank()
            ) {
                if (updateState is UpdateResultState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("确定")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ChangePasswordDialog(viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    val updateState by viewModel.updatePasswordState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateResultState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateStates()
                onDismiss()
            }
            is UpdateResultState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateStates()
            }
            else -> Unit
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("原密码") },
                    visualTransformation = if (oldPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = updateState is UpdateResultState.Error,
                    trailingIcon = {
                        IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                            Icon(
                                imageVector = if (oldPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (oldPasswordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码") },
                    visualTransformation = if (newPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = updateState is UpdateResultState.Error,
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (newPasswordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.changePassword(oldPassword, newPassword) },
                enabled = updateState !is UpdateResultState.Loading && oldPassword.isNotBlank() && newPassword.isNotBlank()
            ) {
                if (updateState is UpdateResultState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("确定")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
