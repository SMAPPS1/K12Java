package com.bccl.dxapi.apiutility;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.*;
import java.io.*;

import java.net.ProtocolException;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64; //need jars to be put
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
//import org.slf4j.logger;
//import org.slf4j.loggerFactory;

import com.bccl.dxapi.controller.InternalportalController;



//import com.bccl.dxapi.apiimpl.ImageUploadImpl;

//import com.bccl.dxapi.bean.grnbean;
//import com.bccl.dxapi.bean.onexbean;

public class Onexinsert {

//	private static final logger logger=loggerFactory.getlogger(Onexinsert.class.getName());

	public static Connection con;
	public static PreparedStatement ps;
	public static ResultSet rs;
	// static InputStream input =
	// Onexinsert.class.getResourceAsStream("/dxproperties.properties");
	// static Properties prop = new Properties();
	// public static SqlConnectionAPI sqlCon = new SqlConnectionAPI();

	protected static String logFilePath;

	protected static Calendar cal;

	protected static PrintWriter pw;

	protected static String excepLogFilePath;

	static {
		disableSslVerification();
	}

	private static void disableSslVerification() {
		try {
			// Create a trust manager that does not validate certificate chains
			// TrustManager[] trustAllCerts = new TrustManager[] {
			// new X509TrustManager() {
			// public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			// return null;
			// }
			// public void checkClientTrusted(X509Certificate[] certs, String authType) {
			// }
			// public void checkServerTrusted(X509Certificate[] certs, String authType) {
			// }
			// }
			X509TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			X509TrustManager[] trustAllCerts = new X509TrustManager[] { tm };

			// trustAllCerts;

			// };

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// requisitionerid to be put in place of contactperson

		// String getpoinvoicesummary="Select DISTINCT i.DEPARTMENT,i.VENDORID AS "
		// +
		// "VENDORNAME,i.IRNNUMBER,i.IRNDATE,i.DESCRIPTION,i.ACTUALFILENAME,i.SAVEDFILENAME,p.INVOICENUMBER,p.PONUMBER,"
		// + "p.INVOICEDATE,p.AMOUNT AS INVOICEAMOUNT,p.REQUSITIONER AS POUSERID from "
		// + "poninvoicesummery p join INVOICEEVENTDETAILS i on
		// i.INVOICENUMBER=p.INVOICENUMBER "
		// + "AND i.PONUMBER=p.PONUMBER where p.OVERALLSTATUS=? AND p.PGQ = ? AND
		// p.ONEXSTATUS=? ";
		//	    writeLog("Onex Integration Started..",1);
//		String getpoinvoicesummary = "Select DISTINCT p.DEPARTMENT,"
//				+ "to_char(to_number(p.VENDORID))AS VENDORNAME,	"
//				+ "p.IRNNUMBER,to_CHAR(p.IRNDATE,'DD.MM.RRRR') as IRNDATE,p.DESCRIPTION,"
//				+ "p.ACTUALFILENAME,p.SAVEDFILENAME,p.INVOICENUMBER,"
//				+ "p.PONUMBER,to_CHAR(p.INVOICEDATE,'DD.MM.RRRR') as INVOICEDATE,"
//				+ "p.AMOUNT AS INVOICEAMOUNT,pd.TAX_CODE,p.REQUSITIONER AS POUSERID "
//				+ "from poninvoicesummery p join POEVENTDETAILS pd on pd.PONUMBER=p.PONUMBER "
//				+ "where p.OVERALLSTATUS=? AND p.UNIQUEREFERENCENUMBER IS NOT NULL AND p.ONEXSTATUS=?";
//		String getgrnnumber = "select * from GRNMAPPING g where g.INVOICENUMBER=? AND g.PONUMBER=?";
		String getpoinvoicesummary ="Select DISTINCT p.DEPARTMENT,"
				+ "to_char(to_number(p.VENDORID))AS VENDORNAME,p.IRNNUMBER,"
				+ "to_CHAR(p.IRNDATE,'DD.MM.RRRR') as IRNDATE,p.DESCRIPTION,p.ACTUALFILENAME,"
				+ "p.SAVEDFILENAME,p.INVOICENUMBER,p.PONUMBER,p.UNIQUEREFERENCENUMBER,p.ENDUSERREMARKS,"
				+ "p.BILLOFLADINGDATE,to_CHAR(p.INVOICEDATE,'DD.MM.RRRR') as INVOICEDATE,p.AMOUNT AS INVOICEAMOUNT,"
				+ "pd.TAX_CODE,p.REQUSITIONER AS POUSERID from poninvoicesummery p join POEVENTDETAILS "
				+ "pd on pd.PONUMBER=p.PONUMBER where p.OVERALLSTATUS=? AND "
				+ "p.UNIQUEREFERENCENUMBER IS NOT NULL AND p.ONEXSTATUS=?";
		
		String geturnnumber = "select distinct SAPUNIQUEREFERENCENO,TO_CHAR(TO_NUMBER(SAPLINEITEMNO), 'fm0000') AS SAPLINEITEMNO,PONUMBER,"
				+ "INVOICENUMBER from DELIVERYSUMMARY g where g.INVOICENUMBER=? AND g.PONUMBER=? AND SAPUNIQUEREFERENCENO IS NOT NULL "
				+ "AND SAPLINEITEMNO IS NOT NULL";
		ArrayList<HashMap<String, String>> internallist = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> internalgrnlist = new ArrayList<HashMap<String, String>>();
		try {
			con = getConnection();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			//			writeLog("Exception in connectin . Method:'main' ",1);
			e2.printStackTrace();
		}
		try {
			ps = con.prepareStatement(getpoinvoicesummary);
			ps.setString(1, "A");
			//ps.setInt(2, 0);
			ps.setString(2, "R");
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poEvent = new HashMap<String, String>();
				poEvent.put("DEPARTMENT", rs.getString("DEPARTMENT"));
				poEvent.put("VENDORNAME", rs.getString("VENDORNAME"));
				poEvent.put("IRNNUMBER", rs.getString("IRNNUMBER"));
				poEvent.put("IRNDATE", rs.getString("IRNDATE"));
				poEvent.put("DESCRIPTION", rs.getString("DESCRIPTION"));
				poEvent.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				poEvent.put("PONUMBER", rs.getString("PONUMBER"));
				poEvent.put("INVOICEDATE", rs.getString("INVOICEDATE"));
				poEvent.put("INVOICEAMOUNT", rs.getString("INVOICEAMOUNT"));
				poEvent.put("POUSERID", rs.getString("POUSERID"));
				poEvent.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
				poEvent.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
				poEvent.put("TAXCODE", rs.getString("TAX_CODE"));
//				poEvent.put("UNIQUEREFERENCENUMBER", rs.getString("UNIQUEREFERENCENUMBER"));
				poEvent.put("ENDUSERREMARKS", rs.getString("ENDUSERREMARKS")!= null ?rs.getString("ENDUSERREMARKS").toString():"");
				poEvent.put("BILLOFLADINGDATE", rs.getString("BILLOFLADINGDATE"));
				

				internallist.add(poEvent);
			}
		} catch (SQLException e) {
			try {
				ps.close();
			} catch (SQLException e1) {
				//				writeLog("Exception while fetching data from poninvoicesummry . Method:'main' ",1);
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block

				e.printStackTrace();
			}
			// rs.close();
		}

		writeLog("POinvoice list is here ==> "+internallist.toString(),1);
		System.out.println("POinvoice list is here ==> " + internallist.toString());
		// System.out.println("GRN list is here ==> "+internalgrnlist.toString());
		List<HashMap<String, String>> onexlist = new ArrayList<HashMap<String, String>>();
		StringBuffer sb = new StringBuffer();
		
		for (int j = 0; j < internallist.size(); j++) {
			String sheetid = "";
			
			try {
				ps = con.prepareStatement(geturnnumber);
				ps.setString(1, internallist.get(j).get("INVOICENUMBER"));
				ps.setString(2, internallist.get(j).get("PONUMBER"));
				rs = ps.executeQuery();
				while (rs.next()) {

					HashMap<String, String> poEvent = new HashMap<String, String>();
					poEvent.put("PONUMBER", rs.getString("PONUMBER"));
//					poEvent.put("GRNNUMBER", rs.getString("GRNNUMBER"));
					poEvent.put("SAPUNIQUEREFERENCENO", rs.getString("SAPUNIQUEREFERENCENO"));
					poEvent.put("SAPLINEITEMNO", rs.getString("SAPLINEITEMNO"));
//					poEvent.put("GRNQTY", rs.getString("GRNQTY"));
//					poEvent.put("LINEITEMNO", rs.getString("LINEITEMNO"));
//					poEvent.put("DCNUMBER", rs.getString("DCNUMBER"));
					poEvent.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
//					poEvent.put("STATUS", rs.getString("STATUS"));
//					poEvent.put("CREATEDON", rs.getString("CREATEDON"));
					internalgrnlist.add(poEvent);
				}

			} catch (SQLException e) {
				try {
					ps.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			} finally {
				try {
					ps.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// rs.close();
			}
			if(internalgrnlist.size()>0)
			{
				String getuniquenumber = "select (to_char(sysdate,'yyyymmdd') || ONEX_SEQ.nextval) as expensesheetid from dual";
//				String getuniquenumber =internallist.get(j).get("UNIQUEREFERENCENUMBER");
				try {
					ps = con.prepareStatement(getuniquenumber);
					rs = ps.executeQuery();
					while (rs.next()) {
						sheetid = rs.getString("expensesheetid");
					}
				} catch (Exception e) {
					writeLog("Exception while fetching data from grnsummary . Method:'main' ",1);
					e.printStackTrace();
				}
				LinkedHashMap<String, String> ob1 = new LinkedHashMap<String, String>();
				ob1.put("ExpenseSheetId", sheetid);
				if (internallist.get(j).get("IRNNUMBER") == null) {
					ob1.put("InvoiceType", "1");
					ob1.put("IRNDATE", "");
					ob1.put("IRNNUMBER", "");
				} else {
					ob1.put("IRNDATE", internallist.get(j).get("IRNDATE"));
					ob1.put("IRNNUMBER", internallist.get(j).get("IRNNUMBER"));
					ob1.put("InvoiceType", "2");
				}
				ob1.put("SupplierGSTIN", "GST0987654321"); // to send gst from bussiness attributes table.

				ob1.put("PoNo", internallist.get(j).get("PONUMBER") != null ? internallist.get(j).get("PONUMBER") : "");
				ob1.put("POUserId", internallist.get(j).get("POUSERID") != null ? internallist.get(j).get("POUSERID") : "");
				ob1.put("Department",
						internallist.get(j).get("DEPARTMENT") != null ? internallist.get(j).get("DEPARTMENT") : "");
				ob1.put("VendorName",
						internallist.get(j).get("VENDORNAME") != null ? internallist.get(j).get("VENDORNAME") : "");
				ob1.put("BillofLadingDate", internallist.get(j).get("BILLOFLADINGDATE") != null ? internallist.get(j).get("BILLOFLADINGDATE") : "");
				ob1.put("InvoiceDate",
						internallist.get(j).get("INVOICEDATE") != null ? internallist.get(j).get("INVOICEDATE") : "");
				ob1.put("InvoiceNo",
						internallist.get(j).get("INVOICENUMBER") != null ? internallist.get(j).get("INVOICENUMBER") : "");
				ob1.put("InvoiceAmount",
						internallist.get(j).get("INVOICEAMOUNT") != null ? internallist.get(j).get("INVOICEAMOUNT") : "");
				ob1.put("IRNDate", internallist.get(j).get("IRNDATE"));
				ob1.put("IRNNO", internallist.get(j).get("IRNNUMBER"));
				ob1.put("Remark",
						internallist.get(j).get("DESCRIPTION") != null ? "Description : "+internallist.get(j).get("DESCRIPTION")+" , BCCL User Remarks : "+ internallist.get(j).get("ENDUSERREMARKS"): "");
				ob1.put("ACTUALFILENAME", internallist.get(j).get("ACTUALFILENAME"));
				ob1.put("SAVEDFILENAME", internallist.get(j).get("SAVEDFILENAME"));
				//ob1.put("REF3", internallist.get(j).get("ENDUSERREMARKS"));  
				ob1.put("REF3", "" );
				ob1.put("TAXCODE", internallist.get(j).get("TAXCODE"));

				List<HashMap<String, String>> ligb = new ArrayList<HashMap<String, String>>();

				sb = new StringBuffer();
				int count = 0;
//				Date d = new Date();
//				int year = d.getYear();
//				int currentYear=year+1900; 
				writeLog("internalgrnlist.size()--"+internalgrnlist.size(),1);
				System.out.println("internalgrnlist.size() ==>"+internalgrnlist.size());
				for (int k = 0; k < internalgrnlist.size(); k++) {
					String lineitemnumber="";
					writeLog("sheet id "+sheetid,1);
//					try
//					{
//						String fetchlinitemnumber="select TO_CHAR(TO_NUMBER(LINEITEMNO), 'fm0000') AS LINEITEMNUMBER from GRNODS where UNIQUEREFERENCENUMBER=?";
//						ps = con.prepareStatement(fetchlinitemnumber);
//						ps.setString(1, internalgrnlist.get(j).get("UNIQUEREFERENCENUMBER"));
//						rs = ps.executeQuery();
//						while(rs.next())
//						{
//							lineitemnumber=rs.getString("LINEITEMNUMBER");
//						}
//					}catch(Exception e)
//					{
//						writeLog("Exception while introducing leading 0 . Method:'main' ",1);
//						e.printStackTrace();
//					}
//						lineitemnumber= 
					if ((internalgrnlist.get(k).get("INVOICENUMBER")
							.equalsIgnoreCase(internallist.get(j).get("INVOICENUMBER")))
							&& (internalgrnlist.get(k).get("PONUMBER")
									.equalsIgnoreCase(internallist.get(j).get("PONUMBER")))) {
						LinkedHashMap<String, String> gb = new LinkedHashMap<String, String>();
						
						writeLog("sheet id--2-- "+sheetid,1);
//					
						if (count == 0) {
							//sb.append("[{ \"ExpenseSheetId\" : \"" + internallist.get(j).get("ExpenseSheetId") + "\",\r\n");
							sb.append("[{ \"ExpenseSheetId\" : \"" + sheetid + "\",\r\n");
							sb.append(" \"DeliveryNote\" : \"" + internalgrnlist.get(k).get("SAPUNIQUEREFERENCENO") + "_"
									+ internalgrnlist.get(k).get("SAPLINEITEMNO") + "\",\r\n");
							sb.append(" \"TaxCode\" : \"" + internallist.get(j).get("TAXCODE") + "\"},\r\n");
						} else {
							writeLog("sheet id--3-- "+sheetid,1);
							sb.append("{ \"ExpenseSheetId\" : \"" + sheetid + "\",\r\n");
							sb.append(" \"DeliveryNote\" : \"" + internalgrnlist.get(k).get("SAPUNIQUEREFERENCENO") + "_"
									+ internalgrnlist.get(k).get("SAPLINEITEMNO") + "\",\r\n");
							sb.append(" \"TaxCode\" : \"" + internallist.get(j).get("TAXCODE") + "\"},\r\n");
						}

						ligb.add(gb);
						count = 2;
					}
				}
				sb.append("\"]");
				ob1.put("DeliveryNoteList", sb.toString());
				// System.out.println("onexlist first place ==>"+ob1.toString());
				// System.out.println("position first place ==>"+ob1.toString());
				onexlist.add(ob1);
			}
			else
			{
				try {
					String onexstatus = "update PONINVOICESUMMERY set ONEXSTATUS=? where INVOICENUMBER=? "
							+ "AND PONUMBER=? ";
					ps = con.prepareStatement(onexstatus);
					ps.setString(1, "DS");
					ps.setString(2, internallist.get(j).get("InvoiceNo"));
					ps.setString(3, internallist.get(j).get("PoNo"));
					//ps.setInt(4, 0);
					int a = ps.executeUpdate();
					writeLog("Updated status to DS . Method:'main()' ",1);
				} catch (Exception e) {
					writeLog("Exception while updating poninvoicesummary by DS . Method:'insertintoonex()' ",1);
					e.printStackTrace();
				}
			}

		}

			System.out.println("One list to pass into onex database is here => " + onexlist.size());
			for (int k = 0; k < onexlist.size(); k++) {
				//System.out.println("Onex list value is here ==>" + onexlist.get(k).toString());
				writeLog("Onex list value is here ==>" + onexlist.get(k).toString(),1);
				String msg = insertintoonex(onexlist.get(k), k);
			}
	
		writeLog("Onex Integration Completed..",1);
	}

	private static String insertintoonex(HashMap<String, String> hashMap, int k) {

		String input = "{\r\n" + "\r\n" + "    \"ExpenseSheetId\" : \"" + hashMap.get("ExpenseSheetId") + "\",\r\n"
				+ "    \"InvoiceType\" :\"" + hashMap.get("InvoiceType") + "\",\r\n"
				+ "    \"SupplierGSTIN\" : \"\",\r\n" + "    \"PoNo\":\"" + hashMap.get("PoNo") + "\",\r\n"
				+ "    \"POUserId\":\"" + hashMap.get("POUserId") + "\",\r\n" + "    \"Department\":\"\",\r\n"
				+ "    \"VendorName\":\"" + hashMap.get("VendorName") + "\",\r\n" + "    \"BillofLadingDate\":\""
				+ hashMap.get("BillofLadingDate") + "\",\r\n" + "    \"InvoiceDate\":\"" + hashMap.get("InvoiceDate")
				+ "\",\r\n" + "    \"InvoiceNo\":\"" + hashMap.get("InvoiceNo") + "\",\r\n" + "    \"InvoiceAmount\":\""
				+ hashMap.get("InvoiceAmount") + "\",\r\n" + "    \"IRNDate\":\"" + hashMap.get("IRNDATE") + "\",\r\n"
				+ "    \"IRNNO\":\"" + hashMap.get("IRNNUMBER") + "\",\r\n" + "    \"Remark\":\""
				+ hashMap.get("Remark") + "\",\r\n" + "    \"REF3\":\""+hashMap.get("REF3")+"\",\r\n" + "    \"DeliveryNoteList\":"
				+ hashMap.get("DeliveryNoteList") + "\r\n" + "\r\n" + "}";

		String encodedfile = "";
		String actualfilename = hashMap.get("ACTUALFILENAME");
		String savedfilename = hashMap.get("SAVEDFILENAME");
		// System.out.println("actualfilename ==>"+actualfilename);
		// System.out.println("savedfilename ==>"+savedfilename);
		String path = null;
		StringBuffer downloadfilename = null;
		String extension = "";
		String filenamewithoutextension = "";
		try {
			// prop.load(input);
			// System.out.println("filename is here ==>> " + savedfilename);

			String[] fileName = savedfilename.split("_");
			String timestamp = fileName[fileName.length - 1];
			int iend = timestamp.indexOf(".");
			if (iend != -1) {
				timestamp = timestamp.substring(0, iend);
			}

			int i = actualfilename.lastIndexOf('.');
			if (i > 0) {
				extension = actualfilename.substring(i + 1);
			}
			// System.out.println("Extension is here ==>"+extension);

			int b = actualfilename.lastIndexOf('.');
			if (b != -1) {
				filenamewithoutextension = actualfilename.substring(0, b);
			}
			// System.out.println("Extension is here ==>"+filenamewithoutextension);
			path = fileName[0] + "//" + fileName[1] + "//" + timestamp + "//" + savedfilename;
			// System.out.println("path is here ==>"+path);
			// File file = new File("D://tejas//BCCL//DX-Vendor_Portal//java_code//" +
			// path);
			File file = new File("//var//timescapeattachments//dxprojectuploads//" + path);
			InputStream inputStream;
			inputStream = new FileInputStream(file);
			byte[] bytes1 = new byte[(int) file.length()];
			inputStream.read(bytes1);
			encodedfile = new String(Base64.encodeBase64(bytes1), "UTF-8");

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		StringBuffer sb1 = new StringBuffer();// ExpenseSheetId
		sb1.append("{ \"ExpenseSheetId\" : \"" + hashMap.get("ExpenseSheetId") + "\",\r\n");
		sb1.append(" \"FileName\" : \"" + filenamewithoutextension + "\",\r\n");
		sb1.append(" \"FileContent\" : \"" + encodedfile + "\",\r\n");
		sb1.append(" \"FileExt\" : \"" + extension + "\"}\r\n");


		//			if (message1.equalsIgnoreCase("true")) {
		writeLog("input is here in loop ==> " + input,1);
		try {
			System.out.println("input is here " + input);
			URL urlfordataupload = new URL(
					"https://10.201.114.61/VENDORPORTALAPI_UAT/api/SaveVendorPaymentDetails");
			HttpURLConnection connofdataupload;
			connofdataupload = (HttpURLConnection) urlfordataupload.openConnection();
			connofdataupload.setDoOutput(true);
			connofdataupload.setRequestMethod("POST");
			connofdataupload.setRequestProperty("Content-Type", "application/json");

			//writeLog("input is here " + input,1);
			OutputStream osofdataupload = connofdataupload.getOutputStream();
			osofdataupload.write(input.getBytes());
			

			osofdataupload.flush();
			writeLog("Output from Server .... " + connofdataupload.getResponseCode(),1);
			BufferedReader brofdataupload = new BufferedReader(
					new InputStreamReader((connofdataupload.getInputStream())));

			String output;
			System.out.println("Output from Server start.... ");
			String message = null;
			while ((output = brofdataupload.readLine()) != null) {
				// System.out.println("--"+output);
				JSONObject jobj = new JSONObject(output);
				message = jobj.getString("SuccessMsg");
				System.out.println("Output from Server end.... " + message);
				try {
					String onexstatus = "update PONINVOICESUMMERY set ONEXSTATUS=? where INVOICENUMBER=? "
							+ "AND PONUMBER=? ";
					ps = con.prepareStatement(onexstatus);
					ps.setString(1, "RP");
					ps.setString(2, hashMap.get("InvoiceNo"));
					ps.setString(3, hashMap.get("PoNo"));
					//ps.setInt(4, 0);
					int a = ps.executeUpdate();
					writeLog("Updated status to RP . Method:'insertintoonex()' ",1);
				} catch (Exception e) {
					writeLog("Exception while updating poninvoicesummary by RP . Method:'insertintoonex()' ",1);
					e.printStackTrace();
				}
				if (message.equalsIgnoreCase("true")) {
					try {
						String onexstatus = "update PONINVOICESUMMERY set ONEXSTATUS=? where INVOICENUMBER=? "
								+ "AND PONUMBER=? ";
						ps = con.prepareStatement(onexstatus);
						ps.setString(1, "DS");
						ps.setString(2, hashMap.get("InvoiceNo"));
						ps.setString(3, hashMap.get("PoNo"));
						//ps.setInt(4, 0);
						int a = ps.executeUpdate();
						writeLog("Updated status to DS . Method:'insertintoonex()' ",1);
					} catch (Exception e) {
						writeLog("Exception while updating poninvoicesummary by DS . Method:'insertintoonex()' ",1);
						e.printStackTrace();
					}
					String message1 = null;
					try {
						System.out.println("Output from Server start....123. ");

						URL urlforfileupload = new URL("https://10.201.114.61/VENDORPORTALAPI_UAT/api/VendorPaymentFileUpload");

						HttpURLConnection connforfileupload = (HttpURLConnection) urlforfileupload.openConnection();
						connforfileupload.setDoOutput(true);
						connforfileupload.setRequestMethod("POST");
						connforfileupload.setRequestProperty("Content-Type", "application/json");

						OutputStream os = connforfileupload.getOutputStream();
						os.write(sb1.toString().getBytes());

						os.flush();
						System.out.println("String is here " + sb1.toString());

						System.out.println("Output from Server file upload .... " + connforfileupload.getResponseCode());
						BufferedReader br = new BufferedReader(new InputStreamReader((connforfileupload.getInputStream())));

						String output1;
						System.out.println("Output from Server start.... ");

						while ((output1 = br.readLine()) != null) {
							System.out.println("--" + output1);
							JSONObject jobj2 = new JSONObject(output1);
							message1 = jobj2.getString("SuccessMsg");
							System.out.println("Output from Server end.... " + message1);

						}
						if(message1.equalsIgnoreCase("true"))
						{
							try {
								String onexstatus = "update PONINVOICESUMMERY set ONEXSTATUS=? where INVOICENUMBER=? "
										+ "AND PONUMBER=? ";
								ps = con.prepareStatement(onexstatus);
								ps.setString(1, "P");
								ps.setString(2, hashMap.get("InvoiceNo"));
								ps.setString(3, hashMap.get("PoNo"));
								//ps.setInt(4, 0);
								int a = ps.executeUpdate();

							} catch (Exception e) {
								writeLog("Exception while updating poninvoicesummary . Method:'insertintoonex()' ",1);
								e.printStackTrace();
							}
						}

						System.out.println("Output from Server end........................................ ");
						writeLog("Output from Server end........................................ ",1);

						writeLog("Success..",1);
						connforfileupload.disconnect();

					} catch (Exception e) {
						writeLog("Exception while hitting fileupload api . Method:'insertintoonex()' ",1);
						e.printStackTrace();
					}

				}
			}
			connofdataupload.disconnect(); // to be uncommented


			//			} else {
			//				System.out.println("Not properly inserted.");
			//			}

		} catch (MalformedURLException e) {
			writeLog("Exception while hitting savedetails api. Method:'insertintoonex()' ",1);
			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	@Method Name 				: writeLog
	@Arguments					: String - writeFilePath, int - logType
	@Returns					: Nil
	<b>Comments</b>				: This method writes the log for a purged Timescape User
	 */
	public static synchronized void writeLog(String msg, int logType) {

		try {
			cal = new GregorianCalendar();

			if(logType == 1){
				logFilePath =  "/usr/local/timescape/programs/schedules/Test/logs/onex/" + cal.get(Calendar.YEAR) + (cal.get(Calendar.MONTH)+1) + cal.get(Calendar.DATE)+ ".log";
				pw = new PrintWriter(new FileWriter(logFilePath, true));
			}else{
				excepLogFilePath =  "/usr/local/timescape/programs/schedules/Test/logs/onex/" + cal.get(Calendar.YEAR) + (cal.get(Calendar.MONTH)+1) + cal.get(Calendar.DATE)+ ".excepLog";
				pw = new PrintWriter(new FileWriter(excepLogFilePath, true));
			}

			pw.println();
			pw.println("TIME      ::"+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND));
			pw.println();

			if (msg != null) {
				pw.println(msg);
			}
			pw.println("_____________________________________________________________________________________");

			pw.flush();
			pw.close();
		}catch (Throwable th) {
			th.printStackTrace(System.out);
		}
	}

	public static Connection getConnection() throws SQLException {

		// InputStream input =
		// SqlConnectionAPI.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();

		String driver = null;
		String conUrl = null;
		String userName = null;
		String password = null;
		try {
			// prop.load(input);
			if (con == null) {
				// driver = prop.getProperty("connection_driver");
				// conUrl = prop.getProperty("connection_url");
				// userName = prop.getProperty("userName");
				// password = prop.getProperty("pass");

				Class.forName("oracle.jdbc.driver.OracleDriver");
//				con = DriverManager.getConnection("jdbc:oracle:thin:@115.242.9.72:8521:dxdb", "dxvendor", "bcclvendor@123");
								con = DriverManager.getConnection("jdbc:oracle:thin:@10.200.105.195:1521:timesrmd", "rappweb",
										"rappweb123");

			} else {
				return con;
			}

			System.out.println("connection done" + con);
		} catch (ClassNotFoundException e1) {
			con.close();
			e1.printStackTrace();
		} catch (SQLException e2) {
			con.close();
			e2.printStackTrace();
		}
		return con;
	}
}
