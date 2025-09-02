package com.ProductClientService.ProductClientService.Service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class S3Service {

    private final AmazonS3 amazonS3;
    private final String bucketName = "your-bucket-name"; // move to config later

    public S3Service(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            String key = "products/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());

            amazonS3.putObject(bucketName, key, file.getInputStream(), metadata);

            String url = amazonS3.getUrl(bucketName, key).toString();
            imageUrls.add(url);
        }

        return imageUrls;
    }
}
