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

    private final String BASE_URL = "https://cyb3rm0nk.pesapal.com/pesapalv3/api"; // Sandbox URL

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

        return (String) response.getBody().get("token");
    }

    public String submitOrder(String token, String merchantRef, Double amount, String email, String phone) {
        RestTemplate restTemplate = new RestTemplate();
        String url = BASE_URL + "/Transactions/SubmitOrderRequest";

        Map<String, Object> request = new HashMap<>();
        request.put("id", merchantRef);
        request.put("currency", "TZS");
        request.put("amount", amount);
        request.put("description", "Malipo ya Kuona Mawasiliano - PangaSmart");
        request.put("callback_url", "http://localhost:8080/api/payments/callback");
        request.put("notification_id", "YOUR_NOTIFICATION_ID");

        Map<String, String> billingAddress = new HashMap<>();
        billingAddress.put("email_address", email);
        billingAddress.put("phone_number", phone);
        request.put("billing_address", billingAddress);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        return (String) response.getBody().get("redirect_url");
    }
}