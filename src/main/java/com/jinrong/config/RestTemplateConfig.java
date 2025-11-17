package com.jinrong.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // 1. 定义一个信任所有证书的策略
        TrustStrategy acceptingTrustStrategy = (chain, authType) -> true;

        // 2. 创建 SSL 上下文，并使用这个信任策略
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        // 3. 创建 SSL 连接套接字工厂，并设置不进行主机名验证
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE);

        // 4. 使用构建器创建连接池管理器，并设置 SSLSocketFactory
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory) // 关键步骤：在这里设置 SSL 套接字工厂
                .build();
        connectionManager.setMaxTotal(200); // 连接池最大连接数[citation:1]
        connectionManager.setDefaultMaxPerRoute(50); // 每个路由的最大连接数[citation:1]
        // 5. 构建 HttpClient
        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager) // 关联连接池管理器
                .build();

        // 6. 创建使用 HttpClient 的请求工厂
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        // 可选：设置超时时间
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(30000);

        // 7. 创建并返回 RestTemplate
        return new RestTemplate(requestFactory);
    }
}