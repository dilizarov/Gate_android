package com.unlock.gate.models;

/**
 * Created by davidilizarov on 10/27/14.
 */
public class Network {
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
}
