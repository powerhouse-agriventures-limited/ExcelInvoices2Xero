package com.powerhouseag.xero;

import com.xero.api.client.AccountingApi;

public class ApiStorage {

	private static AccountingApi accApi = null;
	
	public static void setAccountingApi(AccountingApi api) {
		ApiStorage.accApi = api;
	}
	
	public static AccountingApi getAccountingApi() {
		return accApi;
	}
}
