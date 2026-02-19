package com.nduyuwilson.thitima.data.model;

import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;

import java.util.List;

public class BackupData {
    public List<Project> projects;
    public List<Item> items;
    public List<ItemVariant> itemVariants;
    public List<ProjectItem> projectItems;
    public SettingsData settings;

    public BackupData() {}

    public BackupData(List<Project> projects, List<Item> items, List<ItemVariant> itemVariants, List<ProjectItem> projectItems, SettingsData settings) {
        this.projects = projects;
        this.items = items;
        this.itemVariants = itemVariants;
        this.projectItems = projectItems;
        this.settings = settings;
    }
}
