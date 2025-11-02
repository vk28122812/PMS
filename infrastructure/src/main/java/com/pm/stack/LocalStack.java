package com.pm.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {

    private final Vpc vpc;
    private final Cluster ecsCluster;

    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();

        DatabaseInstance authServiceDb = createDatabase("AuthServiceDB", "auth-service-db");

        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDB", "patient-service-db");

        CfnHealthCheck authServiceDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");

        CfnHealthCheck patientServiceDbHealthCheck = createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");

        CfnCluster kafkaCluster = createMskCluster();

        this.ecsCluster = createEcsCluster();

        FargateService billingService = createFargateService("billing-service", "billing-service",
                List.of(4001, 9001),
                null,
                null
        );

        FargateService analyticsService = createFargateService("analytics-service", "analytics-service",
                List.of(4002),
                null,
                null
        );

        FargateService authService = createFargateService("auth-service", "auth-service",
                List.of(4005),
                authServiceDb,
                Map.of("JWT_SECRET", "bf17dfc038c823d921976218093b982b8922e802df4dd2c6260c00927c9375bb")
        );

        authService.getNode().addDependency(authServiceDb);
        authService.getNode().addDependency(authServiceDbHealthCheck);



        analyticsService.getNode().addDependency(kafkaCluster);

        FargateService patientService = createFargateService("patient-service", "patient-service",
                List.of(4000),
                patientServiceDb,
                Map.of(
                        "BILLING_SERVICE_ADDRESS", "billing-service.patient-management.local",
                        "BILLING_SERVICE_GRPC_PORT", "9001"
                ));

        patientService.getNode().addDependency(patientServiceDbHealthCheck);
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(kafkaCluster);

        createApiGateway();

    }

    // VPC => Top level abstraction of an AWS Virtual Private Cloud, all infrastructure is deployed in VPC
    // Sets up Networking for all our services and resources
    private Vpc createVpc() {

        return Vpc.Builder
                .create(this, "PatientManagementVPC")
                .vpcName("PatientManagementVPC")
                .maxAzs(2)
                .build();

    }

    private DatabaseInstance createDatabase(String id, String dbName) {

        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_17_2)
                                .build()))
                .vpc(this.vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30)
                        .failureThreshold(5)
                        .build())
                .build();
    }


    private CfnCluster createMskCluster() {

        return CfnCluster.Builder.create(this, "MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("3.6.0")
                .numberOfBrokerNodes(2)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.large")
                        .clientSubnets(vpc.getPrivateSubnets().stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList())
                        )
                        .brokerAzDistribution("DEFAULT")
                        .build()
                )
                .build();
    }

    private Cluster createEcsCluster() {
        return Cluster.Builder.create(this, "PatientManagementCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local").build())
                .build();
    }


    // ECS Task => actual thing that runs the container
    // Task Definition => blueprint for the container (memory, image, env, etc.)
    private FargateService createFargateService(String id, String imageName, List<Integer> ports, DatabaseInstance db, Map<String, String> additionalEnvVar) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, id+"Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions.Builder containerOptionsBuilder = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imageName))
                .portMappings(ports.stream()
                        .map(port -> PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                .logGroupName("/ecs/" + imageName)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .streamPrefix(imageName)
                        .build()));


        Map<String, String> envVars = new HashMap<>();

        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510,localhost.localstack.cloud:4511,localhost.localstack.cloud:4512");
        if (additionalEnvVar != null) {
            envVars.putAll(additionalEnvVar);
        }
        if(db != null){
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName
            ));

            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("SPRING_DATASOURCE_PASSWORD",
                    db.getSecret().secretValueFromJson("password").toString());

            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "600000");

        }

        containerOptionsBuilder.environment(envVars);

        taskDefinition.addContainer(id, containerOptionsBuilder.build());

        int servicePort = ports.get(0);

        return FargateService.Builder.create(this, id)
                .cluster(this.ecsCluster)
                .cloudMapOptions(CloudMapOptions.builder()
                        .name(id)
                        .cloudMapNamespace(this.ecsCluster.getDefaultCloudMapNamespace())
                        .dnsRecordType(DnsRecordType.A)
                        .containerPort(servicePort)
                        .build())
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .build();

    }


    public void createApiGateway(){
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this,"ApiGatewayTaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        String imageName = "api-gateway";

        ContainerDefinitionOptions containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imageName))
                .environment(
                        Map.of(
                                "SPRING_PROFILES_ACTIVE", "prod",
                                "AUTH_SERVICE_URL", "http://auth-service.patient-management.local:4005"
                        )
                )
                .portMappings(List.of(4004).stream()
                        .map(port -> PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                .logGroupName("/ecs/" + imageName)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .streamPrefix(imageName)
                        .build()))
                .build();


        taskDefinition.addContainer(imageName, containerOptions);

        ApplicationLoadBalancedFargateService apiGateway = ApplicationLoadBalancedFargateService.Builder.create(this, imageName)
                .cluster(this.ecsCluster)
                .taskDefinition(taskDefinition)
                .serviceName(imageName)
                .desiredCount(1)
                .healthCheckGracePeriod(Duration.seconds(60))
                .build();

    }




    public static void main(String[] args) {

        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        // Additional properties that we want to apply to our stack
        // Synthensizer => used to convert our code(infrastructure) to AWS cloudformation template
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app, "localstack", props);
        app.synth(); // Take our app and synthesize it to a cloudformation template and place it in the cdk.out directory

        System.out.println("App synthesizing in progress");

    }
}
