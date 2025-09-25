package com.jinrong.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolComom {
    public static ExecutorService executorService = Executors.newFixedThreadPool(48);
}
