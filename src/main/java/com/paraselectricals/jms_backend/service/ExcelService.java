package com.paraselectricals.jms_backend.service;

import com.paraselectricals.jms_backend.entity.Job;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
public class ExcelService {

    private final String FILE_PATH = "reports/JobMasterSheet.xlsx";
    private final String[] COLUMNS = {
        "Job ID", "Date", "Client", "Contact Person", "Phone", 
        "Motor Type", "Pole", "Capacity", "Voltage", "Weight", 
        "Problem Reported", "Priority", "Expected Delivery", "Start Date"
    };

    public synchronized void appendJobToExcel(Job job) {
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        File file = new File(FILE_PATH);
        Workbook workbook;
        Sheet sheet;

        try {
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
                fis.close();
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Jobs");
                Row headerRow = sheet.createRow(0);
                
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (int i = 0; i < COLUMNS.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(COLUMNS[i]);
                    cell.setCellStyle(headerStyle);
                }
            }

            int lastRowNum = sheet.getLastRowNum();
            Row row = sheet.createRow(lastRowNum + 1);

            row.createCell(0).setCellValue(job.getJobId());
            row.createCell(1).setCellValue(job.getReceivedDate() != null ? job.getReceivedDate().toString() : "");
            row.createCell(2).setCellValue(job.getClientName());
            row.createCell(3).setCellValue(job.getContactPerson() != null ? job.getContactPerson() : "");
            row.createCell(4).setCellValue(job.getPhoneNumber() != null ? job.getPhoneNumber() : "");
            row.createCell(5).setCellValue(job.getMotorType() != null ? job.getMotorType() : "");
            row.createCell(6).setCellValue(job.getPole() != null ? job.getPole() : "");
            row.createCell(7).setCellValue(job.getCapacity() != null ? job.getCapacity() : "");
            row.createCell(8).setCellValue(job.getVoltage() != null ? job.getVoltage() : "");
            row.createCell(9).setCellValue(job.getWeight() != null ? job.getWeight() : "");
            row.createCell(10).setCellValue(job.getProblemReported() != null ? job.getProblemReported() : "");
            row.createCell(11).setCellValue(job.getPriority() != null ? job.getPriority() : "");
            row.createCell(12).setCellValue(job.getExpectedDeliveryDate() != null ? job.getExpectedDeliveryDate().toString() : "");
            row.createCell(13).setCellValue(job.getStartDate() != null ? job.getStartDate().toString() : "");

            // Auto-size columns
            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            FileOutputStream fos = new FileOutputStream(FILE_PATH);
            workbook.write(fos);
            fos.close();
            workbook.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public synchronized byte[] generateFullExcel(java.util.List<Job> jobs) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Jobs");
        Row headerRow = sheet.createRow(0);
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < COLUMNS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(COLUMNS[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Job job : jobs) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(job.getJobId());
            row.createCell(1).setCellValue(job.getReceivedDate() != null ? job.getReceivedDate().toString() : "");
            row.createCell(2).setCellValue(job.getClientName());
            row.createCell(3).setCellValue(job.getContactPerson() != null ? job.getContactPerson() : "");
            row.createCell(4).setCellValue(job.getPhoneNumber() != null ? job.getPhoneNumber() : "");
            row.createCell(5).setCellValue(job.getMotorType() != null ? job.getMotorType() : "");
            row.createCell(6).setCellValue(job.getPole() != null ? job.getPole() : "");
            row.createCell(7).setCellValue(job.getCapacity() != null ? job.getCapacity() : "");
            row.createCell(8).setCellValue(job.getVoltage() != null ? job.getVoltage() : "");
            row.createCell(9).setCellValue(job.getWeight() != null ? job.getWeight() : "");
            row.createCell(10).setCellValue(job.getProblemReported() != null ? job.getProblemReported() : "");
            row.createCell(11).setCellValue(job.getPriority() != null ? job.getPriority() : "");
            row.createCell(12).setCellValue(job.getExpectedDeliveryDate() != null ? job.getExpectedDeliveryDate().toString() : "");
            row.createCell(13).setCellValue(job.getStartDate() != null ? job.getStartDate().toString() : "");
        }

        for (int i = 0; i < COLUMNS.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            workbook.write(bos);
            workbook.close();
            return bos.toByteArray();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
