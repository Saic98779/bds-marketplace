package com.sidbi.bds.auth.repository;

import com.sidbi.bds.auth.domain.Enums;
import com.sidbi.bds.auth.domain.User;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailHash(String emailHash);

    Optional<User> findByMobileHash(String mobileHash);

    boolean existsByEmailHash(String emailHash);

    boolean existsByMobileHash(String mobileHash);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, Enums.UserStatus status);

    List<User> findByCatalogSyncPendingTrue();

    @Query("select u.id from User u join u.permissions p where p = :permission and u.status = com.sidbi.bds.auth.domain.Enums.UserStatus.ACTIVE")
    List<Long> findActiveIdsWithPermission(@Param("permission") Permission permission);

    @Query("select u.role, u.status, count(u) from User u group by u.role, u.status")
    List<Object[]> countByRoleAndStatus();

    @Query("select u from User u where u.role = :role and u.createdAt >= :from")
    List<User> findRegisteredSince(@Param("role") Role role, @Param("from") LocalDateTime from);
}
