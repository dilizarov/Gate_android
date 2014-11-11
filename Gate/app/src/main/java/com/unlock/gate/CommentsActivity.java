package com.unlock.gate;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

        comments = new ArrayList<Comment>();
        requestCommentsAndPopulateListView();
    }

    private void instantiateViews() {
        postName          = (TextView) findViewById(R.id.name);
        postTimestamp     = (TextView) findViewById(R.id.timestamp);
        postBody          = (TextView) findViewById(R.id.body);
        postCommentsCount = (TextView) findViewById(R.id.commentsCount);

        commentsList      = getListView();
        addComment        = (EditText) findViewById(R.id.addComment);
        sendComment       = (Button) findViewById(R.id.sendComment);
    }

    private void setPostViews() {
        postName.setText(post.getName());
        postBody.setText(post.getBody());
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
