package com.wtbruh.fakelauncher.utils;

import android.view.KeyEvent;

import androidx.fragment.app.Fragment;

public abstract class MyFragment extends Fragment {

    public abstract boolean onKeyDown(int keyCode, KeyEvent event);
}
