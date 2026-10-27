package com.cafe.backend.controller.table;

import com.cafe.backend.dto.table.TableRequest;
import com.cafe.backend.dto.table.TableResponse;
import com.cafe.backend.service.qr.QrCodeService;
import com.cafe.backend.service.table.TableService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tables")
public class TableController {

    private final QrCodeService qrCodeService;
    private final TableService tableService;

    public TableController(QrCodeService qrCodeService, TableService tableService) {
        this.qrCodeService = qrCodeService;
        this.tableService = tableService;
    }

    @PostMapping
    public ResponseEntity<TableResponse> createTable(
            @RequestBody TableRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tableService.createTable(request));
    }

    @GetMapping
    public ResponseEntity<List<TableResponse>> getAllTables() {

        return ResponseEntity.ok(tableService.getAllTables());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableResponse> getTableById(
            @PathVariable Long id) {

        return ResponseEntity.ok(tableService.getTableById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TableResponse> updateTable(
            @PathVariable Long id,
            @RequestBody TableRequest request) {

        return ResponseEntity.ok(
                tableService.updateTable(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(
            @PathVariable Long id) {

        tableService.deleteTable(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> generateTableQr(
            @PathVariable Long id) {

        TableResponse table =
                tableService.getTableById(id);

        byte[] qrImage =
                qrCodeService.generateTableQr(
                        table.getQrToken()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + table.getTableNumber()
                                + "-QR.png\""
                )
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }
}