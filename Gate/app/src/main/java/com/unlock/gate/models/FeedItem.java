package com.unlock.gate.models;

/**
 * Created by davidilizarov on 10/22/14.
 */
public class FeedItem {
    private String id, name, message, networkName, timestamp;

    public FeedItem(){}

    public FeedItem(String id, String name, String message, String networkName, String timestamp) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.networkName = networkName;
        this.timestamp = timestamp;
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

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
