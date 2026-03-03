package com.nduyuwilson.thitima.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.nduyuwilson.thitima.data.AppDatabase;
import com.nduyuwilson.thitima.data.dao.RulesTemplateDao;
import com.nduyuwilson.thitima.data.entity.RulesTemplate;
import java.util.List;

public class RulesTemplateRepository {
    private RulesTemplateDao mDao;
    private LiveData<List<RulesTemplate>> mAllTemplates;

    public RulesTemplateRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mDao = db.rulesTemplateDao();
        mAllTemplates = mDao.getAllTemplates();
    }

    public LiveData<List<RulesTemplate>> getAllTemplates() {
        return mAllTemplates;
    }

    public void insert(RulesTemplate template) {
        AppDatabase.databaseWriteExecutor.execute(() -> mDao.insert(template));
    }

    public void update(RulesTemplate template) {
        AppDatabase.databaseWriteExecutor.execute(() -> mDao.update(template));
    }

    public void delete(RulesTemplate template) {
        AppDatabase.databaseWriteExecutor.execute(() -> mDao.delete(template));
    }
}
