package com.nduyuwilson.thitima.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "item_variants",
        foreignKeys = @ForeignKey(entity = Item.class,
                parentColumns = "id",
                childColumns = "itemId",
                onDelete = ForeignKey.CASCADE))
public class ItemVariant {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int itemId;
    private String brandName;
    private double buyingPrice;
    private double sellingPrice;
    private String imageUri;

    public ItemVariant(int itemId, String brandName, double buyingPrice, double sellingPrice, String imageUri) {
        this.itemId = itemId;
        this.brandName = brandName;
        this.buyingPrice = buyingPrice;
        this.sellingPrice = sellingPrice;
        this.imageUri = imageUri;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public double getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(double buyingPrice) { this.buyingPrice = buyingPrice; }
    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }
    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
}
