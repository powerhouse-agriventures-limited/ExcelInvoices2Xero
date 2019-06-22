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
import com.xero.models.accounting.Attachment;
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
	private String[] fileNames;
	private String[] failedProcessing = new String[100];
	private File fileObject;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) {

		// Receive file
		try {
			List<FileItem> multiparts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
			noFiles = multiparts.size()-1;
			fileNames = new String[noFiles];
			for(int i = 0;i < multiparts.size(); i++) {
				FileItem item = multiparts.get(i);
				if(!item.isFormField()){
					String name = new File(item.getName()).getName();
					fileNames[i] = name;
					System.out.println(fileNames[i]);
					item.write( new File(UPLOAD_DIRECTORY + File.separator + name));
				}
			}
			//File uploaded successfully
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		//######################################################################################################

		// api to access xero database
		AccountingApi accountingApi = ApiStorage.getApi();

		// objects for making invoice objects
		Excel excel;
		Invoice invoice = new Invoice();
		LineItem lineItem = new LineItem();
		Contact contact = new Contact();

		// invoices to create
		Invoices invoices = new Invoices();

		// take excel data and put onto invoice
		for(int i = 0; i < noFiles; i++) {

			// files that were uploaded
			fileObject = new File(UPLOAD_DIRECTORY+File.separator+fileNames[i]);

			// putting files into excel objects
			excel = new Excel(fileObject.getAbsolutePath());

			// date due and date made
			LocalDate dueDate;
			LocalDate madeDate;

			LocalDate date = LocalDate.now();

			String name = "Someone";
			double subTotal = 0.00;
			String invoiceNumber = "An Invoice Number";

			// using try/catch to see if any files fail and keep processing others
			try {
				// get name from excel sheet
				name = excel.getName();
			}catch(NullPointerException e) {
				failedProcessing[i] = fileNames[i];
			}
			
			try {
				// get invoice number from excel sheet
				invoiceNumber = excel.getInvoiceNo();
			}catch(NullPointerException e) {
				failedProcessing[i] = fileNames[i];
			}

			try {
				// get date from excel sheet
				date = excel.getDate();
			}catch(NullPointerException e) {
				failedProcessing[i] = fileNames[i];
			}

			try {
				// get sub total from excel sheet
				subTotal = excel.getSubTotal();
			}catch(NullPointerException e) {
				failedProcessing[i] = fileNames[i];
			}

			// extracting individual year, month, and day integers and adjusting to actual values
			int year = date.getYear()-70;
			int month = date.getMonthValue()+1;
			int day = date.getDayOfMonth()-1;

			// date object for date invoice was made
			madeDate = LocalDate.of(year, month-1, day);

			// if month is bigger than 12 reset back to one and add year
			if(month>=13) {
				month-=12;
				year+=1;
			}

			// if repeating invoice sub total is divided by 12
			if(excel.getRepeating()) {
				lineItem.setLineAmount(subTotal/12);
				lineItem.setUnitAmount(subTotal/12);

				// due date object gets set to 20th of next month
				dueDate = LocalDate.of(year, month, 20);

				// else sub total is as written on invoice
			}else {
				lineItem.setLineAmount(subTotal);
				lineItem.setUnitAmount(subTotal);

				// due date object gets set to 20th of next month
				dueDate = LocalDate.of(year, month, 20);
			}

			// adding other attributes to line on invoice
			lineItem.setDescription("May-May Grazing");
			lineItem.setQuantity(1.0);
			lineItem.setTaxType("OUTPUT2");
			lineItem.setAccountCode("200");

			// contact to send invoice to
			contact = new Contact();
			contact.setName(name);
			
			// pdf attachment
			Attachment attachment = new Attachment();

			// invoice object
			invoice = new Invoice();
			invoice.addLineItemsItem(lineItem);
			invoice.setContact(contact);
			if(lineItem.getUnitAmount() >= 0.0) {
				invoice.setType(TypeEnum.ACCREC);
			}else {
				invoice.setType(TypeEnum.ACCPAY);
				lineItem.setUnitAmount(-lineItem.getUnitAmount());
			}
			invoice.setDueDate(dueDate);
			invoice.setDate(madeDate);
			invoice.setReference(invoiceNumber);
			invoice.setStatus(StatusEnum.SUBMITTED);
			invoice.addAttachmentsItem(attachment);

			// adding invoice items to the object that gets sent to the xero servers
			invoices.addInvoicesItem(invoice);
		}
		
//###########################################################################################################

		// add invoices to xero database
		try {
			accountingApi.createInvoice(invoices, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// printwriter to output to screen
		PrintWriter out;

		try {
			
			boolean failed = false;
			out = response.getWriter();

			out.println("<html>");
			out.println("<body>");
			out.println("All File(s) Uploaded and Processed<br>");
			out.println("<br>");
			out.println("<br>");
			out.println("File(s) failed, check invoices:<br>");
			
			for(int i = 0; i < failedProcessing.length; i++) {
				System.out.println(failedProcessing[i]);
				if(failedProcessing[i]!=null) {
					out.println(failedProcessing[i]+"<br>");
					failed=true;
					failedProcessing[i]=null;
				}
				System.out.println(failedProcessing[i]);
				System.out.println("End");
			}

			System.out.println("End End");
			if(!failed) {
				out.println("No Failed Files<br>");
			}
			
			out.println("<br>");
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
		File directory = new File(UPLOAD_DIRECTORY);

		File[] files = directory.listFiles();
		for(File file:files) {
			file.delete();
		}

	}

}
