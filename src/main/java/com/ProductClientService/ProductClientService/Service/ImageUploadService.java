package com.ProductClientService.ProductClientService.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageUploadService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadBytes(file.getBytes());
    }

    public String uploadImage(byte[] bytes) throws IOException {
        return uploadBytes(bytes);
    }

    @SuppressWarnings("unchecked")
    private String uploadBytes(byte[] bytes) throws IOException {
        Map<String, Object> uploadResult = cloudinary.uploader().upload(bytes, ObjectUtils.emptyMap());
        return uploadResult.get("url").toString();
    }
}
