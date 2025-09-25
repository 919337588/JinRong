package com.jinrong.schedule;

import com.jinrong.service.TtmAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@EnableScheduling
public class TtmAnalysisScheduler {
    @Autowired
    private TtmAnalysisService service;

    // 每月第一个交易日执行
//    @Scheduled(cron = "0 0 18 * * MON-FRI") // 工作日下午6点
    public void runAnalysis() {
        service.calculateAndSavePettm(LocalDate.now());
    }
}