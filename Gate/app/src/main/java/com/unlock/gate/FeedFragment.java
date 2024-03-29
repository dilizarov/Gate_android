package com.unlock.gate;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gc.materialdesign.views.ButtonFloat;
import com.unlock.gate.adapters.FeedListAdapter;
import com.unlock.gate.models.Gate;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.InfiniteScrollListener;
import com.unlock.gate.utils.RegexConstants;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.DefaultHeaderTransformer;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must interface
 * to handle interaction events.
 * Use the {@link FeedFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class FeedFragment extends ListFragment implements OnRefreshListener {

    private ListView feed;
    private ButtonFloat createPost;
    private FeedListAdapter listAdapter;
    private ArrayList<Post> posts;
    private ArrayList<Post> adapterPosts;
    private SharedPreferences mSessionPreferences;
    private PullToRefreshLayout mPullToRefreshLayout;
    private DateTime infiniteScrollTimeBuffer;
    private int currentPage;

    private InfiniteScrollListener infiniteScrollListener;

    private Gate previousGate;
    private Gate currentGate;

    private TextView noPostsMessage;
    private LinearLayout progressBarHolder;

    private ProgressBar postLoading;

    private final int UPDATE_POST_INTENT = 2;

    private Gate selectedGate;

    public static FeedFragment newInstance() {
        FeedFragment fragment = new FeedFragment();

        return fragment;
    }
    public FeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // For some reason, on very rare occasions when you navigate to FeedFragment, this doesn't stay
        // The way it was initially initialized, so on resume, we just do it each time. Quick fix for now.
        DefaultHeaderTransformer transformer = (DefaultHeaderTransformer) mPullToRefreshLayout
                .getHeaderTransformer();
        transformer.getHeaderView().findViewById(R.id.ptr_text)
                .setBackgroundColor(getResources().getColor(R.color.black));
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
        mPullToRefreshLayout = new PullToRefreshLayout(view.getContext());

        ActionBarPullToRefresh.from(getActivity())
                .insertLayoutInto(viewGroup)
                .theseChildrenArePullable(getListView(), getListView().getEmptyView())
                .listener(this)
                .setup(mPullToRefreshLayout);

        DefaultHeaderTransformer transformer = (DefaultHeaderTransformer) mPullToRefreshLayout
                .getHeaderTransformer();
        transformer.getHeaderView().findViewById(R.id.ptr_text)
                .setBackgroundColor(getResources().getColor(R.color.black));
        transformer.setProgressBarColor(getResources().getColor(R.color.white));
        transformer.setProgressBarHeight(2);
        transformer.setPullText("Pull down the internet...");
        transformer.setRefreshingText("Finding posts...");

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSessionPreferences = this.getActivity().getSharedPreferences(
                getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        //Handles the case of when a user backs out of the App. We want to have them back at their
        //original gate viewed.
        currentGate = Gate.deserialize(mSessionPreferences.getString(getString(R.string.user_last_gate_viewed_key), null));

        feed = getListView();

        posts = new ArrayList<Post>();
        adapterPosts = new ArrayList<Post>();

        noPostsMessage = (TextView) this.getActivity().findViewById(R.id.noPostsMessage);
        progressBarHolder = (LinearLayout) this.getActivity().findViewById(R.id.feedProgressBarHolder);

        postLoading = (ProgressBar) this.getActivity().findViewById(R.id.postLoading);

        createPost = (ButtonFloat) this.getActivity().findViewById(R.id.createPost);

        if (savedInstanceState != null) {
            posts            = savedInstanceState.getParcelableArrayList("posts");
            currentGate      = savedInstanceState.getParcelable("currentGate");
            infiniteScrollTimeBuffer =
                    (DateTime) savedInstanceState.getSerializable("infiniteScrollTimeBuffer");
            currentPage      = savedInstanceState.getInt("currentPage");
            int index        = savedInstanceState.getInt("feedsFirstVisiblePosition");
            int top          = savedInstanceState.getInt("topOfFeed");

            progressBarHolder.setVisibility(View.GONE);
            setPositionInList(index, top);
            adaptNewPostsToFeed();

            setInfiniteScrollListener(savedInstanceState.getBoolean("atEndOfList"));
        } else {
            setInfiniteScrollListener(false);
            requestPostsAndPopulateListView(true, true);
        }

        ((MainActivity) getActivity()).setTitle(currentGate);

        setListViewItemClickListener();
        setCreatePostClickListener();
    }

    @Override
    public void onRefreshStarted(View view) {
        infiniteScrollListener.setAtEndOfList(false);
        requestPostsAndPopulateListView(true);
    }

    public Gate getCurrentGate() {
        return currentGate;
    }


    public void getGateFeed(Gate gate) {
        ((MainActivity) getActivity()).setTitle(gate);

        APIRequestManager.getInstance().cancelAllFeedRequests();

        mSessionPreferences.edit().putString(getString(R.string.user_last_gate_viewed_key),
                    (gate != null) ? gate.serialize() : null).apply();

        feed.post(new Runnable() {
                @Override
                public void run() {
                    feed.setSelection(0);
                }
            });

        previousGate = currentGate;

        currentGate = gate;

        progressBarHolder.setVisibility(View.VISIBLE);

        infiniteScrollListener.setAtEndOfList(false);
        requestPostsAndPopulateListView(true, true);
    }

    private void requestPostsAndPopulateListView(final boolean refreshing) {
        requestPostsAndPopulateListView(refreshing, -1, false);
    }

    private void requestPostsAndPopulateListView(final boolean refreshing, final boolean changingGates) {
        requestPostsAndPopulateListView(refreshing, -1, changingGates);
    }

    private void requestPostsAndPopulateListView(final boolean refreshing, final int page, final boolean changingGates) {
        try {

            JSONObject params = new JSONObject();

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

                            // Pages have 15 posts each. If the last page hands us 15,
                            // the very next request for more posts will hand back 0.
                            if (len < 15) {
                                infiniteScrollListener.reachedEndOfList();
                            }

                            for (int i = 0; i < len; i++) {
                                JSONObject jsonPost = jsonPosts.optJSONObject(i);
                                Post post = new Post(jsonPost.optString("external_id"),
                                        jsonPost.optJSONObject("user").optString("name"),
                                        jsonPost.optString("body"),
                                        jsonPost.optJSONObject("gate").optString("external_id"),
                                        jsonPost.optJSONObject("gate").optString("name"),
                                        jsonPost.optInt("comments_count"),
                                        jsonPost.optInt("up_count"),
                                        jsonPost.optBoolean("uped"),
                                        jsonPost.optString("created_at"));

                                // Server accounts for posts created before this time, so we need
                                // to add a millisecond so this gets caught.
                                if (i == 0 && (infiniteScrollTimeBuffer == null || refreshing))
                                    infiniteScrollTimeBuffer = post.getTimeCreated().plusMillis(1);

                                posts.add(post);
                            }

                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(new Runnable() {
                                public void run() {

                                    if (refreshing) {
                                        setCurrentPage(1);
                                        mPullToRefreshLayout.setRefreshComplete();

                                        if (!changingGates) Butter.down(getActivity(), "Refreshed");
                                    }

                                    progressBarHolder.setVisibility(View.GONE);

                                    keepPositionInList();
                                    adaptNewPostsToFeed();

                                    if (infiniteScrollListener.getAtEndOfList() && page > 1)
                                        Butter.down(getActivity(), getString(R.string.all_posts_loaded));

                                    infiniteScrollListener.setHadProblemsLoading(false);
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

                    if (refreshing) mPullToRefreshLayout.setRefreshComplete();
                    progressBarHolder.setVisibility(View.GONE);

                    if (posts.size() == 0) {
                        if (volleyError.isConnectionError())
                            noPostsMessage.setText(R.string.volley_no_connection_error);
                        else
                            noPostsMessage.setText(R.string.gate_error_message);

                        noPostsMessage.setVisibility(View.VISIBLE);
                    }

                    infiniteScrollListener.setHadProblemsLoading(true);

                    if (changingGates) {
                        ((MainActivity) getActivity()).setTitle(previousGate);
                        currentGate = previousGate;
                    }

                    Butter.downUnlessButtered(getActivity(), volleyError.getMessage());

                }
            };

            if (currentGate == null) {
                APIRequestManager.getInstance().doRequest().getAggregate(params, listener, errorListener);
            } else {
                APIRequestManager.getInstance().doRequest().getGatePosts(currentGate, params, listener, errorListener);
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    //Usual listAdapter & notifyDataSetChanged stuff wrapped around ensuring
    //The same position is kept.
    private void setPositionInList(int index, int top) {
        if (index == -1) index = firstVisiblePost();
        if (top   == -1) top = topOfFeed();

        feed.setSelectionFromTop(index, top);
    }

    private void keepPositionInList() {
        setPositionInList(-1, -1);
    }

    private void adaptNewPostsToFeed() {
        noPostsMessage.setText(R.string.no_posts_default);
        noPostsMessage.setVisibility(
                posts.size() == 0
                ? View.VISIBLE
                : View.GONE
        );

        if (listAdapter == null) {
            listAdapter = new FeedListAdapter(getActivity(), adapterPosts, currentGate);
            feed.setAdapter(listAdapter);
        }

        listAdapter.setGate(currentGate);
        adapterPosts.clear();
        adapterPosts.addAll(posts);
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("posts", posts);
        outState.putParcelable("currentGate", currentGate);
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
                requestPostsAndPopulateListView(false, page, false);
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
                startActivityForResult(intent, UPDATE_POST_INTENT);
            }
        });
    }

    private void setCreatePostClickListener() {
        createPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreatePostDialog(currentGate, getGates(), null);
            }
        });
    }

    public void openSharePost(String post) {
        showCreatePostDialog(currentGate, getGates(), post);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case UPDATE_POST_INTENT:
                if (resultCode == getActivity().RESULT_OK) {

                    final Post post = data.getParcelableExtra("updatedPost");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int len = posts.size();
                            for (int i = 0; i < len; i++) {
                                if (posts.get(i).getId().equals(post.getId())) {
                                    posts.set(i, post);
                                    break;
                                }
                            }

                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setPositionInList(0, 0);
                                    adaptNewPostsToFeed();
                                }
                            });
                        }
                    }).start();
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

    private boolean onAggregateAndGettingAggregate(Gate gate) {
        return currentGate == null && gate == null;
    }

    private boolean onGateAndGettingSameGate(Gate gate) {
        return currentGate != null && gate != null &&
               currentGate.getId().equals(gate.getId());
    }

    private void setCurrentPage(int page) {
        currentPage = page;
        infiniteScrollListener.setCurrentPage(currentPage);
    }

    public void expandCreatedPostLoading() {
        postLoading.setVisibility(View.VISIBLE);
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        postLoading.measure(widthSpec, heightSpec);

        ValueAnimator animator = slideAnimator(0, postLoading.getMeasuredHeight(), postLoading);
        animator.start();
    }

    public void collapseCreatedPostLoading() {
        int finalHeight = postLoading.getHeight();

        ValueAnimator animator = slideAnimator(finalHeight, 0, postLoading);
        animator.start();
    }

    private void showCreatePostDialog(Gate current, final ArrayList<Gate> gates, String postBody) {
        selectedGate = current;

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.title_dialog_create_post)
                .customView(R.layout.dialog_create_post, true)
                .positiveText("POST")
                .negativeText("CANCEL")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        dialog.dismiss();

                        EditText postInput = (EditText) dialog.getCustomView().findViewById(R.id.postInput);
                        createPost(selectedGate, postInput.getText().toString().trim());
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.dismiss();
                    }
                }).build();

        final View positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        final TextView gateSelection = (TextView) dialog.getCustomView().findViewById(R.id.gateSelection);
        final EditText postInput = (EditText) dialog.getCustomView().findViewById(R.id.postInput);

        if (!(postInput.getText().toString().trim().length() > 0)) positiveAction.setEnabled(false);

        postInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 0 && selectedGate != null) positiveAction.setEnabled(true);
                else positiveAction.setEnabled(false);
            }
        });

        if (postBody != null) postInput.append(postBody);

        gateSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gates.isEmpty()) {
                    gateSelection.setText("Loading Gates...");
                    gateSelection.setEnabled(false);

                    Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(final JSONObject response) {

                            new Thread(new Runnable() {

                              public void run() {
                                  gates.clear();

                                  JSONArray jsonGates = response.optJSONArray("gates");
                                  int len = jsonGates.length();
                                  for (int i = 0; i < len; i++) {
                                      JSONObject jsonGate = jsonGates.optJSONObject(i);

                                      String creator;

                                      if (jsonGate.optJSONObject("creator") != null) {
                                          creator = jsonGate.optJSONObject("creator").optString("name");
                                      } else {
                                          creator = "";
                                      }

                                      Gate gate = new Gate(jsonGate.optString("external_id"),
                                              jsonGate.optString("name"),
                                              jsonGate.optInt("users_count"),
                                              creator,
                                              jsonGate.optBoolean("generated"),
                                              jsonGate.optBoolean("session"),
                                              jsonGate.optBoolean("unlocked_perm"));

                                      gates.add(gate);
                                  }

                                  getActivity().runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          if (gates.isEmpty()) {
                                              Butter.between(getActivity(), "No Gates Unlocked\nUnlock a Gate so you can post");

                                            dialog.dismiss();
                                          } else {
                                              gateSelection.setText("Select a Gate");
                                              gateSelection.setEnabled(true);

                                              gateSelection.performClick();
                                          }
                                      }
                                  });
                              }

                            }).start();

                        }
                    };

                    Response.ErrorListener errorListener = new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Butter.between(getActivity(), "We couldn't load your Gates");

                            gateSelection.setText("Reload Gates");
                            gateSelection.setEnabled(true);
                        }
                    };

                    loadGates(listener, errorListener);
                } else {
                    final CharSequence[] items = new CharSequence[gates.size()];
                    for (int i = 0; i < items.length; i++) {
                        items[i] = gates.get(i).getName();
                    }

                     new MaterialDialog.Builder(getActivity())
                            .title("Select a Gate to post in")
                            .items(items)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, int which, CharSequence charSequence) {
                                    materialDialog.dismiss();

                                    if (which >= 0 && which < items.length) {
                                        selectedGate = gates.get(which);
                                        gateSelection.setText(selectedGate.getName());

                                        if (selectedGate != null && postInput.getText().toString().trim().length() > 0) positiveAction.setEnabled(true);
                                        else positiveAction.setEnabled(false);
                                    }
                                }
                            }).show();
                }
            }
        });

        dialog.show();

        if (gates.isEmpty()) {
            gateSelection.setText("Loading Gates...");
            gateSelection.setEnabled(false);

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {

                    new Thread(new Runnable() {

                        public void run() {
                            gates.clear();

                            JSONArray jsonGates = response.optJSONArray("gates");
                            int len = jsonGates.length();
                            for (int i = 0; i < len; i++) {
                                JSONObject jsonGate = jsonGates.optJSONObject(i);

                                String creator;

                                if (jsonGate.optJSONObject("creator") != null) {
                                    creator = jsonGate.optJSONObject("creator").optString("name");
                                } else {
                                    creator = "";
                                }

                                Gate gate = new Gate(jsonGate.optString("external_id"),
                                        jsonGate.optString("name"),
                                        jsonGate.optInt("users_count"),
                                        creator,
                                        jsonGate.optBoolean("generated"),
                                        jsonGate.optBoolean("session"),
                                        jsonGate.optBoolean("unlocked_perm"));

                                gates.add(gate);
                            }

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (gates.isEmpty()) {
                                        Butter.between(getActivity(), "No Gates Unlocked\nUnlock a Gate so you can post");

                                        dialog.dismiss();
                                    } else {
                                        gateSelection.setText("Select a Gate");
                                        gateSelection.setEnabled(true);
                                    }
                                }
                            });
                        }

                    }).start();

                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    Butter.between(getActivity(), "We couldn't load your Gates");

                    gateSelection.setText("Reload Gates");
                    gateSelection.setEnabled(true);
                }
            };

            loadGates(listener, errorListener);
        } else {
            if (selectedGate != null) gateSelection.setText(selectedGate.getName());
            else gateSelection.performClick();
        }
    }

    private void createPost(final Gate gate, String postContents) {
        // We only need to show a post is loading if we're on the same gate
        // or if we're in the Aggregate
        if (onGateAndGettingSameGate(gate) || currentGate == null) {
            feed.post(new Runnable() {
                @Override
                public void run() {
                    feed.setSelection(0);
                }
            });

            expandCreatedPostLoading();
        }

        final String postBody = postContents
                .replaceAll(RegexConstants.NEW_LINE, "\n")
                .replaceAll(RegexConstants.DOUBLE_SPACE, " ");

        try {
            JSONObject params = new JSONObject();

            JSONObject post = new JSONObject();
            post.put("body", postBody);

            params.put("post", post);

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    if (currentGate == null ||
                            onGateAndGettingSameGate(gate)) {

                        JSONObject jsonPost = response.optJSONObject("post");
                        Post post = new Post(jsonPost.optString("external_id"),
                                jsonPost.optJSONObject("user").optString("name"),
                                jsonPost.optString("body"),
                                jsonPost.optJSONObject("gate").optString("external_id"),
                                jsonPost.optJSONObject("gate").optString("name"),
                                jsonPost.optInt("comments_count"),
                                jsonPost.optInt("up_count"),
                                jsonPost.optBoolean("uped"),
                                jsonPost.optString("created_at"));

                        posts.add(0, post);
                        adaptNewPostsToFeed();

                        collapseCreatedPostLoading();
                    } else {
                        Butter.down(getActivity(), "Successfully posted to another Gate");
                    }
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    collapseCreatedPostLoading();
                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    Butter.down(getActivity(), volleyError.getMessage());
                    showCreatePostDialog(gate, getGates(), postBody);
                }
            };

            APIRequestManager.getInstance().doRequest().createPost(gate, params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void loadGates(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        JSONObject params = new JSONObject();

        APIRequestManager.getInstance().doRequest().getGates(params, listener, errorListener);
    }

    public static ValueAnimator slideAnimator(int start, int end, final ProgressBar postLoading) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                int value = (Integer) animation.getAnimatedValue();

                ViewGroup.LayoutParams layoutParams = postLoading.getLayoutParams();
                layoutParams.height = value;
                postLoading.setLayoutParams(layoutParams);
            }
        });

        return animator;
    }

    private ArrayList<Gate> getGates() {
        return ((MainActivity) getActivity()).getGates();
    }

}
