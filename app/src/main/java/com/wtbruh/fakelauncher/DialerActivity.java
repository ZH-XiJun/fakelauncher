package com.wtbruh.fakelauncher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.ui.fragment.phone.DialerFragment;

/**
 * 老人机风格拨号界面：作为系统默认通话应用的拨号入口，
 * 处理 ACTION_DIAL / ACTION_VIEW(tel:) 意图（同时满足 DIALER 角色资格）。
 */
public class DialerActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);

        String number = "";
        Intent i = getIntent();
        String action = i != null ? i.getAction() : null;
        Uri data = i != null ? i.getData() : null;
        if ((Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action))
                && data != null && "tel".equals(data.getScheme())) {
            number = data.getSchemeSpecificPart();
        }
        if (number == null) number = "";

        Bundle args = new Bundle();
        args.putString(DialerFragment.ARG_INPUT, number);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.dialer_container, DialerFragment.newInstance(args))
                .commit();
    }
}
