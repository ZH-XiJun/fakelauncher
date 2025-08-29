package com.wtbruh.fakelauncher.ui.fragment.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SettingsActivity;
import com.wtbruh.fakelauncher.utils.UIHelper;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    public final static String FUN = "fun";
    public final static String PAGE = "page";

    public final static String PAGE_PERMISSION = "page_permission";
    public final static String PAGE_VIEW = "page_view";
    public final static String PAGE_BEHAVIOUR = "page_behaviour";
    public final static String PAGE_ABOUT = "page_about";
    public final static String FUN_OPEN_FAKEUI = "fun_fakeui";
    public final static String FUN_TOUCH = "fun_touch";


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_index, rootKey);
        String[] pages = {
                FUN_OPEN_FAKEUI,
                FUN_TOUCH,
                PAGE_PERMISSION,
                PAGE_VIEW,
                PAGE_BEHAVIOUR,
                PAGE_ABOUT
        };
        Preference pref = findPreference(FUN_TOUCH);
        if (pref != null) pref.setVisible(!UIHelper.getTouchscreenState(requireContext()));
        for (String page : pages) {
            if ((pref = findPreference(page)) != null) pref.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        SettingsActivity activity = (SettingsActivity) requireActivity();
        activity.openSubSettings(preference.getKey());
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        SettingsActivity activity = (SettingsActivity)getActivity();
        if (activity != null) activity.setToolbarTitle(R.string.app_settings_name);
    }
}
