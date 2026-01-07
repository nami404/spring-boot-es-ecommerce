package com.ecommerce.es.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author Nami
 * date 2026/1/7 09:23
 * description Jackson配置类
 */
@Configuration
public class JacksonConfig {

    // 方案1：全局配置 Date 类型的格式
    @Bean
    public ObjectMapper dateObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置 Date 反序列化格式
        objectMapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 忽略未知字段（避免前端传多余字段报错）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
