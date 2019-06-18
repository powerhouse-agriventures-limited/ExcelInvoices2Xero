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
	
	public LocalDate getDate() {
		LocalDate date;
		date = LocalDate.ofEpochDay((long) find("Date:", 1).getNumericCellValue());
		return date;
	}
	
	public String getInvoiceNo() {
		return find("Invoice No.:", 1).getStringCellValue();
	}
	
	public double getSubTotal() {
		return find("Sub total", 5).getNumericCellValue();
	}
	
	public String getName() {
		return find("To:", 1).getStringCellValue();
	}
	
	public boolean getRepeating() {
		boolean repeating;
		if(find("Monthly amount due by automatic payment (total divided by 12 months)", 5)!=null) {
		repeating=true;
		}else {
			repeating=false;
		}
		return repeating;
	}

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
		for(int y = 0; y < 60; y++) {
			Row r = sheet.getRow(y);
			for(int x = 0; x < 8; x++) {
				try {
					if(r.getCell(x).getCellType() == CellType.STRING) {
						if(r.getCell(x).getStringCellValue().equals(text)) {
							//System.out.println(text+" Found: "+r.getCell(x));
							//System.out.println(r.getCell(x));
							output = r.getCell(x+right);
						}
					}
				}catch(NullPointerException e) {
					
				}
			}
		}
		return output;
	}
}
