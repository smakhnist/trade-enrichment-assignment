package com.verygoodbank.tes.service.trade.impl;

import com.verygoodbank.tes.service.product.ProductService;
import com.verygoodbank.tes.service.trade.TradeEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EfficientStructuresTradeEnrichmentService implements TradeEnrichmentService {
    private final ProductService productService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final ConcurrentHashMap.KeySetView<String, Boolean> dateValidationCache = ConcurrentHashMap.newKeySet();

    @Override
    public void enrichTrades(InputStream tradeInputStream, PrintWriter printWriter) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(tradeInputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (header) {
                    header = false;
                    printWriter.println(line);
                    continue;
                }

                String processedLine = processLine(line);
                if (processedLine != null) {
                    printWriter.println(processedLine);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String processLine(String line) {
        try {
            int firstCommaIdx = line.indexOf(',');
            int secondCommaIdx = line.indexOf(',', firstCommaIdx + 1);
            String productId = line.substring(firstCommaIdx + 1, secondCommaIdx);
            String productName = Optional.ofNullable(productService.getProductName(productId)).orElse("Missing Product Name");

            String dateAsString = line.substring(0, firstCommaIdx);
            if (!isValidDate(dateAsString)) {
                return null;
            }
            return String.join(",", dateAsString, productName, line.substring(secondCommaIdx + 1));
        } catch (Exception e) {
            log.error("Error processing line: {}", line, e);
            return null;
        }
    }

    private boolean isValidDate(String dateStr) {
        if (dateValidationCache.contains(dateStr)) {
            return true;
        } else {
            try {
                LocalDate.parse(dateStr, dateFormatter);
                dateValidationCache.add(dateStr);
                return true;
            } catch (DateTimeParseException e) {
                log.error("Invalid date format: {}", dateStr);
                return false;
            }
        }
    }
}
