package com.fernandobarillas.linkshare.ui;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.fernandobarillas.linkshare.callbacks.ItemSwipedRightCallback;

/**
 * Created by fb on 2/2/16.
 */
public class ItemTouchHelperCallback extends ItemTouchHelper.SimpleCallback {
    ItemSwipedRightCallback mCallback;

    public ItemTouchHelperCallback(ItemSwipedRightCallback callback) {
        // Only swipe right is implemented
        super(0, ItemTouchHelper.RIGHT);
        mCallback = callback;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        if (mCallback != null) {
            mCallback.swipeCallback(viewHolder);
        }
    }
}
