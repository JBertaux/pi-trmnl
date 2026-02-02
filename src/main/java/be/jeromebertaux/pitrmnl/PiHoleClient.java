package be.jeromebertaux.pitrmnl;

import org.json.JSONObject;

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

public class PiHoleClient {
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
        JSONObject payload = new JSONObject();
        payload.put("password", password);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JSONObject authResponse = new JSONObject(response.body());
                JSONObject sessionObj = authResponse.getJSONObject("session");
                this.session = new Session(
                    sessionObj.getString("sid"),
                    sessionObj.getString("csrf")
                );
            } else {
                throw new RuntimeException("Failed to authenticate: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cannot authenticate to PiHole: " + e.getMessage(), e);
        }
    }

    public String getPaddData() {
        System.out.println("⬇ Fetching PADD data from Pi-hole server...");

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
                return response.body();
            } else {
                throw new RuntimeException("Failed to fetch PADD data: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch PADD data: " + e.getMessage());
            throw new RuntimeException("Failed to fetch PADD data", e);
        }
    }

    public JSONObject getHistory() {
        System.out.println("⬇ Fetching History data from Pi-hole server...");

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
                return new JSONObject(response.body());
            } else {
                throw new RuntimeException("Failed to fetch History data: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch History data: " + e.getMessage());
            throw new RuntimeException("Failed to fetch History data", e);
        }
    }

    public record Session(String sid, String csrf) {
    }
}