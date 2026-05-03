package com.nduyuwilson.thitima.auth;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nduyuwilson.thitima.MainActivity;
import com.nduyuwilson.thitima.R;

import java.util.concurrent.TimeUnit;

/**
 * Login screen using Firebase Email/Password authentication.
 *
 * Offline behaviour:
 *  - If the device is offline but there is a valid locally-cached session
 *    (within GRACE_PERIOD_MS), the user is immediately forwarded to MainActivity.
 *  - If no cached session exists the user must connect once to authenticate.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator progressBar;
    private TextView tvError, tvOfflineNotice, tvRegisterLink;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // ---------------------------------------------------------------
        // Fast path: already authenticated and within grace period
        // ---------------------------------------------------------------
        if (AuthManager.hasAccess(this)) {
            goToMain();
            return;
        }

        bindViews();

        // Show offline notice if no network
        if (!isNetworkAvailable()) {
            tvOfflineNotice.setVisibility(View.VISIBLE);
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void bindViews() {
        tilEmail       = findViewById(R.id.til_email);
        tilPassword    = findViewById(R.id.til_password);
        etEmail        = findViewById(R.id.et_email);
        etPassword     = findViewById(R.id.et_password);
        btnLogin       = findViewById(R.id.btn_login);
        progressBar    = findViewById(R.id.progress_bar);
        tvError        = findViewById(R.id.tv_error);
        tvOfflineNotice = findViewById(R.id.tv_offline_notice);
        tvRegisterLink = findViewById(R.id.tv_register_link);
    }

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------

    private void attemptLogin() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        // Basic validation
        tilEmail.setError(null);
        tilPassword.setError(null);
        tvError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Enter your email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Enter your password");
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        onSignInSuccess(mAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Authentication failed.";
                        showError(msg);
                    }
                });
    }

    private void onSignInSuccess(FirebaseUser user) {
        if (user == null) { showError("Sign-in failed."); return; }

        // Retrieve the Firebase ID token, then check subscription in Firestore
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            String token = tokenTask.isSuccessful() && tokenTask.getResult() != null
                    ? tokenTask.getResult().getToken()
                    : "";

            // Save basic auth info locally (for offline re-launches)
            AuthManager.saveAuthInfo(this, user.getUid(), token);

            // Now verify subscription online
            verifySubscription(user.getUid());
            Log.d("LoginActivity", "onSignInSuccess: " + user.getUid());
        });
    }

    private void verifySubscription(String uid) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db.collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        com.google.firebase.firestore.DocumentSnapshot doc = task.getResult();
                        
                        // 1. Check Device ID
                        String currentDeviceId = AuthManager.getDeviceId(this);
                        String storedDeviceId = doc.getString("deviceId");

                        if (storedDeviceId == null) {
                            // First time login - link this device
                            db.collection("users").document(uid).update("deviceId", currentDeviceId);
                        } else if (!storedDeviceId.equals(currentDeviceId)) {
                            // Device mismatch
                            mAuth.signOut();
                            AuthManager.clearAuthInfo(this);
                            showError("This account is linked to another device. Please contact support to reset.");
                            return;
                        }

                        // 2. Check Subscription
                        Boolean isPremium = doc.getBoolean("isPremium");
                        boolean premium = Boolean.TRUE.equals(isPremium);
                        AuthManager.saveSubscriptionStatus(this, premium);

                        if (premium) {
                            schedulePeriodicSyncWorker();
                            goToMain();
                        } else {
                            showError("No active subscription found. Please purchase a plan to access Thitima.");
                        }
                    } else {
                        Log.d("LoginActivity", "verifySubscription: failed", task.getException());
                        // Firestore call failed – apply cached result if available
                        if (AuthManager.isPremiumCached(this)) {
                            schedulePeriodicSyncWorker();
                            goToMain();
                        } else {
                            showError("Could not verify subscription. Please check your connection.");
                        }
                    }
                });
    }

    // ---------------------------------------------------------------
    // WorkManager periodic sync (every 12 h, only when connected)
    // ---------------------------------------------------------------

    private void schedulePeriodicSyncWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(SubscriptionSyncWorker.class, 12, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "subscription_sync",
                ExistingPeriodicWorkPolicy.KEEP,   // don't reset if already scheduled
                syncRequest
        );
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // remove LoginActivity from back stack
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in…" : "Sign In");
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
