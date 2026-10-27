package com.cafe.backend.repository;

import com.cafe.backend.entity.CafeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CafeSettingRepository extends JpaRepository<CafeSetting, Long> {
}