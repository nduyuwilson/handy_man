package com.nduyuwilson.thitima.ui.catalogue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;

import java.util.ArrayList;
import java.util.List;

public class CatalogueFragment extends Fragment {

    private ItemViewModel itemViewModel;
    private com.nduyuwilson.thitima.viewmodel.CategoryViewModel categoryViewModel;
    private com.google.android.material.chip.ChipGroup chipGroupCategories;
    private CatalogueAdapter adapter;
    private List<Item> allItems = new ArrayList<>();
    private int currentCategoryId = -1; // -1 means "All"

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
        
        // Single click to view variants
        adapter.setOnItemClickListener(item -> {
            Bundle bundle = new Bundle();
            bundle.putInt("itemId", item.getId());
            Navigation.findNavController(view).navigate(R.id.action_navigation_catalogue_to_itemVariantsFragment, bundle);
        });

        // Long click to edit/delete the main item itself
        adapter.setOnItemLongClickListener(this::showItemManagementDialog);
        
        recyclerView.setAdapter(adapter);

        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(com.nduyuwilson.thitima.viewmodel.CategoryViewModel.class);

        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        setupCategoryChips();

        itemViewModel.getAllItems().observe(getViewLifecycleOwner(), items -> {
            allItems = items;
            applyFilters();
        });

        FloatingActionButton fab = view.findViewById(R.id.fabAddItem);
        fab.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_navigation_catalogue_to_addItemFragment);
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.search_menu, menu);
                // Add Manage Categories option
                menu.add(0, 100, 0, "Manage Categories")
                    .setIcon(android.R.drawable.ic_menu_edit)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

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
                if (menuItem.getItemId() == 100) {
                    showManageCategoriesDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void setupCategoryChips() {
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            // Keep the "All" chip, remove others
            int childCount = chipGroupCategories.getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                View child = chipGroupCategories.getChildAt(i);
                if (child.getId() != R.id.chipAll) {
                    chipGroupCategories.removeView(child);
                }
            }

            for (com.nduyuwilson.thitima.data.entity.Category category : categories) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
                chip.setText(category.getName());
                chip.setCheckable(true);
                chip.setTag(category.getId());
                chipGroupCategories.addView(chip);
            }
        });

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || checkedIds.get(0) == R.id.chipAll) {
                currentCategoryId = -1;
            } else {
                View chip = group.findViewById(checkedIds.get(0));
                currentCategoryId = (int) chip.getTag();
            }
            applyFilters();
        });
    }

    private void showManageCategoriesDialog() {
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories == null) return;
            
            String[] names = new String[categories.size()];
            for (int i = 0; i < categories.size(); i++) {
                names[i] = categories.get(i).getName();
            }

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Manage Categories")
                    .setItems(names, (dialog, which) -> {
                        confirmDeleteCategory(categories.get(which));
                    })
                    .setNeutralButton("Add New", (dialog, which) -> {
                        showAddCategoryDialog();
                    })
                    .setPositiveButton("Done", null)
                    .show();
        });
    }

    private void confirmDeleteCategory(com.nduyuwilson.thitima.data.entity.Category category) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Category")
                .setMessage("Delete '" + category.getName() + "'? Items in this category will become uncategorized.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    categoryViewModel.delete(category);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddCategoryDialog() {
        com.google.android.material.textfield.TextInputEditText input = new com.google.android.material.textfield.TextInputEditText(requireContext());
        com.google.android.material.textfield.TextInputLayout layout = new com.google.android.material.textfield.TextInputLayout(requireContext());
        layout.setPadding(48, 24, 48, 24);
        layout.setHint("Category Name");
        layout.addView(input);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New Category")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!android.text.TextUtils.isEmpty(name)) {
                        categoryViewModel.insert(new com.nduyuwilson.thitima.data.entity.Category(name));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyFilters() {
        List<Item> filtered = new ArrayList<>();
        for (Item item : allItems) {
            if (currentCategoryId == -1 || item.getCategoryId() == currentCategoryId) {
                filtered.add(item);
            }
        }
        adapter.submitList(filtered);
    }

    private void showItemManagementDialog(Item item) {
        String[] options = {"Edit Component", "Delete Component", "Manage Brands/Variants"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Edit item
                        Bundle bundle = new Bundle();
                        bundle.putInt("itemId", item.getId());
                        Navigation.findNavController(requireView()).navigate(R.id.action_navigation_catalogue_to_addItemFragment, bundle);
                    } else if (which == 1) {
                        // Delete item
                        confirmDeleteItem(item);
                    } else if (which == 2) {
                        // View variants
                        Bundle bundle = new Bundle();
                        bundle.putInt("itemId", item.getId());
                        Navigation.findNavController(requireView()).navigate(R.id.action_navigation_catalogue_to_itemVariantsFragment, bundle);
                    }
                })
                .show();
    }

    private void confirmDeleteItem(Item item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Component")
                .setMessage("Are you sure you want to delete '" + item.getName() + "' and all its variants? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    itemViewModel.delete(item);
                    Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
