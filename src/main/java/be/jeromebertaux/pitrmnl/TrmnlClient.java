package be.jeromebertaux.pitrmnl;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TrmnlClient {
    private static final Logger logger = LoggerFactory.getLogger(TrmnlClient.class);
    
    private final String pluginId;
    private final HttpClient httpClient;

    public TrmnlClient(String pluginId) {
        this.pluginId = pluginId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendData(JSONObject data) {
        logger.info("📤 Sending data to TRMNL plugin...");

        String url = "https://usetrmnl.com/api/custom_plugins/" + pluginId;
        JSONObject payload = new JSONObject();
        payload.put("merge_variables", data.toString());

        logger.debug("Payload to send: {}", payload);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                logger.info("Data sent successfully.");
                logger.debug("Response: {}", response.body());
            } else {
                logger.error("Failed to send data: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error sending data", e);
        }
    }
}