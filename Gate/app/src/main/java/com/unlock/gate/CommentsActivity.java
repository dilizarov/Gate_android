package com.unlock.gate;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.unlock.gate.models.Comment;
import com.unlock.gate.models.Post;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        instantiateViews();

        Intent intent = getIntent();
        post = (Post) intent.getParcelableExtra("post");
        setPostViews();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_comments, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
