package com.nduyuwilson.thitima.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class Project {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String location;
    private String description;
    private String clientName;
    private String clientContact;
    private double labourCost;
    private double labourPercentage;
    private String rulesOfEngagement;
    private long createdAt;
    private String status; // New field: QUOTATION, ONGOING, COMPLETED, PAID

    public Project(String name, String location, String description, String clientName, String clientContact) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.clientName = clientName;
        this.clientContact = clientContact;
        this.createdAt = System.currentTimeMillis();
        this.status = "QUOTATION"; // Default status
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientContact() { return clientContact; }
    public void setClientContact(String clientContact) { this.clientContact = clientContact; }
    public double getLabourCost() { return labourCost; }
    public void setLabourCost(double labourCost) { this.labourCost = labourCost; }
    public double getLabourPercentage() { return labourPercentage; }
    public void setLabourPercentage(double labourPercentage) { this.labourPercentage = labourPercentage; }
    public String getRulesOfEngagement() { return rulesOfEngagement; }
    public void setRulesOfEngagement(String rulesOfEngagement) { this.rulesOfEngagement = rulesOfEngagement; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
