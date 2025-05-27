package com.wtbruh.fakelauncher;

import static com.wtbruh.fakelauncher.utils.PrivilegeProvider.PERMISSION_REQUEST_CODE;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import com.wtbruh.fakelauncher.ui.SettingsFragment;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

import java.util.ArrayList;
import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private final static String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment(SettingsActivity.this))
                .commit();
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
                    map.put("subItem", getResources().getString(R.string.pref_check_privilege_denied));
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