package com.nduyuwilson.thitima.auth;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.nduyuwilson.thitima.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminTenantsFragment extends Fragment implements AdminTenantsAdapter.TenantActionListener {

    private RecyclerView rvTenants;
    private AdminTenantsAdapter adapter;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvTotalCount, tvActiveCount, tvTrialCount;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilters;

    private FirebaseFirestore db;
    private final List<TenantModel> allTenants = new ArrayList<>();
    private String currentSearchText = "";
    private int currentFilterId = R.id.chipFilterAll;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_tenants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Security check: Only super-admin allowed
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (!AdminManager.isAdmin(currentUser)) {
            Toast.makeText(requireContext(), "Access Denied: Super-admin only", Toast.LENGTH_LONG).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = view.findViewById(R.id.adminToolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_refresh_tenants) {
                fetchTenantsFromFirestore();
                return true;
            }
            return false;
        });

        tvTotalCount = view.findViewById(R.id.tvTotalTenantsCount);
        tvActiveCount = view.findViewById(R.id.tvActiveProCount);
        tvTrialCount = view.findViewById(R.id.tvTrialCount);

        rvTenants = view.findViewById(R.id.rvTenants);
        rvTenants.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminTenantsAdapter(this);
        rvTenants.setAdapter(adapter);

        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        etSearch = view.findViewById(R.id.etTenantSearch);
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                currentFilterId = checkedIds.get(0);
                filterAndDisplay();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                currentSearchText = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                filterAndDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fetchTenantsFromFirestore();
    }

    private void fetchTenantsFromFirestore() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        allTenants.clear();
                        QuerySnapshot snapshot = task.getResult();
                        int proCount = 0;
                        int trialCount = 0;

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            TenantModel tenant = new TenantModel();
                            tenant.setUid(doc.getId());
                            tenant.setEmail(doc.getString("email"));
                            Boolean isPrem = doc.getBoolean("isPremium");
                            tenant.setPremium(Boolean.TRUE.equals(isPrem));
                            tenant.setDeviceId(doc.getString("deviceId"));
                            tenant.setCreatedAt(doc.getTimestamp("createdAt"));

                            allTenants.add(tenant);
                            if (tenant.isPremium()) {
                                proCount++;
                            } else {
                                trialCount++;
                            }
                        }

                        tvTotalCount.setText(String.valueOf(allTenants.size()));
                        tvActiveCount.setText(String.valueOf(proCount));
                        tvTrialCount.setText(String.valueOf(trialCount));

                        filterAndDisplay();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Failed to load customers";
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterAndDisplay() {
        List<TenantModel> filtered = new ArrayList<>();

        for (TenantModel t : allTenants) {
            boolean matchesFilter = true;
            if (currentFilterId == R.id.chipFilterPro) {
                matchesFilter = t.isPremium();
            } else if (currentFilterId == R.id.chipFilterTrial) {
                matchesFilter = !t.isPremium();
            } else if (currentFilterId == R.id.chipFilterLocked) {
                matchesFilter = t.getDeviceId() != null && !t.getDeviceId().trim().isEmpty();
            }

            boolean matchesSearch = true;
            if (!currentSearchText.isEmpty()) {
                String email = t.getEmail() != null ? t.getEmail().toLowerCase(Locale.getDefault()) : "";
                String uid = t.getUid() != null ? t.getUid().toLowerCase(Locale.getDefault()) : "";
                matchesSearch = email.contains(currentSearchText) || uid.contains(currentSearchText);
            }

            if (matchesFilter && matchesSearch) {
                filtered.add(t);
            }
        }

        adapter.setTenants(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyTitle.setText(allTenants.isEmpty() ? "No registered customers yet" : "No matching customers found");
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onToggleSubscription(TenantModel tenant) {
        boolean newStatus = !tenant.isPremium();
        String actionTitle = newStatus ? "Activate PRO Plan" : "Suspend PRO Plan";
        String message = newStatus
                ? "Grant full PRO SaaS access to " + tenant.getEmail() + "?"
                : "Suspend PRO access for " + tenant.getEmail() + "? Their access will expire upon next verification.";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(actionTitle)
                .setMessage(message)
                .setPositiveButton(newStatus ? "Activate PRO" : "Suspend", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    db.collection("users").document(tenant.getUid())
                            .update("isPremium", newStatus)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                tenant.setPremium(newStatus);
                                Toast.makeText(requireContext(), "Updated subscription for " + tenant.getEmail(), Toast.LENGTH_SHORT).show();
                                fetchTenantsFromFirestore();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResetDeviceLock(TenantModel tenant) {
        if (tenant.getDeviceId() == null || tenant.getDeviceId().trim().isEmpty()) {
            Toast.makeText(requireContext(), "No hardware device lock is currently active for this account", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset Device Lock")
                .setMessage("Reset the hardware lock for " + tenant.getEmail() + "?\n\nThis will unbind their current phone and allow them to pair a new device upon their next login.")
                .setPositiveButton("Reset Lock", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    db.collection("users").document(tenant.getUid())
                            .update("deviceId", null)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                tenant.setDeviceId(null);
                                Toast.makeText(requireContext(), "Device lock cleared for " + tenant.getEmail(), Toast.LENGTH_SHORT).show();
                                fetchTenantsFromFirestore();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onTenantClick(TenantModel tenant) {
        String[] options = {"Copy Tenant UID", "Send Email to Customer", "Toggle Plan", "Reset Device Lock"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(tenant.getEmail())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText("Tenant UID", tenant.getUid()));
                            Toast.makeText(requireContext(), "UID copied to clipboard", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            if (tenant.getEmail() != null) {
                                Intent intent = new Intent(Intent.ACTION_SENDTO);
                                intent.setData(Uri.parse("mailto:" + tenant.getEmail()));
                                intent.putExtra(Intent.EXTRA_SUBJECT, "Thitima SaaS Workspace Support");
                                startActivity(Intent.createChooser(intent, "Email Customer"));
                            }
                            break;
                        case 2:
                            onToggleSubscription(tenant);
                            break;
                        case 3:
                            onResetDeviceLock(tenant);
                            break;
                    }
                })
                .show();
    }
}