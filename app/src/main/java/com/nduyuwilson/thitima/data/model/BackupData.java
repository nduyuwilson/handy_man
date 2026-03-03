package com.nduyuwilson.thitima.data.model;

import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.data.entity.RulesTemplate;

import java.util.List;

public class BackupData {
    public List<Project> projects;
    public List<Item> items;
    public List<ItemVariant> itemVariants;
    public List<ProjectItem> projectItems;
    public List<LabourActivity> labourActivities;
    public List<RulesTemplate> rulesTemplates;
    public SettingsData settings;

    public BackupData() {}

    public BackupData(List<Project> projects, List<Item> items, List<ItemVariant> itemVariants, 
                      List<ProjectItem> projectItems, List<LabourActivity> labourActivities, 
                      List<RulesTemplate> rulesTemplates, SettingsData settings) {
        this.projects = projects;
        this.items = items;
        this.itemVariants = itemVariants;
        this.projectItems = projectItems;
        this.labourActivities = labourActivities;
        this.rulesTemplates = rulesTemplates;
        this.settings = settings;
    }
}
