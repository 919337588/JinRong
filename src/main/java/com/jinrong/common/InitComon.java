package com.jinrong.common;

import com.jinrong.entity.StockDailyBasic;
import com.jinrong.util.ShortUUID;
import io.micrometer.common.util.StringUtils;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class InitComon<T> {
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @SneakyThrows
    public List<T> parse(Class c, List<HashMap<String, Object>> dailyBasic) {
        Field id = c.getDeclaredField("id");
        id.setAccessible(true);
        Field[] declaredFields = c.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
        }

        List<T> collect = dailyBasic.stream().map(item -> {
                    T gupiao = null;
                    try {
                        gupiao = (T) c.newInstance();
                        if (id.getType() == String.class) {
                            id.set(gupiao, ShortUUID.compressToBase62());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    for (Field declaredField : Arrays.stream(declaredFields).filter(v->!"id".equals(v.getName())).toList()) {
                        try {
                            Object o = item.get(declaredField.getName());
                            if(o==null){
                                continue;
                            }
                            if (declaredField.getType() == LocalDate.class) {
                                if (o instanceof String && StringUtils.isNotBlank((String) o)) {
                                    LocalDate localDate = LocalDate.parse((String) o, formatter);
                                    declaredField.set(gupiao, localDate);
                                }
                            } else if (declaredField.getType() == Double.class) {
                                if (o instanceof String && StringUtils.isNotBlank((String) o)) {
                                    declaredField.set(gupiao, Double.valueOf((String) o));
                                }
                            } else if (declaredField.getType() == BigDecimal.class) {
                                if (o instanceof String && StringUtils.isNotBlank((String) o)) {
                                    declaredField.set(gupiao, new BigDecimal((String) o));
                                }
                            } else if (declaredField.getType() == Long.class) {
                                if (o instanceof String && StringUtils.isNotBlank((String) o)) {
                                    declaredField.set(gupiao, Long.valueOf((String) o));
                                }
                            } else {
                                    declaredField.set(gupiao, o);
                            }

                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return gupiao;
                }
        ).collect(Collectors.toList());
        return collect;
    }

}
