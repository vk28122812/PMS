package com.pm.patientservice.grpc;

// First have proto folder and then proto file inside it and then mvn compile
// Then create BillingServiceGrpcClient

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    // Create a variable to hold the grpc client
    // BlockingStub is used for synchronous calls, call the server and wait for the response
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    public BillingServiceGrpcClient(
        @Value("${billing.service.address:localhost}") String serverAddress,
        @Value("${billing.service.grpc.port:9001}") int serverPort) {

        log.info("Connecting to Billing Service GRPC service at {}:{}", serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
                .usePlaintext() // disable TLS for local development and testing
                .build();

        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(String patientId, String patientName, String patientEmail) {
        log.info("Creating billing account for patientId: {}, patientName: {}, patientEmail: {}", patientId, patientName, patientEmail);

        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setName(patientName)
                .setEmail(patientEmail)
                .build();

        BillingResponse response = blockingStub.createBillingAccount(request);

        log.info("Received response from billing service via GRPC : {}", response);
        return response;
    }
}
