package com.cafe.backend.service.admin;

import com.cafe.backend.dto.admin.CreateMarketingCampaignRequest;
import com.cafe.backend.entity.MarketingCampaign;
import com.cafe.backend.entity.MarketingCampaignRecipient;

import java.util.List;

public interface MarketingCampaignService {

    MarketingCampaign createCampaign(CreateMarketingCampaignRequest request);

    void sendCampaign(Long campaignId);

    List<MarketingCampaign> getCampaignHistory();

    List<MarketingCampaignRecipient> getCampaignRecipients(Long campaignId);
}