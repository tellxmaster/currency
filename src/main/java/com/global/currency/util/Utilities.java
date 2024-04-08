package com.global.currency.util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.currency.model.ExchangeRate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
@Component
public class Utilities {
    public List<ExchangeRate> parseJsonToExchangeRates(String rateJson) {
        List<ExchangeRate> rates = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(rateJson);
            JsonNode ratesNode = root.path("rates");
            String baseCurrency = root.path("base").asText();
            long timestamp = root.path("timestamp").asLong();
            LocalDate effectiveDate = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();

            Iterator<String> currencyCodes = ratesNode.fieldNames();
            while (currencyCodes.hasNext()) {
                String targetCurrency = currencyCodes.next();
                BigDecimal rate = ratesNode.get(targetCurrency).decimalValue();
                rates.add(new ExchangeRate(baseCurrency, targetCurrency, rate, effectiveDate));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rates;
    }

}
