package com.nduyuwilson.thitima.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.nduyuwilson.thitima.R;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private TextInputEditText editTextBusinessName, editTextUserName, editTextUserNumber, editTextBankDetails, editTextPaybill, editTextPaybillAccount, editTextTillNumber;
    private MaterialButton buttonEditProfile, buttonSaveProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        sharedPreferences = requireActivity().getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);

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
