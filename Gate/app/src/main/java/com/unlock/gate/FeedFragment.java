package com.unlock.gate;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.unlock.gate.adapters.FeedListAdapter;
import com.unlock.gate.models.FeedItem;
import com.unlock.gate.models.Network;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FeedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FeedFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class FeedFragment extends ListFragment {
    private static final String ARG_POSITION = "position";
    private ListView feed;
    private FeedListAdapter listAdapter;
    private List<FeedItem> feedItems;
    private Network currentNetwork;

    private int position;

    public static FeedFragment newInstance(int position) {
        FeedFragment fragment = new FeedFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }
    public FeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        feed = getListView();

        feedItems = new ArrayList<FeedItem>();

        new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 9; i++) {
                    FeedItem item = new FeedItem("123" + i, "David Ilibaba", "Hi, this is message number AP" + i, "Network " + i, "2014-10-24T22:3" + i + ":24.642Z");

                    feedItems.add(item);
                }

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        listAdapter = new FeedListAdapter(getActivity(), feedItems);
                        feed.setAdapter(listAdapter);
                        listAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    public void getNetworkFeed(Network network) {
        Toast.makeText(getActivity(), network.getName(), Toast.LENGTH_LONG).show();
        //Remember, if this is the network we are already showing, then DON'T go through all the effort of a network request, just treat it as a page change.

//        if (currentNetwork.getId().equals(network.getId())) return;
//        else {
//            //Load
//        }
    }

//    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }
//

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
