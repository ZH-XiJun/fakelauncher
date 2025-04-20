package com.wtbruh.fakelauncher;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;


public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener {

    private final static String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        initPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the listener that observe changes of SharedPreference
        // 注册检测SharedPreference变化的监听器
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener that observe changes of SharedPreference
        // 注销检测SharedPreference变化的监听器
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Init necessary items<br>
     * 对必要的选项进行初始化
     */
    private void initPreferences() {
        Preference pref;
        SharedPreferences defaultPref = getDefaultSharedPreferences(SettingsActivity.this);
        // Init of clickable preferences
        String[] clickablePrefs = getResources().getStringArray(R.array.clickable_prefs);
        for (String key : clickablePrefs) {
            pref = findPreference(key);
            pref.setOnPreferenceClickListener(this);
        }
        // Init of pref "privilege_provider"
        pref = findPreference("privilege_provider");
        pref.setSummary(defaultPref.getString("privilege_provider", "None"));
        // Init of pref "check_xposed"
        pref = findPreference("check_xposed");
        if (MainActivity.isXposedModuleActivated()) {
            pref.setSummary(R.string.pref_xposed_activated);
        } else {
            pref.setSummary(R.string.pref_xposed_not_activated);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        Log.d(TAG, "Shared preference changed! key:"+key);
        Preference pref = findPreference(key);
        switch (key) {
            case "privilege_provider":
                pref.setSummary(sharedPreferences.getString(key, "None"));
                findPreference("check_privilege").setSummary("");
                break;
            case "enable_dhizuku":
                findPreference("check_device_admin").setSummary("");
                break;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        SharedPreferences defaultPref = pref.getSharedPreferences();
        String key = pref.getKey();
        switch (key) {
            case "check_privilege":
                String value = defaultPref.getString("privilege_provider", "None");
                if (!"None".equals(value)) {
                    if (PrivilegeProvider.checkPrivilege(value)) {
                        pref.setSummary(R.string.pref_check_privilege_granted);
                    } else {
                        pref.setSummary(R.string.pref_check_privilege_not_granted);
                    }
                } else {
                    pref.setSummary(R.string.pref_check_privilege_none);
                }
                break;
            case "check_device_admin":
                boolean dhizuku = defaultPref.getBoolean("enable_dhizuku", false);
                boolean result = PrivilegeProvider.checkDeviceAdmin(dhizuku, SettingsActivity.this);
                if (! result) {
                    pref.setSummary(R.string.pref_check_privilege_not_granted);
                } else if (dhizuku){
                    pref.setSummary(R.string.pref_check_privilege_granted_dhizuku);
                } else {
                    pref.setSummary(R.string.pref_check_privilege_granted);
                }
            case "permission_grant_status":
                UIHelper.intentStarter(SettingsActivity.this, PermissionStatus.class);
        }
        return false;
    }

    public static class PermissionStatus extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_listview);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }
}