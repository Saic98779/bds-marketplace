package com.sidbi.bds.notification.domain;

import com.sidbi.bds.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
public class PlatformSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "setting_value", length = 2000)
    private String value;

    @Column(name = "setting_group", length = 40)
    private String group;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "public_flag", nullable = false)
    private boolean publicSetting;
}
