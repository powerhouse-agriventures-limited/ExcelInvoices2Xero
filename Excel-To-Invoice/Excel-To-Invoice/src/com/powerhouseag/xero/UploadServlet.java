package com.powerhouseag.xero;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UploadServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		
		PrintWriter out;
		
		try {
			out = response.getWriter();
			
			out.println("<html>");
			out.println("<body>");
			
			out.println("<form action=\"./process\" method=\"post\" enctype=\"multipart/form-data\">");
			out.println("Select Excel File(s) to upload:");
			out.println("<input type=\"file\" accept=\".xlsm\" name=\"fileToUpload\" id=\"fileToUpload\" multiple=\"multiple\">");
			out.println("<br />");
			out.println("<input type=\"submit\" value=\"Upload File(s)\" name=\"submit\">");
		    out.println("</form>");
			
			out.println("</body>");
			out.println("</html>");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
