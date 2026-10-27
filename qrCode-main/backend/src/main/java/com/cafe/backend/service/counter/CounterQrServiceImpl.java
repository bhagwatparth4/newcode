package com.cafe.backend.service.counter;

import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.entity.CounterQr;
import com.cafe.backend.repository.CounterQrRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CounterQrServiceImpl implements CounterQrService {

    private final CounterQrRepository counterQrRepository;

    public CounterQrServiceImpl(CounterQrRepository counterQrRepository) {
        this.counterQrRepository = counterQrRepository;
    }

    @Override
    public CounterQr createCounterQr() {

        CounterQr counterQr = new CounterQr();

        counterQr.setQrToken(UUID.randomUUID().toString());
        counterQr.setActive(true);

        return counterQrRepository.save(counterQr);
    }

    @Override
    public CounterQr getActiveCounterQr() {

        return counterQrRepository.findAll()
                .stream()
                .filter(CounterQr::isActive)
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException("COUNTER_QR_NOT_FOUND", "Active counter QR not found"));
    }

    @Override
    public void deactivateCounterQr(Long id) {

        CounterQr counterQr = counterQrRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("COUNTER_QR_NOT_FOUND", "Counter QR not found"));

        counterQr.setActive(false);

        counterQrRepository.save(counterQr);
    }

    @Override
    public CounterQr getCounterQrById(Long id) {

        return counterQrRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("COUNTER_QR_NOT_FOUND", "Counter QR not found"));
    }

    @Override
    public void activateCounterQr(Long id) {

        CounterQr counterQr =
                counterQrRepository.findById(id)
                        .orElseThrow(() ->
                                new NotFoundException("COUNTER_QR_NOT_FOUND", "Counter QR not found"));

        counterQr.setActive(true);

        counterQrRepository.save(counterQr);
    }
}