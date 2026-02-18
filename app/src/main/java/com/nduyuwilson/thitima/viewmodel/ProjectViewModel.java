package com.nduyuwilson.thitima.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.data.repository.ProjectRepository;

import java.util.List;

public class ProjectViewModel extends AndroidViewModel {
    private ProjectRepository mRepository;
    private final LiveData<List<Project>> mAllProjects;

    public ProjectViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ProjectRepository(application);
        mAllProjects = mRepository.getAllProjects();
    }

    public LiveData<List<Project>> getAllProjects() {
        return mAllProjects;
    }

    public void insert(Project project) {
        mRepository.insert(project);
    }

    public void update(Project project) {
        mRepository.update(project);
    }

    public void delete(Project project) {
        mRepository.delete(project);
    }

    public LiveData<Project> getProjectById(int id) {
        return mRepository.getProjectById(id);
    }

    // Project Items
    public LiveData<List<ProjectItem>> getItemsForProject(int projectId) {
        return mRepository.getItemsForProject(projectId);
    }

    public void insertProjectItem(ProjectItem projectItem) {
        mRepository.insertProjectItem(projectItem);
    }
}
