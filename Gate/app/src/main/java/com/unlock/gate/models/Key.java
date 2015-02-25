package com.unlock.gate.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Hours;

import java.util.ArrayList;

/**
 * Created by davidilizarov on 2/24/15.
 */
public class Key implements Parcelable {
    private String key;
    private DateTime timeUpdated;
    private ArrayList<String> gateNames;

    public Key(){}

    public Key(String key, String timeUpdated, ArrayList<String> gateNames) {
        this.key = key;
        this.timeUpdated = new DateTime(timeUpdated);
        this.gateNames = gateNames;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public DateTime getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(DateTime timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    public ArrayList<String> getGateNames() {
        return gateNames;
    }

    public void setGateNames(ArrayList<String> gateNames) {
        this.gateNames = gateNames;
    }

    public Boolean expiresSoon() {
        DateTime threeDaysAgo = DateTime.now().minusDays(3);
        return Hours.hoursBetween(threeDaysAgo, timeUpdated).getHours() <= 24;
    }

    public Boolean expired() {
        DateTime threeDaysAgo = DateTime.now().minusDays(3);

        DateTimeComparator comparator = DateTimeComparator.getInstance();

        return comparator.compare(threeDaysAgo, timeUpdated) == 1;
    }

    public String expireTime() {
        DateTime threeDaysAgo = DateTime.now().minusDays(3);

        DateTimeComparator comparator = DateTimeComparator.getInstance();

        if (comparator.compare(threeDaysAgo, timeUpdated.minusMinutes(1)) == 1) {
            return "less than a minute";
        } else if (comparator.compare(threeDaysAgo, timeUpdated.minusHours(1)) == 1) {
            return "less than an hour";
        } else {
            int hours = Hours.hoursBetween(threeDaysAgo, timeUpdated).getHours();
            if (hours <= 0) {
                return "less than an hour";
            } else if (hours == 1) {
                return "1 hour";
            } else if (hours > 1) {
                return hours + " hours";
            }
        }
        
        return "over a day";
    }

    public String gatesList() {
        final StringBuilder gatesBuilder = new StringBuilder();
        int len = gateNames.size();
        if (len == 1) {
            gatesBuilder.append(gateNames.get(0));
        } else if (len == 2) {
            gatesBuilder.append(gateNames.get(0))
                    .append(" and ")
                    .append(gateNames.get(1));
        } else if (len > 2) {
            for (int i = 0; i < len; i++) {
                if (i != 0) gatesBuilder.append(", ");
                if (i == len - 1) gatesBuilder.append("and ");
                gatesBuilder.append(gateNames.get(i));
            }
        }

        return gatesBuilder.toString();
    }

    protected Key(Parcel in) {
        key = in.readString();
        timeUpdated = (DateTime) in.readValue(DateTime.class.getClassLoader());
        if (in.readByte() == 0x01) {
            gateNames = new ArrayList<String>();
            in.readList(gateNames, String.class.getClassLoader());
        } else {
            gateNames = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeValue(timeUpdated);
        if (gateNames == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(gateNames);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Key> CREATOR = new Parcelable.Creator<Key>() {
        @Override
        public Key createFromParcel(Parcel in) {
            return new Key(in);
        }

        @Override
        public Key[] newArray(int size) {
            return new Key[size];
        }
    };
}
