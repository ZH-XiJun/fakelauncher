package com.wtbruh.fakelauncher.utils;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;

public abstract class MyFragment extends Fragment {
    public View rootView;
    public abstract boolean onKeyDown(int keyCode, KeyEvent event);

    public void setFooterBar(String... texts) {
        ((SubActivity) getActivity()).setFooterBar(texts);
    }

}
