package com.unlock.gate.utils;

import android.widget.AbsListView;

/**
 * Created by davidilizarov on 11/7/14.
 */
public abstract class InfiniteScrollListener implements AbsListView.OnScrollListener {
    private int bufferItemCount = 3;
    private int currentPage = 0;
    private int itemCount = 0;
    private boolean isLoading = false;

    public InfiniteScrollListener(int currentPage) {
        this.currentPage = currentPage;
    }

    public InfiniteScrollListener(int bufferItemCount, int currentPage) {
        this.bufferItemCount = bufferItemCount;
        this.currentPage = currentPage;
    }

    public InfiniteScrollListener() {}

    public abstract void loadMore(int page);

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount < itemCount) {
            this.itemCount = totalItemCount;
            if (totalItemCount == 0) isLoading = true;
        }

        if (isLoading && (totalItemCount > itemCount)) {
            isLoading = false;
            itemCount = totalItemCount;
            currentPage++;
        }

        if (!isLoading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + bufferItemCount)) {
            loadMore(currentPage + 1);
            isLoading = true;
        }
    }
}
