package com.verygoodbank.tes.web.controller;


import com.verygoodbank.tes.service.trade.impl.EfficientStructuresTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.NaiveTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.QuickTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.ReadWriteTreadsSplitTradeEnrichmentService;
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
    private final ReadWriteTreadsSplitTradeEnrichmentService readWriteTreadsSplitTradeEnrichmentService;
    private final EfficientStructuresTradeEnrichmentService efficientStructuresTradeEnrichmentService;
    private final QuickTradeEnrichmentService quickTradeEnrichmentService;

    @RequestMapping(value = "/enrich-naive", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichNaive(HttpServletResponse response, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        naiveTradeEnrichmentService.enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @RequestMapping(value = "/enrich-read-write-split", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichTrade2Threads(HttpServletResponse response, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        readWriteTreadsSplitTradeEnrichmentService.enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @RequestMapping(value = "/enrich-efficient-structures", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichEfficientStructures(HttpServletResponse response, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        efficientStructuresTradeEnrichmentService.enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @RequestMapping(value = "/enrich-quick", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichQuick(HttpServletResponse response, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        quickTradeEnrichmentService.enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @GetMapping("/echo")
    public String echo() {
        return "Hello World!";
    }
}


