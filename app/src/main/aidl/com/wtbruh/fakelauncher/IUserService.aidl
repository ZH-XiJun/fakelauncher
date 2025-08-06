// IUserService.aidl
package com.wtbruh.fakelauncher;

import android.os.Bundle;

interface IUserService {
    Bundle runMultiCmd(in String[] cmd) = 1;
    void destroy() = 16777114;
}