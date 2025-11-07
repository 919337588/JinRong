package com.jinrong.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinrong.common.BotChatCompletionsExample;
import com.jinrong.common.ThreadPoolComom;
import com.jinrong.entity.AiRequestLog;
import com.jinrong.entity.StockDailyBasic;
import com.jinrong.mapper.AiRequestLogMapper;
import com.jinrong.mapper.StockDailyBasicMapper;
import com.jinrong.mapper.ValuationMapper;
import com.jinrong.util.StockTradingStrategy;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class ValuationService {


    @Autowired
    private ValuationMapper valuationMapper;
    @Autowired
    private AiRequestLogMapper aiRequestLogMapper;
    @Autowired
    BotChatCompletionsExample botChatCompletionsExample;
    @Autowired
    private StockDailyBasicMapper stockDailyBasicMapper;

    public List<Map<String, Object>> getValuationData(String sql) {

        List<Map<String, Object>> maps = valuationMapper.selectValuationWithConditions(
                sql
        );
        for (Map<String, Object> map : maps) {

                ThreadPoolComom.executorService.execute(() -> {
                    requestAi(map, "askxinji");
                });
                ThreadPoolComom.executorService.execute(() -> {
                    requestAi(map, "syfx");
                });
        }
        return maps;
    }
    public void requestAi(Map<String,Object> map,String type){
        String ts_code = (String) map.get("ts_code");

        AiRequestLog aiRequestLog1 = aiRequestLogMapper.selectOne(new QueryWrapper<AiRequestLog>()
                .lambda()
                .eq(AiRequestLog::getTsCode, ts_code)
                .eq(AiRequestLog::getRequestFormatId, type)
                .orderByDesc(AiRequestLog::getRequestTime).last("limit 1"));
        if(aiRequestLog1!=null&&aiRequestLog1.getRequestTime().isBefore(LocalDateTime.now().minusDays(30))){
                aiRequestLogMapper.deleteById(aiRequestLog1.getId());
                aiRequestLog1=null;
        }
        if (aiRequestLog1==null){
            AiRequestLog aiRequestLog = new AiRequestLog();
            aiRequestLog.setTsCode(ts_code);
            while (aiRequestLog.getResponseMsg() == null || aiRequestLog.getResponseMsg().length() < 200) {
                botChatCompletionsExample.requestStock(aiRequestLog, type,  map);
            }
            aiRequestLogMapper.insert(aiRequestLog);
        }
    }

    public Map<String, Object> mock(String sql, int date, int end, int waittime, double stopLossRatio, double stopLossRatiov2) {
        HashMap<String, Object> map1 = new HashMap<>();
        List<Map<String, Object>> re = new ArrayList<>();

        double br = 100, cy = 100, hs = 100;
        for (int i = date; i <= end; i++) {
            String replace = sql.replace("@target_year", i + "");
            List<Map<String, Object>> maps = valuationMapper.selectValuationWithConditions(
                    replace
            );

            List<String> list = new CopyOnWriteArrayList<>();
            ConcurrentHashMap<String, Object> hashMap = new ConcurrentHashMap<>();
            List<Future<Object[]>> futures = new ArrayList<>();
            List<Map<String, Object>> indexDaily = valuationMapper.getIndexDaily(Integer.valueOf(i + "0901"), Integer.valueOf((i + waittime) + "0901"), "000300.SH");
            double be300 = (double) indexDaily.get(0).get("close");
            double en300 = (double) indexDaily.get(indexDaily.size() - 1).get("close");
            hs *= (en300 / be300);
            indexDaily = valuationMapper.getIndexDaily(Integer.valueOf(i + "0901"), Integer.valueOf((i + waittime) + "0901"), "399006.SZ");
            double bec = (double) indexDaily.get(0).get("close");
            double enc = (double) indexDaily.get(indexDaily.size() - 1).get("close");
            cy *= enc / bec;
            hashMap.put("创业板 " + waittime + "年涨幅", Math.round((enc - bec) / bec * 100 * 100) / 100.0 + "%");
            hashMap.put("沪深300 " + waittime + "年涨幅", Math.round((en300 - be300) / be300 * 100 * 100) / 100.0 + "%");
            for (Map<String, Object> map : maps) {
                int finalI = i;
                futures.add(ThreadPoolComom.executorService.submit(() -> {
                    if (futures.isEmpty()) {
                        return new Double[]{0d, 0d};
                    }
                    String tradeDate = map.get("trade_date").toString();
                    hashMap.put("购买时间", tradeDate);
                    String ts_code = (String) map.get("ts_code");
                    String name = (String) map.get("name");
                    double hlj = (double) map.get("hlj");
//                    hlj*=0.7;
                    Double adjFactor = stockDailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>().lambda()
                                    .eq(StockDailyBasic::getTsCode, ts_code).orderByDesc(StockDailyBasic::getTradeDate).last("limit 1"))
                            .get(0).getAdjFactor();
                    double close = (double) map.get("adj_factor_close");
                    double zs = close * 0.1;

//                    List<StockDailyBasic> stockDailyBasics1 = stockDailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>().lambda()
//                            .eq(StockDailyBasic::getTsCode, ts_code)
//                            .between(StockDailyBasic::getTradeDate, LocalDate.parse(tradeDate).minusDays(90), LocalDate.parse(tradeDate))
//                            .le(StockDailyBasic::getAdjFactorClose, close * 0.6).last("limit 1"));
//                    if(!stockDailyBasics1.isEmpty()){
//                        return new Double[]{0d,0d};
//                    }
//                    List<StockDailyBasic>     stockDailyBasics2 = stockDailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>().lambda()
//                            .eq(StockDailyBasic::getTsCode, ts_code)
//                            .between(StockDailyBasic::getTradeDate, LocalDate.parse(tradeDate).minusDays(30), LocalDate.parse(tradeDate))
//                            .le(StockDailyBasic::getAdjFactorClose, close * 0.9).last("limit 1"));
//                    if(stockDailyBasics2.isEmpty()){
//                        return new Double[]{0d,0d};
//                    }
//                    stockDailyBasics2 = stockDailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>().lambda()
//                                    .eq(StockDailyBasic::getTsCode, ts_code)
//                                    .between(StockDailyBasic::getTradeDate, LocalDate.parse(finalI + "-09-01").minusDays(30), LocalDate.parse(finalI + "-09-01"))
//                            .orderByAsc(StockDailyBasic::getAdjFactorClose).last("limit 1"));
//                            close/=adjFactor;
                    int bushand = (int) (3000 / close);
                    List<StockDailyBasic> stockDailyBasics = stockDailyBasicMapper.selectList(new QueryWrapper<StockDailyBasic>().lambda()
                            .eq(StockDailyBasic::getTsCode, ts_code)
                            .between(StockDailyBasic::getTradeDate, LocalDate.parse((finalI - 1) + "-09-01"), LocalDate.parse((finalI + waittime) + "-09-01"))
                            .orderByAsc(StockDailyBasic::getTradeDate));
                    for (StockDailyBasic stockDailyBasic : stockDailyBasics) {
                        stockDailyBasic.setAdjFactorClose(stockDailyBasic.getAdjFactorClose() / adjFactor);
                    }

                    Object[] doubles = StockTradingStrategy.executeStrategy(stockDailyBasics, name, stockDailyBasics.get(0).getTradeDate(),

                            bushand);
                    return doubles;
                }));
            }
            double buy = 0.1, sell = 0.1, size = 20;
            for (Future<Object[]> future : futures) {
                try {

                    Object[] doubles = future.get();
                    if ((Double) doubles[0] > 0 && size > 0) {
                        buy += (Double) doubles[0];
                        sell += (Double) doubles[1];
                        list.add((String) doubles[2]);
                        size--;
                    }
                    if (size == 0) {///
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            futures.clear();
            br *= sell / buy;
            hashMap.put("购买股票", list.toString());
            hashMap.put(waittime + "年内盈利", Math.round(((sell - buy) / buy * 100) * 100) / 100.0 + "%");
            re.add(hashMap);
        }
        int zy = end - date + 1;
        List<String> jg = new ArrayList<>();
        jg.add("本人：" + Math.round((br - 100) / 100 * 100 * 100) / 100.0 + "%");
        jg.add("创业板：" + Math.round((cy - 100) / 100 * 100 * 100) / 100.0 + "%");
        jg.add("沪深300：" + Math.round((hs - 100) / 100 * 100 * 100) / 100.0 + "%");
        map1.put(zy + "年总收益对比", jg);
        map1.put("re", re);
        return map1;
    }
}