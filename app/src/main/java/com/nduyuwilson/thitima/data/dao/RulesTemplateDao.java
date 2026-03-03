package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.RulesTemplate;

import java.util.List;

@Dao
public interface RulesTemplateDao {
    @Insert
    void insert(RulesTemplate template);

    @Update
    void update(RulesTemplate template);

    @Delete
    void delete(RulesTemplate template);

    @Query("SELECT * FROM rules_templates ORDER BY title ASC")
    LiveData<List<RulesTemplate>> getAllTemplates();

    @Query("SELECT * FROM rules_templates")
    List<RulesTemplate> getAllTemplatesSync();

    @Query("DELETE FROM rules_templates")
    void deleteAll();
}
