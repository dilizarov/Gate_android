package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
    private List<Gate> gates;

    static class ViewHolder {
        TextView gateItemName;
        TextView gateUserCount;
        ImageView generatedGate;
    }

    public GatesListAdapter(Context context, List<Gate> gates) {
        this.context = context;
        this.gates = gates;
    }

    @Override
    public int getCount() {
        return gates.size();
    }

    @Override
    public Object getItem(int position) {
        return gates.get(position);
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
            convertView = mInflater.inflate(R.layout.gate_item, null);

            viewHolder = new ViewHolder();

            viewHolder.gateItemName  = (TextView) convertView.findViewById(R.id.gateItemName);
            viewHolder.gateUserCount = (TextView) convertView.findViewById(R.id.usersCount);
            viewHolder.generatedGate = (ImageView) convertView.findViewById(R.id.generatedGate);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (position % 2 == 1) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.light_grey));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        Gate gate = gates.get(position);

        viewHolder.gateItemName.setText(gate.getName());
        viewHolder.gateUserCount.setText(context.getResources()
                .getQuantityString(R.plurals.users_count,
                                   gate.getUsersCount(),
                                   gate.getUsersCount()));

        viewHolder.generatedGate.setVisibility(gate.getGenerated() ? View.VISIBLE : View.INVISIBLE);

        return convertView;
    }
}
