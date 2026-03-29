package com.nduyuwilson.thitima.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.nduyuwilson.thitima.data.AppDatabase;
import com.nduyuwilson.thitima.data.dao.WorkerDao;
import com.nduyuwilson.thitima.data.dao.WorkerPaymentDao;
import com.nduyuwilson.thitima.data.entity.Worker;
import com.nduyuwilson.thitima.data.entity.WorkerPayment;
import java.util.List;

public class WorkerRepository {
    private WorkerDao mWorkerDao;
    private WorkerPaymentDao mWorkerPaymentDao;
    private LiveData<List<Worker>> mAllWorkers;

    public WorkerRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mWorkerDao = db.workerDao();
        mWorkerPaymentDao = db.workerPaymentDao();
        mAllWorkers = mWorkerDao.getAllWorkers();
    }

    public LiveData<List<Worker>> getAllWorkers() {
        return mAllWorkers;
    }

    public void insert(Worker worker) {
        AppDatabase.databaseWriteExecutor.execute(() -> mWorkerDao.insert(worker));
    }

    public void update(Worker worker) {
        AppDatabase.databaseWriteExecutor.execute(() -> mWorkerDao.update(worker));
    }

    public void delete(Worker worker) {
        AppDatabase.databaseWriteExecutor.execute(() -> mWorkerDao.delete(worker));
    }

    // Worker Payments
    public LiveData<List<WorkerPayment>> getPaymentsForProject(int projectId) {
        return mWorkerPaymentDao.getPaymentsForProject(projectId);
    }

    public void insertPayment(WorkerPayment payment) {
        AppDatabase.databaseWriteExecutor.execute(() -> mWorkerPaymentDao.insert(payment));
    }

    public void updatePayment(WorkerPayment payment) {
        AppDatabase.databaseWriteExecutor.execute(() -> mWorkerPaymentDao.update(payment));
    }

    public void deletePayment(WorkerPayment payment) {
        AppDatabase.databaseWriteExecutor.execute(() -> mWorkerPaymentDao.delete(payment));
    }
}
