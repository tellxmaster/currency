package com.global.currency.repository;

import com.global.currency.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate,Long>
{
    List<ExchangeRate> findBySourceCurrencyAndEffectiveDate(String sourceCurrency, LocalDate effectiveDate);
    Optional<ExchangeRate> findBySourceCurrencyAndTargetCurrencyAndEffectiveDate(String sourceCurrency, String targetCurrency, LocalDate effectiveDate);

}
