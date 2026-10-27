package com.cafe.backend.service.qr;

import com.cafe.backend.dto.qr.QrContextResponse;
import com.cafe.backend.entity.CafeTable;
import com.cafe.backend.entity.CounterQr;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.CafeTableRepository;
import com.cafe.backend.repository.CounterQrRepository;
import org.springframework.stereotype.Service;

@Service
public class QrContextServiceImpl implements QrContextService {

    private final CafeTableRepository cafeTableRepository;
    private final CounterQrRepository counterQrRepository;

    public QrContextServiceImpl(
            CafeTableRepository cafeTableRepository,
            CounterQrRepository counterQrRepository) {

        this.cafeTableRepository = cafeTableRepository;
        this.counterQrRepository = counterQrRepository;
    }

    @Override
    public QrContextResponse resolveQrToken(String qrToken) {

        // First check if the QR belongs to a cafe table
        CafeTable cafeTable = cafeTableRepository
                .findByQrToken(qrToken)
                .orElse(null);

        if (cafeTable != null) {

            QrContextResponse response = new QrContextResponse();

            response.setOrderType(OrderType.TABLE);
            response.setTableNumber(cafeTable.getTableNumber());
            response.setValid(cafeTable.isActive());

            return response;
        }

        // If not a table QR, check if it is the counter QR
        CounterQr counterQr = counterQrRepository
                .findByQrToken(qrToken)
                .orElse(null);

        if (counterQr != null) {

            QrContextResponse response = new QrContextResponse();

            response.setOrderType(OrderType.WALK_IN);
            response.setTableNumber(null);
            response.setValid(counterQr.isActive());

            return response;
        }

        // Token does not belong to any registered QR
        throw new NotFoundException("INVALID_QR_TOKEN", "This QR code is invalid or no longer active.");
    }
}