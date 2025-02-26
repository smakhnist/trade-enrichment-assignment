package com.verygoodbank.misc;

import com.verygoodbank.tes.service.trade.SolutionType;
import org.springframework.util.StopWatch;

import java.util.concurrent.Callable;

public class Util {
    public static <T> Pair<T, Long> runWithStopWath(Callable<T> callable) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        T result = callable.call();
        stopWatch.stop();
        return Pair.of(result, stopWatch.getTotalTimeMillis());
    }

    public static String getPath(SolutionType solutionType, int port) {
        return String.format("http://localhost:%d/api/v1/enrich/%s", port, solutionType);
    }
}
