package com.sidbi.bds.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bds.storage")
public class StorageProperties {

    private String root = "./data/files";
    private long maxFileSizeBytes = 5 * 1024 * 1024;
    private List<String> allowedExtensions = List.of("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx");

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}
