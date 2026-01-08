package com.menekseyuncu.fundsearchservice.controller;

import com.menekseyuncu.fundsearchservice.controller.request.FundSearchRequest;
import com.menekseyuncu.fundsearchservice.model.document.FundDocument;
import com.menekseyuncu.fundsearchservice.service.FundSearchService;
import com.menekseyuncu.fundsearchservice.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for managing fund operations.
 * Provides endpoints for searching for funds and uploading fund data via Excel.
 */
@RestController
@RequestMapping("/api/v1/funds")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;
    private final FundSearchService searchService;

    /**
     * Searches for funds based on the provided criteria (pagination, filtering, sorting).
     * This endpoint uses the POST method to support complex request bodies.
     *
     * @param request The search request object containing filter parameters.
     * @return A list of found {@link FundDocument} objects.
     */
    @PostMapping("/search")
    public ResponseEntity<List<FundDocument>> searchFunds(@RequestBody FundSearchRequest request) {
        List<FundDocument> results = searchService.searchFunds(request);
        return ResponseEntity.ok(results);
    }

    /**
     * Uploads an Excel file to import fund data into the system.
     * Triggers parsing, database persistence, and Elasticsearch indexing.
     *
     * @param file The Excel file containing fund data.
     * @return A success message if processed correctly, or a bad request if the file is missing.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcelFile(@RequestParam("file") MultipartFile file) {

        fundService.importFundsFromExcel(file);

        return ResponseEntity.ok("File processed successfully. Database and Elasticsearch have been updated.");
    }

    /**
     * Triggers a full re-indexing of data from PostgreSQL to Elasticsearch
     * and invalidates the cache to ensure data consistency.
     *
     * @return Response indicating that the synchronization process has started.
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncIndex() {
        fundService.fullSyncToElasticsearch();
        fundService.clearCache();

        return ResponseEntity.ok("Full synchronization started and cache cleared.");
    }

    /**
     * Manually clears all search result caches.
     * Useful for forcing fresh data retrieval when the underlying data has changed.
     *
     * @return Response indicating that the cache has been cleared.
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        fundService.clearCache();
        return ResponseEntity.ok("Cache cleared successfully.");
    }
}
