package com.menekseyuncu.fundsearchservice.service;

import com.menekseyuncu.fundsearchservice.controller.request.FundSearchRequest;
import com.menekseyuncu.fundsearchservice.exception.SearchOperationException;
import com.menekseyuncu.fundsearchservice.model.document.FundDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class responsible for executing search operations against Elasticsearch.
 * It utilizes {@link ElasticsearchOperations} to perform dynamic queries built by the request object.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Searches for funds based on the criteria defined in the {@link FundSearchRequest}.
     *
     * @param request The search request object containing filters, pagination, and sorting logic.
     * @return A list of {@link FundDocument} matching the search criteria. Returns an empty list if no matches are found.
     * @throws IllegalArgumentException if the request object is null.
     */
    public List<FundDocument> searchFunds(FundSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Search request cannot be null.");
        }

        try {
            Query query = request.toElasticsearchQuery();
            log.debug("Executing Elasticsearch query: {}", query.toString());

            SearchHits<FundDocument> searchHits = elasticsearchOperations.search(query, FundDocument.class);

            if (searchHits.hasSearchHits()) {
                log.info("Search completed successfully. Found {} funds.", searchHits.getTotalHits());
                return searchHits.stream()
                        .map(SearchHit::getContent)
                        .collect(Collectors.toList());
            } else {
                log.info("Search completed. No funds found matching the criteria.");
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("Elasticsearch search operation failed: {}", e.getMessage(), e);
            throw new SearchOperationException("Failed to execute search operation", e);
        }
    }
}
