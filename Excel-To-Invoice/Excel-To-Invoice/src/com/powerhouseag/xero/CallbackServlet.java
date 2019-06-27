package com.powerhouseag.xero;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xero.api.ApiClient;
import com.xero.api.Config;
import com.xero.api.JsonConfig;
import com.xero.api.OAuthAccessToken;
import com.xero.api.client.AccountingApi;
import com.xero.api.client.FilesApi;


@WebServlet("/CallbackServlet")
public class CallbackServlet extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	
	public CallbackServlet() 
	{
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{	
		TokenStorage storage = new TokenStorage();
		String verifier = request.getParameter("oauth_verifier");

		try {
			Config config = JsonConfig.getInstance();
			
			OAuthAccessToken accessToken = new OAuthAccessToken(config);
			
			accessToken.build(verifier,storage.get(request,"tempToken"),storage.get(request,"tempTokenSecret")).execute();
			
			if(!accessToken.isSuccess()) {
				storage.clear(response);
				request.getRequestDispatcher("index.jsp").forward(request, response);
			} else {
				
				storage.save(response,accessToken.getAll());
				
				ApiClient apiClientForAccounting = new ApiClient(config.getApiUrl(),null,null,null);
				
				AccountingApi accountingApi = new AccountingApi(apiClientForAccounting);
				accountingApi.setOAuthToken(accessToken.getToken(), accessToken.getTokenSecret());
				
				ApiStorage.setAccountingApi(accountingApi);
				
				FilesApi fileApi = new FilesApi(apiClientForAccounting);
				fileApi.setOAuthToken(accessToken.getToken(), accessToken.getTokenSecret());
				
				ApiStorage.setFilesApi(fileApi);
				
				response.sendRedirect("./upload");
				
			}
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}	
}		