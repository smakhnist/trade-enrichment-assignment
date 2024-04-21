package com.verygoodbank.tes.service.product;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DefaultProductService implements ProductService {
    private final Map<String, String> products; // yes, not Map<Integer, String> for efficiency

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
