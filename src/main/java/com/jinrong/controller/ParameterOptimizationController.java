package com.jinrong.controller;

import com.jinrong.service.ai.ParameterOptimizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/parameter-optimization")
public class ParameterOptimizationController {

    @Autowired
    private ParameterOptimizationService parameterOptimizationService;

    @PostMapping("/optimize-fall-stabilize-rise")
    public Map<String, Object> optimizeFallStabilizeRise(
            @RequestParam String tsCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        Map<String, Object> result = new HashMap<>();

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            ParameterOptimizationService.OptimizationResult optimizationResult =
                    parameterOptimizationService.optimizeFallStabilizeRise(tsCode, start, end);

            result.put("success", true);
            result.put("strategyType", "fall-stabilize-rise");
            result.put("bestParameters", optimizationResult.getBestParameters());
            result.put("totalEvaluations", optimizationResult.getEvaluations().size());

            // 返回前10个最佳参数
            if (optimizationResult.getEvaluations().size() > 10) {
                result.put("topParameters", optimizationResult.getEvaluations().subList(0, 10));
            } else {
                result.put("topParameters", optimizationResult.getEvaluations());
            }

        } catch (Exception e) {
            log.error("回落企稳上涨参数优化失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/optimize-ma-consistency-breakout")
    public Map<String, Object> optimizeMaConsistencyBreakout(
            @RequestParam String tsCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        Map<String, Object> result = new HashMap<>();

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            ParameterOptimizationService.OptimizationResult optimizationResult =
                    parameterOptimizationService.optimizeMaConsistencyBreakout(tsCode, start, end);

            result.put("success", true);
            result.put("strategyType", "ma-consistency-breakout");
            result.put("bestParameters", optimizationResult.getBestParameters());
            result.put("totalEvaluations", optimizationResult.getEvaluations().size());

            // 返回前10个最佳参数
            if (optimizationResult.getEvaluations().size() > 10) {
                result.put("topParameters", optimizationResult.getEvaluations().subList(0, 10));
            } else {
                result.put("topParameters", optimizationResult.getEvaluations());
            }

        } catch (Exception e) {
            log.error("均线粘滞突破参数优化失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/optimize-all-strategies")
    public Map<String, Object> optimizeAllStrategies(
            @RequestParam String tsCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        Map<String, Object> result = new HashMap<>();

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // 优化回落企稳上涨策略
            ParameterOptimizationService.OptimizationResult fsrResult =
                    parameterOptimizationService.optimizeFallStabilizeRise(tsCode, start, end);

            // 优化均线粘滞突破策略
            ParameterOptimizationService.OptimizationResult mcbResult =
                    parameterOptimizationService.optimizeMaConsistencyBreakout(tsCode, start, end);

            result.put("success", true);
            result.put("fallStabilizeRise", fsrResult);
            result.put("maConsistencyBreakout", mcbResult);

        } catch (Exception e) {
            log.error("全策略参数优化失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }
}