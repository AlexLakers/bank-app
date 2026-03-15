package com.alex.bank.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class AccountAgeValidator implements ConstraintValidator<ValidAge, LocalDate> {
    @Override
    public boolean isValid(LocalDate localDate, ConstraintValidatorContext constraintValidatorContext) {
        return Period.between(localDate, LocalDate.now()).getYears() >= 18;
    }
}
