package com.pangasmart.pangasmart.services;

import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SmsService {

    // 1. Weka TRUE ukishapata API Keys na Salio Beem Africa ili zifike kwenye simu
    private static final boolean USE_REAL_SMS = false;

    // 2. Weka API Keys zako kutoka https://beem.africa hapa baadaye
    private static final String API_KEY = "YOUR_BEEM_API_KEY";
    private static final String SECRET_KEY = "YOUR_BEEM_SECRET_KEY";

    public void sendSms(String toPhoneNumber, String messageBody) {
        if (toPhoneNumber == null || toPhoneNumber.trim().isEmpty()) {
            return;
        }

        // Format namba 07... au 06... kuwa 2557... au 2556...
        if (toPhoneNumber.startsWith("0")) {
            toPhoneNumber = "255" + toPhoneNumber.substring(1);
        } else if (toPhoneNumber.startsWith("+")) {
            toPhoneNumber = toPhoneNumber.substring(1);
        }

        if (USE_REAL_SMS) {
            sendRealSmsViaBeem(toPhoneNumber, messageBody);
        } else {
            printConsoleSimulation(toPhoneNumber, messageBody);
        }
    }

    private void sendRealSmsViaBeem(String toPhoneNumber, String messageBody) {
        try {
            URL url = new URL("https://api.beem.africa/v1/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // Basic Auth Header kwa ajili ya Beem Africa
            String auth = API_KEY + ":" + SECRET_KEY;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setDoOutput(true);

            // Structure ya JSON inayotakiwa na Beem Africa API
            String jsonInputString = "{"
                    + "\"source_addr\": \"INFO\","
                    + "\"schedule_time\": \"\","
                    + "\"message\": \"" + messageBody.replace("\"", "\\\"") + "\","
                    + "\"recipients\": [{\"recipient_id\": 1, \"dest_addr\": \"" + toPhoneNumber + "\"}]"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Beem SMS Server Response: " + responseCode);

        } catch (Exception e) {
            System.err.println("Kosa wakati wa kutuma SMS halisi: " + e.getMessage());
        }
    }

    private void printConsoleSimulation(String toPhoneNumber, String messageBody) {
        System.out.println("==========================================");
        System.out.println("📱 [PANGA-SMART SMS SIMULATION]");
        System.out.println("Kwenda: +" + toPhoneNumber);
        System.out.println("Ujumbe: " + messageBody);
        System.out.println("==========================================");
    }
}