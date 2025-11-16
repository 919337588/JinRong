package com.jinrong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 股票技术指标数据表
 * </p>
 *
 * @author example
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("stock_technical_indicators")
public class StockTechnicalIndicators implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;


    /**
     * 股票代码
     */
    @TableField("ts_code")
    private String tsCode;

    /**
     * 交易日期
     */
    @TableField("trade_date")
    private LocalDate tradeDate;

    /**
     * 开盘价
     */
    private Double open;

    /**
     * 开盘价（后复权）
     */
    @TableField("open_hfq")
    private Double openHfq;

    /**
     * 开盘价（前复权）
     */
    @TableField("open_qfq")
    private Double openQfq;

    /**
     * 最高价
     */
    private Double high;

    /**
     * 最高价（后复权）
     */
    @TableField("high_hfq")
    private Double highHfq;

    /**
     * 最高价（前复权）
     */
    @TableField("high_qfq")
    private Double highQfq;

    /**
     * 最低价
     */
    private Double low;

    /**
     * 最低价（后复权）
     */
    @TableField("low_hfq")
    private Double lowHfq;

    /**
     * 最低价（前复权）
     */
    @TableField("low_qfq")
    private Double lowQfq;

    /**
     * 收盘价
     */
    private Double close;

    /**
     * 收盘价（后复权）
     */
    @TableField("close_hfq")
    private Double closeHfq;

    /**
     * 收盘价（前复权）
     */
    @TableField("close_qfq")
    private Double closeQfq;

    /**
     * 昨收价(前复权)--为daily接口的pre_close,以当时复权因子计算值跟前一日close_qfq对不上，可不用
     */
    @TableField("pre_close")
    private Double preClose;

    /**
     * 涨跌额
     */
    @TableField("`change`")
    private Double change;

    /**
     * 涨跌幅 （未复权，如果是复权请用 通用行情接口 ）
     */
    @TableField("pct_chg")
    private Double pctChg;

    /**
     * 成交量 （手）
     */
    private Double vol;

    /**
     * 成交额 （千元）
     */
    private Double amount;

    /**
     * 换手率（%）
     */
    @TableField("turnover_rate")
    private Double turnoverRate;

    /**
     * 换手率（自由流通股）
     */
    @TableField("turnover_rate_f")
    private Double turnoverRateF;

    /**
     * 量比
     */
    @TableField("volume_ratio")
    private Double volumeRatio;

    /**
     * 市盈率（总市值/净利润， 亏损的PE为空）
     */
    private Double pe;

    /**
     * 市盈率（TTM，亏损的PE为空）
     */
    @TableField("pe_ttm")
    private Double peTtm;

    /**
     * 市净率（总市值/净资产）
     */
    private Double pb;

    /**
     * 市销率
     */
    private Double ps;

    /**
     * 市销率（TTM）
     */
    @TableField("ps_ttm")
    private Double psTtm;

    /**
     * 股息率 （%）
     */
    @TableField("dv_ratio")
    private Double dvRatio;

    /**
     * 股息率（TTM）（%）
     */
    @TableField("dv_ttm")
    private Double dvTtm;

    /**
     * 总股本 （万股）
     */
    @TableField("total_share")
    private Double totalShare;

    /**
     * 流通股本 （万股）
     */
    @TableField("float_share")
    private Double floatShare;

    /**
     * 自由流通股本 （万）
     */
    @TableField("free_share")
    private Double freeShare;

    /**
     * 总市值 （万元）
     */
    @TableField("total_mv")
    private Double totalMv;

    /**
     * 流通市值（万元）
     */
    @TableField("circ_mv")
    private Double circMv;

    /**
     * 复权因子
     */
    @TableField("adj_factor")
    private Double adjFactor;

    /**
     * 振动升降指标-OPEN, CLOSE, HIGH, LOW, M1=26, M2=10
     */
    @TableField("asi_bfq")
    private Double asiBfq;

    /**
     * 振动升降指标-OPEN, CLOSE, HIGH, LOW, M1=26, M2=10
     */
    @TableField("asi_hfq")
    private Double asiHfq;

    /**
     * 振动升降指标-OPEN, CLOSE, HIGH, LOW, M1=26, M2=10
     */
    @TableField("asi_qfq")
    private Double asiQfq;

    /**
     * 振动升降指标-OPEN, CLOSE, HIGH, LOW, M1=26, M2=10
     */
    @TableField("asit_bfq")
    private Double asitBfq;

    /**
     * 振动升降指标-OPEN, CLOSE, HIGH, LOW, M1=26, M2=10
     */
    @TableField("asit_hfq")
    private Double asitHfq;

    /**
     * 振动升降指标-OPEN, CLOSE, HIGH, LOW, M1=26, M2=10
     */
    @TableField("asit_qfq")
    private Double asitQfq;

    /**
     * 真实波动N日平均值-CLOSE, HIGH, LOW, N=20
     */
    @TableField("atr_bfq")
    private Double atrBfq;

    /**
     * 真实波动N日平均值-CLOSE, HIGH, LOW, N=20
     */
    @TableField("atr_hfq")
    private Double atrHfq;

    /**
     * 真实波动N日平均值-CLOSE, HIGH, LOW, N=20
     */
    @TableField("atr_qfq")
    private Double atrQfq;

    /**
     * BBI多空指标-CLOSE, M1=3, M2=6, M3=12, M4=20
     */
    @TableField("bbi_bfq")
    private Double bbiBfq;

    /**
     * BBI多空指标-CLOSE, M1=3, M2=6, M3=12, M4=21
     */
    @TableField("bbi_hfq")
    private Double bbiHfq;

    /**
     * BBI多空指标-CLOSE, M1=3, M2=6, M3=12, M4=22
     */
    @TableField("bbi_qfq")
    private Double bbiQfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias1_bfq")
    private Double bias1Bfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias1_hfq")
    private Double bias1Hfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias1_qfq")
    private Double bias1Qfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias2_bfq")
    private Double bias2Bfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias2_hfq")
    private Double bias2Hfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias2_qfq")
    private Double bias2Qfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias3_bfq")
    private Double bias3Bfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias3_hfq")
    private Double bias3Hfq;

    /**
     * BIAS乖离率-CLOSE, L1=6, L2=12, L3=24
     */
    @TableField("bias3_qfq")
    private Double bias3Qfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_lower_bfq")
    private Double bollLowerBfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_lower_hfq")
    private Double bollLowerHfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_lower_qfq")
    private Double bollLowerQfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_mid_bfq")
    private Double bollMidBfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_mid_hfq")
    private Double bollMidHfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_mid_qfq")
    private Double bollMidQfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_upper_bfq")
    private Double bollUpperBfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_upper_hfq")
    private Double bollUpperHfq;

    /**
     * BOLL指标，布林带-CLOSE, N=20, P=2
     */
    @TableField("boll_upper_qfq")
    private Double bollUpperQfq;

    /**
     * BRAR情绪指标-OPEN, CLOSE, HIGH, LOW, M1=26
     */
    @TableField("brar_ar_bfq")
    private Double brarArBfq;

    /**
     * BRAR情绪指标-OPEN, CLOSE, HIGH, LOW, M1=26
     */
    @TableField("brar_ar_hfq")
    private Double brarArHfq;

    /**
     * BRAR情绪指标-OPEN, CLOSE, HIGH, LOW, M1=26
     */
    @TableField("brar_ar_qfq")
    private Double brarArQfq;

    /**
     * BRAR情绪指标-OPEN, CLOSE, HIGH, LOW, M1=26
     */
    @TableField("brar_br_bfq")
    private Double brarBrBfq;

    /**
     * BRAR情绪指标-OPEN, CLOSE, HIGH, LOW, M1=26
     */
    @TableField("brar_br_hfq")
    private Double brarBrHfq;

    /**
     * BRAR情绪指标-OPEN, CLOSE, HIGH, LOW, M1=26
     */
    @TableField("brar_br_qfq")
    private Double brarBrQfq;

    /**
     * 顺势指标又叫CCI指标-CLOSE, HIGH, LOW, N=14
     */
    @TableField("cci_bfq")
    private Double cciBfq;

    /**
     * 顺势指标又叫CCI指标-CLOSE, HIGH, LOW, N=14
     */
    @TableField("cci_hfq")
    private Double cciHfq;

    /**
     * 顺势指标又叫CCI指标-CLOSE, HIGH, LOW, N=14
     */
    @TableField("cci_qfq")
    private Double cciQfq;

    /**
     * CR价格动量指标-CLOSE, HIGH, LOW, N=20
     */
    @TableField("cr_bfq")
    private Double crBfq;

    /**
     * CR价格动量指标-CLOSE, HIGH, LOW, N=20
     */
    @TableField("cr_hfq")
    private Double crHfq;

    /**
     * CR价格动量指标-CLOSE, HIGH, LOW, N=20
     */
    @TableField("cr_qfq")
    private Double crQfq;

    /**
     * 平行线差指标-CLOSE, N1=10, N2=50, M=10
     */
    @TableField("dfma_dif_bfq")
    private Double dfmaDifBfq;

    /**
     * 平行线差指标-CLOSE, N1=10, N2=50, M=10
     */
    @TableField("dfma_dif_hfq")
    private Double dfmaDifHfq;

    /**
     * 平行线差指标-CLOSE, N1=10, N2=50, M=10
     */
    @TableField("dfma_dif_qfq")
    private Double dfmaDifQfq;

    /**
     * 平行线差指标-CLOSE, N1=10, N2=50, M=10
     */
    @TableField("dfma_difma_bfq")
    private Double dfmaDifmaBfq;

    /**
     * 平行线差指标-CLOSE, N1=10, N2=50, M=10
     */
    @TableField("dfma_difma_hfq")
    private Double dfmaDifmaHfq;

    /**
     * 平行线差指标-CLOSE, N1=10, N2=50, M=10
     */
    @TableField("dfma_difma_qfq")
    private Double dfmaDifmaQfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_adx_bfq")
    private Double dmiAdxBfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_adx_hfq")
    private Double dmiAdxHfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_adx_qfq")
    private Double dmiAdxQfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_adxr_bfq")
    private Double dmiAdxrBfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_adxr_hfq")
    private Double dmiAdxrHfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_adxr_qfq")
    private Double dmiAdxrQfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_mdi_bfq")
    private Double dmiMdiBfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_mdi_hfq")
    private Double dmiMdiHfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_mdi_qfq")
    private Double dmiMdiQfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_pdi_bfq")
    private Double dmiPdiBfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_pdi_hfq")
    private Double dmiPdiHfq;

    /**
     * 动向指标-CLOSE, HIGH, LOW, M1=14, M2=6
     */
    @TableField("dmi_pdi_qfq")
    private Double dmiPdiQfq;

    /**
     * 连跌天数
     */
    private Double downdays;

    /**
     * 连涨天数
     */
    private Double updays;

    /**
     * 区间震荡线-CLOSE, M1=20, M2=10, M3=6
     */
    @TableField("dpo_bfq")
    private Double dpoBfq;

    /**
     * 区间震荡线-CLOSE, M1=20, M2=10, M3=6
     */
    @TableField("dpo_hfq")
    private Double dpoHfq;

    /**
     * 区间震荡线-CLOSE, M1=20, M2=10, M3=6
     */
    @TableField("dpo_qfq")
    private Double dpoQfq;

    /**
     * 区间震荡线-CLOSE, M1=20, M2=10, M3=6
     */
    @TableField("madpo_bfq")
    private Double madpoBfq;

    /**
     * 区间震荡线-CLOSE, M1=20, M2=10, M3=6
     */
    @TableField("madpo_hfq")
    private Double madpoHfq;

    /**
     * 区间震荡线-CLOSE, M1=20, M2=10, M3=6
     */
    @TableField("madpo_qfq")
    private Double madpoQfq;

    /**
     * 指数移动平均-N=10
     */
    @TableField("ema_bfq_10")
    private Double emaBfq10;

    /**
     * 指数移动平均-N=20
     */
    @TableField("ema_bfq_20")
    private Double emaBfq20;

    /**
     * 指数移动平均-N=250
     */
    @TableField("ema_bfq_250")
    private Double emaBfq250;

    /**
     * 指数移动平均-N=30
     */
    @TableField("ema_bfq_30")
    private Double emaBfq30;

    /**
     * 指数移动平均-N=5
     */
    @TableField("ema_bfq_5")
    private Double emaBfq5;

    /**
     * 指数移动平均-N=60
     */
    @TableField("ema_bfq_60")
    private Double emaBfq60;

    /**
     * 指数移动平均-N=90
     */
    @TableField("ema_bfq_90")
    private Double emaBfq90;

    /**
     * 指数移动平均-N=10
     */
    @TableField("ema_hfq_10")
    private Double emaHfq10;

    /**
     * 指数移动平均-N=20
     */
    @TableField("ema_hfq_20")
    private Double emaHfq20;

    /**
     * 指数移动平均-N=250
     */
    @TableField("ema_hfq_250")
    private Double emaHfq250;

    /**
     * 指数移动平均-N=30
     */
    @TableField("ema_hfq_30")
    private Double emaHfq30;

    /**
     * 指数移动平均-N=5
     */
    @TableField("ema_hfq_5")
    private Double emaHfq5;

    /**
     * 指数移动平均-N=60
     */
    @TableField("ema_hfq_60")
    private Double emaHfq60;

    /**
     * 指数移动平均-N=90
     */
    @TableField("ema_hfq_90")
    private Double emaHfq90;

    /**
     * 指数移动平均-N=10
     */
    @TableField("ema_qfq_10")
    private Double emaQfq10;

    /**
     * 指数移动平均-N=20
     */
    @TableField("ema_qfq_20")
    private Double emaQfq20;

    /**
     * 指数移动平均-N=250
     */
    @TableField("ema_qfq_250")
    private Double emaQfq250;

    /**
     * 指数移动平均-N=30
     */
    @TableField("ema_qfq_30")
    private Double emaQfq30;

    /**
     * 指数移动平均-N=5
     */
    @TableField("ema_qfq_5")
    private Double emaQfq5;

    /**
     * 指数移动平均-N=60
     */
    @TableField("ema_qfq_60")
    private Double emaQfq60;

    /**
     * 指数移动平均-N=90
     */
    @TableField("ema_qfq_90")
    private Double emaQfq90;

    /**
     * 简易波动指标-HIGH, LOW, VOL, N=14, M=9
     */
    @TableField("emv_bfq")
    private Double emvBfq;

    /**
     * 简易波动指标-HIGH, LOW, VOL, N=14, M=9
     */
    @TableField("emv_hfq")
    private Double emvHfq;

    /**
     * 简易波动指标-HIGH, LOW, VOL, N=14, M=9
     */
    @TableField("emv_qfq")
    private Double emvQfq;

    /**
     * 简易波动指标-HIGH, LOW, VOL, N=14, M=9
     */
    @TableField("maemv_bfq")
    private Double maemvBfq;

    /**
     * 简易波动指标-HIGH, LOW, VOL, N=14, M=9
     */
    @TableField("maemv_hfq")
    private Double maemvHfq;

    /**
     * 简易波动指标-HIGH, LOW, VOL, N=14, M=9
     */
    @TableField("maemv_qfq")
    private Double maemvQfq;

    /**
     * EMA指数平均数指标-CLOSE, N1=12, N2=50
     */
    @TableField("expma_12_bfq")
    private Double expma12Bfq;

    /**
     * EMA指数平均数指标-CLOSE, N1=12, N2=50
     */
    @TableField("expma_12_hfq")
    private Double expma12Hfq;

    /**
     * EMA指数平均数指标-CLOSE, N1=12, N2=50
     */
    @TableField("expma_12_qfq")
    private Double expma12Qfq;

    /**
     * EMA指数平均数指标-CLOSE, N1=12, N2=50
     */
    @TableField("expma_50_bfq")
    private Double expma50Bfq;

    /**
     * EMA指数平均数指标-CLOSE, N1=12, N2=50
     */
    @TableField("expma_50_hfq")
    private Double expma50Hfq;

    /**
     * EMA指数平均数指标-CLOSE, N1=12, N2=50
     */
    @TableField("expma_50_qfq")
    private Double expma50Qfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_bfq")
    private Double kdjBfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_hfq")
    private Double kdjHfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_qfq")
    private Double kdjQfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_d_bfq")
    private Double kdjDBfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_d_hfq")
    private Double kdjDHfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_d_qfq")
    private Double kdjDQfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_k_bfq")
    private Double kdjKBfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_k_hfq")
    private Double kdjKHfq;

    /**
     * KDJ指标-CLOSE, HIGH, LOW, N=9, M1=3, M2=3
     */
    @TableField("kdj_k_qfq")
    private Double kdjKQfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_down_bfq")
    private Double ktnDownBfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_down_hfq")
    private Double ktnDownHfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_down_qfq")
    private Double ktnDownQfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_mid_bfq")
    private Double ktnMidBfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_mid_hfq")
    private Double ktnMidHfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_mid_qfq")
    private Double ktnMidQfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_upper_bfq")
    private Double ktnUpperBfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_upper_hfq")
    private Double ktnUpperHfq;

    /**
     * 肯特纳交易通道, N选20日，ATR选10日-CLOSE, HIGH, LOW, N=20, M=10
     */
    @TableField("ktn_upper_qfq")
    private Double ktnUpperQfq;

    /**
     * LOWRANGE(LOW)表示当前最低价是近多少周期内最低价的最小值
     */
    private Double lowdays;

    /**
     * TOPRANGE(HIGH)表示当前最高价是近多少周期内最高价的最大值
     */
    private Double topdays;

    /**
     * 简单移动平均-N=10
     */
    @TableField("ma_bfq_10")
    private Double maBfq10;

    /**
     * 简单移动平均-N=20
     */
    @TableField("ma_bfq_20")
    private Double maBfq20;

    /**
     * 简单移动平均-N=250
     */
    @TableField("ma_bfq_250")
    private Double maBfq250;

    /**
     * 简单移动平均-N=30
     */
    @TableField("ma_bfq_30")
    private Double maBfq30;

    /**
     * 简单移动平均-N=5
     */
    @TableField("ma_bfq_5")
    private Double maBfq5;

    /**
     * 简单移动平均-N=60
     */
    @TableField("ma_bfq_60")
    private Double maBfq60;

    /**
     * 简单移动平均-N=90
     */
    @TableField("ma_bfq_90")
    private Double maBfq90;

    /**
     * 简单移动平均-N=10
     */
    @TableField("ma_hfq_10")
    private Double maHfq10;

    /**
     * 简单移动平均-N=20
     */
    @TableField("ma_hfq_20")
    private Double maHfq20;

    /**
     * 简单移动平均-N=250
     */
    @TableField("ma_hfq_250")
    private Double maHfq250;

    /**
     * 简单移动平均-N=30
     */
    @TableField("ma_hfq_30")
    private Double maHfq30;

    /**
     * 简单移动平均-N=5
     */
    @TableField("ma_hfq_5")
    private Double maHfq5;

    /**
     * 简单移动平均-N=60
     */
    @TableField("ma_hfq_60")
    private Double maHfq60;

    /**
     * 简单移动平均-N=90
     */
    @TableField("ma_hfq_90")
    private Double maHfq90;

    /**
     * 简单移动平均-N=10
     */
    @TableField("ma_qfq_10")
    private Double maQfq10;

    /**
     * 简单移动平均-N=20
     */
    @TableField("ma_qfq_20")
    private Double maQfq20;

    /**
     * 简单移动平均-N=250
     */
    @TableField("ma_qfq_250")
    private Double maQfq250;

    /**
     * 简单移动平均-N=30
     */
    @TableField("ma_qfq_30")
    private Double maQfq30;

    /**
     * 简单移动平均-N=5
     */
    @TableField("ma_qfq_5")
    private Double maQfq5;

    /**
     * 简单移动平均-N=60
     */
    @TableField("ma_qfq_60")
    private Double maQfq60;

    /**
     * 简单移动平均-N=90
     */
    @TableField("ma_qfq_90")
    private Double maQfq90;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_bfq")
    private Double macdBfq;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_hfq")
    private Double macdHfq;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_qfq")
    private Double macdQfq;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_dea_bfq")
    private Double macdDeaBfq;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_dea_hfq")
    private Double macdDeaHfq;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_dea_qfq")
    private Double macdDeaQfq;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_dif_bfq")
    private Double macdDifBfq;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_dif_hfq")
    private Double macdDifHfq;

    /**
     * MACD指标-CLOSE, SHORT=12, LONG=26, M=9
     */
    @TableField("macd_dif_qfq")
    private Double macdDifQfq;

    /**
     * 梅斯线-HIGH, LOW, N1=9, N2=25, M=6
     */
    @TableField("mass_bfq")
    private Double massBfq;

    /**
     * 梅斯线-HIGH, LOW, N1=9, N2=25, M=6
     */
    @TableField("mass_hfq")
    private Double massHfq;

    /**
     * 梅斯线-HIGH, LOW, N1=9, N2=25, M=6
     */
    @TableField("mass_qfq")
    private Double massQfq;

    /**
     * 梅斯线-HIGH, LOW, N1=9, N2=25, M=6
     */
    @TableField("ma_mass_bfq")
    private Double maMassBfq;

    /**
     * 梅斯线-HIGH, LOW, N1=9, N2=25, M=6
     */
    @TableField("ma_mass_hfq")
    private Double maMassHfq;

    /**
     * 梅斯线-HIGH, LOW, N1=9, N2=25, M=6
     */
    @TableField("ma_mass_qfq")
    private Double maMassQfq;

    /**
     * MFI指标是成交量的RSI指标-CLOSE, HIGH, LOW, VOL, N=14
     */
    @TableField("mfi_bfq")
    private Double mfiBfq;

    /**
     * MFI指标是成交量的RSI指标-CLOSE, HIGH, LOW, VOL, N=14
     */
    @TableField("mfi_hfq")
    private Double mfiHfq;

    /**
     * MFI指标是成交量的RSI指标-CLOSE, HIGH, LOW, VOL, N=14
     */
    @TableField("mfi_qfq")
    private Double mfiQfq;

    /**
     * 动量指标-CLOSE, N=12, M=6
     */
    @TableField("mtm_bfq")
    private Double mtmBfq;

    /**
     * 动量指标-CLOSE, N=12, M=6
     */
    @TableField("mtm_hfq")
    private Double mtmHfq;

    /**
     * 动量指标-CLOSE, N=12, M=6
     */
    @TableField("mtm_qfq")
    private Double mtmQfq;

    /**
     * 动量指标-CLOSE, N=12, M=6
     */
    @TableField("mtmma_bfq")
    private Double mtmmaBfq;

    /**
     * 动量指标-CLOSE, N=12, M=6
     */
    @TableField("mtmma_hfq")
    private Double mtmmaHfq;

    /**
     * 动量指标-CLOSE, N=12, M=6
     */
    @TableField("mtmma_qfq")
    private Double mtmmaQfq;

    /**
     * 能量潮指标-CLOSE, VOL
     */
    @TableField("obv_bfq")
    private Double obvBfq;

    /**
     * 能量潮指标-CLOSE, VOL
     */
    @TableField("obv_hfq")
    private Double obvHfq;

    /**
     * 能量潮指标-CLOSE, VOL
     */
    @TableField("obv_qfq")
    private Double obvQfq;

    /**
     * 投资者对股市涨跌产生心理波动的情绪指标-CLOSE, N=12, M=6
     */
    @TableField("psy_bfq")
    private Double psyBfq;

    /**
     * 投资者对股市涨跌产生心理波动的情绪指标-CLOSE, N=12, M=6
     */
    @TableField("psy_hfq")
    private Double psyHfq;

    /**
     * 投资者对股市涨跌产生心理波动的情绪指标-CLOSE, N=12, M=6
     */
    @TableField("psy_qfq")
    private Double psyQfq;

    /**
     * 投资者对股市涨跌产生心理波动的情绪指标-CLOSE, N=12, M=6
     */
    @TableField("psyma_bfq")
    private Double psymaBfq;

    /**
     * 投资者对股市涨跌产生心理波动的情绪指标-CLOSE, N=12, M=6
     */
    @TableField("psyma_hfq")
    private Double psymaHfq;

    /**
     * 投资者对股市涨跌产生心理波动的情绪指标-CLOSE, N=12, M=6
     */
    @TableField("psyma_qfq")
    private Double psymaQfq;

    /**
     * 变动率指标-CLOSE, N=12, M=6
     */
    @TableField("roc_bfq")
    private Double rocBfq;

    /**
     * 变动率指标-CLOSE, N=12, M=6
     */
    @TableField("roc_hfq")
    private Double rocHfq;

    /**
     * 变动率指标-CLOSE, N=12, M=6
     */
    @TableField("roc_qfq")
    private Double rocQfq;

    /**
     * 变动率指标-CLOSE, N=12, M=6
     */
    @TableField("maroc_bfq")
    private Double marocBfq;

    /**
     * 变动率指标-CLOSE, N=12, M=6
     */
    @TableField("maroc_hfq")
    private Double marocHfq;

    /**
     * 变动率指标-CLOSE, N=12, M=6
     */
    @TableField("maroc_qfq")
    private Double marocQfq;

    /**
     * RSI指标-CLOSE, N=12
     */
    @TableField("rsi_bfq_12")
    private Double rsiBfq12;

    /**
     * RSI指标-CLOSE, N=24
     */
    @TableField("rsi_bfq_24")
    private Double rsiBfq24;

    /**
     * RSI指标-CLOSE, N=6
     */
    @TableField("rsi_bfq_6")
    private Double rsiBfq6;

    /**
     * RSI指标-CLOSE, N=12
     */
    @TableField("rsi_hfq_12")
    private Double rsiHfq12;

    /**
     * RSI指标-CLOSE, N=24
     */
    @TableField("rsi_hfq_24")
    private Double rsiHfq24;

    /**
     * RSI指标-CLOSE, N=6
     */
    @TableField("rsi_hfq_6")
    private Double rsiHfq6;

    /**
     * RSI指标-CLOSE, N=12
     */
    @TableField("rsi_qfq_12")
    private Double rsiQfq12;

    /**
     * RSI指标-CLOSE, N=24
     */
    @TableField("rsi_qfq_24")
    private Double rsiQfq24;

    /**
     * RSI指标-CLOSE, N=6
     */
    @TableField("rsi_qfq_6")
    private Double rsiQfq6;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_down_bfq")
    private Double taqDownBfq;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_down_hfq")
    private Double taqDownHfq;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_down_qfq")
    private Double taqDownQfq;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_mid_bfq")
    private Double taqMidBfq;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_mid_hfq")
    private Double taqMidHfq;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_mid_qfq")
    private Double taqMidQfq;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_up_bfq")
    private Double taqUpBfq;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_up_hfq")
    private Double taqUpHfq;

    /**
     * 唐安奇通道(海龟)交易指标-HIGH, LOW, 20
     */
    @TableField("taq_up_qfq")
    private Double taqUpQfq;

    /**
     * 三重指数平滑平均线-CLOSE, M1=12, M2=20
     */
    @TableField("trix_bfq")
    private Double trixBfq;

    /**
     * 三重指数平滑平均线-CLOSE, M1=12, M2=20
     */
    @TableField("trix_hfq")
    private Double trixHfq;

    /**
     * 三重指数平滑平均线-CLOSE, M1=12, M2=20
     */
    @TableField("trix_qfq")
    private Double trixQfq;

    /**
     * 三重指数平滑平均线-CLOSE, M1=12, M2=20
     */
    @TableField("trma_bfq")
    private Double trmaBfq;

    /**
     * 三重指数平滑平均线-CLOSE, M1=12, M2=20
     */
    @TableField("trma_hfq")
    private Double trmaHfq;

    /**
     * 三重指数平滑平均线-CLOSE, M1=12, M2=20
     */
    @TableField("trma_qfq")
    private Double trmaQfq;

    /**
     * VR容量比率-CLOSE, VOL, M1=26
     */
    @TableField("vr_bfq")
    private Double vrBfq;

    /**
     * VR容量比率-CLOSE, VOL, M1=26
     */
    @TableField("vr_hfq")
    private Double vrHfq;

    /**
     * VR容量比率-CLOSE, VOL, M1=26
     */
    @TableField("vr_qfq")
    private Double vrQfq;

    /**
     * W&R 威廉指标-CLOSE, HIGH, LOW, N=10, N1=6
     */
    @TableField("wr_bfq")
    private Double wrBfq;

    /**
     * W&R 威廉指标-CLOSE, HIGH, LOW, N=10, N1=6
     */
    @TableField("wr_hfq")
    private Double wrHfq;

    /**
     * W&R 威廉指标-CLOSE, HIGH, LOW, N=10, N1=6
     */
    @TableField("wr_qfq")
    private Double wrQfq;

    /**
     * W&R 威廉指标-CLOSE, HIGH, LOW, N=10, N1=6
     */
    @TableField("wr1_bfq")
    private Double wr1Bfq;

    /**
     * W&R 威廉指标-CLOSE, HIGH, LOW, N=10, N1=6
     */
    @TableField("wr1_hfq")
    private Double wr1Hfq;

    /**
     * W&R 威廉指标-CLOSE, HIGH, LOW, N=10, N1=6
     */
    @TableField("wr1_qfq")
    private Double wr1Qfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td1_bfq")
    private Double xsiiTd1Bfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td1_hfq")
    private Double xsiiTd1Hfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td1_qfq")
    private Double xsiiTd1Qfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td2_bfq")
    private Double xsiiTd2Bfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td2_hfq")
    private Double xsiiTd2Hfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td2_qfq")
    private Double xsiiTd2Qfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td3_bfq")
    private Double xsiiTd3Bfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td3_hfq")
    private Double xsiiTd3Hfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td3_qfq")
    private Double xsiiTd3Qfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td4_bfq")
    private Double xsiiTd4Bfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td4_hfq")
    private Double xsiiTd4Hfq;

    /**
     * 薛斯通道II-CLOSE, HIGH, LOW, N=102, M=7
     */
    @TableField("xsii_td4_qfq")
    private Double xsiiTd4Qfq;

    /**
     * 创建时间
     */
    @TableField("created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField("updated_time")
    private LocalDateTime updatedTime;
}