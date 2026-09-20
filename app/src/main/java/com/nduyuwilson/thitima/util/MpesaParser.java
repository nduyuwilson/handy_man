package com.nduyuwilson.thitima.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to parse M-Pesa SMS confirmation messages.
 * Extracts:
 *  - Transaction Code (e.g., QA12BC34DE)
 *  - Amount (e.g., 15000.00)
 *  - Sender Name & Phone
 *  - Date and Time
 *  - Full raw message for audit reference
 */
public class MpesaParser {

    public static class MpesaResult {
        public boolean isSuccess;
        public String transactionCode = "";
        public double amount = 0.0;
        public String sender = "";
        public String dateTime = "";
        public String rawMessage = "";

        @Override
        public String toString() {
            return "MpesaResult{" +
                    "isSuccess=" + isSuccess +
                    ", transactionCode='" + transactionCode + '\'' +
                    ", amount=" + amount +
                    ", sender='" + sender + '\'' +
                    ", dateTime='" + dateTime + '\'' +
                    '}';
        }
    }

    // Pattern 1: Standard C2B / Person-to-Person: "QA12BC34DE Confirmed. You have received Ksh1,500.00 from JOHN DOE 0712345678 on 20/9/26 at 11:30 AM..."
    private static final Pattern PATTERN_C2B = Pattern.compile(
            "([A-Z0-9]{8,12})\\s+Confirmed\\.?\\s+You have received\\s+Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{2})?)\\s+from\\s+([^\\.\\n]+?)(?:\\s+on\\s+([0-9/\\-]+\\s+at\\s+[0-9:AMPamp\\s]+))?",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern 2: Till / Paybill: "QA12BC34DE Confirmed. on 20/9/26 at 11:30 AM Ksh1,500.00 received from JOHN DOE..."
    private static final Pattern PATTERN_TILL = Pattern.compile(
            "([A-Z0-9]{8,12})\\s+Confirmed\\.?\\s+(?:on\\s+([0-9/\\-]+\\s+at\\s+[0-9:AMPamp\\s]+))?\\s*Ksh\\.?\\s*([0-9,]+(?:\\.[0-9]{2})?)\\s+received from\\s+([^\\.\\n]+)",
            Pattern.CASE_INSENSITIVE
    );

    // General Pattern: Transaction Code at start
    private static final Pattern PATTERN_CODE = Pattern.compile("^([A-Z0-9]{8,12})\\s+Confirmed", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_AMOUNT = Pattern.compile("(?:Ksh|KES)\\.?\\s*([0-9,]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SENDER = Pattern.compile("(?:from)\\s+([A-Za-z\\s]+?)(?:\\s+[0-9]{7,}|\\s+on|\\.|\\,|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DATE = Pattern.compile("on\\s+([0-9]{1,2}[/\\-][0-9]{1,2}[/\\-][0-9]{2,4}\\s+at\\s+[0-9]{1,2}:[0-9]{2}\\s*(?:AM|PM|am|pm)?)", Pattern.CASE_INSENSITIVE);

    public static MpesaResult parse(String smsText) {
        MpesaResult result = new MpesaResult();
        if (smsText == null || smsText.trim().isEmpty()) {
            return result;
        }

        String cleaned = smsText.trim();
        result.rawMessage = cleaned;

        // Try Pattern 1
        Matcher m1 = PATTERN_C2B.matcher(cleaned);
        if (m1.find()) {
            result.transactionCode = m1.group(1).trim();
            result.amount = parseAmount(m1.group(2));
            result.sender = cleanSender(m1.group(3));
            if (m1.groupCount() >= 4 && m1.group(4) != null) {
                result.dateTime = m1.group(4).trim();
            }
            result.isSuccess = true;
            return result;
        }

        // Try Pattern 2
        Matcher m2 = PATTERN_TILL.matcher(cleaned);
        if (m2.find()) {
            result.transactionCode = m2.group(1).trim();
            if (m2.group(2) != null) {
                result.dateTime = m2.group(2).trim();
            }
            result.amount = parseAmount(m2.group(3));
            result.sender = cleanSender(m2.group(4));
            result.isSuccess = true;
            return result;
        }

        // Fallback Extraction: Extract components individually
        Matcher codeMatcher = PATTERN_CODE.matcher(cleaned);
        if (codeMatcher.find()) {
            result.transactionCode = codeMatcher.group(1).trim();
            result.isSuccess = true;
        }

        Matcher amtMatcher = PATTERN_AMOUNT.matcher(cleaned);
        if (amtMatcher.find()) {
            result.amount = parseAmount(amtMatcher.group(1));
            result.isSuccess = true;
        }

        Matcher senderMatcher = PATTERN_SENDER.matcher(cleaned);
        if (senderMatcher.find()) {
            result.sender = cleanSender(senderMatcher.group(1));
        }

        Matcher dateMatcher = PATTERN_DATE.matcher(cleaned);
        if (dateMatcher.find()) {
            result.dateTime = dateMatcher.group(1).trim();
        }

        return result;
    }

    private static double parseAmount(String rawAmount) {
        if (rawAmount == null) return 0.0;
        try {
            String sanitized = rawAmount.replace(",", "").trim();
            return Double.parseDouble(sanitized);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String cleanSender(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        // Remove trailing phone numbers if joined
        s = s.replaceAll("\\s+[0-9]{7,}$", "");
        return s.trim();
    }
}
