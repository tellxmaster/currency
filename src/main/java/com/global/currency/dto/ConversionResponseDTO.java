package com.global.currency.dto;

import java.math.BigDecimal;

public class ConversionResponseDTO {
    private BigDecimal convertedAmount;

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public void setConvertedAmount(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }
}
