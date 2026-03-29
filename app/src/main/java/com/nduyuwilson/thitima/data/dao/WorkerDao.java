package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.Worker;

import java.util.List;

@Dao
public interface WorkerDao {
    @Insert
    void insert(Worker worker);

    @Update
    void update(Worker worker);

    @Delete
    void delete(Worker worker);

    @Query("SELECT * FROM workers ORDER BY name ASC")
    LiveData<List<Worker>> getAllWorkers();

    @Query("SELECT * FROM workers")
    List<Worker> getAllWorkersSync();

    @Query("DELETE FROM workers")
    void deleteAll();
}
