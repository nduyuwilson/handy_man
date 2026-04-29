package com.nduyuwilson.thitima.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages local caching of Firebase auth state and subscription status.
 * Offline grace period: user keeps premium access for GRACE_PERIOD_MS after
 * the last successful online verification (default 7 days), allowing them to
 * work in the field without losing access.
 */
public class AuthManager {

    private static final String PREFS_NAME  = "thitima_auth";
    private static final String KEY_UID     = "uid";
    private static final String KEY_TOKEN   = "id_token";
    private static final String KEY_PREMIUM = "is_premium";
    private static final String KEY_LAST_VERIFIED = "last_verified_at";

    // 7-day offline grace period (adjustable)
    public static final long GRACE_PERIOD_MS = 7L * 24 * 60 * 60 * 1000;

    // -------------------------------------------------------------------------
    // Save / Clear
    // -------------------------------------------------------------------------

    /** Call this after a successful Firebase sign-in. */
    public static void saveAuthInfo(Context ctx, String uid, String idToken) {
        prefs(ctx).edit()
                .putString(KEY_UID,   uid)
                .putString(KEY_TOKEN, idToken)
                .apply();
    }

    /** Call this after a successful online subscription verification. */
    public static void saveSubscriptionStatus(Context ctx, boolean isPremium) {
        prefs(ctx).edit()
                .putBoolean(KEY_PREMIUM,       isPremium)
                .putLong(KEY_LAST_VERIFIED,    System.currentTimeMillis())
                .apply();
    }

    /** Call this on sign-out. */
    public static void clearAuthInfo(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    // -------------------------------------------------------------------------
    // Read helpers
    // -------------------------------------------------------------------------

    public static String getUid(Context ctx) {
        return prefs(ctx).getString(KEY_UID, null);
    }

    public static String getIdToken(Context ctx) {
        return prefs(ctx).getString(KEY_TOKEN, null);
    }

    public static boolean isPremiumCached(Context ctx) {
        return prefs(ctx).getBoolean(KEY_PREMIUM, false);
    }

    public static long getLastVerifiedAt(Context ctx) {
        return prefs(ctx).getLong(KEY_LAST_VERIFIED, 0L);
    }

    // -------------------------------------------------------------------------
    // Access gate
    // -------------------------------------------------------------------------

    /**
     * Returns true if the user is allowed to use the app right now.
     * Rules:
     *  1. Must have a stored UID (i.e. has logged in before).
     *  2. Must be marked premium.
     *  3. Last verification must be within the grace period.
     * This runs completely offline.
     */
    public static boolean hasAccess(Context ctx) {
        if (getUid(ctx) == null)            return false;
        if (!isPremiumCached(ctx))          return false;
        long elapsed = System.currentTimeMillis() - getLastVerifiedAt(ctx);
        return elapsed <= GRACE_PERIOD_MS;
    }

    /**
     * Days remaining in the current grace period (for informational UI display).
     * Returns 0 if the period has already elapsed.
     */
    public static int graceDaysRemaining(Context ctx) {
        long elapsed = System.currentTimeMillis() - getLastVerifiedAt(ctx);
        long remaining = GRACE_PERIOD_MS - elapsed;
        if (remaining <= 0) return 0;
        return (int) (remaining / (24 * 60 * 60 * 1000L));
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
