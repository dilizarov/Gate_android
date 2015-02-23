package com.unlock.gate;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.adapters.CommentsListAdapter;
import com.unlock.gate.models.Comment;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.ActionBarListActivity;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.CustomEditText;
import com.unlock.gate.utils.PostViewHelper;
import com.unlock.gate.utils.RegexConstants;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class CommentsActivity extends ActionBarListActivity {

    private View postHolder;
    private TextView postName;
    private TextView postGateName;
    private TextView postTimestamp;
    private TextView postBody;
    private TextView postCommentsCount;
    private TextView postUpCountPost;
    private TextView noCommentsMessage;
    private ListView commentsList;
    private LinearLayout postStats;
    private CustomEditText addComment;
    private ImageButton sendComment;
    private ImageView upPost;
    private ImageView postSmileyCount;
    private ImageView postCommentsCountBubble;

    private ProgressBar commentLoading;

    private Menu menu;
    private MenuItem refreshButton;

    private Post post;
    private ArrayList<Comment> comments;
    private ArrayList<Comment> adapterComments;
    private CommentsListAdapter listAdapter;
    private boolean creatingComment;

    private boolean notification;

    private LinearLayout progressBarHolder;

    private final int BODY_CUTOFF = 220;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));

        instantiateViews();

        Intent intent = getIntent();

        post = intent.getParcelableExtra("post");
        notification = intent.getBooleanExtra("commentsActivityNotification", false);
        creatingComment = intent.getBooleanExtra("creatingComment", false);
        setPostViews();

        setPostBodyClickListener();
        setSendCommentClickListener();
        setAddCommentTypeListener();

        PostViewHelper.handleUpBehavior(this, post, upPost, postUpCountPost, postSmileyCount, postStats);
        PostViewHelper.handleCommentBehavior(post, postCommentsCount, postCommentsCountBubble, postStats);

        comments = new ArrayList<Comment>();
        adapterComments = new ArrayList<Comment>();

        if (savedInstanceState != null) {
            comments = savedInstanceState.getParcelableArrayList("comments");
            adaptNewCommentsToList();
            progressBarHolder.setVisibility(View.GONE);
        } else {
            requestCommentsAndPopulateListView(false);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (creatingComment) {
            showKeyboard(addComment);
        }

        /*
        When in landscape mode on device, if there isn't enough space
        to see a reasonable amount of comments, get rid of the post.
        There are more definitive ways to get if the phone is in landscape mode
        but it is buggy across different devices.
         */
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        postHolder.post(new Runnable() {
            @Override
            public void run() {
                if (displayMetrics.widthPixels > displayMetrics.heightPixels &&
                    postHolder.getHeight() * 4 > displayMetrics.heightPixels) {
                    postHolder.setVisibility(View.GONE);
                }
            }
        });
    }

    private void instantiateViews() {
        postHolder        = findViewById(R.id.post);
        postName          = (TextView) findViewById(R.id.name);
        postGateName   = (TextView) findViewById(R.id.gateName);
        postTimestamp     = (TextView) findViewById(R.id.timestamp);
        postBody          = (TextView) findViewById(R.id.body);
        postUpCountPost   = (TextView) findViewById(R.id.upCountPost);
        postCommentsCount = (TextView) findViewById(R.id.commentsCount);

        upPost                  = (ImageView) findViewById(R.id.upPost);
        postCommentsCountBubble = (ImageView) findViewById(R.id.commentsCountBubble);
        postSmileyCount         = (ImageView) findViewById(R.id.smileyCount);
        findViewById(R.id.createComment).setVisibility(View.GONE);

        postStats         = (LinearLayout) findViewById(R.id.postStats);

        noCommentsMessage = (TextView) findViewById(R.id.noCommentsMessage);
        commentsList      = getListView();
        addComment        = (CustomEditText) findViewById(R.id.addComment);
        sendComment       = (ImageButton) findViewById(R.id.sendComment);
        if (!(addComment.getText().toString().trim().length() > 0))
            sendComment.setEnabled(false);

        commentLoading = (ProgressBar) findViewById(R.id.commentLoading);

        progressBarHolder = (LinearLayout) findViewById(R.id.commentsProgressBarHolder);

        addComment.setFilters(new InputFilter[] { new InputFilter.LengthFilter(500)});
    }

    private void setPostViews() {
        postName.setText(post.getName());
        postGateName.setText(post.getGateName());

        postBody.setText(
                (post.getBody().length() > BODY_CUTOFF) ? cutoffBody() : post.getBody()
        );

        postTimestamp.setText(post.getTimestamp());
    }

    private void adaptNewCommentsToList() {

        noCommentsMessage.setText(R.string.no_comments_default);
        noCommentsMessage.setVisibility(
                comments.size() == 0
                ? View.VISIBLE
                : View.GONE
        );

        if (listAdapter == null) {
            listAdapter = new CommentsListAdapter(CommentsActivity.this, adapterComments);
            commentsList.setAdapter(listAdapter);
        }

        adapterComments.clear();
        adapterComments.addAll(comments);
        listAdapter.notifyDataSetChanged();

    }

    private void requestCommentsAndPopulateListView(final boolean refreshing) {

        JSONObject params = new JSONObject();

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                final JSONObject jsonResponse = response;

                new Thread(new Runnable() {

                    public void run() {
                        comments.clear();

                        JSONArray jsonComments = jsonResponse.optJSONArray("comments");
                        int len = jsonComments.length();
                        for (int i = 0; i < len; i++) {
                            JSONObject jsonComment = jsonComments.optJSONObject(i);
                            Comment comment = new Comment(jsonComment.optString("external_id"),
                                    jsonComment.optJSONObject("user").optString("name"),
                                    jsonComment.optString("body"),
                                    jsonComment.optInt("up_count"),
                                    jsonComment.optBoolean("uped"),
                                    jsonComment.optString("created_at"));

                            comments.add(comment);
                        }

                        runOnUiThread(new Runnable() {
                            public void run() {
                                adaptNewCommentsToList();
                                progressBarHolder.setVisibility(View.GONE);

                                if (notification) commentsList.setSelection(
                                        listAdapter.getCount() - 1);

                                if (refreshing && refreshButton != null) {
                                    commentsList.setSelection(listAdapter.getCount() - 1);
                                    refreshButton.setActionView(null);
                                }

                                handleCommentCount(false);
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
                progressBarHolder.setVisibility(View.GONE);

                if (comments.size() == 0) {
                    if (volleyError.isConnectionError())
                        noCommentsMessage.setText(R.string.volley_no_connection_error);
                    else
                        noCommentsMessage.setText(R.string.gate_error_message);

                    noCommentsMessage.setVisibility(View.VISIBLE);
                }

                Butter.down(CommentsActivity.this, volleyError.getMessage());

                if (refreshing && refreshButton != null) {
                    refreshButton.setActionView(null);
                }
            }
        };

        APIRequestManager.getInstance().doRequest().getComments(post, params, listener, errorListener);
    }

    private void setSendCommentClickListener() {
        sendComment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String comment = addComment.getText().toString().trim();

                if (comment.length() == 0) {
                    addComment.setError(getString(R.string.no_comment_inputted));
                } else if (comment.length() > 500) {
                    addComment.setError(getString(R.string.over_500_characters_inputted));
                } else {
                    sendComment(comment);
                }
            }
        });
    }

    private void setPostBodyClickListener() {
        postBody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String body = postBody.getText().toString();

                if (body.length() < BODY_CUTOFF) return;
                else if (body.length() == BODY_CUTOFF && body.substring(BODY_CUTOFF - 3, BODY_CUTOFF).equals("...")) {
                    postBody.setText(post.getBody());
                } else {
                    postBody.setText(cutoffBody());
                }
            }
        });
    }

    private void setAddCommentTypeListener() {
        addComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (addComment.getError() != null) addComment.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 0) {
                    sendComment.setEnabled(true);
                    sendComment.setImageResource(R.drawable.ic_content_send);
                } else {
                    sendComment.setEnabled(false);
                    sendComment.setImageResource(R.drawable.ic_greyed_out_content_send);
                }
            }
        });
    }

    private void sendComment(final String comment) {
        try {
            hideKeyboard();
            addComment.setText("");

            toggleLoadingComment(true);

            JSONObject params = new JSONObject();

            JSONObject commentJson = new JSONObject();
            commentJson.put("body", comment.replaceAll(RegexConstants.NEW_LINE, "\n")
                    .replaceAll(RegexConstants.DOUBLE_SPACE, " "));

            params.put("comment", commentJson);

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    toggleLoadingComment(false);

                    JSONObject jsonComment = response.optJSONObject("comment");
                    Comment comment = new Comment(
                            jsonComment.optString("external_id"),
                            jsonComment.optJSONObject("user").optString("name"),
                            jsonComment.optString("body"),
                            jsonComment.optInt("up_count"),
                            jsonComment.optBoolean("uped"),
                            jsonComment.optString("created_at"));

                    comments.add(comment);

                    adaptNewCommentsToList();
                    commentsList.setSelection(listAdapter.getCount() - 1);

                    handleCommentCount(true);
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    toggleLoadingComment(false);

                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    addComment.append(comment);
                    showKeyboard(addComment);

                    Butter.down(CommentsActivity.this, volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().createComment(post, params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private String cutoffBody() {
        return post.getBody().substring(0, 217) + "...";
    }

    private void handleCommentCount(boolean creating) {
        boolean wasCommentCountZero = post.getCommentCount() == 0;

        post.setCommentCount(creating ? post.getCommentCount() + 1 : comments.size());
        postCommentsCount.setText(Integer.toString(post.getCommentCount()));

        if (wasCommentCountZero &&
            post.getCommentCount() > 0) {
            postCommentsCount.setVisibility(View.VISIBLE);
            postCommentsCountBubble.setVisibility(View.VISIBLE);

            if (post.getUpCount() == 0) {
                PostViewHelper.expandPostStats(postStats);
            }
        }
    }

    private void showKeyboard(View view) {
        view.requestFocus();
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(addComment, 0);
            }
        }, 200);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    private void toggleLoadingComment(boolean loading) {
        if (loading) {
            addComment.setVisibility(View.INVISIBLE);
            sendComment.setVisibility(View.INVISIBLE);
            commentLoading.setVisibility(View.VISIBLE);
        } else {
            commentLoading.setVisibility(View.GONE);
            addComment.setVisibility(View.VISIBLE);
            sendComment.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("comments", comments);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_comments, menu);
        this.menu = menu;

        return true;
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        intent.putExtra("updatedPost", post);
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_refresh_comments:
                if (refreshButton == null) refreshButton = item;
                ProgressBar progressBar = new ProgressBar(this);
                progressBar.setIndeterminate(true);
                progressBar.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_action_bar));
                refreshButton.setActionView(progressBar);
                requestCommentsAndPopulateListView(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

