package com.alex.bank.ui.controller;


import com.alex.bank.ui.client.AccountServiceClient;
import com.alex.bank.ui.client.CashServiceClient;
import com.alex.bank.ui.client.TransferServiceClient;
//import com.alex.bank.ui.dto.ApiResult;
//import com.alex.bank.ui.dto.account.AccountDto;
//import com.alex.bank.ui.dto.account.AccountEditDto;
import com.alex.bank.common.dto.account.*;
//import com.alex.bank.ui.dto.cash.CashAction;
//import com.alex.bank.ui.dto.cash.CashRequest;
//import com.alex.bank.ui.dto.cash.CashResponse;
import com.alex.bank.common.dto.cash.*;
//import com.alex.bank.ui.dto.transfer.TransferRequest;
import com.alex.bank.common.dto.transfer.TransferRequest;
//import com.alex.bank.ui.dto.transfer.TransferRequestUI;
import com.alex.bank.common.dto.ui.*;
//import com.alex.bank.ui.dto.transfer.TransferResponse;
import com.alex.bank.common.dto.transfer.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class FrontMainController {

    private final AccountServiceClient accountServiceClient;
    private final CashServiceClient cashServiceClient;
    private final TransferServiceClient transferServiceClient;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @PostMapping("/account")
    public String updateAccount(@Validated @ModelAttribute AccountEditDto accountEditDto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("errors", errors);

            return "redirect:/account";
        }

        ApiResult<AccountDto> updatingResult = accountServiceClient.updateAuthAccount(accountEditDto);
        if (updatingResult.isSuccess()) {
            redirectAttributes.addFlashAttribute("info", "Данные успешно обновлены");
        } else {
            redirectAttributes.addFlashAttribute("errors", List.of(updatingResult.error()));
        }
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String showMainPage(Model model) {
        ApiResult<AccountDto> accountResult = accountServiceClient.getAuthAccount();
        ApiResult<List<AccountDto>> accountsResult = accountServiceClient.getAccountsExcludeAuth();

        Set<String> errors = new TreeSet<>();
        String info = null;

        if (accountResult.isSuccess()) {
            model.addAttribute("account", accountResult.payload());
            info = accountResult.info();
        } else {
            errors.add(accountResult.error());
            model.addAttribute("account", createEmptyPayload());
        }

        if (accountsResult.isSuccess()) {
            model.addAttribute("accounts", accountsResult.payload());
            if (info == null) info = accountsResult.info();
        } else {
            errors.add(accountsResult.error());
            model.addAttribute("accounts", Collections.emptyList());
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
        }
        if (info != null) {
            model.addAttribute("info", info);
        }

        return "main";
    }

    @PostMapping("/cash")
    public String cash(@Validated CashRequest request,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
        }

        ApiResult<CashResponse> withdrawResult = cashServiceClient.processCashOperation(request);


        if (withdrawResult.isSuccess()) {
            redirectAttributes.addFlashAttribute("info",
                    (request.action() == CashAction.GET)
                            ? "Снято %s".formatted(request.amount())
                            : "Положено %s".formatted(request.amount()));
        } else {
            redirectAttributes.addFlashAttribute("errors", List.of(withdrawResult.error()));
        }
        return "redirect:/account";
    }

    @PostMapping("/transfer")
    public String transfer(@AuthenticationPrincipal OidcUser oidcUser,
                           @Validated TransferRequestUI request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.toList());
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/account";
        }

        String fromAccount = oidcUser.getName();

        ApiResult<TransferResponse> transferResult = transferServiceClient.processTransferOperation(
                new TransferRequest(request.toAccount(), fromAccount, request.amount())
        );

        if (transferResult.isSuccess()) {
            redirectAttributes.addFlashAttribute("info",
                    "Успешно переведено %s руб клиенту %s"
                            .formatted(request.amount(), request.toAccount()));
        } else {
            redirectAttributes.addFlashAttribute("errors", List.of(transferResult.error()));
        }

        return "redirect:/account";
    }

    private AccountDto createEmptyPayload() {
        return new AccountDto("", null, BigDecimal.ZERO, "");
    }


}
