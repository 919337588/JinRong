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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
@EnableScheduling
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

    // 每月第一个交易日执行
    @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void runAnalysis() {
        List<Future> list=new ArrayList<>();
        list.add(ThreadPoolComom.executorService.submit(() -> {
            cws(LocalDate.now());
        }));


        StockDailyBasic stockDailyBasic = stockDailyBasicMapper.selectMaxTradeDate();
        LocalDate tradeDate = stockDailyBasic.getTradeDate();
        stockController.initgp();
        LocalDate now = LocalDate.now();
        while (!tradeDate.isAfter(now)) {
            String date = tradeDate.format(InitComon.formatter);
            list.add(ThreadPoolComom.executorService.submit(() -> { stockService.initdaily_basic(date);}));
            list.add(ThreadPoolComom.executorService.submit(()->{ ttmAnalysisService.initreport_rc(date);}));
            for (Future future : list) {
                try {
                    future.get();
                } catch (Exception e) {
                  e.printStackTrace();
                }
            }
            list.clear();
            ttmAnalysisService.calculateAndSavePettm(tradeDate);
            ttmAnalysisService.dxpez(tradeDate);
            tradeDate=tradeDate.plusDays(1);
        }


    }

    public void cws( LocalDate tradeDate){
        List<String> list = List.of(
                "0331", "0630", "0930", "1231"
        );
        for (String s : list) {
            String time=tradeDate.getYear()+s;
            LocalDate parse = LocalDate.parse( tradeDate.getYear()+s, InitComon.formatter);
            if(!parse.isBefore(tradeDate)) {
                finIndicatorMapper.delete(new QueryWrapper<FinIndicator>().lambda().eq(FinIndicator::getEndDate, parse));
                cashFlowStatementMapper.delete(new QueryWrapper<CashFlowStatement>().lambda().eq(CashFlowStatement::getEndDate, parse));
                incomeStatementMapper.delete(new QueryWrapper<IncomeStatement>().lambda().eq(IncomeStatement::getEndDate, parse));
                balanceSheetMapper.delete(new QueryWrapper<BalanceSheet>().lambda().eq(BalanceSheet::getEndDate, parse));
                stockService.fina_indicator_vip(time);
                stockService.balancesheet_vip(time);
                stockService.cashflow_vip(time);
                stockService.income_vip(time);
                break;
            }
        }
    }
}