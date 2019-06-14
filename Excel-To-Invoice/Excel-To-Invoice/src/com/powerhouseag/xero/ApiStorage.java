package com.powerhouseag.xero;

import com.xero.api.client.AccountingApi;

public class ApiStorage {

	private static AccountingApi api = null;
	
	public static void setApi(AccountingApi api) {
		ApiStorage.api = api;
	}
	
	public static AccountingApi getApi() {
		return api;
	}
}
