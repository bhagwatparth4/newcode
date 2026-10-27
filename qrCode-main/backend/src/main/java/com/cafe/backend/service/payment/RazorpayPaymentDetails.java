package com.cafe.backend.service.payment;

import org.json.JSONObject;

/*
 * Small helper to read the bank/UPI reference from a Razorpay payment.
 *
 * For UPI payments Razorpay returns acquirer_data.rrn (the UPI RRN the
 * customer also sees in their UPI app). Cards/netbanking use other keys.
 * If nothing is present we return null (the field is optional).
 */
public final class RazorpayPaymentDetails {

    private static final String[] REFERENCE_KEYS = {
            "rrn",
            "upi_transaction_id",
            "bank_transaction_id",
            "auth_code",
            "arn"
    };

    private RazorpayPaymentDetails() {
    }

    public static String extractTransactionReference(Object acquirerData) {

        if (!(acquirerData instanceof JSONObject data)) {
            return null;
        }

        for (String key : REFERENCE_KEYS) {
            String value = data.optString(key, "");
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value.length() > 150 ? value.substring(0, 150) : value;
            }
        }

        return null;
    }
}
