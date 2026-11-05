package com.healthcare.interoperability.eventconsumer.fhir;

import java.math.BigDecimal;

public record FhirQuantity(BigDecimal value, String unit) {
}
