package com.verygoodbank.tes;

import com.verygoodbank.misc.RunType;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TradeEnrichmentServiceApplicationTests {

    @LocalServerPort
    int randomServerPort;

    @ParameterizedTest
    @EnumSource(RunType.class)
    public void testTradeEnrichment(RunType runType) {
        ResponseEntity<String> response = processFile(runType, "trade.csv");
        BDDAssertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        BDDAssertions.assertThat(response.getBody()).isNotNull();
        assertActualIsExpected(response.getBody(), "expected/trade.output.csv");
    }

    @ParameterizedTest
    @EnumSource(RunType.class)
    public void testEnrichmentInputWithIllegalLines(RunType runType) {
        ResponseEntity<String> response = processFile(runType, "trade-with-corrupted-lines.csv");
        BDDAssertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        BDDAssertions.assertThat(response.getBody()).isNotNull();
        assertActualIsExpected(response.getBody(), "expected/trade-with-corrupted-lines.output.csv");
    }

    @ParameterizedTest
    @EnumSource(RunType.class)
    public void testEnsureThreadSafety(RunType runType) throws InterruptedException {
        final int THREADS_NUMBER = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS_NUMBER);
        CountDownLatch countDownLatch = new CountDownLatch(THREADS_NUMBER);

        List<String> callOutputs = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < THREADS_NUMBER; i++) {
            executorService.submit(() -> {
                String body = processFile(runType, "trade.csv").getBody();
                callOutputs.add(body);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await(3, TimeUnit.SECONDS);

        BDDAssertions.assertThat(callOutputs).hasSize(THREADS_NUMBER);
        callOutputs.forEach(body -> assertActualIsExpected(body, "expected/trade.output.csv"));
    }


    private static void assertActualIsExpected(String actualOutput, String expectedResourcePath) {
        var expectedContent = new Scanner(getClasspathResource(expectedResourcePath), StandardCharsets.UTF_8)
                .useDelimiter("\\A").next();

        // just to make sure that the line endings are consistent (windows new line is \r\n, while unix is \n)
        BDDAssertions.assertThat(actualOutput.replace("\r\n", "\n"))
                .isEqualTo(expectedContent.replace("\r\n", "\n"));
    }

    private ResponseEntity<String> processFile(RunType runType, String inputResourcePath) {
        RestTemplate restTemplate = new RestTemplate();

        ContentDisposition contentDisposition = ContentDisposition.builder("form-data")
                .name("file")
                .filename("trade.csv")
                .build();

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>(Map.of(HttpHeaders.CONTENT_DISPOSITION, List.of(contentDisposition.toString())));

        InputStreamResource inputStreamResource = new InputStreamResource(getClasspathResource(inputResourcePath));
        HttpEntity<Resource> fileEntity = new HttpEntity<>(inputStreamResource, fileMap);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(Map.of("file", List.of(fileEntity)));

        HttpHeaders headers = new HttpHeaders(new LinkedMultiValueMap<>(Map.of(HttpHeaders.CONTENT_TYPE, List.of(MediaType.MULTIPART_FORM_DATA_VALUE))));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(runType.getPath(randomServerPort), HttpMethod.POST, requestEntity, String.class);
    }

    private static InputStream getClasspathResource(String classPath) {
        return TradeEnrichmentServiceApplicationTests.class.getClassLoader().getResourceAsStream(classPath);
    }
}
