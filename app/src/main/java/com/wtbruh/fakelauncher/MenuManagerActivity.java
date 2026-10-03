package com.wtbruh.fakelauncher;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.ui.BaseAppCompatActivity;
import com.wtbruh.fakelauncher.ui.view.SingleTextviewAdapter;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 菜单管理（老人机风格）：
 * 选择已安装的应用加入“主菜单”或“工具箱”二级菜单。
 * 方向键上下选择，确认键加入/移除，返回键保存并退出。
 */
public class MenuManagerActivity extends BaseAppCompatActivity {

    private static final int MODE_MAIN = 0;
    private static final int MODE_TOOLBOX = 1;
    private static final int MODE_PREVIEW = 2;
    private static final int MODE_COUNT = 3;

    private int mode = MODE_MAIN;
    private TextView modeMainBtn;
    private TextView modeToolboxBtn;
    private TextView modePreviewBtn;
    private RecyclerView appList;
    private AppAdapter adapter;
    private SingleTextviewAdapter previewAdapter;

    private final List<android.os.Bundle> apps = new ArrayList<>();
    private LinkedHashSet<String> mainSelection = new LinkedHashSet<>();
    private LinkedHashSet<String> toolboxSelection = new LinkedHashSet<>();

    // 预览模式数据：工具箱有序应用
    private final List<String> previewPkgs = new ArrayList<>();
    private final List<String> previewOrig = new ArrayList<>();
    private final List<String> previewShown = new ArrayList<>();

    // 预览模式：长按拖拽排序
    private androidx.recyclerview.widget.ItemTouchHelper previewDragHelper;

