package com.interview.auth.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 记录 Spring Boot 实际启动端口，供前端开发代理自动读取。
 * 应用启动后把端口写入约定文件，避免 IDE 直启动时前端仍然指向旧地址。
 */
@Component
@Slf4j
public class RuntimePortRecorder {

    /**
     * 将容器绑定后的真实端口写到本地文件。
     * 写入失败只影响联调代理，不影响服务本身启动与对外提供接口。
     */
    @EventListener
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        Path portFile = resolvePortFile();
        try {
            Files.createDirectories(portFile.getParent());
            Files.writeString(portFile, String.valueOf(port), StandardCharsets.UTF_8);
            log.info("Runtime port {} written to {}", port, portFile.toAbsolutePath());
        } catch (IOException exception) {
            log.warn("Failed to write runtime port file: {}", exception.getMessage());
        }
    }

    private Path resolvePortFile() {
        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        if (userDir.getFileName() != null && "server-java".equalsIgnoreCase(userDir.getFileName().toString())) {
            return userDir.resolve("target/runtime-port.txt");
        }
        return userDir.resolve("server-java/target/runtime-port.txt");
    }
}
