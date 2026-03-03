package com.nduyuwilson.thitima.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.model.PaymentMethod;
import com.nduyuwilson.thitima.data.repository.BackupRepository;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private TextInputEditText editTextBusinessName, editTextUserName, editTextUserNumber;
    private MaterialButton buttonEditProfile, buttonSaveProfile, buttonAddPaymentMethod;
    private RecyclerView recyclerViewPaymentMethods;
    private PaymentMethodAdapter paymentAdapter;
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private ItemViewModel itemViewModel;
    private BackupRepository backupRepository;

    private final ActivityResultLauncher<String> mGetBackupFile = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    confirmRestore(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        sharedPreferences = requireActivity().getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
        itemViewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        backupRepository = new BackupRepository(requireActivity().getApplication());

        editTextBusinessName = view.findViewById(R.id.editTextBusinessName);
        editTextUserName = view.findViewById(R.id.editTextUserName);
        editTextUserNumber = view.findViewById(R.id.editTextUserNumber);
        
        recyclerViewPaymentMethods = view.findViewById(R.id.recyclerViewPaymentMethods);
        buttonAddPaymentMethod = view.findViewById(R.id.buttonAddPaymentMethod);
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile);

        setupPaymentRecyclerView();
        loadProfileData();

        buttonEditProfile.setOnClickListener(v -> toggleEditMode(true));

        buttonSaveProfile.setOnClickListener(v -> {
            saveProfileData();
            toggleEditMode(false);
        });

        buttonAddPaymentMethod.setOnClickListener(v -> showAddPaymentMethodDialog());

        view.findViewById(R.id.buttonAppearance).setOnClickListener(v -> showThemeDialog());
        view.findViewById(R.id.buttonCurrency).setOnClickListener(v -> showCurrencyDialog());
        view.findViewById(R.id.buttonExport).setOnClickListener(v -> exportCatalogueToCsv());
        view.findViewById(R.id.buttonBackup).setOnClickListener(v -> performFullBackup());
        view.findViewById(R.id.buttonRestore).setOnClickListener(v -> mGetBackupFile.launch("application/zip"));

        view.findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> requireActivity().finish())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupPaymentRecyclerView() {
        paymentAdapter = new PaymentMethodAdapter();
        paymentAdapter.setOnDeleteClickListener(position -> {
            paymentMethods.remove(position);
            paymentAdapter.notifyDataSetChanged();
        });
        recyclerViewPaymentMethods.setAdapter(paymentAdapter);
    }

    private void loadProfileData() {
        editTextBusinessName.setText(sharedPreferences.getString("business_name", "THITIMA ELECTRICALS"));
        editTextUserName.setText(sharedPreferences.getString("user_name", ""));
        editTextUserNumber.setText(sharedPreferences.getString("user_number", ""));
        
        String json = sharedPreferences.getString("payment_methods_json", "[]");
        Type type = new TypeToken<List<PaymentMethod>>() {}.getType();
        paymentMethods = new Gson().fromJson(json, type);
        paymentAdapter.setMethods(paymentMethods);
    }

    private void toggleEditMode(boolean editing) {
        editTextBusinessName.setEnabled(editing);
        editTextUserName.setEnabled(editing);
        editTextUserNumber.setEnabled(editing);
        
        buttonEditProfile.setVisibility(editing ? View.GONE : View.VISIBLE);
        buttonSaveProfile.setVisibility(editing ? View.VISIBLE : View.GONE);
        buttonAddPaymentMethod.setVisibility(editing ? View.VISIBLE : View.GONE);
        paymentAdapter.setEditing(editing);
    }

    private void saveProfileData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("business_name", editTextBusinessName.getText().toString().trim());
        editor.putString("user_name", editTextUserName.getText().toString().trim());
        editor.putString("user_number", editTextUserNumber.getText().toString().trim());
        
        String json = new Gson().toJson(paymentMethods);
        editor.putString("payment_methods_json", json);
        
        editor.apply();
        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
    }

    private void showAddPaymentMethodDialog() {
        String[] types = {"Bank Account", "M-Pesa Paybill", "M-Pesa Till"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Payment Type")
                .setItems(types, (dialog, which) -> {
                    switch (which) {
                        case 0: showBankDialog(); break;
                        case 1: showPaybillDialog(); break;
                        case 2: showTillDialog(); break;
                    }
                })
                .show();
    }

    private void showBankDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editBankName = new EditText(requireContext());
        editBankName.setHint("Bank Name (e.g. KCB)");
        layout.addView(editBankName);

        final EditText editAccNumber = new EditText(requireContext());
        editAccNumber.setHint("Account Number");
        editAccNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(editAccNumber);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Bank Account")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String bank = editBankName.getText().toString().trim();
                    String acc = editAccNumber.getText().toString().trim();
                    if (!bank.isEmpty() && !acc.isEmpty()) {
                        paymentMethods.add(new PaymentMethod(PaymentMethod.Type.BANK, "Bank", acc, bank));
                        paymentAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPaybillDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editPaybill = new EditText(requireContext());
        editPaybill.setHint("Paybill Number");
        editPaybill.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(editPaybill);

        final EditText editAcc = new EditText(requireContext());
        editAcc.setHint("Account Name/Number");
        layout.addView(editAcc);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add M-Pesa Paybill")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String pb = editPaybill.getText().toString().trim();
                    String acc = editAcc.getText().toString().trim();
                    if (!pb.isEmpty() && !acc.isEmpty()) {
                        paymentMethods.add(new PaymentMethod(PaymentMethod.Type.PAYBILL, "M-Pesa", pb, acc));
                        paymentAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTillDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText editTill = new EditText(requireContext());
        editTill.setHint("Till Number");
        editTill.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(editTill);

        final EditText editLabel = new EditText(requireContext());
        editLabel.setHint("Label (e.g. Shop Name)");
        layout.addView(editLabel);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add M-Pesa Till")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String till = editTill.getText().toString().trim();
                    String label = editLabel.getText().toString().trim();
                    if (!till.isEmpty()) {
                        paymentMethods.add(new PaymentMethod(PaymentMethod.Type.TILL, label, till, ""));
                        paymentAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportCatalogueToCsv() {
        itemViewModel.getAllItems().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) {
                Toast.makeText(requireContext(), "No items to export", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder csvData = new StringBuilder();
            csvCatalogueHeader(csvData);

            for (Item item : items) {
                csvData.append(item.getId()).append(",")
                        .append("\"").append(item.getName()).append("\",")
                        .append("\"").append(item.getDescription().replace("\n", " ")).append("\",")
                        .append(item.getBuyingPrice()).append(",")
                        .append(item.getSellingPrice()).append("\n");
            }

            try {
                File file = new File(requireContext().getExternalCacheDir(), "Catalogue_Export.csv");
                FileOutputStream out = new FileOutputStream(file);
                out.write(csvData.toString().getBytes());
                out.close();

                Uri contentUri = FileProvider.getUriForFile(requireContext(), "com.nduyuwilson.thitima.fileprovider", file);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/csv");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Thitima Catalogue Export");
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Export Catalogue via"));

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void csvCatalogueHeader(StringBuilder sb) {
        sb.append("ID,Item Name,Description,Buying Price,Selling Price\n");
    }

    private void performFullBackup() {
        Future<File> backupFuture = backupRepository.createFullBackupZip();
        new Thread(() -> {
            try {
                File zipFile = backupFuture.get();
                requireActivity().runOnUiThread(() -> {
                    if (zipFile != null && zipFile.exists()) {
                        Uri contentUri = FileProvider.getUriForFile(requireContext(), "com.nduyuwilson.thitima.fileprovider", zipFile);
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("application/zip");
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Thitima Full Data & Media Backup");
                        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "Save/Share Backup via"));
                    } else {
                        Toast.makeText(requireContext(), "Backup failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (InterruptedException | ExecutionException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Backup error", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void confirmRestore(Uri uri) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Restore Data & Media")
                .setMessage("Warning: Restoring will overwrite all current data AND images. This cannot be undone. Continue?")
                .setPositiveButton("Restore Now", (dialog, which) -> {
                    backupRepository.restoreFromZip(requireContext(), uri, new BackupRepository.RestoreCallback() {
                        @Override
                        public void onSuccess() {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Full restoration successful", Toast.LENGTH_LONG).show();
                                loadProfileData(); // Refresh UI
                            });
                        }

                        @Override
                        public void onError(String message) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Restoration failed: " + message, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemeDialog() {
        String[] themes = {"Light", "Dark", "System Default"};
        int checkedItem = sharedPreferences.getInt("theme_mode", 2);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Appearance")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("theme_mode", which);
                    editor.apply();

                    switch (which) {
                        case 0: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
                        case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
                        case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void showCurrencyDialog() {
        String[] currencies = {"Ksh", "$", "€", "£", "UGX", "TZS"};
        String currentCurrency = sharedPreferences.getString("currency_symbol", "Ksh");
        
        int checkedItem = 0;
        for (int i = 0; i < currencies.length; i++) {
            if (currencies[i].equals(currentCurrency)) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Currency")
                .setSingleChoiceItems(currencies, checkedItem, (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("currency_symbol", currencies[which]);
                    editor.apply();
                    dialog.dismiss();
                })
                .show();
    }
}
