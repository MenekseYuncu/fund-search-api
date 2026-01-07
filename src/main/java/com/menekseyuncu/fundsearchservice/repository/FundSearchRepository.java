package com.menekseyuncu.fundsearchservice.repository;

import com.menekseyuncu.fundsearchservice.model.document.FundDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FundSearchRepository extends ElasticsearchRepository<FundDocument, String> {
}
