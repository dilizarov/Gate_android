package com.unlock.gate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
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

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FeedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FeedFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class FeedFragment extends ListFragment implements OnRefreshListener {

    private static final String ARG_POSITION = "position";
    private ListView feed;
    private Button createPost;
    private FeedListAdapter listAdapter;
    private ArrayList<Post> posts;
    private ArrayList<Post> adapterPosts;
    private SharedPreferences mSessionPreferences;
    private PullToRefreshLayout mPullToRefreshLayout;
    private DateTime infiniteScrollTimeBuffer;
    private int currentPage;

    private InfiniteScrollListener infiniteScrollListener;

    private Network currentNetwork;

    private LinearLayout progressBarHolder;

    private final int CREATE_POST_INTENT = 1;

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup viewGroup = (ViewGroup) view;
        mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());

        ActionBarPullToRefresh.from(getActivity())
                .insertLayoutInto(viewGroup)
                .theseChildrenArePullable(getListView(), getListView().getEmptyView())
                .listener(this)
                .setup(mPullToRefreshLayout);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSessionPreferences = this.getActivity().getSharedPreferences(
                getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        //Handles the case of when a user backs out of the App. We want to have them back at their
        //original network viewed.
        currentNetwork = Network.deserialize(mSessionPreferences.getString(getString(R.string.user_last_gate_viewed_key), null));

        feed = getListView();

        posts = new ArrayList<Post>();
        adapterPosts = new ArrayList<Post>();

        progressBarHolder = (LinearLayout) this.getActivity().findViewById(R.id.feedProgressBarHolder);

        createPost = (Button) this.getActivity().findViewById(R.id.createPost);

        if (savedInstanceState != null) {
            posts            = savedInstanceState.getParcelableArrayList("posts");
            currentNetwork   = savedInstanceState.getParcelable("currentNetwork");
            infiniteScrollTimeBuffer =
                    (DateTime) savedInstanceState.getSerializable("infiniteScrollTimeBuffer");
            currentPage      = savedInstanceState.getInt("currentPage");
            int index        = savedInstanceState.getInt("feedsFirstVisiblePosition");
            int top          = savedInstanceState.getInt("topOfFeed");
            currentPage      = savedInstanceState.getInt("currentPage");

            progressBarHolder.setVisibility(View.GONE);
            keepPositionInListAndAdaptNewPostsToFeed(index, top);

            setInfiniteScrollListener(savedInstanceState.getBoolean("atEndOfList"));
        } else {
            setInfiniteScrollListener(false);
        }

        ((MainActivity) getActivity()).setTitle(currentNetwork);

        setListViewItemClickListener();
        setCreatePostClickListener();

    }

    @Override
    public void onRefreshStarted(View view) {
        infiniteScrollListener.setAtEndOfList(false);
        requestPostsAndPopulateListView(true);
        setCurrentPage(1);
    }

    public void getNetworkFeed(Network network) {
        if (network == null) Toast.makeText(getActivity(), "Aggregate", Toast.LENGTH_LONG).show();
        else Toast.makeText(getActivity(), network.getName(), Toast.LENGTH_LONG).show();

        ((MainActivity) getActivity()).setTitle(network);

        if (!onAggregateAndGettingAggregate(network) &&
            !onNetworkAndGettingSameNetwork(network)) {

            APIRequestManager.getInstance().cancelAllFeedRequests();

            mSessionPreferences.edit().putString(getString(R.string.user_last_gate_viewed_key),
                    (network != null) ? network.serialize() : null).apply();

            currentNetwork = network;
            feed.setSelection(0);
            mPullToRefreshLayout.setRefreshing(true);
            infiniteScrollListener.setAtEndOfList(false);
            requestPostsAndPopulateListView(true);
            setCurrentPage(1);
        }
    }

    private void requestPostsAndPopulateListView(final boolean refreshing) {
        requestPostsAndPopulateListView(refreshing, -1);
    }

    private void requestPostsAndPopulateListView(final boolean refreshing, int page) {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

            if (!refreshing) {
                if (page > 0) params.put("page", page);

                if (infiniteScrollTimeBuffer != null)
                    params.put("infinite_scroll_time_buffer", infiniteScrollTimeBuffer);
            }

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    final JSONObject jsonResponse = response;

                    new Thread(new Runnable() {

                        public void run() {
                            if (refreshing) posts.clear();

                            JSONArray jsonPosts = jsonResponse.optJSONArray("posts");
                            int len = jsonPosts.length();

                            if (len == 0) infiniteScrollListener.reachedEndOfList();

                            for (int i = 0; i < len; i++) {
                                JSONObject jsonPost = jsonPosts.optJSONObject(i);
                                Post post = new Post(jsonPost.optString("external_id"),
                                        jsonPost.optJSONObject("user").optString("name"),
                                        jsonPost.optString("body"),
                                        jsonPost.optJSONObject("network").optString("external_id"),
                                        jsonPost.optJSONObject("network").optString("name"),
                                        jsonPost.optInt("comments_count"),
                                        jsonPost.optInt("up_count"),
                                        jsonPost.optBoolean("uped"),
                                        jsonPost.optString("created_at"));

                                if (i == 0 && (infiniteScrollTimeBuffer == null || refreshing))
                                    infiniteScrollTimeBuffer = post.getTimeCreated();

                                posts.add(post);
                            }

                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    if (refreshing) {
                                        mPullToRefreshLayout.setRefreshComplete();
                                        Toast.makeText(getActivity(), "Refreshed", Toast.LENGTH_LONG).show();
                                    } else {
                                        progressBarHolder.setVisibility(View.GONE);
                                    }

                                    keepPositionInListAndAdaptNewPostsToFeed();
                                }
                            });
                        }
                    }).start();
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (refreshing) mPullToRefreshLayout.setRefreshComplete();
                    progressBarHolder.setVisibility(View.GONE);

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

    //Usual listAdapter & notifyDataSetChanged stuff wrapped around ensuring
    //The same position is kept.
    private void keepPositionInListAndAdaptNewPostsToFeed(int index, int top) {
        if (index == -1) index = firstVisiblePost();
        if (top   == -1) top = topOfFeed();

        if (listAdapter == null) {
            listAdapter = new FeedListAdapter(getActivity(), adapterPosts, currentNetwork);
            feed.setAdapter(listAdapter);
        }

        listAdapter.setNetwork(currentNetwork);
        adapterPosts.clear();
        adapterPosts.addAll(posts);
        listAdapter.notifyDataSetChanged();

        feed.setSelectionFromTop(index, top);
    }

    private void keepPositionInListAndAdaptNewPostsToFeed() {
        keepPositionInListAndAdaptNewPostsToFeed(-1, -1);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("posts", posts);
        outState.putParcelable("currentNetwork", currentNetwork);
        outState.putSerializable("infiniteScrollTimeBuffer", infiniteScrollTimeBuffer);
        outState.putInt("feedsFirstVisiblePosition", firstVisiblePost());
        outState.putInt("topOfFeed", topOfFeed());
        outState.putInt("currentPage", currentPage);
        outState.putBoolean("atEndOfList", infiniteScrollListener.getAtEndOfList());
    }

    private void setInfiniteScrollListener(boolean atEndOfList) {
        infiniteScrollListener = new InfiniteScrollListener(currentPage, posts.size()) {
            @Override
            public void loadMore(int page) {
                requestPostsAndPopulateListView(false, page);
                currentPage = page;
            }
        };

        infiniteScrollListener.setAtEndOfList(atEndOfList);

        feed.setOnScrollListener(infiniteScrollListener);
    }

    private void setListViewItemClickListener() {
        feed.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                Intent intent = new Intent(getActivity(), CommentsActivity.class);
                intent.putExtra("post", posts.get(position));
                startActivity(intent);

            }
        });
    }

    private void setCreatePostClickListener() {
        createPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CreatePostActivity.class);
                intent.putExtra("currentNetwork", currentNetwork);
                intent.putExtra("networks", getNetworks());
                startActivityForResult(intent, CREATE_POST_INTENT);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CREATE_POST_INTENT:
                if (resultCode == getActivity().RESULT_OK) {
                    final Network network = (Network) data.getParcelableExtra("network");
                    final String postBody = data.getStringExtra("postBody").replaceAll("\\n+", "\n")
                                                                           .replaceAll("[ \\t\\x0b\\r\\f]{2,}", " ");

                    try {
                        JSONObject params = new JSONObject();
                        params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                              .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

                        JSONObject post = new JSONObject();
                        post.put("body", postBody);

                        params.put("post", post);

                        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {

                                if (currentNetwork == null ||
                                    onNetworkAndGettingSameNetwork(network)) {

                                    JSONObject jsonPost = response.optJSONObject("post");
                                    Post post = new Post(jsonPost.optString("external_id"),
                                            jsonPost.optJSONObject("user").optString("name"),
                                            jsonPost.optString("body"),
                                            jsonPost.optJSONObject("network").optString("external_id"),
                                            jsonPost.optJSONObject("network").optString("name"),
                                            jsonPost.optInt("comments_count"),
                                            jsonPost.optInt("up_count"),
                                            jsonPost.optBoolean("uped"),
                                            jsonPost.optString("created_at"));

                                    posts.add(0, post);
                                    feed.setSelection(0);
                                    listAdapter = new FeedListAdapter(getActivity(), posts, currentNetwork);
                                    feed.setAdapter(listAdapter);
                                    listAdapter.notifyDataSetChanged();
                                } else {
                                    //Crouton/Toast stuff.
                                    Toast.makeText(getActivity(), "Successfully posted to another network", Toast.LENGTH_LONG).show();
                                }

                            }
                        };

                        Response.ErrorListener errorListener = new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                                Intent intent = new Intent(getActivity(), CreatePostActivity.class);
                                intent.putExtra("currentNetwork", network);
                                intent.putExtra("networks", getNetworks());
                                intent.putExtra("postBody", postBody);

                                if (volleyError.isExpectedError()) {
                                    intent.putExtra("errors", volleyError.getErrors().toString());
                                } else {
                                    intent.putExtra("errorMessage", volleyError.getMessage());
                                }

                                startActivityForResult(intent, CREATE_POST_INTENT);
                            }
                        };

                        APIRequestManager.getInstance().doRequest().createPost(network, params, listener, errorListener);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }

                }
                break;
            }
    }

    private int firstVisiblePost() {
        return feed.getFirstVisiblePosition();
    }

    private int topOfFeed() {
        View v = feed.getChildAt(0);
        return (v == null) ? 0 : v.getTop();
    }

    private boolean onAggregateAndGettingAggregate(Network network) {
        return currentNetwork == null && network == null;
    }

    private boolean onNetworkAndGettingSameNetwork(Network network) {
        return currentNetwork != null && network != null &&
               currentNetwork.getId().equals(network.getId());
    }

    private void setCurrentPage(int page) {
        currentPage = page;
        infiniteScrollListener.setCurrentPage(currentPage);
    }

    private ArrayList<Network> getNetworks() {
        return ((MainActivity) getActivity()).getNetworks();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
