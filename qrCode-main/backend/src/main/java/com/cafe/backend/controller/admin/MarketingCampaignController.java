package com.cafe.backend.controller.admin;
import com.cafe.backend.entity.MarketingCampaignRecipient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import com.cafe.backend.dto.admin.CreateMarketingCampaignRequest;
import com.cafe.backend.entity.MarketingCampaign;
import com.cafe.backend.service.admin.MarketingCampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cafe.backend.security.RateLimitService;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/admin/marketing/campaigns")
public class MarketingCampaignController {

    private final RateLimitService rateLimitService;
    private final MarketingCampaignService marketingCampaignService;

    public MarketingCampaignController(
            RateLimitService rateLimitService, MarketingCampaignService marketingCampaignService) {
        this.rateLimitService = rateLimitService;
        this.marketingCampaignService = marketingCampaignService;
    }

    @PostMapping
    public ResponseEntity<MarketingCampaign> createCampaign(
            @RequestBody CreateMarketingCampaignRequest request) {

        return ResponseEntity.ok(
                marketingCampaignService.createCampaign(request)
        );
    }

    @PostMapping("/{campaignId}/send")
    public ResponseEntity<Void> sendCampaign(
            @PathVariable Long campaignId,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();

        boolean allowed = rateLimitService.isAllowed(
                "rate-limit:marketing-send:" + clientIp,
                5,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            return ResponseEntity.status(429).build();
        }

        marketingCampaignService.sendCampaign(campaignId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<MarketingCampaign>> getCampaignHistory() {

        return ResponseEntity.ok(
                marketingCampaignService.getCampaignHistory()
        );
    }

    @GetMapping("/{campaignId}/recipients")
    public ResponseEntity<List<MarketingCampaignRecipient>> getCampaignRecipients(
            @PathVariable Long campaignId) {

        return ResponseEntity.ok(
                marketingCampaignService.getCampaignRecipients(campaignId)
        );
    }
}