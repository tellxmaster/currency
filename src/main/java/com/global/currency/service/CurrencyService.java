package com.global.currency.service;

import com.global.currency.model.ExchangeRate;
import com.global.currency.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.global.currency.util.Utilities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CurrencyService {
    private final WebClient webClient;
    private final String apiKey;
    private final ExchangeRateCache exchangeRateCache;
    private final String commonSymbols;

    private final Utilities utilities = new Utilities();

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @PostConstruct
    public void init() {
        // Preload common exchange rates into the cache at application startup
        getCommonExchangeRates().subscribe(); // It subscribes to trigger the operation, but does not block.
    }

    public CurrencyService(WebClient.Builder webClientBuilder, @Value("${openexchangerates.api.key}") String apiKey, @Value("${openexchangerates.api.baseUrl}") String baseUrl, @Value("${currency.symbols.common}") String commonSymbols, ExchangeRateCache exchangeRateCache) {
        this.apiKey = apiKey;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.commonSymbols = commonSymbols;
        this.exchangeRateCache = exchangeRateCache;
    }


    public Mono<String> getCommonExchangeRates() {
        LocalDate today = LocalDate.now();
        List<ExchangeRate> cachedRates = exchangeRateRepository.findBySourceCurrencyAndEffectiveDate("USD", today);

        if (!cachedRates.isEmpty()) {
            System.out.println("[ Using cached rates from database ]");
            cachedRates.forEach(rate -> {
                String cacheKey = exchangeRateCache.generateCacheKey(rate);
                exchangeRateCache.addExchangeRate(cacheKey, rate);
            });
            return Mono.just(cachedRates.toString());
        } else {
            return fetchAndStoreRates();
        }
    }
    public Mono<String> fetchAndStoreRates() {
        System.out.println("[ Getting Common rates from API ]");
        String url = String.format("latest.json?app_id=%s&symbols=%s", apiKey, commonSymbols);
        return this.webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(rateJson -> {
                    System.out.println("[ API Response ] " + rateJson);
                    List<ExchangeRate> rates = utilities.parseJsonToExchangeRates(rateJson);
                    rates.forEach(rate -> {
                        String cacheKey = exchangeRateCache.generateCacheKey(rate);
                        exchangeRateCache.addExchangeRate(cacheKey, rate);
                        exchangeRateRepository.save(rate);
                    });
                    return rateJson;
                });
    }

    public Mono<ExchangeRate> fetchExchangeRate(String source, String target) {
        if (source.equals(target)) {
            // No conversion needed if source and target are the same
            return Mono.just(new ExchangeRate(source, target, BigDecimal.ONE, LocalDate.now()));
        }

        LocalDate today = LocalDate.now();

        // Attempts to obtain the cache rate of change
        String cacheKey = source + "-" + target + "-" + today;
        ExchangeRate cachedRate = exchangeRateCache.getExchangeRate(cacheKey);

        if (cachedRate != null) {
            return Mono.just(cachedRate);
        }

        // If not in the cache, search the database
        Optional<ExchangeRate> dbRateOptional = exchangeRateRepository.findBySourceCurrencyAndTargetCurrencyAndEffectiveDate(source, target, today);

        if (dbRateOptional.isPresent()) {
            ExchangeRate dbRate = dbRateOptional.get();
            exchangeRateCache.addExchangeRate(cacheKey, dbRate);
            return Mono.just(dbRate);
        }

        if (!"USD".equals(target)) {
            String url = String.format("latest.json?app_id=%s&symbols=%s", apiKey, target);
            return this.webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(rateJson -> {
                        System.out.println("Server get");
                        System.out.println(rateJson);
                        List<ExchangeRate> rates = utilities.parseJsonToExchangeRates(rateJson);
                        if (!rates.isEmpty()) {
                            ExchangeRate rate = rates.stream().findFirst().orElse(null);
                            exchangeRateRepository.save(rate); // Almacena en la base de datos
                            exchangeRateCache.addExchangeRate(cacheKey, rate); // Almacena en caché
                            System.out.println(rate.toString());
                            return Mono.just(rate);
                        }
                        return Mono.error(new RuntimeException("No rate found"));
                    });
        } else {
            // If the target is USD, then check if there is an inverse rate for the source
            String inverseCacheKey = target + "-" + source+ "-" + today;
            ExchangeRate inverseCachedRate = exchangeRateCache.getExchangeRate(inverseCacheKey);

            if (inverseCachedRate != null) {
                // Invert the rate to get the source rate to USD
                BigDecimal invertedRate = BigDecimal.ONE.divide(inverseCachedRate.getRate(), 6, RoundingMode.HALF_UP);
                ExchangeRate invertedExchangeRate = new ExchangeRate(source, target, invertedRate, today);
                return Mono.just(invertedExchangeRate);
            } else {
                // Invert the rate to get the source rate to USD
                return Mono.error(new RuntimeException("No inverse rate found for " + source + " to USD"));
            }
        }
    }
}
