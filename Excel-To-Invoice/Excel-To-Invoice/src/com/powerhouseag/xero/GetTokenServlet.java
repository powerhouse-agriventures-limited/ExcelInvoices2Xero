package com.powerhouseag.xero;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xero.api.Config;
import com.xero.api.JsonConfig;
import com.xero.api.OAuthAuthorizeToken;
import com.xero.api.OAuthRequestToken;

@WebServlet("/RequestTokenServlet")
public class GetTokenServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public GetTokenServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			Config config = JsonConfig.getInstance();
			
			OAuthRequestToken requestToken = new OAuthRequestToken(config);
			requestToken.execute();	
			
			TokenStorage storage = new TokenStorage();
			storage.save(response,requestToken.getAll());

			OAuthAuthorizeToken authToken = new OAuthAuthorizeToken(config, requestToken.getTempToken());
			response.sendRedirect(authToken.getAuthUrl());	
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}

	}
}