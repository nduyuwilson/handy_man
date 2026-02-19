package com.nduyuwilson.thitima.ui.catalogue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;

import java.util.ArrayList;
import java.util.List;

public class CatalogueFragment extends Fragment {

    private ItemViewModel itemViewModel;
    private CatalogueAdapter adapter;
    private List<Item> allItems = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalogue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCatalogue);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        
        adapter = new CatalogueAdapter();
        adapter.setOnItemClickListener(item -> {
            Bundle bundle = new Bundle();
            bundle.putInt("itemId", item.getId());
            Navigation.findNavController(view).navigate(R.id.action_navigation_catalogue_to_itemVariantsFragment, bundle);
        });
        recyclerView.setAdapter(adapter);

        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        itemViewModel.getAllItems().observe(getViewLifecycleOwner(), items -> {
            allItems = items;
            adapter.submitList(items);
        });

        FloatingActionButton fab = view.findViewById(R.id.fabAddItem);
        fab.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_navigation_catalogue_to_addItemFragment);
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.search_menu, menu);
                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();
                searchView.setQueryHint("Search components...");
                
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        filterItems(newText);
                        return true;
                    }
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void filterItems(String query) {
        if (query == null || query.isEmpty()) {
            adapter.submitList(allItems);
            return;
        }
        
        List<Item> filteredList = new ArrayList<>();
        for (Item item : allItems) {
            if (item.getName().toLowerCase().contains(query.toLowerCase()) || 
                (item.getDescription() != null && item.getDescription().toLowerCase().contains(query.toLowerCase()))) {
                filteredList.add(item);
            }
        }
        adapter.submitList(filteredList);
    }
}
