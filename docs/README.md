<div align="center">
<img src="readme-assets/app-icon.png" width=128 />

## fakelauncher
*I swear, it is really just a feature phone, not a smart phone!*

**Disguise your feature-phone-like smart phone (feature phone style but uses Android system) as a real feature phone.**

</div>

我是[中国人](README-ZH.md)

> [!Caution]
> **DON'T USE THE APP ON YOUR SMARTPHONE BECAUSE EXIT THE APP REQUIRES HARDWARE KEYS**
> 
> **I AM NOT RESPONSIBLE FOR ANY DAMAGE TO YOUR PHONE**

## Description

Disguise your feature-phone-like smartphone (feature phone style but uses Android system) as a real feature phone.

Once you open it, you will be brought to a feature phone UI and not able to open any apps. Status bar will disabled. Touchscreen operation is not allowed, only keys take effect. Thus achieving a disguise effect.

So, when do you need the App? What kinds of devices are suitable for the App? I'll give an example.

## What kind of devices is suitable for the App?

Here's a feature-phone-like smart phone appears in China: 
<div align="center">

![TCL T508N](readme-assets/E1.jpg)

| Item | Value |
| --- | --- |
| Name | TCL onetouch E1 5G |
| Code | T508N |
| SoC | Unisoc T157 (ums9620) |
| Android ver. | 13 Tiramisu |

</div>

As you can see, this phone **looks like a feature phone** but actually has an high version of Android and 64 bit SoC with 5G support. That means you can use it as an common Android phone, **with games and media softwares installed.**

This feature attracted a number of senior high students in China to choose it and bring it to school so that they can use it as smartphone much safer than real smartphone. Including me.

But what if teachers notice its **UI**? It is just a common smartphone UI. So if your teachers find out your feature phone is actually a smartphone, **you're done.**

As a result, `fakelauncher` is developed by me to solve this problem. 

## How to use

1. After installed it, activate it in Xposed. Requires reboot!
2. Open `FakeL Settings` and grant all permissions
3. Use a key mapping app (For example, `Xposed Edge Pro`) and bind a key to launch fakelauncher (com.wtbruh.fakelauncher.SplashActivity). After launching it, you will be brought to a feature-phone like UI
4. If you want to exit, go to main screen, and press DPAD keys: Up, Up, Down, Down, Left, Right, Left, Right. Program will exit after pressing these keys.

## How does it work?

Take a look of a function that natively supported by Android: [Screen Pinning](https://support.google.com/android/answer/9455138)

That's why the App can disable status bar and prevent you from exiting itself and open any other apps. Here I used Xposed to hook system service. When the app starts, xposed will call method to enable Screen Pinning. Similarly, Screen Pinning will be disabled after exiting app.

Specially thanks the program: [PinningApp](https://github.com/HChenX/PinningApp), a number of codes related to Screen Pinning were used for reference.

## TO-DO

- [x] Add support for other privillege providers, like Shizuku, Dhizuku, thus achieving rootless support
- [x] Support for old Android (Tested on Android 5.1, Android 13)
- [x] Add "Star key unlock" feature
- [ ] Add Media Player (MP3) UI style
- [ ] ~~Add stroke text~~ Resizable text stroke width
- [x] Add Contacts page
- [ ] Add Message page
- [ ] ~~Video progress bar~~, select file in fullscreen photo view, option menu (Gallery)
- [ ] Dpad action can be customized (Exit method)
- [ ] ~~Camera can take photos~~ Video recording runs on Android 7 and below versions
- [ ] Dialer can make a phone call (Custom calling UI may not impossible)
- [x] Lunar calender support
- [x] Show battery percent through icons instead of accurate numbers
- [x] ~~Deprecate the default font of Android, use a bitmap font instead~~ Bitmap font didn't reach my expectations, keep on using Roboto
- [x] Add Gallery page
- [x] Add more exit method, like typing a secret code in dialer, add a avaliable password in "Please input password" page, or customize key action in replace of default, to name but a few.
- [x] Optimize the transmission of task id between UI part and Hook part (PinningApp uses `SettingsProvider`, as well as `adb shell settings put xxx`, UI part needs `WRITE_SECURE_SETTINGS` permission to set task id)

## Thanks
- [Android](https://source.android.com/)
- [Xposed](https://github.com/LSPosed/LSPosed)
- [PinningApp](https://github.com/HChenX/PinningApp)
- [Shizuku API](https://github.com/RikkaApps/Shizuku-API)
- [Dhizuku API](https://github.com/iamr0s/Dhizuku-API)
