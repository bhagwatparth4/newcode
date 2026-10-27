package com.cafe.backend.service.marketing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private final RestClient restClient;

    @Value("${whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.api-version:}")
    private String apiVersion;

    @Value("${whatsapp.template-name:}")
    private String templateName;

    @Value("${whatsapp.template-language:en_US}")
    private String templateLanguage;

    public WhatsAppServiceImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://graph.facebook.com")
                .build();
    }

    @Override
    public String sendMessage(String mobileNumber, String message) {

        if (!enabled) {
            throw new IllegalStateException(
                    "WhatsApp provider is not enabled"
            );
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "WhatsApp access token is not configured"
            );
        }

        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new IllegalStateException(
                    "WhatsApp phone number ID is not configured"
            );
        }

        if (apiVersion == null || apiVersion.isBlank()) {
            throw new IllegalStateException(
                    "WhatsApp API version is not configured"
            );
        }

        if (templateName == null || templateName.isBlank()) {
            throw new IllegalStateException(
                    "WhatsApp template name is not configured"
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "WhatsApp message cannot be empty"
            );
        }

        String phoneNumber = normalizeIndianNumber(mobileNumber);

        Map<String, Object> bodyParameter = Map.of(
                "type", "text",
                "text", message
        );

        Map<String, Object> bodyComponent = Map.of(
                "type", "body",
                "parameters", List.of(bodyParameter)
        );

        Map<String, Object> template = Map.of(
                "name", templateName,
                "language", Map.of("code", templateLanguage),
                "components", List.of(bodyComponent)
        );

        Map<String, Object> requestBody = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", phoneNumber,
                "type", "template",
                "template", template
        );

        try {

            Map<String, Object> response = restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages",
                            apiVersion,
                            phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return null;
            }

            Object messages = response.get("messages");

            if (!(messages instanceof List<?> list) || list.isEmpty()) {
                return null;
            }

            Object firstMessage = list.get(0);

            if (!(firstMessage instanceof Map<?, ?> messageData)) {
                return null;
            }

            Object messageId = messageData.get("id");

            return messageId != null ? messageId.toString() : null;

        } catch (RestClientResponseException e) {

            throw new IllegalStateException(
                    "WhatsApp provider failed: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private String normalizeIndianNumber(String mobileNumber) {

        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Mobile number cannot be empty"
            );
        }

        String number = mobileNumber
                .trim()
                .replaceAll("[\\s-]", "");

        if (number.startsWith("+91")) {
            number = number.substring(3);
        } else if (number.startsWith("91") && number.length() == 12) {
            number = number.substring(2);
        } else if (number.startsWith("0")) {
            number = number.substring(1);
        }

        if (number.length() != 10) {
            throw new IllegalArgumentException(
                    "Invalid Indian mobile number"
            );
        }

        return "91" + number;
    }
}