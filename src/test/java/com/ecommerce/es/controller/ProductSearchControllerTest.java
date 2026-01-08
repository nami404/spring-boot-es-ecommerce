package com.ecommerce.es.controller;

import com.ecommerce.es.entity.Product;
import com.ecommerce.es.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.elastic.clients.elasticsearch._types.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductSearchController productSearchController;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 测试用例数据
    private Product testProduct;
    private List<Product> testProductList;

    /**
     * 每个测试方法执行前初始化
     */
    @BeforeEach
    void setUp() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);

        // 手动创建 UTF-8 编码的 StringHttpMessageConverter
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);

        mockMvc = MockMvcBuilders.standaloneSetup(productSearchController)
                .setMessageConverters(stringConverter, new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilter(encodingFilter) // 添加UTF-8编码过滤器
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();

         testProduct = new Product();
         testProduct.setId("1001");
         testProduct.setProductName("测试手机");
         testProduct.setPrice(new BigDecimal("2999.99"));
         testProduct.setCategory("手机");
         testProduct.setSubCategory("智能手机");
         testProduct.setStock(100);
         testProduct.setSales(50);
         testProduct.setTags(Arrays.asList("智能", "5G", "新品"));
         testProduct.setCreateTime(new Date());
         testProduct.setDescription("这是一款测试手机，功能强大");
         testProduct.setMerchantId("merchant_001");
         testProduct.setScore(4.5);

        testProductList = Arrays.asList(testProduct);
    }

    @Test
    void testCreateProductIndex_Success() throws Exception {
        doReturn(true).when(productService).createProductIndex();

        mockMvc.perform(post("/product/index/create")
                        .contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string("商品索引创建成功（或已存在）"));
    }

    @Test
    void testCreateProductIndex_Fail() throws Exception {
        doThrow(new RuntimeException("ES 连接失败")).when(productService).createProductIndex();

        mockMvc.perform(post("/product/index/create")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("创建失败：ES 连接失败"));
    }

    @Test
    void testDeleteProductIndex_Success() throws Exception {
        doReturn(true).when(productService).deleteProductIndex();

        mockMvc.perform(MockMvcRequestBuilders.delete("/product/index/delete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("商品索引删除成功（或不存在）"));
    }

    @Test
    void testDeleteProductIndex_Fail() throws Exception {
        doThrow(new RuntimeException("ES 连接失败")).when(productService).deleteProductIndex();

        mockMvc.perform(MockMvcRequestBuilders.delete("/product/index/delete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("删除失败：ES 连接失败"));
    }

    @Test
    void testSaveProduct_Success() throws Exception {
        doReturn("新增成功，ID：1001").when(productService).saveProduct(any(Product.class));

        mockMvc.perform(post("/product/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProduct)))
                .andExpect(status().isOk())
                .andExpect(content().string("商品新增成功，操作结果：新增成功，ID：1001"));
    }

    @Test
    void testSaveProduct_ParamError() throws Exception {
        doThrow(new IllegalArgumentException("商品名称不能为空")).when(productService).saveProduct(any(Product.class));

        mockMvc.perform(post("/product/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProduct)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("参数错误：商品名称不能为空"));
    }

    @Test
    void testBatchSaveProduct_Success() throws Exception {
        doReturn("批量新增成功，共1条数据").when(productService).batchSaveProduct(anyList());

        mockMvc.perform(post("/product/batch/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductList)))
                .andExpect(status().isOk())
                .andExpect(content().string("批量新增成功，共1条数据"));
    }

    @Test
    void testGetProductById_Success() throws Exception {
        doReturn(testProduct).when(productService).getProductById(anyString());

        mockMvc.perform(MockMvcRequestBuilders.get("/product/1001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("1001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.productName").value("测试手机"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.price").value(2999.99));
    }

    @Test
    void testGetProductById_NotFound() throws Exception {
        doReturn(null).when(productService).getProductById(anyString());

        mockMvc.perform(MockMvcRequestBuilders.get("/product/1002")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateProduct_Success() throws Exception {
        doReturn("更新成功").when(productService).updateProduct(any(Product.class));

        mockMvc.perform(MockMvcRequestBuilders.put("/product/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProduct)))
                .andExpect(status().isOk())
                .andExpect(content().string("商品更新成功，操作结果：更新成功"));
    }

    @Test
    void testDeleteProductById_Success() throws Exception {
        doReturn("删除成功").when(productService).deleteProductById(anyString());

        mockMvc.perform(MockMvcRequestBuilders.delete("/product/1001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("商品删除成功，操作结果：删除成功"));
    }

//    @Test
//    void testSearchProduct_Success() throws Exception {
//        // Mock 搜索结果 - 使用any()匹配所有参数
//        doReturn(testProductList).when(productService).searchProduct(
//                anyString(), any(BigDecimal.class), any(BigDecimal.class),
//                anyString(), anyList(), anyString(), any(SortOrder.class)
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.get("/product/search")
//                        .param("keyword", "手机")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
//                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("1001"))
//                .andExpect(MockMvcResultMatchers.jsonPath("$[0].productName").value("测试手机"))
//                .andExpect(MockMvcResultMatchers.jsonPath("$[0].price").value(2999.99))
//                .andExpect(MockMvcResultMatchers.jsonPath("$[0].category").value("手机"))
//                .andExpect(MockMvcResultMatchers.jsonPath("$[0].tags[0]").value("智能"))
//                .andExpect(MockMvcResultMatchers.jsonPath("$[0].tags[1]").value("5G"))
//                .andExpect(MockMvcResultMatchers.jsonPath("$[0].tags[2]").value("新品"));
//    }
//
//    @Test
//    void testSearchProduct_ParamError() throws Exception {
//        doThrow(new IllegalArgumentException("排序字段不能为空")).when(productService).searchProduct(
//                anyString(), any(BigDecimal.class), any(BigDecimal.class),
//                anyString(), anyList(), anyString(), any(SortOrder.class)
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.get("/product/search")
//                        .param("keyword", "手机")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isBadRequest())
//                .andExpect(content().string("")); // 错误时返回 null，响应体为空字符串
//    }

    @Test
    void testAggProductByCategory_Success() throws Exception {
        // Mock 聚合结果
        Map<String, Long> aggResult = new HashMap<>();
        aggResult.put("手机", 100L);
        aggResult.put("电脑", 80L);
        doReturn(aggResult).when(productService).aggProductByCategory();

        mockMvc.perform(MockMvcRequestBuilders.get("/product/agg/category")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.手机").value(100))
                .andExpect(MockMvcResultMatchers.jsonPath("$.电脑").value(80));
    }

    @Test
    void testAggProductByCategoryAndSubCategory_Success() throws Exception {
        // Mock 二级分类聚合结果
        Map<String, Map<String, Long>> subAggResult = new HashMap<>();
        Map<String, Long> phoneSub = new HashMap<>();
        phoneSub.put("华为", 50L);
        phoneSub.put("苹果", 30L);
        subAggResult.put("手机", phoneSub);

        doReturn(subAggResult).when(productService).aggProductByCategoryAndSubCategory();

        mockMvc.perform(MockMvcRequestBuilders.get("/product/agg/category/sub")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.手机.华为").value(50))
                .andExpect(MockMvcResultMatchers.jsonPath("$.手机.苹果").value(30));
    }
}