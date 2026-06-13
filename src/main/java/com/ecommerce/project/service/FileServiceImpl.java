package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();

        log.info("Image upload requested. originalFileName={}", originalFileName);

        if (originalFileName == null || !originalFileName.contains(".")) {
            log.warn("Image upload failed. Invalid filename={}", originalFileName);
            throw new APIException("Invalid file name");
        }

        String randomId = UUID.randomUUID().toString();
        String fileName = randomId.
                concat(originalFileName.substring(originalFileName.lastIndexOf('.')));
        String filePath = path + File.separator + fileName;

        File folder = new File(path);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                log.error("Failed to create upload directory. path={}", path);
                throw new APIException("Could not create upload directory");
            }
            log.info("Upload directory created. path={}", path);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, Paths.get(filePath));
        } catch (IOException ex) {
            log.error("Image upload failed. originalFileName={}, targetPath={}", originalFileName, filePath, ex);
            throw ex;
        }

        log.info("Image uploaded successfully. originalFileName={}, storedFileName={}", originalFileName, fileName);
        return fileName;
    }
}
