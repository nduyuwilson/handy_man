package com.nduyuwilson.thitima.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DecimalFormat;
import java.util.Locale;

public class Formatter {
    // DecimalFormat is not thread-safe, so we create a new instance or synchronize access
    private static DecimalFormat getDecimalFormat() {
        return new DecimalFormat("#,##0.00");
    }

    public static String formatPrice(Context context, double amount) {
        SharedPreferences prefs = AppPrefs.getPreferences(context);
        String currency = prefs.getString("currency_symbol", "Ksh");
        return currency + " " + getDecimalFormat().format(amount);
    }
    
    public static String formatNumber(double amount) {
        return getDecimalFormat().format(amount);
    }

    public static String getCurrencySymbol(Context context) {
        SharedPreferences prefs = AppPrefs.getPreferences(context);
        return prefs.getString("currency_symbol", "Ksh");
    }
}
