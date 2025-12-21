package com.wtbruh.fakelauncher.ui.fragment.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SettingsActivity;

public class AboutFragment extends Fragment {

    public final static String GITHUB_REPO_URL = "https://github.com/ZH-XiJun/fakelauncher";
    private View rootView;
    private final static String TAG = AboutFragment.class.getSimpleName();

    public static AboutFragment newInstance() { return new AboutFragment(); }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_about, container, false);
        init();
        return rootView;
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        ((SettingsActivity) requireActivity()).setToolbarTitle(R.string.pref_page_about);

        String versionName;
        String versionCode;
        try {
            PackageInfo packageInfo = requireContext().getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = String.valueOf(packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error on initializing About page: "+e);
            versionName = "unknown";
            versionCode = "unknown";
        }
        TextView appVersion = rootView.findViewById(R.id.appVersion);
        appVersion.setText(versionName + " (" + versionCode + ")");
        Button jumpToGithub = rootView.findViewById(R.id.jumpToGithub);
        jumpToGithub.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL));
            startActivity(intent);
        });
        TextView debugVariant = rootView.findViewById(R.id.debugVariant);
        if ((requireContext().getApplicationInfo().flags &
                ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            debugVariant.setVisibility(View.VISIBLE);
        }
    }
}
