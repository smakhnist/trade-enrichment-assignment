package com.verygoodbank.tes.service.trade.impl;

import com.verygoodbank.tes.service.product.ProductService;
import com.verygoodbank.tes.service.trade.TradeEnrichmentService;
import lombok.extern.slf4j.Slf4j;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;


@Slf4j
public class ThreadsSplitTradeEnrichmentService implements TradeEnrichmentService {
    private final ProductService productService;
    private final ThreadLocal<DateTimeFormatter> dateFormatterTH = new ThreadLocal<>();
    private final ExecutorService consumerExecutorPool;
    private final ExecutorService producerExecutorPool;
    private final ConcurrentHashMap.KeySetView<String, Boolean> dateValidationCache = ConcurrentHashMap.newKeySet();  // it's thread-safe
    private final int bufferSize;

    public ThreadsSplitTradeEnrichmentService(ProductService productService,
                                              boolean isVirtual, int bufferSize) {
        this.productService = productService;
        this.consumerExecutorPool = isVirtual? Executors.newVirtualThreadPerTaskExecutor() : Executors.newCachedThreadPool();
        this.producerExecutorPool = isVirtual? Executors.newVirtualThreadPerTaskExecutor() : Executors.newCachedThreadPool();
        this.bufferSize = bufferSize;
    }

    @Override
    public void enrichTrades(InputStream tradeInputStream, PrintWriter printWriter) {
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        ReentrantLock lock = new ReentrantLock();
        AtomicBoolean consumerDone = new AtomicBoolean(false);
        consumerExecutorPool.execute(new ThreadsSplitTradeEnrichmentService.DataConsumer(queue, consumerDone, tradeInputStream, this::processLine, lock, bufferSize));
        Future<?> producerThread = producerExecutorPool.submit(new ThreadsSplitTradeEnrichmentService.DataProducer(queue, printWriter, consumerDone::get, lock));
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
        private final AtomicBoolean doneAtomic;
        private final InputStream inputStream;
        private final Function<String, String> processLine;
        private final ReentrantLock semaphore;
        private final int bufferSize;

        public DataConsumer(Queue<String> queue, AtomicBoolean doneAtomic, InputStream inputStream, Function<String, String> processLine, ReentrantLock semaphore,
        int bufferSize ) {
            this.queue = queue;
            this.doneAtomic = doneAtomic;
            this.inputStream = inputStream;
            this.processLine = processLine;
            this.semaphore = semaphore;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            long b = System.currentTimeMillis();
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                int count = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    if (header) {
                        header = false;
                        queue.add(line);
                        continue;
                    }
                    String processedLine = processLine.apply(line);
                    if (processedLine != null) {
                        if (++count % bufferSize == 0) {
                            unlockSafe();
                        }
                        queue.add(processedLine);
                    }
                }
                doneAtomic.set(true);
                unlockSafe();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            log.info("Consumer done in {} ms", System.currentTimeMillis() - b);
        }

        private void unlockSafe() {
            try {
                semaphore.unlock();
            } catch (IllegalMonitorStateException e) {
                // ignore
            }
        }
    }

    private static class DataProducer implements Runnable {
        private final Queue<String> queue;
        private final PrintWriter writer;
        private final Supplier<Boolean> consumerDoneChecker;
        private final ReentrantLock semaphore;

        public DataProducer(Queue<String> queue, PrintWriter printWriter, Supplier<Boolean> consumerDoneChecker, ReentrantLock semaphore) {
            this.queue = queue;
            this.writer = printWriter;
            this.consumerDoneChecker = consumerDoneChecker;
            this.semaphore = semaphore;
        }

        @Override
        public void run() {
            long b = System.currentTimeMillis();
                while (!consumerDoneChecker.get()) {
                    String line = queue.poll();
                    if (line != null) {
                        writer.println(line);
                    } else {
//                        log.info("Producer waiting, wrote {} lines", i);
                        if (!consumerDoneChecker.get()) {
                            semaphore.lock();
                        }
                    }
                }
                String line;
                while ((line = queue.poll()) != null) {
                    writer.println(line);
                }
                log.info("Producer done in {} ms", System.currentTimeMillis() - b);
        }
    }
}
