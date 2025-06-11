package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import care.eka.EkaCareClient;
import care.eka.utils.Constants;

/**
 * Sample usage of the EkaCare Java SDK.
 */
public class SampleUsage {
    public static void main(String[] args) {
        try {
            // Initialize the client with client ID and secret
            EkaCareClient client = new EkaCareClient("<client_id", "client-secret");
            
            // Collect all audio files in a list
            List<String> audioFiles = new ArrayList<>();
            audioFiles.add("<full file path>");
            // audioFiles.add("file_path2");

            // Set your own txn id
            String txnId = "test-11jun25-03";

            // Set the Config for running the V2RX action
            // You can refre to : https://developer.eka.care/api-reference/general-tools/medical/voice/overview
            // Mode : dictation | consultation
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("mode", "dictation");
            //Custom keys , you'll get back the exact same in result API
            extraData.put("uhid", "unique_patient_id"); // Replace with actual patient ID
            extraData.put("hfid", "unique_health_facility_id"); // Replace with actual health facility ID

            // Define the output format for the V2RX action
            // Templates and languages can be customized as per your requirements
            Map<String, Object> outputFormat = new HashMap<>();
            outputFormat.put("input_language", Arrays.asList("en-IN", "hi"));

            // output template ids
            Map<String, Object> templateMap1 = new HashMap<>();
            templateMap1.put("template_id", "<your_template_id>");
            templateMap1.put("language_output", "en-IN");
            // set true if you need codified data (if out supports that)
            templateMap1.put("codification_needed", true);
            
            // Sample second template id
            Map<String, Object> templateMap2 = new HashMap<>();
            templateMap2.put("template_id", "clinical_notes_template");
            templateMap2.put("language_output", "en-IN");
            
            // Add both templates to the output_template list
            outputFormat.put("output_template", Arrays.asList(templateMap1, templateMap2));
            
            // Keep this the same
            String action = "ekascribe-v2";

            // Step 1: Authentication
            authenticationExample(client);

            // Step 2: Upload files
            fileUploadExample(client, audioFiles, txnId, action, extraData, outputFormat);

            // Below step should be done after the webhook is received

            // Step 3: Get V2RX status
            getV2RxStatusExample(client, txnId, action);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Authentication example.
     */
    private static void authenticationExample(EkaCareClient client) throws IOException {
        // Extras
        System.out.println("=== Authentication Example ===");

        // Login to get tokens
        JsonNode tokenResponse = client.getAuth().login();

        // Extras
        System.out.println("Access Token: " + tokenResponse.get("access_token").asText());
        System.out.println("Refresh Token: " + tokenResponse.get("refresh_token").asText());

        // Set access token manually
        client.setAccessToken(tokenResponse.get("access_token").asText());

        // Refresh token when needed
        JsonNode refreshedTokens = client.getAuth().refreshToken(tokenResponse.get("refresh_token").asText());
        
        // Extras
        System.out.println("New Access Token: " + refreshedTokens.get("access_token").asText());
    }

    /**
     * File upload example.
     */
    private static void fileUploadExample(EkaCareClient client, List<String> audioFiles, String txnId, String action, Map<String, Object> extraData, Map<String, Object> outputFormat) throws IOException {
        // printed data
        System.out.println("=== File Upload Example ===");
        System.out.println("extraData" +  extraData.toString());
        System.out.println("opformat" +  outputFormat.toString());
        
        // Main Code
        JsonNode response = client.getV2RX().upload(audioFiles, txnId, action, extraData, outputFormat);
        
        // Output printed
        System.out.println("Json Node: " + response.toPrettyString());

        JsonNode outputNode = response.get("output");
        if (outputNode != null && !outputNode.isNull()) {
            for (JsonNode output : outputNode) {
                System.out.println("template id: " + output.get("template_id"));
                System.out.println("template name: " + output.get("name"));
                System.out.println("status: " + output.get("status"));
                System.out.println("errors: " + output.get("errors"));
                System.out.println("warnings: " + output.get("warnings"));
                System.out.println("Value: " + output.get("value"));
            }
        } else {
            System.out.println("No output field found in the response or it's null.");
            System.out.println("Please check the API response structure.");
        }
    }

    /**
     * V2RX status example.
     */
    private static void getV2RxStatusExample(EkaCareClient client, String responseId, String action) throws IOException {
        // Extras
        System.out.println("=== V2RX Fetcher Example ===");

        // Fetch session status
        JsonNode sessionStatus = client.getV2RX().getSessionStatus(responseId, action);
        
        // Extras
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
