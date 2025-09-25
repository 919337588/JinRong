package com.jinrong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ComponentScan("com.jinrong.*")
@MapperScan("com.jinrong.mapper")
@EnableScheduling
public class JinRongApplication {

    public static void main(String[] args) {
        SpringApplication.run(JinRongApplication.class, args);
    }

}
