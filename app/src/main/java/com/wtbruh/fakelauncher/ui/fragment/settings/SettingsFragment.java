package com.wtbruh.fakelauncher.ui.fragment.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SettingsActivity;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    public final static String PAGE_PERMISSION = "permission";
    public final static String PAGE_VIEW = "view";
    public final static String PAGE_BEHAVIOUR = "behaviour";


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_index, rootKey);
        String[] pages = {
                PAGE_PERMISSION,
                PAGE_VIEW,
                PAGE_BEHAVIOUR
        };
        Preference pref;
        for (String page : pages) {
            if ((pref = findPreference(page)) != null) pref.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        SettingsActivity activity = (SettingsActivity) getActivity();
        if (activity != null) activity.openSubSettings(preference.getKey());
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        SettingsActivity activity = (SettingsActivity)getActivity();
        if (activity != null) activity.setToolbarTitle(R.string.app_settings_name);
    }
}
