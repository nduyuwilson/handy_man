package com.nduyuwilson.thitima.data.repository;

import android.app.Application;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.nduyuwilson.thitima.data.AppDatabase;
import com.nduyuwilson.thitima.data.dao.ItemDao;
import com.nduyuwilson.thitima.data.dao.ItemVariantDao;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;

import java.io.File;
import java.util.List;

public class ItemRepository {
    private ItemDao mItemDao;
    private ItemVariantDao mItemVariantDao;
    private LiveData<List<Item>> mAllItems;

    public ItemRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mItemDao = db.itemDao();
        mItemVariantDao = db.itemVariantDao();
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
            deleteInternalFile(item.getImageUri());
            mItemDao.delete(item);
        });
    }

    public LiveData<Item> getItemById(int id) {
        return mItemDao.getItemById(id);
    }

    // Item Variants
    public LiveData<List<ItemVariant>> getVariantsForItem(int itemId) {
        return mItemVariantDao.getVariantsForItem(itemId);
    }

    public LiveData<ItemVariant> getVariantById(int id) {
        return mItemVariantDao.getVariantById(id);
    }

    public void insertVariant(ItemVariant variant) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemVariantDao.insert(variant);
        });
    }

    public void updateVariant(ItemVariant variant) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mItemVariantDao.update(variant);
        });
    }

    public void deleteVariant(ItemVariant variant) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            deleteInternalFile(variant.getImageUri());
            mItemVariantDao.delete(variant);
        });
    }

    private void deleteInternalFile(String uriString) {
        if (uriString == null || uriString.isEmpty()) return;
        try {
            Uri uri = Uri.parse(uriString);
            if ("file".equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
