package com.fps.svmes.utils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DispatchRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDispatchRequest {
    String message() default "Invalid Dispatch Request for the specified schedule type";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
