package com.jinrong.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.common.InitComon;
import com.jinrong.common.SDKforTushare;
import com.jinrong.common.ThreadPoolComom;
import com.jinrong.entity.*;
import com.jinrong.mapper.*;
import com.jinrong.service.*;
import com.jinrong.util.ShortUUID;
import io.micrometer.common.util.StringUtils;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
public class StockController {

    @Autowired
    BalanceSheetMapper balanceSheetMapper;
    @Autowired
    IncomeStatementMapper incomeStatementMapper;
    @Autowired
    CashFlowStatementMapper cashFlowStatementMapper;
    @Autowired
    TtmAnalysisService ttmAnalysisService;
    @Autowired
    StockTechnicalIndicatorsServiceImpl stockTechnicalIndicatorsService;
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
    StockService stockService;
    @Autowired
    StockBasicMapper stockBasicMapper;



    @Autowired
    StockReportPredictionMapper stockReportPredictionMapper;

    @GetMapping("/initgp")
    public HashMap initgp() {
        List<HashMap<String, Object>> stockBasic = sdKforTushare.getApiResponse("stock_basic",
                new HashMap<>() {{
                }},
                "ts_code,symbol,name,area,industry,fullname,enname,cnspell,market,exchange,curr_type,list_status" +
                        ",list_date,delist_date,is_hs,act_name,act_ent_type" // 如 "ts_code,symbol,name"
        );
        Field[] declaredFields = StockBasic.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
        }
        int delete = stockBasicMapper.delete(new QueryWrapper<>());
        List<StockBasic> parse = new InitComon<StockBasic>().parse(StockBasic.class, stockBasic);
        stockBasicMapper.insert(parse);
        return new HashMap<>();
    }

    @GetMapping("/initdaily_basic")
    public HashMap initdaily_basic(String date) {
        ThreadPoolComom.executorService.execute(() -> {
            stockService.initdaily_basic(date);
        });
        return new HashMap<>();
    }


    @GetMapping("/initcw")
    public HashMap initcw() {
        List<String> list = List.of(
             "0930"
        );
        int be = 2025, end = 2025;
        ThreadPoolComom.executorService.execute(() -> {
            for (int i = be; i <= end; i++) {
                for (String s : list) {
                    String date = i + s;
                    finIndicatorMapper.delete(new QueryWrapper<FinIndicator>().lambda().eq(FinIndicator::getEndDate, LocalDate.parse(date, InitComon.formatter)));
                    cashFlowStatementMapper.delete(new QueryWrapper<CashFlowStatement>().lambda().eq(CashFlowStatement::getEndDate, LocalDate.parse(date, InitComon.formatter)));
                    incomeStatementMapper.delete(new QueryWrapper<IncomeStatement>().lambda().eq(IncomeStatement::getEndDate, LocalDate.parse(date, InitComon.formatter)));
                    balanceSheetMapper.delete(new QueryWrapper<BalanceSheet>().lambda().eq(BalanceSheet::getEndDate, LocalDate.parse(date, InitComon.formatter)));
                }
            }


            for (int i = be; i <= end; i++) {
                for (String s : list) {
                    String date = i + s;
                    ThreadPoolComom.executorService.execute(() -> {
                        stockService.fina_indicator_vip(date);
                    });

                    ThreadPoolComom.executorService.execute(() -> {
                        stockService.balancesheet_vip(date);
                    });
                    ThreadPoolComom.executorService.execute(() -> {
                        stockService.cashflow_vip(date);
                    });
                    ThreadPoolComom.executorService.execute(() -> {
                        stockService.income_vip(date);
                    });
                }
            }
        });


        return new HashMap<>();
    }

    @GetMapping("/initreport_rc")
    public HashMap initreport_rc() {
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 15; i++) {
            String format = now.minusDays(i).format(InitComon.formatter);
            ThreadPoolComom.executorService.execute(() -> {
                ttmAnalysisService.initreport_rc(format);
            });
        }

        return new HashMap<>();
    }
        @GetMapping("/inittechinical")
    public HashMap inittechinical() {
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 100; i++) {
            String format = now.minusDays(i).format(InitComon.formatter);
            ThreadPoolComom.executorService.execute(() -> {
                stockTechnicalIndicatorsService.init(format);
            });
        }

        return new HashMap<>();
    }


    @SneakyThrows
    @GetMapping("/intitfinalscore")
    public Object initfinalscore(
            @RequestParam(required = false) String code) {
        List<LocalDate> lastDays = new ArrayList<>();
        int currentYear = LocalDate.now().getYear(); // 当前年份
        List<String> list = List.of(
                 "-09-30"
        );
        // 遍历过去5年（含当前年的前5年）
        for (int i = 0; i <= 0; i++) {
            int year = currentYear - i; // 目标年份
            for (String s : list) {
                String date=year+s;
                lastDays.add(LocalDate.parse(date));
            }

        }
        List<StockBasic> stockBasics = stockBasicMapper.selectList(new QueryWrapper<StockBasic>()
                        .lambda()
//                .eq(StockBasic::getName,"德业股份")
        );
//        ttmAnalysisService.dosave(stockBasic.getTsCode(), LocalDate.now(),"peTtm");
        for (StockBasic stockBasic : stockBasics) {
            ThreadPoolComom.executorService.execute(() -> {
                for (LocalDate lastDay : lastDays) {
                    wallScoreService.calculateWallScore(stockBasic.getTsCode(),stockBasic.getName(), lastDay);
                }
            });
        }
        return new HashMap<>();
    }

    @GetMapping("/test2")
    public Object test2(@RequestParam(required = false) String date) {

        ttmAnalysisService.calculateAndSavePettm(StringUtils.isBlank(date) ? LocalDate.now() : LocalDate.parse(date));
        return new HashMap<>();
    }

    @GetMapping("/test3")
    public Object test3(@RequestParam(required = false) String date) {
        ttmAnalysisService.dxpez(StringUtils.isBlank(date) ? LocalDate.now() : LocalDate.parse(date));
        return new HashMap<>();
    }




}