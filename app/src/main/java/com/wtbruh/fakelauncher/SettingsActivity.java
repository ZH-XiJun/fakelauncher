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

    final static String TAG = SettingsActivity.class.getSimpleName();

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
        // Init of pref "pref_check_privilege"
        pref = findPreference("pref_check_privilege");
        pref.setOnPreferenceClickListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        Log.d(TAG, "Shared preference changed! key:"+key);
        Preference pref = findPreference(key);
        switch (key) {
            case "privilege_provider":
                pref.setSummary(sharedPreferences.getString(key, "None"));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        SharedPreferences defaultPref = pref.getSharedPreferences();
        String key = pref.getKey();
        String value;
        if ("pref_check_privilege".equals(key)){
            value = defaultPref.getString("privilege_provider", "None");
            if (! "None".equals(value)) {
                if (PrivilegeProvider.checkPrivilege(value)) {
                    pref.setSummary(R.string.pref_check_privilege_granted);
                } else {
                    pref.setSummary(R.string.pref_check_privilege_not_granted);
                }
            } else {
                pref.setSummary(R.string.pref_check_privilege_none);
            }
        }
        return false;
    }
}