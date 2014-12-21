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

    // Represents the fact that there is no more data from the server to populate the list.
    // Example: Indicates that we've retrieved all the posts in the feed.
    // TODO: Maybe rename for clarity?
    private boolean atEndOfList = false;

    private boolean hadProblemsLoading = false;
    private long lastTimeLoading;

    public InfiniteScrollListener(int currentPage) {
        this.currentPage = currentPage;
    }

    public InfiniteScrollListener(int currentPage, int itemCount) {
        this.currentPage = currentPage;
        this.itemCount = itemCount;
    }

    public InfiniteScrollListener() {}

    public int getBufferItemCount() {
        return bufferItemCount;
    }

    public void setBufferItemCount(int bufferItemCount) {
        this.bufferItemCount = bufferItemCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public void reachedEndOfList() {
        atEndOfList = true;
        isLoading = false;
    }

    public boolean getAtEndOfList() {
        return atEndOfList;
    }

    public void setAtEndOfList(boolean atEndOfList) {
        this.atEndOfList = atEndOfList;
    }

    public void setHadProblemsLoading(boolean flag) {
        hadProblemsLoading = flag;
    }

    public abstract void loadMore(int page);

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        if (atEndOfList) return;

        if (totalItemCount < itemCount) {
            this.itemCount = totalItemCount;
            if (totalItemCount == 0) isLoading = true;
        }

        if (isLoading && (totalItemCount > itemCount)) {
            isLoading = false;
            itemCount = totalItemCount;
            currentPage++;
        }

        if (hadProblemsLoading && (lastTimeLoading + 4000 < System.currentTimeMillis())) isLoading = false;

        if (!isLoading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + bufferItemCount)) {
            loadMore(currentPage + 1);
            isLoading = true;
            lastTimeLoading = System.currentTimeMillis();
        }
    }
}
