package my.example;

import dev.restate.sdk.JsonSerdes;
import dev.restate.sdk.WorkflowContext;
import dev.restate.sdk.annotation.Workflow;
import my.example.types.DepositRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Workflow
public class PaymentWorkflow {
    private static final Logger logger =
            LogManager.getLogger(PaymentWorkflow.class);
    @Workflow
    public boolean run(WorkflowContext ctx, DepositRequest req) {

        logger.info("deposit");
        var amt = req.getAmount();
        logger.info("amount: " + amt);
        // 0. Create the clients to interact with the other services
        var rgClient = RGServiceClient.fromContext(ctx, req.getUserId());
        var commsClient = CommsServiceClient.fromContext(ctx);
        var walletClient = WalletServiceClient.fromContext(ctx);


        // 1. Check if the limit is passed, and update the limit
        boolean withinLimit = rgClient.updateLimit(amt).await();

        if(!withinLimit){
            commsClient.send().notifyFailure(req.getEmail());
            return false;
        }

        // 2. Charge the customer bank account and deposit the amount
        String paymentId = ctx.random().nextUUID().toString();
        boolean paymentSuccess =
                ctx.run(JsonSerdes.BOOLEAN,
                        () -> chargeCustomer(paymentId, amt, req.getPaymentMethod()));
        walletClient.send().deposit(amt);

        // 3. If the payment failed, reset the limit and send a failure notification
        if(!paymentSuccess){
            rgClient.send().resetLimit(amt);
            commsClient.send().notifyFailure(req.getEmail());
            return false;
        }

        // 4. Send a success notification and reset the limit in 7 days
        commsClient.send().notifySuccess(req.getEmail());
        rgClient.send(Duration.ofDays(7)).resetLimit(amt);
        return true;
    }

    private boolean chargeCustomer(String paymentId, int amt, String paymentMethod){
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(AppMain.BASE_URL+"charge-customer"))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());


        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
