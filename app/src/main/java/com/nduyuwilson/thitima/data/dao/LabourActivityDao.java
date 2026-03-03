package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.LabourActivity;

import java.util.List;

@Dao
public interface LabourActivityDao {
    @Insert
    void insert(LabourActivity activity);

    @Update
    void update(LabourActivity activity);

    @Delete
    void delete(LabourActivity activity);

    @Query("SELECT * FROM labour_activities WHERE projectId = :projectId ORDER BY date DESC")
    LiveData<List<LabourActivity>> getActivitiesForProject(int projectId);

    @Query("SELECT * FROM labour_activities")
    List<LabourActivity> getAllActivitiesSync();

    @Query("DELETE FROM labour_activities")
    void deleteAll();
}
