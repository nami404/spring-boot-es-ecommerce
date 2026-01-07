package com.ecommerce.es.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author Nami
 * date 2026/1/5 15:42
 * description ES客户端配置类
 */
@Configuration
@Slf4j
public class ElasticsearchConfig {
    @Value("${elasticsearch.host}")
    private String esHost;

    @Value("${elasticsearch.port}")
    private int esPort;

    @Value("${elasticsearch.scheme}")
    private String esScheme;

    @Value("${elasticsearch.connect-timeout}")
    private int esConnectTimeout;

    @Value("${elasticsearch.socket-timeout}")
    private int esSocketTimeout;

//    @Value("${elasticsearch.username}")
//    private String esUserName;
//
//    @Value("${elasticsearch.password}")
//    private String esPassword;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 1. 构建HTTP Host
        HttpHost httpHost = new HttpHost(esHost, esPort, esScheme);

        // 2. 构建RestClientBuilder
        RestClientBuilder builder = RestClient.builder(httpHost)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(esConnectTimeout)
                        .setSocketTimeout(esSocketTimeout))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        // 生产环境开启账号密码认证（电商安全要求）
                        // if (esUsername != null && !esUsername.isEmpty()) {
                        //     CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        //     credentialsProvider.setCredentials(AuthScope.ANY,
                        //             new UsernamePasswordCredentials(esUsername, esPassword));
                        //     httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        // }
                        return httpClientBuilder;
                    }
                });

        // 3. 构建Transport
        RestClientTransport transport = new RestClientTransport(builder.build(), new JacksonJsonpMapper());

        // 4. 构建ES Client
        ElasticsearchClient client = new ElasticsearchClient(transport);
        log.info("ES 8.6.2 客户端初始化完成，连接地址：{}://{}:{}", esScheme, esHost, esPort);
        return client;
    }
}
