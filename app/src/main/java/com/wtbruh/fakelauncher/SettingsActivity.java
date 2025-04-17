package com.wtbruh.fakelauncher;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.wtbruh.fakelauncher.utils.PrivilegeProvider;


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
        // Init of pref "privilege_provider"
        pref = findPreference("privilege_provider");
        pref.setSummary(defaultPref.getString("privilege_provider", "None"));
        // Init of pref "check_privilege"
        pref = findPreference("check_privilege");
        pref.setOnPreferenceClickListener(this);
        // Init of pref "check_device_admin"
        pref = findPreference("check_device_admin");
        pref.setOnPreferenceClickListener(this);
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
        }
        return false;
    }
}