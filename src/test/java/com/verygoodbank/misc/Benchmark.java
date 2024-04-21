package com.verygoodbank.misc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Benchmark {

    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        final int THREADS_NUMBER = 20;
        final String FILE_PATH = "medium-trade-file.csv";
        final RunType[] runTypes = RunType.values();

        Arrays.stream(runTypes).forEach(runType -> {
            try {
                List<Long> respTimes = runForType(THREADS_NUMBER, FILE_PATH, runType);
                System.out.printf("%s - %s%n", runType, respTimes.stream().mapToLong(Long::longValue).summaryStatistics());
            } catch (InterruptedException e) {
                log.error("Error running benchmark", e);
            }
        });
    }

    private static List<Long> runForType(int THREADS_NUMBER, String FILE_PATH, RunType runType) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS_NUMBER);

        CountDownLatch countDownLatch = new CountDownLatch(THREADS_NUMBER);
        List<Long> respTimes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREADS_NUMBER; i++) {
            executorService.submit(() -> {
                try {
                    var respWithTiming = Util.runWithStopWath(() -> processFile(new File(FILE_PATH), runType));
                    respTimes.add(respWithTiming.second());
                } catch (Exception e) {
                    log.error("Error processing file", e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        executorService.shutdown();

        return respTimes;
    }

    private static ResponseEntity<String> processFile(File file, RunType runType) throws FileNotFoundException {
        RestTemplate restTemplate = new RestTemplate();

        ContentDisposition contentDisposition = ContentDisposition.builder("form-data")
                .name("file")
                .filename(file.getName())
                .build();

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>(Map.of(HttpHeaders.CONTENT_DISPOSITION, List.of(contentDisposition.toString())));

        HttpEntity<Resource> fileEntity = new HttpEntity<>(new InputStreamResource(new FileInputStream(file)), fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(Map.of("file", List.of(fileEntity)));

        HttpHeaders headers = new HttpHeaders(new LinkedMultiValueMap<>(Map.of(HttpHeaders.CONTENT_TYPE, List.of(MediaType.MULTIPART_FORM_DATA_VALUE))));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(runType.getPath(SERVER_PORT), HttpMethod.POST, requestEntity, String.class);
    }
}
