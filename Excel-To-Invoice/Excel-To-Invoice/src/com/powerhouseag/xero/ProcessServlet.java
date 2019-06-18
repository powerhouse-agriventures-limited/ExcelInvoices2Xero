package com.powerhouseag.xero;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.threeten.bp.LocalDate;

import com.powerhouseag.excel.Excel;
import com.xero.api.client.AccountingApi;
import com.xero.models.accounting.Contact;
import com.xero.models.accounting.Invoice;
import com.xero.models.accounting.Invoice.StatusEnum;
import com.xero.models.accounting.Invoice.TypeEnum;
import com.xero.models.accounting.Invoices;
import com.xero.models.accounting.LineItem;

public class ProcessServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private final String UPLOAD_DIRECTORY = "./WEB-INF/files";
	private int noFiles;
	private String[] files;
	private String[] failedProcessing = new String[100];
	private File[] fileObjects = new File[100];
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {
		
		// recieve file
            try {
                List<FileItem> multiparts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
               noFiles = multiparts.size()-1;
               files = new String[noFiles];
                for(int i = 0;i < multiparts.size(); i++) {
            	   FileItem item = multiparts.get(i);
                    if(!item.isFormField()){
                        String name = new File(item.getName()).getName();
                        files[i] = name;
                        System.out.println(files[i]);
                        item.write( new File(UPLOAD_DIRECTORY + File.separator + name));
                    }
               }
               //File uploaded successfully
            } catch (Exception ex) {
               ex.printStackTrace();
            }
		
		AccountingApi accountingApi = ApiStorage.getApi();
		
		// objects for making invoice objects
		Excel[] excel = new Excel[noFiles];
		Invoice[] invoice = new Invoice[noFiles];
		LineItem[] lineItem = new LineItem[noFiles];
		Contact[] contact = new Contact[noFiles];
		
		// take excel data and put onto invoice
		for(int i = 0; i < noFiles; i++) {
			fileObjects[i] = new File(UPLOAD_DIRECTORY+File.separator+files[i]);
			excel[i] = new Excel(fileObjects[i].getAbsolutePath());
			
			LocalDate dueDate;
			LocalDate madeDate;
			
			// date created
			//date = LocalDate.of(excel[i].getYear()+1900, excel[i].getMonth()+3, excel[i].getDay()+1);
			//System.out.println(excel[i].getYear()+1900);
			//System.out.println(excel[i].getMonth()+3);
			//System.out.println(excel[i].getDay()+1);
			
			try {
				
			// line on invoice
			lineItem[i] = new LineItem();
			
			LocalDate date = excel[i].getDate();
			
			int year = date.getYear()-70;
			int month = date.getMonthValue()+1;
			int day = date.getDayOfMonth()-1;

			System.out.println(date.getYear());
			System.out.println(date.getMonthValue());
			System.out.println(date.getDayOfMonth());
			
			// if month is bigger than 12 reset back to one and add year
			if(month>=13) {
				month-=12;
				year+=1;

				madeDate = LocalDate.of(year, 1, day);
			}else {
				madeDate = LocalDate.of(year, month-1, day);
			}

			System.out.println(date.getYear());
			System.out.println(date.getMonthValue());
			System.out.println(date.getDayOfMonth());
			
			
			// if repeating subtotal is divided by 12
			if(excel[i].getRepeating()) {
				lineItem[i].setLineAmount(excel[i].getSubTotal()/12);
				lineItem[i].setUnitAmount(excel[i].getSubTotal()/12);
				
				// due date object #############################
				dueDate = LocalDate.of(year, month, 20);
				
			// else subtotal is as writen on invoice
			}else {
				lineItem[i].setLineAmount(excel[i].getSubTotal());
				lineItem[i].setUnitAmount(excel[i].getSubTotal());

				// due date object #############################
				dueDate = LocalDate.of(year, month, 20);
			}
			
			lineItem[i].setDescription("May-May Grazing");
			lineItem[i].setQuantity(1.0);
			lineItem[i].setTaxType("OUTPUT2");
			lineItem[i].setAccountCode("200");
		
			// contact to send invoice to
			contact[i] = new Contact();
			contact[i].setName(excel[i].getName());
			
			// invoice object
			invoice[i] = new Invoice();
			invoice[i].addLineItemsItem(lineItem[i]);
			invoice[i].setContact(contact[i]);
			invoice[i].setType(TypeEnum.ACCREC);
			invoice[i].setDueDate(dueDate); //####### Date and due date not working
			invoice[i].setDate(madeDate); //#########################
			invoice[i].setReference(excel[i].getInvoiceNo());
			invoice[i].setStatus(StatusEnum.SUBMITTED);
			
			}catch(NullPointerException e) {
				failedProcessing[i] = files[i];
			}
		}
		
		// invoices to create
		Invoices invoices = new Invoices();
		
		// adding invoice items to the object that gets sent to the xero servers
		for(int i = 0; i < noFiles; i++) {
		invoices.addInvoicesItem(invoice[i]);
		}
		
		// create invoice
		try {
			accountingApi.createInvoice(invoices, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        
	PrintWriter out;
	
	try {
		out = response.getWriter();

		out.println("<html>");
		out.println("<body>");
		out.println("All File(s) Uploaded and Processed<br>");
		out.println("<br>");
		out.println("File(s) failed, check invoices:<br>");
		for(String file:failedProcessing) {
			if(file!=null) {
				out.println(file+"<br>");
			}else {
				out.println("No Failed Files<br>");
				break;
			}
		}
		out.println("<br>");
		out.println("<form action=\"./upload\">");
		out.println("<input type=\"submit\" value=\"Upload more File(s)\">");
		out.println("</form>");
		out.println("</body>");
		out.println("</html>");
		
	} catch (IOException e1) {
		e1.printStackTrace();
	}
	
	// delete all uploaded files
	for(File file:fileObjects) {
		if(file!=null) {
			file.delete();
		}
	}
	
	}

}
