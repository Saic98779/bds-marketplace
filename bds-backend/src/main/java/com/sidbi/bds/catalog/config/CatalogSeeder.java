package com.sidbi.bds.catalog.config;

import com.sidbi.bds.catalog.domain.CatalogEnums;
import com.sidbi.bds.catalog.domain.CatalogSeller;
import com.sidbi.bds.catalog.domain.ListingModeration;
import com.sidbi.bds.catalog.domain.ServiceCategory;
import com.sidbi.bds.catalog.domain.ServiceListing;
import com.sidbi.bds.catalog.domain.ServiceSubcategory;
import com.sidbi.bds.catalog.repository.CatalogSellerRepository;
import com.sidbi.bds.catalog.repository.ListingModerationRepository;
import com.sidbi.bds.catalog.repository.ServiceCategoryRepository;
import com.sidbi.bds.catalog.repository.ServiceListingRepository;
import com.sidbi.bds.common.client.IdentityClient;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.util.References;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogSeeder implements ApplicationRunner {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceListingRepository listingRepository;
    private final CatalogSellerRepository sellerRepository;
    private final ListingModerationRepository moderationRepository;
    private final IdentityClient identityClient;

    @Value("${bds.catalog.demo-listings:false}")
    private boolean demoListings;

    private volatile boolean demoDone;
    private volatile int demoAttempts;

    private record Seed(String code, String name, String icon, String description, List<String> subcategories) {
    }

    private static final List<Seed> SEEDS = List.of(
            new Seed("CONSULTANCY", "Business & Management Consultancy", "business_center",
                    "Business plans, DPRs, project reports, feasibility studies, strategy and process advisory",
                    List.of("Detailed Project Reports", "Business Plans", "Feasibility Studies", "Strategy Advisory", "Process Advisory")),
            new Seed("FINANCE", "Finance & Credit Facilitation", "account_balance",
                    "Loan documentation, financial modelling, subsidy guidance and credit facilitation",
                    List.of("Loan Documentation", "Financial Modelling", "Subsidy Guidance", "Credit Facilitation", "Working Capital Advisory")),
            new Seed("COMPLIANCE", "Legal, Tax & Compliance", "gavel",
                    "GST, accounting, audit, registrations, legal and regulatory support",
                    List.of("GST Services", "Accounting & Bookkeeping", "Audit", "Business Registrations", "Legal Advisory")),
            new Seed("TECHNOLOGY", "Digital Transformation & IT", "devices",
                    "Websites, ERP, CRM, cybersecurity, automation and cloud services",
                    List.of("Website Development", "ERP Implementation", "CRM Solutions", "Cybersecurity", "Cloud & Automation")),
            new Seed("MARKETING", "Marketing & Market Access", "campaign",
                    "Branding, advertising, export consultancy, market research and buyer linkages",
                    List.of("Branding", "Digital Marketing", "Export Consultancy", "Market Research", "Buyer Linkages")),
            new Seed("TRAINING", "HR & Skill Development", "school",
                    "Recruitment, training, capacity building and workforce development",
                    List.of("Recruitment", "Skill Training", "Capacity Building", "HR Policy Advisory")),
            new Seed("QUALITY", "Quality & Productivity", "verified",
                    "ISO, ZED, lean manufacturing, quality certification and process improvement",
                    List.of("ISO Certification", "ZED Certification", "Lean Manufacturing", "Process Improvement")),
            new Seed("SUSTAINABILITY", "Sustainability & Energy", "eco",
                    "Energy audits, renewable energy, environmental compliance and ESG",
                    List.of("Energy Audit", "Renewable Energy", "Environmental Compliance", "ESG Advisory"))
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        int order = 1;
        for (Seed seed : SEEDS) {
            ServiceCategory category = new ServiceCategory();
            category.setCode(seed.code());
            category.setName(seed.name());
            category.setIcon(seed.icon());
            category.setDescription(seed.description());
            category.setDisplayOrder(order++);
            category.setActive(true);
            int subOrder = 1;
            for (String name : seed.subcategories()) {
                ServiceSubcategory sub = new ServiceSubcategory();
                sub.setCategory(category);
                sub.setName(name);
                sub.setDisplayOrder(subOrder++);
                sub.setActive(true);
                category.getSubcategories().add(sub);
            }
            categoryRepository.save(category);
        }
    }

    @Scheduled(fixedDelay = 20_000, initialDelay = 15_000)
    @Transactional
    public void seedDemoListings() {
        if (!demoListings || demoDone || demoAttempts > 30) {
            return;
        }
        demoAttempts++;
        if (listingRepository.count() > 0) {
            demoDone = true;
            return;
        }
        UserSummary seller;
        try {
            seller = identityClient.lookupByEmail("seller@bdsmarketplace.in");
        } catch (RuntimeException ex) {
            return;
        }
        CatalogSeller catalogSeller = sellerRepository.findById(seller.id()).orElseGet(CatalogSeller::new);
        catalogSeller.setSellerId(seller.id());
        catalogSeller.setSellerName(seller.displayName());
        catalogSeller.setState(seller.state());
        catalogSeller.setActive(seller.active());
        catalogSeller.setUpdatedAt(LocalDateTime.now());
        sellerRepository.save(catalogSeller);

        List<ServiceCategory> categories = categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
        listing(seller, categories.get(0), 0, "Detailed Project Report (DPR) for Term Loan",
                "Bank-ready DPR covering technical, market and financial viability for MSME term loan applications.",
                "Site visit, promoter discussion, market assessment, cost of project, means of finance, 7-year projections, ratio analysis and sensitivity analysis.",
                "Bank-ready DPR (PDF and editable Word), financial model (Excel), presentation deck",
                CatalogEnums.DeliveryMode.HYBRID, "3-4 weeks", new BigDecimal("45000"), CatalogEnums.PricingModel.FIXED_FEE, 12, true);
        listing(seller, categories.get(1), 1, "Financial Modelling & Credit Appraisal Support",
                "Integrated three-statement financial model with DSCR, IRR and break-even analysis aligned to lender formats.",
                "Assumption workshop, revenue and cost build-up, funding structure, scenario analysis and lender query support.",
                "Financial model, assumptions note, CMA data, query response support",
                CatalogEnums.DeliveryMode.ONLINE, "2 weeks", new BigDecimal("30000"), CatalogEnums.PricingModel.PER_ASSIGNMENT, 10, true);
        listing(seller, categories.get(1), 3, "CGTMSE & Collateral-free Credit Facilitation",
                "End-to-end support for collateral-free credit under CGTMSE including eligibility review and documentation.",
                "Eligibility assessment, documentation checklist, application preparation, bank liaison guidance.",
                "Eligibility report, complete application file, lender liaison note",
                CatalogEnums.DeliveryMode.HYBRID, "4-6 weeks", null, CatalogEnums.PricingModel.CUSTOM_QUOTATION, 12, false);
        demoDone = true;
    }

    private void listing(UserSummary seller, ServiceCategory category, int subIndex, String title, String description,
                         String scope, String deliverables, CatalogEnums.DeliveryMode mode, String duration, BigDecimal fee,
                         CatalogEnums.PricingModel pricing, int experience, boolean featured) {
        ServiceListing listing = new ServiceListing();
        listing.setSellerId(seller.id());
        listing.setSellerName(seller.displayName());
        listing.setTitle(title);
        listing.setCategory(category);
        listing.setSubcategory(category.getSubcategories().size() > subIndex ? category.getSubcategories().get(subIndex) : null);
        listing.setShortDescription(description);
        listing.setDescription(description + " Delivered by experienced chartered accountants and sector specialists with a structured, lender-aligned methodology.");
        listing.setScopeOfWork(scope);
        listing.setDeliverables(deliverables);
        listing.setMethodology("Kick-off discussion, data collection, analysis, draft review with client, finalisation and handover.");
        listing.setTargetIndustries("Manufacturing,Food Processing,Textiles,Services");
        listing.setDeliveryMode(mode);
        listing.setServiceLocations("Uttar Pradesh,Delhi,Uttarakhand");
        listing.setState(seller.state());
        listing.setDistrict(seller.district());
        listing.setExperienceYears(experience);
        listing.setEstimatedDuration(duration);
        listing.setIndicativeFee(fee);
        listing.setPricingModel(pricing);
        listing.setTags("MSME,Bank Loan,Project Finance");
        listing.setStatus(CatalogEnums.ListingStatus.APPROVED);
        listing.setFeatured(featured);
        listing.setSubmittedAt(LocalDateTime.now());
        listing.setPublishedAt(LocalDateTime.now());
        listing.setModeratedAt(LocalDateTime.now());
        listing.setModerationRemarks("Demo listing");
        listingRepository.save(listing);
        listing.setReferenceNo(References.of("SVC", listing.getId()));
        ListingModeration moderation = new ListingModeration();
        moderation.setListingId(listing.getId());
        moderation.setAction(CatalogEnums.ModerationAction.APPROVED);
        moderation.setToStatus(CatalogEnums.ListingStatus.APPROVED);
        moderation.setRemarks("Demo listing approved at setup");
        moderation.setActorName("System");
        moderation.setActorRole("SYSTEM");
        moderationRepository.save(moderation);
    }
}
