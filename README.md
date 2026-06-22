# BBZQ
> 使用 `libxposed API 102`、由 Kotlin 全量编写的哔哩哔哩增强 Xposed 模组

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![API](https://img.shields.io/badge/libxposed-API%20102-orange)
![License](https://img.shields.io/badge/license-Mulan%20PubL%20v2-blue)

---

## 简介
BBZQ 是一款适配 libxposed API 102 的哔哩哔哩功能增强模组，采用 Kotlin 完整实现实作，旨在去除不必要内容、优化核心体验，同时提供各类实功能。
模组通过 `META-INF/xposed/java_init.list` 完成入口注册，核心逻辑入口为 `io.github.bbzq.BbzqModule`；配套设置页面内嵌于哔哩哔哩宿主应用内，方便快捷开关各项功能、调整个性化参数，整体架构便于后续迭代维护与功能扩展。


---

## 支持目标

| 包名 | 说明 |
|------|------|
| `tv.danmaku.bili` | 哔哩哔哩 |
| `com.bilibili.app.in` | BiliBili 旧版 |
| `tv.danmaku.bilibilihd` | HD 版 |
| `com.bilibili.app.blue` | 概念版 |

---

## 功能
反正就是非常多, 具体请前往 [HSSkyBoy/BBZQ](https://github.com/HSSkyBoy/BBZQ/) 查看

---

## 使用方式

1. 安装模组 APK
2. 在支持 `libxposed API 102` 的框架（如 LSPosed）中启用模组
3. 将哔哩哔哩加入作用域
4. 重启目标应用
5. 进入 `我的 → 设置 → 高级设置`
6. 启用需要的功能

> 桌面图标是模组自身介绍页，不是独立的调试工具。  
> 双击 版本 有隐藏的秘密。
### 已知限制

- 设置入口依赖宿主设置页结构，大版本更新后可能不会及时适配
- 暂不计划支持地区解锁功能

---

## 许可证

本项目使用木兰公共许可证，第 2 版（Mulan PubL v2）。 
完整授权见 [LICENSE](https://license.coscl.org.cn/MulanPubL-2.0)。
