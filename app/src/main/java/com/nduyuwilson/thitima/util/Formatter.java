package com.nduyuwilson.thitima.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DecimalFormat;
import java.util.Locale;

public class Formatter {
    private static final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    public static String formatPrice(Context context, double amount) {
        SharedPreferences prefs = context.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
        String currency = prefs.getString("currency_symbol", "Ksh");
        return currency + " " + decimalFormat.format(amount);
    }
    
    public static String formatNumber(double amount) {
        return decimalFormat.format(amount);
    }

    public static String getCurrencySymbol(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ThitimaPrefs", Context.MODE_PRIVATE);
        return prefs.getString("currency_symbol", "Ksh");
    }
}
