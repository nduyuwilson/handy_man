package com.nduyuwilson.thitima.auth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodic WorkManager job that, when the device has network access, queries
 * Firebase Firestore for the user's subscription status and caches the result
 * locally via AuthManager.
 *
 * Schedule: every 12 hours, ONLY when network is connected.
 * This means users can work offline for up to 7 days (grace period) before
 * they must connect to re-verify.
 *
 * Firestore document structure:
 *   users/{uid} → { isPremium: true/false, expiresAt: Timestamp }
 */
public class SubscriptionSyncWorker extends Worker {

    private static final String TAG = "SubSyncWorker";

    public SubscriptionSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // 1. Must be logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No signed-in user – skipping sync.");
            return Result.success(); // not a failure, just nothing to do
        }

        // 2. Fetch subscription doc from Firestore synchronously (Worker runs on background thread)
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean premium = new AtomicBoolean(false);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc != null && doc.exists()) {
                            Boolean isPremium = doc.getBoolean("isPremium");
                            premium.set(Boolean.TRUE.equals(isPremium));
                        }
                    } else {
                        Log.w(TAG, "Firestore fetch failed", task.getException());
                    }
                    latch.countDown();
                });

        try {
            // Wait up to 15 s for the Firestore response
            boolean finished = latch.await(15, TimeUnit.SECONDS);
            if (!finished) {
                Log.w(TAG, "Firestore timed out – will retry next cycle.");
                return Result.retry();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }

        // 3. Persist result locally
        AuthManager.saveSubscriptionStatus(ctx, premium.get());
        Log.d(TAG, "Subscription sync complete. isPremium=" + premium.get());

        return Result.success();
    }
}
