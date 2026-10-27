package com.cafe.backend.repository;

import com.cafe.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Notification> findByReadFalseOrderByCreatedAtDesc();
}