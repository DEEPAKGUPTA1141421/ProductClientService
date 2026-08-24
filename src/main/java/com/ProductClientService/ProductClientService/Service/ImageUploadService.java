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
        return uploadBytes(file.getBytes(), ObjectUtils.emptyMap());
    }

    public String uploadImage(byte[] bytes) throws IOException {
        return uploadBytes(bytes, ObjectUtils.emptyMap());
    }

    /**
     * Uploads into a specific Cloudinary folder — used for sensitive documents
     * (e.g. KYC) that should be kept out of general/public media folders.
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        return uploadBytes(file.getBytes(), ObjectUtils.asMap("folder", folder));
    }

    @SuppressWarnings("unchecked")
    private String uploadBytes(byte[] bytes, Map<String, Object> options) throws IOException {
        Map<String, Object> uploadResult = cloudinary.uploader().upload(bytes, options);
        Object secureUrl = uploadResult.get("secure_url");
        return secureUrl != null ? secureUrl.toString() : uploadResult.get("url").toString();
    }
}
