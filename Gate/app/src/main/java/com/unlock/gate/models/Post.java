package com.unlock.gate.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

/**
 * For the most part, everything is stored as a String because it makes the Parcelable
 * implementation much simpler. Conversions when getting and setting are easy.
 */
public class Post implements Parcelable {
    private String id, name, body, networkId, networkName, commentCount, upCount, uped, timestamp;
    private DateTime timeCreated;

    public Post(){}

    public Post(String id, String name, String body, String networkId, String networkName, int commentCount, int upCount, boolean uped, String timeCreated) {
        this.id = id;
        this.name = name;
        this.body = body;
        this.networkId = networkId;
        this.networkName = networkName;
        this.commentCount = Integer.toString(commentCount);
        this.upCount = Integer.toString(upCount);
        this.uped = Boolean.toString(uped);
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

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public int getCommentCount() {
        return Integer.parseInt(commentCount);
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = Integer.toString(commentCount);
    }

    public int getUpCount() {
        return Integer.parseInt(upCount);
    }

    public void setUpCount(int upCount) {
        this.upCount = Integer.toString(upCount);
    }

    public boolean getUped() {
        return Boolean.parseBoolean(uped);
    }

    public void setUped(boolean uped) {
        this.uped = Boolean.toString(uped);
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
        DateTime tenSecondsAgo = DateTime.now().minusSeconds(10);
        if (DateTimeComparator.getInstance().compare(timeCreated, tenSecondsAgo) == 1)
            timestamp = "moments ago";
        else {
            timestamp = DateUtils.getRelativeTimeSpanString(timeCreated.getMillis(),
                    System.currentTimeMillis(),
                    DateUtils.SECOND_IN_MILLIS).toString();

        }
    }

    //Parcelable implementation
    public Post(Parcel in) {
        String[] data = new String[9];

        in.readStringArray(data);

        this.id           = data[0];
        this.name         = data[1];
        this.body         = data[2];
        this.networkId    = data[3];
        this.networkName  = data[4];
        this.commentCount = data[5];
        this.upCount      = data[6];
        this.uped         = data[7];
        this.timeCreated = DateTime.parse(data[8]);
        setTimestamp();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {
            this.id,
            this.name,
            this.body,
            this.networkId,
            this.networkName,
            this.commentCount,
            this.upCount,
            this.uped,
            this.timeCreated.toString()
        });
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        public Post[] newArray(int size) {
            return new Post[size];
        }
    };
}
