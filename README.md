# AI Project Coder

一个 Android / Jetpack Compose / Material 3 风格的 AI 编程助手 MVP。

## 已实现

- Material 3 / dynamic color UI
- 手动输入并保存 API Key
- 支持三类模型接口：
  - OpenAI-compatible：OpenAI、OpenRouter、Mistral 等兼容 Chat Completions 的接口
  - Google Gemini generateContent
  - Anthropic Claude Messages API
- 使用 Android Storage Access Framework 让用户选择项目根目录
- 扫描项目中的文本源码文件
- 把项目上下文和任务发给模型
- 模型返回 JSON PatchPlan
- 用户检查后，点击“应用到项目”执行 write / append / delete / mkdir

## 使用

1. 用 Android Studio 打开本目录。
2. 确认本机安装了 Android SDK 36。
3. Sync Gradle。
4. 运行到真机或模拟器。
5. 选择项目文件夹。
6. 配置模型、API Key。
7. 点击“扫描项目”。
8. 输入任务，点击“生成修改计划”。
9. 检查计划后点击“应用到项目”。

## OpenAI-compatible Endpoint 示例

- OpenAI: `https://api.openai.com/v1/chat/completions`
- OpenRouter: `https://openrouter.ai/api/v1/chat/completions`
- Mistral: `https://api.mistral.ai/v1/chat/completions`

## 安全策略

这个 MVP 默认不会让模型直接静默改文件。流程是：

1. 用户选择目录授权。
2. App 扫描文本文件。
3. 模型生成 JSON 修改计划。
4. 用户确认后才写入文件。

建议继续增强：

- 在应用前展示 diff。
- 添加 Git 快照或本地备份。
- 给每次操作增加撤销功能。
- 限制单次最大写入文件数和最大字节数。
- 为 API Key 增加生物识别解锁。

## 模型输出格式

模型必须返回如下 JSON：

```json
{
  "summary": "这次要做什么",
  "operations": [
    {"op": "write", "path": "app/src/main/java/.../File.kt", "content": "完整文件内容"},
    {"op": "mkdir", "path": "app/src/test/java"},
    {"op": "delete", "path": "old/file.txt"}
  ]
}
```

