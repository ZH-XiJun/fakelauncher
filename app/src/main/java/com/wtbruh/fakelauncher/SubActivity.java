package com.wtbruh.fakelauncher;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.wtbruh.fakelauncher.ui.MenuFragment;
import com.wtbruh.fakelauncher.utils.MyFragment;
import com.wtbruh.fakelauncher.utils.MyAppCompatActivity;

public class SubActivity extends MyAppCompatActivity {

    public final static int LEFT_BUTTON = R.id.leftButton;
    public final static int CENTER_BUTTON = R.id.centerButton;
    public final static int RIGHT_BUTTON = R.id.rightButton;
    public final static String L_DEFAULT = "L_DEFAULT";
    public final static String L_MENU = "L_MENU";
    public final static String R_DEFAULT = "R_DEFAULT";
    public final static String R_EDITTEXT = "R_EDITTEXT";

    private final static String TAG = SubActivity.class.getSimpleName();

    private MyFragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentFragment = MenuFragment.newInstance();
        setContentView(R.layout.activity_fragment);
        if (savedInstanceState == null) {
            fragmentStarter(mCurrentFragment);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mCurrentFragment.onKeyDown(keyCode, event)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() <= 0) super.onBackPressed();
        else {
            getSupportFragmentManager().popBackStack();
            mCurrentFragment = (MyFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        }
    }

    public void fragmentStarter(MyFragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commitNow();
        mCurrentFragment = fragment;
    }

    public void setActionBar(String... texts) {
        TextView view;
        boolean showCenterButton = false;
        for (String text : texts) {
            Log.d(TAG, "setActionBar: "+text);
            if (text.contains("L_")) view = findViewById(LEFT_BUTTON);
            else if (text.contains("R_")) view = findViewById(RIGHT_BUTTON);
            else if (text.contains("C_")) {
                view = findViewById(CENTER_BUTTON);
                showCenterButton = true;
            }
            else return;
            switch (text) {
                case L_DEFAULT:
                    view.setText(R.string.common_leftbutton);
                    break;
                case L_MENU:
                    view.setText(R.string.main_leftbutton);
                    break;
                case R_DEFAULT:
                    view.setText(R.string.common_rightbutton);
                    break;
                case R_EDITTEXT:
                    view.setText(R.string.edittext_rightbutton);
            }
        }
        view = findViewById(CENTER_BUTTON);
        if (showCenterButton) {
            view.setVisibility(TextView.VISIBLE);
        } else {
            view.setVisibility(TextView.INVISIBLE);
        }
    }

}