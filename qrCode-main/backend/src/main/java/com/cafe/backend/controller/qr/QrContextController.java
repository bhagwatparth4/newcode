package com.cafe.backend.controller.qr;

import com.cafe.backend.dto.qr.QrContextResponse;
import com.cafe.backend.service.qr.QrContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
public class QrContextController {

    private final QrContextService qrContextService;

    public QrContextController(QrContextService qrContextService) {
        this.qrContextService = qrContextService;
    }

    @GetMapping("/{qrToken}")
    public ResponseEntity<QrContextResponse> resolveQrToken(
            @PathVariable String qrToken) {

        return ResponseEntity.ok(
                qrContextService.resolveQrToken(qrToken)
        );
    }
}