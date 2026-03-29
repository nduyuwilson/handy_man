package com.nduyuwilson.thitima.ui.projects;

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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;

import java.util.ArrayList;
import java.util.List;

public class ProjectsFragment extends Fragment {

    private ProjectViewModel projectViewModel;
    private ProjectAdapter adapter;
    private List<Project> allProjects = new ArrayList<>();
    private String currentFilterStatus = "ALL";
    private String currentSearchQuery = "";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_projects, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewProjects);
        adapter = new ProjectAdapter(project -> {
            Bundle bundle = new Bundle();
            bundle.putInt("projectId", project.getId());
            Navigation.findNavController(view).navigate(R.id.action_navigation_projects_to_projectDetailsFragment, bundle);
        });
        recyclerView.setAdapter(adapter);

        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        projectViewModel.getAllProjects().observe(getViewLifecycleOwner(), projects -> {
            allProjects = projects;
            applyFilters();
        });

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutStatus);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilterStatus = tab.getText().toString();
                applyFilters();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        FloatingActionButton fab = view.findViewById(R.id.fabAddProject);
        fab.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_navigation_projects_to_addProjectFragment);
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.search_menu, menu);
                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();
                searchView.setQueryHint("Search projects...");
                
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        currentSearchQuery = newText;
                        applyFilters();
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

    private void applyFilters() {
        List<Project> filteredList = new ArrayList<>();
        for (Project p : allProjects) {
            boolean matchesStatus = currentFilterStatus.equals("ALL") || p.getStatus().equals(currentFilterStatus);
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                                   p.getName().toLowerCase().contains(currentSearchQuery.toLowerCase()) || 
                                   p.getLocation().toLowerCase().contains(currentSearchQuery.toLowerCase());
            
            if (matchesStatus && matchesSearch) {
                filteredList.add(p);
            }
        }
        adapter.submitList(filteredList);
    }
}
