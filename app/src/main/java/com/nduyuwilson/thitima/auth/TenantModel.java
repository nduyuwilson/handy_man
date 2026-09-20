package com.nduyuwilson.thitima.auth;

import com.google.firebase.Timestamp;

public class TenantModel {
    private String uid;
    private String email;
    private boolean isPremium;
    private String deviceId;
    private Timestamp createdAt;

    public TenantModel() {}

    public TenantModel(String uid, String email, boolean isPremium, String deviceId, Timestamp createdAt) {
        this.uid = uid;
        this.email = email;
        this.isPremium = isPremium;
        this.deviceId = deviceId;
        this.createdAt = createdAt;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
