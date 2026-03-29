package com.nduyuwilson.thitima.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "payments",
        foreignKeys = @ForeignKey(entity = Project.class,
                parentColumns = "id",
                childColumns = "projectId",
                onDelete = ForeignKey.CASCADE))
public class Payment {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int projectId;
    private double amount;
    private long date;
    private String method; // e.g., "Cash", "M-Pesa", "Bank"
    private String reference; // Transaction ID

    public Payment(int projectId, double amount, String method, String reference) {
        this.projectId = projectId;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
        this.date = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
