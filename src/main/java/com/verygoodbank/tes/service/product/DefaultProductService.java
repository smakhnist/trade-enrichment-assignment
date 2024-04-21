package com.verygoodbank.tes.service.product;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ProductService}.
 * Products are stored in HashMap ensuring O(1) access time,
 * and String keys are used to ensure the best performance, not requiring Integer conversion on lines parse logic.
 */
@Service
public class DefaultProductService implements ProductService {
    private final Map<String, String> products;

    public DefaultProductService() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(DefaultProductService.class.getClassLoader().getResourceAsStream("product.csv"))
        ))) {
            products = bufferedReader.lines()
                    .map(line -> line.split(","))
                    .skip(1)
                    .collect(Collectors.toMap(
                            columns -> columns[0],
                            columns -> columns[1]
                    ));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getProductName(String productId) {
        return products.get(productId);
    }
}
