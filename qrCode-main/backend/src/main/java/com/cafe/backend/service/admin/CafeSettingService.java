package com.cafe.backend.service.admin;

import com.cafe.backend.enums.BusinessType;

public interface CafeSettingService {

    Integer getDefaultPreparationMinutes();

    Integer updateDefaultPreparationMinutes(Integer minutes);

    BusinessType getBusinessType();

    boolean isAcceptingOrders();

    boolean setAcceptingOrders(boolean accepting);

    boolean isCashEnabled();
    boolean setCashEnabled(boolean enabled);

}