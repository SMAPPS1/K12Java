package com.bccl.dxapi.apiutility;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.bccl.dxapi.apiimpl.InternalportalImpl;
import com.bccl.dxapi.apiimpl.InvoiceDetailsImpl;
import com.bccl.dxapi.apiimpl.POImpl;
import com.bccl.dxapi.security.EmailImpl;
import com.bccl.dxapi.security.GenrateToken;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import au.com.bytecode.opencsv.CSVWriter;

public class SqlConnectionAPI {

	static Connection con = null;
	static Connection con1 = null;
	static JSONObject responsejson = new JSONObject();
	static JSONArray jsonArray = new JSONArray();
	static SqlConnectionAPI sqlCon = new SqlConnectionAPI();
	GenrateToken genratetoken = new GenrateToken();
	// Connection con;
	static PreparedStatement ps;
	static ResultSet rs;
	static ResultSet rs1;
	static ResultSet rs2;

	public static Connection getConnection() throws SQLException {

		InputStream input = SqlConnectionAPI.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();

		String driver = null;
		String conUrl = null;
		String userName = null;
		String password = null;
		try {
			prop.load(input);
			if (con == null) {
				driver = prop.getProperty("connection_driver");
				conUrl = prop.getProperty("connection_url");
				userName = prop.getProperty("userName");
				password = prop.getProperty("pass");
				Class.forName(driver);
				con = DriverManager.getConnection(conUrl, userName, password);
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
		} catch (IOException e) {
			con.close();
			e.printStackTrace();
		}
		return con;

	}
	
	public static JSONArray getRequsitionerPoninvoiceSummery(String emailid, int nPage, String status, String invno,
			String pono, String fdate, String tdate, String plant, String vendor) throws SQLException {

		String subQuery = "";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		try {
				//con = DBConnection.getConnection();
				con = getConnection();
				ArrayList<String> param = new ArrayList<String>();
				Pagination pg = null;
	
				if("ASSQ".equalsIgnoreCase(status)) {
					String shortQuantity = " AND ps.CREDITADVICENO IS NOT NULL ";
					subQuery = subQuery + shortQuantity;
				}else if("P".equalsIgnoreCase(status)) {
					String statusQuery = " AND ( ps.overallstatus = ? OR ps.overallstatus = ? ) ";
					subQuery = subQuery + statusQuery;
					param.add("P");
					param.add("M");
				}if("V".equalsIgnoreCase(status)) {
					String statusQuery = " AND ( ps.overallstatus = ? OR ps.overallstatus = ? ) ";
					subQuery = subQuery + statusQuery;
					param.add("V");
					param.add("RO");
				}else if("A".equalsIgnoreCase(status) || "PRO".equalsIgnoreCase(status) 
						|| "PP".equalsIgnoreCase(status) || "PD".equalsIgnoreCase(status)
						|| "O".equalsIgnoreCase(status)) {
					String statusQuery = " AND ps.overallstatus = ?  ";
					subQuery = subQuery + statusQuery;
					param.add(status);
				}else if("ALL".equalsIgnoreCase(status)) {
					
				}
				if (!"NA".equalsIgnoreCase(plant)) {
					String po = " AND ps.PLANT = ? ";
					subQuery = subQuery + po;
					param.add(plant);
				}
				if (!"NA".equalsIgnoreCase(vendor)) {
					String po = " AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid = ? ) ";
					subQuery = subQuery + po;
					param.add(vendor);
				}
				if (!"NA".equalsIgnoreCase(pono)) {
					String po = " AND ps.PONUMBER = ? ";
					subQuery = subQuery + po;
					param.add(pono);
				}
				if (!"NA".equalsIgnoreCase(invno)) {
					String in = " AND ps.INVOICENUMBER = ? ";
					subQuery = subQuery + in;
					param.add(invno);
				}
				if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate)) 
						&& (!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate))) {
					String dt = " AND ps.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY') ";
					subQuery = subQuery + dt;
					param.add(fdate);
					param.add(tdate);
				}
				if(!"NA".equalsIgnoreCase(emailid)) {
					String emailQuery = " AND (ia.enduseid = ? or ps.requsitioner = ? ) ";
					subQuery = subQuery + emailQuery;
					param.add(emailid);
					param.add(emailid);
				}

					String mainQuery = "SELECT "
							+ "    ps.invoicenumber, "
							+ "    ps.plant, "
							+ "    ps.vendorid, "
							+ "    ps.ponumber, "
							+ "    ps.businesspartneroid, "
							+ "    ps.message, "
							+ "    ps.requsitioner, "
							+ "    ps.buyer, "
							+ "    ps.amount, "
							+ "    ps.createdon, "
							+ "    ps.macount, "
							+ "    ps.holdcount, "
							+ "    ps.overallstatus, "
							+ "    ps.invoicedate, "
							+ "    ps.totalamount, "
							+ "    ps.material_type, "
							+ "    ps.pgq, "
							+ "    ps.onexstatus, "
							+ "    ps.actualfilename, "
							+ "    ps.savedfilename, "
							+ "    ps.paymentamount, "
							+ "    ps.creditnoteno, "
							+ "    ps.creditadviceno, "
							+ "    ps.totalamtinctaxes, "
							+ "    ps.taxamount, "
							+ "    ps.businesspartnertext, "
							+ "    ps.expensesheetid, "
							+ "    ps.mpo, "
							+ "    ps.allpo "
							+ "FROM "
							+ "    poninvoicesummery ps,invoiceapproval ia "
							+ "WHERE "
							+ "    ps.invoicenumber = ia.invoicenumber "
							+ "    AND ps.ponumber = ia.ponumber "
							+ "    AND ps.invoicenumber IS NOT NULL "
							+ "    AND ps.mpo IS NULL "
							+ subQuery
							+ " UNION "
							+ " SELECT "
							+ "    ps.invoicenumber, "
							+ "    ps.plant, "
							+ "    ps.vendorid, "
							+ "    ia.ponumber, "
							+ "    ps.businesspartneroid, "
							+ "    ps.message, "
							+ "    ps.requsitioner, "
							+ "    ps.buyer, "
							+ "    ps.amount, "
							+ "    ps.createdon, "
							+ "    ps.macount, "
							+ "    ps.holdcount, "
							+ "    ps.overallstatus, "
							+ "    ps.invoicedate, "
							+ "    ps.totalamount, "
							+ "    ps.material_type, "
							+ "    ps.pgq, "
							+ "    ps.onexstatus, "
							+ "    ps.actualfilename, "
							+ "    ps.savedfilename, "
							+ "    ps.paymentamount, "
							+ "    ps.creditnoteno, "
							+ "    ps.creditadviceno, "
							+ "    ps.totalamtinctaxes, "
							+ "    ps.taxamount, "
							+ "    ps.businesspartnertext, "
							+ "    ps.expensesheetid, "
							+ "    ps.mpo, "
							+ "    ps.allpo "
							+ "FROM "
							+ "         poninvoicesummery ps, invoiceapproval ia "
							+ "WHERE "
							+ "    ps.invoicenumber = ia.invoicenumber "
							+ "    AND ps.ponumber = ia.ponumber "
							+ "    AND ps.invoicenumber IS NOT NULL "
							+ "    AND ps.allpo IS NOT NULL "
							+ "    AND ps.mpo = 'Y' "
							+ subQuery
							+ "ORDER BY "
							+ "    createdon DESC";
	
				pg = new Pagination(mainQuery, nPage);

				System.out.println("mainQuery : "+ mainQuery);
				
				if("P".equalsIgnoreCase(status)) {
					param.add("P");
					param.add("M");
				}if("V".equalsIgnoreCase(status)) {
					param.add("V");
					param.add("RO");
				}else if("A".equalsIgnoreCase(status) || "PRO".equalsIgnoreCase(status) 
						|| "PP".equalsIgnoreCase(status) || "PD".equalsIgnoreCase(status)
						|| "O".equalsIgnoreCase(status)) {
					param.add(status);
				}else if("ALL".equalsIgnoreCase(status)) {
					
				}
				if (!"NA".equalsIgnoreCase(plant)) {
					param.add(plant);
				}
				if (!"NA".equalsIgnoreCase(vendor)) {
					param.add(vendor);
				}
				if (!"NA".equalsIgnoreCase(pono)) {
					param.add(pono);
				}
				if (!"NA".equalsIgnoreCase(invno)) {
					param.add(invno);
				}
				if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate)) 
						&& (!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate))) {
					param.add(fdate);
					param.add(tdate);
				}
				if(!"NA".equalsIgnoreCase(emailid)) {
					param.add(emailid);
					param.add(emailid);
				}
							
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);
				
				while (rs.next()) {
					
					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("PONUMBER", rs.getString("PONUMBER"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poData.put("MESSAGE", rs.getString("MESSAGE"));
					poData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
					poData.put("BUYER", rs.getString("BUYER"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poData.put("AMOUNT", rs.getString("AMOUNT"));
					poData.put("MACOUNT", rs.getString("MACOUNT"));
					poData.put("HOLDCOUNT", rs.getString("HOLDCOUNT"));
					poData.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					poData.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
					poData.put("MATERIAL_TYPE", rs.getString("MATERIAL_TYPE"));
					poData.put("PGQ", rs.getString("PGQ"));
					poData.put("ONEXSTATUS", rs.getString("ONEXSTATUS"));
					poData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					poData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					poData.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
					poData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					poData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					poData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					poData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					poData.put("EXPENSESHEETID",rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					poData.put("MPO", rs.getString("MPO"));
					poData.put("ALLPO", rs.getString("ALLPO"));
					invoiceList.add(poData);
				}
				rs.close();
				pg.close();
				pg = null;
				
			try {
				//getInternalPonInvoiceSummeryCountsAsPerStatus(emailid, nPage, status, invno, pono, fdate, tdate, plant, vendor, "enduser", con, ps, rs);
			} catch (Exception e) {
				System.out.println("getRequsitionerPoninvoiceSummery() 1 :"+ e.fillInStackTrace());
			}

		} catch (Exception e) {
			System.out.println("getRequsitionerPoninvoiceSummery() 2 :"+ e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (invoiceList.size() > 0) {
			responsejson.put("message", "Success");
			responsejson.put("invoiceData", invoiceList);
			responsejson.put("invoicelistpages", pages);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
		}

		jsonArray.add(responsejson);

		return jsonArray;
	}


	
	public static void main(String args[]) throws SQLException {

		try {
			JSONArray JSONArray  =  getRequsitionerPoninvoiceSummery("sachin.mehta@timesgroup.com", 1, "P", "NA",
					"NA", "NA", "NA", "NA", "NA");
			System.out.println("jsonArray :" + jsonArray.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
