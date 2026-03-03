package com.nduyuwilson.thitima.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.nduyuwilson.thitima.data.AppDatabase;
import com.nduyuwilson.thitima.data.dao.LabourActivityDao;
import com.nduyuwilson.thitima.data.dao.ProjectDao;
import com.nduyuwilson.thitima.data.dao.ProjectItemDao;
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProjectRepository {
    private ProjectDao mProjectDao;
    private ProjectItemDao mProjectItemDao;
    private LabourActivityDao mLabourActivityDao;
    private LiveData<List<Project>> mAllProjects;

    public ProjectRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mProjectDao = db.projectDao();
        mProjectItemDao = db.projectItemDao();
        mLabourActivityDao = db.labourActivityDao();
        mAllProjects = mProjectDao.getAllProjects();
    }

    public LiveData<List<Project>> getAllProjects() {
        return mAllProjects;
    }

    public void insert(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mProjectDao.insert(project);
        });
    }

    public void update(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mProjectDao.update(project);
        });
    }

    public void delete(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mProjectDao.delete(project);
        });
    }

    public LiveData<Project> getProjectById(int id) {
        return mProjectDao.getProjectById(id);
    }

    // Project Items
    public LiveData<List<ProjectItem>> getItemsForProject(int projectId) {
        return mProjectItemDao.getItemsForProject(projectId);
    }

    public void insertProjectItem(ProjectItem projectItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mProjectItemDao.insert(projectItem);
        });
    }

    public void updateProjectItem(ProjectItem projectItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mProjectItemDao.update(projectItem);
        });
    }

    public void deleteProjectItem(ProjectItem projectItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mProjectItemDao.delete(projectItem);
        });
    }

    public ProjectItem getExistingProjectItem(int projectId, int itemId, Integer variantId) {
        Future<ProjectItem> future = AppDatabase.databaseWriteExecutor.submit(() -> 
            mProjectItemDao.getExistingItem(projectId, itemId, variantId)
        );
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    // Labour Activities
    public LiveData<List<LabourActivity>> getActivitiesForProject(int projectId) {
        return mLabourActivityDao.getActivitiesForProject(projectId);
    }

    public void insertLabourActivity(LabourActivity activity) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mLabourActivityDao.insert(activity);
        });
    }

    public void updateLabourActivity(LabourActivity activity) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mLabourActivityDao.update(activity);
        });
    }

    public void deleteLabourActivity(LabourActivity activity) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mLabourActivityDao.delete(activity);
        });
    }
}
