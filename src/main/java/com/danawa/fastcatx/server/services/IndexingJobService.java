package com.danawa.fastcatx.server.services;

import com.danawa.fastcatx.server.config.ElasticsearchFactory;
import com.danawa.fastcatx.server.entity.Collection;
import com.danawa.fastcatx.server.entity.IndexStep;
import com.danawa.fastcatx.server.entity.IndexingStatus;
import com.danawa.fastcatx.server.excpetions.IndexingJobFailureException;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@Service
@ConfigurationProperties(prefix = "fastcatx.collection")
public class IndexingJobService {
    private static Logger logger = LoggerFactory.getLogger(IndexingJobService.class);
    private final ElasticsearchFactory elasticsearchFactory;
    private RestTemplate restTemplate;

    private Map<String, Object> params;

    public IndexingJobService(ElasticsearchFactory elasticsearchFactory) {
        this.elasticsearchFactory = elasticsearchFactory;

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10 * 1000);
        factory.setReadTimeout(10 * 1000);
        restTemplate = new RestTemplate(factory);
    }

    /**
     * 소스를 읽어들여 ES 색인에 입력하는 작업.
     * indexer를 외부 프로세스로 실행한다.
     *
     * @return */
    public IndexingStatus indexing(UUID clusterId, Collection collection, IndexStep step) throws IndexingJobFailureException {
        return indexing(clusterId, collection, false, step, new ArrayDeque<>());
    }
    public IndexingStatus indexing(UUID clusterId, Collection collection, IndexStep step, Queue<IndexStep> nextStep) throws IndexingJobFailureException {
        return indexing(clusterId, collection, false, step, nextStep);
    }
    public synchronized IndexingStatus indexing(UUID clusterId, Collection collection, boolean autoRun, IndexStep step, Queue<IndexStep> nextStep) throws IndexingJobFailureException {
        IndexingStatus indexingStatus = new IndexingStatus();
        try (RestHighLevelClient client = elasticsearchFactory.getClient(clusterId)) {
            Collection.Index indexA = collection.getIndexA();
            Collection.Index indexB = collection.getIndexB();

//            1. 대상 인덱스 찾기.
            Collection.Index index = getTargetIndex(collection.getBaseId(), indexA, indexB);

//            2. 인덱스 설정 변경하기.
            editPreparations(client, index);

//            3. 런처 파라미터 변환작업
            Collection.Launcher launcher = collection.getLauncher();
            String host = launcher.getHost();
            int port = launcher.getPort();
            Map<String, Object> body = convertRequestParams(launcher.getYaml());
            body.put("index", index.getIndex());

//            4. indexer 색인 전송
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    new URI(String.format("http://%s:%d/async/start", host, port)),
                    HttpMethod.POST,
                    new HttpEntity(body),
                    Map.class
            );

            String id = (String) responseEntity.getBody().get("id");
            logger.info("Job ID: {}", id);
            indexingStatus.setCollectionId(collection.getId());
            indexingStatus.setClusterId(clusterId);
            indexingStatus.setIndex(index.getIndex());
            indexingStatus.setHost(host);
            indexingStatus.setStartTime(System.currentTimeMillis());
            indexingStatus.setPort(port);
            indexingStatus.setIndexingJobId(id);
            indexingStatus.setAutoRun(autoRun);
            indexingStatus.setCurrentStep(step);
            indexingStatus.setNextStep(nextStep);
            indexingStatus.setRetry(50);
        } catch (Exception e) {
            logger.error("", e);
            throw new IndexingJobFailureException(e);
        }
        return indexingStatus;
    }

    /**
     * 색인 샤드의 TAG 제약을 풀고 전체 클러스터로 확장시킨다.
     * */
    public void propagate() {

    }

    /**
     * 색인을 대표이름으로 alias 하고 노출한다.
     * */
    public void expose() {

    }

    private Collection.Index getTargetIndex(String baseId, Collection.Index indexA, Collection.Index indexB) {
        Collection.Index index = null;
        // 인덱스에 대한 alias를 확인
        if (indexA.getAliases().size() == 0 && indexB.getAliases().size() == 0) {
            index = indexA;
        } else if (indexA.getAliases().get(baseId) != null) {
            index = indexB;
        } else if (indexB.getAliases().get(baseId) != null) {
            index = indexA;
        }
        return index;
    }

    private void editPreparations(RestHighLevelClient client, Collection.Index index) throws IOException {
        Map<String, Object> settings = new HashMap<>();
        settings.put("refresh_interval", "-1");
        settings.put("index.routing.allocation.include._exclude", "search*");
        if (index.getUuid() == null) {
            // 인덱스 존재하지 않기 때문에 생성해주기.
            // 인덱스 템플릿이 존재하기 때문에 맵핑 설정 패쓰
            client.indices().create(new CreateIndexRequest(index.getIndex()).settings(settings), RequestOptions.DEFAULT);
        } else {
            // 기존 인덱스가 존재하기 때문에 셋팅 설정만 변경함.
            // settings에 index.routing.allocation.include._exclude=search* 호출
            // refresh interval: -1로 변경. 색인도중 검색노출하지 않음. 성능향상목적.
            client.indices().putSettings(new UpdateSettingsRequest().indices(index.getIndex()).settings(settings), RequestOptions.DEFAULT);
        }
    }
    private Map<String, Object> convertRequestParams(String yamlStr) throws IndexingJobFailureException {
        Map<String, Object> convert = new HashMap<>();
//        default param mixed
        convert.putAll(params);
        try {
            Map<String, Object> tmp = new Yaml().load(yamlStr);
            if (tmp != null) {
                convert.putAll(tmp);
            }
        } catch (ClassCastException | NullPointerException e) {
            throw new IndexingJobFailureException("invalid yaml");
        }
        return convert;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
