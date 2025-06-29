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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.wtbruh.fakelauncher.ui.settings.SettingsFragment;
import com.wtbruh.fakelauncher.ui.settings.SubSettingsFragment;
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
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    public void openSubSettings(String page) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.settings_container, SubSettingsFragment.newInstance(page));
        ft.addToBackStack(null);
        ft.commit();
    }

    public void setToolbarTitle(int resId) {
        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.setTitle(resId);
        else Log.e(TAG, "Set title failed, got null ActionBar!!!");
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
                        data = permissionGrantStatus(PrivilegeProvider.getAllPermissions(this));
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
            data = permissionGrantStatus(PrivilegeProvider.getAllPermissions(this));
            adapter = new SimpleAdapter(this,
                    data,
                    R.layout.listview_item,
                    new String[] {"Item", "subItem"},
                    new int[] {R.id.Item, R.id.subItem});
            listView.setAdapter(adapter);
            itemClick(listView);

        }

        private ArrayList<HashMap<String, String>> permissionGrantStatus(String[] permissions) {
            ArrayList<HashMap<String, String>> mylist = new ArrayList<>();

            for(String permission : permissions)
            {
                HashMap<String, String> map = new HashMap<>();
                map.put("Item", permission);
                if (PrivilegeProvider.checkPermission(PermissionStatus.this, permission)) {
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
                if (! PrivilegeProvider.checkPermission(PermissionStatus.this, permission)) {
                    PrivilegeProvider.requestPermission(PermissionStatus.this, permission);
                    Toast.makeText(PermissionStatus.this, R.string.toast_reopen_to_refresh, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}