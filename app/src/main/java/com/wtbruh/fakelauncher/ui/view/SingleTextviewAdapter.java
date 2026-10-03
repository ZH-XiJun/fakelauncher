package com.wtbruh.fakelauncher.ui.view;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.R;

import java.util.HashMap;
import java.util.List;

public class SingleTextviewAdapter extends BaseAdapter{
    private final List<String> data;
    public HashMap<Integer, TextView> tvSet;
    private int scale = 1;
    private View.OnLongClickListener onLongClick;
    private View.OnClickListener onClick;
    /** 选中项滚动显示（跑马灯）：开启后所有项单行显示，未选中截断，选中项滚动完整文字 */
    private boolean marqueeSelected = false;
    public SingleTextviewAdapter(List<String> data) {
        this.data = data;
        tvSet = new HashMap<>();
    }

    /** 设置条目长按回调（可选；未设置时无长按行为） */
    public void setOnLongClickListener(View.OnLongClickListener l) {
        this.onLongClick = l;
    }

    /** 设置条目点击回调（可选；未设置时条目不可点击） */
    public void setOnClickListener(View.OnClickListener l) {
        this.onClick = l;
    }

    /** 开启/关闭选中项滚动显示（默认关闭，不影响其他列表） */
    public void setSelectedMarquee(boolean enabled) {
        this.marqueeSelected = enabled;
    }

    /** 交换两个位置的数据并刷新（音乐列表上移/下移排序用） */
    public void swapItems(int from, int to) {
        if (from < 0 || to < 0 || from >= data.size() || to >= data.size()) return;
        String tmp = data.get(from);
        data.set(from, data.get(to));
        data.set(to, tmp);
        notifyItemMoved(from, to);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_textview_item, parent, false);
        view.setFocusable(false);
        view.setClickable(onClick != null);
        view.setOnClickListener(onClick);
        view.setOnLongClickListener(onLongClick);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseAdapter.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            tvSet.put(position, ((ViewHolder) holder).tv);
            TextView tv = ((ViewHolder) holder).tv;
            tv.setText(data.get(position));
            tv.getLayoutParams().height /= scale;
            if (marqueeSelected) {
                boolean sel = position == getSelectedPosition();
                tv.setSingleLine(true);
                tv.setMarqueeRepeatLimit(-1);
                if (sel) {
                    tv.setSelected(true);
                    tv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                } else {
                    tv.setSelected(false);
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                }
            }
        }
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Get TextView by position<br>
     * 通过位置获取TextView
     * @param position Position of the TextView | TextView的位置
     * @return The TextView at the position, or null if not found | 对应位置的TextView，未找到则返回null
     */
    @Nullable
    public TextView getTextView(int position) {
        return tvSet.get(position);
    }

    /**
     * Reduce the height of TextView to scale<br>
     * 按比例减小TextView高度
     * @param scale 比例（高度除以几）
     */
    @SuppressLint("NotifyDataSetChanged")
    public void smallerTextViewHeight(int scale) {
        if (scale == 0) scale = 1;
        this.scale = scale;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends BaseAdapter.ViewHolder {
        TextView tv;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.item);
        }
    }
}
