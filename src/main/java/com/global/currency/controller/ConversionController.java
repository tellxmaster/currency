package com.global.currency.controller;

import com.global.currency.dto.ConversionRequestDTO;
import com.global.currency.dto.ConversionResponseDTO;
import com.global.currency.service.ConversionService;
import com.global.currency.service.CurrencyService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestController
@RequestMapping("/api/currency")
public class ConversionController {

    private final CurrencyService currencyService;
    private final ConversionService conversionService;

    public ConversionController(CurrencyService currencyService, ConversionService conversionService) {
        this.currencyService = currencyService;
        this.conversionService = conversionService;

    }


    @PostMapping("/convert")
    public Mono<ResponseEntity<ConversionResponseDTO>> convertCurrency(@RequestBody ConversionRequestDTO conversionRequestDTO) {
        return conversionService.convert(conversionRequestDTO.getSourceCurrency(), conversionRequestDTO.getTargetCurrency(), conversionRequestDTO.getAmount())
                .map(convertedAmount -> {
                    ConversionResponseDTO responseDTO = new ConversionResponseDTO();
                    responseDTO.setConvertedAmount(convertedAmount);
                    return ResponseEntity.ok(responseDTO);
                });
    }


}
