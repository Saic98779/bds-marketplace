package com.sidbi.bds.auth.repository;

import com.sidbi.bds.auth.domain.PurchaserProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaserProfileRepository extends JpaRepository<PurchaserProfile, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<PurchaserProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<PurchaserProfile> findByUserIdIn(Collection<Long> userIds);

    @Query("select p.purchaserType, count(p) from PurchaserProfile p group by p.purchaserType")
    List<Object[]> countByType();

    @Query("select p.industrySector, count(p) from PurchaserProfile p where p.industrySector is not null group by p.industrySector order by count(p) desc")
    List<Object[]> countByIndustry();
}
