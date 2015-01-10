package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.unlock.gate.CommentsActivity;
import com.unlock.gate.MainActivity;
import com.unlock.gate.R;
import com.unlock.gate.models.Network;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.PostViewHelper;

import java.util.List;

/**
 * Created by davidilizarov on 10/22/14.
 */
public class FeedListAdapter extends BaseAdapter {

    private final int UPDATE_POST_INTENT = 2;
    private Context context;
    private LayoutInflater mInflater;
    private List<Post> posts;
    private Network network;

    static class ViewHolder {
        TextView name;
        TextView body;
        TextView timestamp;
        TextView commentsCount;
        TextView networkName;
        ImageView createComment;
        ImageView commentsCountBubble;
        ImageView smileyCount;
        ImageView upPost;
        TextView upCountPost;
        LinearLayout postStats;
    }

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
        ViewHolder viewHolder;

        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.feed_item, null);

            viewHolder = new ViewHolder();

            viewHolder.name                = (TextView) convertView.findViewById(R.id.name);
            viewHolder.body                = (TextView) convertView.findViewById(R.id.body);
            viewHolder.timestamp           = (TextView) convertView.findViewById(R.id.timestamp);
            viewHolder.commentsCount       = (TextView) convertView.findViewById(R.id.commentsCount);
            viewHolder.networkName         = (TextView) convertView.findViewById(R.id.networkName);
            viewHolder.createComment       = (ImageView) convertView.findViewById(R.id.createComment);
            viewHolder.commentsCountBubble = (ImageView) convertView.findViewById(R.id.commentsCountBubble);
            viewHolder.smileyCount         = (ImageView) convertView.findViewById(R.id.smileyCount);
            viewHolder.upPost              = (ImageView) convertView.findViewById(R.id.upPost);
            viewHolder.upCountPost         = (TextView) convertView.findViewById(R.id.upCountPost);
            viewHolder.postStats           = (LinearLayout) convertView.findViewById(R.id.postStats);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (position % 2 == 1) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.light_grey));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        final Post post = posts.get(position);

        viewHolder.name.setText(post.getName());
        viewHolder.body.setText(post.getBody());
        viewHolder.timestamp.setText(post.getTimestamp());

        PostViewHelper.handleUpBehavior(context, post, viewHolder.upPost, viewHolder.upCountPost, viewHolder.smileyCount, viewHolder.postStats);
        PostViewHelper.handleCommentBehavior(post, viewHolder.commentsCount, viewHolder.commentsCountBubble, viewHolder.postStats);

        viewHolder.createComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, CommentsActivity.class);
                intent.putExtra("post", post);
                intent.putExtra("creatingComment", true);
                ((MainActivity) context).startActivityForResult(intent, UPDATE_POST_INTENT);
            }
        });

        if (network == null) {
            viewHolder.networkName.setText(post.getNetworkName());
            viewHolder.networkName.setVisibility(View.VISIBLE);
        } else {
            viewHolder.networkName.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

}
