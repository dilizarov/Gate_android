package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.unlock.gate.R;
import com.unlock.gate.models.Network;

import java.util.List;

/**
 * Created by davidilizarov on 10/27/14.
 */
public class NetworksListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<Network> networkItems;

    public NetworksListAdapter(Context context, List<Network> networkItems) {
        this.context = context;
        this.networkItems = networkItems;
    }

    @Override
    public int getCount() {
        return networkItems.size();
    }

    @Override
    public Object getItem(int position) {
        return networkItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) convertView = mInflater.inflate(R.layout.network_item, null);

        if (position % 2 == 1) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.light_grey));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        TextView networkItemName = (TextView) convertView.findViewById(R.id.networkItemName);
        TextView networkUserCount = (TextView) convertView.findViewById(R.id.usersCount);

        Network network = networkItems.get(position);

        networkItemName.setText(network.getName());
        networkUserCount.setText(context.getResources()
                .getQuantityString(R.plurals.users_count,
                                   network.getUsersCount(),
                                   network.getUsersCount()));

        return convertView;
    }
}
