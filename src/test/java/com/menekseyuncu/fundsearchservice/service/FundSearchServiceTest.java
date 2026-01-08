package com.menekseyuncu.fundsearchservice.service;

import com.menekseyuncu.fundsearchservice.controller.request.FundSearchRequest;
import com.menekseyuncu.fundsearchservice.exception.SearchOperationException;
import com.menekseyuncu.fundsearchservice.model.document.FundDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class FundSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private SearchHits<FundDocument> searchHits;

    @InjectMocks
    private FundSearchService fundSearchService;

    private FundSearchRequest request;

    @BeforeEach
    void setUp() {
        request = new FundSearchRequest();

        FundSearchRequest.Filter filter = new FundSearchRequest.Filter();
        filter.setFundCode("DLZ");
        filter.setFundName("DENİZ");
        filter.setUmbrellaType("Şemsiye");
        filter.setMinReturn1Year(BigDecimal.valueOf(157.50));

        FundSearchRequest.Pagination pagination = new FundSearchRequest.Pagination();
        pagination.setPageNumber(1);
        pagination.setPageSize(10);

        request.setFilter(filter);
        request.setPagination(pagination);
    }

    @Test
    void searchFunds_shouldReturnDocuments_whenSearchHitsExist() {
        // given
        FundDocument fund = FundDocument.builder()
                .fundCode("DLZ")
                .fundName("DENİZ PORTFÖY ALİZE HİSSE SENEDİ SERBEST (TL) FON (HİSSE SENEDİ YOĞUN FON)")
                .umbrellaType("Serbest Şemsiye Fonu")
                .return1Month(new BigDecimal("86.4372"))
                .return3Month(new BigDecimal("-34.0917"))
                .return6Month(new BigDecimal("-6.8773"))
                .returnYtd(new BigDecimal("156.7887"))
                .return1Year(new BigDecimal("157.8626"))
                .build();

        SearchHit<FundDocument> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(fund);

        when(searchHits.hasSearchHits()).thenReturn(true);
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(searchHits.stream()).thenReturn(Stream.of(searchHit));

        when(elasticsearchOperations.search(any(Query.class), eq(FundDocument.class)))
                .thenReturn(searchHits);

        // when
        List<FundDocument> result = fundSearchService.searchFunds(request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFundCode()).isEqualTo("DLZ");
        assertThat(result.get(0).getUmbrellaType()).isEqualTo("Serbest Şemsiye Fonu");

        verify(elasticsearchOperations, times(1))
                .search(any(Query.class), eq(FundDocument.class));
    }

    @Test
    void searchFunds_shouldReturnEmptyList_whenNoSearchHits() {
        // given
        when(searchHits.hasSearchHits()).thenReturn(false);

        when(elasticsearchOperations.search(any(Query.class), eq(FundDocument.class)))
                .thenReturn(searchHits);

        // when
        List<FundDocument> result = fundSearchService.searchFunds(request);

        // then
        assertThat(result).isEmpty();

        verify(elasticsearchOperations, times(1))
                .search(any(Query.class), eq(FundDocument.class));
    }

    @Test
    void searchFunds_shouldBuildQueryFromRequest() {
        //given
        FundSearchRequest spyRequest = spy(request);

        // when
        when(elasticsearchOperations.search(any(Query.class), eq(FundDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.hasSearchHits()).thenReturn(false);

        //then
        fundSearchService.searchFunds(spyRequest);

        verify(spyRequest, times(1)).toElasticsearchQuery();
    }

    @Test
    void searchFunds_shouldThrowIllegalArgumentException_whenRequestIsNull() {
        // when & then
        assertThatThrownBy(() -> fundSearchService.searchFunds(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Search request cannot be null.");

        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void searchFunds_shouldThrowSearchOperationException_whenElasticsearchFails() {
        when(elasticsearchOperations.search(any(Query.class), eq(FundDocument.class)))
                .thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> fundSearchService.searchFunds(request))
                .isInstanceOf(SearchOperationException.class)
                .hasMessage("Failed to execute search operation")
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("ES down");
    }

}
