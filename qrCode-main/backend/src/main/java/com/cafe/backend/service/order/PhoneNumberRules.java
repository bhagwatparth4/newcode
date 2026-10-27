package com.cafe.backend.service.order;

// Free "soft" checks that catch obviously fake mobile numbers (no SMS, no API)
public final class PhoneNumberRules {

    private PhoneNumberRules() {}

    private static final String ASC = "0123456789012345";
    private static final String DESC = "9876543210987654";

    public static boolean looksFake(String mobile) {
        if (mobile == null || !mobile.matches("[6-9]\\d{9}")) {
            return true;                                   // not a valid Indian mobile
        }
        if (mobile.chars().distinct().count() <= 2) {
            return true;                                   // 9999999999, 9898989898, 9999900000
        }
        if (ASC.contains(mobile) || DESC.contains(mobile)) {
            return true;                                   // 6789012345, 9876543210
        }
        if (mobile.matches(".*(\\d)\\1{6,}.*")) {
            return true;                                   // 7+ same digits in a row: 9000000012
        }
        return false;
    }
}