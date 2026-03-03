package com.nduyuwilson.thitima.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "labour_activities",
        foreignKeys = @ForeignKey(entity = Project.class,
                parentColumns = "id",
                childColumns = "projectId",
                onDelete = ForeignKey.CASCADE))
public class LabourActivity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int projectId;
    private String name; // e.g., "Consultation", "Transport"
    private double cost;
    private long date;

    public LabourActivity(int projectId, String name, double cost) {
        this.projectId = projectId;
        this.name = name;
        this.cost = cost;
        this.date = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
}
