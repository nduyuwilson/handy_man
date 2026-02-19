package com.nduyuwilson.thitima;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkTrialPeriod();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navView, navController);
        }
    }

    private void checkTrialPeriod() {
        SharedPreferences prefs = getSharedPreferences("ThitimaPrefs", MODE_PRIVATE);
        long trialStart = prefs.getLong("trial_start_date", 0);
        
        if (trialStart == 0) {
            // First time loading the app, set the trial start date
            trialStart = System.currentTimeMillis();
            prefs.edit().putLong("trial_start_date", trialStart).apply();
        }

        long oneWeekInMillis = 7L * 24 * 60 * 60 * 1000;
        long expiryDate = trialStart + oneWeekInMillis;

        if (System.currentTimeMillis() > expiryDate) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Trial Period Expired")
                    .setMessage("Your 1-week testing period has ended. Please contact the developer (undrix Int @ 0716729060) to activate the full version.")
                    .setCancelable(false)
                    .setPositiveButton("Exit", (dialog, which) -> finish())
                    .show();
        }
    }
}
