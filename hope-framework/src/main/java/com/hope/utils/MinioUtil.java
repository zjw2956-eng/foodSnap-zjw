package com.hope.utils;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hope.config.MinioConfig;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MinioUtil {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 上传文件到 Minio
     * @param inputStream 文件流
     * @param objectName  对象名（Controller 负责生成，如 UUID.扩展名）
     * @param contentType MIME 类型
     * @param fileSize    文件大小（字节）
     * @return 文件访问 URL
     */
    public String upload(InputStream inputStream, String objectName, String contentType, long fileSize) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .stream(inputStream, fileSize, -1)
                .contentType(contentType)
                .build());
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + objectName;
    }
}
