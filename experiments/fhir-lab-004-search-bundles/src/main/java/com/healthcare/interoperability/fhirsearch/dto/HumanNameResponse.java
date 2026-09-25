package com.healthcare.interoperability.fhirsearch.dto;

import java.util.List;

public record HumanNameResponse(String family, List<String> given) {
}
