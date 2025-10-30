package com.jinrong.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FinancialReportDate {
    
    // 财报季度的最后一天
    private static final int[][] QUARTER_END_DAYS = {
        {3, 31},  // Q1: 3月31日
        {6, 30},  // Q2: 6月30日
        {9, 30},  // Q3: 9月30日
        {12, 31}  // Q4: 12月31日
    };
    
    public static List<String> getLastThreeReportDates() {
        List<String> reportDates = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        
        // 获取当前年份和季度
        int currentYear = currentDate.getYear();
        int currentMonth = currentDate.getMonthValue();
        int currentQuarter = (currentMonth - 1) / 3; // 0-3 对应 Q1-Q4
        
        // 从当前季度开始往前推，获取最近的3个财报日
        int quarter = currentQuarter;
        int year = currentYear;
        int count = 0;
        
        while (count < 3) {
            // 如果当前季度还没到财报日，则用上一个季度的
            if (quarter == currentQuarter) {
                LocalDate quarterEnd = LocalDate.of(year, QUARTER_END_DAYS[quarter][0], QUARTER_END_DAYS[quarter][1]);
                if (currentDate.isAfter(quarterEnd)) {
                    // 当前日期已经过了本季度财报日，包含这个季度
                    reportDates.add(quarterEnd.format(formatter));
                    count++;
                }
                // 无论如何都要往前推一个季度
                quarter--;
            } else {
                // 处理其他季度
                LocalDate quarterEnd = LocalDate.of(year, QUARTER_END_DAYS[quarter][0], QUARTER_END_DAYS[quarter][1]);
                reportDates.add(quarterEnd.format(formatter));
                count++;
                quarter--;
            }
            
            // 处理跨年
            if (quarter < 0) {
                quarter = 3; // 回到Q4
                year--;
            }
        }
        
        return reportDates;
    }
    
    // 另一种实现方式：更简洁的方法
    public static List<String> getLastThreeReportDatesSimple() {
        List<String> reportDates = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        
        // 确定最近的财报季度
        int currentMonth = currentDate.getMonthValue();
        int currentQuarter = (currentMonth - 1) / 3;
        int startQuarter = currentQuarter;
        int startYear = currentDate.getYear();
        
        // 如果当前日期在当前季度财报日之前，则从上一个季度开始
        LocalDate currentQuarterEnd = LocalDate.of(startYear, 
            QUARTER_END_DAYS[startQuarter][0], QUARTER_END_DAYS[startQuarter][1]);
        if (currentDate.isBefore(currentQuarterEnd) || currentDate.isEqual(currentQuarterEnd)) {
            startQuarter--;
            if (startQuarter < 0) {
                startQuarter = 3;
                startYear--;
            }
        }
        
        // 获取最近的3个财报日
        int quarter = startQuarter;
        int year = startYear;
        for (int i = 0; i < 3; i++) {
            LocalDate reportDate = LocalDate.of(year, 
                QUARTER_END_DAYS[quarter][0], QUARTER_END_DAYS[quarter][1]);
            reportDates.add(reportDate.format(formatter));
            
            // 移动到上一个季度
            quarter--;
            if (quarter < 0) {
                quarter = 3;
                year--;
            }
        }
        
        return reportDates;
    }
    
    public static void main(String[] args) {
        // 测试当前日期
        System.out.println("当前日期: " + LocalDate.now());
        System.out.println("最近3个财报日: " + getLastThreeReportDatesSimple());
        
        // 测试特定日期（1月2日）
        LocalDate testDate = LocalDate.of(2024, 1, 2);
        System.out.println("\n测试日期: " + testDate);
        System.out.println("最近3个财报日: " + getLastThreeReportDatesForDate(testDate));
    }
    
    // 为指定日期获取财报日的方法
    public static List<String> getLastThreeReportDatesForDate(LocalDate date) {
        List<String> reportDates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        
        int currentMonth = date.getMonthValue();
        int currentQuarter = (currentMonth - 1) / 3;
        int startQuarter = currentQuarter;
        int startYear = date.getYear();
        
        // 如果指定日期在当前季度财报日之前，则从上一个季度开始
        LocalDate currentQuarterEnd = LocalDate.of(startYear, 
            QUARTER_END_DAYS[startQuarter][0], QUARTER_END_DAYS[startQuarter][1]);
        if (date.isBefore(currentQuarterEnd) || date.isEqual(currentQuarterEnd)) {
            startQuarter--;
            if (startQuarter < 0) {
                startQuarter = 3;
                startYear--;
            }
        }
        
        // 获取最近的3个财报日
        int quarter = startQuarter;
        int year = startYear;
        for (int i = 0; i < 3; i++) {
            LocalDate reportDate = LocalDate.of(year, 
                QUARTER_END_DAYS[quarter][0], QUARTER_END_DAYS[quarter][1]);
            reportDates.add(reportDate.format(formatter));
            
            // 移动到上一个季度
            quarter--;
            if (quarter < 0) {
                quarter = 3;
                year--;
            }
        }
        
        return reportDates;
    }
}