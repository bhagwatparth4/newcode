package com.cafe.backend.service.counter;

import com.cafe.backend.entity.CounterQr;

public interface CounterQrService {

    CounterQr createCounterQr();

    CounterQr getActiveCounterQr();

    void deactivateCounterQr(Long id);

    CounterQr getCounterQrById(Long id);

    void activateCounterQr(Long id);

}