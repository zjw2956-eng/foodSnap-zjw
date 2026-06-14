package com.hope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accesskey;

    @Value("${minio.secret-key}")
    private String secretkey;

    @Value("${minio.bucketName:food-snap}") // :food-snap是默认值
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        // 1. 用 builder 创建 MinioClient
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accesskey, secretkey)
                .build();
        // 2. 检查 bucket 是否存在
        try {
            boolean found = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build());
            // 3. 不存在就创建
            if (!found) {
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build());
                log.info("[Minio] Bucket [{}] 创建成功", bucketName);
            }
        } catch (Exception e) {
            log.error("[Minio] Bucket 初始化失败", e);
            throw new RuntimeException("Minio bucket 初始化失败", e);
        }
        return client;
    }
}
