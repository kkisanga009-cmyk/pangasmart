package com.pangasmart.pangasmart.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class PesapalService {

    @Value("${pesapal.consumer.key:YOUR_KEY}")
    private String consumerKey;

    @Value("${pesapal.consumer.secret:YOUR_SECRET}")
    private String consumerSecret;

    @Value("${pesapal.notification.id:PENDING}")
    private String notificationId;

    // Live URL ya Pesapal v3
    private final String BASE_URL = "https://pay.pesapal.com/v3/api";

    public String getAuthToken() {
        RestTemplate restTemplate = new RestTemplate();
        String url = BASE_URL + "/Auth/RequestToken";

        Map<String, String> request = new HashMap<>();
        request.put("consumer_key", consumerKey);
        request.put("consumer_secret", consumerSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getBody() != null && response.getBody().get("token") != null) {
            return (String) response.getBody().get("token");
        }
        return null;
    }

    // Method ya kusajili IPN URL kiotomatiki kama notification_id haijatengenezwa
    public String registerIpnUrl(String token) {
        RestTemplate restTemplate = new RestTemplate();
        String url = BASE_URL + "/URLSetup/RegisterIPN";

        Map<String, Object> request = new HashMap<>();
        request.put("url", "https://pangasmart-production.up.railway.app/api/payments/callback");
        request.put("ipn_notification_type", "POST");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getBody() != null && response.getBody().get("ipn_id") != null) {
            return (String) response.getBody().get("ipn_id");
        }
        return null;
    }

    public String submitOrder(String token, String merchantRef, Double amount, String email, String phone) {
        RestTemplate restTemplate = new RestTemplate();
        String url = BASE_URL + "/Transactions/SubmitOrderRequest";

        // Kama notificationId haina thamani halisi, mfumo unasajili IPN hapo hapo kupitia API
        String activeNotificationId = notificationId;
        if (activeNotificationId == null || activeNotificationId.equals("YOUR_NOTIFICATION_ID") || activeNotificationId.equals("PENDING")) {
            activeNotificationId = registerIpnUrl(token);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("id", merchantRef);
        request.put("currency", "TZS");
        request.put("amount", amount);
        request.put("description", "Malipo ya Kuona Mawasiliano - PangaSmart");
        request.put("callback_url", "https://pangasmart-production.up.railway.app/api/payments/callback");
        request.put("notification_id", activeNotificationId);

        Map<String, String> billingAddress = new HashMap<>();
        billingAddress.put("email_address", (email != null && !email.isEmpty()) ? email : "customer@pangasmart.com");
        billingAddress.put("phone_number", (phone != null && !phone.isEmpty()) ? phone : "0700000000");
        request.put("billing_address", billingAddress);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getBody() != null && response.getBody().get("redirect_url") != null) {
            return (String) response.getBody().get("redirect_url");
        }
        return null;
    }
}