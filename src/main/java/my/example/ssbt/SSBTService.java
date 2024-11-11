package my.example.ssbt;

import dev.restate.sdk.Context;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.annotation.Service;
import my.example.types.DepositRequest;
import my.example.types.WorkflowRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class SSBTService {
    private static final Logger logger =
            LogManager.getLogger(SSBTService.class);

    @Handler
    public String deposit(Context ctx, DepositRequest dr) {
        String workflowId = ctx.random().nextUUID().toString();
        logger.info("Initiating workflowId {}", workflowId);
        SSBTWorkflowClient.fromContext(ctx, workflowId).run(dr);
        logger.info("Initiated workflowId {}", workflowId);
        return workflowId;
    }

    @Handler
    public String query(Context ctx, WorkflowRequest workflowId) {
        logger.info("querying workflowId {}", workflowId);
        return SSBTWorkflowClient.fromContext(ctx, workflowId.workflowId()).status().await();
    }
    @Handler
    public void providerSuccess(Context ctx, WorkflowRequest workflowId) {
        logger.info("success workflowId {}", workflowId);
        SSBTWorkflowClient.fromContext(ctx, workflowId.workflowId()).providerSuccess("YEAH").await();
    }

    @Handler
    public void providerFailure(Context ctx, WorkflowRequest workflowId) {
        logger.info("failure provider workflowId {}", workflowId);
        SSBTWorkflowClient.fromContext(ctx, workflowId.workflowId()).providerFailure("OH NOOOO").await();
    }

    @Handler
    public void cacnel(Context ctx, WorkflowRequest workflowId) {
        logger.info("cancel workflowId {}", workflowId);
        SSBTWorkflowClient.fromContext(ctx, workflowId.workflowId()).cancel().await();
    }



}
