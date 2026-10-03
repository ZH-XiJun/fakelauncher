package com.wtbruh.fakelauncher.ui.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.wtbruh.fakelauncher.R;

import java.util.List;

/** 两行文本列表项：第一行名称（加粗），第二行号码，两行同字号。 */
public class TwoLineTextAdapter extends BaseAdapter {
    private final List<String[]> data; // 每项 [名称, 号码]

    public TwoLineTextAdapter(List<String[]> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.two_line_textview_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseAdapter.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            ViewHolder vh = (ViewHolder) holder;
            String[] row = data.get(position);
            vh.tv.setText(row[0]);
            vh.tvSub.setText(row[1]);
        }
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends BaseAdapter.ViewHolder {
        TextView tv;
        TextView tvSub;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.item);
            tvSub = itemView.findViewById(R.id.item_sub);
        }
    }
}
