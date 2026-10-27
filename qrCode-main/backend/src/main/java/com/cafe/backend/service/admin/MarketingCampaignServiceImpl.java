package com.cafe.backend.service.admin;

import com.cafe.backend.dto.admin.CreateMarketingCampaignRequest;
import com.cafe.backend.entity.MarketingCampaign;
import com.cafe.backend.entity.MarketingCampaignRecipient;
import com.cafe.backend.enums.MarketingCampaignStatus;
import com.cafe.backend.enums.MarketingDeliveryStatus;
import com.cafe.backend.repository.MarketingCampaignRecipientRepository;
import com.cafe.backend.repository.MarketingCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cafe.backend.entity.Customer;
import com.cafe.backend.enums.MarketingChannel;
import com.cafe.backend.repository.CustomerRepository;
import com.cafe.backend.service.marketing.SmsService;
import com.cafe.backend.service.marketing.WhatsAppService;

import java.util.List;
@Service
@Transactional
public class MarketingCampaignServiceImpl implements MarketingCampaignService {

    private final MarketingCampaignRepository campaignRepository;

    private final MarketingCampaignRepository marketingCampaignRepository;
    private final CustomerRepository customerRepository;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;

    private final MarketingCampaignRecipientRepository recipientRepository;

    public MarketingCampaignServiceImpl(
            MarketingCampaignRepository campaignRepository, MarketingCampaignRepository marketingCampaignRepository, CustomerRepository customerRepository, SmsService smsService, WhatsAppService whatsAppService, MarketingCampaignRecipientRepository recipientRepository) {
        this.campaignRepository = campaignRepository;
        this.marketingCampaignRepository = marketingCampaignRepository;
        this.customerRepository = customerRepository;
        this.smsService = smsService;
        this.whatsAppService = whatsAppService;
        this.recipientRepository = recipientRepository;
    }

    @Override
    public MarketingCampaign createCampaign(
            CreateMarketingCampaignRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Campaign message is required");
        }

        if (request.getChannel() == null) {
            throw new IllegalArgumentException("Marketing channel is required");
        }

        MarketingCampaign campaign = new MarketingCampaign();

        campaign.setName(request.getName().trim());
        campaign.setMessage(request.getMessage().trim());
        campaign.setChannel(request.getChannel());
        campaign.setStatus(MarketingCampaignStatus.DRAFT);

        return campaignRepository.save(campaign);
    }

    @Override
    public void sendCampaign(Long campaignId) {

        MarketingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (campaign.getStatus() != MarketingCampaignStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT campaigns can be sent");
        }

        List<Customer> customers = customerRepository.findAll();

        int totalRecipients = 0;
        int sentCount = 0;
        int failedCount = 0;

        // Only ONE request can move DRAFT -> SENDING; a double click gets 0 here
        int locked = campaignRepository.markSending(campaignId, MarketingCampaignStatus.DRAFT, MarketingCampaignStatus.SENDING);
        if (locked == 0) {
            throw new IllegalStateException("This campaign is already being sent");
        }
        campaign.setStatus(MarketingCampaignStatus.SENDING);
        java.util.Set<String> seenNumbers = new java.util.HashSet<>();
        for (Customer customer : customers) {

            if (customer.isMarketingOptOut()) {
                continue;
            }
            // Same number twice in the table -> send only once
            String mobile = customer.getMobileNumber() == null ? "" : customer.getMobileNumber().trim();
            if (!seenNumbers.add(mobile)) {
                continue;
            }

            // =========================
            // SMS
            // =========================
            if (campaign.getChannel() == MarketingChannel.SMS
                    || campaign.getChannel() == MarketingChannel.BOTH) {

                if (customer.isSmsOptIn()) {

                    MarketingCampaignRecipient recipient =
                            new MarketingCampaignRecipient();

                    recipient.setCampaign(campaign);
                    recipient.setCustomer(customer);
                    recipient.setChannel(MarketingChannel.SMS);

                    totalRecipients++;

                    try {
                        boolean sent = smsService.sendSms(
                                customer.getMobileNumber(),
                                campaign.getMessage()
                        );

                        if (sent) {

                            recipient.setStatus(MarketingDeliveryStatus.SENT);
                            recipient.setSentAt(java.time.LocalDateTime.now());

                            sentCount++;

                        } else {

                            recipient.setStatus(MarketingDeliveryStatus.FAILED);
                            recipient.setFailedAt(java.time.LocalDateTime.now());
                            recipient.setFailureReason(
                                    "SMS provider returned failure"
                            );

                            failedCount++;
                        }

                    } catch (Exception e) {

                        recipient.setStatus(MarketingDeliveryStatus.FAILED);
                        recipient.setFailedAt(java.time.LocalDateTime.now());
                        recipient.setFailureReason(
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Unknown SMS provider error"
                        );

                        failedCount++;
                    }

                    recipientRepository.save(recipient);
                }
            }

            // =========================
            // WHATSAPP
            // =========================
            if (campaign.getChannel() == MarketingChannel.WHATSAPP
                    || campaign.getChannel() == MarketingChannel.BOTH) {

                if (customer.isWhatsappOptIn()) {

                    MarketingCampaignRecipient recipient =
                            new MarketingCampaignRecipient();

                    recipient.setCampaign(campaign);
                    recipient.setCustomer(customer);
                    recipient.setChannel(MarketingChannel.WHATSAPP);

                    totalRecipients++;

                    try {
                        String providerMessageId =
                                whatsAppService.sendMessage(
                                        customer.getMobileNumber(),
                                        campaign.getMessage()
                                );

                        if (providerMessageId != null
                                && !providerMessageId.isBlank()) {

                            recipient.setStatus(
                                    MarketingDeliveryStatus.SENT
                            );

                            recipient.setProviderMessageId(
                                    providerMessageId
                            );

                            recipient.setSentAt(
                                    java.time.LocalDateTime.now()
                            );

                            sentCount++;

                        } else {

                            recipient.setStatus(
                                    MarketingDeliveryStatus.FAILED
                            );

                            recipient.setFailureReason(
                                    "WhatsApp provider did not return a message ID"
                            );

                            recipient.setFailedAt(
                                    java.time.LocalDateTime.now()
                            );

                            failedCount++;
                        }

                    } catch (Exception e) {

                        recipient.setStatus(
                                MarketingDeliveryStatus.FAILED
                        );

                        recipient.setFailureReason(
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Unknown WhatsApp provider error"
                        );

                        recipient.setFailedAt(
                                java.time.LocalDateTime.now()
                        );

                        failedCount++;
                    }

                    recipientRepository.save(recipient);
                }
            }
        }

        campaign.setTotalRecipients(totalRecipients);
        campaign.setSentCount(sentCount);
        campaign.setFailedCount(failedCount);

        if (sentCount == 0 && failedCount > 0) {

            campaign.setStatus(
                    MarketingCampaignStatus.FAILED
            );

        } else if (failedCount > 0) {

            campaign.setStatus(
                    MarketingCampaignStatus.PARTIALLY_FAILED
            );

        } else {

            campaign.setStatus(
                    MarketingCampaignStatus.COMPLETED
            );
        }

        campaignRepository.save(campaign);
    }

    @Override
    public List<MarketingCampaign> getCampaignHistory() {
        return campaignRepository.findAll();
    }

    @Override
    public List<MarketingCampaignRecipient> getCampaignRecipients(Long campaignId) {
        return recipientRepository.findByCampaignId(campaignId);
    }


}