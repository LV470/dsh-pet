# dsh-pet · DSH 鲸鱼娘桌宠

把 DSH 的鲸鱼娘从聊天窗口里放出来，**常驻在你的手机桌面上，盖在所有 App 之上**。

## 功能

| 能力 | 状态 | 说明 |
|---|---|---|
| 桌面常驻悬浮窗 | ✅ | `TYPE_APPLICATION_OVERLAY`，前台服务保活 |
| Live2D 形象 | ✅ | 复用 `c_0120` 模型（带 10 组动作 + 44 个表情） |
| 拖动 / 甩到任意位置 | ✅ | 用 `rawX/rawY` 拦截，位置自动记忆 |
| 单击 → 查余额 | ✅ | 调 `https://api.deepseek.com/user/balance` |
| 双击 → 打开 DSH 对话 | ✅ | 快捷入口 |
| 三连击 → 打开设置 | ✅ | 填 API Key |
| 定时刷新余额 | ✅ | 每 5 分钟一次 |
| 内嵌对话（在桌宠里直接聊） | 🚧 v2 | DSH 有信任栅栏，需要逆向会话协议 |

## 一键出包（不用装 Android Studio）

把本仓库推到 GitHub，**Actions 会自动编译**：

```
Actions → Build APK → 跑完在 Artifacts 里下载 dsh-pet-debug.apk
```

工作流定义在 `.github/workflows/build.yml`，用的 `gradle assembleDebug`。

## 装完怎么用

1. 打开 App → **填 DeepSeek API Key**（形如 `sk-...`）→ 保存
2. 点 **申请通知权限**（前台服务保活用）
3. 点 **启动桌宠** → 首次会跳系统设置，允许「**显示在其他应用上层**」
4. 鲸鱼娘就出现在屏幕上了：
   - **单击** → 显示余额
   - **双击** → 打开 DSH 跟你说话
   - **三连击** → 打开设置页
   - **按住拖** → 随便挪，松手自动记住位置

## 目录结构

```
app/src/main/
├── java/com/dsh/pet/
│   ├── MainActivity.kt   权限引导 + API Key 配置
│   ├── PetService.kt     悬浮窗服务 + JS 桥（余额/拖动/点击）
│   └── Prefs.kt          SharedPreferences 封装
├── assets/
│   ├── pet.html          桌宠页面（Live2D 渲染 + 气泡）
│   ├── model/            c_0120 Live2D 模型（4.2 MB）
│   └── vendor/           pixi.js + live2dcubismcore + cubism4
└── res/values/           文案与主题
```

## 技术要点（踩过的坑）

- **透明 WebView**：`setBackgroundColor(Color.TRANSPARENT)` 必须在 `loadUrl` **之前**调用，否则白底。
- **拖动不抖**：用 `MotionEvent.rawX/rawY`（屏幕绝对坐标），别用相对坐标 —— 窗口一移动，相对坐标的参考系也跟着动。
- **点击判定**：拖动由服务端 `setOnTouchListener` 拦截（返回 `true` 吃掉事件），点击通过 `evaluateJavascript("onPetClick()")` 回吐给页面。
- **前台服务类型**：Android 14 要求声明 `foregroundServiceType="specialUse"` 并配 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`。
- **华为保活**：需要在系统设置里加电池白名单 + 允许自启动。

## 素材授权

Live2D 模型与 `pixi-live2d-display` 运行时来自
[Andersen216/dsh-whale-girl-live2d](https://github.com/Andersen216/dsh-whale-girl-live2d)，
遵循 **CC BY-NC-SA 4.0**（署名 · 非商业 · 相同方式共享）。

**请勿商用。**
