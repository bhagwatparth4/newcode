package com.cafe.backend.controller.counter;

import com.cafe.backend.entity.CounterQr;
import com.cafe.backend.service.counter.CounterQrService;
import com.cafe.backend.service.qr.QrCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/counter-qr")
public class CounterQrController {

    private final QrCodeService qrCodeService;
    private final CounterQrService counterQrService;

    public CounterQrController(QrCodeService qrCodeService, CounterQrService counterQrService) {
        this.qrCodeService = qrCodeService;
        this.counterQrService = counterQrService;
    }

    @PostMapping
    public ResponseEntity<CounterQr> createCounterQr() {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(counterQrService.createCounterQr());
    }

    @GetMapping
    public ResponseEntity<CounterQr> getActiveCounterQr() {

        return ResponseEntity.ok(
                counterQrService.getActiveCounterQr()
        );
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCounterQr(
            @PathVariable Long id) {

        counterQrService.deactivateCounterQr(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> generateCounterQr(
            @PathVariable Long id) {

        CounterQr counterQr =
                counterQrService.getCounterQrById(id);

        byte[] qrImage =
                qrCodeService.generateCounterQr(
                        counterQr.getQrToken()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"COUNTER-QR.png\""
                )
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateCounterQr(
            @PathVariable Long id) {

        counterQrService.activateCounterQr(id);

        return ResponseEntity.noContent().build();
    }
}