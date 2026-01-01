package com.thinkinghelp.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.thinkinghelp.system.mapper")
public class ThinkingHelpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThinkingHelpApplication.class, args);
    }

}
