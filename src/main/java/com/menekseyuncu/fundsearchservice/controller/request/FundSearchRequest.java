package com.menekseyuncu.fundsearchservice.controller.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;


@Getter
@Setter
public class FundSearchRequest {

    private Pagination pagination;
    private Filter filter;
    private Sorting sorting;

    @Getter
    @Setter
    public static class Pagination {
        private int pageNumber;
        private int pageSize;
    }


    @Getter
    @Setter
    public static class Sorting {
        private String property;
        private String direction;
    }

    @Getter
    @Setter
    public static class Filter {
        private String fundCode;
        private String fundName;
        private String umbrellaType;

        private BigDecimal minReturn1Year;
        private BigDecimal maxReturn1Year;
    }


    /**
     * Converts the filter criteria, pagination, and sorting settings into an Elasticsearch {@link Query}.
     * The method dynamically constructs a query using the provided filter values. It supports:
     * <ul>
     * - Exact matches (e.g., fundCode).
     * - Text-based search for fields (e.g., fundName and umbrellaType).
     * - Range queries for numeric fields (e.g., return1Year).
     * </ul>
     * Returns a {@link Query} object configured with the constructed criteria, along with any pagination
     * and sorting settings derived using {@link #toPageable()}.
     *
     * @return A {@link Query} object containing the search criteria, pagination, and sorting details.
     */
    public Query toElasticsearchQuery() {

        Criteria criteria = new Criteria("*");

        if (filter != null) {

            // --- EXACT MATCH ---
            if (filter.getFundCode() != null && !filter.getFundCode().isBlank()) {
                criteria = criteria.and(
                        new Criteria("fundCode").is(filter.getFundCode())
                );
            }

            // --- TEXT SEARCH ---
            if (filter.getFundName() != null && !filter.getFundName().isBlank()) {
                criteria = criteria.and(
                        new Criteria("fundName").matches(filter.getFundName())
                );
            }

            // --- TEXT SEARCH ---
            if (filter.getUmbrellaType() != null && !filter.getUmbrellaType().isBlank()) {
                criteria = criteria.and(
                        new Criteria("umbrellaType").contains(filter.getUmbrellaType())
                );
            }

            // --- RANGE (NULL-SAFE) ---
            if (filter.getMinReturn1Year() != null || filter.getMaxReturn1Year() != null) {

                criteria = criteria.and(new Criteria("return1Year").exists());

                if (filter.getMinReturn1Year() != null) {
                    criteria = criteria.and(
                            new Criteria("return1Year")
                                    .greaterThanEqual(filter.getMinReturn1Year())
                    );
                }

                if (filter.getMaxReturn1Year() != null) {
                    criteria = criteria.and(
                            new Criteria("return1Year")
                                    .lessThanEqual(filter.getMaxReturn1Year())
                    );
                }
            }
        }

        return new CriteriaQuery(criteria)
                .setPageable(toPageable());
    }

    /**
     * Converts the pagination and sorting settings into a {@link Pageable} object.
     * The method ensures default values are applied if pagination or sorting
     * settings are not provided. It calculates the page number and size based on
     * the input and applies sorting if specified.
     *
     * @return A {@link Pageable} object containing the configured page number,
     *         page size, and sorting options.
     */
    private Pageable toPageable() {
        int page = (pagination != null) ? Math.max(0, pagination.getPageNumber() - 1) : 0;
        int size = (pagination != null) ? pagination.getPageSize() : 10;

        Sort sort = Sort.unsorted();
        if (sorting != null && sorting.getProperty() != null) {
            Sort.Direction dir = Sort.Direction.fromString(
                    sorting.getDirection() != null ? sorting.getDirection() : "ASC"
            );
            sort = Sort.by(dir, sorting.getProperty());
        }
        return PageRequest.of(page, size, sort);
    }
}
