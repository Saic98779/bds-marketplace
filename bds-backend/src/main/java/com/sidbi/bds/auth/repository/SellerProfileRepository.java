package com.sidbi.bds.auth.repository;

import com.sidbi.bds.auth.domain.SellerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long>, JpaSpecificationExecutor<SellerProfile> {

    @EntityGraph(attributePaths = "user")
    Optional<SellerProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<SellerProfile> findByUserIdIn(Collection<Long> userIds);

    boolean existsByPanHash(String panHash);

    @Query("select s.state, count(s) from SellerProfile s where s.state is not null group by s.state order by count(s) desc")
    List<Object[]> countByState();

    @Query("select s.providerType, count(s) from SellerProfile s group by s.providerType")
    List<Object[]> countByProviderType();
}
