# 文奇俄罗斯方块

一款以恒定下落速度为特色的俄罗斯方块游戏，包含原生 Android 版本和可添加到手机主屏幕的网页版。游戏免费，无广告。

- 在线游玩：[game.rongwenqi.me/tetris/](https://game.rongwenqi.me/tetris/)
- 安卓：支持 Android 8.0（API 26）及以上，可自行编译安装包。
- 交流和下载：QQ群 1095699226。

## 玩法

棋盘为 10 列 × 20 行，使用七种方块、七袋随机和 SRS 旋转。消除 1、2、3、4 行分别得 100、300、500、800 分。游戏默认每 0.7 秒下降一格；可在设置中选择 0.1～2.0 秒/格，游戏不会随着分数自动加速。

支持落点预览、左移、右移、旋转、软降和硬降。按住移动或软降可连发；暂停后继续有 3 秒倒计时。安卓版本提供遵循系统设置的触觉反馈开关；网页版不提供震动反馈。

主页的记录按钮可查看最高分、历史消行总数和局数。游戏状态、设置和统计仅保存在当前设备的应用数据或浏览器中，安卓与网页版之间不会同步。卸载应用或清除浏览器站点数据可能导致记录丢失。

安卓本局分数采用 64 位有符号整数，而不是旧说明中的 32 位整数；它仍有数据类型上限（约 922 京），极端情况下可能溢出。网页使用 JavaScript 安全整数，最大为 9,007,199,254,740,991，计分达到该值后不再增长。

## 安装与使用

### 网页版

访问[在线游戏](https://game.rongwenqi.me/tetris/)。iPhone 可使用 Safari 的“分享 → 添加到主屏幕”；支持的 Android 浏览器也可安装为网页应用。首次联网加载并完成离线缓存后可尝试离线使用；浏览器清理缓存后可能需要重新联网。

### 安卓版

从信任的来源获取 APK，或按下述步骤自行构建。覆盖安装要求应用 ID 和签名与原安装包一致；自行构建的测试签名通常不同，无法直接覆盖原版本。卸载旧版会删除其本地数据。

## 源码和开发

```text
android/  原生 Android 项目、构建脚本及游戏规则测试
web/      网页源文件、预览与测试脚本
```

### 运行和测试网页

需要 Node.js，无第三方运行时依赖：

```sh
cd web
node preview.mjs
# 浏览器打开 http://127.0.0.1:4173
```

在 `web` 目录执行以下命令，检查游戏逻辑及 `/tetris/` 子路径和离线缓存：

```sh
node test.mjs
node prepare-subpath.mjs
node test-sw.mjs
```

`prepare-subpath.mjs` 生成的 `dist/tetris/` 用于部署在 `/tetris/` 子路径，属于生成文件。正式离线功能需要 HTTPS（本机 localhost 除外）；直接打开 `file://` 无法注册 Service Worker。修改缓存资源时应同步更新 `dist/sw.js` 的缓存版本和对应测试。

### 构建安卓

需要 JDK 17、Android SDK Platform 35 和 Build Tools 35.0.0。Windows PowerShell 中：

```powershell
cd android
.\build.ps1 -JavaHome 'C:\path\to\jdk17' -SdkRoot 'C:\path\to\Android\Sdk'
```

脚本先运行游戏逻辑测试，再编译并签名 APK，默认输出到 `android/build/文奇俄罗斯方块.apk`。脚本自动在构建目录创建本地测试密钥；发布或覆盖已有安装时需要妥善保存并使用原签名密钥。本仓库不包含原应用签名密钥。也可用 Android Studio 打开 `android/`；项目未附带 Gradle Wrapper。

安卓使用本地 VectorDrawable 作为图标。`Game.java` 实现规则、计时和统计；`MainActivity.java` 负责界面、触控和本地存储。

## 项目说明

开发目的之一是探索用俄罗斯方块帮助应对侵入性记忆及 PTSD，因此采用恒定速度。本项目不是医疗设备，不能替代专业诊断或治疗。

项目按 [Apache License 2.0](LICENSE) 提供。
