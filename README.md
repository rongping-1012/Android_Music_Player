# 音乐播放器 (Music Player)

一个基于 Jetpack Compose 构建的现代化 Android 音乐播放器应用，采用 MVVM 架构模式，提供完整的音乐播放、用户管理和数据持久化功能。

## 📱 功能特性

### 核心功能
- 🎵 **音乐播放**：支持本地音乐文件播放，提供播放/暂停、上一首/下一首、进度控制等功能
- 🔄 **播放模式**：支持顺序播放、随机播放、单曲循环三种播放模式
- 📂 **音乐管理**：自动扫描设备中的音乐文件，支持从指定文件夹读取音乐，支持音乐文件自动打包（首次启动自动复制到/Music/test/）
- ⏯️ **后台播放**：使用 Service 实现后台音乐播放，支持播放控制
- 🎤 **歌词显示**：支持LRC歌词解析和显示，播放页面左右滑动切换播放控制和歌词展示模块，支持桌面歌词悬浮窗（两行滚动显示：当前行深色高亮，下一行浅色）

### 用户功能
- 👤 **用户认证**：用户注册、登录功能，注册成功后自动登录，支持用户信息管理
- ⭐ **收藏功能**：收藏喜欢的音乐，支持收藏列表查看和管理
- 📜 **播放历史**：自动记录播放历史，方便用户快速找到最近播放的音乐
- 🎨 **主题切换**：支持深色/浅色主题切换（管理员页面固定为浅色主题），主题切换按钮位于播放控件最左侧
- 👥 **个人资料**：用户个人信息管理，包括性别、主题偏好等设置

### 管理员功能
- 🔐 **管理员系统**：管理员账号（admin）可查看和管理所有用户信息
- 📊 **用户管理**：查看用户列表、用户注册时间、最后登录时间等信息

## 🏗️ 技术架构

### 核心技术栈
- **UI 框架**：Jetpack Compose
- **架构模式**：MVVM (Model-View-ViewModel)
- **导航**：Navigation Compose
- **状态管理**：ViewModel + StateFlow/LiveData
- **本地数据库**：Room Database
- **数据持久化**：DataStore Preferences
- **图片加载**：Coil
- **文件访问**：DocumentFile API

### 主要组件

#### 1. UI 层 (Compose)
- `MainActivity.kt` - 应用入口，主题管理
- `navigation/AppNavigation.kt` - 导航配置
- `ui/screen/` - 各个功能页面
  - `SplashScreen.kt` - 启动页
  - `LoginScreen.kt` - 登录页
  - `RegisterScreen.kt` - 注册页
  - `MainScreen.kt` - 主页面
  - `UserHomeScreen.kt` - 用户主页
  - `AdminScreen.kt` - 管理员页面
  - `PlayerScreen.kt` - 播放器页面
  - `ProfileScreen.kt` - 个人资料页
  - `PlayHistoryScreen.kt` - 播放历史页
  - `FavoriteScreen.kt` - 收藏列表页
  - `AboutScreen.kt` - 关于页面
- `ui/component/` - 可复用组件
  - `BottomPlayerBar.kt` - 底部播放控制栏
  - `ScrollingTitle.kt` - 滚动标题组件
  - `LyricDisplay.kt` - 歌词展示组件
  - `BannerCarousel.kt` - 轮播图组件

#### 2. ViewModel 层
- `LoginViewModel.kt` - 登录/注册逻辑
- `UserViewModel.kt` - 用户主页音乐列表管理
- `PlayerViewModel.kt` - 播放器状态管理
- `ProfileViewModel.kt` - 个人资料和收藏/历史管理
- `AdminViewModel.kt` - 管理员功能

#### 3. 数据层
- `data/MusicRepository.kt` - 音乐文件数据仓库
- `data/UserRepository.kt` - 用户数据仓库
- `data/AppDatabase.kt` - Room 数据库配置
  - `User` - 用户实体
  - `PlayHistory` - 播放历史实体
  - `FavoriteMusic` - 收藏音乐实体
- `data/DataStoreManager.kt` - DataStore 偏好设置管理
- `data/UserService.kt` - 用户服务（登录状态、主题偏好）

#### 4. 服务层
- `service/MusicService.kt` - 后台音乐播放服务
- `service/MusicServiceConnection.kt` - 服务连接管理
- `service/FloatingLyricService.kt` - 桌面歌词悬浮窗服务（两行滚动显示）

#### 5. 工具类
- `utils/AssetMusicManager.kt` - 音乐文件自动打包管理器
- `utils/PermissionUtils.kt` - 权限管理工具
- `data/parser/LyricParser.kt` - LRC歌词解析器

## 📦 项目结构

