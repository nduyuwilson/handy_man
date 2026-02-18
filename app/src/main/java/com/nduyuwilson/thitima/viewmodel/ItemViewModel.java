package com.nduyuwilson.thitima.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.repository.ItemRepository;

import java.util.List;

public class ItemViewModel extends AndroidViewModel {
    private ItemRepository mRepository;
    private final LiveData<List<Item>> mAllItems;

    public ItemViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ItemRepository(application);
        mAllItems = mRepository.getAllItems();
    }

    public LiveData<List<Item>> getAllItems() {
        return mAllItems;
    }

    public void insert(Item item) {
        mRepository.insert(item);
    }

    public void update(Item item) {
        mRepository.update(item);
    }

    public void delete(Item item) {
        mRepository.delete(item);
    }

    public LiveData<Item> getItemById(int id) {
        return mRepository.getItemById(id);
    }
}
