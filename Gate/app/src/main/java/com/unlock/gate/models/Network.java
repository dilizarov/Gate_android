package com.unlock.gate.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by davidilizarov on 10/27/14.
 */
public class Network implements Parcelable {
    private String id, name, creator;

    public Network(){}

    public Network(String id, String name, String creator) {
        this.id = id;
        this.name = name;
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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    //Parcelable implementation
    public Network(Parcel in) {
        String[] data = new String[3];

        in.readStringArray(data);

        this.id      = data[0];
        this.name    = data[1];
        this.creator = data[2];
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
                                            this.creator
                                            });
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Network createFromParcel(Parcel in) {
            return new Network(in);
        }

        public Network[] newArray(int size) {
            return new Network[size];
        }
    };
}
