package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.client.NotificationClient;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.storage.FileStorageService;
import com.sidbi.bds.common.storage.StoredFile;
import com.sidbi.bds.procurement.domain.Engagement;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.domain.ProcurementDocument;
import com.sidbi.bds.procurement.domain.QueryTicket;
import com.sidbi.bds.procurement.domain.Quotation;
import com.sidbi.bds.procurement.domain.Rfq;
import com.sidbi.bds.procurement.domain.WorkOrder;
import com.sidbi.bds.procurement.dto.RfqDtos;
import com.sidbi.bds.procurement.repository.EngagementRepository;
import com.sidbi.bds.procurement.repository.ProcurementDocumentRepository;
import com.sidbi.bds.procurement.repository.QueryTicketRepository;
import com.sidbi.bds.procurement.repository.QuotationRepository;
import com.sidbi.bds.procurement.repository.RfqInvitationRepository;
import com.sidbi.bds.procurement.repository.RfqRepository;
import com.sidbi.bds.procurement.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final int MAX_PER_OWNER = 15;

    private final ProcurementDocumentRepository repository;
    private final RfqRepository rfqRepository;
    private final RfqInvitationRepository invitationRepository;
    private final QuotationRepository quotationRepository;
    private final EngagementRepository engagementRepository;
    private final WorkOrderRepository workOrderRepository;
    private final QueryTicketRepository ticketRepository;
    private final FileStorageService storage;
    private final NotificationClient notificationClient;

    @Transactional
    public RfqDtos.DocumentResponse upload(ProcEnums.OwnerType ownerType, Long ownerId, String category, MultipartFile file) {
        AuthUser user = CurrentUser.get();
        if (repository.countByOwnerTypeAndOwnerId(ownerType, ownerId) >= MAX_PER_OWNER) {
            throw BusinessException.badRequest("Maximum number of attachments reached");
        }
        StoredFile stored = storage.store(file, folder(ownerType, ownerId));
        ProcurementDocument document = new ProcurementDocument();
        document.setOwnerType(ownerType);
        document.setOwnerId(ownerId);
        document.setCategory(category == null || category.isBlank() ? "GENERAL" : category.trim().toUpperCase());
        document.setOriginalName(stored.originalName());
        document.setStoredName(stored.storedName());
        document.setContentType(stored.contentType());
        document.setFileSize(stored.size());
        document.setUploadedBy(user.userId());
        document.setUploaderRole(user.role().name());
        document.setUploaderName(user.name());
        repository.save(document);
        notificationClient.audit("DOCUMENT_UPLOADED", ownerType.name(), ownerId, stored.originalName());
        return map(document);
    }

    @Transactional(readOnly = true)
    public List<RfqDtos.DocumentResponse> list(ProcEnums.OwnerType ownerType, Long ownerId) {
        return repository.findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(ownerType, ownerId).stream().map(this::map).toList();
    }

    @Transactional
    public void delete(Long documentId) {
        AuthUser user = CurrentUser.get();
        ProcurementDocument document = repository.findById(documentId).orElseThrow(() -> BusinessException.notFound("Document not found"));
        if (!user.isAdmin() && !document.getUploadedBy().equals(user.userId())) {
            throw BusinessException.forbidden("Only the uploader can remove this document");
        }
        if (document.getOwnerType() == ProcEnums.OwnerType.QUOTATION) {
            Quotation quotation = quotationRepository.findById(document.getOwnerId()).orElseThrow();
            if (quotation.getStatus() != ProcEnums.QuotationStatus.DRAFT) {
                throw BusinessException.conflict("Attachments of a submitted quotation cannot be removed");
            }
        }
        storage.delete(folder(document.getOwnerType(), document.getOwnerId()), document.getStoredName());
        repository.delete(document);
        notificationClient.audit("DOCUMENT_DELETED", document.getOwnerType().name(), document.getOwnerId(), document.getOriginalName());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long documentId) {
        ProcurementDocument document = repository.findById(documentId).orElseThrow(() -> BusinessException.notFound("Document not found"));
        if (!canAccess(document, CurrentUser.get())) {
            throw BusinessException.notFound("Document not found");
        }
        notificationClient.audit("DOCUMENT_DOWNLOADED", document.getOwnerType().name(), document.getOwnerId(), document.getOriginalName());
        return storage.download(folder(document.getOwnerType(), document.getOwnerId()), document.getStoredName(),
                document.getOriginalName(), document.getContentType());
    }

    private boolean canAccess(ProcurementDocument document, AuthUser user) {
        if (user.isAdmin()) {
            return user.has(Permission.RFQ_MONITOR) || user.has(Permission.GRIEVANCE_MANAGE);
        }
        Long uid = user.userId();
        return switch (document.getOwnerType()) {
            case RFQ -> {
                Rfq rfq = rfqRepository.findById(document.getOwnerId()).orElse(null);
                yield rfq != null && (rfq.getBuyerId().equals(uid) || invitationRepository.existsByRfqIdAndSellerId(rfq.getId(), uid));
            }
            case QUOTATION -> {
                Quotation q = quotationRepository.findById(document.getOwnerId()).orElse(null);
                yield q != null && (q.getSellerId().equals(uid)
                        || (q.getBuyerId().equals(uid) && q.getStatus() != ProcEnums.QuotationStatus.DRAFT));
            }
            case ENGAGEMENT -> {
                Engagement e = engagementRepository.findById(document.getOwnerId()).orElse(null);
                yield e != null && (e.getSellerId().equals(uid) || e.getBuyerId().equals(uid));
            }
            case WORK_ORDER -> {
                WorkOrder w = workOrderRepository.findById(document.getOwnerId()).orElse(null);
                yield w != null && (w.getSellerId().equals(uid) || w.getBuyerId().equals(uid));
            }
            case QUERY -> {
                QueryTicket t = ticketRepository.findById(document.getOwnerId()).orElse(null);
                yield t != null && (uid.equals(t.getSellerId()) || uid.equals(t.getBuyerId()) || uid.equals(t.getRaisedById()));
            }
        };
    }

    RfqDtos.DocumentResponse map(ProcurementDocument d) {
        return new RfqDtos.DocumentResponse(d.getId(), d.getOwnerType(), d.getOwnerId(), d.getCategory(), d.getOriginalName(),
                d.getContentType(), d.getFileSize(), d.getUploaderRole(), d.getUploaderName(), d.getCreatedAt());
    }

    private static String folder(ProcEnums.OwnerType type, Long ownerId) {
        return "procurement/" + type.name().toLowerCase() + "/" + ownerId;
    }
}
