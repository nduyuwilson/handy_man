package com.nduyuwilson.thitima.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "project_items",
        foreignKeys = {
                @ForeignKey(entity = Project.class,
                        parentColumns = "id",
                        childColumns = "projectId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Item.class,
                        parentColumns = "id",
                        childColumns = "itemId",
                        onDelete = ForeignKey.CASCADE)
        })
public class ProjectItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int projectId;
    private int itemId;
    private Integer variantId; // Optional: reference to the selected brand variant
    private int quantity;
    private double quotedPrice;

    public ProjectItem(int projectId, int itemId, int quantity, double quotedPrice) {
        this.projectId = projectId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.quotedPrice = quotedPrice;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public Integer getVariantId() { return variantId; }
    public void setVariantId(Integer variantId) { this.variantId = variantId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getQuotedPrice() { return quotedPrice; }
    public void setQuotedPrice(double quotedPrice) { this.quotedPrice = quotedPrice; }
}
