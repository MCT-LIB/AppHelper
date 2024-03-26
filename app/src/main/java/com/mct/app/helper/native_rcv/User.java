package com.mct.app.helper.native_rcv;

import java.util.UUID;

public class User {

    private final String name;

    public User(int index) {
        this.name = "User " + index;
    }

    public String getName() {
        return name;
    }
}
