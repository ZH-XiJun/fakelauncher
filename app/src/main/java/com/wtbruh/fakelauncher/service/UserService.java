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
        return PrivilegeProvider.runCommand(PrivilegeProvider.METHOD_NORMAL, cmd);
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}