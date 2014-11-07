package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.unlock.gate.R;
import com.unlock.gate.models.Post;

import java.util.List;

/**
 * Created by davidilizarov on 10/22/14.
 */
public class FeedListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<Post> posts;

    public FeedListAdapter(Context context, List<Post> posts) {
        this.context = context;
        this.posts = posts;
    }

    @Override
    public int getCount() {
        return posts.size();
    }

    @Override
    public Object getItem(int position) {
        return posts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) convertView = mInflater.inflate(R.layout.feed_item, null);

        TextView name          = (TextView) convertView.findViewById(R.id.name);
        TextView body          = (TextView) convertView.findViewById(R.id.body);
        TextView timestamp     = (TextView) convertView.findViewById(R.id.timestamp);
        TextView commentsCount = (TextView) convertView.findViewById(R.id.commentsCount);

        Post post = posts.get(position);

        name.setText(post.getName());
        body.setText(post.getBody());
        timestamp.setText(post.getTimestamp());
        commentsCount.setText(context.getResources()
                .getQuantityString(R.plurals.comments_count,
                                   post.getCommentCount(),
                                   post.getCommentCount()));

        return convertView;
    }
}
