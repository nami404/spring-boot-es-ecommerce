package com.ecommerce.es.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.JsonData;
import com.ecommerce.es.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * author Nami
 * date 2026/1/5 15:44
 * description 商品ES CRUD
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductEsRepository {
    private final ElasticsearchClient esClient;
    // 电商商品索引名（规范命名）
    private static final String PRODUCT_INDEX = "ecommerce_product";

    // ====================== 电商索引设计（核心） ======================
    /**
     * 创建商品索引（贴合电商搜索需求的映射设计）
     * - 商品名称/描述：IK分词（需提前安装IK插件）
     * - 分类/标签：关键词（聚合/筛选）
     * - 价格/销量/评分：数值型（排序/范围筛选）
     */
    public boolean createProductIndex() throws IOException {
        // 1. 检查索引是否存在
        if (esClient.indices().exists(e -> e.index(PRODUCT_INDEX)).value()) {
            log.info("商品索引{}已存在，无需重复创建", PRODUCT_INDEX);
            return true;
        }
        // 2. 创建索引 + 映射（电商场景精准字段类型）
        CreateIndexResponse response = esClient.indices().create(c -> c
                .index(PRODUCT_INDEX)
                .mappings(m -> m
                        // 商品名称：IK分词（max_word细粒度）
                        .properties("productName", p -> p.text(t -> t.analyzer("ik_max_word")))
                        // 分类/子分类：关键词（聚合/精准筛选）
                        .properties("category", p -> p.keyword(k->k))
                        .properties("subCategory", p -> p.keyword(k->k))
                        // 价格：双精度（范围筛选/排序）
                        .properties("price", p -> p.double_(d->d))
                        // 库存/销量：整型（数值筛选）
                        .properties("stock", p -> p.integer(i->i))
                        .properties("sales", p -> p.integer(i->i))
                        // 标签：关键词数组（多值聚合/筛选）
                        .properties("tags", p -> p.keyword(k->k))
                        // 上架时间：日期（时间范围筛选）
                        .properties("createTime", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss")))
                        // 商品描述：IK分词（粗粒度）
                        .properties("description", p -> p.text(t -> t.analyzer("ik_smart")))
                        // 商家ID：关键词（多商家筛选）
                        .properties("merchantId", p -> p.keyword(k->k))
                        // 评分：浮点型（排序/筛选）
                        .properties("score", p -> p.double_(d->d))
                )
        );
        log.info("商品索引{}创建成功，响应：{}", PRODUCT_INDEX, response.acknowledged());
        return response.acknowledged();
    }

    /**
     * 删除商品索引（电商运维操作）
     */
    public boolean deleteProductIndex() throws IOException {
        if (!esClient.indices().exists(e -> e.index(PRODUCT_INDEX)).value()) {
            log.info("商品索引{}不存在，无需删除", PRODUCT_INDEX);
            return true;
        }
        DeleteIndexResponse response = esClient.indices().delete(d -> d.index(PRODUCT_INDEX));
        log.info("商品索引{}删除成功，响应：{}", PRODUCT_INDEX, response.acknowledged());
        return response.acknowledged();
    }

    // ====================== 电商商品CRUD（核心） ======================
    /**
     * 新增商品（电商上架）
     */
    public String saveProduct(Product product) throws IOException {
        IndexResponse response = esClient.index(i -> i
                .index(PRODUCT_INDEX)
                .id(product.getId()) // 绑定商品ID为文档ID
                .document(product)
        );
        log.info("商品{}新增成功，操作结果：{}", product.getId(), response.result().name());
        return response.result().name();
    }

    /**
     * 批量新增商品（电商批量上架）
     */
    public String batchSaveProduct(List<Product> productList) throws IOException {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (Product product : productList) {
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(PRODUCT_INDEX)
                            .id(product.getId())
                            .document(product)
                    )
            );
        }
        BulkResponse bulkResponse = esClient.bulk(bulkBuilder.build());
        // 电商级异常处理：批量操作失败时打印失败详情
        if (bulkResponse.errors()) {
            log.error("批量新增商品失败，失败数量：{}", bulkResponse.items().size());
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    log.error("商品{}新增失败：{}", item.id(), item.error().reason());
                }
            }
            return "批量新增失败，失败数量：" + bulkResponse.items().size();
        }
        log.info("批量新增商品成功，总数：{}", productList.size());
        return "批量新增成功，总数：" + productList.size();
    }

    /**
     * 根据商品ID查询（电商详情页）
     */
    public Product getProductById(String productId) throws IOException {
        GetResponse<Product> response = esClient.get(g -> g
                        .index(PRODUCT_INDEX)
                        .id(productId),
                Product.class
        );
        if (response.found()) {
            log.info("商品{}查询成功", productId);
            return response.source();
        } else {
            log.warn("商品{}不存在", productId);
            return null;
        }
    }

    /**
     * 更新商品（电商商品编辑）
     */
    public String updateProduct(Product product) throws IOException {
        UpdateResponse<Product> response = esClient.update(u -> u
                        .index(PRODUCT_INDEX)
                        .id(product.getId())
                        .doc(product),
                Product.class
        );
        log.info("商品{}更新成功，操作结果：{}", product.getId(), response.result().name());
        return response.result().name();
    }

    /**
     * 删除商品（电商下架）
     */
    public String deleteProductById(String productId) throws IOException {
        DeleteResponse response = esClient.delete(d -> d
                .index(PRODUCT_INDEX)
                .id(productId)
        );
        log.info("商品{}删除成功，操作结果：{}", productId, response.result().name());
        return response.result().name();
    }

    // ====================== 电商商品搜索（高频场景） ======================
    /**
     * 商品模糊搜索（电商首页/搜索页）
     * - 支持商品名称/描述分词搜索
     * - 支持价格范围、分类、标签筛选
     * - 支持销量/价格/评分排序
     */
    public List<Product> searchProduct(
            String keyword,          // 搜索关键词
            BigDecimal minPrice,     // 最低价格
            BigDecimal maxPrice,     // 最高价格
            String category,         // 分类
            List<String> tags,       // 标签
            String sortField,        // 排序字段（sales/price/score）
            SortOrder sortOrder      // 排序方向（asc/desc）
    ) throws IOException {
        // 1. 构建基础查询：关键词匹配商品名称/描述
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        if (keyword != null && !keyword.isEmpty()) {
            boolQuery.should(MatchQuery.of(m -> m
                    .field("productName")
                    .query(keyword)
                    .analyzer("ik_max_word")
            )._toQuery());
            boolQuery.should(MatchQuery.of(m -> m
                    .field("description")
                    .query(keyword)
                    .analyzer("ik_smart")
            )._toQuery());
            boolQuery.minimumShouldMatch("1"); // 至少匹配一个
        }

        // 2. 价格范围筛选（电商核心筛选）
        if (minPrice != null || maxPrice != null) {
            RangeQuery.Builder rangeQueryBuilder  = new RangeQuery.Builder().field("price");
            if (minPrice != null) {
                rangeQueryBuilder.gte(JsonData.of(minPrice.doubleValue()));
            }
            if (maxPrice != null) {
                rangeQueryBuilder.lte(JsonData.of(maxPrice.doubleValue()));
            }
            RangeQuery rangeQuery = rangeQueryBuilder.build();
            boolQuery.filter(rangeQuery._toQuery());
        }

        // 3. 分类筛选（精准匹配）
        if (category != null && !category.isEmpty()) {
            boolQuery.filter(TermQuery.of(t -> t
                    .field("category")
                    .value(category)
            )._toQuery());
        }

        // 4. 标签筛选（多标签匹配）
        if (tags != null && !tags.isEmpty()) {
            boolQuery.filter(TermsQuery.of(t -> t
                    .field("tags")
                    .terms(tt -> tt.value(tags.stream().map(FieldValue::of).collect(Collectors.toList())))
            )._toQuery());
        }

        // 3. 构建排序（电商默认按销量降序）
        SortOptions sortOptions = SortOptions.of(s -> s.field(
                f -> f.field(sortField == null || sortField.isEmpty() ? "sales" : sortField)
                        .order(sortOrder == null ? SortOrder.Desc : sortOrder)
        ));

        // 4. 执行查询
        SearchResponse<Product> response = esClient.search(s -> s
                        .index(PRODUCT_INDEX)
                        .query(boolQuery.build()._toQuery())
                        .sort(sortOptions)
                        .size(100), // 电商分页可扩展from/size
                Product.class
        );

        // 5. 解析结果
        List<Product> productList = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
        log.info("商品搜索完成，关键词：{}，匹配数量：{}", keyword, productList.size());
        return productList;
    }

    // ====================== 电商商品聚合（运营分析） ======================
    /**
     * 按分类聚合商品数量（电商运营报表）
     */
    public Map<String, Long> aggProductByCategory() throws IOException {
        // 构建分类聚合
        Map<String, Aggregation> aggMap = Collections.singletonMap(
                "category_agg", Aggregation.of(a -> a
                        .terms(TermsAggregation.of(t -> t
                                .field("category")
                                .size(20) // 最多返回20个分类
                        ))
                )
        );

        // 执行聚合查询（不返回原始文档，提升性能）
        SearchResponse<Product> response = esClient.search(s -> s
                        .index(PRODUCT_INDEX)
                        .size(0)
                        .aggregations(aggMap),
                Product.class
        );

        // 解析聚合结果
        Map<String, Aggregate> aggResult = response.aggregations();
        Map<String, Long> categoryCountMap = aggResult.get("category_agg").sterms().buckets().array().stream()
                .collect(Collectors.toMap(
                        bucket -> bucket.key().toString(),
                        bucket -> bucket.docCount()
                ));
        log.info("商品分类聚合完成，聚合分类数：{}", categoryCountMap.size());
        return categoryCountMap;
    }

    /**
     * 按分类+子分类二级聚合（电商多级分类分析）
     */
    public Map<String, Map<String, Long>> aggProductByCategoryAndSubCategory() throws IOException {
        // 构建二级聚合：分类 -> 子分类
        Map<String, Aggregation> aggMap = Collections.singletonMap(
                "category_agg",
                Aggregation.of(a -> a
                        .terms(TermsAggregation.of(t -> t
                                .field("category")
                                .size(20)

                        ))
                        .aggregations(
                                Collections.singletonMap(
                                        "sub_category_agg",
                                        Aggregation.of(aa -> aa
                                                .terms(TermsAggregation.of(tt -> tt
                                                        .field("subCategory")
                                                        .size(10)
                                                ))
                                        )
                                )
                        )
                )
        );

        // 执行聚合
        SearchResponse<Product> response = esClient.search(s -> s
                        .index(PRODUCT_INDEX)
                        .size(0)
                        .aggregations(aggMap),
                Product.class
        );

        // 解析二级聚合结果
        Map<String, Map<String, Long>> resultMap = new HashMap<>();
        Map<String, Aggregate> aggResult = response.aggregations();
        aggResult.get("category_agg").sterms().buckets().array().forEach(categoryBucket -> {
            String category = categoryBucket.key().toString();
            Map<String, Long> subCategoryMap = categoryBucket.aggregations()
                    .get("sub_category_agg").sterms().buckets().array().stream()
                    .collect(Collectors.toMap(
                            subBucket -> subBucket.key().toString(),
                            subBucket -> subBucket.docCount()
                    ));
            resultMap.put(category, subCategoryMap);
        });
        log.info("商品二级分类聚合完成，聚合分类数：{}", resultMap.size());
        return resultMap;
    }
}
