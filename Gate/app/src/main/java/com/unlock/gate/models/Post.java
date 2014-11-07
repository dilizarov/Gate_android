package com.unlock.gate.models;

import android.text.format.DateUtils;

import org.joda.time.DateTime;

/**
 * Created by davidilizarov on 10/22/14.
 */
public class Post {
    private String id, name, body, networkId, timestamp;
    private int commentCount;
    private DateTime timeCreated;

    public Post(){}

    public Post(String id, String name, String body, String networkId, int commentCount, String timeCreated) {
        this.id = id;
        this.name = name;
        this.body = body;
        this.networkId = networkId;
        this.commentCount = commentCount;
        this.timeCreated = new DateTime(timeCreated);
        setTimestamp();
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public DateTime getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = new DateTime(timeCreated);
    }

    private void setTimestamp() {
        timestamp = DateUtils.getRelativeTimeSpanString(timeCreated.getMillis(),
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS).toString();
    }
}
