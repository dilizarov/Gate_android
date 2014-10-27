package com.unlock.gate.models;

import android.text.format.DateUtils;

import org.joda.time.DateTime;

/**
 * Created by davidilizarov on 10/22/14.
 */
public class FeedItem {
    private String id, name, message, networkName, timestamp;
    private DateTime timeCreated;

    public FeedItem(){}

    public FeedItem(String id, String name, String message, String networkName, String timeCreated) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.networkName = networkName;
        this.timeCreated = new DateTime(timeCreated);
        setTimestamp();
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
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
