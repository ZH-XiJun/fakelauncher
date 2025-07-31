package com.wtbruh.fakelauncher.ui.fragment;

import android.view.KeyEvent;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.wtbruh.fakelauncher.SubActivity;

public abstract class BaseFragment extends Fragment {

    public final static String L_DEFAULT = "L_DEFAULT";
    public final static String L_EMPTY = "L_EMPTY";
    public final static String L_OPTION = "L_OPTION";
    public final static String R_DEFAULT = "R_DEFAULT";
    public final static String R_EMPTY = "R_EMPTY";
    public final static String R_EDITTEXT = "R_EDITTEXT";
    public final static String C_PLAY = "C_PLAY";
    public final static String C_PAUSE = "C_PAUSE";
    public final static String C_RESUME = "C_RESUME";
    public View rootView;
    public abstract boolean onKeyUp(int keyCode, KeyEvent event);

    public void setFooterBar(String... texts) {
        ((SubActivity) getActivity()).setFooterBar(texts);
    }

}
