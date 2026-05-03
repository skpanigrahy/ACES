package com.aces.common.model;

import java.util.Map;

public class Signal {

    private String type;
    private String seal;
    private Map<String, String> payload;

    public Signal() {
    }

    public Signal(String type, String seal, Map<String, String> payload) {
        this.type = type;
        this.seal = seal;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeal() {
        return seal;
    }

    public void setSeal(String seal) {
        this.seal = seal;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, String> payload) {
        this.payload = payload;
    }
}
