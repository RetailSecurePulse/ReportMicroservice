package com.retailpulse.dto;

import java.time.Instant;

public record TimeSearchFilterRequestDto(Instant startDateTime, Instant endDateTime) {
}
