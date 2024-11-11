package my.example;

import dev.restate.sdk.Context;
import dev.restate.sdk.JsonSerdes;
import dev.restate.sdk.ObjectContext;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.annotation.Service;
import dev.restate.sdk.annotation.Shared;
import dev.restate.sdk.annotation.VirtualObject;
import dev.restate.sdk.common.StateKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class WalletService {
    private static final Logger logger =
            LogManager.getLogger(WalletService.class);
    StateKey<Long> BALANCE = StateKey.of("balance", JsonSerdes.LONG);

    @Handler
    public void deposit(Context ctx, int amt){
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI( AppMain.BASE_URL+ "deposit"))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.info("Notifying failure");
    }

    @Handler
    public long getBalance(Context ctx){
        return 0L;
    }
}
