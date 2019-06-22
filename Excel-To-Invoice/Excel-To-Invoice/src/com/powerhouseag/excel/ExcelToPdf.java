package com.powerhouseag.excel;

import java.io.File;
import java.io.FileInputStream;

import com.aspose.cells.PageSetup;
import com.aspose.cells.PaperSizeType;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;



public class ExcelToPdf {

	public static void createPdf(File dest, File input) throws Exception {
		FileInputStream input_document = new FileInputStream(input);
        Workbook wb = new Workbook(input_document);
        Worksheet ws = wb.getWorksheets().get(0);
        PageSetup ps = ws.getPageSetup();
        ps.setPaperSize(PaperSizeType.PAPER_A_4);
        ps.setBottomMargin(0.0);
        ps.setTopMargin(0.0);
        ps.setLeftMargin(0.0);
        ps.setRightMargin(0.0);
        wb.save(dest.getAbsolutePath(), SaveFormat.PDF);
	}
	
}
