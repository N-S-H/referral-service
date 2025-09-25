package com.backbase.referral;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.client.MockServerClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractTestContainersSetup {

    static MockServerClient mockServerClient;

    public static  DockerImageName DB_IMAGE = null;

    static{
        String osName = System.getProperty("os.name");
        if(null!= osName && osName.toLowerCase().trim().startsWith("mac")){
            DB_IMAGE = DockerImageName.parse("arm64v8/mysql:8.0.33").asCompatibleSubstituteFor("mysql");
        }
        else{
            DB_IMAGE =  DockerImageName.parse("mysql:8.0.31").asCompatibleSubstituteFor("mysql");
        }
    }

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse("mockserver/mockserver").withTag("mockserver-5.14.0");

    @Container
    public static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);

    @Container
    public static MySQLContainer dbContainer = new MySQLContainer(DB_IMAGE)
            .withDatabaseName("referral-service")
            .withUsername("root")
            .withPassword("root");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url" ,() -> dbContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> dbContainer.getUsername());
        registry.add("spring.datasource.password", ()-> dbContainer.getPassword());
        registry.add("spring.datasource.driver-class-name" ,() -> dbContainer.getDriverClassName());

        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.client.simple.instances.token-converter[0].uri",
                () -> "http://" + mockServer.getHost() + ":" + mockServer.getServerPort());
    }

    @BeforeAll
    public static void startUp() {
        System.setProperty("SIG_SECRET_KEY","JWTSecretKeyDontUseInProduction!");
        dbContainer.start();
        mockServer.start();
        mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
    }

    @AfterAll
    public static void cleanUp() {
        dbContainer.stop();
        mockServer.stop();
    }

}