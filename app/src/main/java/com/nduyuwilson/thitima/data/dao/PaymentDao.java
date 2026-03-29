package com.nduyuwilson.thitima.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.nduyuwilson.thitima.data.entity.Payment;

import java.util.List;

@Dao
public interface PaymentDao {
    @Insert
    void insert(Payment payment);

    @Update
    void update(Payment payment);

    @Delete
    void delete(Payment payment);

    @Query("SELECT * FROM payments WHERE projectId = :projectId ORDER BY date DESC")
    LiveData<List<Payment>> getPaymentsForProject(int projectId);

    @Query("SELECT * FROM payments")
    List<Payment> getAllPaymentsSync();

    @Query("DELETE FROM payments")
    void deleteAll();
}
