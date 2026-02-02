package be.jeromebertaux.pitrmnl.client;

import be.jeromebertaux.pitrmnl.type.HistoryData;
import be.jeromebertaux.pitrmnl.type.PaddData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.security.cert.X509Certificate;

import static be.jeromebertaux.pitrmnl.util.TypeConverter.OBJECT_MAPPER;

public class PiHoleClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PiHoleClient.class);

    private final String endpoint;
    private final String password;
    private final HttpClient httpClient;
    private Session session;

    public PiHoleClient(String endpoint, String password) {
        this.endpoint = endpoint;
        this.password = password;
        this.httpClient = createInsecureHttpClient();
        authenticate();
    }

    private HttpClient createInsecureHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509ExtendedTrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {

                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {

                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // Install the all-trusting trust manager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HTTP client", e);
        }
    }

    public void authenticate() {
        String url = "https://" + endpoint + "/api/auth";
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("password", password);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode authResponse = OBJECT_MAPPER.readTree(response.body());
                JsonNode sessionObj = authResponse.get("session");
                this.session = new Session(
                    sessionObj.get("sid").asString(),
                    sessionObj.get("csrf").asString()
                );
            } else {
                throw new RuntimeException("Failed to authenticate: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cannot authenticate to PiHole: " + e.getMessage(), e);
        }
    }

    public PaddData getPaddData() {
        LOGGER.info("⬇ Fetching PADD data from Pi-hole server...");

        if (session == null) {
            throw new RuntimeException("You must authenticate first.");
        }

        String url = "https://" + endpoint + "/api/padd?full=true";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-FTL-SID", session.sid())
                    .header("X-FTL-CSRF", session.csrf())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return PaddData.fromJson(response.body());
            } else {
                throw new RuntimeException("Failed to fetch PADD data: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to fetch PADD data", e);
            throw new RuntimeException("Failed to fetch PADD data", e);
        }
    }

    public HistoryData getHistory() {
        LOGGER.info("⬇ Fetching History data from Pi-hole server...");

        if (session == null) {
            throw new RuntimeException("You must authenticate first.");
        }

        String url = "https://" + endpoint + "/api/history";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-FTL-SID", session.sid())
                    .header("X-FTL-CSRF", session.csrf())
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return HistoryData.fromJson(response.body());
            } else {
                throw new RuntimeException("Failed to fetch History data: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to fetch History data", e);
            throw new RuntimeException("Failed to fetch History data", e);
        }
    }

    public record Session(String sid, String csrf) {
    }
}