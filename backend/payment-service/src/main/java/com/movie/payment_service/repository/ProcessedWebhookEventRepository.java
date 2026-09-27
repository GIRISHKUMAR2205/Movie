package com.movie.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.payment_service.entity.ProcessedWebhookEvent;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, String> {
}
