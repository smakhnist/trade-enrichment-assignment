package com.verygoodbank.tes.web.controller;


import com.verygoodbank.tes.service.trade.impl.EfficientStructuresTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.NaiveTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.ThreadLocalDateFormaterTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.ThreadsSplitTradeEnrichmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TradeEnrichmentController {
    private final NaiveTradeEnrichmentService naiveTradeEnrichmentService;
    private final ThreadLocalDateFormaterTradeEnrichmentService threadLocalDateFormaterTradeEnrichmentService;
    private final EfficientStructuresTradeEnrichmentService efficientStructuresTradeEnrichmentService;
    private final ThreadsSplitTradeEnrichmentService threadsSplitTradeEnrichmentService;

    @RequestMapping(value = "/enrich-naive", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichNaive(HttpServletResponse response, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        naiveTradeEnrichmentService.enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @RequestMapping(value = "/enrich-df-thread-local", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichNaive2(HttpServletResponse response, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        threadLocalDateFormaterTradeEnrichmentService.enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @RequestMapping(value = "/enrich-efficient-structures", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichEfficientStructures(HttpServletResponse response, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        efficientStructuresTradeEnrichmentService.enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @RequestMapping(value = "/enrich-threads-split", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichQuick(HttpServletResponse response, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        threadsSplitTradeEnrichmentService.enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @GetMapping("/echo")
    public String echo() {
        return "Hello New World!!!";
    }
}


