package com.wtbruh.fakelauncher.ui.fragment.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.wtbruh.fakelauncher.BuildConfig;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SettingsActivity;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    public final static String FUN = "fun";
    public final static String PAGE = "page";

    public final static String PAGE_PERMISSION = "page_permission";
    public final static String PAGE_VIEW = "page_view";
    public final static String PAGE_BEHAVIOUR = "page_behaviour";
    public final static String PAGE_DEBUG = "page_debug";
    public final static String PAGE_ABOUT = "page_about";
    public final static String PAGE_HOTKEY = "page_hotkey";
    public final static String FUN_OPEN_FAKEUI = "fun_fakeui";
    public final static String FUN_TOUCH = "fun_touch";
    public final static String FUN_SHORTCUT_DESKTOP = "fun_shortcut_desktop";
    public final static String FUN_HOTKEY = "fun_hotkey";


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_index, rootKey);
        String[] pages = {
                FUN_OPEN_FAKEUI,
                FUN_TOUCH,
                FUN_SHORTCUT_DESKTOP,
                FUN_HOTKEY,
                PAGE_PERMISSION,
                PAGE_VIEW,
                PAGE_BEHAVIOUR,
                PAGE_DEBUG,
                PAGE_ABOUT
        };
        Preference pref = findPreference(FUN_TOUCH);
        if (pref != null) pref.setVisible(!UIHelper.getTouchscreenState(requireContext()));
        pref = findPreference(PAGE_DEBUG);
        if (pref != null) pref.setVisible(BuildConfig.DEBUG);
        for (String page : pages) {
            if ((pref = findPreference(page)) != null) pref.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (FUN_SHORTCUT_DESKTOP.equals(preference.getKey())) {
            addDesktopShortcut();
            return true;
        }
        if (FUN_HOTKEY.equals(preference.getKey())) {
            SettingsActivity activity = (SettingsActivity) requireActivity();
            activity.openSubSettings(PAGE_HOTKEY);
            return true;
        }
        SettingsActivity activity = (SettingsActivity) requireActivity();
        activity.openSubSettings(preference.getKey());
        return false;
    }

    /** 在系统桌面创建“启动伪装”快捷方式（requestPinShortcut + 回调反馈） */
    private void addDesktopShortcut() {
        try {
            android.content.pm.ShortcutManager sm =
                    ContextCompat.getSystemService(requireContext(), android.content.pm.ShortcutManager.class);
            if (sm == null) {
                Toast.makeText(requireContext(), R.string.pref_shortcut_desktop_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!sm.isRequestPinShortcutSupported()) {
                Toast.makeText(requireContext(), R.string.pref_shortcut_not_supported, Toast.LENGTH_LONG).show();
                return;
            }
            android.content.pm.ShortcutInfo si = new android.content.pm.ShortcutInfo.Builder(
                    requireContext(), "launch_fakeui_shortcut")
                    .setIntent(new Intent(requireContext(), com.wtbruh.fakelauncher.SplashActivity.class)
                            .setAction(Intent.ACTION_MAIN)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    .setShortLabel(getString(R.string.shortcut_name))
                    .setLongLabel(getString(R.string.shortcut_name))
                    .setIcon(android.graphics.drawable.Icon.createWithResource(
                            requireContext(), R.mipmap.ic_launcher))
                    .build();
            sm.addDynamicShortcuts(java.util.Collections.singletonList(si));
            // 带回调：用户确认后 PinShortcutReceiver 会收到广播，反馈添加结果
            Intent cb = new Intent(requireContext(), com.wtbruh.fakelauncher.receiver.PinShortcutReceiver.class);
            android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(requireContext(), 0, cb,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            sm.requestPinShortcut(si, pi.getIntentSender());
            Toast.makeText(requireContext(), R.string.pref_shortcut_desktop_confirm, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("FakeUI-Shortcut", "addDesktopShortcut failed", e);
            Toast.makeText(requireContext(), R.string.pref_shortcut_desktop_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SettingsActivity activity = (SettingsActivity)getActivity();
        if (activity != null) activity.setToolbarTitle(R.string.app_settings_name);
    }
}
