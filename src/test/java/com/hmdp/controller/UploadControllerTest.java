package com.hmdp.controller;

import com.hmdp.config.HmdpProperties;
import com.hmdp.dto.Result;
import com.hmdp.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadControllerTest {

    @TempDir
    Path uploadDir;

    @Test
    void uploadImageRejectsNonImageFile() {
        UploadController controller = createController();
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));

        assertThrows(BizException.class, () -> controller.uploadImage(textFile));
    }

    @Test
    void uploadImageStoresAllowedImageInsideUploadDir() {
        UploadController controller = createController();
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        Result result = controller.uploadImage(imageFile);

        String storedName = (String) result.getData();
        assertTrue(storedName.matches("/blogs/([0-9]|1[0-5])/([0-9]|1[0-5])/[0-9a-f\\-]{36}\\.png"));
        assertTrue(Files.exists(uploadDir.resolve(storedName.substring(1))));
    }

    @Test
    void deleteBlogImgRejectsPathTraversal() throws Exception {
        Path outsideFile = uploadDir.getParent().resolve("outside.txt");
        Files.write(outsideFile, "keep".getBytes(StandardCharsets.UTF_8));

        UploadController controller = createController();

        assertThrows(BizException.class, () -> controller.deleteBlogImg("../outside.txt"));
        assertTrue(Files.exists(outsideFile));
    }

    @Test
    void deleteBlogImgDeletesOnlyUploadFile() throws Exception {
        Path target = uploadDir.resolve("blogs/1/2/a.png");
        Files.createDirectories(target.getParent());
        Files.write(target, new byte[]{1, 2, 3});

        UploadController controller = createController();

        controller.deleteBlogImg("/blogs/1/2/a.png");
        assertFalse(Files.exists(target));
    }

    private UploadController createController() {
        HmdpProperties properties = new HmdpProperties();
        properties.setUploadDir(uploadDir.toString());
        return new UploadController(properties);
    }
}
