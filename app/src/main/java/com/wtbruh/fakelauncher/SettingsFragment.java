package com.wtbruh.fakelauncher;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.Arrays;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private final static String TAG = SettingsFragment.class.getSimpleName();

    private final Activity activity;

    public final static String PREF_PRIVILEGE_PROVIDER = "privilege_provider";
    public final static String PREF_EXIT_FAKEUI_CONFIG = "exit_fakeui_config";
    public final static String PREF_EXIT_FAKEUI_METHOD = "exit_fakeui_method";
    public final static String PREF_CHECK_PRIVILEGE = "check_privilege";
    public final static String PREF_CHECK_DEVICE_ADMIN = "check_device_admin";
    public final static String PREF_ENABLE_DHIZUKU = "enable_dhizuku";
    public final static String PREF_PERMISSION_GRANT_STATUS = "permission_grant_status";
    public final static String PREF_CHECK_XPOSED = "check_xposed";
    public final static String PREF_GRANT_ALL_PERMISSIONS = "grant_all_permissions";

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
        pref = findPreference(PREF_PRIVILEGE_PROVIDER);
        if (pref != null) prefSetup(pref);
        // Init of pref "exit_fakeui_method"
        pref = findPreference(PREF_EXIT_FAKEUI_METHOD);
        if (pref != null) prefSetup(pref);
        // Init of pref "exit_fakeui_config"
        pref = findPreference(PREF_EXIT_FAKEUI_CONFIG);
        if (pref != null) prefSetup(pref);
        // Init of pref "check_xposed"
        pref = findPreference(PREF_CHECK_XPOSED);
        if (pref != null) prefSetup(pref);
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
        int index = Arrays.asList(valueArray).indexOf(value);
        if (index == -1) return;
        pref.setSummary(valueToStringArray[index]);
    }

    /**
     * Preference setup code package<br>
     * Preference设置代码 封装
     * @param pref Preference
     */
    private void prefSetup(Preference pref) {
        SharedPreferences defaultPref = getDefaultSharedPreferences(activity);
        String[] valueArray;
        String value;
        switch (pref.getKey()) {
            case PREF_PRIVILEGE_PROVIDER:
                setListPrefSummary(
                        defaultPref.getString(pref.getKey(), "None"),
                        pref,
                        R.array.pref_privilege_provider,
                        R.array.pref_privilege_provider_string
                );
                break;
            case PREF_EXIT_FAKEUI_METHOD:
                setListPrefSummary(
                        defaultPref.getString(pref.getKey(), "dpad"),
                        pref,
                        R.array.pref_exit_fakeui_method,
                        R.array.pref_exit_fakeui_method_string
                );
                break;
            case PREF_EXIT_FAKEUI_CONFIG:
                DialogPreference dPref = (DialogPreference) pref;
                // DialogPreference dPref = findPreference(PREF_EXIT_FAKEUI_CONFIG);
                valueArray = getResources().getStringArray(R.array.pref_exit_fakeui_method);
                value = defaultPref.getString(PREF_EXIT_FAKEUI_METHOD, valueArray[0]);
                if (value.equals(valueArray[1])) {
                    dPref.setDialogTitle(R.string.dialog_title_exit_dialer);
                    dPref.setDialogMessage(R.string.dialog_title_exit_dialer_hint);
                } else if (value.equals(valueArray[2])) {
                    dPref.setDialogTitle(R.string.dialog_title_exit_passwd);
                    dPref.setDialogMessage(null);
                } else if (value.equals(valueArray[0])) {
                    dPref.setDialogTitle(R.string.dialog_title_exit_dpad);
                    dPref.setDialogMessage(null);
                }
                break;
            case PREF_CHECK_XPOSED:
                if (MainActivity.isXposedModuleActivated()) {
                    pref.setSummary(R.string.pref_xposed_activated);
                } else {
                    pref.setSummary(R.string.pref_xposed_not_activated);
                }
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null) return;
        Log.d(TAG, "Shared preference changed! key:"+key);
        Preference pref = findPreference(key);
        String value;
        String[] valueArray;

        if (pref == null) return;
        switch (key) {
            case PREF_PRIVILEGE_PROVIDER:
                prefSetup(pref);
                findPreference(PREF_CHECK_PRIVILEGE).setSummary("");
                break;
            case PREF_EXIT_FAKEUI_METHOD:
                EditTextPreference exitFakeuiConfig = findPreference(PREF_EXIT_FAKEUI_CONFIG);
                prefSetup(pref);
                prefSetup(exitFakeuiConfig);
                sharedPreferences.edit()
                        .putString(PREF_EXIT_FAKEUI_CONFIG, "")
                        .apply();
                exitFakeuiConfig.setText("");
                break;
            case PREF_ENABLE_DHIZUKU:
                findPreference(PREF_CHECK_DEVICE_ADMIN).setSummary("");
                break;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        SharedPreferences defaultPref = pref.getSharedPreferences();
        if (defaultPref == null) return false;
        String key = pref.getKey();
        String value;
        switch (key) {
            case PREF_CHECK_PRIVILEGE:
                value = defaultPref.getString(PREF_PRIVILEGE_PROVIDER, "None");
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
            case PREF_CHECK_DEVICE_ADMIN:
                boolean dhizuku = defaultPref.getBoolean(PREF_ENABLE_DHIZUKU, false);
                boolean checkDeviceAdmin = PrivilegeProvider.checkDeviceAdmin(dhizuku, activity);
                if (! checkDeviceAdmin) {
                    pref.setSummary(R.string.pref_check_privilege_not_granted);
                } else if (dhizuku){
                    pref.setSummary(R.string.pref_check_privilege_granted_dhizuku);
                } else {
                    pref.setSummary(R.string.pref_check_privilege_granted);
                }
                break;
            case PREF_PERMISSION_GRANT_STATUS:
                UIHelper.intentStarter(activity, SettingsActivity.PermissionStatus.class);
                break;
            case PREF_GRANT_ALL_PERMISSIONS:
                // to-do
                break;
        }
        return false;
    }
}