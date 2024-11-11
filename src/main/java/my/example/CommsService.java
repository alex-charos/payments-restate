package my.example;

import dev.restate.sdk.Context;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.annotation.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class CommsService {
    private static final Logger logger =
            LogManager.getLogger(CommsService.class);

    @Handler
    public void notifySuccess(Context ctx, String email)   {
        logger.info("Notifying success");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(AppMain.BASE_URL+"notify-success"))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.info("Notified success");
    }

    @Handler
    public void notifyFailure(Context ctx, String email){
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI( AppMain.BASE_URL+ "notify-failure"))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.info("Notifying failure");
    }

}
