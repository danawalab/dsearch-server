package com.danawa.fastcatx.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;

@RestController
@RequestMapping("/elasticsearch")
@ConfigurationProperties(prefix="elasticsearch")
public class ElasticSearchController {
    private static Logger logger = LoggerFactory.getLogger(ElasticSearchController.class);

    private List<String> nodes;

    private static RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/**/*")
    @CrossOrigin("*")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) throws URISyntaxException {
        logger.debug("proxy {}", request.getRequestURI());
        Enumeration<String> headerNames = request.getHeaderNames();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.add(headerName, headerValue);
        }

        HttpEntity<byte[]> httpEntity = new HttpEntity<>(body, headers);

        String originQueryString = request.getQueryString();
        ResponseEntity<byte[]> response = null;
        for (String node : nodes) {
            String url = String.format("%s%s%s", node, request.getRequestURI().substring(14), (StringUtils.isEmpty(originQueryString) ? "" : "?" + originQueryString));
            try {
                response = restTemplate.exchange(new URI(url), HttpMethod.valueOf(request.getMethod()), httpEntity, byte[].class);
                break;
            } catch (RestClientException e) {
                logger.debug("proxy fail : {}, {}", node, e);
            }
        }
        return response;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }
}
