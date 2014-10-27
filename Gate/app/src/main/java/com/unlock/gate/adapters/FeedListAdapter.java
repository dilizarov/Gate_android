package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.unlock.gate.R;
import com.unlock.gate.models.FeedItem;

import java.util.List;

/**
 * Created by davidilizarov on 10/22/14.
 */
public class FeedListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<FeedItem> feedItems;

    public FeedListAdapter(Context context, List<FeedItem> feedItems) {
        this.context = context;
        this.feedItems = feedItems;
    }

    @Override
    public int getCount() {
        return feedItems.size();
    }

    @Override
    public Object getItem(int position) {
        return feedItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) convertView = mInflater.inflate(R.layout.feed_item, null);

        TextView name        = (TextView) convertView.findViewById(R.id.name);
        TextView networkName = (TextView) convertView.findViewById(R.id.networkName);
        TextView message     = (TextView) convertView.findViewById(R.id.message);
        TextView timestamp   = (TextView) convertView.findViewById(R.id.timestamp);

        FeedItem feedItem = feedItems.get(position);

        name.setText(feedItem.getName());
        networkName.setText(feedItem.getNetworkName());
        message.setText(feedItem.getMessage());
        timestamp.setText(feedItem.getTimestamp());

        return convertView;
    }
}
