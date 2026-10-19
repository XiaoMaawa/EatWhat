package com.eatwhat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EatWhat 外卖平台启动类。
 */
@SpringBootApplication
@MapperScan("com.eatwhat.modules.**.mapper")
@EnableScheduling
public class EatWhatApplication {

    public static void main(String[] args) {
        SpringApplication.run(EatWhatApplication.class, args);
    }
}
