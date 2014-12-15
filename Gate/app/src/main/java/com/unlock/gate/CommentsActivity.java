package com.unlock.gate;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.adapters.CommentsListAdapter;
import com.unlock.gate.models.Comment;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.PostViewHelper;
import com.unlock.gate.utils.SetErrorBugFixer;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class CommentsActivity extends ListActivity {

    private TextView postName;
    private TextView postNetworkName;
    private TextView postTimestamp;
    private TextView postBody;
    private TextView postCommentsCount;
    private TextView postUpCountPost;
    private ListView commentsList;
    private LinearLayout postStats;
    private EditText addComment;
    private Button sendComment;
    private ImageView upPost;
    private ImageView postSmileyCount;
    private ImageView postCommentsCountBubble;


    private Post post;
    private ArrayList<Comment> comments;
    private ArrayList<Comment> adapterComments;
    private CommentsListAdapter listAdapter;
    private SharedPreferences mSessionPreferences;
    private boolean creatingComment;

    private LinearLayout progressBarHolder;

    private final int BODY_CUTOFF = 220;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        mSessionPreferences = getSharedPreferences(
                getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        instantiateViews();

        Intent intent = getIntent();
        post = (Post) intent.getParcelableExtra("post");
        creatingComment = intent.getBooleanExtra("creatingComment", false);
        setPostViews();

        setPostBodyClickListener();
        setSendCommentClickListener();

        PostViewHelper.handleUpBehavior(this, post, upPost, postUpCountPost, postSmileyCount, postStats);
        PostViewHelper.handleCommentBehavior(this, post, null, postCommentsCount, postCommentsCountBubble);

        comments = new ArrayList<Comment>();
        adapterComments = new ArrayList<Comment>();
        listAdapter = new CommentsListAdapter(CommentsActivity.this, adapterComments);
        commentsList.setAdapter(listAdapter);

        if (savedInstanceState != null) {
            comments = savedInstanceState.getParcelableArrayList("comments");
            adapterComments.addAll(comments);
            listAdapter.notifyDataSetChanged();
            progressBarHolder.setVisibility(View.GONE);
        } else {
            requestCommentsAndPopulateListView(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (creatingComment) {
            addComment.requestFocus();
            addComment.postDelayed(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    keyboard.showSoftInput(addComment, 0);
                }
            }, 200);
        }

    }

    private void instantiateViews() {
        postName          = (TextView) findViewById(R.id.name);
        postNetworkName   = (TextView) findViewById(R.id.networkName);
        postTimestamp     = (TextView) findViewById(R.id.timestamp);
        //postCommentsCount = (TextView) findViewById(R.id.commentsCount);
        postBody          = (TextView) findViewById(R.id.body);
        postUpCountPost   = (TextView) findViewById(R.id.upCountPost);
        postCommentsCount = (TextView) findViewById(R.id.commentsCount);

        upPost                  = (ImageView) findViewById(R.id.upPost);
        postCommentsCountBubble = (ImageView) findViewById(R.id.commentsCountBubble);
        postSmileyCount         = (ImageView) findViewById(R.id.smileyCount);
        findViewById(R.id.createComment).setVisibility(View.GONE);

        postStats         = (LinearLayout) findViewById(R.id.postStats);

        commentsList      = getListView();
        addComment        = (EditText) findViewById(R.id.addComment);
        sendComment       = (Button) findViewById(R.id.sendComment);

        progressBarHolder = (LinearLayout) findViewById(R.id.commentsProgressBarHolder);

        addComment.addTextChangedListener(new SetErrorBugFixer(addComment));
        addComment.setFilters(new InputFilter[] { new InputFilter.LengthFilter(500)});
    }

    private void setPostViews() {
        postName.setText(post.getName());
        postNetworkName.setText(post.getNetworkName());

        postBody.setText(
                (post.getBody().length() > BODY_CUTOFF) ? cutoffBody() : post.getBody()
        );

        postTimestamp.setText(post.getTimestamp());
        /*postCommentsCount.setText(getResources()
                .getQuantityString(R.plurals.comments_count,
                        post.getCommentCount(),
                        post.getCommentCount()));*/
    }

    private void requestCommentsAndPopulateListView(final boolean refreshing) {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null))
                  .put("post_id", post.getId());

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
                                    adapterComments.clear();
                                    adapterComments.addAll(comments);
                                    listAdapter.notifyDataSetChanged();
                                    progressBarHolder.setVisibility(View.GONE);

                                    if (refreshing) {
                                        commentsList.setSelection(listAdapter.getCount() - 1);
                                    }
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
                    Log.v("Error", "nooo");
                }
            };

            APIRequestManager.getInstance().doRequest().getComments(params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void setSendCommentClickListener() {
        sendComment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String comment = addComment.getText().toString();

                if (comment.trim().length() == 0) {
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

                Log.v("Body Size", Integer.toString(body.length()));

                if (body.length() < BODY_CUTOFF) return;
                else if (body.length() == BODY_CUTOFF && body.substring(BODY_CUTOFF - 3, BODY_CUTOFF).equals("...")) {
                    postBody.setText(post.getBody());
                } else {
                    postBody.setText(cutoffBody());
                }
            }
        });
    }

    private void sendComment(String comment) {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

            JSONObject commentJson = new JSONObject();
            commentJson.put("body", comment);

            params.put("comment", commentJson);

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                    JSONObject jsonComment = response.optJSONObject("comment");
                    Comment comment = new Comment(
                            jsonComment.optString("external_id"),
                            jsonComment.optJSONObject("user").optString("name"),
                            jsonComment.optString("body"),
                            jsonComment.optInt("up_count"),
                            jsonComment.optBoolean("uped"),
                            jsonComment.optString("created_at"));

                    comments.add(comment);

                    adapterComments.clear();
                    adapterComments.addAll(comments);
                    listAdapter.notifyDataSetChanged();
                    commentsList.setSelection(listAdapter.getCount() - 1);
                    addComment.setText("");
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    //Crouton stuff.

                    Log.v("Error", "nooo");
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("comments", comments);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_comments, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_refresh_comments:
                adapterComments.clear();
                listAdapter.notifyDataSetChanged();
                progressBarHolder.setVisibility(View.VISIBLE);
                requestCommentsAndPopulateListView(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
