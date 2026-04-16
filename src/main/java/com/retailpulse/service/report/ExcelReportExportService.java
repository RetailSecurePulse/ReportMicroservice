package com.retailpulse.service.report;

import com.retailpulse.service.report.extractor.TableDataExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelReportExportService<T> extends AbstractReportExportService<T> {

    private final String title;
    private final String[] headers;
    private final TableDataExtractor<T> extractor;
    private XSSFWorkbook workbook;
    private Sheet sheet;
    private final ByteArrayOutputStream baos;

    // Added "title" as parameter for flexibility
    public ExcelReportExportService(String title, String[] headers, TableDataExtractor<T> extractor, ByteArrayOutputStream baos) {
        this.title = title;
        this.headers = headers;
        this.extractor = extractor;
        this.baos = baos;
    }

    @Override
    protected void writeReportHeader(Instant start, Instant end) {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Report");

        // Title row (index 0) with flexible title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(this.title);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Generated On row (index 1)
        Row generatedOnRow = sheet.createRow(1);
        Cell generatedOnCell = generatedOnRow.createCell(0);
        String generatedOn = dtf.format(LocalDateTime.now());
        generatedOnCell.setCellValue("Report Generated On: " + generatedOn);

        // Add date range row only if both start and end are provided
        if (start != null && end != null) {
            Row dateRow = sheet.createRow(2);
            Cell dateCell = dateRow.createCell(0);
            String startFormatted = dtf.format(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
            String endFormatted = dtf.format(LocalDateTime.ofInstant(end, ZoneId.systemDefault()));
            dateCell.setCellValue("Start Date: " + startFormatted + "    End Date: " + endFormatted);
            // Create an extra row for spacing (index 3)
            sheet.createRow(3);
        } else {
            // Create a blank row at index 2 for spacing 
            sheet.createRow(2);
        }
    }

    @Override
    protected void writeTableHeader() {
        // Write header row based on predetermined row index
        int headerRowIndex = (sheet.getRow(2) != null) ? sheet.getRow(2).getRowNum() : 2;
        Row headerRow = sheet.createRow(headerRowIndex);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    @Override
    protected void writeTableData(List<T> data) {
        // Data rows start after header row.
        int rowNum = (sheet.getRow(2) != null) ? 3 : 4;
        int serialNumber = 0;
        for (T item : data) {
            serialNumber++;
            Row row = sheet.createRow(rowNum++);
            Object[] rowData = extractor.getRowData(item, serialNumber);
            int cellNum = 0;
            for (Object cellData : rowData) {
                row.createCell(cellNum++).setCellValue((cellData != null) ? cellData.toString() : "");
            }
        }
    }

    @Override
    protected void finalizeReport() throws IOException {
        workbook.write(baos);
        workbook.close();
    }
}
