package com.alex.bank.cash.api.rest.controller;

import com.alex.bank.cash.dto.CashRequest;
import com.alex.bank.cash.dto.CashResponse;
import com.alex.bank.cash.service.CashService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {
    private final CashService cashService;

    @PreAuthorize("hasRole('USER') and hasAuthority('CASH_WRITE')")
    @PostMapping("/owner/operations")
    public CashResponse withdrawCash(@AuthenticationPrincipal Jwt jwt,
                                     @RequestBody CashRequest request) {
            String accountHolder=jwt.getClaimAsString("preferred_username");
           return cashService.processCash(accountHolder,request);
    }
}
