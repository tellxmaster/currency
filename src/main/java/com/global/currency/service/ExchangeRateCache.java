package com.global.currency.service;

import com.global.currency.model.ExchangeRate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExchangeRateCache {
    private final ConcurrentHashMap<String, ExchangeRate> exchangeRates = new ConcurrentHashMap<>();

    public String generateCacheKey(ExchangeRate rate) {
        return rate.getSourceCurrency() + "-" + rate.getTargetCurrency() + "-" + rate.getEffectiveDate();
    }
    public void addExchangeRate(String key, ExchangeRate rate) {
        exchangeRates.put(key, rate);
    }

    public ExchangeRate getExchangeRate(String key) {
        ExchangeRate cachedRate = exchangeRates.get(key);
        if (cachedRate != null && !isExpired(cachedRate)) {
            return cachedRate;
        } else {
            exchangeRates.remove(key);
            return null;
        }
    }

    private boolean isExpired(ExchangeRate rate) {
        return rate.getEffectiveDate().plusWeeks(1).isBefore(LocalDate.now());
    }

    @Scheduled(fixedRate = 3600000) // Run cleaning every hour
    public void cleanUpExpiredEntries() {
        exchangeRates.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
}
