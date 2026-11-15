package com.sidbi.bds.common.storage;

public record StoredFile(String storedName, String originalName, String contentType, long size, String checksum) {
}
