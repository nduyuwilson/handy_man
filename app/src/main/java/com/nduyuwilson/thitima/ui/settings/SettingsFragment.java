package com.nduyuwilson.thitima.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;
import com.nduyuwilson.thitima.data.entity.Item;
import com.nduyuwilson.thitima.data.repository.BackupRepository;
import com.nduyuwilson.thitima.viewmodel.ItemViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private TextInputEditText editTextBusinessName, editTextUserName, editTextUserNumber, editTextBankDetails, editTextPaybill, editTextPaybillAccount, editTextTillNumber;
    private MaterialButton buttonEditProfile, buttonSaveProfile;
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
        editTextBankDetails = view.findViewById(R.id.editTextBankDetails);
        editTextPaybill = view.findViewById(R.id.editTextPaybill);
        editTextPaybillAccount = view.findViewById(R.id.editTextPaybillAccount);
        editTextTillNumber = view.findViewById(R.id.editTextTillNumber);
        
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile);
        buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile);

        loadProfileData();

        buttonEditProfile.setOnClickListener(v -> toggleEditMode(true));

        buttonSaveProfile.setOnClickListener(v -> {
            saveProfileData();
            toggleEditMode(false);
        });

        MaterialButton buttonAppearance = view.findViewById(R.id.buttonAppearance);
        buttonAppearance.setOnClickListener(v -> showThemeDialog());

        MaterialButton buttonCurrency = view.findViewById(R.id.buttonCurrency);
        buttonCurrency.setOnClickListener(v -> showCurrencyDialog());

        MaterialButton buttonExport = view.findViewById(R.id.buttonExport);
        buttonExport.setOnClickListener(v -> exportCatalogueToCsv());

        MaterialButton buttonBackup = view.findViewById(R.id.buttonBackup);
        buttonBackup.setOnClickListener(v -> performFullBackup());

        MaterialButton buttonRestore = view.findViewById(R.id.buttonRestore);
        buttonRestore.setOnClickListener(v -> mGetBackupFile.launch("application/zip"));

        view.findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> requireActivity().finish())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadProfileData() {
        editTextBusinessName.setText(sharedPreferences.getString("business_name", "THITIMA ELECTRICALS"));
        editTextUserName.setText(sharedPreferences.getString("user_name", ""));
        editTextUserNumber.setText(sharedPreferences.getString("user_number", ""));
        editTextBankDetails.setText(sharedPreferences.getString("bank_details", ""));
        editTextPaybill.setText(sharedPreferences.getString("paybill_number", ""));
        editTextPaybillAccount.setText(sharedPreferences.getString("paybill_account", ""));
        editTextTillNumber.setText(sharedPreferences.getString("till_number", ""));
    }

    private void toggleEditMode(boolean editing) {
        editTextBusinessName.setEnabled(editing);
        editTextUserName.setEnabled(editing);
        editTextUserNumber.setEnabled(editing);
        editTextBankDetails.setEnabled(editing);
        editTextPaybill.setEnabled(editing);
        editTextPaybillAccount.setEnabled(editing);
        editTextTillNumber.setEnabled(editing);
        
        buttonEditProfile.setVisibility(editing ? View.GONE : View.VISIBLE);
        buttonSaveProfile.setVisibility(editing ? View.VISIBLE : View.GONE);
    }

    private void saveProfileData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("business_name", editTextBusinessName.getText().toString().trim());
        editor.putString("user_name", editTextUserName.getText().toString().trim());
        editor.putString("user_number", editTextUserNumber.getText().toString().trim());
        editor.putString("bank_details", editTextBankDetails.getText().toString().trim());
        editor.putString("paybill_number", editTextPaybill.getText().toString().trim());
        editor.putString("paybill_account", editTextPaybillAccount.getText().toString().trim());
        editor.putString("till_number", editTextTillNumber.getText().toString().trim());
        editor.apply();
        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
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
