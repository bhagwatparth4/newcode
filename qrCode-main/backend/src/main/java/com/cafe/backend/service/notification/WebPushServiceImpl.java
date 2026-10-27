package com.cafe.backend.service.notification;

import com.cafe.backend.entity.PushSubscription;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
@Service
public class WebPushServiceImpl implements WebPushService {


    // web-push encryption needs the BouncyCastle provider
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }


    @Value("${web-push.vapid-public-key}")
    private String vapidPublicKey;

    @Value("${web-push.vapid-private-key}")
    private String vapidPrivateKey;

    @Value("${web-push.vapid-subject}")
    private String vapidSubject;

    @Override
    public boolean isConfigured() {
        return vapidPublicKey != null && !vapidPublicKey.isBlank()
                && vapidPrivateKey != null && !vapidPrivateKey.isBlank()
                && vapidSubject != null && !vapidSubject.isBlank();
    }

    @Override
    public String getPublicKey() {
        return isConfigured() ? vapidPublicKey : "";
    }

    @Override
    public void sendNotification(
            PushSubscription pushSubscription,
            String title,
            String message
    ) {

        try {
            Subscription.Keys keys = new Subscription.Keys(
                    pushSubscription.getP256dhKey(),
                    pushSubscription.getAuthKey()
            );

            Subscription subscription = new Subscription(
                    pushSubscription.getEndpoint(),
                    keys
            );

            String payload = """
                    {
                      "title": "%s",
                      "message": "%s"
                    }
                    """.formatted(
                    escapeJson(title),
                    escapeJson(message)
            );

            Notification notification = Notification.builder()
                    .subscription(subscription)
                    .payload(payload)
                    .ttl(300)
                    .build();

            PushService pushService = PushService.builder()
                    .withVapidPublicKey(vapidPublicKey)
                    .withVapidPrivateKey(vapidPrivateKey)
                    .withVapidSubject(vapidSubject)
                    .build();

            var response = pushService.send(notification);
            System.out.println("WEB PUSH RESPONSE → " + response);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to send Web Push notification: " + e,
                    e
            );
        }
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}