package com.jinrong.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.common.InitComon;
import com.jinrong.common.SDKforTushare;
import com.jinrong.entity.*;
import com.jinrong.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
@Service@Slf4j
public class StockService {
    @Autowired
    SDKforTushare sdKforTushare;
    @Autowired
    StockDailyBasicMapper stockDailyBasicMapper;

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
    StockService stockService;
    @Autowired
    StockBasicMapper stockBasicMapper;



    @Autowired
    StockReportPredictionMapper stockReportPredictionMapper;
    public List<StockDailyBasic> initdaily_basic(String finalDate){
        List<StockDailyBasic> parse = new InitComon<StockDailyBasic>().parse(StockDailyBasic.class, sdKforTushare.getApiResponse("daily_basic",
                new HashMap<>() {{
                    put("trade_date", finalDate);
                }},
                "" // 如 "ts_code,symbol,name"
        ));
        if (parse.isEmpty()) {
            return parse;
        }
        List<HashMap<String, Object>> apiResponse = sdKforTushare.getApiResponse("adj_factor",
                new HashMap<>() {{
                    put("trade_date", finalDate);
                }},
                "" // 如 "ts_code,symbol,name"
        );
        Map<String, Double> collect = apiResponse.stream().collect(Collectors.toMap(v -> v.get("tsCode").toString(), v -> Double.valueOf((String) v.get("adjFactor"))));

        for (StockDailyBasic stockDailyBasic : parse) {
            Double v = collect.get(stockDailyBasic.getTsCode());
            if (v != null) {
                stockDailyBasic.setAdjFactor(v);
                stockDailyBasic.setAdjFactorClose(v * stockDailyBasic.getClose());
            }
        }
        stockDailyBasicMapper.delete(new QueryWrapper<StockDailyBasic>().lambda()
                .eq(StockDailyBasic::getTradeDate, LocalDate.parse(finalDate, InitComon.formatter)));
        stockDailyBasicMapper.insert(parse);
        return parse;
    }

    public void fina_indicator_vip(String date){

        log.info("fina_indicator_vip " + date);
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
    }

    public void balancesheet_vip(String date){
        log.info("balancesheet_vip " + date);
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
    }
    public void income_vip(String date){
        log.info("income_vip " + date);
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
        }}

    public void cashflow_vip(String date){
        log.info("cashflow_vip " + date);
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
    }
}
