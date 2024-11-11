/*
 * Copyright (c) 2024 - Restate Software, Inc., Restate GmbH
 *
 * This file is part of the Restate examples,
 * which is released under the MIT license.
 *
 * You can find a copy of the license in the file LICENSE
 * in the root directory of this repository or package or at
 * https://github.com/restatedev/examples/
 */

package my.example;

import dev.restate.sdk.Context;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.annotation.Service;
import dev.restate.sdk.http.vertx.RestateHttpEndpointBuilder;
import my.example.ssbt.SSBTService;
import my.example.ssbt.SSBTWorkflow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppMain {

    public static final String BASE_URL = "https://opap-restate-demo.free.beeceptor.com/";
    private static final Logger logger =
            LogManager.getLogger(AppMain.class);

    public static void main(String[] args) {

       int port = 9080;
        if (args.length > 0) {
            logger.info("Using args:" + args[0]);
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                //ignored
                logger.warn("Did not pass readable port {}, reverting to default {}", args[0], port);
            }
        }



        RestateHttpEndpointBuilder.builder()
                .bind(new PaymentService())
                .bind(new RGService())
                .bind(new WalletService())
                .bind(new CommsService())
                .bind(new PaymentWorkflow())
                .bind(new SSBTWorkflow())
                .bind(new SSBTService())
                .buildAndListen(port);


    }
}
