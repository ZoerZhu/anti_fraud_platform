package com.anti.controller;

import com.anti.common.Result;
import com.anti.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileControllerTest {

    @TempDir
    Path uploadRoot;

    private FileController controller;

    @BeforeEach
    void setUp() {
        controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadPath", uploadRoot.toString());
        ReflectionTestUtils.setField(controller, "baseUrl", "http://localhost:8080");
    }

    @Test
    void uploadRequiresAuthenticatedUser() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        Result<String> result = controller.uploadImage(file, null);

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).contains("请先登录");
    }

    @Test
    void uploadRejectsUnsupportedMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.png", "text/plain", new byte[]{1, 2, 3});

        Result<String> result = controller.uploadImage(file, loginUser());

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("仅支持");
    }

    @Test
    void uploadRejectsUnsupportedExtensionEvenWhenMimeIsImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "image/png", new byte[]{1, 2, 3});

        Result<String> result = controller.uploadImage(file, loginUser());

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("扩展名");
    }

    @Test
    void uploadStoresAllowedImageUnderConfiguredDirectory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        Result<String> result = controller.uploadImage(file, loginUser());

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).startsWith("http://localhost:8080/uploads/images/");
        try (var files = Files.walk(uploadRoot.resolve("images"))) {
            assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(1);
        }
    }

    private LoginUser loginUser() {
        return new LoginUser(1L, "student", "student");
    }
}
