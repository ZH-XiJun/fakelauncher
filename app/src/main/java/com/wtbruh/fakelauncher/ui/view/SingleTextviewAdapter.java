package com.wtbruh.fakelauncher.ui.view;

import android.annotation.SuppressLint;
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
    public SingleTextviewAdapter(List<String> data) {
        this.data = data;
        tvSet = new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_textview_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseAdapter.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            tvSet.put(position, ((ViewHolder) holder).tv);
            TextView tv = ((ViewHolder) holder).tv;
            tv.setText(data.get(position));
            tv.getLayoutParams().height /= scale;
        }
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

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
