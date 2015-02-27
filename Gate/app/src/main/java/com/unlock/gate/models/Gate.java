package com.unlock.gate.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

/**
 * Created by davidilizarov on 10/27/14.
 */
public class Gate implements Parcelable {
    private String id, name, usersCount, creator;

    public Gate(){}

    // NOTE:
    // This is used when showing appropriate feed based on Gate from notification.
    // I don't pass in the usersCount nor the creator. usersCount isn't used in FeedFragment
    // and creator isn't used at all right now.
    public Gate(String id, String name) {
        this.id = id;
        this.name = name;
        this.usersCount = "";
        this.creator = "";
    }

    public Gate(String id, String name, int usersCount, String creator) {
        this.id = id;
        this.name = name;
        this.usersCount = Integer.toString(usersCount);
        this.creator = creator;
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

    public int getUsersCount() {
        return Integer.parseInt(usersCount);
    }

    public void setUsersCount(int usersCount) {
        this.usersCount = Integer.toString(usersCount);
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String serialize() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    static public Gate deserialize(String serializedGate) {
        if (serializedGate == null) return null;

        Gson gson = new Gson();
        return gson.fromJson(serializedGate, Gate.class);
    }

    //Parcelable implementation
    public Gate(Parcel in) {
        String[] data = new String[4];

        in.readStringArray(data);

        this.id         = data[0];
        this.name       = data[1];
        this.usersCount = data[2];
        this.creator    = data[3];
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
                this.usersCount,
                this.creator
        });
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Gate createFromParcel(Parcel in) {
            return new Gate(in);
        }

        public Gate[] newArray(int size) {
            return new Gate[size];
        }
    };
}
