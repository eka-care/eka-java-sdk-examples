package org.example;

import care.eka.EkaCareClient;
import care.eka.utils.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sample usage of the EkaCare Java SDK.
 */
public class SampleUsage {
    public static void main(String[] args) {
        try {
            // Initialize the client with client ID and secret
            EkaCareClient client = new EkaCareClient("your_client_id", "your_client_secret");
            List<String> audioFiles = new ArrayList<>();
            audioFiles.add("file_path1");
            audioFiles.add("file_path2");
            String txnId = "transaction_id";
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("mode", "dictation");
            String action = "ekascribe";

            // Example 1: Authentication
             authenticationExample(client);

            // Example 2: Upload files
             fileUploadExample(client, audioFiles, txnId, action, extraData);

             // Below step should be done after the webhook is received

            // Example 3: Get V2RX status
             getV2RxStatusExample(client, txnId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Authentication example.
     */
    private static void authenticationExample(EkaCareClient client) throws IOException {
        System.out.println("=== Authentication Example ===");

        // Login to get tokens
        JsonNode tokenResponse = client.getAuth().login();
        System.out.println("Access Token: " + tokenResponse.get("access_token").asText());
        System.out.println("Refresh Token: " + tokenResponse.get("refresh_token").asText());

        // Set access token manually
        client.setAccessToken(tokenResponse.get("access_token").asText());

        // Refresh token when needed
        JsonNode refreshedTokens = client.getAuth().refreshToken(tokenResponse.get("refresh_token").asText());
        System.out.println("New Access Token: " + refreshedTokens.get("access_token").asText());
    }

    /**
     * File upload example.
     */
    private static void fileUploadExample(EkaCareClient client, List<String> audioFiles, String txnId, String action, Map<String, Object> extraData) throws IOException {
        System.out.println("=== File Upload Example ===");

        // Upload files
        List<JsonNode> responses = client.getFiles().upload(audioFiles, txnId, action, extraData);

        // Print upload results
        for (JsonNode response : responses) {
            System.out.println("Uploaded: " + response.get("key").asText());
            System.out.println("Content Type: " + response.get("contentType").asText());
            System.out.println("File Size: " + response.get("size").asText() + " bytes");
        }
    }

    /**
     * V2RX status example.
     */
    private static void getV2RxStatusExample(EkaCareClient client, String responseId) throws IOException {
        System.out.println("=== V2RX Fetcher Example ===");

        // Fetch session status
        JsonNode sessionStatus = client.getV2RX().getSessionStatus(responseId);
        System.out.println("Session Status: " + sessionStatus.toPrettyString());
    }

    /**
     * Vitals management example.
     */
    private static void vitalsExample(EkaCareClient client) throws IOException {
        System.out.println("=== Vitals Example ===");

        // Create heart rate vital
        JsonNode heartRate = client.getVitals().createHeartRateVital(75, "2023-01-01T12:00:00");

        // Create blood glucose vital
        JsonNode bloodGlucose = client.getVitals().createBloodGlucoseVital(120, "2023-01-01T08:00:00", "fasting");

        // Create blood oxygen vital
        JsonNode bloodOxygen = client.getVitals().createBloodOxygenVital(98, "2023-01-01T12:30:00");

        // Create blood pressure vital
        List<JsonNode> bloodPressure = client.getVitals().createBloodPressureVital(120, 80, "2023-01-01T14:45:00");

        // Combine vitals
        List<JsonNode> allVitals = new ArrayList<>();
        allVitals.add(heartRate);
        allVitals.add(bloodGlucose);
        allVitals.add(bloodOxygen);
        allVitals.addAll(bloodPressure);

        // Update vitals
        JsonNode response = client.getVitals().updateVitals("txn123", allVitals);
        System.out.println("Vitals Updated: " + response);
    }

    /**
     * ABDM profile example.
     */
    private static void profileExample(EkaCareClient client) throws IOException {
        System.out.println("=== ABDM Profile Example ===");

        // Get profile
        JsonNode profile = client.getAbdmProfile().getProfile();
        System.out.println("ABHA Address: " + (profile.has("abha_address") ?
                profile.get("abha_address").asText() : "Not available"));

        // Update profile
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("address", "123 Main St");
        profileData.put("pincode", "110001");
        JsonNode updateResponse = client.getAbdmProfile().updateProfile(profileData);
        System.out.println("Profile Updated: " + updateResponse);

        // Get ABHA card
        byte[] cardImage = client.getAbdmProfile().getAbhaCard();
        Files.write(Paths.get("abha_card.png"), cardImage);
        System.out.println("ABHA Card saved to abha_card.png");

        // Get QR code data
        JsonNode qrData = client.getAbdmProfile().getAbhaQrCode("json");
        System.out.println("QR Code Data: " + qrData);
    }

    /**
     * Health records example.
     */
    private static void recordsExample(EkaCareClient client) throws IOException {
        System.out.println("=== Health Records Example ===");

        // Upload a document
        JsonNode uploadResponse = client.getRecords().uploadDocument(
                "/path/to/lab_report.pdf",
                Constants.DocumentTypes.LAB_REPORT,
                System.currentTimeMillis() / 1000, // Current time in epoch seconds
                List.of("covid", "test"),
                "COVID-19 Test Report"
        );
        System.out.println("Document ID: " + uploadResponse.get("document_id").asText());

        // List documents
        JsonNode documents = client.getRecords().listDocuments(null, null);
        System.out.println("Total Documents: " +
                (documents.has("total") ? documents.get("total").asInt() : "N/A"));

        // If documents exist, get the first one
        if (documents.has("items") && documents.get("items").size() > 0) {
            String documentId = documents.get("items").get(0).get("record").get("item").get("document_id").asText();

            // Get document details
            JsonNode document = client.getRecords().getDocument(documentId);
            System.out.println("Document Type: " + document.get("document_type").asText());

            // Update document
            client.getRecords().updateDocument(
                    documentId,
                    null,
                    null,
                    List.of("updated", "tag"),
                    true,
                    null
            );
            System.out.println("Document updated with new tags");

            // Delete document (commented out to avoid accidental deletion)
            // client.getRecords().deleteDocument(documentId);
            // System.out.println("Document deleted");
        }

        // Retrieve health records in FHIR format
        JsonNode healthRecords = client.getRecords().retrieveHealthRecords(
                "care_context_123",
                "hip123",
                "user@abdm"
        );
        System.out.println("Health Records: " + healthRecords);
    }
}
