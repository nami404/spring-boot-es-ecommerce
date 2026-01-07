package com.ecommerce.es.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.ecommerce.es.entity.Product;
import com.ecommerce.es.repository.ProductEsRepository;
import com.ecommerce.es.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * author Nami
 * date 2026/1/5 15:47
 * description 业务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductEsRepository productEsRepository;

    @Override
    public boolean createProductIndex() throws IOException {
        return productEsRepository.createProductIndex();
    }

    @Override
    public boolean deleteProductIndex() throws IOException {
        return productEsRepository.deleteProductIndex();
    }

    @Override
    public String saveProduct(Product product) throws IOException {
        // 电商业务校验：商品ID/名称非空
        if (product.getId() == null || product.getProductName() == null) {
            throw new IllegalArgumentException("商品ID和名称不能为空");
        }
        return productEsRepository.saveProduct(product);
    }

    @Override
    public String batchSaveProduct(List<Product> productList) throws IOException {
        // 电商批量校验：列表非空、商品ID/名称非空
        if (productList == null || productList.isEmpty()) {
            throw new IllegalArgumentException("批量新增商品列表不能为空");
        }
        for (Product product : productList) {
            if (product.getId() == null || product.getProductName() == null) {
                throw new IllegalArgumentException("商品ID和名称不能为空，商品ID：" + product.getId());
            }
        }
        return productEsRepository.batchSaveProduct(productList);
    }

    @Override
    public Product getProductById(String productId) throws IOException {
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        return productEsRepository.getProductById(productId);
    }

    @Override
    public String updateProduct(Product product) throws IOException {
        if (product.getId() == null || product.getProductName() == null) {
            throw new IllegalArgumentException("商品ID和名称不能为空");
        }
        return productEsRepository.updateProduct(product);
    }

    @Override
    public String deleteProductById(String productId) throws IOException {
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        return productEsRepository.deleteProductById(productId);
    }

    @Override
    public List<Product> searchProduct(String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                       String category, List<String> tags, String sortField, SortOrder sortOrder) throws IOException {
        // 电商搜索容错：价格范围校验
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("最低价格不能高于最高价格");
        }
        return productEsRepository.searchProduct(keyword, minPrice, maxPrice, category, tags, sortField, sortOrder);
    }

    @Override
    public Map<String, Long> aggProductByCategory() throws IOException {
        return productEsRepository.aggProductByCategory();
    }

    @Override
    public Map<String, Map<String, Long>> aggProductByCategoryAndSubCategory() throws IOException {
        return productEsRepository.aggProductByCategoryAndSubCategory();
    }
}
