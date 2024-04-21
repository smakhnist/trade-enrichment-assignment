package com.verygoodbank.tes.service.trade;

import java.io.InputStream;
import java.io.PrintWriter;

public interface TradeEnrichmentService {
    void enrichTrades(InputStream tradeInputStream, PrintWriter printWriter);
}
