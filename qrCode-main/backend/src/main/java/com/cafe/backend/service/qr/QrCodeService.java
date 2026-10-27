package com.cafe.backend.service.qr;

public interface QrCodeService {

    byte[] generateTableQr(String qrToken);

    byte[] generateCounterQr(String qrToken);
}