package com.sidbi.bds;

import com.sidbi.bds.common.client.CatalogClient;
import com.sidbi.bds.common.client.IdentityClient;
import com.sidbi.bds.common.client.dto.UserSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "bds.storage.root=./target/test-data")
@ActiveProfiles("h2")
class BdsApplicationTests {

    @Autowired
    private IdentityClient identityClient;

    @Autowired
    private CatalogClient catalogClient;

    @Test
    void modulesCallEachOtherInProcess() {
        UserSummary seller = identityClient.lookupByEmail("seller@bdsmarketplace.in");
        assertThat(seller.role()).isEqualTo("SELLER");

        assertThat(catalogClient.resolveCategory(1L, null).categoryId()).isEqualTo(1L);
    }
}
