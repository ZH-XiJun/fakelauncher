package com.wtbruh.fakelauncher;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.Arrays;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private final static String TAG = SettingsFragment.class.getSimpleName();

    private final Activity activity;

    public SettingsFragment (Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register the listener that observe changes of SharedPreference
        // 注册检测SharedPreference变化的监听器
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener that observe changes of SharedPreference
        // 注销检测SharedPreference变化的监听器
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Initialize 初始化
     */
    private void init() {
        Preference pref;
        SharedPreferences defaultPref = getDefaultSharedPreferences(activity);
        // Init of clickable preferences
        String[] clickablePrefs = getResources().getStringArray(R.array.clickable_prefs);
        for (String key : clickablePrefs) {
            pref = findPreference(key);
            if (pref != null) pref.setOnPreferenceClickListener(this);
        }
        // Init of pref "privilege_provider"
        pref = findPreference("privilege_provider");
        if (pref != null) {
            setListPrefSummary(
                    defaultPref.getString("privilege_provider", "None"),
                    pref,
                    R.array.pref_privilege_provider,
                    R.array.pref_privilege_provider_string
            );
        }
        // Init of pref "check_xposed"
        pref = findPreference("check_xposed");
        if (pref != null){
            if (MainActivity.isXposedModuleActivated()) {
                pref.setSummary(R.string.pref_xposed_activated);
            } else {
                pref.setSummary(R.string.pref_xposed_not_activated);
            }
        }
    }

    /**
     * Convert value of ListPreference to text description<br>
     * and set the description as the summary the List Preference<br>
     *
     * 将列表类首选项（ListPreference）的值转为文字介绍<br>
     * 然后将文字介绍设置成该ListPreference的小字简介
     *
     *
     * @param value ListPreference目前的值
     * @param pref 这个ListPreference的对象
     * @param valueArrayResId 该ListPreference的原始值，对应字符串数组的资源ID
     * @param valueToStringArrayResId 该ListPreference的值转换为文字介绍后，对应的字符串数组的资源ID
     */
    private void setListPrefSummary(String value, Preference pref, int valueArrayResId, int valueToStringArrayResId){
        String[] valueArray = getResources().getStringArray(valueArrayResId);
        String[] valueToStringArray = getResources().getStringArray(valueToStringArrayResId);
        int index  = Arrays.asList(valueArray).indexOf(value);
        pref.setSummary(valueToStringArray[index]);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null) return;
        Log.d(TAG, "Shared preference changed! key:"+key);
        Preference pref = findPreference(key);

        if (pref == null) return;
        switch (key) {
            case "privilege_provider":
                setListPrefSummary(
                        sharedPreferences.getString(key, "None"),
                        pref,
                        R.array.pref_privilege_provider,
                        R.array.pref_privilege_provider_string
                );
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
        if (defaultPref == null) return false;
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
                boolean result = PrivilegeProvider.checkDeviceAdmin(dhizuku, activity);
                if (! result) {
                    pref.setSummary(R.string.pref_check_privilege_not_granted);
                } else if (dhizuku){
                    pref.setSummary(R.string.pref_check_privilege_granted_dhizuku);
                } else {
                    pref.setSummary(R.string.pref_check_privilege_granted);
                }
                break;
            case "permission_grant_status":
                UIHelper.intentStarter(activity, SettingsActivity.PermissionStatus.class);
                break;
        }
        return false;
    }
}