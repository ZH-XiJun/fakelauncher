package com.wtbruh.fakelauncher.ui.fragment;

import android.view.KeyEvent;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.wtbruh.fakelauncher.SubActivity;

public abstract class BaseFragment extends Fragment {
    public View rootView;
    public abstract boolean onKeyDown(int keyCode, KeyEvent event);

    public void setFooterBar(String... texts) {
        ((SubActivity) getActivity()).setFooterBar(texts);
    }

}
