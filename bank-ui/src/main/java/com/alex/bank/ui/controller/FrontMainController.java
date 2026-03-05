package com.alex.bank.ui.controller;


import com.alex.bank.ui.client.AccountServiceClient;
import com.alex.bank.ui.client.CashServiceClient;
import com.alex.bank.ui.dto.ApiResult;
import com.alex.bank.ui.dto.account.AccountDto;
import com.alex.bank.ui.dto.account.AccountEditDto;
import com.alex.bank.ui.dto.cash.CashAction;
import com.alex.bank.ui.dto.cash.CashRequest;
import com.alex.bank.ui.dto.cash.CashResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class FrontMainController {

    private final AccountServiceClient accountServiceClient;
    private final CashServiceClient cashServiceClient;

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
    public String cash() {

        //TODO

        return "redirect:/account";
    }

    private AccountDto createEmptyPayload() {
        return new AccountDto("", null, BigDecimal.ZERO, "");
    }


}
