# Betting example with Restate

This is a simple example of how to use Restate to model a betting system.

Restate is an open-source Durable Execution Engine. 

## Deployment model 

Your services run like **normal Java services in a JVM / Docker container**. 
They have the Java Restate SDK embedded in them.
Restate does not require you to run "workers" and deploy your code on there, like many other workflow orchestrators do.

Restate itself runs as a separate component in front of your services.
Restate runs as a **single binary**. No databases, queues, etc. **Easy to self-host** on K8s.

## Programming model

With Restate, you write your services in the same way as you do in any microservices architecture.
You don't need to morph your services into workflows, activities etc. 

You just write **services with a set of Durable Functions** that can be called by other services. 
You can turn any function into a Durable Function by adding a simple annotation, and by using the SDK to register the results of actions throughout your code. 

## Performance

Restate is written as an event-driven foundation. 
You can see it as a message broker on steroids that doesn't only pass messages, but also keeps track of the execution progress and state of your services.

Restate invokes your Durable Functions via RPC calls, which is very fast. 
Benchmarks of 3-step workflows can execute in 30 ms. 
These kind of latencies are not possible with other existing workflow engines.

[Blog on performance measurements](https://restate.dev/blog/restate-1.1.0-and-multiple-sdks-released/#performance-improvements)

## Application State

Restate has an embedded K/V store that you can use to store the state of your application.
This state is long-lived and can be called by all the functions of your service.
This state takes part in Durable Execution and is always up to date with the code execution progress.


## Versioning
[Blog post on our approach vs Temporal, Azure Durable Functions etc](https://restate.dev/blog/solving-durable-executions-immutability-problem)

In short, we consider endpoints to be immutable. 
When you deploy a new version, you register the endpoint with Restate and Restate will forward new requests to it. 
Old requests will still be forwarded to the old version.
You need to keep the old version around until all old requests are processed.
The easiest way to do that is to split long-running workflows into multiple steps, with delayed calls in between them.


## Running the example

Restate has a CLI that you can use to interact with your applications.

But here I will just use a Docker container and curl so you don't need to install anything to try it out.

To start Restate:
```
docker run --name restate_dev --rm -p 8080:8080 -p 9070:9070 -p 9071:9071 \
--add-host=host.docker.internal:host-gateway docker.io/restatedev/restate:1.1
```

To run your services, do in this directory:
```
./gradlew run
```

Tell Restate where your services are running so it can forward requests to them:
```
curl localhost:9070/deployments -H 'content-type: application/json' \
-d '{"uri": "http://localhost:9080"}'
```

Now you can make requests to your services through Restate:
```
curl localhost:8080/PaymentService/deposit \
    -H "Content-Type: application/json" \
    -d '{                                            
        "walletId": "exampleWalletId",
        "userId": "exampleUserId",
        "email": "example@example.com",
        "amount": 100,
        "paymentMethod": "creditCard"
    }'
```

Or add an idempotency key, and let Restate automatically deduplicate requests:
```
curl localhost:8080/PaymentService/deposit \
    -H "Content-Type: application/json" \
    -H 'idempotency-key: ad5472esg4dsg525dssdfa5loi' \
    -d '{                                            
        "walletId": "exampleWalletId",
        "userId": "exampleUserId",
        "email": "example@example.com",
        "amount": 100,
        "paymentMethod": "creditCard"
    }'
```
