package com.ecommerce.es;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类
 */
@SpringBootApplication
public class EcommerceEsApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceEsApplication.class, args);
        System.out.println("==== 项目启动成功 ====");
    }
}