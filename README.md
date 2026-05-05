# AAI

AAI 是一个 Android / Jetpack Compose / Material 3 风格的 AI 编程助手 MVP。

开发者：Prodee163  
GitHub 仓库：Prodee1633/AAI

## 已实现

- Material 3 / dynamic color UI
- 底部三分页：
  - 首页：添加、选择、重命名、移除多个项目文件夹
  - 聊天：按项目保存所有对话，显示 AI 反馈、输入/输出 Token、思考时长、工作步骤
  - 设置：保存多个模型配置，每个模型配置独立保存 API Key
- 支持三类模型接口：
  - OpenAI-compatible：OpenAI、OpenRouter、Mistral 等兼容 Chat Completions 的接口
  - Google Gemini generateContent
  - Anthropic Claude Messages API
- 使用 Android Storage Access Framework 让用户选择项目根目录
- 扫描项目中的文本源码文件
- 把项目上下文和任务发给模型
- 模型返回 JSON PatchPlan
- 默认流程：用户检查修改计划后，点击“应用修改”执行 write / append / delete / mkdir
- 可选流程：打开“无需用户确认直接应用”开关后，模型生成修改计划后立即写入当前项目目录
- GitHub Actions：每次 push 自动构建 debug APK 并上传 artifact

## 使用

1. 用 Android Studio 打开本目录。
2. 确认本机安装了 Android SDK 36 / Build Tools 36。
3. Sync Gradle。
4. 运行到真机或模拟器。
5. 在首页添加一个或多个项目文件夹。
6. 选择当前项目并点击“扫描项目”。
7. 在设置页添加或选择模型配置，并填写对应 API Key。
8. 到聊天页输入需求并发送。
9. 检查 AI 反馈和修改计划后点击“应用修改”。

如果你想让模型生成后直接写入文件，在设置页打开“无需用户确认直接应用”。这个开关默认关闭；开启后请只对已经提交到 Git、能随时回滚的项目使用。

## OpenAI-compatible Endpoint 示例

- OpenAI: `https://api.openai.com/v1/chat/completions`
- OpenRouter: `https://openrouter.ai/api/v1/chat/completions`
- Mistral: `https://api.mistral.ai/v1/chat/completions`

## 安全策略

AAI 默认不会让模型直接静默改文件。流程是：

1. 用户选择目录授权。
2. App 扫描文本文件。
3. 模型生成 JSON 修改计划。
4. 用户确认后才写入文件。

如果打开“无需用户确认直接应用”，第 4 步会被跳过，模型输出会直接写入当前项目的已授权目录。

聊天页中的“思考步骤”显示的是 App 可验证的工作流程步骤，例如读取快照、发送请求、解析计划、应用文件操作等；不会要求模型泄露隐藏推理链。

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
