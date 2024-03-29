package com.mct.app.helper.native_rcv;

public class User {

    private String name;

    public User(int index) {
        this.name = "User " + index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
