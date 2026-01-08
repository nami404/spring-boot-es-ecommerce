//package com.ecommerce.es.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.converter.HttpMessageConverter;
//import org.springframework.http.converter.StringHttpMessageConverter;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//
///**
// * author Nami
// * date 2026/1/7 16:36
// * description
// */
//@Configuration
//public class WebMvcConfig implements WebMvcConfigurer {
//
//    // 替换 StringHttpMessageConverter 的默认编码为 UTF-8
//    @Override
//    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        for (org.springframework.http.converter.HttpMessageConverter<?> converter : converters) {
//            if (converter instanceof StringHttpMessageConverter) {
//                ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8);
//            }
//        }
//    }
//}
