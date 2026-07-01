# BBZQ
> 使用 `libxposed API 102`、由 Kotlin 全量编写的哔哩哔哩增强 Xposed 模组

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)
![API](https://img.shields.io/badge/libxposed-API%20102-orange)
![License](https://img.shields.io/badge/license-Mulan%20PubL%20v2-blue)

---

## 简介
BBZQ 是一款适配 libxposed API 102 的哔哩哔哩功能增强模组，采用 Kotlin 完整实作，旨在去除不必要内容、优化核心体验，同时提供各类实用功能。	
模组通过 `META-INF/xposed/java_init.list` 完成入口注册，核心逻辑入口为 `io.github.bbzq.BbzqModule`；配套设置页面内嵌于哔哩哔哩宿主应用外，方便快捷开关各项功能、调整个性化参数，整体架构便于后续迭代维护与功能扩展。

---

## 版本适配说明

| 模块版本    | 哔哩哔哩版本 |
| ------- | ------ |
| v1.0.1  | 8.97.0 |
| v1.0.2  | 8.98.0 |
| v1.0.3  | 8.99.0 |
| v1.1.0+ | 9.0.0+ |

> 说明：
> v1.0.3 的 CI 构建版本推荐 9.0.0。  
> 从 v1.1.0 开始，模组将仅适配 9.0.0 及以上版本。

---

## 使用方式

1. 安装模组 APK
2. 在支持 `libxposed API 102` 的 Xposed 框架中启用該模组
3. 将哔哩哔哩加入作用域
4. 重启目标应用
5. 进入 `我的 → 设置 → 高级设置`
6. 启用需要的功能

> [!WARNING]
> **避坑提示：** 请勿使用 Hide My Applist (HMA) 等应用隐藏模组去对「哔哩哔哩」或「BBZQ」启用隐藏列表，否则会导致功能失效或应用崩溃。
> 參考 https://github.com/HSSkyBoy/BBZQ/issues/5 和 https://github.com/HSSkyBoy/BBZQ/issues/17


> 桌面图标是模组自身介绍页，不是独立的调试工具。  
> 双击 版本 有隐藏的秘密。

---

## 功能
反正就是非常多, 具体请前往 [HSSkyBoy/BBZQ](https://github.com/HSSkyBoy/BBZQ/) 查看

---

### 已知限制

- 设置入口依赖宿主设置页结构，大版本更新后可能不会及时适配
- 暂不计划支持地区解锁功能

---

## 许可证

本项目使用木兰公共许可证，第 2 版（Mulan PubL v2）。 
完整授权见 [LICENSE](https://license.coscl.org.cn/MulanPubL-2.0)。
