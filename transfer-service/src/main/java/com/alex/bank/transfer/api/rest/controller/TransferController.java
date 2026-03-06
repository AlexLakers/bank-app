package com.alex.bank.transfer.api.rest.controller;


import com.alex.bank.transfer.dto.TransferRequest;
import com.alex.bank.transfer.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.alex.bank.transfer.service.TransferService;

@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/api/v1/transfer")
    public TransferResponse transfer(@Validated @RequestBody TransferRequest transferRequest) {
        return transferService.transfer(transferRequest);
    }
}
