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

## 下载

> [!Note]
> CI自动构建的版本可以尝鲜到最新功能，但可能不稳定
>
> 提醒下没用过Github的：下载CI构建版本[需要一个Github帐号](https://github.com/signup)

正式版：[![GitHub Release](https://img.shields.io/github/v/release/ZH-XiJun/fakelauncher?include_prereleases)](https://github.com/zh-xijun/fakelauncher/releases)

CI构建版：[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/zh-xijun/fakelauncher/android.yml)](https://github.com/zh-xijun/fakelauncher/actions/workflows/android.yml)

## 介绍

把你的伪老机（老人机外貌但是有智能系统）伪装成一个真正的老人机

启动App后，就会进入一个仿老人机的界面，其他App全都打不开，状态栏拉不下来，触屏也没用，只能按键操控，达到伪装效果

所以这玩意有啥用，适用于哪些设备呢？下面我[举个栗子]()

## 内置App支持状态

| App | Status |
| --- | --- |
| 音乐播放器 | 老吊了，不仅能播放本地音乐还能控制其它音乐软件 |
| 相机 | 没毛病 |
| 相册 | 没毛病 |
| 联系人 | 看起来没毛病 |
| 电话 | 假的 |
| 信息 | 完全没写 |
| 设置 | 完全没写 |
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
2. 打开`FakeL 设置`并给予所有权限
3. 随便找个按键映射软件（例如`Xposed Edge Pro`）然后绑定个按键用来启动fakelauncher (com.wtbruh.fakelauncher.SplashActivity)。搞好之后，你就获得了一个老人机界面
4. 如果你想退出去，在主屏幕按Dpad键：上上下下左右左右 就可以出去了。在设置 - 行为里可以自定义！

如需视频教程，可前往[Bilibili](https://www.bilibili.com/video/BV1AweqzjEJj)查看

## 工作原理

### 0x01 屏幕固定

fakelauncher的一个招牌功能就是它能让你没法跳出这个App外，老师能看见的都是这个软件里设计好的UI。那么它是怎么实现的呢

先看看安卓原生支持的一个功能：[屏幕固定](https://support.google.com/android/answer/9455138)，但是国内可能访问不了这个网站

这是一个很冷门的功能。在启用了屏幕固定后，手机将完全定在这个App里，就连锁屏都会被覆盖。禁止跳转到其它外部App，就算被固定的App内做了跳转到其它外部App的功能也会被系统拦截。同时，状态栏在这个模式中也会被禁用。

如果希望退出App：

- 要么就要执行系统设定的特定操作（例如全面屏模式时要求从屏幕边缘上滑两次，而启用了三大金刚键则需要同时长按返回和多任务键）
- 要么，让App主动退出
- 要么，手机重启

由于老人机系统的特殊原因：

- 全按键设计，没有全面屏的同时还没给三大金刚键。
- 键盘上刚好给的是**菜单键**而并非**多任务键**

一般来讲，只能通过App自己主动退出才能退出屏幕固定，让手机恢复正常（除非不小心手机重启了）。因此，fakelauncher设计了多个隐蔽出口，一般人根本没法发现，只有你自己才能退出

在此特别感谢开源项目：[PinningApp](https://github.com/HChenX/PinningApp)，屏幕固定相关的代码基本都抄的他的

### 0x02 触控禁用

我们知道老人机是不能触控的对吧，能触控的话那不就说明这玩意有大问题了吗？所以我们还得把触控屏蔽掉

最初，我想过用Xposed把触屏事件全部拦截，但由于当时技术不精并没有实现成功。后来我们换了一个思路：

只要不设计能触屏的控件，不就是不能触屏了吗？

就这样，fakelauncher造成了无法触屏的假象。但很快我发现了几个问题：

- 如果使用了全屏模式，那个理应被禁用的导航栏居然还能被划下来，虽然里面是空的，但重点是能划下来啊，稳稳穿帮
- 百合A89那个顽固的导航栏就摆在那，隐藏不掉，稳稳穿帮
- 全面屏手势是挡不住的，稳稳穿帮
- 长按电源键能出来个可以触屏的电源菜单，稳稳穿帮（在[0x03](#0x03-xposed的神力)以另一种方式解决了）
- 快熄屏时，屏幕会降亮度，这时候点屏幕能明显看到亮度恢复，说明它能对触屏发生反应，稳稳穿帮
- 有些带高亮，可以选东西的的地方（例如相册、联系人），点屏幕时能把高亮点没，稳稳穿帮

于是在`maidang2233`的启发下，我添加了**增强型触控禁用**，使用root把触屏在/dev里对应的节点文件直接移动走，等到软件退出再恢复。现在，实现了真正意义的禁用触控


### 0x03 Xposed的神力

为了让伪装更加完美，我用到了Xposed框架。它主要在这些方面起作用：

- 启用屏幕固定：屏幕固定需要设备管理员权限才能启动，有了Xposed可以实现免权限直接启动

- 电源键逻辑修改：

老人机的定制系统中，电源键的功能很复杂。

在实际体验中我们发现电源键还起到了Home键的作用。首先，它会检测当前是否处于系统桌面。不在的话，它就会执行返回桌面的操作。

在系统桌面后，再点一下电源键，才是正常的熄屏功能。

问题在于，我被固定在fakelauncher里了，那么按电源键时理所当然的应该返回系统桌面

但是屏幕固定**是不允许打开外部App**的，所以返回系统桌面会失败。所以你怎么按电源键，它都是要返回桌面，永远没法熄屏。而且如果返回成功了，那也穿帮了是不是，无论怎样都是问题

因此我们修改了这个逻辑，现在，检测到在fakelauncher内，它也会直接执行熄屏操作。用其它App时它还是原来的逻辑，所以大可放心

电源键还有一个**长按打开电源菜单**的功能，弹出来那个电源菜单了那不就稳稳穿帮了吗，况且那里的触摸事件fakelauncher还管不到（除非你开了[0x02](#0x02-触控禁用)的增强型触控禁用）

因此我们还会修改它的长按逻辑，现在，在fakelauncher中，长按电源键会直接关机。

- 状态栏、导航栏禁用 （没做好）

## TO-DO

- [ ] 发布v1.0
- [x] 发布v0.3
- [ ] 加入MP3界面样式
- [x] 完善联系人页面
- [ ] 加入短信页面
- [ ] ~~视频的进度条~~，照片全屏浏览时可以左右换照片，~~选项菜单（相册）~~
- [ ] 伪装界面开机自启动
- [ ] 拨号盘可以打电话
- [ ] 隐藏导航栏

## 项目感谢
- [Android](https://source.android.google.cn/)
- [Xposed](https://github.com/LSPosed/LSPosed)
- [PinningApp](https://github.com/HChenX/PinningApp)
- [Shizuku API](https://github.com/RikkaApps/Shizuku-API)
- [Dhizuku API](https://github.com/iamr0s/Dhizuku-API)
