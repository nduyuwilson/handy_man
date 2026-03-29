package com.nduyuwilson.thitima.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workers")
public class Worker {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String phone;
    private String role; // e.g., "Technician", "Assistant"

    public Worker(String name, String phone, String role) {
        this.name = name;
        this.phone = phone;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
