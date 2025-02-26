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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirtualThreadsSplitTradeEnrichmentService implements TradeEnrichmentService {
    private final ProductService productService;
    private final ThreadLocal<DateTimeFormatter> dateFormatterTH = new ThreadLocal<>();
    private final ExecutorService consumerExecutorPool = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService producerExecutorPool = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentHashMap.KeySetView<String, Boolean> dateValidationCache = ConcurrentHashMap.newKeySet();  // it's thread-safe

    @Override
    public void enrichTrades(InputStream tradeInputStream, PrintWriter printWriter) {
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        Future<?> consumerThread = consumerExecutorPool.submit(new VirtualThreadsSplitTradeEnrichmentService.DataConsumer(queue, tradeInputStream, this::processLine));
        Future<?> producerThread = producerExecutorPool.submit(new VirtualThreadsSplitTradeEnrichmentService.DataProducer(queue, printWriter, consumerThread::isDone));
        try {
            producerThread.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                Optional.ofNullable(dateFormatterTH.get()).orElseGet(() -> {
                    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyyMMdd");
                    dateFormatterTH.set(formatter1);
                    return formatter1;
                }).parse(dateStr);
                dateValidationCache.add(dateStr);
                return true;
            } catch (DateTimeParseException e) {
                log.error("Invalid date format: {}", dateStr);
                return false;
            }
        }
    }

    private static class DataConsumer implements Runnable {
        private final Queue<String> queue;
        private final InputStream inputStream;
        private final Function<String, String> processLine;

        public DataConsumer(Queue<String> queue, InputStream inputStream, Function<String, String> processLine) {
            this.queue = queue;
            this.inputStream = inputStream;
            this.processLine = processLine;
        }

        @Override
        public void run() {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = bufferedReader.readLine()) != null) {
                    if (header) {
                        header = false;
                        queue.add(line);
                        continue;
                    }
                    String processedLine = processLine.apply(line);
                    if (processedLine != null) {
                        queue.add(processedLine);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static class DataProducer implements Runnable {
        private final Queue<String> queue;
        private final PrintWriter writer;
        private final Supplier<Boolean> consumerDoneChecker;

        public DataProducer(Queue<String> queue, PrintWriter printWriter, Supplier<Boolean> consumerDoneChecker) {
            this.queue = queue;
            this.writer = printWriter;
            this.consumerDoneChecker = consumerDoneChecker;
        }

        @Override
        public void run() {
            while (!consumerDoneChecker.get()) {
                String line = queue.poll();
                if (line != null) {
                    writer.println(line);
                }
            }
        }
    }
}
