package com.menekseyuncu.fundsearchservice.service;

import com.menekseyuncu.fundsearchservice.model.document.FundDocument;
import com.menekseyuncu.fundsearchservice.model.entity.FundEntity;
import com.menekseyuncu.fundsearchservice.repository.FundRepository;
import com.menekseyuncu.fundsearchservice.repository.FundSearchRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for managing Fund operations.
 * <p>
 * Handles parsing of Excel files, data validation, persistence to PostgreSQL,
 * and synchronization with Elasticsearch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundService {


    private final FundRepository fundRepository;
    private final FundSearchRepository fundSearchRepository;

    private static final String STARTUP_DATA_PATH = "/data/funds_data.xlsx";
    private static final int DATA_START_ROW = 2;
    private static final int SCALE_PRECISION = 4;
    private static final int BATCH_SIZE = 500;

    /**
     * Processes an uploaded Excel file containing fund data.
     * Validates the file and triggers the internal import process.
     *
     * @param file The uploaded Excel file.
     * @throws IllegalArgumentException if the file is empty.
     * @throws RuntimeException if the file cannot be read.
     */
    @Transactional
    public void importFundsFromExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        try (InputStream inputStream = file.getInputStream()) {
            this.processExcelData(inputStream);
        } catch (IOException e) {
            log.error("API Upload error: ", e);
            throw new RuntimeException("Failed to read the file: " + e.getMessage());
        }
    }

    /**
     * Loads initial fund data from the classpath resources upon application startup.
     * Skips execution if the data file is not found.
     */
    @Transactional
    public void initializeData() {
        try (InputStream inputStream = getClass().getResourceAsStream(STARTUP_DATA_PATH)) {
            if (inputStream == null) {
                log.warn("Startup data file ({}) not found. System starting with empty DB.", STARTUP_DATA_PATH);
                return;
            }

            log.info("Loading startup data from resources...");
            this.processExcelData(inputStream);
        } catch (IOException e) {
            log.error("Error loading startup data: ", e);
        }
    }


    /**
     * Core logic that parses the Excel stream, filters valid rows,
     * and persists data to both the database and search index.
     *
     * @param inputStream The input stream of the Excel file.
     */
    private void processExcelData(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<FundEntity> fundList = new ArrayList<>();

            for (int i = DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                FundEntity fund = this.parseRowToEntity(row);
                if (fund != null && this.hasValidFundCode(fund)) {
                    fundList.add(fund);
                }
            }

            if (!fundList.isEmpty()) {
                this.saveAndSyncFundsInBatches(fundList);
            } else {
                log.warn("No valid fund data found in the Excel file.");
            }

        } catch (IOException e) {
            throw new RuntimeException("Error processing Excel file: " + e.getMessage());
        }
    }


    /**
     * Validates if the fund entity has a valid (non-null, non-empty) fund code.
     */
    private boolean hasValidFundCode(FundEntity fund) {
        return fund.getFundCode() != null && !fund.getFundCode().trim().isEmpty();
    }

    /**
     * Saves and synchronizes a list of FundEntity objects in batches. This method processes
     * the provided list in chunks of a predefined batch size. Each batch is first saved to
     * the database and then synchronized to Elasticsearch.
     *
     * @param fundList the list of FundEntity objects to be processed in batches
     */
    private void saveAndSyncFundsInBatches(List<FundEntity> fundList) {
        int totalSize = fundList.size();
        log.info("Starting batch processing for {} records...", totalSize);

        for (int i = 0; i < totalSize; i += BATCH_SIZE) {
            int end = Math.min(totalSize, i + BATCH_SIZE);
            List<FundEntity> batch = fundList.subList(i, end);

            // 1. DB Save
            List<FundEntity> savedBatch = fundRepository.saveAll(batch);

            // 2. Elasticsearch Sync
            syncToElasticsearch(savedBatch);
        }
        log.info("Batch processing completed.");
    }


    /**
     * Synchronizes a list of FundEntity objects to Elasticsearch by mapping them to FundDocument
     * instances and saving them in the Elasticsearch repository.
     *
     * @param funds the list of FundEntity objects to be synchronized with Elasticsearch
     */
    private void syncToElasticsearch(List<FundEntity> funds) {
        try {
            List<FundDocument> documents = funds.stream()
                    .map(this::mapToDocument)
                    .toList();

            fundSearchRepository.saveAll(documents);
            log.info("Elasticsearch: Synced batch of {} documents.", documents.size());

        } catch (Exception e) {
            log.error("Failed to sync batch to Elasticsearch. Count: {}. Error: {}", funds.size(), e.getMessage());
        }
    }

    /**
     * Maps a {@link FundEntity} to a {@link FundDocument} for Elasticsearch indexing.
     */
    private FundDocument mapToDocument(FundEntity entity) {
        return FundDocument.builder()
                .fundCode(entity.getFundCode())
                .fundName(entity.getFundName())
                .umbrellaType(entity.getUmbrellaType())
                .return1Month(entity.getReturn1Month())
                .return3Month(entity.getReturn3Month())
                .return6Month(entity.getReturn6Month())
                .returnYtd(entity.getReturnYtd())
                .return1Year(entity.getReturn1Year())
                .return3Year(entity.getReturn3Year())
                .return5Year(entity.getReturn5Year())
                .build();
    }

    /**
     * Parses a single Excel row into a {@link FundEntity}.
     * Returns null if parsing fails.
     */
    private FundEntity parseRowToEntity(Row row) {
        try {
            return FundEntity.builder()
                    .fundCode(getCellValueAsString(row.getCell(0)))
                    .fundName(getCellValueAsString(row.getCell(1)))
                    .umbrellaType(getCellValueAsString(row.getCell(2)))
                    .return1Month(getCellValueAsBigDecimal(row.getCell(3)))
                    .return3Month(getCellValueAsBigDecimal(row.getCell(4)))
                    .return6Month(getCellValueAsBigDecimal(row.getCell(5)))
                    .returnYtd(getCellValueAsBigDecimal(row.getCell(6)))
                    .return1Year(getCellValueAsBigDecimal(row.getCell(7)))
                    .return3Year(getCellValueAsBigDecimal(row.getCell(8)))
                    .return5Year(getCellValueAsBigDecimal(row.getCell(9)))
                    .build();
        } catch (Exception e) {
            log.warn("Row parse error (Row {}): {}", row.getRowNum(), e.getMessage());
            return null;
        }
    }

    /**
     * Converts the value of a given cell to a trimmed string representation.
     *
     * @param cell The cell whose value needs to be converted to a string.
     *             Can be null, in which case an empty string is returned.
     * @return A trimmed string representation of the cell's value.
     *         Returns an empty string if the cell is null.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return cell.toString().trim();
    }

    /**
     * Extracts a BigDecimal value from a cell, handling various formats.
     * Supports numeric cells and string cells with percentage signs or commas.
     *
     * @return BigDecimal value scaled to {@value #SCALE_PRECISION}, or null if invalid.
     */
    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;

        String rawValue = this.getRawValueFromCell(cell);

        if (this.isInvalidNumberString(rawValue)) {
            return null;
        }

        String sanitizedValue = this.sanitizeNumberString(rawValue);

        try {
            return new BigDecimal(sanitizedValue).setScale(SCALE_PRECISION, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            log.warn("Non-numeric value encountered, setting to null: {}", rawValue);
            return null;
        }
    }

    /**
     * Extracts the raw textual value from the given cell.
     *
     * @param cell the cell from which the raw value is to be retrieved
     * @return a string representing the raw value of the cell; for numeric cells, the value is converted to text
     */
    private String getRawValueFromCell(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            return NumberToTextConverter.toText(cell.getNumericCellValue());
        }
        return cell.toString().trim();
    }

    /**
     * Checks if the provided string represents an invalid number.
     *
     * @param value the string to be checked for validity as a number.
     * @return true if the string is empty or equals a lone "-", indicating invalidity; false otherwise.
     */
    private boolean isInvalidNumberString(String value) {
        return value.isEmpty() || value.equals("-");
    }

    /**
     * Sanitizes the numeric string by removing percentages and normalizing
     * decimal separators (e.g., converting '1.234,56' to '1234.56').
     */
    private String sanitizeNumberString(String value) {
        String cleanValue = value.replace("%", "").trim();

        if (cleanValue.contains(",")) {
            // Remove thousands separator if present (e.g. 1.000,50 -> 1000,50)
            if (cleanValue.contains(".")) {
                cleanValue = cleanValue.replace(".", "");
            }
            // Replace decimal comma with dot (1000,50 -> 1000.50)
            cleanValue = cleanValue.replace(",", ".");
        }
        return cleanValue;
    }
}
