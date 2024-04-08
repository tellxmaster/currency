package com.global.currency.service;

import com.global.currency.model.ExchangeRate;
import com.global.currency.repository.ExchangeRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ConversionService {
    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private ExchangeRateCache exchangeRateCache;

    public Mono<BigDecimal> convert(String source, String target, BigDecimal value) {
        if (source.equals(target)) {
            // No conversion needed if source and target are the same
            return Mono.just(value);
        }

        LocalDate today = LocalDate.now();
        String directCacheKey = source + "-" + target + "-" + today;
        String inverseCacheKey = target + "-" + source + "-" + today;

        // Attempt to get the rate of change directly from the cache.
        ExchangeRate directCachedRate = exchangeRateCache.getExchangeRate(directCacheKey);
        if (directCachedRate != null) {
            return Mono.just(value.multiply(directCachedRate.getRate()));
        }

        // Attempts to obtain the inverse rate of change of the cache.
        ExchangeRate inverseCachedRate = exchangeRateCache.getExchangeRate(inverseCacheKey);
        if (inverseCachedRate != null) {
            // Invert rate for conversion
            return Mono.just(value.divide(inverseCachedRate.getRate(), 6, BigDecimal.ROUND_HALF_UP));
        }

        // Si no está en el caché, verifica si es necesario una conversión directa o triangular
        if (!"USD".equals(source) && !"USD".equals(target)) {
            // Conversión triangular
            return convertUsingTriangularMethod(source, "USD", target, value);
        } else {
            // Conversión directa
            return currencyService.fetchExchangeRate(source, target)
                    .doOnSuccess(rate -> exchangeRateCache.addExchangeRate(directCacheKey, rate))
                    .map(rate -> value.multiply(rate.getRate()));
        }
    }

    private Mono<BigDecimal> convertUsingTriangularMethod(String source, String intermediate, String target, BigDecimal value) {
        // Step 1: Convert from source to USD
        return currencyService.fetchExchangeRate(source, intermediate)
                .flatMap(rateSourceToUSD -> {
                    BigDecimal amountInUSD = value.multiply(rateSourceToUSD.getRate());
                    // Step 2: Convert from USD to target
                    return currencyService.fetchExchangeRate(intermediate, target)
                            .map(rateUSDToTarget -> {
                                return amountInUSD.multiply(rateUSDToTarget.getRate());
                            });
                });
    }
}
