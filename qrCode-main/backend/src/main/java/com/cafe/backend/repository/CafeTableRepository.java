package com.cafe.backend.repository;

import com.cafe.backend.entity.CafeTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CafeTableRepository extends JpaRepository<CafeTable, Long> {

    Optional<CafeTable> findByQrToken(String qrToken);
}