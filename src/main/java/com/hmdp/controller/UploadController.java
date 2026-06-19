package com.hmdp.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.config.HmdpProperties;
import com.hmdp.dto.Result;
import com.hmdp.enums.ErrorCode;
import com.hmdp.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("upload")
@RequiredArgsConstructor
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_SUFFIXES = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList("image/jpeg", "image/png", "image/webp"));

    private final HmdpProperties hmdpProperties;

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            validateImage(image);
            // 获取原始文件名称
            String originalFilename = image.getOriginalFilename();
            // 生成新文件名
            String fileName = createNewFileName(originalFilename);
            ensureUploadDirExists();
            // 保存文件
            image.transferTo(resolveUploadFile(fileName));
            // 返回结果
            log.debug("文件上传成功，{}", fileName);
            return Result.ok(fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @DeleteMapping("/blog")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        File file = resolveUploadFile(filename);
        if (file.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(file);
        return Result.ok();
    }

    @GetMapping("/blog/delete")
    public Result deleteBlogImgCompat(@RequestParam("name") String filename) {
        return deleteBlogImg(filename);
    }

    private String createNewFileName(String originalFilename) {
        // 获取后缀
        String suffix = getSuffix(originalFilename);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        File dir = Paths.get(hmdpProperties.getUploadDir(), StrUtil.format("blogs/{}/{}", d1, d2)).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new BizException(ErrorCode.BAD_REQUEST, "图片大小不能超过5MB");
        }
        String suffix = getSuffix(image.getOriginalFilename());
        if (!ALLOWED_SUFFIXES.contains(suffix)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅支持jpg、jpeg、png、webp格式图片");
        }
        String contentType = image.getContentType();
        if (StrUtil.isBlank(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件类型必须为图片");
        }
    }

    private String getSuffix(String originalFilename) {
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        if (StrUtil.isBlank(suffix) || suffix.equals(originalFilename)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件名缺少扩展名");
        }
        return suffix.toLowerCase();
    }

    private void ensureUploadDirExists() {
        File uploadDir = new File(hmdpProperties.getUploadDir());
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    private String toStoragePath(String filePath) {
        return StrUtil.removePrefix(filePath, "/");
    }

    private File resolveUploadFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件名称不能为空");
        }
        Path uploadRoot = Paths.get(hmdpProperties.getUploadDir()).toAbsolutePath().normalize();
        Path target = uploadRoot.resolve(toStoragePath(filePath)).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "错误的文件名称");
        }
        return target.toFile();
    }
}
