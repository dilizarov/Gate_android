package com.unlock.gate.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.joda.time.Weeks;
import org.joda.time.Years;

/**
 * For the most part, everything is stored as a String because it makes the Parcelable
 * implementation much simpler. Conversions when getting and setting are easy.
 */
public class Post implements Parcelable {
    private String id, name, body, gateId, gateName, commentCount, upCount, uped, timestamp;
    private DateTime timeCreated;

    public Post(){}

    public Post(String id, String name, String body, String gateId, String gateName, int commentCount, int upCount, boolean uped, String timeCreated) {
        this.id = id;
        this.name = name;
        this.body = body;
        this.gateId = gateId;
        this.gateName = gateName;
        this.commentCount = Integer.toString(commentCount);
        this.upCount = Integer.toString(upCount);
        this.uped = Boolean.toString(uped);
        this.timeCreated = new DateTime(timeCreated);
        setTimestamp();
    }

    public Post(String id, String name, String body, String gateId, String gateName, String commentCount, String upCount, String uped, String timeCreated) {
        this.id = id;
        this.name = name;
        this.body = body;
        this.gateId = gateId;
        this.gateName = gateName;
        this.commentCount = commentCount;
        this.upCount = upCount;
        this.uped = uped;
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

    public String getGateId() {
        return gateId;
    }

    public void setGateId(String gateId) {
        this.gateId = gateId;
    }

    public String getGateName() {
        return gateName;
    }

    public void setGateName(String gateName) {
        this.gateName = gateName;
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
        DateTime now = DateTime.now();
        DateTimeComparator comparator = DateTimeComparator.getInstance();

        if (comparator.compare(timeCreated, now.minusSeconds(1)) == 1) {
           timestamp = "1s";
        } else if (comparator.compare(timeCreated, now.minusMinutes(1)) == 1) {
            timestamp = Seconds.secondsBetween(timeCreated, now).getSeconds() + "s";
        } else if (comparator.compare(timeCreated, now.minusHours(1)) == 1) {
            timestamp = Minutes.minutesBetween(timeCreated, now).getMinutes() + "m";
        } else if (comparator.compare(timeCreated, now.minusDays(1)) == 1) {
            timestamp = Hours.hoursBetween(timeCreated, now).getHours() + "h";
        } else if (comparator.compare(timeCreated, now.minusWeeks(1)) == 1) {
            timestamp = Days.daysBetween(timeCreated, now).getDays() + "d";
        } else if (comparator.compare(timeCreated, now.minusYears(1)) == 1) {
            timestamp = Weeks.weeksBetween(timeCreated, now).getWeeks() + "w";
        } else {
            timestamp = Years.yearsBetween(timeCreated, now).getYears() + "y";
        }
    }

    //Parcelable implementation
    public Post(Parcel in) {
        String[] data = new String[9];

        in.readStringArray(data);

        this.id           = data[0];
        this.name         = data[1];
        this.body         = data[2];
        this.gateId       = data[3];
        this.gateName     = data[4];
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
            this.gateId,
            this.gateName,
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
