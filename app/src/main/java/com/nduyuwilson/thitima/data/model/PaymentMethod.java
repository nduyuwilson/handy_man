package com.nduyuwilson.thitima.data.model;

public class PaymentMethod {
    public enum Type { BANK, PAYBILL, TILL }

    public Type type;
    public String label; // e.g. "KCB Bank", "M-Pesa"
    public String value1; // Account Number or Paybill Number
    public String value2; // Bank Name or Paybill Account Name (optional)

    public PaymentMethod(Type type, String label, String value1, String value2) {
        this.type = type;
        this.label = label;
        this.value1 = value1;
        this.value2 = value2;
    }

    public String getDisplayText() {
        switch (type) {
            case BANK:
                return "Bank: " + value2 + " | Acc: " + value1;
            case PAYBILL:
                return "Paybill: " + value1 + " | Acc: " + value2;
            case TILL:
                return "M-Pesa Till: " + value1 + (label.isEmpty() ? "" : " (" + label + ")");
            default:
                return "";
        }
    }
}
