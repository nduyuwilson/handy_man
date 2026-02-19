package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.Item;

import java.util.List;

@Dao
public interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Item item);

    @Update
    void update(Item item);

    @Delete
    void delete(Item item);

    @Query("SELECT * FROM items ORDER BY name ASC")
    LiveData<List<Item>> getAllItems();

    @Query("SELECT * FROM items ORDER BY name ASC")
    List<Item> getAllItemsSync();

    @Query("SELECT * FROM items WHERE id = :id")
    LiveData<Item> getItemById(int id);

    @Query("DELETE FROM items")
    void deleteAll();
}
