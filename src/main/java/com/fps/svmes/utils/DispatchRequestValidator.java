package com.fps.svmes.utils;

import com.fps.svmes.dto.requests.DispatchRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DispatchRequestValidator implements ConstraintValidator<ValidDispatchRequest, DispatchRequest> {

    @Override
    public boolean isValid(DispatchRequest request, ConstraintValidatorContext context) {
        if (request == null) return false;

        boolean valid = true;

        if (request.getScheduleType() == DispatchRequest.ScheduleType.INTERVAL) {
            // Validate fields specific to INTERVAL
            if (request.getStartTime() == null) {
                context.buildConstraintViolationWithTemplate("Start time must be provided for INTERVAL schedule")
                        .addPropertyNode("startTime").addConstraintViolation();
                valid = false;
            }
            if (request.getIntervalMinutes() == null || request.getRepeatCount() == null) {
                context.buildConstraintViolationWithTemplate("Interval minutes and repeat count are required for INTERVAL schedule")
                        .addConstraintViolation();
                valid = false;
            }
        } else if (request.getScheduleType() == DispatchRequest.ScheduleType.SPECIFIC_DAYS) {
            // Validate fields specific to SPECIFIC_DAYS
            if (request.getTimeOfDay() == null || request.getTimeOfDay().isBlank()) {
                context.buildConstraintViolationWithTemplate("Time of day must be provided for SPECIFIC_DAYS schedule")
                        .addPropertyNode("timeOfDay").addConstraintViolation();
                valid = false;
            }
            if (request.getSpecificDays() == null || request.getSpecificDays().isEmpty()) {
                context.buildConstraintViolationWithTemplate("Specific days must be provided for SPECIFIC_DAYS schedule")
                        .addPropertyNode("specificDays").addConstraintViolation();
                valid = false;
            }
        } else {
            context.buildConstraintViolationWithTemplate("Invalid schedule type")
                    .addPropertyNode("scheduleType").addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
