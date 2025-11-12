package com.jinrong.schedule;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.common.InitComon;
import com.jinrong.common.SDKforTushare;
import com.jinrong.common.ThreadPoolComom;
import com.jinrong.controller.StockController;
import com.jinrong.entity.*;
import com.jinrong.mapper.*;
import com.jinrong.service.StockService;
import com.jinrong.service.TableMetaService;
import com.jinrong.service.TtmAnalysisService;
import com.jinrong.service.WallScoreService;
import com.jinrong.util.FinancialReportDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
@EnableScheduling
@RestController
public class TtmAnalysisScheduler {
    @Autowired
    private TtmAnalysisService service;
    @Autowired
    StockDailyBasicMapper stockDailyBasicMapper;
    @Autowired
    StockController stockController;
    @Autowired
    StockService stockService;

    @Autowired
    BalanceSheetMapper balanceSheetMapper;
    @Autowired
    IncomeStatementMapper incomeStatementMapper;
    @Autowired
    CashFlowStatementMapper cashFlowStatementMapper;
    @Autowired
    TtmAnalysisService ttmAnalysisService;
    @Autowired
    WallScoreService wallScoreService;
    @Autowired
    FinancialScoreMapper financialScoreMapper;
    @Autowired
    TableMetaService tableMetaService;
    @Autowired
    FinIndicatorMapper finIndicatorMapper;
    @Autowired
    SDKforTushare sdKforTushare;
    @Autowired
    StockBasicMapper stockBasicMapper;
    @RequestMapping("runDailyScheduled")
    public Map run(){
       new Thread(this::runDailyScheduled).start();
       return new HashMap<>();
    }
    // 每月第一个交易日执行
    @Scheduled(cron = "00 59 17,23 * * MON-FRI")

    public void runDailyScheduled() {
        System.out.println("runDailyScheduled");
        List<Future> list = new ArrayList<>();
        StockDailyBasic stockDailyBasic = stockDailyBasicMapper.selectMaxTradeDate();
        LocalDate tradeDate = stockDailyBasic.getTradeDate();

        stockController.initgp();
        LocalDate now = LocalDate.now();
        while (!tradeDate.isAfter(now)) {
            String date = tradeDate.format(InitComon.formatter);
            list.add(ThreadPoolComom.executorService.submit(() -> {
                stockService.initdaily_basic(date);
            }));
            list.add(ThreadPoolComom.executorService.submit(() -> {
                ttmAnalysisService.initreport_rc(date);
            }));
            tradeDate = tradeDate.plusDays(1);
        }
        waitFuturList(list);
        StockDailyBasic stockDailyBasic1 = stockDailyBasicMapper.selectMaxTradeDate();
        LocalDate tradeDateNewEst = stockDailyBasic1.getTradeDate();
        cws(tradeDateNewEst, list);
        if (tradeDateNewEst.isAfter(stockDailyBasic.getTradeDate())) {
            list.addAll(ttmAnalysisService.calculateAndSavePettm(tradeDateNewEst));
        }
        if (!tradeDateNewEst.isBefore(stockDailyBasic.getTradeDate())) {
            waitFuturList(list);
            ttmAnalysisService.dxpez(tradeDateNewEst);
        }

    }

    public void waitFuturList(List<Future> list) {
        for (Future future : list) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        list.clear();
    }

    public void cws(LocalDate tradeDate, List<Future> list) {
        List<String> lastThreeReportDatesForDate = FinancialReportDate.getLastThreeReportDatesForDate(tradeDate);
        String time = lastThreeReportDatesForDate.get(0);
        LocalDate parse = LocalDate.parse(time, InitComon.formatter);
        finIndicatorMapper.delete(new QueryWrapper<FinIndicator>().lambda().eq(FinIndicator::getEndDate, parse));
        cashFlowStatementMapper.delete(new QueryWrapper<CashFlowStatement>().lambda().eq(CashFlowStatement::getEndDate, parse));
        incomeStatementMapper.delete(new QueryWrapper<IncomeStatement>().lambda().eq(IncomeStatement::getEndDate, parse));
        balanceSheetMapper.delete(new QueryWrapper<BalanceSheet>().lambda().eq(BalanceSheet::getEndDate, parse));
        list.add(ThreadPoolComom.executorService.submit(() -> stockService.fina_indicator_vip(time)));
        list.add(ThreadPoolComom.executorService.submit(() -> stockService.balancesheet_vip(time)));
        list.add(ThreadPoolComom.executorService.submit(() -> stockService.cashflow_vip(time)));
        list.add(ThreadPoolComom.executorService.submit(() -> stockService.income_vip(time)));waitFuturList(list);
        waitFuturList(list);
        List<StockDailyBasic> stockDailyBasics = stockDailyBasicMapper
                .selectList(new QueryWrapper<StockDailyBasic>().lambda().eq(StockDailyBasic::getTradeDate, tradeDate));
        financialScoreMapper.delete(new QueryWrapper<FinancialScore>().lambda().eq(FinancialScore::getEndDate, parse));
        for (StockDailyBasic stockBasic : stockDailyBasics) {
            list.add(ThreadPoolComom.executorService.submit(() -> {
                wallScoreService.calculateWallScore(stockBasic.getTsCode(), null, parse);
            }));
        }
    }
}