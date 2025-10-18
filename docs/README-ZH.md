<div align="center">
<img src="readme-assets/app-icon.png" width=174 alt="App图标"/>

## fakelauncher ![GitHub Release](https://img.shields.io/github/v/release/ZH-XiJun/fakelauncher?include_prereleases)

*你自己看，这绝对是个老人机，不是智能机啊*

**把你的伪老机（老人机外貌但是有智能系统）伪装成一个真正的老人机**

</div>

> [!Caution]
> **不要尝试在真智能手机上运行这个App，等会没实体按键退不出去就老实了**
> 
> **如果因为这个软件导致你的手机出现问题，本人概不负责**

## 介绍

把你的伪老机（老人机外貌但是有智能系统）伪装成一个真正的老人机

启动App后，就会进入一个仿老人机的界面，其他App全都打不开，状态栏拉不下来，触屏也没用，只能按键操控，达到伪装效果

所以这玩意有啥用，适用于哪些设备呢？下面我举个栗子

## 适用设备

这里有款应该算是耳熟能详的老人机：
<div align="center">

![TCL T508N](readme-assets/E1.jpg)

| 项目         | 参数                    |
|------------|-----------------------|
| 名称         | TCL onetouch E1 5G    |
| 代号         | T508N                 |
| SoC        | Unisoc T157 (ums9620) |
| Android 版本 | 13 Tiramisu           |

</div>

可以发现，这玩意**长得像个老人机**但其实有高达安卓13的系统，还是64位系统，还支持5G（至于为什么强调64位，问就是64位SoC跑32位系统的痛）。这意味着，这玩意完全能当个正常手机用，**装游戏、抖音B站、微信QQ都没问题**

基于这个牛逼的特性，有一批高中生就买这类老人机带学校里玩，啊当然也包括我。但是这玩意长得像老人机，但他UI完全就是普通手机啊，如果哪天老师看到你这玩意是个智能机系统那你不炸了？

为了解决这个Bug，`fakelauncher`就此被开发出来了

## 食用方法

1. 安装好之后，去Xposed里激活它（推荐用LSPosed，因为我只测试过用它）。记得重启！
2. 打开`FakeL Settings`并给予所有权限
3. 随便找个按键映射软件（例如`Xposed Edge Pro`）然后绑定个按键用来启动fakelauncher (com.wtbruh.fakelauncher.SplashActivity)。搞好之后，你就获得了一个老人机界面
4. 如果你想退出去，在主屏幕按Dpad键：上上下下左右左右 就可以出去了

如需视频教程，可前往[Bilibili](https://www.bilibili.com/video/BV1AweqzjEJj)查看

## 工作原理

先看看安卓原生支持的一个功能：[屏幕固定](https://support.google.com/android/answer/9455138)，但是国内可能访问不了

利用屏幕固定，就可以达到禁用状态栏并阻止打开其他App的功能了。在此使用了Xposed，hook了系统服务后检测到应用打开就会调用启动屏幕固定的方法，检测到应用退出后就会按照同样方法解除屏幕固定。

在此特别感谢开源项目：[PinningApp](https://github.com/HChenX/PinningApp)，屏幕固定相关的代码基本都抄的他的

## 下载

> [!Note]
> CI自动构建的版本可以尝鲜到最新功能，但可能不稳定
>
> 提醒下没用过Github的：下载CI构建版本[需要一个Github帐号](https://github.com/signup)

正式版：[![GitHub Release](https://img.shields.io/github/v/release/ZH-XiJun/fakelauncher?include_prereleases)](https://github.com/zh-xijun/fakelauncher/releases)

CI构建版：[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/zh-xijun/fakelauncher/android.yml)](https://github.com/zh-xijun/fakelauncher/actions/workflows/android.yml)

## TO-DO

- [ ] 发布v0.3
- [x] 远古安卓支持（已测试安卓5.1和安卓13）
- [x] 在系统层面上屏蔽触控（目前是在软件层面，避免任何可触控的控件出现）（感谢maidang2233）
- [ ] 加入MP3界面样式
- [x] ~~加入描边字体~~ 字体描边宽度可调整
- [x] 主界面的界面大小可调
- [ ] 完善联系人页面
- [ ] 加入短信页面
- [ ] ~~视频的进度条~~，照片全屏浏览时可以左右换照片，选项菜单（相册）
- [ ] 伪装界面开机自启动
- [ ] 长按电源键时，直接关机而非打开电源菜单
- [x] Dpad操作可以自定义（退出方法）
- [ ] 拨号盘可以打电话（但是自定义打电话的界面可能做不到）
- [ ] 隐藏导航栏

## 项目感谢
- [Android](https://source.android.google.cn/)
- [Xposed](https://github.com/LSPosed/LSPosed)
- [PinningApp](https://github.com/HChenX/PinningApp)
- [Shizuku API](https://github.com/RikkaApps/Shizuku-API)
- [Dhizuku API](https://github.com/iamr0s/Dhizuku-API)
