package com.kiwi.engine.exception;

import java.math.BigDecimal;

public class InsufficientCreditException extends RuntimeException {
    public InsufficientCreditException(BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient credit. Requested: ₹%s, Available: ₹%s",
                requested, available));
    }
}
