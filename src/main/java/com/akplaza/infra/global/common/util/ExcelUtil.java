package com.akplaza.infra.global.common.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

    /**
     * 🌟 엑셀 다운로드 공통 메서드
     */
    public static void download(HttpServletResponse response, String fileName, List<String> headers,
            List<List<Object>> data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        // 1. 헤더 행 생성 및 스타일 적용
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setWrapText(true); // Alt+Enter 효과 적용
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000); // 기본 열 너비
        }

        // 2. 데이터 행 생성
        int rowIdx = 1;
        for (List<Object> rowData : data) {
            Row row = sheet.createRow(rowIdx++);
            for (int i = 0; i < rowData.size(); i++) {
                Cell cell = row.createCell(i);
                if (rowData.get(i) != null) {
                    cell.setCellValue(rowData.get(i).toString());
                    cell.setCellStyle(dataStyle);
                }
            }
        }

        // 3. 브라우저 응답 헤더 설정
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + ".xlsx\"");

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 🌟 엑셀 업로드(파싱) 공통 메서드
     */
    public static List<List<String>> readExcel(MultipartFile file) throws IOException {
        List<List<String>> data = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            List<String> rowData = new ArrayList<>();
            // 마지막 셀 인덱스까지 순회하여 빈 셀도 빈 문자열로 처리
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellType(CellType.STRING); // 모든 데이터를 안전하게 문자로 취급
                rowData.add(cell.getStringCellValue().trim());
            }
            data.add(rowData);
        }
        workbook.close();
        return data;
    }
}