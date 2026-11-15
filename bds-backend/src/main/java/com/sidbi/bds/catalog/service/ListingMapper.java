package com.sidbi.bds.catalog.service;

import com.sidbi.bds.catalog.domain.CatalogEnums;
import com.sidbi.bds.catalog.domain.CatalogSeller;
import com.sidbi.bds.catalog.domain.ListingModeration;
import com.sidbi.bds.catalog.domain.ServiceAttachment;
import com.sidbi.bds.catalog.domain.ServiceCategory;
import com.sidbi.bds.catalog.domain.ServiceListing;
import com.sidbi.bds.catalog.domain.ServiceSubcategory;
import com.sidbi.bds.catalog.dto.CategoryDtos;
import com.sidbi.bds.catalog.dto.ListingDtos;
import com.sidbi.bds.common.util.Csv;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ListingMapper {

    public ListingDtos.ListingCard card(ServiceListing l, CatalogSeller seller) {
        ServiceCategory c = l.getCategory();
        ServiceSubcategory s = l.getSubcategory();
        return new ListingDtos.ListingCard(
                l.getId(),
                l.getReferenceNo(),
                l.getTitle(),
                l.getShortDescription() != null ? l.getShortDescription() : abbreviate(l.getDescription(), 220),
                c.getId(),
                c.getName(),
                c.getIcon(),
                s != null ? s.getId() : null,
                s != null ? s.getName() : null,
                l.getSellerId(),
                seller != null && seller.getSellerName() != null ? seller.getSellerName() : l.getSellerName(),
                seller != null ? seller.getState() : null,
                seller != null ? seller.getAverageRating() : null,
                seller != null ? seller.getRatingCount() : null,
                seller != null && seller.isActive(),
                l.getDeliveryMode(),
                l.getState(),
                l.getExperienceYears(),
                l.getEstimatedDuration(),
                l.getIndicativeFee(),
                l.getPricingModel(),
                l.getStatus(),
                l.isFeatured(),
                l.getPublishedAt(),
                l.getUpdatedAt()
        );
    }

    public ListingDtos.ListingDetail detail(ServiceListing l, CatalogSeller seller) {
        return new ListingDtos.ListingDetail(
                card(l, seller),
                l.getDescription(),
                l.getScopeOfWork(),
                l.getDeliverables(),
                l.getMethodology(),
                Csv.split(l.getTargetIndustries()),
                Csv.split(l.getServiceLocations()),
                l.getDistrict(),
                Csv.split(l.getTags()),
                l.getAttachments().stream().map(this::attachment).toList(),
                l.getModerationRemarks(),
                l.getViewCount(),
                l.getEnquiryCount(),
                l.getSubmittedAt(),
                l.getCreatedAt(),
                isPublic(l, seller)
        );
    }

    public ListingDtos.AttachmentResponse attachment(ServiceAttachment a) {
        return new ListingDtos.AttachmentResponse(a.getId(), a.getLabel(), a.getOriginalName(), a.getContentType(), a.getFileSize());
    }

    public ListingDtos.ModerationResponse moderation(ListingModeration m) {
        return new ListingDtos.ModerationResponse(m.getId(), m.getAction(), m.getFromStatus(), m.getToStatus(), m.getRemarks(),
                m.getActorName(), m.getActorRole(), m.getCreatedAt());
    }

    public CategoryDtos.CategoryResponse category(ServiceCategory c, Map<Long, Long> categoryCounts, Map<Long, Long> subcategoryCounts, boolean activeOnly) {
        return new CategoryDtos.CategoryResponse(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getDescription(),
                c.getIcon(),
                c.getDisplayOrder(),
                c.isActive(),
                Csv.split(c.getMandatoryFields()),
                c.getEligibilityRules(),
                categoryCounts.getOrDefault(c.getId(), 0L),
                c.getSubcategories().stream()
                        .filter(s -> !activeOnly || s.isActive())
                        .map(s -> new CategoryDtos.SubcategoryResponse(s.getId(), s.getName(), s.getDescription(), s.getDisplayOrder(),
                                s.isActive(), subcategoryCounts.getOrDefault(s.getId(), 0L)))
                        .toList()
        );
    }

    public static boolean isPublic(ServiceListing l, CatalogSeller seller) {
        return l.getStatus() == CatalogEnums.ListingStatus.APPROVED && seller != null && seller.isActive();
    }

    private static String abbreviate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1).trim() + "…";
    }
}
