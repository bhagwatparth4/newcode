package com.cafe.backend.service.qr;

import com.cafe.backend.dto.qr.QrContextResponse;

public interface QrContextService {

    QrContextResponse resolveQrToken(String qrToken);
}