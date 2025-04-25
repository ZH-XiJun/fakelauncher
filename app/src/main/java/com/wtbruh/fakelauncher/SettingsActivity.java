package com.wtbruh.fakelauncher;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import static com.wtbruh.fakelauncher.utils.PrivilegeProvider.PERMISSION_REQUEST_CODE;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener {

    private final static String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        initPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the listener that observe changes of SharedPreference
        // 注册检测SharedPreference变化的监听器
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener that observe changes of SharedPreference
        // 注销检测SharedPreference变化的监听器
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Init necessary items<br>
     * 对必要的选项进行初始化
     */
    private void initPreferences() {
        Preference pref;
        SharedPreferences defaultPref = getDefaultSharedPreferences(SettingsActivity.this);
        // Init of clickable preferences
        String[] clickablePrefs = getResources().getStringArray(R.array.clickable_prefs);
        for (String key : clickablePrefs) {
            pref = findPreference(key);
            pref.setOnPreferenceClickListener(this);
        }
        // Init of pref "privilege_provider"
        pref = findPreference("privilege_provider");
        pref.setSummary(defaultPref.getString("privilege_provider", "None"));
        // Init of pref "check_xposed"
        pref = findPreference("check_xposed");
        if (MainActivity.isXposedModuleActivated()) {
            pref.setSummary(R.string.pref_xposed_activated);
        } else {
            pref.setSummary(R.string.pref_xposed_not_activated);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) return;
        Log.d(TAG, "Shared preference changed! key:"+key);
        Preference pref = findPreference(key);
        switch (key) {
            case "privilege_provider":
                pref.setSummary(sharedPreferences.getString(key, "None"));
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
                boolean result = PrivilegeProvider.checkDeviceAdmin(dhizuku, SettingsActivity.this);
                if (! result) {
                    pref.setSummary(R.string.pref_check_privilege_not_granted);
                } else if (dhizuku){
                    pref.setSummary(R.string.pref_check_privilege_granted_dhizuku);
                } else {
                    pref.setSummary(R.string.pref_check_privilege_granted);
                }
                break;
            case "permission_grant_status":
                UIHelper.intentStarter(SettingsActivity.this, PermissionStatus.class);
                break;
        }
        return false;
    }

    public static class PermissionStatus extends AppCompatActivity {

        ListView listView;
        SimpleAdapter adapter;
        ArrayList<HashMap<String, String>> data;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_listview);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
            init();
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                               int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            switch (requestCode) {
                case PERMISSION_REQUEST_CODE:
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "perm123");
                        data = permissionGrantStatus(getPermissions());
                        adapter.notifyDataSetInvalidated();
                    }  else {
                        // Explain to the user that the feature is unavailable because
                        // the feature requires a permission that the user has denied.
                        // At the same time, respect the user's decision. Don't link to
                        // system settings in an effort to convince the user to change
                        // their decision.
                    }
                    return;
            }
            // Other 'case' lines to check for other
            // permissions this app might request.
        }
        private void init() {
            listView = findViewById(R.id.permissions_list);
            data = permissionGrantStatus(getPermissions());
            adapter = new SimpleAdapter(this,
                    data,
                    R.layout.activity_listview_item,
                    new String[] {"Item", "subItem"},
                    new int[] {R.id.Item, R.id.subItem});
            listView.setAdapter(adapter);
            itemClick(listView);
        }
        private String[] getPermissions() {
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
                String[] raw = packageInfo.requestedPermissions;

                if (raw == null) return new String[]{};

                ArrayList<String> arrayList = new ArrayList<>();
                for (String permission: raw) {
                    if (permission.contains("android.permission.")) {
                        arrayList.add(permission);
                    }
                }

                return arrayList.toArray(new String[0]);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Error on getting permissions: "+e);
                return new String[]{};
            }
        }

        private ArrayList<HashMap<String, String>> permissionGrantStatus(String[] permissions) {
            ArrayList<HashMap<String, String>> mylist = new ArrayList<>();

            for(String permission : permissions)
            {
                HashMap<String, String> map = new HashMap<>();
                map.put("Item", permission);
                if (PrivilegeProvider.CheckPermission(PermissionStatus.this, permission)) {
                    map.put("subItem", getResources().getString(R.string.pref_check_privilege_granted));
                } else {
                    map.put("subItem", getResources().getString(R.string.pref_check_privilege_not_granted));
                }
                mylist.add(map);
            }
            return mylist;
        }

        private void itemClick(ListView listView) {
            listView.setOnItemClickListener((parent, view, position, id) -> {
                TextView textView = view.findViewById(R.id.Item);
                String permission = textView.getText().toString();
                Log.d(TAG, "Clicked: "+permission);
                if (! PrivilegeProvider.CheckPermission(PermissionStatus.this, permission)) {
                    PrivilegeProvider.requestPermission(PermissionStatus.this, permission);
                    Toast.makeText(PermissionStatus.this, R.string.toast_reopen_to_refresh, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}