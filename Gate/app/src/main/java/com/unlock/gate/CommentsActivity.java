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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.adapters.CommentsListAdapter;
import com.unlock.gate.models.Comment;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.SetErrorBugFixer;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class CommentsActivity extends ListActivity {

    private TextView postName;
    private TextView postTimestamp;
    private TextView postBody;
    private TextView postCommentsCount;
    private ListView commentsList;
    private EditText addComment;
    private Button sendComment;

    private Post post;
    private ArrayList<Comment> comments;
    private CommentsListAdapter listAdapter;
    private SharedPreferences mSessionPreferences;

    private final int bodyCutoff = 220;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        mSessionPreferences = getSharedPreferences(
                getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        instantiateViews();

        Intent intent = getIntent();
        post = (Post) intent.getParcelableExtra("post");
        setPostViews();

        setPostBodyClickListener();
        setSendCommentClickListener();

        comments = new ArrayList<Comment>();
        requestCommentsAndPopulateListView();
    }

    private void instantiateViews() {
        postName          = (TextView) findViewById(R.id.name);
        postTimestamp     = (TextView) findViewById(R.id.timestamp);
        postCommentsCount = (TextView) findViewById(R.id.commentsCount);
        postBody          = (TextView) findViewById(R.id.body);

        commentsList      = getListView();
        addComment        = (EditText) findViewById(R.id.addComment);
        sendComment       = (Button) findViewById(R.id.sendComment);

        addComment.addTextChangedListener(new SetErrorBugFixer(addComment));
        addComment.setFilters(new InputFilter[] { new InputFilter.LengthFilter(500)});
    }

    private void setPostViews() {
        postName.setText(post.getName());

        postBody.setText(
                (post.getBody().length() > bodyCutoff) ? cutoffBody() : post.getBody()
        );

        postTimestamp.setText(post.getTimestamp());
        postCommentsCount.setText(getResources()
                .getQuantityString(R.plurals.comments_count,
                        post.getCommentCount(),
                        post.getCommentCount()));
    }

    private void requestCommentsAndPopulateListView() {
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
                                    listAdapter = new CommentsListAdapter(CommentsActivity.this, comments);
                                    commentsList.setAdapter(listAdapter);
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
                if (body.length() < bodyCutoff) return;
                else if (body.length() == bodyCutoff && body.substring(216, 219).equals("...")) {
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

                    listAdapter = new CommentsListAdapter(CommentsActivity.this, comments);
                    commentsList.setAdapter(listAdapter);
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
        return post.getBody().substring(0, 216) + "...";
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
        }

        return super.onOptionsItemSelected(item);
    }
}
