package com.wtbruh.fakelauncher.ui.fragment.settings;


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

public class SubSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private final static String TAG = SubSettingsFragment.class.getSimpleName();
    private final static String ARG_PAGE = "page";

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
    public final static String PREF_STYLE = "style";
    public final static String PREF_TIME_SHOW_SECOND = "time_show_second";
    public final static String PREF_SHOW_ACCURATE_BATTERY = "show_accurate_battery";
    // todo: public final static String PREF_TEXT_STROKE_WIDTH = "text_stroke_width";

    public SubSettingsFragment() {
    }

    public static SubSettingsFragment newInstance(String page) {
        SubSettingsFragment fragment = new SubSettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE, page);
        fragment.setArguments(args);
        return fragment;
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
                                SharedPreferences sp = getDefaultSharedPreferences(requireContext());
                                if (uri != null) {
                                    String oldUri = sp.getString(PREF_GALLERY_ACCESS_URI, "");
                                    if (!oldUri.isEmpty() && !oldUri.equals(String.valueOf(uri))) {
                                        Log.d(TAG, "User has granted another directory's access permission, revoking the old one...");
                                        requireActivity().revokeUriPermission(Uri.parse(oldUri), takeFlags);
                                    }
                                    // 获取权限
                                    requireActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
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
        // If getArguments() is null, fallback to permission page
        // 尝试调用getArguments()获取将要打开的设置子页面，如果getArguments()为null就默认打开权限界面
        int xml = R.xml.preference_permission;
        String page = SettingsFragment.PAGE_PERMISSION;

        Bundle args = getArguments();
        if (args != null) {
            page = args.getString(ARG_PAGE, SettingsFragment.PAGE_PERMISSION);
            xml = switch (page) {
                case SettingsFragment.PAGE_BEHAVIOUR -> R.xml.preference_behaviour;
                case SettingsFragment.PAGE_VIEW -> R.xml.preference_view;
                // If argument invalid, fallback to permission page
                // 如果乱传参数，默认也是打开权限页面
                default -> R.xml.preference_permission;
            };
        }
        setPreferencesFromResource(xml, rootKey);
        init(page);
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
    private void init(String page) {
        Preference pref;
        // List of preferences need OnPreferenceClickListener
        String[] clickablePrefs = new String[0];
        // List of preferences need call prefSetup()
        String[] setupPrefs = new String[0];
        // Resource ID of title
        int titleResId = R.string.pref_page_permissions;

        switch (page) {
            case SettingsFragment.PAGE_PERMISSION:
                clickablePrefs = new String[]{
                        PREF_CHECK_PRIVILEGE,
                        PREF_CHECK_DEVICE_ADMIN,
                        PREF_GRANT_ALL_PERMISSIONS,
                        PREF_PERMISSION_GRANT_STATUS,
                        PREF_DEACTIVATE_DEVICE_OWNER,
                        PREF_GALLERY_ACCESS
                };
                setupPrefs = new String[]{
                        PREF_PRIVILEGE_PROVIDER,
                        PREF_CHECK_XPOSED,
                        PREF_CHECK_DEVICE_ADMIN
                };
                // titleResId = R.string.pref_page_permissions;
                break;
            case SettingsFragment.PAGE_VIEW:
                setupPrefs = new String[]{
                        PREF_STYLE
                };
                titleResId = R.string.pref_page_view;
                break;
            case SettingsFragment.PAGE_BEHAVIOUR:
                setupPrefs = new String[]{
                        PREF_EXIT_FAKEUI_METHOD
                };
                titleResId = R.string.pref_page_behaviour;
                break;
        }
        // Init of clickable preferences
        for (String key : clickablePrefs) {
            if ((pref = findPreference(key)) != null) pref.setOnPreferenceClickListener(this);
        }
        // Init of preferences need call prefSetup()
        for (String key : setupPrefs) {
            if ((pref = findPreference(key)) != null) prefSetup(pref);
        }
        // Init of title
        SettingsActivity activity = (SettingsActivity)getActivity();
        if (activity != null) activity.setToolbarTitle(titleResId);
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
     * Replace the summary of the switch preference in need with warning message<br>
     * 将某些开关设置（SwitchPreference）的简介设置为警告（该设置为启用状态）
     * @param pref 该设置的Preference对象
     * @param defaultSummary 默认的简介
     */
    private void addWarningToSummary(Preference pref, int defaultSummary) {
        boolean prefValue = pref.getSharedPreferences().getBoolean(pref.getKey(), false);
        pref.setSummary(prefValue? R.string.pref_caution : defaultSummary);
    }

    /**
     * Preference setup code package<br>
     * Preference设置代码 封装
     * @param pref Preference
     */
    private void prefSetup(Preference pref) {
        SharedPreferences defaultPref = getDefaultSharedPreferences(requireContext());
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
            case PREF_STYLE:
                setListPrefSummary(
                        defaultPref.getString(pref.getKey(), "phone"),
                        pref,
                        R.array.pref_style,
                        R.array.pref_style_string
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

        int defaultSummary = 0;
        switch (key) {
            case PREF_PRIVILEGE_PROVIDER:
                prefSetup(pref);
                if ((pref = findPreference(PREF_CHECK_PRIVILEGE)) != null) pref.setSummary(R.string.pref_tap_me);
                break;
            case PREF_EXIT_FAKEUI_METHOD:
                EditTextPreference exitFakeuiConfig = findPreference(PREF_EXIT_FAKEUI_CONFIG);
                prefSetup(pref);
                if (exitFakeuiConfig != null) {
                    prefSetup(exitFakeuiConfig);
                    sharedPreferences.edit()
                            .putString(PREF_EXIT_FAKEUI_CONFIG, "")
                            .apply();
                    exitFakeuiConfig.setText("");
                }
                break;
            case PREF_ENABLE_DHIZUKU:
                if ((pref = findPreference(PREF_CHECK_DEVICE_ADMIN)) != null) prefSetup(pref);
                break;
            case PREF_STYLE:
                prefSetup(pref);
                break;
            case PREF_TIME_SHOW_SECOND:
                defaultSummary = R.string.pref_time_show_seconds_summary;
                break;
            case PREF_SHOW_ACCURATE_BATTERY:
                defaultSummary = R.string.pref_show_accurate_battery_summary;
                break;
        }
        if (defaultSummary != 0) addWarningToSummary(pref, defaultSummary);
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        SharedPreferences defaultPref = pref.getSharedPreferences();
        if (defaultPref == null) return false;
        String key = pref.getKey();
        String value;
        switch (key) {
            case PREF_CHECK_PRIVILEGE -> {
                value = defaultPref.getString(PREF_PRIVILEGE_PROVIDER, "None");
                new Thread(() -> {
                    if (!"None".equals(value)) {
                        boolean isGranted = PrivilegeProvider.checkPrivilege(PrivilegeProvider.privilegeToInt(value));
                        requireActivity().runOnUiThread(() -> {
                            if (isGranted) {
                                pref.setSummary(R.string.pref_check_privilege_granted);
                            } else {
                                pref.setSummary(R.string.pref_check_privilege_denied);
                            }
                        });

                    } else {
                        requireActivity().runOnUiThread(() -> pref.setSummary(R.string.pref_check_privilege_none));
                    }
                }).start();
            }
            case PREF_CHECK_DEVICE_ADMIN -> prefSetup(pref);
            case PREF_PERMISSION_GRANT_STATUS ->
                    UIHelper.intentStarter(getActivity(), SettingsActivity.PermissionStatus.class);
            case PREF_GRANT_ALL_PERMISSIONS -> {
                value = defaultPref.getString(PREF_PRIVILEGE_PROVIDER, "None");
                new Thread(() -> {
                    PrivilegeProvider.requestAllPermissions(getActivity(), PrivilegeProvider.privilegeToInt(value));
                    requireActivity().runOnUiThread(() -> pref.setSummary(R.string.pref_operation_completed));
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    requireActivity().runOnUiThread(() -> pref.setSummary(""));
                }).start();

            }
            case PREF_DEACTIVATE_DEVICE_OWNER -> {
                DevicePolicyManager dpm = getSystemService(requireContext(), DevicePolicyManager.class);
                dpm.clearDeviceOwnerApp(requireActivity().getPackageName());
                prefSetup(findPreference(PREF_CHECK_DEVICE_ADMIN));
            }
            case PREF_GALLERY_ACCESS ->
                    SAFlauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
        }
        return false;
    }
}