package com.wtbruh.fakelauncher.ui.fragment.settings;


import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.tencent.mmkv.MMKV;
import com.wtbruh.fakelauncher.MainActivity;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SettingsActivity;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.ui.preference.SeekBarPreference;
import com.wtbruh.fakelauncher.ui.widget.StrokeTextView;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.Arrays;
import java.util.Objects;

public class SubSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private final static String TAG = SubSettingsFragment.class.getSimpleName();
    private final static String ARG_PAGE = "page";

    private ActivityResultLauncher<Intent> gallery_saf = null;
    private ActivityResultLauncher<Intent> music_saf = null;

    public SubSettingsFragment() {
    }

    public static SubSettingsFragment newInstance(String page) {
        SubSettingsFragment fragment = new SubSettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE, page);
        fragment.setArguments(args);
        return fragment;
    }

    private ActivityResultLauncher<Intent> getSAFLauncher(String targetKeyToSave) {
        return registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
                        final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        Uri uri = result.getData().getData();
                        MMKV kv = MMKV.defaultMMKV();
                        // SharedPreferences sp = getDefaultSharedPreferences(requireContext());
                        if (uri != null) {
                            // String oldUri = sp.getString(targetKeyToSave, "");
                            String oldUri = kv.decodeString(targetKeyToSave, "");
                            if (!oldUri.isEmpty() && !oldUri.equals(String.valueOf(uri))) {
                                Log.d(TAG, "User has granted another directory's access permission, revoking the old one...");
                                requireActivity().revokeUriPermission(Uri.parse(oldUri), takeFlags);
                            }
                            // 获取权限
                            requireActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                            // 存入SharedPreferences
                            kv.encode(targetKeyToSave, String.valueOf(uri));
                            Preference pref = findPreference(targetKeyToSave);
                            if (pref != null) pref.setSummary(kv.decodeString(targetKeyToSave, getString(R.string.pref_saf_access_summary)));
                        }
                    }
                }
        );
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
        gallery_saf = getSAFLauncher(SettingsConstants.PREF_GALLERY_ACCESS);
        music_saf =  getSAFLauncher(SettingsConstants.PREF_MUSIC_ACCESS_SAF);
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
                case SettingsFragment.PAGE_DEBUG -> R.xml.preference_debug;
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
        // Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener that observe changes of SharedPreference
        // 注销检测SharedPreference变化的监听器
        // Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
        PreferenceManager.getDefaultSharedPreferences(requireContext()).unregisterOnSharedPreferenceChangeListener(this);
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
        // List of ListPreferences
        String[] listPrefs = new String[0];
        // List of preferences require root access
        String[] requireRootAccessPrefs = new String[0];
        // Resource ID of title
        int titleResId = R.string.pref_page_permissions;

        switch (page) {
            case SettingsFragment.PAGE_PERMISSION:
                clickablePrefs = new String[]{
                        SettingsConstants.PREF_CHECK_PRIVILEGE,
                        SettingsConstants.PREF_CHECK_DEVICE_ADMIN,
                        SettingsConstants.PREF_GRANT_ALL_PERMISSIONS,
                        SettingsConstants.PREF_PERMISSION_GRANT_STATUS,
                        SettingsConstants.PREF_DEACTIVATE_DEVICE_OWNER,
                        SettingsConstants.PREF_GALLERY_ACCESS,
                        SettingsConstants.PREF_MUSIC_ACCESS_SAF
                };
                setupPrefs = new String[]{
                        SettingsConstants.PREF_CHECK_XPOSED,
                        SettingsConstants.PREF_CHECK_DEVICE_ADMIN,
                        SettingsConstants.PREF_GALLERY_ACCESS,
                        SettingsConstants.PREF_MUSIC_ACCESS_SAF,
                        SettingsConstants.PREF_MUSIC_ACCESS_TYPE
                };
                listPrefs = new String[] {
                        SettingsConstants.PREF_PRIVILEGE_PROVIDER,
                        SettingsConstants.PREF_MUSIC_ACCESS_TYPE
                };
                // titleResId = R.string.pref_page_permissions;
                break;
            case SettingsFragment.PAGE_VIEW:
                clickablePrefs = new String[]{
                        SettingsConstants.PREF_TEXT_STROKE_WIDTH,
                        SettingsConstants.PREF_MAIN_UI_HEIGHT_SCALE
                };
                listPrefs = new String[]{
                        SettingsConstants.PREF_STYLE
                };
                titleResId = R.string.pref_page_view;
                break;
            case SettingsFragment.PAGE_BEHAVIOUR:
                clickablePrefs = new String[] {
                        SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_KEY,
                        SettingsConstants.PREF_SELF_DESTROY_CONFIG
                };
                setupPrefs = new String[]{
                        SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_KEY,
                        SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_PASSWD,
                };
                listPrefs = new String[]{
                        SettingsConstants.PREF_EXIT_FAKEUI_METHOD,
                };
                requireRootAccessPrefs = new String[] {
                        SettingsConstants.PREF_ENHANCED_TOUCH_BLOCKING,
                        SettingsConstants.PREF_SELF_DESTROY
                };
                titleResId = R.string.pref_page_behaviour;
                break;
            case SettingsFragment.PAGE_DEBUG:
                titleResId = R.string.pref_page_debug;
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
        for (String key : requireRootAccessPrefs) {
            if ((pref = findPreference(key)) != null) rootCheck(pref);
        }
        ListPreference lpref;
        for (String key : listPrefs) {
            if ((lpref = findPreference(key)) != null) {
                setListPrefSummary(lpref);
                lpref.setOnPreferenceChangeListener((preference, newValue) -> {
                    setListPrefSummary((ListPreference) preference, (String) newValue);
                    return true;
                });
            }
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
     * @param pref 这个ListPreference的对象
     */
    private void setListPrefSummary(ListPreference pref){
        setListPrefSummary(pref, pref.getValue());
    }

    private void setListPrefSummary(ListPreference pref, String value){
        CharSequence[] valueArray = pref.getEntryValues();
        CharSequence[] valueToStringArray = pref.getEntries();
        int index = Arrays.asList(valueArray).indexOf(value);
        if (index == -1) return;
        pref.setSummary(valueToStringArray[index]);
    }

    /**
     * Disable preference if root access is not available<br>
     * 如果Root权限不可用，就禁用该首选项
     * @param pref Preference
     */
    private void rootCheck(Preference pref) {
        SharedPreferences sp = getDefaultSharedPreferences(requireContext());
        if (PrivilegeProvider.getCurrentPrivilegeProvider(requireContext()) != PrivilegeProvider.PRIVILEGE_ROOT) {
            pref.setEnabled(false);
            pref.setSummary(R.string.pref_use_root);
            // TODO: add switch preference check
            sp.edit().putBoolean(pref.getKey(), false).apply();
        }
    }

    /**
     * Preference setup code package<br>
     * Preference设置代码 封装
     * @param pref Preference
     */
    private void prefSetup(Preference pref) {
        SharedPreferences sp = getDefaultSharedPreferences(requireContext());
        String key = pref.getKey();
        switch (key) {
            case SettingsConstants.PREF_PRIVILEGE_PROVIDER -> {
                // Shizuku available on Android 6+
                boolean shizuku = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
                if (!shizuku) {
                    ListPreference p = (ListPreference) pref;
                    p.setEntryValues(R.array.pref_privilege_provider_old);
                    p.setEntries(R.array.pref_privilege_provider_old_string);
                }
            }
            case SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_PASSWD -> {
                EditTextPreference p = (EditTextPreference) pref;
                String[] valueArray = getResources().getStringArray(R.array.pref_exit_fakeui_method);
                String value = sp.getString(SettingsConstants.PREF_EXIT_FAKEUI_METHOD, valueArray[0]);
                if (value.equals(valueArray[1])) {
                    p.setDialogTitle(R.string.dialog_title_exit_dialer);
                    p.setDialogMessage(R.string.dialog_message_exit_dialer);
                } else if (value.equals(valueArray[2])) {
                    p.setDialogTitle(R.string.dialog_title_exit_passwd);
                    p.setDialogMessage(null);
                } else if (value.equals(valueArray[0])) {
                    // Use EXIT_FAKEUI_CONFIG_KEY
                    p.setVisible(false);
                    break;
                }
                p.setVisible(true);
            }
            case SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_KEY -> {
                String[] valueArray = getResources().getStringArray(R.array.pref_exit_fakeui_method);
                String value = sp.getString(SettingsConstants.PREF_EXIT_FAKEUI_METHOD, valueArray[0]);
                pref.setVisible(value.equals(valueArray[0]));
            }
            case SettingsConstants.PREF_CHECK_XPOSED -> {
                if (ApplicationHelper.isXposedModuleActivated()) {
                    pref.setSummary(R.string.pref_xposed_activated);
                } else {
                    pref.setSummary(R.string.pref_xposed_not_activated);
                }
            }
            case SettingsConstants.PREF_CHECK_DEVICE_ADMIN -> {
                pref.setEnabled(true);
                Preference p = findPreference(SettingsConstants.PREF_DEACTIVATE_DEVICE_OWNER);
                if (p == null) break;
                switch (PrivilegeProvider.checkDeviceAdmin(requireContext())) {
                    case PrivilegeProvider.DHIZUKU -> {
                        pref.setSummary(R.string.pref_activated_dhizuku);
                        p.setVisible(false);
                    }
                    case PrivilegeProvider.DEVICE_OWNER -> {
                        pref.setSummary(R.string.pref_activated_device_owner);
                        p.setVisible(true);
                    }
                    case PrivilegeProvider.DEVICE_ADMIN -> {
                        pref.setSummary(R.string.pref_activated_device_admin);
                        p.setVisible(false);
                    }
                    default -> {
                        String provider;
                        boolean isPrivilegeProviderNone =
                                (provider = sp.getString(SettingsConstants.PREF_PRIVILEGE_PROVIDER, getString(R.string.pref_privilege_provider_default))).equals("None");
                        boolean isDhizukuEnabled =
                                sp.getBoolean(SettingsConstants.PREF_ENABLE_DHIZUKU, false);
                        int newSummary = isDhizukuEnabled? R.string.pref_deactivated :
                                isPrivilegeProviderNone? R.string.pref_command_activate: R.string.pref_click_to_activate;
                        if (!Objects.equals(pref.getSummary(), getString(newSummary)) || isDhizukuEnabled) pref.setSummary(newSummary);
                        else {
                            String deviceOwnerActiveCmd = "adb shell " + PrivilegeProvider.getDeviceOwnerActiveCmd(requireContext());
                            StringBuilder sb = new StringBuilder();
                            sb.append(getString(R.string.dialog_message_active_device_owner, isPrivilegeProviderNone? "adb shell" : provider));
                            if (isPrivilegeProviderNone) sb.append(": ")
                                    .append(deviceOwnerActiveCmd);
                            new AlertDialog.Builder(requireContext())
                                    .setTitle(R.string.dialog_title_active_device_owner)
                                    .setMessage(sb.toString())
                                    .setPositiveButton(isPrivilegeProviderNone? R.string.dialog_button_copy : android.R.string.yes, (dialogInterface, i) -> {
                                        if (isPrivilegeProviderNone) {
                                            ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                            ClipData clipData = ClipData.newPlainText("sb", deviceOwnerActiveCmd);
                                            clipboardManager.setPrimaryClip(clipData);
                                        } else {
                                            PrivilegeProvider.requestDeviceOwner(requireContext(), PrivilegeProvider.privilegeToInt(provider));
                                            Preference p2 = findPreference(SettingsConstants.PREF_ENABLE_DHIZUKU);
                                            if (p2 == null) return;
                                            p2.setEnabled(false);
                                            pref.setSummary(R.string.pref_wait);
                                            pref.setEnabled(false);
                                            new Handler().postDelayed(() -> {
                                                p2.setEnabled(true);
                                                prefSetup(pref);
                                            }, 5000);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, null)
                                    .show();
                        }
                        p.setVisible(false);
                    }
                }
            }
            case SettingsConstants.PREF_SELF_DESTROY_CONFIG -> {
                SwitchPreference p = findPreference(SettingsConstants.PREF_SELF_DESTROY);
                boolean v = p != null && p.isChecked();
                pref.setVisible(v);

            }

            case SettingsConstants.PREF_GALLERY_ACCESS, SettingsConstants.PREF_MUSIC_ACCESS_SAF -> {
                MMKV kv = MMKV.defaultMMKV();
                pref.setSummary(kv.decodeString(key, getString(R.string.pref_saf_access_summary)));
            }
            case SettingsConstants.PREF_MUSIC_ACCESS_TYPE -> {
                Preference p = findPreference(SettingsConstants.PREF_MUSIC_ACCESS_SAF);
                if (p != null) p.setVisible(
                        sp.getString(SettingsConstants.PREF_MUSIC_ACCESS_TYPE, getString(R.string.pref_music_access_type_default)).equals("custom_dir")
                );
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, @Nullable String key) {
        if (key == null) return;

        Log.d(TAG, "Shared preference changed! key:"+key);
        Preference pref = findPreference(key);

        if (pref == null) return;

        switch (key) {
            case SettingsConstants.PREF_PRIVILEGE_PROVIDER -> {
                prefSetup(pref);
                if ((pref = findPreference(SettingsConstants.PREF_CHECK_PRIVILEGE)) != null)
                    pref.setSummary(R.string.pref_tap_me);
                Preference p = findPreference(SettingsConstants.PREF_CHECK_DEVICE_ADMIN);
                if (p != null) p.setSummary(R.string.pref_tap_me);
            }
            case SettingsConstants.PREF_EXIT_FAKEUI_METHOD -> {
                EditTextPreference exitFakeuiConfig = findPreference(SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_PASSWD);
                Preference p = findPreference(SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_KEY);
                prefSetup(pref);
                if (exitFakeuiConfig != null && p != null) {
                    prefSetup(exitFakeuiConfig);
                    prefSetup(p);
                    String currentPasswd = exitFakeuiConfig.getText();
                    if (currentPasswd == null || currentPasswd.isEmpty()) {
                        sp.edit().putString(SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_PASSWD, "1234")
                                .apply();
                        exitFakeuiConfig.setText("1234");
                    }
                }
            }
            case SettingsConstants.PREF_ENABLE_DHIZUKU -> {
                if ((pref = findPreference(SettingsConstants.PREF_CHECK_DEVICE_ADMIN)) != null) prefSetup(pref);
            }
            case SettingsConstants.PREF_MAIN_UI_HEIGHT_SCALE -> {
                SeekBarPreference p = findPreference(key);
                if (p != null && p.getValue() == 0) p.setValue(10);
            }
            case SettingsConstants.PREF_ENHANCED_TOUCH_BLOCKING -> {
                SwitchPreference p = (SwitchPreference) pref;
                if (sp.getBoolean(SettingsConstants.PREF_ENHANCED_TOUCH_BLOCKING, false)) {

                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.dialog_message_enhance_touch_blocking)
                            .setPositiveButton(R.string.dialog_btn_understand, null)
                            .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> p.setChecked(false))
                            .show();
                }
            }
            case SettingsConstants.PREF_SELF_DESTROY -> {
                SwitchPreference p = (SwitchPreference) pref;
                if (sp.getBoolean(SettingsConstants.PREF_SELF_DESTROY, false)) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_warning)
                            .setMessage(R.string.dialog_message_self_destroy)
                            .setPositiveButton(R.string.dialog_btn_understand, null)
                            .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> p.setChecked(false))
                            .show();
                }
                prefSetup(Objects.requireNonNull(findPreference(SettingsConstants.PREF_SELF_DESTROY_CONFIG)));

            }

            case SettingsConstants.PREF_MUSIC_ACCESS_TYPE -> prefSetup(pref);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        SharedPreferences sp = pref.getSharedPreferences();
        if (sp == null) return false;
        String key = pref.getKey();
        String value;
        switch (key) {
            case SettingsConstants.PREF_CHECK_PRIVILEGE -> {
                value = sp.getString(SettingsConstants.PREF_PRIVILEGE_PROVIDER, getString(R.string.pref_privilege_provider_default));
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
            case SettingsConstants.PREF_CHECK_DEVICE_ADMIN -> prefSetup(pref);
            case SettingsConstants.PREF_PERMISSION_GRANT_STATUS ->
                    UIHelper.startIntent(requireActivity(), SettingsActivity.PermissionStatus.class);
            case SettingsConstants.PREF_GRANT_ALL_PERMISSIONS -> {
                value = sp.getString(SettingsConstants.PREF_PRIVILEGE_PROVIDER, getString(R.string.pref_privilege_provider_default));
                new Thread(() -> {
                    PrivilegeProvider.requestAllPermissions(requireActivity(), PrivilegeProvider.privilegeToInt(value));
                    requireActivity().runOnUiThread(() -> {
                        pref.setSummary(R.string.pref_operation_completed);
                        new Handler().postDelayed(() -> pref.setSummary(""), 2000);
                    });

                }).start();

            }
            case SettingsConstants.PREF_DEACTIVATE_DEVICE_OWNER -> {
                DevicePolicyManager dpm = getSystemService(requireContext(), DevicePolicyManager.class);
                if (dpm == null) break;
                dpm.clearDeviceOwnerApp(requireActivity().getPackageName());

                Preference p;
                Preference p2;
                if ((p = findPreference(SettingsConstants.PREF_CHECK_DEVICE_ADMIN)) == null ||
                        (p2 = findPreference(SettingsConstants.PREF_ENABLE_DHIZUKU)) == null) break;
                p.setSummary(R.string.pref_wait);
                p.setEnabled(false);
                p2.setEnabled(false);
                pref.setEnabled(false);

                new Handler().postDelayed(() -> {
                    prefSetup(p);
                    pref.setEnabled(true);
                    p2.setEnabled(true);
                }, 1000);
            }
            case SettingsConstants.PREF_GALLERY_ACCESS ->
                    gallery_saf.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
            case SettingsConstants.PREF_MUSIC_ACCESS_SAF ->
                    music_saf.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
            case SettingsConstants.PREF_TEXT_STROKE_WIDTH -> {
                View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_stroke_text, null);
                StrokeTextView preview = view.findViewById(R.id.strokeTextPreview);
                SeekBar bar = view.findViewById(R.id.strokeSeekBar);

                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
                builder.setTitle(R.string.pref_text_stroke_width)
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> sp.edit().putInt(key, bar.getProgress()).apply())
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();

                bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        preview.setStrokeWidth(i);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                bar.setProgress(sp.getInt(key, 3));
            }
            case SettingsConstants.PREF_MAIN_UI_HEIGHT_SCALE -> requireActivity().startActivity(
                    UIHelper.makeIntent(requireActivity(), MainActivity.class)
                            .putExtra(MainActivity.EXTRA_PREVIEW, true)
            );
            case SettingsConstants.PREF_EXIT_FAKEUI_CONFIG_KEY -> {
                if (UIHelper.checkExitMethod(requireContext(), UIHelper.EXIT_METHOD_DPAD)) {
                    View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.preference_dialog_edittext, null, false);
                    dialogView.findViewById(android.R.id.edit).setVisibility(View.GONE);
                    TextView tv = dialogView.findViewById(android.R.id.message);
                    tv.setText(R.string.dialog_message_exit_dpad);
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
                    StringBuilder sb = new StringBuilder();
                    builder.setTitle(R.string.dialog_title_exit_dpad)
                            .setView(dialogView)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> sp.edit().putString(key, sb.toString()).apply())
                            .setNegativeButton(android.R.string.cancel, null)
                            .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                final int keyLimit = 10;
                                int recordedKeys = 0;

                                @Override
                                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                                    if (keyEvent.getAction() != KeyEvent.ACTION_UP) return true;
                                    if (recordedKeys >= keyLimit || keyCode < 19 || keyCode > 22) return false;
                                    CharSequence tvText = tv.getText().equals(getString(R.string.dialog_message_exit_dpad))? "" : tv.getText();
                                    StringBuilder textViewSB = new StringBuilder(tvText);
                                    if (sb.length() > 0) {//Fuck that shit, sb.isEmpty() requires api 35?!?!
                                        sb.append(",");
                                        textViewSB.append(", ");
                                    }
                                    sb.append(keyCode);
                                    textViewSB.append(switch (keyCode) {
                                        case KeyEvent.KEYCODE_DPAD_UP -> getString(R.string.up);
                                        case KeyEvent.KEYCODE_DPAD_DOWN -> getString(R.string.down);
                                        case KeyEvent.KEYCODE_DPAD_LEFT -> getString(R.string.left);
                                        case KeyEvent.KEYCODE_DPAD_RIGHT -> getString(R.string.right);
                                        default -> "unknown";
                                    });
                                    tv.setText(textViewSB.toString());
                                    if (recordedKeys == 0) tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                                    recordedKeys++;
                                    return true;
                                }
                            })
                            .create().show();
                }
            }
        }
        return false;
    }
}