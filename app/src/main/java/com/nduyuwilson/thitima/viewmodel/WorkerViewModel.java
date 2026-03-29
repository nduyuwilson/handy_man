package com.nduyuwilson.thitima.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.nduyuwilson.thitima.data.entity.Worker;
import com.nduyuwilson.thitima.data.entity.WorkerPayment;
import com.nduyuwilson.thitima.data.repository.WorkerRepository;
import java.util.List;

public class WorkerViewModel extends AndroidViewModel {
    private WorkerRepository mRepository;
    private final LiveData<List<Worker>> mAllWorkers;

    public WorkerViewModel(@NonNull Application application) {
        super(application);
        mRepository = new WorkerRepository(application);
        mAllWorkers = mRepository.getAllWorkers();
    }

    public LiveData<List<Worker>> getAllWorkers() {
        return mAllWorkers;
    }

    public void insert(Worker worker) {
        mRepository.insert(worker);
    }

    public void update(Worker worker) {
        mRepository.update(worker);
    }

    public void delete(Worker worker) {
        mRepository.delete(worker);
    }

    public LiveData<List<WorkerPayment>> getPaymentsForProject(int projectId) {
        return mRepository.getPaymentsForProject(projectId);
    }

    public void insertPayment(WorkerPayment payment) {
        mRepository.insertPayment(payment);
    }

    public void updatePayment(WorkerPayment payment) {
        mRepository.updatePayment(payment);
    }

    public void deletePayment(WorkerPayment payment) {
        mRepository.deletePayment(payment);
    }
}
