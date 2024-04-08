package com.global.currency.util;

public enum CurrencyCode {
    USD("United States Dollar"),
    EUR("Euro"),
    GBP("British Pound Sterling"),
    JPY("Japanese Yen"),
    CAD("Canadian Dollar"),
    AUD("Australian Dollar"),
    CHF("Swiss Franc"),
    MXN("Mexican Peso"),
    COP("Colombian Peso");

    private final String description;

    CurrencyCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
