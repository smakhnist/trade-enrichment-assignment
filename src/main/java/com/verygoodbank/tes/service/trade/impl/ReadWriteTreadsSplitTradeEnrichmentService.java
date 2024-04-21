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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadWriteTreadsSplitTradeEnrichmentService implements TradeEnrichmentService {
    private final ProductService productService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final ExecutorService consumerExecutorPool = Executors.newCachedThreadPool();
    private final ExecutorService producerExecutorPool = Executors.newCachedThreadPool();

    @Override
    public void enrichTrades(InputStream tradeInputStream, PrintWriter printWriter) {
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        Future<?> consumerThread = consumerExecutorPool.submit(new DataConsumer(queue, tradeInputStream, printWriter, this::processLine));
        Future<?> producerThread = producerExecutorPool.submit(new DataProducer(queue, printWriter, consumerThread::isDone));
        try {
            producerThread.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private static class DataConsumer implements Runnable {
        private final Queue<String> queue;
        private final InputStream inputStream;
        private final PrintWriter printWriter;
        private final Function<String, String> processLine;

        public DataConsumer(Queue<String> queue, InputStream inputStream, PrintWriter printWriter, Function<String, String> processLine) {
            this.queue = queue;
            this.inputStream = inputStream;
            this.printWriter = printWriter;
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
                        printWriter.println(line);
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
            while (!consumerDoneChecker.get() || !queue.isEmpty()) {
                String line = queue.poll();
                if (line != null) {
                    writer.println(line);
                } else {
                    Thread.yield();
                }
            }
        }
    }
}
