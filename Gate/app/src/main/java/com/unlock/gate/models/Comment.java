package com.unlock.gate.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import org.joda.time.DateTime;

/**
 * Created by davidilizarov on 11/10/14.
 */
public class Comment implements Parcelable {
    private String id, name, body, timestamp;
    private DateTime timeCreated;

    public Comment(){}

    public Comment(String id, String name, String body, String timeCreated) {
        this.id = id;
        this.name = name;
        this.body = body;
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

    //Parcelable implementation
    public Comment(Parcel in) {
        String[] data = new String[4];

        in.readStringArray(data);

        this.id   = data[0];
        this.name = data[1];
        this.body = data[2];
        this.timeCreated = DateTime.parse(data[3]);
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
                this.timeCreated.toString()
        });
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

}
