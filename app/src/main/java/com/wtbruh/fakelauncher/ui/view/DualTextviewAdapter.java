package com.wtbruh.fakelauncher.ui.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wtbruh.fakelauncher.R;

import java.util.HashMap;
import java.util.List;

public class DualTextviewAdapter extends BaseAdapter{
    private final List<Bundle> data;
    public HashMap<Integer, TextView> tvSet;
    public HashMap<Integer, TextView> tv2Set;
    private int scale = 1;
    private OnItemClickListener listener;
    public final static String ITEM = "item";
    public final static String SUB_ITEM = "subItem";
    public DualTextviewAdapter(List<Bundle> data) {
        this.data = data;
        tvSet = new HashMap<>();
        tv2Set = new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dual_textview_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseAdapter.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            Bundle b = data.get(position);
            tvSet.put(position, ((ViewHolder) holder).tv);
            tv2Set.put(position, ((ViewHolder) holder).tv2);

            TextView tv = ((ViewHolder) holder).tv;
            tv.setText(b.getString(ITEM));

            TextView tv2 = ((ViewHolder) holder).tv2;
            tv2.setText(b.getString(SUB_ITEM));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(holder.getAdapterPosition(), tv, tv2);
                }
            });

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
     * @param sub False for the first TextView, true for the second TextView | false为第一个TextView，true为第二个TextView
     * @param position Position of the TextView | TextView的位置
     * @return The TextView at the position, or null if not found | 对应位置的TextView，未找到则返回null
     */
    @Nullable
    public TextView getTextView(boolean sub, int position) {
        if (sub) {
            return tv2Set.get(position);
        } else {
            return tvSet.get(position);
        }
    }

    /**
     * Get TextView by position<br>
     * 通过位置获取TextView
     * @param position Position of the TextView | TextView的位置
     * @return The TextView at the position, or null if not found | 对应位置的TextView，未找到则返回null
     */
    public TextView getTextView(int position) {
        return getTextView(false, position);
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

    public interface OnItemClickListener {
        void onItemClick(int position, TextView itemTv, TextView subItemTv);
    }

    public void setOnItemClickListener(OnItemClickListener listenser) {
        this.listener = listenser;
    }

    public static class ViewHolder extends BaseAdapter.ViewHolder {
        TextView tv;
        TextView tv2;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.item);
            tv2 = itemView.findViewById(R.id.subItem);
        }
    }
}
