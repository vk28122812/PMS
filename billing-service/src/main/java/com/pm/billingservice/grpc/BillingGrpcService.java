package com.pm.billingservice.grpc;

import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// First create proto folder and then proto file inside it and then mvn compile
// Then create BillingGrpcService

@Service
public class BillingGrpcService extends BillingServiceGrpc.BillingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    @Override
    public void createBillingAccount(billing.BillingRequest billingRequest, StreamObserver<BillingResponse> responseObserver){

        log.info("createBillingAccount request received {}", billingRequest.toString());

        // Business logic - for example, save to db, preform calculations, etc.

        BillingResponse billingResponse = BillingResponse.newBuilder()
                .setAccountId("12345")
                .setStatus("ACTIVE")
                .build();

        responseObserver.onNext(billingResponse);
        /*
         * We can use onNext multiple times if we want to send multiple responses
         */
        responseObserver.onCompleted();

    }
}