    @Override
    protected void onResume() {
        super.onResume();
        // 菜单管理属于设置类界面：触屏可用
        UIHelper.setTouchscreenState(true, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu_manager);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu_manager_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        modeMainBtn = findViewById(R.id.mm_mode_main);
        modeToolboxBtn = findViewById(R.id.mm_mode_toolbox);
        modePreviewBtn = findViewById(R.id.mm_mode_preview);
        // 触屏：点击顶部标签切换子页面
        modeMainBtn.setOnClickListener(v -> { mode = MODE_MAIN; updateModeUi(); });
        modeToolboxBtn.setOnClickListener(v -> { mode = MODE_TOOLBOX; updateModeUi(); });
        modePreviewBtn.setOnClickListener(v -> { mode = MODE_PREVIEW; updateModeUi(); });
        appList = findViewById(R.id.mm_app_list);
        appList.setLayoutManager(new LinearLayoutManager(this));
        appList.setFocusable(false);
        appList.setItemAnimator(null);

        // 预览模式拖拽排序：长按条目拖动（由 previewAdapter 长按回调触发 startDrag）
        previewDragHelper = new androidx.recyclerview.widget.ItemTouchHelper(
                new androidx.recyclerview.widget.ItemTouchHelper.Callback() {
                    @Override
                    public boolean isLongPressDragEnabled() {
                        return false;
                    }
                    @Override
                    public boolean isItemViewSwipeEnabled() {
                        return false;
                    }
                    @Override
                    public int getMovementFlags(@NonNull RecyclerView rv,
                                                @NonNull RecyclerView.ViewHolder vh) {
                        return makeMovementFlags(
                                androidx.recyclerview.widget.ItemTouchHelper.UP
                                        | androidx.recyclerview.widget.ItemTouchHelper.DOWN, 0);
                    }
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder src,
                                          @NonNull RecyclerView.ViewHolder target) {
                        int from = src.getAdapterPosition();
                        int to = target.getAdapterPosition();
                        if (from < 0 || to < 0 || from >= previewPkgs.size() || to >= previewPkgs.size()) {
                            return false;
                        }
                        java.util.Collections.swap(previewPkgs, from, to);
                        java.util.Collections.swap(previewOrig, from, to);
                        java.util.Collections.swap(previewShown, from, to);
                        UIHelper.saveToolboxOrder(MenuManagerActivity.this, previewPkgs);
                        previewAdapter.notifyItemMoved(from, to);
                        previewAdapter.setSelectedPosition(to);
                        return true;
                    }
                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) { }
                });
        previewDragHelper.attachToRecyclerView(appList);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mainSelection = UIHelper.loadAppSelection(this, SettingsConstants.PREF_MENU_EXTERNAL_APPS);
        toolboxSelection = UIHelper.loadAppSelection(this, SettingsConstants.PREF_TOOLBOX_EXTERNAL_APPS);

        apps.addAll(UIHelper.getInstalledLauncherApps(this));
        adapter = new AppAdapter();
        appList.setAdapter(adapter);

        updateModeUi();
    }

    private void updateModeUi() {
        boolean main = mode == MODE_MAIN;
        boolean toolbox = mode == MODE_TOOLBOX;
        boolean preview = mode == MODE_PREVIEW;
        modeMainBtn.setSelected(main);
        modeToolboxBtn.setSelected(toolbox);
        modePreviewBtn.setSelected(preview);
        modeMainBtn.setAlpha(main ? 1f : 0.45f);
        modeToolboxBtn.setAlpha(toolbox ? 1f : 0.45f);
        modePreviewBtn.setAlpha(preview ? 1f : 0.45f);
        if (preview) {
            // 进入预览前先持久化当前勾选，避免退出时旧选择集覆盖预览排序
            UIHelper.saveAppSelection(this, SettingsConstants.PREF_MENU_EXTERNAL_APPS,
                    new ArrayList<>(mainSelection));
            UIHelper.saveAppSelection(this, SettingsConstants.PREF_TOOLBOX_EXTERNAL_APPS,
                    new ArrayList<>(toolboxSelection));
            reloadPreview();
            if (previewAdapter == null) {
                previewAdapter = new SingleTextviewAdapter(previewShown);
                previewAdapter.setSelectedPosition(0);
                // 触屏：点击条目 = 与确定键一致唤出操作菜单；长按 = 自由拖动调整位置
                previewAdapter.setOnClickListener(v -> {
                    int pos = appList.getChildAdapterPosition(v);
                    if (pos < 0) pos = previewAdapter.getSelectedPosition();
                    previewAdapter.setSelectedPosition(pos);
                    showPreviewAction(pos);
                });
                previewAdapter.setOnLongClickListener(v -> {
                    int pos = appList.getChildAdapterPosition(v);
                    if (pos < 0) pos = previewAdapter.getSelectedPosition();
                    previewAdapter.setSelectedPosition(pos);
                    RecyclerView.ViewHolder h = appList.findViewHolderForAdapterPosition(pos);
                    if (h != null && previewDragHelper != null) {
                        previewDragHelper.startDrag(h);
                    }
                    return true;
                });
                appList.setAdapter(previewAdapter);
            } else {
                if (appList.getAdapter() != previewAdapter) {
                    appList.setAdapter(previewAdapter);
                }
                previewAdapter.notifyDataSetChanged();
            }
        } else {
            if (appList.getAdapter() != adapter) {
                appList.setAdapter(adapter);
            }
            adapter.notifyDataSetChanged();
        }
        setFooterText();
    }

    private void setFooterText() {
        TextView footer = findViewById(R.id.mm_footer);
        if (mode == MODE_PREVIEW) {
            footer.setText(getString(R.string.menu_manager_preview_footer));
        } else {
            footer.setText(getString(R.string.menu_manager_footer));
        }
    }

    /** 重新加载工具箱预览数据（新名（原名）） */
    private void reloadPreview() {
        previewPkgs.clear();
        previewOrig.clear();
        previewShown.clear();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String raw = sp.getString(SettingsConstants.PREF_TOOLBOX_EXTERNAL_APPS, "");
        android.content.pm.PackageManager pm = getPackageManager();
        for (String pkg : raw.split(";")) {
            if (pkg == null || pkg.isEmpty()) continue;
            String orig = pkg;
            try {
                orig = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (Exception ignored) { }
            String renamed = UIHelper.getToolboxDisplayName(this, pkg);
            previewPkgs.add(pkg);
            previewOrig.add(orig);
            previewShown.add(renamed == null || renamed.isEmpty() ? orig : renamed + "（" + orig + "）");
        }
    }

    /** 上移/下移 */
    private void movePreviewItem(int position, int dir) {
        int target = position + dir;
        if (position < 0 || position >= previewPkgs.size()) return;
        if (target < 0 || target >= previewPkgs.size()) return;
        java.util.Collections.swap(previewPkgs, position, target);
        java.util.Collections.swap(previewOrig, position, target);
        java.util.Collections.swap(previewShown, position, target);
        UIHelper.saveToolboxOrder(this, previewPkgs);
        previewAdapter.setSelectedPosition(target);
    }

    /** 预览模式：弹操作菜单 */
    private void showPreviewAction(int position) {
        if (position < 0 || position >= previewPkgs.size()) return;
        String pkg = previewPkgs.get(position);
        String orig = previewOrig.get(position);
        final String[] items = {
                getString(R.string.menu_manager_opt_rename),
                getString(R.string.menu_manager_opt_up),
                getString(R.string.menu_manager_opt_down)
        };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: showRenameDialog(pkg, orig); break;
                        case 1: movePreviewItem(position, -1); break;
                        case 2: movePreviewItem(position, 1); break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** 重命名对话框 */
    private void showRenameDialog(String pkg, String orig) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setSingleLine(true);
        et.setTextSize(20);
        String cur = UIHelper.getToolboxDisplayName(this, pkg);
        et.setText(cur == null ? "" : cur);
        et.setHint(R.string.menu_manager_rename_hint);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.menu_manager_rename_title)
                .setView(et)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = et.getText().toString().trim();
                    UIHelper.setToolboxDisplayName(this, pkg, name);
                    reloadPreview();
                    if (previewAdapter != null) previewAdapter.notifyDataSetChanged();
                    android.widget.Toast.makeText(this,
                            name.isEmpty() ? R.string.menu_manager_rename_removed : R.string.menu_manager_rename_done,
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.menu_manager_rename_clear, (d, w) -> {
                    UIHelper.setToolboxDisplayName(this, pkg, "");
                    reloadPreview();
                    if (previewAdapter != null) previewAdapter.notifyDataSetChanged();
                    android.widget.Toast.makeText(this, R.string.menu_manager_rename_removed,
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /** 切换勾选状态并持久化 */
    private void toggleApp(int position) {
        if (position < 0 || position >= apps.size()) return;
        String pkg = apps.get(position).getString(UIHelper.EXT_APP_PKG);
        if (pkg == null) return;
        LinkedHashSet<String> target = mode == MODE_MAIN ? mainSelection : toolboxSelection;
        if (!target.remove(pkg)) target.add(pkg);
        // 已勾选的应用直接从另一个菜单中移除（避免重复）
        if (mode == MODE_MAIN) {
            toolboxSelection.remove(pkg);
        } else {
            mainSelection.remove(pkg);
        }
        saveSelections();
        adapter.notifyItemChanged(position);
    }

    private void saveSelections() {
        UIHelper.saveAppSelection(this, SettingsConstants.PREF_MENU_EXTERNAL_APPS,
                new ArrayList<>(mainSelection));
        UIHelper.saveAppSelection(this, SettingsConstants.PREF_TOOLBOX_EXTERNAL_APPS,
                new ArrayList<>(toolboxSelection));
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mode == MODE_PREVIEW) {
            int pos = previewAdapter != null ? previewAdapter.getSelectedPosition() : 0;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (pos > 0) {
                        appList.scrollToPosition(pos - 1);
                        previewAdapter.setSelectedPosition(pos - 1);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (previewAdapter != null && pos < previewAdapter.getItemCount() - 1) {
                        appList.scrollToPosition(pos + 1);
                        previewAdapter.setSelectedPosition(pos + 1);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mode = (mode - 1 + MODE_COUNT) % MODE_COUNT;
                    updateModeUi();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mode = (mode + 1) % MODE_COUNT;
                    updateModeUi();
                    return true;
                case KeyEvent.KEYCODE_MENU:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    showPreviewAction(pos);
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    // 预览模式：主菜单照常保存；工具箱顺序已在 movePreviewItem 中保存，勿用旧选择集覆盖
                    UIHelper.saveAppSelection(this, SettingsConstants.PREF_MENU_EXTERNAL_APPS,
                            new ArrayList<>(mainSelection));
                    finish();
                    return true;
                default:
                    return false;
            }
        }
        int position = adapter != null ? adapter.getSelectedPosition() : 0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (position > 0) {
                    appList.scrollToPosition(position - 1);
                    adapter.setSelectedPosition(position - 1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (position < adapter.getItemCount() - 1) {
                    appList.scrollToPosition(position + 1);
                    adapter.setSelectedPosition(position + 1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mode = (mode - 1 + MODE_COUNT) % MODE_COUNT;
                updateModeUi();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mode = (mode + 1) % MODE_COUNT;
                updateModeUi();
                return true;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                toggleApp(position);
                return true;
            case KeyEvent.KEYCODE_BACK:
                saveSelections();
                finish();
                return true;
            default:
                return false;
        }
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {
        private int selected = 0;

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.menu_manager_item, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            android.os.Bundle b = apps.get(position);
            String pkg = b.getString(UIHelper.EXT_APP_PKG);
            holder.icon.setImageDrawable(UIHelper.getExternalAppIcon(holder.itemView.getContext(), pkg));
            holder.name.setText(b.getString(UIHelper.EXT_APP_LABEL));
            boolean inTarget = (mode == MODE_MAIN ? mainSelection : toolboxSelection).contains(pkg);
            boolean inOther = (mode == MODE_MAIN ? toolboxSelection : mainSelection).contains(pkg);
            holder.state.setText(inTarget ? holder.itemView.getContext().getString(R.string.menu_manager_added)
                    : inOther ? holder.itemView.getContext().getString(R.string.menu_manager_other)
                    : holder.itemView.getContext().getString(R.string.menu_manager_not_added));
            holder.itemView.setSelected(selected == position);
            holder.itemView.setAlpha(selected == position ? 1f : 0.8f);
            holder.itemView.setOnClickListener(v -> {
                selected = holder.getAdapterPosition();
                toggleApp(selected);
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        public int getSelectedPosition() {
            return selected;
        }

        public void setSelectedPosition(int pos) {
            selected = pos;
            notifyDataSetChanged();
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name;
            TextView state;

            Holder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.mm_item_icon);
                name = itemView.findViewById(R.id.mm_item_name);
                state = itemView.findViewById(R.id.mm_item_state);
            }
        }
    }
}
