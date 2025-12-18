package com.bccl.dxapi.apiimpl;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.bccl.dxapi.apiutility.DXPortalException;
import com.bccl.dxapi.apiutility.Pagination;
import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.apiutility.Validation;
import com.bccl.dxapi.bean.AdvanceShippingNotification;
import com.bccl.dxapi.bean.Bulkfailbean;
import com.bccl.dxapi.bean.Invoicesubmission;
import com.bccl.dxapi.bean.PoeventdetailsPojo;
import com.bccl.dxapi.bean.SimpoDeliveryItems;
import com.bccl.dxapi.security.EmailImpl;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.bean.ColumnPositionMappingStrategy;
import au.com.bytecode.opencsv.bean.CsvToBean;

public class POImpl {

	static Logger log = Logger.getLogger(POImpl.class.getName());

	public POImpl() {
		responsejson = new JSONObject();
		jsonArray = new JSONArray();
	}

	@Override
	protected void finalize() throws Throwable {
		responsejson = null;
		jsonArray = null;
		super.finalize();
	}

	static HashMap<String, Object> poMap = new HashMap<String, Object>();

	JSONObject responsejson = null;
	JSONArray jsonArray = null;

	private String[] fullName;
	private String skype;
	private String invoicenumber;
	private String message;
	private String ponumber;

	public synchronized static Object getPOObj(String poNumber) {

		if (poMap.get(poNumber) == null) {
			poMap.put(poNumber, new Object());
		}
		return poMap.get(poNumber);
	}

