package com.cafe.backend.service.admin;

import com.cafe.backend.dto.admin.*;

import java.time.LocalDate;
import java.util.List;

public interface AdminSalesService {

    AdminSalesSummaryResponse getSalesSummary();

    List<DailySalesResponse> getDailySales(
            LocalDate from,
            LocalDate to
    );

    List<TopSellingItemResponse> getTopSellingItems(
            LocalDate from,
            LocalDate to
    );

    List<DietarySalesResponse> getDietarySales(
            LocalDate from,
            LocalDate to
    );

    List<OrderTypeSalesResponse> getOrderTypeSales(
            LocalDate from,
            LocalDate to
    );
}