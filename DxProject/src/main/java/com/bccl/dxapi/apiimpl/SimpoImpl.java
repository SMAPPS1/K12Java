package com.bccl.dxapi.apiimpl;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.apiutility.DXPortalException;
import com.bccl.dxapi.apiutility.JcoGetDataFromSAP;
import com.bccl.dxapi.apiutility.Pagination;
import com.bccl.dxapi.apiutility.Validation;
import com.bccl.dxapi.bean.EndUserReturn;
import com.bccl.dxapi.bean.Invoicesubmission;
import com.bccl.dxapi.bean.SimpoDeliveryItems;

public class SimpoImpl {

	static Logger log = Logger.getLogger(SimpoImpl.class.getName());

	POImpl poImpl = null;
	InternalportalImpl internal = null;

	public SimpoImpl() {
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

	public JSONArray getSimPoDetailEvent(List<String> po_num_list, String id) throws SQLException {

		boolean result = false;
		if (po_num_list != null && !po_num_list.isEmpty()) {
			result = true;
		}
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String po_data = "SELECT * FROM poeventdetails where PONumber =? AND BUSINESSPARTNEROID=?";

		String uniquePoInCount = "Select count(*) as counter from PONINVOICESUMMERY where "
				+ "BUSINESSPARTNEROID = ?  and PONUMBER = ? AND INVOICENUMBER IS NOT NULL";

		int count = 0;
		int count1 = 0;
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			for (int i = 0; i < po_num_list.size(); i++) {

				ps = con.prepareStatement(po_data);
				ps.setString(1, po_num_list.get(i));
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
					poData.put("CURRENCY", rs.getString("Currency"));
					poData.put("STORAGELOCATION", rs.getString("STORAGELOCATION"));
					poData.put("SERVICENUMBER", rs.getString("SERVICE"));
					poData.put("FORECLOSESTATUSCHECK", rs.getString("FORECLOSESTATUSCHECK"));
					POList.add(poData);
				}
				rs.close();
				ps.close();
			}

			try {

				for (int i = 0; i < po_num_list.size(); i++) {
					ps = con.prepareStatement(uniquePoInCount);
					ps.setString(1, id);
					ps.setString(2, po_num_list.get(i));

					rs = ps.executeQuery();
					while (rs.next()) {
						count += rs.getInt("counter");
					}
					rs.close();
					ps.close();
				}
				if (count > 0) {
					responsejson.put("deliveryitem", "present");
				} else {
					responsejson.put("deliveryitem", "absent");
				}
			} catch (Exception e) {
				log.error("getSimPoDetailEvent() 1 : ", e.fillInStackTrace());
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
			log.error("getSimPoDetailEvent() 2 : ", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getSimPoProcessedPos(String bid, String plantCode, String companyCode) {

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
		poImpl = new POImpl();

		String compCodeQuery = " AND pd.companycode = ? ";

		try {

			String po_data = "Select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT"
					+ ",pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,"
					+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,"
					+ "pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,"
					+ "pd.PURCHASINGGROUP,pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT "
					+ "from podetails pd join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where "
					+ "pd.BusinessPartnerOID=? and poe.PLANT = ?" + compCodeQuery + " and pd.Status IN ('A','P')";
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, bid);
			ps.setString(2, plantCode);
			ps.setString(3, companyCode);
			rs = ps.executeQuery();

			ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();

			while (rs.next()) {

				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("PO_NUMBER", rs.getString("PONumber"));
				poData.put("DATE", rs.getString("PODate"));
				poData.put("AMOUNT", rs.getString("POAmount"));
				poData.put("STATUS", rs.getString("Status"));
				poData.put("Quantity", rs.getString("Quantity"));
				poData.put("COMPANY", rs.getString("Company"));
				poData.put("PLANT", rs.getString("Plant"));
				poData.put("PLANTNAME", poImpl.getPlantName(rs.getString("Plant"), con));
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
		} catch (Exception e) {
			log.error("getSimPoProcessedPos() :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray insertSimpoInvoice(List<Invoicesubmission> persons, String vendorEmail) throws SQLException {

		if (persons != null && !persons.isEmpty()) {
			if ("reopen".equalsIgnoreCase(persons.get(0).getType())) {

				SimpoImpl simpoimpl = new SimpoImpl();
				try {
					return simpoimpl.updateReopenedInvoice(persons);
				} catch (SQLException e) {
					log.error("insertSimpoInvoice() 1 : ", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "SQLException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} catch (DXPortalException e) {
					log.error("insertSimpoInvoice() 2 : ", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "DXPortalException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} catch (ParseException e) {
					log.error("insertSimpoInvoice() 3 : ", e.fillInStackTrace());
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
		poImpl = new POImpl();
		boolean validation = poImpl.validateSubmitInvoice(persons);
		if (validation == false) {

			jsonArray = poImpl.jsonArray;
			poImpl.deletebaddeliveries(persons);
			return jsonArray;
		}

		int counterSupp = 0;
		String poNum = "";
		String invNum = "";
		LinkedHashSet<String> poNumHs = new LinkedHashSet<String>();
		ArrayList<ArrayList<Invoicesubmission>> poWiseInvSubList = new ArrayList<ArrayList<Invoicesubmission>>();
		boolean approvalFlag = false;

		String allPo = "";

		try {
			con = DBConnection.getConnection();

			int pocount = 0;
			int poincount = 0;
			double balance = 0;

			for (int i = 0; i < persons.size(); i++) {

				poNum = persons.get(i).getPo_num();
				invNum = persons.get(i).getInvoiceNumber();
				poNumHs.add(poNum);

				if (persons.get(i).getGrnnumber().equalsIgnoreCase("-")) {
					balance = poImpl.getBalanceCount(persons.get(i).getPo_num(), persons.get(i).getLineItemNumber(),
							persons.get(i).getBid(), con);
					if (balance < Double.parseDouble(persons.get(i).getQuantity())) {
						List<Invoicesubmission> invSubLis = getPoWiseInvoiceSubmissionDetails(persons,
								persons.get(i).getPo_num());
						poImpl.updategrnstatus(invSubLis, "B", con);
						responsejson.put("Uniquemessage", "Insufficient balance to create invoice");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						poImpl.deletebaddeliveries(persons);
						return jsonArray;
					}
				}

				if (persons.get(i).getGrnnumber().equalsIgnoreCase("-")) {
					boolean alreadypresent = false;
					int checkinvoiceingrn = checksimpoinvoiceingrntable(persons.get(i), con);
					if (checkinvoiceingrn > 0) {
						alreadypresent = true;
					}
					if (alreadypresent) {
						responsejson.put("Uniquemessage",
								"INVOICENUMBER is already present in to-be-invoice list. Please  use a different invoice number.");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						poImpl.deletebaddeliveries(persons);
						return jsonArray;
					}
				}

				poincount = poImpl.getUniquePONInCheck(persons.get(i).getInvoiceNumber(), con, persons.get(i).getBid(),
						persons.get(i).getInvoiceDate());
				if (poincount > 0) {
					grnerror = true;

					responsejson.put("Uniquemessage",
							"Invoice number already exists. Please  use a different invoice number");

					poImpl.deletebaddeliveries(persons);
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
						poImpl.deletebaddeliveries(persons);
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
			log.error("insertSimpoInvoice() 4 : ", e.fillInStackTrace());
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", poNum);
			responsejson.put("invoicenumber", invNum);
			jsonArray.add(responsejson);
			return jsonArray;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		String basePO = null;
		for (String po : poNumHs) {
			ArrayList<Invoicesubmission> invSubLis = new ArrayList<Invoicesubmission>();
			invSubLis = getPoWiseInvoiceSubmissionDetails(persons, po);
			poWiseInvSubList.add(invSubLis);

			allPo = allPo + po + ",";

			if (basePO == null) {
				basePO = po;
			}
		}
		allPo = allPo.substring(0, allPo.length() - 1);

		for (List<Invoicesubmission> invoiceSubmission : poWiseInvSubList) {

			String marterialCode = "";
			String deliveryUniqueNoString = null;
			double totalquantity = 0;
			StringBuffer grnnumber = new StringBuffer();
			StringBuffer srcnnumber = new StringBuffer();
			Set<String> s = new HashSet<String>();
			Set<String> s1 = new HashSet<String>();
			for (int i = 0; i < invoiceSubmission.size(); i++) {
				s.add(invoiceSubmission.get(i).getGrnnumber());
				s1.add(invoiceSubmission.get(i).getSrcnnumber());
				totalquantity = totalquantity + Double.parseDouble(invoiceSubmission.get(i).getQuantity());

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
						+ "CONTACTPERSON,CONTACTPERSONPHONE,REMARK,TOTALAMTINCTAXES,TAXAMOUNT,GRNNUMBER,SCRNNUMBER,UNIQUEREFERENCENUMBER,MPO,ALLPO,BASEPO)"
						+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				ps = con.prepareStatement(querySummary);
				ps.setString(1, invoiceSubmission.get(0).getInvoiceNumber());
				ps.setString(2, invoiceSubmission.get(0).getPo_num());
				ps.setString(3, invoiceSubmission.get(0).getBid());
				ps.setString(4, "N");
				ps.setString(5, invoiceSubmission.get(0).getContactPerson());
				ps.setString(6, invoiceSubmission.get(0).getBuyerid());
				ps.setString(7, invoiceSubmission.get(0).getTotalAmount());
				ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setInt(9, 1);
				ps.setInt(10, 0);
				ps.setString(11, invoiceSubmission.get(0).getStatus());
				ps.setDate(12, new java.sql.Date(
						new SimpleDateFormat("dd/MM/yyyy").parse(invoiceSubmission.get(0).getInvoiceDate()).getTime()));

				ps.setString(13, invoiceSubmission.get(0).getMaterial());
				ps.setDouble(14, totalquantity);
				if (invoiceSubmission.get(0).getGrnnumber().equalsIgnoreCase("-")) {
					ps.setString(15, null);
				} else {
					ps.setString(15, "R");
				}
				ps.setString(16, invoiceSubmission.get(0).getActualfilename());
				ps.setString(17, invoiceSubmission.get(0).getSavedfilename());
				ps.setString(18, invoiceSubmission.get(0).getPlant());
				ps.setString(19, invoiceSubmission.get(0).getIrnNumber());
				if (invoiceSubmission.get(0).getIrnDate() != null && invoiceSubmission.get(0).getIrnDate() != ""
						&& invoiceSubmission.get(0).getIrnDate() != "null") {
					ps.setDate(20, new java.sql.Date(
							new SimpleDateFormat("dd/MM/yyyy").parse(invoiceSubmission.get(0).getIrnDate()).getTime()));
				} else {
					ps.setString(20, invoiceSubmission.get(0).getIrnDate());
				}
				ps.setString(21, invoiceSubmission.get(0).getDescription());
				ps.setString(22, invoiceSubmission.get(0).getCreatedby());
				ps.setString(23, invoiceSubmission.get(0).getBusinessPartnerText());
				ps.setString(24, invoiceSubmission.get(0).getVendorID());
				if (invoiceSubmission.get(0).getBillofladingdate() != null
						&& !"".equals(invoiceSubmission.get(0).getBillofladingdate())
						&& !("Invalid date").equalsIgnoreCase(invoiceSubmission.get(0).getBillofladingdate())) {

					ps.setDate(25, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy")
							.parse(invoiceSubmission.get(0).getBillofladingdate()).getTime()));
				} else {
					ps.setDate(25, null);
				}
				ps.setString(26, invoiceSubmission.get(0).getContactPerson());
				ps.setString(27, invoiceSubmission.get(0).getContactPersonPhone());
				ps.setString(28, invoiceSubmission.get(0).getRemark());
				ps.setString(29, invoiceSubmission.get(0).getTotalamtinctaxes());
				ps.setString(30, invoiceSubmission.get(0).getTaxamount());
				if (invoiceSubmission.get(0).getGrnnumber().equalsIgnoreCase("-")) {
					ps.setString(31, null);
				} else {
					ps.setString(31, grnnumber.toString());
				}
				if (invoiceSubmission.get(0).getSrcnnumber().equalsIgnoreCase("-")) {
					ps.setString(32, null);
				} else {
					ps.setString(32, srcnnumber.toString());
				}
				if (invoiceSubmission.get(0).getGrnnumber().equalsIgnoreCase("-")) {
					ps.setString(33, null);
				} else {
					ps.setString(33, "Y");
				}

				ps.setString(34, "Y");
				ps.setString(35, allPo);
				ps.setString(36, basePO);

				ps.executeUpdate();
				ps.close();

				String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
						+ "MODIFIEDTIME,MODIFIEDBY,RESUBMITTEDINVOICENO)" + " values(?,?,?,?,?,?,?)";

				ps = con.prepareStatement(insertaudit);
				ps.setString(1, invoiceSubmission.get(0).getInvoiceNumber());
				ps.setString(2, invoiceSubmission.get(0).getPo_num());
				ps.setString(3, invoiceSubmission.get(0).getBid());
				if ("".equalsIgnoreCase(invoiceSubmission.get(0).getBeforesubmissioninvoicenumber())
						|| "null".equalsIgnoreCase(invoiceSubmission.get(0).getBeforesubmissioninvoicenumber())
						|| invoiceSubmission.get(0).getBeforesubmissioninvoicenumber() == null) {
					ps.setString(4, invoiceSubmission.get(0).getStatus());
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//					ps.setString(6, invoiceSubmission.get(0).getContactPerson());
					ps.setString(6, vendorEmail);
					ps.setString(7, invoiceSubmission.get(0).getBeforesubmissioninvoicenumber());
				} else {
					ps.setString(4, "S");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//					ps.setString(6, invoiceSubmission.get(0).getContactPerson());
					ps.setString(6, vendorEmail);
					ps.setString(7, invoiceSubmission.get(0).getBeforesubmissioninvoicenumber());
				}
				ps.executeUpdate();
				ps.close();

				// Inserting into INVOICESUPPDOCS
				String insertsupportingdocument = "insert into INVOICESUPPDOCS (BUSINESSPARTNEROID,"
						+ "INVOICENUMBER,PONUMBER,ACTUALFILENAME,SAVEDFILENAME) values " + "(?,?,?,?,?)";

				ps = con.prepareStatement(insertsupportingdocument);
				ps.setString(1, invoiceSubmission.get(0).getBid());
				ps.setString(2, invoiceSubmission.get(0).getInvoiceNumber());
				ps.setString(3, invoiceSubmission.get(0).getPo_num());
				ps.setString(4, invoiceSubmission.get(0).getMultipleactualfilename());
				ps.setString(5, invoiceSubmission.get(0).getMultiplesavedfilename());
				ps.executeUpdate();
				ps.close();
				counterSupp++;

				// Updating or Inserting in DELIVERYSUMMARY Table
				for (int i = 0; i < invoiceSubmission.size(); i++) {
					deliveryUniqueNoString = invoiceSubmission.get(i).getOrderNumber();
					poImpl.updatedeliverysumary(invoiceSubmission.get(i).getPo_num(),
							invoiceSubmission.get(i).getLineItemNumber(), invoiceSubmission.get(i).getInvoiceNumber(),
							invoiceSubmission.get(i).getOrderNumber(), invoiceSubmission.get(i).getInvoiceDate(),
							invoiceSubmission.get(i).getQuantity(), invoiceSubmission.get(i).getTotalAmount(),
							invoiceSubmission.get(i).getuOM(), invoiceSubmission.get(i).getRateperquantity(),
							invoiceSubmission.get(i).getLineitemtext(), invoiceSubmission.get(i).getStatus(),
							invoiceSubmission.get(i).getInvoiceamount(), invoiceSubmission.get(i).getStoragelocation(),
							invoiceSubmission.get(i).getGrnnumber(),
							invoiceSubmission.get(i).getUniquereferencenumber(),
							invoiceSubmission.get(i).getSaplineitemnumber(),
							invoiceSubmission.get(i).getServicenumber(), con, invoiceSubmission.get(i).getSrcnnumber());
				}

				// updating balance in poeventdetails table
				poImpl.executeUpdateBalance(con, invoiceSubmission);

				for (int i = 0; i < invoiceSubmission.size(); i++) {
					String queryPolineitem = "Select * from inventoryuserlist where MTYP = "
							+ "(select MATERIAL_TYPE from poeventdetails  where (ponumber = ? and lineitemnumber = ? "
							+ "and businesspartneroid = ? and ordernumber is null)) AND plant = (select PLANT "
							+ "from poeventdetails  where (ponumber = ? and lineitemnumber =? and businesspartneroid =? "
							+ "and ordernumber is null))";

					ps = con.prepareStatement(queryPolineitem);
					ps.setString(1, invoiceSubmission.get(i).getPo_num());
					ps.setString(2, invoiceSubmission.get(i).getLineItemNumber());
					ps.setString(3, invoiceSubmission.get(i).getBid());
					ps.setString(4, invoiceSubmission.get(i).getPo_num());
					ps.setString(5, invoiceSubmission.get(i).getLineItemNumber());
					ps.setString(6, invoiceSubmission.get(i).getBid());
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
				poImpl.getUpdateinvoiceeventdetailwopo(invoiceSubmission.get(0).getInvoiceNumber(),
						invoiceSubmission.get(0).getBid(), invoiceSubmission.get(0).getPo_num(), con);

				String status = null;
				String buyerId = null;

				if (!approvalFlag) {

					String sqlUpdate1 = "insert into invoiceapproval (VENDORID,INVOICENUMBER,PONUMBER,BUYERID,ENDUSEID,"
							+ "ENDUSERSTATUS,STAGE,MODIFIEDDATE,INVOICEDATE,STATUS,PROXY,MPO) values (?,?,?,?,?,?,?,?,?,?,?,?)";
					ps = con.prepareStatement(sqlUpdate1);
					ps.setString(1, invoiceSubmission.get(0).getVendorID());
					ps.setString(2, invoiceSubmission.get(0).getInvoiceNumber());
					ps.setString(3, invoiceSubmission.get(0).getPo_num());
					ps.setString(4, invoiceSubmission.get(0).getBuyerid());
					ps.setString(5, invoiceSubmission.get(0).getContactPerson());
					ps.setString(6, "P");
					ps.setString(7, invoiceSubmission.get(0).getStage());
					ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setDate(9, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy")
							.parse(invoiceSubmission.get(0).getInvoiceDate()).getTime()));
					ps.setString(10, "M");
					if (marterialCode != null && marterialCode != "") {
						ps.setString(11, "X");
					} else {
						ps.setString(11, null);
					}
					ps.setString(12, "Y");
					ps.executeUpdate();
					ps.close();

					approvalFlag = true;
				}

				// Update GRNMAPPING
				String updategrn = "Update GRNMAPPING set STATUS=?,INVOICENUMBER=? where PONUMBER=? AND "
						+ "LINEITEMNO=? AND DCNUMBER=? AND GRNNUMBER=?";

				for (int j = 0; j < invoiceSubmission.size(); j++) {
					if (!("-").equalsIgnoreCase(invoiceSubmission.get(j).getGrnnumber())) {
						ps = con.prepareStatement(updategrn);
						ps.setString(1, "D");
						ps.setString(2, invoiceSubmission.get(j).getInvoiceNumber());
						ps.setString(3, invoiceSubmission.get(j).getPo_num());
						ps.setString(4, invoiceSubmission.get(j).getLineItemNumber());
						ps.setString(5, invoiceSubmission.get(j).getDcnumber());
						ps.setString(6, invoiceSubmission.get(j).getGrnnumber());
						ps.executeUpdate();
						ps.close();
					}
				}

				con.commit();
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);

			} catch (DXPortalException dxp) {
				con.rollback();
				poImpl.deletebaddeliveries(invoiceSubmission);
				responsejson.put("error", dxp.reason);
				responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
				responsejson.put("message", "SQL Error while submitting Invoice !!");
				responsejson.put("ponumber", invoiceSubmission.get(0).getPo_num());
				responsejson.put("invoicenumber", invoiceSubmission.get(0).getInvoiceNumber());
				jsonArray.add(responsejson);
				log.error("insertSimpoInvoice() 5 : ", dxp.fillInStackTrace());
				return jsonArray;

			} catch (Exception e) {
				log.error("insertSimpoInvoice() 6 : ", e.fillInStackTrace());
				con.rollback();
				responsejson.put("error", e.getLocalizedMessage());
				responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
				responsejson.put("message", "SQL Error while submitting Invoice !!");
				responsejson.put("ponumber", invoiceSubmission.get(0).getPo_num());
				responsejson.put("invoicenumber", invoiceSubmission.get(0).getInvoiceNumber());
				jsonArray.add(responsejson);
				return jsonArray;
			} finally {
				DBConnection.closeConnection(rs, ps, con);
			}
		}
		return jsonArray;
	}

	private int checksimpoinvoiceingrntable(Invoicesubmission persons, Connection con)
			throws SQLException, DXPortalException {

		String gettobeinvoicednumber = "Select count(*) as counter from GRNMAPPING where "
				+ "PONUMBER=? and DCNUMBER=? AND STATUS IS NULL";
		int count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(gettobeinvoicednumber);
			ps.setString(1, persons.getPo_num());
			ps.setString(2, persons.getDcnumber());
			rs = ps.executeQuery();
			rs.next();
			count = rs.getInt("counter");
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("checksimpoinvoiceingrntable() :", e.fillInStackTrace());
			throw new DXPortalException("Error in To-Be-Invoice Submission !!", "SQL Error in checkinvoiceingrntable.");
		}
		return count;
	}

	public ArrayList<Invoicesubmission> getPoWiseInvoiceSubmissionDetails(List<Invoicesubmission> invSubLis,
			String po_num) {

		ArrayList<Invoicesubmission> updatedInvSubLis = new ArrayList<Invoicesubmission>();

		if (invSubLis != null && !invSubLis.isEmpty()) {
			for (Invoicesubmission obj : invSubLis) {
				if (obj.getPo_num().equals(po_num)) {
					updatedInvSubLis.add(obj);
				}
			}
		}
		return updatedInvSubLis;
	}

	public JSONArray createcustomdeliveryitemsforsimpo(List<String> itemList, String id) {

		List<SimpoDeliveryItems> items = new ArrayList<SimpoDeliveryItems>();
		try {

			if (itemList == null || itemList.isEmpty()) {
				responsejson.put("text", "Delivery improper");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Delivery improper");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			String[] itemListArray = itemList.toArray(new String[itemList.size()]);
			String ponumber = "";
			String lineitemnumber = "";
			String quantity = "";
			String dcn = "";

			for (int counterValue = 0; counterValue < itemList.size(); counterValue++) {
				String[] fatchedValues = itemListArray[counterValue].split(",");
				ponumber = fatchedValues[0];
				lineitemnumber = fatchedValues[1];
				quantity = fatchedValues[2];
				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setPonumber(ponumber.trim());
				obj.setLineitemnumber(lineitemnumber.trim());
				obj.setQuantity(quantity.trim());
				obj.setDcn(dcn.trim());
				items.add(obj);
			}
		} catch (Exception e) {
			responsejson.put("text", "Delivery improper!");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Delivery improper!");
			jsonArray.add(responsejson);
			log.error("createcustomdeliveryitemsforsimpo() 1 :", e.fillInStackTrace());
			return jsonArray;
		}

		if (!validateCreateDelivery(items)) {
			return jsonArray;
		}

		String result = "";
		poImpl = new POImpl();
		ArrayList<HashMap<String, String>> autodcnvalues = new ArrayList<HashMap<String, String>>();
		for (SimpoDeliveryItems item : items) {
			try {

				result = poImpl.deliverysummaryInsert(item.getPonumber(), item.getLineitemnumber(), null,
						item.getQuantity(), id, "");
				HashMap<String, String> innerMap = new HashMap<String, String>();

				innerMap.put("PONUMBER", item.getPonumber());
				innerMap.put("LINEITEMNUMBER", item.getLineitemnumber());
				innerMap.put("DCNNUM", result);
				autodcnvalues.add(innerMap);

			} catch (DXPortalException dxp) {
				log.error("createcustomdeliveryitemsforsimpo() 2 :", dxp.fillInStackTrace());
				responsejson.put("error", dxp.reason);
			} catch (Exception e) {
				log.error("createcustomdeliveryitemsforsimpo() 3 :", e.fillInStackTrace());
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

	public JSONArray createcustomdeliveryitemsforsimpoWithDC(List<String> itemList, String id) {

		List<SimpoDeliveryItems> items = new ArrayList<SimpoDeliveryItems>();
		try {

			if (itemList == null || itemList.isEmpty()) {
				responsejson.put("text", "Delivery improper");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Delivery improper");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			String[] itemListArray = itemList.toArray(new String[itemList.size()]);
			String ponumber = "";
			String lineitemnumber = "";
			String quantity = "";
			String dcn = "";

			for (int counterValue = 0; counterValue < itemList.size(); counterValue++) {
				String[] fatchedValues = itemListArray[counterValue].split(",");
				ponumber = fatchedValues[0];
				lineitemnumber = fatchedValues[1];
				quantity = fatchedValues[2];
				dcn = fatchedValues[3];
				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setPonumber(ponumber.trim());
				obj.setLineitemnumber(lineitemnumber.trim());
				obj.setQuantity(quantity.trim());
				obj.setDcn(dcn.trim());
				items.add(obj);
			}
		} catch (Exception e) {
			responsejson.put("text", "Delivery improper!");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Delivery improper!");
			jsonArray.add(responsejson);
			log.error("createcustomdeliveryitemsforsimpo() 1 :", e.fillInStackTrace());
			return jsonArray;
		}

		if (!validateCreateDelivery(items)) {
			return jsonArray;
		}

		String result = "";
		poImpl = new POImpl();
		ArrayList<HashMap<String, String>> autodcnvalues = new ArrayList<HashMap<String, String>>();
		for (SimpoDeliveryItems item : items) {
			try {

				result = poImpl.deliverysummaryInsert(item.getPonumber(), item.getLineitemnumber(), null,
						item.getQuantity(), id, "");
				HashMap<String, String> innerMap = new HashMap<String, String>();

				innerMap.put("PONUMBER", item.getPonumber());
				innerMap.put("LINEITEMNUMBER", item.getLineitemnumber());
				innerMap.put("OLDDC", item.getDcn());
				innerMap.put("DCNNUM", result);
				autodcnvalues.add(innerMap);

			} catch (DXPortalException dxp) {
				log.error("createcustomdeliveryitemsforsimpo() 2 :", dxp.fillInStackTrace());
				responsejson.put("error", dxp.reason);
			} catch (Exception e) {
				log.error("createcustomdeliveryitemsforsimpo() 3 :", e.fillInStackTrace());
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

	public boolean validateCreateDelivery(List<SimpoDeliveryItems> items) {
		boolean result;

		result = items.isEmpty();

		for (SimpoDeliveryItems item : items) {
			result = Validation.StringChecknull(item.getPonumber());
			if (result == false) {
				responsejson.put("text", "Delivery improper");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "ponumber is null");
				jsonArray.add(responsejson);
				return false;
			}
			if (item.getLineitemnumber() == null || item.getLineitemnumber().equals("")) {
				responsejson.put("text", "Delivery improper");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "lineitemnumber is null");
				jsonArray.add(responsejson);
				return false;
			}
			if (item.getQuantity() == null || item.getQuantity().equals("")) {
				responsejson.put("text", "Delivery improper");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "quantity size is null");
				jsonArray.add(responsejson);
				return false;
			}
		}
		return true;
	}

	public JSONArray getorderhavingdcnforsimpo(String bid, List<String> itemList) {

		List<SimpoDeliveryItems> items = new ArrayList<SimpoDeliveryItems>();
		try {
			if (itemList == null || itemList.isEmpty()) {
				responsejson.put("text", "Delivery improper");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Delivery improper");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			String[] itemListArray = itemList.toArray(new String[itemList.size()]);
			String ponumber = "";
			String lineitemnumber = "";
			String quantity = "";
			String dcn = "";

			for (int counterValue = 0; counterValue < itemList.size(); counterValue++) {
				String[] fatchedValues = itemListArray[counterValue].split(",");
				ponumber = fatchedValues[0];
				dcn = fatchedValues[1];
				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setPonumber(ponumber.trim());
				obj.setLineitemnumber(lineitemnumber.trim());
				obj.setQuantity(quantity.trim());
				obj.setDcn(dcn.trim());
				items.add(obj);
			}
		} catch (Exception e) {
			responsejson.put("text", "Delivery improper!");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Delivery improper!");
			jsonArray.add(responsejson);
			return jsonArray;
		}
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
		for (SimpoDeliveryItems item : items) {
			result = Validation.StringChecknull(item.getPonumber());
			if (!result) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "PO number value is missing");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			if (!Validation.StringChecknull(item.getDcn())) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "DCN value is missing!");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String orderitemquery = "select * from DELIVERYSUMMARY where BUSSINESSPARTNEROID = ? and PONUMBER = ? and DC = ? ";
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {

			con = DBConnection.getConnection();
			for (SimpoDeliveryItems item : items) {

				log.info("Item : " + item.getDcn() + " / " + item.getPonumber() + " / " + bid);

				ps = con.prepareStatement(orderitemquery);
				ps.setString(1, bid);
				ps.setString(2, item.getPonumber().trim());
				ps.setString(3, item.getDcn().trim());
				String dcNumber = item.getDcn().trim() == null ? "-" : item.getDcn().trim();
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
					if (!t.equals("")) {
						poData.put("INVOICEDATE",
								new SimpleDateFormat("dd-MMM-yyyy").format(rs.getTimestamp("INVOICEDATE")) == null ? ""
										: new SimpleDateFormat("dd-MMM-yyyy").format(rs.getTimestamp("INVOICEDATE")));
					} else {
						poData.put("INVOICEDATE", null);
					}
					poData.put("OLDDCNUMBER", dcNumber);
					POList.add(poData);
				}
				rs.close();
				ps.close();

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
			log.error("getorderhavingdcnforsimpo() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray deleteemptydeliveries(List<String> itemList) {

		List<SimpoDeliveryItems> items = new ArrayList<SimpoDeliveryItems>();
		try {
			if (itemList == null || itemList.isEmpty()) {
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			log.info("Items List - " + itemList);
			String[] itemListArray = itemList.toArray(new String[itemList.size()]);
			String ponumber = "";
			String lineitemnumber = "";
			String quantity = "";
			String dcn = "";

			for (int counterValue = 0; counterValue < itemList.size(); counterValue++) {
				String[] fatchedValues = itemListArray[counterValue].split(",");
				ponumber = fatchedValues[0];
				dcn = fatchedValues[1];
				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setPonumber(ponumber.trim());
				obj.setLineitemnumber(lineitemnumber.trim());
				obj.setQuantity(quantity.trim());
				obj.setDcn(dcn.trim());
				items.add(obj);
			}
		} catch (Exception e) {
			responsejson.put("text", "Delivery improper!");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Delivery improper!");
			jsonArray.add(responsejson);
			log.error("deleteemptydeliveries() 1 :", e.fillInStackTrace());
			return jsonArray;
		}

		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			con = DBConnection.getConnection();
			String deletebaddelivery = "DELETE FROM DELIVERYSUMMARY WHERE  PONUMBER=? and DC=? and INVOICENUMBER is null";
			for (SimpoDeliveryItems item : items) {
				ps = con.prepareStatement(deletebaddelivery);
				ps.setString(1, item.getPonumber());
				ps.setString(2, item.getDcn());
				ps.executeUpdate();
				ps.close();
			}
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("deleteemptydeliveries() 2 :", e.fillInStackTrace());
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
				+ "sd.SAVEDFILENAME AS MULTISAVEDFILENAME,i.SERVICENUMBER,ps.IRNNUMBER,ps.IRNDATE, ps.NOTIFYENDUSEREMAILID "
				+ "from DELIVERYSUMMARY i join poninvoicesummery ps on i.InvoiceNumber=ps.InvoiceNumber "
				+ "and i.PONumber=ps.PONumber join INVOICESUPPDOCS sd on "
				+ "sd.INVOICENUMBER = ps.InvoiceNumber and  sd.PONUMBER = ps.PONumber "
				+ "where i.InvoiceNumber=? and i.PONumber=? order by i.LINEITEMNUMBER";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		InvoiceDetailsImpl invImpl = new InvoiceDetailsImpl();
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
					invoiceData.put("ACCEPTTYPE",
							invImpl.getacceptancetype(rs.getString("InvoiceNumber"), rs.getString("PONumber"), con));
					invoiceData.put("NOTIFYENDUSEREMAILID",
							rs.getString("NOTIFYENDUSEREMAILID") == null ? "" : rs.getString("NOTIFYENDUSEREMAILID"));
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

	public JSONArray getSimpoManagerCountDetails(String invoice, List<String> po_num_list) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(invoice);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		for (String po_num : po_num_list) {
			result = Validation.StringChecknull(po_num);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String holdcount = "select EUMANAGER from invoiceapproval where InvoiceNumber=? and PONumber=?";
		int count = 0;
		ArrayList<String> managerlist = new ArrayList<String>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			for (String po_num : po_num_list) {

				ps = con.prepareStatement(holdcount);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				rs = ps.executeQuery();
				while (rs.next()) {
					managerlist.add(rs.getString("EUMANAGER"));
				}
				rs.close();
				ps.close();

			}
			responsejson.put("managerlist", managerlist);
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);
		} catch (Exception e) {
			responsejson.put("message", "Fail");
			log.error("getSimpoManagerCountDetails() :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getaddedMultipleManagerListForSimpo(String invoice, List<String> po_num_list) throws SQLException {

		String qdata = "SELECT VENDORID,INVOICENUMBER,INVOICEDATE,PONUMBER,BUYERID,ENDUSEID,"
				+ "ENDUSERSTATUS,EUMANAGER,STATUS,STAGE,MODIFIEDDATE,TOTALAMOUNT FROM "
				+ "invoiceapproval where INVOICENUMBER =? and PONUMBER=? ORDER BY MODIFIEDDATE desc";

		boolean result;
		result = Validation.StringChecknull(invoice);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		for (String po_num : po_num_list) {
			result = Validation.StringChecknull(po_num);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			for (String po_num : po_num_list) {
				ps = con.prepareStatement(qdata);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				rs = ps.executeQuery();
				while (rs.next()) {
					HashMap<String, String> invoiceData = new HashMap<String, String>();
					invoiceData.put("VENDORID", rs.getString("VENDORID"));
					invoiceData.put("INVOICENUMBER", rs.getString("InvoiceNumber"));
					invoiceData.put("INVOICEDATE", rs.getString("InvoiceDate"));
					invoiceData.put("PO_NUMBER", rs.getString("PONumber"));
					invoiceData.put("BUYERID", rs.getString("BUYERID"));
					invoiceData.put("ENDUSEID", rs.getString("ENDUSEID"));
					invoiceData.put("ENDUSERSTATUS", rs.getString("ENDUSERSTATUS"));
					invoiceData.put("EUMANAGER", rs.getString("EUMANAGER"));
					invoiceData.put("STATUS", rs.getString("STATUS"));
					invoiceData.put("STAGE", rs.getString("STAGE"));
					invoiceData.put("MODIFIEDDATE", rs.getString("MODIFIEDDATE"));
					invoiceData.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
					invoiceList.add(invoiceData);
				}
				rs.close();
				ps.close();
			}
		} catch (SQLException e) {
			log.error("getaddedMultipleManagerListForSimpo() :", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (invoiceList.size() > 0) {
			responsejson.put("invoiceData", invoiceList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	/*
	 * commented on 09-04-2024 for advance search with filter public JSONArray
	 * getInvoiceDetails(String bid, int nPage, String status, String invno, String
	 * pono, String fdate, String tdate, String plant, String companyCode) {
	 * 
	 * InvoiceDetailsImpl invImpl = new InvoiceDetailsImpl(); boolean result;
	 * 
	 * result = Validation.StringChecknull(bid); if (result == false) {
	 * responsejson.put("validation", "validation Fail");
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); return
	 * jsonArray; } else { result = Validation.numberCheck(bid); if (result ==
	 * false) { responsejson.put("validation", "validation Fail");
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); return
	 * jsonArray; } }
	 * 
	 * result = Validation.StringChecknull(companyCode); if (result == false) {
	 * responsejson.put("validation", "Validation Fail");
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); return
	 * jsonArray; } // validation is pending String basePoQuery =
	 * " and pis.ponumber=ia.ponumber "; boolean basePoFlag = false; // For WOPO
	 * String qdata1 =
	 * "SELECT * FROM INVOICEEVENTDETAILWOPO where BUSSINESSPARTNEROID =? and PONUMBER is NULL and STATUS <> 'A' "
	 * + "ORDER BY createdon desc";
	 * 
	 * String compCodeJoinQuery =
	 * " join podetails pod on pis.ponumber = pod.ponumber "; String compCodeQuery =
	 * " AND pod.companycode = ? ";
	 * 
	 * 
	 * // For all filters except Offline Invoices and ALL and Pending . String qdata
	 * = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
	 * +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
	 * + compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND PIS.OVERALLSTATUS= ? AND " +
	 * "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery +
	 * "Union " +
	 * "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
	 * +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
	 * + "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber " +
	 * compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND PIS.OVERALLSTATUS= ? AND " +
	 * "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery +
	 * "ORDER BY CREATEDON DESC";
	 * 
	 * // For ALL filter
	 * 
	 * String alldata =
	 * "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
	 * + compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ?  AND " +
	 * "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery +
	 * "Union " +
	 * "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
	 * +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
	 * + "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber " +
	 * compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ?  AND " +
	 * "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery +
	 * "ORDER BY CREATEDON DESC";
	 * 
	 * // For OFFLINE INVOICES
	 * 
	 * String hdata =
	 * "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
	 * + compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? and ONEXSTATUS=? AND " +
	 * "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery +
	 * "Union " +
	 * "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
	 * +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
	 * + "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber " +
	 * compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? and ONEXSTATUS=? AND " +
	 * "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery +
	 * "ORDER BY CREATEDON DESC";
	 * 
	 * String pdata =
	 * "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
	 * + compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND (PIS.OVERALLSTATUS= ? OR PIS.OVERALLSTATUS= ?) AND "
	 * + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery +
	 * "Union " +
	 * "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
	 * +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
	 * + "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber " +
	 * compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND (PIS.OVERALLSTATUS= ? OR PIS.OVERALLSTATUS= ?) AND "
	 * + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery +
	 * "ORDER BY CREATEDON DESC";
	 * 
	 * 
	 * ArrayList<HashMap<String, String>> invoiceList = new
	 * ArrayList<HashMap<String, String>>(); ArrayList<HashMap<String, String>>
	 * invoiceList1 = new ArrayList<HashMap<String, String>>(); Connection con =
	 * null; PreparedStatement ps = null; ResultSet rs = null; int pages = 0; int
	 * invoicewopopages = 0; try { con = DBConnection.getConnection();
	 * 
	 * if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP")) &&
	 * (!status.equalsIgnoreCase("ASSQ"))) {
	 * 
	 * if (!status.equalsIgnoreCase("WOPO")) { ArrayList<String> param = new
	 * ArrayList<String>(); param.add(bid); Pagination pg = null; if
	 * (status.equalsIgnoreCase("H")) { param.add(status); param.add(companyCode);
	 * param.add(bid); param.add(status); param.add(companyCode); pg = new
	 * Pagination(hdata, nPage); } else if (status.equalsIgnoreCase("ALL")) {
	 * param.add(companyCode); param.add(bid); param.add(companyCode); pg = new
	 * Pagination(alldata, nPage); } else if (status.equalsIgnoreCase("P")) {
	 * param.add(status); param.add("M"); param.add(companyCode); param.add(bid);
	 * param.add(status); param.add("M"); param.add(companyCode); pg = new
	 * Pagination(pdata, nPage); } else if (status.equalsIgnoreCase("V")) {
	 * param.add(status); param.add("RO"); param.add(companyCode); param.add(bid);
	 * param.add(status); param.add("RO"); param.add(companyCode); pg = new
	 * Pagination(pdata, nPage); } else { param.add(status); param.add(companyCode);
	 * param.add(bid); param.add(status); param.add(companyCode); pg = new
	 * Pagination(qdata, nPage); } pages = pg.getPages(con, param); rs =
	 * pg.execute(con, param); String invNumber = null; String invDate = null;
	 * String mPO = null; String bpid = null; int count = 0; while (rs.next()) {
	 * count++; HashMap<String, String> invoiceData = new HashMap<String, String>();
	 * invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER")); invNumber =
	 * rs.getString("INVOICENUMBER"); invoiceData.put("INVOICEDATE",
	 * rs.getString("INVOICEDATE")); invDate = rs.getString("INVOICEDATE");
	 * invoiceData.put("PO_NUMBER", rs.getString("PONUMBER"));
	 * invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
	 * invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
	 * invoiceData.put("VENDORID", rs.getString("VENDORID"));
	 * invoiceData.put("PLANT", rs.getString("PLANT")); POImpl po = new POImpl();
	 * invoiceData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
	 * invoiceData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
	 * bpid = rs.getString("BUSINESSPARTNEROID"); invoiceData.put("CREATEDBY",
	 * rs.getString("CreatedBy")); invoiceData.put("CREATEDON",
	 * rs.getString("CreatedOn")); invoiceData.put("TOTALAMOUNT",
	 * rs.getString("AMOUNT")); invoiceData.put("PAYMENTAMOUNT",
	 * rs.getString("PAYMENTAMOUNT") != null ? rs.getString("PAYMENTAMOUNT") : "0");
	 * invoiceData.put("DESCRIPTION", rs.getString("DESCRIPTION"));
	 * invoiceData.put("STATUS", rs.getString("OVERALLSTATUS"));
	 * invoiceData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
	 * invoiceData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
	 * invoiceData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
	 * invoiceData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
	 * invoiceData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
	 * invoiceData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
	 * invoiceData.put("MPO", rs.getString("MPO")); invoiceData.put("ALLPO",
	 * rs.getString("ALLPO")); invoiceList.add(invoiceData); } pg.close();
	 * rs.close(); pg = null; }
	 * 
	 * if (status.equalsIgnoreCase("WOPO")) { ArrayList<String> param1 = new
	 * ArrayList<String>(); param1.add(bid); Pagination pg1 = new Pagination(qdata1,
	 * nPage); invoicewopopages = pg1.getPages(con, param1); rs = pg1.execute(con,
	 * param1);
	 * 
	 * while (rs.next()) {
	 * 
	 * HashMap<String, String> invoiceData1 = new HashMap<String, String>();
	 * invoiceData1.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
	 * invoiceData1.put("INVOICEDATE", rs.getString("INVOICEDATE"));
	 * invoiceData1.put("BUSSINESSPARTNEROID", rs.getString("BUSSINESSPARTNEROID"));
	 * invoiceData1.put("PO_NUMBER", rs.getString("PONUMBER"));
	 * invoiceData1.put("CREATEDON", rs.getString("CREATEDON"));
	 * invoiceData1.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
	 * invoiceData1.put("DESCRIPTION", rs.getString("DESCRIPTION"));
	 * invoiceData1.put("STATUS", rs.getString("STATUS"));
	 * invoiceData1.put("INVOICEAMOUNT", rs.getString("INVOICEAMOUNT"));
	 * invoiceData1.put("USEREMAILID", rs.getString("USEREMAILID"));
	 * invoiceData1.put("POINVOICENUMBER", rs.getString("POINVOICENUMBER"));
	 * invoiceList1.add(invoiceData1); } pg1.close(); rs.close(); pg1 = null; } }
	 * else { String subquery = ""; ArrayList<String> param = new
	 * ArrayList<String>(); param.add(bid); Pagination pg = null;
	 * 
	 * String advqdata = ""; if ((!status.equalsIgnoreCase("ASWP")) &&
	 * (!status.equalsIgnoreCase("ASSQ"))) { if (!plant.equalsIgnoreCase("NA")) {
	 * String po = " AND PIS.PLANT=?"; subquery = subquery + po; param.add(plant); }
	 * if (!pono.equalsIgnoreCase("NA")) { String po = " AND PIS.PONUMBER=?";
	 * subquery = subquery + po; basePoQuery = " and pis.basepo=ia.ponumber ";
	 * basePoFlag = true; param.add(pono); } if (!invno.equalsIgnoreCase("NA")) {
	 * String in = " AND PIS.INVOICENUMBER=?"; subquery = subquery + in;
	 * param.add(invno); } if ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { String dt =
	 * " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " +
	 * "AND TO_DATE(?, 'DD/MM/YYYY')"; subquery = subquery + dt; param.add(fdate);
	 * param.add(tdate); } param.add(companyCode);
	 * 
	 * param.add(bid); if (!plant.equalsIgnoreCase("NA")) { param.add(plant); } if
	 * (!pono.equalsIgnoreCase("NA")) { param.add(pono); } if
	 * (!invno.equalsIgnoreCase("NA")) { param.add(invno); } if
	 * ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { param.add(fdate);
	 * param.add(tdate); } param.add(companyCode);
	 * 
	 * advqdata =
	 * "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO,PIS.BASEPO FROM PONINVOICESUMMERY PIS "
	 * + compCodeJoinQuery + "  WHERE " + "PIS.BUSINESSPARTNEROID = ? " + subquery +
	 * " AND " + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " +
	 * compCodeQuery + "Union " +
	 * "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
	 * +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO,PIS.BASEPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
	 * + "ON PIS.invoicenumber = ia.invoicenumber " + basePoQuery + "" +
	 * compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ? " + subquery +
	 * " AND " + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
	 * + "ORDER BY CREATEDON DESC";
	 * 
	 * } else if (status.equalsIgnoreCase("ASWP")) {
	 * 
	 * if (!pono.equalsIgnoreCase("NA")) { String po = " AND PIS.PONUMBER=?";
	 * subquery = subquery + po; basePoQuery = " and pis.basepo=ia.ponumber ";
	 * basePoFlag = true; param.add(pono); } if (!invno.equalsIgnoreCase("NA")) {
	 * String in = " AND PIS.INVOICENUMBER=?"; subquery = subquery + in;
	 * param.add(invno); } if ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { String dt =
	 * " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " +
	 * "AND TO_DATE(?, 'DD/MM/YYYY')"; subquery = subquery + dt; param.add(fdate);
	 * param.add(tdate); } advqdata =
	 * "SELECT * FROM INVOICEEVENTDETAILWOPO where BUSSINESSPARTNEROID =? and PONUMBER is NULL "
	 * + subquery + " and STATUS <> 'A' " + "ORDER BY createdon desc"; } else if
	 * (status.equalsIgnoreCase("ASSQ")) {
	 * 
	 * if (!invno.equalsIgnoreCase("NA")) { String in = " AND PIS.INVOICENUMBER=?";
	 * subquery = subquery + in; param.add(invno); } if
	 * ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { String dt =
	 * " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " +
	 * "AND TO_DATE(?, 'DD/MM/YYYY')"; subquery = subquery + dt; param.add(fdate);
	 * param.add(tdate); } param.add(companyCode);
	 * 
	 * param.add(bid); if (!invno.equalsIgnoreCase("NA")) { param.add(invno); } if
	 * ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { param.add(fdate);
	 * param.add(tdate); } param.add(companyCode);
	 * 
	 * advqdata =
	 * "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO,PIS.BASEPO FROM PONINVOICESUMMERY PIS "
	 * + compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND PIS.CREDITADVICENO IS NOT NULL " + subquery +
	 * " AND " + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " +
	 * compCodeQuery + "Union " +
	 * "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
	 * +
	 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
	 * +
	 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
	 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
	 * +
	 * "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO,PIS.BASEPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
	 * + "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber " +
	 * compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND PIS.CREDITADVICENO IS NOT NULL " + subquery +
	 * " AND " + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
	 * + "ORDER BY CREATEDON DESC";
	 * 
	 * } pg = new Pagination(advqdata, nPage); pages = pg.getPages(con, param); rs =
	 * pg.execute(con, param);
	 * 
	 * if (status.equalsIgnoreCase("ASWP")) {
	 * 
	 * } else { String invNumber = null; String invDate = null; String mPO = null;
	 * String bpid = null; while (rs.next()) { HashMap<String, String> invoiceData =
	 * new HashMap<String, String>();
	 * 
	 * String poNum = rs.getString("PONUMBER"); if (basePoFlag &&
	 * rs.getString("BASEPO") != null) { poNum = rs.getString("BASEPO"); }
	 * 
	 * invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER")); invNumber =
	 * rs.getString("INVOICENUMBER"); invoiceData.put("INVOICEDATE",
	 * rs.getString("INVOICEDATE")); invDate = rs.getString("INVOICEDATE");
	 * invoiceData.put("PO_NUMBER", poNum); invoiceData.put("CONTACTPERSON",
	 * rs.getString("CONTACTPERSON")); invoiceData.put("CONTACTPERSONPHONE",
	 * rs.getString("CONTACTPERSONPHONE")); invoiceData.put("VENDORID",
	 * rs.getString("VENDORID")); invoiceData.put("PLANT", rs.getString("PLANT"));
	 * POImpl po = new POImpl(); invoiceData.put("PLANTNAME",
	 * po.getPlantName(rs.getString("PLANT"), con));
	 * invoiceData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
	 * bpid = rs.getString("BUSINESSPARTNEROID"); invoiceData.put("CREATEDBY",
	 * rs.getString("CreatedBy")); invoiceData.put("CREATEDON",
	 * rs.getString("CreatedOn")); invoiceData.put("TOTALAMOUNT",
	 * rs.getString("AMOUNT")); invoiceData.put("PAYMENTAMOUNT",
	 * rs.getString("PAYMENTAMOUNT") != null ? rs.getString("PAYMENTAMOUNT") : "0");
	 * invoiceData.put("DESCRIPTION", rs.getString("DESCRIPTION"));
	 * invoiceData.put("STATUS", rs.getString("OVERALLSTATUS"));
	 * invoiceData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
	 * invoiceData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
	 * invoiceData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
	 * invoiceData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
	 * invoiceData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
	 * invoiceData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
	 * invoiceData.put("MPO", rs.getString("MPO")); invoiceData.put("ALLPO",
	 * rs.getString("ALLPO")); invoiceList.add(invoiceData); } }
	 * 
	 * pg.close(); rs.close(); pg = null;
	 * 
	 * String subquery1 = ""; ArrayList<String> param1 = new ArrayList<String>();
	 * param1.add(bid); if (!invno.equalsIgnoreCase("NA")) { String in =
	 * " AND PIS.INVOICENUMBER=?"; subquery1 = subquery1 + in; param1.add(invno); }
	 * if (!fdate.equalsIgnoreCase("NA")) { String dt =
	 * " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " +
	 * "AND TO_DATE(?, 'DD/MM/YYYY')"; subquery1 = subquery1 + dt;
	 * param1.add(fdate); param1.add(tdate); }
	 * 
	 * String advqdata1 =
	 * "SELECT * FROM INVOICEEVENTDETAILWOPO PIS where PIS.BUSSINESSPARTNEROID =? "
	 * + "and PIS.PONUMBER is NULL " + subquery1 +
	 * " AND PIS.STATUS <> 'A' ORDER BY PIS.createdon desc"; Pagination pg1 = new
	 * Pagination(advqdata1, nPage); invoicewopopages = pg1.getPages(con, param1);
	 * rs = pg1.execute(con, param1);
	 * 
	 * while (rs.next()) { HashMap<String, String> invoiceData1 = new
	 * HashMap<String, String>(); invoiceData1.put("INVOICENUMBER",
	 * rs.getString("INVOICENUMBER")); invoiceData1.put("INVOICEDATE",
	 * rs.getString("INVOICEDATE")); invoiceData1.put("BUSSINESSPARTNEROID",
	 * rs.getString("BUSSINESSPARTNEROID")); invoiceData1.put("PO_NUMBER",
	 * rs.getString("PONUMBER")); invoiceData1.put("CREATEDON",
	 * rs.getString("CREATEDON")); invoiceData1.put("TOTALAMOUNT",
	 * rs.getString("TOTALAMOUNT")); invoiceData1.put("DESCRIPTION",
	 * rs.getString("DESCRIPTION")); invoiceData1.put("STATUS",
	 * rs.getString("STATUS")); invoiceData1.put("INVOICEAMOUNT",
	 * rs.getString("INVOICEAMOUNT")); invoiceData1.put("USEREMAILID",
	 * rs.getString("USEREMAILID")); invoiceData1.put("POINVOICENUMBER",
	 * rs.getString("POINVOICENUMBER")); invoiceList1.add(invoiceData1); }
	 * pg1.close(); rs.close(); pg1 = null; }
	 * 
	 * try { getInvoiceDetailsCountAsPerStatus(bid, nPage, status, invno, pono,
	 * fdate, tdate, plant, con, ps, rs, companyCode); } catch (Exception e) {
	 * 
	 * }
	 * 
	 * } catch (Exception e) { log.error("getInvoiceDetails() :",
	 * e.fillInStackTrace()); responsejson.put("message", "Fail");
	 * jsonArray.add(responsejson); } finally { DBConnection.closeConnection(rs, ps,
	 * con); }
	 * 
	 * if (invoiceList.size() > 0) { responsejson.put("message", "Sucessinvlist");
	 * responsejson.put("invoiceData", invoiceList);
	 * responsejson.put("invoicelistpages", pages);
	 * 
	 * } else { responsejson.put("message", "No Data Found for given Vendor Id"); }
	 * if (invoiceList1.size() > 0) { responsejson.put("message1",
	 * "Sucessinvlist1"); responsejson.put("invoiceDataWOPO", invoiceList1);
	 * responsejson.put("invoicewopopages", invoicewopopages); } else {
	 * responsejson.put("message1", "No Data Found for given Vendor Id"); }
	 * jsonArray.add(responsejson);
	 * 
	 * return jsonArray;
	 * 
	 * }
	 */
	public JSONArray getSimpoinvoicebasedonemailid(String emailId, HttpSession session, int nPage, String status,
			String pono, String invno, String fdate, String tdate, String plant, String vendor) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(emailId);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.emailCheck(emailId);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String sql = null;
		ArrayList<HashMap<String, String>> POEvent = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> POListEvent = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		String storeKepeerQuery = null;
		String storeKepeer = (String) session.getAttribute("shopkepeer");
		String getstoreKepeermaterialtype = "Select MTYP,PLANT from INVENTORYUSERLIST where USERID=?";

		try {
			con = DBConnection.getConnection();
			if (!"AS".equalsIgnoreCase(status)) {

				if ("C".equalsIgnoreCase(status)) {

					sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,PAYMENTDATE,ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
							+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO," 
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
							+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
							+ "AND (CREDITADVICENO IS NOT NULL) " + "UNION "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, " 
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
							+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ " FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(CREDITADVICENO IS NOT NULL AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
							// + "(CREDITADVICENO IS NOT NULL AND A1.STATUS NOT LIKE 'C%' )"
							+ "UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, " 
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
							+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+" FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND"
							+ " (CREDITADVICENO IS NOT NULL  AND A1.STATUS LIKE 'C%') ORDER BY CREATEDON DESC) c )";

					storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,PAYMENTDATE, "
							+ " ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
							+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND CREDITADVICENO IS NOT NULL "
							+ " union all " + " SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
							+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+" FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
							// + "A1.PONumber=B1.PONumber AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%'
							// AND "
							+ " CREDITADVICENO IS NOT NULL " + " union all  "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
							+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+" FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  AND "
							+ "CREDITADVICENO IS NOT NULL " + " union all " + " SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
							+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND CREDITADVICENO IS NOT NULL) JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "ORDER BY CREATEDON DESC) c )";

				} else if ("ALL".equalsIgnoreCase(status)) {

					sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,PAYMENTDATE,ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
							+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
							+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? " + ""
							+ "union " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
							+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+" FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL) " + "union  "
							// + "(A1.STATUS NOT LIKE 'C%' ) " + "union "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
							+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ " FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND"
							+ " (A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

					storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,PAYMENTDATE,ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
							+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + " union all "
							+ " SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
							+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+" FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL "
							// + "A1.PONumber=B1.PONumber AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%'
							// "
							+ " union all  " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
							+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ " FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' " + " union all "
							+ " SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
							+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
							+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
							+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "ORDER BY CREATEDON DESC) c )";

				} else {
					if ("P".equalsIgnoreCase(status) || status.equalsIgnoreCase("V")) {

						sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE, "
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,PAYMENTDATE,ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER, "
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS, "
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS, "
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER, "
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO, "
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
								+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? OR B.OVERALLSTATUS=?) " + " UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
								+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+" FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
								+ "(B1.OVERALLSTATUS=?  OR B1.OVERALLSTATUS=? AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
								// + "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=? AND A1.STATUS NOT LIKE 'C%' )"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
								+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
								+ "(B1.OVERALLSTATUS=?  OR B1.OVERALLSTATUS=? AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

						storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,PAYMENTDATE,ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
								+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ " A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=? OR B.OVERALLSTATUS=? )"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
								+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
								// + "A1.PONumber=B1.PONumber AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%'
								// AND "
								+ "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=?) " + "UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
								+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' AND "
								+ "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=?)" + " UNION "
								+ "SELECT DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
								+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND (B.OVERALLSTATUS IN (?,?)) JOIN "
								+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "ORDER BY CREATEDON DESC) c )";

					} else {

						sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE, "
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,PAYMENTDATE,ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER, "
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS, "
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS, "
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER, "
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO, "
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
								+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? ) " + " UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
								+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+" FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
								+ "(B1.OVERALLSTATUS=?  AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
								// + "(B1.OVERALLSTATUS=? AND A1.STATUS NOT LIKE 'C%' ) "
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
								+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
								+ "(B1.OVERALLSTATUS=?  AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

						storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,PAYMENTDATE,ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
								+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ " A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=?  )"
								+ " union all " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
								+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
								// + "A1.PONumber=B1.PONumber AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%'
								// AND "
								+ "(B1.OVERALLSTATUS=? ) " + " union all "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B1.INVOICENUMBER "
								+ " AND PONUMBER =  B1.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  AND "
								+ "(B1.OVERALLSTATUS=? )" + " union all  " + "SELECT DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO, "
								+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
								+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
								+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND (B.OVERALLSTATUS =?) JOIN "
								+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "ORDER BY CREATEDON DESC) c )";

					}

				}

				if ("true".equalsIgnoreCase(storeKepeer)) {
					ps = con.prepareStatement(storeKepeerQuery);
					responsejson.put("storekeeper", "true");
					ArrayList<String> param = new ArrayList<String>();

					if (status.equalsIgnoreCase("C") || status.equalsIgnoreCase("ALL")) {
						param.add(emailId);
						param.add(emailId);
						param.add(emailId);
						param.add(emailId);
					} else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {
						param.add(emailId);
						param.add(status);
						if ("P".equalsIgnoreCase(status)) {

							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
							param.add(status);
							param.add("M");
							param.add(emailId);
						} else if (status.equalsIgnoreCase("V")) {

							param.add("RO");
							param.add(emailId);
							param.add(status);
							param.add("RO");
							param.add(emailId);
							param.add(status);
							param.add("RO");
							param.add(status);
							param.add("RO");
							param.add(emailId);

						} else {
							param.add(emailId);
							param.add(status);
							param.add(emailId);
							param.add(status);
							param.add(status);
							param.add(emailId);
						}
					}

					log.info("storeKepeerQuery :" + storeKepeerQuery);

					Pagination pg = new Pagination(storeKepeerQuery, nPage, 1);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				} else {

					ps = con.prepareStatement(sql);
					responsejson.put("storekeeper", "false");

					ArrayList<String> param = new ArrayList<String>();
					if (status.equalsIgnoreCase("C") || status.equalsIgnoreCase("ALL")) {
						param.add(emailId);
						param.add(emailId);
						param.add(emailId);
					} else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {
						param.add(emailId);
						param.add(status);
						if ("P".equalsIgnoreCase(status)) {

							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
						} else if ("V".equalsIgnoreCase(status)) {

							param.add("RO");
							param.add(emailId);
							param.add(status);
							param.add("RO");
							param.add(emailId);
							param.add(status);
							param.add("RO");
						} else {
							param.add(emailId);
							param.add(status);
							param.add(emailId);
							param.add(status);
						}
					}
					log.info("sql :" + sql);
					Pagination pg = new Pagination(sql, nPage, 1);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				}
				InternalportalImpl ipi = new InternalportalImpl();
				while (rs.next()) {
					HashMap<String, String> poEvent = new HashMap<String, String>();
					poEvent.put("PONUMBER", rs.getString("PONUMBER"));
					poEvent.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poEvent.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					poEvent.put("ENDUSERID", rs.getString("ENDUSEID"));
					poEvent.put("ENDUSERSTATUS", rs.getString("ENDUSERSTATUS"));
					poEvent.put("EUMANAGER",
							ipi.getmanagerslist(rs.getString("INVOICENUMBER"), rs.getString("PONUMBER"), con));
					poEvent.put("STATUS", rs.getString("STATUS"));
					poEvent.put("TOTALAMOUNT", rs.getString("amount"));
					poEvent.put("STAGE", rs.getString("STAGE"));
					poEvent.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poEvent.put("PROXY", rs.getString("PROXY"));
					poEvent.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					poEvent.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					poEvent.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poEvent.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
					poEvent.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poEvent.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poEvent.put("VENDORID", rs.getString("VENDORID"));
					poEvent.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poEvent.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					poEvent.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					poEvent.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					poEvent.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					poEvent.put("ACTIONBY", rs.getString("ACTIONBY"));
					poEvent.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
					poEvent.put("GRNNUMBER", rs.getString("GRNNUMBER"));
					poEvent.put("USERID",
							ipi.getemailidbasedonmaterial(rs.getString("MATERIAL_TYPE"), rs.getString("PLANT")));
					poEvent.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					poEvent.put("MPO", rs.getString("MPO"));
					poEvent.put("ALLPO", rs.getString("ALLPO"));
					poEvent.put("ACCEPTEDFLAG", checkAcceptedQtyStatus(con, rs.getString("INVOICENUMBER"),
							rs.getString("INVOICEDATE"), rs.getString("PONUMBER"), rs.getString("BUSINESSPARTNEROID")));
					poEvent.put("PAYMENTDATE", rs.getString("PAYMENTDATE"));
								
					POListEvent.add(poEvent);
					
					
				}
			} else {
				String basePoQuery = " A.PONumber = B.PONUMBER ";
				String basePoQuery1 = " A1.PONumber = B.PONUMBER ";
				boolean basePoFlag = false;
				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				if ("true".equalsIgnoreCase(storeKepeer)) {
					param.add(emailId);
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND B.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = " AND B.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND B.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " A.PONumber=B.BASEPO ";
						basePoQuery1 = " A1.PONumber = B.BASEPO ";
						basePoFlag = true;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND B.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND B.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);
					if (!plant.equalsIgnoreCase("NA")) {

						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {

						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {

						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {

						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {

						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);
					for (int i = 0; i < 2; i++) {
						if (!plant.equalsIgnoreCase("NA")) {
							param.add(plant);
						}
						if (!vendor.equalsIgnoreCase("NA")) {
							param.add(vendor);
						}
						if (!pono.equalsIgnoreCase("NA")) {

							param.add(pono);
						}
						if (!invno.equalsIgnoreCase("NA")) {

							param.add(invno);
						}
						if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {

							param.add(fdate);
							param.add(tdate);
						}
					}
					param.add(emailId);
				} else {
					param.add(emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND B.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = " AND B.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND B.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " A.PONumber = B.BASEPO ";
						basePoQuery1 = " A1.PONumber = B.BASEPO ";
						basePoFlag = true;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND B.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND B.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}
				}

				sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
						+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
						+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
						+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,BASEPO,PAYMENTDATE,ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER ,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
						+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
						+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
						+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO,B.BASEPO, "
						+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
						+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
						+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
						+ "A.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery + " AND A.ENDUSEID = ? " + " " + subquery
						+ " " + " UNION " + "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ "B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO,B.BASEPO, "
						+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
						+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
						+" FROM INVOICEAPPROVAL "
						+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery1
						+ "  AND (A1.EUMANAGER = ? )  AND" + "( A1.STATUS NOT LIKE 'C%' AND B.GRNNUMBER IS NOT NULL "
						+ subquery + ") " + "UNION " + "SELECT B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ " B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO,B.BASEPO, "
						+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
						+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
						+" FROM INVOICEAPPROVAL "
						+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery1
						+ "  AND (A1.EUMANAGER = ? ) AND " + "(A1.STATUS LIKE 'C%'  " + subquery
						+ ") ORDER BY CREATEDON DESC) c )";

				storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
						+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
						+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
						+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO,BASEPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
						+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
						+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
						+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO,B.BASEPO, "
						+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
						+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
						+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
						+ "A.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery
						+ " AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + subquery + " " + "UNION "
						+ "SELECT B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,"
						+ "B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO,B.BASEPO, "
						+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
						+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
						+ " FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ basePoQuery1 + " AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' "
						+ "AND B.GRNNUMBER IS NOT NULL  " + subquery + " " + " UNION "
						+ "SELECT B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,"
						+ "B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO,B.BASEPO, "
						+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
						+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
						+ " FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ basePoQuery1 + " AND (A1.EUMANAGER = ?) " + "AND A1.STATUS LIKE 'C%' " + subquery + " "
						+ "UNION " + "SELECT DISTINCT B.PONUMBER ,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
						+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
						+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
						+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO,B.BASEPO, "
						+ " (SELECT MAX(TO_CHAR(TRUNC(MODIFIEDTIME),'DD-MON-RRRR')) FROM INVOICETRACKER WHERE INVOICENUMBER = B.INVOICENUMBER "
						+ " AND PONUMBER =  B.PONUMBER AND STATUS = 'PD') AS PAYMENTDATE "
						+ " FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
						+ basePoQuery + " AND A.PROXY = 'X' " + subquery + " JOIN "
						+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
						+ "ORDER BY CREATEDON DESC) c )";

				Pagination pg = null;
				if ("true".equalsIgnoreCase(storeKepeer)) {
					responsejson.put("storekeeper", "true");
					log.info("storeKepeerQuery :" + storeKepeerQuery);
					pg = new Pagination(storeKepeerQuery, nPage, 1);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				} else {
					responsejson.put("storekeeper", "false");
					log.info("sql : " + sql);
					pg = new Pagination(sql, nPage, 1);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				}
				InternalportalImpl ipi = new InternalportalImpl();
				while (rs.next()) {
					HashMap<String, String> poEvent = new HashMap<String, String>();
					String poNum = rs.getString("PONUMBER");
					if (basePoFlag && rs.getString("BASEPO") != null) {
						poNum = rs.getString("BASEPO");
					}
					poEvent.put("PONUMBER", poNum);
					poEvent.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poEvent.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					poEvent.put("ENDUSERID", rs.getString("ENDUSEID"));
					poEvent.put("ENDUSERSTATUS", rs.getString("ENDUSERSTATUS"));
					poEvent.put("EUMANAGER", ipi.getmanagerslist(rs.getString("INVOICENUMBER"), poNum, con));
					poEvent.put("STATUS", rs.getString("STATUS"));
					poEvent.put("TOTALAMOUNT", rs.getString("amount"));
					poEvent.put("STAGE", rs.getString("STAGE"));
					poEvent.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poEvent.put("PROXY", rs.getString("PROXY"));
					poEvent.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					poEvent.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					poEvent.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poEvent.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
					poEvent.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poEvent.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poEvent.put("VENDORID", rs.getString("VENDORID"));
					poEvent.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poEvent.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					poEvent.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					poEvent.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					poEvent.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					poEvent.put("ACTIONBY", rs.getString("ACTIONBY"));
					poEvent.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
					poEvent.put("GRNNUMBER", rs.getString("GRNNUMBER"));
					poEvent.put("USERID",
							ipi.getemailidbasedonmaterial(rs.getString("MATERIAL_TYPE"), rs.getString("PLANT")));
					poEvent.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					poEvent.put("MPO", rs.getString("MPO"));
					poEvent.put("ALLPO", rs.getString("ALLPO"));
					poEvent.put("ACCEPTEDFLAG", checkAcceptedQtyStatus(con, rs.getString("INVOICENUMBER"),
							rs.getString("INVOICEDATE"), poNum, rs.getString("BUSINESSPARTNEROID")));
					
					poEvent.put("PAYMENTDATE", rs.getString("PAYMENTDATE"));
					POListEvent.add(poEvent);
				}
				pg.close();
				rs.close();
				pg = null;
			}
			if (POListEvent.size() > 0) {
				responsejson.put("invoicedetails", POListEvent);
				responsejson.put("invoicedetailsrecords", pages);
				responsejson.put("message", "Success");
			} else {
				responsejson.put("message", "No Data Found");
			}
			try {
				getinvoicebasedonemailidCountAsPerStatus(emailId, session, nPage, status, pono, invno, fdate, tdate,
						plant, vendor, con, ps, rs);
			} catch (Exception e1) {
				log.error("getSimpoinvoicebasedonemailid() 1 :", e1.fillInStackTrace());
			}
		} catch (Exception e) {
			log.error("getSimpoinvoicebasedonemailid() 2 :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	private String checkAcceptedQtyStatus(Connection con, String invoiceNumber, String invoiceDate, String poNumber,
			String bid) {

		DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		Date date = null;
		try {
			date = inputFormat.parse(invoiceDate);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}

		DateFormat fmt = new SimpleDateFormat("dd-MM-yy");
		invoiceDate = fmt.format(date);
		String acceptedQtyCheckQuery = "select count(*) as count from deliverysummary where invoicenumber = ? and ponumber = ? "
				+ "and invoicedate = TO_DATE(?, 'DD-MM-YY') and bussinesspartneroid = ?"
				+ "and acceptedqty is not null and acceptedqty <> 0";
		String acceptedFlag = "N";
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = con.prepareStatement(acceptedQtyCheckQuery);
			ps.setString(1, invoiceNumber);
			ps.setString(2, poNumber);
			ps.setString(3, invoiceDate);
			ps.setString(4, bid);
			rs = ps.executeQuery();
			int count = 0;
			while (rs.next()) {
				count = Integer.parseInt(rs.getString("count"));
			}
			rs.close();
			ps.close();

			if (count > 0) {
				acceptedFlag = "Y";
			}
		} catch (Exception e) {
			log.error("checkAcceptedQtyStatus() :", e.fillInStackTrace());
		}
		return acceptedFlag;
	}

	public JSONArray getAcceptQtynGRN(List<String> acceptedValues, String email) throws SQLException {

		String[] acceptedList = acceptedValues.toArray(new String[acceptedValues.size()]);
		int value = 0;
		String returnGRN = null;
		String poNumber = null;
		String invoiceNumber = null;
		String invoiceDate = null;
		String poLineitem = null;
		String storageLocation = null;
		String quantity = null;
		String grnyear = null;
		String grnqty = null;
		String error = null;
		String message = null;
		String orderNumber = null;
		String enduser = null;
		String timescapeid = null;
		String status = null;
		String warningMessage = null;
		String portalid = null;
		ArrayList sendingList = new ArrayList();
		HashMap poNoMap = new HashMap();
		HashMap poNoMapMain = new HashMap();
		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		ArrayList lineItemlist = new ArrayList();
		ArrayList SAPReturnValues = new ArrayList();
		ArrayList grnLIST = new ArrayList();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			prop.load(input);
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

				String[] fatchedValues = acceptedList[counterValue].split(",");
				poNumber = null;
				invoiceNumber = null;
				invoiceDate = null;
				poLineitem = null;
				quantity = null;
				storageLocation = null;
				orderNumber = null;
				timescapeid = null;
				poNumber = fatchedValues[0];
				poLineitem = fatchedValues[1];
				Date date = (Date) formatter.parseObject(fatchedValues[2]);
				invoiceDate = sm.format(date);
				invoiceNumber = fatchedValues[3];
				quantity = fatchedValues[4];
				storageLocation = fatchedValues[5];
				enduser = fatchedValues[6];
				portalid = fatchedValues[7];
				status = fatchedValues[8];
				poNoMap.put(poNumber, "-");
				poNoMapMain.put(poNumber, "-");

				log.info("#" + poNumber + "#" + poLineitem + "#" + invoiceDate + "#" + invoiceNumber + "#" + quantity
						+ "#" + storageLocation + "#" + portalid);
				if ("".equalsIgnoreCase(portalid) || portalid == null) {
					portalid = "dsubra1007";
				}
				String mainList[] = { poNumber, poLineitem, invoiceDate, invoiceNumber, quantity, storageLocation,
						portalid };
				sendingList.add(mainList);
			}

			SAPConnectionDetails.put("CLIENT", prop.getProperty("CLIENT"));
			SAPConnectionDetails.put("USERID", prop.getProperty("USERID"));
			SAPConnectionDetails.put("PASSWORD", prop.getProperty("PASSWORD"));
			SAPConnectionDetails.put("LANGUAGE", prop.getProperty("LANGUAGE"));
			SAPConnectionDetails.put("HOSTNAME", prop.getProperty("HOSTNAME"));
			SAPConnectionDetails.put("SYSTEMNO", prop.getProperty("SYSTEMNO"));
			SAPConnectionDetails.put("RFCNAME", prop.getProperty("RFCNAME"));
			SAPConnectionDetails.put("TOTALIMPORTTABLESTOSET", prop.getProperty("TOTALIMPORTTABLESTOSET"));
			SAPConnectionDetails.put("RETURNTABLE1", prop.getProperty("RETURNTABLE1"));
			SAPConnectionDetails.put("RETURNTABLE2", prop.getProperty("RETURNTABLE2"));
			SAPConnectionDetails.put("IMPORTTABLENAME1", prop.getProperty("IMPORTTABLENAME1"));
			SAPConnectionDetails.put("TOTALTABLESTORETURN", prop.getProperty("TOTALTABLESTORETURN"));

			String itemList1[] = { "EBELN", "EBELP", "BUDAT", "XBLNR", "ERFMG", "LGORT", "WEMPF" };

			String returnKeys1[] = { "MAT_DOC", "DOC_YEAR", "QUANTITY" };

			String returnKeys2[] = { "TYPE", "ID", "NUMBER", "MESSAGE", "LOG_NO", "LOG_MSG_NO", "MESSAGE_V1",
					"MESSAGE_V2", "MESSAGE_V3", "MESSAGE_V4", "PARAMETER", "ROW", "FIELD", "SYSTEM" };

			SAPColumnHeads.put("IMPORTLINEITEM1", itemList1);

			Set<String> keys = poNoMap.keySet();
			for (String key : keys) {
				log.info("Value of " + key + " is: " + poNoMap.get(key));

				ArrayList newLineItemList = new ArrayList();
				for (int ii = 0; ii < sendingList.size(); ii++) {

					String sapList[] = (String[]) sendingList.get(ii);
					String myPonumber = sapList[0];
					if (key.equals(myPonumber)) {
						newLineItemList.add(sapList);
					}
				}

				SAPValues.put("IMPORTLINEITEMLIST1", newLineItemList);
				SAPColumnHeads.put("RETURNKEYS1", returnKeys1);
				SAPColumnHeads.put("RETURNKEYS2", returnKeys2);
				JcoGetDataFromSAP jco = new JcoGetDataFromSAP("dxproject");
				SAPReturnData = jco.jcoGetData(SAPConnectionDetails, SAPColumnHeads, SAPValues);

				log.info("SAPReturnData :" + SAPReturnData.size() + " Invoice Number : " + invoiceNumber);
				SAPReturnData.forEach((k, v) -> log.info("Key : " + k + ", Value : " + v));

				warningMessage = "";
				if (SAPReturnData.containsKey("RETURNDATA1")) {
					SAPReturnValues = (ArrayList) SAPReturnData.get("RETURNDATA1");
					int counter = SAPReturnValues.size();
					if (counter > 0) {
						for (int ii = 0; ii < counter; ii++) {
							String arrayData[] = (String[]) SAPReturnValues.get(ii);
							log.info("arrayData[] 1 :" + arrayData.toString() + " Invoice Number : " + invoiceNumber);
							returnGRN = arrayData[0];
							grnyear = arrayData[1];
							grnqty = arrayData[2];
							poNoMapMain.put(key, returnGRN);
							log.info("returnGRN :" + returnGRN + " :grnyear: " + grnyear + " :grnqty: " + grnqty
									+ " Invoice Number : " + invoiceNumber);
						}
					}
				}

				if (SAPReturnData.containsKey("RETURNDATA2")) {
					SAPReturnValues = (ArrayList) SAPReturnData.get("RETURNDATA2");
					log.info("SAPReturnValues :" + SAPReturnValues.size() + " Invoice Number : " + invoiceNumber);
					int counter = SAPReturnValues.size();
					if (counter > 0) {
						warningMessage = "";
						for (int ii = 0; ii < counter; ii++) {
							String arrayData[] = (String[]) SAPReturnValues.get(ii);
							log.info("arrayData[] 2 :" + arrayData.length + " Invoice Number : " + invoiceNumber);
							error = arrayData[0];
							message = arrayData[3];
							log.info("error:" + error + " :message: " + message + " Invoice Number : " + invoiceNumber);

							if ("W".equalsIgnoreCase(error) || "S".equalsIgnoreCase(error)
									|| "I".equalsIgnoreCase(error) || "E".equalsIgnoreCase(error)) {
								warningMessage = warningMessage + "PO:" + key + " " + message + ",";
							}
						}

						if (warningMessage.length() > 2) {
							warningMessage = warningMessage.substring(0, warningMessage.length() - 1);
						}
					}
				}

				if (returnGRN != null && returnGRN != "") {
					String returnGRNForPO = null;
					for (int counterValue = 0; counterValue < newLineItemList.size(); counterValue++) {
						String fatchedValues[] = (String[]) newLineItemList.get(counterValue);
						poNumber = null;
						invoiceNumber = null;
						poLineitem = null;
						quantity = null;
						storageLocation = null;
						orderNumber = null;
						returnGRNForPO = null;
						poNumber = fatchedValues[0]; // 00010-10
						poLineitem = fatchedValues[1];
						invoiceNumber = fatchedValues[3];
						quantity = fatchedValues[4];
						storageLocation = fatchedValues[5];
						returnGRNForPO = (String) poNoMapMain.get(key);
						if (key.equalsIgnoreCase(poNumber)) {
							log.info("inside for key--" + key + "--returnGRNForPO--" + returnGRNForPO
									+ "--invoiceNumber--" + invoiceNumber);

							String actualquantityupdate = "update DELIVERYSUMMARY set ACCEPTEDQTY=? ,STORAGELOCATION=?,GRNNUMBER=? "
									+ "where INVOICENUMBER=? AND PONUMBER=? AND LINEITEMNUMBER=?";

							ps = con.prepareStatement(actualquantityupdate);
							ps.setString(1, quantity);
							ps.setString(2, storageLocation);
							ps.setString(3, returnGRNForPO);
							ps.setString(4, invoiceNumber);
							ps.setString(5, key);
							ps.setString(6, poLineitem);
							value = ps.executeUpdate();
							ps.close();
						}
					}

					log.info("outside for key--" + key + "--returnGRNForPO--" + returnGRNForPO + "--invoiceNumber--"
							+ invoiceNumber);

					String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ? "
							+ " where INVOICENUMBER = ? AND PONUMBER = ? ";

					ps = con.prepareStatement(updateGRN);
					ps.setString(1, returnGRNForPO);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, invoiceNumber);
					ps.setString(4, key);
					ps.executeUpdate();
					ps.close();

					if (("Y").equalsIgnoreCase(status)) {
						String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=? "
								+ "where INVOICENUMBER=? AND PONUMBER=?";

						ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
						ps.setString(1, enduser);
						ps.setString(2, invoiceNumber);
						ps.setString(3, key);
						value = ps.executeUpdate();
						ps.close();

						String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";
						ps = con.prepareStatement(insertauditacceptqty);
						ps.setString(1, key);
						ps.setString(2, invoiceNumber);
						ps.setString(3, email);
						ps.setString(4, "Y");
						ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
						value = ps.executeUpdate();
						ps.close();
					}
				}
			} // end of map for loop
			con.commit();
			grnLIST.add(poNoMapMain);
			if (grnLIST != null && grnLIST.size() > 0) {
				responsejson.put("grn", grnLIST);
				responsejson.put("message", "Success");
				responsejson.put("warningMessage", warningMessage);
				jsonArray.add(responsejson);
			} else {

				if (!"E".equalsIgnoreCase(error)) {
					message = warningMessage;
				}
				responsejson.put("message", message);
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("getAcceptQtynGRN() :", e.fillInStackTrace());
			responsejson.put("message", "SQL Error while accepting Quantity !!");
			jsonArray.add(responsejson);
			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	// for uat
	public JSONArray getAcceptQtynServiceGRN(List<String> acceptedValues, String email) throws SQLException {

		String[] acceptedList = acceptedValues.toArray(new String[acceptedValues.size()]);
		int value = 0;
		String returnGRN = null;
		String poNumber = null;
		String invoiceNumber = null;
		String invoiceDate = null;
		String poLineitem = null;
		String storageLocation = null;
		String quantity = null;
		String grnyear = null;
		String grnqty = null;
		String grnNumber = null;
		String serviceNo = null;
		String serviceLineitem = null;
		String scrnNoList = null;
		String error = null;
		String message = null;
		String orderNumber = null;
		String enduser = null;
		String portalid = null;
		String status = null;
		String warningMessage = null;
		ArrayList sendingList = new ArrayList();
		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		ArrayList SAPReturnValues = new ArrayList();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		int poCounter = 1;
		String showGrnList = null;
		String showScrnList = null;
		String comboPoLineitem = null;
		portalid = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		LinkedHashMap poNoMap = new LinkedHashMap();
		LinkedHashMap poNoLineItemMap = new LinkedHashMap();
		LinkedHashMap poNoLineItemIndexWiseMap = new LinkedHashMap();
		LinkedHashMap poWiseGrnMap = new LinkedHashMap();
		LinkedHashMap poWiseScrnMap = new LinkedHashMap();
		ArrayList poWiseGrnList = new ArrayList();
		ArrayList poWiseScrnList = new ArrayList();

		try {
			prop.load(input);
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

				String[] fatchedValues = acceptedList[counterValue].split(",");
				poNumber = null;
				invoiceNumber = null;
				invoiceDate = null;
				poLineitem = null;
				quantity = null;
				grnNumber = null;
				grnyear = null;
				grnqty = null;
				serviceNo = null;
				serviceLineitem = null;
				String[] lineItemNo = null;
				comboPoLineitem = null;

				poNumber = fatchedValues[0]; // 00010-10
				comboPoLineitem = fatchedValues[1];
				poNumber = fatchedValues[0]; // 00010-10
				if (fatchedValues[1].contains("-")) {
					lineItemNo = fatchedValues[1].split("-");
					poLineitem = lineItemNo[0]; // 00010
					serviceLineitem = lineItemNo[1]; // 10
				} else {
					poLineitem = fatchedValues[1];
					serviceLineitem = fatchedValues[1];
				}

				poNoMap.put(poNumber, "-"); // ponumber-00010 ,ponumber-00020
				serviceNo = fatchedValues[2];
				invoiceNumber = fatchedValues[3];
				Date date = (Date) formatter.parseObject(fatchedValues[4]);
				invoiceDate = sm.format(date);
				quantity = fatchedValues[5];
				storageLocation = fatchedValues[6];
				enduser = fatchedValues[7];
				portalid = fatchedValues[8];
				status = fatchedValues[9];

				String mainList[] = { poNumber, poLineitem, serviceLineitem, serviceNo, invoiceNumber, invoiceDate,
						quantity, storageLocation, portalid };
				sendingList.add(mainList);

				log.info("poNumber : " + poNumber + " poLineitem :  " + poLineitem + " serviceLineitem : "
						+ serviceLineitem + " serviceNo : " + serviceNo + " invoiceNumber : " + invoiceNumber
						+ " invoiceDate : " + invoiceDate + " quantity : " + quantity + " storageLocation : "
						+ storageLocation + " portalid : " + portalid);

				String actualQuantityUpdate = "update DELIVERYSUMMARY set ACCEPTEDQTY = ? ,STORAGELOCATION = ?  "
						+ " where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER = ? ";

				ps = con.prepareStatement(actualQuantityUpdate);
				ps.setString(1, quantity);
				ps.setString(2, storageLocation);
				ps.setString(3, invoiceNumber);
				ps.setString(4, poNumber);
				ps.setString(5, comboPoLineitem);
				value = ps.executeUpdate();
				ps.close();
			}

			SAPConnectionDetails.put("CLIENT", prop.getProperty("CLIENT"));
			SAPConnectionDetails.put("USERID", prop.getProperty("USERID"));
			SAPConnectionDetails.put("PASSWORD", prop.getProperty("PASSWORD"));
			SAPConnectionDetails.put("LANGUAGE", prop.getProperty("LANGUAGE"));
			SAPConnectionDetails.put("HOSTNAME", prop.getProperty("HOSTNAME"));
			SAPConnectionDetails.put("SYSTEMNO", prop.getProperty("SYSTEMNO"));
			SAPConnectionDetails.put("RFCNAME", prop.getProperty("SERVICE_RFCNAME"));
			SAPConnectionDetails.put("TOTALIMPORTTABLESTOSET", prop.getProperty("SERVICE_TOTALIMPORTTABLESTOSET"));
			SAPConnectionDetails.put("RETURNTABLE1", prop.getProperty("SERVICE_RETURNTABLE1"));
			SAPConnectionDetails.put("RETURNTABLE2", prop.getProperty("SERVICE_RETURNTABLE2"));
			SAPConnectionDetails.put("IMPORTTABLENAME1", prop.getProperty("SERVICE_IMPORTTABLENAME1"));
			SAPConnectionDetails.put("TOTALTABLESTORETURN", prop.getProperty("SERVICE_TOTALTABLESTORETURN"));

			String itemList1[] = { "EBELN", "EBELP", "SL_ITEM", "ACTIVITY", "XBLNR", "BLDAT", "MENGE", "LGORT",
					"WEMPF" };
			String returnKeys1[] = { "MAT_DOC", "DOC_YEAR", "QUANTITY", "SERVICE" };
			String returnKeys2[] = { "TYPE", "CODE", "MESSAGE" };

			SAPColumnHeads.put("IMPORTLINEITEM1", itemList1);

			Set<String> keys = poNoMap.keySet();

			for (String key : keys) {

				log.info("Value of " + key + " is: " + poNoMap.get(key));

				poNoLineItemMap = new LinkedHashMap();
				poNoLineItemIndexWiseMap = new LinkedHashMap<>();
				int index = 0;
				ArrayList newLineItemList = new ArrayList();

				for (int ii = 0; ii < sendingList.size(); ii++) {

					String sapList[] = (String[]) sendingList.get(ii);
					String myPonumber = sapList[0];
					String myPoLineItemNumber = sapList[1];

					if (key.equals(myPonumber)) {
						newLineItemList.add(sapList);
						if (!poNoLineItemMap.containsKey(myPonumber + "-" + myPoLineItemNumber)) {
							poNoLineItemMap.put(myPonumber + "-" + myPoLineItemNumber, index + 1);
							index++;
						}
					}
				}

				log.info(key + " SIZE of lineitem " + poNoLineItemMap.size());

				SAPValues.put("IMPORTLINEITEMLIST1", newLineItemList);
				SAPColumnHeads.put("RETURNKEYS1", returnKeys1);
				SAPColumnHeads.put("RETURNKEYS2", returnKeys2);
				JcoGetDataFromSAP jco = new JcoGetDataFromSAP("dxproject");
				SAPReturnData = jco.jcoGetData(SAPConnectionDetails, SAPColumnHeads, SAPValues);

				log.info("SAPReturnData :" + SAPReturnData.size() + " Invoice Number : " + invoiceNumber);
				SAPReturnData.forEach((k, v) -> log.info("Key : " + k + ", Value : " + v));

				warningMessage = "";
				if (SAPReturnData.containsKey("RETURNDATA1")) {
					SAPReturnValues = (ArrayList) SAPReturnData.get("RETURNDATA1");

					int counter = SAPReturnValues.size();
					if (counter > 0) {

						for (int ii = 0; ii < counter; ii++) {

							String arrayData[] = (String[]) SAPReturnValues.get(ii);
							log.info("arrayData[] 1 :" + arrayData.toString() + " Invoice Number : " + invoiceNumber);
							returnGRN = arrayData[0];
							grnyear = arrayData[1];
							grnqty = arrayData[2];
							serviceLineitem = arrayData[3];
							log.info("returnGRN :" + returnGRN + " :grnyear: " + grnyear + " :grnqty: " + grnqty
									+ " Invoice Number : " + invoiceNumber);
							String putGRNNSCRNValues = returnGRN + "-" + serviceLineitem;
							int myKey = ii + 1;
							poNoLineItemIndexWiseMap.put(myKey, putGRNNSCRNValues);
						}
					}
				}

				log.info(key + " size of polinitem response from sap " + poNoLineItemIndexWiseMap.size());

				if (SAPReturnData.containsKey("RETURNDATA2")) {
					SAPReturnValues = (ArrayList) SAPReturnData.get("RETURNDATA2");
					log.info("SAPReturnValues :" + SAPReturnValues.size() + " Invoice Number : " + invoiceNumber);
					int counter = SAPReturnValues.size();
					if (counter > 0) {
						warningMessage = "";
						for (int ii = 0; ii < counter; ii++) {
							String arrayData[] = (String[]) SAPReturnValues.get(ii);
							error = arrayData[0];
							message = arrayData[2];
							log.info("arrayData[] 2 :" + arrayData.length + " : error : " + error + " : message : "
									+ message + " Invoice Number : " + invoiceNumber);

							if ("W".equalsIgnoreCase(error) || "S".equalsIgnoreCase(error)
									|| "I".equalsIgnoreCase(error) || "E".equalsIgnoreCase(error)) {
								warningMessage = warningMessage + message + ",";
							}
						}

						if (warningMessage.length() > 2) {
							warningMessage = warningMessage.substring(0, warningMessage.length() - 1);
						}
					}
				}

				if (returnGRN != null && returnGRN != "") {
					showGrnList = "";
					showScrnList = "";
					for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

						String[] fatchedValues = acceptedList[counterValue].split(",");

						poNumber = null;
						invoiceNumber = null;
						invoiceDate = null;
						poLineitem = null;
						quantity = null;
						grnNumber = null;
						grnyear = null;
						grnqty = null;
						serviceNo = null;
						serviceLineitem = null;
						String[] lineItemNo = null;
						comboPoLineitem = null;

						poNumber = fatchedValues[0]; // 00010-10
						comboPoLineitem = fatchedValues[1];
						if (fatchedValues[1].contains("-")) {
							lineItemNo = fatchedValues[1].split("-");
							poLineitem = lineItemNo[0]; // 00010
							serviceLineitem = lineItemNo[1]; // 10
						} else {
							poLineitem = fatchedValues[1];
							serviceLineitem = fatchedValues[1];
						}
						invoiceNumber = fatchedValues[3];

						quantity = fatchedValues[5];
						storageLocation = fatchedValues[6];

						if (!key.equals(poNumber)) {
							continue;
						}
						String getGrnScrnVal = (String) poNoLineItemIndexWiseMap
								.get(poNoLineItemMap.get(poNumber + "-" + poLineitem));
						log.info("getGrnScrnVal ==> " + getGrnScrnVal + " Invoice Number : " + invoiceNumber);

						String[] grnNscrnValue = getGrnScrnVal.split("-");

						if (!showGrnList.contains(grnNscrnValue[0]) == true) {
							showGrnList = showGrnList + grnNscrnValue[0] + ",";
						}
						if (!showScrnList.contains(grnNscrnValue[1]) == true) {
							showScrnList = showScrnList + grnNscrnValue[1] + ",";
						}
						log.info("showGrnList ==> " + showGrnList.toString() + "showScrnList ==> "
								+ showScrnList.toString() + " Invoice Number : " + invoiceNumber);

						String actualQuantityUpdate = "update DELIVERYSUMMARY set GRNNUMBER = ?, "
								+ "SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER like ? ";

						ps = con.prepareStatement(actualQuantityUpdate);
						ps.setString(1, grnNscrnValue[0]);
						ps.setString(2, grnNscrnValue[1]);
						ps.setString(3, invoiceNumber);
						ps.setString(4, poNumber);
						ps.setString(5, poLineitem + "%");
						value = ps.executeUpdate();
						ps.close();
					}

					if (("Y").equalsIgnoreCase(status)) {
						String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=? "
								+ "where INVOICENUMBER=? AND PONUMBER=?";
						ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
						ps.setString(1, enduser);
						ps.setString(2, invoiceNumber);
						ps.setString(3, key);
						value = ps.executeUpdate();
						ps.close();

						if (value != 0) {

							String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";
							ps = con.prepareStatement(insertauditacceptqty);
							ps.setString(1, key);
							ps.setString(2, invoiceNumber);
							ps.setString(3, email);
							ps.setString(4, "Y");
							ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
							value = ps.executeUpdate();
							ps.close();
						}
					}

					String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ?, "
							+ " SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? ";

					showScrnList = showScrnList.substring(0, showScrnList.length() - 1);
					showGrnList = showGrnList.substring(0, showGrnList.length() - 1);
					ps = con.prepareStatement(updateGRN);
					ps.setString(1, showGrnList);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, showScrnList);
					ps.setString(4, invoiceNumber);
					ps.setString(5, key);
					ps.executeUpdate();
					ps.close();
				}
				con.commit();
				poWiseGrnList.add(showGrnList);
				poWiseScrnList.add(showScrnList);
			}

			if (returnGRN != null && returnGRN != "") {
				responsejson.put("grnlist", poWiseGrnList.toString());
				responsejson.put("scrnlist", poWiseScrnList.toString());
				responsejson.put("message", "Success");
				responsejson.put("warningMessage", warningMessage);
				jsonArray.add(responsejson);
			} else {
				if ("E".equalsIgnoreCase(error)) {
				} else {
					message = "Empty";
				}

				if (!"E".equalsIgnoreCase(error)) {
					message = warningMessage;
				}
				responsejson.put("message", message);
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			con.rollback();
			log.error("getAcceptQtynServiceGRN() :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray getManagerApprvalStatusall(String invoiceNumber, String po_num, String managerstatus,
			String managerId, String stage, String invoiceDate, String bId) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(invoiceNumber);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(managerId);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(stage);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		result = Validation.StringChecknull(managerstatus);
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

		String po_status = "UPDATE invoiceapproval set STATUS=?,MODIFIEDDATE=?,STAGE=? "
				+ "where INVOICENUMBER=? and PONUMBER =? and EUMANAGER =? ";

		int value = 0;
		int valueDetails = 0;
		int holdCount = 0;
		int maCount = 0;
		String Status = null;
		String overAllStatus = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean statusValueHo = false;
		boolean allManagerStatus = false;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(po_status);
			ps.setString(1, managerstatus);
			ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(3, stage);
			ps.setString(4, invoiceNumber);
			ps.setString(5, po_num);
			ps.setString(6, managerId);
			value = ps.executeUpdate();
			ps.close();

			if (value <= 0) {
				throw new DXPortalException("Error while updating INVOICEAPPROVAL",
						" Error in getManagerApprvalStatusall()");
			}
			int onHoldCounter = 0;
			String onHoldStatus = "select count(*) as counter from INVOICEAPPROVAL where (STATUS = 'O' OR STATUS = 'CO') AND INVOICENUMBER = ? "
					+ "AND PONUMBER = ? ";

			ps = con.prepareStatement(onHoldStatus);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			if (rs.next()) {
				onHoldCounter = rs.getInt("counter");
			}
			ps.close();

			InternalportalImpl ipi = new InternalportalImpl();
			if (managerstatus.equalsIgnoreCase("A")) {

				overAllStatus = ipi.getoverAllStatus(invoiceNumber, po_num, con);
				if (overAllStatus.equalsIgnoreCase("O")) {

					if (onHoldCounter > 1) {
						statusValueHo = ipi.getdecHoCount(invoiceNumber, po_num, con);
					} else {
						statusValueHo = ipi.getDecMaCount(invoiceNumber, po_num, con);
					}

					if (statusValueHo) {
						maCount = ipi.getmaCount(invoiceNumber, po_num, con);
						if (onHoldCounter > 0) {
							Status = "O";
						} else if ((maCount == 0)) {
							Status = "A";
						} else {
							Status = "M";
						}
					}
				} else if (overAllStatus.equalsIgnoreCase("M")) {
					maCount = ipi.getmaCount(invoiceNumber, po_num, con);
					if ((maCount == 0)) {
						Status = "A";
					} else {
						statusValueHo = ipi.getDecMaCount(invoiceNumber, po_num, con);
						if (statusValueHo) {
							maCount = ipi.getmaCount(invoiceNumber, po_num, con);
							if ((maCount == 0)) {
								Status = "A";
							} else {
								Status = "M";
							}
							if (onHoldCounter > 0) {
								Status = "O";
							}
						} else {
							Status = "M";
						}
					}
				} else {
					Status = "M";
				}
			}

			else if (managerstatus.equalsIgnoreCase("M")) {
				overAllStatus = ipi.getoverAllStatus(invoiceNumber, po_num, con);
				if (overAllStatus.equalsIgnoreCase("A")) {
					statusValueHo = ipi.getIncMaCount(invoiceNumber, po_num, con);
					Status = "M";
				} else if (overAllStatus.equalsIgnoreCase("O")) {
					statusValueHo = true;
					if (onHoldCounter > 1) {
						statusValueHo = ipi.getdecHoCountOnly(invoiceNumber, po_num, con);
					}
					if (statusValueHo) {
						holdCount = ipi.getholdcount(invoiceNumber, po_num, con);
						if (holdCount == 0) {
							Status = "M";
						} else {
							Status = "O";
						}
					} else {
						Status = "O";
					}
				}
			} else if (managerstatus.equalsIgnoreCase("O")) {
				overAllStatus = ipi.getoverAllStatus(invoiceNumber, po_num, con);
				if (overAllStatus.equalsIgnoreCase("A")) {
					statusValueHo = ipi.getIncHOMaCount(invoiceNumber, po_num, con);
					if (statusValueHo) {
						Status = "O";
					} else {
						Status = "M";
					}
				} else if (overAllStatus.equalsIgnoreCase("M")) {
					statusValueHo = ipi.getIncHoCountOnly(invoiceNumber, po_num, con);
					if (statusValueHo) {
						Status = "O";
					} else {
						Status = "M";
					}
				} else {
					statusValueHo = ipi.getIncHoCountOnly(invoiceNumber, po_num, con);
					Status = "O";
				}
			} else {
				Status = "M";
			}
			String summaryStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and INVOICEDATE=? "
					+ "and  BUSINESSPARTNEROID = ?";
			ps = con.prepareStatement(summaryStatus);
			ps.setString(1, Status);
			ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(3, invoiceNumber);
			ps.setString(4, invoiceDate);
			ps.setString(5, bId);
			valueDetails = ps.executeUpdate();
			ps.close();

			boolean flag = false;
			if ((!managerstatus.equalsIgnoreCase("O")) && (!managerstatus.equalsIgnoreCase("P"))) {

				String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
						+ "MODIFIEDTIME,MODIFIEDBY)" + " values(?,?,?,?,?,?)";
				ps = con.prepareStatement(insertaudit);
				ps.setString(1, invoiceNumber);
				ps.setString(2, po_num);
				ps.setString(3, "");
				ps.setString(4, Status);
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(6, managerId);
				ps.executeUpdate();
				ps.close();
			}
			if (!managerstatus.equalsIgnoreCase("P")) {
				// flag = ipi.sendMailtoManager(invoiceNumber, po_num, managerId, con);
				flag = true;
			}
			responsejson.put("emailstatus", flag);
			if (valueDetails <= 0) {
				throw new DXPortalException("Error while updating PONINVOICESUMMERY",
						" Error in getManagerApprvalStatusall()");
			}
			con.commit();
			responsejson.put("message", "Success");

		} catch (DXPortalException dxp) {
			con.rollback();
			responsejson.put("error", dxp.reason);
			log.error("getManagerApprvalStatusall() 1 :", dxp.fillInStackTrace());

		} catch (Exception e) {
			log.error("getManagerApprvalStatusall() 2 :", e.fillInStackTrace());
			con.rollback();
			responsejson.put("error", e.getMessage());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getsimpoinvoicebasedonemailid(String emailId, HttpSession session, int nPage, String status,
			String pono, String invno, String fdate, String tdate, String plant, String vendor) throws SQLException {
		InternalportalImpl internalImpl = new InternalportalImpl();
		boolean result;
		result = Validation.StringChecknull(emailId);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.emailCheck(emailId);
			if (result == false) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String sql = null;
		ArrayList<HashMap<String, String>> POEvent = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> POListEvent = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		String storeKepeerQuery = null;
		String storeKepeer = (String) session.getAttribute("shopkepeer");
		String getstoreKepeermaterialtype = "Select MTYP,PLANT from INVENTORYUSERLIST where USERID=?";
		try {
			con = DBConnection.getConnection();
			if (!"AS".equalsIgnoreCase(status)) {

				if ("C".equalsIgnoreCase(status)) {

					sql = "select * from ( select PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO rownum rnum from (select DISTINCT "
							+ "B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,"
							+ "B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,"
							+ "A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,"
							+ "B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
							+ "B.CREATEDON,B.PAYMENTAMOUNT,"
							+ "B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID, B.MPO, B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  "
							+ "A.InvoiceNumber=B.InvoiceNumber and A.PONumber=B.PONumber AND A.ENDUSEID = ?  "
							+ "AND CREDITADVICENO IS NOT NULL " + "UNION "
							+ "select  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,"
							+ "A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,B1.ACTUALFILENAME,"
							+ "B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,"
							+ "B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,B1.CREDITNOTENO,B1.CREDITADVICENO,"
							+ "B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,"
							+ "B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO from "
							+ "invoiceapproval A1 join PONINVOICESUMMERY B1  on A1.InvoiceNumber=B1.InvoiceNumber and "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?)  AND "
							+ "CREDITADVICENO IS NOT NULL order by CREATEDON desc) c )";

					storeKepeerQuery = "select * from ( select PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO rownum rnum from (select DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,"
							+ "B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,"
							+ "A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,"
							+ "B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,"
							+ "B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,"
							+ "B.EXPENSESHEETID,B.MPO,B.ALLPO from "
							+ "invoiceapproval A join PONINVOICESUMMERY B  on  A.InvoiceNumber=B.InvoiceNumber and "
							+ "A.PONumber=B.PONumber 	AND A.ENDUSEID = ? AND A.PROXY IS NULL AND CREDITADVICENO IS NOT NULL  "
							+ "UNION " + "select  B1.PONUMBER,B1.INVOICENUMBER,"
							+ "B1.INVOICEDATE,A1.ENDUSEID,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STATUS,	"
							+ "A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,B1.ACTUALFILENAME,B1.SAVEDFILENAME,"
							+ "B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,B1.CREATEDON,"
							+ "B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO "
							+ "from invoiceapproval A1 join PONINVOICESUMMERY B1  on  A1.InvoiceNumber=B1.InvoiceNumber and "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND CREDITADVICENO IS NOT NULL"
							+ " UNION " + "select DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO "
							+ " from invoiceapproval A "
							+ "join PONINVOICESUMMERY B  on  A.InvoiceNumber=B.InvoiceNumber and "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND CREDITADVICENO IS NOT NULL  join "
							+ "inventoryuserlist inv on inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? order by CREATEDON desc) c )";
				} else if ("ALL".equalsIgnoreCase(status)) {

					sql = "select * from ( select PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, rownum rnum from (select DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID from invoiceapproval A join PONINVOICESUMMERY B  on  "
							+ "A.InvoiceNumber=B.InvoiceNumber and A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
							+ "UNION " + "select  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,"
							+ "B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,B1.CREDITNOTENO,B1.CREDITADVICENO,"
							+ "B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID"
							+ " from invoiceapproval "
							+ "A1 join PONINVOICESUMMERY B1  on  A1.InvoiceNumber=B1.InvoiceNumber and "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?)  " + " order by CREATEDON desc) c )";

					storeKepeerQuery = "select * from ( select PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO rownum rnum from (select DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  "
							+ "A.InvoiceNumber=B.InvoiceNumber and "
							+ "A.PONumber=B.PONumber 	AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + "UNION "
							+ "select  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,"
							+ "A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,B1.ACTUALFILENAME,"
							+ "B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,"
							+ "B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,B1.CREDITNOTENO,B1.CREDITADVICENO,"
							+ "B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO from "
							+ "invoiceapproval A1 join PONINVOICESUMMERY B1  on  A1.InvoiceNumber=B1.InvoiceNumber and "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) " + " UNION "
							+ "select DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  A.InvoiceNumber=B.InvoiceNumber and "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X'  join "
							+ "inventoryuserlist inv on inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? order by CREATEDON desc) c )";

				} else {
					if ("P".equalsIgnoreCase(status)) {
						sql = "select * from ( select PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO rownum rnum from (select DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
								+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
								+ "B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  "
								+ "on  "
								+ "A.InvoiceNumber=B.InvoiceNumber and A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? OR  B.OVERALLSTATUS=?) " + "UNION "
								+ "select  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO from invoiceapproval "
								+ "A1 join PONINVOICESUMMERY B1  on  A1.InvoiceNumber=B1.InvoiceNumber and "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?)  AND "
								+ "(B1.OVERALLSTATUS=? OR  B1.OVERALLSTATUS=?) order by CREATEDON desc) c )";

						storeKepeerQuery = "select * from ( select PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO rownum rnum from (select DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  "
								+ "A.InvoiceNumber=B.InvoiceNumber and "
								+ "A.PONumber=B.PONumber 	AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=? OR  "
								+ "B.OVERALLSTATUS=?) " + "UNION "
								+ "select  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,B1.CREATEDON,"
								+ "B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO from invoiceapproval "
								+ "A1 join PONINVOICESUMMERY B1  on  A1.InvoiceNumber=B1.InvoiceNumber and "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND (B1.OVERALLSTATUS=? OR  B1.OVERALLSTATUS=?)"
								+ " UNION " + "select DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,b.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  A.InvoiceNumber=B.InvoiceNumber and "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND (B.OVERALLSTATUS=? OR  B.OVERALLSTATUS=?) join "
								+ "inventoryuserlist inv on inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? order by CREATEDON desc) c )";
					} else {
						sql = "select * from ( select PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, rownum rnum from (select DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  "
								+ "A.InvoiceNumber=B.InvoiceNumber and A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? ) " + "UNION "
								+ "select  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO from invoiceapproval "
								+ "A1 join PONINVOICESUMMERY B1  on  A1.InvoiceNumber=B1.InvoiceNumber and "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?)  AND "
								+ "(B1.OVERALLSTATUS=? ) order by CREATEDON desc) c )";

						storeKepeerQuery = "select * from ( select PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, rownum rnum from (select DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  "
								+ "A.InvoiceNumber=B.InvoiceNumber and "
								+ "A.PONumber=B.PONumber 	AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=? ) "
								+ "UNION " + "select  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO from invoiceapproval A1 join PONINVOICESUMMERY B1  on  A1.InvoiceNumber=B1.InvoiceNumber and "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND" + "(B1.OVERALLSTATUS=?)"
								+ " UNION " + "select DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  A.InvoiceNumber=B.InvoiceNumber and "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND (B.OVERALLSTATUS=?) join "
								+ "inventoryuserlist inv on inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "order by CREATEDON desc) c )";
					}
				}

				if ("true".equalsIgnoreCase(storeKepeer)) {
					ps = con.prepareStatement(storeKepeerQuery);
					responsejson.put("storekeeper", "true");
					ArrayList<String> param = new ArrayList<String>();

					if (status.equalsIgnoreCase("C") || status.equalsIgnoreCase("ALL")) {
						param.add(emailId);
						param.add(emailId);
						param.add(emailId);
					} else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {
						param.add(emailId);
						param.add(status);
						if ("P".equalsIgnoreCase(status)) {
							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
							param.add(status);
							param.add("M");
							param.add(emailId);
						} else {
							param.add(emailId);
							param.add(status);
							param.add(status);
							param.add(emailId);
						}
					}
					Pagination pg = new Pagination(storeKepeerQuery, nPage, 1);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				} else {

					ps = con.prepareStatement(sql);
					responsejson.put("storekeeper", "false");
					ArrayList<String> param = new ArrayList<String>();
					if (status.equalsIgnoreCase("C") || status.equalsIgnoreCase("ALL")) {
						param.add(emailId);
						param.add(emailId);
					} else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {
						param.add(emailId);
						param.add(status);
						if ("P".equalsIgnoreCase(status)) {
							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
						} else {
							param.add(emailId);
							param.add(status);
						}
					}
					Pagination pg = new Pagination(sql, nPage, 1);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				}
				while (rs.next()) {
					HashMap<String, String> poEvent = new HashMap<String, String>();
					poEvent.put("PONUMBER", rs.getString("PONUMBER"));
					poEvent.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poEvent.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					poEvent.put("ENDUSERID", rs.getString("ENDUSEID"));
					poEvent.put("ENDUSERSTATUS", rs.getString("ENDUSERSTATUS"));
					poEvent.put("EUMANAGER",
							internalImpl.getmanagerslist(rs.getString("INVOICENUMBER"), rs.getString("PONUMBER"), con));
					poEvent.put("STATUS", rs.getString("STATUS"));
					poEvent.put("TOTALAMOUNT", rs.getString("amount"));
					poEvent.put("STAGE", rs.getString("STAGE"));
					poEvent.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poEvent.put("PROXY", rs.getString("PROXY"));
					poEvent.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					poEvent.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					poEvent.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poEvent.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
					poEvent.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poEvent.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poEvent.put("VENDORID", rs.getString("VENDORID"));
					poEvent.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poEvent.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					poEvent.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					poEvent.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					poEvent.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					poEvent.put("ACTIONBY", rs.getString("ACTIONBY"));
					poEvent.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
					poEvent.put("GRNNUMBER", rs.getString("GRNNUMBER"));
					poEvent.put("USERID", internalImpl.getemailidbasedonmaterial(rs.getString("MATERIAL_TYPE"),
							rs.getString("PLANT")));
					poEvent.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					poEvent.put("MPO", rs.getString("MPO"));
					poEvent.put("ALLPO", rs.getString("ALLPO"));
					POListEvent.add(poEvent);
				}
			} else {
				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				if ("true".equalsIgnoreCase(storeKepeer)) {
					param.add(emailId);
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND B.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = " AND B.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND B.PONUMBER=?";
						subquery = subquery + po;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND B.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND B.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);
					for (int i = 0; i < 2; i++) {
						if (!plant.equalsIgnoreCase("NA")) {
							param.add(plant);
						}
						if (!vendor.equalsIgnoreCase("NA")) {
							param.add(vendor);
						}
						if (!pono.equalsIgnoreCase("NA")) {
							param.add(pono);
						}
						if (!invno.equalsIgnoreCase("NA")) {
							param.add(invno);
						}
						if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
							param.add(fdate);
							param.add(tdate);
						}
					}
					param.add(emailId);
				} else {
					param.add(emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND B.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = " AND B.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND B.PONUMBER=?";
						subquery = subquery + po;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND B.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND B.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}
				}

				sql = "select * from ( select BASEPO,PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
						+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
						+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
						+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, rownum rnum from (select DISTINCT "
						+ "B.BASEPO , B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,"
						+ "A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,"
						+ "B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
						+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from "
						+ "invoiceapproval A join PONINVOICESUMMERY B  on  "
						+ "A.InvoiceNumber=B.InvoiceNumber and A.PONumber=B.BASEPO  AND A.ENDUSEID = ? " + subquery
						+ " " + "UNION " + "select  B.BASEPO,B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ "B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval "
						+ "A1 join PONINVOICESUMMERY B  on  A1.InvoiceNumber=B.InvoiceNumber and "
						+ "A1.PONumber=B.BASEPO  AND (A1.EUMANAGER = ?)  " + subquery + " "
						+ " order by CREATEDON desc) c )";

				storeKepeerQuery = "select * from ( select BASEPO,PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
						+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
						+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
						+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, rownum rnum from (select DISTINCT B.BASEPO,B.PONUMBER,B.INVOICENUMBER,"
						+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
						+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
						+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  "
						+ "A.InvoiceNumber=B.InvoiceNumber and "
						+ "A.PONumber=B.BASEPO 	AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + subquery + " " + "UNION "
						+ "select B.BASEPO, B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ "B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval "
						+ "A1 join PONINVOICESUMMERY B  on  A1.InvoiceNumber=B.InvoiceNumber and "
						+ "A1.PONumber=B.BASEPO  AND (A1.EUMANAGER = ?) " + subquery + " " + " UNION "
						+ "select DISTINCT B.BASEPO,B.PONUMBER,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
						+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
						+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
						+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO from invoiceapproval A join PONINVOICESUMMERY B  on  A.InvoiceNumber=B.InvoiceNumber and "
						+ "A.PONumber=B.BASEPO  AND A.PROXY = 'X' " + subquery + " join "
						+ "inventoryuserlist inv on inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? order by CREATEDON desc) c )";

				Pagination pg = null;
				if ("true".equalsIgnoreCase(storeKepeer)) {
					responsejson.put("storekeeper", "true");
					pg = new Pagination(storeKepeerQuery, nPage, 2);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				} else {
					responsejson.put("storekeeper", "false");
					pg = new Pagination(sql, nPage, 1);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				}

				while (rs.next()) {
					HashMap<String, String> poEvent = new HashMap<String, String>();
					poEvent.put("PONUMBER", rs.getString("PONUMBER"));
					poEvent.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poEvent.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					poEvent.put("ENDUSERID", rs.getString("ENDUSEID"));
					poEvent.put("ENDUSERSTATUS", rs.getString("ENDUSERSTATUS"));
					poEvent.put("EUMANAGER",
							internalImpl.getmanagerslist(rs.getString("INVOICENUMBER"), rs.getString("PONUMBER"), con));
					poEvent.put("STATUS", rs.getString("STATUS"));
					poEvent.put("TOTALAMOUNT", rs.getString("amount"));
					poEvent.put("STAGE", rs.getString("STAGE"));
					poEvent.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poEvent.put("PROXY", rs.getString("PROXY"));
					poEvent.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					poEvent.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					poEvent.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poEvent.put("MATERIAL", rs.getString("MATERIAL_TYPE"));
					poEvent.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poEvent.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poEvent.put("VENDORID", rs.getString("VENDORID"));
					poEvent.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poEvent.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					poEvent.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					poEvent.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					poEvent.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					poEvent.put("ACTIONBY", rs.getString("ACTIONBY"));
					poEvent.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
					poEvent.put("GRNNUMBER", rs.getString("GRNNUMBER"));
					poEvent.put("USERID", internalImpl.getemailidbasedonmaterial(rs.getString("MATERIAL_TYPE"),
							rs.getString("PLANT")));
					poEvent.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					poEvent.put("MPO", rs.getString("MPO"));
					poEvent.put("ALLPO", rs.getString("ALLPO"));
					POListEvent.add(poEvent);
				}
				pg.close();
				rs.close();
				pg = null;
			}
			if (POListEvent.size() > 0) {
				responsejson.put("invoicedetails", POListEvent);
				responsejson.put("invoicedetailsrecords", pages);
				responsejson.put("message", "Success");
			} else {
				responsejson.put("message", "No Data Found");
			}
			try {
				internalImpl.getinvoicebasedonemailidCountAsPerStatus(emailId, session, nPage, status, pono, invno,
						fdate, tdate, plant, vendor, con, ps, rs);
			} catch (Exception e) {
			}

		} catch (Exception e) {
			log.error("getsimpoinvoicebasedonemailid() :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getPayerPoninvoiceSummery(int nPage, String status, String invno, String pono, String fdate,
			String tdate, String plant, String vendor) throws SQLException {
		InternalportalImpl internalImpl = new InternalportalImpl();

		String basePoQuery = " and ps.ponumber=ia.ponumber ";
		boolean basePoFlag = false;
		// For pending QUANTITY
		String pendingsql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL "
				+ "AND (OVERALLSTATUS=? or OVERALLSTATUS=?) " + "UNION "
				+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
				+ "(OVERALLSTATUS=? or OVERALLSTATUS=?)" + " order by CREATEDON desc";

		// For OFFLINE INVOICES
		String hdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL "
				+ "AND ONEXSTATUS=? " + "UNION "
				+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
				+ "AND ONEXSTATUS=?" + " order by CREATEDON desc";

		// All Filter
		String sql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL "
				+ "AND OVERALLSTATUS=? " + "UNION "
				+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
				+ "OVERALLSTATUS=?" + " order by CREATEDON desc";

		// For Short quantity
		String sdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL "
				+ "AND ps.CREDITADVICENO IS NOT NULL " + "UNION "
				+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
				+ "ps.CREDITADVICENO IS NOT NULL" + " order by CREATEDON desc";

		// All Status
		String alldata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL " + "UNION "
				+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
				+ "ps.CREDITADVICENO IS NOT NULL" + " order by CREATEDON desc";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		int pages = 0;
		try {
			con = DBConnection.getConnection();
			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ"))) {
				ArrayList<String> param = new ArrayList<String>();
				Pagination pg = null;
				if (status.equalsIgnoreCase("H")) {
					param.add(status);
					param.add(status);
					log.info("hdata : " + hdata);
					pg = new Pagination(hdata, nPage);
				} else if (status.equalsIgnoreCase("ALL")) {
					log.info("alldata : " + alldata);
					pg = new Pagination(alldata, nPage);
				} else if (status.equalsIgnoreCase("C")) {
					log.info("sdata : " + sdata);
					pg = new Pagination(sdata, nPage);
				} else if (status.equalsIgnoreCase("P")) {
					param.add("P");
					param.add("M");
					param.add("P");
					param.add("M");
					log.info("pendingsql : " + pendingsql);
					pg = new Pagination(pendingsql, nPage);
				} else if (status.equalsIgnoreCase("V")) {
					param.add("V");
					param.add("RO");
					param.add("V");
					param.add("RO");
					log.info("pendingsql : " + pendingsql);
					pg = new Pagination(pendingsql, nPage);
				} else {
					param.add(status);
					param.add(status);
					log.info("sql : " + sql);
					pg = new Pagination(sql, nPage);
				}

				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				int count = 0;
				String invNumber = null;
				String invDate = null;
				String mPO = null;
				String bpid = null;

				while (rs.next()) {
					count++;
					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("PONUMBER", rs.getString("PONUMBER"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poData.put("MESSAGE", rs.getString("MESSAGE"));
					poData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
					poData.put("BUYER", rs.getString("BUYER"));
					poData.put("AMOUNT", rs.getString("AMOUNT"));
					poData.put("MACOUNT", rs.getString("MACOUNT"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poData.put("HOLDCOUNT", rs.getString("HOLDCOUNT"));
					poData.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					poData.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
					poData.put("MATERIAL_TYPE", rs.getString("MATERIAL_TYPE"));
					poData.put("PGQ", rs.getString("PGQ"));
					poData.put("ONEXSTATUS", rs.getString("ONEXSTATUS"));
					poData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					poData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					poData.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
					poData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					poData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					poData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					poData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					poData.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					invNumber = rs.getString("INVOICENUMBER");
					bpid = rs.getString("BUSINESSPARTNEROID");
					invDate = rs.getString("INVOICEDATE");
					poData.put("MPO", rs.getString("MPO"));
					poData.put("ALLPO", rs.getString("ALLPO"));
					mPO = (rs.getString("MPO") == null) ? "-" : rs.getString("MPO");
					invoiceList.add(poData);
				}
				rs.close();
				pg.close();
				pg = null;
			} else {
				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				Pagination pg = null;
				String advqdata = "";
				if (!status.equalsIgnoreCase("ASSQ")) {
					if (!("NA").equalsIgnoreCase(vendor)) {
						String po = "AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND ps.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND ps.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " and ps.basepo = ia.ponumber ";
						basePoFlag = true;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND ps.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND ps.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					if (!("NA").equalsIgnoreCase(vendor)) {
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null " + subquery
							+ "  AND ps.MPO IS NULL " + "UNION "
							+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
							+ basePoQuery + "where ps.invoicenumber is not null " + subquery + " AND ps.MPO = 'Y' "
							+ " order by CREATEDON desc";

				} else if (status.equalsIgnoreCase("ASSQ")) {
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND ps.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND ps.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND ps.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}

					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null " + subquery + " AND  "
							+ "ps.CREDITADVICENO IS NOT NULL AND ps.MPO IS NULL " + "UNION "
							+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
							+ " and ps.ponumber=ia.ponumber where ps.invoicenumber is not null " + subquery
							+ " AND ps.MPO = 'Y' AND  " + "ps.CREDITADVICENO IS NOT NULL" + " order by CREATEDON desc";

				}

				log.info("advqdata : " + advqdata);
				pg = new Pagination(advqdata, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);
				String invNumber = null;
				String invDate = null;
				String mPO = null;
				String bpid = null;

				while (rs.next()) {
					HashMap<String, String> poData = new HashMap<String, String>();
					String poNum = rs.getString("PONUMBER");
					if (basePoFlag && rs.getString("BASEPO") != null) {
						poNum = rs.getString("BASEPO");
					}
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("PONUMBER", poNum);
					poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poData.put("MESSAGE", rs.getString("MESSAGE"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
					poData.put("BUYER", rs.getString("BUYER"));
					poData.put("AMOUNT", rs.getString("AMOUNT"));
					poData.put("MACOUNT", rs.getString("MACOUNT"));
					poData.put("HOLDCOUNT", rs.getString("HOLDCOUNT"));
					poData.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					poData.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
					poData.put("MATERIAL_TYPE", rs.getString("MATERIAL_TYPE"));
					poData.put("PGQ", rs.getString("PGQ"));
					poData.put("ONEXSTATUS", rs.getString("ONEXSTATUS"));
					poData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					poData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					poData.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
					poData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					poData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					poData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					poData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					poData.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					invNumber = rs.getString("INVOICENUMBER");
					bpid = rs.getString("BUSINESSPARTNEROID");
					invDate = rs.getString("INVOICEDATE");
					poData.put("MPO", rs.getString("MPO"));
					poData.put("ALLPO", rs.getString("ALLPO"));
					mPO = (rs.getString("MPO") == null) ? "-" : rs.getString("MPO");
					invoiceList.add(poData);
				}
				pg.close();
				rs.close();
				pg = null;

			} // end of else

			try {
				getInternalPonInvoiceSummeryCountsAsPerStatus("", nPage, status, invno, pono, fdate, tdate, plant,
						vendor, "payer", con, ps, rs);
			} catch (Exception e) {

			}

		} catch (SQLException e) {
			log.error("getPayerPoninvoiceSummery() :", e.fillInStackTrace());
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

	public JSONArray getBuyerPoninvoiceSummery(String emailid, int nPage, String status, String invno, String pono,
			String fdate, String tdate, String plant, String vendor) throws SQLException {

		String basePoQuery = " and ps.ponumber=ia.ponumber ";
		boolean basePoFlag = false;

		// For pending QUANTITY
		String pendingsql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps where  ps.BUYER = ? "
				+ "AND ps.invoicenumber is not null AND  ps.ALLPO IS NULL AND ps.MPO IS NULL "
				+ "AND (OVERALLSTATUS=? OR OVERALLSTATUS=?)  " + "UNION "
				+ "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where (ps.BUYER = ? or ia.enduseid = ?) "
				+ "AND ps.invoicenumber is not null AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' AND "
				+ " (OVERALLSTATUS=? OR OVERALLSTATUS=?) " + "order by CREATEDON desc";

		// For OFFLINE INVOICES
		String hdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO " + "from PONINVOICESUMMERY ps where"
				+ " ps.invoicenumber is not null AND ps.BUYER =? AND ONEXSTATUS=?  ps.ALLPO IS NULL AND ps.MPO IS NULL "
				+ " UNION " + "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO  "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where "
				+ "ps.invoicenumber is not null and (ps.BUYER = ? or ia.enduseid = ? )AND ONEXSTATUS=? "
				+ "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";

		// All Filter
		String sql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO " + "from PONINVOICESUMMERY ps where  "
				+ " ps.invoicenumber is not null AND ps.BUYER =? AND OVERALLSTATUS=? AND ps.ALLPO IS NULL AND ps.MPO IS NULL "
				+ "UNION " + "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND (ps.BUYER = ? or ia.enduseid = ? ) AND OVERALLSTATUS=? "
				+ "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";

		// For Short quantity
		String sdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and "
				+ "BUYER =? AND ps.CREDITADVICENO IS NOT NULL AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
				+ "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO   "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null and "
				+ " (ps.BUYER = ? or ia.enduseid = ?) AND ps.CREDITADVICENO IS NOT NULL AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' "
				+ "order by CREATEDON desc";

		// All Status
		String alldata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO " + "from PONINVOICESUMMERY ps where "
				+ "ps.invoicenumber is not null and ps.BUYER =? AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
				+ "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO   "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where"
				+ " ps.invoicenumber is not null and (ps.BUYER = ? or ia.enduseid = ?) AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' "
				+ "order by CREATEDON desc";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		int invoicewopopages = 0;
		int pages = 0;

		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> invoiceList1 = new ArrayList<HashMap<String, String>>();

		try {
			con = DBConnection.getConnection();

			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ"))) {

				ArrayList<String> param = new ArrayList<String>();
				param.add(emailid);
				Pagination pg = null;
				if (status.equalsIgnoreCase("H")) {
					param.add(status);
					param.add(emailid);
					param.add(status);
					log.info("hdata :" + hdata);
					pg = new Pagination(hdata, nPage);
				} else if (status.equalsIgnoreCase("ALL")) {
					param.add(emailid);
					log.info("alldata : " + alldata);
					pg = new Pagination(alldata, nPage);
				} else if (status.equalsIgnoreCase("C")) {
					param.add(emailid);
					log.info("sdata : " + sdata);
					pg = new Pagination(sdata, nPage);
				} else if (status.equalsIgnoreCase("P")) {
					param.add("P");
					param.add("M");
					param.add(emailid);
					param.add("P");
					param.add("M");
					param.add(emailid);
					param.add(emailid);
					log.info("pendingsql : " + pendingsql);
					pg = new Pagination(pendingsql, nPage);
				} else if (status.equalsIgnoreCase("V")) {
					param.add("V");
					param.add("RO");
					param.add(emailid);
					param.add("V");
					param.add("RO");
					param.add(emailid);
					param.add(emailid);
					log.info("pendingsql : " + pendingsql);
					pg = new Pagination(pendingsql, nPage);
				} else {
					param.add(status);
					param.add(emailid);
					param.add(status);
					log.info("sql : " + sql);
					pg = new Pagination(sql, nPage);

				}

				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				int count = 0;

				String invNumber = null;
				String invDate = null;
				String mPO = null;
				String bpid = null;
				while (rs.next()) {
					count++;
					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					invNumber = rs.getString("INVOICENUMBER");
					poData.put("PONUMBER", rs.getString("PONUMBER"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					bpid = rs.getString("BUSINESSPARTNEROID");
					poData.put("MESSAGE", rs.getString("MESSAGE"));
					poData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
					poData.put("BUYER", rs.getString("BUYER"));
					poData.put("AMOUNT", rs.getString("AMOUNT"));
					poData.put("MACOUNT", rs.getString("MACOUNT"));
					poData.put("HOLDCOUNT", rs.getString("HOLDCOUNT"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poData.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					invDate = rs.getString("INVOICEDATE");
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
					poData.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					poData.put("MPO", rs.getString("MPO"));
					poData.put("ALLPO", rs.getString("ALLPO"));
					mPO = (rs.getString("MPO") == null) ? "-" : rs.getString("MPO");
					invoiceList.add(poData);
				}
				rs.close();
				pg.close();
				pg = null;
			} else {
				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				param.add(emailid);
				Pagination pg = null;

				String advqdata = "";
				if (!status.equalsIgnoreCase("ASSQ")) {
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND ps.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND ps.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " and ps.basepo=ia.ponumber ";
						basePoFlag = true;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND ps.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND ps.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and BUYER =? " + subquery
							+ " " + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
							+ "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
							+ basePoQuery + "  where ps.invoicenumber is not null and BUYER =? " + subquery + " "
							+ "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";
				} else if (status.equalsIgnoreCase("ASSQ")) {
					if (!vendor.equalsIgnoreCase("NA")) {

						String po = "AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND ps.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND ps.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND ps.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailid);
					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and BUYER =? " + subquery
							+ " " + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
							+ "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
							+ " and ps.ponumber=ia.ponumber where ps.invoicenumber is not null and BUYER =? " + subquery
							+ " " + "AND ps.ALLPO IS NOT NULL " + "AND ps.MPO ='Y' " + "order by CREATEDON desc";

				}

				log.info("advqdata : " + advqdata);
				pg = new Pagination(advqdata, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				String invNumber = null;
				String invDate = null;
				String mPO = null;
				String bpid = null;

				while (rs.next()) {
					HashMap<String, String> poData = new HashMap<String, String>();
					String poNum = rs.getString("PONUMBER");
					if (basePoFlag && rs.getString("BASEPO") != null) {
						poNum = rs.getString("BASEPO");
					}

					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("PONUMBER", poNum);
					poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poData.put("MESSAGE", rs.getString("MESSAGE"));
					poData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poData.put("BUYER", rs.getString("BUYER"));
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
					poData.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");

					invNumber = rs.getString("INVOICENUMBER");
					bpid = rs.getString("BUSINESSPARTNEROID");
					invDate = rs.getString("INVOICEDATE");

					poData.put("MPO", rs.getString("MPO"));
					poData.put("ALLPO", rs.getString("ALLPO"));
					mPO = (rs.getString("MPO") == null) ? "-" : rs.getString("MPO");
					invoiceList.add(poData);
				}
				pg.close();
				rs.close();
				pg = null;
			} // end of else

			// try {
			getInternalPonInvoiceSummeryCountsAsPerStatus(emailid, nPage, status, invno, pono, fdate, tdate, plant,
					vendor, "buyer", con, ps, rs);
			// } catch (Exception e) {

			// }

		} catch (SQLException e) {
			log.error("getBuyerPoninvoiceSummery() :", e.fillInStackTrace());
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

	public JSONArray getRequsitionerPoninvoiceSummery(String emailid, int nPage, String status, String invno,
			String pono, String fdate, String tdate, String plant, String vendor) throws SQLException {

		String subQuery = "";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		try {
				con = DBConnection.getConnection();
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

				log.info("mainQuery : "+ mainQuery);
				
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
				getInternalPonInvoiceSummeryCountsAsPerStatus(emailid, nPage, status, invno, pono, fdate, tdate, plant,
						vendor, "enduser", con, ps, rs);
			} catch (Exception e) {
				log.error("getRequsitionerPoninvoiceSummery() 1 :", e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("getRequsitionerPoninvoiceSummery() 2 :", e.fillInStackTrace());
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

	public JSONArray getInternalPonInvoiceSummeryCountsAsPerStatus(String emailid, int nPage, String status,
			String invno, String pono, String fdate, String tdate, String plant, String vendor, String mode,
			Connection con, PreparedStatement ps, ResultSet rs) throws SQLException {

		try {

			HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
			int allCounter = 0;
			String joinQuery = " join invoiceapproval ia ON pis.invoicenumber = ia.invoicenumber and pis.invoicedate = ia.invoicedate and pis.ponumber=ia.ponumber ";

			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ")) && (!status.equalsIgnoreCase("Invalid Invoices"))
					&& (!status.equalsIgnoreCase("INV"))) {

				String invoice_data = "";

				if (mode.equalsIgnoreCase("buyer")) {
					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND  PIS.buyer = ? and pis.overallstatus is not null and pis.mpo is null "
							+ "Group by pis.overallstatus " + "union all "
							+ "SELECT pis.overallstatus ,count(Distinct pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis "
							+ joinQuery + " WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND  PIS.buyer = ? and pis.overallstatus is not null and pis.mpo ='Y' "
							+ "Group by pis.overallstatus";

				} else if (mode.equalsIgnoreCase("enduser")) {

					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND  PIS.REQUSITIONER = ? and pis.overallstatus is not null and pis.mpo is null "
							+ "Group by pis.overallstatus " + "union all "
							+ "SELECT pis.overallstatus ,count(Distinct pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis "
							+ joinQuery + " WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND  PIS.REQUSITIONER = ? and pis.overallstatus is not null and pis.mpo ='Y' "
							+ "Group by pis.overallstatus";

				} else if (mode.equalsIgnoreCase("payer")) {

					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL and pis.overallstatus is not null and pis.mpo is null "
							+ "Group by pis.overallstatus " + "union all "
							+ "SELECT pis.overallstatus ,count(Distinct pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid) as count FROM PONINVOICESUMMERY pis "
							+ joinQuery + " WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL and pis.overallstatus is not null and pis.mpo ='Y' "
							+ "Group by pis.overallstatus";
				}

				ps = con.prepareStatement(invoice_data);
				if (!mode.equalsIgnoreCase("payer")) {
					ps.setString(1, emailid);
					ps.setString(2, emailid);
				}

				rs = ps.executeQuery();
				while (rs.next()) {
					String sts = rs.getString("overallstatus");
					String count = rs.getString("count");
					if (countAsPerStatus.get(sts) == null) {
						countAsPerStatus.put(sts, count);
					} else {
						count = String.valueOf(Integer.parseInt(countAsPerStatus.get(sts)) + Integer.parseInt(count));
						countAsPerStatus.put(sts, count);
					}
					allCounter += Integer.parseInt(count);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				rs.close();
				ps.close();

			} else {
				String subquery = "";
				String basePoQuery = " and pis.ponumber=ia.ponumber ";
				if ((!status.equalsIgnoreCase("ASWP")) && (!status.equalsIgnoreCase("ASSQ"))
						&& (!status.equalsIgnoreCase("Invalid Invoices")) && (!status.equalsIgnoreCase("INV"))) {
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND PIS.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
					}
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PLANT=?";
						subquery = subquery + po;
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " and pis.basepo = ia.ponumber ";
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
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND PIS.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " and pis.basepo = ia.ponumber ";
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
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND PIS.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
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
				} else if (status.equalsIgnoreCase("Invalid Invoices") || status.equalsIgnoreCase("INV")) {

					String in = " AND PIS.overallstatus = ? ";
					subquery = subquery + in;
				}

				String invoice_data = "";

				if (mode.equalsIgnoreCase("buyer")) {

					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND  PIS.buyer = ? and pis.overallstatus is not null and "
							+ " pis.mpo is null " + subquery + "Group by pis.overallstatus " + "union all "
							+ "SELECT pis.overallstatus ,count(Distinct pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid) as count FROM PONINVOICESUMMERY pis "
							+ joinQuery + " WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND  PIS.buyer = ? and pis.overallstatus is not null and "
							+ " pis.mpo ='Y' " + subquery + "Group by pis.overallstatus";
				} else if (mode.equalsIgnoreCase("enduser")) {

					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND  PIS.REQUSITIONER = ? "
							+ " and pis.overallstatus is not null and pis.mpo is null " + subquery
							+ " Group by pis.overallstatus " + "union all "
							+ " SELECT pis.overallstatus ,count(Distinct pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid) as count FROM PONINVOICESUMMERY pis "
							+ joinQuery + " WHERE " + " PIS.INVOICENUMBER IS NOT NULL AND  PIS.REQUSITIONER = ? "
							+ " and pis.overallstatus is not null and  pis.mpo ='Y' " + subquery
							+ " Group by pis.overallstatus";
				} else if (mode.equalsIgnoreCase("payer")) {

					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL " + subquery
							+ " and pis.overallstatus is not null and  pis.mpo is null " + "Group by pis.overallstatus "
							+ "union all "
							+ "SELECT pis.overallstatus ,count(Distinct pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid) as count FROM PONINVOICESUMMERY pis "
							+ joinQuery + " WHERE " + "PIS.INVOICENUMBER IS NOT NULL  " + subquery
							+ "  and pis.overallstatus is not null and  pis.mpo ='Y' " + "Group by pis.overallstatus";
				} else {
					if (status.equalsIgnoreCase("Invalid Invoices")) {
						responsejson.put("Message", "Not a valid user.");
						jsonArray.add(responsejson);
						return jsonArray;
					}
					return null;
				}

				ps = con.prepareStatement(invoice_data);

//				log.info("INVOICE DATA Q : "+invoice_data);

				int queryCounter = 0;
				if (!mode.equalsIgnoreCase("payer")) {
					queryCounter++;
					ps.setString(queryCounter, emailid);
				}

				if ((!status.equalsIgnoreCase("ASWP")) && (!status.equalsIgnoreCase("ASSQ"))
						&& (!status.equalsIgnoreCase("Invalid Invoices")) && (!status.equalsIgnoreCase("INV"))) {
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
					}
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
					if (!mode.equalsIgnoreCase("payer")) {
						queryCounter++;
						ps.setString(queryCounter, emailid);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
					}
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

					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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
					if (!mode.equalsIgnoreCase("payer")) {
						queryCounter++;
						ps.setString(queryCounter, emailid);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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
				} else if (status.equalsIgnoreCase("ASSQ")) {

					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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
					if (!mode.equalsIgnoreCase("payer")) {
						queryCounter++;
						ps.setString(queryCounter, emailid);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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
				} else if (status.equalsIgnoreCase("Invalid Invoices") || status.equalsIgnoreCase("INV")) {

					queryCounter++;
					ps.setString(queryCounter, "INV");

					if (!mode.equalsIgnoreCase("payer")) {
						queryCounter++;
						ps.setString(queryCounter, emailid);
					}

					queryCounter++;
					ps.setString(queryCounter, "INV");

				}

				rs = ps.executeQuery();
				while (rs.next()) {

					String sts = rs.getString("overallstatus");
					String count = rs.getString("count");

					if (countAsPerStatus.get(sts) == null) {
						countAsPerStatus.put(sts, count);
					} else {
						count = String.valueOf(Integer.parseInt(countAsPerStatus.get(sts)) + Integer.parseInt(count));
						countAsPerStatus.put(sts, count);
					}
					allCounter += Integer.parseInt(count);
				}

				if ((status.equalsIgnoreCase("Invalid Invoices") || status.equalsIgnoreCase("INV"))
						&& countAsPerStatus.isEmpty()) {
					log.info("No Invalid Invoices Found!!");
					countAsPerStatus.put("INV", "0");
				}

				if (!status.equalsIgnoreCase("Invalid Invoices")) {
					countAsPerStatus.put("ALL", allCounter + "");
				}

				rs.close();
				ps.close();
			}

			if (!status.equalsIgnoreCase("Invalid Invoices") && (!status.equalsIgnoreCase("INV"))) {

				String invoice_data = "select count(*) as count from poninvoicesummery where onexstatus = ? "
						+ "AND INVOICENUMBER IS NOT NULL and overallstatus is not null ";

				if (mode.equalsIgnoreCase("buyer")) {
					invoice_data = "select count(*) as count from poninvoicesummery where onexstatus = ? AND BUYER = ? "
							+ "AND INVOICENUMBER IS NOT NULL and overallstatus is not null ";

				} else if (mode.equalsIgnoreCase("enduser")) {
					invoice_data = "select count(*) as count from poninvoicesummery where onexstatus = ? AND REQUSITIONER = ? "
							+ "AND INVOICENUMBER IS NOT NULL and overallstatus is not null ";

				}

				ps = con.prepareStatement(invoice_data);
				if (mode.equalsIgnoreCase("payer")) {
					ps.setString(1, "H");
				} else {
					ps.setString(1, "H");
					ps.setString(2, emailid);
				}

				rs = ps.executeQuery();

				while (rs.next()) {
					String count = rs.getString("count");
					countAsPerStatus.put("H", count);
				}
				rs.close();
				ps.close();

			}

			if (!countAsPerStatus.isEmpty()) {
				if (status.equalsIgnoreCase("Invalid Invoices")) {
					responsejson.put("invalidInvoiceCount", countAsPerStatus);
					jsonArray.add(responsejson);
				} else {
					responsejson.put("invoiceCountAsPerStatus", countAsPerStatus);
				}
			}

		} catch (Exception e) {
			log.error("getInternalPonInvoiceSummeryCountsAsPerStatus() :", e.fillInStackTrace());
		}

		if (status.equalsIgnoreCase("Invalid Invoices")) {
			return jsonArray;
		}

		return null;
	}

	/*
	 * commented for advanced search with filter date 19-04-2024 public JSONArray
	 * getInvoiceDetailsCountAsPerStatus(String bid, int nPage, String status,
	 * String invno, String pono, String fdate, String tdate, String plant,
	 * Connection con, PreparedStatement ps, ResultSet rs, String companyCode) {
	 * 
	 * log.info("Inside getInvoiceDetailsCountAsPerStatus method " + status +
	 * " bid - " + bid);
	 * 
	 * try {
	 * 
	 * HashMap<String, String> countAsPerStatus = new HashMap<String, String>(); int
	 * allCounter = 0;
	 * 
	 * String joinQuery =
	 * " join invoiceapproval ia ON pis.invoicenumber = ia.invoicenumber and pis.invoicedate = ia.invoicedate and pis.ponumber=ia.ponumber "
	 * ;
	 * 
	 * String compCodeJoinQuery =
	 * " join podetails pod on pis.ponumber = pod.ponumber "; String compCodeQuery =
	 * " AND pod.companycode = ? ";
	 * 
	 * if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP")) &&
	 * (!status.equalsIgnoreCase("ASSQ")) &&
	 * (!status.equalsIgnoreCase("Invalid Invoices")) &&
	 * (!status.equalsIgnoreCase("INV"))) {
	 * 
	 * String invoice_data =
	 * "SELECT  pis.overallstatus,count(pis.invoicenumber) as count FROM PONINVOICESUMMERY pis "
	 * + compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND PIS.INVOICENUMBER IS NOT NULL and pis.overallstatus is not null and pis.mpo is null "
	 * + compCodeQuery + " Group by pis.overallstatus " + "union all " +
	 * "SELECT pis.overallstatus,count(distinct pis.invoicenumber) as count  FROM PONINVOICESUMMERY pis "
	 * + joinQuery + compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND PIS.INVOICENUMBER IS NOT NULL and pis.overallstatus is not null and pis.mpo ='Y'"
	 * + compCodeQuery + " Group by pis.overallstatus";
	 * 
	 * log.info("counts invoice_data --:" + invoice_data); ps =
	 * con.prepareStatement(invoice_data); ps.setString(1, bid); ps.setString(2,
	 * companyCode);
	 * 
	 * ps.setString(3, bid); ps.setString(4, companyCode);
	 * 
	 * rs = ps.executeQuery(); while (rs.next()) { String sts =
	 * rs.getString("overallstatus"); String count = rs.getString("count"); if
	 * (countAsPerStatus.get(sts) == null) { countAsPerStatus.put(sts, count); }
	 * else { count = String.valueOf(Integer.parseInt(countAsPerStatus.get(sts)) +
	 * Integer.parseInt(count)); countAsPerStatus.put(sts, count); } allCounter +=
	 * Integer.parseInt(count); } countAsPerStatus.put("ALL", allCounter + "");
	 * rs.close(); ps.close();
	 * 
	 * } else { String subquery = ""; if ((!status.equalsIgnoreCase("ASWP")) &&
	 * (!status.equalsIgnoreCase("ASSQ")) &&
	 * (!status.equalsIgnoreCase("Invalid Invoices")) &&
	 * (!status.equalsIgnoreCase("INV"))) { if (!plant.equalsIgnoreCase("NA")) {
	 * String po = " AND PIS.PLANT=?"; subquery = subquery + po; } if
	 * (!pono.equalsIgnoreCase("NA")) { String po = " AND PIS.PONUMBER=?"; subquery
	 * = subquery + po; } if (!invno.equalsIgnoreCase("NA")) { String in =
	 * " AND PIS.INVOICENUMBER=?"; subquery = subquery + in; } if
	 * ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { String dt =
	 * " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " +
	 * "AND TO_DATE(?, 'DD/MM/YYYY')"; subquery = subquery + dt; } } else if
	 * (status.equalsIgnoreCase("ASWP")) {
	 * 
	 * if (!pono.equalsIgnoreCase("NA")) { String po = " AND PIS.PONUMBER=?";
	 * subquery = subquery + po; } if (!invno.equalsIgnoreCase("NA")) { String in =
	 * " AND PIS.INVOICENUMBER=?"; subquery = subquery + in; } if
	 * ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { String dt =
	 * " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " +
	 * "AND TO_DATE(?, 'DD/MM/YYYY')"; subquery = subquery + dt; } } else if
	 * (status.equalsIgnoreCase("ASSQ")) {
	 * 
	 * if (!invno.equalsIgnoreCase("NA")) { String in = " AND PIS.INVOICENUMBER=?";
	 * subquery = subquery + in; } if ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { String dt =
	 * " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " +
	 * "AND TO_DATE(?, 'DD/MM/YYYY')"; subquery = subquery + dt; } } else if
	 * (status.equalsIgnoreCase("Invalid Invoices") ||
	 * status.equalsIgnoreCase("INV")) { String in = " AND PIS.overallstatus = ? ";
	 * subquery = subquery + in; }
	 * 
	 * String invoice_data =
	 * "SELECT  pis.overallstatus,count(pis.invoicenumber) as count   FROM PONINVOICESUMMERY pis "
	 * + compCodeJoinQuery + "  WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND PIS.INVOICENUMBER IS NOT NULL and pis.overallstatus is not null and pis.mpo is null "
	 * + " " + subquery + compCodeQuery + " Group by pis.overallstatus " +
	 * "union all " +
	 * "SELECT pis.overallstatus,count(distinct pis.invoicenumber) as count  FROM PONINVOICESUMMERY pis "
	 * + joinQuery + compCodeJoinQuery + " WHERE " +
	 * "PIS.BUSINESSPARTNEROID = ? AND PIS.INVOICENUMBER IS NOT NULL and pis.overallstatus is not null and pis.mpo ='Y'"
	 * + " " + subquery + compCodeQuery + " Group by pis.overallstatus";
	 * 
	 * log.info("counts invoice_data -- :  " + invoice_data);
	 * 
	 * ps = con.prepareStatement(invoice_data); ps.setString(1, bid); int
	 * queryCounter = 1; if ((!status.equalsIgnoreCase("ASWP")) &&
	 * (!status.equalsIgnoreCase("ASSQ")) &&
	 * (!status.equalsIgnoreCase("Invalid Invoices")) &&
	 * (!status.equalsIgnoreCase("INV"))) {
	 * 
	 * if (!plant.equalsIgnoreCase("NA")) { queryCounter++;
	 * ps.setString(queryCounter, plant); } if (!pono.equalsIgnoreCase("NA")) {
	 * queryCounter++; ps.setString(queryCounter, pono); } if
	 * (!invno.equalsIgnoreCase("NA")) { queryCounter++; ps.setString(queryCounter,
	 * invno); } if ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { queryCounter++;
	 * ps.setString(queryCounter, fdate); queryCounter++; ps.setString(queryCounter,
	 * tdate); } queryCounter++; ps.setString(queryCounter, companyCode);
	 * 
	 * queryCounter++; ps.setString(queryCounter, bid); if
	 * (!plant.equalsIgnoreCase("NA")) { queryCounter++; ps.setString(queryCounter,
	 * plant); } if (!pono.equalsIgnoreCase("NA")) { queryCounter++;
	 * ps.setString(queryCounter, pono); } if (!invno.equalsIgnoreCase("NA")) {
	 * queryCounter++; ps.setString(queryCounter, invno); } if
	 * ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { queryCounter++;
	 * ps.setString(queryCounter, fdate); queryCounter++; ps.setString(queryCounter,
	 * tdate); } queryCounter++; ps.setString(queryCounter, companyCode);
	 * 
	 * } else if (status.equalsIgnoreCase("ASWP")) {
	 * 
	 * if (!pono.equalsIgnoreCase("NA")) { queryCounter++;
	 * ps.setString(queryCounter, pono); } if (!invno.equalsIgnoreCase("NA")) {
	 * queryCounter++; ps.setString(queryCounter, invno); } if
	 * ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { queryCounter++;
	 * ps.setString(queryCounter, fdate); queryCounter++; ps.setString(queryCounter,
	 * tdate); } queryCounter++; ps.setString(queryCounter, companyCode);
	 * 
	 * queryCounter++; ps.setString(queryCounter, bid); if
	 * (!pono.equalsIgnoreCase("NA")) { queryCounter++; ps.setString(queryCounter,
	 * pono); } if (!invno.equalsIgnoreCase("NA")) { queryCounter++;
	 * ps.setString(queryCounter, invno); } if ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { queryCounter++;
	 * ps.setString(queryCounter, fdate); queryCounter++; ps.setString(queryCounter,
	 * tdate); } queryCounter++; ps.setString(queryCounter, companyCode);
	 * 
	 * } else if (status.equalsIgnoreCase("ASSQ")) {
	 * 
	 * if (!invno.equalsIgnoreCase("NA")) { queryCounter++;
	 * ps.setString(queryCounter, invno); } if ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { queryCounter++;
	 * ps.setString(queryCounter, fdate); queryCounter++; ps.setString(queryCounter,
	 * tdate); } queryCounter++; ps.setString(queryCounter, companyCode);
	 * 
	 * queryCounter++; ps.setString(queryCounter, bid); if
	 * (!invno.equalsIgnoreCase("NA")) { queryCounter++; ps.setString(queryCounter,
	 * invno); } if ((!fdate.equalsIgnoreCase("NA")) &&
	 * (!fdate.equalsIgnoreCase("Invalid date"))) { queryCounter++;
	 * ps.setString(queryCounter, fdate); queryCounter++; ps.setString(queryCounter,
	 * tdate); } queryCounter++; ps.setString(queryCounter, companyCode); }
	 * 
	 * else if (status.equalsIgnoreCase("Invalid Invoices") ||
	 * status.equalsIgnoreCase("INV")) { queryCounter++; ps.setString(queryCounter,
	 * "INV");
	 * 
	 * queryCounter++; ps.setString(queryCounter, companyCode);
	 * 
	 * queryCounter++; ps.setString(queryCounter, bid);
	 * 
	 * queryCounter++; ps.setString(queryCounter, "INV");
	 * 
	 * queryCounter++; ps.setString(queryCounter, companyCode); }
	 * 
	 * rs = ps.executeQuery(); while (rs.next()) { String sts =
	 * rs.getString("overallstatus"); String count = rs.getString("count"); if
	 * (countAsPerStatus.get(sts) == null) { countAsPerStatus.put(sts, count); }
	 * else { count = String.valueOf(Integer.parseInt(countAsPerStatus.get(sts)) +
	 * Integer.parseInt(count)); countAsPerStatus.put(sts, count); } allCounter +=
	 * Integer.parseInt(count); }
	 * 
	 * if ((status.equalsIgnoreCase("Invalid Invoices") ||
	 * status.equalsIgnoreCase("INV")) && countAsPerStatus.isEmpty()) {
	 * log.info("No Invalid Invoices Found!!"); countAsPerStatus.put("INV", "0"); }
	 * 
	 * if (!status.equalsIgnoreCase("Invalid Invoices")) {
	 * countAsPerStatus.put("ALL", allCounter + ""); }
	 * 
	 * rs.close(); ps.close();
	 * 
	 * }
	 * 
	 * if (!status.equalsIgnoreCase("Invalid Invoices") &&
	 * (!status.equalsIgnoreCase("INV"))) {
	 * 
	 * String invoice_data = "select count(*) as count from poninvoicesummery pis "
	 * + compCodeJoinQuery +
	 * " where pis.onexstatus = 'H' AND pis.BUSINESSPARTNEROID = ? " +
	 * "AND pis.INVOICENUMBER IS NOT NULL and pis.overallstatus is not null " +
	 * compCodeQuery;
	 * 
	 * log.info("counters invoice_data :" + invoice_data); ps =
	 * con.prepareStatement(invoice_data); ps.setString(1, bid); ps.setString(2,
	 * companyCode);
	 * 
	 * rs = ps.executeQuery(); while (rs.next()) { String count =
	 * rs.getString("count"); countAsPerStatus.put("H", count); } rs.close();
	 * ps.close(); }
	 * 
	 * if (!countAsPerStatus.isEmpty()) {
	 * 
	 * if (status.equalsIgnoreCase("Invalid Invoices")) {
	 * responsejson.put("invalidInvoiceCount", countAsPerStatus);
	 * jsonArray.add(responsejson); } else {
	 * responsejson.put("invoiceCountAsPerStatus", countAsPerStatus); } } } catch
	 * (Exception e) { log.error("getInvoiceDetailsCountAsPerStatus() :",
	 * e.fillInStackTrace()); }
	 * 
	 * if (status.equalsIgnoreCase("Invalid Invoices")) { return jsonArray; } return
	 * null; }
	 */
	public JSONArray getinvoicebasedonemailidCountAsPerStatus(String emailId, HttpSession session, int nPage,
			String status, String pono, String invno, String fdate, String tdate, String plant, String vendor,
			Connection con, PreparedStatement ps, ResultSet rs) throws SQLException {

		String storeKepeer = (String) session.getAttribute("shopkepeer")==null?"false":(String) session.getAttribute("shopkepeer");

		try {

			HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
			int allCounter = 0;
			List<HashMap<String, String>> hashlist = new ArrayList<HashMap<String, String>>();
			if (!status.equalsIgnoreCase("AS")) {

				String invoice_data = "";

				if (storeKepeer.equalsIgnoreCase("true")) {
					invoice_data = "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber   AND A.ENDUSEID = ? "
							+ "AND A.PROXY IS NULL Group by B.overallstatus " + "union all "
							+ "SELECT  B1.OVERALLSTATUS, count(DISTINCT B1.INVOICENUMBER || '-' || B1.invoicedate||'-'||B1.businesspartneroid) as count FROM INVOICEAPPROVAL A1 "
							+ "JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' "
							+ "AND B1.GRNNUMBER IS NOT NULL Group by B1.overallstatus " + "union all "
							+ "SELECT  B1.OVERALLSTATUS, count(DISTINCT B1.INVOICENUMBER || '-' || B1.invoicedate||'-'||B1.businesspartneroid) as count FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' Group by B1.overallstatus "
							+ "union all "
							+ "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  "
							+ "ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X'  JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "Group by B.overallstatus";

					if ("C".equalsIgnoreCase(status)) {
						invoice_data = "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ "A.InvoiceNumber=B.InvoiceNumber AND " + "A.PONumber=B.PONumber   AND A.ENDUSEID = ? "
								+ "AND A.PROXY IS NULL AND CREDITADVICENO IS NOT NULL and B.overallstatus is not null "
								+ "and B.invoicenumber is not null " + "Group by B.overallstatus " + "union all "
								+ "SELECT B1.OVERALLSTATUS,count(DISTINCT B1.INVOICENUMBER || '-' || B1.invoicedate||'-'||B1.businesspartneroid) as count "
								+ "FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) "
								+ "AND A1.STATUS NOT LIKE 'C%' And B1.invoicenumber is not null "
								+ "AND B1.GRNNUMBER IS NOT NULL AND B1.overallstatus is not null and "
								+ "CREDITADVICENO IS NOT NULL Group by B1.overallstatus " + "union all  "
								+ "SELECT B1.OVERALLSTATUS,count(DISTINCT B1.INVOICENUMBER || '-' || B1.invoicedate||'-'||B1.businesspartneroid) as count "
								+ "FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  "
								+ "And B1.invoicenumber is not null AND B1.overallstatus is not null AND "
								+ "CREDITADVICENO IS NOT NULL Group by B1.overallstatus " + "union all "
								+ " SELECT B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL A "
								+ "JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' And B.invoicenumber is not null AND B.overallstatus is not null "
								+ "AND  CREDITADVICENO IS NOT NULL JOIN "
								+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "Group by B.overallstatus";
					}
				} else {
					invoice_data = "SELECT B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count "
							+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
							+ "AND B.overallstatus is not null and B.invoicenumber is not null "
							+ "Group by B.overallstatus " + "union all  "
							+ "SELECT  B1.OVERALLSTATUS,count(DISTINCT B1.INVOICENUMBER || '-' || B1.invoicedate||'-'||B1.businesspartneroid) as count FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL) "
							// + "(A1.STATUS NOT LIKE 'C%' ) "
							+ "AND B1.overallstatus is not null and B1.invoicenumber is not null "
							+ "Group by B1.overallstatus " + "union all  "
							+ "SELECT  B1.OVERALLSTATUS,count(DISTINCT B1.INVOICENUMBER || '-' || B1.invoicedate||'-'||B1.businesspartneroid) as count FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
							+ "B1.overallstatus is not null and B1.invoicenumber is not null AND "
							+ "(A1.STATUS LIKE 'C%' ) Group by B1.overallstatus";

					if ("C".equalsIgnoreCase(status)) {
						invoice_data = "SELECT  B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count  "
								+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
								+ "AND B.INVOICENUMBER IS NOT NULL AND B.CREDITADVICENO IS NOT NULL and B.overallstatus is not null "
								+ "AND A.PROXY IS NULL Group by B.overallstatus  " + "union all  "
								+ "SELECT  B1.OVERALLSTATUS,count(DISTINCT B1.INVOICENUMBER || '-' || B1.invoicedate||'-'||B1.businesspartneroid) as count "
								+ "FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  "
								+ "ON  A1.InvoiceNumber=B1.InvoiceNumber AND B1.INVOICENUMBER IS NOT NULL "
								+ "and B1.overallstatus is not null "
								+ "AND A1.PONumber=B1.PONumber  AND A1.EUMANAGER = ?   AND "
								+ "(B1.CREDITADVICENO IS NOT NULL AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL) "
								+ "Group by B1.overallstatus " + "union all  "
								+ "SELECT  B1.OVERALLSTATUS,count(DISTINCT B1.INVOICENUMBER || '-' || B1.invoicedate||'-'||B1.businesspartneroid) as count "
								+ "FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND A1.EUMANAGER = ? AND B1.INVOICENUMBER IS NOT NULL  "
								+ "and B1.overallstatus is not null AND "
								+ "(B1.CREDITADVICENO IS NOT NULL  AND A1.STATUS LIKE 'C%') "
								+ "Group by B1.overallstatus ";
					}
				}
				log.info("invoice_data : " + invoice_data);

				ps = con.prepareStatement(invoice_data);
				if (!storeKepeer.equalsIgnoreCase("true")) {
					ps.setString(1, emailId);
					ps.setString(2, emailId);
					ps.setString(3, emailId);
				} else {
					ps.setString(1, emailId);
					ps.setString(2, emailId);
					ps.setString(3, emailId);
					ps.setString(4, emailId);
				}

				rs = ps.executeQuery();
				while (rs.next()) {
					HashMap<String, String> valmap = new HashMap<String, String>();
					valmap.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					String sts = rs.getString("overallstatus");
					String count = rs.getString("count");
					if (countAsPerStatus.isEmpty()) {
						countAsPerStatus.put(sts, count);
					} else {
						if (countAsPerStatus.containsKey(sts)) {
							int sum = Integer.parseInt(countAsPerStatus.get(sts)) + Integer.parseInt(count);
							countAsPerStatus.put(sts, sum + "");
						} else {
							countAsPerStatus.put(sts, count);
						}
					}

					allCounter += Integer.parseInt(count);
					hashlist.add(valmap);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				rs.close();
				ps.close();

			} else {
				String basePoQuery = " A.PONumber = B.PONUMBER ";
				String basePoQuery1 = " A1.PONumber = B.PONUMBER ";
				String subquery = "";
				if (!plant.equalsIgnoreCase("NA")) {
					String po = " AND B.PLANT=?";
					subquery = subquery + po;
				}
				if (!vendor.equalsIgnoreCase("NA")) {
					String po = " AND B.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
					subquery = subquery + po;
				}
				if (!pono.equalsIgnoreCase("NA")) {
					String po = " AND B.PONUMBER=?";
					subquery = subquery + po;
					basePoQuery = " A.PONumber = B.PONUMBER ";
					basePoQuery1 = " A1.PONumber = B.PONUMBER ";
				}
				if (!invno.equalsIgnoreCase("NA")) {
					String in = " AND B.INVOICENUMBER=?";
					subquery = subquery + in;
				}
				if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
					String dt = " AND B.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + "AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
				}

				String invoice_data = "";

				if (storeKepeer.equalsIgnoreCase("true")) {
					invoice_data = "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery + "   AND A.ENDUSEID = ? "
							+ "AND A.PROXY IS NULL " + subquery + " Group by B.overallstatus " + "union all  "
							+ "SELECT  B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL A1 "
							+ "JOIN PONINVOICESUMMERY B ON  A1.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery1
							+ " AND (A1.EUMANAGER = ?) " + subquery + " AND A1.STATUS NOT LIKE 'C%' "
							+ "AND B.GRNNUMBER IS NOT NULL Group by B.overallstatus " + "union all  "
							+ "SELECT  B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
							+ basePoQuery1 + " AND (A1.EUMANAGER = ?) " + subquery
							+ " AND A1.STATUS LIKE 'C%' Group by B.overallstatus " + "union all  "
							+ "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  "
							+ "ON  A.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery + " AND A.PROXY = 'X'  JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT " + subquery
							+ " AND USERID=? " + "Group by B.overallstatus";

				} else {

					invoice_data = "SELECT B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count "
							+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
							+ "A.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery + "  AND A.ENDUSEID = ?  "
							+ "AND B.overallstatus is not null and B.invoicenumber is not null " + subquery + " "
							+ "Group by B.overallstatus " + "union all  "
							+ "SELECT  B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery1
							+ " AND (A1.EUMANAGER = ? )  AND "
							+ "(A1.STATUS NOT LIKE 'C%' AND B.GRNNUMBER IS NOT NULL) "
							// + "(A1.STATUS NOT LIKE 'C%' ) "
							+ "AND B.overallstatus is not null and B.invoicenumber is not null " + subquery + " "
							+ "Group by B.overallstatus " + "union all  "
							+ "SELECT  B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER || '-' || B.invoicedate||'-'||B.businesspartneroid) as count FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery1
							+ "  AND (A1.EUMANAGER = ? ) AND "
							+ "B.overallstatus is not null and B.invoicenumber is not null " + subquery + " AND"
							+ "(A1.STATUS LIKE 'C%' ) Group by B.overallstatus ";
				}

				log.info("invoice_data : " + invoice_data);
				ps = con.prepareStatement(invoice_data);
				int queryCounter = 0;
				if (!storeKepeer.equalsIgnoreCase("true")) {

					queryCounter++;
					ps.setString(queryCounter, emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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
					queryCounter++;
					ps.setString(queryCounter, emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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
					queryCounter++;
					ps.setString(queryCounter, emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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

				} else {
					queryCounter++;
					ps.setString(queryCounter, emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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

					queryCounter++;
					ps.setString(queryCounter, emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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

					queryCounter++;
					ps.setString(queryCounter, emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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
					queryCounter++;
					ps.setString(queryCounter, emailId);

					if (!plant.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, vendor);
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
				}
				rs = ps.executeQuery();

				while (rs.next()) {
					String sts = rs.getString("overallstatus");
					String count = rs.getString("count");
					HashMap<String, String> valmap = new HashMap<String, String>();
					valmap.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					if (countAsPerStatus.isEmpty()) {
						countAsPerStatus.put(sts, count);
					} else {
						if (countAsPerStatus.containsKey(sts)) {
							int sum = Integer.parseInt(countAsPerStatus.get(sts)) + Integer.parseInt(count);
							countAsPerStatus.put(sts, sum + "");
						} else {
							countAsPerStatus.put(sts, count);
						}
					}
					allCounter += Integer.parseInt(count);
					hashlist.add(valmap);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				rs.close();
				ps.close();

			}
			if (!countAsPerStatus.isEmpty()) {
				responsejson.put("invoiceCountAsPerStatus", countAsPerStatus);
			}
		} catch (Exception e) {
			log.error("getinvoicebasedonemailidCountAsPerStatus() :", e.fillInStackTrace());
		} finally {
			// DBConnection.closeConnection(rs, ps, con);
		}
		return null;
	}

	public JSONArray updateacceptedquantity(List<String> values, String email) {

		String[] billDatearr = values.toArray(new String[values.size()]);
		int value = 0;
		String grn = "";
		HashMap<String, String> poGrnMap = new HashMap<String, String>();
		ArrayList<HashMap<String, String>> poGrnList = new ArrayList<HashMap<String, String>>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String enduser = "";
			String invoice = "";
			String po = "";
			String status = "";

			for (int k = 0; k < values.size(); k++) {

				String[] temp = billDatearr[k].split(",");
				String ponumber = temp[0];
				poGrnMap.put(ponumber, "-");
			}

			Set<String> keys = poGrnMap.keySet();
			for (String key : keys) {

				StringBuilder generatedToken = new StringBuilder();
				try {
					SecureRandom number = SecureRandom.getInstance("SHA1PRNG");
					for (int i = 0; i < 7; i++) {
						generatedToken.append(number.nextInt(7));
					}
				} catch (NoSuchAlgorithmException e) {
					log.error("updateacceptedquantity() 1 :", e.fillInStackTrace());
				}
				grn = "GRN" + generatedToken.toString();
				poGrnMap.put(key, grn);
			}

			for (int k = 0; k < values.size(); k++) {

				String[] temp = billDatearr[k].split(",");
				String ponumber = temp[0];
				po = temp[0];
				String lineitemnumber = temp[1];
				String invoiceDate = temp[2];
				String invoicenumber = temp[3];
				invoice = temp[3];
				String actualquantity = temp[4];
				String storagelocation = temp[5];
				enduser = temp[6];
				String portalid = temp[7];
				status = temp[8];

				String actualquantityupdate = "update DELIVERYSUMMARY set ACCEPTEDQTY=? ,STORAGELOCATION=?,GRNNUMBER=? "
						+ "where INVOICENUMBER=? AND PONUMBER=? AND LINEITEMNUMBER=?";

				ps = con.prepareStatement(actualquantityupdate);
				ps.setString(1, actualquantity);
				ps.setString(2, storagelocation);
				ps.setString(3, poGrnMap.get(ponumber));
				ps.setString(4, invoicenumber);
				ps.setString(5, ponumber);
				ps.setString(6, lineitemnumber);
				value = ps.executeUpdate();
				ps.close();
			}

			for (String key : keys) {
				String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ? "
						+ " where INVOICENUMBER = ? AND PONUMBER = ?  AND MPO='Y'";

				ps = con.prepareStatement(updateGRN);
				ps.setString(1, poGrnMap.get(key));
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, invoice);
				ps.setString(4, key);
				ps.executeUpdate();
				ps.close();

				if (("Y").equalsIgnoreCase(status)) {
					String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=? "
							+ "where INVOICENUMBER=? AND PONUMBER=? AND MPO='Y'";

					ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
					ps.setString(1, enduser);
					ps.setString(2, invoice);
					ps.setString(3, key);
					value = ps.executeUpdate();
					ps.close();

					String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

					ps = con.prepareStatement(insertauditacceptqty);
					ps.setString(1, key);
					ps.setString(2, invoice);
					ps.setString(3, email);
					ps.setString(4, "Y");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					value = ps.executeUpdate();
					ps.close();
				}
			}
			con.commit();
			poGrnList.add(poGrnMap);

			if (poGrnList != null && poGrnList.size() > 0) {
				responsejson.put("grn", poGrnList);
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("updateacceptedquantity() 2 : ", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray updateacceptedquantitywithoutgrn(List<String> values, String email) {

		String[] billDatearr = values.toArray(new String[values.size()]);
		int value = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		LinkedHashSet<String> poHs = new LinkedHashSet<String>();

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String enduser = "";
			String invoice = "";
			String po = "";
			String status = "";

			for (int k = 0; k < values.size(); k++) {

				String[] temp = billDatearr[k].split(",");
				String ponumber = temp[0];
				po = temp[0];
				String lineitemnumber = temp[1];
				String invoicenumber = temp[3];
				invoice = temp[3];
				String actualquantity = temp[4];
				String storagelocation = temp[5];
				enduser = temp[6];
				String portalid = temp[7];
				status = temp[8];
				poHs.add(ponumber);

				String actualquantityupdate = "update DELIVERYSUMMARY set ACCEPTEDQTY=? ,STORAGELOCATION=?,GRNNUMBER=? "
						+ "where INVOICENUMBER=? AND PONUMBER=? AND LINEITEMNUMBER=?";

				ps = con.prepareStatement(actualquantityupdate);
				ps.setString(1, actualquantity);
				ps.setString(2, storagelocation);
				ps.setString(3, null);
				ps.setString(4, invoicenumber);
				ps.setString(5, ponumber);
				ps.setString(6, lineitemnumber);
				value = ps.executeUpdate();
				ps.close();
			}

			int appCount = 0;
			for (String poNum : poHs) {

				try {
					String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";

					ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
					ps.setString(1, enduser);
					ps.setString(2, null);
					ps.setString(3, invoice);
					ps.setString(4, poNum);
					appCount = ps.executeUpdate();
					ps.close();
				} catch (Exception e) {
					log.error("updateacceptedquantitywithoutgrn() 1 :", e.fillInStackTrace());
				}

				if (appCount != 0) {

					try {
						String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY_BEHALF (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

						ps = con.prepareStatement(insertauditacceptqty);
						ps.setString(1, poNum);
						ps.setString(2, invoice);
						ps.setString(3, email);
						ps.setString(4, "Y");
						ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
						value = ps.executeUpdate();
						ps.close();
					} catch (Exception e) {
						log.error("updateacceptedquantitywithoutgrn() 2 :", e.fillInStackTrace());
					}
				}
			}
			con.commit();
			if (value > 0) {
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("updateacceptedquantitywithoutgrn() 3 :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray updateacceptedquantitywithoutgrnforprod(List<String> values, String email) {

		String[] billDatearr = values.toArray(new String[values.size()]);
		int value = 0;

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String enduser = "";
			String invoice = "";
			String po = "";
			String status = "";

			LinkedHashSet<String> poHs = new LinkedHashSet<String>();

			for (int k = 0; k < values.size(); k++) {

				String[] temp = billDatearr[k].split(",");

				String ponumber = temp[0];
				po = temp[0];
				String lineitemnumber = temp[1];

				String invoicenumber = temp[3];
				invoice = temp[3];

				String actualquantity = temp[4];

				String storagelocation = temp[5];

				enduser = temp[6];

				String portalid = temp[7];

				status = temp[8];

				poHs.add(ponumber);

				String actualquantityupdate = "update DELIVERYSUMMARY set ACCEPTEDQTY=? ,STORAGELOCATION=?,GRNNUMBER=? "
						+ "where INVOICENUMBER=? AND PONUMBER=? AND LINEITEMNUMBER=?";

				ps = con.prepareStatement(actualquantityupdate);
				ps.setString(1, actualquantity);
				ps.setString(2, storagelocation);
				ps.setString(3, null);
				ps.setString(4, invoicenumber);
				ps.setString(5, ponumber);
				ps.setString(6, lineitemnumber);
				value = ps.executeUpdate();

				log.info("ACCEPTED : " + ponumber + " / " + lineitemnumber + " / " + actualquantity);
				ps.close();
			}

			int appCount = 0;
			for (String poNum : poHs) {

				try {
					String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";

					ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
					ps.setString(1, enduser);
					ps.setString(2, null);
					ps.setString(3, invoice);
					ps.setString(4, poNum);
					appCount = ps.executeUpdate();
					ps.close();
				} catch (Exception e) {
					log.error("updateacceptedquantitywithoutgrnforprod() 1 : ", e.fillInStackTrace());
				}

				if (appCount != 0) {
					log.info("APPCOUNT : " + appCount + " / " + poNum + " / " + invoice);
					try {
						String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY_BEHALF (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

						ps = con.prepareStatement(insertauditacceptqty);
						ps.setString(1, poNum);
						ps.setString(2, invoice);
						ps.setString(3, email);
						ps.setString(4, "Y");
						ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
						value = ps.executeUpdate();
						ps.close();
					} catch (Exception e) {
						log.error("updateacceptedquantitywithoutgrnforprod() 2 : ", e.fillInStackTrace());
					}
				}
			}
			con.commit();

			if (value > 0) {
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("updateacceptedquantitywithoutgrnforprod() 3 : ", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getVendorReturn(String invoice, String po_num) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String updatedPoNum = "";
		String poArray[] = null;
		if (po_num != null) {
			poArray = po_num.split(",");
		}

		ArrayList<String> poList = new ArrayList<String>();

		if (poArray != null && poArray.length > 0) {
			for (int i = 0; i < poArray.length; i++) {
				poList.add(poArray[i]);
			}
		} else {
			poList.add(po_num);
		}

		String archiveInsert = "INSERT INTO PONINVOICESUMMERYARCHIVE "
				+ "SELECT *  FROM PONINVOICESUMMERY where INVOICENUMBER=? and PONUMBER IN (?) ";
		String deleteInvoice = "DELETE FROM PONINVOICESUMMERY WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deleteStatus = "DELETE FROM DELIVERYSUMMARY WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deleteStatusInvoiceApp = " DELETE FROM INVOICEAPPROVAL WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deleteSupportingDocs = "DELETE FROM INVOICESUPPDOCS WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deletecheckboxentry = "DELETE FROM AUDIT_ACCEPTQTY WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deletecheckboxentryofbehalf = "DELETE FROM AUDIT_ACCEPTQTY_BEHALF WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		int value = 0;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			for (String po : poList) {
				po_num = po;
				ps = con.prepareStatement(archiveInsert);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				value = ps.executeUpdate();
				ps.close();
				ps = con.prepareStatement(deleteInvoice);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				value = ps.executeUpdate();
				ps.close();
				ps = con.prepareStatement(deleteStatus);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deleteSupportingDocs);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deletecheckboxentry);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deletecheckboxentryofbehalf);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deleteStatusInvoiceApp);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				ps.executeUpdate();
			}
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

	public JSONArray updateacceptedservicequantitywithoutgrn(List<String> acceptedValues, String email)
			throws SQLException {

		String[] acceptedList = acceptedValues.toArray(new String[acceptedValues.size()]);
		int value = 0;
		String returnGRN = null;
		String poNumber = null;
		String invoiceNumber = null;
		String invoiceDate = null;
		String poLineitem = null;
		String storageLocation = null;
		String quantity = null;
		String grnyear = null;
		String grnqty = null;
		String grnNumber = null;
		String serviceNo = null;
		String portalid = null;
		String serviceLineitem = null;
		String scrnNoList = null;
		String error = null;
		String message = null;
		String orderNumber = null;
		String enduser = null;
		String comboPoLineItem = null;
		ArrayList sendingList = new ArrayList();
		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		HashMap<Integer, String> grnHashMapping = new HashMap<Integer, String>();
		ArrayList lineItemlist = new ArrayList();
		ArrayList SAPReturnValues = new ArrayList();
		HashMap poLineGrnValue = new HashMap();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		int poCounter = 1;
		String comboPoLineitem = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			LinkedHashSet<String> poHs = new LinkedHashSet<String>();
			prop.load(input);
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

				String[] fatchedValues = acceptedList[counterValue].split(",");

				poNumber = null;
				invoiceNumber = null;
				invoiceDate = null;
				poLineitem = null;
				quantity = null;
				grnNumber = null;
				grnyear = null;
				grnqty = null;
				serviceNo = null;
				serviceLineitem = null;
				String[] lineItemNo = null;
				poLineitem = fatchedValues[1];
				comboPoLineItem = fatchedValues[1];
				quantity = fatchedValues[5];
				poNumber = fatchedValues[0]; // 00010-10
				invoiceNumber = fatchedValues[3];
				storageLocation = fatchedValues[6];
				enduser = fatchedValues[7];
				portalid = fatchedValues[8];
				poHs.add(poNumber);

				if (fatchedValues[1].contains("-")) {
					lineItemNo = fatchedValues[1].split("-");
					poLineitem = lineItemNo[0]; // 00010
					serviceLineitem = lineItemNo[1]; // 10
				} else {
					poLineitem = fatchedValues[1];
					serviceLineitem = fatchedValues[1];
				}

				String actualQuantityUpdate = "update DELIVERYSUMMARY set GRNNUMBER = ?, "
						+ "SCRNNUMBER= ?,ACCEPTEDQTY = ? ,STORAGELOCATION = ?  where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER = ? ";

				ps = con.prepareStatement(actualQuantityUpdate);
				ps.setString(1, null);
				ps.setString(2, null);
				ps.setString(3, quantity);
				ps.setString(4, storageLocation);
				ps.setString(5, invoiceNumber);
				ps.setString(6, poNumber);
				ps.setString(7, comboPoLineItem);
				value = ps.executeUpdate();
				ps.close();
			}

			int appCount = 0;
			for (String poNum : poHs) {

				String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=? "
						+ "where INVOICENUMBER=? AND PONUMBER=?";

				ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
				ps.setString(1, enduser);
				ps.setString(2, invoiceNumber);
				ps.setString(3, poNum);
				appCount = ps.executeUpdate();
				ps.close();

				log.info("APPCOUNT : " + appCount + " / " + poNum + " / " + invoiceNumber);

				if (appCount != 0) {
					String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY_BEHALF (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

					ps = con.prepareStatement(insertauditacceptqty);
					ps.setString(1, poNum);
					ps.setString(2, invoiceNumber);
					ps.setString(3, email);
					ps.setString(4, "Y");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					value = ps.executeUpdate();
					ps.close();
				}
			}

			con.commit();
			if (value > 0) {
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				if ("E".equalsIgnoreCase(error)) {
				} else {
					message = "Empty";
				}
				responsejson.put("message", message);
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("updateacceptedservicequantity() :", e.fillInStackTrace());
			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray updateacceptedservicequantity(List<String> acceptedValues, String email) throws SQLException {

		String[] acceptedList = acceptedValues.toArray(new String[acceptedValues.size()]);
		int value = 0;
		String returnGRN = null;
		String poNumber = null;
		String invoiceNumber = null;
		String invoiceDate = null;
		String poLineitem = null;
		String storageLocation = null;
		String quantity = null;
		String grnyear = null;
		String grnqty = null;
		String grnNumber = null;
		String serviceNo = null;
		String portalid = null;
		String status = null;
		String serviceLineitem = null;
		String scrnNoList = null;
		String error = null;
		String message = null;
		String orderNumber = null;
		String enduser = null;
		ArrayList sendingList = new ArrayList();
		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		ArrayList SAPReturnValues = new ArrayList();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		int poCounter = 1;
		String showGrnList = null;
		String showScrnList = null;
		String comboPoLineitem = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		LinkedHashMap poNoMap = new LinkedHashMap();
		LinkedHashMap poNoLineItemMap = new LinkedHashMap();
		LinkedHashMap poNoLineItemIndexWiseMap = new LinkedHashMap();

		LinkedHashMap poWiseGrnMap = new LinkedHashMap();
		LinkedHashMap poWiseScrnMap = new LinkedHashMap();
		ArrayList poWiseGrnList = new ArrayList();
		ArrayList poWiseScrnList = new ArrayList();

		try {
			prop.load(input);
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

				String[] fatchedValues = acceptedList[counterValue].split(",");
				poNumber = null;
				invoiceNumber = null;
				invoiceDate = null;
				poLineitem = null;
				quantity = null;
				grnNumber = null;
				grnyear = null;
				grnqty = null;
				serviceNo = null;
				serviceLineitem = null;
				comboPoLineitem = null;
				poNumber = fatchedValues[0]; // 00010-10
				comboPoLineitem = fatchedValues[1];

				String[] lineItemNo = null;
				if (fatchedValues[1].contains("-")) {
					lineItemNo = fatchedValues[1].split("-");
					poLineitem = lineItemNo[0]; // 00010
					serviceLineitem = lineItemNo[1]; // 10
				} else {
					poLineitem = fatchedValues[1];
					serviceLineitem = fatchedValues[1];
				}

				serviceNo = fatchedValues[2];
				invoiceNumber = fatchedValues[3];
				invoiceDate = fatchedValues[4];
				quantity = fatchedValues[5];
				storageLocation = fatchedValues[6];
				enduser = fatchedValues[7];
				portalid = fatchedValues[8];
				status = fatchedValues[9];

				log.info("Portal id is here in service == >" + portalid);
				poNoMap.put(poNumber, "-");

				String mainList[] = { poNumber, poLineitem, serviceLineitem, serviceNo, invoiceNumber, invoiceDate,
						quantity, storageLocation, portalid };

				log.info("poNumber : " + poNumber + " poLineitem :  " + poLineitem + " serviceLineitem : "
						+ serviceLineitem + " serviceNo : " + serviceNo + " invoiceNumber : " + invoiceNumber
						+ " invoiceDate : " + invoiceDate + " quantity : " + quantity + " storageLocation : "
						+ storageLocation + " portalid : " + portalid);

				sendingList.add(mainList);

				String actualQuantityUpdate = "update DELIVERYSUMMARY set ACCEPTEDQTY = ? ,STORAGELOCATION = ?  "
						+ " where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER = ? ";

				ps = con.prepareStatement(actualQuantityUpdate);
				ps.setString(1, quantity);
				ps.setString(2, storageLocation);
				ps.setString(3, invoiceNumber);
				ps.setString(4, poNumber);
				ps.setString(5, comboPoLineitem);
				value = ps.executeUpdate();
				ps.close();

			}

			SAPConnectionDetails.put("CLIENT", prop.getProperty("SERVICE_CLIENT"));
			SAPConnectionDetails.put("USERID", prop.getProperty("SERVICE_USERID"));
			SAPConnectionDetails.put("PASSWORD", prop.getProperty("SERVICE_PASSWORD"));
			SAPConnectionDetails.put("LANGUAGE", prop.getProperty("SERVICE_LANGUAGE"));
			SAPConnectionDetails.put("HOSTNAME", prop.getProperty("SERVICE_HOSTNAME"));
			SAPConnectionDetails.put("SYSTEMNO", prop.getProperty("SERVICE_SYSTEMNO"));
			SAPConnectionDetails.put("RFCNAME", prop.getProperty("SERVICE_RFCNAME"));
			SAPConnectionDetails.put("TOTALIMPORTTABLESTOSET", prop.getProperty("SERVICE_TOTALIMPORTTABLESTOSET"));
			SAPConnectionDetails.put("RETURNTABLE1", prop.getProperty("SERVICE_RETURNTABLE1"));
			SAPConnectionDetails.put("RETURNTABLE2", prop.getProperty("SERVICE_RETURNTABLE2"));
			SAPConnectionDetails.put("IMPORTTABLENAME1", prop.getProperty("SERVICE_IMPORTTABLENAME1"));
			SAPConnectionDetails.put("TOTALTABLESTORETURN", prop.getProperty("SERVICE_TOTALTABLESTORETURN"));

			String itemList1[] = { "EBELN", "EBELP", "SL_ITEM", "ACTIVITY", "XBLNR", "BLDAT", "MENGE", "LGORT",
					"WEMPF" };

			String returnKeys1[] = { "MAT_DOC", "DOC_YEAR", "QUANTITY", "SERVICE" };

			String returnKeys2[] = { "TYPE", "CODE", "MESSAGE" };

			SAPColumnHeads.put("IMPORTLINEITEM1", itemList1);
			SAPValues.put("IMPORTLINEITEMLIST1", sendingList);
			SAPColumnHeads.put("RETURNKEYS1", returnKeys1);
			SAPColumnHeads.put("RETURNKEYS2", returnKeys2);
			Set<String> keys = poNoMap.keySet();

			for (String key : keys) {

				log.info("Value of " + key + " is: " + poNoMap.get(key));

				poNoLineItemMap = new LinkedHashMap();
				poNoLineItemIndexWiseMap = new LinkedHashMap<>();
				int index = 0;
				ArrayList newLineItemList = new ArrayList();

				for (int ii = 0; ii < sendingList.size(); ii++) {

					String sapList[] = (String[]) sendingList.get(ii);
					String myPonumber = sapList[0];
					String myPoLineItemNumber = sapList[1];

					if (key.equals(myPonumber)) {
						newLineItemList.add(sapList);

						if (!poNoLineItemMap.containsKey(myPonumber + "-" + myPoLineItemNumber)) {

							poNoLineItemMap.put(myPonumber + "-" + myPoLineItemNumber, index + 1);

							StringBuilder generatedToken = new StringBuilder();
							StringBuilder servicelineitem = new StringBuilder();

							try {
								SecureRandom number = SecureRandom.getInstance("SHA1PRNG");
								SecureRandom servicenumber = SecureRandom.getInstance("SHA1PRNG");

								for (int i = 0; i < 7; i++) {
									generatedToken.append(number.nextInt(7));
									servicelineitem.append(servicenumber.nextInt(7));
								}
							} catch (NoSuchAlgorithmException e) {
								log.error("updateacceptedservicequantity() 1 : ", e.fillInStackTrace());
							}

							returnGRN = generatedToken.toString();
							serviceLineitem = servicelineitem.toString();
							String putGRNNSCRNValues = returnGRN + "-" + serviceLineitem;
							poNoLineItemIndexWiseMap.put(index + 1, putGRNNSCRNValues);
							index++;
						}
					}
				}

				log.info(key + " SIZE of lineitem " + poNoLineItemMap.size());

				log.info(key + " SIZE of lineitem response " + poNoLineItemIndexWiseMap.size());

				if (returnGRN != null && returnGRN != "") {
					showGrnList = "";
					showScrnList = "";

					for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

						String[] fatchedValues = acceptedList[counterValue].split(",");

						poNumber = null;
						invoiceNumber = null;
						invoiceDate = null;
						poLineitem = null;
						quantity = null;
						grnNumber = null;
						grnyear = null;
						grnqty = null;
						serviceNo = null;
						serviceLineitem = null;
						String[] lineItemNo = null;
						poLineitem = fatchedValues[1];
						quantity = fatchedValues[5];
						poNumber = fatchedValues[0]; // 00010-10
						invoiceNumber = fatchedValues[3];
						storageLocation = fatchedValues[6];
						enduser = fatchedValues[7];
						portalid = fatchedValues[8];
						status = fatchedValues[9];
						if (fatchedValues[1].contains("-")) {
							lineItemNo = fatchedValues[1].split("-");
							poLineitem = lineItemNo[0]; // 00010
							serviceLineitem = lineItemNo[1]; // 10
						} else {
							poLineitem = fatchedValues[1];
							serviceLineitem = fatchedValues[1];
						}

						if (!key.equals(poNumber)) {
							continue;
						}
						String getGrnScrnVal = (String) poNoLineItemIndexWiseMap
								.get(poNoLineItemMap.get(poNumber + "-" + poLineitem));

						log.info("getGrnScrnVal ==> " + getGrnScrnVal);
						String[] grnNscrnValue = getGrnScrnVal.split("-");

						if (!showGrnList.contains(grnNscrnValue[0]) == true) {
							showGrnList = showGrnList + grnNscrnValue[0] + ",";
						}
						if (!showScrnList.contains(grnNscrnValue[1]) == true) {
							showScrnList = showScrnList + grnNscrnValue[1] + ",";
						}
						log.info("showGrnList ==> " + showGrnList.toString());
						log.info("showScrnList ==> " + showScrnList.toString());
						log.info("grnNscrnValue[0] " + grnNscrnValue[0] + " grnNscrnValue[1]" + grnNscrnValue[1]
								+ " invoiceNumber" + invoiceNumber + " poNumber" + poNumber + " poLineitem "
								+ poLineitem);
						String actualQuantityUpdate = "update DELIVERYSUMMARY set GRNNUMBER = ?, "
								+ "SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER like ? ";

						ps = con.prepareStatement(actualQuantityUpdate);
						ps.setString(1, grnNscrnValue[0]);
						ps.setString(2, grnNscrnValue[1]);
						ps.setString(3, invoiceNumber);
						ps.setString(4, poNumber);
						ps.setString(5, poLineitem + "%");
						value = ps.executeUpdate();
						log.info("grnNscrnValue[0] " + grnNscrnValue[0] + " grnNscrnValue[1]" + grnNscrnValue[1]
								+ " invoiceNumber" + invoiceNumber + " poNumber" + poNumber + " poLineitem "
								+ poLineitem + " value" + value);
						ps.close();
					}

					showScrnList = showScrnList.substring(0, showScrnList.length() - 1);
					showGrnList = showGrnList.substring(0, showGrnList.length() - 1);

					log.info("show scrn list " + showScrnList);
					log.info("show grn list " + showGrnList);
					log.info("INVOICENUMBER " + invoiceNumber);
					log.info("PONUMBER " + poNumber);

					poWiseGrnList.add(showGrnList);
					poWiseScrnList.add(showScrnList);

					String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ?, "
							+ " SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? ";

					ps = con.prepareStatement(updateGRN);
					ps.setString(1, showGrnList);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, showScrnList);
					ps.setString(4, invoiceNumber);
					ps.setString(5, key);
					value = ps.executeUpdate();
					log.info("showGrnList " + showGrnList + " showGrnList" + showGrnList + " invoiceNumber"
							+ invoiceNumber + " poNumber" + key + " poLineitem " + poLineitem + " value" + value);
					ps.close();
					if (("Y").equalsIgnoreCase(status)) {
						String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
								+ "where INVOICENUMBER=? AND PONUMBER=?";

						ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
						ps.setString(1, enduser);
						ps.setString(2, null);
						ps.setString(3, invoiceNumber);
						ps.setString(4, key);
						value = ps.executeUpdate();
						ps.close();

						if (value != 0) {
							String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

							ps = con.prepareStatement(insertauditacceptqty);
							ps.setString(1, key);
							ps.setString(2, invoiceNumber);
							ps.setString(3, email);
							ps.setString(4, "Y");
							ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
							value = ps.executeUpdate();
							ps.close();
						}
					}
				}

			}

			if (returnGRN != null && returnGRN != "") {

				responsejson.put("grnlist", poWiseGrnList.toString());
				responsejson.put("scrnlist", poWiseScrnList.toString());
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
				con.commit();
			} else {
				if ("E".equalsIgnoreCase(error)) {
				} else {
					message = "Empty";
				}
				responsejson.put("message", message);
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("updateacceptedservicequantity() :", e.fillInStackTrace());
			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray getEnderUserReturn(List<EndUserReturn> enduserList, String email) throws SQLException {

		List<EndUserReturn> updatedEnduserList = new ArrayList<EndUserReturn>();
		LinkedHashSet<String> pohs = new LinkedHashSet<String>();
		for (int i = 0; i < enduserList.size(); i++) {
			pohs.add(enduserList.get(i).getPonumber());
		}

		for (String po : pohs) {
			for (int i = 0; i < enduserList.size(); i++) {
				if (enduserList.get(i).getPonumber().equals(po)) {
					updatedEnduserList.add(enduserList.get(i));
					break;
				}
			}
		}

		log.info("updatedEnduserList " + updatedEnduserList.size());

		String checkInvStatus = "SELECT OVERALLSTATUS FROM PONINVOICESUMMERY where INVOICENUMBER =? and PONUMBER=? ";

		String overAllStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";

		String insertintotracker = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
				+ "MODIFIEDTIME,MODIFIEDBY)" + " values(?,?,?,?,?,?)";

		String deleteresubmitstatus = "DELETE FROM INVOICETRACKER WHERE INVOICENUMBER =? and PONUMBER=? and STATUS=?";

		String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

		InternalportalImpl internalImpl = new InternalportalImpl();

		double actualbalanceQty = 0.0;
		int value = 0;
		String status = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			for (int i = 0; i < updatedEnduserList.size(); i++) {

				ps = con.prepareStatement(checkInvStatus);
				ps.setString(1, updatedEnduserList.get(i).getInvoicenumber());
				ps.setString(2, updatedEnduserList.get(i).getPonumber());
				rs = ps.executeQuery();
				if (rs.next()) {
					status = rs.getString("OVERALLSTATUS");
				}
				rs.close();
				ps.close();

				if ("V".equalsIgnoreCase(status)) {
					throw new DXPortalException("Invoice has been already returned !!", "Invoice already returned");
				}

				if (i == 0) {
					internalImpl.executeUpdateBalance(con, enduserList);
				}

				ps = con.prepareStatement(overAllStatus);
				ps.setString(1, "V");
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, updatedEnduserList.get(i).getInvoicenumber());
				ps.setString(4, updatedEnduserList.get(i).getPonumber());
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(insertintotracker);
				ps.setString(1, updatedEnduserList.get(i).getInvoicenumber());
				ps.setString(2, updatedEnduserList.get(i).getPonumber());
				ps.setString(3, updatedEnduserList.get(i).getBid());
				ps.setString(4, "V");
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//				ps.setString(6, updatedEnduserList.get(i).getUserid());
				ps.setString(6, email);
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deleteresubmitstatus);
				ps.setString(1, updatedEnduserList.get(i).getInvoicenumber());
				ps.setString(2, updatedEnduserList.get(i).getPonumber());
				ps.setString(3, "S");
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(insertauditacceptqty);
				ps.setString(1, updatedEnduserList.get(i).getPonumber());
				ps.setString(2, updatedEnduserList.get(i).getInvoicenumber());
				ps.setString(3, email);
				ps.setString(4, updatedEnduserList.get(i).getStatus());
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
				value = ps.executeUpdate();
				ps.close();
			}

			con.commit();
			responsejson.put("message", "Invoice returned successfully");
			jsonArray.add(responsejson);
		} catch (DXPortalException dxp) {
			responsejson.put("message", dxp.reason);
			jsonArray.add(responsejson);
			log.error("getEnderUserReturn() 1 : ", dxp.fillInStackTrace());
			con.rollback();
		} catch (Exception e) {
			log.error("getEnderUserReturn() :", e.fillInStackTrace());
			responsejson.put("message", e.getLocalizedMessage());
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray getapprovalinvoicelistfordownload(String emailId, String status, String invno, String pono,
			String fdate, String tdate, HttpSession session) throws SQLException {

		ArrayList<HashMap<String, String>> POEvent = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> POListEvent = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		List<String> invoicedata = new ArrayList<>();
		List<String> podata = new ArrayList<>();
		ResultSet rs = null;
		int pages = 0;
		String storeKepeerQuery = null;
		String sql = null;
		String storeKepeer = (String) session.getAttribute("shopkepeer");

		try {
			con = DBConnection.getConnection();
			if (!"AS".equalsIgnoreCase(status)) {

				if ("C".equalsIgnoreCase(status)) {

					sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
							+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
							+ "AND (CREDITADVICENO IS NOT NULL) " + "UNION"
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(CREDITADVICENO IS NOT NULL AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
							+ "UNION" + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND"
							+ " (CREDITADVICENO IS NOT NULL AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

					storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND CREDITADVICENO IS NOT NULL "
							+ " UNION" + " SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
							+ " CREDITADVICENO IS NOT NULL " + "UNION "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' AND "
							+ "CREDITADVICENO IS NOT NULL " + "UNION" + " SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND CREDITADVICENO IS NOT NULL) JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "ORDER BY CREATEDON DESC) c )";
				} else if ("ALL".equalsIgnoreCase(status)) {

					sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
							+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
							+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "( A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)" + " UNION "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?)  AND "
							+ " (A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

					storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + " UNION"
							+ " SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL  "
							+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' " + "UNION"
							+ " SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "ORDER BY CREATEDON DESC) c )";

				} else {
					if ("P".equalsIgnoreCase(status) || "V".equalsIgnoreCase(status)) {
						sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE, "
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER, "
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO, "
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? OR B.OVERALLSTATUS=?) " + " UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
								+ "(B1.OVERALLSTATUS=?  OR B1.OVERALLSTATUS=? AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?)  AND "
								+ "(B1.OVERALLSTATUS=?  OR B1.OVERALLSTATUS=? AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

						storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ " A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=? OR B.OVERALLSTATUS=? )"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
								+ "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=?) " + "UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  AND "
								+ "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=?)" + " UNION "
								+ "SELECT DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND (B.OVERALLSTATUS IN (?,?)) JOIN "
								+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "ORDER BY CREATEDON DESC) c )";
					} else {
						sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE, "
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER, "
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS, "
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS, "
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER, "
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO, "
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? ) " + " UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
								+ "(B1.OVERALLSTATUS=?  AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
								+ "(B1.OVERALLSTATUS=?  AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

						storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ " A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber 	AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=? ) "
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
								+ "(B1.OVERALLSTATUS=? ) " + "UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID,B1.MPO,B1.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  AND "
								+ "(B1.OVERALLSTATUS=? )" + " UNION " + "SELECT DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND (B.OVERALLSTATUS =?) JOIN "
								+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "ORDER BY CREATEDON DESC) c )";
					}

				}

				if ("true".equalsIgnoreCase(storeKepeer)) {
					ps = con.prepareStatement(storeKepeerQuery);
					responsejson.put("storekeeper", "true");
					ArrayList<String> param = new ArrayList<String>();

					if (status.equalsIgnoreCase("C") || status.equalsIgnoreCase("ALL")) {
						param.add(emailId);
						param.add(emailId);
						param.add(emailId);
						param.add(emailId);
					} else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {
						param.add(emailId);
						param.add(status);
						if ("P".equalsIgnoreCase(status)) {

							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
							param.add(status);
							param.add("M");
							param.add(emailId);
						} else if ("V".equalsIgnoreCase(status)) {

							param.add("RO");
							param.add(emailId);
							param.add(status);
							param.add("RO");
							param.add(emailId);
							param.add(status);
							param.add("RO");
							param.add(status);
							param.add("RO");
							param.add(emailId);
						} else {
							param.add(emailId);
							param.add(status);
							param.add(emailId);
							param.add(status);
							param.add(status);
							param.add(emailId);
						}
					}
					Pagination pg = new Pagination(storeKepeerQuery, 0, 1);
					rs = pg.execute(con, param);
				} else {

					ps = con.prepareStatement(sql);
					responsejson.put("storekeeper", "false");

					ArrayList<String> param = new ArrayList<String>();
					if (status.equalsIgnoreCase("C") || status.equalsIgnoreCase("ALL")) {
						param.add(emailId);
						param.add(emailId);
						param.add(emailId);
					}

					else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {
						param.add(emailId);
						param.add(status);
						if ("P".equalsIgnoreCase(status)) {

							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
							param.add(emailId);
							param.add(status);
							param.add("M");
						} else if ("V".equalsIgnoreCase(status)) {

							param.add("RO");
							param.add(emailId);
							param.add(status);
							param.add("RO");
							param.add(emailId);
							param.add(status);
							param.add("RO");
						} else {
							param.add(emailId);
							param.add(status);
							param.add(emailId);
							param.add(status);
						}
					}
					Pagination pg = new Pagination(sql, 0, 1);
					rs = pg.execute(con, param);
				}

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
			} else {
				String basePoQuery = " A.PONumber = B.PONUMBER ";
				String basePoQuery1 = " A1.PONumber = B.PONUMBER ";

				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				if ("true".equalsIgnoreCase(storeKepeer)) {
					param.add(emailId);
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND B.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " A.PONumber = B.BASEPO ";
						basePoQuery1 = " A1.PONumber = B.BASEPO ";
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND B.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND B.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);

					if (!pono.equalsIgnoreCase("NA")) {

						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {

						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {

						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);
					for (int i = 0; i < 2; i++) {
						if (!pono.equalsIgnoreCase("NA")) {

							param.add(pono);
						}
						if (!invno.equalsIgnoreCase("NA")) {

							param.add(invno);
						}
						if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {

							param.add(fdate);
							param.add(tdate);
						}
					}
					param.add(emailId);

				} else {
					param.add(emailId);

					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND B.PONUMBER=?";
						subquery = subquery + po;
						param.add(pono);

						basePoQuery = " A.PONumber = B.BASEPO ";
						basePoQuery1 = " A1.PONumber = B.BASEPO ";
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND B.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND B.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}

					param.add(emailId);

					if (!pono.equalsIgnoreCase("NA")) {
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}
					param.add(emailId);
					if (!pono.equalsIgnoreCase("NA")) {

						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {

						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {

						param.add(fdate);
						param.add(tdate);
					}
				}
				sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
						+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
						+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
						+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
						+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
						+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
						+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
						+ "A.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery + " AND A.ENDUSEID = ? " + " " + subquery
						+ " " + " UNION " + "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ "B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL "
						+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery1
						+ " AND (A1.EUMANAGER = ? )  AND" + "(A1.STATUS NOT LIKE 'C%' AND B.GRNNUMBER IS NOT NULL "
						+ subquery + ") " + "UNION " + "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ " B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL "
						+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery1
						+ "  AND (A1.EUMANAGER = ? ) AND " + "(A1.STATUS LIKE 'C%' " + subquery
						+ ") ORDER BY CREATEDON DESC) c )";

				storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
						+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
						+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
						+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID,MPO,ALLPO, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
						+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
						+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
						+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
						+ "A.InvoiceNumber=B.InvoiceNumber AND " + basePoQuery
						+ " AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + subquery + " " + "UNION "
						+ "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,"
						+ "B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ basePoQuery1 + " AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' "
						+ "AND B.GRNNUMBER IS NOT NULL  " + subquery + " " + " UNION "
						+ "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,"
						+ "B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ basePoQuery1 + " AND (A1.EUMANAGER = ?) " + "AND A1.STATUS LIKE 'C%'  " + subquery + " "
						+ "UNION " + "SELECT DISTINCT B.PONUMBER,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
						+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
						+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
						+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID,B.MPO,B.ALLPO FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
						+ basePoQuery + " AND A.PROXY = 'X' " + subquery + " JOIN "
						+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
						+ "ORDER BY CREATEDON DESC) c )";

				Pagination pg = null;
				if ("true".equalsIgnoreCase(storeKepeer)) {
					responsejson.put("storekeeper", "true");
					pg = new Pagination(storeKepeerQuery, 0, 2);
					rs = pg.execute(con, param);
				} else {
					responsejson.put("storekeeper", "false");
					pg = new Pagination(sql, 0, 1);
					rs = pg.execute(con, param);

				}
				String mPO;
				String allPO;
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
				pg.close();
				rs.close();
				pg = null;

			}
		} catch (Exception e) {
			log.error("getapprovalinvoicelistfordownload() :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray getAcceptQtynServiceGRN(List<String> acceptedValues, String email, Connection con)
			throws SQLException {

		String[] acceptedList = acceptedValues.toArray(new String[acceptedValues.size()]);
		int value = 0;
		String returnGRN = null;
		String poNumber = null;
		String invoiceNumber = null;
		String invoiceDate = null;
		String poLineitem = null;
		String storageLocation = null;
		String quantity = null;
		String grnyear = null;
		String grnqty = null;
		String grnNumber = null;
		String serviceNo = null;
		String serviceLineitem = null;
		String scrnNoList = null;
		String error = null;
		String message = null;
		String orderNumber = null;
		String enduser = null;
		String portalid = null;
		String status = null;
		String warningMessage = null;
		String errorMessage = "";
		ArrayList sendingList = new ArrayList();

		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		ArrayList SAPReturnValues = new ArrayList();

		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		int poCounter = 1;
		String showGrnList = null;
		String showScrnList = null;
		String comboPoLineitem = null;
		portalid = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		LinkedHashMap poNoMap = new LinkedHashMap();
		LinkedHashMap poNoLineItemMap = new LinkedHashMap();
		LinkedHashMap poNoLineItemIndexWiseMap = new LinkedHashMap();
		ArrayList poWiseGrnList = new ArrayList();
		ArrayList poWiseScrnList = new ArrayList();

		try {
			prop.load(input);
			con.setAutoCommit(false);

			for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

				String[] fatchedValues = acceptedList[counterValue].split(",");
				poNumber = null;
				invoiceNumber = null;
				invoiceDate = null;
				poLineitem = null;
				quantity = null;
				grnNumber = null;
				grnyear = null;
				grnqty = null;
				serviceNo = null;
				serviceLineitem = null;
				String[] lineItemNo = null;
				comboPoLineitem = null;

				poNumber = fatchedValues[0]; // 00010-10
				comboPoLineitem = fatchedValues[1];
				poNumber = fatchedValues[0]; // 00010-10
				if (fatchedValues[1].contains("-")) {
					lineItemNo = fatchedValues[1].split("-");
					poLineitem = lineItemNo[0]; // 00010
					serviceLineitem = lineItemNo[1]; // 10
				} else {
					poLineitem = fatchedValues[1];
					serviceLineitem = fatchedValues[1];
				}

				poNoMap.put(poNumber, "-"); // ponumber-00010 ,ponumber-00020

				serviceNo = fatchedValues[2];
				invoiceNumber = fatchedValues[3];
				Date date = (Date) formatter.parseObject(fatchedValues[4]);
				invoiceDate = sm.format(date);
				quantity = fatchedValues[5];
				storageLocation = fatchedValues[6];
				enduser = fatchedValues[7];
				portalid = fatchedValues[8];
				status = fatchedValues[9];

				String mainList[] = { poNumber, poLineitem, serviceLineitem, serviceNo, invoiceNumber, invoiceDate,
						quantity, storageLocation, portalid };
				sendingList.add(mainList);

				log.info("#" + poNumber + "#" + poLineitem + "#" + serviceLineitem + "#" + serviceNo + "#"
						+ invoiceNumber + "#" + invoiceDate + "#" + quantity + "#" + storageLocation + "#" + portalid);

				String actualQuantityUpdate = "update DELIVERYSUMMARY set ACCEPTEDQTY = ? ,STORAGELOCATION = ?  "
						+ " where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER = ? ";

				ps = con.prepareStatement(actualQuantityUpdate);
				ps.setString(1, quantity);
				ps.setString(2, storageLocation);
				ps.setString(3, invoiceNumber);
				ps.setString(4, poNumber);
				ps.setString(5, comboPoLineitem);
				value = ps.executeUpdate();
				ps.close();
			}

			SAPConnectionDetails.put("CLIENT", prop.getProperty("CLIENT"));
			SAPConnectionDetails.put("USERID", prop.getProperty("USERID"));
			SAPConnectionDetails.put("PASSWORD", prop.getProperty("PASSWORD"));
			SAPConnectionDetails.put("LANGUAGE", prop.getProperty("LANGUAGE"));
			SAPConnectionDetails.put("HOSTNAME", prop.getProperty("HOSTNAME"));
			SAPConnectionDetails.put("SYSTEMNO", prop.getProperty("SYSTEMNO"));
			SAPConnectionDetails.put("RFCNAME", prop.getProperty("SERVICE_RFCNAME"));
			SAPConnectionDetails.put("TOTALIMPORTTABLESTOSET", prop.getProperty("SERVICE_TOTALIMPORTTABLESTOSET"));
			SAPConnectionDetails.put("RETURNTABLE1", prop.getProperty("SERVICE_RETURNTABLE1"));
			SAPConnectionDetails.put("RETURNTABLE2", prop.getProperty("SERVICE_RETURNTABLE2"));
			SAPConnectionDetails.put("IMPORTTABLENAME1", prop.getProperty("SERVICE_IMPORTTABLENAME1"));
			SAPConnectionDetails.put("TOTALTABLESTORETURN", prop.getProperty("SERVICE_TOTALTABLESTORETURN"));

			String itemList1[] = { "EBELN", "EBELP", "SL_ITEM", "ACTIVITY", "XBLNR", "BLDAT", "MENGE", "LGORT",
					"WEMPF" };
			String returnKeys1[] = { "MAT_DOC", "DOC_YEAR", "QUANTITY", "SERVICE" };
			String returnKeys2[] = { "TYPE", "CODE", "MESSAGE" };

			SAPColumnHeads.put("IMPORTLINEITEM1", itemList1);

			Set<String> keys = poNoMap.keySet();

			int successCounter = 0;

			for (String key : keys) {

				returnGRN = null;
				showGrnList = "";
				showScrnList = "";

				log.info("Value of " + key + " is: " + poNoMap.get(key));

				poNoLineItemMap = new LinkedHashMap();
				poNoLineItemIndexWiseMap = new LinkedHashMap<>();
				int index = 0;
				ArrayList newLineItemList = new ArrayList();

				for (int ii = 0; ii < sendingList.size(); ii++) {

					String sapList[] = (String[]) sendingList.get(ii);
					String myPonumber = sapList[0];
					String myPoLineItemNumber = sapList[1];

					if (key.equals(myPonumber)) {
						newLineItemList.add(sapList);
						if (!poNoLineItemMap.containsKey(myPonumber + "-" + myPoLineItemNumber)) {
							poNoLineItemMap.put(myPonumber + "-" + myPoLineItemNumber, index + 1);
							index++;
						}
					}
				}

				log.info(key + " SIZE of lineitem " + poNoLineItemMap.size());

				SAPValues.put("IMPORTLINEITEMLIST1", newLineItemList);
				SAPColumnHeads.put("RETURNKEYS1", returnKeys1);
				SAPColumnHeads.put("RETURNKEYS2", returnKeys2);
				JcoGetDataFromSAP jco = new JcoGetDataFromSAP("dxproject");

				SAPReturnData = jco.jcoGetData(SAPConnectionDetails, SAPColumnHeads, SAPValues);

				log.info("SAPReturnData :" + SAPReturnData.size() + " Invoice Number : " + invoiceNumber);
				SAPReturnData.forEach((k, v) -> log.info("Key : " + k + ", Value : " + v));

				warningMessage = "";
				if (SAPReturnData.containsKey("RETURNDATA1")) {
					SAPReturnValues = (ArrayList) SAPReturnData.get("RETURNDATA1");

					int counter = SAPReturnValues.size();
					log.info("counter :" + counter + " Invoice Number : " + invoiceNumber);
					if (counter > 0) {

						for (int ii = 0; ii < counter; ii++) {

							String arrayData[] = (String[]) SAPReturnValues.get(ii);
							log.info("arrayData[] 1 :" + arrayData.toString() + " Invoice Number : " + invoiceNumber);
							returnGRN = arrayData[0];
							grnyear = arrayData[1];
							grnqty = arrayData[2];
							serviceLineitem = arrayData[3];
							log.info("returnGRN:" + returnGRN + " :grnyear: " + grnyear + " :grnqty: " + grnqty
									+ " Invoice Number : " + invoiceNumber);
							String putGRNNSCRNValues = returnGRN + "-" + serviceLineitem;
							int myKey = ii + 1;
							poNoLineItemIndexWiseMap.put(myKey, putGRNNSCRNValues);
						}

					}
				}

				log.info(key + " size of polinitem response from sap " + poNoLineItemIndexWiseMap.size()
						+ " Invoice Number : " + invoiceNumber);

				if (SAPReturnData.containsKey("RETURNDATA2")) {
					SAPReturnValues = (ArrayList) SAPReturnData.get("RETURNDATA2");
					log.info("SAPReturnValues :" + SAPReturnValues.size() + " Invoice Number : " + invoiceNumber);
					int counter = SAPReturnValues.size();
					warningMessage = "";
					if (counter > 0) {

						for (int ii = 0; ii < counter; ii++) {
							String arrayData[] = (String[]) SAPReturnValues.get(ii);
							error = arrayData[0];
							message = arrayData[2];
							log.info("arrayData[] 2 :" + arrayData.length + " : error : " + error + " : message : "
									+ message + " Invoice Number : " + invoiceNumber);

							if ("W".equalsIgnoreCase(error) || "S".equalsIgnoreCase(error)
									|| "I".equalsIgnoreCase(error) || "E".equalsIgnoreCase(error)) {
								warningMessage = warningMessage + message + ",";
							}

							if ("E".equalsIgnoreCase(error)) {
								errorMessage = key + "-" + errorMessage + message + " ";
							}
						}

						if (warningMessage.length() > 2) {
							warningMessage = warningMessage.substring(0, warningMessage.length() - 1);
						}
					}
				}

				if (returnGRN != null && returnGRN != "") {
					showGrnList = "";
					showScrnList = "";
					for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

						String[] fatchedValues = acceptedList[counterValue].split(",");
						poNumber = null;
						invoiceNumber = null;
						invoiceDate = null;
						poLineitem = null;
						quantity = null;
						grnNumber = null;
						grnyear = null;
						grnqty = null;
						serviceNo = null;
						serviceLineitem = null;
						String[] lineItemNo = null;
						poNumber = fatchedValues[0]; // 00010-10

						if (fatchedValues[1].contains("-")) {
							lineItemNo = fatchedValues[1].split("-");
							poLineitem = lineItemNo[0]; // 00010
							serviceLineitem = lineItemNo[1]; // 10
						} else {
							poLineitem = fatchedValues[1];
							serviceLineitem = fatchedValues[1];
						}
						invoiceNumber = fatchedValues[3];
						quantity = fatchedValues[5];
						storageLocation = fatchedValues[6];

						if (!key.equals(poNumber)) {
							continue;
						}

						String getGrnScrnVal = (String) poNoLineItemIndexWiseMap
								.get(poNoLineItemMap.get(poNumber + "-" + poLineitem));
						log.info("getGrnScrnVal ==> " + getGrnScrnVal + " Invoice Number : " + invoiceNumber);

						String[] grnNscrnValue = getGrnScrnVal.split("-");

						if (!showGrnList.contains(grnNscrnValue[0]) == true) {
							showGrnList = showGrnList + grnNscrnValue[0] + ",";
						}
						if (!showScrnList.contains(grnNscrnValue[1]) == true) {
							showScrnList = showScrnList + grnNscrnValue[1] + ",";
						}
						log.info("showGrnList ==> " + showGrnList.toString() + "showScrnList ==> "
								+ showScrnList.toString() + " Invoice Number : " + invoiceNumber);

						String actualQuantityUpdate = "update DELIVERYSUMMARY set GRNNUMBER = ?, "
								+ "SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER like ? ";

						ps = con.prepareStatement(actualQuantityUpdate);
						ps.setString(1, grnNscrnValue[0]);
						ps.setString(2, grnNscrnValue[1]);
						ps.setString(3, invoiceNumber);
						ps.setString(4, poNumber);
						ps.setString(5, poLineitem + "%");
						value = ps.executeUpdate();
						ps.close();
					}

					if (("Y").equalsIgnoreCase(status)) {
						String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=? "
								+ "where INVOICENUMBER=? AND PONUMBER=?";

						ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
						ps.setString(1, enduser);
						ps.setString(2, invoiceNumber);
						ps.setString(3, key);
						value = ps.executeUpdate();
						ps.close();

						if (value != 0) {

							String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";
							ps = con.prepareStatement(insertauditacceptqty);
							ps.setString(1, key);
							ps.setString(2, invoiceNumber);
							ps.setString(3, email);
							ps.setString(4, "Y");
							ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
							value = ps.executeUpdate();
							ps.close();
						}
					}

					String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ?, "
							+ " SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? ";

					showScrnList = showScrnList.substring(0, showScrnList.length() - 1);
					showGrnList = showGrnList.substring(0, showGrnList.length() - 1);
					ps = con.prepareStatement(updateGRN);
					ps.setString(1, showGrnList);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, showScrnList);
					ps.setString(4, invoiceNumber);
					ps.setString(5, key);
					ps.executeUpdate();
					ps.close();
					successCounter++;
				}
				con.commit();

				if (showGrnList != null && !"".equals(showGrnList)) {
					poWiseGrnList.add(key + "-" + showGrnList);
				}
				if (showScrnList != null && !"".equals(showScrnList)) {
					poWiseScrnList.add(key + "-" + showScrnList);
				}

			}

			log.info("Success Counter : " + successCounter + " Invoice Number : " + invoiceNumber);

			if (keys.size() > successCounter) {

				String grnNums = "";
				for (int i = 0; i < poWiseGrnList.size(); i++) {
					if (i == poWiseGrnList.size() - 1) {
						grnNums = grnNums + poWiseGrnList.get(i);
					} else {
						grnNums = grnNums + poWiseGrnList.get(i) + ",";
					}
				}

				String scrnNums = "";
				for (int i = 0; i < poWiseScrnList.size(); i++) {
					if (i == poWiseScrnList.size() - 1) {
						scrnNums = scrnNums + poWiseScrnList.get(i);
					} else {
						scrnNums = scrnNums + poWiseScrnList.get(i) + ",";
					}
				}
				log.info("grnlist ------------------:" + grnNums + " Invoice Number : " + invoiceNumber);
				log.info("scrnlist -----------------:" + scrnNums + " Invoice Number : " + invoiceNumber);

				responsejson.put("grnlist", grnNums);
				responsejson.put("scrnlist", scrnNums);
				responsejson.put("message", "Success");

				responsejson.put("warningMessage", warningMessage + " " + errorMessage);

				jsonArray.add(responsejson);

			} else if (returnGRN != null && returnGRN != "") {

				String grnNums = "";
				for (int i = 0; i < poWiseGrnList.size(); i++) {
					if (i == poWiseGrnList.size() - 1) {
						grnNums = grnNums + poWiseGrnList.get(i);
					} else {
						grnNums = grnNums + poWiseGrnList.get(i) + ",";
					}
				}

				String scrnNums = "";
				for (int i = 0; i < poWiseScrnList.size(); i++) {
					if (i == poWiseScrnList.size() - 1) {
						scrnNums = scrnNums + poWiseScrnList.get(i);
					} else {
						scrnNums = scrnNums + poWiseScrnList.get(i) + ",";
					}
				}
				log.info("grnlist ------------------:" + grnNums + " Invoice Number : " + invoiceNumber);
				log.info("scrnlist -----------------:" + scrnNums + " Invoice Number : " + invoiceNumber);
				responsejson.put("grnlist", grnNums);
				responsejson.put("scrnlist", scrnNums);

				responsejson.put("message", "Success");
				responsejson.put("warningMessage", warningMessage);
				jsonArray.add(responsejson);
			} else {
				if ("E".equalsIgnoreCase(error)) {
					warningMessage = message;
					log.info("warningMessage -------------------------:" + warningMessage + " Invoice Number : "
							+ invoiceNumber);
				} else {
					message = "Empty";
				}

				if (!"E".equalsIgnoreCase(error)) {
					warningMessage = message;
					log.info("message -------------------------:" + message + " Invoice Number : " + invoiceNumber);
				} else {
					warningMessage = message;
				}

				responsejson.put("message", message);
				responsejson.put("warningMessage", warningMessage);
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			con.rollback();
			log.error("getAcceptQtynServiceGRN() :", e.fillInStackTrace());
		}

		return jsonArray;
	}

	public JSONArray getAcceptQtynGRN(List<String> acceptedValues, String email, Connection con) throws SQLException {

		String[] acceptedList = acceptedValues.toArray(new String[acceptedValues.size()]);
		int value = 0;
		String returnGRN = null;
		String poNumber = null;
		String invoiceNumber = null;
		String invoiceDate = null;
		String poLineitem = null;
		String storageLocation = null;
		String quantity = null;
		String grnyear = null;
		String grnqty = null;
		String error = null;
		String message = null;
		String orderNumber = null;
		String enduser = null;
		String timescapeid = null;
		String status = null;

		String warningMessage = null;

		String portalid = null;
		ArrayList sendingList = new ArrayList();
		HashMap poNoMap = new HashMap();
		HashMap poNoMapMain = new HashMap();

		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		ArrayList lineItemlist = new ArrayList();
		ArrayList SAPReturnValues = new ArrayList();
		ArrayList grnLIST = new ArrayList();

		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			prop.load(input);
			con.setAutoCommit(false);

			for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

				String[] fatchedValues = acceptedList[counterValue].split(",");

				poNumber = null;
				invoiceNumber = null;
				invoiceDate = null;
				poLineitem = null;
				quantity = null;
				storageLocation = null;
				orderNumber = null;
				timescapeid = null;
				poNumber = fatchedValues[0];
				poLineitem = fatchedValues[1];
				Date date = (Date) formatter.parseObject(fatchedValues[2]);
				invoiceDate = sm.format(date);
				invoiceNumber = fatchedValues[3];
				quantity = fatchedValues[4];
				storageLocation = fatchedValues[5];
				enduser = fatchedValues[6];
				portalid = fatchedValues[7];
				status = fatchedValues[8];

				poNoMap.put(poNumber, "-");
				poNoMapMain.put(poNumber, "-");

				log.info("#" + poNumber + "#" + poLineitem + "#" + invoiceDate + "#" + invoiceNumber + "#" + quantity
						+ "#" + storageLocation + "#" + portalid);
				if ("".equalsIgnoreCase(portalid) || portalid == null) {
					portalid = "dsubra1007";
				}
				String mainList[] = { poNumber, poLineitem, invoiceDate, invoiceNumber, quantity, storageLocation,
						portalid };
				sendingList.add(mainList);
			}

			SAPConnectionDetails.put("CLIENT", prop.getProperty("CLIENT"));
			SAPConnectionDetails.put("USERID", prop.getProperty("USERID"));
			SAPConnectionDetails.put("PASSWORD", prop.getProperty("PASSWORD"));
			SAPConnectionDetails.put("LANGUAGE", prop.getProperty("LANGUAGE"));
			SAPConnectionDetails.put("HOSTNAME", prop.getProperty("HOSTNAME"));
			SAPConnectionDetails.put("SYSTEMNO", prop.getProperty("SYSTEMNO"));
			SAPConnectionDetails.put("RFCNAME", prop.getProperty("RFCNAME"));
			SAPConnectionDetails.put("TOTALIMPORTTABLESTOSET", prop.getProperty("TOTALIMPORTTABLESTOSET"));
			SAPConnectionDetails.put("RETURNTABLE1", prop.getProperty("RETURNTABLE1"));
			SAPConnectionDetails.put("RETURNTABLE2", prop.getProperty("RETURNTABLE2"));
			SAPConnectionDetails.put("IMPORTTABLENAME1", prop.getProperty("IMPORTTABLENAME1"));
			SAPConnectionDetails.put("TOTALTABLESTORETURN", prop.getProperty("TOTALTABLESTORETURN"));

			String itemList1[] = { "EBELN", "EBELP", "BUDAT", "XBLNR", "ERFMG", "LGORT", "WEMPF" };

			String returnKeys1[] = { "MAT_DOC", "DOC_YEAR", "QUANTITY" };

			String returnKeys2[] = { "TYPE", "ID", "NUMBER", "MESSAGE", "LOG_NO", "LOG_MSG_NO", "MESSAGE_V1",
					"MESSAGE_V2", "MESSAGE_V3", "MESSAGE_V4", "PARAMETER", "ROW", "FIELD", "SYSTEM" };

			SAPColumnHeads.put("IMPORTLINEITEM1", itemList1);

			Set<String> keys = poNoMap.keySet();
			for (String key : keys) {
				log.info("Value of " + key + " is: " + poNoMap.get(key));

				ArrayList newLineItemList = new ArrayList();
				for (int ii = 0; ii < sendingList.size(); ii++) {

					String sapList[] = (String[]) sendingList.get(ii);
					String myPonumber = sapList[0];
					if (key.equals(myPonumber)) {
						newLineItemList.add(sapList);
					}
				}

				SAPValues.put("IMPORTLINEITEMLIST1", newLineItemList);
				SAPColumnHeads.put("RETURNKEYS1", returnKeys1);
				SAPColumnHeads.put("RETURNKEYS2", returnKeys2);

				JcoGetDataFromSAP jco = new JcoGetDataFromSAP("dxproject");
				SAPReturnData = jco.jcoGetData(SAPConnectionDetails, SAPColumnHeads, SAPValues);

				log.info("SAPReturnData :" + SAPReturnData.size() + " Invoice Number : " + invoiceNumber);
				SAPReturnData.forEach((k, v) -> log.info("Key : " + k + ", Value : " + v));

				warningMessage = "";
				if (SAPReturnData.containsKey("RETURNDATA1")) {
					SAPReturnValues = (ArrayList) SAPReturnData.get("RETURNDATA1");

					int counter = SAPReturnValues.size();
					log.info("counter :" + counter + " Invoice Number : " + invoiceNumber);
					if (counter > 0) {
						for (int ii = 0; ii < counter; ii++) {
							String arrayData[] = (String[]) SAPReturnValues.get(ii);
							log.info("arrayData[] 1 :" + arrayData.toString() + " Invoice Number : " + invoiceNumber);
							returnGRN = arrayData[0];
							grnyear = arrayData[1];
							grnqty = arrayData[2];
							log.info("returnGRN :" + returnGRN + " :grnyear: " + grnyear + " :grnqty: " + grnqty
									+ " Invoice Number : " + invoiceNumber);
							poNoMapMain.put(key, returnGRN);
						}
					}
				}

				if (SAPReturnData.containsKey("RETURNDATA2")) {
					SAPReturnValues = (ArrayList) SAPReturnData.get("RETURNDATA2");
					log.info("SAPReturnValues :" + SAPReturnValues.size() + " Invoice Number : " + invoiceNumber);
					int counter = SAPReturnValues.size();
					warningMessage = "";
					if (counter > 0) {

						for (int ii = 0; ii < counter; ii++) {
							String arrayData[] = (String[]) SAPReturnValues.get(ii);
							log.info("arrayData[] 2 :" + arrayData.length + " Invoice Number : " + invoiceNumber);
							error = arrayData[0];
							message = arrayData[3];
							log.info("error : " + error + " :message: " + message + " Invoice Number : "
									+ invoiceNumber);
							if ("W".equalsIgnoreCase(error) || "S".equalsIgnoreCase(error)
									|| "I".equalsIgnoreCase(error) || "E".equalsIgnoreCase(error)) {
								warningMessage = warningMessage + "PO:" + key + " " + message + ",";
							}
						}

						if (warningMessage.length() > 2) {
							warningMessage = warningMessage.substring(0, warningMessage.length() - 1);
						}
					}
				}

				if (returnGRN != null && returnGRN != "") {
					String returnGRNForPO = null;

					for (int counterValue = 0; counterValue < newLineItemList.size(); counterValue++) {

						String fatchedValues[] = (String[]) newLineItemList.get(counterValue);
						poNumber = null;
						invoiceNumber = null;
						poLineitem = null;
						quantity = null;
						storageLocation = null;
						orderNumber = null;
						returnGRNForPO = null;
						poNumber = fatchedValues[0]; // 00010-10
						poLineitem = fatchedValues[1];
						invoiceNumber = fatchedValues[3];
						quantity = fatchedValues[4];
						storageLocation = fatchedValues[5];

						returnGRNForPO = (String) poNoMapMain.get(key);
						if (key.equalsIgnoreCase(poNumber)) {

							String actualquantityupdate = "update DELIVERYSUMMARY set ACCEPTEDQTY=? ,STORAGELOCATION=?,GRNNUMBER=? "
									+ "where INVOICENUMBER=? AND PONUMBER=? AND LINEITEMNUMBER=?";

							ps = con.prepareStatement(actualquantityupdate);
							ps.setString(1, quantity);
							ps.setString(2, storageLocation);
							ps.setString(3, returnGRNForPO);
							ps.setString(4, invoiceNumber);
							ps.setString(5, key);
							ps.setString(6, poLineitem);
							value = ps.executeUpdate();
							ps.close();
						}
					}

					String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ? "
							+ " where INVOICENUMBER = ? AND PONUMBER = ? ";

					ps = con.prepareStatement(updateGRN);
					ps.setString(1, returnGRNForPO);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, invoiceNumber);
					ps.setString(4, key);
					ps.executeUpdate();
					ps.close();

					if (("Y").equalsIgnoreCase(status)) {
						String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=? "
								+ "where INVOICENUMBER=? AND PONUMBER=?";

						ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
						ps.setString(1, enduser);
						ps.setString(2, invoiceNumber);
						ps.setString(3, key);
						value = ps.executeUpdate();
						ps.close();

						String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

						ps = con.prepareStatement(insertauditacceptqty);
						ps.setString(1, key);
						ps.setString(2, invoiceNumber);
						ps.setString(3, email);
						ps.setString(4, "Y");
						ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
						value = ps.executeUpdate();
						ps.close();

					}

				}

			} // end of map for loop
			con.commit();

			grnLIST.add(poNoMapMain);

			if (grnLIST != null && grnLIST.size() > 0) {
				String grnNums = "";
				for (int i = 0; i < grnLIST.size(); i++) {
					if (i == grnLIST.size() - 1) {
						grnNums = grnNums + grnLIST.get(i);
					} else {
						grnNums = grnNums + grnLIST.get(i) + ",";
					}
				}
				responsejson.put("grn", grnNums);
				responsejson.put("message", "Success");
				responsejson.put("warningMessage", warningMessage == null ? "" : warningMessage);
				jsonArray.add(responsejson);
			} else {

				if (!"E".equalsIgnoreCase(error)) {
					message = warningMessage;
				}
				responsejson.put("message", message);
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("getAcceptQtynGRN() :", e.fillInStackTrace());
			responsejson.put("message", "SQL Error while accepting Quantity !!");
			jsonArray.add(responsejson);
			con.rollback();
		}
		return jsonArray;
	}

	public JSONArray updateBasePO() throws SQLException, DXPortalException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		PreparedStatement ps1 = null;
		ResultSet rs1 = null;
		DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		Date date = null;
		DateFormat fmt = new SimpleDateFormat("dd-MM-yy");

		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String loadPoInvoiceQuery = "select ponumber,invoicenumber,invoicedate,businesspartneroid,mpo,allpo from poninvoicesummery order by invoicenumber";
			String updatePoInvoiceQuery = "UPDATE PONINVOICESUMMERY set BASEPO = ? where INVOICENUMBER = ? and PONUMBER=? and invoicedate = ? and businesspartneroid = ? ";
			String loadInvoiceApprovalQuery = "select DISTINCT ponumber from invoiceapproval where ponumber=? and invoicenumber=? and invoicedate=?";

			String ponumber;
			String invoicenumber;
			String invoicedate;
			String businesspartneroid;
			String mpo;
			String allpo;

			String basePo = null;
			ps = con.prepareStatement(loadPoInvoiceQuery);
			rs = ps.executeQuery();

			while (rs.next()) {

				ponumber = rs.getString("ponumber");
				invoicenumber = rs.getString("invoicenumber");
				invoicedate = rs.getString("invoicedate");
				businesspartneroid = rs.getString("businesspartneroid");
				mpo = rs.getString("mpo");
				allpo = rs.getString("allpo");
				basePo = ponumber;

				if (invoicenumber != null && !"".equals(invoicenumber) && invoicedate != null
						&& !"".equals(invoicedate)) {

					try {
						date = inputFormat.parse(invoicedate);
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
					invoicedate = fmt.format(date);

					if (mpo != null && "Y".equals(mpo)) {

						String poArray[] = null;
						if (allpo != null) {
							poArray = allpo.split(",");
						}

						if (poArray != null && poArray.length > 0) {

							for (int i = 0; i < poArray.length; i++) {

								ps1 = con.prepareStatement(loadInvoiceApprovalQuery);
								ps1.setString(1, poArray[i]);
								ps1.setString(2, invoicenumber);
								ps1.setString(3, invoicedate);
								rs1 = ps1.executeQuery();
								if (rs1.next()) {
									basePo = rs1.getString("ponumber");
									if (basePo != null && !"".equals(basePo)) {
										break;
									}
								}
								rs1.close();
								ps1.close();
							}
						}
					}
				}

				ps1 = con.prepareStatement(updatePoInvoiceQuery);
				ps1.setString(1, basePo);
				ps1.setString(2, invoicenumber);
				ps1.setString(3, ponumber);
				ps1.setString(4, invoicedate);
				ps1.setString(5, businesspartneroid);
				ps1.executeUpdate();
				ps1.close();
			}
			rs.close();
			ps.close();
			con.commit();
			responsejson.put("message", "Update successful.");
		} catch (Exception e) {
			log.error("updateBasePO() :", e.fillInStackTrace());
			con.rollback();
			responsejson.put("message", e.getLocalizedMessage());
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);

		}
		return jsonArray;
	}

	public String getVendorReturn(String invoice, String po_num, Connection con) throws SQLException {

		String response = "Success";
		PreparedStatement ps = null;
		ResultSet rs = null;
		String updatedPoNum = "";
		String poArray[] = null;
		if (po_num != null) {
			poArray = po_num.split(",");
		}

		ArrayList<String> poList = new ArrayList<String>();

		if (poArray != null && poArray.length > 0) {
			for (int i = 0; i < poArray.length; i++) {
				poList.add(poArray[i]);
			}
		} else {
			poList.add(po_num);
		}

		String archiveInsert = "INSERT INTO PONINVOICESUMMERYARCHIVE "
				+ "SELECT *  FROM PONINVOICESUMMERY where INVOICENUMBER=? and PONUMBER IN (?) ";

		String deleteInvoice = "DELETE FROM PONINVOICESUMMERY WHERE INVOICENUMBER =? and PONUMBER IN (?) ";

		String deleteStatus = "DELETE FROM DELIVERYSUMMARY WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deleteStatusInvoiceApp = " DELETE FROM INVOICEAPPROVAL WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deleteSupportingDocs = "DELETE FROM INVOICESUPPDOCS WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deletecheckboxentry = "DELETE FROM AUDIT_ACCEPTQTY WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		String deletecheckboxentryofbehalf = "DELETE FROM AUDIT_ACCEPTQTY_BEHALF WHERE INVOICENUMBER =? and PONUMBER IN (?) ";
		int value = 0;
		try {

			for (String po : poList) {
				po_num = po;
				ps = con.prepareStatement(archiveInsert);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				value = ps.executeUpdate();
				ps.close();
				ps = con.prepareStatement(deleteInvoice);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				value = ps.executeUpdate();
				ps.close();
				ps = con.prepareStatement(deleteStatus);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deleteSupportingDocs);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deletecheckboxentry);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deletecheckboxentryofbehalf);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(deleteStatusInvoiceApp);
				ps.setString(1, invoice);
				ps.setString(2, po_num);
				ps.executeUpdate();

			}

			ps.close();
			if (value > 0) {
				response = "Success";
			} else {
				response = "Vendor Return Fail";
			}
		} catch (Exception e) {
			log.error("getVendorReturn() :", e.fillInStackTrace());
			response = "Exception : " + e.getLocalizedMessage();
		} finally {
		}
		return response;
	}

	public List<SimpoDeliveryItems> createcustomdeliveryitemsforsimpo(List<String> itemList, String id,
			Connection con) {

		List<SimpoDeliveryItems> items = new ArrayList<SimpoDeliveryItems>();
		List<SimpoDeliveryItems> responseList = new ArrayList<SimpoDeliveryItems>();
		try {
			if (itemList == null || itemList.isEmpty()) {

				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setDcn("Exception : Delivery improper");
				obj.setPonumber("ERROR");
				responseList.add(obj);
				return responseList;
			}

			String[] itemListArray = itemList.toArray(new String[itemList.size()]);
			String ponumber = "";
			String lineitemnumber = "";
			String quantity = "";
			String dcn = "";

			for (int counterValue = 0; counterValue < itemList.size(); counterValue++) {
				String[] fatchedValues = itemListArray[counterValue].split(",");
				ponumber = fatchedValues[0];
				lineitemnumber = fatchedValues[1];
				quantity = fatchedValues[2];
				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setPonumber(ponumber.trim());
				obj.setLineitemnumber(lineitemnumber.trim());
				obj.setQuantity(quantity.trim());
				obj.setDcn(dcn.trim());
				items.add(obj);
			}
		} catch (Exception e) {
			SimpoDeliveryItems obj = new SimpoDeliveryItems();
			obj.setDcn("Exception : Delivery improper");
			obj.setPonumber("ERROR");
			responseList.add(obj);
			log.error("createcustomdeliveryitemsforsimpo() 1 :", e.fillInStackTrace());
			return responseList;
		}

		String result = "";
		poImpl = new POImpl();

		for (SimpoDeliveryItems item : items) {
			try {

				result = poImpl.deliverysummaryInsert(item.getPonumber(), item.getLineitemnumber(), null,
						item.getQuantity(), id, "");

				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setPonumber(item.getPonumber());
				obj.setLineitemnumber(item.getLineitemnumber());
				obj.setQuantity(item.getQuantity());
				obj.setDcn(result.trim());
				responseList.add(obj);

			} catch (DXPortalException dxp) {
				log.error("createcustomdeliveryitemsforsimpo() 2 :", dxp.fillInStackTrace());
				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setDcn("Exception : Delivery improper");
				obj.setPonumber("ERROR");
				responseList.add(obj);
				return responseList;

			} catch (Exception e) {
				log.error("createcustomdeliveryitemsforsimpo() 3 :", e.fillInStackTrace());
				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setDcn("Exception : Delivery improper");
				obj.setPonumber("ERROR");
				responseList.add(obj);
				return responseList;
			}
		}
		if (!responseList.isEmpty()) {
		}
		return responseList;
	}

	public JSONArray getorderhavingdcnforsimpo(String bid, List<String> itemList, Connection con) {

		List<SimpoDeliveryItems> items = new ArrayList<SimpoDeliveryItems>();
		try {
			if (itemList == null || itemList.isEmpty()) {
				responsejson.put("text", "Delivery improper");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Delivery improper");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			String[] itemListArray = itemList.toArray(new String[itemList.size()]);
			String ponumber = "";
			String lineitemnumber = "";
			String quantity = "";
			String dcn = "";

			for (int counterValue = 0; counterValue < itemList.size(); counterValue++) {
				String[] fatchedValues = itemListArray[counterValue].split(",");
				ponumber = fatchedValues[0];
				dcn = fatchedValues[1];

				SimpoDeliveryItems obj = new SimpoDeliveryItems();
				obj.setPonumber(ponumber.trim());
				obj.setLineitemnumber(lineitemnumber.trim());
				obj.setQuantity(quantity.trim());
				obj.setDcn(dcn.trim());
				items.add(obj);
			}
		} catch (Exception e) {
			responsejson.put("text", "Delivery improper!");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Delivery improper!");
			jsonArray.add(responsejson);
			return jsonArray;
		}

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

		for (SimpoDeliveryItems item : items) {
			result = Validation.StringChecknull(item.getPonumber());
			if (!result) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "PO number value is missing");
				jsonArray.add(responsejson);
				return jsonArray;
			}

			if (!Validation.StringChecknull(item.getDcn())) {
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "DCN value is missing!");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String orderitemquery = "select * from DELIVERYSUMMARY where BUSSINESSPARTNEROID = ? and PONUMBER = ? and DC = ? ";
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DBConnection.getConnection();
			for (SimpoDeliveryItems item : items) {

				ps = con.prepareStatement(orderitemquery);
				ps.setString(1, bid);
				ps.setString(2, item.getPonumber().trim());
				ps.setString(3, item.getDcn().trim());
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
					if (!t.equals("")) {
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
			log.error("getorderhavingdcnforsimpo() :", e.fillInStackTrace());
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray insertSimpoInvoice(List<Invoicesubmission> persons, String id, String vendorEmail)
			throws SQLException {

		if (persons != null && !persons.isEmpty()) {
			if ("reopen".equalsIgnoreCase(persons.get(0).getType())) {

				SimpoImpl simpoimpl = new SimpoImpl();
				try {
					for (Invoicesubmission obj : persons) {
						obj.setContactPerson(vendorEmail);
					}
					return simpoimpl.updateReopenedInvoice(persons);
				} catch (SQLException e) {
					log.error("insertSimpoInvoice() 1 : ", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "SQLException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} catch (DXPortalException e) {
					log.error("insertSimpoInvoice() 2 : ", e.fillInStackTrace());
					responsejson.put("Uniquemessage", "DXPortalException occured.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				} catch (ParseException e) {
					log.error("insertSimpoInvoice() 3 : ", e.fillInStackTrace());
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
		poImpl = new POImpl();
		boolean validation = poImpl.validateSubmitInvoice(persons);
		if (validation == false) {
			jsonArray = poImpl.jsonArray;
			poImpl.deletebaddeliveries(persons, con);
			return jsonArray;
		}

		int counterSupp = 0;
		String poNum = "";
		String invNum = "";
		LinkedHashSet<String> poNumHs = new LinkedHashSet<String>();
		ArrayList<ArrayList<Invoicesubmission>> poWiseInvSubList = new ArrayList<ArrayList<Invoicesubmission>>();
		boolean approvalFlag = false;
		String allPo = "";

		String beforesubmissioninvoicenumber = "";

		log.info("Normal Validation Checked : ");

		try {

			int pocount = 0;
			int poincount = 0;
			double balance = 0;

			for (int i = 0; i < persons.size(); i++) {

				poNum = persons.get(i).getPo_num();
				invNum = persons.get(i).getInvoiceNumber();
				poNumHs.add(poNum);

				if (persons.get(i).getGrnnumber().equalsIgnoreCase("-")) {
					balance = poImpl.getBalanceCount(persons.get(i).getPo_num(), persons.get(i).getLineItemNumber(),
							persons.get(i).getBid(), con);
					if (balance < Double.parseDouble(persons.get(i).getQuantity())) {
						List<Invoicesubmission> invSubLis = getPoWiseInvoiceSubmissionDetails(persons,
								persons.get(i).getPo_num());
						poImpl.updategrnstatus(invSubLis, "B", con);
						responsejson.put("Uniquemessage", "Insufficient balance to create invoice");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						poImpl.deletebaddeliveries(persons, con);
						return jsonArray;
					}
				}

				if (persons.get(i).getGrnnumber().equalsIgnoreCase("-")) {
					boolean alreadypresent = false;

					boolean checkValidationFlag = true;
					if (persons.get(i).getType() != null && "resubmit".equals(persons.get(i).getType())) {
						if (persons.get(i).getInvoiceNumber().equals(persons.get(i).getPrevinvno())) {
							checkValidationFlag = false;
						}
					}

					if (checkValidationFlag) {
						int checkinvoiceingrn = checksimpoinvoiceingrntable(persons.get(i), con);
						if (checkinvoiceingrn > 0) {
							alreadypresent = true;
						}
						if (alreadypresent) {
							responsejson.put("Uniquemessage",
									"INVOICENUMBER is already present in to-be-invoice list. Please  use a different invoice number.");
							responsejson.put("message", "Fail");
							jsonArray.add(responsejson);
							poImpl.deletebaddeliveries(persons, con);
							return jsonArray;
						}
					}
				}

				boolean checkValidationFlag = true;
				if (persons.get(i).getType() != null && "resubmit".equals(persons.get(i).getType())) {
					if (persons.get(i).getInvoiceNumber().equals(persons.get(i).getPrevinvno())) {
						checkValidationFlag = false;
					}
				}
				log.info("getUniquePONInCheck : " + checkValidationFlag);
				if (checkValidationFlag) {

					poincount = poImpl.getUniquePONInCheck(persons.get(i).getInvoiceNumber(), con,
							persons.get(i).getBid(), persons.get(i).getInvoiceDate());
					log.info("poincount : " + poincount);
					if (poincount > 0) {
						grnerror = true;
						poImpl.deletebaddeliveries(persons, con);
						responsejson.put("Uniquemessage",
								"Invoice number already exists. Please  use a different invoice number");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);

						log.info(jsonArray);
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
						poImpl.deletebaddeliveries(persons, con);
						responsejson.put("Uniquemessage", "GRNNUMBER already present");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						return jsonArray;
					}
				} else if (persons.get(i).getActualfilename() == null
						|| persons.get(i).getActualfilename().equals("null")
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
			log.error("insertSimpoInvoice() 1 :", e.fillInStackTrace());
			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", poNum);
			responsejson.put("invoicenumber", invNum);
			jsonArray.add(responsejson);
			con.rollback();
			return jsonArray;
		}

		String type = persons.get(0).getType();
		String previousInvNum = persons.get(0).getPrevinvno();
		String previouseSelectedPO = persons.get(0).getPrevponos();

		List<String> listOfItemsForDeliveryCreation = new ArrayList<String>();

		/**
		 * NEW LOGIC FOR CREATING DELIVERY SUMMERRY
		 */
		List<SimpoDeliveryItems> responseList = new ArrayList<SimpoDeliveryItems>();
		try {

			for (Invoicesubmission item : persons) {
				String dcNum = poImpl.deliverysummaryInsert(item.getPo_num(), item.getLineItemNumber(), null,
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
			responsejson.put("ponumber", poNum);
			responsejson.put("invoicenumber", invNum);
			jsonArray.add(responsejson);
			con.rollback();
			return jsonArray;

		}

		String basePO = null;
		for (String po : poNumHs) {
			ArrayList<Invoicesubmission> invSubLis = new ArrayList<Invoicesubmission>();
			invSubLis = getPoWiseInvoiceSubmissionDetails(persons, po);
			poWiseInvSubList.add(invSubLis);

			allPo = allPo + po + ",";

			if (basePO == null) {
				basePO = po;
			}
			beforesubmissioninvoicenumber = persons.get(0).getBeforesubmissioninvoicenumber();
			for (Invoicesubmission obj : invSubLis) {
				String item = obj.getPo_num() + "," + obj.getLineItemNumber() + "," + obj.getQuantity();
				listOfItemsForDeliveryCreation.add(item);
			}
		}
		allPo = allPo.substring(0, allPo.length() - 1);

		if (type != null && type.equals("resubmit")) {

			if (previousInvNum != null && !"".equals(previousInvNum) && !"undefined".equals(previousInvNum)
					&& !"null".equals(previousInvNum) && previouseSelectedPO != null && !"".equals(previouseSelectedPO)
					&& !"undefined".equals(previouseSelectedPO) && !"null".equals(previouseSelectedPO)) {
				String response1 = getVendorReturn(previousInvNum, previouseSelectedPO, con);

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
		 * OLD LOGIN FOR CREATING DELIVERY SUMMERRY
		 * 
		 * List<SimpoDeliveryItems> responseList = new ArrayList<SimpoDeliveryItems>();
		 * if (!listOfItemsForDeliveryCreation.isEmpty()) { responseList =
		 * createcustomdeliveryitemsforsimpo(listOfItemsForDeliveryCreation, id, con);
		 * if (!responseList.isEmpty()) {
		 * 
		 * if (responseList.get(0).getPonumber().equals("ERROR")) { con.rollback();
		 * responsejson.put("Uniquemessage", responseList.get(0).getDcn());
		 * responsejson.put("message", "Fail"); responsejson.put("error",
		 * responseList.get(0).getDcn()); jsonArray.add(responsejson); return jsonArray;
		 * }
		 * 
		 * HashSet<String> keyToValidate=new HashSet<>();
		 * 
		 * for (SimpoDeliveryItems obj : responseList) {
		 * 
		 * log.info("RESPONSE PO NO. : "+obj.getPonumber()+" LINE ITEM NO. :
		 * "+obj.getLineitemnumber()+" QTY : "+obj.getQuantity()+" DCN :
		 * "+obj.getDcn());
		 * 
		 * for (Invoicesubmission invoice : persons) {
		 * 
		 * log.info("INVOICE PO NO. : "+invoice.getPo_num()+" LINE ITEM NO. :
		 * "+invoice.getLineItemNumber()+" QTY : "+invoice.getQuantity()+" DCN :
		 * "+invoice.getDcnumber());
		 * 
		 * if (obj.getPonumber().equals(invoice.getPo_num()) &&
		 * obj.getLineitemnumber().equals(invoice.getLineItemNumber())) {
		 * 
		 * //
		 * if(Double.parseDouble(obj.getQuantity().trim())==Double.parseDouble(invoice.getQuantity()))
		 * {
		 * 
		 * String
		 * key=invoice.getPo_num()+invoice.getLineItemNumber()+invoice.getQuantity()+invoice.getDcnumber();
		 * 
		 * if(!keyToValidate.contains(key)) { log.info(key);
		 * invoice.setQuantity(obj.getQuantity()); invoice.setOrderNumber(obj.getDcn());
		 * keyToValidate.add(key);
		 * 
		 * break; } // }
		 * 
		 * } } } } }
		 * 
		 **/

		try {

			for (List<Invoicesubmission> invoiceSubmission : poWiseInvSubList) {

				poNum = invoiceSubmission.get(0).getPo_num();
				invNum = invoiceSubmission.get(0).getInvoiceNumber();
				String marterialCode = "";
				String deliveryUniqueNoString = null;
				double totalquantity = 0;
				StringBuffer grnnumber = new StringBuffer();
				StringBuffer srcnnumber = new StringBuffer();
				Set<String> s = new HashSet<String>();
				Set<String> s1 = new HashSet<String>();
				for (int i = 0; i < invoiceSubmission.size(); i++) {
					s.add(invoiceSubmission.get(i).getGrnnumber());
					s1.add(invoiceSubmission.get(i).getSrcnnumber());
					totalquantity = totalquantity + Double.parseDouble(invoiceSubmission.get(i).getQuantity());

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

				String querySummary = "insert into PONINVOICESUMMERY (INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,MESSAGE,"
						+ "REQUSITIONER,BUYER,AMOUNT,CREATEDON,MACOUNT,HOLDCOUNT,OVERALLSTATUS,INVOICEDATE,MATERIAL_TYPE,"
						+ "PGQ,ONEXSTATUS,ACTUALFILENAME,SAVEDFILENAME,PLANT,IRNNUMBER,"
						+ "IRNDATE,DESCRIPTION,CREATEDBY,BUSINESSPARTNERTEXT,VENDORID,BILLOFLADINGDATE,"
						+ "CONTACTPERSON,CONTACTPERSONPHONE,REMARK,TOTALAMTINCTAXES,TAXAMOUNT,GRNNUMBER,SCRNNUMBER,UNIQUEREFERENCENUMBER,MPO,ALLPO,BASEPO,NOTIFYENDUSEREMAILID)"
						+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

				ps = con.prepareStatement(querySummary);
				ps.setString(1, invoiceSubmission.get(0).getInvoiceNumber());
				ps.setString(2, invoiceSubmission.get(0).getPo_num());
				ps.setString(3, invoiceSubmission.get(0).getBid());
				ps.setString(4, "N");
				ps.setString(5, invoiceSubmission.get(0).getContactPerson());
				ps.setString(6, invoiceSubmission.get(0).getBuyerid());
				ps.setString(7, invoiceSubmission.get(0).getTotalAmount());
				ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setInt(9, 1);
				ps.setInt(10, 0);
				ps.setString(11, invoiceSubmission.get(0).getStatus());
				ps.setDate(12, new java.sql.Date(
						new SimpleDateFormat("dd/MM/yyyy").parse(invoiceSubmission.get(0).getInvoiceDate()).getTime()));
				ps.setString(13, invoiceSubmission.get(0).getMaterial());
				ps.setDouble(14, totalquantity);
				if (invoiceSubmission.get(0).getGrnnumber().equalsIgnoreCase("-")) {
					ps.setString(15, null);
				} else {
					ps.setString(15, "R");
				}
				ps.setString(16, invoiceSubmission.get(0).getActualfilename());
				ps.setString(17, invoiceSubmission.get(0).getSavedfilename());
				ps.setString(18, invoiceSubmission.get(0).getPlant());
				ps.setString(19, invoiceSubmission.get(0).getIrnNumber());
				if (invoiceSubmission.get(0).getIrnDate() != null && invoiceSubmission.get(0).getIrnDate() != ""
						&& invoiceSubmission.get(0).getIrnDate() != "null") {
					ps.setDate(20, new java.sql.Date(
							new SimpleDateFormat("dd/MM/yyyy").parse(invoiceSubmission.get(0).getIrnDate()).getTime()));
				} else {
					ps.setString(20, invoiceSubmission.get(0).getIrnDate());
				}
				ps.setString(21, invoiceSubmission.get(0).getDescription());
				ps.setString(22, invoiceSubmission.get(0).getCreatedby());
				ps.setString(23, invoiceSubmission.get(0).getBusinessPartnerText());
				ps.setString(24, invoiceSubmission.get(0).getVendorID());
				if (!("Invalid date").equalsIgnoreCase(invoiceSubmission.get(0).getBillofladingdate())) {
					ps.setDate(25, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy")
							.parse(invoiceSubmission.get(0).getBillofladingdate()).getTime()));
				} else {
					ps.setDate(25, null);
				}
				ps.setString(26, invoiceSubmission.get(0).getContactPerson());
				ps.setString(27, invoiceSubmission.get(0).getContactPersonPhone());
				ps.setString(28, invoiceSubmission.get(0).getRemark());
				ps.setString(29, invoiceSubmission.get(0).getTotalamtinctaxes());
				ps.setString(30, invoiceSubmission.get(0).getTaxamount());
				if (invoiceSubmission.get(0).getGrnnumber().equalsIgnoreCase("-")) {
					ps.setString(31, null);
				} else {
					ps.setString(31, grnnumber.toString());
				}
				if (invoiceSubmission.get(0).getSrcnnumber().equalsIgnoreCase("-")) {
					ps.setString(32, null);
				} else {
					ps.setString(32, srcnnumber.toString());
				}
				if (invoiceSubmission.get(0).getGrnnumber().equalsIgnoreCase("-")) {
					ps.setString(33, null);
				} else {
					ps.setString(33, "Y");
				}

				ps.setString(34, "Y");
				ps.setString(35, allPo);
				ps.setString(36, basePO);
				ps.setString(37, invoiceSubmission.get(0).getNotifyenduseremailiD() == null ? ""
						: invoiceSubmission.get(0).getNotifyenduseremailiD());

				ps.executeUpdate();
				ps.close();

				String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
						+ "MODIFIEDTIME,MODIFIEDBY,RESUBMITTEDINVOICENO)" + " values(?,?,?,?,?,?,?)";

				ps = con.prepareStatement(insertaudit);
				ps.setString(1, invoiceSubmission.get(0).getInvoiceNumber());
				ps.setString(2, invoiceSubmission.get(0).getPo_num());
				ps.setString(3, invoiceSubmission.get(0).getBid());
				if ("".equalsIgnoreCase(invoiceSubmission.get(0).getBeforesubmissioninvoicenumber())
						|| "null".equalsIgnoreCase(invoiceSubmission.get(0).getBeforesubmissioninvoicenumber())
						|| invoiceSubmission.get(0).getBeforesubmissioninvoicenumber() == null) {
					ps.setString(4, invoiceSubmission.get(0).getStatus());
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//					ps.setString(6, invoiceSubmission.get(0).getContactPerson());
					ps.setString(6, vendorEmail);
					ps.setString(7, invoiceSubmission.get(0).getBeforesubmissioninvoicenumber());
				} else {
					ps.setString(4, "S");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
//					ps.setString(6, invoiceSubmission.get(0).getContactPerson());
					ps.setString(6, vendorEmail);
					ps.setString(7, invoiceSubmission.get(0).getBeforesubmissioninvoicenumber());
				}
				ps.executeUpdate();
				ps.close();

				String insertsupportingdocument = "insert into INVOICESUPPDOCS (BUSINESSPARTNEROID,"
						+ "INVOICENUMBER,PONUMBER,ACTUALFILENAME,SAVEDFILENAME) values " + "(?,?,?,?,?)";

				ps = con.prepareStatement(insertsupportingdocument);
				ps.setString(1, invoiceSubmission.get(0).getBid());
				ps.setString(2, invoiceSubmission.get(0).getInvoiceNumber());
				ps.setString(3, invoiceSubmission.get(0).getPo_num());
				ps.setString(4, invoiceSubmission.get(0).getMultipleactualfilename());
				ps.setString(5, invoiceSubmission.get(0).getMultiplesavedfilename());
				ps.executeUpdate();
				ps.close();
				counterSupp++;

				for (int i = 0; i < invoiceSubmission.size(); i++) {
					deliveryUniqueNoString = invoiceSubmission.get(i).getOrderNumber();
					poImpl.updatedeliverysumary(invoiceSubmission.get(i).getPo_num(),
							invoiceSubmission.get(i).getLineItemNumber(), invoiceSubmission.get(i).getInvoiceNumber(),
							invoiceSubmission.get(i).getOrderNumber(), invoiceSubmission.get(i).getInvoiceDate(),
							invoiceSubmission.get(i).getQuantity(), invoiceSubmission.get(i).getTotalAmount(),
							invoiceSubmission.get(i).getuOM(), invoiceSubmission.get(i).getRateperquantity(),
							invoiceSubmission.get(i).getLineitemtext(), invoiceSubmission.get(i).getStatus(),
							invoiceSubmission.get(i).getInvoiceamount(), invoiceSubmission.get(i).getStoragelocation(),
							invoiceSubmission.get(i).getGrnnumber(),
							invoiceSubmission.get(i).getUniquereferencenumber(),
							invoiceSubmission.get(i).getSaplineitemnumber(),
							invoiceSubmission.get(i).getServicenumber(), con, invoiceSubmission.get(i).getSrcnnumber());

				}
				poImpl.executeUpdateBalance(con, invoiceSubmission);

				for (int i = 0; i < invoiceSubmission.size(); i++) {

					String queryPolineitem = "Select * from inventoryuserlist where MTYP = "
							+ "(select MATERIAL_TYPE from poeventdetails  where (ponumber = ? and lineitemnumber = ? "
							+ "and businesspartneroid = ? and ordernumber is null)) AND plant = (select PLANT "
							+ "from poeventdetails  where (ponumber = ? and lineitemnumber =? and businesspartneroid =? "
							+ "and ordernumber is null))";

					ps = con.prepareStatement(queryPolineitem);
					ps.setString(1, invoiceSubmission.get(i).getPo_num());
					ps.setString(2, invoiceSubmission.get(i).getLineItemNumber());
					ps.setString(3, invoiceSubmission.get(i).getBid());
					ps.setString(4, invoiceSubmission.get(i).getPo_num());
					ps.setString(5, invoiceSubmission.get(i).getLineItemNumber());
					ps.setString(6, invoiceSubmission.get(i).getBid());
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

				poImpl.getUpdateinvoiceeventdetailwopo(invoiceSubmission.get(0).getInvoiceNumber(),
						invoiceSubmission.get(0).getBid(), invoiceSubmission.get(0).getPo_num(), con);

				String status = null;
				String buyerId = null;

				if (!approvalFlag) {

					String sqlUpdate1 = "insert into invoiceapproval (VENDORID,INVOICENUMBER,PONUMBER,BUYERID,ENDUSEID,"
							+ "ENDUSERSTATUS,STAGE,MODIFIEDDATE,INVOICEDATE,STATUS,PROXY,MPO) values (?,?,?,?,?,?,?,?,?,?,?,?)";
					ps = con.prepareStatement(sqlUpdate1);
					ps.setString(1, invoiceSubmission.get(0).getVendorID());
					ps.setString(2, invoiceSubmission.get(0).getInvoiceNumber());
					ps.setString(3, invoiceSubmission.get(0).getPo_num());
					ps.setString(4, invoiceSubmission.get(0).getBuyerid());
					ps.setString(5, invoiceSubmission.get(0).getContactPerson());
					ps.setString(6, "P");
					ps.setString(7, invoiceSubmission.get(0).getStage());
					ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setDate(9, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy")
							.parse(invoiceSubmission.get(0).getInvoiceDate()).getTime()));
					ps.setString(10, "M");
					if (marterialCode != null && marterialCode != "") {
						ps.setString(11, "X");
					} else {
						ps.setString(11, null);
					}
					ps.setString(12, "Y");
					ps.executeUpdate();
					ps.close();

					approvalFlag = true;
				}

				String updategrn = "Update GRNMAPPING set STATUS=?,INVOICENUMBER=? where PONUMBER=? AND "
						+ "LINEITEMNO=? AND DCNUMBER=? AND GRNNUMBER=?";

				for (int j = 0; j < invoiceSubmission.size(); j++) {
					if (!("-").equalsIgnoreCase(invoiceSubmission.get(j).getGrnnumber())) {
						ps = con.prepareStatement(updategrn);
						ps.setString(1, "D");
						ps.setString(2, invoiceSubmission.get(j).getInvoiceNumber());
						ps.setString(3, invoiceSubmission.get(j).getPo_num());
						ps.setString(4, invoiceSubmission.get(j).getLineItemNumber());
						ps.setString(5, invoiceSubmission.get(j).getDcnumber());
						ps.setString(6, invoiceSubmission.get(j).getGrnnumber());
						ps.executeUpdate();
						ps.close();
					}
				}
			}

			con.commit();
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);

		} catch (DXPortalException dxp) {
			con.rollback();

			deletebaddeliveries(responseList);

			responsejson.put("error", dxp.reason);
			log.error(dxp.reason + " - " + dxp.subReason);
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", poNum);
			responsejson.put("invoicenumber", invNum);
			jsonArray.add(responsejson);
			log.error("insertSimpoInvoice() 2 : ", dxp.fillInStackTrace());
			return jsonArray;

		} catch (Exception e) {
			con.rollback();

			deletebaddeliveries(responseList);

			responsejson.put("error", e.getLocalizedMessage());
			responsejson.put("Uniquemessage", "SQL Error while submitting Invoice !!");
			responsejson.put("message", "SQL Error while submitting Invoice !!");
			responsejson.put("ponumber", poNum);
			responsejson.put("invoicenumber", invNum);
			jsonArray.add(responsejson);
			log.error("insertSimpoInvoice() 3 : ", e.fillInStackTrace());
			return jsonArray;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public void deletebaddeliveries(List<SimpoDeliveryItems> persons) {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			con = DBConnection.getConnection();
			String deletebaddelivery = "DELETE FROM DELIVERYSUMMARY WHERE  PONUMBER=? and DC=?";
			for (int i = 0; i < persons.size(); i++) {
				ps = con.prepareStatement(deletebaddelivery);
				ps.setString(1, persons.get(i).getPonumber());
				ps.setString(2, persons.get(i).getDcn());
				ps.executeUpdate();
				ps.close();
			}
		} catch (Exception e) {
			log.error("deletebaddeliveries() :", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

	}

	public JSONArray updateReopenedInvoice(List<Invoicesubmission> persons)
			throws SQLException, DXPortalException, ParseException {

		boolean validation = validateSubmitInvoice(persons);
		if (validation == false) {
			return jsonArray;
		}

		if (persons.get(0).getInvoicetype() != null && !persons.get(0).getInvoicetype().equals("E-Invoice")) {
			if ("Invalid date".equalsIgnoreCase(persons.get(0).getIrnDate())) {
				persons.get(0).setIrnDate(null);
			}
		}

		String prevInvNo = persons.get(0).getPrevinvno();
		String prevInvDate = persons.get(0).getPrevinvdate();
		String poNumber = persons.get(0).getPrevponos();
		String bid = persons.get(0).getBid();

		String invoiceNo = persons.get(0).getInvoiceNumber();
		String invoiceDt = persons.get(0).getInvoiceDate();
		String irnNo = persons.get(0).getIrnNumber();
		String irnDt = persons.get(0).getIrnDate();

		String invAmt = persons.get(0).getTotalAmount();
		String taxAmt = persons.get(0).getTaxamount();
		String totalAmtIncTaxes = persons.get(0).getTotalamtinctaxes();

		String invAttacActualName = persons.get(0).getActualfilename();
		String invAttacSavedName = persons.get(0).getSavedfilename();
		String invSuppDocActualName = persons.get(0).getMultipleactualfilename();
		String invSuppDocSavedName = persons.get(0).getMultiplesavedfilename();
		String invRemark = persons.get(0).getRemark();

		String response = "Success";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		con = DBConnection.getConnection();
		con.setAutoCommit(false);

		if (persons.get(0).getActualfilename() == null || persons.get(0).getActualfilename().equals("null")
				|| persons.get(0).getSavedfilename() == null || persons.get(0).getSavedfilename().equals("null")
				|| persons.get(0).getActualfilename().equals("undefined")
				|| persons.get(0).getSavedfilename().equals("undefined")) {
			responsejson.put("Uniquemessage", "Invoice file not uploaded correctly. Please try again.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		boolean checkValidationFlag = true;
		if (persons.get(0).getType() != null && "reopen".equalsIgnoreCase(persons.get(0).getType())) {
			if (persons.get(0).getInvoiceNumber().equalsIgnoreCase(persons.get(0).getPrevinvno())) {
				checkValidationFlag = false;
			}
		}
		if (checkValidationFlag) {
			int invCount = validateInvoiceNumber(persons.get(0).getInvoiceNumber(), con, persons.get(0).getBid(),
					persons.get(0).getInvoiceDate());
			if (invCount > 0) {
				responsejson.put("Uniquemessage",
						"Invoice number already exists. Please  use a different invoice number");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		log.info("PO NUMBER : " + poNumber + "  PREV INVOICE NUMBER : " + prevInvNo + " PREV INVOICE DATE : "
				+ prevInvDate);
		log.info("INVOICE NUMBER : " + prevInvNo + " INVOICE DATE : " + prevInvDate + " invAmt : " + invAmt
				+ " taxAmt : " + taxAmt + " totalAmtIncTaxes : " + totalAmtIncTaxes + " invAttacActualName : "
				+ invAttacActualName + " invAttacSavedName : " + invAttacSavedName + " invSuppDocActualName : "
				+ invSuppDocActualName + " invSuppDocSavedName : " + invSuppDocSavedName);

		String updatedPoNum = "";
		String poArray[] = null;
		if (poNumber != null) {
			poArray = poNumber.split(",");
		}

		ArrayList<String> poList = new ArrayList<String>();
		if (poArray != null && poArray.length > 0) {
			for (int i = 0; i < poArray.length; i++) {
				poList.add(poArray[i]);
			}
		} else {
			poList.add(poNumber);
		}

		String allpo = null;
		if (!poList.isEmpty()) {
			String simpoValQuery = "select allPO from poninvoicesummery where invoicenumber=? and ponumber=? and mpo is not null and mpo='Y'";

			ps = con.prepareStatement(simpoValQuery);
			ps.setString(1, prevInvNo);
			ps.setString(2, poList.get(0));
			rs = ps.executeQuery();
			while (rs.next()) {
				allpo = rs.getString("allPO");
			}
			rs.close();
			ps.close();
		}

		if (allpo != null && !"".equals(allpo)) {
			String allpoArray[] = null;
			if (allpo != null) {
				allpoArray = allpo.split(",");
			}
			ArrayList<String> allpoList = new ArrayList<String>();
			if (allpoArray != null && allpoArray.length > 0) {
				for (int i = 0; i < allpoArray.length; i++) {
					allpoList.add(allpoArray[i]);
				}
			} else {
				allpoList.add(poNumber);
			}

			if (poList.size() != allpoList.size()) {
				responsejson.put("Uniquemessage", "Please send all po numbers.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		DateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
		Date invoiceDate = null;
		try {
			invoiceDate = fmt.parse(prevInvDate);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		DateFormat fmt1 = new SimpleDateFormat("dd-MM-yy");
		prevInvDate = fmt1.format(invoiceDate);

		String updateponinvoicesummary = "UPDATE PONINVOICESUMMERY "
				+ "SET OVERALLSTATUS = ?, INVOICENUMBER = ?, INVOICEDATE = ?, "
				+ "AMOUNT = ?, TAXAMOUNT = ?, TOTALAMTINCTAXES = ?, ACTUALFILENAME = ?, SAVEDFILENAME = ?, "
				+ "IRNNUMBER = ?, IRNDATE = ? ,REMARK = ? "
				+ "WHERE PONUMBER = ? and INVOICENUMBER = ? and INVOICEDATE = TO_DATE(?, 'DD-MM-YY') and BUSINESSPARTNEROID = ? ";

		String updateinvoicesuppdocs = "UPDATE INVOICESUPPDOCS SET INVOICENUMBER = ?, ACTUALFILENAME = ?, SAVEDFILENAME = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and BUSINESSPARTNEROID = ? ";

		String updatedeliverysummary = "UPDATE DELIVERYSUMMARY SET INVOICENUMBER = ?,INVOICEDATE = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and INVOICEDATE = TO_DATE(?, 'DD-MM-YY') and BUSSINESSPARTNEROID = ? ";

		String updategrnmapping = "UPDATE GRNMAPPING  SET INVOICENUMBER = ?,INVOICEDATE = ? where PONUMBER = ? and INVOICENUMBER = ? ";

		String updateinvoiceapproval = "UPDATE INVOICEAPPROVAL SET INVOICENUMBER = ?,INVOICEDATE = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and INVOICEDATE = TO_DATE(?, 'DD-MM-YY')";

		String updateauditqtyself = "UPDATE AUDIT_ACCEPTQTY SET INVOICENUMBER = ? WHERE PONUMBER = ? and INVOICENUMBER = ? ";

		String updateauditqtybehalf = "UPDATE AUDIT_ACCEPTQTY_BEHALF SET INVOICENUMBER = ? WHERE PONUMBER = ? and INVOICENUMBER = ? ";

		String updateinvoicetracker = "UPDATE INVOICETRACKER SET INVOICENUMBER = ? WHERE PONUMBER = ? and INVOICENUMBER = ? ";

		String updatechatstatus = "UPDATE CHATSTATUS SET INVOICENUMBER = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and BUSINESSPARTNEROID = ? ";

		String updatechat = "UPDATE CHATMESSAGE SET INVOICENUMBER = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and BUSINESSPARTNEROID = ? ";

		String updateinvoicearchiv = "UPDATE PONINVOICESUMMERYARCHIVE SET INVOICENUMBER = ?,INVOICEDATE = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and INVOICEDATE = TO_DATE(?, 'DD-MM-YY') and BUSINESSPARTNEROID = ? ";

		String updatecreditnote = "UPDATE CREDITNOTE SET INVOICENUMBER = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and BUSSINESSPARTNEROID = ? ";

		String updatecreditadvicesummary = "UPDATE CREDITADVICESUMMARY SET INVOICENUMBER = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and BUSSINESSPARTNEROID = ? ";

		String updatecreditadvicedetails = "UPDATE CREDITADVICEDETAILS SET INVOICENUMBER = ? WHERE PONUMBER = ? and INVOICENUMBER = ? and BUSSINESSPARTNEROID = ? ";

		String insertIntoTracker = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
				+ "MODIFIEDTIME,MODIFIEDBY,RESUBMITTEDINVOICENO)" + " values(?,?,?,?,?,?,?)";

		int value = 0;
		try {

			for (String po_num : poList) {

				ps = con.prepareStatement(updateponinvoicesummary);
				ps.setString(1, "A");
				ps.setString(2, invoiceNo);
				ps.setDate(3, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoiceDt).getTime()));
				ps.setString(4, invAmt);
				ps.setString(5, taxAmt);
				ps.setString(6, totalAmtIncTaxes);
				ps.setString(7, invAttacActualName);
				ps.setString(8, invAttacSavedName);
				ps.setString(9, irnNo);
				if (irnDt != null) {
					ps.setDate(10, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(irnDt).getTime()));
				} else {
					ps.setDate(10, null);
				}

				ps.setString(11, invRemark);
				ps.setString(12, po_num);
				ps.setString(13, prevInvNo);
				ps.setString(14, prevInvDate);
				ps.setString(15, bid);
				value = ps.executeUpdate();
				ps.close();

				if (value == 0) {
					responsejson.put("Uniquemessage",
							"PONINVOICESUMMERY not found with given prevoius invoice number and date.");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return jsonArray;
				}

				ps = con.prepareStatement(updateinvoicesuppdocs);
				ps.setString(1, invoiceNo);
				ps.setString(2, invSuppDocActualName);
				ps.setString(3, invSuppDocSavedName);
				ps.setString(4, po_num);
				ps.setString(5, prevInvNo);
				ps.setString(6, bid);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updatedeliverysummary);
				ps.setString(1, invoiceNo);
				ps.setDate(2, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoiceDt).getTime()));
				ps.setString(3, po_num);
				ps.setString(4, prevInvNo);
				ps.setString(5, prevInvDate);
				ps.setString(6, bid);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updategrnmapping);
				ps.setString(1, invoiceNo);
				ps.setDate(2, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoiceDt).getTime()));
				ps.setString(3, po_num);
				ps.setString(4, prevInvNo);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updateinvoiceapproval);
				ps.setString(1, invoiceNo);
				ps.setDate(2, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoiceDt).getTime()));
				ps.setString(3, po_num);
				ps.setString(4, prevInvNo);
				ps.setString(5, prevInvDate);
				ps.executeUpdate();

				ps = con.prepareStatement(updateauditqtyself);
				ps.setString(1, invoiceNo);
				ps.setString(2, po_num);
				ps.setString(3, prevInvNo);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updateauditqtybehalf);
				ps.setString(1, invoiceNo);
				ps.setString(2, po_num);
				ps.setString(3, prevInvNo);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updateinvoicetracker);
				ps.setString(1, invoiceNo);
				ps.setString(2, po_num);
				ps.setString(3, prevInvNo);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updatechatstatus);
				ps.setString(1, invoiceNo);
				ps.setString(2, po_num);
				ps.setString(3, prevInvNo);
				ps.setString(4, bid);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updatechat);
				ps.setString(1, invoiceNo);
				ps.setString(2, po_num);
				ps.setString(3, prevInvNo);
				ps.setString(4, bid);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updateinvoicearchiv);
				ps.setString(1, invoiceNo);
				ps.setDate(2, new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(invoiceDt).getTime()));
				ps.setString(3, po_num);
				ps.setString(4, prevInvNo);
				ps.setString(5, prevInvDate);
				ps.setString(6, bid);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updatecreditnote);
				ps.setString(1, invoiceNo);
				ps.setString(2, po_num);
				ps.setString(3, prevInvNo);
				ps.setString(4, bid);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updatecreditadvicesummary);
				ps.setString(1, invoiceNo);
				ps.setString(2, po_num);
				ps.setString(3, prevInvNo);
				ps.setString(4, bid);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(updatecreditadvicedetails);
				ps.setString(1, invoiceNo);
				ps.setString(2, po_num);
				ps.setString(3, prevInvNo);
				ps.setString(4, bid);
				value = ps.executeUpdate();
				ps.close();

				ps = con.prepareStatement(insertIntoTracker);
				ps.setString(1, persons.get(0).getInvoiceNumber());
				ps.setString(2, persons.get(0).getPo_num());
				ps.setString(3, persons.get(0).getBid());
				ps.setString(4, "C");
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(6, persons.get(0).getContactPerson());
				ps.setString(7, persons.get(0).getPrevinvno());
				ps.executeUpdate();
				ps.close();
			}

			con.commit();
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("updateReopenedInvoice() :", e.fillInStackTrace());
			responsejson.put("message", e.getLocalizedMessage());
			jsonArray.add(responsejson);
			con.rollback();
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
			responsejson.put("Uniquemessage", "No data sent for submission.");
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return false;
		}

		for (int i = 0; i < persons.size(); i++) {

			result = Validation.StringChecknull(persons.get(i).getBid());
			if (result == false) {
				responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			} else {
				result = Validation.numberCheck(persons.get(i).getBid());
				if (result == false) {
					responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
			result = Validation.StringChecknull(persons.get(i).getInvoiceNumber());
			if (result == false) {
				responsejson.put("Uniquemessage", "Please enter invoice number.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			} else {
//				if (persons.get(i).getInvoiceNumber().contains("\'")) {
//					responsejson.put("Uniquemessage", "Invoice number should not contain single quotation mark.");
//					responsejson.put("validation", "validation Fail");
//					responsejson.put("message", "Fail");
//					jsonArray.add(responsejson);
//					return false;
//				}

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

			result = Validation.StringChecknull(persons.get(i).getInvoiceDate());
			if (result == false) {
				responsejson.put("Uniquemessage", "Please enter invoice date.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			} else {
				if (persons.get(i).getInvoiceDate().equalsIgnoreCase("Invalid date")) {
					responsejson.put("Uniquemessage", "Please enter invoice date.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}

			result = Validation.StringChecknull(persons.get(i).getTotalAmount());
			if (result == false) {
				responsejson.put("Uniquemessage", "Please enter total amount.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}

			result = Validation.StringChecknull(persons.get(i).getTaxamount());
			if (result == false) {
				responsejson.put("Uniquemessage", "Please enter tax amount.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}

			result = Validation.StringChecknull(persons.get(i).getTotalamtinctaxes());
			if (result == false) {
				responsejson.put("Uniquemessage", "Please enter total amount including taxes.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}

			result = Validation.StringChecknull(persons.get(i).getRemark());
			if (result == false) {
				responsejson.put("Uniquemessage", "Please enter remark.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}

			result = Validation.StringChecknull(persons.get(i).getType());
			if (result == false) {
				responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}

			result = Validation.StringChecknull(persons.get(i).getInvoicetype());
			if (result == false) {
				responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
				responsejson.put("validation", "validation Fail");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return false;
			}

			if (persons.get(i).getInvoicetype() != null
					&& persons.get(i).getInvoicetype().equalsIgnoreCase("E-Invoice")) {
				result = Validation.StringChecknull(persons.get(i).getIrnNumber());
				if (result == false) {
					responsejson.put("Uniquemessage", "Please enter IRN Number.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
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

				result = Validation.StringChecknull(persons.get(i).getIrnDate());
				if (result == false) {
					responsejson.put("Uniquemessage", "Please enter IRN Date.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				} else {
					if (persons.get(i).getIrnDate().equalsIgnoreCase("Invalid date")) {
						responsejson.put("Uniquemessage", "Please enter IRN date.");
						responsejson.put("validation", "validation Fail");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						return false;
					} else {
						DateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
						Date invoiceDate = null;
						try {
							invoiceDate = fmt.parse(persons.get(i).getIrnDate());
						} catch (ParseException e1) {
							log.error("validateSubmitInvoice() 1 :", e1.fillInStackTrace());
							responsejson.put("Uniquemessage", "IRN date format should be dd/MM/yyyy.");
							responsejson.put("validation", "validation Fail");
							responsejson.put("message", "Fail");
							jsonArray.add(responsejson);
							return false;
						}
					}
				}
			}

			if (persons.get(i).getType() != null && persons.get(i).getType().equalsIgnoreCase("reopen")) {
				result = Validation.StringChecknull(persons.get(i).getPrevinvno());
				if (result == false) {
					responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}

				result = Validation.StringChecknull(persons.get(i).getPrevinvdate());
				if (result == false) {
					responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				} else {
					if (persons.get(i).getPrevinvdate().equalsIgnoreCase("Invalid date")) {
						responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
						responsejson.put("validation", "validation Fail");
						responsejson.put("message", "Fail");
						jsonArray.add(responsejson);
						return false;
					} else {
						DateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
						Date invoiceDate = null;
						try {
							invoiceDate = fmt.parse(persons.get(i).getPrevinvdate());
						} catch (ParseException e1) {
							log.error("validateSubmitInvoice() 2 :", e1.fillInStackTrace());
							responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
							responsejson.put("validation", "validation Fail");
							responsejson.put("message", "Fail");
							jsonArray.add(responsejson);
							return false;
						}
					}
				}
				result = Validation.StringChecknull(persons.get(i).getPrevponos());
				if (result == false) {
					responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}

				result = Validation.StringChecknull(persons.get(i).getContactPerson());
				if (result == false) {
					responsejson.put("Uniquemessage", "Error while submitting invoice. Please try again.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
			
			if(persons.get(i).getActualfilename()!=null) {				
				result = Validation.fileExtension(persons.get(i).getActualfilename());
				if(result) {
					responsejson.put("Uniquemessage", "Only pdf file extension is allowed.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}				
			}
			if(persons.get(i).getSavedfilename()!=null) {
				result = Validation.fileExtension(persons.get(i).getSavedfilename());
				if(result) {
					responsejson.put("Uniquemessage", "Only pdf file extension is allowed.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}		
			}
			if(persons.get(i).getMultipleactualfilename()!=null) {
				result = Validation.multiFilesExtension(persons.get(i).getMultipleactualfilename());
				if(result) {
					responsejson.put("Uniquemessage", "Only JPEG, JPG, PNG, DOC, DOCX, XLS, XLSX, CSV, PDF file extensions are allowed.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
			if(persons.get(i).getMultiplesavedfilename()!=null) {
				result = Validation.multiFilesExtension(persons.get(i).getMultiplesavedfilename());
				if(result) {
					responsejson.put("Uniquemessage", "Only JPEG, JPG, PNG, DOC, DOCX, XLS, XLSX, CSV, PDF file extensions are allowed.");
					responsejson.put("validation", "validation Fail");
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
					return false;
				}
			}
			
			

		}
		return true;
	}

	public int validateInvoiceNumber(String invoice, Connection con, String bid, String invoicedate)
			throws SQLException, DXPortalException {
		String validateInvoiceNumber = "Select count(*) as counter from poninvoicesummery where BUSINESSPARTNEROID = ? "
				+ "and lower(INVOICENUMBER) =lower(?)";
		PreparedStatement ps = null;
		ResultSet rs = null;
		int count = 0;
		try {
			ps = con.prepareStatement(validateInvoiceNumber);
			ps.setString(1, bid);
			ps.setString(2, invoice);
			rs = ps.executeQuery();
			while (rs.next()) {
				count = rs.getInt("counter");
			}
			rs.close();
			ps.close();

		} catch (Exception e) {
			log.error("validateInvoiceNumber() :", e.fillInStackTrace());
			count = 0;
			throw new DXPortalException("Error in Invoice Submission !!", "SQL Error in getUniquePONInCheck.");
		}
		return count;
	}

	public JSONArray getgrnbasedonpo(String id, String ponumber) throws SQLException {

		String getgrnbasedonpo = "select distinct DCNUMBER,ponumber from grnmapping where STATUS IS NULL and ponumber in ( "
				+ "select distinct a.ponumber from poeventdetails a,podetails b where a.ponumber=b.ponumber and  a.businesspartneroid = ? "
				+ "and a.plant=( select distinct plant from poeventdetails where ponumber = ? ) "
				+ "and b.potype=( select distinct potype from podetails where ponumber = ? ) ) "
				+ "and (select distinct ponumber from grnmapping where STATUS IS NULL and ponumber= ? ) is not null "
				+ "order by dcnumber";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int bCount = 0;
			con = DBConnection.getConnection();

			ps = con.prepareStatement(getgrnbasedonpo);
			ps.setString(1, id);
			ps.setString(2, ponumber);
			ps.setString(3, ponumber);
			ps.setString(4, ponumber);
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("DCNUMBER", rs.getString("DCNUMBER"));
				poData.put("ponumber", rs.getString("ponumber"));
				poData.put("BASEPO", ponumber);
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

	public JSONArray getgrnbasedoninvoiceandpo(String id, List<String> dcnumber, String basepo) {

		String poNum = null;
		String dcnNum = null;
		boolean validationFlag = true;

		if (dcnumber == null || dcnumber.isEmpty()) {
			validationFlag = false;
		}

		String[] dcnArrayList = dcnumber.toArray(new String[dcnumber.size()]);

		if (dcnArrayList == null) {
			validationFlag = false;
		}

		for (String dcn : dcnArrayList) {
			String[] dcnArray = dcn.split(",");
			if (dcnArray == null || dcnArray.length != 2) {
				validationFlag = false;
			}
		}

		if (!validationFlag) {
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

//		String getgrnbasedonpo = "select * from grnmapping where ponumber = ? and dcnumber = ? AND STATUS IS NULL";

		String getgrnbasedonpo = "select a.PONUMBER, a.GRNNUMBER, a.LINEITEMNO, b.lineitemtext,a.DCNUMBER, a.INVOICENUMBER, a.STATUS, a.CREATEDON, a.AMOUNT, a.INVOICEDATE, "
				+ "a.SAPUNIQUEREFERENCENO, a.SAPLINEITEMNO, a.SERVICENO, a.SRCNO, a.RATEPERQTY, a.GRNQTY,b.balance_qty,b.quantity "
				+ "from grnmapping a ,poeventdetails b "
				+ "where a.ponumber=b.ponumber and a.lineitemno=b.lineitemnumber and "
				+ "a.ponumber = ? and a.dcnumber = ? AND a.STATUS IS NULL";

//		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();

		LinkedHashMap<String, Object> poWiseMap = new LinkedHashMap<String, Object>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int bCount = 0;
			con = DBConnection.getConnection();

			for (String dcn : dcnArrayList) {

				ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();

				String[] dcnArray = dcn.split(",");
				poNum = dcnArray[0];
				dcnNum = dcnArray[1];

				ps = con.prepareStatement(getgrnbasedonpo);
				ps.setString(1, poNum);
				ps.setString(2, dcnNum);
				rs = ps.executeQuery();

				while (rs.next()) {
					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PONUMBER", rs.getString("PONUMBER"));
					poData.put("GRNNUMBER", rs.getString("GRNNUMBER"));
					poData.put("GRNQTY", rs.getString("GRNQTY"));
					poData.put("LINEITEMNO", rs.getString("LINEITEMNO"));
					poData.put("lineitemtext", rs.getString("lineitemtext"));
					poData.put("balance_qty", rs.getString("balance_qty"));
					poData.put("quantity", rs.getString("quantity"));
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

				if (poWiseMap.isEmpty()) {
					poWiseMap.put(poNum, POList);
				} else {
					if (poWiseMap.containsKey(poNum)) {
						ArrayList<HashMap<String, String>> POList11 = (ArrayList<HashMap<String, String>>) poWiseMap
								.get(poNum);
						POList11.addAll(POList);
						poWiseMap.put(poNum, POList11);
					} else {
						poWiseMap.put(poNum, POList);
					}
				}

			}

			ArrayList<HashMap<String, Object>> POList1 = new ArrayList<HashMap<String, Object>>();
			HashMap<String, Object> poData = new HashMap<String, Object>();
			poData.put("PONUMBER", basepo);
			poData.put("LINEITEMS", poWiseMap.get(basepo));
			POList1.add(poData);

			poWiseMap.remove(basepo);

			for (Map.Entry<String, Object> entry : poWiseMap.entrySet()) {

				poData = new HashMap<String, Object>();
				poData.put("PONUMBER", entry.getKey());
				poData.put("LINEITEMS", entry.getValue());
				POList1.add(poData);

			}

			rs.close();
			ps.close();
//			if (POList.size() > 0) {
//				responsejson.put("message", "Success");
//				responsejson.put("grnbasedonpoandinvoice", POList);
//				jsonArray.add(responsejson);
//			} else {
//				responsejson.put("message", "Empty");
//				jsonArray.add(responsejson);
//			}

			if (POList1.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("grnbasedonpoandinvoice", POList1);
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

	public JSONArray getInvalidInvoiceDetails(String bid, int nPage, String status, String invno, String pono,
			String fdate, String tdate, String plant, String companyCode) {

		InvoiceDetailsImpl invImpl = new InvoiceDetailsImpl();
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
			responsejson.put("validation", "Validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String compCodeJoinQuery = " join podetails pod on pis.ponumber = pod.ponumber ";
		String compCodeQuery = " AND pod.companycode = ? ";

		// validation is pending
		String basePoQuery = " and pis.ponumber=ia.ponumber ";
		boolean basePoFlag = false;

		// For WOPO
//		String wopoQ = "SELECT * FROM INVOICEEVENTDETAILWOPO where BUSSINESSPARTNEROID =? and PONUMBER is NULL and STATUS <> 'A' "
//				+ "ORDER BY createdon desc";

		String wopoQ = "SELECT * FROM INVOICEEVENTDETAILWOPO where BUSSINESSPARTNEROID =? and PONUMBER is NULL and STATUS = 'INV' "
				+ "ORDER BY createdon desc";

		// For all filters except Offline Invoices and ALL and Pending .
		String qdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
				+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
				+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
				+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
				+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS "
				+ compCodeJoinQuery + " WHERE " + "PIS.BUSINESSPARTNEROID = ? AND PIS.OVERALLSTATUS= ? AND "
				+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery + "Union "
				+ "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
				+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
				+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
				+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
				+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
				+ "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber " + compCodeJoinQuery
				+ " WHERE " + "PIS.BUSINESSPARTNEROID = ? AND PIS.OVERALLSTATUS= ? AND "
				+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery + "ORDER BY CREATEDON DESC";

		/**
		 * 
		 * // For ALL filter
		 * 
		 * String alldata = "SELECT
		 * PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
		 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
		 * +
		 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
		 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
		 * + "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM
		 * PONINVOICESUMMERY PIS WHERE " + "PIS.BUSINESSPARTNEROID = ? AND " +
		 * "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + "Union " + "SELECT
		 * distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
		 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
		 * +
		 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
		 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
		 * + "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM
		 * PONINVOICESUMMERY PIS join invoiceapproval ia " + "ON PIS.invoicenumber =
		 * ia.invoicenumber and PIS.ponumber=ia.ponumber WHERE " +
		 * "PIS.BUSINESSPARTNEROID = ? AND " + "PIS.INVOICENUMBER IS NOT NULL AND
		 * PIS.MPO = 'Y' " + "ORDER BY CREATEDON DESC";
		 * 
		 * // For OFFLINE INVOICES
		 * 
		 * String hdata = "SELECT
		 * PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
		 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
		 * +
		 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
		 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
		 * + "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM
		 * PONINVOICESUMMERY PIS WHERE " + "PIS.BUSINESSPARTNEROID = ? and ONEXSTATUS=?
		 * AND " + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + "Union " +
		 * "SELECT distinct
		 * PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
		 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
		 * +
		 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
		 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
		 * + "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM
		 * PONINVOICESUMMERY PIS join invoiceapproval ia " + "ON PIS.invoicenumber =
		 * ia.invoicenumber and PIS.ponumber=ia.ponumber WHERE " +
		 * "PIS.BUSINESSPARTNEROID = ? and ONEXSTATUS=? AND " + "PIS.INVOICENUMBER IS
		 * NOT NULL AND PIS.MPO = 'Y' " + "ORDER BY CREATEDON DESC";
		 * 
		 * String pdata = "SELECT
		 * PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
		 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
		 * +
		 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
		 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
		 * + "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM
		 * PONINVOICESUMMERY PIS WHERE " + "PIS.BUSINESSPARTNEROID = ? AND
		 * (PIS.OVERALLSTATUS= ? OR PIS.OVERALLSTATUS= ?) AND " + "PIS.INVOICENUMBER IS
		 * NOT NULL AND PIS.MPO IS NULL " + "Union " + "SELECT distinct
		 * PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON," +
		 * "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
		 * +
		 * "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
		 * + "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
		 * + "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO FROM
		 * PONINVOICESUMMERY PIS join invoiceapproval ia " + "ON PIS.invoicenumber =
		 * ia.invoicenumber and PIS.ponumber=ia.ponumber WHERE " +
		 * "PIS.BUSINESSPARTNEROID = ? AND (PIS.OVERALLSTATUS= ? OR PIS.OVERALLSTATUS=
		 * ?) AND " + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + "ORDER BY
		 * CREATEDON DESC";
		 * 
		 **/

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

					if (!"INV".equalsIgnoreCase(status)) {
						status = "INV";
					}

					ArrayList<String> param = new ArrayList<String>();
					param.add(bid);
					Pagination pg = null;

//					if (status.equalsIgnoreCase("H")) {
//						param.add(status);
//						param.add(bid);
//						param.add(status);
//						pg = new Pagination(hdata, nPage);
//					} else if (status.equalsIgnoreCase("ALL")) {
//						param.add(bid);
//						pg = new Pagination(alldata, nPage);
//					} else if (status.equalsIgnoreCase("P")) {
//						param.add(status);
//						param.add("M");
//						param.add(bid);
//						param.add(status);
//						param.add("M");
//						pg = new Pagination(pdata, nPage);
//					} else if (status.equalsIgnoreCase("V")) {
//						param.add(status);
//						param.add("RO");
//						param.add(bid);
//						param.add(status);
//						param.add("RO");
//						pg = new Pagination(pdata, nPage);
//					} else {
					param.add(status);
					param.add(companyCode);
					param.add(bid);
					param.add(status);
					param.add(companyCode);
					log.info("qdata--:" + qdata);
					pg = new Pagination(qdata, nPage);
//					}
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
					String invNumber = null;
					String invDate = null;
					String mPO = null;
					String bpid = null;
					int count = 0;

//					log.info("QUERY : "+qdata);

					while (rs.next()) {
						count++;
						HashMap<String, String> invoiceData = new HashMap<String, String>();
						invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
						invNumber = rs.getString("INVOICENUMBER");
						invoiceData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
						invDate = rs.getString("INVOICEDATE");
						invoiceData.put("PO_NUMBER", rs.getString("PONUMBER"));
						invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
						invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
						invoiceData.put("VENDORID", rs.getString("VENDORID"));
						invoiceData.put("PLANT", rs.getString("PLANT"));
						POImpl po = new POImpl();
						invoiceData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
						invoiceData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
						bpid = rs.getString("BUSINESSPARTNEROID");
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
						invoiceData.put("MPO", rs.getString("MPO"));
						invoiceData.put("ALLPO", rs.getString("ALLPO"));
						invoiceList.add(invoiceData);
					}
					pg.close();
					rs.close();
					pg = null;
				}

				if (status.equalsIgnoreCase("WOPO")) {
					ArrayList<String> param1 = new ArrayList<String>();
					param1.add(bid);
					Pagination pg1 = new Pagination(wopoQ, nPage);
					log.info("WOPO QUERY : " + wopoQ);
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
						basePoQuery = " and pis.basepo=ia.ponumber ";
						basePoFlag = true;
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
					param.add(companyCode);

					param.add(bid);
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}
					param.add(companyCode);

					advqdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
							+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
							+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
							+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
							+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO,PIS.BASEPO FROM PONINVOICESUMMERY PIS "
							+ compCodeJoinQuery + " WHERE PIS.OVERALLSTATUS = 'INV' AND "
							+ "PIS.BUSINESSPARTNEROID = ? " + subquery + " AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery + "Union "
							+ "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
							+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
							+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
							+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
							+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO,PIS.BASEPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
							+ "ON PIS.invoicenumber = ia.invoicenumber " + basePoQuery + compCodeJoinQuery
							+ " WHERE PIS.OVERALLSTATUS = 'INV' AND " + "PIS.BUSINESSPARTNEROID = ? " + subquery
							+ " AND " + "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
							+ "ORDER BY CREATEDON DESC";

				} else if (status.equalsIgnoreCase("ASWP")) {

					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND PIS.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " and pis.basepo=ia.ponumber ";
						basePoFlag = true;
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
					param.add(companyCode);

					param.add(bid);
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}
					param.add(companyCode);

					advqdata = "SELECT PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
							+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
							+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
							+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
							+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO,PIS.BASEPO FROM PONINVOICESUMMERY PIS "
							+ compCodeJoinQuery + " WHERE PIS.OVERALLSTATUS = 'INV' AND "
							+ "PIS.BUSINESSPARTNEROID = ? AND PIS.CREDITADVICENO IS NOT NULL " + subquery + " AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO IS NULL " + compCodeQuery + "Union "
							+ "SELECT distinct PIS.INVOICENUMBER,PIS.INVOICEDATE,PIS.PONUMBER,PIS.CONTACTPERSON,"
							+ "PIS.CONTACTPERSONPHONE,PIS.VENDORID,PIS.PLANT,PIS.BUSINESSPARTNEROID,PIS.CREATEDBY,"
							+ "PIS.CREATEDON,PIS.AMOUNT,PIS.PAYMENTAMOUNT,PIS.DESCRIPTION,PIS.OVERALLSTATUS,"
							+ "PIS.ACTUALFILENAME,PIS.SAVEDFILENAME,PIS.CREDITNOTENO,PIS.CREDITADVICENO,"
							+ "PIS.TOTALAMTINCTAXES,PIS.TAXAMOUNT, PIS.MPO,PIS.ALLPO,PIS.BASEPO FROM PONINVOICESUMMERY PIS join invoiceapproval ia "
							+ "ON PIS.invoicenumber = ia.invoicenumber and PIS.ponumber=ia.ponumber "
							+ compCodeJoinQuery + " WHERE PIS.OVERALLSTATUS = 'INV' AND "
							+ "PIS.BUSINESSPARTNEROID = ? AND PIS.CREDITADVICENO IS NOT NULL " + subquery + " AND "
							+ "PIS.INVOICENUMBER IS NOT NULL AND PIS.MPO = 'Y' " + compCodeQuery
							+ "ORDER BY CREATEDON DESC";

				}

				log.info("AS QUERY : " + advqdata);

				pg = new Pagination(advqdata, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				if (status.equalsIgnoreCase("ASWP")) {

				} else {
					String invNumber = null;
					String invDate = null;
					String mPO = null;
					String bpid = null;
					while (rs.next()) {
						HashMap<String, String> invoiceData = new HashMap<String, String>();

						String poNum = rs.getString("PONUMBER");
						if (basePoFlag && rs.getString("BASEPO") != null) {
							poNum = rs.getString("BASEPO");
						}

						invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
						invNumber = rs.getString("INVOICENUMBER");
						invoiceData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
						invDate = rs.getString("INVOICEDATE");
						invoiceData.put("PO_NUMBER", poNum);
						invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
						invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
						invoiceData.put("VENDORID", rs.getString("VENDORID"));
						invoiceData.put("PLANT", rs.getString("PLANT"));
						POImpl po = new POImpl();
						invoiceData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
						invoiceData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
						bpid = rs.getString("BUSINESSPARTNEROID");
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
						invoiceData.put("MPO", rs.getString("MPO"));
						invoiceData.put("ALLPO", rs.getString("ALLPO"));
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

//				String advqdata1 = "SELECT * FROM INVOICEEVENTDETAILWOPO PIS where PIS.BUSSINESSPARTNEROID =? "
//						+ "and PIS.PONUMBER is NULL " + subquery1
//						+ " AND PIS.STATUS <> 'A' ORDER BY PIS.createdon desc";
				String advqdata1 = "SELECT * FROM INVOICEEVENTDETAILWOPO PIS where PIS.BUSSINESSPARTNEROID =? "
						+ "and PIS.PONUMBER is NULL " + subquery1
						+ " AND PIS.STATUS = 'INV' ORDER BY PIS.createdon desc";

				log.info("AS - WOPO : " + advqdata1);

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
				getInvoiceDetailsCountAsPerStatus(bid, nPage, "INV", invno, pono, fdate, tdate, plant, con, ps, rs,
						companyCode);
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

	public JSONArray getInvalidInvoiceDetailsForInternalUser(String emailid, int nPage, String status, String invno,
			String pono, String fdate, String tdate, String plant, String vendor, String mode) throws SQLException {

		String userQuery = "";
		if (mode.equalsIgnoreCase("buyer")) {
			userQuery = "AND BUYER = ? ";
		} else if (mode.equalsIgnoreCase("enduser")) {
			userQuery = "AND REQUSITIONER = ? ";
		}

		String basePoQuery = " and ps.ponumber=ia.ponumber ";
		boolean basePoFlag = false;

		/**
		 * // For pending QUANTITY String pendingsql = "select
		 * ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID," +
		 * "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT," +
		 * "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT," +
		 * "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME," +
		 * "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,
		 * ps.TAXAMOUNT," + "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
		 * + "from PONINVOICESUMMERY ps where BUYER =? " + "AND ps.invoicenumber is not
		 * null AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "AND (OVERALLSTATUS=? OR
		 * OVERALLSTATUS=?) " + "UNION " + "select
		 * ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID," +
		 * "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT," +
		 * "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT," +
		 * "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME," +
		 * "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,
		 * ps.TAXAMOUNT," + "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
		 * + "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber =
		 * ia.invoicenumber " + "and ps.ponumber=ia.ponumber where BUYER =? " + "AND
		 * ps.invoicenumber is not null AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' AND " +
		 * " (OVERALLSTATUS=? OR OVERALLSTATUS=?) " + "order by CREATEDON desc";
		 * 
		 * // For OFFLINE INVOICES String hdata = "select
		 * ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID," +
		 * "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT," +
		 * "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT," +
		 * "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME," +
		 * "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,
		 * ps.TAXAMOUNT," + "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
		 * + "from PONINVOICESUMMERY ps where" + " ps.invoicenumber is not null AND
		 * BUYER =? AND ONEXSTATUS=? ps.ALLPO IS NULL AND ps.MPO IS NULL " + " UNION " +
		 * "select
		 * ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID," +
		 * "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT," +
		 * "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT," +
		 * "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME," +
		 * "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,
		 * ps.TAXAMOUNT," + "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
		 * + "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber =
		 * ia.invoicenumber " + "and ps.ponumber=ia.ponumber where " + "ps.invoicenumber
		 * is not null and BUYER =? AND ONEXSTATUS=? " + "AND ps.ALLPO IS NOT NULL AND
		 * ps.MPO ='Y' " + "order by CREATEDON desc";
		 * 
		 **/
		// All Filter
		String sql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO " + "from PONINVOICESUMMERY ps where  "
				+ " ps.invoicenumber is not null " + userQuery + " AND OVERALLSTATUS = ? "
				// + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL "
				+ "UNION " + "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null " + userQuery
				+ " AND OVERALLSTATUS = ? "
				// + "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' "
				+ "order by CREATEDON desc";

		// For Short quantity
		String sdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
				+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND OVERALLSTATUS = 'INV' " + userQuery
				+ " "
				// + "AND ps.CREDITADVICENO IS NOT NULL AND ps.ALLPO IS NULL AND ps.MPO IS NULL
				// "
				+ " UNION " + "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO   "
				+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
				+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND OVERALLSTATUS = 'INV' "
				+ userQuery
				// +" AND ps.CREDITADVICENO IS NOT NULL AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y'
				// "
				+ " order by CREATEDON desc";

		/**
		 * // All Status String alldata = "select
		 * ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID," +
		 * "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT," +
		 * "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT," +
		 * "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME," +
		 * "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,
		 * ps.TAXAMOUNT," + "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
		 * + "from PONINVOICESUMMERY ps where " + "ps.invoicenumber is not null and
		 * BUYER =? AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION " + "select
		 * ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID," +
		 * "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT," +
		 * "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT," +
		 * "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME," +
		 * "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,
		 * ps.TAXAMOUNT," + "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
		 * + "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber =
		 * ia.invoicenumber " + "and ps.ponumber=ia.ponumber where" + " ps.invoicenumber
		 * is not null and BUYER =? AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order
		 * by CREATEDON desc";
		 **/

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		int invoicewopopages = 0;
		int pages = 0;

		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> invoiceList1 = new ArrayList<HashMap<String, String>>();

		try {
			con = DBConnection.getConnection();

			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ"))) {

				if (!"INV".equalsIgnoreCase(status)) {
					status = "INV";
				}

				ArrayList<String> param = new ArrayList<String>();
				if (!mode.equalsIgnoreCase("payer")) {
					param.add(emailid);
				}
				Pagination pg = null;

				/**
				 * if (status.equalsIgnoreCase("H")) { param.add(status); param.add(emailid);
				 * param.add(status); pg = new Pagination(hdata, nPage); } else if
				 * (status.equalsIgnoreCase("ALL")) { param.add(emailid); pg = new
				 * Pagination(alldata, nPage); } else if (status.equalsIgnoreCase("C")) {
				 * param.add(emailid); pg = new Pagination(sdata, nPage); } else if
				 * (status.equalsIgnoreCase("P")) { param.add("P"); param.add("M");
				 * param.add(emailid); param.add("P"); param.add("M"); pg = new
				 * Pagination(pendingsql, nPage); } else if (status.equalsIgnoreCase("V")) {
				 * param.add("V"); param.add("RO"); param.add(emailid); param.add("V");
				 * param.add("RO"); pg = new Pagination(pendingsql, nPage); } else {
				 * param.add(status); param.add(emailid); param.add(status); pg = new
				 * Pagination(sql, nPage);
				 * 
				 * }
				 **/

				if (status.equalsIgnoreCase("C")) {
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}
					pg = new Pagination(sdata, nPage);

					log.info("SQ QUERY : " + sdata);
				} else {
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(status);
						param.add(emailid);
						param.add(status);
					} else {
						param.add(status);
						param.add(status);
					}

					pg = new Pagination(sql, nPage);

					log.info("QUERY : " + sdata);

				}

				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				int count = 0;

				String invNumber = null;
				String invDate = null;
				String mPO = null;
				String bpid = null;

				while (rs.next()) {
					count++;
					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					invNumber = rs.getString("INVOICENUMBER");
					poData.put("PONUMBER", rs.getString("PONUMBER"));
					poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					bpid = rs.getString("BUSINESSPARTNEROID");
					poData.put("MESSAGE", rs.getString("MESSAGE"));
					poData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
					poData.put("BUYER", rs.getString("BUYER"));
					poData.put("AMOUNT", rs.getString("AMOUNT"));
					poData.put("MACOUNT", rs.getString("MACOUNT"));
					poData.put("HOLDCOUNT", rs.getString("HOLDCOUNT"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poData.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
					poData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					invDate = rs.getString("INVOICEDATE");
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
					poData.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
					poData.put("MPO", rs.getString("MPO"));
					poData.put("ALLPO", rs.getString("ALLPO"));
					mPO = (rs.getString("MPO") == null) ? "-" : rs.getString("MPO");
					invoiceList.add(poData);
				}
				rs.close();
				pg.close();
				pg = null;
			} else {

				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				if (!mode.equalsIgnoreCase("payer")) {
					param.add(emailid);
				}
				Pagination pg = null;

				String advqdata = "";
				if (!status.equalsIgnoreCase("ASSQ")) {
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND ps.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND ps.PONUMBER=?";
						subquery = subquery + po;
						basePoQuery = " and ps.basepo=ia.ponumber ";
						basePoFlag = true;
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND ps.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND ps.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}

					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}

					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						param.add(pono);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND OVERALLSTATUS = 'INV' "
							+ userQuery + " " + subquery
							// + " " + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL "
							+ "UNION "
							+ "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
							+ basePoQuery + "  where ps.invoicenumber is not null AND OVERALLSTATUS = 'INV' "
							+ userQuery + " " + subquery + " "
							// + "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' "
							+ "order by CREATEDON desc";

				} else if (status.equalsIgnoreCase("ASSQ")) {
					if (!vendor.equalsIgnoreCase("NA")) {

						String po = "AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND ps.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						String in = " AND ps.INVOICENUMBER=?";
						subquery = subquery + in;
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND ps.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ "AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fdate);
						param.add(tdate);
					}

					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}

					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!invno.equalsIgnoreCase("NA")) {
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND OVERALLSTATUS = 'INV' "
							+ userQuery + " " + subquery
							// + " " + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL "
							+ " UNION "
							+ "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
							+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
							+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.BASEPO "
							+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
							+ " and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND OVERALLSTATUS = 'INV' "
							+ userQuery + " " + subquery
							// + " " + "AND ps.ALLPO IS NOT NULL " + "AND ps.MPO ='Y' "
							+ " order by CREATEDON desc";

				}
				pg = new Pagination(advqdata, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				log.info("AS QUERY : " + advqdata);

				String invNumber = null;
				String invDate = null;
				String mPO = null;
				String bpid = null;

				while (rs.next()) {
					HashMap<String, String> poData = new HashMap<String, String>();
					String poNum = rs.getString("PONUMBER");
					if (basePoFlag && rs.getString("BASEPO") != null) {
						poNum = rs.getString("BASEPO");
					}

					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("PONUMBER", poNum);
					poData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					poData.put("MESSAGE", rs.getString("MESSAGE"));
					poData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
					poData.put("BUYER", rs.getString("BUYER"));
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
					poData.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");

					invNumber = rs.getString("INVOICENUMBER");
					bpid = rs.getString("BUSINESSPARTNEROID");
					invDate = rs.getString("INVOICEDATE");

					poData.put("MPO", rs.getString("MPO"));
					poData.put("ALLPO", rs.getString("ALLPO"));
					mPO = (rs.getString("MPO") == null) ? "-" : rs.getString("MPO");
					invoiceList.add(poData);
				}
				pg.close();
				rs.close();
				pg = null;
			} // end of else

			// try {
			getInternalPonInvoiceSummeryCountsAsPerStatus(emailid, nPage, "INV", invno, pono, fdate, tdate, plant,
					vendor, mode, con, ps, rs);
			// } catch (Exception e) {

			// }

		} catch (SQLException e) {
			log.error("getInvalidInvoiceDetailsForInternalUser() :", e.fillInStackTrace());
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

	public JSONArray getPartnerGroupCompanyDetails(String bid, String serverType) {

		String fromClause = " from COMP_COMPANY ";
		if ("uat".equalsIgnoreCase(serverType)) {
			// fromClause=" from timescapeweb.comp_company ";
			fromClause = " from VW_GET_COMPANYLIST ";
		}

		String pgQuery = "select COMP_COMPANYCODE,COMP_COMPANYNAME " + fromClause
				+ " where COMP_COMPANYCODE IN (select distinct companycode from podetails where businesspartneroid = ? ) order by 2 ";

		ArrayList<HashMap<String, String>> pgList = new ArrayList<HashMap<String, String>>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		// log.info("PG Query : "+pgQuery);

		try {
			con = DBConnection.getConnection();

			ps = con.prepareStatement(pgQuery);
			ps.setString(1, bid);
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> pgMap = new HashMap<String, String>();
				pgMap.put("COMPANYCODE", rs.getString("COMP_COMPANYCODE"));
				pgMap.put("COMPANYNAME", rs.getString("COMP_COMPANYNAME"));
				pgMap.put("BUSINESSPARTNEROID", bid);
				pgList.add(pgMap);
			}

			rs.close();
			ps.close();

			log.info("pgList : " + pgList.size());

			if (pgList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("partnerGroups", pgList);
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

	public JSONArray getPartnerGroupCompanyList(String emailid, String serverType, String bid) {

		String fromClause = " from COMP_COMPANY ";
		if ("uat".equalsIgnoreCase(serverType)) {
			fromClause = " from VW_GET_COMPANYLIST ";
		}

		String pgQuery = "select COMP_COMPANYCODE,COMP_COMPANYNAME " + fromClause
				+ "where COMP_COMPANYCODE IN (select DISTINCT pd.companycode from podetails pd "
				+ "where pd.businesspartneroid in (select DISTINCT bp.businesspartneroid from businesspartner bp where bp.primaryemailid = ? or "
				+ "bp.secondaryemailid = ? or bp.tertiaryemailid = ? )) " + "Union "
				+ "select COMP_COMPANYCODE,COMP_COMPANYNAME " + fromClause
				+ "where COMP_COMPANYCODE IN (select DISTINCT pd.companycode from podetails pd "
				+ "where pd.businesspartneroid in (select DISTINCT bp.BusinessPartnerOID from businesspartnerattributes bp where bp.AttributeValue = ?)) "
				+ "Union " + "select COMP_COMPANYCODE,COMP_COMPANYNAME " + fromClause
				+ "where COMP_COMPANYCODE IN (select DISTINCT pd.companycode from podetails pd "
				+ "where pd.businesspartneroid in (select DISTINCT bp.BusinessPartnerOID from businesspartnercontacts bp where bp.ContactEmailID = ?))  "
				+ "order by 1 ";

		if (bid != null && !"".equals(bid)) {

			pgQuery = "select COMP_COMPANYCODE,COMP_COMPANYNAME " + fromClause
					+ " where COMP_COMPANYCODE IN (select DISTINCT pd.companycode from podetails pd "
					+ " where pd.businesspartneroid = ? )    order by 1 ";
		}

		ArrayList<HashMap<String, String>> pgList = new ArrayList<HashMap<String, String>>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		// log.info("PG Query : "+pgQuery);

		try {
			con = DBConnection.getConnection();

			ps = con.prepareStatement(pgQuery);

			if (bid != null && !"".equals(bid)) {
				ps.setString(1, bid);
			} else {
				ps.setString(1, emailid);
				ps.setString(2, emailid);
				ps.setString(3, emailid);
				ps.setString(4, emailid);
				ps.setString(5, emailid);
			}
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> pgMap = new HashMap<String, String>();
				pgMap.put("COMPANYCODE", rs.getString("COMP_COMPANYCODE"));
				pgMap.put("COMPANYNAME", rs.getString("COMP_COMPANYNAME"));
				pgList.add(pgMap);
			}

			rs.close();
			ps.close();

			log.info("pgList : " + pgList.size());

			if (pgList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("partnerGroups", pgList);
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

	/*
	 * Invoice tracker for pending enduser ,confirmer and approver
	 */

	public JSONArray getPendingInvoiceTracker(String poNumber, String invoiceNumber) {

		String invoiceQuery = "select ip.invoicenumber,ip.enduserstatus,ip.enduseid,ip.status ,listagg (ip.EUMANAGER,',\n') "
				+ "within group (order by ip.status) managerId from invoiceapproval ip, poninvoicesummery ps "
				+ "where ps.invoicenumber = ip.invoicenumber and ps.ponumber = ip.ponumber and overallstatus in ('P','M') "
				+ "and ip.invoicenumber = ? and ip.ponumber = ? "
				+ "group by ip.invoicenumber,ip.enduserstatus,ip.enduseid,ip.status order by ip.status ";

		// HashMap<String, ArrayList<String>> invoiceMap = new HashMap<String,
		// ArrayList<String>>();
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
				responsejson.put("validation", "Allready Approved");
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

	/*
	 * This method has been changed. Advanced search added with filter search Date
	 * 08-04-2024
	 */

	public JSONArray getInvoiceDetails(String bid, int nPage, String status, String invno, String pono, String fdate,
			String tdate, String plant, String companyCode) {

		InvoiceDetailsImpl invImpl = new InvoiceDetailsImpl();
		boolean result;
		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> invoiceList1 = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String subQuery = "";
		String queryStatus = "";
		int pages = 0;
		int invoicewopopages = 0;
		ArrayList<String> param = new ArrayList<String>();

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
			responsejson.put("validation", "Validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		boolean basePoFlag = false;

		try {

			con = DBConnection.getConnection();

			if ("WOPO".equalsIgnoreCase(status) || "ASWP".equalsIgnoreCase(status)) {

				String mySubQuery = "";
				ArrayList<String> param1 = new ArrayList<String>();

				if (bid != null) {

					String bidQuery = " AND BUSSINESSPARTNEROID = ? ";
					mySubQuery = mySubQuery + bidQuery;
					param1.add(bid);

				}
				if (!"NA".equalsIgnoreCase(pono)) {

					String po = " AND PONUMBER = ? ";
					mySubQuery = mySubQuery + po;
					param1.add(pono);

				} else {

					String po = " AND PONUMBER IS NULL ";
					mySubQuery = mySubQuery + po;

				}
				if (!"NA".equalsIgnoreCase(invno)) {

					String in = " AND INVOICENUMBER = ? ";
					mySubQuery = mySubQuery + in;
					param1.add(invno);

				}
				if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate))
						&& (!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate))) {

					String dt = " AND INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY') ";
					mySubQuery = mySubQuery + dt;
					param1.add(fdate);
					param1.add(tdate);
				}

				String withOutPOQuery = "SELECT * FROM INVOICEEVENTDETAILWOPO WHERE STATUS <> 'A' " + mySubQuery
						+ " ORDER BY CREATEDON DESC ";

				log.info("withOutPOQuery : " + withOutPOQuery);

				Pagination pg1 = new Pagination(withOutPOQuery, nPage);
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

			} else {

				if (bid != null) {

					queryStatus = " AND PIS.BUSINESSPARTNEROID = ? ";
					subQuery = subQuery + queryStatus;
					param.add(bid);

				}
				if (companyCode != null) {

					queryStatus = " AND POD.COMPANYCODE = ? ";
					subQuery = subQuery + queryStatus;
					param.add(companyCode);

				}
				if (!"NA".equalsIgnoreCase(plant)) {

					queryStatus = " AND PIS.PLANT = ? ";
					subQuery = subQuery + queryStatus;
					param.add(plant);

				}
				if (!"NA".equalsIgnoreCase(pono)) {

					queryStatus = " AND PIS.PONUMBER = ? ";
					subQuery = subQuery + queryStatus;
					param.add(pono);
				}
				if (!"NA".equalsIgnoreCase(invno)) {

					queryStatus = " AND PIS.INVOICENUMBER = ? ";
					subQuery = subQuery + queryStatus;
					param.add(invno);

				}
				if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate))
						&& (!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate))) {

					queryStatus = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY') ";
					subQuery = subQuery + queryStatus;
					param.add(fdate);
					param.add(tdate);

				}
				if ("ASSQ".equalsIgnoreCase(status)) {

					queryStatus = " AND PIS.CREDITADVICENO IS NOT NULL ";
					subQuery = subQuery + queryStatus;

				} else if ("H".equalsIgnoreCase(status)) {

					queryStatus = " AND ONEXSTATUS = ? ";
					subQuery = subQuery + queryStatus;
					param.add(status);

				} else if ("P".equalsIgnoreCase(status)) {

					queryStatus = " AND (PIS.OVERALLSTATUS = ? OR PIS.OVERALLSTATUS = ?) ";
					subQuery = subQuery + queryStatus;
					param.add(status);
					param.add("M");

				} else if ("V".equalsIgnoreCase(status)) {

					queryStatus = " AND (PIS.OVERALLSTATUS = ? OR PIS.OVERALLSTATUS = ?) ";
					subQuery = subQuery + queryStatus;
					param.add(status);
					param.add("RO");

				} else if ("PRO".equalsIgnoreCase(status) || "PP".equalsIgnoreCase(status)
						|| "PD".equalsIgnoreCase(status) || "O".equalsIgnoreCase(status)
						|| "A".equalsIgnoreCase(status)) {

					queryStatus = " AND PIS.OVERALLSTATUS = ? ";
					subQuery = subQuery + queryStatus;
					param.add(status);

				}

				String mainQuery = " SELECT PIS.INVOICENUMBER," + "	PIS.INVOICEDATE, " + " PIS.PONUMBER, "
						+ " PIS.CONTACTPERSON, " + " PIS.CONTACTPERSONPHONE," + " PIS.VENDORID," + " PIS.PLANT,"
						+ " PIS.BUSINESSPARTNEROID," + " PIS.CREATEDBY," + " PIS.CREATEDON," + " PIS.AMOUNT,"
						+ " PIS.PAYMENTAMOUNT," + " PIS.DESCRIPTION," + " PIS.OVERALLSTATUS," + " PIS.ACTUALFILENAME,"
						+ " PIS.SAVEDFILENAME," + " PIS.CREDITNOTENO," + " PIS.CREDITADVICENO,"
						+ " PIS.TOTALAMTINCTAXES," + " PIS.TAXAMOUNT," + " PIS.MPO," + " PIS.ALLPO "
						+ " FROM PONINVOICESUMMERY PIS," + " PODETAILS POD " + " WHERE "
						+ " PIS.PONUMBER = POD.PONUMBER " + " AND PIS.INVOICENUMBER IS NOT NULL "
						+ " AND PIS.MPO IS NULL " + subQuery + " UNION " + " SELECT DISTINCT PIS.INVOICENUMBER,"
						+ " PIS.INVOICEDATE," + " PIS.PONUMBER," + " PIS.CONTACTPERSON," + " PIS.CONTACTPERSONPHONE,"
						+ " PIS.VENDORID," + " PIS.PLANT," + " PIS.BUSINESSPARTNEROID," + " PIS.CREATEDBY,"
						+ " PIS.CREATEDON," + " PIS.AMOUNT," + " PIS.PAYMENTAMOUNT," + " PIS.DESCRIPTION,"
						+ " PIS.OVERALLSTATUS," + " PIS.ACTUALFILENAME," + " PIS.SAVEDFILENAME," + " PIS.CREDITNOTENO,"
						+ " PIS.CREDITADVICENO," + " PIS.TOTALAMTINCTAXES," + " PIS.TAXAMOUNT," + " PIS.MPO,"
						+ " PIS.ALLPO " + " FROM PONINVOICESUMMERY PIS," + " INVOICEAPPROVAL IA," + " PODETAILS POD "
						+ " WHERE " + " PIS.INVOICENUMBER = IA.INVOICENUMBER " + " AND PIS.PONUMBER=IA.PONUMBER "
						+ " AND PIS.PONUMBER = POD.PONUMBER " + " AND IA.PONUMBER = POD.PONUMBER "
						+ " AND PIS.INVOICENUMBER IS NOT NULL " + " AND PIS.MPO = 'Y' " + subQuery
						+ " ORDER BY CREATEDON DESC ";

				Pagination pg = null;
				log.info("main Query : " + mainQuery);
				pg = new Pagination(mainQuery, nPage);

				if (bid != null) {
					param.add(bid);
				}
				if (companyCode != null) {
					param.add(companyCode);
				}
				if (!"NA".equalsIgnoreCase(plant)) {
					param.add(plant);
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
				if ("H".equalsIgnoreCase(status)) {
					param.add(status);
				} else if ("P".equalsIgnoreCase(status)) {
					param.add(status);
					param.add("M");
				} else if ("V".equalsIgnoreCase(status)) {
					param.add(status);
					param.add("RO");

				} else if ("PRO".equalsIgnoreCase(status) || "PP".equalsIgnoreCase(status)
						|| "PD".equalsIgnoreCase(status) || "O".equalsIgnoreCase(status)
						|| "A".equalsIgnoreCase(status)) {
					param.add(status);
				}

				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);
				String invNumber = null;
				String invDate = null;
				String mPO = null;
				String bpid = null;
				int count = 0;
				while (rs.next()) {
					count++;
					HashMap<String, String> invoiceData = new HashMap<String, String>();
					invoiceData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					invNumber = rs.getString("INVOICENUMBER");
					invoiceData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
					invDate = rs.getString("INVOICEDATE");
					invoiceData.put("PO_NUMBER", rs.getString("PONUMBER"));
					invoiceData.put("CONTACTPERSON", rs.getString("CONTACTPERSON"));
					invoiceData.put("CONTACTPERSONPHONE", rs.getString("CONTACTPERSONPHONE"));
					invoiceData.put("VENDORID", rs.getString("VENDORID"));
					invoiceData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					invoiceData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					invoiceData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
					bpid = rs.getString("BUSINESSPARTNEROID");
					invoiceData.put("CREATEDBY", rs.getString("CreatedBy"));
					invoiceData.put("CREATEDON", rs.getString("CreatedOn"));
					invoiceData.put("TOTALAMOUNT", rs.getString("AMOUNT"));
					invoiceData.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT") != null ? rs.getString("PAYMENTAMOUNT") : "0");
					invoiceData.put("DESCRIPTION", rs.getString("DESCRIPTION"));
					invoiceData.put("STATUS", rs.getString("OVERALLSTATUS"));
					invoiceData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					invoiceData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					invoiceData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					invoiceData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					invoiceData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					invoiceData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					invoiceData.put("MPO", rs.getString("MPO"));
					invoiceData.put("ALLPO", rs.getString("ALLPO"));
					invoiceList.add(invoiceData);
				}
				pg.close();
				rs.close();
				pg = null;
			}

			try {
				getInvoiceDetailsCountAsPerStatus(bid, nPage, status, invno, pono, fdate, tdate, plant, con, ps, rs,
						companyCode);
			} catch (Exception e) {
				log.error("getInvoiceDetails() counter :" + e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("getInvoiceDetails() :" + e.fillInStackTrace());
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

	/*
	 * This method has been changed. Advanced search added with filter search Date
	 * 08-04-2024
	 */

	public JSONArray getInvoiceDetailsCountAsPerStatus(String bid, int nPage, String status, String invno, String pono,
			String fdate, String tdate, String plant, Connection con, PreparedStatement ps, ResultSet rs,
			String companyCode) {

		log.info("Inside getInvoiceDetailsCountAsPerStatus method " + status + " bid - " + bid);

		try {

			HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
			int allCounter = 0;
			String subQuery = "";
			String mySubQuery = "";

			if ("ASWP".equalsIgnoreCase(status)) {
						
				if (bid != null) {
					String bidQuery = " AND BUSSINESSPARTNEROID = ? ";
					mySubQuery = mySubQuery + bidQuery;
				}
				if (!"NA".equalsIgnoreCase(pono)) {
					String po = " AND PONUMBER = ? ";
					mySubQuery = mySubQuery + po;
				} else {
					String po = " AND PONUMBER IS NULL ";
					mySubQuery = mySubQuery + po;
				}
				if (!"NA".equalsIgnoreCase(invno)) {
					String in = " AND INVOICENUMBER = ? ";
					mySubQuery = mySubQuery + in;
				}
				if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate))
						&& (!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate))) {

					String dt = " AND INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY') ";
					mySubQuery = mySubQuery + dt;
				}

				String withOutPOQuery = "SELECT STATUS, COUNT(INVOICENUMBER) AS COUNT FROM INVOICEEVENTDETAILWOPO WHERE STATUS <> 'A' " + mySubQuery
						+ " GROUP BY STATUS ";

				log.info("withOutPOQuery : "+ withOutPOQuery);
	
				ps = con.prepareStatement(withOutPOQuery);
				int counter = 1;				
	
				if (bid != null) {
					ps.setString(counter, bid);
					counter++;
				}
				if (!"NA".equalsIgnoreCase(pono)) {
					ps.setString(counter, pono);
					counter++;
				} 
				if (!"NA".equalsIgnoreCase(invno)) {
					ps.setString(counter, invno);
					counter++;
				}
				if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate))
						&& (!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate))) {
					ps.setString(counter, fdate);
					counter++;
					ps.setString(counter, tdate);
					counter++;
				}
				
				rs = ps.executeQuery();
				while (rs.next()) {
					String sts = rs.getString("STATUS");
					String count = rs.getString("COUNT");
					if (countAsPerStatus.get(sts) == null) {
						countAsPerStatus.put("ASWP", count);
					} else {
						count = String.valueOf(Integer.parseInt(countAsPerStatus.get(sts)) + Integer.parseInt(count));
						countAsPerStatus.put("ASWP", count);
					}
					allCounter += Integer.parseInt(count);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				rs.close();
				ps.close();	
			
			}
			
			if("ASSQ".equalsIgnoreCase(status)) {
				String shortQunatityQuery = " AND PIS.CREDITADVICENO IS NOT NULL ";
				subQuery = subQuery + shortQunatityQuery;
			}
			if(!"NA".equalsIgnoreCase(bid)) {
				String boId = "    AND PIS.BUSINESSPARTNEROID = ? "; 
				subQuery = subQuery + boId;
			}
			if(!"NA".equalsIgnoreCase(companyCode)) {
				String comp =  "    AND POD.COMPANYCODE = ? " ;
				subQuery = subQuery + comp;
			}
			if (!"NA".equalsIgnoreCase(plant)) {
				String myPlant = " AND PIS.PLANT = ? ";
				subQuery = subQuery + myPlant;
			}
			if (!"NA".equalsIgnoreCase(pono)) {
				String po = " AND PIS.PONUMBER = ? ";
				subQuery = subQuery + po;
			}
			if (!"NA".equalsIgnoreCase(invno)) {
				String in = " AND PIS.INVOICENUMBER = ? ";
				subQuery = subQuery + in;
			}
			if ((!"NA".equalsIgnoreCase(fdate)) && (!"Invalid date".equalsIgnoreCase(fdate))) {
				String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY') ";
				subQuery = subQuery + dt;
			}
			
			String invoice_data = "SELECT " 
					+ "    PIS.OVERALLSTATUS," 
					+ "    COUNT(PIS.INVOICENUMBER) AS COUNT "
					+ "    FROM PONINVOICESUMMERY PIS, PODETAILS POD " 
					+ "    WHERE "
					+ "    PIS.PONUMBER = POD.PONUMBER " 
					+ "    AND PIS.INVOICENUMBER IS NOT NULL "
					+ "    AND PIS.OVERALLSTATUS IS NOT NULL " 
					+ "    AND PIS.MPO IS NULL "
					+ subQuery				
					+ "	   GROUP BY "
					+ "    PIS.OVERALLSTATUS " 
					+ " UNION ALL " 
					+ "    SELECT " 
					+ "    PIS.OVERALLSTATUS, "
					+ "    COUNT(DISTINCT PIS.INVOICENUMBER) AS COUNT " 
					+ "    FROM "
					+ "    PONINVOICESUMMERY PIS, INVOICEAPPROVAL IA, PODETAILS POD " 
					+ "    WHERE "
					+ "    PIS.INVOICENUMBER = IA.INVOICENUMBER " 
					+ "    AND PIS.INVOICEDATE = IA.INVOICEDATE "
					+ "    AND PIS.PONUMBER = IA.PONUMBER " 
					+ "    AND PIS.PONUMBER = POD.PONUMBER "
					+ "    AND IA.PONUMBER = POD.PONUMBER " 
					+ "    AND PIS.INVOICENUMBER IS NOT NULL "
					+ "    AND PIS.OVERALLSTATUS IS NOT NULL " 
					+ "    AND PIS.MPO = 'Y' "
					+ subQuery
					+ "    GROUP BY "
					+ "    PIS.OVERALLSTATUS ";

			log.info("counts invoice_data --:" + invoice_data);
			ps = con.prepareStatement(invoice_data);
			int queryCounter = 1;
			
			if(!"NA".equalsIgnoreCase(bid)) {
				ps.setString(queryCounter, bid);
				queryCounter++;
			}
			if(!"NA".equalsIgnoreCase(companyCode)) {
				ps.setString(queryCounter, companyCode);
				queryCounter++;
			}
			if (!"NA".equalsIgnoreCase(plant)) {
				ps.setString(queryCounter, plant);
				queryCounter++;
			}
			if (!"NA".equalsIgnoreCase(pono)) {
				ps.setString(queryCounter, pono);
				queryCounter++;
			}
			if (!"NA".equalsIgnoreCase(invno)) {
				ps.setString(queryCounter, invno);
				queryCounter++;
			}
			if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate))
					&&(!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate)) ) {
		
				ps.setString(queryCounter, fdate);
				queryCounter++;
				ps.setString(queryCounter, tdate);
				queryCounter++;				
			}
			
			if(!"NA".equalsIgnoreCase(bid)) {
				ps.setString(queryCounter, bid);
				queryCounter++;
			}
			if(!"NA".equalsIgnoreCase(companyCode)) {
				ps.setString(queryCounter, companyCode);
				queryCounter++;
			}
			if (!"NA".equalsIgnoreCase(plant)) {
				ps.setString(queryCounter, plant);
				queryCounter++;
			}
			if (!"NA".equalsIgnoreCase(pono)) {
				ps.setString(queryCounter, pono);
				queryCounter++;
			}
			if (!"NA".equalsIgnoreCase(invno)) {
				ps.setString(queryCounter, invno);
				queryCounter++;
			}
			if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate))
					&&(!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate)) ) {
				ps.setString(queryCounter, fdate);
				queryCounter++;
				ps.setString(queryCounter, tdate);
				queryCounter++;				
			}
			rs = ps.executeQuery();
			while (rs.next()) {
				String sts = rs.getString("overallstatus");
				String count = rs.getString("count");
				if (countAsPerStatus.get(sts) == null) {
					countAsPerStatus.put(sts, count);
				} else {
					count = String.valueOf(Integer.parseInt(countAsPerStatus.get(sts)) + Integer.parseInt(count));
					countAsPerStatus.put(sts, count);
				}
				allCounter += Integer.parseInt(count);
			}
			countAsPerStatus.put("ALL", allCounter + "");

			if ( "INV".equalsIgnoreCase(status)) {
				log.info("No Invalid Invoices Found!!");
				if (countAsPerStatus.get(status) == null) {
					countAsPerStatus.put("INV", "0");
				}					
			}

			rs.close();
			ps.close();		
			subQuery = "";

			if ("H".equalsIgnoreCase(status)) {

				if(!"NA".equalsIgnoreCase(bid)) {
					String boId = "    AND PIS.BUSINESSPARTNEROID = ? "; 
					subQuery = subQuery + boId;
				}
				if(!"NA".equalsIgnoreCase(companyCode)) {
					String comp =  "    AND POD.COMPANYCODE = ? " ;
					subQuery = subQuery + comp;
				}
				if (!"NA".equalsIgnoreCase(plant)) {
					String myPlant = " AND PIS.PLANT = ? ";
					subQuery = subQuery + myPlant;
				}
				if (!"NA".equalsIgnoreCase(pono)) {
					String po = " AND PIS.PONUMBER = ? ";
					subQuery = subQuery + po;
				}
				if (!"NA".equalsIgnoreCase(invno)) {
					String in = " AND PIS.INVOICENUMBER = ? ";
					subQuery = subQuery + in;
				}
				if ((!"NA".equalsIgnoreCase(fdate)) && (!"Invalid date".equalsIgnoreCase(fdate))) {
					String dt = " AND PIS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY') ";
					subQuery = subQuery + dt;
				}
				
				String history_data = "SELECT " 
						+ "    PIS.OVERALLSTATUS," 
						+ "    COUNT(PIS.INVOICENUMBER) AS COUNT "
						+ "    FROM PONINVOICESUMMERY PIS, PODETAILS POD " 
						+ "    WHERE "
						+ "    PIS.PONUMBER = POD.PONUMBER " 
						+ "    AND PIS.INVOICENUMBER IS NOT NULL "
						+ "    AND PIS.OVERALLSTATUS IS NOT NULL " 
						+ "    AND PIS.MPO IS NULL "
						+ "    AND ONEXSTATUS = 'H' "
						+ subQuery				
						+ "	   GROUP BY "
						+ "    PIS.OVERALLSTATUS " 
						+ " UNION ALL " 
						+ "    SELECT " 
						+ "    PIS.OVERALLSTATUS, "
						+ "    COUNT(DISTINCT PIS.INVOICENUMBER) AS COUNT " 
						+ "    FROM "
						+ "    PONINVOICESUMMERY PIS, INVOICEAPPROVAL IA, PODETAILS POD " 
						+ "    WHERE "
						+ "    PIS.INVOICENUMBER = IA.INVOICENUMBER " 
						+ "    AND PIS.INVOICEDATE = IA.INVOICEDATE "
						+ "    AND PIS.PONUMBER = IA.PONUMBER " 
						+ "    AND PIS.PONUMBER = POD.PONUMBER "
						+ "    AND IA.PONUMBER = POD.PONUMBER " 
						+ "    AND PIS.INVOICENUMBER IS NOT NULL "
						+ "    AND PIS.OVERALLSTATUS IS NOT NULL " 
						+ "    AND PIS.MPO = 'Y' "
						+ "    AND ONEXSTATUS = 'H' "
						+ subQuery
						+ "    GROUP BY "
						+ "    PIS.OVERALLSTATUS ";

				log.info("counts history_data --:" + history_data);
				ps = con.prepareStatement(history_data);
				int qCounter = 1;
				
				if(!"NA".equalsIgnoreCase(bid)) {
					ps.setString(qCounter, bid);
					qCounter++;
				}
				if(!"NA".equalsIgnoreCase(companyCode)) {
					ps.setString(qCounter, companyCode);
					qCounter++;
				}
				if (!"NA".equalsIgnoreCase(plant)) {
					ps.setString(qCounter, plant);
					qCounter++;
				}
				if (!"NA".equalsIgnoreCase(pono)) {
					ps.setString(qCounter, pono);
					qCounter++;
				}
				if (!"NA".equalsIgnoreCase(invno)) {
					ps.setString(qCounter, invno);
					qCounter++;
				}
				if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate))
						&&(!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate)) ) {
					ps.setString(qCounter, fdate);
					qCounter++;
					ps.setString(qCounter, tdate);
					qCounter++;				
				}
				
				if(!"NA".equalsIgnoreCase(bid)) {
					ps.setString(qCounter, bid);
					qCounter++;
				}
				if(!"NA".equalsIgnoreCase(companyCode)) {
					ps.setString(qCounter, companyCode);
					qCounter++;
				}
				if (!"NA".equalsIgnoreCase(plant)) {
					ps.setString(qCounter, plant);
					qCounter++;
				}
				if (!"NA".equalsIgnoreCase(pono)) {
					ps.setString(qCounter, pono);
					qCounter++;
				}
				if (!"NA".equalsIgnoreCase(invno)) {
					ps.setString(qCounter, invno);
					qCounter++;
				}
				if ((!"NA".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(fdate))
						&&(!"NA".equalsIgnoreCase(tdate) && !"Invalid date".equalsIgnoreCase(tdate)) ) {
			
					ps.setString(qCounter, fdate);
					qCounter++;
					ps.setString(qCounter, tdate);
					qCounter++;				
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					String count = rs.getString("count");
					countAsPerStatus.put("H", count);
				}
				rs.close();
				ps.close();
			}

			if (!countAsPerStatus.isEmpty()) {

			//	if (status.equalsIgnoreCase("Invalid Invoices")) {
		///			responsejson.put("invalidInvoiceCount", countAsPerStatus);					
			//	} else {
					responsejson.put("invoiceCountAsPerStatus", countAsPerStatus);
		//		}
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getInvoiceDetailsCountAsPerStatus() :", e.fillInStackTrace());
		}

		if (status.equalsIgnoreCase("Invalid Invoices")) {
			return jsonArray;
		}
		return null;
	}

}
