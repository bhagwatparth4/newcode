package com.cafe.backend.repository;

import com.cafe.backend.entity.MarketingCampaign;
import com.cafe.backend.enums.MarketingCampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketingCampaignRepository
        extends JpaRepository<MarketingCampaign, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "update MarketingCampaign c set c.status = :sending where c.id = :id and c.status = :draft")
    int markSending(@org.springframework.data.repository.query.Param("id") Long id,
                    @org.springframework.data.repository.query.Param("draft") MarketingCampaignStatus draft,
                    @org.springframework.data.repository.query.Param("sending") MarketingCampaignStatus sending);
}