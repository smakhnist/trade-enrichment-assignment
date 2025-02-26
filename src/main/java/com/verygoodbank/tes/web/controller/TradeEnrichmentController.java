package com.verygoodbank.tes.web.controller;


import com.verygoodbank.tes.service.trade.SolutionImplFactory;
import com.verygoodbank.tes.service.trade.SolutionType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final SolutionImplFactory solutionImplFactory;

    @RequestMapping(value = "/enrich/{solutionType}", method = RequestMethod.POST, produces = "text/csv", consumes = "multipart/form-data")
    public void enrichNaive(HttpServletResponse response, @PathVariable String solutionType, @RequestParam("file") MultipartFile multipartFile) throws IOException {
        SolutionType solutionTypeEnum = SolutionType.valueOf(solutionType);  // validate solutionType
        solutionImplFactory.getBean(solutionTypeEnum).enrichTrades(multipartFile.getInputStream(), response.getWriter());
    }

    @GetMapping("/echo")
    public String echo() {
        return "Yahoo New!!! Hello My Beautiful World.";
    }
}


