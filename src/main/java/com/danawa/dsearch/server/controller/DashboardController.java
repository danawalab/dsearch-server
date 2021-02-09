package com.danawa.dsearch.server.controller;

import com.danawa.dsearch.server.services.IndexingJobManager;
import com.danawa.dsearch.server.services.IndexingJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private static Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final IndexingJobService indexingJobService;
    private final IndexingJobManager indexingJobManager;
    public DashboardController(IndexingJobService indexingJobService, IndexingJobManager indexingJobManager) {
        this.indexingJobService = indexingJobService;
        this.indexingJobManager = indexingJobManager;
    }

    @GetMapping("/indexing")
    public ResponseEntity<?> getRunningIndexingStatus(@RequestHeader(value = "cluster-id") UUID clusterId){
        logger.debug("현재 Indexing Status 확인");
        return new ResponseEntity<>(indexingJobManager.getIndexingStatusList(clusterId), HttpStatus.OK);
    }
}
