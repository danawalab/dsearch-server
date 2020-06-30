package com.danawa.fastcatx.server.controller;

import com.danawa.fastcatx.server.entity.DocumentPagination;
import com.danawa.fastcatx.server.services.IndicesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/indices")
public class IndicesController {
    private static Logger logger = LoggerFactory.getLogger(IndicesController.class);

    private IndicesService indicesService;

    public IndicesController(IndicesService indicesService) {
        this.indicesService = indicesService;
    }

    @GetMapping("/{index}/_docs")
    public ResponseEntity<?> findAllDocument(@PathVariable String index,
                                             @RequestHeader(value = "cluster-id") String clusterId,
                                             @RequestParam int pageNum,
                                             @RequestParam int rowSize,
                                             @RequestParam(required = false, defaultValue = "false") boolean analysis,
                                             @RequestParam(required = false) String id) throws IOException {

        DocumentPagination documentPagination = indicesService.findAllDocumentPagination(UUID.fromString(clusterId), index, pageNum, rowSize, id, analysis, null);

        return new ResponseEntity<>(documentPagination, HttpStatus.OK);
    }

}
