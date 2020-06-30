package com.danawa.fastcatx.server.config;

import com.danawa.fastcatx.server.entity.ElasticsearchNode;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class ElasticSearchConfig {





    public RestHighLevelClient getRestHighLevelClient() {
        HttpHost[] httpHostList = new HttpHost[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            httpHostList[i] = new HttpHost(nodes.get(i).getHost(), nodes.get(i).getPort(), nodes.get(i).getScheme());
        }

        RestClientBuilder builder = RestClient.builder(httpHostList)
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultIOReactorConfig(IOReactorConfig
                                .custom()
                                .setIoThreadCount(thread)
                                .build()));

        if (username != null && !"".equals(username)
                && password != null && !"".equals(password)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return new RestHighLevelClient(builder);
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<ElasticsearchNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<ElasticsearchNode> nodes) {
        this.nodes = nodes;
    }



    @Override
    public void close() throws IOException {

    }
}
