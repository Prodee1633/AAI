# AAI

AAI 是一个 Android / Kotlin / Jetpack Compose / Material 3 的本机 AI 编程助手。

## 主要功能

- 首页：管理多个项目文件夹，选择当前项目并扫描项目文本文件。
- 聊天：针对当前项目向 AI 提要求，显示项目独立对话、输入/输出 Token、耗时和可验证的执行步骤。
- 设置：保存多个模型配置，单独保存每个模型的 API Key，切换当前模型。
- 行为开关：可单独开启“无需用户确认直接应用”。关闭时先生成修改计划，手动确认后再写入项目。
- 附件输入：聊天输入栏支持添加文件和图片。文本文件会作为上下文发送；图片/PDF 会按当前模型接口能力以多模态输入发送；不支持的二进制文件仅保留记录。
- 关于：开发者 Prodee163，仓库链接 https://github.com/Prodee1633/AAI。

## 支持的模型接口

- OpenAI-compatible Chat Completions，例如 OpenAI、OpenRouter、Mistral 兼容接口等。
- Google Gemini `generateContent`。
- Anthropic Claude Messages API。

## 构建

GitHub Actions 已包含 `.github/workflows/android-build.yml`，每次 push 会构建 debug APK 并上传 artifact。

本地构建可在 Android Studio 中打开项目后运行：

```bash
./gradlew :app:assembleDebug
```

如果仓库还没有 Gradle Wrapper，GitHub Actions 会安装指定 Gradle 版本进行构建。

## 2026-05-05 修复

- 修复聊天页键盘弹出时底部三栏导航被顶起的问题：键盘显示时隐藏底部导航，只保留输入栏贴在键盘上方。
- 模型执行改为单步模式：提示词要求每次最多返回 1 个原子文件操作；如果模型仍返回多个操作，App 只保留并执行第 1 个，防止一次性执行一大串修改。
- Ktor HTTP 客户端增加超时配置：请求超时、Socket 超时均为 10 分钟，连接超时为 30 秒，以减少 DeepSeek 等 OpenAI-compatible 接口在长响应时触发 Socket timeout。
