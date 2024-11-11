package my.example.ssbt;


import dev.restate.sdk.JsonSerdes;
import dev.restate.sdk.SharedWorkflowContext;
import dev.restate.sdk.WorkflowContext;
import dev.restate.sdk.annotation.Shared;
import dev.restate.sdk.annotation.Workflow;
import dev.restate.sdk.common.DurablePromiseKey;
import dev.restate.sdk.common.StateKey;
import my.example.AppMain;
import my.example.PaymentWorkflow;
import my.example.types.DepositRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.Deque;

@Workflow
public class SSBTWorkflow {

    private static final Logger logger =
            LogManager.getLogger(SSBTWorkflow.class);

    private static final DurablePromiseKey<String> PROVIDER_RESPONSE =
            DurablePromiseKey.of("provider_completed", JsonSerdes.STRING);
    private static final StateKey<String> STATUS =
            StateKey.of("status", JsonSerdes.STRING);

    private static final StateKey<String> SSBT_STATE =
            StateKey.of("ssbt_state", JsonSerdes.STRING);

    private static final DurablePromiseKey<String> SSBT_CANCELLED =
            DurablePromiseKey.of("ssbt_canceled", JsonSerdes.STRING);

    @Workflow
    public boolean run(WorkflowContext ctx, DepositRequest req) {
        logger.info("Workflow {}", ctx.key());
        ctx.set(STATUS, "INITIATED");
        ctx.set(SSBT_STATE, "REQUESTED");
        logger.info("Workflow {} PEEK AT SSBT_CANCEL : {}", ctx.key(), ctx.promise(SSBT_CANCELLED).peek().isReady());
        String providerResponse = ctx.promise(PROVIDER_RESPONSE).awaitable().await();
        Deque<Runnable> compensations = new ArrayDeque<>();
        ctx.set(STATUS, "RESPONSE_PROVIDED");

        if ("success".equals(providerResponse) ) {
            if ( ctx.promise(SSBT_CANCELLED).peek().isReady()) {
                ctx.promise(SSBT_CANCELLED).awaitable().await();
                ctx.set(STATUS, "REJECTING_AT_PROVIDER");
                ctx.run("rejecting-at-provider", ()->this.rejectAtProvider(req));
                ctx.set(STATUS, "REJECTED");
            } else {
                ctx.set(STATUS, "PAYMENT_SUCCESSFUL");
                ctx.run("send-to-pam", ()-> this.sendToPAM(req));
                ctx.set(STATUS, "PAM_SENT");
                if ( ctx.promise(SSBT_CANCELLED).peek().isReady()) {
                    ctx.promise(SSBT_CANCELLED).awaitable().await();
                    ctx.set(STATUS, "UNDOING_AT_PAM");
                    ctx.run("undto-to-pam", ()-> this.undoToPAM(req));
                    ctx.set(STATUS, "REJECTING_AT_PROVIDER");
                    ctx.run("rejecting-at-provider", ()->this.rejectAtProvider(req));
                    ctx.set(STATUS, "REJECTED");
                }
            }

        } else {
            ctx.set(STATUS, "PAYMENT_REJECTED_BY_PROVIDER");
        }

        logger.info("Workflow {} PEEK AT SSBT_CANCEL : {}", ctx.key(), ctx.promise(SSBT_CANCELLED).peek().isReady());

        return false;
    }

    @Shared
    public void cancel(SharedWorkflowContext ctx) {
        logger.info("CANCEL requested for workflowId {}", ctx.key());
        ctx.promiseHandle( SSBT_CANCELLED).resolve("cancelled");

    }

    @Shared
    public String status(SharedWorkflowContext ctx) {
        String workflowId = ctx.key();
        logger.info("returning status workflowId {}", workflowId);
        return ctx.get(STATUS).get();
    }


    @Shared
    public void providerSuccess(SharedWorkflowContext ctx, String providerId) {
        logger.info("providerSuccess for workflowId {}", ctx.key());
        logger.info("providerId {}", providerId);
        ctx.promiseHandle(PROVIDER_RESPONSE).resolve("success");
    }

    @Shared
    public void providerFailure(SharedWorkflowContext ctx, String providerId) {
        logger.info("providerFailure for workflowId {}", ctx.key());
        logger.info("providerId {}", providerId);
        ctx.promiseHandle(PROVIDER_RESPONSE).resolve("failure");
    }




    private void sendToPAM(DepositRequest req) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI( AppMain.BASE_URL+ "send-to-pam"))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void undoToPAM(DepositRequest req) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI( AppMain.BASE_URL+ "undo-to-pam"))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void rejectAtProvider(DepositRequest req) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI( AppMain.BASE_URL+ "reject-at-provider"))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
