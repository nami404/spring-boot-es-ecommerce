package com.ecommerce.es.controller;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.ecommerce.es.entity.Product;
import com.ecommerce.es.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * author Nami
 * date 2026/1/5 15:45
 * description 商品搜索接口
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Slf4j
public class ProductSearchController {

    private final ProductService productService;

    // ====================== 索引管理接口（电商运维） ======================
    @PostMapping("/index/create")
    public ResponseEntity<String> createProductIndex() {
        try {
            boolean result = productService.createProductIndex();
            return ResponseEntity.ok(result ? "商品索引创建成功（或已存在）" : "商品索引创建失败");
        } catch (Exception e) {
            log.error("创建商品索引失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("创建失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/index/delete")
    public ResponseEntity<String> deleteProductIndex() {
        try {
            boolean result = productService.deleteProductIndex();
            return ResponseEntity.ok(result ? "商品索引删除成功（或不存在）" : "商品索引删除失败");
        } catch (Exception e) {
            log.error("删除商品索引失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败：" + e.getMessage());
        }
    }

    // ====================== 商品CRUD接口（电商业务） ======================
    @PostMapping("/save")
    public ResponseEntity<String> saveProduct(@RequestBody Product product) {
        try {
            String result = productService.saveProduct(product);
            return ResponseEntity.ok("商品新增成功，操作结果：" + result);
        } catch (IllegalArgumentException e) {
            log.error("新增商品参数错误", e);
            return ResponseEntity.badRequest().body("参数错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("新增商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("新增失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch/save")
    public ResponseEntity<String> batchSaveProduct(@RequestBody List<Product> productList) {
        try {
            String result = productService.batchSaveProduct(productList);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("批量新增商品参数错误", e);
            return ResponseEntity.badRequest().body("参数错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("批量新增商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("批量新增失败：" + e.getMessage());
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable String productId) {
        try {
            Product product = productService.getProductById(productId);
            if (product != null) {
                return ResponseEntity.ok(product);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            log.error("查询商品参数错误", e);
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("查询商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateProduct(@RequestBody Product product) {
        try {
            String result = productService.updateProduct(product);
            return ResponseEntity.ok("商品更新成功，操作结果：" + result);
        } catch (IllegalArgumentException e) {
            log.error("更新商品参数错误", e);
            return ResponseEntity.badRequest().body("参数错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("更新商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteProductById(@PathVariable String productId) {
        try {
            String result = productService.deleteProductById(productId);
            return ResponseEntity.ok("商品删除成功，操作结果：" + result);
        } catch (IllegalArgumentException e) {
            log.error("删除商品参数错误", e);
            return ResponseEntity.badRequest().body("参数错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("删除商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败：" + e.getMessage());
        }
    }

    // ====================== 商品搜索接口（电商核心） ======================
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProduct(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false, defaultValue = "Desc") SortOrder sortOrder
    ) {
        try {
            List<Product> productList = productService.searchProduct(
                    keyword, minPrice, maxPrice, category, tags, sortField, sortOrder
            );
            return ResponseEntity.ok(productList);
        } catch (IllegalArgumentException e) {
            log.error("商品搜索参数错误", e);
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("商品搜索失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ====================== 商品聚合接口（电商运营） ======================
    @GetMapping("/agg/category")
    public ResponseEntity<Map<String, Long>> aggProductByCategory() {
        try {
            Map<String, Long> result = productService.aggProductByCategory();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("商品分类聚合失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/agg/category/sub")
    public ResponseEntity<Map<String, Map<String, Long>>> aggProductByCategoryAndSubCategory() {
        try {
            Map<String, Map<String, Long>> result = productService.aggProductByCategoryAndSubCategory();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("商品二级分类聚合失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
