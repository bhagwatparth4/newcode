package com.cafe.backend.service.marketing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class SmsServiceImpl implements SmsService {

    private final RestClient restClient;

    @Value("${twofactor.api-key}")
    private String apiKey;

    @Value("${twofactor.sms-header:}")
    private String smsHeader;

    public SmsServiceImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://2factor.in")
                .build();
    }

    @Override
    public boolean sendSms(String mobileNumber, String message) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "2Factor API key is not configured"
            );
        }

        if (smsHeader == null || smsHeader.isBlank()) {
            throw new IllegalStateException(
                    "2Factor SMS header is not configured. " +
                            "Client DLT-approved header is required."
            );
        }

        String phoneNumber = normalizeIndianNumber(mobileNumber);

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add("module", "PROMO_SMS");
        formData.add("apikey", apiKey);
        formData.add("to", phoneNumber);
        formData.add("from", smsHeader);
        formData.add("msg", message);

        Map<String, Object> response = restClient.post()
                .uri("/API/R1/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            return false;
        }

        Object status = response.get("Status");

        return status != null
                && "Success".equalsIgnoreCase(status.toString());
    }

    private String normalizeIndianNumber(String mobileNumber) {

        String number = mobileNumber.trim()
                .replace(" ", "")
                .replace("-", "");

        if (number.startsWith("+91")) {
            return number.substring(1);
        }

        if (number.startsWith("0")) {
            return "91" + number.substring(1);
        }

        if (number.startsWith("91")) {
            return number;
        }

        return "91" + number;
    }
}