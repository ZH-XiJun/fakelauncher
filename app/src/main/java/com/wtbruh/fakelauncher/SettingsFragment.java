package com.wtbruh.fakelauncher;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

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
        String[] valueArray;
        for (String key : clickablePrefs) {
            pref = findPreference(key);
            if (pref != null) pref.setOnPreferenceClickListener(this);
        }
        // Init of pref "privilege_provider"
        pref = findPreference("privilege_provider");
        if (pref != null) {
            setListPrefSummary(
                    defaultPref.getString(pref.getKey(), "None"),
                    pref,
                    R.array.pref_privilege_provider,
                    R.array.pref_privilege_provider_string
            );
        }
        // Init of pref "exit_fakeui_method"
        pref = findPreference("exit_fakeui_method");
        if (pref != null) {
            setListPrefSummary(
                    defaultPref.getString(pref.getKey(), "dpad"),
                    pref,
                    R.array.pref_exit_fakeui_method,
                    R.array.pref_exit_fakeui_method_string
            );
        }
        // Init of pref "exit_fakeui_config"
        DialogPreference dPref = findPreference("exit_fakeui_config");
        if (dPref != null) {
            valueArray = getResources().getStringArray(R.array.pref_exit_fakeui_method);
            if (defaultPref.getString("exit_fakeui_method", valueArray[0]).equals(valueArray[1])) {
                dPref.setDialogTitle(R.string.dialog_title_exit_dialer);
            } else if (defaultPref.getString("exit_fakeui_method", valueArray[0]).equals(valueArray[2])) {
                dPref.setDialogTitle(R.string.dialog_title_exit_passwd);
            } else if (defaultPref.getString("exit_fakeui_method", valueArray[0]).equals(valueArray[0])) {
                dPref.setDialogTitle(R.string.dialog_title_exit_dpad);
            }
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
        int index = Arrays.asList(valueArray).indexOf(value);
        pref.setSummary(valueToStringArray[index]);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null) return;
        Log.d(TAG, "Shared preference changed! key:"+key);
        Preference pref = findPreference(key);
        DialogPreference dPref;
        String value;
        String[] valueArray;

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
            case "exit_fakeui_method":
                valueArray = getResources().getStringArray(R.array.pref_exit_fakeui_method);
                value = sharedPreferences.getString(key, valueArray[0]);
                DialogPreference exitFakeuiConfig = findPreference("exit_fakeui_config");
                setListPrefSummary(
                        value,
                        pref,
                        R.array.pref_exit_fakeui_method,
                        R.array.pref_exit_fakeui_method_string
                );
                if (value.equals(valueArray[1])) {
                    exitFakeuiConfig.setDialogTitle(R.string.dialog_title_exit_dialer);
                } else if (value.equals(valueArray[2])) {
                    exitFakeuiConfig.setDialogTitle(R.string.dialog_title_exit_passwd);
                } else if (value.equals(valueArray[0])) {
                    exitFakeuiConfig.setDialogTitle(R.string.dialog_title_exit_dpad);
                }
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
        String value;
        switch (key) {
            case "check_privilege":
                value = defaultPref.getString("privilege_provider", "None");
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
                boolean checkDeviceAdmin = PrivilegeProvider.checkDeviceAdmin(dhizuku, activity);
                if (! checkDeviceAdmin) {
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
            case "grant_all_permissions":
                // to-do
                break;
        }
        return false;
    }
}