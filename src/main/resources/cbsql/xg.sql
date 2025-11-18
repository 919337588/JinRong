 select valuation.ts_code
     , valuation.name
     , stock_technical_indicators.close_qfq                                       收盘价
     , stock_basic.industry                   行业
     , stock_technical_indicators.dv_ttm               股息
     , stock_technical_indicators.trade_date            数据日期
     , ROUND(total_mv / incomed, 2)           一年后pe
     , ROUND(total_mv / incomedv2, 2)         两年后年底pe
     , ROUND(pe_ttm / hlpe/income_finished_ratio, 2)                估值分位_现在
     , ROUND(valuation_percentage, 2)         估值分位_一年
     , ROUND(total_mv / reason_market_val, 2) 估值分位_三年
     , income_increate_percentage             预计年利润增长
     , valuation.income_finished_ratio        今年实际业绩与预计业绩比值
#      , hlpe                                                          合理pe
#      , pettmz                                                        过去平均pe
#      , financial_socre.roe
     , f_score                                                                                                         财务分
#       ,syfx.request_msg
     , syfx.response_msg                                                                                               商业分析
     , q_gr_yoy                                                                                                        营业总收入同比增长率_单季度
     , q_profit_yoy                                                                                                    净利润同比增长率_单季度
     , or_yoy                                                                                                          营业收入同比增长率
     , ebt_yoy                                                                                                         利润总额同比增长率

,   CONCAT(stock_technical_indicators.trade_date,"收盘价:",stock_technical_indicators.close_qfq ,"研报预计年利润增长平均值:",income_increate_percentage  ,"% pettm 当前：",stock_technical_indicators.pe_ttm,", 5年中位数：",fy.mean_value,", 10年中位数：",Ty.mean_value) pettm
     , financial_socre.end_date                                                                                        财报日
  ,financial_socre.roe
     , IF(stock_fall_stabilize_rise_analysis.is_stabilized, '是', '否')                                               是否股价启稳
     , IF(stock_fall_stabilize_rise_analysis.is_fall_stabilize_rise_pattern, '是', '否')                              是否启稳上涨信号
     , IF(stock_ma_breakout_analysis.is_consistent, '是', '否')                                                       是否均线粘滞
     , IF(stock_ma_breakout_analysis.is_ma_consistency_breakout, '是', '否')                                          是否均线粘滞放量突破均线信号
#      , financial_socre.detail                                                                  评分详情
from valuation
         inner join financial_socre
                    on financial_socre.ts_code = valuation.ts_code and
                       financial_socre.end_date = (select end_date
                                                   from financial_socre fs
                                                   where fs.ts_code = valuation.ts_code
                                                   order by fs.end_date desc
                                                   limit 1)
         inner join stock_basic on stock_basic.ts_code = valuation.ts_code
         inner join fin_indicator on fin_indicator.ts_code = valuation.ts_code and
                                     fin_indicator.end_date = (select end_date
                                                               from fin_indicator fi
                                                               where fi.ts_code = valuation.ts_code
                                                               order by fi.end_date desc
                                                               limit 1)
         inner join stock_technical_indicators
                    on stock_technical_indicators.ts_code = valuation.ts_code and
                       stock_technical_indicators.trade_date = (select max(trade_date) from stock_technical_indicators)
#          left join manual_mark on manual_mark.ts_code = valuation.ts_code and manual_mark.type = 'mbm'
         inner join stock_ttm_analysis fy on fy.ts_code=valuation.ts_code and fy.analysis_period='5Y'
    and fy.calc_date= (select max(calc_date) from stock_ttm_analysis)
         inner join stock_ttm_analysis ty on ty.ts_code=valuation.ts_code and ty.analysis_period='10Y'
    and ty.calc_date= (select max(calc_date) from stock_ttm_analysis)
         left join ai_request_log syfx on syfx.ts_code = valuation.ts_code and syfx.request_format_id = 'syfx'

         inner join stock_fall_stabilize_rise_analysis on stock_fall_stabilize_rise_analysis.ts_code=valuation.ts_code and stock_fall_stabilize_rise_analysis.trade_date=stock_technical_indicators.trade_date
         inner join stock_ma_breakout_analysis on stock_ma_breakout_analysis.ts_code=valuation.ts_code and stock_ma_breakout_analysis.trade_date=stock_technical_indicators.trade_date

where market in ("主板", "创业板")
  and valuation.date = (select max(date) from valuation)
#   and ROUND(valuation_percentage, 2) between 0 and 0.71
  and ROUND(pe_ttm / hlpe/income_finished_ratio, 2)    between 0 and 1
  and valuation.type='y'
  and total_mv / incomedv2 < 20
  and cr > 1
  and income_increate_percentage >= 10
  and valuation.f_score > 60
  and financial_socre.roe >= 0.10
  and income_finished_ratio >= 0.85
  and (q_gr_yoy > -5 or q_profit_yoy >= 10)
  and (or_yoy >= -5 or ebt_yoy >= 10)
  and total_mv < safe_margin * 1.25
#   and CONVERT(SUBSTRING_INDEX(REPLACE(REPLACE(syfx.response_msg,'\r\n','\n'),'\r','\n'),'\n',1),DECIMAL(10,2))>88
  and syfx.response_msg like '%★★★★★%'
#     and valuation.name like "%阳光电源%"
#   and (manual_mark.mark in ("1", "2") or manual_mark.mark is null)
# and      (stock_fall_stabilize_rise_analysis.is_fall_stabilize_rise_pattern  =1 or  stock_ma_breakout_analysis.is_ma_consistency_breakout   =1)
# and   (stock_fall_stabilize_rise_analysis.is_stabilized  =1 )
order by ROUND(pe_ttm / hlpe/income_finished_ratio, 2)
