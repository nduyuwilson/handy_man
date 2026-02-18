package com.nduyuwilson.thitima.ui.projects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;

public class ProjectsFragment extends Fragment {

    private ProjectViewModel projectViewModel;
    private ProjectAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_projects, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewProjects);
        adapter = new ProjectAdapter(project -> {
            Bundle bundle = new Bundle();
            bundle.putInt("projectId", project.getId());
            Navigation.findNavController(view).navigate(R.id.action_navigation_projects_to_projectDetailsFragment, bundle);
        });
        recyclerView.setAdapter(adapter);

        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        projectViewModel.getAllProjects().observe(getViewLifecycleOwner(), projects -> {
            adapter.submitList(projects);
        });

        FloatingActionButton fab = view.findViewById(R.id.fabAddProject);
        fab.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_navigation_projects_to_addProjectFragment);
        });
    }
}
