package com.wtbruh.fakelauncher.service;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;

import com.wtbruh.fakelauncher.IUserService;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

public class UserService extends IUserService.Stub {

    @Keep
    public UserService() {

    }

    @Keep
    public UserService(Context context) {

    }

    @Override
    public Bundle runMultiCmd(String[] cmd) {
        return PrivilegeProvider.runCommand(PrivilegeProvider.PRIVILEGE_NORMAL, cmd);
    }

    @Override
    public void destroy() {
        // 不退出进程：Shizuku 解绑时进程存活，KeyInterceptService 不随之崩溃，native 按键不丢失（2026-09-29 修复无法唤醒根因）
        android.util.Log.d("UserService", "destroy called - keep process alive");
    }
}