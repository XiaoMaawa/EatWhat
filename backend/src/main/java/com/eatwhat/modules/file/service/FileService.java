package com.eatwhat.modules.file.service;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务：校验类型与大小，按 yyyy/MM/dd 分目录存储。
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    /** 单文件大小上限 5MB */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final String uploadPath;
    private final String urlPrefix;

    public FileService(@Value("${eatwhat.upload.path:./data/upload}") String uploadPath,
                       @Value("${eatwhat.upload.url-prefix:/uploads}") String urlPrefix) {
        this.uploadPath = uploadPath;
        this.urlPrefix = urlPrefix;
    }

    /**
     * 保存上传文件，返回可访问 URL、大小与原始文件名。
     */
    public Map<String, Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BizException.of(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String originalName = file.getOriginalFilename() == null ? "unknown"
                : Paths.get(file.getOriginalFilename().replace("\\", "/")).getFileName().toString();
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw BizException.of(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        if (!matchesMagic(file, extension)) {
            throw BizException.of(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
        }

        String datePath = LocalDate.now().format(DATE_PATH);
        Path dir = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(datePath);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = dir.resolve(fileName).normalize();
        if (!target.startsWith(Paths.get(uploadPath).toAbsolutePath().normalize())) {
            // 防御路径穿越
            throw BizException.of(ErrorCode.PARAM_INVALID, "非法的文件名");
        }

        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw BizException.of(ErrorCode.UPLOAD_FAILED);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", urlPrefix + "/" + datePath + "/" + fileName);
        result.put("size", file.getSize());
        result.put("originalName", originalName);
        return result;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 通过文件头魔数校验真实类型，避免改扩展名绕过。
     */
    private boolean matchesMagic(MultipartFile file, String extension) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(head);
        } catch (IOException e) {
            log.warn("读取文件头失败", e);
            return false;
        }
        if (read < 4) {
            return false;
        }
        boolean jpeg = (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
        boolean png = (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
        boolean webp = read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        return switch (extension) {
            case "jpg", "jpeg" -> jpeg;
            case "png" -> png;
            case "webp" -> webp;
            default -> false;
        };
    }
}
