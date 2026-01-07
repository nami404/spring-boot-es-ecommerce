package com.ecommerce.es.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.ecommerce.es.entity.Product;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * author Nami
 * date 2026/1/5 15:46
 * description 商品业务接口
 */
public interface ProductService {
    // 索引管理
    boolean createProductIndex() throws IOException;
    boolean deleteProductIndex() throws IOException;

    // CRUD
    String saveProduct(Product product) throws IOException;
    String batchSaveProduct(List<Product> productList) throws IOException;
    Product getProductById(String productId) throws IOException;
    String updateProduct(Product product) throws IOException;
    String deleteProductById(String productId) throws IOException;

    // 搜索
    List<Product> searchProduct(String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                String category, List<String> tags, String sortField, SortOrder sortOrder) throws IOException;

    // 聚合
    Map<String, Long> aggProductByCategory() throws IOException;
    Map<String, Map<String, Long>> aggProductByCategoryAndSubCategory() throws IOException;
}
