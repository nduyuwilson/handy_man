package com.nduyuwilson.thitima.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.nduyuwilson.thitima.data.entity.RulesTemplate;
import com.nduyuwilson.thitima.data.repository.RulesTemplateRepository;
import java.util.List;

public class RulesTemplateViewModel extends AndroidViewModel {
    private RulesTemplateRepository mRepository;
    private final LiveData<List<RulesTemplate>> mAllTemplates;

    public RulesTemplateViewModel(@NonNull Application application) {
        super(application);
        mRepository = new RulesTemplateRepository(application);
        mAllTemplates = mRepository.getAllTemplates();
    }

    public LiveData<List<RulesTemplate>> getAllTemplates() {
        return mAllTemplates;
    }

    public void insert(RulesTemplate template) {
        mRepository.insert(template);
    }

    public void update(RulesTemplate template) {
        mRepository.update(template);
    }

    public void delete(RulesTemplate template) {
        mRepository.delete(template);
    }
}
