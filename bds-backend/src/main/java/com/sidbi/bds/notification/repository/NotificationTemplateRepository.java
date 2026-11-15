package com.sidbi.bds.notification.repository;

import com.sidbi.bds.notification.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByCode(String code);

    boolean existsByCode(String code);

    List<NotificationTemplate> findAllByOrderByCategoryAscCodeAsc();
}
