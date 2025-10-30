
select valuation.ts_code
     , valuation.name
                                                                            #      , stock_daily_basic.close                                       收盘价
     , stock_basic.industry                                          行业
     ,stock_daily_basic.dv_ttm                                      股息
     ,financial_socre.end_date   财报日
     , ROUND(total_mv / incomed, 2)                                  一年后pe
     , ROUND(total_mv / incomedv2, 2)                                两年后年底pe
     , ROUND(income_finished_ratio * (total_mv / incomed) / hlpe, 2) 估值分位
     , ROUND(total_mv / reason_market_val, 2)                               估值2
     , income_increate_percentage                                    预计年利润增长
     , ROUND(((1 - income_finished_ratio * (total_mv / incomed) / hlpe) * 100 + income_increate_percentage) *
             income_finished_ratio, 2)                               一年后可能涨幅
     , valuation.income_finished_ratio                               今年实际业绩与预计业绩比值
    #      , hlpe                                                          合理pe
    #      , pettmz                                                        过去平均pe
    #      , financial_socre.roe
     , f_score                                                       财务分
     , px.response_msg                                               评星
     , syfx.response_msg                                             商业分析
     ,q_gr_yoy 营业总收入同比增长率_单季度
     ,q_profit_yoy 净利润同比增长率_单季度
     ,or_yoy 营业收入同比增长率
     ,ebt_yoy 利润总额同比增长率
    #      , financial_socre.detail                                                                  评分详情
from valuation
         inner join financial_socre
                    on financial_socre.ts_code = valuation.ts_code and
                       financial_socre.end_date = (select end_date from financial_socre fs where fs.ts_code=valuation.ts_code order by fs.end_date desc limit 1)
    inner join stock_basic on stock_basic.ts_code = valuation.ts_code
    inner join fin_indicator on fin_indicator.ts_code = valuation.ts_code and
    fin_indicator.end_date =(select end_date from fin_indicator fi where  fi.ts_code = valuation.ts_code order by fi.end_date desc limit 1)
    inner join stock_daily_basic
    on stock_daily_basic.ts_code = valuation.ts_code and stock_daily_basic.trade_date = (select max(trade_date) from stock_daily_basic)
    left join manual_mark on manual_mark.ts_code = valuation.ts_code and manual_mark.type = 'mbm'
    left join ai_request_log syfx on syfx.ts_code = valuation.ts_code and syfx.request_format_id = 'syfx'
    left join ai_request_log px on px.ts_code = valuation.ts_code and px.request_format_id = 'askxinji'
where market in ("主板", "创业板")
  and valuation.date = (select max(date) from valuation)
  and  ROUND(income_finished_ratio * (total_mv / incomed) / hlpe, 2) between 0 and 0.61
  and total_mv / incomedv2 < 16
  and cr > 1
  and income_increate_percentage >= 10
  and valuation.f_score > 75
  and financial_socre.roe >= 0.10
  and income_finished_ratio >= 0.85
  and (q_gr_yoy > -5 or q_profit_yoy >= 10)
  and (or_yoy >= -5  or ebt_yoy >= 10)
  and total_mv<safe_margin*1.2
    #     and valuation.name like "%浙江%"
    #   and (manual_mark.mark in ("1", "2") or manual_mark.mark is null)


order by 一年后可能涨幅 desc
