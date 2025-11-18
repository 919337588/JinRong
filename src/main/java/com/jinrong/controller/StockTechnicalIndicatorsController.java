package com.jinrong.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinrong.common.ThreadPoolComom;
import com.jinrong.entity.StockFallStabilizeRiseAnalysis;
import com.jinrong.entity.StockMaBreakoutAnalysis;
import com.jinrong.entity.StockTechnicalIndicators;
import com.jinrong.mapper.StockFallStabilizeRiseAnalysisMapper;
import com.jinrong.mapper.StockMaBreakoutAnalysisMapper;
import com.jinrong.mapper.StockTechnicalIndicatorsMapper;
import com.jinrong.service.StockTechnicalIndicatorsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 股票技术指标数据表 前端控制器
 * </p>
 *
 * @author example
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/stock-technical-indicators")
public class StockTechnicalIndicatorsController {

    @Autowired
    private StockTechnicalIndicatorsServiceImpl stockTechnicalIndicatorsService;
    @Autowired
    private StockMaBreakoutAnalysisMapper stockMaBreakoutAnalysisMapper;

    @Autowired
    private StockFallStabilizeRiseAnalysisMapper stockFallStabilizeRiseAnalysisMapper;

    /**
     * 保存实体
     */
    @PostMapping("/save")
    public boolean save(@RequestBody StockTechnicalIndicators entity) {
        return stockTechnicalIndicatorsService.save(entity);
    }

    /**
     * 根据ID删除
     */
    @DeleteMapping("/remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return stockTechnicalIndicatorsService.removeById(id);
    }

    /**
     * 根据ID更新
     */
    @PutMapping("/update")
    public boolean update(@RequestBody StockTechnicalIndicators entity) {
        return stockTechnicalIndicatorsService.updateById(entity);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/get/{id}")
    public StockTechnicalIndicators get(@PathVariable Long id) {
        return stockTechnicalIndicatorsService.getById(id);
    }

    /**
     * 查询所有
     */
    @GetMapping("/list")
    public List<StockTechnicalIndicators> list() {
        return stockTechnicalIndicatorsService.list();
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public Page<StockTechnicalIndicators> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return stockTechnicalIndicatorsService.page(new Page<>(current, size));
    }

    /**
     * 根据股票代码查询
     */
    @GetMapping("/by-ts-code/{tsCode}")
    public List<StockTechnicalIndicators> getByTsCode(@PathVariable String tsCode) {
        QueryWrapper<StockTechnicalIndicators> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ts_code", tsCode);
        queryWrapper.orderByDesc("trade_date");
        return stockTechnicalIndicatorsService.list(queryWrapper);
    }

    /**
     * 根据股票代码和交易日期查询
     */
    @GetMapping("/by-ts-code-and-date")
    public StockTechnicalIndicators getByTsCodeAndDate(
            @RequestParam String tsCode,
            @RequestParam String tradeDate) {
        QueryWrapper<StockTechnicalIndicators> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ts_code", tsCode);
        queryWrapper.eq("trade_date", tradeDate);
        return stockTechnicalIndicatorsService.getOne(queryWrapper);
    }

    /**
     * 批量保存
     */
    @PostMapping("/save-batch")
    public boolean saveBatch(@RequestBody List<StockTechnicalIndicators> entities) {
        return stockTechnicalIndicatorsService.saveBatch(entities);
    }

    /**
     * 批量检查多个股票的均线突破情况
     */
    @GetMapping("/batch-check-ma-breakout")
    public List<Map<String, Object>> batchCheckMaBreakout(
            @RequestParam String tsCodes,
            @RequestParam(defaultValue = "30") int days) {
        return Arrays.stream(tsCodes.split(","))
                .map(tsCode -> stockTechnicalIndicatorsService.checkMaConsistencyAndBreakout(tsCode, days,null,false))
                .collect(Collectors.toList());
    }

    @GetMapping("/batch-check-fall-stabilize-rise")
    public List<Map<String, Object>> batchCheckFallStabilizeRise(
            @RequestParam String tsCodes,
            @RequestParam(defaultValue = "30") int days) {
        return Arrays.stream(tsCodes.split(","))
                .map(tsCode -> stockTechnicalIndicatorsService.checkFallStabilizeRiseSignal(tsCode, days,null,false))
                .collect(Collectors.toList());
    }


    @GetMapping("/allcheck")
    public void allcheck(
    ) {
        stockTechnicalIndicatorsService.checkAll(LocalDate.now().minusDays(1));
    }
}