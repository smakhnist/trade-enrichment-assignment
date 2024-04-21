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

@Service
@RequiredArgsConstructor
@Slf4j
public class NaiveTradeEnrichmentService implements TradeEnrichmentService {
    private final ProductService productService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd"); // DateTimeFormatter is thread-safe

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
            String[] columns = line.split(",");
            String productId = columns[1];
            String productName = Optional.ofNullable(productService.getProductName(productId)).orElse("Missing Product Name");

            String dateAsString = columns[0];
            if (!isValidDate(dateAsString)) {
                return null;
            }
            return String.join(",", columns[0], productName, columns[2], columns[3]);
        } catch (Exception e) {
            log.error("Error processing line: {}", line, e);
            return null;
        }
    }

    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, dateFormatter);
            return true;
        } catch (DateTimeParseException e) {
            log.error("Invalid date: {}", dateStr);
            return false;
        }
    }
}
