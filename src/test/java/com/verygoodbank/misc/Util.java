package com.verygoodbank.misc;

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
}
