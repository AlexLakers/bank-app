package com.alex.bank.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AccountAgeValidator.class)
public @interface ValidAge {

    String message() default "Возраст должен быть не менее 18 лет";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
