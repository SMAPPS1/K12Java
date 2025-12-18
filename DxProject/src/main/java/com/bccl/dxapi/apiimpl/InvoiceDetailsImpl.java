package com.bccl.dxapi.apiimpl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.apiutility.Pagination;
import com.bccl.dxapi.apiutility.Validation;
import com.bccl.dxapi.security.EmailImpl;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.parser.JSONParser;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import au.com.bytecode.opencsv.CSVWriter;

public class InvoiceDetailsImpl {

	static Logger log = Logger.getLogger(InvoiceDetailsImpl.class.getName());

	public InvoiceDetailsImpl() {
		responsejson = new JSONObject();
		jsonArray = new JSONArray();
	}

	@Override
	protected void finalize() throws Throwable {
		responsejson = null;
		jsonArray = null;
		super.finalize();
	}

	JSONObject responsejson = null;
	JSONArray jsonArray = null;

	public JSONArray getInvoiceDetails(String bid, int nPage, String status, String invno, String pono, String fdate,
			String tdate, String plant) {

		boolean result;

		result = Validation.StringChecknull(bid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(bid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

// For all filters except Offline Invoices and ALL and Pending .
		String qdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
				+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
				+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
				+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
				+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT FROM PONINVOICESUMMERY PIS WHERE "
				+ "PIS.BUSINESSPARTNEROID = ? AND PIS.OVERALLSTATUS= ? AND "
				+ "PIS.INVOICENUMBER IS NOT NULL ORDER BY PIS.CREATEDON DESC";

// For ALL filter
		String alldata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
				+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
				+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
				+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
				+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT FROM PONINVOICESUMMERY PIS WHERE "
				+ "PIS.BUSINESSPARTNEROID = ? AND " + "PIS.INVOICENUMBER IS NOT NULL ORDER BY PIS.CREATEDON DESC";

// For WOPO
		String qdata1 = "SELECT * FROM INVOICEEVENTDETAILWOPO where BUSSINESSPARTNEROID =? and PONUMBER is NULL and STATUS <> 'A' "
				+ "ORDER BY createdon desc";

// For OFFLINE INVOICES
		String hdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
				+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
				+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
				+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
				+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT FROM PONINVOICESUMMERY PIS where "
				+ "pis.BusinessPartnerOID =? and invoicenumber is not null and ONEXSTATUS=? ORDER BY pis.createdon desc";

// For PENDING and MANAGERPENDING INVOICES
		String pdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
				+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
				+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
				+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
				+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT FROM PONINVOICESUMMERY PIS WHERE "
				+ "PIS.BUSINESSPARTNEROID = ? AND (PIS.OVERALLSTATUS= ? OR PIS.OVERALLSTATUS= ?) AND "
				+ "PIS.INVOICENUMBER IS NOT NULL ORDER BY PIS.CREATEDON DESC";

		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> invoiceList1 = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		int invoicewopopages = 0;
		try {
			con = DBConnection.getConnection();
			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ"))) {
				if (!status.equalsIgnoreCase("WOPO")) {
					ArrayList<String> param = new ArrayList<String>();
					param.add(bid);
					Pagination pg = null;
					if (status.equalsIgnoreCase("H")) {
						param.add(status);
						pg = new Pagination(hdata, nPage);
					} else if (status.equalsIgnoreCase("ALL")) {
						pg = new Pagination(alldata, nPage);
					} else if (status.equalsIgnoreCase("P")) {
						param.add(status);
						param.add("M");
						pg = new Pagination(pdata, nPage);

					} else {
						param.add(status);
						pg = new Pagination(qdata, nPage);

					}
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);

					int count = 0;
					while (rs.next()) {
						count++;
						HashMap<String, String> invoiceData = new HashMap<String, String>();
						invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
						invoiceData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
						invoiceData.put("PO_NUMBER", rs.getString("PONUMBER"));
						invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
						invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
						invoiceData.put("VENDORID", rs.getString("VENDORID"));
						invoiceData.put("PLANT", rs.getString("PLANT"));
						POImpl po = new POImpl();
						invoiceData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
						invoiceData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
						invoiceData.put("CREATEDBY", rs.getString("CreatedBy"));
						invoiceData.put("CREATEDON", rs.getString("CreatedOn"));
						invoiceData.put("TOTALAMOUNT", rs.getString("AMOUNT"));
						invoiceData.put("PAYMENTAMOUNT",
								rs.getString("PAYMENTAMOUNT") != null ? rs.getString("PAYMENTAMOUNT") : "0");
						invoiceData.put("DESCRIPTION", rs.getString("DESCRIPTION"));
						invoiceData.put("STATUS", rs.getString("OVERALLSTATUS"));
						invoiceData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
						invoiceData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
						invoiceData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
						invoiceData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
						invoiceData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
						invoiceData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
						invoiceList.add(invoiceData);
					}
					pg.close();
					rs.close();
					pg = null;

				}

				if (status.equalsIgnoreCase("WOPO")) {
					ArrayList<String> param1 = new ArrayList<String>();
					param1.add(bid);
					Pagination pg1 = new Pagination(qdata1, nPage);
					invoicewopopages = pg1.getPages(con, param1);
					rs = pg1.execute(con, param1);

					while (rs.next()) {

						HashMap<String, String> invoiceData1 = new HashMap<String, String>();

						invoiceData1.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
						invoiceData1.put("INVOICEDATE", rs.getString("INVOICEDATE"));
						invoiceData1.put("BUSSINESSPARTNEROID", rs.getString("BUSSINESSPARTNEROID"));
						invoiceData1.put("PO_NUMBER", rs.getString("PONUMBER"));
						invoiceData1.put("CREATEDON", rs.getString("CREATEDON"));
						invoiceData1.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
						invoiceData1.put("DESCRIPTION", rs.getString("DESCRIPTION"));
						invoiceData1.put("STATUS", rs.getString("STATUS"));
						invoiceData1.put("INVOICEAMOUNT", rs.getString("INVOICEAMOUNT"));
						invoiceData1.put("USEREMAILID", rs.getString("USEREMAILID"));
						invoiceData1.put("POINVOICENUMBER", rs.getString("POINVOICENUMBER"));

						invoiceList1.add(invoiceData1);
					}
					pg1.close();
					rs.close();
					pg1 = null;
				}
			} else {
				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				param.add(bid);
				Pagination pg = null;

				String advqdata = "";
				if ((!status.equalsIgnoreCase("ASWP")) && (!status.equalsIgnoreCase("ASSQ"))) {
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PONUMBER=?";
						subquery = subquery + po;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND PIS.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					advqdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
							+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
							+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
							+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
							+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT FROM PONINVOICESUMMERY PIS WHERE "
							+ "PIS.BUSINESSPARTNEROID = ? " + subquery + " AND "
							+ "PIS.INVOICENUMBER IS NOT NULL ORDER BY PIS.CREATEDON DESC";

				} else if (status.equalsIgnoreCase("ASWP")) {

					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PONUMBER=?";
						subquery = subquery + po;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND PIS.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					advqdata = "SELECT * FROM INVOICEEVENTDETAILWOPO where BUSSINESSPARTNEROID =? and PONUMBER is NULL "
							+ subquery + " and STATUS <> 'A' " + "ORDER BY createdon desc";

				} else if (status.equalsIgnoreCase("ASSQ")) {

					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND PIS.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}

					advqdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
							+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
							+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
							+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
							+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT FROM PONINVOICESUMMERY PIS WHERE "
							+ "PIS.BUSINESSPARTNEROID = ? AND PIS.CREDITADVICENO IS NOT NULL " + subquery + " AND "
							+ "PIS.INVOICENUMBER IS NOT NULL ORDER BY PIS.CREATEDON DESC";

				}
				pg = new Pagination(advqdata, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				if (status.equalsIgnoreCase("ASWP")) {

				} else {
					while (rs.next()) {
						HashMap<String, String> invoiceData = new HashMap<String, String>();
						invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
						invoiceData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
						invoiceData.put("PO_NUMBER", rs.getString("PONUMBER"));
						invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
						invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
						invoiceData.put("VENDORID", rs.getString("VENDORID"));
						invoiceData.put("PLANT", rs.getString("PLANT"));
						POImpl po = new POImpl();
						invoiceData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
						invoiceData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
						invoiceData.put("CREATEDBY", rs.getString("CreatedBy"));
						invoiceData.put("CREATEDON", rs.getString("CreatedOn"));
						invoiceData.put("TOTALAMOUNT", rs.getString("AMOUNT"));
						invoiceData.put("PAYMENTAMOUNT",
								rs.getString("PAYMENTAMOUNT") != null ? rs.getString("PAYMENTAMOUNT") : "0");
						invoiceData.put("DESCRIPTION", rs.getString("DESCRIPTION"));
						invoiceData.put("STATUS", rs.getString("OVERALLSTATUS"));
						invoiceData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
						invoiceData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
						invoiceData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
						invoiceData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
						invoiceData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
						invoiceData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
						invoiceList.add(invoiceData);
					}
				}

				pg.close();
				rs.close();
				pg = null;

				String subquery1 = "";
				ArrayList<String> param1 = new ArrayList<String>();
				param1.add(bid);
				if (!invno.equalsIgnoreCase("NA")) {
					String in = " AND PIS.INVOICENUMBER=?";
					subquery1 = subquery1 + in;
					param1.add(invno);
				}
				if (!fdate.equalsIgnoreCase("NA")) {
					String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
							+ "AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery1 = subquery1 + dt;
					param1.add(fdate);
					param1.add(tdate);
				}

				String advqdata1 = "SELECT * FROM INVOICEEVENTDETAILWOPO PIS where PIS.BUSSINESSPARTNEROID =? "
						+ "and PIS.PONUMBER is NULL " + subquery1
						+ " AND PIS.STATUS <> 'A' ORDER BY PIS.createdon desc";
				Pagination pg1 = new Pagination(advqdata1, nPage);
				invoicewopopages = pg1.getPages(con, param1);
				rs = pg1.execute(con, param1);

				while (rs.next()) {
					HashMap<String, String> invoiceData1 = new HashMap<String, String>();

					invoiceData1.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					invoiceData1.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					invoiceData1.put("BUSSINESSPARTNEROID", rs.getString("BUSSINESSPARTNEROID"));
					invoiceData1.put("PO_NUMBER", rs.getString("PONUMBER"));
					invoiceData1.put("CREATEDON", rs.getString("CREATEDON"));
					invoiceData1.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
					invoiceData1.put("DESCRIPTION", rs.getString("DESCRIPTION"));
					invoiceData1.put("STATUS", rs.getString("STATUS"));
					invoiceData1.put("INVOICEAMOUNT", rs.getString("INVOICEAMOUNT"));
					invoiceData1.put("USEREMAILID", rs.getString("USEREMAILID"));
					invoiceData1.put("POINVOICENUMBER", rs.getString("POINVOICENUMBER"));

					invoiceList1.add(invoiceData1);

				}
				pg1.close();
				rs.close();
				pg1 = null;
			}

			try {
				getInvoiceDetailsCountAsPerStatus(bid, nPage, status, invno, pono, fdate, tdate, plant, con, ps, rs);
			} catch (Exception e) {

			}

		} catch (Exception e) {
			log.error("getInvoiceDetails() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		if (invoiceList.size() > 0) {
			responsejson.put("message", "Sucessinvlist");
			responsejson.put("invoiceData", invoiceList);
			responsejson.put("invoicelistpages", pages);

		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
		}
		if (invoiceList1.size() > 0) {
			responsejson.put("message1", "Sucessinvlist1");
			responsejson.put("invoiceDataWOPO", invoiceList1);
			responsejson.put("invoicewopopages", invoicewopopages);
		} else {
			responsejson.put("message1", "No Data Found for given Vendor Id");
		}
		jsonArray.add(responsejson);

		return jsonArray;

	}

	public JSONArray getBussiIdInvoceidIdDetails(String BusinessPartnerOID, String invoiceId) throws SQLException {
		// not used
		return jsonArray;
	}

	public JSONArray getInvoiceSubmitQuery(String po_num, String emailid, String requsitionerEmail, String message,
			String bid, String subject, String status) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(subject);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(message);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(bid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(bid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		result = Validation.StringChecknull(emailid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.emailCheck(emailid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		result = Validation.StringChecknull(requsitionerEmail);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.emailCheck(requsitionerEmail);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String invoice_data = "";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String sqlUpdate = "insert into CHATMESSAGE "
					+ "(BusinessPartnerOID,Sender,PONUMBER,INVOICENUMBER,MESSAGETEXT,SUBJECT,STATUS,CREATEDON) values (?,?,?,?,?,?,?,?)";
			ps = con.prepareStatement(sqlUpdate);
			ps.setString(1, bid);
			ps.setString(2, emailid);
			ps.setString(3, requsitionerEmail);
			ps.setString(4, po_num);
			ps.setString(5, subject);
			ps.setString(6, message);
			ps.setString(7, status);
			ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.executeUpdate();
			ps.close();
			if (subject.equalsIgnoreCase("InvoiceConfirmation")) {

				if (status.equalsIgnoreCase("Yes")) {
					status = "A";
					String invoice_status = "UPDATE DELIVERYSUMMARY set INVOICESTATUS=? where PONumber=?";
					ps = con.prepareStatement(invoice_status);
					ps.setString(1, status);
					ps.setString(2, po_num);
					ps.executeUpdate();
					ps.close();
				}
			}
			responsejson.put("message", "Success");
			con.commit();
			jsonArray.add(responsejson);
		} catch (SQLException e) {
			log.error("getInvoiceSubmitQuery() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

// invoice data download in excel format 	
	private String writeintoexcelfile(List<List<String>> totallist) {
		String encodedfile = "";
		XSSFWorkbook workbook = new XSSFWorkbook();
		try {
			FileOutputStream out = new FileOutputStream(new File("demo.xlsx"));
			XSSFSheet sheet = workbook.createSheet("Invoice Data");
			List<String> heading = new ArrayList<String>();
			heading.add("INVOICE NUMBER");
			heading.add("PONUMBER");
			heading.add("LINEITEMNUMBER");
			heading.add("QUANTITY");
			heading.add("INVOICEAMOUNT");

			heading.add("VENDORID");
			heading.add("CREATEDON");
			heading.add("OVERALLSTATUS");
			heading.add("PAID AMOUNT");
			heading.add("EXPENSE SHEET ID");

			heading.add("UTR NUMBER");
			heading.add("PAYMENT DATE");
			heading.add("PLANT NAME");
			Iterator<String> tempIterator1 = heading.iterator();
			Iterator<List<String>> i = totallist.iterator();
			int rownum = 0;
			int cellnum = 0;
			XSSFRow row1 = sheet.createRow(rownum++);
			while (tempIterator1.hasNext()) {
				String temp = (String) tempIterator1.next();
				Cell cell = row1.createCell(cellnum++);

				XSSFFont fontBold = workbook.createFont();
				fontBold.setBold(true);
				XSSFCellStyle cellStyle1 = workbook.createCellStyle();
				cellStyle1.setAlignment(HorizontalAlignment.CENTER);
				XSSFRichTextString cellValue = new XSSFRichTextString();
				cellValue.append(temp, fontBold);
				cell.setCellValue(cellValue);
				cell.setCellStyle(cellStyle1);

			}
			rownum++;
			while (i.hasNext()) {
				List<String> templist = (List<String>) i.next();
				Iterator<String> tempIterator = templist.iterator();
				XSSFRow row = sheet.createRow(rownum++);
				cellnum = 0;
				int k = 0;
				while (tempIterator.hasNext()) {
					String temp = (String) tempIterator.next();
					Cell cell = row.createCell(cellnum++);

					sheet.autoSizeColumn(cellnum);
					XSSFCellStyle cellStyle1 = workbook.createCellStyle();
					if (k == 0) {
						cellStyle1.setAlignment(HorizontalAlignment.LEFT);
					} else if (k == 1) {
						cellStyle1.setAlignment(HorizontalAlignment.CENTER);
					} else if (k == 2) {
						cellStyle1.setAlignment(HorizontalAlignment.CENTER);
					} else if (k == 3) {
						cellStyle1.setAlignment(HorizontalAlignment.RIGHT);
					} else if (k == 4) {
						cellStyle1.setAlignment(HorizontalAlignment.RIGHT);
					} else if (k == 5) {
						cellStyle1.setAlignment(HorizontalAlignment.CENTER);
					} else if (k == 6) {
						cellStyle1.setAlignment(HorizontalAlignment.CENTER);
					} else if (k == 7) {
						cellStyle1.setAlignment(HorizontalAlignment.CENTER);
					} else if (k == 8) {
						cellStyle1.setAlignment(HorizontalAlignment.RIGHT);
					} else if (k == 9) {
						cellStyle1.setAlignment(HorizontalAlignment.RIGHT);
					} else if (k == 10) {
						cellStyle1.setAlignment(HorizontalAlignment.RIGHT);
					}
					cell.setCellValue(temp);
					cell.setCellStyle(cellStyle1);
					k++;
				}

			}
			workbook.write(out);
			out.close();
			workbook.close();
			POImpl poimpl = new POImpl();

			byte[] temp = poimpl.convert(sheet);
			File file = new File("demo.xlsx");
			try {
				InputStream inputStream;
				inputStream = new FileInputStream(file);
				byte[] bytes1 = new byte[(int) file.length()];
				inputStream.read(bytes1);
				encodedfile = new String(Base64.encodeBase64(bytes1), "UTF-8");
			} catch (IOException e) {
				log.error("writeintoexcelfile() 1: ", e.fillInStackTrace());

			}

		} catch (Exception e) {
			log.error("writeintoexcelfile() 2: ", e.fillInStackTrace());

		}
		return encodedfile;

	}

	public JSONArray getPoDetailsStatusN(String bid) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(bid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(bid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		String invoice_data1 = "select count(Status) as Status from podetails where BusinessPartnerOID =? and Status =?";
		String invoice_data2 = "select count(INVOICESTATUS) as Status from DELIVERYSUMMARY where BusinessPartnerOID =? and Status =?";

		ArrayList<HashMap<String, String>> InvoiceQueryList = new ArrayList<HashMap<String, String>>();
		int coun = 0;
		int cound = 0;
		int add = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(invoice_data1);
			ps.setString(1, bid);
			ps.setString(2, "N");
			rs = ps.executeQuery();
			while (rs.next()) {
				coun = Integer.parseInt(rs.getString("Status"));
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement(invoice_data2);
			ps.setString(1, bid);
			ps.setString(2, "N");
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> invoiceQuery = new HashMap<String, String>();
				cound = Integer.parseInt(rs.getString("Status"));
				add = coun + cound;
				invoiceQuery.put("Status", Integer.toString(add));
				InvoiceQueryList.add(invoiceQuery);
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getPoDetailsStatusN() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		if (InvoiceQueryList.size() > 0) {
			responsejson.put("invoiceQuery", InvoiceQueryList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	/*
	 * public JSONArray downloadInvoceidIdDetails(String bid, List<String>
	 * invoicedata, List<String> podata) throws SQLException {
	 * 
	 * String downloaddataquery = null; if (!bid.equalsIgnoreCase("0")) {
	 * 
	 * downloaddataquery = "SELECT DISTINCT PS.INVOICENUMBER, " +
	 * "TO_CHAR(PS.INVOICEDATE,'DD-MON-RRRR') INVOICEDATE, PS.OVERALLSTATUS, " +
	 * "PS.EXPENSESHEETID, " +
	 * "TRIM(TO_CHAR(PS.PAYMENTAMOUNT, '999,999,999,999,999')) AS PAYMENTAMOUNT, " +
	 * "TRIM(TO_CHAR(DS.LINEITEMTOTALAMOUNT, '999,999,999,999,999')) AS INVOICEAMOUNT, "
	 * +
	 * "DS.INVOICESTATUS AS STATUS, PS.VENDORID, DS.LINEITEMTOTALQUANTITY, PS.PONUMBER, "
	 * + "DS.LINEITEMNUMBER, TO_CHAR(PS.MODIFIEDON,'DD-MON-RRRR') MODIFIEDON, " +
	 * "PS.UTRCHEQUENUMBER UTR, TO_CHAR(PS.UTRDATE, 'DD-MON-RRRR') UTRDATE, PD.PLANT PLANT "
	 * + "FROM PONINVOICESUMMERY PS, DELIVERYSUMMARY DS, POEVENTDETAILS PD  WHERE "
	 * + "PS.INVOICENUMBER = DS.INVOICENUMBER AND PS.PONUMBER = DS.PONUMBER " +
	 * "AND PD.PONUMBER = PS.PONUMBER AND PD.PONUMBER = DS.PONUMBER " +
	 * "AND PS.INVOICENUMBER=? AND PS.PONUMBER=? AND PS.BUSINESSPARTNEROID=? " +
	 * "AND PS.OVERALLSTATUS IS NOT NULL";
	 * 
	 * } else {
	 * 
	 * downloaddataquery = "SELECT DISTINCT PS.INVOICENUMBER, " +
	 * "TO_CHAR(PS.INVOICEDATE,'DD-MON-RRRR') INVOICEDATE, PS.OVERALLSTATUS, " +
	 * "PS.EXPENSESHEETID, " +
	 * "TRIM(TO_CHAR(PS.PAYMENTAMOUNT, '999,999,999,999,999')) AS PAYMENTAMOUNT, " +
	 * "TRIM(TO_CHAR(DS.LINEITEMTOTALAMOUNT, '999,999,999,999,999')) AS INVOICEAMOUNT, "
	 * +
	 * "DS.INVOICESTATUS AS STATUS, PS.VENDORID, DS.LINEITEMTOTALQUANTITY, PS.PONUMBER, "
	 * + "DS.LINEITEMNUMBER, TO_CHAR(PS.MODIFIEDON,'DD-MON-RRRR') MODIFIEDON, " +
	 * "PS.UTRCHEQUENUMBER UTR, TO_CHAR(PS.UTRDATE, 'DD-MON-RRRR') UTRDATE, PD.PLANT PLANT "
	 * + "FROM PONINVOICESUMMERY PS, DELIVERYSUMMARY DS, POEVENTDETAILS PD  WHERE "
	 * + "PS.INVOICENUMBER = DS.INVOICENUMBER AND PS.PONUMBER = DS.PONUMBER " +
	 * "AND PD.PONUMBER = PS.PONUMBER AND PD.PONUMBER = DS.PONUMBER " +
	 * "AND PS.INVOICENUMBER=? AND PS.PONUMBER=? AND PS.OVERALLSTATUS IS NOT NULL";
	 * }
	 * 
	 * ArrayList<HashMap<String, String>> invoiceList = new
	 * ArrayList<HashMap<String, String>>();
	 * 
	 * List<List<String>> allresult = new ArrayList<List<String>>(); Connection con
	 * = null; PreparedStatement ps = null; ResultSet rs = null; try { con =
	 * DBConnection.getConnection(); for (int i = 0; i < invoicedata.size(); i++) {
	 * ps = con.prepareStatement(downloaddataquery); if (!bid.equalsIgnoreCase("0"))
	 * { ps.setString(1, invoicedata.get(i)); ps.setString(2, podata.get(i));
	 * ps.setString(3, bid); } else { ps.setString(1, invoicedata.get(i));
	 * ps.setString(2, podata.get(i)); } rs = ps.executeQuery();
	 * 
	 * while (rs.next()) { List<String> invoiceData = new ArrayList<String>();
	 * invoiceData.add(rs.getString("InvoiceNumber"));
	 * invoiceData.add(rs.getString("PONumber"));
	 * invoiceData.add(rs.getString("LineItemNumber"));
	 * invoiceData.add(rs.getString("LINEITEMTOTALQUANTITY"));
	 * invoiceData.add(rs.getString("INVOICEAMOUNT"));
	 * invoiceData.add(rs.getString("VendorID"));
	 * invoiceData.add(rs.getString("MODIFIEDON")); if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("A")) {
	 * invoiceData.add("Approved"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("N")) {
	 * invoiceData.add("New"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("O")) {
	 * invoiceData.add("On Hold"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("P")) {
	 * invoiceData.add("Pending"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PRO")) {
	 * invoiceData.add("Processed"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("S")) {
	 * invoiceData.add("Submitted"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PP")) {
	 * invoiceData.add("Partially Paid"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("M")) {
	 * invoiceData.add("Pending"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("R")) {
	 * invoiceData.add("Rejected"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PD")) {
	 * invoiceData.add("Paid"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("V")) {
	 * invoiceData.add("Returned"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("RO")) {
	 * invoiceData.add("Reopened"); }else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("INV")) {
	 * invoiceData.add("Invalid Invoice"); } else { invoiceData.add("-"); }
	 * invoiceData.add(rs.getString("PAYMENTAMOUNT"));
	 * invoiceData.add(rs.getString("EXPENSESHEETID"));
	 * invoiceData.add(rs.getString("UTR")); invoiceData.add(rs.getString("UTRDATE")
	 * == null ? "" : rs.getString("UTRDATE")); String plantCode =
	 * rs.getString("PLANT") == null ? "" : rs.getString("PLANT"); if
	 * ("".equalsIgnoreCase(plantCode)) { invoiceData.add(""); } else { String
	 * plantName = getPlantName(plantCode, con); if (plantName == null ||
	 * plantName.equalsIgnoreCase("null")) { invoiceData.add(""); } else {
	 * invoiceData.add(plantName); } }
	 * 
	 * allresult.add(invoiceData); } rs.close(); ps.close(); }
	 * 
	 * String encodedfile = writeintoexcelfile(allresult); if
	 * (encodedfile.equalsIgnoreCase("")) { responsejson.put("message", "Fail"); }
	 * else { responsejson.put("message", "Success"); responsejson.put("data",
	 * encodedfile); } jsonArray.add(responsejson);
	 * 
	 * } catch (Exception e) { log.error("downloadInvoceidIdDetails() :",
	 * e.fillInStackTrace());
	 * 
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); } finally {
	 * DBConnection.closeConnection(rs, ps, con); } return jsonArray; }
	 */
	public String getPlantName(String plantCode, Connection con) throws SQLException {

		String plantQuery = "SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = ? ";
		PreparedStatement ps = null;
		ResultSet rs = null;
		String plantName = null;

		try {
			ps = con.prepareStatement(plantQuery);
			ps.setString(1, plantCode);
			rs = ps.executeQuery();
			while (rs.next()) {
				plantName = rs.getString("PLANTNAME") == null ? "" : rs.getString("PLANTNAME").trim();
			}
			rs.close();
			ps.close();

		} catch (Exception e) {
			log.error("getPlantName() :", e.fillInStackTrace());

		}
		return plantName;
	}

	public JSONArray setChatStatus(String bid, String invoiceNumber, String poNumber, String userEmailId, String topic,
			String message, String subject) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(poNumber);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(subject);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(invoiceNumber);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(bid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(bid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		result = Validation.StringChecknull(userEmailId);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.emailCheck(userEmailId);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String userStatus = null;
		boolean flag = false;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs1 = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			if (invoiceNumber != null && !"".equalsIgnoreCase(invoiceNumber)) {

				String queryFindmsgStatus = "Select MESSAGE from PONINVOICESUMMERY where PONUMBER = ? and INVOICENUMBER = ? ";
				ps = con.prepareStatement(queryFindmsgStatus);
				ps.setString(1, poNumber);
				ps.setString(2, invoiceNumber);
				rs = ps.executeQuery();

				if (rs.next()) {
					userStatus = rs.getString("MESSAGE") == null ? "" : rs.getString("MESSAGE");
					if ("N".equalsIgnoreCase(userStatus)) {
						String queryUpdate = "update PONINVOICESUMMERY set MESSAGE = ? where PONUMBER = ? and INVOICENUMBER =?";
						PreparedStatement ps1 = con.prepareStatement(queryUpdate);
						ps1.setString(1, "Y");
						ps1.setString(2, poNumber);
						ps1.setString(3, invoiceNumber);
						ps1.executeUpdate();
						flag = true;
						ps1.close();
					}
				} else {
					flag = true;
				}
				rs.close();
				ps.close();
				if (flag) {
					String queryFindPoDetails = "Select CONTACTPERSONEMAILID,REQUSITIONER,POAMOUNT from PODETAILS where PONUMBER = ? "
							+ "AND BUSINESSPARTNEROID = ? ";
					ps = con.prepareStatement(queryFindPoDetails);
					ps.setString(1, poNumber);
					ps.setString(2, bid);
					rs = ps.executeQuery();

					String buyerId = null;
					String endUserId = null;
					String amount = null;
					String msg = null;
					String primaryEmailid = null;
					String secondaryEmailid = null;
					String tertiaryEmailid = null;
					ArrayList<String> emailArrayList = new ArrayList<String>();
					if (rs.next()) {
						buyerId = rs.getString("CONTACTPERSONEMAILID") == null ? "-"
								: rs.getString("CONTACTPERSONEMAILID");
						endUserId = rs.getString("REQUSITIONER") == null ? "-" : rs.getString("REQUSITIONER");
						amount = rs.getString("POAMOUNT") == null ? "0" : rs.getString("POAMOUNT");
						emailArrayList.add(buyerId);
						emailArrayList.add(endUserId);
						String queryFindPoSummery = "Select MESSAGE from PONINVOICESUMMERY where PONUMBER = ? and INVOICENUMBER = ? ";
						PreparedStatement ps1 = con.prepareStatement(queryFindPoSummery);
						ps1.setString(1, poNumber);
						ps1.setString(2, invoiceNumber);
						rs1 = ps1.executeQuery();
						if (rs1.next()) {
							String queryBusinessPartner = "Select BUSINESSPARTNEROID,PRIMARYEMAILID,SECONDARYEMAILID,TERTIARYEMAILID"
									+ " from BUSINESSPARTNER where BUSINESSPARTNEROID = ? and STATUS =? ";
							PreparedStatement ps2 = con.prepareStatement(queryBusinessPartner);
							ps2.setString(1, bid);
							ps2.setString(2, "A");
							rs2 = ps2.executeQuery();
							if (rs2.next()) {
								primaryEmailid = rs2.getString("PRIMARYEMAILID") == null ? "-"
										: rs2.getString("PRIMARYEMAILID");
								secondaryEmailid = rs2.getString("SECONDARYEMAILID") == null ? "-"
										: rs2.getString("SECONDARYEMAILID");
								tertiaryEmailid = rs2.getString("TERTIARYEMAILID") == null ? "-"
										: rs2.getString("TERTIARYEMAILID");
								emailArrayList.add(primaryEmailid);
								emailArrayList.add(secondaryEmailid);
								emailArrayList.add(tertiaryEmailid);
							}
							rs2.close();
							ps2.close();
							for (int counter = 0; counter <= emailArrayList.size() - 1; counter++) {

								String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
										+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
								ps2 = con.prepareStatement(queryChat);
								ps2.setString(1, bid);
								ps2.setString(2, invoiceNumber);
								ps2.setString(3, poNumber);
								ps2.setString(4, emailArrayList.get(counter));
								ps2.setString(5, "A");
								ps2.executeUpdate();
								ps2.close();
							}

						} else {
							String querySubmitSummary = "insert into PONINVOICESUMMERY (INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,"
									+ "MESSAGE,REQUSITIONER,BUYER,AMOUNT) values (?,?,?,?,?,?,?)";
							PreparedStatement ps2 = con.prepareStatement(querySubmitSummary);
							ps2.setString(1, invoiceNumber);
							ps2.setString(2, poNumber);
							ps2.setString(3, bid);
							ps2.setString(4, "Y");
							ps2.setString(5, buyerId);
							ps2.setString(6, endUserId);
							ps2.setString(7, amount);
							ps2.executeUpdate();
							ps2.close();
							String queryBusinessPartner = "Select BUSINESSPARTNEROID,PRIMARYEMAILID,SECONDARYEMAILID,TERTIARYEMAILID"
									+ " from BUSINESSPARTNER where BUSINESSPARTNEROID = ? and STATUS =? ";
							ps2 = con.prepareStatement(queryBusinessPartner);
							ps2.setString(1, bid);
							ps2.setString(2, "A");
							rs2 = ps2.executeQuery();
							if (rs2.next()) {

								primaryEmailid = rs2.getString("PRIMARYEMAILID") == null ? "-"
										: rs2.getString("PRIMARYEMAILID");
								secondaryEmailid = rs2.getString("SECONDARYEMAILID") == null ? "-"
										: rs2.getString("SECONDARYEMAILID");
								tertiaryEmailid = rs2.getString("TERTIARYEMAILID") == null ? "-"
										: rs2.getString("TERTIARYEMAILID");

								emailArrayList.add(primaryEmailid);
								emailArrayList.add(secondaryEmailid);
								emailArrayList.add(tertiaryEmailid);

							}
							rs2.close();
							ps2.close();
							for (int counter = 0; counter <= emailArrayList.size() - 1; counter++) {

								String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
										+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
								ps2 = con.prepareStatement(queryChat);
								ps2.setString(1, bid);
								ps2.setString(2, invoiceNumber);
								ps2.setString(3, poNumber);
								ps2.setString(4, emailArrayList.get(counter));
								ps2.setString(5, "A");
								ps2.executeUpdate();
								ps2.close();
							}
						}
						rs1.close();
						ps1.close();
					}
					rs.close();
					ps.close();
				}

				String queryNotification = "insert into CHATMESSAGE (BUSINESSPARTNEROID,SENDER,PONUMBER,"
						+ "INVOICENUMBER,MESSAGETEXT,SUBJECT,STATUS,CREATEDON) values (?,?,?,?,?,?,?,?)";
				ps = con.prepareStatement(queryNotification);
				ps.setString(1, bid);
				ps.setString(2, userEmailId);
				ps.setString(3, poNumber);
				ps.setString(4, invoiceNumber);
				ps.setString(5, message);
				ps.setString(6, subject);
				ps.setString(7, "A");
				ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.executeUpdate();
				ps.close();
				String queryFindStatus = "Select status from CHATSTATUS where PONUMBER = ? and INVOICENUMBER = ? "
						+ "and LOGGEDIN = ? ";
				ps = con.prepareStatement(queryFindStatus);
				ps.setString(1, poNumber);
				ps.setString(2, invoiceNumber);
				ps.setString(3, userEmailId);
				rs = ps.executeQuery();

				String statusUser = null;

				if (rs.next()) {

					String queryUpdate = "update CHATSTATUS set status = ? where PONUMBER = ? and INVOICENUMBER = ?"
							+ " and loggedin <> ? ";
					PreparedStatement ps1 = con.prepareStatement(queryUpdate);
					ps1.setString(1, "A");
					ps1.setString(2, poNumber);
					ps1.setString(3, invoiceNumber);
					ps1.setString(4, userEmailId);
					ps1.executeUpdate();
					ps1.close();

					String queryUpdateChat = "update CHATSTATUS set status =? where PONUMBER = ? and INVOICENUMBER = ?"
							+ "and LOGGEDIN = ? ";
					ps1 = con.prepareStatement(queryUpdateChat);
					ps1.setString(1, "R");
					ps1.setString(2, poNumber);
					ps1.setString(3, invoiceNumber);
					ps1.setString(4, userEmailId);
					ps1.executeUpdate();
					ps1.close();
				} else {

					String queryUpdate = "update CHATSTATUS set status = ? where PONUMBER = ? and INVOICENUMBER = ?"
							+ " and loggedin <> ? ";
					PreparedStatement ps1 = con.prepareStatement(queryUpdate);
					ps1.setString(1, "A");
					ps1.setString(2, poNumber);
					ps1.setString(3, invoiceNumber);
					ps1.setString(4, userEmailId);
					ps1.executeUpdate();
					ps1.close();
					String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
							+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
					ps1 = con.prepareStatement(queryChat);
					ps1.setString(1, bid);
					ps1.setString(2, invoiceNumber);
					ps1.setString(3, poNumber);
					ps1.setString(4, userEmailId);
					ps1.setString(5, "R");
					ps1.executeUpdate();
					ps1.close();
				}
				rs.close();
				ps.close();
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);

			} else {
				boolean poFlag = false;
				if (invoiceNumber == null || "".equals(invoiceNumber)) {
					String queryFindmsgStatus = "Select MESSAGE from PONINVOICESUMMERY where PONUMBER = ? and INVOICENUMBER is null ";
					ps = con.prepareStatement(queryFindmsgStatus);
					ps.setString(1, poNumber);
					rs = ps.executeQuery();

					if (rs.next()) {
						userStatus = rs.getString("MESSAGE") == null ? "" : rs.getString("MESSAGE");
						if ("N".equalsIgnoreCase(userStatus)) {
							String queryUpdate = "update PONINVOICESUMMERY set MESSAGE = ? where PONUMBER = ? and INVOICENUMBER is null ";
							PreparedStatement ps1 = con.prepareStatement(queryUpdate);
							ps1.setString(1, "Y");
							ps1.setString(2, poNumber);
							ps1.executeUpdate();
							ps1.close();
							poFlag = true;
						}

					} else {
						poFlag = true;
					}
					rs.close();
					ps.close();
					if (poFlag) {
						String queryFindPoDetails = "Select CONTACTPERSONEMAILID,REQUSITIONER,POAMOUNT from PODETAILS where PONUMBER = ? "
								+ "AND BUSINESSPARTNEROID = ? ";
						ps = con.prepareStatement(queryFindPoDetails);
						ps.setString(1, poNumber);
						ps.setString(2, bid);
						rs = ps.executeQuery();

						String buyerId = null;
						String endUserId = null;
						String amount = null;
						String msg = null;
						String primaryEmailid = null;
						String secondaryEmailid = null;
						String tertiaryEmailid = null;
						ArrayList<String> emailArrayList = new ArrayList<String>();

						if (rs.next()) {
							buyerId = rs.getString("CONTACTPERSONEMAILID") == null ? "-"
									: rs.getString("CONTACTPERSONEMAILID");
							endUserId = rs.getString("REQUSITIONER") == null ? "-" : rs.getString("REQUSITIONER");
							amount = rs.getString("POAMOUNT") == null ? "0" : rs.getString("POAMOUNT");

							String queryFindPoSummery = "Select MESSAGE from PONINVOICESUMMERY where PONUMBER = ? and INVOICENUMBER is null ";
							ps = con.prepareStatement(queryFindPoSummery);
							ps.setString(1, poNumber);
							rs1 = ps.executeQuery();
							if (rs1.next()) {

								String queryBusinessPartner = "Select BUSINESSPARTNEROID,PRIMARYEMAILID,SECONDARYEMAILID,TERTIARYEMAILID"
										+ " from BUSINESSPARTNER where BUSINESSPARTNEROID = ? and STATUS =? ";
								PreparedStatement ps2 = con.prepareStatement(queryBusinessPartner);
								ps2.setString(1, bid);
								ps2.setString(2, "A");
								rs2 = ps2.executeQuery();
								if (rs2.next()) {
									primaryEmailid = rs2.getString("PRIMARYEMAILID") == null ? "-"
											: rs2.getString("PRIMARYEMAILID");
									secondaryEmailid = rs2.getString("SECONDARYEMAILID") == null ? "-"
											: rs2.getString("SECONDARYEMAILID");
									tertiaryEmailid = rs2.getString("TERTIARYEMAILID") == null ? "-"
											: rs2.getString("TERTIARYEMAILID");

									emailArrayList.add(primaryEmailid);
									emailArrayList.add(secondaryEmailid);
									emailArrayList.add(tertiaryEmailid);

								}
								rs2.close();
								ps2.close();
								for (int counter = 0; counter <= emailArrayList.size() - 1; counter++) {

									String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
											+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
									ps2 = con.prepareStatement(queryChat);
									ps2.setString(1, bid);
									ps2.setString(2, invoiceNumber);
									ps2.setString(3, poNumber);
									ps2.setString(4, emailArrayList.get(counter));
									ps2.setString(5, "A");
									ps2.executeUpdate();
									ps2.close();
								}

							} else {
								String querySubmitSummary = "insert into PONINVOICESUMMERY (INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,"
										+ "MESSAGE,REQUSITIONER,BUYER,AMOUNT) values (?,?,?,?,?,?,?)";
								PreparedStatement ps1 = con.prepareStatement(querySubmitSummary);
								ps1.setString(1, null);
								ps1.setString(2, poNumber);
								ps1.setString(3, bid);
								ps1.setString(4, "Y");
								ps1.setString(5, buyerId);
								ps1.setString(6, endUserId);
								ps1.setString(7, amount);
								ps1.executeUpdate();
								ps1.close();
								String queryBusinessPartner = "Select BUSINESSPARTNEROID,PRIMARYEMAILID,SECONDARYEMAILID,TERTIARYEMAILID"
										+ " from BUSINESSPARTNER where BUSINESSPARTNEROID = ? and STATUS =? ";
								PreparedStatement ps2 = con.prepareStatement(queryBusinessPartner);
								ps2.setString(1, bid);
								ps2.setString(2, "A");
								rs2 = ps2.executeQuery();
								if (rs2.next()) {
									primaryEmailid = rs2.getString("PRIMARYEMAILID") == null ? "-"
											: rs2.getString("PRIMARYEMAILID");
									secondaryEmailid = rs2.getString("SECONDARYEMAILID") == null ? "-"
											: rs2.getString("SECONDARYEMAILID");
									tertiaryEmailid = rs2.getString("TERTIARYEMAILID") == null ? "-"
											: rs2.getString("TERTIARYEMAILID");

									emailArrayList.add(primaryEmailid);
									emailArrayList.add(secondaryEmailid);
									emailArrayList.add(tertiaryEmailid);
								}
								rs2.close();
								ps2.close();
								for (int counter = 0; counter <= emailArrayList.size() - 1; counter++) {

									String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
											+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
									ps2 = con.prepareStatement(queryChat);
									ps2.setString(1, bid);
									ps2.setString(2, invoiceNumber);
									ps2.setString(3, poNumber);
									ps2.setString(4, emailArrayList.get(counter));
									ps2.setString(5, "A");
									ps2.executeUpdate();
									ps2.close();
								}
							}
							rs1.close();
							ps.close();
						}
						rs.close();
						ps.close();
					}

					String queryNotification = "insert into CHATMESSAGE (BUSINESSPARTNEROID,SENDER,PONUMBER,"
							+ "INVOICENUMBER,MESSAGETEXT,SUBJECT,STATUS,CREATEDON) values (?,?,?,?,?,?,?,?)";
					ps = con.prepareStatement(queryNotification);
					ps.setString(1, bid);
					ps.setString(2, userEmailId);
					ps.setString(3, poNumber);
					ps.setString(4, invoiceNumber);
					ps.setString(5, message);
					ps.setString(6, subject);
					ps.setString(7, "A");
					ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.executeUpdate();
					ps.close();
					String queryFindStatus = "Select status from CHATSTATUS where PONUMBER = ?  "
							+ "and LOGGEDIN = ?  and INVOICENUMBER is null";
					ps = con.prepareStatement(queryFindStatus);
					ps.setString(1, poNumber);
					ps.setString(2, userEmailId);
					rs = ps.executeQuery();

					String statusUser = null;

					if (rs.next()) {
						String queryUpdate = "update CHATSTATUS set status = ? where PONUMBER = ?  "
								+ " and loggedin <> ? and INVOICENUMBER is null ";
						PreparedStatement ps1 = con.prepareStatement(queryUpdate);
						ps1.setString(1, "A");
						ps1.setString(2, poNumber);
						ps1.setString(3, userEmailId);
						ps1.executeUpdate();
						ps1.close();
						String queryUpdateChat = "update CHATSTATUS set status =? where PONUMBER = ? "
								+ "and LOGGEDIN = ? and INVOICENUMBER is null";
						ps1 = con.prepareStatement(queryUpdateChat);
						ps1.setString(1, "R");
						ps1.setString(2, poNumber);
						ps1.setString(3, userEmailId);
						ps1.executeUpdate();
						ps1.close();
					} else {

						String queryUpdate = "update CHATSTATUS set status = ? where PONUMBER = ? "
								+ " and loggedin <> ? and INVOICENUMBER is null";
						PreparedStatement ps1 = con.prepareStatement(queryUpdate);
						ps1.setString(1, "A");
						ps1.setString(2, poNumber);
						ps1.setString(3, userEmailId);
						ps1.executeUpdate();

						String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
								+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
						ps1 = con.prepareStatement(queryChat);
						ps1.setString(1, bid);
						ps1.setString(2, invoiceNumber);
						ps1.setString(3, poNumber);
						ps1.setString(4, userEmailId);
						ps1.setString(5, "R");
						ps1.executeUpdate();
						ps1.close();
					}
					rs.close();
					ps.close();
					responsejson.put("message", "Success");
					jsonArray.add(responsejson);
				}
			}
			con.commit();

		} catch (Exception e) {
			log.error("setChatStatus() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);

		}
		return jsonArray;
	}

	public JSONArray getInvoiceReadStatus(String bid, String userMailId) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(bid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(bid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		result = Validation.StringChecknull(userMailId);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.emailCheck(userMailId);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String po_data = "Select PONUMBER,INVOICENUMBER,STATUS from CHATSTATUS where BusinessPartnerOID =? "
				+ "and loggedin = ? and INVOICENUMBER is not null ";
		String poninvoices = "Select PONUMBER,INVOICENUMBER from poninvoicesummery where BusinessPartnerOID =? and MESSAGE= ? "
				+ "and INVOICENUMBER is not null ";

		ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> mapQueryList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, bid);
			ps.setString(2, userMailId);
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> singleList = new HashMap<String, String>();
				HashMap<String, String> poQuery = new HashMap<String, String>();

				String status1 = rs.getString("STATUS") == null ? "" : rs.getString("STATUS");
				if (!"".equalsIgnoreCase(status1)) {
					if ("A".equalsIgnoreCase(status1)) {
						poQuery.put("PONUMBER", rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER"));
						poQuery.put("INVOICENUMBER",
								rs.getString("INVOICENUMBER") == null ? "" : rs.getString("INVOICENUMBER"));
						poQuery.put("STATUS", status1);
						POQueryList.add(poQuery);
					}
				}

				String poNumber = rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER");
				String invoiceNumber = rs.getString("INVOICENUMBER") == null ? "" : rs.getString("INVOICENUMBER");
				String status = rs.getString("STATUS") == null ? "" : rs.getString("STATUS");
				singleList.put(poNumber + "_" + invoiceNumber, status);
				mapQueryList.add(singleList);
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement(poninvoices);
			ps.setString(1, bid);
			ps.setString(2, "Y");
			rs = ps.executeQuery();

			HashMap<String, String> singleList = new HashMap<String, String>();
			for (int ii = 0; ii < mapQueryList.size(); ii++) {
				singleList.putAll(mapQueryList.get(ii));
			}
			int jj = 0;
			while (rs.next()) {
				if (jj == 0) {
					POQueryList.clear();
					jj++;
				}
				jj++;

				HashMap<String, String> margeList = new HashMap<String, String>();

				String poNumber = rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER");
				String invoiceNumber = rs.getString("INVOICENUMBER") == null ? "" : rs.getString("INVOICENUMBER");

				if (singleList.containsKey(poNumber + "_" + invoiceNumber)) {
					String poStatus = singleList.get(poNumber + "_" + invoiceNumber);
					if ("A".equalsIgnoreCase(poStatus)) {
						margeList.put("PONUMBER", poNumber);
						margeList.put("INVOICENUMBER", invoiceNumber);
						margeList.put("STATUS", "A");
						POQueryList.add(margeList);
					}

				} else {
					margeList.put("PONUMBER", poNumber);
					margeList.put("INVOICENUMBER", invoiceNumber);
					margeList.put("STATUS", "A");
					POQueryList.add(margeList);
				}
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getInvoiceReadStatus() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POQueryList.size() > 0) {
			responsejson.put("poQueryList", POQueryList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray setChatStatusUpdate(String bid, String userMailId, String invoiceNumber, String poNumber)
			throws SQLException {

		String searchInvPOData = "Select count(*) as counter from CHATSTATUS where BusinessPartnerOID =? and loggedin = ? and PONUMBER = ? "
				+ " and INVOICENUMBER = ? ";

		String searchPOData = "Select count(*) as counter from CHATSTATUS where BusinessPartnerOID =? and loggedin = ? and PONUMBER = ? "
				+ " and INVOICENUMBER is null ";

		String po_data = "Update CHATSTATUS set STATUS =? where BusinessPartnerOID =? and loggedin = ? and PONUMBER = ? "
				+ " and INVOICENUMBER = ? ";

		String invdata = "Update CHATSTATUS set STATUS =? where BusinessPartnerOID =? and loggedin = ? and PONUMBER = ? "
				+ " and INVOICENUMBER is null ";

		Connection con = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		ResultSet rs = null;

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			if (invoiceNumber != null && !"".equalsIgnoreCase(invoiceNumber)) {

				ps = con.prepareStatement(searchInvPOData);
				ps.setString(1, bid);
				ps.setString(2, userMailId);
				ps.setString(3, poNumber);
				ps.setString(4, invoiceNumber);
				rs = ps.executeQuery();
				int count = 0;
				if (rs.next()) {
					count = rs.getInt("counter");

					if (count == 0) {
						String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
								+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
						ps2 = con.prepareStatement(queryChat);
						ps2.setString(1, bid);
						ps2.setString(2, invoiceNumber);
						ps2.setString(3, poNumber);
						ps2.setString(4, userMailId);
						ps2.setString(5, "R");
						ps2.executeUpdate();
						ps2.close();

					} else {
						PreparedStatement ps1 = con.prepareStatement(po_data);
						ps1.setString(1, "R");
						ps1.setString(2, bid);
						ps1.setString(3, userMailId);
						ps1.setString(4, poNumber);
						ps1.setString(5, invoiceNumber);
						ps1.executeUpdate();
						ps1.close();
					}
					responsejson.put("message", "Success");
					jsonArray.add(responsejson);
				}
				rs.close();
				ps.close();

			} else {

				ps = con.prepareStatement(searchPOData);
				ps.setString(1, bid);
				ps.setString(2, userMailId);
				ps.setString(3, poNumber);
				rs = ps.executeQuery();
				int count = 0;
				if (rs.next()) {
					count = rs.getInt("counter");

					if (count == 0) {

					} else {
						PreparedStatement ps1 = con.prepareStatement(invdata);
						ps1.setString(1, "R");
						ps1.setString(2, bid);
						ps1.setString(3, userMailId);
						ps1.setString(4, poNumber);
						ps1.executeUpdate();
						ps1.close();
					}
					responsejson.put("message", "Success");
					jsonArray.add(responsejson);
				}
				rs.close();
				ps.close();
			}
			con.commit();
		} catch (Exception e) {
			log.error("setChatStatusUpdate() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getInvoiceLineItemDetails(String invoice, String po_num) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		result = Validation.StringChecknull(invoice);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		/*
		String po_data = "Select distinct ps.INVOICENUMBER,ps.INVOICEDATE,ps.BILLOFLADINGDATE,ps.PONUMBER,"
				+ "ps.GRNNUMBER,i.LINEITEMNUMBER,i.DC,i.LINEITEMTOTALQUANTITY,i.UOM,"
				+ "ps.VENDORID,ps.PLANT,ps.BUSINESSPARTNEROID,i.LINEITEMTEXT,"
				+ "ps.CONTACTPERSON,ps.CONTACTPERSONPHONE,ps.CREATEDON,ps.CREATEDBY,"
				+ "ps.MODIFIEDON,ps.AMOUNT,ps.DESCRIPTION,i.INVOICESTATUS,ps.REMARK,"
				+ "ps.ENDUSERREMARKS,ps.MATERIAL_TYPE,i.LINEITEMTOTALAMOUNT,ps.ACTUALFILENAME,ps.SAVEDFILENAME,i.ACCEPTEDQTY,"
				+ "ps.OVERALLSTATUS,i.RATEPERQTY,ps.BUSINESSPARTNERTEXT,ps.PAYMENTAMOUNT,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT"
				+ ",sd.ACTUALFILENAME AS MULTIACTUALFILENAME,i.STORAGELOCATION,ps.ENDUSERSUPPSAVEDFILE,"
				+ "ps.ENDUSERSUPPACTUALFILE,ps.REQUSITIONER,ps.BUYER,ps.ENDUSERREMARKS,ps.CREDITADVICENO,ps.CREDITNOTENO, ps.SCRNNUMBER,"
				+ "sd.SAVEDFILENAME AS MULTISAVEDFILENAME,i.SERVICENUMBER,ps.IRNNUMBER,ps.IRNDATE,ps.NOTIFYENDUSEREMAILID "
				+ "from DELIVERYSUMMARY i join poninvoicesummery ps on i.InvoiceNumber=ps.InvoiceNumber "
				+ "and i.PONumber=ps.PONumber join INVOICESUPPDOCS sd on "
				+ "sd.INVOICENUMBER = ps.InvoiceNumber and  sd.PONUMBER = ps.PONumber "
				+ "where i.InvoiceNumber=? and i.PONumber=? order by i.LINEITEMNUMBER";
		*/
		
		String po_data =  "SELECT DISTINCT PS.INVOICENUMBER,PS.INVOICEDATE,PS.BILLOFLADINGDATE,PS.PONUMBER, "
				+ "PS.GRNNUMBER,I.LINEITEMNUMBER,I.DC,I.LINEITEMTOTALQUANTITY,I.UOM, "
				+ "PS.VENDORID,PS.PLANT,PS.BUSINESSPARTNEROID,I.LINEITEMTEXT, "
				+ "PS.CONTACTPERSON,PS.CONTACTPERSONPHONE,PS.CREATEDON,PS.CREATEDBY, "
				+ "PS.MODIFIEDON,PS.AMOUNT,PS.DESCRIPTION,I.INVOICESTATUS,PS.REMARK, "
				+ "PS.ENDUSERREMARKS,PS.MATERIAL_TYPE,I.LINEITEMTOTALAMOUNT,PS.ACTUALFILENAME,PS.SAVEDFILENAME,I.ACCEPTEDQTY, "
				+ "PS.OVERALLSTATUS,I.RATEPERQTY,PS.BUSINESSPARTNERTEXT,PS.PAYMENTAMOUNT,PS.TOTALAMTINCTAXES,PS.TAXAMOUNT "
				+ ",SD.ACTUALFILENAME AS MULTIACTUALFILENAME,I.STORAGELOCATION,PS.ENDUSERSUPPSAVEDFILE, "
				+ "PS.ENDUSERSUPPACTUALFILE,PS.REQUSITIONER,PS.BUYER,PS.ENDUSERREMARKS,PS.CREDITADVICENO,PS.CREDITNOTENO, PS.SCRNNUMBER, "
				+ "SD.SAVEDFILENAME AS MULTISAVEDFILENAME,I.SERVICENUMBER,PS.IRNNUMBER,PS.IRNDATE,PS.NOTIFYENDUSEREMAILID, "
				+ "PED.DELVPLANT,PED.DELVPLANTNAME "
				+ "FROM DELIVERYSUMMARY I, PONINVOICESUMMERY PS, INVOICESUPPDOCS SD , POEVENTDETAILS PED "
				+ "WHERE I.INVOICENUMBER=PS.INVOICENUMBER  "
				+ "AND I.PONUMBER=PS.PONUMBER AND PED.PONUMBER = PS.PONUMBER AND I.PONUMBER = PED.PONUMBER AND "
				+ "SD.INVOICENUMBER = PS.INVOICENUMBER AND  SD.PONUMBER = PS.PONUMBER  "
				+ "AND I.INVOICENUMBER = ? AND I.PONUMBER = ? ORDER BY I.LINEITEMNUMBER ";
		
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> invoiceData = new HashMap<String, String>();

				invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				invoiceData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
				invoiceData.put("PO_NUMBER", rs.getString("PONUMBER"));
				invoiceData.put("GRNNUMBER", rs.getString("GRNNUMBER"));
				invoiceData.put("LINEITEMNUMBER", rs.getString("LINEITEMNUMBER"));
				invoiceData.put("ORDERNUMBER", rs.getString("DC"));
				invoiceData.put("QUANTITY", rs.getString("LINEITEMTOTALQUANTITY"));
				invoiceData.put("UOM", rs.getString("UOM"));
				invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
				invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
				invoiceData.put("VENDORID", rs.getString("VENDORID"));
				invoiceData.put("BILLOFLADINGDATE", rs.getString("BILLOFLADINGDATE"));
				invoiceData.put("STORAGELOCATION", rs.getString("STORAGELOCATION"));
				invoiceData.put("ENDUSERREMARKS", rs.getString("ENDUSERREMARKS"));
				invoiceData.put("ENDUSERSUPPSAVEDFILE", rs.getString("ENDUSERSUPPSAVEDFILE"));
				invoiceData.put("ENDUSERSUPPACTUALFILE", rs.getString("ENDUSERSUPPACTUALFILE"));
				invoiceData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
				invoiceData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
				invoiceData.put("PLANT", rs.getString("PLANT"));
				invoiceData.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
				invoiceData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
				invoiceData.put("BUSSINESSPARTNERTEXT", rs.getString("BUSINESSPARTNERTEXT"));
				invoiceData.put("CREATEDBY", rs.getString("CREATEDBY"));
				invoiceData.put("CREATEDON", rs.getString("CREATEDON"));
				invoiceData.put("MODIFIEDON", rs.getString("MODIFIEDON"));
				invoiceData.put("TOTALAMOUNT", rs.getString("AMOUNT"));
				invoiceData.put("DESCRIPTION", rs.getString("DESCRIPTION"));
				invoiceData.put("STATUS", rs.getString("INVOICESTATUS"));
				invoiceData.put("INVOICEAMOUNT", rs.getString("LINEITEMTOTALAMOUNT"));
				invoiceData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
				invoiceData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
				invoiceData.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
				invoiceData.put("LINEITEMTEXT", rs.getString("LINEITEMTEXT"));
				invoiceData.put("RATEPERQTY", rs.getString("RATEPERQTY"));
				invoiceData.put("PAYMENTAMOUNT",
						rs.getString("PAYMENTAMOUNT") != null ? rs.getString("PAYMENTAMOUNT") : "0");
				invoiceData.put("MULTIACTUALFILENAME", rs.getString("MULTIACTUALFILENAME"));
				invoiceData.put("MULTISAVEDFILENAME", rs.getString("MULTISAVEDFILENAME"));
				invoiceData.put("ACCEPTEDQTY", rs.getString("ACCEPTEDQTY") != null ? rs.getString("ACCEPTEDQTY") : "0");
				invoiceData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
				invoiceData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
				invoiceData.put("SERVICENUMBER", rs.getString("SERVICENUMBER"));
				invoiceData.put("SCRNNUMBER", rs.getString("SCRNNUMBER"));
				invoiceData.put("USERREMARK", rs.getString("ENDUSERREMARKS"));
				invoiceData.put("VENDORREMARKS", rs.getString("REMARK"));
				invoiceData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
				invoiceData.put("BUYER", rs.getString("BUYER"));
				invoiceData.put("IRNNUMBER", rs.getString("IRNNUMBER"));
				invoiceData.put("IRNDATE", rs.getString("IRNDATE"));
				invoiceData.put("ACCEPTTYPE", getacceptancetype(rs.getString("INVOICENUMBER"), rs.getString("PONUMBER"), con));
				invoiceData.put("NOTIFYENDUSEREMAILID",	rs.getString("NOTIFYENDUSEREMAILID") == null ? "" : rs.getString("NOTIFYENDUSEREMAILID"));
				invoiceData.put("DELVPLANT", rs.getString("DELVPLANT") == null ? "" : rs.getString("DELVPLANT"));
				invoiceData.put("DELVPLANTNAME",rs.getString("DELVPLANTNAME") == null ? "" : rs.getString("DELVPLANTNAME"));

				POList.add(invoiceData);
			}
			rs.close();
			ps.close();

			if (POList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("poData", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Fail"); // add error and display it the front.
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getInvoiceLineItemDetails() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public String getacceptancetype(String invoicenumber, String ponumber, Connection con) {

		PreparedStatement ps = null;
		ResultSet rs = null;
		String accepttype = "";
		try {
			String acceptancecheck = "select count(*) as scounter from AUDIT_ACCEPTQTY "
					+ "where PONUMBER=? AND INVOICENUMBER = ? AND FLAG !=?";

			int count = 0;
			ps = con.prepareStatement(acceptancecheck);
			ps.setString(1, ponumber);
			ps.setString(2, invoicenumber);
			ps.setString(3, "N");
			rs = ps.executeQuery();
			if (rs.next()) {
				count = rs.getInt("scounter");
			}
			if (count > 0) {
				accepttype = "S";
			}
			rs.close();
			ps.close();
			String behalfcheck = "select count(*) as bcounter from AUDIT_ACCEPTQTY_BEHALF "
					+ "where PONUMBER=? AND INVOICENUMBER = ? AND FLAG !=?";

			int bcount = 0;
			ps = con.prepareStatement(behalfcheck);
			ps.setString(1, ponumber);
			ps.setString(2, invoicenumber);
			ps.setString(3, "N");
			rs = ps.executeQuery();
			if (rs.next()) {
				bcount = rs.getInt("bcounter");
			}
			rs.close();
			ps.close();
			if (bcount > 0) {
				accepttype = "B";
			}
			if (count == 0 && bcount == 0) {
				accepttype = "S";
			}

		} catch (Exception e) {
			log.error("getacceptancetype() :", e.fillInStackTrace());

		}
		return accepttype;
	}

	public JSONArray getinvoiceQueryDetails(String bid, String po_num, String invoice_num) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		result = Validation.StringChecknull(invoice_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(bid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(bid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String po_data = "SELECT * FROM CHATMESSAGE where BusinessPartnerOID = ? AND PONUMBER = ? AND INVOICENUMBER = ? "
				+ "AND STATUS=? order by createdon desc";
		ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, bid);
			ps.setString(2, po_num);
			ps.setString(3, invoice_num);
			ps.setString(4, "A");
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> poQuery = new HashMap<String, String>();
				poQuery.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
				poQuery.put("SENDER", rs.getString("Sender"));
				poQuery.put("PONUMBER", rs.getString("PONUMBER"));
				poQuery.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				poQuery.put("MESSAGETEXT", rs.getString("MESSAGETEXT"));
				poQuery.put("SUBJECT", rs.getString("SUBJECT"));
				poQuery.put("STATUS", rs.getString("STATUS"));
				poQuery.put("CREATEDON", rs.getString("CREATEDON"));
				POQueryList.add(poQuery);
			}
			rs.close();
			ps.close();
			if (POQueryList.size() > 0) {
				responsejson.put("poQueryList", POQueryList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getinvoiceQueryDetails() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray gettrackinvoicestatus(String invoicenumber, String ponumber, String bussinesspartneroid)
			throws SQLException {

		boolean result;
		result = Validation.StringChecknull(ponumber);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(invoicenumber);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(bussinesspartneroid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(bussinesspartneroid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		String qdata = "Select INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,TO_CHAR(MODIFIEDTIME,'DD-MM-RRRR HH:MI:SS AM') MODIFYTIME, "
				+ " MODIFIEDBY,STATUS ,RESUBMITTEDINVOICENO from invoicetracker where ponumber =? AND invoicenumber =?"
				+ " order by MODIFIEDTIME desc";

		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(qdata);
			ps.setString(1, ponumber);
			ps.setString(2, invoicenumber);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> invoiceData = new HashMap<String, String>();
				invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				invoiceData.put("PONUMBER", rs.getString("PONUMBER"));
				invoiceData.put("BUSSINESSPARTNEROID", rs.getString("BUSSINESSPARTNEROID"));
				invoiceData.put("MODIFIEDTIME", rs.getString("MODIFYTIME"));
				invoiceData.put("MODIFIEDBY", rs.getString("MODIFIEDBY"));
				invoiceData.put("STATUS", rs.getString("STATUS"));
				invoiceData.put("RESUBMITTEDINVOICENO", rs.getString("RESUBMITTEDINVOICENO"));
				invoiceList.add(invoiceData);
			}
			rs.close();
			ps.close();
			if (invoiceList.size() > 0) {
				ArrayList<HashMap<String, String>> trackstatusList = new ArrayList<HashMap<String, String>>();
				HashMap<String, String> trackstatus = new HashMap<String, String>();
				int flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("P")) {
						trackstatus.put("PENDINGSTATUS", region.get("STATUS"));

						try {
							/*
							 * String dateqw = region.get("MODIFIEDTIME"); DateFormat formatter = new
							 * SimpleDateFormat(); SimpleDateFormat inputFormat = new
							 * SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("PENDINGMODIFIEDTIME", outputFormat.format(date).toString());
							 */
							String dateqw = region.get("MODIFIEDTIME");
							trackstatus.put("PENDINGMODIFIEDTIME", dateqw);

						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 1 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;

					}
					if (flag == 1) {
						break;
					}

				}
				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("M")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("MPENDINGSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("MPENDINGMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("MPENDINGMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 2 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}
				}
				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("R")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("REJECTEDSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("REJECTEDMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("REJECTEDMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 3 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}

				}
				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("A")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("ACCEPTEDSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("ACCEPTEDMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("ACCEPTEDMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 4 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}
				}
				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("PP")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("PARTIALLYPAIDSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("PARTIALLYPAIDMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("PARTIALLYPAIDMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 5: ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;

					}
					if (flag == 1) {
						break;
					}
				}
				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("PRO")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("PROCESSEDSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("PROCESSEDMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("PROCESSEDMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 6 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;

					}
					if (flag == 1) {
						break;
					}
				}
				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("RSAP")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("REVERSEDSAPSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("REVERSEDSAPMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("REVERSEDSAPMODIFIEDTIME", dateqw);

						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 7 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}
				}
				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("PD")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("FULLYPAIDSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("FULLYPAIDSTATUSMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("FULLYPAIDSTATUSMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 8 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}
				}

				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("V")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("RETURNSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("RETURNMODIFIEDTIME", outputFormat.format(date).toString());
							 */
							trackstatus.put("RETURNMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 9 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}
				}

				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("S")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("RESUBMITSTATUS", region.get("STATUS"));
						trackstatus.put("RESUBMITTEDINVOICENO", region.get("RESUBMITTEDINVOICENO"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("RESUBMITMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("RESUBMITMODIFIEDTIME", dateqw);

						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 10 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}
				}

				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("RO")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("REOPENED", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("REOPENEDMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("REOPENEDMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 11 : ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}
				}

				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("C")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("CORRECTEDSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("CORRECTEDMODIFIEDTIME",
							 * outputFormat.format(date).toString());
							 */
							trackstatus.put("CORRECTEDMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 11: ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}
					if (flag == 1) {
						break;
					}
				}

				flag = 0;
				for (int j = 0; j < invoiceList.size(); j++) {
					HashMap<String, String> region = invoiceList.get(j);
					if (region.get("STATUS").equalsIgnoreCase("INV")) {
						trackstatus = new HashMap<String, String>();
						trackstatus.put("INVALIDSTATUS", region.get("STATUS"));
						try {
							String dateqw = region.get("MODIFIEDTIME");
							/*
							 * DateFormat formatter = new SimpleDateFormat(); SimpleDateFormat inputFormat =
							 * new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS"); Date date; date =
							 * inputFormat.parse(dateqw); SimpleDateFormat outputFormat = new
							 * SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
							 * trackstatus.put("INVALIDMODIFIEDTIME", outputFormat.format(date).toString());
							 */
							trackstatus.put("INVALIDMODIFIEDTIME", dateqw);
						} catch (Exception e) {
							log.error("gettrackinvoicestatus() 12: ", e.fillInStackTrace());

						}
						trackstatusList.add(trackstatus);
						flag = 1;
					}

					if (flag == 1) {
						break;
					}
				}

				responsejson.put("message", "Success");
				responsejson.put("trackstatusListdata", trackstatusList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("gettrackinvoicestatus() 12 : ", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getIRNDetails(String actualfilename, String savedfilename) throws Exception {

		String sourceDir = "";
		String path = "";
		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();

		String[] fileName = savedfilename.split("_");
		String timestamp = fileName[fileName.length - 1];
		int iend = timestamp.indexOf(".");
		if (iend != -1) {
			timestamp = timestamp.substring(0, iend);
		}
		path = fileName[0] + "//" + fileName[1] + "//" + timestamp + "//" + savedfilename;
		HashMap<String, String> invoiceData = new HashMap<String, String>();

		InputStream input = EmailImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		prop.load(input);
		String tempsourceDir = prop.getProperty("fileLocation") + path;
		sourceDir = tempsourceDir;

		try {

			PDDocument document = null;
			document = PDDocument.load(sourceDir);
			List<PDPage> pages = document.getDocumentCatalog().getAllPages();
			PDPage page = pages.get(0);
			BufferedImage image = page.convertToImage();
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			BinaryBitmap bitmap = null;
			bitmap = new BinaryBitmap(new HybridBinarizer(source));
			com.google.zxing.qrcode.QRCodeReader qr = new com.google.zxing.qrcode.QRCodeReader();
			Result qrCodeResult = qr.decode(bitmap);
			String qrCode = qrCodeResult.getText();
			java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
			String parts = qrCode.split("\\.")[1];
			String payloadJson = new String(decoder.decode(parts));

			Object object = new JSONParser().parse(payloadJson);
			JSONObject jsonObject = (JSONObject) object;
			String data = (String) jsonObject.get("data");
			Object obj = new JSONParser().parse(data);
			JSONObject json = (JSONObject) obj;
			String SellerGstin = (String) json.get("SellerGstin");
			String BuyerGstin = (String) json.get("BuyerGstin");
			String DocNo = (String) json.get("DocNo");
			String DocDt = (String) json.get("DocDt");
			Double TotInvVal = (Double) json.get("TotInvVal");
			String Irn = (String) json.get("Irn");

			invoiceData.put("IRN_Number", Irn);
			invoiceData.put("IR_DocDt", DocDt);
			invoiceData.put("SellerGstin", SellerGstin);
			invoiceData.put("BuyerGstin", BuyerGstin);
			invoiceData.put("Invoice_Number", DocNo);
			invoiceList.add(invoiceData);
			if (invoiceList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("invoiceData", invoiceList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			log.error("getIRNDetails() :", e.fillInStackTrace());

		}
		return jsonArray;
	}

	public JSONArray getVendorReturn(String invoice, String po_num) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String archiveInsert = "INSERT INTO PONINVOICESUMMERYARCHIVE "
				+ "SELECT *  FROM PONINVOICESUMMERY where INVOICENUMBER=? and  PONUMBER=? and OVERALLSTATUS='V'";

		String deleteInvoice = "DELETE FROM PONINVOICESUMMERY WHERE INVOICENUMBER =? and PONUMBER=?";

		String deleteStatus = "DELETE FROM DELIVERYSUMMARY WHERE INVOICENUMBER =? and PONUMBER=?";
		String deleteStatusInvoiceApp = " DELETE FROM INVOICEAPPROVAL WHERE INVOICENUMBER =? and PONUMBER=?";
		String deleteSupportingDocs = "DELETE FROM INVOICESUPPDOCS WHERE INVOICENUMBER =? and PONUMBER=?";
		String deletecheckboxentry = "DELETE FROM AUDIT_ACCEPTQTY WHERE INVOICENUMBER =? and PONUMBER=?";
		String deletecheckboxentryofbehalf = "DELETE FROM AUDIT_ACCEPTQTY_BEHALF WHERE INVOICENUMBER =? and PONUMBER=?";
		int value = 0;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(archiveInsert);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement(deleteInvoice);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement(deleteStatus);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			value = ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(deleteSupportingDocs);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			value = ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(deletecheckboxentry);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			value = ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(deletecheckboxentryofbehalf);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			value = ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(deleteStatusInvoiceApp);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			value = ps.executeUpdate();
			ps.close();
			con.commit();
			if (value > 0) {
				responsejson.put("message", " Vendor Return done Sucess");
			} else {
				responsejson.put("message", "Vendor Return  Fail");
			}
		} catch (Exception e) {
			log.error("getVendorReturn() :", e.fillInStackTrace());

			responsejson.put("message", e.getLocalizedMessage());
			jsonArray.add(responsejson);
			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray gethistoricinvoice(String bid) throws SQLException {
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();

		String po_query = "SELECT pis.INVOICENUMBER,pis.INVOICEDATE,"
				+ "pis.PONUMBER,pis.AMOUNT,pis.PAYMENTAMOUNT,pis.OVERALLSTATUS FROM poninvoicesummery pis where "
				+ "pis.BusinessPartnerOID =? and invoicenumber is not null and ONEXSTATUS=? ORDER BY pis.createdon desc";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_query);
			ps.setString(1, bid);
			ps.setString(2, "H");
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();

				poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				poData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
				poData.put("PO_NUMBER", rs.getString("PONUMBER"));
				poData.put("TOTALAMOUNT", rs.getString("AMOUNT"));
				poData.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
				poData.put("STATUS", rs.getString("OVERALLSTATUS"));
				POList.add(poData);
			}
			rs.close();
			ps.close();
			if (POList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("historyinvoice", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Id");
				jsonArray.add(responsejson);
			}
		} catch (SQLException e) {
			log.error("gethistoricinvoice() :", e.fillInStackTrace());

			responsejson.put("message", e.getLocalizedMessage());
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getdownloadInvoceidIdDetails(String bid, String status, String ponumber, String invoicenumber,
			String fromdate, String todate, String plant, String companyCode) {

		String query = "";
		List<String> invoicedata = new ArrayList<>();
		List<String> podata = new ArrayList<>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String basePoQuery = " and pis.ponumber=ia.ponumber ";

		String compCodeJoinQuery = " join podetails pod on pis.ponumber = pod.ponumber ";
		String compCodeQuery = " AND pod.companycode = ? ";

		try {
			con = DBConnection.getConnection();
			if (!status.equalsIgnoreCase("AS")) {
				if (status.equalsIgnoreCase("H")) {

					query = "SELECT PIS.INVOICENUMBER,PIS.PONUMBER,PIS.CREATEDON,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
							+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ? and PIS.ONEXSTATUS=? AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery + "Union "
							+ "SELECT distinct PIS.INVOICENUMBER,PIS.PONUMBER,PIS.CREATEDON,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
							+ "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber "
							+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ? and ONEXSTATUS=? AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
							+ "ORDER BY CREATEDON DESC";
				} else if (status.equalsIgnoreCase("ALL")) {

					query = "SELECT PIS.INVOICENUMBER,PIS.PONUMBER,PIS.CREATEDON,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
							+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ?  AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery + "Union "
							+ "SELECT distinct PIS.INVOICENUMBER,PIS.PONUMBER,PIS.CREATEDON,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
							+ "join invoiceapproval ia "
							+ "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber "
							+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ?  AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
							+ "ORDER BY CREATEDON DESC";
				} else if (status.equalsIgnoreCase("P") || status.equalsIgnoreCase("V")) {

					query = "SELECT PIS.INVOICENUMBER,PIS.PONUMBER,PIS.CREATEDON,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
							+ compCodeJoinQuery + " WHERE "
							+ "PIS.BUSINESSPARTNEROID = ? AND (PIS.OVERALLSTATUS= ? OR PIS.OVERALLSTATUS= ?) AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery + "Union "
							+ "SELECT distinct PIS.INVOICENUMBER,PIS.PONUMBER,PIS.CREATEDON,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
							+ "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber "
							+ compCodeJoinQuery + " WHERE "
							+ "PIS.BUSINESSPARTNEROID = ? AND (PIS.OVERALLSTATUS= ? OR PIS.OVERALLSTATUS= ?) AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
							+ "ORDER BY CREATEDON DESC";
				} else {

					query = "SELECT PIS.INVOICENUMBER,PIS.PONUMBER,PIS.CREATEDON,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
							+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ? AND PIS.OVERALLSTATUS= ? AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery + "Union "
							+ "SELECT distinct PIS.INVOICENUMBER,PIS.PONUMBER,PIS.CREATEDON,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
							+ "join invoiceapproval ia "
							+ "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber "
							+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ? AND PIS.OVERALLSTATUS= ? AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
							+ "ORDER BY CREATEDON DESC";
				}

				ps = con.prepareStatement(query);

				if (status.equalsIgnoreCase("ALL")) {
					ps.setString(1, bid);
					ps.setString(2, companyCode);
					ps.setString(3, bid);
					ps.setString(4, companyCode);
				} else if (status.equalsIgnoreCase("P")) {
					ps.setString(1, bid);
					ps.setString(2, "P");
					ps.setString(3, "M");
					ps.setString(4, companyCode);
					ps.setString(5, bid);
					ps.setString(6, "P");
					ps.setString(7, "M");
					ps.setString(8, companyCode);
				} else if (status.equalsIgnoreCase("V")) {
					ps.setString(1, bid);
					ps.setString(2, "V");
					ps.setString(3, "RO");
					ps.setString(4, companyCode);
					ps.setString(5, bid);
					ps.setString(6, "V");
					ps.setString(7, "RO");
					ps.setString(8, companyCode);
				} else {
					ps.setString(1, bid);
					ps.setString(2, status);
					ps.setString(3, companyCode);
					ps.setString(4, bid);
					ps.setString(5, status);
					ps.setString(6, companyCode);
				}

				rs = ps.executeQuery();
				String mPO = null;
				String allPO = null;
				while (rs.next()) {
					mPO = (rs.getString("MPO") == null) ? "-" : rs.getString("MPO");
					if ("Y".equals(mPO)) {
						allPO = (rs.getString("ALLPO") == null) ? "-" : rs.getString("ALLPO");
						String allPOArr[] = null;
						if (allPO != null) {
							allPOArr = allPO.split(",");
						}

						if (allPOArr != null && allPOArr.length > 0) {
							for (int i = 0; i < allPOArr.length; i++) {
								invoicedata.add(rs.getString("INVOICENUMBER"));
								podata.add(allPOArr[i]);
							}

						} else {
							invoicedata.add(rs.getString("INVOICENUMBER"));
							podata.add(rs.getString("PONUMBER"));
						}
					} else {
						invoicedata.add(rs.getString("INVOICENUMBER"));
						podata.add(rs.getString("PONUMBER"));
					}
				}
				if (invoicedata.size() > 0) {
					responsejson.put("message", "Success");
					responsejson.put("invoicedata", invoicedata);
					responsejson.put("podata", podata);
					jsonArray.add(responsejson);
				} else {
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
				}
			}

			else {
				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				param.add(bid);
				if (!plant.equalsIgnoreCase("NA")) {
					String plantCode = " AND PIS.PLANT=?";
					subquery = subquery + plantCode;
					param.add(plant);
				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND PIS.PONUMBER=?";
					subquery = subquery + po;
					basePoQuery = " and pis.basepo=ia.ponumber ";
					param.add(ponumber);
				}
				if (!invoicenumber.equalsIgnoreCase("NA")) {
					String in = " AND PIS.INVOICENUMBER=?";
					subquery = subquery + in;
					param.add(invoicenumber);
				}
				if ((!fromdate.equalsIgnoreCase("NA")) && (!fromdate.equalsIgnoreCase("Invalid date"))) {
					String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
							+ "AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
					param.add(fromdate);
					param.add(todate);
				}
				param.add(companyCode);

				param.add(bid);

				if (!plant.equalsIgnoreCase("NA")) {
					param.add(plant);
				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					param.add(ponumber);
				}
				if (!invoicenumber.equalsIgnoreCase("NA")) {
					param.add(invoicenumber);
				}
				if ((!fromdate.equalsIgnoreCase("NA")) && (!fromdate.equalsIgnoreCase("Invalid date"))) {
					param.add(fromdate);
					param.add(todate);
				}
				param.add(companyCode);

				String advqdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
						+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
						+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
						+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
						+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
						+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ? " + subquery + " AND "
						+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery + "Union "
						+ "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
						+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
						+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
						+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
						+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT,PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
						+ "join invoiceapproval ia " + "ON PIS.invoicenumber = ia.invoicenumber " + basePoQuery
						+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ? " + subquery + " AND "
						+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
						+ "ORDER BY CREATEDON DESC";

				Pagination pg = new Pagination(advqdata, 0);
				rs = pg.execute(con, param);
				String mPO = null;
				String allPO = null;
				while (rs.next()) {
					mPO = (rs.getString("MPO") == null) ? "-" : rs.getString("MPO");
					if ("Y".equals(mPO)) {
						allPO = (rs.getString("ALLPO") == null) ? "-" : rs.getString("ALLPO");
						String allPOArr[] = null;
						if (allPO != null) {
							allPOArr = allPO.split(",");
						}

						if (allPOArr != null && allPOArr.length > 0) {
							for (int i = 0; i < allPOArr.length; i++) {
								invoicedata.add(rs.getString("INVOICENUMBER"));
								podata.add(allPOArr[i]);
							}

						} else {
							invoicedata.add(rs.getString("INVOICENUMBER"));
							podata.add(rs.getString("PONUMBER"));
						}
					} else {
						invoicedata.add(rs.getString("INVOICENUMBER"));
						podata.add(rs.getString("PONUMBER"));
					}
				}
				if (invoicedata.size() > 0) {
					responsejson.put("message", "Success");
					responsejson.put("invoicedata", invoicedata);
					responsejson.put("podata", podata);
					jsonArray.add(responsejson);
				} else {
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
				}
			}
		}

		catch (Exception e) {
			log.error("getdownloadInvoceidIdDetails() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray getSimpoInvoiceLineItemDetails(String invoice, List<String> po_num_list) throws SQLException {

		boolean result;

		for (String po_num : po_num_list) {
			result = Validation.StringChecknull(po_num);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(invoice);

		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String po_data = "Select distinct ps.INVOICENUMBER,ps.INVOICEDATE,ps.BILLOFLADINGDATE,ps.PONUMBER,"
				+ "ps.GRNNUMBER,i.LINEITEMNUMBER,i.DC,i.LINEITEMTOTALQUANTITY,i.UOM,"
				+ "ps.VENDORID,ps.PLANT,ps.BUSINESSPARTNEROID,i.LINEITEMTEXT,"
				+ "ps.CONTACTPERSON,ps.CONTACTPERSONPHONE,ps.CREATEDON,ps.CREATEDBY,"
				+ "ps.MODIFIEDON,ps.AMOUNT,ps.DESCRIPTION,i.INVOICESTATUS,ps.REMARK,"
				+ "ps.ENDUSERREMARKS,i.LINEITEMTOTALAMOUNT,ps.ACTUALFILENAME,ps.SAVEDFILENAME,i.ACCEPTEDQTY,"
				+ "ps.OVERALLSTATUS,i.RATEPERQTY,ps.BUSINESSPARTNERTEXT,ps.PAYMENTAMOUNT,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT"
				+ ",sd.ACTUALFILENAME AS MULTIACTUALFILENAME,i.STORAGELOCATION,ps.ENDUSERSUPPSAVEDFILE,"
				+ "ps.ENDUSERSUPPACTUALFILE,ps.REQUSITIONER,ps.BUYER,ps.ENDUSERREMARKS,ps.CREDITADVICENO,ps.CREDITNOTENO, ps.SCRNNUMBER,"
				+ "sd.SAVEDFILENAME AS MULTISAVEDFILENAME,i.SERVICENUMBER,ps.IRNNUMBER,ps.IRNDATE "
				+ "from DELIVERYSUMMARY i join poninvoicesummery ps on i.InvoiceNumber=ps.InvoiceNumber "
				+ "and i.PONumber=ps.PONumber join INVOICESUPPDOCS sd on "
				+ "sd.INVOICENUMBER = ps.InvoiceNumber and  sd.PONUMBER = ps.PONumber "
				+ "where i.InvoiceNumber=? and i.PONumber=? order by i.LINEITEMNUMBER";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			for (int i = 0; i < po_num_list.size(); i++) {

				ps = con.prepareStatement(po_data);
				ps.setString(1, invoice);
				ps.setString(2, po_num_list.get(i));
				rs = ps.executeQuery();
				while (rs.next()) {

					HashMap<String, String> invoiceData = new HashMap<String, String>();

					invoiceData.put("INVOICENUMBER", rs.getString("InvoiceNumber"));
					invoiceData.put("INVOICEDATE", rs.getString("InvoiceDate"));
					invoiceData.put("PO_NUMBER", rs.getString("PONumber"));
					invoiceData.put("GRNNUMBER", rs.getString("GRNNumber"));
					invoiceData.put("LINEITEMNUMBER", rs.getString("LineItemNumber"));
					invoiceData.put("ORDERNUMBER", rs.getString("DC"));
					invoiceData.put("QUANTITY", rs.getString("LINEITEMTOTALQUANTITY"));
					invoiceData.put("UOM", rs.getString("UOM"));
					invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
					invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
					invoiceData.put("VENDORID", rs.getString("VendorID"));
					invoiceData.put("BILLOFLADINGDATE", rs.getString("BILLOFLADINGDATE"));
					invoiceData.put("STORAGELOCATION", rs.getString("STORAGELOCATION"));
					invoiceData.put("ENDUSERREMARKS", rs.getString("ENDUSERREMARKS"));
					invoiceData.put("ENDUSERSUPPSAVEDFILE", rs.getString("ENDUSERSUPPSAVEDFILE"));
					invoiceData.put("ENDUSERSUPPACTUALFILE", rs.getString("ENDUSERSUPPACTUALFILE"));
					invoiceData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					invoiceData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					invoiceData.put("PLANT", rs.getString("Plant"));
					invoiceData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
					invoiceData.put("BUSSINESSPARTNERTEXT", rs.getString("BUSINESSPARTNERTEXT"));
					invoiceData.put("CREATEDBY", rs.getString("CreatedBy"));
					invoiceData.put("CREATEDON", rs.getString("CreatedOn"));
					invoiceData.put("MODIFIEDON", rs.getString("ModifiedOn"));
					invoiceData.put("TOTALAMOUNT", rs.getString("AMOUNT"));
					invoiceData.put("DESCRIPTION", rs.getString("Description"));
					invoiceData.put("STATUS", rs.getString("INVOICESTATUS"));
					invoiceData.put("INVOICEAMOUNT", rs.getString("LINEITEMTOTALAMOUNT"));
					invoiceData.put("ACTUALFILENAME", rs.getString("ActualFileName"));
					invoiceData.put("SAVEDFILENAME", rs.getString("SavedFileName"));
					invoiceData.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					invoiceData.put("LINEITEMTEXT", rs.getString("LINEITEMTEXT"));
					invoiceData.put("RATEPERQTY", rs.getString("RATEPERQTY"));
					invoiceData.put("PAYMENTAMOUNT",
							rs.getString("PAYMENTAMOUNT") != null ? rs.getString("PAYMENTAMOUNT") : "0");
					invoiceData.put("MULTIACTUALFILENAME", rs.getString("MULTIACTUALFILENAME"));
					invoiceData.put("MULTISAVEDFILENAME", rs.getString("MULTISAVEDFILENAME"));
					invoiceData.put("ACCEPTEDQTY", rs.getString("ACCEPTEDQTY"));
					invoiceData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					invoiceData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					invoiceData.put("SERVICENUMBER", rs.getString("SERVICENUMBER"));
					invoiceData.put("SCRNNUMBER", rs.getString("SCRNNUMBER"));
					invoiceData.put("USERREMARK", rs.getString("ENDUSERREMARKS"));
					invoiceData.put("VENDORREMARKS", rs.getString("REMARK"));
					invoiceData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
					invoiceData.put("BUYER", rs.getString("BUYER"));
					invoiceData.put("IRNNUMBER", rs.getString("IRNNUMBER"));
					invoiceData.put("IRNDATE", rs.getString("IRNDATE"));

					POList.add(invoiceData);
				}
				rs.close();
				ps.close();
			}

			if (POList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("poData", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Fail"); // add error and display it the front.
				jsonArray.add(responsejson);
			}
		} catch (SQLException e) {
			log.error("getSimpoInvoiceLineItemDetails() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getInvoiceDetailsCountAsPerStatus(String bid, int nPage, String status, String invno, String pono,
			String fdate, String tdate, String plant, Connection con, PreparedStatement ps, ResultSet rs) {

		try {

			HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
			int allCounter = 0;

			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ"))) {

				String invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber)as count FROM PONINVOICESUMMERY pis WHERE "
						+ "PIS.INVOICENUMBER IS NOT NULL AND " + "PIS.BUSINESSPARTNEROID = ?"
						+ " and pis.overallstatus is not null " + " Group by pis.overallstatus";

				ps = con.prepareStatement(invoice_data);
				ps.setString(1, bid);

				rs = ps.executeQuery();
				while (rs.next()) {
					String sts = rs.getString("overallstatus");
					String count = rs.getString("count");
					countAsPerStatus.put(sts, count);
					allCounter += Integer.parseInt(count);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				rs.close();
				ps.close();

			} else {
				String subquery = "";
				if ((!status.equalsIgnoreCase("ASWP")) && (!status.equalsIgnoreCase("ASSQ"))) {
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PLANT=?";
						subquery = subquery + po;
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PONUMBER=?";
						subquery = subquery + po;
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND PIS.INVOICENUMBER=?";
						subquery = subquery + in;
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
					}
				} else if (status.equalsIgnoreCase("ASWP")) {

					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PONUMBER=?";
						subquery = subquery + po;
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND PIS.INVOICENUMBER=?";
						subquery = subquery + in;
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
					}
				} else if (status.equalsIgnoreCase("ASSQ")) {

					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND PIS.INVOICENUMBER=?";
						subquery = subquery + in;
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
					}
				}

				String invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber)as count FROM PONINVOICESUMMERY pis WHERE "
						+ "PIS.INVOICENUMBER IS NOT NULL AND " + "PIS.BUSINESSPARTNEROID = ? "
						+ " and pis.overallstatus is not null " + subquery + " Group by pis.overallstatus";

				ps = con.prepareStatement(invoice_data);
				ps.setString(1, bid);
				int queryCounter = 1;
				if ((!status.equalsIgnoreCase("ASWP")) && (!status.equalsIgnoreCase("ASSQ"))) {

					if (!plant.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, plant);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						queryCounter++;
						ps.setString(queryCounter, fdate);
						queryCounter++;
						ps.setString(queryCounter, tdate);
					}
				} else if (status.equalsIgnoreCase("ASWP")) {

					if (!pono.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						queryCounter++;
						ps.setString(queryCounter, fdate);
						queryCounter++;
						ps.setString(queryCounter, tdate);
					}
				} else if (status.equalsIgnoreCase("ASSQ")) {

					if (!invno.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						queryCounter++;
						ps.setString(queryCounter, fdate);
						queryCounter++;
						ps.setString(queryCounter, tdate);
					}
				}

				rs = ps.executeQuery();
				while (rs.next()) {
					String sts = rs.getString("overallstatus");
					String count = rs.getString("count");
					countAsPerStatus.put(sts, count);
					allCounter += Integer.parseInt(count);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				rs.close();
				ps.close();
			}

			String invoice_data = "select count(*) as count from poninvoicesummery where onexstatus = 'H' AND BUSINESSPARTNEROID = ? "
					+ " AND INVOICENUMBER IS NOT NULL and overallstatus is not null";

			ps = con.prepareStatement(invoice_data);
			ps.setString(1, bid);
			rs = ps.executeQuery();
			while (rs.next()) {
				String count = rs.getString("count");
				countAsPerStatus.put("H", count);
			}
			rs.close();
			ps.close();

			if (!countAsPerStatus.isEmpty()) {
				responsejson.put("invoiceCountAsPerStatus", countAsPerStatus);
			}
		} catch (Exception e) {
			log.error("getInvoiceDetailsCountAsPerStatus() : ", e.fillInStackTrace());

		}
		return null;
	}
	/*
	 * public JSONArray downloadInvoceidIdDetails(String bid, String status, String
	 * ponumber, String invoicenumber, String fromdate, String todate, String plant,
	 * String companyCode) throws SQLException {
	 * 
	 * String downloadDataQuery = null; String addQuery = null; String statusQuery =
	 * null; String dateQuery = null; String companyQuery = null; String plantQuery
	 * = null;
	 * 
	 * downloadDataQuery = "SELECT DISTINCT PS.INVOICENUMBER, " +
	 * " TO_CHAR(PS.INVOICEDATE,'DD-MON-RRRR') INVOICEDATE," + " PS.OVERALLSTATUS, "
	 * + " PS.EXPENSESHEETID, " +
	 * " TRIM(TO_CHAR(PS.PAYMENTAMOUNT, '999,999,999,999,999')) AS PAYMENTAMOUNT, "
	 * +
	 * " TRIM(TO_CHAR(DS.LINEITEMTOTALAMOUNT, '999,999,999,999,999')) AS INVOICEAMOUNT, "
	 * + " DS.INVOICESTATUS AS STATUS, " + " PS.VENDORID, " +
	 * " DS.LINEITEMTOTALQUANTITY, " + " PS.PONUMBER, " + " DS.LINEITEMNUMBER, " +
	 * " TO_CHAR(PS.MODIFIEDON,'DD-MON-RRRR') MODIFIEDON, " +
	 * " PS.UTRCHEQUENUMBER UTR, " + " TO_CHAR(PS.UTRDATE, 'DD-MON-RRRR') UTRDATE,"
	 * + " PD.PLANT PLANT " + " FROM PONINVOICESUMMERY PS, " +
	 * " DELIVERYSUMMARY DS, " + " POEVENTDETAILS PD  " + " WHERE " +
	 * " PS.INVOICENUMBER = DS.INVOICENUMBER " + " AND PS.PONUMBER = DS.PONUMBER " +
	 * " AND PD.PONUMBER = PS.PONUMBER " + " AND PD.PONUMBER = DS.PONUMBER " +
	 * " AND PS.PONUMBER = POD.PONUMBER ";
	 * 
	 * // + "AND PS.INVOICENUMBER=? AND PS.PONUMBER=? "
	 * 
	 * if ("AS".equalsIgnoreCase(status)) {
	 * 
	 * if ("H".equalsIgnoreCase(status)) {
	 * 
	 * statusQuery = " AND PS.ONEXSTATUS= 'H' ";
	 * 
	 * } else if ("ALL".equalsIgnoreCase(status)) {
	 * 
	 * statusQuery = " AND PS.OVERALLSTATUS IS NOT NULL ";
	 * 
	 * } else if ("P".equalsIgnoreCase(status)) {
	 * 
	 * statusQuery = " AND (PS.OVERALLSTATUS = 'P' OR PS.OVERALLSTATUS = 'M') ";
	 * 
	 * } else if ("V".equalsIgnoreCase(status)) {
	 * 
	 * statusQuery = " AND (PS.OVERALLSTATUS = 'V' OR PS.OVERALLSTATUS = 'RO') ";
	 * 
	 * } else {
	 * 
	 * statusQuery = " AND PS.OVERALLSTATUS = '" + status + "' ";
	 * 
	 * } downloadDataQuery = downloadDataQuery + statusQuery;
	 * 
	 * }
	 * 
	 * if (!"NA".equalsIgnoreCase(plant)) { plantQuery = " AND PS.PLANT = '" + plant
	 * + "' "; downloadDataQuery = downloadDataQuery + plantQuery; }
	 * 
	 * if (companyCode != null) {
	 * 
	 * companyQuery = " AND POD.COMPANYCODE = ? "; downloadDataQuery =
	 * downloadDataQuery + companyQuery; }
	 * 
	 * if (!"0".equalsIgnoreCase(bid)) { addQuery = " AND PS.BUSINESSPARTNEROID = '"
	 * + bid + "'"; downloadDataQuery = downloadDataQuery + addQuery; }
	 * 
	 * downloadDataQuery = downloadDataQuery + companyQuery;
	 * 
	 * if ((!"NA".equalsIgnoreCase(fromdate) && !"NA".equalsIgnoreCase(todate))) {
	 * 
	 * dateQuery =
	 * " AND PS.INVOICEDATE BETWEEN TO_DATE('01-MAY-2023', 'DD-MON-YYYY') AND TO_DATE('01-OCT-2023', 'DD-MON-YYYY') "
	 * ; downloadDataQuery = downloadDataQuery + dateQuery;
	 * 
	 * }
	 * 
	 * ArrayList<HashMap<String, String>> invoiceList = new
	 * ArrayList<HashMap<String, String>>();
	 * 
	 * List<List<String>> allresult = new ArrayList<List<String>>(); Connection con
	 * = null; PreparedStatement ps = null; ResultSet rs = null; try { con =
	 * DBConnection.getConnection(); // for (int i = 0; i < invoicedata.size(); i++)
	 * { ps = con.prepareStatement(downloadDataQuery); rs = ps.executeQuery();
	 * 
	 * while (rs.next()) { List<String> invoiceData = new ArrayList<String>();
	 * invoiceData.add(rs.getString("InvoiceNumber"));
	 * invoiceData.add(rs.getString("PONumber"));
	 * invoiceData.add(rs.getString("LineItemNumber"));
	 * invoiceData.add(rs.getString("LINEITEMTOTALQUANTITY"));
	 * invoiceData.add(rs.getString("INVOICEAMOUNT"));
	 * invoiceData.add(rs.getString("VendorID"));
	 * invoiceData.add(rs.getString("MODIFIEDON")); if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("A")) {
	 * invoiceData.add("Approved"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("N")) {
	 * invoiceData.add("New"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("O")) {
	 * invoiceData.add("On Hold"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("P")) {
	 * invoiceData.add("Pending"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PRO")) {
	 * invoiceData.add("Processed"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("S")) {
	 * invoiceData.add("Submitted"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PP")) {
	 * invoiceData.add("Partially Paid"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("M")) {
	 * invoiceData.add("Pending"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("R")) {
	 * invoiceData.add("Rejected"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PD")) {
	 * invoiceData.add("Paid"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("V")) {
	 * invoiceData.add("Returned"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("RO")) {
	 * invoiceData.add("Reopened"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("INV")) {
	 * invoiceData.add("Invalid Invoice"); } else { invoiceData.add("-"); }
	 * invoiceData.add(rs.getString("PAYMENTAMOUNT"));
	 * invoiceData.add(rs.getString("EXPENSESHEETID"));
	 * invoiceData.add(rs.getString("UTR")); invoiceData.add(rs.getString("UTRDATE")
	 * == null ? "" : rs.getString("UTRDATE")); String plantCode =
	 * rs.getString("PLANT") == null ? "" : rs.getString("PLANT"); if
	 * ("".equalsIgnoreCase(plantCode)) { invoiceData.add(""); } else { String
	 * plantName = getPlantName(plantCode, con); if (plantName == null ||
	 * plantName.equalsIgnoreCase("null")) { invoiceData.add(""); } else {
	 * invoiceData.add(plantName); } }
	 * 
	 * allresult.add(invoiceData); } rs.close(); ps.close(); // }
	 * 
	 * String encodedfile = writeintoexcelfile(allresult); if
	 * (encodedfile.equalsIgnoreCase("")) { responsejson.put("message", "Fail"); }
	 * else { responsejson.put("message", "Success"); responsejson.put("data",
	 * encodedfile); } jsonArray.add(responsejson);
	 * 
	 * } catch (Exception e) { log.error("downloadInvoceidIdDetails() :",
	 * e.fillInStackTrace());
	 * 
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); } finally {
	 * DBConnection.closeConnection(rs, ps, con); } return jsonArray; }
	 */

	public JSONArray downloadInvoceidIdDetails(String bid, String status, String ponumber, String invoicenumber,
			String fromdate, String todate, String plant, String companyCode) throws SQLException {

		String downloadDataQuery = null;
		String addQuery = null;
		String statusQuery = null;
		String dateQuery = null;
		String companyQuery = null;
		String plantQuery = null;
		String poQuery = null;
		String invoiceQuery = null;

		downloadDataQuery = "SELECT DISTINCT PS.INVOICENUMBER, " + " TO_CHAR(PS.INVOICEDATE,'DD-MON-RRRR') INVOICEDATE,"
				+ " PS.OVERALLSTATUS, " + " PS.EXPENSESHEETID, "
				+ " TRIM(TO_CHAR(PS.PAYMENTAMOUNT, '999,999,999,999,999')) AS PAYMENTAMOUNT, "
				+ " TRIM(TO_CHAR(DS.LINEITEMTOTALAMOUNT, '999,999,999,999,999')) AS INVOICEAMOUNT, "
				+ " DS.INVOICESTATUS AS STATUS, " + " PS.VENDORID, " + " DS.LINEITEMTOTALQUANTITY, " + " PS.PONUMBER, "
				+ " DS.LINEITEMNUMBER, " + " TO_CHAR(PS.MODIFIEDON,'DD-MON-RRRR') MODIFIEDON, "
				+ " PS.UTRCHEQUENUMBER UTR, " + " TO_CHAR(PS.PAYMENTDATE, 'DD-MON-RRRR') PAYMENTDATE," + " PD.PLANT PLANT "
				+ " FROM PONINVOICESUMMERY PS, " + " DELIVERYSUMMARY DS, " + " PODETAILS         POD,"
				+ " POEVENTDETAILS PD  " + " WHERE " + " PS.INVOICENUMBER = DS.INVOICENUMBER "
				+ " AND PS.PONUMBER = DS.PONUMBER " + " AND PD.PONUMBER = PS.PONUMBER "
				+ " AND PD.PONUMBER = DS.PONUMBER " + " AND PS.PONUMBER = POD.PONUMBER ";

		//if (!"AS".equalsIgnoreCase(status)) {

			if ("H".equalsIgnoreCase(status)) {

				statusQuery = " AND PS.ONEXSTATUS= 'H' ";

			} else if ("ALL".equalsIgnoreCase(status)) {

				statusQuery = " AND PS.OVERALLSTATUS IS NOT NULL ";

			} else if ("P".equalsIgnoreCase(status)) {

				statusQuery = " AND (PS.OVERALLSTATUS = 'P' OR PS.OVERALLSTATUS = 'M') ";

			} else if ("V".equalsIgnoreCase(status)) {

				statusQuery = " AND (PS.OVERALLSTATUS = 'V' OR PS.OVERALLSTATUS = 'RO') ";

			} else {

				statusQuery = " AND PS.OVERALLSTATUS = '" + status + "' ";

			}
			downloadDataQuery = downloadDataQuery + statusQuery;

		//}

		if (!"NA".equalsIgnoreCase(plant)) {
			plantQuery = " AND PS.PLANT = '" + plant + "' ";
			downloadDataQuery = downloadDataQuery + plantQuery;
		}

		if (companyCode != null) {
			companyQuery = " AND POD.COMPANYCODE = '" + companyCode + "'";
			downloadDataQuery = downloadDataQuery + companyQuery;
		}

		if (!"0".equalsIgnoreCase(bid)) {
			addQuery = " AND PS.BUSINESSPARTNEROID = '" + bid + "'";
			downloadDataQuery = downloadDataQuery + addQuery;
		}

		if ((!"NA".equalsIgnoreCase(fromdate) && !"NA".equalsIgnoreCase(todate))) {
			dateQuery = " AND PS.INVOICEDATE BETWEEN TO_DATE('" + fromdate + "', 'DD-MON-YYYY') AND TO_DATE('" + todate
					+ "', 'DD-MON-YYYY') ";
			downloadDataQuery = downloadDataQuery + dateQuery;
		}

		if (!"NA".equalsIgnoreCase(ponumber)) {
			poQuery = " AND PS.PONUMBER = '" + ponumber + "'";
			downloadDataQuery = downloadDataQuery + poQuery;
		}

		if (!"NA".equalsIgnoreCase(invoicenumber)) {
			invoiceQuery = " AND PS.INVOICENUMBER = '" + invoicenumber + "'";
			downloadDataQuery = downloadDataQuery + invoiceQuery;
		}
		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();

		List<List<String>> allresult = new ArrayList<List<String>>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			log.info("downloadDataQuery : " + downloadDataQuery);
			con = DBConnection.getConnection();
			ps = con.prepareStatement(downloadDataQuery);
			rs = ps.executeQuery();

			while (rs.next()) {
				List<String> invoiceData = new ArrayList<String>();
				invoiceData.add(rs.getString("INVOICENUMBER"));
				invoiceData.add(rs.getString("PONUMBER"));
				invoiceData.add(rs.getString("LINEITEMNUMBER"));
				invoiceData.add(rs.getString("LINEITEMTOTALQUANTITY"));
				invoiceData.add(rs.getString("INVOICEAMOUNT"));
				invoiceData.add(rs.getString("VENDORID"));
				invoiceData.add(rs.getString("MODIFIEDON"));
				if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("A")) {
					invoiceData.add("Approved");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("N")) {
					invoiceData.add("New");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("O")) {
					invoiceData.add("On Hold");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("P")) {
					invoiceData.add("Pending");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PRO")) {
					invoiceData.add("Processed");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("S")) {
					invoiceData.add("Submitted");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PP")) {
					invoiceData.add("Partially Paid");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("M")) {
					invoiceData.add("Pending");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("R")) {
					invoiceData.add("Rejected");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PD")) {
					invoiceData.add("Paid");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("V")) {
					invoiceData.add("Returned");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("RO")) {
					invoiceData.add("Reopened");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("INV")) {
					invoiceData.add("Invalid Invoice");
				} else {
					invoiceData.add("-");
				}
				invoiceData.add(rs.getString("PAYMENTAMOUNT"));
				invoiceData.add(rs.getString("EXPENSESHEETID"));
				invoiceData.add(rs.getString("UTR"));
				invoiceData.add(rs.getString("PAYMENTDATE") == null ? "" : rs.getString("PAYMENTDATE"));
				String plantCode = rs.getString("PLANT") == null ? "" : rs.getString("PLANT");
				if ("".equalsIgnoreCase(plantCode)) {
					invoiceData.add("");
				} else {
					String plantName = getPlantName(plantCode, con);
					if (plantName == null || plantName.equalsIgnoreCase("null")) {
						invoiceData.add("");
					} else {
						invoiceData.add(plantName);
					}
				}

				allresult.add(invoiceData);
			}
			rs.close();
			ps.close();

			String encodedfile = writeintoexcelfile(allresult);
			if (encodedfile.equalsIgnoreCase("")) {
				responsejson.put("message", "Fail");
			} else {
				responsejson.put("message", "Success");
				responsejson.put("data", encodedfile);
			}
			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.info("downloadInvoceidIdDetails() :" + e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	// invoice data download for csv format
	/*
	 * private String writeintoexcelfile(List<List<String>> totallist) { String
	 * encodedfile = ""; try { ByteArrayOutputStream stream = new
	 * ByteArrayOutputStream(); OutputStreamWriter streamWriter = new
	 * OutputStreamWriter(stream); CSVWriter writer = new CSVWriter(streamWriter);
	 * List<String[]> heading = new ArrayList<String[]>();
	 * 
	 * String header = "INVOICE NUMBER" + "|" + "PONUMBER" + "|" + "LINEITEMNUMBER"
	 * + "|" + "QUANTITY" + "|" + "INVOICEAMOUNT" + "|" + "VENDORID" + "|" +
	 * "CREATEDON" + "|" + "OVERALLSTATUS" + "|" + "PAID AMOUNT" + "|" +
	 * "EXPENSE SHEET ID" + "|" + "UTR NUMBER" + "|" + "UTR DATE" + "|" +
	 * "PLANT NAME";
	 * 
	 * String headerList[] = { header };
	 * 
	 * Iterator<List<String>> i = totallist.iterator(); heading.add(headerList);
	 * while (i.hasNext()) {
	 * 
	 * List<String> templist = (List<String>) i.next(); Iterator<String>
	 * tempIterator = templist.iterator(); String data = ""; while
	 * (tempIterator.hasNext()) { String tempData = (String) tempIterator.next();
	 * String temp = (tempData == null) ? "-" : tempData; data += temp + "|"; } data
	 * = data.substring(0, data.length() - 1); String listData[] = { data };
	 * heading.add(listData); }
	 * 
	 * writer.writeAll(heading); writer.flush(); streamWriter.flush(); byte[]
	 * byteArrayOutputStream = stream.toByteArray();
	 * 
	 * try { encodedfile = new String(Base64.encodeBase64(byteArrayOutputStream),
	 * "UTF-8"); } catch (IOException e) { log.error("writeintoexcelfile() 1: " +
	 * e.fillInStackTrace());
	 * 
	 * }
	 * 
	 * } catch (Exception e) { log.error("writeintoexcelfile() 2: " +
	 * e.fillInStackTrace());
	 * 
	 * } return encodedfile; }
	 * 
	 */
	/*
	 * Invoice tracker for pending enduser ,confirmer and approver
	 */

	public JSONArray getPendingInvoiceTracker(String poNumber, String invoiceNumber, String bid) {

		String invoiceQuery = "Select ip.invoicenumber,ip.enduserstatus,ip.enduseid,ip.status , "
				+ " listagg (ip.EUMANAGER,',\n') within group (order by ip.status) "
				+ " managerId from invoiceapproval ip , businesspartner bp , poninvoicesummery ps "
				+ " where ps.invoicenumber = ip.invoicenumber and ps.ponumber = ip.ponumber and ps.overallstatus in ('P','M','O') and "
				+ " ip.vendorid = bp.VENDORID and ip.invoicenumber = ? and ip.ponumber = ? and bp.businesspartneroid = ? "
				+ " group by ip.invoicenumber,ip.enduserstatus,ip.enduseid,ip.status order by ip.status ";

//HashMap<String, ArrayList<String>> invoiceMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, String> invoiceMap = new HashMap<String, String>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String endUserStatus = null;
		String endUserId = null;
		String managerStatus = null;
		String managerId = null;
		boolean endUserFlag = false;
		boolean managerFlag = false;
		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(invoiceQuery);
			ps.setString(1, invoiceNumber);
			ps.setString(2, poNumber);
			ps.setString(3, bid);

			rs = ps.executeQuery();

			while (rs.next()) {

				endUserId = rs.getString("enduseid") == null ? "NA" : rs.getString("enduseid");
				endUserStatus = rs.getString("enduserstatus") == null ? "NA" : rs.getString("enduserstatus");
				managerId = rs.getString("managerId") == null ? "NA" : rs.getString("managerId");
				managerStatus = rs.getString("status") == null ? "NA" : rs.getString("status");

				if (!"NA".equalsIgnoreCase(endUserStatus) && !"A".equalsIgnoreCase(endUserStatus)) {
					if ("P".equalsIgnoreCase(endUserStatus) || "O".equalsIgnoreCase(endUserStatus)) {
						// invoiceList.add(endUserId);
						invoiceMap.put("EndUser", endUserId);
						endUserFlag = true;
					}
					if (!"NA".equalsIgnoreCase(managerStatus)) {
						if ("CM".equalsIgnoreCase(managerStatus) || "CO".equalsIgnoreCase(managerStatus)) {
							// invoiceList.add(managerId);
							invoiceMap.put("Confirmer", managerId);
							endUserFlag = true;
						}
					}
				} else {

					if ("A".equalsIgnoreCase(endUserStatus)) {
						if (!"NA".equalsIgnoreCase(managerStatus)) {
							if ("CM".equalsIgnoreCase(managerStatus) || "CO".equalsIgnoreCase(managerStatus)) {
								// invoiceList.add(managerId);
								invoiceMap.put("Confirmer", managerId);
								managerFlag = true;
							}
						}
						if (invoiceMap.isEmpty()) {
							if (!"NA".equalsIgnoreCase(managerStatus)) {
								if ("M".equalsIgnoreCase(managerStatus) || "O".equalsIgnoreCase(managerStatus)) {
									// invoiceList.add(managerId);
									invoiceMap.put("Approver", managerId);
									managerFlag = true;
								}
							}
						}
					}
				}
			}

			rs.close();
			ps.close();
			log.info("added list : " + invoiceMap.toString());

			if (endUserFlag) {
				invoiceList.add(invoiceMap);
			}
			if (managerFlag) {
				invoiceList.add(invoiceMap);
			}
			if (invoiceList.size() > 0) {
				responsejson.put("ReturnCode", "200");
				responsejson.put("PendingTracker", invoiceList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("ReturnCode", "101");
				responsejson.put("validation", "Allready Approved/Returned/Resubmitted.");
				jsonArray.add(responsejson);
			}

		} catch (

		Exception e) {
			log.error("getPendingInvoiceTracker() :", e.fillInStackTrace());
			responsejson.put("ReturnCode", "500");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

}