package com.nduyuwilson.thitima.auth;

import com.google.firebase.auth.FirebaseUser;

public class AdminManager {

    private static final String[] SUPER_ADMIN_EMAILS = {
            "nduyuwilson@gmail.com"
    };

    public static boolean isAdmin(FirebaseUser user) {
        if (user == null || user.getEmail() == null) {
            return false;
        }
        String userEmail = user.getEmail().trim().toLowerCase();
        for (String adminEmail : SUPER_ADMIN_EMAILS) {
            if (userEmail.equalsIgnoreCase(adminEmail)) {
                return true;
            }
        }
        return false;
    }
}
