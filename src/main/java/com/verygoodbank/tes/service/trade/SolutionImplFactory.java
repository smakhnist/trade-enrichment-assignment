package com.verygoodbank.tes.service.trade;

import com.verygoodbank.tes.service.trade.impl.EfficientStructuresTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.NaiveTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.ThreadLocalDateFormatterTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.ThreadsSplitTradeEnrichmentService;
import com.verygoodbank.tes.service.trade.impl.VirtualThreadsSplitTradeEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SolutionImplFactory {
    private final ApplicationContext applicationContext;

    public TradeEnrichmentService getBean(SolutionType solutionType) {
        return switch (solutionType) {
            case Naive -> applicationContext.getBean(NaiveTradeEnrichmentService.class);
            case EfficientStructures -> applicationContext.getBean(EfficientStructuresTradeEnrichmentService.class);
            case ThreadLocalDateFormatter ->
                    applicationContext.getBean(ThreadLocalDateFormatterTradeEnrichmentService.class);
            case ThreadSplit -> applicationContext.getBean(ThreadsSplitTradeEnrichmentService.class);
            case VirtualThreadSleep -> applicationContext.getBean(VirtualThreadsSplitTradeEnrichmentService.class);
            default -> throw new IllegalArgumentException("Unknown solution type: " + solutionType);
        };
    }
}
