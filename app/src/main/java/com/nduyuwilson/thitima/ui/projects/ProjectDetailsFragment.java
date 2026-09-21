package com.nduyuwilson.thitima.ui.projects;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.AppDatabase;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.entity.ItemVariant;
import com.nduyuwilson.thitima.data.entity.LabourActivity;
import com.nduyuwilson.thitima.data.entity.Payment;
import com.nduyuwilson.thitima.data.entity.Project;
import com.nduyuwilson.thitima.data.entity.ProjectItem;
import com.nduyuwilson.thitima.data.entity.Worker;
import com.nduyuwilson.thitima.data.entity.WorkerPayment;
import com.nduyuwilson.thitima.util.Formatter;
import com.nduyuwilson.thitima.util.MpesaParser;
import com.nduyuwilson.thitima.util.PdfGenerator;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;
import com.nduyuwilson.thitima.viewmodel.ProjectViewModel;
import com.nduyuwilson.thitima.viewmodel.WorkerViewModel;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
    private TextView tvTitle, tvLocation, tvClient, tvStatus, tvClientName, tvClientContact, tvClientAvatar, tvBalanceDueLabel;
    private TextView tvGrandTotalTop, tvGrandTotalLabel, tvMaterialTotalTop, tvLabourTotalTop, tvTotalPaidTop, tvBalanceDueTop, tvWorkerWagesTop;
    private TextView tvSubtotalTop, tvVatTotalTop;
    private View layoutVatRow;
    private View buttonCallClient, buttonSmsClient;

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

    private interface OnSmsSelectedListener {
        void onSmsSelected(String smsBody);
    }
    private OnSmsSelectedListener pendingSmsListener;

    private final ActivityResultLauncher<String> requestSmsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showSmsPickerInternal();
                } else {
                    Toast.makeText(requireContext(), "SMS permission denied. You can still paste M-Pesa SMS directly.", Toast.LENGTH_LONG).show();
                }
            });

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
        tvStatus = view.findViewById(R.id.tvProjectStatus);
        tvClientName = view.findViewById(R.id.tvClientName);
        tvClientContact = view.findViewById(R.id.tvClientContact);
        tvClientAvatar = view.findViewById(R.id.tvClientAvatar);
        tvBalanceDueLabel = view.findViewById(R.id.tvBalanceDueLabel);
        tvGrandTotalTop = view.findViewById(R.id.tvGrandTotalTop);
        tvGrandTotalLabel = view.findViewById(R.id.tvGrandTotalLabel);
        tvSubtotalTop = view.findViewById(R.id.tvSubtotalTop);
        tvVatTotalTop = view.findViewById(R.id.tvVatTotalTop);
        layoutVatRow = view.findViewById(R.id.layoutVatRow);
        tvMaterialTotalTop = view.findViewById(R.id.tvMaterialTotalTop);
        tvLabourTotalTop = view.findViewById(R.id.tvLabourTotalTop);
        tvTotalPaidTop = view.findViewById(R.id.tvTotalPaidTop);
        tvBalanceDueTop = view.findViewById(R.id.tvBalanceDueTop);
        tvWorkerWagesTop = view.findViewById(R.id.tvWorkerWagesTop);
        buttonCallClient = view.findViewById(R.id.buttonCallClient);
        buttonSmsClient = view.findViewById(R.id.buttonSmsClient);

        if (buttonCallClient != null) {
            buttonCallClient.setOnClickListener(v -> {
                if (currentProject != null && currentProject.getClientContact() != null && !currentProject.getClientContact().isEmpty()) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentProject.getClientContact()));
                    startActivity(dialIntent);
                } else {
                    Toast.makeText(requireContext(), "No client contact number available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (buttonSmsClient != null) {
            buttonSmsClient.setOnClickListener(v -> {
                if (currentProject != null && currentProject.getClientContact() != null && !currentProject.getClientContact().isEmpty()) {
                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + currentProject.getClientContact()));
                    startActivity(smsIntent);
                } else {
                    Toast.makeText(requireContext(), "No client contact number available", Toast.LENGTH_SHORT).show();
                }
            });
        }

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

        View buttonHeaderEdit = view.findViewById(R.id.buttonHeaderEditProject);
        if (buttonHeaderEdit != null) {
            buttonHeaderEdit.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putInt("projectId", projectId);
                Navigation.findNavController(view).navigate(R.id.action_projectDetailsFragment_to_addProjectFragment, bundle);
            });
        }

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
                if (tvClient != null) {
                    tvClient.setText(String.format("%s\n%s", project.getClientName(), project.getClientContact()));
                }
                if (tvClientName != null) {
                    tvClientName.setText(project.getClientName());
                }
                if (tvClientContact != null) {
                    tvClientContact.setText(project.getClientContact());
                }
                if (tvClientAvatar != null) {
                    String name = project.getClientName() != null ? project.getClientName().trim() : "";
                    if (!name.isEmpty()) {
                        String[] parts = name.split("\\s+");
                        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                            tvClientAvatar.setText(("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase());
                        } else {
                            tvClientAvatar.setText(name.substring(0, Math.min(2, name.length())).toUpperCase());
                        }
                    } else {
                        tvClientAvatar.setText("CL");
                    }
                }
                updateStatusBadge(project.getStatus());
                calculateTotals();
            }
        });

        projectViewModel.getItemsForProject(projectId).observe(getViewLifecycleOwner(), projectItems -> {
            currentProjectItems = projectItems;
            itemAdapter.submitList(projectItems);
            calculateTotals();
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
            calculateTotals();
        });

        projectViewModel.getPaymentsForProject(projectId).observe(getViewLifecycleOwner(), payments -> {
            currentPayments = payments;
            paymentAdapter.submitList(payments);
            calculateTotals();
        });

        workerViewModel.getPaymentsForProject(projectId).observe(getViewLifecycleOwner(), payments -> {
            currentWorkerPayments = payments;
            workerPaymentAdapter.submitList(payments);
            calculateTotals();
        });

        workerViewModel.getAllWorkers().observe(getViewLifecycleOwner(), workers -> {
            allWorkers = workers;
        });
    }

    private void calculateTotals() {
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
        double subtotal = currentMaterialTotal + currentLabourTotal;
        double vatAmount = 0;
        double grandTotal = subtotal;

        if (currentProject != null && currentProject.isIncludeVat()) {
            vatAmount = subtotal * 0.16;
            grandTotal = subtotal + vatAmount;
            if (layoutVatRow != null) layoutVatRow.setVisibility(View.VISIBLE);
            if (tvSubtotalTop != null) tvSubtotalTop.setText(Formatter.formatPrice(requireContext(), subtotal));
            if (tvVatTotalTop != null) tvVatTotalTop.setText(Formatter.formatPrice(requireContext(), vatAmount));
            if (tvGrandTotalLabel != null) tvGrandTotalLabel.setText("Total Quote (Incl. 16% VAT)");
        } else {
            if (layoutVatRow != null) layoutVatRow.setVisibility(View.GONE);
            if (tvGrandTotalLabel != null) tvGrandTotalLabel.setText("Total Quote");
        }

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
        double balanceDue = grandTotal - currentPaidTotal;
        String balStr = Formatter.formatPrice(requireContext(), balanceDue);
        String wageStr = Formatter.formatPrice(requireContext(), currentWorkerWagesTotal);

        if (tvMaterialTotalTop != null) tvMaterialTotalTop.setText(matStr);
        if (tvLabourTotalTop != null) tvLabourTotalTop.setText(labStr);
        if (tvGrandTotalTop != null) tvGrandTotalTop.setText(grandStr);
        if (tvTotalPaidTop != null) tvTotalPaidTop.setText(paidStr);
        if (tvWorkerWagesTop != null) tvWorkerWagesTop.setText(wageStr);

        if (balanceDue <= 0 && grandTotal > 0) {
            if (tvBalanceDueLabel != null) tvBalanceDueLabel.setText("Payment Status");
            if (tvBalanceDueTop != null) {
                tvBalanceDueTop.setText("Fully Settled");
                tvBalanceDueTop.setTextColor(Color.parseColor("#10B981"));
            }
        } else {
            if (tvBalanceDueLabel != null) tvBalanceDueLabel.setText("Outstanding Balance Due");
            if (tvBalanceDueTop != null) {
                tvBalanceDueTop.setText(balStr);
                tvBalanceDueTop.setTextColor(Color.parseColor("#DC2626"));
            }
        }
    }

    private void updateStatusBadge(String status) {
        if (tvStatus == null) return;
        if (status == null || status.trim().isEmpty()) status = "QUOTATION";
        tvStatus.setText(status.toUpperCase());

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(32);

        switch (status.toUpperCase()) {
            case "COMPLETED":
            case "PAID":
                bg.setColor(Color.parseColor("#DCFCE7"));
                tvStatus.setTextColor(Color.parseColor("#15803D"));
                break;
            case "ONGOING":
                bg.setColor(Color.parseColor("#E0F2FE"));
                tvStatus.setTextColor(Color.parseColor("#0369A1"));
                break;
            case "QUOTATION":
            default:
                bg.setColor(Color.parseColor("#FEF3C7"));
                tvStatus.setTextColor(Color.parseColor("#B45309"));
                break;
        }
        tvStatus.setBackground(bg);
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
        if (currentProject == null) return;
        Toast.makeText(requireContext(), "Generating Labour Invoice...", Toast.LENGTH_SHORT).show();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            File pdfFile = PdfGenerator.generateLabourActivityInvoice(requireContext(), currentProject, activity);
            requireActivity().runOnUiThread(() -> {
                if (pdfFile != null && isAdded()) {
                    sharePdfDirectly(pdfFile, "Labour Invoice");
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                }
            });
        });
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
                    if (existing == null)
                        projectViewModel.insertLabourActivity(new LabourActivity(projectId, name, cost));
                    else {
                        existing.setName(name);
                        existing.setCost(cost);
                        projectViewModel.updateLabourActivity(existing);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }).setNegativeButton("Cancel", null).show();
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
                try {
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
                } catch (NumberFormatException ignored) {
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

    private void showPaymentOptions(Payment p) {
        String[] options = {"Generate Receipt", "Edit Payment", "Remove Payment"};
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Payment Option").setItems(options, (d, which) -> {
            if (which == 0) generateReceiptPdf(p);
            else if (which == 1) showAddPaymentDialog(p);
            else projectViewModel.deletePayment(p);
        }).show();
    }

    private void generateReceiptPdf(Payment p) {
        if (currentProject == null) return;
        Toast.makeText(requireContext(), "Generating Receipt...", Toast.LENGTH_SHORT).show();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            File pdfFile = PdfGenerator.generateReceipt(requireContext(), currentProject, p);
            requireActivity().runOnUiThread(() -> {
                if (pdfFile != null && isAdded()) {
                    sharePdfDirectly(pdfFile, "Official Receipt #" + p.getId());
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showAddPaymentDialog(@Nullable Payment existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_payment, null);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etPaymentAmount);
        AutoCompleteTextView actvMethod = dialogView.findViewById(R.id.actvPaymentMethod);
        TextInputEditText etReference = dialogView.findViewById(R.id.etPaymentReference);
        MaterialButton btnPaste = dialogView.findViewById(R.id.btnPasteMpesa);
        MaterialButton btnPickSms = dialogView.findViewById(R.id.btnPickMpesaSms);
        LinearLayout layoutPreview = dialogView.findViewById(R.id.layoutMpesaPreview);
        TextView tvRaw = dialogView.findViewById(R.id.tvMpesaRawMessage);
        TextView tvClear = dialogView.findViewById(R.id.tvClearMpesa);

        // Setup methods dropdown
        String[] methods = new String[]{"M-Pesa", "Cash", "Bank Transfer", "Cheque", "Other"};
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, methods);
        actvMethod.setAdapter(methodAdapter);

        final String[] capturedMpesaMessage = {existing != null && existing.getMpesaMessage() != null ? existing.getMpesaMessage() : ""};

        if (existing != null) {
            etAmount.setText(String.format(java.util.Locale.US, "%.2f", existing.getAmount()));
            actvMethod.setText(existing.getMethod(), false);
            etReference.setText(existing.getReference());
            if (!capturedMpesaMessage[0].isEmpty()) {
                layoutPreview.setVisibility(View.VISIBLE);
                tvRaw.setText(capturedMpesaMessage[0]);
            }
        }

        // Helper to apply M-Pesa parsed results
        OnSmsSelectedListener applyMpesa = smsBody -> {
            MpesaParser.MpesaResult result = MpesaParser.parse(smsBody);
            if (result.amount > 0) {
                etAmount.setText(String.format(java.util.Locale.US, "%.2f", result.amount));
            }
            if (!result.transactionCode.isEmpty()) {
                etReference.setText(result.transactionCode);
            }
            actvMethod.setText("M-Pesa", false);
            capturedMpesaMessage[0] = smsBody;
            layoutPreview.setVisibility(View.VISIBLE);
            tvRaw.setText(smsBody);
            Toast.makeText(requireContext(), "M-Pesa details extracted successfully!", Toast.LENGTH_SHORT).show();
        };

        btnPaste.setOnClickListener(v -> pasteMpesaSms(applyMpesa));
        btnPickSms.setOnClickListener(v -> openMpesaSmsPicker(applyMpesa));

        tvClear.setOnClickListener(v -> {
            capturedMpesaMessage[0] = "";
            layoutPreview.setVisibility(View.GONE);
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? "Log Payment" : "Edit Payment")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                    String method = actvMethod.getText().toString().trim();
                    String ref = etReference.getText() != null ? etReference.getText().toString().trim() : "";

                    if (!amtStr.isEmpty()) {
                        try {
                            double amt = Double.parseDouble(amtStr);
                            if (existing == null) {
                                Payment p = new Payment(projectId, amt, method, ref);
                                p.setMpesaMessage(capturedMpesaMessage[0]);
                                projectViewModel.insertPayment(p);
                            } else {
                                existing.setAmount(amt);
                                existing.setMethod(method);
                                existing.setReference(ref);
                                existing.setMpesaMessage(capturedMpesaMessage[0]);
                                projectViewModel.updatePayment(existing);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openMpesaSmsPicker(OnSmsSelectedListener listener) {
        this.pendingSmsListener = listener;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            showSmsPickerInternal();
        } else {
            requestSmsPermissionLauncher.launch(Manifest.permission.READ_SMS);
        }
    }

    private void showSmsPickerInternal() {
        List<String> mpesaMessages = new ArrayList<>();
        try {
            Uri smsUri = Uri.parse("content://sms/inbox");
            Cursor cursor = requireContext().getContentResolver().query(
                    smsUri,
                    new String[]{"address", "body", "date"},
                    "body LIKE '%Confirmed%' OR address LIKE '%MPESA%'",
                    null,
                    "date DESC LIMIT 25"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    if (body != null && (body.contains("Confirmed") || body.contains("received"))) {
                        mpesaMessages.add(body);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not read SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        if (mpesaMessages.isEmpty()) {
            Toast.makeText(requireContext(), "No M-Pesa confirmation SMS found in inbox. Try pasting directly.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] displayItems = new String[mpesaMessages.size()];
        for (int i = 0; i < mpesaMessages.size(); i++) {
            String msg = mpesaMessages.get(i);
            displayItems[i] = msg.length() > 85 ? msg.substring(0, 85) + "..." : msg;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select M-Pesa Payment SMS")
                .setItems(displayItems, (dialog, which) -> {
                    if (pendingSmsListener != null) {
                        pendingSmsListener.onSmsSelected(mpesaMessages.get(which));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pasteMpesaSms(OnSmsSelectedListener listener) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        String clipText = "";
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (text != null) clipText = text.toString().trim();
        }

        if (!clipText.isEmpty() && (clipText.contains("Confirmed") || clipText.contains("received") || clipText.contains("Ksh"))) {
            listener.onSmsSelected(clipText);
        } else {
            View pasteView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_paste_mpesa, null);
            TextInputEditText etPaste = pasteView.findViewById(R.id.etPasteSmsText);
            if (!clipText.isEmpty()) etPaste.setText(clipText);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Paste M-Pesa Confirmation SMS")
                    .setView(pasteView)
                    .setPositiveButton("Parse & Apply", (d, w) -> {
                        String input = etPaste.getText() != null ? etPaste.getText().toString().trim() : "";
                        if (!input.isEmpty()) {
                            listener.onSmsSelected(input);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
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
                    try {
                        pi.setQuantity(Integer.parseInt(s));
                        projectViewModel.updateProjectItem(pi);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void generateAndSharePdf(boolean labourOnly) {
        if (currentProject == null) return;
        Toast.makeText(requireContext(), "Generating " + (labourOnly ? "Labour Invoice" : "Quotation") + "...", Toast.LENGTH_SHORT).show();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            File pdfFile = PdfGenerator.generateInvoice(requireContext(), currentProject,
                    labourOnly ? null : currentProjectItems,
                    currentLabourActivities,
                    itemMap, variantMap);

            requireActivity().runOnUiThread(() -> {
                if (pdfFile != null && isAdded()) {
                    sharePdfDirectly(pdfFile, labourOnly ? "Labour Invoice" : "Quotation");
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void sharePdfDirectly(File pdfFile, String title) {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(requireContext(), "PDF file could not be found", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(requireContext(), "com.nduyuwilson.thitima.fileprovider", pdfFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        ClipData clipData = new ClipData(
                new ClipDescription(pdfFile.getName(), new String[]{"application/pdf"}),
                new ClipData.Item(uri)
        );
        intent.setClipData(clipData);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share " + title));
    }
}
