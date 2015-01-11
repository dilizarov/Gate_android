package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.unlock.gate.R;
import com.unlock.gate.models.Gate;

import java.util.List;

/**
 * Created by davidilizarov on 10/27/14.
 */
public class GatesListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<Gate> gateItems;

    public GatesListAdapter(Context context, List<Gate> gateItems) {
        this.context = context;
        this.gateItems = gateItems;
    }

    @Override
    public int getCount() {
        return gateItems.size();
    }

    @Override
    public Object getItem(int position) {
        return gateItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) convertView = mInflater.inflate(R.layout.gate_item, null);

        if (position % 2 == 1) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.light_grey));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        TextView gateItemName = (TextView) convertView.findViewById(R.id.gateItemName);
        TextView gateUserCount = (TextView) convertView.findViewById(R.id.usersCount);

        Gate gate = gateItems.get(position);

        gateItemName.setText(gate.getName());
        gateUserCount.setText(context.getResources()
                .getQuantityString(R.plurals.users_count,
                                   gate.getUsersCount(),
                                   gate.getUsersCount()));

        return convertView;
    }
}
