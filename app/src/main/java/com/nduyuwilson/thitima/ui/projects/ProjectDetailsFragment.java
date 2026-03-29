package com.nduyuwilson.thitima.ui.projects;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.data.entity.Payment;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.data.entity.Worker;
import com.nduyuwilson.thitima.data.entity.WorkerPayment;
import com.nduyuwilson.thitima.util.Formatter;
import com.nduyuwilson.thitima.util.PdfGenerator;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;
import com.nduyuwilson.thitima.viewmodel.WorkerViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectDetailsFragment extends Fragment {

    private ProjectViewModel projectViewModel;
    private ItemViewModel itemViewModel;
    private WorkerViewModel workerViewModel;
    private int projectId;

    // View bindings
    private TextView tvTitle, tvLocation, tvClient, tvGrandTotalTop, tvMaterialTotalTop, tvLabourTotalTop, tvTotalPaidTop, tvBalanceDueTop, tvWorkerWagesTop;
    private TextView tvMaterialTotalTable, tvLabourTotalTable, tvRules;
    
    private ProjectItemAdapter itemAdapter;
    private LabourActivityAdapter labourAdapter;
    private PaymentAdapter paymentAdapter;
    private WorkerPaymentAdapter workerPaymentAdapter;
    
    private double currentLabourTotal = 0;
    private double currentMaterialTotal = 0;
    private double currentPaidTotal = 0;
    private double currentWorkerWagesTotal = 0;
    
    private Project currentProject;
    private List<ProjectItem> currentProjectItems;
    private List<LabourActivity> currentLabourActivities;
    private List<Payment> currentPayments;
    private List<WorkerPayment> currentWorkerPayments;
    private List<Worker> allWorkers;
    
    private Map<Integer, Item> itemMap = new HashMap<>();
    private Map<Integer, ItemVariant> variantMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectId = getArguments().getInt("projectId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_project_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Bind Views
        tvTitle = view.findViewById(R.id.tvProjectTitle);
        tvLocation = view.findViewById(R.id.tvProjectLocation);
        tvClient = view.findViewById(R.id.tvClientDetails);
        tvGrandTotalTop = view.findViewById(R.id.tvGrandTotalTop);
        tvMaterialTotalTop = view.findViewById(R.id.tvMaterialTotalTop);
        tvLabourTotalTop = view.findViewById(R.id.tvLabourTotalTop);
        tvTotalPaidTop = view.findViewById(R.id.tvTotalPaidTop);
        tvBalanceDueTop = view.findViewById(R.id.tvBalanceDueTop);
        tvWorkerWagesTop = view.findViewById(R.id.tvWorkerWagesTop);
        
//        tvMaterialTotalTable = view.findViewById(R.id.tvMaterialTotalTable);
//        tvLabourTotalTable = view.findViewById(R.id.tvLabourTotalTable);
//        tvRules = view.findViewById(R.id.tvRules);

        // ViewModels
        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        workerViewModel = new ViewModelProvider(this).get(WorkerViewModel.class);

        setupRecyclerViews(view);
        observeData();

        // Buttons
        view.findViewById(R.id.buttonAddComponent).setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("projectId", projectId);
            Navigation.findNavController(view).navigate(R.id.action_projectDetailsFragment_to_addComponentToProjectFragment, bundle);
        });

        view.findViewById(R.id.buttonAddLabour).setOnClickListener(v -> showAddLabourDialog(null));
        view.findViewById(R.id.buttonAddPayment).setOnClickListener(v -> showAddPaymentDialog(null));
        view.findViewById(R.id.buttonAddWorkerWage).setOnClickListener(v -> showAddWorkerPaymentDialog(null));
        view.findViewById(R.id.buttonGenerateInvoice).setOnClickListener(v -> generateAndSharePdf(false));
        view.findViewById(R.id.buttonGenerateLabourInvoice).setOnClickListener(v -> generateAndSharePdf(true));

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.project_details_menu, menu);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_edit_project) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("projectId", projectId);
                    Navigation.findNavController(view).navigate(R.id.action_projectDetailsFragment_to_addProjectFragment, bundle);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void setupRecyclerViews(View view) {
        RecyclerView rvItems = view.findViewById(R.id.recyclerViewProjectItems);
        itemAdapter = new ProjectItemAdapter(itemViewModel, getViewLifecycleOwner(), this::showProjectItemOptions);
        rvItems.setAdapter(itemAdapter);

        RecyclerView rvLabour = view.findViewById(R.id.recyclerViewLabourActivities);
        labourAdapter = new LabourActivityAdapter(this::showLabourActivityOptions);
        rvLabour.setAdapter(labourAdapter);

        RecyclerView rvPayments = view.findViewById(R.id.recyclerViewPayments);
        paymentAdapter = new PaymentAdapter(this::showPaymentOptions);
        rvPayments.setAdapter(paymentAdapter);

        RecyclerView rvWorkerPayments = view.findViewById(R.id.recyclerViewWorkerPayments);
        workerPaymentAdapter = new WorkerPaymentAdapter(workerViewModel, this::showWorkerPaymentOptions);
        rvWorkerPayments.setAdapter(workerPaymentAdapter);
    }

    private void observeData() {
        projectViewModel.getProjectById(projectId).observe(getViewLifecycleOwner(), project -> {
            if (project != null) {
                currentProject = project;
                tvTitle.setText(project.getName());
                tvLocation.setText(project.getLocation());
                tvClient.setText(String.format("%s\n%s", project.getClientName(), project.getClientContact()));
//                tvRules.setText(project.getRulesOfEngagement() != null && !project.getRulesOfEngagement().isEmpty() ? project.getRulesOfEngagement() : "N/A");
                calculateAllTotals();
            }
        });

        projectViewModel.getItemsForProject(projectId).observe(getViewLifecycleOwner(), projectItems -> {
            currentProjectItems = projectItems;
            itemAdapter.submitList(projectItems);
            calculateAllTotals();
            if (projectItems != null) {
                for (ProjectItem pi : projectItems) {
                    itemViewModel.getItemById(pi.getItemId()).observe(getViewLifecycleOwner(), item -> {
                        if (item != null) itemMap.put(item.getId(), item);
                    });
                    if (pi.getVariantId() != null) {
                        itemViewModel.getVariantById(pi.getVariantId()).observe(getViewLifecycleOwner(), variant -> {
                            if (variant != null) variantMap.put(variant.getId(), variant);
                        });
                    }
                }
            }
        });

        projectViewModel.getActivitiesForProject(projectId).observe(getViewLifecycleOwner(), activities -> {
            currentLabourActivities = activities;
            labourAdapter.submitList(activities);
            calculateAllTotals();
        });

        projectViewModel.getPaymentsForProject(projectId).observe(getViewLifecycleOwner(), payments -> {
            currentPayments = payments;
            paymentAdapter.submitList(payments);
            calculateAllTotals();
        });

        workerViewModel.getPaymentsForProject(projectId).observe(getViewLifecycleOwner(), payments -> {
            currentWorkerPayments = payments;
            workerPaymentAdapter.submitList(payments);
            calculateAllTotals();
        });

        workerViewModel.getAllWorkers().observe(getViewLifecycleOwner(), workers -> {
            allWorkers = workers;
        });
    }

    private void calculateAllTotals() {
        currentMaterialTotal = 0;
        if (currentProjectItems != null) {
            for (ProjectItem item : currentProjectItems) {
                currentMaterialTotal += (item.getQuantity() * item.getQuotedPrice());
            }
        }

        double specificLabourTotal = 0;
        if (currentLabourActivities != null) {
            for (LabourActivity activity : currentLabourActivities) {
                specificLabourTotal += activity.getCost();
            }
        }

        double baseLabour = 0;
        if (currentProject != null) {
            if (currentProject.getLabourPercentage() > 0) {
                baseLabour = (currentProject.getLabourPercentage() / 100.0) * currentMaterialTotal;
            } else {
                baseLabour = currentProject.getLabourCost();
            }
        }

        currentLabourTotal = baseLabour + specificLabourTotal;
        double grandTotal = currentMaterialTotal + currentLabourTotal;

        currentPaidTotal = 0;
        if (currentPayments != null) {
            for (Payment p : currentPayments) currentPaidTotal += p.getAmount();
        }

        currentWorkerWagesTotal = 0;
        if (currentWorkerPayments != null) {
            for (WorkerPayment wp : currentWorkerPayments) {
                currentWorkerWagesTotal += (wp.getWage() + wp.getTransport());
            }
        }

        String matStr = Formatter.formatPrice(requireContext(), currentMaterialTotal);
        String labStr = Formatter.formatPrice(requireContext(), currentLabourTotal);
        String grandStr = Formatter.formatPrice(requireContext(), grandTotal);
        String paidStr = Formatter.formatPrice(requireContext(), currentPaidTotal);
        String balStr = Formatter.formatPrice(requireContext(), grandTotal - currentPaidTotal);
        String wageStr = Formatter.formatPrice(requireContext(), currentWorkerWagesTotal);

        if (tvMaterialTotalTop != null) tvMaterialTotalTop.setText(matStr);
//        if (tvMaterialTotalTable != null) tvMaterialTotalTable.setText(matStr);
        if (tvLabourTotalTop != null) tvLabourTotalTop.setText(labStr);
//        if (tvLabourTotalTable != null) tvLabourTotalTable.setText(labStr);
        if (tvGrandTotalTop != null) tvGrandTotalTop.setText(grandStr);
        if (tvTotalPaidTop != null) tvTotalPaidTop.setText(paidStr);
        if (tvBalanceDueTop != null) tvBalanceDueTop.setText(balStr);
        if (tvWorkerWagesTop != null) tvWorkerWagesTop.setText(wageStr);
    }

    private void showAddWorkerPaymentDialog(@Nullable WorkerPayment existing) {
        if (allWorkers == null || allWorkers.isEmpty()) {
            Toast.makeText(requireContext(), "No workers found. Add them in Settings.", Toast.LENGTH_LONG).show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(existing == null ? "Log Wage/Transport" : "Edit Payment");
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_worker_payment, null);
        AutoCompleteTextView autoCompleteWorkers = dialogView.findViewById(R.id.autoCompleteWorkers);
        TextInputEditText etWage = dialogView.findViewById(R.id.etWage);
        TextInputEditText etTransport = dialogView.findViewById(R.id.etTransport);
        TextInputEditText etDesc = dialogView.findViewById(R.id.etDescription);

        String[] workerNames = new String[allWorkers.size()];
        for (int i = 0; i < allWorkers.size(); i++) workerNames[i] = allWorkers.get(i).getName();
        ArrayAdapter<String> workerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, workerNames);
        autoCompleteWorkers.setAdapter(workerAdapter);

        if (existing != null) {
            for (Worker w : allWorkers) {
                if (w.getId() == existing.getWorkerId()) {
                    autoCompleteWorkers.setText(w.getName(), false);
                    break;
                }
            }
            etWage.setText(String.valueOf(existing.getWage()));
            etTransport.setText(String.valueOf(existing.getTransport()));
            etDesc.setText(existing.getDescription());
        }

        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String selectedName = autoCompleteWorkers.getText().toString();
            String wageStr = etWage.getText().toString();
            String transStr = etTransport.getText().toString();
            String desc = etDesc.getText().toString();

            Worker selectedWorker = null;
            for (Worker w : allWorkers) {
                if (w.getName().equals(selectedName)) {
                    selectedWorker = w;
                    break;
                }
            }

            if (selectedWorker != null && !wageStr.isEmpty()) {
                double wage = Double.parseDouble(wageStr);
                double transport = transStr.isEmpty() ? 0 : Double.parseDouble(transStr);
                
                if (existing == null) {
                    workerViewModel.insertPayment(new WorkerPayment(projectId, selectedWorker.getId(), wage, transport, desc));
                } else {
                    existing.setWorkerId(selectedWorker.getId());
                    existing.setWage(wage);
                    existing.setTransport(transport);
                    existing.setDescription(desc);
                    workerViewModel.updatePayment(existing);
                }
            }
        });
        builder.setNegativeButton("Cancel", null).show();
    }

    private void showWorkerPaymentOptions(WorkerPayment wp) {
        String[] options = {"Edit Entry", "Delete Entry"};
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Manage Worker Payment").setItems(options, (d, which) -> {
            if (which == 0) showAddWorkerPaymentDialog(wp);
            else workerViewModel.deletePayment(wp);
        }).show();
    }

    private void showAddPaymentDialog(@Nullable Payment existing) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(existing == null ? "Log Payment" : "Edit Payment");
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editAmount = new EditText(requireContext());
        editAmount.setHint("Amount");
        editAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null) editAmount.setText(String.valueOf(existing.getAmount()));
        layout.addView(editAmount);

        final EditText editMethod = new EditText(requireContext());
        editMethod.setHint("Method (Cash, M-Pesa, etc.)");
        if (existing != null) editMethod.setText(existing.getMethod());
        layout.addView(editMethod);

        final EditText editRef = new EditText(requireContext());
        editRef.setHint("Reference ID");
        if (existing != null) editRef.setText(existing.getReference());
        layout.addView(editRef);

        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String amtStr = editAmount.getText().toString();
            if (!amtStr.isEmpty()) {
                try {
                    double amt = Double.parseDouble(amtStr);
                    if (existing == null) projectViewModel.insertPayment(new Payment(projectId, amt, editMethod.getText().toString(), editRef.getText().toString()));
                    else {
                        existing.setAmount(amt);
                        existing.setMethod(editMethod.getText().toString());
                        existing.setReference(editRef.getText().toString());
                        projectViewModel.updatePayment(existing);
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
        builder.setNegativeButton("Cancel", null).show();
    }

    private void showPaymentOptions(Payment p) {
        String[] options = {"Generate Receipt", "Edit Payment", "Remove Payment"};
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Payment Option").setItems(options, (d, which) -> {
            if (which == 0) generateReceiptPdf(p);
            else if (which == 1) showAddPaymentDialog(p);
            else projectViewModel.deletePayment(p);
        }).show();
    }

    private void generateReceiptPdf(Payment p) {
        File file = PdfGenerator.generateReceipt(requireContext(), currentProject, p);
        if (file != null) {
            Uri uri = FileProvider.getUriForFile(requireContext(), "com.nduyuwilson.thitima.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "Share Receipt"));
        }
    }

    private void showLabourActivityOptions(LabourActivity activity) {
        String[] options = {"Generate Invoice", "Edit Activity", "Remove Activity"};
        new MaterialAlertDialogBuilder(requireContext()).setTitle(activity.getName()).setItems(options, (dialog, which) -> {
            if (which == 0) generateLabourActivityInvoicePdf(activity);
            else if (which == 1) showAddLabourDialog(activity);
            else projectViewModel.deleteLabourActivity(activity);
        }).show();
    }

    private void generateLabourActivityInvoicePdf(LabourActivity activity) {
        File file = PdfGenerator.generateLabourActivityInvoice(requireContext(), currentProject, activity);
        if (file != null) {
            Uri uri = FileProvider.getUriForFile(requireContext(), "com.nduyuwilson.thitima.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "Share Labour Invoice"));
        }
    }

    private void showAddLabourDialog(@Nullable LabourActivity existing) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(existing == null ? "Add Labour" : "Edit Labour");
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        final EditText editName = new EditText(requireContext());
        editName.setHint("Activity Name");
        if (existing != null) editName.setText(existing.getName());
        layout.addView(editName);
        final EditText editCost = new EditText(requireContext());
        editCost.setHint("Cost");
        editCost.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null) editCost.setText(String.valueOf(existing.getCost()));
        layout.addView(editCost);
        builder.setView(layout).setPositiveButton("Save", (d, w) -> {
            String name = editName.getText().toString();
            String costS = editCost.getText().toString();
            if (!name.isEmpty() && !costS.isEmpty()) {
                try {
                    double cost = Double.parseDouble(costS);
                    if (existing == null) projectViewModel.insertLabourActivity(new LabourActivity(projectId, name, cost));
                    else { existing.setName(name); existing.setCost(cost); projectViewModel.updateLabourActivity(existing); }
                } catch (NumberFormatException ignored) {}
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void showProjectItemOptions(ProjectItem pi) {
        String[] options = {"Edit Quantity", "Remove Item"};
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Manage Item").setItems(options, (d, w) -> {
            if (w == 0) showEditQuantityDialog(pi);
            else projectViewModel.deleteProjectItem(pi);
        }).show();
    }

    private void showEditQuantityDialog(ProjectItem pi) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quantity, null);
        TextInputEditText et = v.findViewById(R.id.editTextDialogQuantity);
        if (et != null) et.setText(String.valueOf(pi.getQuantity()));
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Edit Quantity").setView(v).setPositiveButton("Update", (d, w) -> {
            if (et != null) {
                String s = et.getText().toString();
                if (!s.isEmpty()) {
                    try { pi.setQuantity(Integer.parseInt(s)); projectViewModel.updateProjectItem(pi); } catch (NumberFormatException ignored) {}
                }
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void generateAndSharePdf(boolean labourOnly) {
        if (currentProject == null) return;
        File file = PdfGenerator.generateInvoice(requireContext(), currentProject, labourOnly ? null : currentProjectItems, currentLabourActivities, itemMap, variantMap);
        if (file != null) {
            Uri uri = FileProvider.getUriForFile(requireContext(), "com.nduyuwilson.thitima.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "Share PDF"));
        }
    }
}
