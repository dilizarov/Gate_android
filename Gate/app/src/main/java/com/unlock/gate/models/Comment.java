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
 * Reason for lots of String use same as Post
 */
public class Comment implements Parcelable {
    private String id, name, body, upCount, uped, timestamp;
    private DateTime timeCreated;

    public Comment(){}

    public Comment(String id, String name, String body, int upCount, boolean uped, String timeCreated) {
        this.id = id;
        this.name = name;
        this.body = body;
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
        if (DateTimeComparator.getInstance().compare(timeCreated, now.minusSeconds(1)) == 1) {
            timestamp = "1s";
        } else if (DateTimeComparator.getInstance().compare(timeCreated, now.minusMinutes(1)) == 1) {
            timestamp = Seconds.secondsBetween(timeCreated, now).getSeconds() + "s";
        } else if (DateTimeComparator.getInstance().compare(timeCreated, now.minusHours(1)) == 1) {
            timestamp = Minutes.minutesBetween(timeCreated, now).getMinutes() + "m";
        } else if (DateTimeComparator.getInstance().compare(timeCreated, now.minusDays(1)) == 1) {
            timestamp = Hours.hoursBetween(timeCreated, now).getHours() + "h";
        } else if (DateTimeComparator.getInstance().compare(timeCreated, now.minusWeeks(1)) == 1) {
            timestamp = Days.daysBetween(timeCreated, now).getDays() + "d";
        } else if (DateTimeComparator.getInstance().compare(timeCreated, now.minusYears(1)) == 1) {
            timestamp = Weeks.weeksBetween(timeCreated, now).getWeeks() + "w";
        } else {
            timestamp = Years.yearsBetween(timeCreated, now).getYears() + "y";
        }

//        DateTime tenSecondsAgo = DateTime.now().minusSeconds(10);
//        if (DateTimeComparator.getInstance().compare(timeCreated, tenSecondsAgo) == 1)
//            timestamp = "moments ago";
//        else {
//            timestamp = DateUtils.getRelativeTimeSpanString(timeCreated.getMillis(),
//                    System.currentTimeMillis(),
//                    DateUtils.SECOND_IN_MILLIS).toString();
//
//        }
    }

    //Parcelable implementation
    public Comment(Parcel in) {
        String[] data = new String[6];

        in.readStringArray(data);

        this.id      = data[0];
        this.name    = data[1];
        this.body    = data[2];
        this.upCount = data[3];
        this.uped    = data[4];
        this.timeCreated = DateTime.parse(data[5]);
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
                this.upCount,
                this.uped,
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
