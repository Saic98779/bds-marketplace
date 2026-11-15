package com.sidbi.bds.notification.repository;

import com.sidbi.bds.notification.domain.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {

    Optional<PlatformSetting> findByKey(String key);

    boolean existsByKey(String key);

    List<PlatformSetting> findAllByOrderByGroupAscKeyAsc();

    List<PlatformSetting> findByPublicSettingTrue();
}
