package com.cafe.backend.repository;

import com.cafe.backend.entity.MarketingCampaignRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketingCampaignRecipientRepository
        extends JpaRepository<MarketingCampaignRecipient, Long> {

    List<MarketingCampaignRecipient> findByCampaignId(Long campaignId);



}