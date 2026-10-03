package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.SingleTextviewAdapter;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具箱：二级菜单，显示用户在设置中勾选的已安装应用。
 * 风格与其余老人机界面一致（黑底白字，方向键 + 确认键操作）。
 */
public class ToolboxFragment extends BaseFragment {

    private static final String TAG = ToolboxFragment.class.getSimpleName();
    private RecyclerView menuView;
    private SingleTextviewAdapter adapter;
    private List<String> appNames = new ArrayList<>();
    private List<String> appPkgs = new ArrayList<>();

    public ToolboxFragment() {
        // Required empty public constructor
    }

    public static ToolboxFragment newInstance() {
        return new ToolboxFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_toolbox, container, false);
        init();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadApps();
    }

    private void init() {
        menuView = rootView.findViewById(R.id.toolbox_list);
        menuView.setLayoutManager(new LinearLayoutManager(requireContext()));
        menuView.setFocusable(false);
        menuView.setItemAnimator(null);
        // 功能机风格：禁触（与短信/相册/音乐列表一致）
        menuView.setOnTouchListener((v, e) -> true);
        reloadApps();
        setFooterBar(L_DEFAULT, R_DEFAULT);
    }

    private void reloadApps() {
        appNames.clear();
        appPkgs.clear();
        for (android.os.Bundle b : UIHelper.getToolboxAppList(requireContext())) {
            appNames.add(b.getString(UIHelper.EXT_APP_LABEL));
            appPkgs.add(b.getString(UIHelper.EXT_APP_PKG));
        }
        // 无论是否添加应用，都显示列表界面；未添加时列表为空（无提示文字）
        rootView.findViewById(R.id.toolbox_textHint).setVisibility(View.GONE);
        menuView.setVisibility(View.VISIBLE);
        adapter = new SingleTextviewAdapter(appNames);
        menuView.setAdapter(adapter);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        android.util.Log.d("ToolboxFragment", "KEY onKeyUp code=" + keyCode
                + " adapterNull=" + (adapter == null) + " itemCount=" + (adapter == null ? -1 : adapter.getItemCount())
                + " pos=" + (adapter == null ? -1 : adapter.getSelectedPosition())
                + " pkgs=" + appPkgs.size());
        // 返回：右菜单键 / 返回键 均返回上级（与其余功能机页面一致）
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK) {
            if (requireActivity() != null) requireActivity().onBackPressed();
            return true;
        }
        if (adapter == null || adapter.getItemCount() == 0) {
            return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
        }
        int position = adapter.getSelectedPosition();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (position > 0) {
                    menuView.scrollToPosition(position - 1);
                    adapter.setSelectedPosition(position - 1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (position < adapter.getItemCount() - 1) {
                    menuView.scrollToPosition(position + 1);
                    adapter.setSelectedPosition(position + 1);
                }
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (position >= 0 && position < appPkgs.size()) {
                    UIHelper.launchExternalApp(requireContext(), appPkgs.get(position));
                }
                return true;
            default:
                return false;
        }
    }
}
