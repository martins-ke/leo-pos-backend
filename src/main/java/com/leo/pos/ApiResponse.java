package com.leo.pos;

import java.time.Instant;

public record ApiResponse<T>(T body, String message, boolean success, Instant timeStamp) {}
