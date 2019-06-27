package com.powerhouseag.xero;

import com.xero.api.client.AccountingApi;
import com.xero.api.client.FilesApi;

public class ApiStorage {

	private static AccountingApi accApi = null;
	private static FilesApi fileApi = null;
	
	public static void setAccountingApi(AccountingApi api) {
		ApiStorage.accApi = api;
	}
	
	public static AccountingApi getAccountingApi() {
		return accApi;
	}
	
	public static void setFilesApi(FilesApi api) {
		ApiStorage.fileApi = api;
	}
	
	public static FilesApi getFilesApi() {
		return fileApi;
	}
}
