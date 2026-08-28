# Toast 屏蔽器

基于 libxposed API 102 的 LSPosed 模块，可按目标应用包名和关键词屏蔽 Toast。

## 使用

1. 安装 APK，在 LSPosed 中启用模块。
2. 打开模块，填写目标应用包名和关键词（每行一个），保存并批准作用域。
3. 重启目标应用；若启用了 `android` 或 `com.android.systemui` 作用域，需重启对应进程或设备。

匹配规则为区分大小写的文本包含匹配。空规则不会屏蔽任何 Toast。hook 读取或匹配失败时默认放行。

## 覆盖范围

- 目标应用进程：hook `android.widget.Toast.show()`，覆盖普通 Toast 与可提取文本的自定义 Toast。
- SystemUI：hook `ToastUI.showToast()`，覆盖 Android 11 及以上由系统界面展示的标准文本 Toast。
- system_server：hook `NotificationManagerService.enqueueTextToast()`，作为系统框架层兜底。

厂商 ROM 可能改名或改写 SystemUI / system_server 方法；目标应用进程 hook 仍可独立工作。

## 构建与测试

```bash
./gradlew testDebugUnitTest assembleDebug
```

GitHub Actions 会执行相同命令并上传 `app-debug.apk`。

## 隐私

模块不联网、不收集或上传数据。规则仅保存在 LSPosed Remote Preferences 中。

## 许可证

MIT
