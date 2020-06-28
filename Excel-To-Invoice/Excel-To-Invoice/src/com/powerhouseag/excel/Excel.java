package com.powerhouseag.excel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.threeten.bp.LocalDate;

public class Excel {
	
	public Excel(String file) {
		this.file = file;
	}
	
	private String file;
	
	private InputStream is;
	private XSSFWorkbook wb;
	private XSSFSheet sheet;
	
	private Cell output;
	
	
	
	// get invoice date on invoice
	public LocalDate getInvoiceDate() {
		return LocalDate.ofEpochDay((long) find("Date:", 1).getNumericCellValue());
	}
	
	// get due date on invoice
	public LocalDate getDueDate() {
		//String DueDateLine;
		Integer DueDateDay;
		Integer DueDateMonth;
		Integer DueDateYear;
		LocalDate DueDateLocalDate;
		
		//DueDateLine=find("Payment is due on the ", 0).getStringCellValue();
		DueDateDay=Integer.valueOf("20");
		DueDateMonth=Integer.valueOf("06");
		DueDateYear=Integer.valueOf("2019");

		DueDateLocalDate=LocalDate.of(DueDateYear, DueDateMonth, DueDateDay);
		return DueDateLocalDate;
	}
	
	// get invoice number from invoice
	public String getInvoiceNo() {
		return find("Invoice No.:", 1).getStringCellValue();
	}
	
	// get sub total from invoice
	public double getSubTotal() {
		return find("Sub total", 5).getNumericCellValue();
	}
	
	// get name of recipient from invoice
	public String getName() {
		return find("To:", 1).getStringCellValue();
	}
	
	// check if the invoice is to be paid over 12 months
	public boolean getRepeating() {
		boolean repeating=false;
		try{
			if(find("Monthly amount due by automatic payment (total divided by 12 months)", 5)!=null) {
			
			repeating=true;
			}else {
				repeating=false;
			}
		}catch(NullPointerException e) {
			System.out.println("Search for repeating invoice failed");
		}
		return repeating;
	}

	// find text within an excel file
	private Cell find(String text, int right) {
		
		output = null;
		
		// import excel file
		try {
			is = new FileInputStream(file);
			wb = new XSSFWorkbook(is);
			sheet = wb.getSheetAt(0);
			wb.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//iterate through spreadsheet looking for values
		for(int y = 0; y < 80; y++) {
			Row r = sheet.getRow(y);
			for(int x = 0; x < 8; x++) {
				try {
					if(r.getCell(x).getCellType() == CellType.STRING) {
						if(r.getCell(x).getStringCellValue().equals(text)) {
							output = r.getCell(x+right);
						}
					}
				}catch(NullPointerException e) {}
			}
		}
		return output;
	}
}
