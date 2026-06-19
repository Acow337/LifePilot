package com.hmdp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
public class StartupConfigValidator implements ApplicationRunner {

    private final HmdpProperties hmdpProperties;

    @Override
    public void run(ApplicationArguments args) {
        File dir = new File(hmdpProperties.getUploadDir());
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建上传目录: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new IllegalStateException("上传目录配置无效，必须是目录: " + dir.getAbsolutePath());
        }
    }
}

