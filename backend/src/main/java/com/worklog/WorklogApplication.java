package com.worklog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.worklog.mapper")
public class WorklogApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorklogApplication.class, args);
    }
}
