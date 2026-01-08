package com.menekseyuncu.fundsearchservice.service;

import com.menekseyuncu.fundsearchservice.model.entity.FundEntity;
import com.menekseyuncu.fundsearchservice.repository.FundRepository;
import com.menekseyuncu.fundsearchservice.repository.FundSearchRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundServiceTest {

    @Mock
    private FundRepository fundRepository;

    @Mock
    private FundSearchRepository fundSearchRepository;

    @InjectMocks
    private FundService fundService;

    private MultipartFile excelFile;

    @BeforeEach
    void setUp() throws Exception {
        excelFile = createValidExcelFile();
    }


    @Test
    void importFundsFromExcel_shouldPersistAndSync_whenExcelIsValid() {
        // given
        Mockito.when(fundRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        fundService.importFundsFromExcel(excelFile);

        // then
        ArgumentCaptor<List<FundEntity>> entityCaptor =
                ArgumentCaptor.forClass(List.class);

        Mockito.verify(fundRepository, times(1)).saveAll(entityCaptor.capture());
        Mockito.verify(fundSearchRepository, times(1)).saveAll(anyList());

        List<FundEntity> savedEntities = entityCaptor.getValue();

        Assertions.assertThat(savedEntities).hasSize(2);
        Assertions.assertThat(savedEntities.get(0).getFundCode()).isEqualTo("DLZ");
        Assertions.assertThat(savedEntities.get(1).getFundCode()).isEqualTo("UHS");
    }

    @Test
    void importFundsFromExcel_shouldThrowException_whenFileIsEmpty() {
        MultipartFile emptyFile =
                new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> fundService.importFundsFromExcel(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file is empty.");

        verifyNoInteractions(fundRepository, fundSearchRepository);
    }

    @Test
    void initializeData_shouldLoadAndPersist_whenStartupFileExists() {
        // given
        when(fundRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        fundService.initializeData();

        // then
        ArgumentCaptor<List<FundEntity>> entityCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(fundRepository, times(1)).saveAll(entityCaptor.capture());
        verify(fundSearchRepository, times(1)).saveAll(anyList());

        List<FundEntity> savedFunds = entityCaptor.getValue();

        assertThat(savedFunds).isNotEmpty();
        assertThat(savedFunds)
                .extracting(FundEntity::getFundCode)
                .contains("DLZ", "UHS");
    }


    @Test
    void importFundsFromExcel_shouldSkipRowsWithEmptyFundCode() throws Exception {
        MultipartFile fileWithInvalidRow = createExcelWithEmptyFundCode();

        when(fundRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        fundService.importFundsFromExcel(fileWithInvalidRow);

        ArgumentCaptor<List<FundEntity>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(fundRepository).saveAll(captor.capture());

        List<FundEntity> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getFundCode()).isEqualTo("DLZ");
    }

    @Test
    void importFundsFromExcel_shouldHandleNullNumericCells() {
        when(fundRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        fundService.importFundsFromExcel(excelFile);

        ArgumentCaptor<List<FundEntity>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(fundRepository).saveAll(captor.capture());

        FundEntity uhs = captor.getValue()
                .stream()
                .filter(f -> f.getFundCode().equals("UHS"))
                .findFirst()
                .orElseThrow();

        assertThat(uhs.getReturn1Year()).isNull();
        assertThat(uhs.getReturnYtd()).isNull();
    }

    private MultipartFile createValidExcelFile() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Funds");

        // header + boş satırlar
        sheet.createRow(0);
        sheet.createRow(1);

        Row row1 = sheet.createRow(2);
        row1.createCell(0).setCellValue("DLZ");
        row1.createCell(1).setCellValue("DENİZ PORTFÖY ALİZE HİSSE SENEDİ SERBEST (TL) FON");
        row1.createCell(2).setCellValue("Serbest Şemsiye Fonu");
        row1.createCell(3).setCellValue(86.4372);
        row1.createCell(4).setCellValue(-34.0917);
        row1.createCell(5).setCellValue(-6.8773);
        row1.createCell(6).setCellValue(156.7887);
        row1.createCell(7).setCellValue(157.8626);

        Row row2 = sheet.createRow(3);
        row2.createCell(0).setCellValue("UHS");
        row2.createCell(1).setCellValue("ATLAS PORTFÖY ÜÇÜNCÜ HİSSE SENEDİ SERBEST FON");
        row2.createCell(2).setCellValue("Serbest Şemsiye Fonu");
        row2.createCell(3).setCellValue(75.7093);
        row2.createCell(4).setCellValue(101.4648);
        row2.createCell(5).setCellValue(324.0403);
        // returnYtd, return1Year boş

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return new MockMultipartFile(
                "file",
                "funds.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(out.toByteArray())
        );
    }

    private MultipartFile createExcelWithEmptyFundCode() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();

        sheet.createRow(0);
        sheet.createRow(1);

        Row invalid = sheet.createRow(2);
        invalid.createCell(0).setCellValue(""); // fundCode boş

        Row valid = sheet.createRow(3);
        valid.createCell(0).setCellValue("DLZ");
        valid.createCell(1).setCellValue("Valid Fund");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return new MockMultipartFile(
                "file",
                "funds.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(out.toByteArray())
        );
    }
}