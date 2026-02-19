package com.techchallenge.fiap.cargarage.execution_service.configuration;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS SQS configuration for messaging.
 * Supports both AWS environment and local testing with LocalStack.
 */
@Configuration
public class SqsConfiguration {

    @Value("${spring.cloud.aws.region.static:us-east-1}")
    private String awsRegion;

    @Value("${spring.cloud.aws.sqs.endpoint:#{null}}")
    private String sqsEndpoint;

    @Value("${spring.cloud.aws.credentials.access-key:#{null}}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:#{null}}")
    private String secretKey;

    /**
     * SQS Client for production (AWS).
     */
    @Bean
    @Profile("!local")
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * SQS Client for local development with LocalStack.
     */
    @Bean
    @Profile("local")
    public SqsClient sqsClientLocal() {
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .endpointOverride(URI.create(sqsEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    /**
     * Async SQS Client for production (AWS).
     */
    @Bean
    @Profile("!local")
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Async SQS Client for local development with LocalStack.
     */
    @Bean
    @Profile("local")
    public SqsAsyncClient sqsAsyncClientLocal() {
        return SqsAsyncClient.builder()
                .region(Region.of(awsRegion))
                .endpointOverride(URI.create(sqsEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
