package com.example.aiprojectcoder.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aiprojectcoder.data.AppRepository
import com.example.aiprojectcoder.data.ModelConfig
import com.example.aiprojectcoder.data.ProviderType
import com.example.aiprojectcoder.files.PatchParser
import com.example.aiprojectcoder.files.PatchPlan
import com.example.aiprojectcoder.files.ProjectFileStore
import com.example.aiprojectcoder.files.ProjectSnapshot
import com.example.aiprojectcoder.llm.LlmClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    val context = LocalContext.current
    val repo = remember { AppRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var config by remember { mutableStateOf(repo.readConfig()) }
    var apiKey by remember { mutableStateOf(repo.readApiKey()) }
    var projectUri by remember { mutableStateOf(repo.readProjectUri()) }
    var prompt by remember { mutableStateOf("把这个项目升级为 Material 3 风格，并修复明显的编译问题。") }
    var snapshot by remember { mutableStateOf<ProjectSnapshot?>(null) }
    var plan by remember { mutableStateOf<PatchPlan?>(null) }
    var rawModelOutput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("请选择项目文件夹，然后扫描项目。") }
    var busy by remember { mutableStateOf(false) }

    val openTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            repo.saveProjectUri(uri)
            projectUri = uri
            status = "已授权项目目录：$uri"
        }
    }

    fun saveSettings() {
        repo.saveConfig(config)
        repo.saveApiKey(apiKey)
        scope.launch { snackbarHostState.showSnackbar("配置已保存") }
    }

    fun scanProject() {
        val uri = projectUri
        if (uri == null) {
            scope.launch { snackbarHostState.showSnackbar("请先选择项目文件夹") }
            return
        }
        busy = true
        status = "正在扫描文本文件……"
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ProjectFileStore(context, uri).snapshot()
                }
            }.onSuccess {
                snapshot = it
                plan = null
                rawModelOutput = ""
                status = "已扫描 ${it.files.size} 个文本文件。"
            }.onFailure {
                status = "扫描失败：${it.message}"
            }
            busy = false
        }
    }

    fun askAiForPatch() {
        val currentSnapshot = snapshot
        if (apiKey.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("请先填写 API Key") }
            return
        }
        if (currentSnapshot == null) {
            scope.launch { snackbarHostState.showSnackbar("请先扫描项目") }
            return
        }
        saveSettings()
        busy = true
        plan = null
        status = "正在请求模型生成修改计划……"
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val client = LlmClientFactory.create(config.provider)
                    val output = client.requestPatch(config, apiKey, currentSnapshot, prompt)
                    val parsed = PatchParser.parse(output)
                    output to parsed
                }
            }.onSuccess { (output, parsed) ->
                rawModelOutput = output
                plan = parsed
                status = "模型生成了 ${parsed.operations.size} 个文件操作。请检查后再应用。"
            }.onFailure {
                status = "生成失败：${it.message}"
                rawModelOutput = it.stackTraceToString()
            }
            busy = false
        }
    }

    fun applyPatch() {
        val uri = projectUri
        val currentPlan = plan
        if (uri == null || currentPlan == null) {
            scope.launch { snackbarHostState.showSnackbar("没有可应用的修改计划") }
            return
        }
        busy = true
        status = "正在写入文件……"
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ProjectFileStore(context, uri).apply(currentPlan)
                }
            }.onSuccess { logs ->
                status = "已应用：\n${logs.joinToString("\n")}" 
                scanProject()
            }.onFailure {
                status = "应用失败：${it.message}"
            }
            busy = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI Project Coder") }) },
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

            SettingsCard(
                config = config,
                onConfigChange = { config = it },
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                onSave = ::saveSettings
            )

            Card(colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("项目文件夹")
                    SelectionContainer { Text(projectUri?.toString() ?: "尚未选择", fontFamily = FontFamily.Monospace) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { openTreeLauncher.launch(null) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("选择文件夹")
                        }
                        ElevatedButton(onClick = ::scanProject, enabled = !busy) {
                            Text("扫描项目")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("你想让 AI 做什么") },
                placeholder = { Text("例如：新增登录页、修复编译错误、重构网络层、生成单元测试……") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = ::askAiForPatch, enabled = !busy) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("生成修改计划")
                }
                ElevatedButton(onClick = ::applyPatch, enabled = !busy && plan != null) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("应用到项目")
                }
            }

            StatusCard(status = status, snapshot = snapshot, plan = plan, rawModelOutput = rawModelOutput)
        }
    }
}

@Composable
private fun SettingsCard(
    config: ModelConfig,
    onConfigChange: (ModelConfig) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("模型配置")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSave) {
                    Icon(Icons.Default.Save, contentDescription = "保存")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { expanded = true }, label = { Text(config.provider.label) })
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ProviderType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                expanded = false
                                val default = when (type) {
                                    ProviderType.OPENAI_COMPATIBLE -> ModelConfig(type, "OpenAI compatible", "gpt-4.1", "https://api.openai.com/v1/chat/completions")
                                    ProviderType.GEMINI -> ModelConfig(type, "Gemini", "gemini-2.5-pro", "")
                                    ProviderType.ANTHROPIC -> ModelConfig(type, "Claude", "claude-sonnet-4-5", "")
                                }
                                onConfigChange(default)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = config.model,
                onValueChange = { onConfigChange(config.copy(model = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模型 ID") },
                singleLine = true
            )

            if (config.provider == ProviderType.OPENAI_COMPATIBLE) {
                OutlinedTextField(
                    value = config.baseUrl,
                    onValueChange = { onConfigChange(config.copy(baseUrl = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Chat Completions Endpoint") },
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            TextButton(onClick = onSave) { Text("保存配置与密钥") }
        }
    }
}

@Composable
private fun StatusCard(
    status: String,
    snapshot: ProjectSnapshot?,
    plan: PatchPlan?,
    rawModelOutput: String
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("状态")
            SelectionContainer { Text(status) }

            if (snapshot != null) {
                Text("扫描文件预览")
                SelectionContainer {
                    Text(snapshot.treeText().lineSequence().take(40).joinToString("\n"), fontFamily = FontFamily.Monospace)
                }
            }

            if (plan != null) {
                Text("待应用修改")
                SelectionContainer {
                    Text(
                        buildString {
                            appendLine(plan.summary)
                            appendLine()
                            plan.operations.forEach { op ->
                                appendLine("${op.op.uppercase()}  ${op.path}")
                            }
                        },
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (rawModelOutput.isNotBlank()) {
                Text("模型原始输出")
                SelectionContainer {
                    Text(rawModelOutput.take(6000), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
