package be.jeromebertaux.pitrmnl;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TrmnlClient {
    private final String pluginId;
    private final HttpClient httpClient;

    public TrmnlClient(String pluginId) {
        this.pluginId = pluginId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendData(JSONObject data) {
        System.out.println("📤 Sending data to TRMNL plugin...");

        String url = "https://usetrmnl.com/api/custom_plugins/" + pluginId;
        JSONObject payload = new JSONObject();
        payload.put("merge_variables", data.toString());

        System.out.println("Payload to send: " + payload);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                System.out.println("Data sent successfully.");
                System.out.println("Response: " + response.body());
            } else {
                System.out.println("Failed to send data: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            System.out.println("Error sending data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}