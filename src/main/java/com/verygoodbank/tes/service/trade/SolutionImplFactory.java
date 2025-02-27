package com.verygoodbank.tes.service.trade;

import com.verygoodbank.tes.service.product.ProductService;
import com.verygoodbank.tes.service.trade.impl.EfficientStructuresTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.NaiveTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.ThreadLocalDateFormatterTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.ThreadsSplitTradeEnrichmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.verygoodbank.tes.service.trade.SolutionType.*;

@Service
@RequiredArgsConstructor
public class SolutionImplFactory {
    private final ApplicationContext applicationContext;
    private final ProductService productService;
    private Map<SolutionType, TradeEnrichmentService> map;

    @PostConstruct
    public void init() {
        map = new HashMap<>();
        map.put(Naive, applicationContext.getBean(NaiveTradeEnrichmentService.class));
        map.put(EfficientStructures, applicationContext.getBean(EfficientStructuresTradeEnrichmentService.class));
        map.put(ThreadLocalDateFormatter, applicationContext.getBean(ThreadLocalDateFormatterTradeEnrichmentService.class));
        map.put(ThreadSplit50, new ThreadsSplitTradeEnrichmentService(productService, false, 50));
        map.put(ThreadSplit100, new ThreadsSplitTradeEnrichmentService(productService, false, 100));
        map.put(ThreadSplit500, new ThreadsSplitTradeEnrichmentService(productService, false, 500));
        map.put(ThreadSplit1000, new ThreadsSplitTradeEnrichmentService(productService, false, 1000));
        map.put(ThreadSplit5000, new ThreadsSplitTradeEnrichmentService(productService, false, 5000));
        map.put(VirtualThreadSleep50, new ThreadsSplitTradeEnrichmentService(productService, true, 50));
        map.put(VirtualThreadSleep100, new ThreadsSplitTradeEnrichmentService(productService, true, 100));
        map.put(VirtualThreadSleep500, new ThreadsSplitTradeEnrichmentService(productService, true, 500));
        map.put(VirtualThreadSleep1000, new ThreadsSplitTradeEnrichmentService(productService, true, 1000));
        map.put(VirtualThreadSleep5000, new ThreadsSplitTradeEnrichmentService(productService, true, 5000));
    }

    public TradeEnrichmentService getBean(SolutionType solutionType) {
        return Optional.ofNullable(map.get(solutionType)).orElseThrow(() -> new IllegalArgumentException(String.format("No such solution type mapping for %s", solutionType)));
    }
}
