# Opal v2 FCL / Android 启动修复说明

本补丁针对 FCL / Android arm64 上的 `liblwjgl_nanovg.so: library "libm.so.6" not found` 崩溃。

## 修改内容

1. `build.gradle` 新增 `-Popal.android` 构建开关：
   - Android 构建时不再把桌面 Linux/macOS/Windows 的 `lwjgl-nanovg` native jar 打进 mod。
   - 桌面构建不加这个开关时仍保留原来的 native 依赖。

2. `NVGRenderer` 新增 Android/FCL 检测：
   - 在 FCL/Pojav/Android 环境中不初始化 NanoVG native context。
   - 所有 NanoVG 绘制函数变成安全 no-op，避免启动阶段崩溃。

3. `NVGTextRenderer`、`NVGImageRenderer`、HUD/Overlay/ESP/TargetInfo 相关位置加入保护：
   - NanoVG 不可用时跳过 Opal 的 NanoVG HUD/UI 绘制。
   - Minecraft 本体可以继续启动。

## 构建命令

Linux/macOS:

```bash
chmod +x gradlew
./gradlew clean build -Popal.android
```

Windows:

```bat
gradlew.bat clean build -Popal.android
```

构建完成后，把生成的 remap jar 放进 FCL 对应实例的 `mods` 目录，并保留 Fabric API：

```text
build/libs/*.jar
```

## 注意

这个修复的目标是“能在 Android/FCL 启动”。由于 Android 没有桌面 LWJGL NanoVG native，本补丁会在 Android 上禁用 Opal 的 NanoVG UI/部分视觉覆盖层。桌面端不加 `-Popal.android` 时仍按原逻辑构建。

请只在单人、测试环境或明确允许此类客户端/模组的服务器中使用。
