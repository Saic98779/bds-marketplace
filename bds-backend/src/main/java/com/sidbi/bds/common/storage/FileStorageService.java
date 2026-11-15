package com.sidbi.bds.common.storage;

import com.sidbi.bds.common.exception.BusinessException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class FileStorageService {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final StorageProperties properties;
    private final Path root;

    public FileStorageService(StorageProperties properties) {
        this.properties = properties;
        this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("File is empty");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw BusinessException.badRequest("File exceeds the maximum permitted size of "
                    + (properties.getMaxFileSizeBytes() / (1024 * 1024)) + " MB");
        }
        String original = sanitize(file.getOriginalFilename());
        String extension = extension(original);
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw BusinessException.badRequest("File type ." + extension + " is not permitted");
        }
        try {
            byte[] header = readHeader(file);
            if (!signatureMatches(extension, header)) {
                throw BusinessException.badRequest("File content does not match its declared type");
            }
            Path directory = resolveFolder(folder);
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(storedName).normalize();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(storedName, original, CONTENT_TYPES.get(extension), file.getSize(),
                    HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store file");
        }
    }

    public Resource load(String folder, String storedName) {
        try {
            Path file = resolveFolder(folder).resolve(storedName).normalize();
            if (!file.startsWith(root) || !Files.exists(file)) {
                throw BusinessException.notFound("File not found");
            }
            return new UrlResource(file.toUri());
        } catch (MalformedURLException ex) {
            throw BusinessException.notFound("File not found");
        }
    }

    public void delete(String folder, String storedName) {
        try {
            Path file = resolveFolder(folder).resolve(storedName).normalize();
            if (file.startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {
        }
    }

    public ResponseEntity<Resource> download(String folder, String storedName, String originalName, String contentType) {
        Resource resource = load(folder, storedName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(originalName, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private Path resolveFolder(String folder) {
        Path directory = root.resolve(folder).normalize();
        if (!directory.startsWith(root)) {
            throw BusinessException.badRequest("Invalid storage location");
        }
        return directory;
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "document";
        }
        String base = Path.of(name).getFileName().toString();
        return base.replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readHeader(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(8);
        }
    }

    private static boolean signatureMatches(String extension, byte[] h) {
        return switch (extension) {
            case "pdf" -> starts(h, 0x25, 0x50, 0x44, 0x46);
            case "png" -> starts(h, 0x89, 0x50, 0x4E, 0x47);
            case "jpg", "jpeg" -> starts(h, 0xFF, 0xD8, 0xFF);
            case "docx", "xlsx" -> starts(h, 0x50, 0x4B, 0x03, 0x04);
            case "doc", "xls" -> starts(h, 0xD0, 0xCF, 0x11, 0xE0);
            default -> false;
        };
    }

    private static boolean starts(byte[] header, int... signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((header[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
