package com.alex.bank.account.mapper;

import com.alex.bank.common.dto.account.AccountEditDto;
import com.alex.bank.common.dto.account.AccountDto;
//import com.alex.bank.account.dto.AccountDto;
//import com.alex.bank.account.dto.AccountEditDto;
import com.alex.bank.account.model.Account;
import org.mapstruct.*;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {

    AccountDto toDto(Account account);

    @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
    void updateAccount(AccountEditDto accountEditDto, @MappingTarget Account account);
}
