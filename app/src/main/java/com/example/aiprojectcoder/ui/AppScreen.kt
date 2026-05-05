package com.example.aiprojectcoder.ui

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.aiprojectcoder.data.AppRepository
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
                ?: initialConfigs.first().id
        )
    }
    var apiKey by remember { mutableStateOf(repo.readApiKey(activeConfigId)) }
    var autoApplyWithoutConfirmation by remember { mutableStateOf(repo.readAutoApplyWithoutConfirmation()) }

    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var prompt by remember { mutableStateOf("请帮我修复这个项目的编译问题，并说明做了哪些修改。") }
    var snapshot by remember { mutableStateOf<ProjectSnapshot?>(null) }
    var plan by remember { mutableStateOf<PatchPlan?>(null) }
    var rawModelOutput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("在首页添加或选择项目，然后扫描项目。") }
    var busy by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(activeProjectId?.let(repo::readChatMessages).orEmpty()) }

    val activeProject = projects.firstOrNull { it.id == activeProjectId }
    val activeConfig = modelConfigs.firstOrNull { it.id == activeConfigId } ?: modelConfigs.first()

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

    fun saveModelSettings(showSnackbar: Boolean = true) {
        repo.saveModelConfigs(modelConfigs)
        repo.saveActiveModelConfigId(activeConfigId)
        repo.saveApiKey(activeConfigId, apiKey)
        repo.saveAutoApplyWithoutConfirmation(autoApplyWithoutConfirmation)
        if (showSnackbar) {
            scope.launch { snackbarHostState.showSnackbar("设置已保存") }
        }
    }

    fun updateActiveConfig(updated: ModelConfig) {
        modelConfigs = modelConfigs.map { if (it.id == activeConfigId) updated else it }
    }

    fun addModelConfig(provider: ProviderType = ProviderType.OPENAI_COMPATIBLE) {
        saveModelSettings(showSnackbar = false)
        val config = repo.newModelConfig(provider)
        modelConfigs = modelConfigs + config
        activeConfigId = config.id
        apiKey = ""
        repo.saveActiveModelConfigId(config.id)
    }

    fun selectModelConfig(configId: String) {
        saveModelSettings(showSnackbar = false)
        activeConfigId = configId
        apiKey = repo.readApiKey(configId)
        repo.saveActiveModelConfigId(configId)
    }

    fun deleteActiveModelConfig() {
        if (modelConfigs.size <= 1) {
            scope.launch { snackbarHostState.showSnackbar("至少保留一个模型配置") }
            return
        }
        val removeId = activeConfigId
        val next = modelConfigs.filterNot { it.id == removeId }
        val nextActive = next.first().id
        repo.clearApiKey(removeId)
        modelConfigs = next
        activeConfigId = nextActive
        apiKey = repo.readApiKey(nextActive)
        repo.saveModelConfigs(next)
        repo.saveActiveModelConfigId(nextActive)
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

    fun appendProjectMessage(projectId: String, message: ChatMessage) {
        val next = (messages + message).takeLast(200)
        messages = next
        repo.saveChatMessages(projectId, next)
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
                    )
                )
            }.onFailure {
                status = "应用失败：${it.message}"
            }
            busy = false
        }
    }

    fun askAiForPatch() {
        val project = activeProject
        val currentSnapshot = snapshot
        val currentConfig = activeConfig
        val currentApiKey = apiKey
        val task = prompt.trim()
        if (project == null) {
            scope.launch { snackbarHostState.showSnackbar("请先在首页选择项目") }
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
        if (task.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("请输入要让 AI 做的事情") }
            return
        }

        saveModelSettings(showSnackbar = false)
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            content = task
        )
        val startingMessages = (messages + userMessage).takeLast(200)
        messages = startingMessages
        repo.saveChatMessages(project.id, startingMessages)

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
                    val steps = mutableListOf(
                        "已读取项目快照：${currentSnapshot.files.size} 个文本文件",
                        "已使用模型配置：${currentConfig.displayName} / ${currentConfig.model}",
                        "已发送请求到模型服务"
                    )
                    val response = LlmClientFactory.create(currentConfig.provider)
                        .requestPatch(currentConfig, currentApiKey, currentSnapshot, task)
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
                        thinkingSteps = steps
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
                    rawModelOutput = result.response.rawResponse
                )
                val next = (startingMessages + assistant).takeLast(200)
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
        topBar = { TopAppBar(title = { Text("AAI") }) },
        bottomBar = {
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (selectedTab) {
                AppTab.HOME -> HomePage(
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
                AppTab.CHAT -> ChatPage(
                    project = activeProject,
                    modelConfig = activeConfig,
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    messages = messages,
                    status = status,
                    plan = plan,
                    rawModelOutput = rawModelOutput,
                    busy = busy,
                    autoApplyWithoutConfirmation = autoApplyWithoutConfirmation,
                    onAsk = ::askAiForPatch,
                    onApplyPatch = ::applyPatch,
                    onClearChat = {
                        activeProject?.let {
                            messages = emptyList()
                            repo.clearChatMessages(it.id)
                        }
                    },
                    onGoHome = { selectedTab = AppTab.HOME },
                    onGoSettings = { selectedTab = AppTab.SETTINGS }
                )
                AppTab.SETTINGS -> SettingsPage(
                    configs = modelConfigs,
                    activeConfig = activeConfig,
                    apiKey = apiKey,
                    autoApplyWithoutConfirmation = autoApplyWithoutConfirmation,
                    onSelectConfig = ::selectModelConfig,
                    onAddConfig = ::addModelConfig,
                    onDeleteActiveConfig = ::deleteActiveModelConfig,
                    onConfigChange = ::updateActiveConfig,
                    onApiKeyChange = { apiKey = it },
                    onAutoApplyWithoutConfirmationChange = {
                        autoApplyWithoutConfirmation = it
                        repo.saveAutoApplyWithoutConfirmation(it)
                    },
                    onSave = { saveModelSettings() }
                )
            }
        }
    }
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
    project: ProjectProfile?,
    modelConfig: ModelConfig,
    prompt: String,
    onPromptChange: (String) -> Unit,
    messages: List<ChatMessage>,
    status: String,
    plan: PatchPlan?,
    rawModelOutput: String,
    busy: Boolean,
    autoApplyWithoutConfirmation: Boolean,
    onAsk: () -> Unit,
    onApplyPatch: () -> Unit,
    onClearChat: () -> Unit,
    onGoHome: () -> Unit,
    onGoSettings: () -> Unit
) {
    if (project == null) {
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("聊天", style = MaterialTheme.typography.titleLarge)
                Text("请先在首页添加并选择项目。")
                Button(onClick = onGoHome) { Text("去首页选择项目") }
            }
        }
        return
    }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("聊天 / ${project.name}", style = MaterialTheme.typography.titleLarge)
            Text("当前模型：${modelConfig.displayName} / ${modelConfig.model}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onGoSettings, label = { Text("模型设置") })
                AssistChip(onClick = onGoHome, label = { Text("切换项目") })
                AssistChip(onClick = onClearChat, label = { Text("清空对话") })
            }
        }
    }

    if (messages.isEmpty()) {
        Card { Text("这个项目还没有对话。输入需求后，AI 的反馈会保存在这里。", Modifier.padding(16.dp)) }
    } else {
        messages.forEach { message -> ChatMessageCard(message) }
    }

    OutlinedTextField(
        value = prompt,
        onValueChange = onPromptChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        label = { Text("对 AI 提要求") },
        placeholder = { Text("例如：添加分页、修复构建、重构网络层、生成测试……") }
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onAsk, enabled = !busy) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (autoApplyWithoutConfirmation) "发送并自动应用" else "发送")
        }
        ElevatedButton(onClick = onApplyPatch, enabled = !busy && plan != null && !autoApplyWithoutConfirmation) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("应用修改")
        }
    }

    StatusCard(status = status, plan = plan, rawModelOutput = rawModelOutput)
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
    activeConfig: ModelConfig,
    apiKey: String,
    autoApplyWithoutConfirmation: Boolean,
    onSelectConfig: (String) -> Unit,
    onAddConfig: (ProviderType) -> Unit,
    onDeleteActiveConfig: () -> Unit,
    onConfigChange: (ModelConfig) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onAutoApplyWithoutConfirmationChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    var configExpanded by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("设置 / 模型配置", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSave) { Icon(Icons.Default.Save, contentDescription = "保存") }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { configExpanded = true }, label = { Text(activeConfig.displayName) })
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
                OutlinedButton(onClick = { onAddConfig(ProviderType.OPENAI_COMPATIBLE) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增")
                }
                IconButton(onClick = onDeleteActiveConfig) {
                    Icon(Icons.Default.Delete, contentDescription = "删除当前模型配置")
                }
            }

            OutlinedTextField(
                value = activeConfig.displayName,
                onValueChange = { onConfigChange(activeConfig.copy(displayName = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("配置名称") },
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { providerExpanded = true }, label = { Text(activeConfig.provider.label) })
                DropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                    ProviderType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                providerExpanded = false
                                val default = when (type) {
                                    ProviderType.OPENAI_COMPATIBLE -> activeConfig.copy(
                                        provider = type,
                                        displayName = if (activeConfig.displayName.isBlank()) "OpenAI compatible" else activeConfig.displayName,
                                        model = if (activeConfig.model.isBlank()) "gpt-4.1" else activeConfig.model,
                                        baseUrl = if (activeConfig.baseUrl.isBlank()) "https://api.openai.com/v1/chat/completions" else activeConfig.baseUrl
                                    )
                                    ProviderType.GEMINI -> activeConfig.copy(
                                        provider = type,
                                        displayName = if (activeConfig.displayName.isBlank()) "Gemini" else activeConfig.displayName,
                                        model = if (activeConfig.model.isBlank()) "gemini-2.5-pro" else activeConfig.model,
                                        baseUrl = ""
                                    )
                                    ProviderType.ANTHROPIC -> activeConfig.copy(
                                        provider = type,
                                        displayName = if (activeConfig.displayName.isBlank()) "Claude" else activeConfig.displayName,
                                        model = if (activeConfig.model.isBlank()) "claude-sonnet-4-5" else activeConfig.model,
                                        baseUrl = ""
                                    )
                                }
                                onConfigChange(default)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = activeConfig.model,
                onValueChange = { onConfigChange(activeConfig.copy(model = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模型 ID") },
                singleLine = true
            )

            if (activeConfig.provider == ProviderType.OPENAI_COMPATIBLE) {
                OutlinedTextField(
                    value = activeConfig.baseUrl,
                    onValueChange = { onConfigChange(activeConfig.copy(baseUrl = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Chat Completions Endpoint") },
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key（按模型配置单独保存）") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

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

            TextButton(onClick = onSave) { Text("保存当前设置") }
        }
    }

    Spacer(Modifier.height(24.dp))

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("关于", style = MaterialTheme.typography.titleMedium)
            Text("开发者：Prodee163")
            Text("Github 仓库：Prodee1633/AAI")
        }
    }
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
    val thinkingSteps: List<String>
)

private fun formatDate(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatDuration(millis: Long): String = when {
    millis < 1000 -> "${millis}ms"
    millis < 60_000 -> "${millis / 1000.0}s"
    else -> "${millis / 60_000}m ${millis % 60_000 / 1000}s"
}
