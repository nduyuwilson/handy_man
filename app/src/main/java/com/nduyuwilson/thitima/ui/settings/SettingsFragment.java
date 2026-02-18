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
    private TextInputEditText editTextUserName, editTextUserNumber, editTextPaymentOptions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        sharedPreferences = requireActivity().getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);

        editTextUserName = view.findViewById(R.id.editTextUserName);
        editTextUserNumber = view.findViewById(R.id.editTextUserNumber);
        editTextPaymentOptions = view.findViewById(R.id.editTextPaymentOptions);

        // Load existing values
        editTextUserName.setText(sharedPreferences.getString("user_name", ""));
        editTextUserNumber.setText(sharedPreferences.getString("user_number", ""));
        editTextPaymentOptions.setText(sharedPreferences.getString("payment_details", ""));

        MaterialButton buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile);
        buttonSaveProfile.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_name", editTextUserName.getText().toString().trim());
            editor.putString("user_number", editTextUserNumber.getText().toString().trim());
            editor.putString("payment_details", editTextPaymentOptions.getText().toString().trim());
            editor.apply();
            Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show();
        });

        MaterialButton buttonAppearance = view.findViewById(R.id.buttonAppearance);
        buttonAppearance.setOnClickListener(v -> showThemeDialog());

        MaterialButton buttonCurrency = view.findViewById(R.id.buttonCurrency);
        buttonCurrency.setOnClickListener(v -> showCurrencyDialog());

        view.findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        requireActivity().finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
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
                        case 0:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case 1:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                        case 2:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
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
