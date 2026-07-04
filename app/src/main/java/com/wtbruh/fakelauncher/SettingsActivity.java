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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.ui.fragment.settings.AboutFragment;
import com.wtbruh.fakelauncher.ui.fragment.settings.SettingsFragment;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.ui.view.DualTextviewAdapter;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

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
        if (page.contains(SettingsFragment.FUN)) {
            switch (page) {
                case SettingsFragment.FUN_OPEN_FAKEUI -> UIHelper.intentStarter(this, SplashActivity.class);
                case SettingsFragment.FUN_TOUCH -> UIHelper.setTouchscreenState(true, this);
            }
            return;
        }
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

        RecyclerView recyclerView;
        DualTextviewAdapter adapter;
        ArrayList<Bundle> data;
        int[] clickedPosition;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_permission_status);
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
                    data.set(clickedPosition[0], permissionGrantStatus(permissions[0]));
                    adapter.notifyItemChanged(clickedPosition[0]);
                }
            }
        }
        private void init() {
            recyclerView = findViewById(R.id.permissions_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            data = permissionGrantStatus(PrivilegeProvider.getAllPermissions(this));
            adapter = new DualTextviewAdapter(data);
            recyclerView.setAdapter(adapter);
            adapter.setOnItemClickListener((position, itemTv, subItemTv) -> {
                String permission = itemTv.getText().toString();
                Log.d(TAG, "Clicked: "+permission);
                clickedPosition = new int[]{position};
                if (! PrivilegeProvider.checkPermission(PermissionStatus.this, permission)) {
                    PrivilegeProvider.requestPermission(PermissionStatus.this, permission);
                }
            });

        }

        private ArrayList<Bundle> permissionGrantStatus(String[] permissions) {
            ArrayList<Bundle> list = new ArrayList<>();

            for(String permission : permissions)
            {
                list.add(permissionGrantStatus(permission));
            }
            return list;
        }

        private Bundle permissionGrantStatus(String permission) {
            Bundle b = new Bundle();
            b.putString(DualTextviewAdapter.ITEM, permission);
            if (PrivilegeProvider.checkPermission(PermissionStatus.this, permission)) {
                b.putString(DualTextviewAdapter.SUB_ITEM, getResources().getString(R.string.pref_check_privilege_granted));
            } else {
                b.putString(DualTextviewAdapter.SUB_ITEM, getResources().getString(R.string.pref_check_privilege_denied));
            }
            return b;
        }
    }
}