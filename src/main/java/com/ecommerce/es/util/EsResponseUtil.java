package com.ecommerce.es.util;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * author Nami
 * date 2026/1/5 15:46
 * description ES响应处理工具
 */
@Slf4j
public class EsResponseUtil {
    // ====================== 分页结果封装（电商搜索高频） ======================
    /**
     * 封装ES搜索结果为分页DTO（适配电商商品列表）
     * @param response ES搜索响应
     * @param currentPage 当前页（前端传入）
     * @param pageSize 页大小（前端传入）
     * @param <T> 商品实体类型
     * @return 分页结果（包含总条数、总页数、当前页数据）
     */
    public static <T> EsPageResult<T> wrapPageResult(SearchResponse<T> response, int currentPage, int pageSize) {
        EsPageResult<T> pageResult = new EsPageResult<>();
        try {
            // 1. 处理总条数
            TotalHits totalHits = response.hits().total();
            long totalCount = 0;
            if (totalHits != null && totalHits.relation() == TotalHitsRelation.Eq) {
                totalCount = totalHits.value();
            }
            pageResult.setTotalCount(totalCount);

            // 2. 计算总页数
            long totalPage = totalCount % pageSize == 0 ? totalCount / pageSize : totalCount / pageSize + 1;
            pageResult.setTotalPage(totalPage);

            // 3. 处理当前页数据
            List<Hit<T>> hitList = response.hits().hits();
            if (CollectionUtils.isEmpty(hitList)) {
                pageResult.setList(Collections.emptyList());
                pageResult.setCurrentPage(currentPage);
                pageResult.setPageSize(pageSize);
                return pageResult;
            }

            // 4. 解析商品数据
            List<T> dataList = hitList.stream()
                    .map(Hit::source)
                    .filter(source -> source != null) // 过滤空数据
                    .collect(Collectors.toList());
            pageResult.setList(dataList);
            pageResult.setCurrentPage(currentPage);
            pageResult.setPageSize(pageSize);

            log.info("ES分页结果封装完成：总条数={}, 总页数={}, 当前页={}, 页大小={}",
                    totalCount, totalPage, currentPage, pageSize);
        } catch (Exception e) {
            log.error("封装ES分页结果失败", e);
            throw new EsResponseException("分页结果封装失败：" + e.getMessage());
        }
        return pageResult;
    }

    // ====================== 聚合结果解析（电商运营分析） ======================
    /**
     * 解析一级聚合结果（如分类聚合）
     * @param response ES搜索响应
     * @param aggName 聚合名称（如category_agg）
     * @return 聚合结果Map（key=聚合维度值，value=数量）
     */
    public static Map<String, Long> parseSingleAggResult(SearchResponse<?> response, String aggName) {
        try {
            if (response.aggregations() == null || !response.aggregations().containsKey(aggName)) {
                log.warn("聚合结果中未找到聚合名称：{}", aggName);
                return Collections.emptyMap();
            }

            // 解析Terms聚合结果（电商分类聚合默认用Terms）
            return response.aggregations()
                    .get(aggName)
                    .sterms()
                    .buckets()
                    .array()
                    .stream()
                    .collect(Collectors.toMap(
                            bucket -> bucket.key().toString(), // 分类名称
                            bucket -> bucket.docCount()        // 商品数量
                    ));
        } catch (Exception e) {
            log.error("解析一级聚合结果失败，聚合名称：{}", aggName, e);
            throw new EsResponseException("解析聚合结果失败：" + e.getMessage());
        }
    }

