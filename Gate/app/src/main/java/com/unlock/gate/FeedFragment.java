package com.unlock.gate;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.adapters.FeedListAdapter;
import com.unlock.gate.models.Network;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.InfiniteScrollListener;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


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
    private ArrayList<Post> posts;
    private SharedPreferences mSessionPreferences;
    private DateTime infiniteScrollTimeBuffer;

    private Network currentNetwork;

    private LinearLayout progressBarHolder;

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

        mSessionPreferences = this.getActivity().getSharedPreferences(
                getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        feed = getListView();

        posts = new ArrayList<Post>();

        progressBarHolder = (LinearLayout) this.getActivity().findViewById(R.id.feedProgressBarHolder);

        feed.setOnScrollListener(new InfiniteScrollListener() {
            @Override
            public void loadMore(int page) {
                requestPostsAndPopulateListView(false, page);
            }
        });
        //requestFeedAndPopulateListView(false);

//        new Thread(new Runnable() {
//            public void run() {
//                for (int i = 0; i < 9; i++) {
//                    Post item = new Post("123" + i, "David Ilibaba", "Hi, this is message number AP" + i, "Network " + i, "2014-10-24T22:3" + i + ":24.642Z");
//
//                    posts.add(item);
//                }
//
//                getActivity().runOnUiThread(new Runnable() {
//                    public void run() {
//                        listAdapter = new FeedListAdapter(getActivity(), posts);
//                        feed.setAdapter(listAdapter);
//                        listAdapter.notifyDataSetChanged();
//                    }
//                });
//            }
//        }).start();
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
    private void requestPostsAndPopulateListView(final boolean refreshing) {
        requestPostsAndPopulateListView(refreshing, -1);
    }

    private void requestPostsAndPopulateListView(final boolean refreshing, int page) {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

            if (page > 0) params.put("page", page);

            if (infiniteScrollTimeBuffer != null)
                params.put("infinite_scroll_time_buffer", infiniteScrollTimeBuffer);

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    final JSONObject jsonResponse = response;

                    new Thread(new Runnable() {

                        public void run() {
                            JSONArray jsonPosts = jsonResponse.optJSONArray("posts");
                            int len = jsonPosts.length();
                            for (int i = 0; i < len; i++) {
                                JSONObject jsonPost = jsonPosts.optJSONObject(i);
                                Post post = new Post(jsonPost.optString("external_id"),
                                        jsonPost.optJSONObject("user").optString("name"),
                                        jsonPost.optString("body"),
                                        jsonPost.optJSONObject("network").optString("external_id"),
                                        jsonPost.optInt("comments_count"),
                                        jsonPost.optString("created_at"));

                                if (i == 0 && infiniteScrollTimeBuffer == null)
                                    infiniteScrollTimeBuffer = post.getTimeCreated();

                                posts.add(post);
                            }

                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    progressBarHolder.setVisibility(View.GONE);
                                    listAdapter = new FeedListAdapter(getActivity(), posts);
                                    feed.setAdapter(listAdapter);
                                    listAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }).start();
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    Log.v("Error", "nooooo");
                }
            };

            if (currentNetwork == null) {
                APIRequestManager.getInstance().doRequest().getAggregate(params, listener, errorListener);
            } else {
                params.put("network_id", currentNetwork.getId());
                APIRequestManager.getInstance().doRequest().getNetworkPosts(params, listener, errorListener);
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
