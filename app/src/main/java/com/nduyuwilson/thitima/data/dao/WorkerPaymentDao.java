package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.WorkerPayment;

import java.util.List;

@Dao
public interface WorkerPaymentDao {
    @Insert
    void insert(WorkerPayment payment);

    @Update
    void update(WorkerPayment payment);

    @Delete
    void delete(WorkerPayment payment);

    @Query("SELECT * FROM worker_payments WHERE projectId = :projectId ORDER BY date DESC")
    LiveData<List<WorkerPayment>> getPaymentsForProject(int projectId);

    @Query("SELECT * FROM worker_payments")
    List<WorkerPayment> getAllWorkerPaymentsSync();

    @Query("DELETE FROM worker_payments")
    void deleteAll();
}
