package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileServiceImplTest {

    private final FileServiceImpl fileService = new FileServiceImpl();

    @TempDir
    Path tempDir;

    /// uploadImage()
    @Test
    void uploadImageShouldUploadImageWhenDirectoryAlreadyExists() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "dummy image data".getBytes()
        );

        String result = fileService.uploadImage(tempDir.toString(), file);

        assertNotNull(result);
        assertTrue(result.endsWith(".png"));

        Path uploadedFile = tempDir.resolve(result);
        assertTrue(Files.exists(uploadedFile));

        assertArrayEquals(file.getBytes(), Files.readAllBytes(uploadedFile));
        assertNotEquals("harry-potter.png", result);
    }

    @Test
    void uploadImageShouldCreateDirectoryAndUploadImageWhenDirectoryDoesNotExist() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "harry-potter.png",
                "image/png",
                "dummy image data".getBytes()
        );

        Path uploadDir = tempDir.resolve("upload");

        String result = fileService.uploadImage(uploadDir.toString(), file);

        assertNotNull(result);
        assertTrue(result.endsWith(".png"));

        Path uploadedFile = uploadDir.resolve(result);
        assertTrue(Files.exists(uploadedFile));
        assertTrue(Files.isDirectory(uploadDir));

        assertArrayEquals(file.getBytes(), Files.readAllBytes(uploadedFile));
        assertNotEquals("harry-potter.png", result);
    }

    @Test
    void uploadImageShouldThrowApiExceptionWhenOriginalFilenameIsNull() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.getOriginalFilename())
                .thenReturn(null);

        APIException exception = assertThrows(
                APIException.class,
                () -> fileService.uploadImage(tempDir.toString(), file)
        );

        assertEquals("Invalid file name", exception.getMessage());
    }

    @Test
    void uploadImageShouldThrowApiExceptionWhenFilenameHasNoExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "harry-potter",
                "image/png",
                "dummy image data".getBytes()
        );

        APIException exception = assertThrows(
                APIException.class,
                () -> fileService.uploadImage(tempDir.toString(), file)
        );

        assertEquals("Invalid file name", exception.getMessage());
    }

    @Test
    void uploadImageShouldPropagateIOExceptionWhenReadingFileFails() throws IOException {
        MultipartFile file = mock(MultipartFile.class);

        when(file.getOriginalFilename())
                .thenReturn("harry-potter.png");

        when(file.getInputStream())
                .thenThrow(new IOException("Read failure"));

        IOException exception = assertThrows(
                IOException.class,
                () -> fileService.uploadImage(tempDir.toString(), file)
        );

        assertEquals("Read failure", exception.getMessage());
    }
}