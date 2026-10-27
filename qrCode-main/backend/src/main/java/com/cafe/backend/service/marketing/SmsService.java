package com.cafe.backend.service.marketing;

public interface SmsService {

    boolean sendSms(String mobileNumber, String message);
}