package com.powerhouseag.xero;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.threeten.bp.LocalDate;

import com.powerhouseag.excel.Excel;
import com.xero.api.client.AccountingApi;
import com.xero.api.client.FilesApi;
import com.xero.models.accounting.Contact;
import com.xero.models.accounting.Invoice;
import com.xero.models.accounting.Invoice.StatusEnum;
import com.xero.models.accounting.Invoice.TypeEnum;
import com.xero.models.accounting.Invoices;
import com.xero.models.accounting.LineItem;
import com.xero.models.files.FileObject;
import com.xero.models.files.Files;

public class ProcessServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	// private final String UPLOAD_DIRECTORY = "./WEB-INF/files";
	private final String UPLOAD_DIRECTORY = "c:/temp/excel2invoiceuploads";
	private String[] failedProcessing = new String[100];
	private String[] failedProcessingReason = new String[100];
	private AccountingApi accountingApi;
	private FilesApi fileApi;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		// recieve uploaded files
		File[] files = recieve(request);

		// api to access xero database
		accountingApi = ApiStorage.getAccountingApi();
		fileApi = ApiStorage.getFileApi();

		// take excel data and put onto invoice
		Invoices serverInvoices = parseSingleExcel(files);

		// take excel data and put onto invoice
		//parseRepeatingExcel();

		// attach pdfs to invoices on server
		//attachPdfs(serverInvoices, files);

		// print to screen that transfer has succeeded
		printSuccess(response);

		// delete all files in upload directory
		deleteFiles();
	}



	//#######################################################
	//function to receive file from upload servlet
	private File[] recieve(HttpServletRequest request) {

		// array for files that were uploaded
		File[] files = null;

		// Receive files
		try {
			List<FileItem> multiparts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
			int noFiles = multiparts.size()-1;
			files = new File[noFiles];
			for(int i = 0;i < multiparts.size(); i++) {
				FileItem item = multiparts.get(i);
				if(!item.isFormField()){
					String name = new File(item.getName()).getName();
					item.write(files[i] = new File(UPLOAD_DIRECTORY + File.separator + name));
				}
			}
			//File uploaded successfully
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return files;
	}


	//############################################################
	// function to print to the screen that transfer has succeeded
	private void printSuccess(HttpServletResponse response) {
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
				if(failedProcessing[i]!=null) {
					out.println(failedProcessing[i]+" - Failed to extract "+failedProcessingReason[i]+"<br>");
					failed=true;
					failedProcessing[i]=null;
				}
			}

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
	}

	//#######################################
	// function to attach pdfs to invoices
	private void attachPdfs(Invoices serverInvoices, File[] files) throws IOException {
		for(int i = 0; i < files.length; i++) {
			
			//File pdf = new File(files[i].getAbsolutePath()+".pdf");
			/*
			try {
				ExcelToPdf.createPdf(pdf, files[i]);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			InputStream is = new FileInputStream(pdf);
			byte[] bytes = IOUtils.toByteArray(is);

			System.out.println(serverInvoices.getInvoices().get(i).getInvoiceID());

			try {
				accountingApi.createInvoiceAttachmentByFileName(serverInvoices.getInvoices().get(i).getInvoiceID(), pdf.getName(), bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			
			//accountingApi.createInvoiceAttachmentByFileName(serverInvoices.getInvoices().get(i).getInvoiceID(), arg1, arg2)
			//List<Folder> folders = fileApi.getFolders("Grazing to Sort");
			//Folder folder = folders.get(0);
			//System.out.println(folder.getFileCount());
			
			int pathCutoffLength = 5;
			
			String excelName = files[i].getName();
			String pdfName = excelName.substring(0, excelName.length()-pathCutoffLength)+".pdf";
			
			System.out.println(pdfName);
			
			UUID id = null;
			
			Files xeroFiles = fileApi.getFiles(null,null,pdfName);
			System.out.println(xeroFiles.getTotalCount());
			/*List<FileObject> xeroFilesList = xeroFiles.getItems();
			
			for(int x = 0; x < xeroFilesList.size(); x++) {
				System.out.println(xeroFilesList.get(x).getName());
				if(xeroFilesList.get(x).getName() == pdfName) {
					id = xeroFilesList.get(x).getId();
					break;
				}
			}
			
			ByteArrayInputStream fileContent = fileApi.getFileContent(id);
			byte[] fileBytes = fileContent.readAllBytes();

			accountingApi.createInvoiceAttachmentByFileName(serverInvoices.getInvoices().get(i).getInvoiceID(), pdfName, fileBytes);
			//System.out.println(file.getId());*/
		}
	}

	//#####################################################
	// function to delete all files in the upload directory
	private void deleteFiles() {
		File directory = new File(UPLOAD_DIRECTORY);
		File[] files = directory.listFiles();
		for(File file:files) {
			file.delete();
			System.out.println("Deleting file : "+file.getName());
		}
	}

	//########################################################################
	// function to take data from single excel sheet and put into xero invoice object
	private Invoices parseSingleExcel(File[] uploadFiles) {

		// invoices to get uploaded to xero
		Invoices invoices = new Invoices();
		
		// invoices from xero's servers
		Invoices serverInvoices = new Invoices();

		// files that were uploaded
		File[] files = uploadFiles;

		for(int i = 0; i < files.length; i++) {
			
			System.out.println(files[i].getName());

			// putting files into excel objects
			Excel excel = new Excel(files[i].getAbsolutePath());

			// date due and date made
			LocalDate dueDate;
			LocalDate madeDate;

			LocalDate date = LocalDate.now();

			// Base values used if info can't be found on excel sheet
			String name = "Someone";
			double subTotal = 0.00;
			String invoiceNumber = "An Invoice Number";

			// using try/catch to see if any files fail and keep processing others
			try {
				// get name from excel sheet
				name = excel.getName();
			}catch(NullPointerException e) {
				failedProcessing[i] = files[i].getName();
				failedProcessingReason[i] = "Name";
			}

			try {
				// get invoice number from excel sheet
				invoiceNumber = excel.getInvoiceNo();
			}catch(NullPointerException e) {
				failedProcessing[i] = files[i].getName();
				failedProcessingReason[i] = "Invoice No.";
			}

			try {
				// get date from excel sheet
				date = excel.getInvoiceDate();
			}catch(NullPointerException e) {
				failedProcessing[i] = files[i].getName();
				failedProcessingReason[i] = "Date";
			}

			try {
				// get sub total from excel sheet
				subTotal = excel.getSubTotal();
				
				//cow grazing
				//subTotal = excel.getSubTotal()/4;
				
				//annual heifer grazing
				//subTotal = excel.getSubTotal()/12;
				
			}catch(NullPointerException e) {
				failedProcessing[i] = files[i].getName();
				failedProcessingReason[i] = "Sub Total";
			}

			// extracting individual year, month, and day integers and adjusting to actual values
			//int year = date.getYear()-70;
			int year = 2019;
			//int month = date.getMonthValue()+1;
			int month = 6;
			//int day = date.getDayOfMonth()-1;
			int day = 20;

			// date object for date invoice was made
			//madeDate = LocalDate.of(year, month-1, day);
			//madeDate = LocalDate.of(year, month-1, day);
			madeDate = LocalDate.of(2020, 06, 01);

			// if month is bigger than 12 reset back to one and add year
			if(month>=13) {
				month-=12;
				year+=1;
			}

			// due date object gets set to 20th of next month
			//dueDate = LocalDate.of(year, month, 20);
			dueDate = LocalDate.of(2020, 06, 20);
			
			// line on invoice
			LineItem lineItem = new LineItem();

			lineItem.setLineAmount(subTotal);
			lineItem.setUnitAmount(subTotal);

			// invoice object
			Invoice invoice = new Invoice();

			//configuration for end of year final invoices
			//lineItem.setDescription("2019 Final");
			//lineItem.setQuantity(1.0);
			//lineItem.setTaxType("OUTPUT2");
			//lineItem.setAccountCode("1502"); // "1502 - Grazing - May-May Heifers"

			//configuration for start of year annual invoices
			lineItem.setDescription("May-May Grazing");
			lineItem.setQuantity(1.0);
			lineItem.setTaxType("OUTPUT2");
			lineItem.setAccountCode("1502"); // "1502 - Grazing - May-May Heifers"

			//configuration for start of year cow grazing invoices
//			lineItem.setDescription("2019 Winter Cow Grazing");
//			lineItem.setQuantity(1.0);
//			lineItem.setTaxType("OUTPUT2");
//			lineItem.setAccountCode("1400"); // "1400 - Grazing Income"

			// contact to send invoice to
			Contact contact = new Contact();
			contact.setName(name);

			// if invoice total is negative, invert total and change to accounts payable
			if(lineItem.getUnitAmount() >= 0.0) {
				invoice.setType(TypeEnum.ACCREC);
			}else {
				invoice.setType(TypeEnum.ACCPAY);
				lineItem.setUnitAmount(-lineItem.getUnitAmount());
				lineItem.setLineAmount(-lineItem.getLineAmount());
			}

			invoice.addLineItemsItem(lineItem);
			invoice.setContact(contact);
			invoice.setDueDate(dueDate);
			invoice.setDate(madeDate);
			invoice.setReference(invoiceNumber);
			invoice.setStatus(StatusEnum.SUBMITTED);

			// adding invoice items to the object that gets sent to the xero servers
			invoices.addInvoicesItem(invoice);
		}

		// add invoices to xero database
		try {
			serverInvoices = accountingApi.createInvoice(invoices, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return serverInvoices;
	}

	//########################################################################
	// function to take data from repeating excel sheet and put into xero invoice object
	/*private void parseRepeatingExcel() {

		// invoices to get uploaded to xero
		RepeatingInvoices invoices = new RepeatingInvoices();

		for(int i = 0; i < noFiles; i++) {

			// files that were uploaded
			fileObject = new File(UPLOAD_DIRECTORY+File.separator+fileNames[i]);

			// putting files into excel objects
			Excel excel = new Excel(fileObject.getAbsolutePath());

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
				date = excel.getInvoiceDate();
			}catch(NullPointerException e) {
				failedProcessing[i] = fileNames[i];
			}

			try {
				// get sub total from excel sheet
				subTotal = excel.getSubTotal()/4;
			}catch(NullPointerException e) {
				failedProcessing[i] = fileNames[i];
			}

			// extracting individual year, month, and day integers and adjusting to actual values
			//int year = date.getYear()-70;
			int year = 2019;
			//int month = date.getMonthValue()+1;
			int month = 6;
			//int day = date.getDayOfMonth()-1;
			int day = 20;

			// date object for date invoice was made
			//madeDate = LocalDate.of(year, month-1, day);
			madeDate = LocalDate.of(year, month-1, day);

			// if month is bigger than 12 reset back to one and add year
			if(month>=13) {
				month-=12;
				year+=1;
			}

			// line on invoice
			LineItem lineItem = new LineItem();

			RepeatingInvoice invoice = new RepeatingInvoice();
			lineItem.setLineAmount(subTotal/12);
			lineItem.setUnitAmount(subTotal/12);

			// due date object gets set to 20th of next month
			dueDate = LocalDate.of(year, month, 20);

			//configuration for end of year final invoices
			//				lineItem.setDescription("2019 Final");
			//				lineItem.setQuantity(1.0);
			//				lineItem.setTaxType("OUTPUT2");
			//				lineItem.setAccountCode("1502"); // "1502 - Grazing - May-May Heifers"

			//configuration for start of year annual invoices
			//lineItem.setDescription("May-May Grazing");
			//lineItem.setQuantity(1.0);
			//lineItem.setTaxType("OUTPUT2");
			//lineItem.setAccountCode("1502"); // "1502 - Grazing - May-May Heifers"

			//configuration for start of year cow grazing invoices
			lineItem.setDescription("2019 Winter Cow Grazing");
			lineItem.setQuantity(1.0);
			lineItem.setTaxType("OUTPUT2");
			lineItem.setAccountCode("1400"); // "1400 - Grazing Income"

			// contact to send invoice to
			Contact contact = new Contact();
			contact.setName(name);

			// if invoice total is negative, invert total and change to accounts payable
			if(lineItem.getUnitAmount() >= 0.0) {
				invoice.setType(RepeatingInvoice.TypeEnum.ACCREC);
			}else {
				invoice.setType(RepeatingInvoice.TypeEnum.ACCPAY);
				lineItem.setUnitAmount(-lineItem.getUnitAmount());
				lineItem.setLineAmount(-lineItem.getLineAmount());
			}

			// schedule for repeating invoices
			Schedule schedule = new Schedule();
			schedule.setUnit(UnitEnum.MONTHLY);
			schedule.setStartDate(madeDate);
			schedule.setEndDate(dueDate);

			// set invoice parameters
			invoice.addLineItemsItem(lineItem);
			invoice.setContact(contact);
			invoice.setReference(invoiceNumber);
			invoice.setStatus(RepeatingInvoice.StatusEnum.AUTHORISED);
			invoice.setSchedule(schedule);

			// adding invoice items to object that gets sent to the xero servers
			invoices.addRepeatingInvoicesItem(invoice);
		}

		// add invoices to xero database
		/*
			try {
				serverInvoices = accountingApi.createInvoice(invoices, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		 //
	}*/


}
