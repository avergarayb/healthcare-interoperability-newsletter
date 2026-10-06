package com.healthcare.interoperability.profiles.dto;

import java.util.List;

public record ValidationFailureResponse(boolean valid, List<ValidationError> errors) {
}
