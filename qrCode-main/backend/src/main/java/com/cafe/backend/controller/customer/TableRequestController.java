package com.cafe.backend.controller.customer; // CustomerCashController jaisa package

import com.cafe.backend.entity.CafeTable;
import com.cafe.backend.enums.BusinessType;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.CafeTableRepository;
import com.cafe.backend.service.admin.CafeSettingService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/orders") // same public prefix as customer cancel
public class TableRequestController {

    private static final long COOLDOWN_MS = 60_000; // one alert per table per minute

    private final Map<String, Long> lastCall = new ConcurrentHashMap<>();
    private final CafeTableRepository cafeTableRepository;
    private final CafeSettingService cafeSettingService;
    private final SimpMessagingTemplate messagingTemplate;

    public TableRequestController(CafeTableRepository cafeTableRepository,
                                  CafeSettingService cafeSettingService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.cafeTableRepository = cafeTableRepository;
        this.cafeSettingService = cafeSettingService;
        this.messagingTemplate = messagingTemplate;
    }

    // type: WAITER (all cafes) or BILL (large cafe / restaurant only)
    @PostMapping("/customer/table-request")
    public Map<String, String> request(@RequestBody Map<String, String> body) {

        String type = body.get("type");
        if (!"WAITER".equals(type) && !"BILL".equals(type)) {
            throw new BadRequestException("INVALID_REQUEST", "Invalid request.");
        }
        if ("BILL".equals(type) && cafeSettingService.getBusinessType() == BusinessType.SMALL_CAFE) {
            throw new BadRequestException("INVALID_REQUEST", "Please pay at the counter.");
        }

        String qrToken = body.get("qrToken") == null ? "" : body.get("qrToken");
        CafeTable table = cafeTableRepository.findByQrToken(qrToken)
                .filter(CafeTable::isActive)
                .orElseThrow(() -> new NotFoundException("INVALID_QR_TOKEN", "This option works only at a table."));

        // Same table pressing again within a minute -> no new alert for staff
        String key = table.getId() + ":" + type;
        long now = System.currentTimeMillis();
        Long last = lastCall.get(key);
        if (last != null && now - last < COOLDOWN_MS) {
            return Map.of("status", "ALREADY_CALLED");
        }
        lastCall.put(key, now);

        Object payload = Map.of(
                "type", type,
                "tableNumber", String.valueOf(table.getTableNumber()),
                "at", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/admin/requests", payload);

        return Map.of("status", "SENT");
    }
}