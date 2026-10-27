package com.cafe.backend.service.table;

import com.cafe.backend.dto.table.TableRequest;
import com.cafe.backend.dto.table.TableResponse;

import java.util.List;

public interface TableService {

    TableResponse createTable(TableRequest request);

    TableResponse getTableById(Long id);

    List<TableResponse> getAllTables();

    TableResponse updateTable(Long id, TableRequest request);

    void deleteTable(Long id);
}