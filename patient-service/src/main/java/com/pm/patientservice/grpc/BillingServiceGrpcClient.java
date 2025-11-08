package com.pm.patientservice.grpc;

// First have proto folder and then proto file inside it and then mvn compile
// Then create BillingServiceGrpcClient

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.pm.patientservice.kafka.KafkaProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
    private final KafkaProducer kafkaProducer;

    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort, KafkaProducer kafkaProducer) {

        log.info("Connecting to Billing Service GRPC service at {}:{}", serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
                .usePlaintext() // disable TLS for local development and testing
                .build();

        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
        this.kafkaProducer = kafkaProducer;
    }


    // name is used to track , fallback is used to specify the fallback method
    @CircuitBreaker(name = "billingService", fallbackMethod = "billingFallback")
    @Retry(name = "billingRetry") // Attempt the request 3 times before failing(3 times as default)
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

    // Needs to have same parameters as createBillingAccount plus Throwable
    // Fallback method needs to have return type as the original method,
    // ensuring the calling code receives a valid object without knowing a circuit breaker was triggered
    public BillingResponse billingFallback(String patientId, String patientName, String patientEmail, Throwable t){
        log.warn("[CIRCUIT BREAKER] : Billing service is unavailable. Triggered fallback : {}", t.getMessage());

        kafkaProducer.sendBillingAccountEvent(patientId, patientName, patientEmail);

        return BillingResponse.newBuilder()
                .setAccountId("")
                .setStatus("PENDING")
                .build();
    }
}
