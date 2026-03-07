package com.alex.bank.account.api.rest.controller;

/*import com.alex.bank.account.dto.AccountDto;
import com.alex.bank.account.dto.AccountEditDto;*/
import com.alex.bank.common.dto.account.AccountEditDto;
import com.alex.bank.common.dto.account.AccountDto;
//import com.alex.bank.account.dto.MoneyOperationRequest;
import com.alex.bank.common.dto.account.MoneyOperationRequest;
import com.alex.bank.account.security.SecurityUtil;
import com.alex.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RequestMapping("api/v1/accounts")
@RequiredArgsConstructor
@RestController
public class AccountController {

    public final AccountService accountService;

    @GetMapping(value = "/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccountDto> getAuthAccount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.getAccountByUsername(SecurityUtil.getCurrentUsernameFromJwtToken(jwt)));
    }

    @PreAuthorize("hasRole('USER') and hasAuthority('ACCOUNTS_WRITE')")
    @GetMapping(params = "excludeCurrent")
    public ResponseEntity<List<AccountDto>> getAccountsExcludeAuth(@AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.getAccountsExcludeOwner(SecurityUtil.getCurrentUsernameFromJwtToken(jwt)));
    }

    @PutMapping
    @PreAuthorize("hasRole('USER') and hasAuthority('ACCOUNTS_WRITE')")
    public ResponseEntity<AccountDto> updateUserDataAccount(@AuthenticationPrincipal Jwt jwt,
                                                            @RequestBody AccountEditDto accountEditDto) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.updateAccount(accountEditDto, SecurityUtil.getCurrentUsernameFromJwtToken(jwt)));
    }

    @PreAuthorize("hasRole('SERVICE') and hasAuthority('ACCOUNTS_WRITE')")
    @PatchMapping("/{owner}/balance/increase")
    public ResponseEntity<BigDecimal> increaseMoneyToBalanceAccount(@PathVariable String owner,
                                                                                @Validated @RequestBody MoneyOperationRequest moneyReq){
        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.increaseBalance(owner,moneyReq.amount()));
    }
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('ACCOUNTS_WRITE')")
    @PatchMapping("/{owner}/balance/decrease")
    public ResponseEntity<BigDecimal> decreaseMoneyFromBalanceAccount(@PathVariable String owner,
                                                                                  @Validated @RequestBody MoneyOperationRequest moneyReq){
       return ResponseEntity.status(HttpStatus.OK)
               .body(accountService.decreaseBalance(owner,moneyReq.amount()));
    }


}
