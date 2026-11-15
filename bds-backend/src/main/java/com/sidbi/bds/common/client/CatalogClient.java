package com.sidbi.bds.common.client;

import com.sidbi.bds.catalog.service.CatalogSellerService;
import com.sidbi.bds.catalog.service.CategoryService;
import com.sidbi.bds.catalog.service.PublicCatalogService;
import com.sidbi.bds.common.client.dto.CategoryRef;
import com.sidbi.bds.common.client.dto.ListingSummary;
import com.sidbi.bds.common.client.dto.RatingUpdate;
import com.sidbi.bds.common.client.dto.SellerStatusUpdate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogClient {

    private final ObjectProvider<CatalogSellerService> sellerService;
    private final ObjectProvider<CategoryService> categoryService;
    private final ObjectProvider<PublicCatalogService> catalogService;
    private final ModuleCalls calls;

    public CatalogClient(ObjectProvider<CatalogSellerService> sellerService, ObjectProvider<CategoryService> categoryService,
                         ObjectProvider<PublicCatalogService> catalogService, ModuleCalls calls) {
        this.sellerService = sellerService;
        this.categoryService = categoryService;
        this.catalogService = catalogService;
        this.calls = calls;
    }

    public void updateSellerStatus(Long sellerId, SellerStatusUpdate update) {
        calls.run(() -> sellerService.getObject().updateStatus(sellerId, update));
    }

    public void updateSellerRating(Long sellerId, RatingUpdate update) {
        calls.run(() -> sellerService.getObject().updateRating(sellerId, update));
    }

    public ListingSummary listing(Long listingId) {
        return calls.call(() -> catalogService.getObject().summary(listingId));
    }

    public void recordEnquiry(Long listingId) {
        try {
            calls.run(() -> catalogService.getObject().recordEnquiry(listingId));
        } catch (RuntimeException ignored) {
        }
    }

    public CategoryRef resolveCategory(Long categoryId, Long subcategoryId) {
        return calls.call(() -> categoryService.getObject().resolve(categoryId, subcategoryId));
    }

    public List<Long> sellersForCategory(Long categoryId, Long subcategoryId) {
        return calls.call(() -> catalogService.getObject().sellersForCategory(categoryId, subcategoryId));
    }
}