    /**
     * 解析二级聚合结果（如分类+子分类聚合）
     * @param response ES搜索响应
     * @param firstAggName 一级聚合名称（如category_agg）
     * @param secondAggName 二级聚合名称（如sub_category_agg）
     * @return 二级聚合Map（key=一级维度值，value=二级维度Map）
     */
    public static Map<String, Map<String, Long>> parseDoubleAggResult(SearchResponse<?> response,
                                                                      String firstAggName,
                                                                      String secondAggName) {
        try {
            if (response.aggregations() == null || !response.aggregations().containsKey(firstAggName)) {
                log.warn("聚合结果中未找到一级聚合名称：{}", firstAggName);
                return Collections.emptyMap();
            }

            // 解析一级聚合
            return response.aggregations()
                    .get(firstAggName)
                    .sterms()
                    .buckets()
                    .array()
                    .stream()
                    .collect(Collectors.toMap(
                            firstBucket -> firstBucket.key().toString(),
                            firstBucket -> {
                                // 解析二级聚合
                                return firstBucket.aggregations()
                                        .get(secondAggName)
                                        .sterms()
                                        .buckets()
                                        .array()
                                        .stream()
                                        .collect(Collectors.toMap(
                                                secondBucket -> secondBucket.key().toString(),
                                                secondBucket -> secondBucket.docCount()
                                        ));
                            }
                    ));
        } catch (Exception e) {
            log.error("解析二级聚合结果失败，一级聚合：{}，二级聚合：{}", firstAggName, secondAggName, e);
            throw new EsResponseException("解析二级聚合结果失败：" + e.getMessage());
        }
    }

    // ====================== 搜索参数校验（前置处理） ======================
    /**
     * 电商搜索参数校验（分页、排序、价格范围）
     * @param currentPage 当前页
     * @param pageSize 页大小
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param sortField 排序字段
     */
    public static void validateSearchParams(int currentPage, int pageSize,
                                            Double minPrice, Double maxPrice,
                                            String sortField) {
        // 1. 分页参数校验
        if (currentPage < 1) {
            throw new EsResponseException("当前页不能小于1");
        }
        if (pageSize < 1 || pageSize > 100) { // 电商搜索页大小限制（避免单次查询过多）
            throw new EsResponseException("页大小需在1-100之间");
        }

        // 2. 价格范围校验
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new EsResponseException("最低价格不能高于最高价格");
        }

        // 3. 排序字段校验（仅允许电商商品的指定字段排序）
        if (sortField != null && !sortField.isEmpty()) {
            List<String> allowSortFields = new ArrayList<>(Arrays.asList("price", "sales", "score", "createTime"));
            if (!allowSortFields.contains(sortField)) {
                throw new EsResponseException("仅支持按价格/销量/评分/上架时间排序");
            }
        }
    }

    // ====================== 分页结果DTO（内部类） ======================
    /**
     * ES分页结果DTO（适配电商商品搜索返回格式）
     * @param <T> 商品实体类型
     */
    public static class EsPageResult<T> {
        private int currentPage;    // 当前页
        private int pageSize;       // 页大小
        private long totalCount;    // 总条数
        private long totalPage;     // 总页数
        private List<T> list;       // 当前页数据

        // 空构造
        public EsPageResult() {}

        // Getter & Setter
        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public long getTotalPage() {
            return totalPage;
        }

        public void setTotalPage(long totalPage) {
            this.totalPage = totalPage;
        }

        public List<T> getList() {
            return list;
        }

        public void setList(List<T> list) {
            this.list = list;
        }

        // 重写toString，便于日志打印
        @Override
        public String toString() {
            return "EsPageResult{" +
                    "currentPage=" + currentPage +
                    ", pageSize=" + pageSize +
                    ", totalCount=" + totalCount +
                    ", totalPage=" + totalPage +
                    ", listSize=" + (list == null ? 0 : list.size()) +
                    '}';
        }
    }

    // ====================== 自定义异常（统一处理） ======================
    /**
     * ES响应处理自定义异常
     */
    public static class EsResponseException extends RuntimeException {
        public EsResponseException(String message) {
            super(message);
        }

        public EsResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ====================== 工具类测试（可选） ======================
    public static void main(String[] args) {
        // 测试参数校验
        try {
            validateSearchParams(0, 20, 100.0, 50.0, "invalidField");
        } catch (EsResponseException e) {
            log.error("参数校验失败：{}", e.getMessage());
            // 输出：当前页不能小于1 → 最低价格不能高于最高价格 → 仅支持按价格/销量/评分/上架时间排序
        }
    }
}
