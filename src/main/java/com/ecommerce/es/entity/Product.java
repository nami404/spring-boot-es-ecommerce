package com.ecommerce.es.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * author Nami
 * date 2026/1/5 15:43
 * description 商品实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    /** 商品ID（ES文档ID，电商唯一标识） */
    private String id;
    /** 商品名称（分词搜索） */
    private String productName;
    /** 商品分类（聚合/筛选，不分词） */
    private String category;
    /** 商品子分类（多级聚合） */
    private String subCategory;
    /** 商品价格（范围筛选/排序） */
    private BigDecimal price;
    /** 库存（电商库存管控） */
    private Integer stock;
    /** 销量（排序/聚合） */
    private Integer sales;
    /** 商品标签（如：新品、包邮、爆款） */
    private List<String> tags;
    /** 上架时间（时间范围筛选） */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",  // 匹配你的日期字符串格式
            timezone = "GMT+8"               // 必须指定时区，避免解析偏移
    )
    private Date createTime;
    /** 商品描述（分词搜索） */
    private String description;
    /** 商家ID（多商家场景） */
    private String merchantId;
    /** 商品评分（排序/筛选） */
    private Double score;
}
