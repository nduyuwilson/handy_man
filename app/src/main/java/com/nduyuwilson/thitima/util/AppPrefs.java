package com.nduyuwilson.thitima.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.nduyuwilson.thitima.auth.AuthManager;

public class AppPrefs {

    public static String getPrefsName(Context context) {
        String uid = AuthManager.getUid(context);
        if (uid != null && !uid.trim().isEmpty()) {
            return "ThitimaPrefs_" + uid.trim();
        }
        return "ThitimaPrefs";
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
    }
}