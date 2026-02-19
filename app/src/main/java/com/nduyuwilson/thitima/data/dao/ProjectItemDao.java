package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.ProjectItem;

import java.util.List;

@Dao
public interface ProjectItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProjectItem projectItem);

    @Update
    void update(ProjectItem projectItem);

    @Delete
    void delete(ProjectItem projectItem);

    @Query("SELECT * FROM project_items WHERE projectId = :projectId")
    LiveData<List<ProjectItem>> getItemsForProject(int projectId);

    @Query("SELECT * FROM project_items WHERE projectId = :projectId AND itemId = :itemId AND (variantId = :variantId OR (variantId IS NULL AND :variantId IS NULL)) LIMIT 1")
    ProjectItem getExistingItem(int projectId, int itemId, Integer variantId);

    @Query("SELECT * FROM project_items")
    List<ProjectItem> getAllProjectItemsSync();

    @Query("DELETE FROM project_items")
    void deleteAll();
}
