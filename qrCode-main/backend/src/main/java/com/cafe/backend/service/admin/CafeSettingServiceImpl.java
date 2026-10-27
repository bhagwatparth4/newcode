package com.cafe.backend.service.admin;

import com.cafe.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import com.cafe.backend.entity.CafeSetting;
import com.cafe.backend.repository.CafeSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cafe.backend.enums.BusinessType;

@Service
public class CafeSettingServiceImpl implements CafeSettingService {

    private final CafeSettingRepository cafeSettingRepository;

    /*
     * Business type is a VENDOR configuration (set per client deployment),
     * not something the cafe owner can change from the dashboard.
     *
     * Set it with the BUSINESS_TYPE environment variable:
     *   SMALL_CAFE  -> Admin dashboard only (no Kitchen accounts)
     *   LARGE_CAFE  -> Admin + Kitchen dashboards
     *   RESTAURANT  -> Admin + Kitchen dashboards
     */
    private final BusinessType businessType;

    public CafeSettingServiceImpl(
            CafeSettingRepository cafeSettingRepository,
            @Value("${app.business-type:SMALL_CAFE}") String configuredBusinessType
    ) {
        this.cafeSettingRepository = cafeSettingRepository;

        try {
            this.businessType = BusinessType.valueOf(
                    configuredBusinessType.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            // Fail at startup with a clear message instead of misbehaving later
            throw new IllegalStateException(
                    "Invalid BUSINESS_TYPE '" + configuredBusinessType
                            + "'. Use SMALL_CAFE, LARGE_CAFE or RESTAURANT."
            );
        }
    }

    @Override
    @Transactional
    public Integer getDefaultPreparationMinutes() {

        CafeSetting setting = cafeSettingRepository.findById(1L)
                .orElseGet(() -> {
                    CafeSetting newSetting = new CafeSetting();
                    newSetting.setDefaultPreparationMinutes(20);
                    return cafeSettingRepository.save(newSetting);
                });

        return setting.getDefaultPreparationMinutes();
    }

    @Override
    @Transactional
    public Integer updateDefaultPreparationMinutes(Integer minutes) {

        if (minutes == null || minutes <= 0 || minutes > 180) {
            throw new BadRequestException(
                    "INVALID_PREPARATION_TIME",
                    "Preparation time must be between 1 and 180 minutes"
            );
        }

        CafeSetting setting = cafeSettingRepository.findById(1L)
                .orElseGet(() -> {
                    CafeSetting newSetting = new CafeSetting();
                    newSetting.setDefaultPreparationMinutes(20);
                    return newSetting;
                });

        setting.setDefaultPreparationMinutes(minutes);

        return cafeSettingRepository.save(setting)
                .getDefaultPreparationMinutes();
    }

    @Override
    public BusinessType getBusinessType() {
        return businessType;
    }

    @Override
    public boolean isAcceptingOrders() {
        return cafeSettingRepository.findById(1L)
                .map(setting -> !Boolean.FALSE.equals(setting.getAcceptingOrders()))
                .orElse(true);
    }

    @Override
    @Transactional
    public boolean setAcceptingOrders(boolean accepting) {
        CafeSetting setting = cafeSettingRepository.findById(1L)
                .orElseGet(() -> {
                    CafeSetting newSetting = new CafeSetting();
                    newSetting.setDefaultPreparationMinutes(20);
                    return newSetting;
                });
        setting.setAcceptingOrders(accepting);
        cafeSettingRepository.save(setting);
        return accepting;
    }


    @Override
    public boolean isCashEnabled() {
        return cafeSettingRepository.findById(1L)
                .map(setting -> !Boolean.FALSE.equals(setting.getCashEnabled()))
                .orElse(true);
    }

    @Override
    @Transactional
    public boolean setCashEnabled(boolean enabled) {
        CafeSetting setting = cafeSettingRepository.findById(1L)
                .orElseGet(() -> {
                    CafeSetting newSetting = new CafeSetting();
                    newSetting.setDefaultPreparationMinutes(20);
                    return newSetting;
                });
        setting.setCashEnabled(enabled);
        cafeSettingRepository.save(setting);
        return enabled;
    }


}