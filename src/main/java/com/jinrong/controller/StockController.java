package com.jinrong.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.common.InitComon;
import com.jinrong.common.SDKforTushare;
import com.jinrong.common.ThreadPoolComom;
import com.jinrong.entity.*;
import com.jinrong.mapper.*;
import com.jinrong.service.TableMetaService;
import com.jinrong.service.TtmAnalysisService;
import com.jinrong.service.WallScoreService;
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
    SDKforTushare sdKforTushare;

    @Autowired
    StockBasicMapper stockBasicMapper;

    @Autowired
    StockDailyBasicMapper stockDailyBasicMapper;

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
        List<StockBasic> parse = new InitComon<StockBasic>().parse(StockBasic.class, stockBasic);
        stockBasicMapper.insert(parse);
        return new HashMap<>();
    }

    @GetMapping("/initdaily_basic")
    public HashMap initdaily_basic(String date) {
        LocalDate today = LocalDate.now();
        // 计算13年前的日期

        // 从开始日期循环到今天
//        LocalDate currentDate = LocalDate.of(2013, 8, 1);
//        while (currentDate.getYear()<2020) {
//             date = currentDate.format(InitComon.formatter);
            String finalDate = date;
            ThreadPoolComom.executorService.execute(() -> {
                System.out.println(finalDate);
            List<StockDailyBasic> parse = new InitComon<StockDailyBasic>().parse(StockDailyBasic.class, sdKforTushare.getApiResponse("daily_basic",
                    new HashMap<>() {{
                        put("trade_date", finalDate);
                    }},
                    "" // 如 "ts_code,symbol,name"
            ));
//                List<StockDailyBasic>  parse = stockDailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>()
//                        .lambda().eq(StockDailyBasic::getTradeDate,LocalDate.parse(finalDate, InitComon.formatter) ));
                if(parse.isEmpty()){
                    return;
                }
                List<HashMap<String, Object>> apiResponse = sdKforTushare.getApiResponse("adj_factor",
                        new HashMap<>() {{
                            put("trade_date", finalDate);
                        }},
                        "" // 如 "ts_code,symbol,name"
                );
                Map<String, Double> collect = apiResponse.stream().collect(Collectors.toMap(v -> v.get("tsCode").toString(), v ->Double.valueOf((String) v.get("adjFactor"))));

                for (StockDailyBasic stockDailyBasic : parse) {
                    Double v = collect.get(stockDailyBasic.getTsCode());
                    if(v!=null){
                        stockDailyBasic.setAdjFactor(v);
                        stockDailyBasic.setAdjFactorClose(v*stockDailyBasic.getClose());
                    }
                }
                stockDailyBasicMapper.delete(new QueryWrapper<StockDailyBasic>().lambda()
                        .eq(StockDailyBasic::getTradeDate, LocalDate.parse(finalDate,InitComon.formatter)));
                stockDailyBasicMapper.insert(parse);
            });
//            currentDate = currentDate.plusDays(1); // 日期加一天[6,9](@ref)
//        }

        return new HashMap<>();
    }

    @Autowired
    BalanceSheetMapper balanceSheetMapper;
    @Autowired
    IncomeStatementMapper incomeStatementMapper;
    @Autowired
    CashFlowStatementMapper cashFlowStatementMapper;

    @Autowired
    TableMetaService tableMetaService;
    @Autowired
    FinIndicatorMapper finIndicatorMapper;

    @GetMapping("/initcw")
    public HashMap initcw() {
        List<String> list = List.of(
//                "0331", "0630", "0930", "1231"
        );
        int be = 2009, end = 2025;
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
                        System.out.println("fina_indicator_vip " + date);
                        List<FinIndicator> parse3 = new InitComon<FinIndicator>().parse(FinIndicator.class
                                , sdKforTushare.getApiResponse("fina_indicator_vip",
                                        new HashMap<>() {{
                                            put("period", date);
                                        }},
                                        tableMetaService.getTableColumnsAsString("fin_indicator") // 如 "ts_code,symbol,name"

                                ));
                        Map<String, FinIndicator> h = new HashMap<>();
                        for (FinIndicator finIndicator : parse3) {
                            if (finIndicator.getUpdateFlag().equals("1") || !h.containsKey(finIndicator.getTsCode())) {
                                h.put(finIndicator.getTsCode(), finIndicator);
                            }
                        }

                        finIndicatorMapper.insert(h.values());
                    });

                    ThreadPoolComom.executorService.execute(() -> {
                        System.out.println("balancesheet_vip " + date);
                        for (Integer integer : List.of(1, 2)) {
                            List<BalanceSheet> parse = new InitComon<BalanceSheet>().parse(BalanceSheet.class
                                    , sdKforTushare.getApiResponse("balancesheet_vip",
                                            new HashMap<>() {{
                                                put("period", date);
                                                put("report_type", integer);
                                            }},
                                            tableMetaService.getTableColumnsAsString("balance_sheet") // 如 "ts_code,symbol,name"
                                    ));

                            balanceSheetMapper.insert(parse);
                        }

                    });
                    ThreadPoolComom.executorService.execute(() -> {
                        System.out.println("cashflow_vip " + date);
                        for (Integer integer : List.of(1, 2)) {
                            List<CashFlowStatement> parse = new InitComon<CashFlowStatement>().parse(CashFlowStatement.class
                                    , sdKforTushare.getApiResponse("cashflow_vip",
                                            new HashMap<>() {{
                                                put("period", date);
                                                put("report_type", integer);
                                            }},
                                            tableMetaService.getTableColumnsAsString("cash_flow_statement") // 如 "ts_code,symbol,name"
                                    ));
                            cashFlowStatementMapper.insert(parse);
                        }

                    });
                    ThreadPoolComom.executorService.execute(() -> {
                        System.out.println("income_vip " + date);
                        for (Integer integer : List.of(1, 2)) {
                            List<IncomeStatement> parse2 = new InitComon<IncomeStatement>().parse(IncomeStatement.class,
                                    sdKforTushare.getApiResponse("income_vip",
                                            new HashMap<>() {{
                                                put("period", date);
                                                put("report_type", integer);
                                            }},
                                            tableMetaService.getTableColumnsAsString("income_statement") // 如 "ts_code,symbol,name"

                                    ));
                            incomeStatementMapper.insert(parse2);
                        }

                    });


                }
            }
        });


        return new HashMap<>();
    }

    @GetMapping("/initreport_rc")
    public HashMap initreport_rc(String reportDate) {
        ThreadPoolComom.executorService.execute(() -> {
            List<HashMap<String, Object>> dailyBasic = sdKforTushare.getApiResponse("report_rc",
                    new HashMap<>() {{
                        put("report_date", reportDate);
                    }},
                    "" // 如 "ts_code,symbol,name"
            );
            List<StockReportPrediction> parse = new InitComon<StockReportPrediction>().parse(StockReportPrediction.class, dailyBasic);
            stockReportPredictionMapper.delete(new QueryWrapper<StockReportPrediction>().lambda()
                    .eq(StockReportPrediction::getReportDate, LocalDate.parse(reportDate, InitComon.formatter)));
            stockReportPredictionMapper.insert(parse);
        });
        return new HashMap<>();
    }

    @Autowired
    TtmAnalysisService ttmAnalysisService;
    @Autowired
    WallScoreService wallScoreService;
    @Autowired
    FinancialScoreMapper financialScoreMapper;

    @SneakyThrows
    @GetMapping("/intitfinalscore")
    public Object initfinalscore(
            @RequestParam(required = false) String code) {
        List<LocalDate> lastDays = new ArrayList<>();
        int currentYear = LocalDate.now().getYear(); // 当前年份

        // 遍历过去5年（含当前年的前5年）
        for (int i = 7; i <= 16; i++) {
            int year = currentYear - i; // 目标年份
            // 构造该年最后一天（12月31日）
            LocalDate lastDay = LocalDate.of(year, 12, 31);
            lastDays.add(lastDay);
        }

        List<StockBasic> stockBasics = stockBasicMapper.selectList(new QueryWrapper<StockBasic>()
                        .lambda()
//                .eq(StockBasic::getName,"德业股份")
        );
//        ttmAnalysisService.dosave(stockBasic.getTsCode(), LocalDate.now(),"peTtm");
        for (StockBasic stockBasic : stockBasics) {
            ThreadPoolComom.executorService.execute(() -> {
                for (LocalDate lastDay : lastDays) {

                    wallScoreService.calculateWallScore(stockBasic, lastDay);


                }
            });
        }
        return new HashMap<>();
    }

    @GetMapping("/test2")
    public Object test2(@RequestParam(required = false) String date) {

        ttmAnalysisService.calculateAndSavePettm(StringUtils.isBlank(date)?LocalDate.now():LocalDate.parse(date));
        return new HashMap<>();
    }

    @GetMapping("/test3")
    public Object test3(@RequestParam(required = false) String date) {
        ttmAnalysisService.dxpez(StringUtils.isBlank(date)?LocalDate.now():LocalDate.parse(date));
        return new HashMap<>();
    }


    @GetMapping("/test")
    public Object test() {


        List<CashFlowStatement> parse = new InitComon<CashFlowStatement>().parse(CashFlowStatement.class
                , sdKforTushare.getApiResponse("cashflow_vip",
                        new HashMap<>() {{
                            put("period", "20241231");
                            put("report_type", 1);
                        }},
                        tableMetaService.getTableColumnsAsString("cash_flow_statement") // 如 "ts_code,symbol,name"
                ));
        cashFlowStatementMapper.insert(parse);
        return new HashMap<>();
    }


}