
package com.money.draft.controller;

import com.money.draft.dto.TransferRequest;
import com.money.draft.dto.TransferResponse;
import com.money.draft.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transfer (internal)", description = "Internal transfer endpoint using full DTO (idempotency required)")
@RestController
@RequestMapping("/api/v1/transfers")
@SecurityRequirement(name = "BearerAuth")
public class TransferController {

    private final TransferService transferService;
    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @Operation(summary = "Execute a transfer with an explicit idempotency key")
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.ok(response);
    }
}
