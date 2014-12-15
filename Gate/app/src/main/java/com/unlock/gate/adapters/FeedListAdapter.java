package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.unlock.gate.R;
import com.unlock.gate.models.Network;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.PostViewHelper;

import java.util.List;

/**
 * Created by davidilizarov on 10/22/14.
 */
public class FeedListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<Post> posts;
    private Network network;
    private SharedPreferences mSessionPreferences;

    public FeedListAdapter(Context context, List<Post> posts, Network network) {
        this.context = context;
        this.posts = posts;
        this.network = network;
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

        if (position % 2 == 1) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.light_grey));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        TextView name          = (TextView) convertView.findViewById(R.id.name);
        TextView body          = (TextView) convertView.findViewById(R.id.body);
        TextView timestamp     = (TextView) convertView.findViewById(R.id.timestamp);
        TextView commentsCount = (TextView) convertView.findViewById(R.id.commentsCount);
        TextView networkName   = (TextView) convertView.findViewById(R.id.networkName);
        ImageView createComment = (ImageView) convertView.findViewById(R.id.createComment);
        ImageView commentsCountBubble = (ImageView) convertView.findViewById(R.id.commentsCountBubble);
        final ImageView smileyCount  = (ImageView) convertView.findViewById(R.id.smileyCount);
        final ImageView upPost       = (ImageView) convertView.findViewById(R.id.upPost);
        final TextView upCountPost   = (TextView) convertView.findViewById(R.id.upCountPost);

        final LinearLayout postStats = (LinearLayout) convertView.findViewById(R.id.postStats);

        final Post post = posts.get(position);

        name.setText(post.getName());
        body.setText(post.getBody());
        timestamp.setText(post.getTimestamp());

        PostViewHelper.handleUpBehavior(context, post, upPost, upCountPost, smileyCount, postStats);
        PostViewHelper.handleCommentBehavior(context, post, createComment, commentsCount, commentsCountBubble);

        if (network == null) {
            networkName.setText(post.getNetworkName());
            networkName.setVisibility(View.VISIBLE);
        } else {
            networkName.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

}
