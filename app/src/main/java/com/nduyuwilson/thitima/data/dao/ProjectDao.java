package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.Project;

import java.util.List;

@Dao
public interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Project project);

    @Update
    void update(Project project);

    @Delete
    void delete(Project project);

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    LiveData<List<Project>> getAllProjects();

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    List<Project> getAllProjectsSync();

    @Query("SELECT * FROM projects WHERE id = :id")
    LiveData<Project> getProjectById(int id);

    @Query("DELETE FROM projects")
    void deleteAll();
}