```
app/src/main/java/com/example/music_player/
├── MainActivity.kt                 # 应用入口
├── data/                           # 数据层
│   ├── AppDatabase.kt             # Room 数据库
│   ├── MusicRepository.kt         # 音乐数据仓库
│   ├── UserRepository.kt          # 用户数据仓库
│   ├── UserService.kt             # 用户服务
│   ├── DataStoreManager.kt        # DataStore 管理
│   └── [实体类]                   # User, PlayHistory, FavoriteMusic 等
├── navigation/                     # 导航配置
│   ├── AppNavigation.kt          # 导航路由
│   └── Screen.kt                  # 屏幕路由定义
├── service/                        # 服务层
│   ├── MusicService.kt            # 音乐播放服务
│   └── MusicServiceConnection.kt  # 服务连接
└── ui/                             # UI 层
    ├── screen/                     # 各个功能页面
    ├── component/                  # 可复用组件
    ├── viewmodel/                  # ViewModel
    └── theme/                      # 主题配置
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 8 或更高版本
- Android SDK 24 (Android 7.0) 或更高版本
- Gradle 8.4.0
- Kotlin 1.9.22

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd music_player
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目根目录

3. **同步依赖**
   - Android Studio 会自动同步 Gradle 依赖
   - 如果未自动同步，点击 `File > Sync Project with Gradle Files`

4. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 `Run` 按钮或使用快捷键 `Shift + F10`

### 权限说明
应用需要以下权限：
- `READ_MEDIA_AUDIO` - 读取设备中的音频文件
- `READ_EXTERNAL_STORAGE` - 读取外部存储（Android 12 及以下）
- `WRITE_EXTERNAL_STORAGE` - 写入外部存储（Android 10 及以下，用于音乐文件自动复制）
- `SYSTEM_ALERT_WINDOW` - 悬浮窗权限（用于桌面歌词功能）
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - 前台服务权限（用于后台播放和桌面歌词）
- `INTERNET` - 网络访问（预留功能）

首次运行时，应用会请求存储权限以扫描音乐文件。使用桌面歌词功能时需要授权悬浮窗权限。

## 🎯 使用说明

### 用户操作流程

1. **首次使用**
   - 启动应用后进入启动页（已登录用户跳过动画直接进入主页面）
   - 点击"注册"创建新账号
   - 填写用户名、密码、性别等信息
   - 注册成功后自动登录并跳转到主页面

2. **登录**
   - 输入用户名和密码
   - 点击"登录"按钮
   - 登录成功后进入主页面

3. **播放音乐**
   - 在主页面查看音乐列表
   - 点击音乐项开始播放
   - 使用底部播放栏控制播放
   - 点击播放栏进入全屏播放器
   - 在播放页面左右滑动切换播放控制和歌词展示模块
   - 点击歌词跳转到对应播放时间并继续播放（播放和暂停状态下点击都会自动播放）
   - 点击桌面歌词按钮开启桌面歌词悬浮窗（需要授权悬浮窗权限）

4. **管理收藏**
   - 在播放器页面点击收藏按钮
   - 在"收藏"页面查看所有收藏的音乐
   - 点击收藏列表中的音乐，将整个收藏列表作为播放列表播放
   - 可以取消收藏

5. **查看历史**
   - 在"播放历史"页面查看最近播放的音乐
   - 点击历史记录，将整个播放历史列表作为播放列表播放

6. **个人设置**
   - 在"个人资料"页面修改个人信息
   - 切换深色/浅色主题
   - 查看关于页面（应用信息、功能列表、技术栈等）

### 管理员功能
- 使用 `admin` 账号登录
- 进入管理员页面查看所有用户信息
- 查看用户注册时间、最后登录时间等

## 🛠️ 技术实现细节

### MVVM 架构
- **Model**: Room 数据库实体、Repository 数据仓库
- **View**: Compose UI 组件
- **ViewModel**: 业务逻辑和状态管理

### 数据流
1. UI 层通过 ViewModel 获取数据
2. ViewModel 调用 Repository 获取数据
3. Repository 从 Room 数据库或系统 MediaStore 获取数据
4. 数据通过 StateFlow/LiveData 返回给 UI 层
5. UI 层自动更新

### 播放服务
- 使用 `MediaPlayer` 实现音频播放
- 通过 `Service` 实现后台播放
- 使用 `Binder` 实现 Activity 与 Service 通信
- 支持播放状态监听和进度更新

### 数据库设计
- **user_table**: 存储用户信息（用户名、密码、性别、主题偏好等）
- **play_history**: 存储播放历史（用户名、歌曲路径、播放时间）
- **favorite_music**: 存储收藏音乐（用户名、歌曲路径、歌曲名）

## 📝 开发规范

项目遵循 Android 官方开发规范和最佳实践：
- 使用 Kotlin 语言
- 遵循 Material Design 3 设计规范
- 使用 Compose 声明式 UI
- 采用单一数据源原则
- 使用协程处理异步操作

## 🔧 依赖库版本

主要依赖库版本（详见 `gradle/libs.versions.toml`）：
- Compose BOM: 2024.02.01
- Navigation Compose: 2.7.7
- Room: 2.6.1
- Lifecycle: 2.7.0
- DataStore: 1.1.1
- Coil: 2.5.0

## 📄 许可证

本项目仅供学习和参考使用。

## 👨‍💻 开发说明

### 代码结构说明
- 所有 Compose UI 组件使用 `@Composable` 注解
- ViewModel 继承自 `ViewModel` 或使用 `viewModel()` 函数
- Repository 负责数据访问，ViewModel 负责业务逻辑
- Service 使用 Binder 模式与 Activity 通信

### 扩展建议
- 添加网络音乐播放功能（使用 Retrofit）
- ~~添加歌词显示功能~~（已完成）
- ~~添加播放列表管理~~（已完成）
- ~~添加音乐搜索功能~~（已完成）
- ~~桌面歌词显示~~（已完成）
- 添加音频可视化效果
- 优化歌词显示效果（歌词动画、字体大小渐变等）
- 支持更多歌词格式（SRT、ASS等）

---

**注意**：本项目是一个学习项目，展示了如何使用现代 Android 开发技术栈构建一个完整的音乐播放器应用。