	public void executeUpdateBalance(Connection con, List<Invoicesubmission> persons) throws DXPortalException {

		Object po = POImpl.getPOObj(persons.get(0).getPo_num());
		if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
			updateBalance(con, persons, po);
		}

	}

	public void updateBalance(Connection con, List<Invoicesubmission> persons, Object po) throws DXPortalException {

		double balanceQty = 0.0;
		synchronized (po) {

			PreparedStatement ps = null;
			ResultSet rs = null;
			for (int i = 0; i < persons.size(); i++) {

				try {
					double newbalance = 0.0;
					balanceQty = 0.0;
					balanceQty = getBalanceCount(persons.get(i).getPo_num(), persons.get(i).getLineItemNumber(),
							persons.get(i).getBid(), con);

					if (balanceQty < Double.parseDouble(persons.get(i).getQuantity())) {
						throw new DXPortalException("Error in Invoice Submission !!", "Insufficient balance");
					}

					String po_status = "UPDATE poeventdetails set balance_qty=? where BusinessPartnerOID =? "
							+ "and PONumber= ? and LineItemNumber= ? and OrderNumber is null";

					newbalance = balanceQty - Double.parseDouble(persons.get(i).getQuantity());

					ps = con.prepareStatement(po_status);
					ps.setString(1, String.valueOf(newbalance));
					ps.setString(2, persons.get(i).getBid());
					ps.setString(3, persons.get(i).getPo_num());
					ps.setString(4, persons.get(i).getLineItemNumber());
					ps.executeUpdate();
					ps.close();
				} catch (Exception e) {
					log.error("updateBalance() :", e.fillInStackTrace());
					throw new DXPortalException("Error in invoice submission !!", "SQL Error during updating balance.");
				}
			}
		}

	}

	public JSONArray getPODetails(String bid, String status, int nPage, String ponumber, String fromdateofduration,
			String todateofduration, String fromdateofpo, String todateofpo, String plant, String companyCode)
			throws SQLException {

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

		result = Validation.StringChecknull(companyCode);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		try {
			con = DBConnection.getConnection();

			String po_query = "Select * from CHATMESSAGE where CreatedOn in(select max(CreatedOn) from CHATMESSAGE where PONUMBER=?)"
					+ " and  status =? and businesspartneroid=?";
			String po_Number = "Select PONUMBER from CHATMESSAGE where businesspartneroid=? and status =? and INVOICENUMBER is null ";
			String uniquePoInCount = "Select count(*) as counter from DELIVERYSUMMARY where INVOICENUMBER = ?"
					+ " and PONUMBER = ? ";

			ArrayList<String> topic = new ArrayList<String>();
			HashMap<String, String> QueryList = new HashMap<String, String>();
			ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();

			String queryLists[] = { "payment amount is incorrect", "payment amount is incorrect",
					"payment amount is incorrect", "payment amount is incorrect" };

			try {
				ps = con.prepareStatement(po_Number);
				ps.setString(1, bid);
				ps.setString(2, "A");
				rs = ps.executeQuery();
				while (rs.next()) {
					topic.add(rs.getString("PONUMBER"));
				}
				rs.close();
				ps.close();
			} catch (Exception e) {
				topic.add("0");
				log.error("getPODetails() 1 :", e.fillInStackTrace());
			}

			for (int i = 0; i < topic.size(); i++) {
				try {
					ps = con.prepareStatement(po_query);
					ps.setString(1, topic.get(i));
					ps.setString(2, "A");
					ps.setString(3, bid);
					rs = ps.executeQuery();
					while (rs.next()) {
						QueryList.put(rs.getString("PONUMBER"), rs.getString("MessageText"));
					}
					rs.close();
					ps.close();
				} catch (Exception e) {
					log.error("getPODetails() 2 :", e.fillInStackTrace());
					QueryList.put("PONUMBER", "No data Found");
				}
			}

			String compCodeQuery = " AND pd.companycode = ? ";

			if (!status.equalsIgnoreCase("AS")) {

				String po_data = null;
				if (status.equalsIgnoreCase("SP")) {
					po_data = "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd.VENDORID,"
							+ "pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,"
							+ "pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,"
							+ "pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE,pd.QUOTATIONNO,"
							+ "pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd. POTYPE,poe.PLANT, Case When (select count(*) from poninvoicesummery where poninvoicesummery.ponumber = pd.ponumber "
							+ " and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID and invoicenumber is not null) > 0 then 'True' "
							+ "When (select count(*) from poninvoicesummery where poninvoicesummery.ponumber = pd.ponumber and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID "
							+ " and invoicenumber is not null ) = 0 then 'False' END AS ICount, ASNSTATUS "
							+ "from podetails pd join poeventdetails poe on pd.PONUMBER = poe.PONUMBER "
							+ "where pd.BusinessPartnerOID= ? and pd.POTYPE = ? " + compCodeQuery + " ORDER BY PONUMBER desc,CREATEDON desc ";
				} else if (status.equalsIgnoreCase("ALL")) {

					po_data = "select * from ("
							+ "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID, "
							+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT, "
							+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1, "
							+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY, "
							+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP, "
							+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT, Case When (select count(*) from poninvoicesummery "
							+ "where poninvoicesummery.ponumber = pd.ponumber and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID and invoicenumber is not null) "
							+ " > 0 then 'True' "
							+ "When (select count(*) from poninvoicesummery where poninvoicesummery.ponumber = pd.ponumber and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID "
							+ "and invoicenumber is not null) = 0 then 'False' END AS ICount, ASNSTATUS "
							+ "from podetails pd join poeventdetails poe "
							+ " on pd.PONUMBER = poe.PONUMBER where pd.BusinessPartnerOID=? " + "and pd.Status=? "
							+ compCodeQuery + "Union "
							+ "select DISTINCT  pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID, "
							+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT, "
							+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1, "
							+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER, "
							+ "pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP, "
							+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT, Case When (select count(*) from poninvoicesummery "
							+ "where poninvoicesummery.ponumber = pd.ponumber and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID and invoicenumber is not null) > 0 then 'True' "
							+ "When (select count(*) from poninvoicesummery where poninvoicesummery.ponumber = pd.ponumber and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID "
							+ " and invoicenumber is not null) = 0 then 'False' END AS ICount, ASNSTATUS "
							+ " from podetails pd join poeventdetails poe "
							+ " on pd.PONUMBER = poe.PONUMBER where pd.BusinessPartnerOID=? and pd.Status <> ?"
							+ compCodeQuery + " ) order by " + " PONUMBER desc,CREATEDON desc ";
				} else {
					po_data = "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT"
							+ ",pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,"
							+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,"
							+ "pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,"
							+ "pd.PURCHASINGGROUP,pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT, Case When (select count(*) from poninvoicesummery "
							+ "where poninvoicesummery.ponumber = pd.ponumber and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID and invoicenumber is not null) > 0 then 'True' "
							+ "When (select count(*) from poninvoicesummery where poninvoicesummery.ponumber = pd.ponumber and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID "
							+ "and invoicenumber is not null) = 0 then 'False' END AS ICount , ASNSTATUS "
							+ "from podetails pd join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where "
							+ "pd.BusinessPartnerOID=? and pd.Status = ?" + compCodeQuery  + " ORDER BY PONUMBER desc,CREATEDON desc ";
				}

				ArrayList<String> param = new ArrayList<String>();

				if (status.equalsIgnoreCase("ALL")) {
					param.add(bid);
					param.add("N");
					param.add(companyCode);

					param.add(bid);
					param.add("N");
					param.add(companyCode);

				} else if ("SP".equalsIgnoreCase(status)) {

					param.add(bid);
					param.add("S");
					param.add(companyCode);

				} else {
					param.add(bid);
					param.add(status);
					param.add(companyCode);
				}
				log.info("po_data : " + po_data);
				Pagination pg = new Pagination(po_data, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				while (rs.next()) {

					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PO_NUMBER", rs.getString("PONumber"));
					poData.put("DATE", rs.getString("PODate"));
					poData.put("AMOUNT", rs.getString("POAmount"));
					poData.put("STATUS", rs.getString("Status"));
					poData.put("Quantity", rs.getString("Quantity"));
					poData.put("COMPANY", rs.getString("Company"));
					poData.put("PLANT", rs.getString("Plant"));
					poData.put("PLANTNAME", getPlantName(rs.getString("Plant"), con));
					poData.put("DEPARTMENT", rs.getString("Department"));
					poData.put("COSTCENTRE", rs.getString("CostCentre"));
					poData.put("CATEGORY", rs.getString("Category"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
					poData.put("VENDORID", rs.getString("VendorID"));
					poData.put("CREATEDDATE", rs.getString("CreatedDate"));
					poData.put("STARTDATE", rs.getString("StartDate"));
					poData.put("ENDDATE", rs.getString("EndDate"));
					poData.put("LINEITEMNUMBER", rs.getString("LineItemNumber"));
					poData.put("LINEITEMTEXT", rs.getString("LineItemText"));
					poData.put("UNITOFMEASURE", rs.getString("UnitOfMeasure"));
					poData.put("IGSTAMOUNT", rs.getString("iGSTAmount"));
					poData.put("CGSTAMOUNT", rs.getString("cGSTAmount"));
					poData.put("SGSTAMOUNT", rs.getString("sgstAmount"));
					poData.put("CONTACTPERSONEMAILID", rs.getString("ContactPersonEmailID"));
					poData.put("CONTACTPERSONPHONE", rs.getString("ContactPersonPhone"));
					poData.put("DELIVERYADDRESS1", rs.getString("DeliveryAddress1"));
					poData.put("DELIVERYADDRESS2", rs.getString("DeliveryAddress2"));
					poData.put("DELIVERYADDRESS3", rs.getString("DeliveryAddress3"));
					poData.put("CITY", rs.getString("City"));
					poData.put("STATE", rs.getString("State"));
					poData.put("COUNTRY", rs.getString("Country"));
					poData.put("PINCODE", rs.getString("PinCode"));
					poData.put("BUYER", rs.getString("Buyer"));
					poData.put("REQUSITIONER", rs.getString("Requsitioner"));
					poData.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
					poData.put("POTYPE", rs.getString("POTYPE"));
					poData.put("ICOUNT", rs.getString("ICOUNT"));
					poData.put("ASNSTATUS", rs.getString("ASNSTATUS"));

					for (Entry<String, String> entry : QueryList.entrySet()) {
						if (entry.getKey().equals(rs.getString("PONumber"))) {
							poData.put("QUERY", entry.getValue());
						}
					}
					POList.add(poData);
				}
				pg.close();
				rs.close();
				pg = null;
			} else {
				String subquery = "";

				ArrayList<String> param = new ArrayList<String>();
				param.add(bid);
				param.add("N");
				param.add(companyCode);

				if (!plant.equalsIgnoreCase("NA")) {
					String po = " AND poe.PLANT=?";
					subquery = subquery + po;
					param.add(plant);

				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND pd.PONUMBER=?";
					subquery = subquery + po;
					param.add(ponumber);

				}
				if ((!fromdateofduration.equalsIgnoreCase("NA"))
						&& (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
					String in = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + " AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + in;
					param.add(fromdateofduration);
					param.add(todateofduration);
				}
				if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
					String dt = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + "AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
					param.add(fromdateofpo);
					param.add(todateofpo);
				}
				param.add(bid);
				param.add("N");
				param.add(companyCode);

				if (!plant.equalsIgnoreCase("NA")) {
					param.add(plant);
				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					param.add(ponumber);
				}
				if ((!fromdateofduration.equalsIgnoreCase("NA"))
						&& (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
					param.add(fromdateofduration);
					param.add(todateofduration);
				}
				if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
					param.add(fromdateofpo);
					param.add(todateofpo);
				}

				String po_data = "select * from (select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,"
						+ "pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,"
						+ "pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,"
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,"
						+ "pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,"
						+ "pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,"
						+ "pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,"
						+ "pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,"
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
						+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,"
						+ "pd. POTYPE,poe.PLANT, Case When (select count(*) from poninvoicesummery "
						+ "where poninvoicesummery.ponumber = pd.ponumber and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID "
						+ "and invoicenumber is not null) > 0 then 'True' "
						+ "When (select count(*) from poninvoicesummery where poninvoicesummery.ponumber = pd.ponumber "
						+ "and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID and invoicenumber is not null) = 0 then 'False' END AS ICount, ASNSTATUS "
						+ "from podetails pd join poeventdetails poe on "
						+ "pd.PONUMBER = poe.PONUMBER where pd.BusinessPartnerOID=? and pd.Status=? " + compCodeQuery
						+ subquery + " " + "Union "
						+ "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,"
						+ "pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd.VENDORID,"
						+ "pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,"
						+ "pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,"
						+ "pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,"
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,"
						+ "pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
						+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,"
						+ "pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE,"
						+ "pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT, Case When "
						+ "(select count(*) from poninvoicesummery where poninvoicesummery.ponumber = pd.ponumber "
						+ "and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID and invoicenumber is not null) > 0 then 'True' "
						+ "When (select count(*) from poninvoicesummery where poninvoicesummery.ponumber = pd.ponumber "
						+ "and poninvoicesummery.businesspartneroid=pd.BusinessPartnerOID and invoicenumber is not null) = 0 then 'False' END AS ICount, ASNSTATUS "
						+ "from podetails pd join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where "
						+ "pd.BusinessPartnerOID=? and pd.Status<>? " + compCodeQuery + subquery
						+ ") order by PONUMBER desc,CREATEDON desc ";

				log.info("po_data : " + po_data);

				Pagination pg = new Pagination(po_data, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				while (rs.next()) {

					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PO_NUMBER", rs.getString("PONumber"));
					poData.put("DATE", rs.getString("PODate"));
					poData.put("AMOUNT", rs.getString("POAmount"));
					poData.put("STATUS", rs.getString("Status"));
					poData.put("Quantity", rs.getString("Quantity"));
					poData.put("COMPANY", rs.getString("Company"));
					poData.put("PLANT", rs.getString("Plant"));
					poData.put("PLANTNAME", getPlantName(rs.getString("Plant"), con));
					poData.put("DEPARTMENT", rs.getString("Department"));
					poData.put("COSTCENTRE", rs.getString("CostCentre"));
					poData.put("CATEGORY", rs.getString("Category"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
					poData.put("VENDORID", rs.getString("VendorID"));
					poData.put("CREATEDDATE", rs.getString("CreatedDate"));
					poData.put("STARTDATE", rs.getString("StartDate"));
					poData.put("ENDDATE", rs.getString("EndDate"));
					poData.put("LINEITEMNUMBER", rs.getString("LineItemNumber"));
					poData.put("LINEITEMTEXT", rs.getString("LineItemText"));
					poData.put("UNITOFMEASURE", rs.getString("UnitOfMeasure"));
					poData.put("IGSTAMOUNT", rs.getString("iGSTAmount"));
					poData.put("CGSTAMOUNT", rs.getString("cGSTAmount"));
					poData.put("SGSTAMOUNT", rs.getString("sgstAmount"));
					poData.put("CONTACTPERSONEMAILID", rs.getString("ContactPersonEmailID"));
					poData.put("CONTACTPERSONPHONE", rs.getString("ContactPersonPhone"));
					poData.put("DELIVERYADDRESS1", rs.getString("DeliveryAddress1"));
					poData.put("DELIVERYADDRESS2", rs.getString("DeliveryAddress2"));
					poData.put("DELIVERYADDRESS3", rs.getString("DeliveryAddress3"));
					poData.put("CITY", rs.getString("City"));
					poData.put("STATE", rs.getString("State"));
					poData.put("COUNTRY", rs.getString("Country"));
					poData.put("PINCODE", rs.getString("PinCode"));
					poData.put("BUYER", rs.getString("Buyer"));
					poData.put("REQUSITIONER", rs.getString("Requsitioner"));
					poData.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
					poData.put("POTYPE", rs.getString("POTYPE"));
					poData.put("ICOUNT", rs.getString("ICOUNT"));
					poData.put("ASNSTATUS", rs.getString("ASNSTATUS"));
					for (Entry<String, String> entry : QueryList.entrySet()) {
						if (entry.getKey().equals(rs.getString("PONumber"))) {
							poData.put("QUERY", entry.getValue());
						}
					}
					POList.add(poData);
				}
				pg.close();
				rs.close();
				pg = null;
			}
			if (POList.size() > 0) {
				responsejson.put("poData", POList);
				responsejson.put("popages", pages);

				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}
			try {
				getPODetailsCountAsPerStatus(bid, status, nPage, ponumber, fromdateofduration, todateofduration,
						fromdateofpo, todateofpo, plant, con, ps, rs, companyCode);
			} catch (Exception e) {
				log.error("getPODetails() 3 : ", e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("getPODetails() 4 : ", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPODetailswithoutpo(String id, int pageno, String status, String ponumber,
			String fromdateofduration, String todateofduration, String fromdateofpo, String todateofpo,
			String companyCode) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(id);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(id);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(companyCode);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		try {
			con = DBConnection.getConnection();
			HashMap<String, String> QueryList = new HashMap<String, String>();
			ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
			ArrayList<String> topic = new ArrayList<String>();
			String po_query = "Select * from CHATMESSAGE where CreatedOn in(select max(CreatedOn) from CHATMESSAGE where PONUMBER=?)"
					+ " and  status =? and businesspartneroid=?";
			String po_Number = "Select PONUMBER from CHATMESSAGE where businesspartneroid=? and status =? and INVOICENUMBER is null ";

			String queryLists[] = { "payment amount is incorrect", "payment amount is incorrect",
					"payment amount is incorrect", "payment amount is incorrect" };

			try {
				ps = con.prepareStatement(po_Number);
				ps.setString(1, id);
				ps.setString(2, "A");
				rs = ps.executeQuery();
				while (rs.next()) {
					topic.add(rs.getString("PONUMBER"));
				}
				rs.close();
				ps.close();
			} catch (Exception e) {
				topic.add("0");
				log.error("getPODetailswithoutpo() 1 : ", e.fillInStackTrace());
			}

			for (int i = 0; i < topic.size(); i++) {
				try {
					ps = con.prepareStatement(po_query);
					ps.setString(1, topic.get(i));
					ps.setString(2, "A");
					ps.setString(3, id);
					rs = ps.executeQuery();
					while (rs.next()) {
						QueryList.put(rs.getString("PONUMBER"), rs.getString("MessageText"));
					}
					rs.close();
					ps.close();
				} catch (Exception e) {
					QueryList.put("PONUMBER", "No data Found");
					log.error("getPODetailswithoutpo() 2 : ", e.fillInStackTrace());
				}
			}

			String compCodeQuery = " AND companycode = ? ";

			if (!status.equalsIgnoreCase("AS")) {
				String po_data = "";

				if (status.equalsIgnoreCase("ALL")) {
					po_data = "SELECT * FROM podetails a WHERE a.BUSINESSPARTNEROID=? AND STATUS <> ? " + compCodeQuery
							+ "AND NOT EXISTS (SELECT e.PONUMBER FROM PONINVOICESUMMERY e "
							+ "WHERE e.PONUMBER = a.PONUMBER and e.INVOICENUMBER IS NOT NULL)";

				} else if (status.equalsIgnoreCase("S")) {
					po_data = "SELECT * FROM podetails a WHERE a.BUSINESSPARTNEROID=? AND STATUS = ? AND" + " POTYPE=?"
							+ compCodeQuery + " AND NOT EXISTS (SELECT e.PONUMBER FROM PONINVOICESUMMERY e "
							+ "WHERE e.PONUMBER = a.PONUMBER and e.INVOICENUMBER IS NOT NULL)";
				} else {
					po_data = "SELECT * FROM podetails a WHERE a.BUSINESSPARTNEROID=? AND STATUS = ? " + compCodeQuery
							+ " AND NOT EXISTS (SELECT e.PONUMBER FROM PONINVOICESUMMERY e "
							+ "WHERE e.PONUMBER = a.PONUMBER and e.INVOICENUMBER IS NOT NULL)";
				}

				ArrayList<String> param = new ArrayList<String>();

				if (status.equalsIgnoreCase("ALL")) {
					param.add(id);
					param.add("N");
					param.add(companyCode);

				} else if (status.equalsIgnoreCase("S")) {
					param.add(id);
					param.add("N");
					param.add("S");
					param.add(companyCode);
				} else {
					param.add(id);
					param.add(status);
					param.add(companyCode);
				}

				Pagination pg = new Pagination(po_data, pageno);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				while (rs.next()) {

					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PO_NUMBER", rs.getString("PONumber"));
					poData.put("DATE", rs.getString("PODate"));
					poData.put("AMOUNT", rs.getString("POAmount"));
					poData.put("STATUS", rs.getString("Status"));
					poData.put("Quantity", rs.getString("Quantity"));
					poData.put("COMPANY", rs.getString("Company"));
					poData.put("PLANT", rs.getString("Plant"));
					poData.put("PLANTNAME", getPlantName(rs.getString("Plant"), con));
					poData.put("DEPARTMENT", rs.getString("Department"));
					poData.put("COSTCENTRE", rs.getString("CostCentre"));
					poData.put("CATEGORY", rs.getString("Category"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
					poData.put("VENDORID", rs.getString("VendorID"));
					poData.put("CREATEDDATE", rs.getString("CreatedDate"));
					poData.put("STARTDATE", rs.getString("StartDate"));
					poData.put("ENDDATE", rs.getString("EndDate"));
					poData.put("LINEITEMNUMBER", rs.getString("LineItemNumber"));
					poData.put("LINEITEMTEXT", rs.getString("LineItemText"));
					poData.put("UNITOFMEASURE", rs.getString("UnitOfMeasure"));
					poData.put("IGSTAMOUNT", rs.getString("iGSTAmount"));
					poData.put("CGSTAMOUNT", rs.getString("cGSTAmount"));
					poData.put("SGSTAMOUNT", rs.getString("sgstAmount"));
					poData.put("CONTACTPERSONEMAILID", rs.getString("ContactPersonEmailID"));
					poData.put("CONTACTPERSONPHONE", rs.getString("ContactPersonPhone"));
					poData.put("DELIVERYADDRESS1", rs.getString("DeliveryAddress1"));
					poData.put("DELIVERYADDRESS2", rs.getString("DeliveryAddress2"));
					poData.put("DELIVERYADDRESS3", rs.getString("DeliveryAddress3"));
					poData.put("CITY", rs.getString("City"));
					poData.put("STATE", rs.getString("State"));
					poData.put("COUNTRY", rs.getString("Country"));
					poData.put("PINCODE", rs.getString("PinCode"));
					poData.put("BUYER", rs.getString("Buyer"));
					poData.put("REQUSITIONER", rs.getString("Requsitioner"));
					poData.put("MATERIAL", rs.getString("MATERIAL_TYPE"));

					for (Entry<String, String> entry : QueryList.entrySet()) {
						if (entry.getKey().equals(rs.getString("PONumber"))) {
							poData.put("QUERY", entry.getValue());
						}
					}
					POList.add(poData);
				}
				rs.close();
				ps.close();

			} else {

				String subquery = "";
				String po_data = "";
				ArrayList<String> param = new ArrayList<String>();

				if (status.equalsIgnoreCase("ALL")) {
					param.add(id);
					param.add("N");
					param.add(companyCode);

				} else if (status.equalsIgnoreCase("S")) {
					param.add(id);
					param.add("N");
					param.add("S");
					param.add(companyCode);
				} else {
					param.add(id);
					param.add(status);
					param.add(companyCode);
				}

				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND PONUMBER=?";
					subquery = subquery + po;
					param.add(ponumber);

				}
				if ((!fromdateofduration.equalsIgnoreCase("NA"))
						&& (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
					String in = " AND PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + " AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + in;
					param.add(fromdateofduration);
					param.add(todateofduration);
				}
				if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
					String dt = " AND PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + "AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
					param.add(fromdateofpo);
					param.add(todateofpo);
				}

				po_data = "SELECT * FROM podetails a WHERE a.BUSINESSPARTNEROID=? AND STATUS <> ? " + compCodeQuery
						+ subquery + " " + "AND NOT EXISTS (SELECT e.PONUMBER FROM PONINVOICESUMMERY e "
						+ "WHERE e.PONUMBER = a.PONUMBER and e.INVOICENUMBER IS NOT NULL)";

				Pagination pg = new Pagination(po_data, pageno);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				while (rs.next()) {

					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PO_NUMBER", rs.getString("PONumber"));
					poData.put("DATE", rs.getString("PODate"));
					poData.put("AMOUNT", rs.getString("POAmount"));
					poData.put("STATUS", rs.getString("Status"));
					poData.put("Quantity", rs.getString("Quantity"));
					poData.put("COMPANY", rs.getString("Company"));
					poData.put("PLANT", rs.getString("Plant"));
					poData.put("PLANTNAME", getPlantName(rs.getString("Plant"), con));
					poData.put("DEPARTMENT", rs.getString("Department"));
					poData.put("COSTCENTRE", rs.getString("CostCentre"));
					poData.put("CATEGORY", rs.getString("Category"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
					poData.put("VENDORID", rs.getString("VendorID"));
					poData.put("CREATEDDATE", rs.getString("CreatedDate"));
					poData.put("STARTDATE", rs.getString("StartDate"));
					poData.put("ENDDATE", rs.getString("EndDate"));
					poData.put("LINEITEMNUMBER", rs.getString("LineItemNumber"));
					poData.put("LINEITEMTEXT", rs.getString("LineItemText"));
					poData.put("UNITOFMEASURE", rs.getString("UnitOfMeasure"));
					poData.put("IGSTAMOUNT", rs.getString("iGSTAmount"));
					poData.put("CGSTAMOUNT", rs.getString("cGSTAmount"));
					poData.put("SGSTAMOUNT", rs.getString("sgstAmount"));
					poData.put("CONTACTPERSONEMAILID", rs.getString("ContactPersonEmailID"));
					poData.put("CONTACTPERSONPHONE", rs.getString("ContactPersonPhone"));
					poData.put("DELIVERYADDRESS1", rs.getString("DeliveryAddress1"));
					poData.put("DELIVERYADDRESS2", rs.getString("DeliveryAddress2"));
					poData.put("DELIVERYADDRESS3", rs.getString("DeliveryAddress3"));
					poData.put("CITY", rs.getString("City"));
					poData.put("STATE", rs.getString("State"));
					poData.put("COUNTRY", rs.getString("Country"));
					poData.put("PINCODE", rs.getString("PinCode"));
					poData.put("BUYER", rs.getString("Buyer"));
					poData.put("REQUSITIONER", rs.getString("Requsitioner"));
					poData.put("MATERIAL", rs.getString("MATERIAL_TYPE"));

					for (Entry<String, String> entry : QueryList.entrySet()) {
						if (entry.getKey().equals(rs.getString("PONumber"))) {
							poData.put("QUERY", entry.getValue());
						}
					}
					POList.add(poData);
				}

				rs.close();
				pg.close();
				pg = null;

			}
			if (POList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("poData", POList);
				responsejson.put("popages", pages);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Fail");
				responsejson.put("message", "No Data Found for given Id");
				jsonArray.add(responsejson);
			}
		} catch (SQLException e) {
			log.error("getPODetailswithoutpo() 3 : ", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPOStatus(String id, String select, String remark, String po_num) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			int value = 0;

			boolean result;
			result = Validation.StringChecknull(id);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			} else {
				result = Validation.numberCheck(id);
				if (result == false) {
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}
			}
			result = Validation.StringChecknull(select);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(remark);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(po_num);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			if (select.equalsIgnoreCase("N")) {

				if (remark.isEmpty()) {
					responsejson.put("remark", "No remarks found");
				} else {
					String po_status = "UPDATE po_details set QUERY=? where PO_NUMBER=?";

					ps = con.prepareStatement(po_status);
					ps.setString(1, remark);
					ps.setString(2, po_num);
					value = ps.executeUpdate();
					ps.close();
				}
			} else {

				String po_status = "UPDATE po_details set STATUS=? where PO_NUMBER=?";
				ps = con.prepareStatement(po_status);
				ps.setString(1, "ACCEPTED");
				ps.setString(2, po_num);
				value = ps.executeUpdate();
				ps.close();
			}
			if (value > 0) {
				responsejson.put("message", "Success");
			} else {
				responsejson.put("message", "Fail");
			}
			jsonArray.add(responsejson);
			con.commit();

		} catch (SQLException e) {
			log.error("getPOStatus() :", e.fillInStackTrace());
			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPOEvent(String id, String po_num) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(id);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(id);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			String sql = "select LineItemText, sum(Quantity)as Quantity from poeventdetails where PONumber=? GROUP BY LineItemText";
			String displaySql = "select OrderNumber, LineItemText , DELIVERYDATE, Quantity, Remark from poeventdetails where PONumber=?";

			ArrayList<HashMap<String, String>> POEvent = new ArrayList<HashMap<String, String>>();
			ArrayList<HashMap<String, String>> POListEvent = new ArrayList<HashMap<String, String>>();
			ps = con.prepareStatement(sql);
			ps.setString(1, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poEvent = new HashMap<String, String>();
				poEvent.put("order_item", rs.getString("LineItemText"));
				poEvent.put("qty", rs.getString("Quantity"));
				POEvent.add(poEvent);
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement(displaySql);
			ps.setString(1, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poListEvent = new HashMap<String, String>();
				poListEvent.put("order_no", rs.getString("OrderNumber"));
				poListEvent.put("order_item", rs.getString("LineItemText"));
				poListEvent.put("dispatch_date", rs.getString("DELIVERYDATE"));
				poListEvent.put("qty", rs.getString("Quantity"));
				poListEvent.put("remark", rs.getString("Remark"));
				POListEvent.add(poListEvent);
			}
			rs.close();
			ps.close();
			if (POEvent.size() > 0) {
				responsejson.put("poEvent", POEvent);
			} else {
				responsejson.put("message", "No Data Found");
			}

			if (POListEvent.size() > 0) {
				responsejson.put("poListEvent", POListEvent);
			} else {
				responsejson.put("message", "No Data Found");
			}
			jsonArray.add(responsejson);

		} catch (SQLException e) {
			log.error("getPOEvent() :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPODetail(String po_num, String id) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String po_data = "SELECT * FROM POEVENTDETAILS where PONUMBER =? AND BUSINESSPARTNEROID=?";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, po_num);
			ps.setString(2, id);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("PO_NUMBER", rs.getString("PONumber"));
				poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
				poData.put("LINEITEMNUMBER", rs.getString("LineItemNumber"));
				poData.put("LINEITEMTEXT", rs.getString("LineItemText"));
				poData.put("DELIVERYDATE", rs.getString("DeliveryDate"));
				poData.put("COMPANY", rs.getString("Company"));
				poData.put("PLANT", rs.getString("Plant"));
				poData.put("DEPARTMENT", rs.getString("Department"));
				poData.put("COSTCENTRE", rs.getString("CostCentre"));
				poData.put("CATEGORY", rs.getString("Category"));
				poData.put("BUSINESSPARTNERTEXT", rs.getString("BusinessPartnerText"));
				poData.put("QUANTITY", rs.getString("Quantity"));
				poData.put("UNITOFMEASURE", rs.getString("UnitOfMeasure"));
				poData.put("CONTACTPERSONEMAILID", rs.getString("ContactPersonEmailID"));
				poData.put("CONTACTPERSONPHONE", rs.getString("ContactPersonPhone"));
				poData.put("DELIVERYADDRESS1", rs.getString("DeliveryAddress1"));
				poData.put("DELIVERYADDRESS2", rs.getString("DeliveryAddress2"));
				poData.put("DELIVERYADDRESS3", rs.getString("DeliveryAddress3"));
				poData.put("CITY", rs.getString("City"));
				poData.put("STATE", rs.getString("State"));
				poData.put("COUNTRY", rs.getString("Country"));
				poData.put("PINCODE", rs.getString("PinCode"));
				poData.put("STATUS", rs.getString("Status"));
				poData.put("CREATEDON", rs.getString("CreatedOn"));
				poData.put("MODIFIEDON", rs.getString("ModifiedOn"));
				poData.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
				poData.put("ORDERNUMBER", rs.getString("OrderNumber"));
				poData.put("RATEPERQTY", rs.getString("RATEPERQTY"));
				poData.put("REMARK", rs.getString("Remark"));
				poData.put("BALANCE_QTY", rs.getString("Balance_qty"));
				poData.put("FORECLOSESTATUSCHECK", rs.getString("FORECLOSESTATUSCHECK"));
				POList.add(poData);
			}
			rs.close();
			ps.close();

			if (POList.size() > 0) {
				responsejson.put("poData", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}
		} catch (SQLException e) {
			log.error("getPODetail() :", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPODetailEvent(String po_num, String id) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String po_data = "SELECT PED.PONUMBER,PED.BUSINESSPARTNEROID,PED.LINEITEMNUMBER,PED.LINEITEMTEXT,PED.DELIVERYDATE,PED.COMPANY,PED.PLANT,PED.DEPARTMENT, "
				+ "PED.COSTCENTRE,PED.CATEGORY,PED.BUSINESSPARTNERTEXT,PED.QUANTITY,PED.UNITOFMEASURE,PED.CONTACTPERSONEMAILID,PED.CONTACTPERSONPHONE, "
				+ "PED.DELIVERYADDRESS1,PED.DELIVERYADDRESS2,PED.DELIVERYADDRESS3,PED.CITY,PED.STATE,PED.COUNTRY,PED.PINCODE,PED.STATUS,PED.CREATEDON, "
				+ "PED.MODIFIEDON,PED.MATERIAL_TYPE,PED.ORDERNUMBER,PED.RATEPERQTY,PED.REMARK,PED.BALANCE_QTY,PED.CURRENCY,PED.STORAGELOCATION,PED.SERVICE, "
				+ "PED.FORECLOSESTATUSCHECK,PD.ASNSTATUS,PED.DELVPLANT,PED.DELVPLANTNAME FROM POEVENTDETAILS PED,PODETAILS PD  WHERE "
				+ "PD.PONUMBER = PED.PONUMBER AND PED.PONUMBER = ? AND PED.BUSINESSPARTNEROID = ? ";
		
		String uniquePoInCount = "Select count(*) as counter from PONINVOICESUMMERY where BUSINESSPARTNEROID = ?  and PONUMBER = ? AND INVOICENUMBER IS NOT NULL";

		int count = 0;
		int count1 = 0;
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			ps = con.prepareStatement(po_data);
			ps.setString(1, po_num);
			ps.setString(2, id);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("PO_NUMBER", rs.getString("PONUMBER"));
				poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
				poData.put("LINEITEMNUMBER", rs.getString("LINEITEMNUMBER"));
				poData.put("LINEITEMTEXT", rs.getString("LINEITEMTEXT"));
				poData.put("DELIVERYDATE", rs.getString("DELIVERYDATE"));
				poData.put("COMPANY", rs.getString("COMPANY"));
				poData.put("PLANT", rs.getString("PLANT"));
				poData.put("DEPARTMENT", rs.getString("DEPARTMENT"));
				poData.put("COSTCENTRE", rs.getString("COSTCENTRE"));
				poData.put("CATEGORY", rs.getString("CATEGORY"));
				poData.put("BUSINESSPARTNERTEXT", rs.getString("BUSINESSPARTNERTEXT"));
				poData.put("QUANTITY", rs.getString("QUANTITY"));
				poData.put("UNITOFMEASURE", rs.getString("UNITOFMEASURE"));
				poData.put("CONTACTPERSONEMAILID", rs.getString("CONTACTPERSONEMAILID"));
				poData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
				poData.put("DELIVERYADDRESS1", rs.getString("DELIVERYADDRESS1"));
				poData.put("DELIVERYADDRESS2", rs.getString("DELIVERYADDRESS2"));
				poData.put("DELIVERYADDRESS3", rs.getString("DELIVERYADDRESS3"));
				poData.put("CITY", rs.getString("CITY"));
				poData.put("STATE", rs.getString("STATE"));
				poData.put("COUNTRY", rs.getString("COUNTRY"));
				poData.put("PINCODE", rs.getString("PINCODE"));
				poData.put("STATUS", rs.getString("STATUS"));
				poData.put("CREATEDON", rs.getString("CREATEDON"));
				poData.put("MODIFIEDON", rs.getString("MODIFIEDON"));
				poData.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
				poData.put("ORDERNUMBER", rs.getString("ORDERNUMBER"));
				poData.put("RATEPERQTY", rs.getString("RATEPERQTY"));
				poData.put("REMARK", rs.getString("REMARK"));
				poData.put("BALANCE_QTY", rs.getString("BALANCE_QTY"));
				poData.put("CURRENCY", rs.getString("CURRENCY"));
				poData.put("STORAGELOCATION", rs.getString("STORAGELOCATION"));
				poData.put("SERVICENUMBER", rs.getString("SERVICE"));
				poData.put("FORECLOSESTATUSCHECK", rs.getString("FORECLOSESTATUSCHECK"));
				poData.put("ASNSTATUS", rs.getString("ASNSTATUS"));
				poData.put("DELVPLANT", rs.getString("DELVPLANT")==null?"-":rs.getString("DELVPLANT"));
				poData.put("DELVPLANTNAME", rs.getString("DELVPLANTNAME")==null?"-":rs.getString("DELVPLANTNAME"));
	
				POList.add(poData);
			}
			rs.close();
			ps.close();
			try {

				ps = con.prepareStatement(uniquePoInCount);
				ps.setString(1, id);
				ps.setString(2, po_num);
				rs = ps.executeQuery();
				while (rs.next()) {
					count = rs.getInt("counter");
				}
				rs.close();
				ps.close();
				if (count > 0) {
					responsejson.put("deliveryitem", "present");
				} else {
					responsejson.put("deliveryitem", "absent");
				}
			} catch (Exception e) {
				log.error("getPODetailEvent() 1 : ", e.fillInStackTrace());
				responsejson.put("message", e.getLocalizedMessage());
			}

			if (POList.size() > 0) {
				responsejson.put("poData", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Id");
				jsonArray.add(responsejson);
			}

		} catch (SQLException e) {
			log.error("getVendorDetails() 2 : ", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getPOSubmitQuery(String po_num, String emailid, String invoicenumber, String message, String bid,
			String subject, String status) throws SQLException {

		String po_data = "";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs1 = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			if (subject.equalsIgnoreCase("POconfirmation")) {

				if (status.equalsIgnoreCase("Y")) {
					status = "A";
					String po_status = "UPDATE podetails set Status=?,ModifiedOn=? where PONumber=?";
					ps = con.prepareStatement(po_status);
					ps.setString(1, status);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, po_num);
					ps.executeUpdate();
					ps.close();
					try {
						poAcceptance(po_num, con);
					} catch (Exception e) {
						log.error(" Error in getPOSubmitQuery poAcceptance :", e.fillInStackTrace());
					}
				}else if("N".equalsIgnoreCase(status)) {
					try {
						log.info(" inside the rejecting po ");
						poRejection(po_num, message, con);
					} catch (Exception e) {
						log.error(" Error in getPOSubmitQuery poRejection :", e.fillInStackTrace());
					} 
				}
			}

			String userStatus = null;
			String queryFindmsgStatus = "Select MESSAGE from PONINVOICESUMMERY where PONUMBER = ? and BUSINESSPARTNEROID =?"
					+ " and INVOICENUMBER is null ";
			ps = con.prepareStatement(queryFindmsgStatus);
			ps.setString(1, po_num);
			ps.setString(2, bid);
			rs = ps.executeQuery();

			if (rs.next()) {
				userStatus = rs.getString("MESSAGE") == null ? "" : rs.getString("MESSAGE");
				if ("N".equalsIgnoreCase(userStatus)) {

					String queryFindPoDetails = "Select CONTACTPERSONEMAILID,REQUSITIONER from PODETAILS where PONUMBER = ? "
							+ "AND BUSINESSPARTNEROID = ? ";
					PreparedStatement ps1 = con.prepareStatement(queryFindPoDetails);
					ps1.setString(1, po_num);
					ps1.setString(2, bid);
					rs1 = ps1.executeQuery();

					String buyerId = null;
					String endUserId = null;
					String amount = null;
					String msg = null;
					String primaryEmailid = null;
					String secondaryEmailid = null;
					String tertiaryEmailid = null;
					ArrayList<String> emailArrayList = new ArrayList<String>();

					if (rs1.next()) {
						buyerId = rs1.getString("CONTACTPERSONEMAILID") == null ? "-"
								: rs1.getString("CONTACTPERSONEMAILID");
						endUserId = rs1.getString("REQUSITIONER") == null ? "-" : rs1.getString("REQUSITIONER");

						String queryUpdate = "update PONINVOICESUMMERY set MESSAGE = ?,REQUSITIONER = ?,BUYER= ? where PONUMBER = ? and INVOICENUMBER is null ";
						PreparedStatement ps2 = con.prepareStatement(queryUpdate);
						ps2.setString(1, "Y");
						ps2.setString(2, endUserId);
						ps2.setString(3, buyerId);
						ps2.setString(4, po_num);
						ps2.executeUpdate();
						ps2.close();
					}
					rs1.close();
					ps1.close();
				}

			} else {
				String queryFindPoDetails = "Select CONTACTPERSONEMAILID,REQUSITIONER from PODETAILS where PONUMBER = ? "
						+ "AND BUSINESSPARTNEROID = ? ";
				PreparedStatement ps1 = con.prepareStatement(queryFindPoDetails);
				ps1.setString(1, po_num);
				ps1.setString(2, bid);
				rs1 = ps1.executeQuery();

				String buyerId = null;
				String endUserId = null;
				String amount = null;
				String msg = null;
				String primaryEmailid = null;
				String secondaryEmailid = null;
				String tertiaryEmailid = null;
				ArrayList<String> emailArrayList = new ArrayList<String>();

				if (rs1.next()) {
					buyerId = rs1.getString("CONTACTPERSONEMAILID") == null ? "-"
							: rs1.getString("CONTACTPERSONEMAILID");
					endUserId = rs1.getString("REQUSITIONER") == null ? "-" : rs1.getString("REQUSITIONER");

					emailArrayList.add(buyerId);
					emailArrayList.add(endUserId);
				}
				rs1.close();
				ps1.close();

				String querySubmitSummary = "insert into PONINVOICESUMMERY (INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,"
						+ "MESSAGE,REQUSITIONER,BUYER,AMOUNT,CREATEDON ,MACOUNT,HOLDCOUNT,OVERALLSTATUS) "
						+ "values (?,?,?,?,?,?,?,?,?,?,?)";
				ps1 = con.prepareStatement(querySubmitSummary);
				ps1.setString(1, invoicenumber);
				ps1.setString(2, po_num);
				ps1.setString(3, bid);
				ps1.setString(4, "Y");
				ps1.setString(5, buyerId);
				ps1.setString(6, endUserId);
				ps1.setString(7, "0");
				ps1.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps1.setInt(9, 0);
				ps1.setInt(10, 0);
				ps1.setString(11, "");
				ps1.executeUpdate();
				ps1.close();

				String queryBusinessPartner = "Select BUSINESSPARTNEROID,PRIMARYEMAILID,SECONDARYEMAILID,TERTIARYEMAILID"
						+ " from BUSINESSPARTNER where BUSINESSPARTNEROID = ? and STATUS =? ";
				PreparedStatement ps2 = con.prepareStatement(queryBusinessPartner);
				ps2.setString(1, bid);
				ps2.setString(2, "A");
				rs2 = ps2.executeQuery();

				if (rs2.next()) {
					primaryEmailid = rs2.getString("PRIMARYEMAILID") == null ? "-" : rs2.getString("PRIMARYEMAILID");
					secondaryEmailid = rs2.getString("SECONDARYEMAILID") == null ? "-"
							: rs2.getString("SECONDARYEMAILID");
					tertiaryEmailid = rs2.getString("TERTIARYEMAILID") == null ? "-" : rs2.getString("TERTIARYEMAILID");

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
					ps2.setString(2, invoicenumber);
					ps2.setString(3, po_num);
					ps2.setString(4, emailArrayList.get(counter));
					ps2.setString(5, "A");
					ps2.executeUpdate();
					ps2.close();
				}
			}
			rs.close();
			ps.close();
			String queryNotification = "insert into CHATMESSAGE (BUSINESSPARTNEROID,SENDER,PONUMBER,"
					+ "INVOICENUMBER,MESSAGETEXT,SUBJECT,STATUS,CREATEDON) values (?,?,?,?,?,?,?,?)";
			ps = con.prepareStatement(queryNotification);
			ps.setString(1, bid);
			ps.setString(2, emailid);
			ps.setString(3, po_num);
			ps.setString(4, invoicenumber);
			ps.setString(5, message);
			ps.setString(6, subject);
			ps.setString(7, "A");
			ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.executeUpdate();
			ps.close();
			String queryFindStatus = "Select status from CHATSTATUS where PONUMBER = ?  "
					+ "and LOGGEDIN = ?  and INVOICENUMBER is null";
			ps = con.prepareStatement(queryFindStatus);
			ps.setString(1, po_num);
			ps.setString(2, emailid);
			rs = ps.executeQuery();
			String statusUser = null;

			if (rs.next()) {

				String queryUpdateChat = "update CHATSTATUS set status =? where PONUMBER = ? "
						+ "and LOGGEDIN = ? and INVOICENUMBER is null";
				PreparedStatement ps1 = con.prepareStatement(queryUpdateChat);
				ps1.setString(1, "R");
				ps1.setString(2, po_num);
				ps1.setString(3, emailid);
				ps1.executeUpdate();
				ps1.close();
			} else {

				String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
						+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
				PreparedStatement ps1 = con.prepareStatement(queryChat);
				ps1.setString(1, bid);
				ps1.setString(2, invoicenumber);
				ps1.setString(3, po_num);
				ps1.setString(4, emailid);
				ps1.setString(5, "R");
				ps1.executeUpdate();
				ps1.close();
			}
			rs.close();
			ps.close();

			String queryUpdate = "update CHATSTATUS set status = ? where PONUMBER = ?  "
					+ " and loggedin <> ? and INVOICENUMBER is null ";
			ps = con.prepareStatement(queryUpdate);
			ps.setString(1, "A");
			ps.setString(2, po_num);
			ps.setString(3, emailid);
			ps.executeUpdate();
			ps.close();
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);
			con.commit();
		} catch (SQLException e) {
			log.error("getPOSubmitQuery() :", e.fillInStackTrace());
			con.rollback();
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getSubmitQueryDetails(String bid, String po_num) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(po_num);
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

		String po_data = "SELECT * FROM CHATMESSAGE where BusinessPartnerOID = ? AND PONUMBER = ? "
				+ "and INVOICENUMBER is null order by createdon desc";

		ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, bid);
			ps.setString(2, po_num);
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
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getSubmitQueryDetails() :", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray getPODetails(List<String> ponumbers) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			String po_data = "SELECT * FROM poeventdetails where PONumber =? AND ORDERNUMBER IS NULL";
			Hashtable<String, List<ArrayList>> fulllist = new Hashtable<String, List<ArrayList>>();
			for (int i = 0; i < ponumbers.size(); i++) {

				List<ArrayList> getlistofpoeventlist = new ArrayList<ArrayList>();
				ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
				ps = con.prepareStatement(po_data);
				ps.setString(1, ponumbers.get(i));
				rs = ps.executeQuery();
				while (rs.next()) {

					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PO_NUMBER", rs.getString("PONumber"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
					poData.put("LINEITEMNUMBER", rs.getString("LineItemNumber"));
					poData.put("LINEITEMTEXT", rs.getString("LineItemText"));
					poData.put("ORDERNUMBER", rs.getString("OrderNumber"));
					poData.put("COMPANY", rs.getString("Company"));
					poData.put("PLANT", rs.getString("Plant"));
					poData.put("DEPARTMENT", rs.getString("Department"));
					poData.put("COSTCENTRE", rs.getString("CostCentre"));
					poData.put("CATEGORY", rs.getString("Category"));
					poData.put("BUSINESSPARTNERTEXT", rs.getString("BusinessPartnerText"));
					poData.put("QUANTITY", rs.getString("Quantity"));
					poData.put("UNITOFMEASURE", rs.getString("UnitOfMeasure"));
					poData.put("CONTACTPERSONEMAILID", rs.getString("ContactPersonEmailID"));
					poData.put("CONTACTPERSONPHONE", rs.getString("ContactPersonPhone"));
					poData.put("DELIVERYADDRESS1", rs.getString("DeliveryAddress1"));
					poData.put("DELIVERYADDRESS2", rs.getString("DeliveryAddress2"));
					poData.put("DELIVERYADDRESS3", rs.getString("DeliveryAddress3"));
					poData.put("CITY", rs.getString("City"));
					poData.put("STATE", rs.getString("State"));
					poData.put("COUNTRY", rs.getString("Country"));
					poData.put("PINCODE", rs.getString("PinCode"));
					if (rs.getString("Status").equalsIgnoreCase("A")) {
						poData.put("STATUS", "Accepted");
					} else if (rs.getString("Status").equalsIgnoreCase("P")) {
						poData.put("STATUS", "Pending");
					} else if (rs.getString("Status").equalsIgnoreCase("W")) {
						poData.put("STATUS", "Work In Progress");
					} else if (rs.getString("Status").equalsIgnoreCase("S")) {
						poData.put("STATUS", "Shipped");
					} else if (rs.getString("Status").equalsIgnoreCase("C")) {
						poData.put("STATUS", "Complete");
					}
					poData.put("CREATEDON", rs.getString("CreatedOn"));
					poData.put("MODIFIEDON", rs.getString("ModifiedOn"));
					poData.put("RATEPERQTY", rs.getString("RATEPERQTY"));
					poData.put("BALANCE_QTY", rs.getString("BALANCE_QTY"));
					POList.add(poData);
				}
				rs.close();
				ps.close();
				getlistofpoeventlist.add(POList);
				String po_data1 = "SELECT * FROM podetails where PONumber =?";
				ArrayList<HashMap<String, String>> POList1 = new ArrayList<HashMap<String, String>>();
				String queryLists[] = { "payment amount is incorrect", "payment amount is incorrect",
						"payment amount is incorrect", "payment amount is incorrect" };
				ps = con.prepareStatement(po_data1);
				ps.setString(1, ponumbers.get(i));
				rs = ps.executeQuery();
				while (rs.next()) {

					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PO_NUMBER", rs.getString("PONumber"));
					String dateqw = rs.getDate("PODate").toString();
					DateFormat formatter = new SimpleDateFormat();
					SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
					Date date;
					try {
						date = inputFormat.parse(dateqw);
						SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy");
						poData.put("DATE", outputFormat.format(date).toString());
					} catch (Exception e) {
						log.error("getVendorDetails() :", e.fillInStackTrace());
					}

					poData.put("AMOUNT", rs.getString("POAmount"));

					if (rs.getString("Status").equalsIgnoreCase("A")) {
						poData.put("STATUS", "Accepted");
					} else if (rs.getString("Status").equalsIgnoreCase("P")) {
						poData.put("STATUS", "Work In Progress");
					} else if (rs.getString("Status").equalsIgnoreCase("S")) {
						poData.put("STATUS", "Shipped");
					} else if (rs.getString("Status").equalsIgnoreCase("C")) {
						poData.put("STATUS", "Complete");
					} else if (rs.getString("Status").equalsIgnoreCase("N")) {
						poData.put("STATUS", "New");
					} else {
						poData.put("STATUS", rs.getString("Status"));
					}
					poData.put("Quantity", rs.getString("Quantity"));
					poData.put("COMPANY", rs.getString("Company"));
					poData.put("PLANT", rs.getString("Plant"));
					poData.put("DEPARTMENT", rs.getString("Department"));
					poData.put("COSTCENTRE", rs.getString("CostCentre"));
					poData.put("CATEGORY", rs.getString("Category"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
					poData.put("VENDORID", rs.getString("VendorID"));
					poData.put("CREATEDDATE", rs.getString("CreatedDate"));
					poData.put("STARTDATE", rs.getString("StartDate"));
					poData.put("ENDDATE", rs.getString("EndDate"));
					poData.put("LINEITEMNUMBER", rs.getString("LineItemNumber"));
					poData.put("LINEITEMTEXT", rs.getString("LineItemText"));
					poData.put("UNITOFMEASURE", rs.getString("UnitOfMeasure"));
					poData.put("IGSTAMOUNT", rs.getString("iGSTAmount"));
					poData.put("CGSTAMOUNT", rs.getString("cGSTAmount"));
					poData.put("SGSTAMOUNT", rs.getString("sgstAmount"));
					poData.put("CONTACTPERSONEMAILID", rs.getString("ContactPersonEmailID"));
					poData.put("CONTACTPERSONPHONE", rs.getString("ContactPersonPhone"));
					poData.put("DELIVERYADDRESS1", rs.getString("DeliveryAddress1"));
					poData.put("DELIVERYADDRESS2", rs.getString("DeliveryAddress2"));
					poData.put("DELIVERYADDRESS3", rs.getString("DeliveryAddress3"));
					poData.put("CITY", rs.getString("City"));
					poData.put("STATE", rs.getString("State"));
					poData.put("COUNTRY", rs.getString("Country"));
					poData.put("PINCODE", rs.getString("PinCode"));
					poData.put("BUYER", rs.getString("Buyer"));
					poData.put("REQUSITIONER", rs.getString("Requsitioner"));
					POList1.add(poData);
				}
				rs.close();
				ps.close();
				getlistofpoeventlist.add(POList1);
				fulllist.put(ponumbers.get(i), getlistofpoeventlist);

			}
			String encodedfile = downloadfile(fulllist);
			if (encodedfile.equalsIgnoreCase("")) {
				responsejson.put("message", "Fail");

			} else {
				responsejson.put("message", "Success");
				responsejson.put("data", encodedfile);
			}
			jsonArray.add(responsejson);

		} catch (SQLException e) {
			log.error("getPODetails() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	private String downloadfile(Hashtable<String, List<ArrayList>> fulllist) {

		Set<Entry<String, List<ArrayList>>> entrySet = fulllist.entrySet();
		List<List<String>> totallist = new ArrayList<List<String>>();

		for (Entry<String, List<ArrayList>> entry : entrySet) {
			List<ArrayList> listarray = entry.getValue();
			List<HashMap<String, String>> innerlist2 = listarray.get(1);

			List<HashMap<String, String>> innerlist = listarray.get(0);
			for (int j = 0; j < innerlist.size(); j++) {
				List<String> rowwise = new ArrayList<String>();
				rowwise.add(innerlist2.get(0).get("PO_NUMBER"));
				rowwise.add(innerlist2.get(0).get("DATE"));

				try {
					HashMap<String, String> hashinner = innerlist.get(j);
					rowwise.add(hashinner.get("LINEITEMNUMBER"));
					rowwise.add(hashinner.get("LINEITEMTEXT"));
					rowwise.add(hashinner.get("RATEPERQTY"));
					rowwise.add(hashinner.get("QUANTITY"));
					rowwise.add(hashinner.get("BALANCE_QTY"));
					rowwise.add(String.valueOf(Double.parseDouble(hashinner.get("QUANTITY"))
							* Double.parseDouble(hashinner.get("RATEPERQTY"))));
					rowwise.add(innerlist2.get(0).get("AMOUNT"));
					rowwise.add(innerlist2.get(0).get("STATUS"));
					Set<Entry<String, String>> entrySet1 = hashinner.entrySet();

				} catch (Exception e) {
					log.error("downloadfile() :", e.fillInStackTrace());
				}
				totallist.add(rowwise);
			}
		}
		String encodedfile = writeintoexcelfile(totallist);
		return encodedfile;
	}

	public String writeintoexcelfile(List<List<String>> poDataList) {
		String encodedfile = "";
		XSSFWorkbook workbook = new XSSFWorkbook();
		try {
			FileOutputStream out = new FileOutputStream(new File("demo.xlsx"));
			XSSFSheet sheet = workbook.createSheet("PO Data");
			List<String> heading = new ArrayList<String>();

			heading.add("PLANT CODE");
			heading.add("PLANT NAME");
			heading.add("PO DATE");
			heading.add("PO NUMBER");
			heading.add("LINE ITEM NUMBER");
			heading.add("LINE ITEM DESCRIPTION");
			heading.add("PO AMOUNT");
			heading.add("LINE ITEM RATE");
			heading.add("LINE ITEM QUANTITY");
			heading.add("LINE ITEM AMOUNT");
			heading.add("LINE ITEM BALANCE QUANTITY");
			heading.add("LINE ITEM BALANCE AMOUNT");
			heading.add("STATUS");

			Iterator<String> headingIterator = heading.iterator();
			Iterator<List<String>> i = poDataList.iterator();
			int rownum = 0;
			int cellnum = 0;
			Row headingRow = sheet.createRow(rownum);

			while (headingIterator.hasNext()) {
				String colHeading = (String) headingIterator.next();

				Cell cell = headingRow.createCell(cellnum);
				sheet.autoSizeColumn(cellnum);
				XSSFFont fontBold = workbook.createFont();
				fontBold.setBold(true);
				CellStyle cellStyle1 = workbook.createCellStyle();
				cellStyle1.setAlignment(HorizontalAlignment.CENTER);
				XSSFRichTextString cellValue = new XSSFRichTextString();
				cellValue.append(colHeading, fontBold);
				cell.setCellValue(cellValue);
				cell.setCellStyle(cellStyle1);

				cellnum = cellnum + 1;

			}

			try {

				while (i.hasNext()) {
					cellnum = 0;
					rownum = rownum + 1;

					Row row = sheet.createRow(rownum);
					List<String> poDataRow = (List<String>) i.next();
					Iterator<String> tempIterator = poDataRow.iterator();
					int k = 0;
					while (tempIterator.hasNext()) {
						String temp = (String) tempIterator.next();
						Cell cell = row.createCell(cellnum);
						CellStyle cellStyle = workbook.createCellStyle();
						if (k == 0) {
							cellStyle.setAlignment(HorizontalAlignment.CENTER);
						} else if (k == 1) {
							cellStyle.setAlignment(HorizontalAlignment.LEFT);
						} else if (k == 2) {
							cellStyle.setAlignment(HorizontalAlignment.CENTER);
						} else if (k == 3) {
							cellStyle.setAlignment(HorizontalAlignment.CENTER);
						} else if (k == 4) {
							cellStyle.setAlignment(HorizontalAlignment.CENTER);
						} else if (k == 5) {
							cellStyle.setAlignment(HorizontalAlignment.LEFT);
						} else if (k == 6) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 7) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 8) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 9) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 10) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 11) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 12) {
							cellStyle.setAlignment(HorizontalAlignment.LEFT);
						}
						sheet.autoSizeColumn(cellnum);
						cell.setCellValue(temp);
						cell.setCellStyle(cellStyle);
						cellnum = cellnum + 1;
						k++;
					}
				}

			} catch (Exception e) {
				log.error("writeintoexcelfile() 1 : ", e.fillInStackTrace());
			}
			workbook.write(out);

			File file = new File("demo.xlsx");
			try {
				InputStream inputStream;
				inputStream = new FileInputStream(file);
				byte[] bytes1 = new byte[(int) file.length()];
				inputStream.read(bytes1);
				encodedfile = new String(Base64.encodeBase64(bytes1), "UTF-8");
				out.close();
				workbook.close();
				responsejson.put("message", "Success");
				responsejson.put("data", encodedfile);
			} catch (IOException e) {
				log.error("writeintoexcelfile() 2 : ", e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("writeintoexcelfile() 3 : ", e.fillInStackTrace());
		}

		return encodedfile;
	}

	public byte[] convert(XSSFSheet sheet) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			sheet.getWorkbook().write(baos);
		} catch (IOException e) {
			log.error("convert() :", e.fillInStackTrace());
		}
		return baos.toByteArray();
	}

	public JSONArray getPOReadStatus(String bid, String userMailId) throws SQLException {

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
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			String po_data = "Select PONUMBER,STATUS from CHATSTATUS where BusinessPartnerOID =? "
					+ "and loggedin = ? and INVOICENUMBER is null ";
			String poninvoices = "Select PONUMBER from poninvoicesummery where BusinessPartnerOID =? and MESSAGE= ? "
					+ "and INVOICENUMBER is null ";

			ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
			ArrayList<HashMap<String, String>> mapQueryList = new ArrayList<HashMap<String, String>>();
			ps = con.prepareStatement(po_data);
			ps.setString(1, bid);
			ps.setString(2, userMailId);
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> poQuery = new HashMap<String, String>();
				HashMap<String, String> singleList = new HashMap<String, String>();
				String status1 = rs.getString("STATUS") == null ? "" : rs.getString("STATUS");
				if (!"".equalsIgnoreCase(status1)) {
					if ("A".equalsIgnoreCase(status1)) {
						poQuery.put("PONUMBER", rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER"));
						poQuery.put("STATUS", status1);
						POQueryList.add(poQuery);
					}
				}
				String poNumber = rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER");
				String status = rs.getString("STATUS") == null ? "" : rs.getString("STATUS");
				singleList.put(poNumber, status);
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
				if (singleList.containsKey(poNumber)) {
					String poStatus = singleList.get(poNumber);
					if ("A".equalsIgnoreCase(poStatus)) {
						margeList.put("PONUMBER", poNumber);
						margeList.put("STATUS", "A");
						POQueryList.add(margeList);
					}
				} else {
					margeList.put("PONUMBER", poNumber);
					margeList.put("STATUS", "A");
					POQueryList.add(margeList);
				}
			}
			rs.close();
			ps.close();
			if (POQueryList.size() > 0) {
				responsejson.put("poQueryList", POQueryList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getPOReadStatus() :", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPOReadStatusUpdate(String bid, String po_num) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(po_num);
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

		int value = 0;
		String po_status = "UPDATE CHATMESSAGE set STATUS = ? where PONUMBER=? and BusinessPartnerOID =?";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			ps = con.prepareStatement(po_status);
			ps.setString(1, "R");
			ps.setString(2, po_num);
			ps.setString(3, bid);
			value = ps.executeUpdate();
			if (value > 0) {
				responsejson.put("message", "Success");
			} else {
				responsejson.put("message", "Fail");
			}
		} catch (SQLException e) {
			log.error("getPOReadStatusUpdate() :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		jsonArray.add(responsejson);
		return jsonArray;
	}
	/*
	 * public JSONArray getpOGST(String email) throws SQLException {
	 * 
	 * boolean result = Validation.StringChecknull(email); if (result == false) {
	 * responsejson.put("validation", "validation Fail");
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); return
	 * jsonArray; } else { result = Validation.emailCheck(email); if (result ==
	 * false) { responsejson.put("validation", "validation Fail");
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); return
	 * jsonArray; } }
	 * 
	 * String po_data =
	 * "select AttributeValue,BusinessPartnerOID from businesspartnerattributes where "
	 * +
	 * "AttributeText ='GST_REGN' and BusinessPartnerOID in (select BusinessPartnerOID from "
	 * + "businesspartner where (PrimaryEmailID= ? or SecondaryEmailID= ? or" +
	 * " TertiaryEmailID= ?) AND STATUS = 'A' union select BusinessPartnerOID from businesspartnerattributes "
	 * +
	 * "where AttributeValue= ? AND STATUS = 'A' union select BusinessPartnerOID from businesspartnercontacts where "
	 * + "ContactEmailID= ? AND STATUS = 'A') "; // and NVL(attributeValue,'-') not
	 * LIKE '%-%'
	 * 
	 * ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String,
	 * String>>(); Connection con = null; PreparedStatement ps = null; ResultSet rs
	 * = null; try { con = DBConnection.getConnection(); ps =
	 * con.prepareStatement(po_data); ps.setString(1, email); ps.setString(2,
	 * email); ps.setString(3, email); ps.setString(4, email); ps.setString(5,
	 * email); rs = ps.executeQuery(); while (rs.next()) {
	 * 
	 * HashMap<String, String> poData = new HashMap<String, String>();
	 * poData.put("ATTRIBUTEVALUE", rs.getString("attributevalue"));
	 * poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
	 * POList.add(poData); } rs.close(); ps.close(); if (POList.size() > 0) {
	 * responsejson.put("poData", POList); responsejson.put("message", "Success");
	 * jsonArray.add(responsejson); } else { responsejson.put("message",
	 * "No Data Found for given Vendor Id"); jsonArray.add(responsejson); }
	 * 
	 * } catch (SQLException e) { log.error("getpOGST() :", e.fillInStackTrace());
	 * responsejson.put("message", "Network Issue"); jsonArray.add(responsejson); }
	 * finally { DBConnection.closeConnection(rs, ps, con); } return jsonArray; }
	 */

	public JSONArray getpOGST(String vendorEmailId, String companyCode, String bid) {

//		String pgQuery = " Select bt.attributevalue as GSTNO,bt.businesspartneroid as BID  from businesspartnerattributes bt where bt.businesspartneroid in ( "
//				+ " select distinct bp.businesspartneroid from businesspartner bp where  "
//				+ " (bp.primaryemailid = ? or bp.secondaryemailid = ? or "
//				+ " bp.tertiaryemailid = ? ) and "
//				+ " bp.businesspartneroid in (select distinct pd.businesspartneroid from podetails pd where pd.companycode = ? )) "
//				+ " and bt.attributetext = 'GST_REGN' " ;

//		String pgQuery ="Select distinct bt.attributevalue as GSTNO from businesspartnerattributes bt where bt.businesspartneroid in ( "
//				+ "select distinct bp.businesspartneroid from businesspartner bp where (bp.primaryemailid = ? or "
//				+ "bp.secondaryemailid = ? or bp.tertiaryemailid = ? ) and "
//				+ "bp.businesspartneroid in (select distinct pd.businesspartneroid from podetails pd where pd.companycode = ? )) "
//				+ "and bt.attributetext = 'GST_REGN' ";

		String pgQuery = "Select distinct bt.attributevalue as GSTNO from businesspartnerattributes bt where bt.businesspartneroid in ( "
				+ "select distinct bp.businesspartneroid from businesspartner bp where (bp.primaryemailid = ? or "
				+ "bp.secondaryemailid = ? or bp.tertiaryemailid = ? ) and "
				+ "bp.businesspartneroid in (select distinct pd.businesspartneroid from podetails pd where pd.companycode = ? )) "
				+ "and bt.attributetext = 'GST_REGN' " + "Union "
				+ "Select distinct bt.attributevalue as GSTNO from businesspartnerattributes bt where bt.businesspartneroid in ( "
				+ "select distinct bp.businesspartneroid from businesspartnerattributes bp where bp.AttributeValue = ? and "
				+ "bp.businesspartneroid in (select distinct pd.businesspartneroid from podetails pd where pd.companycode = ? )) "
				+ "and bt.attributetext = 'GST_REGN' " + "Union "
				+ "Select distinct bt.attributevalue as GSTNO from businesspartnerattributes bt where bt.businesspartneroid in ( "
				+ "select distinct bp.businesspartneroid from businesspartnercontacts bp where bp.ContactEmailID = ? and "
				+ "bp.businesspartneroid in (select distinct pd.businesspartneroid from podetails pd where pd.companycode = ? )) "
				+ "and bt.attributetext = 'GST_REGN' ";

		String gstOfBidQuery = "Select distinct bt.attributevalue as GSTNO from businesspartnerattributes bt where bt.attributetext = 'GST_REGN' and bt.businesspartneroid = ?";

		ArrayList<HashMap<String, String>> pgList = new ArrayList<HashMap<String, String>>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DBConnection.getConnection();

			ps = con.prepareStatement(pgQuery);
			ps.setString(1, vendorEmailId);
			ps.setString(2, vendorEmailId);
			ps.setString(3, vendorEmailId);
			ps.setString(4, companyCode);

			ps.setString(5, vendorEmailId);
			ps.setString(6, companyCode);

			ps.setString(7, vendorEmailId);
			ps.setString(8, companyCode);
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> pgData = new HashMap<String, String>();
				pgData.put("ATTRIBUTEVALUE", rs.getString("GSTNO"));
//				pgData.put("BUSINESSPARTNEROID", rs.getString("BID"));
				pgList.add(pgData);
			}

			rs.close();
			ps.close();

			log.info("gstList : " + pgList.size());

			log.info("before mofification gstList : ");
			Iterator it = pgList.iterator();
			while (it.hasNext()) {
				HashMap<String, String> pgData = (HashMap<String, String>) it.next();
				log.info(pgData.get("ATTRIBUTEVALUE"));
			}

			String gstNumOfBid = null;
			if (bid != null && !"".equals(bid)) {
				ps = con.prepareStatement(gstOfBidQuery);
				ps.setString(1, bid);
				rs = ps.executeQuery();
				while (rs.next()) {
					gstNumOfBid = rs.getString("GSTNO");
				}
				rs.close();
				ps.close();

				log.info("BID : " + bid + " GST/PAN : " + gstNumOfBid);

				if (gstNumOfBid != null && !"".equals(gstNumOfBid)) {
					if (pgList.size() > 0) {
						for (int i = 0; i < pgList.size(); i++) {
							if (pgList.get(i).get("ATTRIBUTEVALUE").equals(gstNumOfBid)) {
								Collections.swap(pgList, 0, i);
								break;
							}
						}
					}
					log.info("After mofification gstList : ");
					Iterator it1 = pgList.iterator();
					while (it1.hasNext()) {
						HashMap<String, String> pgData = (HashMap<String, String>) it1.next();
						log.info(pgData.get("ATTRIBUTEVALUE"));
					}
				}
			}

			if (pgList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("poData", pgList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Success");
				responsejson.put("validation", "No data found");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("getPartnerGroupCompanyDetails() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getpOPincode(String gst, HttpSession session) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(gst);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String po_data = "select BusinessPartnerOID, pincode,City from businesspartneraddress where "
				+ "BusinessPartnerOID in ( select BusinessPartnerOID from businesspartnerattributes where"
				+ " AttributeValue= ? )";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, gst);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
				poData.put("PINCODE", rs.getString("pincode"));
				poData.put("CITY", rs.getString("City"));
				POList.add(poData);
			}
			rs.close();
			ps.close();

			if (POList.size() > 0) {
				responsejson.put("poData", POList);
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}
		} catch (SQLException e) {
			log.error("getpOPincode() :", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray getpOGSTPincode(String email) throws SQLException {

		boolean result = Validation.StringChecknull(email);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.emailCheck(email);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String po_data = "select A.businesspartneroid ,A.pincode, B.attributevalue, C.vendorid, A.City from "
				+ "businesspartneraddress A,businesspartnerattributes B,businesspartner C WHERE B.attributetext ='GST_REGN'"
				+ " and A.businesspartneroid = B.businesspartneroid and A.businesspartneroid = C.businesspartneroid and"
				+ " A.businesspartneroid in (select BusinessPartnerOID from businesspartner where "
				+ "PrimaryEmailID= ? or " + "SecondaryEmailID=? or" + " TertiaryEmailID= ? " + " union "
				+ "select BusinessPartnerOID from businesspartnerattributes where AttributeValue= ? " + " union "
				+ "select BusinessPartnerOID from businesspartnercontacts where ContactEmailID= ? )";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, email);
			ps.setString(2, email);
			ps.setString(3, email);
			ps.setString(4, email);
			ps.setString(5, email);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
				poData.put("PINCODE", rs.getString("pincode"));
				poData.put("ATTRIBUTEVALUE", rs.getString("attributevalue"));
				poData.put("VENDORID", rs.getString("vendorid"));
				poData.put("CITY", rs.getString("City"));
				POList.add(poData);
			}
			rs.close();
			ps.close();
			if (POList.size() > 0) {
				responsejson.put("poData", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}

		} catch (SQLException e) {
			log.error("getpOGSTPincode() :", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPoDetailsStatusN(String bid, String Status) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(Status);
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
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			String invoice_data1 = "select count(Status) as Status from podetails where BusinessPartnerOID =? and Status =?";
			String invoice_data2 = "select count(INVOICESTATUS) as Status from DELIVERYSUMMARY where BusinessPartnerOID =? and INVOICESTATUS =?";

			ArrayList<HashMap<String, String>> InvoiceQueryList = new ArrayList<HashMap<String, String>>();
			int coun = 0;
			int cound = 0;
			int add = 0;
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
			if (InvoiceQueryList.size() > 0) {
				responsejson.put("invoiceQuery", InvoiceQueryList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("getPoDetailsStatusN() :", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getModifiedOndDetails(String Bid) throws SQLException {

		boolean result = Validation.StringChecknull(Bid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(Bid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String po_data = "SELECT * FROM businesspartnerattributes where BusinessPartnerOID =? ";

		ArrayList<HashMap<String, String>> invoiceDataBidinvoceidIdList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			ps = con.prepareStatement(po_data);
			ps.setString(1, Bid);

			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> invoiceData = new HashMap<String, String>();

				invoiceData.put("MODIFIEDON", rs.getString("ModifiedOn"));
				invoiceData.put("AttributeText", rs.getString("AttributeText"));
				invoiceDataBidinvoceidIdList.add(invoiceData);
			}
			rs.close();
			ps.close();
			if (invoiceDataBidinvoceidIdList.size() > 0) {
				responsejson.put("invoiceData", invoiceDataBidinvoceidIdList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}

		} catch (SQLException e) {
			log.error("getModifiedOndDetails() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getInvoiceModifiedAndDetailsForInvoice(String Bid) throws SQLException {

		boolean result = Validation.StringChecknull(Bid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(Bid);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String po_data = "SELECT * FROM PONINVOICESUMMERY where BusinessPartnerOID =? ";

		ArrayList<HashMap<String, String>> invoiceDataBidinvoceidIdList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, Bid);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> invoiceData = new HashMap<String, String>();
				invoiceData.put("MODIFIEDON", rs.getString("ModifiedOn"));
				invoiceData.put("INVOICENUMBER", rs.getString("InvoiceNumber"));
				invoiceDataBidinvoceidIdList.add(invoiceData);
			}
			rs.close();
			ps.close();
			if (invoiceDataBidinvoceidIdList.size() > 0) {
				responsejson.put("invoiceData", invoiceDataBidinvoceidIdList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}

		} catch (SQLException e) {
			log.error("getInvoiceModifiedAndDetailsForInvoice() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public boolean validateSubmitInvoice(List<Invoicesubmission> persons) {

		boolean result;

		result = persons.isEmpty();
		int counterSupp = 0;
		if (result == true) {
			responsejson.put("Uniquemessage", "validation Fail");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return false;
		}
		for (int i = 0; i < persons.size(); i++) {
			result = Validation.StringChecknull(persons.get(i).getPo_num());
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}
			result = Validation.StringChecknull(persons.get(i).getuOM());
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}
			result = Validation.StringChecknull(persons.get(i).getBid());
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			} else {
				result = Validation.numberCheck(persons.get(i).getBid());
				if (result == false) {
					responsejson.put("Uniquemessage", "validation Fail");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
			result = Validation.StringChecknull(persons.get(i).getInvoiceNumber());
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			} else {

				result = Validation.invoiceNumberCheck(persons.get(i).getInvoiceNumber());
				if (result) {
					responsejson.put("Uniquemessage",
							"Invoice Number should not contain SPACES and special characters except forward slash (/) and hyphen (-)");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}

			}

			result = Validation.StringChecknull(persons.get(i).getLineItemNumber());
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}
			result = Validation.StringChecknull(persons.get(i).getInvoiceDate());
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}

			result = Validation.StringChecknull(persons.get(i).getQuantity());
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			} else {
				result = Validation.numberCheck(persons.get(i).getQuantity());
				if (result == false) {
					responsejson.put("Uniquemessage", "validation Fail");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}

			if (persons.get(i).getIrnNumber() != null && !"".equals(persons.get(i).getIrnNumber())
					&& !"-".equals(persons.get(i).getIrnNumber()) && !"NA".equals(persons.get(i).getIrnNumber())) {

				if (persons.get(i).getIrnNumber().length() != 64) {
					responsejson.put("Uniquemessage",
							"The IRN no. consists of 64 characters and cannot have special characters.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}

				result = Validation.irnNumberCheck(persons.get(i).getIrnNumber());
				if (result) {
					responsejson.put("Uniquemessage",
							"The IRN no. consists of 64 characters and cannot have special characters.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}

			if (persons.get(i).getActualfilename() != null) {
				result = Validation.fileExtension(persons.get(i).getActualfilename());
				if (result) {
					responsejson.put("Uniquemessage", "Only pdf file extension is allowed.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
			if (persons.get(i).getSavedfilename() != null) {
				result = Validation.fileExtension(persons.get(i).getSavedfilename());
				if (result) {
					responsejson.put("Uniquemessage", "Only pdf file extension is allowed.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
			if (persons.get(i).getMultipleactualfilename() != null) {
				result = Validation.multiFilesExtension(persons.get(i).getMultipleactualfilename());
				if (result) {
					responsejson.put("Uniquemessage",
							"Only JPEG, JPG, PNG, DOC, DOCX, XLS, XLSX, CSV, PDF file extensions are allowed.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
			if (persons.get(i).getMultiplesavedfilename() != null) {
				result = Validation.multiFilesExtension(persons.get(i).getMultiplesavedfilename());
				if (result) {
					responsejson.put("Uniquemessage",
							"Only JPEG, JPG, PNG, DOC, DOCX, XLS, XLSX, CSV, PDF file extensions are allowed.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
		}
		return true;
	}

	public JSONArray insertinvoice(List<Invoicesubmission> persons, String vendorEmail) throws SQLException {

		if (persons != null && !persons.isEmpty()) {

			if ("reopen".equalsIgnoreCase(persons.get(0).getType())) {

				SimpoImpl simpoimpl = new SimpoImpl();
				try {
					return simpoimpl.updateReopenedInvoice(persons);
				} catch (SQLException e) {
					log.error("insertinvoice() :", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "SQLException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} catch (DXPortalException e) {
					log.error("insertinvoice() :", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "DXPortalException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} catch (ParseException e) {
					log.error("insertinvoice() :", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "ParseException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}
			}
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean grnerror = false;
		boolean result;

		boolean validation = validateSubmitInvoice(persons);
		if (validation == false) {
			deletebaddeliveries(persons);
			return jsonArray;
		}
		int counterSupp = 0;

		try {
			con = DBConnection.getConnection();

			int pocount = 0;
			int poincount = 0;
			double balance = 0;

			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				for (int i = 0; i < persons.size(); i++) {
					balance = getBalanceCount(persons.get(i).getPo_num(), persons.get(i).getLineItemNumber(),
							persons.get(i).getBid(), con);
					if (balance < Double.parseDouble(persons.get(i).getQuantity())) {
						updategrnstatus(persons, "B", con);
						responsejson.put("Uniquemessage", "Insufficient balance to create invoice");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						deletebaddeliveries(persons);
						return jsonArray;

					}
				}
			}

			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				boolean alreadypresent = false;
				int checkinvoiceingrn = checkinvoiceingrntable(persons, con);
				if (checkinvoiceingrn > 0) {
					alreadypresent = true;
				}
				if (alreadypresent) {

					responsejson.put("Uniquemessage",
							"INVOICENUMBER is already present in to-be-invoice list. Please  use a different invoice number.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					deletebaddeliveries(persons);
					return jsonArray;
				}
			}

			for (int i = 0; i < persons.size(); i++) {
				poincount = getUniquePONInCheck(persons.get(i).getInvoiceNumber(), con, persons.get(i).getBid(),
						persons.get(i).getInvoiceDate());
				if (poincount > 0) {
					grnerror = true;
					if (!persons.get(i).getGrnnumber().equalsIgnoreCase("-")) {
						responsejson.put("Uniquemessage",
								"Invoice number already exists. Please  use a different invoice number");
					} else {
						responsejson.put("Uniquemessage",
								"Invoice number already exists. Please  use a different invoice number");

					}
					deletebaddeliveries(persons);
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}
				if (!persons.get(i).getGrnnumber().equalsIgnoreCase("-")) {
					String uniquePoInCount = "Select count(*) as counter from deliverysummary where GRNNUMBER = ?  and PONUMBER=? "
							+ "and  INVOICENUMBER=? and BUSSINESSPARTNEROID=? ";

					int count = 0;
					ps = con.prepareStatement(uniquePoInCount);
					ps.setString(1, persons.get(i).getGrnnumber());
					ps.setString(2, persons.get(i).getPo_num());
					ps.setString(3, persons.get(i).getInvoiceNumber());
					ps.setString(4, persons.get(i).getBid());
					rs = ps.executeQuery();
					while (rs.next()) {
						count = rs.getInt("counter");
					}
					if (count > 0) {
						grnerror = true;
						deletebaddeliveries(persons);
						responsejson.put("Uniquemessage", "GRNNUMBER already present");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						return jsonArray;
					}
				} else if (persons.get(i).getActualfilename() == null || persons.get(i).getActualfilename() == "null"
						|| persons.get(i).getSavedfilename() == null || persons.get(i).getSavedfilename() == "null") {
					responsejson.put("Uniquemessage", "Invoice file not uploaded correctly. Please try again.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}
			}
		} catch (Exception e) {
			log.error("insertinvoice() 1 :", e.fillInStackTrace());
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", persons.get(0).getPo_num());
			responsejson.put("invoicenumber", persons.get(0).getInvoiceNumber());
			jsonArray.add(responsejson);
			return jsonArray;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		String marterialCode = "";
		String deliveryUniqueNoString = null;
		double totalquantity = 0;
		StringBuffer grnnumber = new StringBuffer();
		StringBuffer srcnnumber = new StringBuffer();
		Set<String> s = new HashSet<String>();
		Set<String> s1 = new HashSet<String>();
		for (int i = 0; i < persons.size(); i++) {
			s.add(persons.get(i).getGrnnumber());
			s1.add(persons.get(i).getSrcnnumber());
			totalquantity = totalquantity + Double.parseDouble(persons.get(i).getQuantity());

		}
		int b = 0;
		for (String str : s) {

			if (!str.equalsIgnoreCase("-")) {

				grnnumber.append(str);
				if (b < s.size() - 1) {
					grnnumber.append(",");
				}
			}
			b++;
		}
		b = 0;
		for (String str : s1) {

			if (!str.equalsIgnoreCase("-")) {
				if ((!str.equalsIgnoreCase("-")) && (str != null)) {
					srcnnumber.append(str);
				}

				if (b < s1.size() - 1) {
					if ((!str.equalsIgnoreCase("-")) && (str != null)) {
						srcnnumber.append(",");
					}
				}
			}
			b++;
		}

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String querySummary = "insert into PONINVOICESUMMERY (INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,MESSAGE,"
					+ "REQUSITIONER,BUYER,AMOUNT,CREATEDON,MACOUNT,HOLDCOUNT,OVERALLSTATUS,INVOICEDATE,MATERIAL_TYPE,"
					+ "PGQ,ONEXSTATUS,ACTUALFILENAME,SAVEDFILENAME,PLANT,IRNNUMBER,"
					+ "IRNDATE,DESCRIPTION,CREATEDBY,BUSINESSPARTNERTEXT,VENDORID,BILLOFLADINGDATE,"
					+ "CONTACTPERSON,CONTACTPERSONPHONE,REMARK,TOTALAMTINCTAXES,TAXAMOUNT,GRNNUMBER,SCRNNUMBER,UNIQUEREFERENCENUMBER,BASEPO)"
					+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			ps = con.prepareStatement(querySummary);
			ps.setString(1, persons.get(0).getInvoiceNumber());
			ps.setString(2, persons.get(0).getPo_num());
			ps.setString(3, persons.get(0).getBid());
			ps.setString(4, "N");
			ps.setString(5, persons.get(0).getContactPerson());
			ps.setString(6, persons.get(0).getBuyerid());
			ps.setString(7, persons.get(0).getTotalAmount());
			ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setInt(9, 1);
			ps.setInt(10, 0);
			ps.setString(11, persons.get(0).getStatus());
			ps.setDate(12, new java.sql.Date(
					new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(0).getInvoiceDate()).getTime()));
			ps.setString(13, persons.get(0).getMaterial());
			ps.setDouble(14, totalquantity);
			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				ps.setString(15, null);
			} else {
				ps.setString(15, "R");
			}
			ps.setString(16, persons.get(0).getActualfilename());
			ps.setString(17, persons.get(0).getSavedfilename());
			ps.setString(18, persons.get(0).getPlant());
			ps.setString(19, persons.get(0).getIrnNumber());
			if (persons.get(0).getIrnDate() != null && persons.get(0).getIrnDate() != ""
					&& persons.get(0).getIrnDate() != "null") {
				ps.setDate(20, new java.sql.Date(
						new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(0).getIrnDate()).getTime()));
			} else {
				ps.setString(20, persons.get(0).getIrnDate());
			}
			ps.setString(21, persons.get(0).getDescription());
			ps.setString(22, persons.get(0).getCreatedby());
			ps.setString(23, persons.get(0).getBusinessPartnerText());
			ps.setString(24, persons.get(0).getVendorID());
			if (persons.get(0).getBillofladingdate() != null && !"".equals(persons.get(0).getBillofladingdate())
					&& !("Invalid date").equalsIgnoreCase(persons.get(0).getBillofladingdate())) {
				ps.setDate(25, new java.sql.Date(
						new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(0).getBillofladingdate()).getTime()));
			} else {
				ps.setDate(25, null);
			}
			ps.setString(26, persons.get(0).getContactPerson());
			ps.setString(27, persons.get(0).getContactPersonPhone());
			ps.setString(28, persons.get(0).getRemark());
			ps.setString(29, persons.get(0).getTotalamtinctaxes());
			ps.setString(30, persons.get(0).getTaxamount());
			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				ps.setString(31, null);
			} else {
				ps.setString(31, grnnumber.toString());
			}
			if (persons.get(0).getSrcnnumber().equalsIgnoreCase("-")) {
				ps.setString(32, null);
			} else {
				ps.setString(32, srcnnumber.toString());
			}
			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				ps.setString(33, null);
			} else {
				ps.setString(33, "Y");
			}

			ps.setString(34, persons.get(0).getPo_num());

			ps.executeUpdate();
			ps.close();

			String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
					+ "MODIFIEDTIME,MODIFIEDBY,RESUBMITTEDINVOICENO)" + " values(?,?,?,?,?,?,?)";

			ps = con.prepareStatement(insertaudit);
			ps.setString(1, persons.get(0).getInvoiceNumber());
			ps.setString(2, persons.get(0).getPo_num());
			ps.setString(3, persons.get(0).getBid());
			if ("".equalsIgnoreCase(persons.get(0).getBeforesubmissioninvoicenumber())
					|| "null".equalsIgnoreCase(persons.get(0).getBeforesubmissioninvoicenumber())
					|| persons.get(0).getBeforesubmissioninvoicenumber() == null) {
				ps.setString(4, persons.get(0).getStatus());
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//				ps.setString(6, persons.get(0).getContactPerson());
				ps.setString(6, vendorEmail);
				ps.setString(7, persons.get(0).getBeforesubmissioninvoicenumber());
			} else {
				ps.setString(4, "S");
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//				ps.setString(6, persons.get(0).getContactPerson());
				ps.setString(6, vendorEmail);
				ps.setString(7, persons.get(0).getBeforesubmissioninvoicenumber());
			}
			ps.executeUpdate();
			ps.close();

			String insertsupportingdocument = "insert into INVOICESUPPDOCS (BUSINESSPARTNEROID,"
					+ "INVOICENUMBER,PONUMBER,ACTUALFILENAME,SAVEDFILENAME) values " + "(?,?,?,?,?)";

			ps = con.prepareStatement(insertsupportingdocument);
			ps.setString(1, persons.get(0).getBid());
			ps.setString(2, persons.get(0).getInvoiceNumber());
			ps.setString(3, persons.get(0).getPo_num());
			ps.setString(4, persons.get(0).getMultipleactualfilename());
			ps.setString(5, persons.get(0).getMultiplesavedfilename());
			ps.executeUpdate();
			ps.close();
			counterSupp++;

			for (int i = 0; i < persons.size(); i++) {
				deliveryUniqueNoString = persons.get(i).getOrderNumber();
				updatedeliverysumary(persons.get(i).getPo_num(), persons.get(i).getLineItemNumber(),
						persons.get(i).getInvoiceNumber(), persons.get(i).getOrderNumber(),
						persons.get(i).getInvoiceDate(), persons.get(i).getQuantity(), persons.get(i).getTotalAmount(),
						persons.get(i).getuOM(), persons.get(i).getRateperquantity(), persons.get(i).getLineitemtext(),
						persons.get(i).getStatus(), persons.get(i).getInvoiceamount(),
						persons.get(i).getStoragelocation(), persons.get(i).getGrnnumber(),
						persons.get(i).getUniquereferencenumber(), persons.get(i).getSaplineitemnumber(),
						persons.get(i).getServicenumber(), con, persons.get(i).getSrcnnumber());

			}

			executeUpdateBalance(con, persons);

			for (int i = 0; i < persons.size(); i++) {

				String queryPolineitem = "Select * from inventoryuserlist where MTYP = "
						+ "(select MATERIAL_TYPE from poeventdetails  where (ponumber = ? and lineitemnumber = ? "
						+ "and businesspartneroid = ? and ordernumber is null)) AND plant = (select PLANT "
						+ "from poeventdetails  where (ponumber = ? and lineitemnumber =? and businesspartneroid =? "
						+ "and ordernumber is null))";

				ps = con.prepareStatement(queryPolineitem);
				ps.setString(1, persons.get(i).getPo_num());
				ps.setString(2, persons.get(i).getLineItemNumber());
				ps.setString(3, persons.get(i).getBid());
				ps.setString(4, persons.get(i).getPo_num());
				ps.setString(5, persons.get(i).getLineItemNumber());
				ps.setString(6, persons.get(i).getBid());
				rs = ps.executeQuery();

				while (rs.next()) {
					String materialType = rs.getString("MTYP") == null ? "" : rs.getString("MTYP");

					if (materialType != null && materialType != "") {
						if (!marterialCode.contains(materialType)) {
							marterialCode = materialType + ",";
						}
					}
				}
				rs.close();
				ps.close();
			}

			getUpdateinvoiceeventdetailwopo(persons.get(0).getInvoiceNumber(), persons.get(0).getBid(),
					persons.get(0).getPo_num(), con);

			String status = null;
			String buyerId = null;

			String sqlUpdate1 = "insert into invoiceapproval (VENDORID,INVOICENUMBER,PONUMBER,BUYERID,ENDUSEID,"
					+ "ENDUSERSTATUS,STAGE,MODIFIEDDATE,INVOICEDATE,STATUS,PROXY) values (?,?,?,?,?,?,?,?,?,?,?)";
			ps = con.prepareStatement(sqlUpdate1);
			ps.setString(1, persons.get(0).getVendorID());
			ps.setString(2, persons.get(0).getInvoiceNumber());
			ps.setString(3, persons.get(0).getPo_num());
			ps.setString(4, persons.get(0).getBuyerid());
			ps.setString(5, persons.get(0).getContactPerson());
			ps.setString(6, "P");
			ps.setString(7, persons.get(0).getStage());
			ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setDate(9, new java.sql.Date(
					new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(0).getInvoiceDate()).getTime()));

			ps.setString(10, "M");
			if (marterialCode != null && marterialCode != "") {
				ps.setString(11, "X");
			} else {
				ps.setString(11, null);
			}
			ps.executeUpdate();
			ps.close();

			String updategrn = "Update GRNMAPPING set STATUS=?,INVOICENUMBER=? where PONUMBER=? AND "
					+ "LINEITEMNO=? AND DCNUMBER=? AND GRNNUMBER=?";

			for (int j = 0; j < persons.size(); j++) {
				if (!("-").equalsIgnoreCase(persons.get(j).getGrnnumber())) {
					ps = con.prepareStatement(updategrn);
					ps.setString(1, "D");
					ps.setString(2, persons.get(j).getInvoiceNumber());
					ps.setString(3, persons.get(j).getPo_num());
					ps.setString(4, persons.get(j).getLineItemNumber());
					ps.setString(5, persons.get(j).getDcnumber());
					ps.setString(6, persons.get(j).getGrnnumber());
					ps.executeUpdate();
					ps.close();
				}
			}

			con.commit();
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);

		} catch (DXPortalException dxp) {
			con.rollback();
			deletebaddeliveries(persons);
			log.error("insertinvoice() 2 :", dxp.fillInStackTrace());
			responsejson.put("error", dxp.reason);
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", persons.get(0).getPo_num());
			responsejson.put("invoicenumber", persons.get(0).getInvoiceNumber());
			jsonArray.add(responsejson);
			return jsonArray;

		} catch (Exception e) {
			log.error("insertinvoice() 3 :", e.fillInStackTrace());
			con.rollback();
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", persons.get(0).getPo_num());
			responsejson.put("invoicenumber", persons.get(0).getInvoiceNumber());
			jsonArray.add(responsejson);
			return jsonArray;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	private int checkinvoiceingrntable(List<Invoicesubmission> persons, Connection con)
			throws SQLException, DXPortalException {

		String gettobeinvoicednumber = "Select count(*) as counter from GRNMAPPING where "
				+ "PONUMBER=? and DCNUMBER=? AND STATUS IS NULL";
		int count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(gettobeinvoicednumber);
			ps.setString(1, persons.get(0).getPo_num());
			ps.setString(2, persons.get(0).getDcnumber());
			rs = ps.executeQuery();

			rs.next();
			count = rs.getInt("counter");
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("checkinvoiceingrntable() :", e.fillInStackTrace());
			throw new DXPortalException("Error in To-Be-Invoice Submission !!", "SQL Error in checkinvoiceingrntable.");
		}
		return count;
	}

	public String updategrnstatus(List<Invoicesubmission> persons, String status, Connection con)
			throws SQLException, DXPortalException {

		String updategrn = "Update GRNMAPPING set STATUS=? where PONUMBER=? AND "
				+ "LINEITEMNO=? AND DCNUMBER=? AND GRNNUMBER=?";
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			for (int j = 0; j < persons.size(); j++) {
				ps = con.prepareStatement(updategrn);
				ps.setString(1, status);
				ps.setString(2, persons.get(j).getPo_num());
				ps.setString(3, persons.get(j).getLineItemNumber());
				ps.setString(4, persons.get(j).getDcnumber());
				ps.setString(5, persons.get(j).getGrnnumber());
				ps.executeUpdate();
				ps.close();
			}
		} catch (SQLException e) {
			log.error("updategrnstatus() :", e.fillInStackTrace());
			throw new DXPortalException("Error in invoice submission !!", "SQL Error in updategrnstatus.");

		}
		return "updated";
	}

	void updatedeliverysumary(String po_num, String lineItemNumber, String invoiceNumber2, String orderNumber,
			String invoicedate, String quantity, String totalamount, String uom, String rateperquantity,
			String lineitemtext, String invoicestatus, String invoiceamount, String storagelocation, String grnnumber,
			String uniquereferencenumber, String saplineitemnumber, String servicenumber, Connection con,
			String srcnnumber) throws SQLException, DXPortalException {

		try {
			PreparedStatement ps = null;
			ResultSet rs = null;
			String upgetseq1 = "update deliverysummary set invoiceNumber= ?,INVOICEDATE=?,"
					+ "LINEITEMTOTALAMOUNT=?,LINEITEMTOTALQUANTITY=?,"
					+ "RATEPERQTY=?,UOM=?,LINEITEMTEXT=?,INVOICESTATUS=?,STORAGELOCATION=?,"
					+ "SERVICENUMBER=?,SCRNNUMBER=? where " + "ponumber= ? and DC= ? and LineItemNumber= ? ";
			String upgetseqwithacceptedquantity = "update deliverysummary set invoiceNumber= ?,INVOICEDATE=?,"
					+ "LINEITEMTOTALAMOUNT=?,LINEITEMTOTALQUANTITY=?,"
					+ "RATEPERQTY=?,UOM=?,LINEITEMTEXT=?,INVOICESTATUS=?,"
					+ "STORAGELOCATION=?,ACCEPTEDQTY=?,GRNNUMBER=?,SAPUNIQUEREFERENCENO=?,"
					+ "SAPLINEITEMNO=?,SCRNNUMBER=?,SERVICENUMBER=? where ponumber= ? and DC= ? and LineItemNumber= ? ";
			String fetchgrn = "select GRNNUMBER from PONINVOICESUMMERY  where " + "ponumber= ? and INVOICENUMBER= ? ";
			int count = 0;

			if ("".equalsIgnoreCase(invoiceamount) || invoiceamount == null) {
				invoiceamount = String.valueOf(Double.valueOf(quantity) * Double.valueOf(rateperquantity));
			}
			if (!("-").equalsIgnoreCase(grnnumber)) {
				ps = con.prepareStatement(upgetseqwithacceptedquantity);
				ps.setString(1, invoiceNumber2);
				ps.setDate(2, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoicedate).getTime()));
				ps.setString(3, invoiceamount);
				ps.setString(4, quantity);
				ps.setString(5, rateperquantity);
				ps.setString(6, uom);
				ps.setString(7, lineitemtext);
				ps.setString(8, invoicestatus);
				ps.setString(9, storagelocation);
				ps.setString(10, quantity);
				ps.setString(11, grnnumber);
				ps.setString(12, uniquereferencenumber);
				ps.setString(13, saplineitemnumber);
				ps.setString(14, srcnnumber);
				ps.setString(15, servicenumber);
				ps.setString(16, po_num);
				ps.setString(17, orderNumber);
				ps.setString(18, lineItemNumber);
				int rs1 = ps.executeUpdate();
				ps.close();

			} else {
				ps = con.prepareStatement(upgetseq1);
				ps.setString(1, invoiceNumber2);

				ps.setDate(2, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoicedate).getTime()));

				ps.setString(3, invoiceamount);
				ps.setString(4, quantity);
				ps.setString(5, rateperquantity);
				ps.setString(6, uom);
				ps.setString(7, lineitemtext);
				ps.setString(8, invoicestatus);
				ps.setString(9, storagelocation);
				ps.setString(10, servicenumber);
				ps.setString(11, srcnnumber);
				ps.setString(12, po_num);
				ps.setString(13, orderNumber);
				ps.setString(14, lineItemNumber);
				int rs1 = ps.executeUpdate();
				ps.close();
			}

			ps = con.prepareStatement(fetchgrn);
			ps.setString(1, po_num);
			ps.setString(2, invoiceNumber2);
			rs = ps.executeQuery();
			String grnnumber1 = "";
			while (rs.next()) {
				grnnumber1 = rs.getString("GRNNUMBER") == null ? "" : rs.getString("GRNNUMBER").toString();
			}
			responsejson.put("count", count);
			responsejson.put("messageforgrninsert", "Success");
		} catch (Exception e) {
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("message", "fail");
			log.error("updatedeliverysumary() :", e.fillInStackTrace());
			throw new DXPortalException("Error in Invoice Submission !!", "SQL Error in updatedeliverysumary.");
		}
	}

	int getUniquePONInCheck(String invoice, Connection con, String bid, String invoicedate)
			throws SQLException, DXPortalException {

		String uniquePoInCount = "Select count(*) as counter from poninvoicesummery where BUSINESSPARTNEROID = ? "
				+ "and lower(INVOICENUMBER) =lower(?)";
		PreparedStatement ps = null;
		ResultSet rs = null;
		int count = 0;
		try {
			ps = con.prepareStatement(uniquePoInCount);
			ps.setString(1, bid);
			ps.setString(2, invoice);
			rs = ps.executeQuery();
			while (rs.next()) {
				count = rs.getInt("counter");
			}
			rs.close();
			ps.close();
			responsejson.put("count", count);
			responsejson.put("message", "Success");

		} catch (Exception e) {
			log.error("getUniquePONInCheck() :", e.fillInStackTrace());
			count = 0;
			throw new DXPortalException("Error in Invoice Submission !!", "SQL Error in getUniquePONInCheck.");
		}
		return count;
	}

	public JSONArray nonposubmitinvoice(String invoiceNumber, String invoiceDate, String bid, String totalAmount,
			String description, String status, String invoiceamount, String stage, String useremail,
			String billofladingdate, String actualfilename, String savedfilename, String multipleactualfilename,
			String multiplesavedfilename, String irnNumber, String irnDate) throws SQLException {

		ArrayList<HashMap<String, String>> invoiceDataBidinvoceidIdList = new ArrayList<HashMap<String, String>>();

		boolean result;
		result = Validation.StringChecknull(invoiceNumber);
		if (result == false) {
			responsejson.put("Uniquemessage", "validation Fail");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.invoiceNumberCheck(invoiceNumber);
			if (result) {
				responsejson.put("Uniquemessage",
						"Invoice Number should not contain SPACES and special characters except forward slash (/) and hyphen (-)");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		result = Validation.StringChecknull(invoiceDate);
		if (result == false) {
			responsejson.put("Uniquemessage", "validation Fail");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(totalAmount);
		if (result == false) {
			responsejson.put("Uniquemessage", "validation Fail");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(totalAmount);
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			} else {
				if (Double.parseDouble(totalAmount) < 0) {
					responsejson.put("Uniquemessage", "validation Fail");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}

			}
		}

		result = Validation.StringChecknull(invoiceamount);
		if (result == false) {
			responsejson.put("Uniquemessage", "validation Fail");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(invoiceamount);
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			} else {
				if (Double.parseDouble(invoiceamount) < 0) {
					responsejson.put("Uniquemessage", "validation Fail");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}
			}
		}

		result = Validation.StringChecknull(useremail);
		if (result == false) {
			responsejson.put("Uniquemessage", "validation Fail");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.emailCheck(useremail);
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(bid);
		if (result == false) {
			responsejson.put("Uniquemessage", "validation Fail");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(bid);
			if (result == false) {
				responsejson.put("Uniquemessage", "validation Fail");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		if (irnNumber != null && !"".equals(irnNumber) && !"-".equals(irnNumber) && !"NA".equals(irnNumber)) {

			if (irnNumber.length() != 64) {
				responsejson.put("Uniquemessage",
						"The IRN no. consists of 64 characters and cannot have special characters.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			result = Validation.irnNumberCheck(irnNumber);
			if (result) {
				responsejson.put("Uniquemessage",
						"The IRN no. consists of 64 characters and cannot have special characters.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		if (actualfilename != null) {
			result = Validation.fileExtension(actualfilename);
			if (result) {
				responsejson.put("Uniquemessage", "Only pdf file extension is allowed.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		if (savedfilename != null) {
			result = Validation.fileExtension(savedfilename);
			if (result) {
				responsejson.put("Uniquemessage", "Only pdf file extension is allowed.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		if (multipleactualfilename != null) {
			result = Validation.multiFilesExtension(multipleactualfilename);
			if (result) {
				responsejson.put("Uniquemessage",
						"Only JPEG, JPG, PNG, DOC, DOCX, XLS, XLSX, CSV, PDF file extensions are allowed.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		if (multiplesavedfilename != null) {
			result = Validation.multiFilesExtension(multiplesavedfilename);
			if (result) {
				responsejson.put("Uniquemessage",
						"Only JPEG, JPG, PNG, DOC, DOCX, XLS, XLSX, CSV, PDF file extensions are allowed.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			String po_data = "SELECT * FROM INVOICEEVENTDETAILWOPO where INVOICENUMBER =? AND BUSSINESSPARTNEROID=? ";
			ps = con.prepareStatement(po_data);
			ps.setString(1, invoiceNumber);
			ps.setString(2, bid);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> invoiceData = new HashMap<String, String>();
				invoiceData.put("INVOICENUMBER", rs.getString("InvoiceNumber"));
				invoiceDataBidinvoceidIdList.add(invoiceData);
			}
			rs.close();
			ps.close();

			if (invoiceDataBidinvoceidIdList.size() > 0) {
				responsejson.put("message", "Fail");
				responsejson.put("Uniquemessage",
						"Invoice Number already present. Please try again using different invoice number.");
				responsejson.put("err",
						"Invoice Number already present. Please try again using different invoice number.");
				jsonArray.add(responsejson);
			} else {
				String sqlUpdate = "insert into INVOICEEVENTDETAILWOPO (INVOICENUMBER,INVOICEDATE,INVOICEAMOUNT,"
						+ "TOTALAMOUNT,STATUS,CREATEDON,BUSSINESSPARTNEROID,DESCRIPTION,USEREMAILID,"
						+ "BILLOFLADINGDATE,ACTUALFILENAME,SAVEDFILENAME,SUPPORTACTFILENAME,"
						+ "SUPPORTSAVEDFILENAME,IRNNUMBER,IRNDATE) " + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

				ps = con.prepareStatement(sqlUpdate);
				ps.setString(1, invoiceNumber);
				ps.setDate(2, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoiceDate).getTime()));
				ps.setString(3, invoiceamount);
				ps.setString(4, totalAmount);
				ps.setString(5, status);
				ps.setTimestamp(6, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(7, bid);
				ps.setString(8, description);
				ps.setString(9, useremail);
				if (!("Invalid date").equalsIgnoreCase(billofladingdate)) {
					ps.setDate(10,
							new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(billofladingdate).getTime()));
				} else {
					ps.setDate(10, null);
				}
				ps.setString(11, actualfilename);
				ps.setString(12, savedfilename);
				ps.setString(13, multipleactualfilename);
				ps.setString(14, multiplesavedfilename);
				ps.setString(15, irnNumber);
				if (irnDate != null && !irnDate.equals("") && irnDate != "null") {
					ps.setDate(16, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(irnDate).getTime()));
				} else {
					ps.setString(16, null);
				}
				ps.executeUpdate();
				ps.close();
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
				con.commit();
			}
		} catch (Exception e) {
			log.error("nonposubmitinvoice() :", e.fillInStackTrace());
			con.rollback();
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray logout(HttpServletResponse response, HttpServletRequest httpRequest) {
		Cookie[] cookies;
		HttpSession session = httpRequest.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		session = null;

		responsejson.put("status", "success");
		responsejson.put("data", "true");
		responsejson.put("message", "");
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray insertbulkinvoice(String fileName) throws SQLException {

		InputStream input = EmailImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		try {
			prop.load(input);
			String path = prop.getProperty("fileLocation");
			String bussinesspartneroid = "";
			String[] filename = fileName.split("_");
			String timestamp = filename[filename.length - 1];
			bussinesspartneroid = filename[1];
			int iend = timestamp.indexOf(".");

			if (iend != -1) {
				timestamp = timestamp.substring(0, iend);
			}
			String finalpath = path + "bulkupload//" + bussinesspartneroid + "//" + timestamp + "//" + fileName;

			File file = new File(finalpath);
			FileReader filereader = new FileReader(file);
			String tempsourceDir = finalpath;
			CSVReader csvReader = new CSVReader(filereader);
			String[] header = csvReader.readNext();
			String[] expectedheader = { "PONUMBER", "INVOICENUMBER", "INVOICEDATE",
					"INVOICETYPE(STANDARD=1 and EINVOICE=2)", "LINEITEMNUMBER", "LINEITEMQUANTITY", "TOTALAMOUNT",
					"DESCRIPTION", "LINEITEMINVOICEAMOUNT", "FILENAME" };
			/*
			 * for (int a = 0; a < header.length; a++) { log.info("headers are here ==>" +
			 * header[a]); }
			 */ // log.info("headers here " + header.toString());
			if (checkEquality(header, expectedheader)) {
				CsvToBean csv = new CsvToBean();
				try {
					InputStream targetStream = new FileInputStream(file);
					filereader = new FileReader(file);
				} catch (FileNotFoundException e) {
					log.error("insertbulkinvoice() 1 :", e.fillInStackTrace());
				}
				csvReader = new CSVReader(filereader, ',', '\'', 1);
				List list = csv.parse(setColumMapping(), csvReader);
				List<Invoicesubmission> correctinvoiceList = new ArrayList<Invoicesubmission>();
				List<Bulkfailbean> failinginvoiceList = new ArrayList<Bulkfailbean>();
				String sqlgetallinvoice = "Select * from PONINVOICESUMMERY where INVOICENUMBER=?"
						+ " AND PONUMBER=? AND BUSINESSPARTNEROID=?";
				Connection con = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					con = DBConnection.getConnection();
					ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();

					for (Object object : list) {
						Invoicesubmission aglcode = (Invoicesubmission) object;
						Bulkfailbean o = new Bulkfailbean();
						aglcode.setStage("1");
						aglcode.setIrnDate("");
						if (isNull(aglcode.getInvoiceDate()) || isNull(aglcode.getPo_num())
								|| isNull(aglcode.getTotalAmount()) || isNull(aglcode.getLineItemNumber())) {
							o.setInvoicenumber(aglcode.getInvoiceNumber());
							o.setPonumber(aglcode.getPo_num());
							o.setMessage("All the neccesary fields are not available");
							failinginvoiceList.add(o);
						} else if (isNull(aglcode.getInvoiceNumber())) {
							o.setInvoicenumber(aglcode.getInvoiceNumber());
							o.setPonumber(aglcode.getPo_num());
							o.setMessage("Invoice Number field is not present ");
							failinginvoiceList.add(o);
						} else {
							rs = null;
							ps = con.prepareStatement(sqlgetallinvoice);
							ps.setString(1, aglcode.getInvoiceNumber());
							ps.setString(2, aglcode.getPo_num());
							ps.setString(3, aglcode.getBid());
							rs = ps.executeQuery();
							if (!rs.isBeforeFirst()) {
								correctinvoiceList.add(aglcode);
							} else {
								o.setInvoicenumber(aglcode.getInvoiceNumber());
								o.setPonumber(aglcode.getPo_num());
								o.setMessage("Invoice number already present in database");
								failinginvoiceList.add(o);
							}
						}
					}
				} catch (Exception e) {
					log.error("insertbulkinvoice() 2 : ", e.fillInStackTrace());
				} finally {
					DBConnection.closeConnection(rs, ps, con);
				}

				if (correctinvoiceList.size() > 0) {
					for (int j = 0; j < correctinvoiceList.size(); j++) {
						Bulkfailbean o = new Bulkfailbean();
						List<Invoicesubmission> sub = new ArrayList<Invoicesubmission>();
						sub.add(correctinvoiceList.get(j));
						try {

							JSONArray jsonArray2 = insertinvoice(sub, null);

							JSONObject jo = (JSONObject) jsonArray2.get(0);
							if (jo.get("message").toString().equalsIgnoreCase("fail")) {
								o.setInvoicenumber(sub.get(0).getInvoiceNumber());
								o.setPonumber(sub.get(0).getPo_num());
								o.setMessage(jo.get("error").toString());
								failinginvoiceList.add(o);
								for (int k = 0; k < correctinvoiceList.size(); k++) {
									Invoicesubmission invsub = correctinvoiceList.get(k);
									if (invsub.getInvoiceNumber().equalsIgnoreCase(sub.get(0).getInvoiceNumber())) {
										correctinvoiceList.remove(k);
									}
								}
							}
						} catch (Exception e) {
							log.error("insertbulkinvoice() 3 :", e.fillInStackTrace());
						}
					}
				}
				responsejson.put("errorrecordscount", failinginvoiceList.size());
				responsejson.put("correctrecordscount", correctinvoiceList.size());
				responsejson.put("errorrecords", failinginvoiceList);
				responsejson.put("correctrecord", correctinvoiceList);
				responsejson.put("flag", "success");
				responsejson.put("message", "uploaded successfully");
			} else {
				responsejson.put("status", "fail");
				responsejson.put("message", "Kindly upload the right file.");
			}
			jsonArray.add(responsejson);
		} catch (IOException e) {
			log.error("insertbulkinvoice() 4 :", e.fillInStackTrace());
		}
		String tempsourceDir = prop.getProperty("fileLocation");

		return jsonArray;
	}

	public static boolean checkEquality(String[] s1, String[] s2) {
		if (s1 == s2)
			return true;

		if (s1 == null || s2 == null)
			return false;

		int n = s1.length;
		if (n != s2.length)
			return false;

		for (int i = 0; i < n; i++) {
			if (!s1[i].equalsIgnoreCase(s2[i]))
				return false;
		}

		return true;
	}

	public static ColumnPositionMappingStrategy setColumMapping() {
		ColumnPositionMappingStrategy strategy = new ColumnPositionMappingStrategy();
		strategy.setType(Invoicesubmission.class);
		String[] columns = new String[] { "po_num", "invoiceNumber", "invoiceDate", "invoicetype", "lineItemNumber",
				"quantity", "totalAmount", "description", "invoiceamount", "actualfilename", "billofladingdate",
				"remark", "multipleactualfilename" };
		strategy.setColumnMapping(columns);
		return strategy;
	}

	public static boolean isNull(String s) {
		boolean status = true;
		if (s != null && !s.trim().isEmpty()) {
			status = false;
		}
		return status;
	}

	public JSONArray getPOCreateDeliverynew(List<PoeventdetailsPojo> createdelivery) throws SQLException {

		String sqlUpdate = "insert into poeventdetails (PONumber,BusinessPartnerOID,LineItemNumber,LineItemText,Company,Plant,Department,CostCentre,Category,BusinessPartnerText,Quantity"
				+ ",UnitOfMeasure,ContactPersonEmailID,ContactPersonPhone,deliveryAddress1,City,State,Country,PinCode,iGSTAmount,cGSTAmount,sgstAmount,OrderNumber,Remark,DeliveryDate,CreatedOn,MATERIAL_TYPE,Status,"
				+ "Rateperqty,Balance_qty) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		int deliveryuni = 0;
		deliveryuni = getUniqueDeliverySummary(createdelivery.get(0).getOrdernumber(),
				createdelivery.get(0).getPonumber());
		if (deliveryuni > 0) {
			responsejson.put("Uniquemessage", "Ordernumber is already present");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		boolean result;
		result = createdelivery.isEmpty();
		if (result == true) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		for (int i = 0; i < createdelivery.size(); i++) {
			result = Validation.StringChecknull(createdelivery.get(i).getPonumber());
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(createdelivery.get(i).getDeliverydate());
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(createdelivery.get(i).getOrdernumber());
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(createdelivery.get(i).getLineitemnumber());
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			result = Validation.StringChecknull(createdelivery.get(i).getQuantity());
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			} else {
				result = Validation.numberCheck(createdelivery.get(i).getQuantity());
				if (result == false) {
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} else {
					if (Double.parseDouble(createdelivery.get(i).getQuantity()) < 0) {
						responsejson.put("validation", "validation Fail");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						return jsonArray;
					}
				}
			}

			result = Validation.StringChecknull(createdelivery.get(i).getBusinesspartneroid());
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			} else {
				result = Validation.numberCheck(createdelivery.get(i).getBusinesspartneroid());
				if (result == false) {
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}
			}

		}

		String deliveryDate = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			for (int i = 0; i < createdelivery.size(); i++) {
				int deliveryuni1 = 0;
				deliveryuni1 = getUniqueDeliverySummary(createdelivery.get(i).getOrdernumber(),
						createdelivery.get(i).getPonumber());
				if (deliveryuni > 0) {
					responsejson.put("Uniquemessage", "Ordernumber is already present");
					responsejson.put("message", "Fail");
					return jsonArray;
				}
				log.info("Double.parseDouble(createdelivery.get(i).getQuantity()) "
						+ Double.parseDouble(createdelivery.get(i).getQuantity()));
				if (Double.parseDouble(createdelivery.get(i).getQuantity()) > 0.0) {
					String po_status = "insert into deliverysummary (DeliveryUniqueNo,dc,dispatchDate,PONumber,"
							+ "LineItemNumber,Quantity,BUSSINESSPARTNEROID) values (DELIVERYSUMMARY_SEQ.NEXTVAL,?,?,?,?,?,?)";

					con = DBConnection.getConnection();
					ps = con.prepareStatement(po_status);
					ps.setString(1, createdelivery.get(i).getOrdernumber());
					ps.setString(2, createdelivery.get(i).getDeliverydate());
					ps.setString(3, createdelivery.get(i).getPonumber());
					ps.setString(4, createdelivery.get(i).getLineitemnumber());
					ps.setString(5, createdelivery.get(i).getQuantity());
					ps.setString(6, createdelivery.get(i).getBusinesspartneroid());
					ps.executeUpdate();
					ps.close();

					responsejson.put("message", "Success");
					jsonArray.add(responsejson);
				} else {
					responsejson.put("Uniquemessage", "Quantity cannot be 0");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
				}
			}
		} catch (SQLException e) {
			log.error("getPOCreateDeliverynew() :", e.fillInStackTrace());
			responsejson.put("message", "fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	String deliverysummaryInsert(String po_num, String lineItemNumber, String invoiceNumber, String quantity,
			String bussinesspartneroid, String invoicedate) throws SQLException, DXPortalException {

		String sqlIdentifier = "select DELIVERYSUMMARY_SEQ.NEXTVAL from dual";
		String po_status = "insert into deliverysummary (DeliveryUniqueNo,invoicenumber,dispatchDate,ponumber,LineItemNumber,Quantity,BUSSINESSPARTNEROID,INVOICEDATE)"
				+ " values (?,?,?,?,?,?,?,?)";
		String upgetseq = "";
		if (invoiceNumber == null) {
			upgetseq = "update deliverysummary set dc= ? where "
					+ "ponumber= ? and LineItemNumber= ? and  DeliveryUniqueNo=?";
		} else {
			upgetseq = "update deliverysummary set dc= ? where "
					+ "ponumber= ? and invoiceNumber= ? and LineItemNumber= ? and  DeliveryUniqueNo=?";
		}

		long myId = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(sqlIdentifier);
			rs = ps.executeQuery();
			if (rs.next()) {
				myId = rs.getLong(1);
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement(po_status);
			ps.setString(1, String.valueOf(myId));
			ps.setString(2, invoiceNumber);
			Date date = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			String strDate = formatter.format(date);
			ps.setString(3, strDate);
			ps.setString(4, po_num);
			ps.setString(5, lineItemNumber);
			ps.setString(6, quantity);
			ps.setString(7, bussinesspartneroid);
			if (invoicedate.equalsIgnoreCase("")) {
				ps.setDate(8, null);
			} else {
				ps.setDate(8, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoicedate).getTime()));
			}
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement(upgetseq);
			if (invoiceNumber == null) {
				ps.setString(1, "DCN" + myId);
				ps.setString(2, po_num);
				ps.setString(3, lineItemNumber);
				ps.setString(4, String.valueOf(myId));
			} else {
				ps.setString(1, "DCN" + myId);
				ps.setString(2, po_num);
				ps.setString(3, invoiceNumber);
				ps.setString(4, lineItemNumber);
				ps.setString(5, String.valueOf(myId));
			}

			ps.executeUpdate();
			ps.close();
			con.commit();
			return "DCN" + myId;
		} catch (Exception e) {
			log.error("deliverysummaryInsert() :", e.fillInStackTrace());
			con.rollback();
			throw new DXPortalException("Error while creating delivery", "Error in deliverysummaryInsert()");
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

	}

	double getBalanceCount(String po_num, String lineItemNumber, String bid, Connection con)
			throws SQLException, DXPortalException {

		String balancecount = "select balance_qty from poeventdetails where BusinessPartnerOID = ? and "
				+ "PONumber= ? and LineItemNumber= ? and OrderNumber is null";
		PreparedStatement ps = null;
		ResultSet rs = null;
		String bCount = "";
		try {
			ps = con.prepareStatement(balancecount);
			ps.setString(1, bid);
			ps.setString(2, po_num);
			ps.setString(3, lineItemNumber);
			rs = ps.executeQuery();
			while (rs.next()) {
				bCount = rs.getString("balance_qty");
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getBalanceCount() :", e.fillInStackTrace());
			throw new DXPortalException("Error in Invoice Submission !!", "SQL Error in getBalanceCount.");

		}
		return Double.parseDouble(bCount);
	}

	void getUpdateinvoiceeventdetailwopo(String invoice, String bid, String po_num, Connection con)
			throws SQLException, DXPortalException {

		String balancecount = "select count(*) as counter from invoiceeventdetailwopo where INVOICENUMBER= ? and BUSSINESSPARTNEROID=? and PONUMBER is NULL";
		String upatepoevent = "UPDATE INVOICEEVENTDETAILWOPO set PONUMBER=?,POINVOICENUMBER=?,STATUS=? where INVOICENUMBER =? AND BUSSINESSPARTNEROID =? and PONUMBER is NULL";

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int bCount = 0;
			ps = con.prepareStatement(balancecount);
			ps.setString(1, invoice);
			ps.setString(2, bid);
			rs = ps.executeQuery();
			while (rs.next()) {
				bCount = rs.getInt("counter");
			}
			if (bCount > 0) {
				ps = con.prepareStatement(upatepoevent);
				ps.setString(1, po_num);
				ps.setString(2, invoice);
				ps.setString(3, "A");
				ps.setString(4, invoice);
				ps.setString(5, bid);
				ps.executeUpdate();
				ps.close();
			}
		} catch (Exception e) {
			log.error("getUpdateinvoiceeventdetailwopo() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			throw new DXPortalException("Error in Invoice Submission without PO !!",
					"SQL Error in getUpdateinvoiceeventdetailwopo.");
		}
	}

	public JSONArray getorderitems(String bid, String po_num, String lineitemnumber) throws SQLException {

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

		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String orderitemquery = "";
		if (bid.equalsIgnoreCase("0")) {
			orderitemquery = "select * from DELIVERYSUMMARY where LINEITEMNUMBER= ? and PONUMBER=?";

		} else if (lineitemnumber == null) {
			orderitemquery = "select * from DELIVERYSUMMARY where BUSSINESSPARTNEROID=? and PONUMBER=?";
		} else {
			orderitemquery = "select * from DELIVERYSUMMARY where LINEITEMNUMBER= ? and BUSSINESSPARTNEROID=? and PONUMBER=?";

		}
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int bCount = 0;
			con = DBConnection.getConnection();
			ps = con.prepareStatement(orderitemquery);
			if (bid.equalsIgnoreCase("0")) {
				ps.setString(1, lineitemnumber);
				ps.setString(2, po_num);
			} else if (lineitemnumber == null) {
				ps.setString(1, bid);
				ps.setString(2, po_num);
			} else {
				ps.setString(1, lineitemnumber);
				ps.setString(2, bid);
				ps.setString(3, po_num);
			}
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("DELIVERYUNIQUENO", rs.getString("DELIVERYUNIQUENO"));
				poData.put("DC", rs.getString("DC"));
				poData.put("DISPATCHDATE", new SimpleDateFormat("dd-MMM-yyyy")
						.format(new SimpleDateFormat("dd/MM/yyyy").parse(rs.getString("DISPATCHDATE"))));
				poData.put("PONUMBER", rs.getString("PONUMBER"));
				poData.put("LINEITEMNUMBER", rs.getString("LINEITEMNUMBER"));
				poData.put("QUANTITY", rs.getString("QUANTITY"));
				poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				poData.put("GRNNUMBER", rs.getString("GRNNUMBER"));
				poData.put("BUSSINESSPARTNEROID", rs.getString("BUSSINESSPARTNEROID"));
				String t = (String) (rs.getString("INVOICEDATE") == null ? "" : rs.getString("INVOICEDATE"));
				if (t != "") {
					poData.put("INVOICEDATE",
							new SimpleDateFormat("dd-MMM-yyyy").format(rs.getTimestamp("INVOICEDATE")) == null ? ""
									: new SimpleDateFormat("dd-MMM-yyyy").format(rs.getTimestamp("INVOICEDATE")));
				} else {
					poData.put("INVOICEDATE", null);
				}
				POList.add(poData);
			}
			rs.close();
			ps.close();

			if (POList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("orderitems", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getorderitems() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	private int getUniqueDeliverySummary(String ordernumber, String po_num) throws SQLException {

		String uniquePoInCount = "Select count(*) as counter from deliverysummary where DC = ? and PONUMBER =? ";
		int count = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(uniquePoInCount);
			ps.setString(1, ordernumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				count = rs.getInt("counter");
			}
			rs.close();
			ps.close();
			responsejson.put("count", count);
			responsejson.put("message", "Success");
		} catch (Exception e) {
			log.error("getUniqueDeliverySummary() :", e.fillInStackTrace());
			count = 0;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
			return count;
		}
	}

	private int getUniqueInvoiceEventDetailsCheck(String invoice, String po_num, Connection con) throws SQLException {

		String uniquePoInCount = "Select count(*) as counter from PONINVOICESUMMERY where INVOICENUMBER = ?"
				+ " and PONUMBER = ? ";

		int count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(uniquePoInCount);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				count = rs.getInt("counter");
			}
			responsejson.put("count", count);
			responsejson.put("message", "Success");

		} catch (Exception e) {
			con.rollback();
			log.error("getUniqueInvoiceEventDetailsCheck() :", e.fillInStackTrace());
			count = 0;
			return count;
		}
		return count;
	}

	public JSONArray createcustomdeliveryitems(String ponumber, List<String> lineitemnumber, String invoicenumber,
			List<String> quantity, String id, String invoicedate) {

		String result1 = "";
		boolean result;
		result = Validation.StringChecknull(ponumber);

		if (result == false) {
			responsejson.put("text", "Delivery improper");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "ponumber is null");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		if (lineitemnumber.size() == 0) {
			responsejson.put("text", "Delivery improper");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "lineitemnumber is null");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		if (quantity.size() == 0) {
			responsejson.put("text", "Delivery improper");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "quantity size is null");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		List<String> autodcnvalues = new ArrayList<String>();
		for (int a = 0; a < lineitemnumber.size(); a++) {
			try {
				log.info("Double.parseDouble(createdelivery.get(i).getQuantity()) "
						+ Double.parseDouble(quantity.get(a)));
				result1 = deliverysummaryInsert(ponumber, lineitemnumber.get(a), invoicenumber, quantity.get(a), id,
						invoicedate);
				autodcnvalues.add(result1);

			} catch (DXPortalException dxp) {
				dxp.printStackTrace();
				responsejson.put("error", dxp.reason);
			} catch (Exception e) {
				log.error("createcustomdeliveryitems() :", e.fillInStackTrace());
				responsejson.put("text", "Delivery improper");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}
		if (!autodcnvalues.isEmpty()) {
			responsejson.put("text", "Delivery done");
			responsejson.put("message", "Success");
			responsejson.put("dcnvalues", autodcnvalues);
			jsonArray.add(responsejson);
			return jsonArray;
		}
		return jsonArray;
	}

	public JSONArray getorderforfullpo(String ponumber, String bid) {

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
		return jsonArray;
	}

	public JSONArray getwithoutpodetails(String bid, String invoicenumber) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			List<HashMap<String, String>> wopodetails = new ArrayList<HashMap<String, String>>();

			String po_data = "SELECT * FROM INVOICEEVENTDETAILWOPO "
					+ "where INVOICENUMBER =? AND BUSSINESSPARTNEROID=? ";

			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, invoicenumber);
			ps.setString(2, bid);
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> invoiceData = new HashMap<String, String>();
				invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				invoiceData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
				invoiceData.put("INVOICEAMOUNT", rs.getString("INVOICEAMOUNT"));
				invoiceData.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
				invoiceData.put("STATUS", rs.getString("STATUS"));
				invoiceData.put("CREATEDON", rs.getString("CREATEDON"));
				invoiceData.put("BUSSINESSPARTNEROID", rs.getString("BUSSINESSPARTNEROID"));
				invoiceData.put("PONUMBER", rs.getString("PONUMBER"));
				invoiceData.put("DESCRIPTION", rs.getString("DESCRIPTION"));
				invoiceData.put("USEREMAILID", rs.getString("USEREMAILID"));
				invoiceData.put("POINVOICENUMBER", rs.getString("POINVOICENUMBER"));
				invoiceData.put("BILLOFLADINGDATE", rs.getString("BILLOFLADINGDATE"));
				invoiceData.put("IRNNUMBER", rs.getString("IRNNUMBER"));
				invoiceData.put("IRNDATE", rs.getString("IRNDATE"));
				invoiceData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
				invoiceData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
				invoiceData.put("SUPPORTACTFILENAME", rs.getString("SUPPORTACTFILENAME"));
				invoiceData.put("SUPPORTSAVEDFILENAME", rs.getString("SUPPORTSAVEDFILENAME"));
				invoiceData.put("IRNNUMBER", rs.getString("IRNNUMBER"));
				invoiceData.put("IRNDATE", rs.getString("IRNDATE"));
				wopodetails.add(invoiceData);
			}
			rs.close();
			ps.close();
			if (wopodetails.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("poData", wopodetails);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No data found");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getwithoutpodetails() :", e.fillInStackTrace());
			responsejson.put("validation", e.getLocalizedMessage());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getorderhavingdcn(String bid, String po_num, List<String> dcnvalues) {

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

		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String orderitemquery = "select * from DELIVERYSUMMARY where BUSSINESSPARTNEROID=? and PONUMBER=? and DC=?";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int bCount = 0;
			con = DBConnection.getConnection();
			for (int i = 0; i < dcnvalues.size(); i++) {
				HashMap<String, String> poData = new HashMap<String, String>();
				ps = con.prepareStatement(orderitemquery);
				ps.setString(1, bid);
				ps.setString(2, po_num);
				ps.setString(3, dcnvalues.get(i));

				rs = ps.executeQuery();

				while (rs.next()) {

					poData.put("DELIVERYUNIQUENO", rs.getString("DELIVERYUNIQUENO"));
					poData.put("DC", rs.getString("DC"));
					poData.put("DISPATCHDATE", new SimpleDateFormat("dd-MMM-yyyy")
							.format(new SimpleDateFormat("dd/MM/yyyy").parse(rs.getString("DISPATCHDATE"))));
					poData.put("PONUMBER", rs.getString("PONUMBER"));
					poData.put("LINEITEMNUMBER", rs.getString("LINEITEMNUMBER"));
					poData.put("QUANTITY", rs.getString("QUANTITY"));
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("GRNNUMBER", rs.getString("GRNNUMBER"));
					poData.put("BUSSINESSPARTNEROID", rs.getString("BUSSINESSPARTNEROID"));
					String t = (String) (rs.getString("INVOICEDATE") == null ? "" : rs.getString("INVOICEDATE"));
					if (t != "") {
						poData.put("INVOICEDATE",
								new SimpleDateFormat("dd-MMM-yyyy").format(rs.getTimestamp("INVOICEDATE")) == null ? ""
										: new SimpleDateFormat("dd-MMM-yyyy").format(rs.getTimestamp("INVOICEDATE")));
					} else {
						poData.put("INVOICEDATE", null);
					}

				}
				rs.close();
				ps.close();
				POList.add(poData);
			}
			if (POList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("orderitems", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getorderhavingdcn() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);

		}
		return jsonArray;

	}

	public JSONArray getgrnbasedonpo(String id, String ponumber) throws SQLException {

		String getgrnbasedonpo = "select distinct DCNUMBER from grnmapping where ponumber =? and STATUS IS NULL ";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int bCount = 0;
			con = DBConnection.getConnection();

			ps = con.prepareStatement(getgrnbasedonpo);
			ps.setString(1, ponumber);
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("DCNUMBER", rs.getString("DCNUMBER"));
				POList.add(poData);
			}
			rs.close();
			ps.close();
			if (POList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("grnbasedonpo", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getgrnbasedonpo() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getgrnbasedoninvoiceandpo(String id, String ponumber, List<String> dcnumber) {

		String getgrnbasedonpo = "select * from grnmapping where ponumber = ? and dcnumber = ? AND STATUS IS NULL";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int bCount = 0;
			con = DBConnection.getConnection();
			for (int a = 0; a < dcnumber.size(); a++) {
				ps = con.prepareStatement(getgrnbasedonpo);
				ps.setString(1, ponumber);
				ps.setString(2, dcnumber.get(a));
				rs = ps.executeQuery();

				while (rs.next()) {
					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PONUMBER", rs.getString("GRNNUMBER"));
					poData.put("GRNNUMBER", rs.getString("GRNNUMBER"));
					poData.put("GRNQTY", rs.getString("GRNQTY"));
					poData.put("LINEITEMNO", rs.getString("LINEITEMNO"));
					poData.put("DCNUMBER", rs.getString("DCNUMBER"));
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("STATUS", rs.getString("STATUS"));
					poData.put("CREATEDON", rs.getString("CREATEDON"));
					poData.put("RATEPERQTY", rs.getString("RATEPERQTY"));
					poData.put("AMOUNT", rs.getString("AMOUNT"));
					poData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					poData.put("SAPUNIQUEREFERENCENO", rs.getString("SAPUNIQUEREFERENCENO"));
					poData.put("SAPLINEITEMNO", rs.getString("SAPLINEITEMNO"));
					poData.put("SRCNUMBER", rs.getString("SRCNO"));
					poData.put("SERVICENUMBER", rs.getString("SERVICENO"));
					poData.put("DCNUMBER", rs.getString("DCNUMBER"));
					POList.add(poData);
				}
			}

			rs.close();
			ps.close();
			if (POList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("grnbasedonpoandinvoice", POList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getgrnbasedoninvoiceandpo() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	// insert bulk invoices

	public JSONArray insertbulk(String filenameofbulk, String username) {

		log.info("filenameofbulk ==> " + filenameofbulk);
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		JSONArray ja = new JSONArray();
		JSONObject jobj = new JSONObject();

		try {
			prop.load(input);
			String path = prop.getProperty("fileLocation");
			log.info("filename is here " + filenameofbulk);
			String[] filename = filenameofbulk.split("_");
			String timestamp = filename[filename.length - 1];
			int iend = timestamp.indexOf(".");
			String bussid = filename[1];
			String type = filename[0];
			if (iend != -1) {
				timestamp = timestamp.substring(0, iend);
			}
			String finalpath = path + "bulkupload//" + bussid + "//" + timestamp + "//" + filenameofbulk;
			File file = new File(finalpath);
			FileReader filereader = new FileReader(file);
			String tempsourceDir = finalpath;
			CSVReader csvReader = new CSVReader(filereader);
			String[] header = csvReader.readNext();
			String[] expectedheader = { "PONUMBER", "INVOICENUMBER", "INVOICEDATE",
					"INVOICETYPE(STANDARD=1 and EINVOICE=2)", "LINEITEMNUMBER", "LINEITEMQUANTITY", "TOTALAMOUNT",
					"DESCRIPTION", "LINEITEMINVOICEAMOUNT", "FILENAME", "BILLOFLADINGDATE", "REMARKS",
					"SUPPORTING FILES" };
			/*
			 * for (int a = 0; a < header.length; a++) { log.info("headers are here ==>" +
			 * header[a]); }
			 */
			log.info("headers here " + header.toString());

			if (checkEquality(header, expectedheader)) {

				CsvToBean csv = new CsvToBean();
				try {
					InputStream targetStream = new FileInputStream(file);
					filereader = new FileReader(file);
				} catch (FileNotFoundException e) {
					log.error("insertbulk() 1 : ", e.fillInStackTrace());
				}

				csvReader = new CSVReader(filereader, ',', '\'', 1);
				List list = csv.parse(setColumMapping(), csvReader);// contains all data in list
				List<Invoicesubmission> correctinvoiceList = new ArrayList<Invoicesubmission>();
				List<Bulkfailbean> failinginvoiceList = new ArrayList<Bulkfailbean>();
				csvReader.close();
				filereader.close();
				int successcount = 0;
				Connection con = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					con = DBConnection.getConnection();
					ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
					String invoice = "";
					String purchaseorder = "";
					for (Object object : list) {

						Bulkfailbean o = new Bulkfailbean();
						Invoicesubmission aglcode = (Invoicesubmission) object;
						log.info("invoice ==>" + invoice);
						log.info("aglcode.getInvoiceNumber() ==>" + aglcode.getInvoiceNumber());
						log.info("purchaseorder ==>" + purchaseorder);
						log.info("aglcode.getPo_num() ==>" + aglcode.getPo_num());
						if (!(invoice.equalsIgnoreCase(aglcode.getInvoiceNumber())
								&& purchaseorder.equalsIgnoreCase(aglcode.getPo_num()))) {
							aglcode.setStage("1");
							aglcode.setIrnDate("");
							aglcode.setBid(bussid);
							aglcode.setGrnnumber("-");
							aglcode.setStatus("P");
							aglcode.setUniquereferencenumber("-");
							aglcode.setSaplineitemnumber("-");
							aglcode.setMultipleactualfilename(aglcode.getMultipleactualfilename().replace(":", ","));
							if (aglcode.getBillofladingdate().equalsIgnoreCase(null)
									|| aglcode.getBillofladingdate().equalsIgnoreCase("")
									|| aglcode.getBillofladingdate().equalsIgnoreCase("null")) {
								aglcode.setBillofladingdate("Invalid date");
							}
							aglcode.setCreatedby(username); // will come from front end
							aglcode.setBeforesubmissioninvoicenumber("");
							if (isNull(aglcode.getInvoiceDate()) || isNull(aglcode.getPo_num())
									|| isNull(aglcode.getTotalAmount()) || isNull(aglcode.getLineItemNumber())
									|| isNull(aglcode.getDescription()) || isNull(aglcode.getRemark())
									|| isNull(aglcode.getActualfilename()) || isNull(aglcode.getInvoiceamount())
									|| isNull(aglcode.getQuantity())) {
								o.setInvoicenumber(aglcode.getInvoiceNumber());
								o.setPonumber(aglcode.getPo_num());
								o.setBussinesspartneroid(aglcode.getBid());
								o.setMessage("All the neccesary fields are not available");
								failinginvoiceList.add(o);
								invoice = aglcode.getInvoiceNumber();
								purchaseorder = aglcode.getPo_num();
							} else if (isNull(aglcode.getInvoiceNumber())) {
								o.setInvoicenumber(aglcode.getInvoiceNumber());
								o.setPonumber(aglcode.getPo_num());
								o.setBussinesspartneroid(aglcode.getBid());
								o.setMessage("Invoice Number field is not present ");
								failinginvoiceList.add(o);
								invoice = aglcode.getInvoiceNumber();
								purchaseorder = aglcode.getPo_num();
							} else if (ispresent(aglcode.getActualfilename(), filenameofbulk)) {
								o.setInvoicenumber(aglcode.getInvoiceNumber());
								o.setPonumber(aglcode.getPo_num());
								o.setBussinesspartneroid(aglcode.getBid());
								o.setMessage("Invoice file not uploaded ");
								failinginvoiceList.add(o);
								invoice = aglcode.getInvoiceNumber();
								purchaseorder = aglcode.getPo_num();
							} else if (issupportpresent(aglcode.getMultipleactualfilename(), filenameofbulk)) {
								o.setInvoicenumber(aglcode.getInvoiceNumber());
								o.setPonumber(aglcode.getPo_num());
								o.setBussinesspartneroid(aglcode.getBid());
								o.setMessage("Supporting file not uploaded ");
								failinginvoiceList.add(o);
								invoice = aglcode.getInvoiceNumber();
								purchaseorder = aglcode.getPo_num();
							} else if (grnpresent(aglcode.getDcnumber(), aglcode.getPo_num())) {
								o.setInvoicenumber(aglcode.getInvoiceNumber());
								o.setPonumber(aglcode.getPo_num());
								o.setBussinesspartneroid(aglcode.getBid());
								o.setMessage(
										"GRN already present for following invoice. Please proceed to 'to be invoiced' for po "
												+ aglcode.getPo_num());
								failinginvoiceList.add(o);
								invoice = aglcode.getInvoiceNumber();
								purchaseorder = aglcode.getPo_num();
							} else {
								correctinvoiceList.add(aglcode);
							}
						}

					}

					for (int z = 0; z < failinginvoiceList.size(); z++) {
						for (int correct = 0; correct < correctinvoiceList.size(); correct++) {
							Invoicesubmission is = correctinvoiceList.get(correct);
							if (failinginvoiceList.get(z).getInvoicenumber().equalsIgnoreCase(is.getInvoiceNumber())
									&& failinginvoiceList.get(z).getPonumber().equalsIgnoreCase(is.getPo_num())) {
								correctinvoiceList.remove(is);
								correct--;
							}
						}
					}
					log.info("correctinvoiceList is here ==>" + correctinvoiceList.toString());
					log.info("failinginvoiceList is here ==>" + failinginvoiceList.toString());
					log.info("failinginvoiceList size is here ==>" + failinginvoiceList.size());
					log.info("correctinvoiceList size is here ==>" + correctinvoiceList.size());

					HashMap<String, HashMap<String, String>> hashmap2 = new HashMap<String, HashMap<String, String>>();
					String sqlgetalleventdetails = "Select pd.PONUMBER,ped.CATEGORY,ped.COSTCENTRE,ped.LINEITEMNUMBER,"
							+ "ped.LINEITEMTEXT,ped.COMPANY,ped.PLANT,ped.DEPARTMENT,bp.BUSINESSPARTNERTEXT,"
							+ "ped.CONTACTPERSONEMAILID,ped.CONTACTPERSONPHONE,ped.MATERIAL_TYPE,"
							+ "ped.STORAGELOCATION,ped.RATEPERQTY,ped.UNITOFMEASURE,ped.BALANCE_QTY,ped.SERVICE,"
							+ "pd.VENDORID,pd.CONTACTPERSONEMAILID,pd.REQUSITIONER,ped.BUSINESSPARTNEROID,"
							+ "pd.POTYPE from PODETAILS pd join POEVENTDETAILS ped on ped.PONUMBER = pd.PONUMBER "
							+ "and ped.BUSINESSPARTNEROID = pd.BUSINESSPARTNEROID join BUSINESSPARTNER bp "
							+ "on bp.BUSINESSPARTNEROID = pd.BUSINESSPARTNEROID where ped.BUSINESSPARTNEROID=?";
					ps = con.prepareStatement(sqlgetalleventdetails);
					ps.setString(1, bussid);

					rs = ps.executeQuery();
					List<List<String>> podatalistforquantitycheck = new ArrayList<List<String>>();
					while (rs.next()) {
						List<String> poeventdata = new ArrayList<String>();
						poeventdata.add(rs.getString("PONUMBER")); // 0
						poeventdata.add(rs.getString("CATEGORY"));// 1
						poeventdata.add(rs.getString("COSTCENTRE"));// 2
						poeventdata.add(rs.getString("LINEITEMNUMBER"));// 3
						poeventdata.add(rs.getString("LINEITEMTEXT"));// 4
						poeventdata.add(rs.getString("COMPANY"));// 5
						poeventdata.add(rs.getString("PLANT"));// 6
						poeventdata.add(rs.getString("DEPARTMENT"));// 7
						poeventdata.add(rs.getString("BUSINESSPARTNERTEXT"));// 8
						poeventdata.add(rs.getString("CONTACTPERSONEMAILID"));// 9
						poeventdata.add(rs.getString("CONTACTPERSONPHONE")); // 10
						poeventdata.add(rs.getString("MATERIAL_TYPE"));// 11
						poeventdata.add(rs.getString("STORAGELOCATION"));// 12
						poeventdata.add(rs.getString("RATEPERQTY"));// 13
						poeventdata.add(rs.getString("BALANCE_QTY"));// 14
						poeventdata.add(rs.getString("SERVICE"));// 15
						poeventdata.add(rs.getString("VENDORID"));// 16
						poeventdata.add(rs.getString("CONTACTPERSONEMAILID"));// 17
						poeventdata.add(rs.getString("REQUSITIONER"));// 18
						poeventdata.add(rs.getString("BUSINESSPARTNEROID")); // 19
						poeventdata.add(rs.getString("UNITOFMEASURE"));// 20
						podatalistforquantitycheck.add(poeventdata);
					}
					rs.close();
					ps.close();
					Set<String> st = new HashSet<String>();
					if (correctinvoiceList.size() > 0) {
						for (int x = 0; x < correctinvoiceList.size(); x++) {

							for (int y = 0; y < podatalistforquantitycheck.size(); y++) {
								if (correctinvoiceList.get(x).getPo_num()
										.equalsIgnoreCase(podatalistforquantitycheck.get(y).get(0))
										&& correctinvoiceList.get(x).getBid()
												.equalsIgnoreCase(podatalistforquantitycheck.get(y).get(19))
										&& correctinvoiceList.get(x).getLineItemNumber()
												.equalsIgnoreCase(podatalistforquantitycheck.get(y).get(3))) {

									correctinvoiceList.get(x).setVendorID(podatalistforquantitycheck.get(y).get(16));
									correctinvoiceList.get(x)
											.setBusinessPartnerText(podatalistforquantitycheck.get(y).get(8));
									correctinvoiceList.get(x).setBuyerid(podatalistforquantitycheck.get(y).get(9));
									correctinvoiceList.get(x).setCategory(podatalistforquantitycheck.get(y).get(1));
									correctinvoiceList.get(x).setCompany(podatalistforquantitycheck.get(y).get(5));
									correctinvoiceList.get(x)
											.setRateperquantity(podatalistforquantitycheck.get(y).get(13));
									correctinvoiceList.get(x)
											.setContactPerson(podatalistforquantitycheck.get(y).get(18));
									correctinvoiceList.get(x).setCostCentre(podatalistforquantitycheck.get(y).get(2));
									correctinvoiceList.get(x)
											.setContactPersonPhone(podatalistforquantitycheck.get(y).get(10));
									correctinvoiceList.get(x).setLineitemtext(podatalistforquantitycheck.get(y).get(4));
									correctinvoiceList.get(x).setMaterial(podatalistforquantitycheck.get(y).get(11));
									correctinvoiceList.get(x).setPlant(podatalistforquantitycheck.get(y).get(6));
									correctinvoiceList.get(x)
											.setServicenumber(podatalistforquantitycheck.get(y).get(15));
									correctinvoiceList.get(x)
											.setStoragelocation(podatalistforquantitycheck.get(y).get(12));
									correctinvoiceList.get(x).setuOM(podatalistforquantitycheck.get(y).get(20));
									correctinvoiceList.get(x).setBalance_qty(podatalistforquantitycheck.get(y).get(14));
									correctinvoiceList.get(x).setSavedfilename(type + "_" + bussid + "_"
											+ correctinvoiceList.get(x).getActualfilename().substring(0,
													correctinvoiceList.get(x).getActualfilename().indexOf("."))
											+ "_" + timestamp + "."
											+ correctinvoiceList.get(x).getActualfilename().substring(
													correctinvoiceList.get(x).getActualfilename().lastIndexOf(".")
															+ 1));
									correctinvoiceList.get(x)
											.setMultiplesavedfilename(getmultiplesavedfilename(
													correctinvoiceList.get(x).getMultipleactualfilename(), type, bussid,
													timestamp));
								}

							}
							st.add(correctinvoiceList.get(x).getInvoiceNumber() + "_"
									+ correctinvoiceList.get(x).getPo_num());
						}
					}
					HashMap<String, List<Invoicesubmission>> listofuniqueinvoicenumber = new HashMap<String, List<Invoicesubmission>>();
					for (String invpo : st) {
						List<Invoicesubmission> li = new ArrayList<Invoicesubmission>();
						double totalinvoiceamount = 0.0;
						double taxamount = 0.0;
						String[] invoiceponumber = invpo.split("_");
						String inv = invoiceponumber[0];
						String po = invoiceponumber[1];
						for (int k = 0; k < correctinvoiceList.size(); k++) {
							if (inv.equalsIgnoreCase(correctinvoiceList.get(k).getInvoiceNumber())
									&& po.equalsIgnoreCase(correctinvoiceList.get(k).getPo_num())) {

								totalinvoiceamount = totalinvoiceamount
										+ Double.parseDouble(correctinvoiceList.get(k).getInvoiceamount());
								taxamount = Double.parseDouble(correctinvoiceList.get(k).getTotalAmount())
										- totalinvoiceamount;
							}
						}
						for (int b = 0; b < correctinvoiceList.size(); b++) {
							if (inv.equalsIgnoreCase(correctinvoiceList.get(b).getInvoiceNumber())
									&& po.equalsIgnoreCase(correctinvoiceList.get(b).getPo_num())) {
								correctinvoiceList.get(b).setTaxamount(String.valueOf(taxamount));
							}
						}

						for (int uniquehashlist = 0; uniquehashlist < correctinvoiceList.size(); uniquehashlist++) {
							if (inv.equalsIgnoreCase(correctinvoiceList.get(uniquehashlist).getInvoiceNumber())
									&& po.equalsIgnoreCase(correctinvoiceList.get(uniquehashlist).getPo_num())) {
								li.add(correctinvoiceList.get(uniquehashlist));
							}
						}
						listofuniqueinvoicenumber.put(invpo, li);
					}

					if (listofuniqueinvoicenumber.size() > 0) {

						int count = 0;

						for (Map.Entry<String, List<Invoicesubmission>> set : listofuniqueinvoicenumber.entrySet()) { // main
																														// loop
							try {
								Bulkfailbean o = new Bulkfailbean();
								count++;
								List<Invoicesubmission> tocreatedelivery = set.getValue();
								List<String> quantitylist = new ArrayList<String>();
								List<String> lineitemnumberlist = new ArrayList<String>();
								List<String> autodcnbulkvalues = new ArrayList<String>();
								ArrayList<HashMap<String, String>> hashofordernumberlist = new ArrayList<HashMap<String, String>>();
								for (int z = 0; z < tocreatedelivery.size(); z++) {
									quantitylist.add(tocreatedelivery.get(z).getQuantity());
									lineitemnumberlist.add(tocreatedelivery.get(z).getLineItemNumber());
								}
								JSONArray jsonArray = createcustomdeliveryitems(tocreatedelivery.get(0).getPo_num(),
										lineitemnumberlist, tocreatedelivery.get(0).getInvoiceNumber(), quantitylist,
										tocreatedelivery.get(0).getBid(), tocreatedelivery.get(0).getInvoiceDate());
								JSONObject jsonobject = (JSONObject) jsonArray.get(0);
								if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
									autodcnbulkvalues = (List<String>) jsonobject.get("dcnvalues");
									JSONArray jsonArraygetorder = getorderhavingdcn(tocreatedelivery.get(0).getBid(),
											tocreatedelivery.get(0).getPo_num(), autodcnbulkvalues);
									JSONObject jsonobjectgetorder = (JSONObject) jsonArraygetorder.get(0);
									if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
										hashofordernumberlist = (ArrayList<HashMap<String, String>>) jsonobject
												.get("orderitems");
										for (int setorder = 0; setorder < tocreatedelivery.size(); setorder++) {
											for (int hashsetorder = 0; hashsetorder < hashofordernumberlist
													.size(); hashsetorder++) {
												if (tocreatedelivery.get(setorder).getLineItemNumber()
														.equalsIgnoreCase(hashofordernumberlist.get(hashsetorder)
																.get("LINEITEMNUMBER"))) {
													tocreatedelivery.get(setorder).setOrderNumber(
															hashofordernumberlist.get(hashsetorder).get("DC"));
													tocreatedelivery.get(setorder).setOrderNumber(
															hashofordernumberlist.get(hashsetorder).get("DC"));
												}
											}
										}

										JSONArray jsonArrayinsertinvoice = insertinvoice(tocreatedelivery, null);
										log.info("json value ==>" + jsonArrayinsertinvoice.get(0).toString());
										JSONObject joinsertinvoice = (JSONObject) jsonArrayinsertinvoice.get(0);
										if (joinsertinvoice.get("message").toString().equalsIgnoreCase("Success")) {
											successcount++;

										} else if (joinsertinvoice.get("message").toString().equalsIgnoreCase("Fail")) {
											o.setInvoicenumber(tocreatedelivery.get(0).getInvoiceNumber());
											o.setPonumber(tocreatedelivery.get(0).getPo_num());
											o.setBussinesspartneroid(tocreatedelivery.get(0).getBid());
											o.setMessage(joinsertinvoice.get("Uniquemessage").toString());
											failinginvoiceList.add(o);
											for (int k = 0; k < correctinvoiceList.size(); k++) {
												Invoicesubmission invsub = correctinvoiceList.get(k);
												if (invsub.getInvoiceNumber()
														.equalsIgnoreCase(tocreatedelivery.get(0).getInvoiceNumber())
														&& invsub.getPo_num().equalsIgnoreCase(
																tocreatedelivery.get(0).getPo_num())) {
													correctinvoiceList.remove(k);
													k--;
												}
											}
										} else {
											o.setInvoicenumber(tocreatedelivery.get(0).getInvoiceNumber());
											o.setPonumber(tocreatedelivery.get(0).getPo_num());
											o.setBussinesspartneroid(tocreatedelivery.get(0).getBid());
											o.setMessage(joinsertinvoice.get("message").toString());
											failinginvoiceList.add(o);
											for (int k = 0; k < correctinvoiceList.size(); k++) {
												Invoicesubmission invsub = correctinvoiceList.get(k);
												if (invsub.getInvoiceNumber()
														.equalsIgnoreCase(tocreatedelivery.get(0).getInvoiceNumber())
														&& invsub.getPo_num().equalsIgnoreCase(
																tocreatedelivery.get(0).getPo_num())) {
													correctinvoiceList.remove(k);
													k--;
												}
											}
										}
									}
								}
								quantitylist.clear();
								quantitylist = null;
								lineitemnumberlist.clear();
								lineitemnumberlist = null;
								autodcnbulkvalues.clear();
								autodcnbulkvalues = null;
							} catch (Exception e) {
								log.error("insertbulk() 1 : ", e.fillInStackTrace());
							}
						} // main loop close

					}
					listofuniqueinvoicenumber.clear();
					listofuniqueinvoicenumber = null;

				} catch (Exception e) {
					log.error("insertbulk() 2 : ", e.fillInStackTrace());
				} finally {
					DBConnection.closeConnection(rs, ps, con);
				}
				jobj.put("errorrecordscount", failinginvoiceList.size());
				jobj.put("correctrecordscount", successcount);
				jobj.put("errorrecords", failinginvoiceList);
				jobj.put("correctrecord", correctinvoiceList);
				jobj.put("flag", "success");
				jobj.put("message", "uploaded successfully");
			} else {
				jobj.put("flag", "fail");
				jobj.put("message", "Kindly upload the right file.");
			}

			ja.add(jobj);
		} catch (Exception e) {
			log.error("insertbulk() 3 : ", e.fillInStackTrace());

		}
		return ja;
	}

	public boolean issupportpresent(String multiplesupportfilename, String csvfilename) {

		boolean status = true;
		if (!multiplesupportfilename.equalsIgnoreCase(null) && !multiplesupportfilename.equalsIgnoreCase("")
				&& !multiplesupportfilename.equalsIgnoreCase("null")) {

			InputStream input = POImpl.class.getResourceAsStream("/dxproperties.properties");
			String[] multiplesuppactualfilename = multiplesupportfilename.split(",");
			for (int b = 0; b < multiplesuppactualfilename.length; b++) {
				String mainfile = multiplesuppactualfilename[b].substring(0,
						multiplesuppactualfilename[b].indexOf("."));

				Properties prop = new Properties();
				try {
					prop.load(input);
				} catch (IOException e) {
					log.error("issupportpresent() :", e.fillInStackTrace());
				}
				String path = prop.getProperty("fileLocation");
				String[] csvname = csvfilename.split("_");
				String timestamp = csvname[csvname.length - 1];
				int iend = timestamp.indexOf(".");

				if (iend != -1) {
					timestamp = timestamp.substring(0, iend);
				}
				String extension = multiplesuppactualfilename[b]
						.substring(multiplesuppactualfilename[b].lastIndexOf(".") + 1);
				String fullfilename = csvname[0] + "_" + csvname[1] + "_" + mainfile + "_" + timestamp + "."
						+ extension;
				String fullfilename1 = csvname[0] + "_" + csvname[1] + "_" + mainfile + "_" + timestamp + "."
						+ extension.toUpperCase();
				String fullpath = path + csvname[0] + "//" + csvname[1] + "//" + timestamp + "//" + fullfilename;
				String fullpath1 = path + csvname[0] + "//" + csvname[1] + "//" + timestamp + "//" + fullfilename1;
				File file = new File(fullpath);
				if (!file.exists()) {
					File file1 = new File(fullpath1);
					if (!file1.exists()) {
						status = true;
						break;
					} else {
						status = false;
					}
				} else {
					status = false;
				}

			}
		} else {
			status = false;
		}
		return status;
	}

	public boolean ispresent(String filename, String csvfilename) {

		boolean status = true;
		if (!("".equalsIgnoreCase(filename) || "null".equalsIgnoreCase(filename) || filename.equalsIgnoreCase(null))) {

			String mainfile = filename.substring(0, filename.indexOf("."));
			InputStream input = POImpl.class.getResourceAsStream("/dxproperties.properties");
			Properties prop = new Properties();
			try {
				prop.load(input);
			} catch (IOException e) {
				log.error("ispresent() 1 : ", e.fillInStackTrace());

			}
			String path = prop.getProperty("fileLocation");
			String[] csvname = csvfilename.split("_");
			String timestamp = csvname[csvname.length - 1];
			int iend = timestamp.indexOf(".");

			if (iend != -1) {
				timestamp = timestamp.substring(0, iend);
			}
			String fullfilename = csvname[0] + "_" + csvname[1] + "_" + mainfile + "_" + timestamp + ".pdf";
			String fullfilename1 = csvname[0] + "_" + csvname[1] + "_" + mainfile + "_" + timestamp + ".PDF";
			String fullpath = path + csvname[0] + "//" + csvname[1] + "//" + timestamp + "//" + fullfilename;
			String fullpath1 = path + csvname[0] + "//" + csvname[1] + "//" + timestamp + "//" + fullfilename1;
			File file = new File(fullpath);
			if (!file.exists()) {
				File file1 = new File(fullpath1);
				if (!file1.exists()) {
					status = true;
				} else {
					status = false;
				}
			} else {
				status = false;
			}
		}

		return status;
	}

	public boolean grnpresent(String dcnumber, String ponumber) {
		boolean status = false;

		String gettobeinvoicednumber = "Select count(*) as counter from GRNMAPPING where "
				+ "PONUMBER=? and DCNUMBER=? AND STATUS IS NULL";
		int count = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(gettobeinvoicednumber);
			ps.setString(1, ponumber);
			ps.setString(2, dcnumber);
			rs = ps.executeQuery();

			rs.next();
			count = rs.getInt("counter");
			rs.close();
			ps.close();
			if (count > 0) {
				status = true;
			} else {
				status = false;
			}
		} catch (Exception e) {
			log.error("grnpresent() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return status;
	}

	public String getmultiplesavedfilename(String multipleactualfilename, String type, String bussid,
			String timestamp) {
		if (!multipleactualfilename.equalsIgnoreCase(null) && !multipleactualfilename.equalsIgnoreCase("")
				&& !multipleactualfilename.equalsIgnoreCase("null")) {
			String[] multiplesuppactualfilename = multipleactualfilename.split(",");
			StringBuffer sb = new StringBuffer();
			for (int b = 0; b < multiplesuppactualfilename.length; b++) {

				sb.append(type + "_" + bussid + "_"
						+ multiplesuppactualfilename[b].substring(0, multiplesuppactualfilename[b].indexOf(".")) + "_"
						+ timestamp + "."
						+ multiplesuppactualfilename[b].substring(multiplesuppactualfilename[b].lastIndexOf(".") + 1));
				if (b != multiplesuppactualfilename.length - 1) {
					sb.append(",");
				}
			}

			return sb.toString();
		} else {
			return null;
		}

	}

	public void deletebaddeliveries(List<Invoicesubmission> persons) {

		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			con = DBConnection.getConnection();
			String deletebaddelivery = "DELETE FROM DELIVERYSUMMARY WHERE  PONUMBER=? and DC=?";
			for (int value = 0; value < persons.size(); value++) {
				ps = con.prepareStatement(deletebaddelivery);
				ps.setString(1, persons.get(value).getPo_num());
				ps.setString(2, persons.get(value).getOrderNumber());
				ps.executeUpdate();
				ps.close();
			}

		} catch (Exception e) {
			log.error("deletebaddeliveries() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
	}

	public void deletebaddeliveries(List<Invoicesubmission> persons, Connection con) throws SQLException {
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			String deletebaddelivery = "DELETE FROM DELIVERYSUMMARY WHERE  PONUMBER=? and DC=?";
			for (int value = 0; value < persons.size(); value++) {
				ps = con.prepareStatement(deletebaddelivery);
				ps.setString(1, persons.get(value).getPo_num());
				ps.setString(2, persons.get(value).getOrderNumber());
				ps.executeUpdate();
				ps.close();
			}
			con.commit();

		} catch (Exception e) {
			con.rollback();
			log.error("deletebaddeliveries() :", e.fillInStackTrace());

		}
	}

	public JSONArray deleteemptydeliveries(List<String> dcnumbers, String ponumber) {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			con = DBConnection.getConnection();
			String deletebaddelivery = "DELETE FROM DELIVERYSUMMARY WHERE  PONUMBER=? and DC=?";
			for (int value = 0; value < dcnumbers.size(); value++) {
				ps = con.prepareStatement(deletebaddelivery);
				ps.setString(1, ponumber);
				ps.setString(2, dcnumbers.get(value));
				ps.executeUpdate();
				ps.close();
			}
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("deleteemptydeliveries() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPOS(String bid, String mode, String ponum, String fromdateofduration, String todateofduration,
			String fromdateofpo, String todateofpo) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		try {
			con = DBConnection.getConnection();
			List<String> podata = new ArrayList<>();

			String subquery = "";
			ArrayList<String> param = new ArrayList<String>();
			String po_data = null;

			if ("ALL".equalsIgnoreCase(mode)) {

				param.add(bid);
				param.add("N");

				if (!"NA".equalsIgnoreCase(ponum)) {
					String po = " AND PONUMBER = ?";
					subquery = subquery + po;
					param.add(ponum);
				}

				if ((!"NA".equalsIgnoreCase(fromdateofduration))
						&& (!"Invalid date".equalsIgnoreCase(fromdateofduration))
						&& (!"NA".equalsIgnoreCase(todateofduration))
						&& (!"Invalid date".equalsIgnoreCase(todateofduration))) {
					String in = " AND PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + in;
					param.add(fromdateofduration);
					param.add(todateofduration);
				}

				if ((!"NA".equalsIgnoreCase(fromdateofpo)) && (!"Invalid date".equalsIgnoreCase(fromdateofpo))
						&& (!"NA".equalsIgnoreCase(todateofpo)) && (!"Invalid date".equalsIgnoreCase(todateofpo))) {
					String dt = " AND PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
					param.add(fromdateofpo);
					param.add(todateofpo);
				}

				param.add(bid);
				param.add("N");

				if (!"NA".equalsIgnoreCase(ponum)) {
					param.add(ponum);
				}
				if ((!"NA".equalsIgnoreCase(fromdateofduration))
						&& (!"Invalid date".equalsIgnoreCase(fromdateofduration))
						&& (!"NA".equalsIgnoreCase(todateofduration))
						&& (!"Invalid date".equalsIgnoreCase(todateofduration))) {
					param.add(fromdateofduration);
					param.add(todateofduration);
				}
				if ((!"NA".equalsIgnoreCase(fromdateofpo)) && (!"Invalid date".equalsIgnoreCase(fromdateofpo))
						&& (!"NA".equalsIgnoreCase(todateofpo)) && (!"Invalid date".equalsIgnoreCase(todateofpo))) {
					param.add(fromdateofpo);
					param.add(todateofpo);
				}
				po_data = "select * from ("
						+ "select PONUMBER,CREATEDON from podetails where BusinessPartnerOID=? and Status=? " + subquery
						+ " " + "Union "
						+ "select PONUMBER,CREATEDON from podetails where BusinessPartnerOID=? and Status <> ? "
						+ subquery + " " + ") order by CREATEDON desc";
			} else {

				param.add(bid);

				if ("SP".equalsIgnoreCase(mode)) {
					String poType = " and POTYPE = ?";
					subquery = subquery + poType;
					param.add("S");
				} else {
					String poStatus = " and Status = ?";
					subquery = subquery + poStatus;
					param.add(mode);
				}
				if (!"NA".equalsIgnoreCase(ponum)) {
					String po = " AND PONUMBER = ?";
					subquery = subquery + po;
					param.add(ponum);
				}

				if ((!"NA".equalsIgnoreCase(fromdateofduration))
						&& (!"Invalid date".equalsIgnoreCase(fromdateofduration))
						&& (!"NA".equalsIgnoreCase(todateofduration))
						&& (!"Invalid date".equalsIgnoreCase(todateofduration))) {
					String in = " AND PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + in;
					param.add(fromdateofduration);
					param.add(todateofduration);
				}

				if ((!"NA".equalsIgnoreCase(fromdateofpo)) && (!"Invalid date".equalsIgnoreCase(fromdateofpo))
						&& (!"NA".equalsIgnoreCase(todateofpo)) && (!"Invalid date".equalsIgnoreCase(todateofpo))) {
					String dt = " AND PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
					param.add(fromdateofpo);
					param.add(todateofpo);
				}

				po_data = "select PONUMBER from podetails where BusinessPartnerOID= ? " + subquery + " ";
			}

			Pagination pg = new Pagination(po_data, 0);
			pages = pg.getPages(con, param);
			rs = pg.execute(con, param);

			while (rs.next()) {
				podata.add(rs.getString("PONumber"));
			}
			pg.close();
			rs.close();
			pg = null;

			if (podata.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("podata", podata);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("getPOS() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getPOInvoiceSearchData(String id, String poInvNumber) {

		boolean result;
		result = Validation.StringChecknull(id);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.numberCheck(id);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(poInvNumber);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String queryPoNIn = null;
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> INVOICEList = new ArrayList<HashMap<String, String>>();

		String searchInvPOData = "Select count(*) as counter from PODETAILS where BusinessPartnerOID =? and PONUMBER LIKE ? ";
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(searchInvPOData);
			ps.setString(1, id);
			ps.setString(2, "%" + poInvNumber + "%");
			rs = ps.executeQuery();
			int count = 0;
			if (rs.next()) {
				count = rs.getInt("counter");

				if (count == 0) {// invoice table

					queryPoNIn = "select INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,MESSAGE,REQUSITIONER,BUYER,CREATEDON,MACOUNT,HOLDCOUNT,"
							+ "OVERALLSTATUS,INVOICEDATE,MATERIAL_TYPE,ONEXSTATUS,PAYMENTAMOUNT,ACTUALFILENAME,SAVEDFILENAME,PGQ,PLANT,"
							+ "IRNNUMBER,DESCRIPTION,CREATEDBY,BUSINESSPARTNERTEXT,VENDORID,BILLOFLADINGDATE,CONTACTPERSON,CONTACTPERSONPHONE,"
							+ "DEPARTMENT,ENDUSERSUPPACTUALFILE,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,GRNNUMBER,ENDUSERREMARKS,"
							+ "REMARK,ENDUSERSUPPSAVEDFILE,PAYMENTDOCNUMBER,UTRCHEQUENUMBER,UTRDATE,GSTHOLDAMT,GSTPAIDAMOUNT,UNIQUEREFERENCENUMBER,"
							+ "SCRNNUMBER,IRNDATE,AMOUNT,TOTALAMOUNT from PONINVOICESUMMERY where BusinessPartnerOID =? and INVOICENUMBER LIKE ? ";

					ps = con.prepareStatement(queryPoNIn);
					ps.setString(1, id);
					ps.setString(2, "%" + poInvNumber + "%");

					rs = ps.executeQuery();
					while (rs.next()) {

						HashMap<String, String> invoiceData = new HashMap<String, String>();
						invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
						invoiceData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
						invoiceData.put("PO_NUMBER", rs.getString("PONUMBER"));
						invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
						invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
						invoiceData.put("VENDORID", rs.getString("VENDORID"));
						invoiceData.put("PLANT", rs.getString("PLANT"));
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
						INVOICEList.add(invoiceData);
					}

				} else {

					queryPoNIn = "select PONUMBER,PODATE,COMPANY,PLANT,DEPARTMENT,COSTCENTRE,CATEGORY,BUSINESSPARTNEROID,BUSINESSPARTNERTEXT,"
							+ "VENDORID,CREATEDDATE,STARTDATE,ENDDATE,QUANTITY,LINEITEMNUMBER,LINEITEMTEXT,UNITOFMEASURE,IGSTAMOUNT,CGSTAMOUNT,"
							+ "SGSTAMOUNT,CONTACTPERSONEMAILID,CONTACTPERSONPHONE,CITY,STATE,COUNTRY,PINCODE,BUYER,REQUSITIONER,CREATEDBY,CREATEDON,STATUS,"
							+ "PURCHASINGORGANISATION,PURCHASINGGROUP,COMPANYCODE,QUOTATIONNO,QUOTATIONDATE,MATERIAL_TYPE,POTYPE,POAMOUNT  "
							+ "from PODETAILS where BusinessPartnerOID =? and PONUMBER LIKE ? ";

					ps = con.prepareStatement(queryPoNIn);
					ps.setString(1, id);
					ps.setString(2, "%" + poInvNumber + "%");

					rs = ps.executeQuery();
					while (rs.next()) {

						HashMap<String, String> poData = new HashMap<String, String>();
						poData.put("PO_NUMBER", rs.getString("PONUMBER"));
						poData.put("DATE", rs.getString("PODATE"));
						poData.put("AMOUNT", rs.getString("POAMOUNT"));
						poData.put("STATUS", rs.getString("STATUS"));
						poData.put("Quantity", rs.getString("QUANTITY"));
						poData.put("COMPANY", rs.getString("COMPANY"));
						poData.put("PLANT", rs.getString("PLANT"));
						poData.put("DEPARTMENT", rs.getString("DEPARTMENT"));
						poData.put("COSTCENTRE", rs.getString("COSTCENTRE"));
						poData.put("CATEGORY", rs.getString("CATEGORY"));
						poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
						poData.put("BUSINESSPARTNERTEXT", rs.getString("BUSINESSPARTNERTEXT"));
						poData.put("VENDORID", rs.getString("VENDORID"));
						poData.put("CREATEDDATE", rs.getString("CREATEDDATE"));
						poData.put("STARTDATE", rs.getString("STARTDATE"));
						poData.put("ENDDATE", rs.getString("ENDDATE"));
						poData.put("LINEITEMNUMBER", rs.getString("LINEITEMNUMBER"));
						poData.put("LINEITEMTEXT", rs.getString("LINEITEMTEXT"));
						poData.put("UNITOFMEASURE", rs.getString("UNITOFMEASURE"));
						poData.put("IGSTAMOUNT", rs.getString("IGSTAMOUNT"));
						poData.put("CGSTAMOUNT", rs.getString("CGSTAMOUNT"));
						poData.put("SGSTAMOUNT", rs.getString("SGSTAMOUNT"));
						poData.put("CONTACTPERSONEMAILID", rs.getString("CONTACTPERSONEMAILID"));
						poData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
						poData.put("CITY", rs.getString("CITY"));
						poData.put("STATE", rs.getString("STATE"));
						poData.put("COUNTRY", rs.getString("COUNTRY"));
						poData.put("PINCODE", rs.getString("PINCODE"));
						poData.put("BUYER", rs.getString("BUYER"));
						poData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
						poData.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
						poData.put("POTYPE", rs.getString("POTYPE"));
						poData.put("CREATEDBY", rs.getString("CREATEDBY"));
						poData.put("CREATEDON", rs.getString("CREATEDON"));
						poData.put("PURCHASINGORGANISATION", rs.getString("PURCHASINGORGANISATION"));
						poData.put("PURCHASINGGROUP", rs.getString("PURCHASINGGROUP"));
						poData.put("COMPANYCODE", rs.getString("COMPANYCODE"));
						poData.put("QUOTATIONNO", rs.getString("QUOTATIONNO"));
						poData.put("QUOTATIONDATE", rs.getString("QUOTATIONDATE"));
						POList.add(poData);
					}

				}
			}
		} catch (Exception e) {
			responsejson.put("uniquemessage", "No Data Found for given data");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			log.error("getPOInvoiceSearchData() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0 || INVOICEList.size() > 0) {
			responsejson.put("message", "Success");
			responsejson.put("poData", POList);
			responsejson.put("invData", INVOICEList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "Fail");
			responsejson.put("message", "No Data Found for given data");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public String getPlantName(String plant, Connection con) {
		// Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		String qdata = "Select PLANTNAME from PLANTMASTER where PLANTCODE=?";
		String sb = "";
		try {
			// con = DBConnection.getConnection();
			ps = con.prepareStatement(qdata);
			ps.setString(1, plant);
			rs = ps.executeQuery();
			while (rs.next()) {
				sb = rs.getString("PLANTNAME");
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getPlantName() :", e.fillInStackTrace());

		} finally {
			// DBConnection.closeConnection(rs, ps, con);
		}
		return sb;
	}

	public JSONArray getPODetailsCountAsPerStatus(String bid, String status, int nPage, String ponumber,
			String fromdateofduration, String todateofduration, String fromdateofpo, String todateofpo, String plant,
			Connection con, PreparedStatement ps, ResultSet rs, String companyCode) throws SQLException {
		try {

			HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
			int allCounter = 0;

			String compCodeQuery = " AND pd.companycode = ? ";

			if (!"AS".equalsIgnoreCase(status)) {

				String po_data = "select pd.status ,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
						+ " on pd.PONUMBER = poe.PONUMBER where pd.BusinessPartnerOID=? " + compCodeQuery
						+ " group by pd.status";

				ps = con.prepareStatement(po_data);
				ps.setString(1, bid);
				ps.setString(2, companyCode);
				rs = ps.executeQuery();
				while (rs.next()) {
					String sts = rs.getString("status");
					String count = rs.getString("count");
					countAsPerStatus.put(sts, count);
					allCounter += Integer.parseInt(count);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				rs.close();
				ps.close();

				String po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
						+ " on pd.PONUMBER = poe.PONUMBER where pd.BusinessPartnerOID=? and pd.potype=?"
						+ compCodeQuery;

				ps = con.prepareStatement(po_data1);
				ps.setString(1, bid);
				ps.setString(2, "S");
				ps.setString(3, companyCode);
				rs = ps.executeQuery();
				while (rs.next()) {
					String count = rs.getString("count");
					countAsPerStatus.put("SP", count);
				}
				rs.close();
				ps.close();

			} else {
				String subquery = "";
				if (!"NA".equalsIgnoreCase(plant)) {
					String po = " AND poe.PLANT=?";
					subquery = subquery + po;
				}
				if (!"NA".equalsIgnoreCase(ponumber)) {
					String po = " AND pd.PONUMBER=?";
					subquery = subquery + po;

				}
				if ((!"NA".equalsIgnoreCase(fromdateofduration))
						&& (!"Invalid date".equalsIgnoreCase(fromdateofduration))) {
					String in = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + " AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + in;
				}
				if ((!"NA".equalsIgnoreCase(fromdateofpo)) && (!"Invalid date".equalsIgnoreCase(fromdateofpo))) {
					String dt = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + "AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
				}

				String po_data = "select pd.status ,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
						+ " on pd.PONUMBER = poe.PONUMBER where pd.BusinessPartnerOID=?" + compCodeQuery + subquery
						+ " group by pd.status";

				ps = con.prepareStatement(po_data);
				int queryCount = 1;
				ps.setString(queryCount, bid);

				queryCount++;
				ps.setString(queryCount, companyCode);

				if (!"NA".equalsIgnoreCase(plant)) {
					queryCount++;
					ps.setString(queryCount, plant);
				}
				if (!"NA".equalsIgnoreCase(ponumber)) {
					queryCount++;
					ps.setString(queryCount, ponumber);
				}
				if ((!"NA".equalsIgnoreCase(fromdateofduration))
						&& (!"Invalid date".equalsIgnoreCase(fromdateofduration))) {
					queryCount++;
					ps.setString(queryCount, fromdateofduration);
					queryCount++;
					ps.setString(queryCount, todateofduration);
				}
				if ((!"NA".equalsIgnoreCase(fromdateofpo)) && (!"Invalid date".equalsIgnoreCase(fromdateofpo))) {
					queryCount++;
					ps.setString(queryCount, fromdateofpo);
					queryCount++;
					ps.setString(queryCount, todateofpo);
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					String sts = rs.getString("status");
					String count = rs.getString("count");
					countAsPerStatus.put(sts, count);
					allCounter += Integer.parseInt(count);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				rs.close();
				ps.close();

				String po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
						+ " on pd.PONUMBER = poe.PONUMBER where pd.BusinessPartnerOID=? and pd.potype=?" + compCodeQuery
						+ subquery;

				ps = con.prepareStatement(po_data1);
				ps.setString(1, bid);
				ps.setString(2, "S");
				ps.setString(3, companyCode);

				queryCount = 3;
				ps.setString(queryCount, bid);
				if (!plant.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, plant);
				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, ponumber);
				}
				if ((!fromdateofduration.equalsIgnoreCase("NA"))
						&& (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
					queryCount++;
					ps.setString(queryCount, fromdateofduration);
					queryCount++;
					ps.setString(queryCount, todateofduration);
				}
				if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
					queryCount++;
					ps.setString(queryCount, fromdateofpo);
					queryCount++;
					ps.setString(queryCount, todateofpo);
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					String count = rs.getString("count");
					countAsPerStatus.put("SP", count);
				}
				rs.close();
				ps.close();
			}

			if (!countAsPerStatus.isEmpty()) {
				responsejson.put("poCountAsPerStatus", countAsPerStatus);
			}
		} catch (Exception e) {
			log.error("getPODetailsCountAsPerStatus() :", e.fillInStackTrace());

		}
		return null;
	}

	/*
	 * public JSONArray getPODetails(String bid, String mode, String ponum, String
	 * fromdateofduration, String todateofduration, String fromdateofpo, String
	 * todateofpo,String companyCode) throws SQLException { String poDataQuery =
	 * null; String poDateSubquery = "";
	 * 
	 * ArrayList<String> param = new ArrayList<String>();
	 * 
	 * String compCodeQuery=" AND A.companycode = ? ";
	 * 
	 * if ("ALL".equalsIgnoreCase(mode)) {
	 * 
	 * param.add(bid); param.add("N"); param.add(companyCode);
	 * 
	 * if (!"NA".equalsIgnoreCase(ponum)) { String po = " AND A.PONUMBER = ?";
	 * poDateSubquery = poDateSubquery + po; param.add(ponum); } if
	 * ((!"NA".equalsIgnoreCase(fromdateofduration)) &&
	 * (!"Invalid date".equalsIgnoreCase(fromdateofduration)) &&
	 * (!"NA".equalsIgnoreCase(todateofduration)) &&
	 * (!"Invalid date".equalsIgnoreCase(todateofduration))) { String in =
	 * " AND A.CREATEDON BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')"
	 * ; poDateSubquery = poDateSubquery + in; param.add(fromdateofduration);
	 * param.add(todateofduration); } if ((!"NA".equalsIgnoreCase(fromdateofpo)) &&
	 * (!"Invalid date".equalsIgnoreCase(fromdateofpo)) &&
	 * (!"NA".equalsIgnoreCase(todateofpo)) &&
	 * (!"Invalid date".equalsIgnoreCase(todateofpo))) { String dt =
	 * " AND A.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')"
	 * ; poDateSubquery = poDateSubquery + dt; param.add(fromdateofpo);
	 * param.add(todateofpo); } param.add(bid); param.add("N");
	 * param.add(companyCode);
	 * 
	 * if (!"NA".equalsIgnoreCase(ponum)) { param.add(ponum); } if
	 * ((!"NA".equalsIgnoreCase(fromdateofduration)) &&
	 * (!"Invalid date".equalsIgnoreCase(fromdateofduration)) &&
	 * (!"NA".equalsIgnoreCase(todateofduration)) &&
	 * (!"Invalid date".equalsIgnoreCase(todateofduration))) {
	 * param.add(fromdateofduration); param.add(todateofduration); } if
	 * ((!"NA".equalsIgnoreCase(fromdateofpo)) &&
	 * (!"Invalid date".equalsIgnoreCase(fromdateofpo)) &&
	 * (!"NA".equalsIgnoreCase(todateofpo)) &&
	 * (!"Invalid date".equalsIgnoreCase(todateofpo))) { param.add(fromdateofpo);
	 * param.add(todateofpo); }
	 * 
	 * poDataQuery = "select * from ( " +
	 * "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
	 * +
	 * "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE "
	 * + "FROM podetails A,poeventdetails B " +
	 * "WHERE A.BusinessPartnerOID=? AND A.BUSINESSPARTNEROID=B.BUSINESSPARTNEROID AND A.PONUMBER=B.PONUMBER and A.Status=? "
	 * +compCodeQuery + poDateSubquery + " " + "Union " +
	 * "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
	 * +
	 * "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
	 * + "FROM podetails A,poeventdetails B " +
	 * "WHERE A.BusinessPartnerOID=? AND A.BUSINESSPARTNEROID=B.BUSINESSPARTNEROID AND A.PONUMBER=B.PONUMBER and A.Status <> ? "
	 * +compCodeQuery + poDateSubquery + " " + ") ORDER BY 1,4";
	 * 
	 * } else {
	 * 
	 * param.add(bid); param.add(companyCode);
	 * 
	 * if ("SP".equalsIgnoreCase(mode)) { String poType = " and A.POTYPE = ?";
	 * poDateSubquery = poDateSubquery + poType; param.add("S"); } else { String
	 * poStatus = " and A.Status = ?"; poDateSubquery = poDateSubquery + poStatus;
	 * param.add(mode); }
	 * 
	 * if (!"NA".equalsIgnoreCase(ponum)) { String po = " AND A.PONUMBER = ?";
	 * poDateSubquery = poDateSubquery + po; param.add(ponum); }
	 * 
	 * if ((!"NA".equalsIgnoreCase(fromdateofduration)) &&
	 * (!"Invalid date".equalsIgnoreCase(fromdateofduration)) &&
	 * (!"NA".equalsIgnoreCase(todateofduration)) &&
	 * (!"Invalid date".equalsIgnoreCase(todateofduration))) { String in =
	 * " AND A.CREATEDON BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')"
	 * ; poDateSubquery = poDateSubquery + in; param.add(fromdateofduration);
	 * param.add(todateofduration); }
	 * 
	 * if ((!"NA".equalsIgnoreCase(fromdateofpo)) &&
	 * (!"Invalid date".equalsIgnoreCase(fromdateofpo)) &&
	 * (!"NA".equalsIgnoreCase(todateofpo)) &&
	 * (!"Invalid date".equalsIgnoreCase(todateofpo))) { String dt =
	 * " AND A.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')"
	 * ; poDateSubquery = poDateSubquery + dt; param.add(fromdateofpo);
	 * param.add(todateofpo); }
	 * 
	 * poDataQuery =
	 * "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
	 * +
	 * "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
	 * + "FROM podetails A,poeventdetails B " +
	 * "WHERE A.BusinessPartnerOID=? AND A.BUSINESSPARTNEROID=B.BUSINESSPARTNEROID AND A.PONUMBER=B.PONUMBER "
	 * +compCodeQuery + poDateSubquery + " " + "ORDER BY 1,4"; }
	 * 
	 * Connection con = null; PreparedStatement ps = null; ResultSet rs = null; try
	 * { con = DBConnection.getConnection(); Pagination pg = new
	 * Pagination(poDataQuery, 0); rs = pg.execute(con, param); List<List<String>>
	 * poDataList = new ArrayList<List<String>>();
	 * 
	 * while (rs.next()) {
	 * 
	 * List<String> podata = new ArrayList<String>();
	 * 
	 * podata.add(rs.getString("PLANT") == null ? "" : rs.getString("PLANT"));
	 * podata.add(rs.getString("PLANTNAME") == null ? "" :
	 * rs.getString("PLANTNAME")); podata.add(rs.getString("PODATE"));
	 * podata.add(rs.getString("PONUMBER"));
	 * podata.add(rs.getString("LINEITEMNUMBER"));
	 * podata.add(rs.getString("LINEITEMTEXT"));
	 * podata.add(rs.getString("POAMOUNT")); podata.add(rs.getString("RATEPERQTY"));
	 * podata.add(rs.getString("QUANTITY")); podata.add(rs.getString("NETVALUE"));
	 * podata.add(rs.getString("BALANCE_QTY"));
	 * podata.add(rs.getString("BALANCENETVALUE"));
	 * 
	 * if (rs.getString("Status").equalsIgnoreCase("A")) { podata.add("Accepted"); }
	 * else if (rs.getString("Status").equalsIgnoreCase("P")) {
	 * podata.add("Work In Progress"); } else if
	 * (rs.getString("Status").equalsIgnoreCase("S")) { podata.add("Shipped"); }
	 * else if (rs.getString("Status").equalsIgnoreCase("C")) {
	 * podata.add("Complete"); } else if
	 * (rs.getString("Status").equalsIgnoreCase("N")) { podata.add("New"); } else {
	 * podata.add(rs.getString("STATUS")); }
	 * 
	 * poDataList.add(podata); } pg.close(); rs.close(); pg = null;
	 * 
	 * String encodedfile = writeintoexcelfile(poDataList);
	 * 
	 * if (encodedfile.equalsIgnoreCase("")) { responsejson.put("message", "Fail");
	 * 
	 * } else { responsejson.put("message", "Success"); responsejson.put("data",
	 * encodedfile); } jsonArray.add(responsejson);
	 * 
	 * } catch (SQLException e) { log.error("getPODetails() :",
	 * e.fillInStackTrace());
	 * 
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); } finally {
	 * DBConnection.closeConnection(rs, ps, con); } return jsonArray; }
	 */

	public JSONArray insertinvoiceWithDC(List<Invoicesubmission> persons, String vendorEmail) throws SQLException {

		log.info("insertinvoiceWithDC " + vendorEmail);

		if (persons != null && !persons.isEmpty()) {

			if ("reopen".equalsIgnoreCase(persons.get(0).getType())) {

				log.info("REOPEN API SUBMISSION ");

				SimpoImpl simpoimpl = new SimpoImpl();
				try {
					return simpoimpl.updateReopenedInvoice(persons);
				} catch (SQLException e) {
					log.error("insertinvoiceWithDC()  :", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "SQLException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} catch (DXPortalException e) {
					log.error("insertinvoiceWithDC():", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "DXPortalException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} catch (ParseException e) {
					log.error("insertinvoiceWithDC() :", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "ParseException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}
			}
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean grnerror = false;
		boolean result;

		con = DBConnection.getConnection();
		con.setAutoCommit(false);

		boolean validation = validateSubmitInvoice(persons);
		if (validation == false) {
			deletebaddeliveries(persons, con);
			return jsonArray;
		}
		int counterSupp = 0;

		try {
			int pocount = 0;
			int poincount = 0;
			double balance = 0;

			// checking line item balance
			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				for (int i = 0; i < persons.size(); i++) {
					balance = getBalanceCount(persons.get(i).getPo_num(), persons.get(i).getLineItemNumber(),
							persons.get(i).getBid(), con);
					if (balance < Double.parseDouble(persons.get(i).getQuantity())) {
						updategrnstatus(persons, "B", con);
						responsejson.put("Uniquemessage", "Insufficient balance to create invoice");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						deletebaddeliveries(persons, con);
						return jsonArray;
					}
				}
			}
			// checking grn number for to be invoiced case
			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				boolean checkValidationFlag = true;
				if (persons.get(0).getType() != null && "resubmit".equals(persons.get(0).getType())) {
					if (persons.get(0).getInvoiceNumber().equals(persons.get(0).getPrevinvno())) {
						checkValidationFlag = false;
					}
				}

				if (checkValidationFlag) {
					boolean alreadypresent = false;
					int checkinvoiceingrn = checkinvoiceingrntable(persons, con);
					if (checkinvoiceingrn > 0) {
						alreadypresent = true;
					}
					if (alreadypresent) {
						responsejson.put("Uniquemessage",
								"INVOICENUMBER is already present in to-be-invoice list. Please  use a different invoice number.");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						deletebaddeliveries(persons, con);
						return jsonArray;
					}
				}
			}

			// Checking duplicate invoice and ponumber combination.
			for (int i = 0; i < persons.size(); i++) {

				boolean checkValidationFlag = true;
				if (persons.get(0).getType() != null && "resubmit".equals(persons.get(0).getType())) {
					if (persons.get(0).getInvoiceNumber().equals(persons.get(0).getPrevinvno())) {
						checkValidationFlag = false;
					}
				}

				if (checkValidationFlag) {
					poincount = getUniquePONInCheck(persons.get(i).getInvoiceNumber(), con, persons.get(i).getBid(),
							persons.get(i).getInvoiceDate());
					if (poincount > 0) {
						grnerror = true;
						if (!persons.get(i).getGrnnumber().equalsIgnoreCase("-")) {
							responsejson.put("Uniquemessage",
									"Invoice number already exists. Please  use a different invoice number");
						} else {
							responsejson.put("Uniquemessage",
									"Invoice number already exists. Please  use a different invoice number");
						}
						deletebaddeliveries(persons, con);
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						return jsonArray;
					}
				}

				if (!persons.get(i).getGrnnumber().equalsIgnoreCase("-")) {
					String uniquePoInCount = "Select count(*) as counter from deliverysummary where GRNNUMBER = ?  and PONUMBER=? "
							+ "and  INVOICENUMBER=? and BUSSINESSPARTNEROID=? ";

					int count = 0;
					ps = con.prepareStatement(uniquePoInCount);
					ps.setString(1, persons.get(i).getGrnnumber());
					ps.setString(2, persons.get(i).getPo_num());
					ps.setString(3, persons.get(i).getInvoiceNumber());
					ps.setString(4, persons.get(i).getBid());
					rs = ps.executeQuery();
					while (rs.next()) {
						count = rs.getInt("counter");
					}
					if (count > 0) {
						grnerror = true;
						deletebaddeliveries(persons, con);
						responsejson.put("Uniquemessage", "GRNNUMBER already present");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						return jsonArray;
					}
				}

				if (persons.get(i).getActualfilename() == null || persons.get(i).getActualfilename().equals("null")
						|| persons.get(i).getSavedfilename() == null || persons.get(i).getSavedfilename().equals("null")
						|| persons.get(i).getActualfilename().equals("undefined")
						|| persons.get(i).getSavedfilename().equals("undefined")) {
					responsejson.put("Uniquemessage", "Invoice file not uploaded correctly. Please try again.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}
			}
		} catch (Exception e) {
			log.error("insertinvoiceWithDC() : ", e.fillInStackTrace());
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");

			responsejson.put("ponumber", persons.get(0).getPo_num());
			responsejson.put("invoicenumber", persons.get(0).getInvoiceNumber());
			jsonArray.add(responsejson);
			con.rollback();
			return jsonArray;
		}

		log.info("Validation Checked : " + persons.get(0).getType());

		// Validation/check end here

		// prepare inputs for other API' , that need to be incorporated in same invoice
		// call
		SimpoImpl simpoImpl = new SimpoImpl();

		String type = persons.get(0).getType();
		String previousInvNum = persons.get(0).getPrevinvno();
		String previouseSelectedPO = persons.get(0).getPrevponos();
		String id = persons.get(0).getBid();
		String prevInvoiceDate = persons.get(0).getPrevinvdate();
		String invStatus = persons.get(0).getStage();

		// API reponsible for deleting old inoive releated enteries

		if (type != null && type.equals("resubmit")) {

			if (previousInvNum != null && !"".equals(previousInvNum) && !"undefined".equals(previousInvNum)
					&& !"null".equals(previousInvNum) && previouseSelectedPO != null && !"".equals(previouseSelectedPO)
					&& !"undefined".equals(previouseSelectedPO) && !"null".equals(previouseSelectedPO)) {
				log.info("Invoice Return API Called");
				String response1 = simpoImpl.getVendorReturn(previousInvNum, previouseSelectedPO, con);

				if (!response1.equals("Success")) {
					con.rollback();
					responsejson.put("Uniquemessage", response1);
					responsejson.put("message", "Fail");
					responsejson.put("error", response1);
					jsonArray.add(responsejson);
					return jsonArray;
				}
			} else {
				responsejson.put("Uniquemessage", "Previous PO/Invoice number not found.");
				responsejson.put("message", "Fail");
				responsejson.put("error", "Previous PO/Invoice number not found.");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		/**
		 * NEW LOGIC FOR CREATING DELIVERY SUMMERRY
		 */
		List<SimpoDeliveryItems> responseList = new ArrayList<SimpoDeliveryItems>();
		try {

			for (Invoicesubmission item : persons) {
				String dcNum = deliverysummaryInsert(item.getPo_num(), item.getLineItemNumber(), null,
						item.getQuantity(), id, "", con);
				item.setOrderNumber(dcNum.trim());

				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setPonumber(item.getPo_num().trim());
				obj.setLineitemnumber(item.getLineItemNumber().trim());
				obj.setQuantity(item.getQuantity().trim());
				obj.setDcn(item.getOrderNumber().trim());
				responseList.add(obj);
			}
		} catch (Exception e) {

			log.error("insertSimpoInvoice() 1 :", e.fillInStackTrace());
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("message", "Error while submitting invoice. Please try again.");
			responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
			responsejson.put("ponumber", persons.get(0).getPo_num());
			responsejson.put("invoicenumber", persons.get(0).getInvoiceNumber());
			jsonArray.add(responsejson);
			con.rollback();
			return jsonArray;

		}

		// Inserting invoice
		// Calculate totalquantity

		String marterialCode = "";
		String deliveryUniqueNoString = null;
		double totalquantity = 0;
		StringBuffer grnnumber = new StringBuffer();
		StringBuffer srcnnumber = new StringBuffer();
		Set<String> s = new HashSet<String>();
		Set<String> s1 = new HashSet<String>();
		for (int i = 0; i < persons.size(); i++) {
			s.add(persons.get(i).getGrnnumber());
			s1.add(persons.get(i).getSrcnnumber());
			totalquantity = totalquantity + Double.parseDouble(persons.get(i).getQuantity());

		}
		int b = 0;
		for (String str : s) {

			if (!str.equalsIgnoreCase("-")) {

				grnnumber.append(str);
				if (b < s.size() - 1) {
					grnnumber.append(",");
				}
			}
			b++;
		}
		b = 0;
		for (String str : s1) {

			if (!str.equalsIgnoreCase("-")) {
				if ((!str.equalsIgnoreCase("-")) && (str != null)) {
					srcnnumber.append(str);
				}

				if (b < s1.size() - 1) {
					if ((!str.equalsIgnoreCase("-")) && (str != null)) {
						srcnnumber.append(",");
					}
				}
			}
			b++;
		}

		try {
			// Inserting invoice in PONINVOICESUMMERY
			String querySummary = "insert into PONINVOICESUMMERY (INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,MESSAGE,"
					+ "REQUSITIONER,BUYER,AMOUNT,CREATEDON,MACOUNT,HOLDCOUNT,OVERALLSTATUS,INVOICEDATE,MATERIAL_TYPE,"
					+ "PGQ,ONEXSTATUS,ACTUALFILENAME,SAVEDFILENAME,PLANT,IRNNUMBER,"
					+ "IRNDATE,DESCRIPTION,CREATEDBY,BUSINESSPARTNERTEXT,VENDORID,BILLOFLADINGDATE,"
					+ "CONTACTPERSON,CONTACTPERSONPHONE,REMARK,TOTALAMTINCTAXES,TAXAMOUNT,GRNNUMBER,SCRNNUMBER,UNIQUEREFERENCENUMBER,BASEPO,NOTIFYENDUSEREMAILID)"
					+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			ps = con.prepareStatement(querySummary);
			ps.setString(1, persons.get(0).getInvoiceNumber());
			ps.setString(2, persons.get(0).getPo_num());
			ps.setString(3, persons.get(0).getBid());
			ps.setString(4, "N");
			ps.setString(5, persons.get(0).getContactPerson());
			ps.setString(6, persons.get(0).getBuyerid());
			ps.setString(7, persons.get(0).getTotalAmount());
			ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setInt(9, 1);
			ps.setInt(10, 0);
			ps.setString(11, persons.get(0).getStatus());
			ps.setDate(12, new java.sql.Date(
					new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(0).getInvoiceDate()).getTime()));

			ps.setString(13, persons.get(0).getMaterial());
			ps.setDouble(14, totalquantity);
			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				ps.setString(15, null);
			} else {
				ps.setString(15, "R");
			}
			ps.setString(16, persons.get(0).getActualfilename());
			ps.setString(17, persons.get(0).getSavedfilename());
			ps.setString(18, persons.get(0).getPlant());
			ps.setString(19, persons.get(0).getIrnNumber());
			if (persons.get(0).getIrnDate() != null && persons.get(0).getIrnDate() != ""
					&& persons.get(0).getIrnDate() != "null") {
				ps.setDate(20, new java.sql.Date(
						new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(0).getIrnDate()).getTime()));
			} else {
				ps.setString(20, persons.get(0).getIrnDate());
			}
			ps.setString(21, persons.get(0).getDescription());
			ps.setString(22, persons.get(0).getCreatedby());
			ps.setString(23, persons.get(0).getBusinessPartnerText());
			ps.setString(24, persons.get(0).getVendorID());
			if (persons.get(0).getBillofladingdate() != null && !"".equals(persons.get(0).getBillofladingdate())
					&& !("Invalid date").equalsIgnoreCase(persons.get(0).getBillofladingdate())) {
				ps.setDate(25, new java.sql.Date(
						new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(0).getBillofladingdate()).getTime()));
			} else {
				ps.setDate(25, null);
			}
			ps.setString(26, persons.get(0).getContactPerson());
			ps.setString(27, persons.get(0).getContactPersonPhone());
			ps.setString(28, persons.get(0).getRemark());
			ps.setString(29, persons.get(0).getTotalamtinctaxes());
			ps.setString(30, persons.get(0).getTaxamount());
			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				ps.setString(31, null);
			} else {
				ps.setString(31, grnnumber.toString());
			}
			if (persons.get(0).getSrcnnumber().equalsIgnoreCase("-")) {
				ps.setString(32, null);
			} else {
				ps.setString(32, srcnnumber.toString());
			}
			if (persons.get(0).getGrnnumber().equalsIgnoreCase("-")) {
				ps.setString(33, null);
			} else {
				ps.setString(33, "Y");
			}

			ps.setString(34, persons.get(0).getPo_num());
			ps.setString(35,
					persons.get(0).getNotifyenduseremailiD() == null ? "" : persons.get(0).getNotifyenduseremailiD());
			ps.executeUpdate();
			ps.close();

			// Inserting into INVOICETRACKER
			String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
					+ "MODIFIEDTIME,MODIFIEDBY,RESUBMITTEDINVOICENO)" + " values(?,?,?,?,?,?,?)";

			ps = con.prepareStatement(insertaudit);
			ps.setString(1, persons.get(0).getInvoiceNumber());
			ps.setString(2, persons.get(0).getPo_num());
			ps.setString(3, persons.get(0).getBid());
			if ("".equalsIgnoreCase(persons.get(0).getBeforesubmissioninvoicenumber())
					|| "null".equalsIgnoreCase(persons.get(0).getBeforesubmissioninvoicenumber())
					|| persons.get(0).getBeforesubmissioninvoicenumber() == null) {
				ps.setString(4, persons.get(0).getStatus());
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//					ps.setString(6, persons.get(0).getContactPerson());
				ps.setString(6, vendorEmail);
				ps.setString(7, persons.get(0).getBeforesubmissioninvoicenumber());
			} else {
				ps.setString(4, "S");
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//					ps.setString(6, persons.get(0).getContactPerson());
				ps.setString(6, vendorEmail);
				ps.setString(7, persons.get(0).getBeforesubmissioninvoicenumber());
			}
			ps.executeUpdate();
			ps.close();

			// Inserting into INVOICESUPPDOCS
			String insertsupportingdocument = "insert into INVOICESUPPDOCS (BUSINESSPARTNEROID,"
					+ "INVOICENUMBER,PONUMBER,ACTUALFILENAME,SAVEDFILENAME) values " + "(?,?,?,?,?)";

			ps = con.prepareStatement(insertsupportingdocument);
			ps.setString(1, persons.get(0).getBid());
			ps.setString(2, persons.get(0).getInvoiceNumber());
			ps.setString(3, persons.get(0).getPo_num());
			ps.setString(4, persons.get(0).getMultipleactualfilename());
			ps.setString(5, persons.get(0).getMultiplesavedfilename());
			ps.executeUpdate();
			ps.close();
			counterSupp++;

			// Updating or Inserting in DELIVERYSUMMARY Table
			for (int i = 0; i < persons.size(); i++) {
				deliveryUniqueNoString = persons.get(i).getOrderNumber();
				// log.info(deliveryUniqueNoString);

				log.info("Update Delivery : " + deliveryUniqueNoString);
				updatedeliverysumary(persons.get(i).getPo_num(), persons.get(i).getLineItemNumber(),
						persons.get(i).getInvoiceNumber(), persons.get(i).getOrderNumber(),
						persons.get(i).getInvoiceDate(), persons.get(i).getQuantity(), persons.get(i).getTotalAmount(),
						persons.get(i).getuOM(), persons.get(i).getRateperquantity(), persons.get(i).getLineitemtext(),
						persons.get(i).getStatus(), persons.get(i).getInvoiceamount(),
						persons.get(i).getStoragelocation(), persons.get(i).getGrnnumber(),
						persons.get(i).getUniquereferencenumber(), persons.get(i).getSaplineitemnumber(),
						persons.get(i).getServicenumber(), con, persons.get(i).getSrcnnumber());

			}

			// updating balance in poeventdetails table

			executeUpdateBalance(con, persons);

			for (int i = 0; i < persons.size(); i++) {

				// Getting materialtype according to the material code and plant from
				// inventoryuserlist
				String queryPolineitem = "Select * from inventoryuserlist where MTYP = "
						+ "(select MATERIAL_TYPE from poeventdetails  where (ponumber = ? and lineitemnumber = ? "
						+ "and businesspartneroid = ? and ordernumber is null)) AND plant = (select PLANT "
						+ "from poeventdetails  where (ponumber = ? and lineitemnumber =? and businesspartneroid =? "
						+ "and ordernumber is null))";

				ps = con.prepareStatement(queryPolineitem);
				ps.setString(1, persons.get(i).getPo_num());
				ps.setString(2, persons.get(i).getLineItemNumber());
				ps.setString(3, persons.get(i).getBid());
				ps.setString(4, persons.get(i).getPo_num());
				ps.setString(5, persons.get(i).getLineItemNumber());
				ps.setString(6, persons.get(i).getBid());
				rs = ps.executeQuery();

				while (rs.next()) {
					String materialType = rs.getString("MTYP") == null ? "" : rs.getString("MTYP");

					if (materialType != null && materialType != "") {
						if (!marterialCode.contains(materialType)) {
							marterialCode = materialType + ",";
						}
					}
				}
				rs.close();
				ps.close();

			}

			// update ponumber in invoicedetailwopo.
			getUpdateinvoiceeventdetailwopo(persons.get(0).getInvoiceNumber(), persons.get(0).getBid(),
					persons.get(0).getPo_num(), con);

			String status = null;
			String buyerId = null;

			String sqlUpdate1 = "insert into invoiceapproval (VENDORID,INVOICENUMBER,PONUMBER,BUYERID,ENDUSEID,"
					+ "ENDUSERSTATUS,STAGE,MODIFIEDDATE,INVOICEDATE,STATUS,PROXY) values (?,?,?,?,?,?,?,?,?,?,?)";
			ps = con.prepareStatement(sqlUpdate1);
			ps.setString(1, persons.get(0).getVendorID());
			ps.setString(2, persons.get(0).getInvoiceNumber());
			ps.setString(3, persons.get(0).getPo_num());
			ps.setString(4, persons.get(0).getBuyerid());
			ps.setString(5, persons.get(0).getContactPerson());
			ps.setString(6, "P");
			ps.setString(7, persons.get(0).getStage());
			ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setDate(9, new java.sql.Date(
					new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(0).getInvoiceDate()).getTime()));

			ps.setString(10, "M");
			if (marterialCode != null && marterialCode != "") {
				ps.setString(11, "X");
			} else {
				ps.setString(11, null);
			}
			ps.executeUpdate();
			ps.close();

			// Update GRNMAPPING
			String updategrn = "Update GRNMAPPING set STATUS=?,INVOICENUMBER=? where PONUMBER=? AND "
					+ "LINEITEMNO=? AND DCNUMBER=? AND GRNNUMBER=?";

			for (int j = 0; j < persons.size(); j++) {
				if (!("-").equalsIgnoreCase(persons.get(j).getGrnnumber())) {
					ps = con.prepareStatement(updategrn);
					ps.setString(1, "D");
					ps.setString(2, persons.get(j).getInvoiceNumber());
					ps.setString(3, persons.get(j).getPo_num());
					ps.setString(4, persons.get(j).getLineItemNumber());
					ps.setString(5, persons.get(j).getDcnumber());
					ps.setString(6, persons.get(j).getGrnnumber());
					ps.executeUpdate();
					ps.close();
				}
			}

			con.commit();
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);

		} catch (DXPortalException dxp) {
			log.error("insertinvoiceWithDC() 1 :", dxp.fillInStackTrace());
			con.rollback();
			deletebaddeliveries(persons, con);
			responsejson.put("error", dxp.reason);
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", persons.get(0).getPo_num());
			responsejson.put("invoicenumber", persons.get(0).getInvoiceNumber());
			jsonArray.add(responsejson);
			return jsonArray;

		} catch (Exception e) {
			log.error("insertinvoiceWithDC() 2 :", e.fillInStackTrace());
			con.rollback();
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", persons.get(0).getPo_num());
			responsejson.put("invoicenumber", persons.get(0).getInvoiceNumber());
			jsonArray.add(responsejson);
			return jsonArray;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	String deliverysummaryInsert(String po_num, String lineItemNumber, String invoiceNumber, String quantity,
			String bussinesspartneroid, String invoicedate, Connection con) throws SQLException, DXPortalException {

		String sqlIdentifier = "select DELIVERYSUMMARY_SEQ.NEXTVAL from dual";
		String po_status = "insert into deliverysummary (DeliveryUniqueNo,invoicenumber,dispatchDate,ponumber,LineItemNumber,Quantity,BUSSINESSPARTNEROID,INVOICEDATE)"
				+ " values (?,?,?,?,?,?,?,?)";
		String upgetseq = "";
		if (invoiceNumber == null) {
			upgetseq = "update deliverysummary set dc= ? where "
					+ "ponumber= ? and LineItemNumber= ? and  DeliveryUniqueNo=?";
		} else {
			upgetseq = "update deliverysummary set dc= ? where "
					+ "ponumber= ? and invoiceNumber= ? and LineItemNumber= ? and  DeliveryUniqueNo=?";
		}

		long myId = 0;
//		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
//			con = DBConnection.getConnection();
//			con.setAutoCommit(false);
			ps = con.prepareStatement(sqlIdentifier);
			rs = ps.executeQuery();
			if (rs.next()) {
				myId = rs.getLong(1);
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement(po_status);
			ps.setString(1, String.valueOf(myId));
			ps.setString(2, invoiceNumber);
			Date date = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			String strDate = formatter.format(date);
			ps.setString(3, strDate);
			ps.setString(4, po_num);
			ps.setString(5, lineItemNumber);
			ps.setString(6, quantity);
			ps.setString(7, bussinesspartneroid);
			if (invoicedate.equalsIgnoreCase("")) {
				ps.setDate(8, null);
			} else {
				ps.setDate(8, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoicedate).getTime()));
			}
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement(upgetseq);
			if (invoiceNumber == null) {
				ps.setString(1, "DCN" + myId);
				ps.setString(2, po_num);
				ps.setString(3, lineItemNumber);
				ps.setString(4, String.valueOf(myId));
			} else {
				ps.setString(1, "DCN" + myId);
				ps.setString(2, po_num);
				ps.setString(3, invoiceNumber);
				ps.setString(4, lineItemNumber);
				ps.setString(5, String.valueOf(myId));
			}

			ps.executeUpdate();
			ps.close();
//			con.commit();
			return "DCN" + myId;
		} catch (Exception e) {
			log.error("deliverysummaryInsert() :", e.fillInStackTrace());
//			con.rollback();
			throw new DXPortalException("Error while creating delivery", "Error in deliverysummaryInsert()");
		}
//		finally {
//			DBConnection.closeConnection(rs, ps, con);
//		}

	}

	public String writeintoCSVfile(List<List<String>> poDataList) {
		String encodedfile = "";
		XSSFWorkbook workbook = new XSSFWorkbook();
		try {
			FileOutputStream out = new FileOutputStream(new File("demo.xlsx"));
			XSSFSheet sheet = workbook.createSheet("PO Data");
			List<String> heading = new ArrayList<String>();

			heading.add("PLANT CODE");
			heading.add("PLANT NAME");
			heading.add("PO DATE");
			heading.add("PO NUMBER");
			heading.add("LINE ITEM NUMBER");
			heading.add("LINE ITEM DESCRIPTION");
			heading.add("PO AMOUNT");
			heading.add("LINE ITEM RATE");
			heading.add("LINE ITEM QUANTITY");
			heading.add("LINE ITEM AMOUNT");
			heading.add("LINE ITEM BALANCE QUANTITY");
			heading.add("LINE ITEM BALANCE AMOUNT");
			heading.add("STATUS");

			Iterator<String> headingIterator = heading.iterator();
			Iterator<List<String>> i = poDataList.iterator();
			int rownum = 0;
			int cellnum = 0;
			Row headingRow = sheet.createRow(rownum);

			while (headingIterator.hasNext()) {
				String colHeading = (String) headingIterator.next();

				Cell cell = headingRow.createCell(cellnum);
				sheet.autoSizeColumn(cellnum);
				XSSFFont fontBold = workbook.createFont();
				fontBold.setBold(true);
				CellStyle cellStyle1 = workbook.createCellStyle();
				cellStyle1.setAlignment(HorizontalAlignment.CENTER);
				XSSFRichTextString cellValue = new XSSFRichTextString();
				cellValue.append(colHeading, fontBold);
				cell.setCellValue(cellValue);
				cell.setCellStyle(cellStyle1);

				cellnum = cellnum + 1;

			}

			try {

				while (i.hasNext()) {
					cellnum = 0;
					rownum = rownum + 1;

					Row row = sheet.createRow(rownum);
					List<String> poDataRow = (List<String>) i.next();
					Iterator<String> tempIterator = poDataRow.iterator();
					int k = 0;
					while (tempIterator.hasNext()) {
						String temp = (String) tempIterator.next();
						Cell cell = row.createCell(cellnum);
						CellStyle cellStyle = workbook.createCellStyle();
						if (k == 0) {
							cellStyle.setAlignment(HorizontalAlignment.CENTER);
						} else if (k == 1) {
							cellStyle.setAlignment(HorizontalAlignment.LEFT);
						} else if (k == 2) {
							cellStyle.setAlignment(HorizontalAlignment.CENTER);
						} else if (k == 3) {
							cellStyle.setAlignment(HorizontalAlignment.CENTER);
						} else if (k == 4) {
							cellStyle.setAlignment(HorizontalAlignment.CENTER);
						} else if (k == 5) {
							cellStyle.setAlignment(HorizontalAlignment.LEFT);
						} else if (k == 6) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 7) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 8) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 9) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 10) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 11) {
							cellStyle.setAlignment(HorizontalAlignment.RIGHT);
						} else if (k == 12) {
							cellStyle.setAlignment(HorizontalAlignment.LEFT);
						}
						sheet.autoSizeColumn(cellnum);
						cell.setCellValue(temp);
						cell.setCellStyle(cellStyle);
						cellnum = cellnum + 1;
						k++;
					}
				}

			} catch (Exception e) {
				log.error("writeintoexcelfile() 1 : ", e.fillInStackTrace());
			}
			workbook.write(out);

			File file = new File("demo.xlsx");
			try {
				InputStream inputStream;
				inputStream = new FileInputStream(file);
				byte[] bytes1 = new byte[(int) file.length()];
				inputStream.read(bytes1);
				encodedfile = new String(Base64.encodeBase64(bytes1), "UTF-8");
				out.close();
				workbook.close();
				responsejson.put("message", "Success");
				responsejson.put("data", encodedfile);
			} catch (IOException e) {
				log.error("writeintoexcelfile() 2 : ", e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("writeintoexcelfile() 3 : ", e.fillInStackTrace());
		}

		return encodedfile;
	}

	public JSONArray getPODetails(String bid, String mode, String ponum, String fromdateofduration,
			String todateofduration, String fromdateofpo, String todateofpo, String companyCode) throws SQLException {
		String poDataQuery = null;
		String poDateSubquery = "";

		ArrayList<String> param = new ArrayList<String>();

		String compCodeQuery = " AND A.companycode = ? ";

		if ("ALL".equalsIgnoreCase(mode)) {

			param.add(bid);
			param.add("N");
			param.add(companyCode);

			if (!"NA".equalsIgnoreCase(ponum)) {
				String po = " AND A.PONUMBER = ?";
				poDateSubquery = poDateSubquery + po;
				param.add(ponum);
			}
			if ((!"NA".equalsIgnoreCase(fromdateofduration)) && (!"Invalid date".equalsIgnoreCase(fromdateofduration))
					&& (!"NA".equalsIgnoreCase(todateofduration))
					&& (!"Invalid date".equalsIgnoreCase(todateofduration))) {
				String in = " AND A.CREATEDON BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
				poDateSubquery = poDateSubquery + in;
				param.add(fromdateofduration);
				param.add(todateofduration);
			}
			if ((!"NA".equalsIgnoreCase(fromdateofpo)) && (!"Invalid date".equalsIgnoreCase(fromdateofpo))
					&& (!"NA".equalsIgnoreCase(todateofpo)) && (!"Invalid date".equalsIgnoreCase(todateofpo))) {
				String dt = " AND A.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
				poDateSubquery = poDateSubquery + dt;
				param.add(fromdateofpo);
				param.add(todateofpo);
			}
			param.add(bid);
			param.add("N");
			param.add(companyCode);

			if (!"NA".equalsIgnoreCase(ponum)) {
				param.add(ponum);
			}
			if ((!"NA".equalsIgnoreCase(fromdateofduration)) && (!"Invalid date".equalsIgnoreCase(fromdateofduration))
					&& (!"NA".equalsIgnoreCase(todateofduration))
					&& (!"Invalid date".equalsIgnoreCase(todateofduration))) {
				param.add(fromdateofduration);
				param.add(todateofduration);
			}
			if ((!"NA".equalsIgnoreCase(fromdateofpo)) && (!"Invalid date".equalsIgnoreCase(fromdateofpo))
					&& (!"NA".equalsIgnoreCase(todateofpo)) && (!"Invalid date".equalsIgnoreCase(todateofpo))) {
				param.add(fromdateofpo);
				param.add(todateofpo);
			}

			poDataQuery = "select * from ( "
					+ "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
					+ "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE "
					+ "FROM podetails A,poeventdetails B "
					+ "WHERE A.BusinessPartnerOID=? AND A.BUSINESSPARTNEROID=B.BUSINESSPARTNEROID AND A.PONUMBER=B.PONUMBER and A.Status=? "
					+ compCodeQuery + poDateSubquery + " " + "Union "
					+ "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
					+ "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
					+ "FROM podetails A,poeventdetails B "
					+ "WHERE A.BusinessPartnerOID=? AND A.BUSINESSPARTNEROID=B.BUSINESSPARTNEROID AND A.PONUMBER=B.PONUMBER and A.Status <> ? "
					+ compCodeQuery + poDateSubquery + " " + ") ORDER BY 1,4";

		} else {

			param.add(bid);
			param.add(companyCode);

			if ("SP".equalsIgnoreCase(mode)) {
				String poType = " and A.POTYPE = ?";
				poDateSubquery = poDateSubquery + poType;
				param.add("S");
			} else if ("AS".equalsIgnoreCase(mode)) {

			} else {
				String poStatus = " and A.Status = ?";
				poDateSubquery = poDateSubquery + poStatus;
				param.add(mode);
			}

			if (!"NA".equalsIgnoreCase(ponum)) {
				String po = " AND A.PONUMBER = ?";
				poDateSubquery = poDateSubquery + po;
				param.add(ponum);
			}

			if ((!"NA".equalsIgnoreCase(fromdateofduration)) && (!"Invalid date".equalsIgnoreCase(fromdateofduration))
					&& (!"NA".equalsIgnoreCase(todateofduration))
					&& (!"Invalid date".equalsIgnoreCase(todateofduration))) {
				String in = " AND A.CREATEDON BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
				poDateSubquery = poDateSubquery + in;
				param.add(fromdateofduration);
				param.add(todateofduration);
			}

			if ((!"NA".equalsIgnoreCase(fromdateofpo)) && (!"Invalid date".equalsIgnoreCase(fromdateofpo))
					&& (!"NA".equalsIgnoreCase(todateofpo)) && (!"Invalid date".equalsIgnoreCase(todateofpo))) {
				String dt = " AND A.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
				poDateSubquery = poDateSubquery + dt;
				param.add(fromdateofpo);
				param.add(todateofpo);
			}

			poDataQuery = "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
					+ "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
					+ "FROM podetails A,poeventdetails B "
					+ "WHERE A.BusinessPartnerOID=? AND A.BUSINESSPARTNEROID=B.BUSINESSPARTNEROID AND A.PONUMBER=B.PONUMBER "
					+ compCodeQuery + poDateSubquery + " " + "ORDER BY 1,4";
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			log.info(" :poDataQuery : " + poDataQuery);
			con = DBConnection.getConnection();
			Pagination pg = new Pagination(poDataQuery, 0);
			rs = pg.execute(con, param);
			List<List<String>> poDataList = new ArrayList<List<String>>();

			while (rs.next()) {

				List<String> podata = new ArrayList<String>();

				podata.add(rs.getString("PLANT") == null ? "" : rs.getString("PLANT"));
				podata.add(rs.getString("PLANTNAME") == null ? "" : rs.getString("PLANTNAME"));
				podata.add(rs.getString("PODATE"));
				podata.add(rs.getString("PONUMBER"));
				podata.add(rs.getString("LINEITEMNUMBER"));
				podata.add(rs.getString("LINEITEMTEXT"));
				podata.add(rs.getString("POAMOUNT"));
				podata.add(rs.getString("RATEPERQTY"));
				podata.add(rs.getString("QUANTITY"));
				podata.add(rs.getString("NETVALUE"));
				podata.add(rs.getString("BALANCE_QTY"));
				podata.add(rs.getString("BALANCENETVALUE"));

				if (rs.getString("Status").equalsIgnoreCase("A")) {
					podata.add("Accepted");
				} else if (rs.getString("Status").equalsIgnoreCase("P")) {
					podata.add("Work In Progress");
				} else if (rs.getString("Status").equalsIgnoreCase("S")) {
					podata.add("Shipped");
				} else if (rs.getString("Status").equalsIgnoreCase("C")) {
					podata.add("Complete");
				} else if (rs.getString("Status").equalsIgnoreCase("N")) {
					podata.add("New");
				} else {
					podata.add(rs.getString("STATUS"));
				}

				poDataList.add(podata);
			}
			pg.close();
			rs.close();
			pg = null;

			String encodedfile = writeintoexcelfile(poDataList);
			// String encodedfile = downloadPOCSVfile(poDataList);

			if (poDataList.isEmpty() == true) {
				responsejson.put("message", "NoDataFound");
			} else if (encodedfile.equalsIgnoreCase("")) {
				responsejson.put("message", "Fail");
			} else {
				responsejson.put("message", "Success");
				responsejson.put("data", encodedfile);
			}
			jsonArray.add(responsejson);

		} catch (SQLException e) {
			log.error("getPODetails() :" + e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	private String downloadPOCSVfile(List<List<String>> totallist) {
		String encodedfile = "";
		try {

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
			CSVWriter writer = new CSVWriter(streamWriter);

			List<String[]> heading = new ArrayList<String[]>();

			String header = "PLANT CODE" + "|" + "PLANT NAME" + "|" + "PO DATE" + "|" + "PO NUMBER" + "|"
					+ "LINE ITEM NUMBER" + "|" + "LINE ITEM DESCRIPTION" + "|" + "PO AMOUNT" + "|" + "LINE ITEM RATE"
					+ "|" + "LINE ITEM QUANTITY" + "|" + "LINE ITEM AMOUNT" + "|" + "LINE ITEM BALANCE QUANTITY" + "|"
					+ "LINE ITEM BALANCE AMOUNT" + "|" + "STATUS";

			String headerList[] = { header };

			Iterator<List<String>> i = totallist.iterator();
			heading.add(headerList);
			while (i.hasNext()) {

				List<String> templist = (List<String>) i.next();
				Iterator<String> tempIterator = templist.iterator();
				String data = "";
				while (tempIterator.hasNext()) {
					String temp = (String) tempIterator.next();
					data += temp + "|";
				}
				data = data.substring(0, data.length() - 1);
				String listData[] = { data };
				heading.add(listData);
			}

			writer.writeAll(heading);
			writer.flush();
			streamWriter.flush();
			byte[] byteArrayOutputStream = stream.toByteArray();

			try {
				encodedfile = new String(Base64.encodeBase64(byteArrayOutputStream), "UTF-8");
			} catch (IOException e) {
				log.error("downloadinernalfile() 1: " + e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("downloadinernalfile() 2: " + e.fillInStackTrace());

		}
		return encodedfile;
	}

	public JSONArray faqDetails(String userType) {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		String questions = null;
		String answers = null;
		try {
			con = DBConnection.getConnection();
			String faqQuery = "SELECT QUESTIONS,ANSWERS FROM FAQDETAILS WHERE FAQTYPE = ? AND STATUS = 'A' ";

			ps = con.prepareStatement(faqQuery);
			ps.setString(1, userType);
			rs = ps.executeQuery();
			List<List<String>> faqList = new ArrayList<List<String>>();

			while (rs.next()) {

				List<String> qaList = new ArrayList<String>();
				questions = rs.getString("QUESTIONS");
				answers = rs.getString("ANSWERS");
				qaList.add(questions);
				qaList.add(answers);
				faqList.add(qaList);
			}

			rs.close();
			ps.close();

			if (faqList.isEmpty() == true) {
				responsejson.put("message", "No Data Found");
				responsejson.put("StatusCode", "203");
			} else {
				responsejson.put("message", "Success");
				responsejson.put("StatusCode", "200");
				responsejson.put("faqlist", faqList);
			}

			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("faqDetails() :", e.fillInStackTrace());
			responsejson.put("StatusCode", "406");

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray locationDetails() {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		String locCode = null;
		String locName = null;
		try {
			con = DBConnection.getConnection();
			String LocQuery = " SELECT LOCATIONCODE,LOCATIONNAME FROM LOCATIONMASTER WHERE STATUS ='A' ";

			ps = con.prepareStatement(LocQuery);
			rs = ps.executeQuery();
			List<List<String>> locList = new ArrayList<List<String>>();

			while (rs.next()) {

				List<String> locationList = new ArrayList<String>();
				locCode = rs.getString("LOCATIONCODE");
				locName = rs.getString("LOCATIONNAME");
				locationList.add(locCode);
				locationList.add(locName);
				locList.add(locationList);
			}

			rs.close();
			ps.close();

			if (locList.isEmpty() == true) {
				responsejson.put("message", "No Data Found");
				responsejson.put("StatusCode", "203");
			} else {
				responsejson.put("message", "Success");
				responsejson.put("StatusCode", "200");
				responsejson.put("locationlist", locList);
			}

			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("faqDetails() :", e.fillInStackTrace());
			responsejson.put("StatusCode", "406");

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray insertASNDetails(List<AdvanceShippingNotification> persons, String vendorEmail)
			throws SQLException {

		log.info("insertASNDetails " + vendorEmail);

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		PreparedStatement ps1 = null;
		ResultSet rs1 = null;
		boolean grnerror = false;
		boolean result;
		String boid = null;
		String ponumber = null;
		String locationCode = null;
		
		con = DBConnection.getConnection();

		try {
			if (persons != null && !persons.isEmpty()) {
				for (int counter = 0; counter < persons.size(); counter++) {
	
					if (persons.get(counter).getAsnQuantity() == null && persons.get(counter).getAsnDate() == null
							&& persons.get(counter).getAsnLocation() == null && persons.get(counter).getAsnNumber()==null ) {
						responsejson.put("Uniquemessage", "Advance Shipping Notification some fields are blank in PO "+persons.get(counter).getLineItemNumber()+ " LINE ITEM ");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						return jsonArray;
					}
				}
			}
			
			if (persons != null && !persons.isEmpty()) {
				for (int i = 0; i < persons.size(); i++) {
		
						String insertASN = "INSERT INTO ASNDETAILS (BUSINESSPARTNEROID, PONUMBER, LINEITEMNUMBER, LINEITEMTEXT,"
								+ " BALANCEQTY, ASNQUANTITY, ASNDATE, LOCATIONCODE, CREATEDBY, CREATEDON, MODIFIEDBY, MODIFIEDON, STATUS,ASNNUMBER,REQNO, SUPPORTINGDOCNAME)"
								+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,ASN_SEQ.NEXTVAL,?) ";
			
						log.info("persons.get(i).getBid() : "+ persons.get(i).getBid());
						log.info("persons.get(i).getPo_num() : "+persons.get(i).getPo_num());
						log.info("persons.get(i).getLineItemNumber() : "+persons.get(i).getLineItemNumber());
						log.info("persons.get(i).getLineItemText() : "+persons.get(i).getLineItemText());
						log.info("persons.get(i).getBalancQTY() : "+persons.get(i).getBalancQTY());
						log.info("persons.get(i).getAsnQuantity() : "+  persons.get(i).getAsnQuantity());
						log.info("persons.get(i).getAsnDate() ; "+persons.get(i).getAsnDate());
						log.info("persons.get(i).getAsnLocation() : "+ persons.get(i).getAsnLocation());
						log.info("vendorEmail : "+vendorEmail);
						log.info("persons.get(i).getAsnNumber() : "+ persons.get(i).getAsnNumber());
						
						String LocQuery = " SELECT DISTINCT A.LOCATIONCODE FROM LOCATIONMASTER A WHERE A.LOCATIONNAME = ? ";
							
						ArrayList<HashMap<String, String>> asnList = new ArrayList<HashMap<String, String>>();
				
						if(!(isNumeric(persons.get(i).getAsnLocation()))) {

							ps1 = con.prepareStatement(LocQuery);
							ps1.setString(1, persons.get(i).getAsnLocation());
							rs1 = ps1.executeQuery();

							while(rs1.next()) {									
								locationCode = rs1.getString(1);
							}
							ps1.close();
							rs1.close();
						}else{
							locationCode = persons.get(i).getAsnLocation();
						}
							
						boid = persons.get(i).getBid();
						ponumber = persons.get(i).getPo_num();
								
						SimpleDateFormat formatter = new SimpleDateFormat();
						SimpleDateFormat inputFormat = new SimpleDateFormat("dd/mm/yy");
						Date date = (Date) inputFormat.parse(persons.get(i).getAsnDate());
						SimpleDateFormat outputFormat = new SimpleDateFormat("mm-dd-yyyy");
						String dateASN = outputFormat.format(date);
						
						ps = con.prepareStatement(insertASN);
						ps.setString(1, persons.get(i).getBid());
						ps.setString(2, persons.get(i).getPo_num());
						ps.setString(3, persons.get(i).getLineItemNumber());
						ps.setString(4, persons.get(i).getLineItemText());
						ps.setString(5, persons.get(i).getBalancQTY());
						ps.setString(6, persons.get(i).getAsnQuantity());
						ps.setDate(7, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(persons.get(i).getAsnDate()).getTime()));
						ps.setString(8, locationCode);
						ps.setString(9,  vendorEmail);
						ps.setTimestamp(10, new java.sql.Timestamp(new java.util.Date().getTime()));
						ps.setString(11, vendorEmail);
						ps.setTimestamp(12, new java.sql.Timestamp(new java.util.Date().getTime()));
						ps.setString(13, "I");
						ps.setString(14, persons.get(i).getAsnNumber()==null?"":persons.get(i).getAsnNumber());
						ps.setString(15, persons.get(i).getSupportingDocName()==null?"":persons.get(i).getSupportingDocName().trim());	
						ps.executeUpdate();
						ps.close();
						
						if(i==0) {
							String poQuery = " UPDATE PODETAILS SET ASNSTATUS = ?, MODIFIEDON = ? WHERE PONUMBER = ? AND BUSINESSPARTNEROID = ? ";
							ps = con.prepareStatement(poQuery);
							ps.setString(1, "S");
							ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
							ps.setString(3, ponumber);
							ps.setString(4, boid);
							int count = ps.executeUpdate();
							ps.close();	
						}												
					}
					responsejson.put("message", "Success");
					jsonArray.add(responsejson);
					
				
			}
		} catch (Exception e) {
			log.error("insertASNDetails()  :", e.fillInStackTrace());
			e.printStackTrace();
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("Uniquemessage", "SQL Error while submitting Advance Shipping Notification !!");
			responsejson.put("message", "SQL Error while submitting Advance Shipping Notification !!");
			responsejson.put("ponumber", persons.get(0).getPo_num());
			jsonArray.add(responsejson);
			return jsonArray;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getASNList(String bid,String poNo) {
		
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		String boId = null;
		String poNumber = null;
		String lineItem = null;
		String lineText = null;
		String balQty = null;
		String asnQty = null;
		String asnDate = null;
		String locName = null;
		String status = null;
		String asnNo = null;
		
		
		try {
			con = DBConnection.getConnection();
			String LocQuery = " SELECT B.BUSINESSPARTNEROID AS BOID,B.PONUMBER AS PONO,B.LINEITEMNUMBER AS LIITNO,B.LINEITEMTEXT AS LIITTX, " 
					+ " B.BALANCEQTY AS BQT,B.ASNQUANTITY AS ASNQTY,TO_CHAR(B.ASNDATE,'DD-MON-RRRR') AS ADATE, "
					+ "(SELECT DISTINCT A.LOCATIONNAME FROM LOCATIONMASTER A WHERE A.LOCATIONCODE = B.LOCATIONCODE) AS LOCNAME, "
					+ "B.STATUS AS STATUS,B.ASNNUMBER AS ASNO, REQNO, B.SUPPORTINGDOCNAME FROM ASNDETAILS B WHERE B.BUSINESSPARTNEROID = ? AND B.PONUMBER = ? " 
					+ " AND B.STATUS <> 'D' ORDER BY B.LINEITEMNUMBER, B.MODIFIEDON DESC ";

			ps = con.prepareStatement(LocQuery);
			ps.setString(1, bid);
			ps.setString(2, poNo);
			rs = ps.executeQuery();
			
			ArrayList<HashMap<String, String>> asnList = new ArrayList<HashMap<String, String>>();

			while (rs.next()) {
	
				HashMap<String, String> asnDataMap = new HashMap<String, String>();
				asnDataMap.put("BUSINESSPARTNEROID", rs.getString("BOID"));			
				asnDataMap.put("PONUMBER", rs.getString("PONO"));
				asnDataMap.put("LINEITEMNUMBER", rs.getString("LIITNO"));
				asnDataMap.put("LINEITEMTEXT", rs.getString("LIITTX"));
				asnDataMap.put("BALANCEQTY", rs.getString("BQT"));
				asnDataMap.put("ASNQUANTITY", rs.getString("ASNQTY"));
				asnDataMap.put("ASNDATE", rs.getString("ADATE"));
				asnDataMap.put("LOCATIONNAME",  rs.getString("LOCNAME"));
				asnDataMap.put("STATUS",  rs.getString("STATUS"));
				asnDataMap.put("ASNNUMBER",  rs.getString("ASNO"));
				asnDataMap.put("REQUESTNO",  rs.getString("REQNO"));
				asnDataMap.put("ASNSUPPORTINGDOCNAME",  rs.getString("SUPPORTINGDOCNAME"));
				asnList.add(asnDataMap);
			}

			rs.close();
			ps.close();

			if (asnList.isEmpty() == true) {
				responsejson.put("message", "No Data Found");
				responsejson.put("StatusCode", "203");
			} else {
				responsejson.put("message", "Success");
				responsejson.put("StatusCode", "200");
				responsejson.put("ASNlist", asnList);
			}

			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("getASNList() :", e.fillInStackTrace());
			responsejson.put("StatusCode", "406");

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}
	
	public JSONArray putAsnNo(String bid,String asnNo,String reqNo,String docName,String status) {
		
		Connection con = null;
		PreparedStatement ps = null;
		String boId = null;
		ResultSet rs = null;
		String poNumber = null;
		String lineItem = null;
		String lineText = null;
		String balQty = null;
		String asnQty = null;
		String asnDate = null;
		String locName = null;		
				
		try {
			con = DBConnection.getConnection();
			String asnUpdateQuery = " UPDATE ASNDETAILS SET ASNNUMBER = ? , MODIFIEDON = ?, STATUS = ?, SUPPORTINGDOCNAME = ? WHERE BUSINESSPARTNEROID = ? AND REQNO = ? ";

			if(status == null || "".equals(status)){
				status = 	"I";
			}else {
				status = (status.trim()==null ||status.trim()=="")?"I":status.trim();
				if("".equalsIgnoreCase(status)) {
					status = "I";
				}
			}
			if("undefined".equalsIgnoreCase(docName)) {
				docName = "";
			}
			if("null".equalsIgnoreCase(asnNo) || asnNo == null) {
				asnNo = "";
			}			
			
			ps = con.prepareStatement(asnUpdateQuery);
			ps.setString(1, asnNo);
			ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(3, status);	
			ps.setString(4, docName);
			ps.setString(5, bid);
			ps.setString(6, reqNo);
			int count = ps.executeUpdate();						
			ps.close();

			if(count > 0) {
				String findPO = "SELECT PONUMBER FROM ASNDETAILS WHERE BUSINESSPARTNEROID = ? AND REQNO = ? ";
				ps = con.prepareStatement(findPO);
				ps.setString(1, bid);
				ps.setString(2, reqNo);
				rs = ps.executeQuery();
				while(rs.next()) {
					poNumber = rs.getString("PONUMBER");
				}
				ps.close();
				rs.close();
				
				String poUpdate = "UPDATE PODETAILS SET STATUS = ? WHERE PONUMBER = ? AND STATUS <> ? ";	
				ps = con.prepareStatement(poUpdate);
				ps.setString(1, "P");
				ps.setString(2, poNumber);	
				ps.setString(3, "C");
				int count1 = ps.executeUpdate();						
				ps.close();				
			}
			
			if (count == 0) {
				responsejson.put("message", "No Data Found");
				responsejson.put("StatusCode", "203");
			} else {
				responsejson.put("message", "Success");
				responsejson.put("StatusCode", "200");			
			}

			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("putAsnNo() :", e.fillInStackTrace());
			responsejson.put("StatusCode", "406");

		} finally {
			DBConnection.closeConnection(null, ps, con);
		}
		return jsonArray;
	}	
		
	public JSONArray poListDetails(String boid,String ponumber) {
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String boId = null;
		String poNumber = null;
		String lineItem = null;
		String lineText = null;
		String balQty = null;
		String asnQty = null;
		String asnDate = null;
		String locName = null;		
				
		try {
			con = DBConnection.getConnection();
			String poQuery = " SELECT PONUMBER, ASNSTATUS FROM PODETAILS WHERE PONUMBER = ? AND BUSINESSPARTNEROID = ? ";
			
			ps = con.prepareStatement(poQuery);
			ps.setString(1, ponumber);
			ps.setString(2, boid);
			rs = ps.executeQuery();
			
			ArrayList<HashMap<String, String>> poList = new ArrayList<HashMap<String, String>>();

			while (rs.next()) {
	
				HashMap<String, String> poDataMap = new HashMap<String, String>();
				poDataMap.put("PONUMBER", rs.getString("PONUMBER"));
				poDataMap.put("ASNSTATUS", rs.getString("ASNSTATUS")==null?"":rs.getString("ASNSTATUS"));
				poList.add(poDataMap);
			}	
			ps.close();
			rs.close();
			
			if (poList.isEmpty() == true) {
				responsejson.put("message", "No Data Found");
				responsejson.put("StatusCode", "203");
			} else {
				responsejson.put("message", "Success");
				responsejson.put("StatusCode", "200");
				responsejson.put("polist", poList);
			}

			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("putAsnNo() :", e.fillInStackTrace());
			responsejson.put("StatusCode", "406");

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}
	
	public boolean isNumeric(String maybeNumeric) {
	    return maybeNumeric != null && maybeNumeric.matches("[0-9]+");
	}

	public void poAcceptance(String PONumber,Connection con) throws Exception {
		
		//Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String posql = "";
		
		try {

			posql = "SELECT PD.PONUMBER,TO_CHAR(PD.PODATE,'DD-MON-RRRR') PODATE,PD.POAMOUNT,PD.REQUSITIONER, " + 
					"BP.VENDORID,BP.BUSINESSPARTNERTEXT " + 
					"FROM PODETAILS PD, BUSINESSPARTNER BP WHERE BP.BUSINESSPARTNEROID =  PD.BUSINESSPARTNEROID " + 
					"AND PD.STATUS = ? AND PD.PONUMBER = ? ";

			log.info("Query = " + posql);

			ArrayList newPo = new ArrayList();
		
			con = DBConnection.getConnection();
			ps = con.prepareStatement(posql);
			ps.setString(1, "N");
			ps.setString(2, PONumber);
		
			rs = ps.executeQuery();
			
			while(rs.next()) {

				String poNumber =  rs.getString("PONUMBER");
				String poDate =  rs.getString("PODATE");
				String poAmount =  rs.getString("POAMOUNT");
				String requisitioner =  rs.getString("REQUSITIONER");
				String code = rs.getString("VENDORID");
				String name = rs.getString("BUSINESSPARTNERTEXT");
				Hashtable<String, String> htTable = new Hashtable<String, String>();
				
				String toAddr = requisitioner;
				
				htTable.put("toAddr", toAddr);

				String msgText1 = "Dear Recipient,<br><br> PO number "+poNumber+" has been accepted by the vendor.<br>";
										
				String subject = "PO : " + poNumber +" acceptance confirmation";
				
				htTable.put("subject", subject);
				
				log.info("msgText1 = " + msgText1 + " and recipient ID = " + toAddr);

				String htmlStr = "<html>" + "\n" + "<head>" + "\n"
						+ "	<title> Advance shipping notification Emails </title>" + "\n" + "	<style>" + "\n"
						+ "		.bold {FONT-WEIGHT: bold; FONT-SIZE: 8.5pt; COLOR: #000000; FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; TEXT-DECORATION: none}"
						+ "		.normal {FONT-WEIGHT: normal; FONT-SIZE: 8.5pt; COLOR: #000000; FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; TEXT-DECORATION: none}"
						+ "		.normalNew {FONT-WEIGHT: normal; FONT-SIZE: 8.5pt; COLOR: #000000; FONT-FAMILY: Calibri; TEXT-DECORATION: none}"
						+ "		.bluenormal {FONT-WEIGHT: normal; FONT-SIZE: 8.5pt; COLOR: #6097cf; FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; TEXT-DECORATION: none}"
						+ "		.rednormal {FONT-WEIGHT: normal; FONT-SIZE: 8.5pt; COLOR: #ff0000; FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; TEXT-DECORATION: none}"
						+ "	</style>" + "</head>" + "<body>" + "	<form name = \"frmMessage\" method = \"post\">"

						+ "		<table width = \"100%\" border = \"0\" cellspacing = \"0\" cellpadding = \"0\" align = \"center\">"
						+ "			<tr>" + " <td width = \"100%\" class = \"normal\" align = \"center\">"
						+ "					<table width = \"100%\" border = \"0\" cellspacing = \"0\" cellpadding = \"0\" align = \"center\">"
						+ "						<tr>"
						+ "							<td colspan = \"3\" align = \"left\" class = \"normal\" height = \"30\">&nbsp;</td>"
						+ "							<td colspan = \"3\" align = \"left\" class = \"normal\">"
						+ "								<div align = \"justify\">" + msgText1 + "</div>"
						+ "							</td>" + "						</tr>"
						+ "					</table><br>"
						+ "					<table width = \"100%\" border = \"1\" cellspacing = \"0\" cellpadding = \"0\" align = \"center\">"
						+ "						<tr>"
						+ "							<td align = \"center\" class = \"normal\" height = \"30\"><b>Partner Name & Code.</b></td>"
						+ "							<td align = \"center\" class = \"normal\" height = \"30\"><b>PO No.</b></td>"
						+ "							<td align = \"center\" class = \"normal\" height = \"30\"><b>PO Date.</b></td>"
						+ "							<td align = \"center\" class = \"normal\" height = \"30\"><b>PO AMOUNT (Excl. Tax)</b></td>";
						
					htmlStr = htmlStr + " </tr>" 
							+ "<tr><td align = \"center\" class = \"normal\" height = \"30\">"+ name +"("+code +")" + "</td>"
							+ "<td align = \"center\" class = \"normal\" height = \"30\">"+ poNumber + "</td>"
							+ "<td align = \"center\" class = \"normal\" height = \"30\">"+ poDate + "</td>" 
							+ "<td align = \"center\" class = \"normal\" height = \"30\">"+ poAmount + "</td>";
				
					htmlStr = htmlStr + " </tr>" + "</table>" + "</td>" + "</tr>" + " </table>" + " </form>" + "</body>"+ "</html>";

					String htmlStr2 = htmlStr + "<br>Regards," + "<br>" + "Admin Team";
					htmlStr2 = htmlStr +"<br><p>This is an automated email, please do not reply to this email.</p><br>";
					
					htTable.put("content", htmlStr2);
					
					EmailImpl myEmail = new EmailImpl();
					
					boolean result = myEmail.sendHtmlMail(htTable, null, null);
					
					log.info("mail sending : " +result);
			}			
				rs.close();
				ps.close();
				
		} catch (Exception e) {
			log.error("poAcceptance() :", e.fillInStackTrace());
		}finally {
			DBConnection.closeConnection(rs, ps, null);
		}
	}


	public void poRejection(String PONumber,String message,Connection con) throws Exception {
		
		//Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String posql = "";
		
		try {

			posql = " SELECT PD.PONUMBER,TO_CHAR(PD.PODATE,'DD-MON-RRRR') PODATE,PD.POAMOUNT,PD.REQUSITIONER, " + 
					" BP.VENDORID,BP.BUSINESSPARTNERTEXT " + 
					" FROM PODETAILS PD, BUSINESSPARTNER BP WHERE BP.BUSINESSPARTNEROID =  PD.BUSINESSPARTNEROID " + 
					" AND PD.STATUS = ? AND PD.PONUMBER = ? ";

			log.info("Query = " + posql);

			ArrayList newPo = new ArrayList();
		
			con = DBConnection.getConnection();
			ps = con.prepareStatement(posql);
			ps.setString(1, "N");
			ps.setString(2, PONumber);
		
			rs = ps.executeQuery();
			
			while(rs.next()) {

				String poNumber =  rs.getString("PONUMBER");
				String poDate =  rs.getString("PODATE");
				String poAmount =  rs.getString("POAMOUNT");
				String requisitioner =  rs.getString("REQUSITIONER");
				String code = rs.getString("VENDORID");
				String name = rs.getString("BUSINESSPARTNERTEXT");	
				
				Hashtable<String, String> htTable = new Hashtable<String, String>();
				
				String toAddr = requisitioner;
				
				htTable.put("toAddr", toAddr);

				String msgText1 = "Dear Recipient,<br><br> PO number "+poNumber+" has been rejected by the vendor with comments - "+ message +".<br>";
										
				String subject = "PO : " + poNumber +" Rejected by Vendor";
				
				htTable.put("subject", subject);
				
				log.info("msgText1 = " + msgText1 + " and recipient ID = " + toAddr);

				String htmlStr = "<html>" + "\n" + "<head>" + "\n"
						+ "	<title> Advance shipping notification Emails </title>" + "\n" + "	<style>" + "\n"
						+ "		.bold {FONT-WEIGHT: bold; FONT-SIZE: 8.5pt; COLOR: #000000; FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; TEXT-DECORATION: none}"
						+ "		.normal {FONT-WEIGHT: normal; FONT-SIZE: 8.5pt; COLOR: #000000; FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; TEXT-DECORATION: none}"
						+ "		.normalNew {FONT-WEIGHT: normal; FONT-SIZE: 8.5pt; COLOR: #000000; FONT-FAMILY: Calibri; TEXT-DECORATION: none}"
						+ "		.bluenormal {FONT-WEIGHT: normal; FONT-SIZE: 8.5pt; COLOR: #6097cf; FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; TEXT-DECORATION: none}"
						+ "		.rednormal {FONT-WEIGHT: normal; FONT-SIZE: 8.5pt; COLOR: #ff0000; FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; TEXT-DECORATION: none}"
						+ "	</style>" + "</head>" + "<body>" + "	<form name = \"frmMessage\" method = \"post\">"

						+ "		<table width = \"100%\" border = \"0\" cellspacing = \"0\" cellpadding = \"0\" align = \"center\">"
						+ "			<tr>" + " <td width = \"100%\" class = \"normal\" align = \"center\">"
						+ "					<table width = \"100%\" border = \"0\" cellspacing = \"0\" cellpadding = \"0\" align = \"center\">"
						+ "						<tr>"
						+ "							<td colspan = \"3\" align = \"left\" class = \"normal\" height = \"30\">&nbsp;</td>"
						+ "							<td colspan = \"3\" align = \"left\" class = \"normal\">"
						+ "								<div align = \"justify\">" + msgText1 + "</div>"
						+ "							</td>" + "						</tr>"
						+ "					</table><br>"
						+ "					<table width = \"100%\" border = \"1\" cellspacing = \"0\" cellpadding = \"0\" align = \"center\">"
						+ "						<tr>"
						+ "							<td align = \"center\" class = \"normal\" height = \"30\"><b>Partner Name & Code</b></td>"
						+ "							<td align = \"center\" class = \"normal\" height = \"30\"><b>PO No.</b></td>"
						+ "							<td align = \"center\" class = \"normal\" height = \"30\"><b>PO Date.</b></td>"
						+ "							<td align = \"center\" class = \"normal\" height = \"30\"><b>PO AMOUNT (Excl. Tax)</b></td>";
						
					htmlStr = htmlStr + " </tr>" 
							+ "<tr><td align = \"center\" class = \"normal\" height = \"30\">"+ name +"("+ code +")" + "</td>" 
							+ "<td align = \"center\" class = \"normal\" height = \"30\">"+ poNumber + "</td>" 
							+ "<td align = \"center\" class = \"normal\" height = \"30\">"+ poDate + "</td>" 
							+ "<td align = \"center\" class = \"normal\" height = \"30\">"+ poAmount + "</td>";
				
					htmlStr = htmlStr + " </tr>" + "</table>" + "</td>" + "</tr>" + " </table>" + " </form>" + "</body>"+ "</html>";

					String htmlStr2 = htmlStr + "<br>Regards," + "<br>" + "Admin Team";
					htmlStr2 = htmlStr +"<br><p>This is an automated email, please do not reply to this email.</p><br>";
					
					htTable.put("content", htmlStr2);
					
					EmailImpl myEmail = new EmailImpl();
					
					boolean result = myEmail.sendHtmlMail(htTable, null, null);
					
					log.info("mail sending : " +result);
			}			
				rs.close();
				ps.close();
				
		} catch (Exception e) {
			log.error("poRejection() :", e.fillInStackTrace());
		}finally {
			DBConnection.closeConnection(rs, ps, null);
		}
	}
	
	public JSONArray getASNHistory(String po, String email) {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		String boId = null;
		String poNumber = null;
		String lineItem = null;
		String lineText = null;
		String balQty = null;
		String asnQty = null;
		String asnDate = null;
		String locName = null;
		String status = null;
		String asnNo = null;
		
		
		try {
			con = DBConnection.getConnection();
			/*
			String LocQuery = " SELECT B.BUSINESSPARTNEROID AS BOID,B.PONUMBER AS PONO,B.LINEITEMNUMBER AS LIITNO,B.LINEITEMTEXT AS LIITTX, " 
					+ " B.BALANCEQTY AS BQT,B.ASNQUANTITY AS ASNQTY,TO_CHAR(B.ASNDATE,'DD-MON-RRRR') AS ADATE, "
					+ "(SELECT DISTINCT A.LOCATIONNAME FROM LOCATIONMASTER A WHERE A.LOCATIONCODE = B.LOCATIONCODE) AS LOCNAME, "
					+ "B.STATUS AS STATUS,B.ASNNUMBER AS ASNO, REQNO, B.SUPPORTINGDOCNAME FROM ASNDETAILS B WHERE B.CREATEDBY = ? " 
					+ " ORDER BY B.LINEITEMNUMBER, B.MODIFIEDON DESC ";
			*/
			/*
			String LocQuery = "SELECT B.BUSINESSPARTNEROID AS BOID,B.PONUMBER AS PONO,B.LINEITEMNUMBER AS LIITNO,B.LINEITEMTEXT AS LIITTX,  " + 
					"B.BALANCEQTY AS BQT,B.ASNQUANTITY AS ASNQTY,TO_CHAR(B.ASNDATE,'DD-MON-RRRR') AS ADATE, " + 
					"(SELECT DISTINCT A.LOCATIONNAME FROM LOCATIONMASTER A WHERE A.LOCATIONCODE = B.LOCATIONCODE) AS LOCNAME, " + 
					"B.STATUS AS STATUS,B.ASNNUMBER AS ASNO, REQNO, B.SUPPORTINGDOCNAME FROM ASNDETAILS B,PODETAILS A  " + 
					"WHERE B.STATUS<>'D' AND A.PONUMBER = B.PONUMBER AND A.REQUSITIONER= ? AND B.PONUMBER = ? " + 
					" ORDER BY B.LINEITEMNUMBER, B.MODIFIEDON DESC ";
			*/
			
			String LocQuery = "SELECT DISTINCT B.REQNO,BP.BUSINESSPARTNERTEXT ||'-'|| BP.VENDORID AS BOID,B.PONUMBER AS PONO,B.LINEITEMNUMBER AS LIITNO, "
							+"B.LINEITEMTEXT AS LIITTX,B.ASNQUANTITY AS ASNQTY,TO_CHAR(B.ASNDATE,'DD-MON-RRRR') AS ADATE,"  
							+ " (SELECT DISTINCT A.LOCATIONNAME FROM LOCATIONMASTER A WHERE A.LOCATIONCODE = B.LOCATIONCODE) AS LOCNAME "
							+ " ,B.ASNNUMBER AS ASNO FROM ASNDETAILS B,PODETAILS A,BUSINESSPARTNER BP,LOCATIONMASTER LM "  
							+ " WHERE B.STATUS<>'D' AND A.PONUMBER = B.PONUMBER AND a.businesspartneroid = bp.businesspartneroid "
							+ " AND LM.LOCATIONCODE= B.LOCATIONCODE AND B.PONUMBER = ? "  
							+ " ORDER BY B.LINEITEMNUMBER ";
			
			//+ " AND LM.LOCATIONCODE= B.LOCATIONCODE AND LM.STOREKEEPEREMILID= ?  AND B.PONUMBER = ? "  
			
			ps = con.prepareStatement(LocQuery);
		//	ps.setString(1, email);
			ps.setString(1, po);
			rs = ps.executeQuery();
			
			List<List<String>> dataList = new ArrayList<List<String>>();
			
			while (rs.next()) {
	
				ArrayList<String> asnList = new ArrayList<String>();
				 asnList.add(rs.getString("BOID"));
				 asnList.add(rs.getString("PONO"));
				 asnList.add(rs.getString("LIITNO"));
				 asnList.add(rs.getString("LIITTX"));
				// asnList.add(rs.getString("BQT"));
				 asnList.add(rs.getString("ASNQTY"));
				 asnList.add(rs.getString("ADATE"));
				 asnList.add(rs.getString("LOCNAME"));
				 
				 /*
					if ("N".equalsIgnoreCase(rs.getString("Status"))) {
						asnList.add("New");
					} else if ("A".equalsIgnoreCase(rs.getString("Status"))) {
						asnList.add("Accepted");
					} else if ("P".equalsIgnoreCase(rs.getString("Status"))) {
						asnList.add("Pending");
					} else if ("W".equalsIgnoreCase(rs.getString("Status"))) {
						asnList.add("Work In Progress");
					} else if ("S".equalsIgnoreCase(rs.getString("Status"))) {
						asnList.add("Shipped");
					} else if ("C".equalsIgnoreCase(rs.getString("Status"))) {
						asnList.add("Complete");
					}
				*/	
				 asnList.add(rs.getString("ASNO"));
				// asnList.add(rs.getString("REQNO"));
				// asnList.add(rs.getString("SUPPORTINGDOCNAME"));
				 dataList.add(asnList);
				 
			}

			rs.close();
			ps.close();
			
			if (dataList.isEmpty() == true) {
				responsejson.put("message", "No Data Found");
				responsejson.put("StatusCode", "203");
			} else {
				String encodedfile = downloadExcelfile(dataList);
				responsejson.put("message", "Success");
				responsejson.put("StatusCode", "200");
				responsejson.put("data", encodedfile);
			}

			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("getASNList() :", e.fillInStackTrace());
			responsejson.put("StatusCode", "406");

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}
	
	private String downloadExcelfile(List<List<String>> totallist) {
		String encodedfile = "";
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("ASN Report");
			int rowIndex = 0;
			int cellIndex = 0; 
			XSSFRow row = sheet.createRow(rowIndex++);
			
			row.createCell(cellIndex++).setCellValue("PARTNER NAME AND CODE");
			row.createCell(cellIndex++).setCellValue("PO NUMBER");
			row.createCell(cellIndex++).setCellValue("LINE ITEM NUMBER");
			row.createCell(cellIndex++).setCellValue("LINE ITEM DESCRIPTION");
			//row.createCell(cellIndex++).setCellValue("BALANCE QUANTITY");
			row.createCell(cellIndex++).setCellValue("ASN QUANTITY");
			row.createCell(cellIndex++).setCellValue("ASN DATE");
			row.createCell(cellIndex++).setCellValue("LOCATION NAME");
			//row.createCell(cellIndex++).setCellValue("STATUS");
			row.createCell(cellIndex++).setCellValue("ASN NUMBER");
			//row.createCell(cellIndex++).setCellValue("REQUEST NO");
			//row.createCell(cellIndex++).setCellValue("ASN SUPPORTING DOC NAME");

			Iterator<List<String>> i = totallist.iterator();
			
			while (i.hasNext()) {

				List<String> templist = (List<String>) i.next();
				Iterator<String> tempIterator = templist.iterator();
				cellIndex = 0;
				row = sheet.createRow(rowIndex++); 				

				while (tempIterator.hasNext()) {
					String data = (String) tempIterator.next();
					row.createCell(cellIndex++).setCellValue(data);
				}
			}
			
			workbook.write(stream);
			workbook.close();
			streamWriter.flush();
			byte[] byteArrayOutputStream = stream.toByteArray();

			try {
				encodedfile = new String(Base64.encodeBase64(byteArrayOutputStream), "UTF-8");
			} catch (IOException e) {
				log.error("asn downloadExcelfile() 1: " + e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("asn downloadExcelfile() 2: " + e.fillInStackTrace());

		}
		return encodedfile;
	}
}
