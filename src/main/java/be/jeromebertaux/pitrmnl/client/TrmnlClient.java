package be.jeromebertaux.pitrmnl.client;

import be.jeromebertaux.pitrmnl.type.ScreenVariables;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static be.jeromebertaux.pitrmnl.util.TypeConverter.OBJECT_MAPPER;

public class TrmnlClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrmnlClient.class);

    private final String pluginId;
    private final HttpClient httpClient;

    public TrmnlClient(String pluginId) {
        this.pluginId = pluginId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendData(final ScreenVariables data) {
        LOGGER.info("📤 Sending data to TRMNL plugin...");

        String url = "https://usetrmnl.com/api/custom_plugins/" + pluginId;

        final TrmnlInput input = new TrmnlInput(data);

        LOGGER.debug("Payload to send: {}", input);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(input.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LOGGER.info("Data sent successfully.");
                LOGGER.debug("Response: {}", response.body());
            } else {
                LOGGER.error("Failed to send data: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            LOGGER.error("Error sending data", e);
        }
    }

    private record TrmnlInput(@JsonProperty("merge_variables") ScreenVariables variables) {
        @Override
        public String toString() {
            return OBJECT_MAPPER.writeValueAsString(this);
        }
    }
}