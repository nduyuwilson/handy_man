package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.ItemVariant;

import java.util.List;

@Dao
public interface ItemVariantDao {
    @Insert
    void insert(ItemVariant variant);

    @Update
    void update(ItemVariant variant);

    @Delete
    void delete(ItemVariant variant);

    @Query("SELECT * FROM item_variants WHERE itemId = :itemId")
    LiveData<List<ItemVariant>> getVariantsForItem(int itemId);

    @Query("SELECT * FROM item_variants WHERE id = :id")
    LiveData<ItemVariant> getVariantById(int id);
}
