package com.wtbruh.fakelauncher.ui;


import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SettingsActivity;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.Arrays;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private final static String TAG = SettingsFragment.class.getSimpleName();

    private ActivityResultLauncher<Intent> SAFlauncher = null;

    public final static String PREF_PRIVILEGE_PROVIDER = "privilege_provider";
    public final static String PREF_EXIT_FAKEUI_CONFIG = "exit_fakeui_config";
    public final static String PREF_EXIT_FAKEUI_METHOD = "exit_fakeui_method";
    public final static String PREF_CHECK_PRIVILEGE = "check_privilege";
    public final static String PREF_CHECK_DEVICE_ADMIN = "check_device_admin";
    public final static String PREF_ENABLE_DHIZUKU = "enable_dhizuku";
    public final static String PREF_PERMISSION_GRANT_STATUS = "permission_grant_status";
    public final static String PREF_CHECK_XPOSED = "check_xposed";
    public final static String PREF_GRANT_ALL_PERMISSIONS = "grant_all_permissions";
    public final static String PREF_DEACTIVATE_DEVICE_OWNER = "deactivate_device_owner";
    public final static String PREF_DPAD_CENTER_OPEN_MENU = "dpad_center_open_menu";
    public final static String PREF_GALLERY_ACCESS = "gallery_access";
    public final static String PREF_VIBRATE_ON_START = "vibrate_on_start";
    public final static String PREF_GALLERY_ACCESS_URI = "gallery_access_uri";

    public final static String[] CLICKABLE_PREFS = {
            "check_privilege",
            "check_device_admin",
            "grant_all_permissions",
            "permission_grant_status",
            "deactivate_device_owner",
            "gallery_access"
    };

    public SettingsFragment () {
    }

    /**
     * Early init
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SAFlauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
                                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                Uri uri = result.getData().getData();
                                SharedPreferences sp = getDefaultSharedPreferences(getContext());
                                if (uri != null) {
                                    String oldUri = sp.getString(PREF_GALLERY_ACCESS_URI, "");
                                    if (!oldUri.isEmpty() && !oldUri.equals(String.valueOf(uri))) {
                                        Log.d(TAG, "User has granted another directory's access permission, revoking the old one...");
                                        getActivity().revokeUriPermission(Uri.parse(oldUri), takeFlags);
                                    }
                                    // 获取权限
                                    getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                                    // 存入SharedPreferences
                                    sp.edit()
                                            .putString(PREF_GALLERY_ACCESS_URI, String.valueOf(uri))
                                            .apply();
                                }
                            }
                        }
                );
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
        // Init of clickable preferences
        for (String key : CLICKABLE_PREFS) {
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
        pref = findPreference(PREF_CHECK_DEVICE_ADMIN);
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
        SharedPreferences defaultPref = getDefaultSharedPreferences(getContext());
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
            case PREF_CHECK_DEVICE_ADMIN:
                switch (PrivilegeProvider.checkDeviceAdmin(getContext())) {
                    case PrivilegeProvider.DHIZUKU:
                        pref.setSummary(R.string.pref_activated_dhizuku);
                        findPreference(PREF_DEACTIVATE_DEVICE_OWNER).setVisible(false);
                        break;
                    case PrivilegeProvider.DEVICE_OWNER:
                        pref.setSummary(R.string.pref_activated_device_owner);
                        findPreference(PREF_DEACTIVATE_DEVICE_OWNER).setVisible(true);
                        break;
                    case PrivilegeProvider.DEVICE_ADMIN:
                        pref.setSummary(R.string.pref_activated_device_admin);
                        findPreference(PREF_DEACTIVATE_DEVICE_OWNER).setVisible(false);
                        break;
                    case PrivilegeProvider.DEACTIVATED:
                    default:
                        pref.setSummary(R.string.pref_deactivated);
                        findPreference(PREF_DEACTIVATE_DEVICE_OWNER).setVisible(false);
                        break;
                }
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null) return;
        Log.d(TAG, "Shared preference changed! key:"+key);
        Preference pref = findPreference(key);

        if (pref == null) return;
        switch (key) {
            case PREF_PRIVILEGE_PROVIDER:
                prefSetup(pref);
                findPreference(PREF_CHECK_PRIVILEGE).setSummary(R.string.pref_tap_me);
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
                prefSetup(findPreference(PREF_CHECK_DEVICE_ADMIN));
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
                        pref.setSummary(R.string.pref_check_privilege_denied);
                    }
                } else {
                    pref.setSummary(R.string.pref_check_privilege_none);
                }
                break;
            case PREF_CHECK_DEVICE_ADMIN:
                prefSetup(pref);
                break;
            case PREF_PERMISSION_GRANT_STATUS:
                UIHelper.intentStarter(getActivity(), SettingsActivity.PermissionStatus.class);
                break;
            case PREF_GRANT_ALL_PERMISSIONS:
                // todo
                break;
            case PREF_DEACTIVATE_DEVICE_OWNER:
                DevicePolicyManager dpm = getSystemService(getActivity(), DevicePolicyManager.class);
                dpm.clearDeviceOwnerApp(getActivity().getPackageName());
                prefSetup(findPreference(PREF_CHECK_DEVICE_ADMIN));
            case PREF_GALLERY_ACCESS:
                SAFlauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
        }
        return false;
    }
}