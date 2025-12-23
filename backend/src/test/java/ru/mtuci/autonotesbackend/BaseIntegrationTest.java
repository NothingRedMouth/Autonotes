package ru.mtuci.autonotesbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("minio-storage")
@ContextConfiguration(initializers = BaseIntegrationTest.Initializer.class)
public abstract class BaseIntegrationTest {

    protected static final String TEST_JWT_SECRET = "dGVzdC1zZWNyZXQtZm9yLWp3dC10ZXN0aW5nLWxvbmctZW5vdWdo";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
        static MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1");
        static RabbitMQContainer rabbit =
                new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management-alpine"));

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            postgres.start();
            minio.start();
            rabbit.start();

            TestPropertyValues.of(
                            "spring.datasource.url=" + postgres.getJdbcUrl(),
                            "spring.datasource.username=" + postgres.getUsername(),
                            "spring.datasource.password=" + postgres.getPassword(),
                            "spring.flyway.url=" + postgres.getJdbcUrl(),
                            "spring.flyway.user=" + postgres.getUsername(),
                            "spring.flyway.password=" + postgres.getPassword(),
                            "JWT_SECRET=" + TEST_JWT_SECRET,
                            "JWT_EXPIRATION_MS=86400000",
                            "aws.s3.endpoint=" + minio.getS3URL(),
                            "aws.s3.access-key=" + minio.getUserName(),
                            "aws.s3.secret-key=" + minio.getPassword(),
                            "aws.s3.bucket=test-bucket",
                            "aws.s3.region=us-east-1",
                            "spring.rabbitmq.host=" + rabbit.getHost(),
                            "spring.rabbitmq.port=" + rabbit.getAmqpPort(),
                            "spring.rabbitmq.username=" + rabbit.getAdminUsername(),
                            "spring.rabbitmq.password=" + rabbit.getAdminPassword())
                    .applyTo(applicationContext.getEnvironment());
        }
    }
}
