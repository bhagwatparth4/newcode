package com.cafe.backend.service.table;

import org.springframework.dao.DataIntegrityViolationException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.dto.table.TableRequest;
import com.cafe.backend.dto.table.TableResponse;
import com.cafe.backend.entity.CafeTable;
import com.cafe.backend.repository.CafeTableRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TableServiceImpl implements TableService {

    private final CafeTableRepository cafeTableRepository;

    public TableServiceImpl(CafeTableRepository cafeTableRepository) {
        this.cafeTableRepository = cafeTableRepository;
    }

    @Override
    public TableResponse createTable(TableRequest request) {

        String tableNumber = validTableNumber(request);

        CafeTable cafeTable = new CafeTable();

        cafeTable.setTableNumber(tableNumber);
        cafeTable.setQrToken(UUID.randomUUID().toString());
        cafeTable.setActive(request.isActive());

        CafeTable savedTable = saveUnique(cafeTable);

        return mapToResponse(savedTable);
    }

    @Override
    public TableResponse getTableById(Long id) {

        CafeTable cafeTable = cafeTableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TABLE_NOT_FOUND", "Table not found"));

        return mapToResponse(cafeTable);
    }

    @Override
    public List<TableResponse> getAllTables() {

        return cafeTableRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public TableResponse updateTable(Long id, TableRequest request) {

        CafeTable cafeTable = cafeTableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TABLE_NOT_FOUND", "Table not found"));

        cafeTable.setTableNumber(validTableNumber(request));
        cafeTable.setActive(request.isActive());

        // QR token is intentionally NOT regenerated during update.
        // Existing printed QR must continue to work.

        CafeTable updatedTable = saveUnique(cafeTable);

        return mapToResponse(updatedTable);
    }

    @Override
    public void deleteTable(Long id) {

        CafeTable cafeTable = cafeTableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TABLE_NOT_FOUND", "Table not found"));

        try {
            cafeTableRepository.delete(cafeTable);
            cafeTableRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "TABLE_IN_USE",
                    "This table has past orders, so it cannot be deleted. "
                            + "Deactivate it instead."
            );
        }
    }

    private String validTableNumber(TableRequest request) {
        if (request == null || request.getTableNumber() == null
                || request.getTableNumber().isBlank()) {
            throw new BadRequestException("TABLE_NUMBER_REQUIRED", "Table number is required");
        }
        String value = request.getTableNumber().trim().toUpperCase();
        if (value.length() > 20) {
            throw new BadRequestException("TABLE_NUMBER_TOO_LONG", "Table number is too long");
        }
        return value;
    }

    private CafeTable saveUnique(CafeTable cafeTable) {
        try {
            return cafeTableRepository.saveAndFlush(cafeTable);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("TABLE_EXISTS", "A table with this number already exists");
        }
    }

    private TableResponse mapToResponse(CafeTable cafeTable) {

        TableResponse response = new TableResponse();

        response.setId(cafeTable.getId());
        response.setTableNumber(cafeTable.getTableNumber());
        response.setQrToken(cafeTable.getQrToken());
        response.setActive(cafeTable.isActive());
        response.setCreatedAt(cafeTable.getCreatedAt());
        response.setUpdatedAt(cafeTable.getUpdatedAt());

        return response;
    }
}