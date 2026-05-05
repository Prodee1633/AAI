package com.example.aiprojectcoder.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.aiprojectcoder.data.AppRepository
import com.example.aiprojectcoder.data.ChatAttachmentInfo
import com.example.aiprojectcoder.data.ChatMessage
import com.example.aiprojectcoder.data.ChatRole
import com.example.aiprojectcoder.data.ModelConfig
import com.example.aiprojectcoder.data.ProjectProfile
import com.example.aiprojectcoder.data.ProviderType
import com.example.aiprojectcoder.files.PatchParser
import com.example.aiprojectcoder.files.PatchPlan
import com.example.aiprojectcoder.files.ProjectFileStore
import com.example.aiprojectcoder.files.ProjectSnapshot
import com.example.aiprojectcoder.llm.LlmClientFactory
import com.example.aiprojectcoder.llm.LlmResponse
import com.example.aiprojectcoder.llm.PromptAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Default.Home),
    CHAT("聊天", Icons.Default.Chat),
    SETTINGS("设置", Icons.Default.Settings)
}

private data class PendingAttachment(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val byteSize: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    val context = LocalContext.current
    val repo = remember { AppRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val initialProjects = remember { repo.readProjects() }
    var projects by remember { mutableStateOf(initialProjects) }
    var activeProjectId by remember {
        mutableStateOf(
            repo.readActiveProjectId()?.takeIf { id -> initialProjects.any { it.id == id } }
                ?: initialProjects.firstOrNull()?.id
        )
    }

    val initialConfigs = remember { repo.readModelConfigs() }
    var modelConfigs by remember { mutableStateOf(initialConfigs) }
    var activeConfigId by remember {
        mutableStateOf(
            repo.readActiveModelConfigId()?.takeIf { id -> initialConfigs.any { it.id == id } }
                ?: initialConfigs.firstOrNull()?.id
        )
    }
    var apiKey by remember { mutableStateOf(activeConfigId?.let(repo::readApiKey).orEmpty()) }
    var autoApplyWithoutConfirmation by remember { mutableStateOf(repo.readAutoApplyWithoutConfirmation()) }

    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var prompt by remember { mutableStateOf("") }
    var selectedAttachments by remember { mutableStateOf<List<PendingAttachment>>(emptyList()) }
    var snapshot by remember { mutableStateOf<ProjectSnapshot?>(null) }
    var plan by remember { mutableStateOf<PatchPlan?>(null) }
    var rawModelOutput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("在首页添加或选择项目，然后扫描项目。") }
    var busy by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(activeProjectId?.let(repo::readChatMessages).orEmpty()) }

    val activeProject = projects.firstOrNull { it.id == activeProjectId }
    val activeConfig = modelConfigs.firstOrNull { it.id == activeConfigId } ?: modelConfigs.firstOrNull()

    fun persistProjects(next: List<ProjectProfile>, nextActiveId: String?) {
        projects = next
        activeProjectId = nextActiveId
        repo.saveProjects(next)
        repo.saveActiveProjectId(nextActiveId)
    }

    fun selectProject(project: ProjectProfile) {
        val updatedProject = project.copy(lastOpenedAtMillis = System.currentTimeMillis())
        val next = projects.map { if (it.id == project.id) updatedProject else it }
        persistProjects(next, project.id)
        messages = repo.readChatMessages(project.id)
        snapshot = null
        plan = null
        rawModelOutput = ""
        status = "当前项目：${project.name}。请扫描后开始聊天。"
        selectedTab = AppTab.HOME
    }

    val openTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            val name = DocumentFile.fromTreeUri(context, uri)?.name
                ?.takeIf { it.isNotBlank() }
                ?: "Project ${projects.size + 1}"
            val existing = projects.firstOrNull { it.uri == uri.toString() }
            val project = existing?.copy(name = existing.name, lastOpenedAtMillis = System.currentTimeMillis())
                ?: ProjectProfile(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    uri = uri.toString()
                )
            val next = if (existing == null) projects + project else projects.map { if (it.id == existing.id) project else it }
            persistProjects(next, project.id)
            messages = repo.readChatMessages(project.id)
            snapshot = null
            plan = null
            rawModelOutput = ""
            status = "已授权项目目录：${project.name}"
            selectedTab = AppTab.HOME
        }
    }

    fun addPendingUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val additions = uris.map { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val doc = DocumentFile.fromSingleUri(context, uri)
            PendingAttachment(
                uri = uri,
                name = doc?.name?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment.orEmpty().ifBlank { "attachment" },
                mimeType = context.contentResolver.getType(uri) ?: doc?.type ?: "application/octet-stream",
                byteSize = doc?.length() ?: 0L
            )
        }
        selectedAttachments = (selectedAttachments + additions).distinctBy { it.uri.toString() }.takeLast(12)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        addPendingUris(uris)
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        addPendingUris(uris)
    }

    fun saveModelConfig(config: ModelConfig, key: String) {
        val cleaned = config.copy(
            displayName = config.displayName.ifBlank { config.provider.label },
            model = config.model.ifBlank {
                when (config.provider) {
                    ProviderType.OPENAI_COMPATIBLE -> "gpt-4.1"
                    ProviderType.GEMINI -> "gemini-2.5-pro"
                    ProviderType.ANTHROPIC -> "claude-sonnet-4-5"
                }
            },
            baseUrl = if (config.provider == ProviderType.OPENAI_COMPATIBLE) {
                config.baseUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
            } else {
                ""
            }
        )
        val next = if (modelConfigs.any { it.id == cleaned.id }) {
            modelConfigs.map { if (it.id == cleaned.id) cleaned else it }
        } else {
            modelConfigs + cleaned
        }
        modelConfigs = next
        activeConfigId = cleaned.id
        apiKey = key
        repo.saveModelConfigs(next)
        repo.saveActiveModelConfigId(cleaned.id)
        repo.saveApiKey(cleaned.id, key)
        scope.launch { snackbarHostState.showSnackbar("模型配置已保存") }
    }

    fun selectModelConfig(configId: String) {
        activeConfigId = configId
        apiKey = repo.readApiKey(configId)
        repo.saveActiveModelConfigId(configId)
    }

    fun deleteModelConfig(config: ModelConfig) {
        val next = modelConfigs.filterNot { it.id == config.id }
        repo.clearApiKey(config.id)
        modelConfigs = next
        val nextActive = if (activeConfigId == config.id) next.firstOrNull()?.id else activeConfigId
        activeConfigId = nextActive
        apiKey = nextActive?.let(repo::readApiKey).orEmpty()
        repo.saveModelConfigs(next)
        repo.saveActiveModelConfigId(nextActive)
        scope.launch { snackbarHostState.showSnackbar("模型配置已删除") }
    }

    fun renameActiveProject(newName: String) {
        val project = activeProject ?: return
        val cleanName = newName.ifBlank { project.name }
        val next = projects.map { if (it.id == project.id) it.copy(name = cleanName) else it }
        persistProjects(next, project.id)
    }

    fun deleteProject(project: ProjectProfile) {
        val next = projects.filterNot { it.id == project.id }
        repo.clearChatMessages(project.id)
        val nextActive = if (activeProjectId == project.id) next.firstOrNull()?.id else activeProjectId
        persistProjects(next, nextActive)
        messages = nextActive?.let(repo::readChatMessages).orEmpty()
        snapshot = null
        plan = null
        rawModelOutput = ""
        status = if (nextActive == null) "还没有项目。请添加一个项目。" else "项目已移除。"
    }

    fun scanProject() {
        val project = activeProject
        if (project == null) {
            scope.launch { snackbarHostState.showSnackbar("请先在首页添加或选择项目") }
            return
        }
        busy = true
        status = "正在扫描 ${project.name} 的文本文件……"
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ProjectFileStore(context, Uri.parse(project.uri)).snapshot()
                }
            }.onSuccess {
                snapshot = it
                plan = null
                rawModelOutput = ""
                status = "${project.name} 已扫描 ${it.files.size} 个文本文件。"
            }.onFailure {
                status = "扫描失败：${it.message}"
            }
            busy = false
        }
    }

    fun applyPatch() {
        val project = activeProject
        val currentPlan = plan
        if (project == null || currentPlan == null) {
            scope.launch { snackbarHostState.showSnackbar("没有可应用的修改计划") }
            return
        }
        busy = true
        status = "正在写入 ${project.name}……"
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val store = ProjectFileStore(context, Uri.parse(project.uri))
                    val logs = store.apply(currentPlan)
                    val updatedSnapshot = store.snapshot()
                    logs to updatedSnapshot
                }
            }.onSuccess { (logs, updatedSnapshot) ->
                snapshot = updatedSnapshot
                status = "已应用：\n${logs.joinToString("\n")}"
                appendProjectMessage(
                    project.id,
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.ASSISTANT,
                        content = "已应用当前修改计划：\n${logs.joinToString("\n")}",
                        thinkingSteps = listOf("已打开项目授权目录", "已执行 ${logs.size} 个文件操作", "已重新扫描项目快照")
                    ),
                    messages,
                    repo,
                    onMessagesChanged = { messages = it }
                )
            }.onFailure {
                status = "应用失败：${it.message}"
            }
            busy = false
        }
    }

    fun clearCurrentChat() {
        activeProject?.let {
            messages = emptyList()
            repo.clearChatMessages(it.id)
            status = "对话已清空。"
        }
    }

    fun askAiForPatch() {
        val project = activeProject
        val currentSnapshot = snapshot
        val currentConfig = activeConfig
        val currentApiKey = apiKey
        val task = prompt.trim()
        val pending = selectedAttachments
        if (project == null) {
            scope.launch { snackbarHostState.showSnackbar("请先在首页选择项目") }
            return
        }
        if (currentConfig == null) {
            scope.launch { snackbarHostState.showSnackbar("请先在设置页添加一个模型") }
            selectedTab = AppTab.SETTINGS
            return
        }
        if (currentApiKey.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("请先在设置页填写 API Key") }
            selectedTab = AppTab.SETTINGS
            return
        }
        if (currentSnapshot == null) {
            scope.launch { snackbarHostState.showSnackbar("请先扫描项目") }
            selectedTab = AppTab.HOME
            return
        }
        if (task.isBlank() && pending.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("请输入要让 AI 做的事情，或添加文件/图片") }
            return
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            content = task.ifBlank { "（仅发送附件）" },
            attachments = pending.map { ChatAttachmentInfo(it.name, it.mimeType, it.byteSize, sentToModel = false) }
        )
        val startingMessages = (messages + userMessage).takeLast(200)
        messages = startingMessages
        repo.saveChatMessages(project.id, startingMessages)
        prompt = ""
        selectedAttachments = emptyList()

        busy = true
        plan = null
        rawModelOutput = ""
        status = if (autoApplyWithoutConfirmation) {
            "正在请求模型；返回修改计划后会自动应用到 ${project.name}……"
        } else {
            "正在请求模型为 ${project.name} 生成修改计划……"
        }
        scope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            runCatching {
                withContext(Dispatchers.IO) {
                    val attachments = loadPromptAttachments(context, pending)
                    val sentCount = attachments.count { attachmentWillBeSent(currentConfig.provider, it) }
                    val steps = mutableListOf(
                        "已读取项目快照：${currentSnapshot.files.size} 个文本文件",
                        "已载入附件：${attachments.size} 个，其中 ${sentCount} 个可发送给当前模型接口",
                        "已使用模型配置：${currentConfig.displayName} / ${currentConfig.model}",
                        "已发送请求到模型服务"
                    )
                    val response = LlmClientFactory.create(currentConfig.provider)
                        .requestPatch(currentConfig, currentApiKey, currentSnapshot, task, attachments)
                    steps += "已收到模型响应"
                    val parsed = PatchParser.parse(response.content)
                    steps += "已解析 ${parsed.operations.size} 个文件操作"
                    val applyLogs: List<String>?
                    val updatedSnapshot: ProjectSnapshot?
                    if (autoApplyWithoutConfirmation) {
                        val store = ProjectFileStore(context, Uri.parse(project.uri))
                        applyLogs = store.apply(parsed)
                        updatedSnapshot = store.snapshot()
                        steps += "已自动应用 ${applyLogs.size} 个文件操作"
                        steps += "已重新扫描项目快照"
                    } else {
                        applyLogs = null
                        updatedSnapshot = null
                        steps += "已等待用户确认应用"
                    }
                    PatchRequestResult(
                        response = response,
                        plan = parsed,
                        applyLogs = applyLogs,
                        updatedSnapshot = updatedSnapshot,
                        thinkingMillis = SystemClock.elapsedRealtime() - startedAt,
                        thinkingSteps = steps,
                        attachments = attachments
                    )
                }
            }.onSuccess { result ->
                rawModelOutput = result.response.content
                plan = result.plan
                if (result.updatedSnapshot != null) snapshot = result.updatedSnapshot
                val summary = buildString {
                    appendLine(result.plan.summary)
                    appendLine()
                    appendLine("文件操作：")
                    result.plan.operations.forEach { op -> appendLine("- ${op.op.uppercase()} ${op.path}") }
                    if (result.applyLogs != null) {
                        appendLine()
                        appendLine("已自动应用：")
                        result.applyLogs.forEach { appendLine("- $it") }
                    }
                }.trim()
                val assistant = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.ASSISTANT,
                    content = summary,
                    inputTokens = result.response.inputTokens,
                    outputTokens = result.response.outputTokens,
                    thinkingMillis = result.thinkingMillis,
                    thinkingSteps = result.thinkingSteps,
                    rawModelOutput = result.response.rawResponse,
                    attachments = result.attachments.map {
                        ChatAttachmentInfo(
                            name = it.name,
                            mimeType = it.mimeType,
                            byteSize = it.byteSize,
                            sentToModel = attachmentWillBeSent(currentConfig.provider, it)
                        )
                    }
                )
                val next = (startingMessages.dropLast(1) + userMessage.copy(
                    attachments = assistant.attachments
                ) + assistant).takeLast(200)
                messages = next
                repo.saveChatMessages(project.id, next)
                status = if (result.applyLogs != null) {
                    "模型生成了 ${result.plan.operations.size} 个文件操作，并已自动应用。"
                } else {
                    "模型生成了 ${result.plan.operations.size} 个文件操作。请检查后再应用。"
                }
            }.onFailure {
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                val assistant = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.ASSISTANT,
                    content = "生成失败：${it.message}",
                    thinkingMillis = elapsed,
                    thinkingSteps = listOf("已读取项目快照", "请求或解析过程中发生错误"),
                    rawModelOutput = it.stackTraceToString()
                )
                val next = (startingMessages + assistant).takeLast(200)
                messages = next
                repo.saveChatMessages(project.id, next)
                status = "生成失败：${it.message}"
                rawModelOutput = it.stackTraceToString()
            }
            busy = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == AppTab.CHAT && activeProject != null) "${activeProject.name}" else "AAI") },
                actions = {
                    if (selectedTab == AppTab.CHAT && activeProject != null) {
                        IconButton(onClick = { selectedTab = AppTab.SETTINGS }) {
                            Icon(Icons.Default.Settings, contentDescription = "聊天模型设置")
                        }
                        IconButton(onClick = ::clearCurrentChat) {
                            Icon(Icons.Default.Delete, contentDescription = "清空对话")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                if (selectedTab == AppTab.CHAT && activeProject != null) {
                    ChatComposer(
                        prompt = prompt,
                        onPromptChange = { prompt = it },
                        attachments = selectedAttachments,
                        busy = busy,
                        plan = plan,
                        autoApplyWithoutConfirmation = autoApplyWithoutConfirmation,
                        onPickFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                        onPickImages = { imagePickerLauncher.launch(arrayOf("image/*")) },
                        onRemoveAttachment = { attachment ->
                            selectedAttachments = selectedAttachments.filterNot { it.uri == attachment.uri }
                        },
                        onAsk = ::askAiForPatch,
                        onApplyPatch = ::applyPatch
                    )
                }
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (selectedTab) {
            AppTab.HOME -> ScrollPage(padding) {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                HomePage(
                    projects = projects,
                    activeProject = activeProject,
                    snapshot = snapshot,
                    status = status,
                    busy = busy,
                    onAddProject = { openTreeLauncher.launch(null) },
                    onSelectProject = ::selectProject,
                    onDeleteProject = ::deleteProject,
                    onRenameActiveProject = ::renameActiveProject,
                    onScanProject = ::scanProject,
                    onGoChat = { selectedTab = AppTab.CHAT }
                )
            }
            AppTab.CHAT -> ChatPage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                project = activeProject,
                modelConfig = activeConfig,
                messages = messages,
                status = status,
                plan = plan,
                rawModelOutput = rawModelOutput,
                busy = busy,
                onGoHome = { selectedTab = AppTab.HOME }
            )
            AppTab.SETTINGS -> ScrollPage(padding) {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                SettingsPage(
                    configs = modelConfigs,
                    activeConfig = activeConfig,
                    activeApiKey = apiKey,
                    autoApplyWithoutConfirmation = autoApplyWithoutConfirmation,
                    onSelectConfig = ::selectModelConfig,
                    onSaveConfig = ::saveModelConfig,
                    onDeleteConfig = ::deleteModelConfig,
                    onAutoApplyWithoutConfirmationChange = {
                        autoApplyWithoutConfirmation = it
                        repo.saveAutoApplyWithoutConfirmation(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun ScrollPage(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun HomePage(
    projects: List<ProjectProfile>,
    activeProject: ProjectProfile?,
    snapshot: ProjectSnapshot?,
    status: String,
    busy: Boolean,
    onAddProject: () -> Unit,
    onSelectProject: (ProjectProfile) -> Unit,
    onDeleteProject: (ProjectProfile) -> Unit,
    onRenameActiveProject: (String) -> Unit,
    onScanProject: () -> Unit,
    onGoChat: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("首页 / 项目", style = MaterialTheme.typography.titleLarge)
            Text("可以添加多个项目文件夹，并在这里选择当前要让 AI 编程的项目。")
            Button(onClick = onAddProject) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加项目文件夹")
            }
        }
    }

    if (projects.isEmpty()) {
        Card { Text("还没有项目。请先添加一个项目文件夹。", Modifier.padding(16.dp)) }
    } else {
        projects.forEach { project ->
            ProjectCard(
                project = project,
                selected = project.id == activeProject?.id,
                onSelect = { onSelectProject(project) },
                onDelete = { onDeleteProject(project) }
            )
        }
    }

    if (activeProject != null) {
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("当前项目", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = activeProject.name,
                    onValueChange = onRenameActiveProject,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("项目名称") },
                    singleLine = true
                )
                SelectionContainer {
                    Text(activeProject.uri, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onScanProject, enabled = !busy) { Text("扫描项目") }
                    ElevatedButton(onClick = onGoChat, enabled = snapshot != null) { Text("去聊天") }
                }
                SelectionContainer { Text(status) }
                if (snapshot != null) {
                    Text("文件预览", style = MaterialTheme.typography.titleSmall)
                    SelectionContainer {
                        Text(snapshot.treeText().lineSequence().take(60).joinToString("\n"), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectProfile,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    Text("最近使用：${formatDate(project.lastOpenedAtMillis)}", style = MaterialTheme.typography.bodySmall)
                }
                if (selected) AssistChip(onClick = {}, label = { Text("当前") })
            }
            SelectionContainer {
                Text(project.uri, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSelect) { Text(if (selected) "已选择" else "选择") }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("移除")
                }
            }
        }
    }
}

@Composable
private fun ChatPage(
    modifier: Modifier,
    project: ProjectProfile?,
    modelConfig: ModelConfig?,
    messages: List<ChatMessage>,
    status: String,
    plan: PatchPlan?,
    rawModelOutput: String,
    busy: Boolean,
    onGoHome: () -> Unit
) {
    if (project == null) {
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("聊天", style = MaterialTheme.typography.titleLarge)
            Text("请先在首页添加并选择项目。")
            Button(onClick = onGoHome) { Text("去首页选择项目") }
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (busy) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        if (modelConfig == null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text("还没有模型。请到设置页添加一个模型后再聊天。", Modifier.padding(16.dp))
                }
            }
        }
        if (messages.isEmpty()) {
            item {
                Card { Text("这个项目还没有对话。底部输入需求后，AI 的反馈会保存在这里。", Modifier.padding(16.dp)) }
            }
        } else {
            items(messages, key = { it.id }) { message -> ChatMessageCard(message) }
        }
        item { StatusCard(status = status, plan = plan, rawModelOutput = rawModelOutput) }
    }
}

@Composable
private fun ChatComposer(
    prompt: String,
    onPromptChange: (String) -> Unit,
    attachments: List<PendingAttachment>,
    busy: Boolean,
    plan: PatchPlan?,
    autoApplyWithoutConfirmation: Boolean,
    onPickFiles: () -> Unit,
    onPickImages: () -> Unit,
    onRemoveAttachment: (PendingAttachment) -> Unit,
    onAsk: () -> Unit,
    onApplyPatch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (attachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    attachments.forEach { attachment ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${attachment.name} · ${attachment.mimeType}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            IconButton(onClick = { onRemoveAttachment(attachment) }) {
                                Icon(Icons.Default.Delete, contentDescription = "移除附件")
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onPickFiles, enabled = !busy) {
                    Icon(Icons.Default.AttachFile, contentDescription = "添加文件")
                }
                IconButton(onClick = onPickImages, enabled = !busy) {
                    Icon(Icons.Default.Image, contentDescription = "添加图片")
                }
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.weight(1f),
                    minLines = 1,
                    maxLines = 5,
                    placeholder = { Text("输入需求") }
                )
                IconButton(onClick = onAsk, enabled = !busy) {
                    Icon(Icons.Default.Send, contentDescription = if (autoApplyWithoutConfirmation) "发送并自动应用" else "发送")
                }
            }
            if (plan != null && !autoApplyWithoutConfirmation) {
                Button(onClick = onApplyPatch, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("应用当前修改计划")
                }
            }
        }
    }
}

