package ru.mtuci.autonotesbackend.config;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Slf4j
@Configuration
@Profile("minio-storage")
public class MinioConfig {

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        S3Configuration s3Configuration =
                S3Configuration.builder().pathStyleAccessEnabled(true).build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .serviceConfiguration(s3Configuration)
                .build();
    }

    @Bean
    public ApplicationRunner minioBucketInitializer(S3Client s3Client) {
        return args -> {
            try {
                s3Client.headBucket(
                        HeadBucketRequest.builder().bucket(bucketName).build());
                log.info("S3 bucket '{}' already exists.", bucketName);
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(
                        CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("S3 bucket '{}' created successfully.", bucketName);
            } catch (Exception e) {
                log.error("Error while initializing S3 bucket", e);
                throw new RuntimeException("Could not initialize S3 bucket", e);
            }
        };
    }
}
