package com.wtbruh.fakelauncher;

import static com.wtbruh.fakelauncher.utils.PrivilegeProvider.PERMISSION_REQUEST_CODE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.ui.fragment.settings.AboutFragment;
import com.wtbruh.fakelauncher.ui.fragment.settings.SettingsFragment;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.ui.view.DualTextviewAdapter;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    private final static String TAG = SettingsActivity.class.getSimpleName();

    private static final String PREF_CHANGELOG_SHOWN = "changelog_shown_v0631";

    private AlertDialog changelogDialog; // 当前显示的更新日志弹窗（防止重复弹出）
    private long lastBackPressedAt = 0;  // 上一次返回键时间（双击退出）

    /** 首次安装后弹出的更新日志（对比最初版 v0.4），HTML 排版：块标题加粗加红、条目标题加粗 */
    private static final String CHANGELOG_HTML =
            "<b><font color=\"#CC0000\">⚠️ 重要提醒<br>" +
            "本软件为开源项目二次修改版，非官方正式发行版本<br>" +
            "仅供个人交流学习使用，请勿用于非法用途<br>" +
            "点击「确定」即表示您已完全知晓并同意以上约定（下滑显示「确定」按钮）</font></b><br><br>" +
            "<b>特别感谢：“我永远喜欢kemonomimigirl”大佬的技术支持！</b><br><br>" +
            "<b><font color=\"#CC0000\">使用前提（必要软件）</font></b><br>" +
            "<b>1. LSPatch（0.5 及以上可用，建议 0.6）：伪装模块注入运行的前提</b><br>" +
            "<b>2. Shizuku（13.0 及以上可用，建议 13.5.3）：系统级权限（电源键拦截、真实熄屏、系统指令）；无线调试免电脑启动需 Android 11 及以上，Android 11 以下需连电脑开 Shizuku</b><br>" +
            "<b>3. Dhizuku（2.0 及以上可用，建议 2.10）：设备所有者授权（伪装锁定任务必需）</b><br>" +
            "<br>" +
            "<b><font color=\"#CC0000\">一、新增功能</font></b><br>" +
            "<b>1. 真实拨打电话</b>：现在可以在伪装界面拨打电话<br>" +
            "<b>2. 联系人</b>：读取并显示真实通讯录，支持直接拨打<br>" +
            "<b>3. 来电显示</b>：呼入自动亮屏，显示号码/联系人姓名，支持铃声与振动<br>" +
            "<b>4. 短信</b>：支持短信查看与发送，同步近三天短信<br>" +
            "<b>5. 自定义伪装主菜单</b>：自由选择已安装应用显示在伪装主菜单<br>" +
            "<b>6. 工具箱</b>：伪装二级菜单，可增删、排序、重命名安卓应用<br>" +
            "<b>7. 长按电源键弹伪装关机/重启界面</b>：依然真实执行关机、重启，只是换了个符合风格的界面<br>" +
            "<b>8. 启动快捷键</b>：按键+方式（长按/连按）一键启动伪装<br>" +
            "<b>9. 添加启动伪装快捷方式到桌面</b>：桌面快捷方式一键启动伪装<br>" +
            "<b>10. 伪装设置二级菜单（密码 0000）</b>：情景模式、音量调节、亮度调节、熄屏设置<br>" +
            "<b>11. 音乐</b>：新增播放列表、循环模式、定时关闭，默认列表循环<br>" +
            "<br>" +
            "<b><font color=\"#CC0000\">二、界面修改</font></b><br>" +
            "<b>1. 桌面状态栏</b>：真实信号（双卡双信号）、渐变电量电池、时间日期调整<br>" +
            "2. 主菜单图标统一功能机风格<br>" +
            "3. 拨号/来电/通话中界面统一为功能机风格<br>" +
            "4. 联系人列表两行显示（名称+号码），支持反向选择<br>" +
            "<b>5. 音乐播放界面</b>：中央播放/暂停按钮、时长、定时图标<br>" +
            "<b>6. 相机/相册</b>：选项文字精简、图片循环跳转<br>" +
            "<br>" +
            "<b><font color=\"#CC0000\">三、问题修复</font></b><br>" +
            "1. 电源键唤醒失效（Shizuku 进程被杀问题）<br>" +
            "2. 接听/挂断不生效、免提切换黑屏<br>" +
            "3. 亮度/音量调节保存后返回异常<br>" +
            "4. 工具箱改名应用启动异常<br>" +
            "5. 快捷键启动后误弹音量界面<br>" +
            "6. 音乐定时界面误弹系统音量<br>" +
            "7. 退出伪装音乐残留<br>" +
            "8. 音乐界面布局抖动/按钮错位<br>" +
            "<br>" +
            "建议使用 Android 8.0 ~ Android 15（已在 Android 13 以晴实机测试）<br><br>" +
            "PS：本软件是修改作者为了方便自己使用以晴手机而改，其他机型很可能不适配，而且大概率不会继续更新，望周知。";

    @Override
    protected void onResume() {
        super.onResume();
        // 设置页是正常触屏界面：进入时恢复触屏（若增强触屏拦截开启）
        com.wtbruh.fakelauncher.utils.UIHelper.setTouchscreenState(true, this);
    }

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
        showChangelogIfFirstRun();
    }

    /** singleTop 复用栈顶实例时也会收到新 Intent：同样检查是否要弹更新日志 */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showChangelogIfFirstRun();
    }

    /** 返回键：有子页面先返回子页面；主页双击退出（第一次提示，2 秒内再按才退出） */
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastBackPressedAt <= 2000) {
            lastBackPressedAt = 0;
            super.onBackPressed();
        } else {
            lastBackPressedAt = now;
            Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
        }
    }

    /** 首次安装后（标记不存在时）弹出更新日志。只有点过「确定」才写标记，没点则每次打开都弹 */
    private void showChangelogIfFirstRun() {
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_CHANGELOG_SHOWN, false)) return;
        if (changelogDialog != null && changelogDialog.isShowing()) return; // 弹窗已在显示，不重复弹
        showChangelogDialog();
    }

    /**
     * 显示更新日志弹窗（含免责声明）。
     * 确定按钮黑色；初始隐藏，滚动接近底部时才出现；不允许返回键关闭；
     * 点击确定才写入「已确认」标记并关闭。
     */
    public void showChangelogDialog() {
        float density = getResources().getDisplayMetrics().density;

        // 内容区：可滚动 HTML 文本 + 底部居中确定按钮
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding((int) (density * 16), (int) (density * 4),
                (int) (density * 16), (int) (density * 10));

        TextView tv = new TextView(this);
        tv.setText(Html.fromHtml(CHANGELOG_HTML, Html.FROM_HTML_MODE_LEGACY));
        tv.setTextSize(14);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final Button btn = new Button(this);
        btn.setText("确定");
        btn.setTextSize(18);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(0xFF000000);
        btn.setVisibility(View.GONE);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                (int) (density * 90), (int) (density * 52));
        blp.gravity = Gravity.CENTER_HORIZONTAL;
        blp.topMargin = (int) (density * 8);
        btn.setLayoutParams(blp);
        GradientDrawable gd = new GradientDrawable();
        gd.setStroke((int) (density * 2), 0xFF000000);
        gd.setCornerRadius(density * 8);
        gd.setColor(0xFFFFFFFF);
        btn.setBackground(gd);

        ScrollView scroll = new ScrollView(this) {
            private boolean btnShown = false;

            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);
                if (btnShown) return; // 显示后锁定，避免反复出现导致弹窗抖动
                View child = getChildAt(0);
                if (child == null || child.getHeight() <= 0) return; // 布局未完成
                int maxScroll = Math.max(0, child.getHeight() - getHeight());
                // 滚动超过 80%（快到底部）或内容无需滚动时，显示确定按钮
                if (maxScroll == 0 || t >= (int) (maxScroll * 0.8f)) {
                    btnShown = true;
                    btn.setVisibility(View.VISIBLE);
                }
            }
        };
        scroll.addView(tv);
        container.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        container.addView(btn);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("更新日志 v0.6.3.1（对比官方版本 v0.4）")
                .setView(container)
                .create();
        dialog.setCancelable(false); // 不允许返回键关闭
        changelogDialog = dialog;
        dialog.setOnDismissListener(d -> changelogDialog = null);
        btn.setOnClickListener(v -> {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putBoolean(PREF_CHANGELOG_SHOWN, true).apply();
            dialog.dismiss();
        });
        dialog.show();
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
                case SettingsFragment.FUN_OPEN_FAKEUI -> UIHelper.startIntent(this, SplashActivity.class);
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
                b.putString(DualTextviewAdapter.SUB_ITEM, getResources().getString(R.string.pref_permissions_granted));
            } else {
                b.putString(DualTextviewAdapter.SUB_ITEM, getResources().getString(R.string.pref_permissions_denied));
            }
            return b;
        }
    }
}