package com.cafe.backend.repository;

import com.cafe.backend.entity.CounterQr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CounterQrRepository extends JpaRepository<CounterQr, Long> {

    Optional<CounterQr> findByQrToken(String qrToken);
}