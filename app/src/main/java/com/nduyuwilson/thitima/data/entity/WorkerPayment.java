package com.nduyuwilson.thitima.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "worker_payments",
        foreignKeys = {
                @ForeignKey(entity = Project.class,
                        parentColumns = "id",
                        childColumns = "projectId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Worker.class,
                        parentColumns = "id",
                        childColumns = "workerId",
                        onDelete = ForeignKey.CASCADE)
        })
public class WorkerPayment {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int projectId;
    private int workerId;
    private double wage;
    private double transport;
    private long date;
    private String description;

    public WorkerPayment(int projectId, int workerId, double wage, double transport, String description) {
        this.projectId = projectId;
        this.workerId = workerId;
        this.wage = wage;
        this.transport = transport;
        this.description = description;
        this.date = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public int getWorkerId() { return workerId; }
    public void setWorkerId(int workerId) { this.workerId = workerId; }
    public double getWage() { return wage; }
    public void setWage(double wage) { this.wage = wage; }
    public double getTransport() { return transport; }
    public void setTransport(double transport) { this.transport = transport; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
