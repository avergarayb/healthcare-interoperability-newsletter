package com.healthcare.interoperability.fhirclient.dto;

import java.util.List;

public record HumanNameResponse(String family, List<String> given) {
}
