package com.cafe.backend.service.marketing;

public interface WhatsAppService {

    String sendMessage(String mobileNumber, String message);
}