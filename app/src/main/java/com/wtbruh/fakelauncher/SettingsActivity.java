package com.wtbruh.fakelauncher;

import static com.wtbruh.fakelauncher.utils.PrivilegeProvider.PERMISSION_REQUEST_CODE;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.wtbruh.fakelauncher.ui.fragment.settings.AboutFragment;
import com.wtbruh.fakelauncher.ui.fragment.settings.SettingsFragment;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
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
        init();
    }

    private void init() {
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    public void openSubSettings(String page) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(
                R.anim.slide_up, // 进入动画
                R.anim.fall_down, // 退出动画
                R.anim.slide_up, // 返回堆栈时的进入动画
                R.anim.fall_down // 返回堆栈时的退出动画
        );
        ft.replace(R.id.settings_container, (page.equals(SettingsFragment.PAGE_ABOUT))? AboutFragment.newInstance() : SubSettingsFragment.newInstance(page));
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
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == PERMISSION_REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    data = permissionGrantStatus(PrivilegeProvider.getAllPermissions(this));
                    adapter.notifyDataSetInvalidated();
                }
            }
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
            ArrayList<HashMap<String, String>> list = new ArrayList<>();

            for(String permission : permissions)
            {
                HashMap<String, String> map = new HashMap<>();
                map.put("Item", permission);
                if (PrivilegeProvider.checkPermission(PermissionStatus.this, permission)) {
                    map.put("subItem", getResources().getString(R.string.pref_check_privilege_granted));
                } else {
                    map.put("subItem", getResources().getString(R.string.pref_check_privilege_denied));
                }
                list.add(map);
            }
            return list;
        }

        private void itemClick(ListView listView) {
            listView.setOnItemClickListener((parent, view, position, id) -> {
                TextView textView = view.findViewById(R.id.Item);
                String permission = textView.getText().toString();
                Log.d(TAG, "Clicked: "+permission);
                if (! PrivilegeProvider.checkPermission(PermissionStatus.this, permission)) {
                    PrivilegeProvider.requestPermission(PermissionStatus.this, permission);
                    Toast.makeText(PermissionStatus.this, R.string.toast_reopen_to_refresh, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}