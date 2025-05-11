// IUserService.aidl
package com.wtbruh.fakelauncher;

interface IUserService {
    void onCreate() = 1;
    void onDestroy() = 2;

    // 20以内的transact code是保留给未来的Dhizuku APi使用的。
    // transact codes up to 20 are reserved for future Dhizuku API.
    void setLockTaskPackages(in ComponentName receiver, in String[] packageNames) = 21;

}