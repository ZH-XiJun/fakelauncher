package com.wtbruh.fakelauncher.ui.fragment;

import android.view.KeyEvent;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;

public abstract class BaseFragment extends Fragment {

    public final static String LEFT_BTN = "l";
    public final static String RIGHT_BTN = "r";
    public final static String CENTER_BTN = "c";

    public final static String[] L_DEFAULT = {LEFT_BTN, String.valueOf(R.string.common_leftButton)};
    public final static String[] L_EMPTY = {LEFT_BTN, "-1"};
    public final static String[] L_OPTION = {LEFT_BTN, String.valueOf(R.string.option_leftButton)};
    public final static String[] L_SAVE = {LEFT_BTN, String.valueOf(R.string.save_leftButton)};
    public final static String[] R_DEFAULT = {RIGHT_BTN, String.valueOf(R.string.common_rightButton)};
    public final static String[] R_EDITTEXT = {RIGHT_BTN, String.valueOf(R.string.edittext_rightButton)};
    public final static String[] R_EMPTY = {RIGHT_BTN, "-1"};
    public final static String[] C_PLAY = {CENTER_BTN, String.valueOf(R.string.play_centerButton)};
    public final static String[] C_PAUSE = {CENTER_BTN, String.valueOf(R.string.pause_centerButton)};
    public final static String[] C_RESUME = {CENTER_BTN, String.valueOf(R.string.resume_centerButton)};
    public final static String[] C_STOP = {CENTER_BTN, String.valueOf(R.string.stop_centerButton)};
    public final static String[] C_SHOOT = {CENTER_BTN, String.valueOf(R.string.shoot_centerButton)};
    public final static String[] C_RECORD = {CENTER_BTN, String.valueOf(R.string.record_centerButton)};

    public View rootView;
    public abstract boolean onKeyUp(int keyCode, KeyEvent event);

    public void setFooterBar(String[]... texts) {
        ((SubActivity) requireActivity()).setFooterBar(texts);
    }

}
