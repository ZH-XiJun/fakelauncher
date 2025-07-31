package com.wtbruh.fakelauncher.ui.view;

import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseAdapter extends RecyclerView.Adapter<BaseAdapter.ViewHolder>{
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setSelected(position == mSelectedPosition);
    }

    private int mSelectedPosition = 0;

    public void setSelectedPosition(int position) {
        int oldPosition = mSelectedPosition;
        mSelectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(position);
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }



}
