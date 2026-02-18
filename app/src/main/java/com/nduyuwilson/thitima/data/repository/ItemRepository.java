package com.nduyuwilson.thitima.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.nduyuwilson.thitima.data.AppDatabase;
import com.nduyuwilson.thitima.data.dao.ItemDao;
import com.nduyuwilson.thitima.data.entity.Item;

import java.util.List;

public class ItemRepository {
    private ItemDao mItemDao;
    private LiveData<List<Item>> mAllItems;

    public ItemRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mItemDao = db.itemDao();
        mAllItems = mItemDao.getAllItems();
    }

    public LiveData<List<Item>> getAllItems() {
        return mAllItems;
    }

    public void insert(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.insert(item);
        });
    }

    public void update(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.update(item);
        });
    }

    public void delete(Item item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemDao.delete(item);
        });
    }

    public LiveData<Item> getItemById(int id) {
        return mItemDao.getItemById(id);
    }
}
