package com.sidbi.bds.auth.service;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.SellerDocument;
import com.sidbi.bds.auth.repository.SellerDocumentRepository;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.storage.FileStorageService;
import com.sidbi.bds.common.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerDocumentService {

    private static final int MAX_DOCUMENTS = 25;

    private final SellerDocumentRepository repository;
    private final FileStorageService storage;

    @Transactional
    public void storeAll(Long userId, List<MultipartFile> files, List<Enums.DocumentType> types) {
        if (files == null || files.isEmpty()) {
            return;
        }
        if (types == null || types.size() != files.size()) {
            throw BusinessException.badRequest("Each uploaded document must have a document type");
        }
        for (int i = 0; i < files.size(); i++) {
            store(userId, types.get(i), files.get(i));
        }
    }

    @Transactional
    public SellerDocument store(Long userId, Enums.DocumentType type, MultipartFile file) {
        if (repository.findByUserIdOrderByCreatedAtDesc(userId).size() >= MAX_DOCUMENTS) {
            throw BusinessException.badRequest("Maximum number of documents reached");
        }
        StoredFile stored = storage.store(file, folder(userId));
        SellerDocument document = new SellerDocument();
        document.setUserId(userId);
        document.setDocumentType(type);
        document.setOriginalName(stored.originalName());
        document.setStoredName(stored.storedName());
        document.setContentType(stored.contentType());
        document.setFileSize(stored.size());
        document.setChecksum(stored.checksum());
        return repository.save(document);
    }

    @Transactional(readOnly = true)
    public List<SellerDocument> list(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long userId, Long documentId) {
        SellerDocument document = repository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> BusinessException.notFound("Document not found"));
        return storage.download(folder(userId), document.getStoredName(), document.getOriginalName(), document.getContentType());
    }

    @Transactional
    public void delete(Long userId, Long documentId) {
        SellerDocument document = repository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> BusinessException.notFound("Document not found"));
        if (document.isVerified()) {
            throw BusinessException.conflict("Verified documents cannot be deleted");
        }
        storage.delete(folder(userId), document.getStoredName());
        repository.delete(document);
    }

    @Transactional
    public SellerDocument verify(Long userId, Long documentId, boolean verified, String remarks, Long adminId) {
        SellerDocument document = repository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> BusinessException.notFound("Document not found"));
        document.setVerified(verified);
        document.setRemarks(remarks);
        document.setVerifiedBy(adminId);
        document.setVerifiedAt(java.time.LocalDateTime.now());
        return document;
    }

    private static String folder(Long userId) {
        return "seller-documents/" + userId;
    }
}