@Composable
private fun ChatMessageCard(message: ChatMessage) {
    val isAssistant = message.role == ChatRole.ASSISTANT
    Card(colors = if (isAssistant) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) else CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.role.label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(formatDate(message.createdAtMillis), style = MaterialTheme.typography.bodySmall)
            }
            if (message.attachments.isNotEmpty()) {
                Text(
                    message.attachments.joinToString("\n") {
                        val state = if (it.sentToModel) "已发送给模型" else "仅保留记录"
                        "附件：${it.name} · ${it.mimeType} · ${formatBytes(it.byteSize)} · $state"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            SelectionContainer { Text(message.content) }
            val metrics = buildList {
                message.inputTokens?.let { add("输入 Token：$it") }
                message.outputTokens?.let { add("输出 Token：$it") }
                message.thinkingMillis?.let { add("思考时长：${formatDuration(it)}") }
            }
            if (metrics.isNotEmpty()) {
                Text(metrics.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            if (message.thinkingSteps.isNotEmpty()) {
                Text("思考步骤", style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(message.thinkingSteps.joinToString("\n") { "- $it" }, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    configs: List<ModelConfig>,
    activeConfig: ModelConfig?,
    activeApiKey: String,
    autoApplyWithoutConfirmation: Boolean,
    onSelectConfig: (String) -> Unit,
    onSaveConfig: (ModelConfig, String) -> Unit,
    onDeleteConfig: (ModelConfig) -> Unit,
    onAutoApplyWithoutConfirmationChange: (Boolean) -> Unit
) {
    var configExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var editingApiKey by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("模型", style = MaterialTheme.typography.titleLarge)
            }

            if (configs.isEmpty() || activeConfig == null) {
                Text("还没有模型。请添加一个模型。")
            } else {
                Text("当前模型")
                AssistChip(onClick = { configExpanded = true }, label = { Text("${activeConfig.displayName} / ${activeConfig.model}") })
                DropdownMenu(expanded = configExpanded, onDismissRequest = { configExpanded = false }) {
                    configs.forEach { config ->
                        DropdownMenuItem(
                            text = { Text("${config.displayName} / ${config.model}") },
                            onClick = {
                                configExpanded = false
                                onSelectConfig(config.id)
                            }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        editingConfig = activeConfig
                        editingApiKey = activeApiKey
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("编辑当前模型")
                    }
                    IconButton(onClick = { onDeleteConfig(activeConfig) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除当前模型")
                    }
                }
            }

            Button(onClick = {
                editingConfig = ModelConfig(id = UUID.randomUUID().toString())
                editingApiKey = ""
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加模型")
            }
        }
    }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("行为", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("无需用户确认直接应用")
                    Text(
                        "开启后，AI 生成修改计划后会立即写入当前项目。建议只在已提交 Git、可回滚的项目中开启。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = autoApplyWithoutConfirmation,
                    onCheckedChange = onAutoApplyWithoutConfirmationChange
                )
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("关于", style = MaterialTheme.typography.titleMedium)
            Text("开发者：Prodee163")
            Text(
                "Github 仓库：https://github.com/Prodee1633/AAI",
                modifier = Modifier.clickable { uriHandler.openUri("https://github.com/Prodee1633/AAI") },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDialog && editingConfig != null) {
        ModelConfigDialog(
            initialConfig = editingConfig!!,
            initialApiKey = editingApiKey,
            onDismiss = { showDialog = false },
            onSave = { config, key ->
                showDialog = false
                onSaveConfig(config, key)
            }
        )
    }
}

@Composable
private fun ModelConfigDialog(
    initialConfig: ModelConfig,
    initialApiKey: String,
    onDismiss: () -> Unit,
    onSave: (ModelConfig, String) -> Unit
) {
    var displayName by remember(initialConfig.id) { mutableStateOf(initialConfig.displayName) }
    var provider by remember(initialConfig.id) { mutableStateOf(initialConfig.provider) }
    var model by remember(initialConfig.id) { mutableStateOf(initialConfig.model) }
    var baseUrl by remember(initialConfig.id) { mutableStateOf(initialConfig.baseUrl) }
    var apiKey by remember(initialConfig.id) { mutableStateOf(initialApiKey) }
    var providerExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模型设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("配置名称") },
                    singleLine = true
                )
                AssistChip(onClick = { providerExpanded = true }, label = { Text(provider.label) })
                DropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                    ProviderType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                providerExpanded = false
                                provider = type
                                when (type) {
                                    ProviderType.OPENAI_COMPATIBLE -> {
                                        if (displayName.isBlank()) displayName = "OpenAI compatible"
                                        if (model.isBlank()) model = "gpt-4.1"
                                        if (baseUrl.isBlank()) baseUrl = "https://api.openai.com/v1/chat/completions"
                                    }
                                    ProviderType.GEMINI -> {
                                        if (displayName.isBlank()) displayName = "Gemini"
                                        if (model.isBlank()) model = "gemini-2.5-pro"
                                        baseUrl = ""
                                    }
                                    ProviderType.ANTHROPIC -> {
                                        if (displayName.isBlank()) displayName = "Claude"
                                        if (model.isBlank()) model = "claude-sonnet-4-5"
                                        baseUrl = ""
                                    }
                                }
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模型 ID") },
                    singleLine = true
                )
                if (provider == ProviderType.OPENAI_COMPATIBLE) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Chat Completions Endpoint") },
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    initialConfig.copy(
                        displayName = displayName,
                        provider = provider,
                        model = model,
                        baseUrl = if (provider == ProviderType.OPENAI_COMPATIBLE) baseUrl else ""
                    ),
                    apiKey
                )
            }) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun StatusCard(
    status: String,
    plan: PatchPlan?,
    rawModelOutput: String
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("反馈", style = MaterialTheme.typography.titleMedium)
            SelectionContainer { Text(status) }

            if (plan != null) {
                HorizontalDivider()
                Text("当前修改计划", style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(
                        buildString {
                            appendLine(plan.summary)
                            appendLine()
                            plan.operations.forEach { op -> appendLine("${op.op.uppercase()}  ${op.path}") }
                        },
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (rawModelOutput.isNotBlank()) {
                HorizontalDivider()
                Text("模型原始输出", style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(rawModelOutput.take(6000), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private data class PatchRequestResult(
    val response: LlmResponse,
    val plan: PatchPlan,
    val applyLogs: List<String>? = null,
    val updatedSnapshot: ProjectSnapshot? = null,
    val thinkingMillis: Long,
    val thinkingSteps: List<String>,
    val attachments: List<PromptAttachment> = emptyList()
)

private fun appendProjectMessage(
    projectId: String,
    message: ChatMessage,
    currentMessages: List<ChatMessage>,
    repo: AppRepository,
    onMessagesChanged: (List<ChatMessage>) -> Unit
) {
    val next = (currentMessages + message).takeLast(200)
    onMessagesChanged(next)
    repo.saveChatMessages(projectId, next)
}

private fun resolvePromptAttachment(context: Context, pending: PendingAttachment): PromptAttachment? {
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(pending.uri)?.use { it.readBytes() } ?: return null
    val maxTextBytes = 1_000_000
    val maxBinaryBytes = 5_000_000
    return when {
        isTextLike(pending.name, pending.mimeType) -> {
            val clipped = if (bytes.size > maxTextBytes) bytes.copyOf(maxTextBytes) else bytes
            PromptAttachment(
                name = pending.name,
                mimeType = pending.mimeType,
                byteSize = pending.byteSize.takeIf { it > 0L } ?: bytes.size.toLong(),
                textContent = clipped.toString(Charsets.UTF_8) + if (bytes.size > maxTextBytes) "\n\n[已截断：文件超过 1MB]" else ""
            )
        }
        pending.mimeType.startsWith("image/") || pending.mimeType.equals("application/pdf", ignoreCase = true) -> {
            if (bytes.size > maxBinaryBytes) {
                PromptAttachment(pending.name, pending.mimeType, pending.byteSize.takeIf { it > 0L } ?: bytes.size.toLong())
            } else {
                PromptAttachment(
                    name = pending.name,
                    mimeType = pending.mimeType,
                    byteSize = pending.byteSize.takeIf { it > 0L } ?: bytes.size.toLong(),
                    base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)
                )
            }
        }
        else -> PromptAttachment(pending.name, pending.mimeType, pending.byteSize.takeIf { it > 0L } ?: bytes.size.toLong())
    }
}

private fun loadPromptAttachments(context: Context, pending: List<PendingAttachment>): List<PromptAttachment> = pending.mapNotNull {
    runCatching { resolvePromptAttachment(context, it) }.getOrNull()
}

private fun attachmentWillBeSent(provider: ProviderType, attachment: PromptAttachment): Boolean {
    if (attachment.textContent != null) return true
    if (attachment.base64Content == null) return false
    return when (provider) {
        ProviderType.OPENAI_COMPATIBLE -> attachment.isImage
        ProviderType.GEMINI -> attachment.isImage || attachment.isPdf
        ProviderType.ANTHROPIC -> attachment.mimeType in setOf("image/jpeg", "image/png", "image/gif", "image/webp")
    }
}

private fun isTextLike(name: String, mimeType: String): Boolean {
    val lowerName = name.lowercase(Locale.getDefault())
    val lowerMime = mimeType.lowercase(Locale.getDefault())
    if (lowerMime.startsWith("text/")) return true
    if (lowerMime in setOf("application/json", "application/xml", "application/javascript", "application/x-yaml")) return true
    return listOf(
        ".kt", ".kts", ".java", ".gradle", ".xml", ".json", ".md", ".txt", ".yml", ".yaml",
        ".toml", ".properties", ".html", ".css", ".js", ".ts", ".tsx", ".jsx", ".py", ".sh",
        ".c", ".cpp", ".h", ".hpp", ".rs", ".go", ".swift", ".php", ".rb", ".sql"
    ).any { lowerName.endsWith(it) }
}

private fun formatDate(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatDuration(millis: Long): String = when {
    millis < 1000 -> "${millis}ms"
    millis < 60_000 -> "${millis / 1000.0}s"
    else -> "${millis / 60_000}m ${millis % 60_000 / 1000}s"
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0L -> "未知大小"
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    else -> "${bytes / 1024 / 1024}MB"
}
