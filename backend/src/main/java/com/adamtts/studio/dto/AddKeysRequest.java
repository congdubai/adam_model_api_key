package com.adamtts.studio.dto;

public class AddKeysRequest {
    private String keys;

    public AddKeysRequest() {
    }

    public AddKeysRequest(String keys) {
        this.keys = keys;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }
}

