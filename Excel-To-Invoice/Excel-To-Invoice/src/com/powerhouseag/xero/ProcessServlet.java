package com.powerhouseag.xero;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.util.IOUtils;
import org.threeten.bp.LocalDate;

import com.powerhouseag.excel.Excel;
import com.xero.api.client.AccountingApi;
import com.xero.api.client.FilesApi;
import com.xero.models.accounting.Contact;
import com.xero.models.accounting.Invoice;
import com.xero.models.accounting.Invoice.StatusEnum;
import com.xero.models.accounting.Invoice.TypeEnum;
import com.xero.models.accounting.Invoices;
import com.xero.models.accounting.LineAmountTypes;
import com.xero.models.accounting.LineItem;
import com.xero.models.files.FileObject;
import com.xero.models.files.Files;

public class ProcessServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	// private final String UPLOAD_DIRECTORY = "./WEB-INF/files";
	private final String UPLOAD_DIRECTORY = "c:/temp/excel2invoiceuploads";
	private boolean[] failedProcessing;
	private boolean[][] failedProcessingReason;
	private boolean[] failedPdf;
	private AccountingApi accountingApi;
	private FilesApi fileApi;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		// recieve uploaded files
		File[] files = recieve(request);
		
		// failed files arrays
		failedProcessing = new boolean[files.length];
		failedProcessingReason = new boolean[files.length][4];

		// api to access xero database
		accountingApi = ApiStorage.getAccountingApi();
		fileApi = ApiStorage.getFileApi();

		// take excel data and put onto invoice
		Invoices serverInvoices = parseSingleExcel(files);

		// attach pdfs to invoices on server
		failedPdf = attachPdfs(serverInvoices, files);

		// print to screen that transfer has succeeded
		printSuccess(response, files);

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
	private void printSuccess(HttpServletResponse response, File[] files) {
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
			for(int i = 0; i < files.length; i++) {
				String message = "";
				if(failedProcessing[i]) {
					failed = true;
					message = files[i].getName()+" failed processing for:<br>";
					if(failedProcessingReason[i][0]) {
						message += "Name<br>";
					}
					if(failedProcessingReason[i][1]) {
						message += "Invoice No.<br>";
					}
					if(failedProcessingReason[i][2]) {
						message += "Date<br>";
					}
					if(failedProcessingReason[i][3]) {
						message += "Sub total<br>";
					}
				}
				if(failedPdf[i]) {
					if(message.equals("")) {
						message = files[i].getName()+" failed processing for:<br>";
					}
					failed = true;
					message += "Pdf attachment<br>";
				}
				out.print(message);
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
	private boolean[] attachPdfs(Invoices serverInvoices, File[] files) throws IOException {
		
		// list of all files on xero database
		ArrayList<FileObject> allFiles = new ArrayList<>();
		// list of files that couldn't find pdfs
		boolean[] errors = new boolean[files.length];
		
		// load all files into list
		for(int i = 0; i < 10; i++) {
			Files xeroFiles = fileApi.getFiles(100,i+1,null);
			List<FileObject> fileObjects = xeroFiles.getItems();
			allFiles.addAll(fileObjects);
		}
		
		System.out.println("Number of files: "+allFiles.size());
		
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
			
			// ammount of chars to trin off end of file name
			int pathCutoffLength = 5;
			
			// change name to a .pdf
			String excelName = files[i].getName();
			String pdfName = excelName.substring(0, excelName.length()-pathCutoffLength)+".pdf";
			
			System.out.println(pdfName);
			
			// file id
			UUID id = null;
			
			// search through all files for matching name
			for(int x = 0; x < allFiles.size(); x++) {
				if(allFiles.get(x).getName().contentEquals(pdfName)) {
					id = allFiles.get(x).getId();
					break;
				}
			}
			
			// if no file is found set error for that file to true
			if(id == null) {
				errors[i] = true;
			// else attach the file to invoice and delete original
			}else {
				errors[i] = false;
				ByteArrayInputStream fileContent = fileApi.getFileContent(id);
				byte[] fileBytes = IOUtils.toByteArray(fileContent);
	
				accountingApi.createInvoiceAttachmentByFileName(serverInvoices.getInvoices().get(i).getInvoiceID(), pdfName, fileBytes);
				//don't delete file for repeating invoices
				//fileApi.deleteFile(id);
			}
		}
		// return all errors
		return errors;
	}

	//#####################################################
	// function to delete all files in the upload directory
	private void deleteFiles() {
		File directory = new File(UPLOAD_DIRECTORY);
		File[] files = directory.listFiles();
		for(File file:files) {
			//comment this out when doing repeating invoices so can manually attach once have converted invoice to repeating in Xero
			//file.delete();
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
			LocalDate date = LocalDate.of(2020, 05, 01);
			LocalDate dueDate;
			LocalDate madeDate;
			
			boolean repeating = true;

			// Base values used if info can't be found on excel sheet
			String name = "Someone";
			double repeatingSubTotal = 0.00;
			double subTotal = 0.00;
			String invoiceNumber = "An Invoice Number";

			// using try/catch to see if any files fail and keep processing others
			try {
				// get name from excel sheet
				name = excel.getName();
			}catch(NullPointerException e) {
				failedProcessing[i] = true;
				failedProcessingReason[i][0] = true;
			}

			try {
				// get invoice number from excel sheet
				invoiceNumber = excel.getInvoiceNo();
			}catch(NullPointerException e) {
				failedProcessing[i] = true;
				failedProcessingReason[i][1] = true;
			}

			try {
				// get date from excel sheet
				date = excel.getInvoiceDate();
			}catch(NullPointerException e) {
				failedProcessing[i] = true;
				failedProcessingReason[i][2] = true;
			}

			try {
				// get sub total from excel sheet
				subTotal = excel.getSubTotal();
			}catch(NullPointerException e) {
				failedProcessing[i] = true;
				failedProcessingReason[i][3] = true;
			}
			
			try {
				// get repeating sub total from excel sheet
				repeatingSubTotal = excel.getRepeating();
			}catch(NullPointerException e) {
				repeating = false;
				failedProcessing[i] = true;
				failedProcessingReason[i][3] = true;
			}
			
			// date invoice was issued ############################################
			//madeDate = LocalDate.of(2020, 06, 01);
			madeDate = LocalDate.of(2020, date.getMonthValue(), date.getDayOfMonth());

			// date invoice is due ################################################
			dueDate = LocalDate.of(2020, 06, 20);
			
			// line on invoice
			LineItem lineItem = new LineItem();
			if(repeating) {
				lineItem.setLineAmount(repeatingSubTotal);
				lineItem.setUnitAmount(repeatingSubTotal);
			} else {
				lineItem.setLineAmount(subTotal);
				lineItem.setUnitAmount(subTotal);
			}

			// invoice object
			Invoice invoice = new Invoice();

			//configuration for start of year annual invoices ######################
			//lineItem.setDescription("May-May Grazing-2020 Final");
			lineItem.setDescription("May-May Grazing-" + name);
			//lineItem.setDescription(("Winter Cow Grazing-" + name ));
			lineItem.setQuantity(1.0);
			lineItem.setTaxType("OUTPUT2");
			
			//change AccountCode depending on what invoices are being uploaded
			lineItem.setAccountCode("1502"); // "1502 - Grazing - May-May Heifers"
			//lineItem.setAccountCode("1400"); // "1400 - Grazing Income"

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

			// add all elements to invoice object
			invoice.addLineItemsItem(lineItem);
			invoice.setContact(contact);
			invoice.setDueDate(dueDate);
			invoice.setDate(madeDate);
			invoice.setReference(invoiceNumber);
			invoice.setStatus(StatusEnum.SUBMITTED);
			if(repeating) {
				invoice.setLineAmountTypes(LineAmountTypes.INCLUSIVE);
			}else {
				invoice.setLineAmountTypes(LineAmountTypes.EXCLUSIVE);
			}
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
}