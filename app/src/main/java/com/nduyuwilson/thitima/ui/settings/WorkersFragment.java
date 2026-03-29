package com.nduyuwilson.thitima.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Worker;
import com.nduyuwilson.thitima.viewmodel.WorkerViewModel;

public class WorkersFragment extends Fragment {

    private WorkerViewModel viewModel;
    private WorkerAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        viewModel = new ViewModelProvider(this).get(WorkerViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewWorkers);
        adapter = new WorkerAdapter(this::showEditDeleteDialog);
        recyclerView.setAdapter(adapter);

        viewModel.getAllWorkers().observe(getViewLifecycleOwner(), workers -> {
            adapter.submitList(workers);
        });

        FloatingActionButton fab = view.findViewById(R.id.fabAddWorker);
        fab.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void showEditDeleteDialog(Worker worker) {
        String[] options = {"Edit Worker", "Delete Worker"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(worker.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddEditDialog(worker);
                    } else {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Delete Worker")
                                .setMessage("Are you sure you want to remove " + worker.getName() + "?")
                                .setPositiveButton("Delete", (d, w) -> viewModel.delete(worker))
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showAddEditDialog(@Nullable Worker existing) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(existing == null ? "New Worker" : "Edit Worker");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editName = new EditText(requireContext());
        editName.setHint("Full Name");
        if (existing != null) editName.setText(existing.getName());
        layout.addView(editName);

        final EditText editPhone = new EditText(requireContext());
        editPhone.setHint("Phone Number");
        editPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        if (existing != null) editPhone.setText(existing.getPhone());
        layout.addView(editPhone);

        final EditText editRole = new EditText(requireContext());
        editRole.setHint("Role (e.g. Technician)");
        if (existing != null) editRole.setText(existing.getRole());
        layout.addView(editRole);

        builder.setView(layout);

        builder.setPositiveButton(existing == null ? "Add" : "Update", (dialog, which) -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String role = editRole.getText().toString().trim();

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(phone)) {
                if (existing == null) {
                    viewModel.insert(new Worker(name, phone, role));
                } else {
                    existing.setName(name);
                    existing.setPhone(phone);
                    existing.setRole(role);
                    viewModel.update(existing);
                }
            } else {
                Toast.makeText(requireContext(), "Name and phone are required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
