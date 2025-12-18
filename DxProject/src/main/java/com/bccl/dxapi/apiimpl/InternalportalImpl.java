package com.bccl.dxapi.apiimpl;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpSession;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.bccl.dxapi.apiutility.DXPortalException;
import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.apiutility.JcoGetDataFromSAP;
import com.bccl.dxapi.apiutility.Pagination;
import com.bccl.dxapi.apiutility.Validation;
import com.bccl.dxapi.bean.CreditAdviceDetails;
import com.bccl.dxapi.bean.EndUserReturn;
import com.bccl.dxapi.bean.Invoiceapproval;
import com.bccl.dxapi.security.SendImapMessage;
import au.com.bytecode.opencsv.CSVWriter;

public class InternalportalImpl {

	static Logger log = Logger.getLogger(InternalportalImpl.class.getName());

	public InternalportalImpl() {
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

	public void executeUpdateBalance(Connection con, List<EndUserReturn> enduserList) throws DXPortalException {

		Object po = POImpl.getPOObj(enduserList.get(0).getPonumber());
		updateBalanceReturned(con, enduserList, po);
	}

	public void updateBalanceReturned(Connection con, List<EndUserReturn> enduserList, Object po)
			throws DXPortalException {

		String poeventdetailsbalanceqty = " select balance_qty from poeventdetails where "
				+ "PONumber=? and LineItemNumber=? and BusinessPartnerOID=?";

		String shortFallQty = "UPDATE poeventdetails set balance_qty=? where BusinessPartnerOID =? and PONumber= ? "
				+ "and LineItemNumber= ? and OrderNumber is null";

		double actualbalanceQty = 0.0;
		int value = 0;
		synchronized (po) {
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {

				for (int z = 0; z < enduserList.size(); z++) {
					ps = con.prepareStatement(poeventdetailsbalanceqty);
					ps.setString(1, enduserList.get(z).getPonumber());
					ps.setString(2, enduserList.get(z).getLineitemnumber());
					ps.setString(3, enduserList.get(z).getBid());
					rs = ps.executeQuery();
					while (rs.next()) {
						actualbalanceQty = Double.parseDouble(rs.getString("balance_qty"));
					}
					rs.close();
					ps.close();
					ps = con.prepareStatement(shortFallQty);
					ps.setDouble(1, actualbalanceQty + Double.parseDouble(enduserList.get(z).getQuantity()));
					ps.setString(2, enduserList.get(z).getBid());
					ps.setString(3, enduserList.get(z).getPonumber());
					ps.setString(4, enduserList.get(z).getLineitemnumber());
					ps.executeUpdate();
					ps.close();
				}
			} catch (Exception e) {
				log.error("updateBalanceReturned() :", e.fillInStackTrace());

				throw new DXPortalException("Error in returning invoice !!", "SQL Error updateBalanceReturned");
			}
		}
	}

	public JSONArray getinvoicebasedonemailid(String emailId, HttpSession session, int nPage, String status,
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
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
							+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
							+ "AND (CREDITADVICENO IS NOT NULL) " + "UNION"
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(CREDITADVICENO IS NOT NULL AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
							+ "UNION" + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND"
							+ " (CREDITADVICENO IS NOT NULL  AND A1.STATUS LIKE 'C%') ORDER BY CREATEDON DESC) c )";

					storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND CREDITADVICENO IS NOT NULL "
							+ " UNION" + " SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
							+ " CREDITADVICENO IS NOT NULL " + "UNION "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  AND "
							+ "CREDITADVICENO IS NOT NULL " + "UNION" + " SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND CREDITADVICENO IS NOT NULL) JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "ORDER BY CREATEDON DESC) c )";
				} else if ("ALL".equalsIgnoreCase(status)) {

					sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
							+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? " + ""
							+ "UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL) " + "UNION "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND"
							+ " (A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

					storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + " UNION"
							+ " SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL "
							+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' " + "UNION"
							+ " SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "ORDER BY CREATEDON DESC) c )";

				} else {
					if ("P".equalsIgnoreCase(status)) {
						sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE, "
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER, "
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS, "
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS, "
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER, "
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO, "
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? OR B.OVERALLSTATUS=?) " + " UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
								+ "(B1.OVERALLSTATUS=?  OR B1.OVERALLSTATUS=? AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
								+ "(B1.OVERALLSTATUS=?  OR B1.OVERALLSTATUS=? AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

						storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ " A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=? OR B.OVERALLSTATUS=? )"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
								+ "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=?) " + "UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' AND "
								+ "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=?)" + " UNION "
								+ "SELECT DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND (B.OVERALLSTATUS IN (?,?)) JOIN "
								+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "ORDER BY CREATEDON DESC) c )";
					} else {
						sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE, "
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER, "
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS, "
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS, "
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER, "
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO, "
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? ) " + " UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
								+ "(B1.OVERALLSTATUS=?  AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
								+ "(B1.OVERALLSTATUS=?  AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

						storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ " A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=?  )"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
								+ "(B1.OVERALLSTATUS=? ) " + "UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  AND "
								+ "(B1.OVERALLSTATUS=? )" + " UNION " + "SELECT DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
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
						} else {
							param.add(emailId);
							param.add(status);
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
						} else {
							param.add(emailId);
							param.add(status);
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
							getmanagerslist(rs.getString("INVOICENUMBER"), rs.getString("PONUMBER"), con));
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
							getemailidbasedonmaterial(rs.getString("MATERIAL_TYPE"), rs.getString("PLANT")));
					poEvent.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
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
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
						+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
						+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
						+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
						+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? " + " "
						+ subquery + " " + " UNION " + "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ "B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL "
						+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ? )  AND"
						+ "( A1.STATUS NOT LIKE 'C%' AND B.GRNNUMBER IS NOT NULL " + subquery + ") " + "UNION "
						+ "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ " B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL "
						+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ? ) AND " + "(A1.STATUS LIKE 'C%'  " + subquery
						+ ") ORDER BY CREATEDON DESC) c )";

				storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
						+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
						+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
						+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
						+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
						+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
						+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
						+ "A.InvoiceNumber=B.InvoiceNumber AND "
						+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + subquery + " " + "UNION "
						+ "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,"
						+ "B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' "
						+ "AND B.GRNNUMBER IS NOT NULL  " + subquery + " " + " UNION "
						+ "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,"
						+ "B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ?) " + "AND A1.STATUS LIKE 'C%' " + subquery
						+ " " + "UNION " + "SELECT DISTINCT B.PONUMBER,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
						+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
						+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
						+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
						+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' " + subquery + " JOIN "
						+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
						+ "ORDER BY CREATEDON DESC) c )";
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
							getmanagerslist(rs.getString("INVOICENUMBER"), rs.getString("PONUMBER"), con));
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
							getemailidbasedonmaterial(rs.getString("MATERIAL_TYPE"), rs.getString("PLANT")));
					poEvent.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
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
			} catch (Exception e) {

			}
		} catch (Exception e) {
			log.error("getinvoicebasedonemailid() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getBuyerApprvalStatus(String INVOICENUMBER, String PONUMBER, String enduserstatus,
			String managerstatus) throws SQLException, DXPortalException {

		boolean result;
		result = Validation.StringChecknull(INVOICENUMBER);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(PONUMBER);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(enduserstatus);
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

		int value = 0;
		String po_status = "UPDATE invoiceapproval set ENDUSERSTATUS=? , STATUS=?  where INVOICENUMBER=? and PONUMBER =? ";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_status);
			ps.setString(1, enduserstatus);
			ps.setString(2, managerstatus);
			ps.setString(3, INVOICENUMBER);
			ps.setString(4, PONUMBER);
			value = ps.executeUpdate();
			ps.close();

			if (value <= 0) {
				throw new DXPortalException("Error while updating invoice approval status !!",
						"Error in getBuyerApprvalStatus()");
			}
			responsejson.put("message", "Success");
		} catch (DXPortalException dxp) {
			dxp.printStackTrace();
			responsejson.put("error", dxp.reason);
		} catch (SQLException e) {
			log.error("getBuyerApprvalStatus() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getInvoiceStatusDetails(String invoice) throws SQLException {

		String po_data = "Select INVOICESTATUS from  DELIVERYSUMMARY where InvoiceNumber= ?";
		boolean result;
		result = Validation.StringChecknull(invoice);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, invoice);
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("STATUS", rs.getString("Status"));
				POList.add(poData);
			}
		} catch (SQLException e) {
			log.error("getInvoiceStatusDetails() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0) {
			responsejson.put("poData", POList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray getManagerApprvalStatus(String invoiceNumber, String po_num, String enduserstatus,
			String managerstatus) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(invoiceNumber);
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
		result = Validation.StringChecknull(enduserstatus);
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

		int value = 0;
		int valueDetails = 0;
		String Status = null;
		String po_status = "UPDATE invoiceapproval set ENDUSERSTATUS=? , STATUS=? ,MODIFIEDDATE=? where INVOICENUMBER=? and PONUMBER =? ";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(po_status);
			ps.setString(1, enduserstatus);
			ps.setString(2, managerstatus);
			ps.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(4, invoiceNumber);
			ps.setString(5, po_num);
			value = ps.executeUpdate();
			ps.close();
			if (value > 0) {

				if (enduserstatus.equalsIgnoreCase("A")) {
					if (managerstatus.equalsIgnoreCase("A")) {
						Status = "G";
					} else if (managerstatus.equalsIgnoreCase("O")) {
						Status = "O";
					} else if (managerstatus.equalsIgnoreCase("R")) {
						Status = "R";
					}

				} else if (enduserstatus.equalsIgnoreCase("R")) {

					// balance qty logic will be here main status r
					Status = "R";
				} else if (enduserstatus.equalsIgnoreCase("P")) {
					Status = "P";
				} else if (enduserstatus.equalsIgnoreCase("O")) {
					Status = "O";
				} else {
					Status = "M";
				}

				String invoice_status = "UPDATE DELIVERYSUMMARY set INVOICESTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";
				ps = con.prepareStatement(invoice_status);
				ps.setString(1, Status);
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, invoiceNumber);
				ps.setString(4, po_num);
				valueDetails = ps.executeUpdate();
				ps.close();
				String SummaryStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";
				ps = con.prepareStatement(invoice_status);
				ps.setString(1, Status);
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, invoiceNumber);
				ps.setString(4, po_num);
				valueDetails = ps.executeUpdate();
				ps.close();

				if (valueDetails <= 0) {
					throw new DXPortalException("Error while updating poninvoicesummery and delierysummary",
							"Error in getManagerApprvalStatus()");
				}
				con.commit();
				responsejson.put("message", "Success");
			} else {
				responsejson.put("message", "Fail");
				throw new DXPortalException("Error while updating PONINVOICESUMMERY and DELIVERYSUMMARY",
						"Error in getManagerApprvalStatus()");
			}

		} catch (DXPortalException dxp) {
			dxp.printStackTrace();
			log.error("getManagerApprvalStatus() :", dxp.fillInStackTrace());
			log.error("Get Cause", dxp.getCause());
			con.rollback();
			responsejson.put("error", dxp.reason);
		} catch (SQLException e) {
			log.error("getManagerApprvalStatus() :", e.fillInStackTrace());

			responsejson.put("error", e.getLocalizedMessage());
			jsonArray.add(responsejson);
			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		jsonArray.add(responsejson);
		return jsonArray;

	}

	@SuppressWarnings("unchecked")
	public JSONArray getEndUserApprvalStatus(String invoiceNumber, String po_num, String enduserstatus,
			String enduserId, String stage, String storekeeperaction) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(invoiceNumber);
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
		result = Validation.StringChecknull(enduserstatus);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(enduserId);
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

		String po_status = "UPDATE invoiceapproval set ENDUSEID=?,ENDUSERSTATUS=?,MODIFIEDDATE=?,STAGE=?, PROXY=?,"
				+ "ACTIONBY=? where INVOICENUMBER=? and PONUMBER =?";

		int value = 0;
		int valueDetails = 0;
		String Status = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(po_status);
			ps.setString(1, enduserId);
			ps.setString(2, enduserstatus);
			ps.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(4, stage);
			ps.setString(5, null);
			if (storekeeperaction.equalsIgnoreCase("true")) {
				ps.setString(6, "ST");
			} else {
				ps.setString(6, "EU");
			}

			ps.setString(7, invoiceNumber);
			ps.setString(8, po_num);
			value = ps.executeUpdate();
			ps.close();

			if (value > 0) {

				if (enduserstatus.equalsIgnoreCase("A")) {
					Status = "M";
				} else if (enduserstatus.equalsIgnoreCase("R")) {

					String rej = getrejectStatusDetails(invoiceNumber, po_num, con);
					if (rej.equalsIgnoreCase("success")) {
						Status = "R";
					}
					Status = "R";
				} else if (enduserstatus.equalsIgnoreCase("P")) {
					Status = "P";
				} else if (enduserstatus.equalsIgnoreCase("O")) {
					Status = "O";
				} else {
					Status = "M";
				}
				String invoice_status = "UPDATE DELIVERYSUMMARY set INVOICESTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";

				ps = con.prepareStatement(invoice_status);
				ps.setString(1, Status);
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, invoiceNumber);
				ps.setString(4, po_num);
				valueDetails = ps.executeUpdate();
				ps.close();

				String summaryStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";
				ps = con.prepareStatement(summaryStatus);
				ps.setString(1, Status);
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, invoiceNumber);
				ps.setString(4, po_num);
				valueDetails = ps.executeUpdate();
				ps.close();

				if ((!enduserstatus.equalsIgnoreCase("O")) && (!enduserstatus.equalsIgnoreCase("P"))) {

					String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
							+ "MODIFIEDTIME,MODIFIEDBY)" + " values(?,?,?,?,?,?)";

					ps = con.prepareStatement(insertaudit);
					ps.setString(1, invoiceNumber);
					ps.setString(2, po_num);
					ps.setString(3, "");
					ps.setString(4, Status);
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(6, enduserId);
					ps.executeUpdate();
					ps.close();

					// boolean flag = sendMailtoManager(invoiceNumber, po_num, enduserId, con);
					boolean flag = true;
					responsejson.put("emailstatus", flag);
				}
				if (valueDetails <= 0) {
					throw new DXPortalException("Unable to update overallstatus !!",
							"Error in getEndUserApprvalStatus()");
				}
				con.commit();
				responsejson.put("message", "Success");
			} else {
				throw new DXPortalException("End user status not updated successfully !!",
						"Error in getEndUserApprvalStatus()");
			}
		} catch (DXPortalException dxp) {
			log.error("getEndUserApprvalStatus() :", dxp.fillInStackTrace());
			log.error("Get Cause", dxp.getCause());
			responsejson.put("message", "Fail");
			con.rollback();
			dxp.printStackTrace();
			responsejson.put("error", dxp.reason);
		} catch (Exception e) {
			responsejson.put("message", "Fail");
			log.error("getEndUserApprvalStatus() :", e.fillInStackTrace());

			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getmanagerDetails() {

		ArrayList<String> managerlist = new ArrayList<String>();
		managerlist.add("ameya.pradhan@timesgroup.com");
		managerlist.add("rishiraj.shukla@timesgroup.com");
		managerlist.add("sumeet.garg@timesgroup.com");
		managerlist.add("vikrant.desai@timesgroup.com");
		managerlist.add("sachin.mehta@timesgroup.com");
		managerlist.add("sachin.kale@timesgroup.com");

		responsejson.put("manager", managerlist);
		jsonArray.add(managerlist);
		return jsonArray;
	}

	public String getmanagerslist(String invoice, String po_num, Connection con) throws SQLException {

		String holdcount = "select EUMANAGER from invoiceapproval where InvoiceNumber=? and PONumber=? ";
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuffer sb = new StringBuffer();

		try {
			ps = con.prepareStatement(holdcount);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				sb.append(rs.getString("EUMANAGER"));
				sb.append("_");
			}
			rs.close();
			ps.close();
			if (sb != null && !(sb.length() == 0)) {
				return sb.toString().substring(0, sb.toString().length() - 1);
			}

		} catch (Exception e) {
			log.error("getmanagerslist() :", e.fillInStackTrace());

		}
		return sb.toString();
	}

	public String getMGRlist(String invoice, String po_num, Connection con) throws SQLException {
		String holdcount = "select EUMANAGER from invoiceapproval where InvoiceNumber=? and PONumber=? and Status in ('A','M','O','P')";
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuffer sb = new StringBuffer();

		try {
			ps = con.prepareStatement(holdcount);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				sb.append(rs.getString("EUMANAGER"));
				sb.append("_");
			}
			rs.close();
			ps.close();
			if (sb != null && !(sb.length() == 0)) {
				return sb.toString().substring(0, sb.toString().length() - 1);
			}

		} catch (Exception e) {
			log.error("getMGRlist() :", e.fillInStackTrace());

		}
		return sb.toString();
	}

	public String getConfirmerlist(String invoice, String po_num, Connection con) throws SQLException {
		String holdcount = "select EUMANAGER from invoiceapproval where InvoiceNumber=? and PONumber=? and Status in ('CA','CM','CO') ";
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuffer sb = new StringBuffer();

		try {
			ps = con.prepareStatement(holdcount);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				sb.append(rs.getString("EUMANAGER"));
				sb.append("_");
			}
			rs.close();
			ps.close();
			if (sb != null && !(sb.length() == 0)) {
				return sb.toString().substring(0, sb.toString().length() - 1);
			}

		} catch (Exception e) {
			log.error("getConfirmerlist() :", e.fillInStackTrace());

		}
		return sb.toString();
	}

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

	public JSONArray getmanagerCountDetails(String invoice, String po_num) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(invoice);
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

		String holdcount = "select EUMANAGER from invoiceapproval where InvoiceNumber=? and PONumber=?";
		int count = 0;
		ArrayList<String> managerlist = new ArrayList<String>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(holdcount);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				managerlist.add(rs.getString("EUMANAGER"));
			}
			rs.close();
			ps.close();
			responsejson.put("managerlist", managerlist);
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);
		} catch (Exception e) {
			responsejson.put("message", "Fail");
			log.error("getmanagerCountDetails() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray InvoiceApprovalConfirmerStatus(String invoiceNumber, String po_num, String managerstatusinitial,
			String managerId, String stage, String username) throws SQLException {

		boolean result;
		boolean status = false;
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

		result = Validation.StringChecknull(managerstatusinitial);
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

		String confirmstatus = "select count(*) as counter from INVOICEAPPROVAL where (STATUS = 'CM' OR  STATUS = 'CR' OR STATUS = 'CO') AND INVOICENUMBER = ? "
				+ "AND PONUMBER = ? ";

		int value = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String grn = "";
		int count = 0;
		boolean confMailFlag = false;
		boolean managerMailFlag = false;
		boolean grnOkFlag = false;
		String bid = null;
		String invoiceDate = null;

		try {

			String managerstatus = "";
			if ("O".equalsIgnoreCase(managerstatusinitial)) {
				managerstatus = "CO";
			} else if ("A".equalsIgnoreCase(managerstatusinitial)) {
				managerstatus = "CA";
			} else if ("R".equalsIgnoreCase(managerstatusinitial)) {
				managerstatus = "CR";
			} else if ("M".equalsIgnoreCase(managerstatusinitial) || "P".equalsIgnoreCase(managerstatusinitial)) {
				managerstatus = "CM";
			}
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(confirmstatus);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();

			if (rs.next()) {
				count = rs.getInt("counter");
			}
			ps.close();
			String mpoStatus = "";
			List<String> approvallist = new ArrayList<String>();
			String getInvoiceDate = " SELECT BUSINESSPARTNEROID,TO_CHAR(INVOICEDATE,'DD-MON-RRRR') AS INVOICEDATE "
					+ " FROM PONINVOICESUMMERY WHERE INVOICENUMBER = ? AND PONUMBER= ? ";

			ps = con.prepareStatement(getInvoiceDate);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);

			rs = ps.executeQuery();

			while (rs.next()) {
				bid = rs.getString("BUSINESSPARTNEROID");
				invoiceDate = rs.getString("INVOICEDATE");
			}
			rs.close();
			ps.close();

			String getdeliverydetails = "SELECT PONUMBER,LINEITEMNUMBER,TO_CHAR(INVOICEDATE,'dd-MON-RRRR') AS INVOICEDATE,SAPLINEITEMNO,"
					+ " INVOICENUMBER,SERVICENUMBER,ACCEPTEDQTY,STORAGELOCATION FROM DELIVERYSUMMARY WHERE INVOICENUMBER = ? "
					+ " AND TRUNC(INVOICEDATE) = TO_DATE(?,'DD-MON-RRRR') AND BUSSINESSPARTNEROID = ? ";

			ps = con.prepareStatement(getdeliverydetails);
			ps.setString(1, invoiceNumber);
			ps.setString(2, invoiceDate);
			ps.setString(3, bid);
			rs = ps.executeQuery();
			String portalid = "";
			int a = 0;
			ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
			Properties prop = new Properties();
			InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
			prop.load(input);
			String server = prop.getProperty("servertype");
			while (rs.next()) {

				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("PONUMBER", rs.getString("PONUMBER"));
				poData.put("LINEITEMNUMBER", rs.getString("LINEITEMNUMBER"));
				poData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
				poData.put("SAPLINEITEMNO", rs.getString("SAPLINEITEMNO"));
				poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				poData.put("SERVICENUMBER", rs.getString("SERVICENUMBER"));
				poData.put("ACCEPTEDQTY", rs.getString("ACCEPTEDQTY"));
				poData.put("STORAGELOCATION", rs.getString("STORAGELOCATION"));

				if ("dev".equalsIgnoreCase(server)) {
					poData.put("PORTALID", "dsubra1007");
				} else if ("uat".equalsIgnoreCase(server)) {
					if (a == 0) {
						JSONArray jsonArraygetorder = getportalid(managerId);
						JSONObject jsonobjectgetorder = (JSONObject) jsonArraygetorder.get(0);
						if (jsonobjectgetorder.get("message").toString().equalsIgnoreCase("Success")) {
							portalid = jsonobjectgetorder.get("portalid").toString();
						}
						poData.put("PORTALID", portalid);
					} else {
						poData.put("PORTALID", portalid);
					}

				}

				POList.add(poData);
				a++;
			}

			rs.close();
			ps.close();
			if (count > 1 || "O".equalsIgnoreCase(managerstatusinitial) || "M".equalsIgnoreCase(managerstatusinitial)
					|| "P".equalsIgnoreCase(managerstatusinitial)) {
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
				confMailFlag = true;
				responsejson.put("pendingcount", count);
				responsejson.put("message", "Success");

			} else if (count == 1 && "A".equalsIgnoreCase(managerstatusinitial)) {

				String data = "";
				for (int c = 0; c < POList.size(); c++) {
					if (POList.get(c).get("LINEITEMNUMBER").contains("-")) {
						data = POList.get(c).get("PONUMBER") + "," + POList.get(c).get("LINEITEMNUMBER") + ","
								+ POList.get(c).get("SERVICENUMBER") + "," + POList.get(c).get("INVOICENUMBER") + ","
								+ POList.get(c).get("INVOICEDATE") + "," + POList.get(c).get("ACCEPTEDQTY") + ","
								+ POList.get(c).get("STORAGELOCATION") + "," + username + ","
								+ POList.get(c).get("PORTALID") + ",'N'";
					} else {

						data = POList.get(c).get("PONUMBER") + "," + POList.get(c).get("LINEITEMNUMBER") + ","
								+ POList.get(c).get("INVOICEDATE") + "," + POList.get(c).get("INVOICENUMBER") + ","
								+ POList.get(c).get("ACCEPTEDQTY") + "," + POList.get(c).get("STORAGELOCATION") + ","
								+ username + "," + POList.get(c).get("PORTALID") + ",'N'";

					}
					approvallist.add(data);
				}
				JSONArray ja = null;
				String message = "";

				String mpoStatusQ = "select MPO from PONINVOICESUMMERY where PONUMBER=? and INVOICENUMBER=?";
				ps = con.prepareStatement(mpoStatusQ);
				ps.setString(1, po_num);
				ps.setString(2, invoiceNumber);
				rs = ps.executeQuery();
				if (rs.next()) {
					mpoStatus = rs.getString("MPO");
				}
				ps.close();

				if ("dev".equalsIgnoreCase(server)) {
					if (POList.get(0).get("LINEITEMNUMBER").contains("-")) {
						if (mpoStatus != null && !"".equals(mpoStatus) && "Y".equals(mpoStatus)) {
							SimpoImpl simpo = new SimpoImpl();
							ja = simpo.updateacceptedservicequantity(approvallist, "");
						} else {
							ja = updateacceptedservicequantity(approvallist, "");
						}
						JSONObject jsonobjectgetorder = (JSONObject) ja.get(0);
						if (jsonobjectgetorder.get("message").toString().equalsIgnoreCase("Success")) {
							responsejson.put("grnlist", jsonobjectgetorder.get("grnlist").toString());
							responsejson.put("scrnlist", jsonobjectgetorder.get("scrnlist").toString());
							message = "Success";
							grnOkFlag = true;
						} else {
							responsejson.put("message", jsonobjectgetorder.get("message").toString());
							message = jsonobjectgetorder.get("message").toString();
						}
					} else {
						if (mpoStatus != null && !"".equals(mpoStatus) && "Y".equals(mpoStatus)) {
							SimpoImpl simpo = new SimpoImpl();
							ja = simpo.updateacceptedquantity(approvallist, "");
						} else {
							ja = updateacceptedquantity(approvallist, "");
						}
						JSONObject jsonobjectgetorder = (JSONObject) ja.get(0);
						if (jsonobjectgetorder.get("message").toString().equalsIgnoreCase("Success")) {
							responsejson.put("grn", jsonobjectgetorder.get("grn").toString());
							message = "Success";
							grnOkFlag = true;
						} else {
							responsejson.put("message", jsonobjectgetorder.get("message").toString());
							message = jsonobjectgetorder.get("message").toString();
						}
					}
				} else if ("uat".equalsIgnoreCase(server)) {

					if (POList.get(0).get("LINEITEMNUMBER").contains("-")) {
						if (mpoStatus != null && !"".equals(mpoStatus) && "Y".equals(mpoStatus)) {
							SimpoImpl simpo = new SimpoImpl();
							ja = simpo.getAcceptQtynServiceGRN(approvallist, "", con);
						} else {
							ja = getAcceptQtynServiceGRN(approvallist, "", con);
						}
						JSONObject jsonobjectgetorder = (JSONObject) ja.get(0);
						if (jsonobjectgetorder.get("message").toString().equalsIgnoreCase("Success")) {
							responsejson.put("grnlist", jsonobjectgetorder.get("grnlist").toString());
							responsejson.put("scrnlist", jsonobjectgetorder.get("scrnlist").toString());
							responsejson.put("warningMessage", jsonobjectgetorder.get("warningMessage").toString());
							grn = jsonobjectgetorder.get("grnlist").toString();
							grnOkFlag = true;
							message = "Success";
						} else {
							responsejson.put("message", jsonobjectgetorder.get("message").toString());
							responsejson.put("warningMessage", jsonobjectgetorder.get("warningMessage") == null ? ""
									: jsonobjectgetorder.get("warningMessage").toString());
							responsejson.put("grnlist", jsonobjectgetorder.get("grnlist") == null ? ""
									: jsonobjectgetorder.get("grnlist").toString());
							responsejson.put("scrnlist", jsonobjectgetorder.get("scrnlist") == null ? ""
									: jsonobjectgetorder.get("scrnlist").toString());
							message = jsonobjectgetorder.get("message").toString();

							grn = jsonobjectgetorder.get("grnlist").toString();
							if (grn != null && !"".equals(grn) && !"-".equals(grn)) {
								grnOkFlag = true;
							}
						}
					} else {
						if (mpoStatus != null && !"".equals(mpoStatus) && "Y".equals(mpoStatus)) {
							SimpoImpl simpo = new SimpoImpl();
							ja = simpo.getAcceptQtynGRN(approvallist, "", con);
						} else {
							ja = getAcceptQtynGRN(approvallist, "", con);
						}
						JSONObject jsonobjectgetorder = (JSONObject) ja.get(0);
						if (jsonobjectgetorder.get("message").toString().equalsIgnoreCase("Success")) {
							responsejson.put("grn", jsonobjectgetorder.get("grn").toString());
							responsejson.put("warningMessage", jsonobjectgetorder.get("warningMessage").toString());
							grn = jsonobjectgetorder.get("grn").toString();
							grnOkFlag = true;
							message = "Success";
						} else {
							responsejson.put("message", jsonobjectgetorder.get("message").toString());
							message = jsonobjectgetorder.get("message").toString();
						}
					}
				}
				responsejson.put("pendingcount", count);
				responsejson.put("message", message);
			}

			if (grnOkFlag) {

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
				confMailFlag = true;

				if ((!managerstatusinitial.equalsIgnoreCase("O")) && (!managerstatusinitial.equalsIgnoreCase("P"))) {

					String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
							+ "MODIFIEDTIME,MODIFIEDBY)" + " values(?,?,?,?,?,?)";
					ps = con.prepareStatement(insertaudit);
					ps.setString(1, invoiceNumber);
					ps.setString(2, po_num);
					ps.setString(3, "");
					ps.setString(4, "M");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(6, managerId);
					ps.executeUpdate();
					ps.close();
				}

			}
			int onHoldCounter = 0;
			String onHoldStatus = "select count(*) as counter from INVOICEAPPROVAL where STATUS = 'CO' AND INVOICENUMBER = ? "
					+ "AND PONUMBER = ? ";

			ps = con.prepareStatement(onHoldStatus);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			if (rs.next()) {
				onHoldCounter = rs.getInt("counter");
			}
			rs.close();
			ps.close();

			String overAllStatus = "";
			String Status = "";
			int maCount = 0;
			boolean statusValueHo;
			boolean allManagerStatus;
			int holdCount;

			if (managerstatus.equalsIgnoreCase("CA")) {
				overAllStatus = getoverAllStatus(invoiceNumber, po_num, con);
				if (overAllStatus.equalsIgnoreCase("O")) {
					statusValueHo = true;
					if (onHoldCounter >= 0) {
						statusValueHo = decreaseHoldCount(invoiceNumber, po_num, con);
					}
					if (statusValueHo) {
						if (onHoldCounter > 0) {
							Status = "O";
						} else {
							Status = "M";
						}
					}

				} else {
					Status = "M";
				}

			} else if (managerstatus.equalsIgnoreCase("CM")) {

				overAllStatus = getoverAllStatus(invoiceNumber, po_num, con);

				if (overAllStatus.equalsIgnoreCase("O")) {
					statusValueHo = true;
					if (onHoldCounter >= 0) {
						statusValueHo = decreaseHoldCount(invoiceNumber, po_num, con);
					}
					if (statusValueHo) {
						holdCount = getholdcount(invoiceNumber, po_num, con);
						if (holdCount > 0) {
							Status = "O";
						} else {
							Status = "M";
						}
					} else {
						Status = "O";
					}
				}

			} else if (managerstatus.equalsIgnoreCase("CO")) {
				if (overAllStatus.equalsIgnoreCase("M") || overAllStatus.equalsIgnoreCase("P")) {
					statusValueHo = getIncHoCountOnly(invoiceNumber, po_num, con);
					if (statusValueHo) {
						Status = "O";
					} else {
						Status = "M";
					}
				} else {
					statusValueHo = getIncHoCountOnly(invoiceNumber, po_num, con);
					Status = "O";
				}
			}

			if (Status != null && !Status.equals("")) {
				String mpoStatusQ = "select MPO from PONINVOICESUMMERY where PONUMBER=? and INVOICENUMBER=?";
				ps = con.prepareStatement(mpoStatusQ);
				ps.setString(1, po_num);
				ps.setString(2, invoiceNumber);
				rs = ps.executeQuery();
				if (rs.next()) {
					mpoStatus = rs.getString("MPO");
				}
				ps.close();
				if (mpoStatus != null && !"".equals(mpoStatus) && "Y".equals(mpoStatus)) {
					String data = "";
					for (int c = 0; c < POList.size(); c++) {
						if (POList.get(c).get("LINEITEMNUMBER").contains("-")) {
							data = POList.get(c).get("PONUMBER") + "," + POList.get(c).get("LINEITEMNUMBER") + ","
									+ POList.get(c).get("SERVICENUMBER") + "," + POList.get(c).get("INVOICENUMBER")
									+ "," + POList.get(c).get("INVOICEDATE") + "," + POList.get(c).get("ACCEPTEDQTY")
									+ "," + POList.get(c).get("STORAGELOCATION") + "," + username + ","
									+ POList.get(c).get("PORTALID") + ",'N'";
						} else {

							data = POList.get(c).get("PONUMBER") + "," + POList.get(c).get("LINEITEMNUMBER") + ","
									+ POList.get(c).get("INVOICEDATE") + "," + POList.get(c).get("INVOICENUMBER") + ","
									+ POList.get(c).get("ACCEPTEDQTY") + "," + POList.get(c).get("STORAGELOCATION")
									+ "," + username + "," + POList.get(c).get("PORTALID") + ",'N'";

						}
						approvallist.add(data);
					}
					String[] acceptedList = approvallist.toArray(new String[approvallist.size()]);
					for (int counterValue = 0; counterValue < approvallist.size(); counterValue++) {

						String[] fatchedValues = acceptedList[counterValue].split(",");
						String ponumber = fatchedValues[0];

						String summaryStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";
						ps = con.prepareStatement(summaryStatus);
						ps.setString(1, Status);
						ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
						ps.setString(3, invoiceNumber);
						ps.setString(4, ponumber);
						int valueDetails = ps.executeUpdate();
						ps.close();
					}

				} else {
					String summaryStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";
					ps = con.prepareStatement(summaryStatus);
					ps.setString(1, Status);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, invoiceNumber);
					ps.setString(4, po_num);
					int valueDetails = ps.executeUpdate();
					ps.close();
					if (valueDetails <= 0) {
						throw new DXPortalException("Error while updating PONINVOICESUMMERY",
								" Error in InvoiceApprovalConfirmerStatus()");
					}
				}
				if (count > 0) {
					if (!"P".equalsIgnoreCase(managerstatusinitial)) {
						if (confMailFlag) {
							status = sendMailtoCentralOps(invoiceNumber, po_num, managerId, con);
						}
					}
				}
				if ("A".equalsIgnoreCase(managerstatusinitial) && grnOkFlag) {
					// status = sendMailtoManager(invoiceNumber, po_num, managerId, con);
					status = true;
				}
			}
			con.commit();
			responsejson.put("emailstatus", status);

		} catch (DXPortalException dxp) {
			con.rollback();
			responsejson.put("error", dxp.reason);
			log.error("InvoiceApprovalConfirmerStatus() :", dxp.fillInStackTrace());
			log.error("Get Cause", dxp.getCause());
		} catch (Exception e) {
			log.error("InvoiceApprovalConfirmerStatus() :", e.fillInStackTrace());

			responsejson.put("error", e.getMessage());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getManagerApprvalStatusall(String invoiceNumber, String po_num, String managerstatus,
			String managerId, String stage) throws SQLException {

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

		String po_status = "UPDATE invoiceapproval set STATUS=?,MODIFIEDDATE=?,STAGE=? where INVOICENUMBER=? and PONUMBER =? and EUMANAGER =? ";

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

			if (managerstatus.equalsIgnoreCase("A")) {

				overAllStatus = getoverAllStatus(invoiceNumber, po_num, con);
				if (overAllStatus.equalsIgnoreCase("O")) {

					if (onHoldCounter > 1) {
						statusValueHo = getdecHoCount(invoiceNumber, po_num, con);
					} else {
						statusValueHo = getDecMaCount(invoiceNumber, po_num, con);
					}

					if (statusValueHo) {
						maCount = getmaCount(invoiceNumber, po_num, con);
						if (onHoldCounter > 0) {
							Status = "O";
						} else if ((maCount == 0)) {
							Status = "A";
						} else {
							Status = "M";
						}
					}
				} else if (overAllStatus.equalsIgnoreCase("M")) {
					maCount = getmaCount(invoiceNumber, po_num, con);
					if ((maCount == 0)) {
						Status = "A";
					} else {
						statusValueHo = getDecMaCount(invoiceNumber, po_num, con);
						if (statusValueHo) {
							maCount = getmaCount(invoiceNumber, po_num, con);
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
					if (overAllStatus.equalsIgnoreCase("A")) {
						Status = "A";
					}
				}
			} else if (managerstatus.equalsIgnoreCase("R")) {
				allManagerStatus = updateManagerStatus(invoiceNumber, po_num, managerId, con);
				if (allManagerStatus) {
					Status = "R";
				} else {
					Status = "R";
				}
				String statusmessage = "";
				String getorderdetails = getandupdateordernumber(invoiceNumber, po_num, con);
				if (!getorderdetails.equalsIgnoreCase("NA")) {
					statusmessage = getorderdetails;
				}
				Status = "R";
			} else if (managerstatus.equalsIgnoreCase("M")) {
				overAllStatus = getoverAllStatus(invoiceNumber, po_num, con);
				if (overAllStatus.equalsIgnoreCase("A")) {
					statusValueHo = getIncMaCount(invoiceNumber, po_num, con);
					Status = "M";
				} else if (overAllStatus.equalsIgnoreCase("O")) {
					statusValueHo = true;
					if (onHoldCounter > 1) {
						statusValueHo = getdecHoCountOnly(invoiceNumber, po_num, con);
					}
					if (statusValueHo) {
						holdCount = getholdcount(invoiceNumber, po_num, con);
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
				overAllStatus = getoverAllStatus(invoiceNumber, po_num, con);
				if (overAllStatus.equalsIgnoreCase("A")) {
					statusValueHo = getIncHOMaCount(invoiceNumber, po_num, con);
					if (statusValueHo) {
						Status = "O";
					} else {
						Status = "M";
					}
				} else if (overAllStatus.equalsIgnoreCase("M")) {
					statusValueHo = getIncHoCountOnly(invoiceNumber, po_num, con);
					if (statusValueHo) {
						Status = "O";
					} else {
						Status = "M";
					}
				} else {
					statusValueHo = getIncHoCountOnly(invoiceNumber, po_num, con);
					Status = "O";
				}
			} else {
				Status = "M";
			}
			String summaryStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";
			ps = con.prepareStatement(summaryStatus);
			ps.setString(1, Status);
			ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(3, invoiceNumber);
			ps.setString(4, po_num);
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
				// flag = sendMailtoManager(invoiceNumber, po_num, managerId, con);
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
			log.error("getManagerApprvalStatusall() :", dxp.fillInStackTrace());
			log.error("Get Cause", dxp.getCause());

		} catch (Exception e) {
			log.error("getManagerApprvalStatusall() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("error", e.getMessage());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		jsonArray.add(responsejson);
		return jsonArray;
	}

	public String getandupdateordernumber(String invoiceNumber, String po_num, Connection con)
			throws SQLException, DXPortalException {

		int value = 0;

		String orderdetailstatus = "Select DC from  DELIVERYSUMMARY where INVOICENUMBER=? and PONUMBER =?";
		String updateorderdetailstatus = "update DELIVERYSUMMARY set DC=? where INVOICENUMBER=? and PONUMBER= ?";
		String updatepoeventdetails = "update poeventdetails set STATUS=? where ORDERNUMBER=? and PONumber=?";

		ArrayList<String> ordernumberlist = new ArrayList<String>();
		HashMap<String, ArrayList<String>> poData = new HashMap<String, ArrayList<String>>();
		String flag = "success";
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(orderdetailstatus);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				ordernumberlist.add(rs.getString("ORDERNUMBER"));
			}
			rs.close();
			ps.close();
			String ordernumber = ordernumberlist.get(0);
			String tempordernumber = ordernumber;

			if (!ordernumber.isEmpty() && (!ordernumber.equalsIgnoreCase(null))
					&& (!ordernumber.equalsIgnoreCase("-"))) {

				ps = con.prepareStatement(updateorderdetailstatus);
				ps.setString(1, "-");
				ps.setString(2, invoiceNumber);
				ps.setString(3, po_num);
				value = ps.executeUpdate();
				ps.close();
				if (value > 0) {
					ps = con.prepareStatement(updatepoeventdetails);
					ps.setString(1, "A");
					ps.setString(2, tempordernumber);
					ps.setString(3, po_num);
					value = ps.executeUpdate();
					ps.close();
				}
			} else {
				flag = "NA";
			}
		} catch (Exception e) {
			log.error("getandupdateordernumber() :", e.fillInStackTrace());

			throw new DXPortalException("Error while updating deliverysummery !!",
					"Error in getandupdateordernumber()");
		}
		return flag;
	}

	String getoverAllStatus(String invoiceNumber, String po_num, Connection con) throws SQLException {

		String statusall = "select OVERALLSTATUS from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";

		String status = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(statusall);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				status = rs.getString("OVERALLSTATUS");
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getoverAllStatus() :", e.fillInStackTrace());

			status = "E";
		} finally {

		}
		return status;
	}

	boolean getdecHoCount(String invoiceNumber, String po_num, Connection con) throws SQLException, DXPortalException {

		String decMaCountvalue = "select MACOUNT,HOLDCOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		String decMaCount = "UPDATE PONINVOICESUMMERY SET MACOUNT=?,HOLDCOUNT=? where INVOICENUMBER =? and PONUMBER=? ";
		int count = 0;
		int Hcount = 0;
		int MaCount = 0;
		int HoCount = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(decMaCountvalue);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				MaCount = rs.getInt("MACount");
				HoCount = rs.getInt("HoldCount");
			}
			rs.close();
			ps.close();
			count = MaCount - 1;
			Hcount = HoCount - 1;

			ps = con.prepareStatement(decMaCount);
			ps.setInt(1, count);
			ps.setInt(2, Hcount);
			ps.setString(3, invoiceNumber);
			ps.setString(4, po_num);
			ps.executeUpdate();
			ps.close();
			return true;

		} catch (Exception e) {
			log.error("getdecHoCount() :", e.fillInStackTrace());

			throw new DXPortalException("Error in updating manager hold count", " Error in getdecHoCount");

		}
	}

	private boolean decreaseHoldCount(String invoiceNumber, String po_num, Connection con)
			throws SQLException, DXPortalException {

		String decMaCountvalue = "select HOLDCOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		String decMaCount = "UPDATE PONINVOICESUMMERY SET HOLDCOUNT=? where INVOICENUMBER =? and PONUMBER=? ";
		int count = 0;
		int Hcount = 0;
		int MaCount = 0;
		int HoCount = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(decMaCountvalue);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				HoCount = rs.getInt("HoldCount");
			}
			rs.close();
			ps.close();
			Hcount = HoCount - 1;

			ps = con.prepareStatement(decMaCount);
			ps.setInt(1, Hcount);
			ps.setString(2, invoiceNumber);
			ps.setString(3, po_num);
			ps.executeUpdate();
			ps.close();
			return true;

		} catch (Exception e) {
			log.error("decreaseHoldCount() :", e.fillInStackTrace());

			throw new DXPortalException("Error in updating manager hold count", " Error in getdecHoCount");
		}
	}

	int getmaCount(String invoiceNumber, String po_num, Connection con) throws SQLException {

		String holdcount = "select MACOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		int count = 0;

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(holdcount);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				count = rs.getInt("MACOUNT");
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getmaCount() :", e.fillInStackTrace());

		} finally {

		}
		return count;
	}

	private boolean updateManagerStatus(String invoiceNumber, String po_num, String managerId, Connection con)
			throws SQLException, DXPortalException {

		String po_status = "UPDATE invoiceapproval set STATUS=?,MODIFIEDDATE=? "
				+ "where INVOICENUMBER=? and PONUMBER =? and EUMANAGER =?";
		String summaryStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? "
				+ "where INVOICENUMBER=? and PONUMBER =?  ";
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(po_status);
			ps.setString(1, "R");
			ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(3, invoiceNumber);
			ps.setString(4, po_num);
			ps.setString(5, managerId);
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(summaryStatus);
			ps.setString(1, "R");
			ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(3, invoiceNumber);
			ps.setString(4, po_num);
			ps.executeUpdate();
			ps.close();
			return true;

		} catch (Exception e) {
			log.error("updateManagerStatus() :", e.fillInStackTrace());

			throw new DXPortalException("Error while updating poninvoicesummery and invoiceapproval !!",
					" Error in updateManagerStatus()");
		}
	}

	boolean getIncMaCount(String invoiceNumber, String po_num, Connection con) throws SQLException, DXPortalException {

		String decMaCountvalue = "select MACOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		String decMaCount = "UPDATE PONINVOICESUMMERY SET MACOUNT=? where INVOICENUMBER =? and PONUMBER=? ";

		int count = 0;
		int MaCount = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(decMaCountvalue);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				MaCount = rs.getInt("MACount");
			}
			rs.close();
			ps.close();
			count = MaCount + 1;
			ps = con.prepareStatement(decMaCount);
			ps.setInt(1, count);
			ps.setString(2, invoiceNumber);
			ps.setString(3, po_num);
			ps.executeUpdate();
			ps.close();
			return true;
		} catch (Exception e) {
			log.error("getIncMaCount() :", e.fillInStackTrace());

			throw new DXPortalException("Error while updating poninvoicesummary !!", "Error in getIncMaCount()");
		}
	}

	boolean getdecHoCountOnly(String invoiceNumber, String po_num, Connection con)
			throws SQLException, DXPortalException {

		String decMaCountvalue = "select HOLDCOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		String decMaCount = "UPDATE PONINVOICESUMMERY SET HOLDCOUNT=? where INVOICENUMBER =? and PONUMBER=? ";
		int Hcount = 0;
		int HoCount = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(decMaCountvalue);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				HoCount = rs.getInt("HoldCount");
			}
			rs.close();
			ps.close();

			Hcount = HoCount - 1;

			ps = con.prepareStatement(decMaCount);
			ps.setInt(1, Hcount);
			ps.setString(2, invoiceNumber);
			ps.setString(3, po_num);
			ps.executeUpdate();
			ps.close();
		} catch (Exception e) {
			log.error("getdecHoCountOnly() :", e.fillInStackTrace());

			throw new DXPortalException("Error while updating poninvoicesummery !!", "Error in getdecHoCountOnly()");
		}
		return true;
	}

	int getholdcount(String invoiceNumber, String po_num, Connection con) throws SQLException {

		String maCount = "select HOLDCOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		int count = 0;

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(maCount);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				count = rs.getInt("HOLDCOUNT");
			}
			rs.close();
			ps.close();

		} catch (Exception e) {
			log.error("getholdcount() :", e.fillInStackTrace());

		} finally {

		}
		return count;
	}

	boolean getIncHOMaCount(String invoiceNumber, String po_num, Connection con)
			throws SQLException, DXPortalException {

		String decMaCountvalue = "select MACOUNT,HOLDCOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		String decMaCount = "UPDATE PONINVOICESUMMERY SET MACOUNT=?,HOLDCOUNT=? where INVOICENUMBER =? and PONUMBER=? ";

		int count = 0;
		int Hcount = 0;
		int MaCount = 0;
		int HoCount = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			ps = con.prepareStatement(decMaCountvalue);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				MaCount = rs.getInt("MACount");
				HoCount = rs.getInt("HoldCount");
			}
			rs.close();
			ps.close();
			count = MaCount + 1;
			Hcount = HoCount + 1;

			ps = con.prepareStatement(decMaCount);
			ps.setInt(1, count);
			ps.setInt(2, Hcount);
			ps.setString(3, invoiceNumber);
			ps.setString(4, po_num);
			ps.executeUpdate();
			ps.close();
			return true;

		} catch (Exception e) {
			log.error("getIncHOMaCount() :", e.fillInStackTrace());

			throw new DXPortalException("Error while updating PONINVOICESUMMERY !!", "Error in getIncHOMaCount()");
		}
	}

	boolean getIncHoCountOnly(String invoiceNumber, String po_num, Connection con) throws DXPortalException {

		String decMaCountvalue = "select HOLDCOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		String decMaCount = "UPDATE PONINVOICESUMMERY SET HOLDCOUNT=? where INVOICENUMBER =? and PONUMBER=? ";
		int Hcount = 0;
		int HoCount = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = con.prepareStatement(decMaCountvalue);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				HoCount = rs.getInt("HOLDCOUNT");
			}
			rs.close();
			ps.close();
			Hcount = HoCount + 1;
			int summaryValue = 0;
			ps = con.prepareStatement(decMaCount);
			ps.setInt(1, Hcount);
			ps.setString(2, invoiceNumber);
			ps.setString(3, po_num);
			summaryValue = ps.executeUpdate();
			ps.close();
		} catch (Exception e) {
			log.error("getIncHoCountOnly() :", e.fillInStackTrace());

			throw new DXPortalException("Error while updating poninvoicesummery !!", "Error in getIncHoCountOnly()");
		}
		return true;
	}

	public JSONArray getInvoiceBankDetails(String bid) throws SQLException {

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

		String po_data = "Select B.BankOID,B.BankCode,B.AccountType,B.AccountNumber, A.attributevalue "
				+ "from businesspartnerbankdetails B, BUSINESSPARTNERATTRIBUTES A "
				+ "where B.businesspartneroid = A.businesspartneroid and A.attributetext ='IFSC_NUMBER' "
				+ "and B.businesspartneroid = ?";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, bid);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("BANKOID", rs.getString("BankOID"));
				poData.put("BANKCODE", rs.getString("BankCode"));
				poData.put("ACCOUNTTYPE", rs.getString("AccountType"));
				poData.put("ACCOUNTNUMBER", rs.getString("AccountNumber"));
				poData.put("ATTRIBUTEVALUE", rs.getString("attributevalue"));
				POList.add(poData);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			log.error("getInvoiceBankDetails() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0) {
			responsejson.put("message", "Success");
			responsejson.put("poData", POList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}

		return jsonArray;
	}

	public String getrejectStatusDetails(String invoice, String po_num, Connection con)
			throws SQLException, DXPortalException {

		String businessId = "select LINEITEMNUMBER,LINEITEMTOTALQUANTITY,BUSSINESSPARTNEROID "
				+ "from DELIVERYSUMMARY where PONUMBER =? and INVOICENUMBER=?";
		String poeventdetails = " select balance_qty from poeventdetails where "
				+ "PONumber=? and LineItemNumber=? and BusinessPartnerOID=?";
		String lineItemNumber = null;
		int quantity = 0;
		int businessPartnerOID = 0;
		String balanceQyt = null;
		ArrayList<String> rejectlist = new ArrayList<String>();
		HashMap<String, ArrayList<String>> poData = new HashMap<String, ArrayList<String>>();

		PreparedStatement ps = null;
		ResultSet rs = null;
		ps = con.prepareStatement(businessId);
		ps.setString(1, po_num);
		ps.setString(2, invoice);
		rs = ps.executeQuery();
		while (rs.next()) {
			rejectlist.add(rs.getString("BusinessPartnerOID"));
			rejectlist.add(rs.getString("LineItemNumber"));
			rejectlist.add(rs.getString("Quantity"));
		}
		rs.close();
		ps.close();
		for (int i = 0; i < rejectlist.size();) {
			businessPartnerOID = Integer.parseInt(rejectlist.get(i));
			lineItemNumber = rejectlist.get(i + 1);
			quantity = Integer.parseInt(rejectlist.get(i + 2));
			getbalanceDetails(businessPartnerOID, lineItemNumber, quantity, po_num, con);
			i = i + 3;
		}
		return "success";
	}

	private void getbalanceDetails(int businessPartnerOID, String lineItemNumber, int quantity, String po_num,
			Connection con) throws SQLException, DXPortalException {

		String poeventdetails = " select balance_qty from poeventdetails where "
				+ "PONumber=? and LineItemNumber=? and BusinessPartnerOID=?";
		String rollbackbalanceQyt = null;
		int newbal = 0;
		int oldbal = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(poeventdetails);
			ps.setString(1, po_num);
			ps.setString(2, lineItemNumber);
			ps.setInt(3, businessPartnerOID);
			rs = ps.executeQuery();
			while (rs.next()) {
				oldbal = Integer.parseInt(rs.getString("balance_qty"));
				newbal = oldbal + quantity;
				getUpdatebalanceDetails(businessPartnerOID, lineItemNumber, po_num, newbal, con);
			}
			rs.close();
			ps.close();

		} finally {

		}
	}

	private void getUpdatebalanceDetails(int businessPartnerOID, String lineItemNumber, String po_num, int newbal,
			Connection con) throws SQLException, DXPortalException {

		String balanceUpdate = " Update poeventdetails set balance_qty=? "
				+ "where PONumber=? and LineItemNumber=? and BusinessPartnerOID=?";

		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(balanceUpdate);
			ps.setString(1, String.valueOf(newbal));
			ps.setString(2, po_num);
			ps.setString(3, lineItemNumber);
			ps.setInt(4, businessPartnerOID);
			ps.executeUpdate();
			ps.close();

		} catch (Exception e) {
			log.error("getUpdatebalanceDetails() :", e.fillInStackTrace());

			throw new DXPortalException("Error in updating balance !!", "Error in getUpdatebalanceDetails()");
		} finally {
		}

	}

	public JSONArray addMultipleManagerwithConfirmer(List<String> manager, List<String> confirmers, HttpSession session)
			throws SQLException {

		int i = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean status = false;
		try {
			String getbuyerid = "Select BUYERID from invoiceapproval where INVOICENUMBER =? and PONUMBER=? ";

			String sqlUpdate = "insert into invoiceapproval (VENDORID,INVOICENUMBER,INVOICEDATE,PONUMBER,"
					+ "BUYERID,ENDUSEID,ENDUSERSTATUS,EUMANAGER,STATUS,STAGE,MODIFIEDDATE,TOTALAMOUNT,PROXY,"
					+ "ACTIONBY)" + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			String sqlUpdateManager = "UPDATE invoiceapproval set ENDUSEID = ?, EUMANAGER=?, PROXY = ?,ACTIONBY=?,STAGE=?,ENDUSERSTATUS=? "
					+ " where INVOICENUMBER =? and PONUMBER=?";

			String storeKepeer = (String) session.getAttribute("shopkepeer");
			String emailId = (String) session.getAttribute("email");
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String buyerid = "";
			for (i = 0; i < manager.size(); i++) {
				String[] arr = manager.get(i).split(",");
				if (i == 0) {

					ps = con.prepareStatement(getbuyerid);

					ps.setString(1, arr[4]);
					ps.setString(2, arr[6]);
					rs = ps.executeQuery();
					while (rs.next()) {
						buyerid = rs.getString("BUYERID");
					}
					ps.close();
					ps = con.prepareStatement(sqlUpdateManager);

					ps.setString(1, arr[0]);
					ps.setString(2, arr[2].trim());
					ps.setString(3, null);
					ps.setString(4, arr[9]);
					ps.setString(5, arr[7]);
					ps.setString(6, arr[1]);
					ps.setString(7, arr[4]);
					ps.setString(8, arr[6]);
					ps.executeUpdate();
					ps.close();

				} else {
					ps = con.prepareStatement(sqlUpdate);
					ps.setString(1, arr[11]);
					ps.setString(2, arr[4]);
					ps.setDate(3, new java.sql.Date(new SimpleDateFormat("yyyy-MM-dd").parse(arr[3]).getTime()));
					ps.setString(4, arr[6]);
					ps.setString(5, buyerid);
					ps.setString(6, arr[0]);
					ps.setString(7, arr[1]);
					ps.setString(8, arr[2].trim());
					ps.setString(9, "M");
					ps.setString(10, arr[7]);
					ps.setTimestamp(11, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(12, arr[10]);
					ps.setString(13, null);
					ps.setString(14, arr[9]);
					ps.executeUpdate();
					ps.close();
				}
			}

			String[] arr = manager.get(0).split(",");
			for (int b = 0; b < confirmers.size(); b++) {

				ps = con.prepareStatement(sqlUpdate);
				ps.setString(1, arr[11]);
				ps.setString(2, arr[4]);
				ps.setDate(3, new java.sql.Date(new SimpleDateFormat("yyyy-MM-dd").parse(arr[3]).getTime()));
				ps.setString(4, arr[6]);
				ps.setString(5, buyerid);
				ps.setString(6, arr[0]);
				ps.setString(7, arr[1]);
				ps.setString(8, confirmers.get(b).trim());
				ps.setString(9, "CM");
				ps.setString(10, arr[7]);
				ps.setTimestamp(11, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(12, arr[10]);
				ps.setString(13, null);
				ps.setString(14, arr[9]);
				ps.executeUpdate();
				ps.close();
			}

			String overallStatus = "";
			if (arr[1].equalsIgnoreCase("O")) {
				overallStatus = "O";
			} else if (arr[1].equalsIgnoreCase("P")) {
				overallStatus = "P";
			} else {
				overallStatus = arr[8];
			}
			int macount = 0;
			macount = Integer.parseInt(arr[5]);
			int hocount = 0;
			if (overallStatus.equalsIgnoreCase("O")) {
				hocount = macount;
			}
			String summaryUpdate = "UPDATE PONINVOICESUMMERY SET MACOUNT = ?, HOLDCOUNT = ?, OVERALLSTATUS = ?,MODIFIEDON =? "
					+ "where INVOICENUMBER =? and BASEPO=? ";
			int summaryValue = 0;
			ps = con.prepareStatement(summaryUpdate);
			ps.setInt(1, macount);
			ps.setInt(2, hocount);
			ps.setString(3, overallStatus);
			ps.setTimestamp(4, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(5, arr[4]);
			ps.setString(6, arr[6]);
			summaryValue = ps.executeUpdate();
			ps.close();
			con.commit();
			if (!"P".equalsIgnoreCase(arr[1])) {
				status = sendMailtoCentralOps(arr[4], arr[6], arr[0], con);
			}
			responsejson.put("emailstatus", status);
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);
		} catch (Exception e) {
			log.error("addMultipleManagerwithConfirmer() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("error", "Error while adding Managers");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray getaddedMultipleManager(List<Invoiceapproval> manager, HttpSession session) throws SQLException {

		boolean result;
		result = manager.isEmpty();
		if (result == true) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		int i = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			String getbuyerid = "Select BUYERID from invoiceapproval where INVOICENUMBER =? and PONUMBER=? ";

			String sqlUpdate = "insert into invoiceapproval (VENDORID,INVOICENUMBER,INVOICEDATE,PONUMBER,"
					+ "BUYERID,ENDUSEID,ENDUSERSTATUS,EUMANAGER,STATUS,STAGE,MODIFIEDDATE,TOTALAMOUNT,PROXY,"
					+ "ACTIONBY)" + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			String sqlUpdateManager = "UPDATE invoiceapproval set ENDUSEID = ?, EUMANAGER=?, PROXY = ?,ACTIONBY=? "
					+ " where INVOICENUMBER =? and PONUMBER=?";

			String storeKepeer = (String) session.getAttribute("shopkepeer");
			String emailId = (String) session.getAttribute("email");
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String buyerid = "";
			for (i = 0; i < manager.size(); i++) {
				if (i == 0) {
					ps = con.prepareStatement(getbuyerid);

					ps.setString(1, manager.get(0).getInvoicenumber());
					ps.setString(2, manager.get(0).getPonumber());
					rs = ps.executeQuery();
					while (rs.next()) {
						buyerid = rs.getString("BUYERID");
					}
					ps.close();
					ps = con.prepareStatement(sqlUpdateManager);
					ps.setString(1, manager.get(i).getEnduserid());
					ps.setString(2, manager.get(i).getEumanager().trim());
					ps.setString(3, null);
					ps.setString(4, manager.get(i).getStorekeeperaction());
					ps.setString(5, manager.get(i).getInvoicenumber());
					ps.setString(6, manager.get(i).getPonumber());
					ps.executeUpdate();
					ps.close();

				} else {

					ps = con.prepareStatement(sqlUpdate);
					ps.setString(1, manager.get(i).getVendorid());
					ps.setString(2, manager.get(i).getInvoicenumber());
					ps.setDate(3, new java.sql.Date(
							new SimpleDateFormat("yyyy-MM-dd").parse(manager.get(i).getInvoicedate()).getTime()));
					ps.setString(4, manager.get(i).getPonumber());
					ps.setString(5, buyerid);
					ps.setString(6, manager.get(i).getEnduserid());
					ps.setString(7, manager.get(i).getEnduserstatus());
					ps.setString(8, manager.get(i).getEumanager().trim());
					ps.setString(9, "M");
					ps.setString(10, manager.get(i).getStage());
					ps.setTimestamp(11, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(12, manager.get(i).getTotalamount());
					ps.setString(13, null);
					ps.setString(14, manager.get(i).getStorekeeperaction());

					ps.executeUpdate();
					ps.close();
				}
			}
			String sqlUpdate1 = "UPDATE invoiceapproval set ENDUSERSTATUS = ?, STATUS = ? "
					+ " , STAGE =? where INVOICENUMBER = ? and PONUMBER = ? ";

			ps = con.prepareStatement(sqlUpdate1);
			ps.setString(1, manager.get(0).getEnduserstatus());
			ps.setString(2, manager.get(0).getStatus());
			ps.setString(3, manager.get(0).getStage());
			ps.setString(4, manager.get(0).getInvoicenumber());
			ps.setString(5, manager.get(0).getPonumber());
			ps.executeUpdate();
			ps.close();
			String overallStatus = "";
			if (manager.get(0).getEnduserstatus().equalsIgnoreCase("O")) {
				overallStatus = "O";
			} else if (manager.get(0).getEnduserstatus().equalsIgnoreCase("P")) {
				overallStatus = "P";
			} else {
				overallStatus = manager.get(0).getStatus();
			}
			int macount = 0;
			macount = Integer.parseInt(manager.get(0).getManagercount());
			int hocount = 0;
			if (overallStatus.equalsIgnoreCase("O")) {
				hocount = macount;
			}

			String summaryUpdate = "UPDATE PONINVOICESUMMERY SET MACOUNT = ?, HOLDCOUNT = ?, OVERALLSTATUS = ?,MODIFIEDON =? "
					+ "where INVOICENUMBER =? and BASEPO=? ";
			int summaryValue = 0;
			ps = con.prepareStatement(summaryUpdate);
			ps.setInt(1, macount);
			ps.setInt(2, hocount);
			ps.setString(3, overallStatus);
			ps.setTimestamp(4, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(5, manager.get(0).getInvoicenumber());
			ps.setString(6, manager.get(0).getPonumber());
			summaryValue = ps.executeUpdate();
			ps.close();
			boolean flag = false;
			if ((!manager.get(0).getEnduserstatus().equalsIgnoreCase("O"))
					&& (!manager.get(0).getEnduserstatus().equalsIgnoreCase("P"))) {

				String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
						+ "MODIFIEDTIME,MODIFIEDBY)" + " values(?,?,?,?,?,?)";
				ps = con.prepareStatement(insertaudit);
				ps.setString(1, manager.get(0).getInvoicenumber());
				ps.setString(2, manager.get(0).getPonumber());
				ps.setString(3, "");
				ps.setString(4, manager.get(0).getStatus());
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(6, manager.get(0).getEnduserid());
				ps.executeUpdate();
				ps.close();
			}

			con.commit();
			if ("A".equalsIgnoreCase(manager.get(0).getEnduserstatus())) {
				// flag = sendMailtoManager(manager.get(0).getInvoicenumber(),
				// manager.get(0).getPonumber(),
				// manager.get(0).getEnduserid(), con);
				flag = true;
			}
			responsejson.put("emailstatus", flag);
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("getaddedMultipleManager() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("error", "Error while adding Managers");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	boolean getDecMaCount(String invoiceNumber, String po_num, Connection con) throws SQLException, DXPortalException {

		String decMaCountvalue = "select MACOUNT from PONINVOICESUMMERY where InvoiceNumber=? and PONumber=?";
		String summaryUpdate = "UPDATE PONINVOICESUMMERY SET MACOUNT=? where INVOICENUMBER =? and PONUMBER=? ";
		int count = 0;
		int MaCount = 0;
		int summaryValue = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(decMaCountvalue);
			ps.setString(1, invoiceNumber);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				MaCount = rs.getInt("MACOUNT");
			}
			rs.close();
			ps.close();
			count = MaCount - 1;
			ps = con.prepareStatement(summaryUpdate);
			ps.setInt(1, count);
			ps.setString(2, invoiceNumber);
			ps.setString(3, po_num);
			summaryValue = ps.executeUpdate();
			ps.close();
			return true;

		} catch (Exception e) {
			log.error("getDecMaCount() :", e.fillInStackTrace());

			throw new DXPortalException("Error while updating the hold count", "Error in getDecMaCount()");
		}

	}

	public JSONArray getaddedMultipleManagerList(String invoice, String po_num) throws SQLException {

		String qdata = "SELECT VENDORID,INVOICENUMBER,INVOICEDATE,PONUMBER,BUYERID,ENDUSEID,"
				+ "ENDUSERSTATUS,EUMANAGER,STATUS,STAGE,MODIFIEDDATE,TOTALAMOUNT FROM "
				+ "invoiceapproval where INVOICENUMBER =? and PONUMBER=? ORDER BY DECODE(STATUS,'CM','X','M','Y','A'), MODIFIEDDATE ASC ";

		boolean result;
		result = Validation.StringChecknull(invoice);
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

		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
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
		} catch (SQLException e) {
			log.error("getaddedMultipleManagerList() :", e.fillInStackTrace());

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

	public JSONArray getdetails(String portalid) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(portalid);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String column = "";
		String sql = "select DISTINCT PERSONNELNUMBER,PORTALID,EMAILID from EMPLOYEEDETAILS  where PORTALID=?";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, portalid);
			rs = ps.executeQuery();

			ArrayList<HashMap<String, String>> detailslist = new ArrayList<HashMap<String, String>>();

			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("SAPNUMBER", rs.getString("PERSONNELNUMBER"));
				poData.put("PORTALID", rs.getString("PORTALID"));
				poData.put("EMAILID", rs.getString("EMAILID"));
				detailslist.add(poData);
			}

			if (detailslist.size() > 0) {
				responsejson.put("managerdetails", detailslist);
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
			}

		} catch (SQLException e) {
			log.error("getdetails() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			responsejson.put("error", e.getLocalizedMessage());
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getportaltype(String emailid, HttpSession session) throws SQLException {

		boolean payee = false;
		String checkportalquery = "select * from BCCLPAYABLES WHERE PAYEREMAILID=? AND STATUS=?";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(checkportalquery);
			ps.setString(1, emailid);
			ps.setString(2, "A");
			rs = ps.executeQuery();

			if (rs.next()) {
				payee = true;
			}
			rs.close();
			ps.close();
			if (payee == true) {
				responsejson.put("payeestatus", "PRESENT");
				session.setAttribute("payer", "true");
				session.setAttribute("mode", "payer");
				responsejson.put("mode", "payer");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			String getdetailsquery = "Select * from PODETAILS where CONTACTPERSONEMAILID = ? OR REQUSITIONER = ? ";

			ps = con.prepareStatement(getdetailsquery);
			ps.setString(1, emailid);
			ps.setString(2, emailid);
			rs = ps.executeQuery();
			List<HashMap<String, String>> listofpodetails = new ArrayList<HashMap<String, String>>();
			while (rs.next()) {
				HashMap<String, String> usertype = new HashMap<String, String>();
				usertype.put("BUYER", rs.getString("CONTACTPERSONEMAILID"));
				usertype.put("REQUSITIONER", rs.getString("REQUSITIONER"));
				listofpodetails.add(usertype);
			}
			rs.close();
			ps.close();
			boolean buyer = false;
			for (int ab = 0; ab < listofpodetails.size(); ab++) {
				if (listofpodetails.get(ab).get("BUYER") != null) {
					if (listofpodetails.get(ab).get("BUYER").equalsIgnoreCase(emailid)) {
						buyer = true;
					}
				}
			}
			boolean enduser = false;
			for (int ab = 0; ab < listofpodetails.size(); ab++) {
				if (listofpodetails.get(ab).get("REQUSITIONER") != null) {
					if (listofpodetails.get(ab).get("REQUSITIONER").equalsIgnoreCase(emailid)) {
						enduser = true;
					}
				}

			}
			
			String getLocationquery = "Select STOREKEEPEREMILID from LOCATIONMASTER where STOREKEEPEREMILID = ? ";

			ps = con.prepareStatement(getLocationquery);
			ps.setString(1, emailid);
			rs = ps.executeQuery();
			while (rs.next()) {
				enduser = true;
				break;
			}
			rs.close();
			ps.close();
			
			if (enduser == true) {
				session.setAttribute("mode", "enduser");
				responsejson.put("mode", "enduser");
				responsejson.put("buyerstatus", "ABSENT");
				responsejson.put("enduserstatus", "PRESENT");
				responsejson.put("functionalDirector", "ABSENT");
			} else if (buyer == true) {
				session.setAttribute("mode", "buyer");
				responsejson.put("mode", "buyer");
				responsejson.put("buyerstatus", "PRESENT");
				responsejson.put("enduserstatus", "ABSENT");
				responsejson.put("functionalDirector", "ABSENT");
				// return jsonArray;
			} 
			/*else {
				session.setAttribute("mode", "internalbcclportal");
				responsejson.put("mode", "internalbcclportal");
				responsejson.put("payeestatus", "ABSENT");
				responsejson.put("functionalDirector", "ABSENT");
				jsonArray.add(responsejson);
			}
			*/
			jsonArray.add(responsejson);
			return jsonArray;

			/*
			 * if (!payee) {
			 * 
			 * log.info("Logged user is other than Payer."); boolean funcDirFlag = false;
			 * String funcDirQuery =
			 * "select * from functionaldirectors WHERE EMAILID = ? AND STATUS = ?";
			 * 
			 * con = DBConnection.getConnection(); ps = con.prepareStatement(funcDirQuery);
			 * ps.setString(1, emailid); ps.setString(2, "A"); rs = ps.executeQuery();
			 * 
			 * if (rs.next()) { funcDirFlag = true; } rs.close(); ps.close();
			 * 
			 * if (funcDirFlag == true) {
			 * 
			 * log.info("Functional Director");
			 * 
			 * session.setAttribute("functionalDirector", "true");
			 * responsejson.put("functionalDirector", "PRESENT"); //
			 * jsonArray.add(responsejson);
			 * 
			 * } else { log.info("Not a Functional Director"); } }
			 */

		} catch (SQLException e) {
			log.error("getportaltype() :", e.fillInStackTrace());
			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	/*
	 * public JSONArray getemailidofpersontobesearched(String text, String actionby,
	 * String plant, String material, String userid) throws SQLException {
	 * 
	 * boolean result; result = Validation.StringChecknull(text); if (result ==
	 * false) { responsejson.put("validation", "validation Fail");
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); return
	 * jsonArray; } String sql = ""; Connection con = null; PreparedStatement ps =
	 * null; ResultSet rs = null; int inventorycount = 0; try { con =
	 * DBConnection.getConnection(); ArrayList<HashMap<String, String>> searchlist =
	 * new ArrayList<HashMap<String, String>>();
	 * 
	 * if (plant.equalsIgnoreCase("NA")) { if (actionby.equalsIgnoreCase("true")) {
	 * sql =
	 * "select * from LIKEDETAILS WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID"
	 * ; } else { sql =
	 * "select * from LIKEDETAILSGMNABOVE WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID"
	 * ; } } else { if (actionby.equalsIgnoreCase("true")) {
	 * 
	 * String inventorychecksql =
	 * "select count(*) as counter from INVENTORYUSERLIST where PLANT=? AND MTYP=? AND USERID=?"
	 * ; ps = con.prepareStatement(inventorychecksql); ps.setString(1, plant);
	 * ps.setString(2, material); ps.setString(3, userid); rs = ps.executeQuery();
	 * 
	 * if (rs.next()) { inventorycount = rs.getInt("counter"); } rs.close();
	 * ps.close(); if (inventorycount > 0) { sql =
	 * "select * from LIKEDETAILS WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID"
	 * ; } else { sql =
	 * "select * from LIKEDETAILSGMNABOVE WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID"
	 * ; }
	 * 
	 * } else { sql =
	 * "select * from LIKEDETAILSGMNABOVE WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID"
	 * ; } }
	 * 
	 * ps = con.prepareStatement(sql); ps.setString(1, text + "%"); rs =
	 * ps.executeQuery();
	 * 
	 * while (rs.next()) { HashMap<String, String> listData = new HashMap<String,
	 * String>(); listData.put("TIMESCAPEUSEROID",
	 * rs.getString("TIMESCAPEUSEROID")); listData.put("NAME",
	 * rs.getString("NAME")); listData.put("EMAILID", rs.getString("EMAILID"));
	 * listData.put("COMPANYNAME", rs.getString("COMPANYNAME"));
	 * listData.put("BRANCHNAME", rs.getString("BRANCHNAME"));
	 * listData.put("LOCATIONNAME", rs.getString("LOCATIONNAME"));
	 * listData.put("DEPARTMENT", rs.getString("DEPARTMENT"));
	 * listData.put("DESIGNATION", rs.getString("DESIGNATION"));
	 * listData.put("DIRECTPHONENO", rs.getString("DIRECTPHONENO"));
	 * listData.put("DIRECTPHONENO1", rs.getString("DIRECTPHONENO1"));
	 * listData.put("DIRECTPHONENO2", rs.getString("DIRECTPHONENO2"));
	 * listData.put("EXTENSION", rs.getString("EXTENSION")); listData.put("HOTLINE",
	 * rs.getString("HOTLINE")); listData.put("MOBILE", rs.getString("MOBILE"));
	 * listData.put("FAX", rs.getString("FAX")); listData.put("RESIDENCEPHONENO",
	 * rs.getString("RESIDENCEPHONENO")); listData.put("PAYROLLTYPETEXT",
	 * rs.getString("PAYROLLTYPETEXT"));
	 * 
	 * searchlist.add(listData); } rs.close(); ps.close(); if (searchlist.size() >
	 * 0) { responsejson.put("searchdetailslist", searchlist);
	 * responsejson.put("message", "Success"); jsonArray.add(responsejson); } else {
	 * responsejson.put("message", "Fail"); jsonArray.add(responsejson); }
	 * 
	 * } catch (SQLException e) { log.error("getemailidofpersontobesearched() :",
	 * e.fillInStackTrace());
	 * 
	 * responsejson.put("message", "Network Issue"); jsonArray.add(responsejson); }
	 * finally { DBConnection.closeConnection(rs, ps, con); } return jsonArray; }
	 */
	public JSONArray getemailidofpersontobesearchedfordev(String text) {

		boolean result;
		result = Validation.StringChecknull(text);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		ArrayList<HashMap<String, String>> searchlist = new ArrayList<HashMap<String, String>>();

		HashMap<String, String> listData = new HashMap<String, String>();
		listData.put("TIMESCAPEUSEROID", "TIMESCAPEOID00000");
		listData.put("NAME", "Vikrant Desai");
		listData.put("EMAILID", "vikrant.desai@timesgroup.com");
		listData.put("COMPANYNAME", "COMPANY00000");
		listData.put("BRANCHNAME", "BRANCH00000");
		listData.put("LOCATIONNAME", "LOCATION00000");
		listData.put("DEPARTMENT", "DEPARTMENT00000");
		listData.put("DESIGNATION", "DESIGNATION00000");
		listData.put("DIRECTPHONENO", "PHONENO00000");
		listData.put("DIRECTPHONENO1", "PHONENO100000");
		listData.put("DIRECTPHONENO2", "DIRECTPHONENO200000");
		listData.put("EXTENSION", "EXTENSION00000");
		listData.put("HOTLINE", "HOTLINE00000");
		listData.put("MOBILE", "MOBILE00000");
		listData.put("FAX", "FAX00000");
		listData.put("RESIDENCEPHONENO", "RESIDENCE00000");
		listData.put("PAYROLLTYPETEXT", "PAYROLLTYPE00000");

		searchlist.add(listData);

		HashMap<String, String> listData1 = new HashMap<String, String>();
		listData1.put("TIMESCAPEUSEROID", "TIMESCAPEOID000001");
		listData1.put("NAME", "Dilip Kukreja");
		listData1.put("EMAILID", "dilip.kukreja@timesgroup.com");
		listData1.put("COMPANYNAME", "COMPANY00001");
		listData1.put("BRANCHNAME", "BRANCH00001");
		listData1.put("LOCATIONNAME", "LOCATION00001");
		listData1.put("DEPARTMENT", "DEPARTMENT00001");
		listData1.put("DESIGNATION", "DESIGNATION00001");
		listData1.put("DIRECTPHONENO", "PHONENO00001");
		listData1.put("DIRECTPHONENO1", "PHONENO100001");
		listData1.put("DIRECTPHONENO2", "DIRECTPHONENO200001");
		listData1.put("EXTENSION", "EXTENSION00001");
		listData1.put("HOTLINE", "HOTLINE00001");
		listData1.put("MOBILE", "MOBILE00001");
		listData1.put("FAX", "FAX00001");
		listData1.put("RESIDENCEPHONENO", "RESIDENCE00001");
		listData1.put("PAYROLLTYPETEXT", "PAYROLLTYPE00001");

		searchlist.add(listData1);

		HashMap<String, String> listData2 = new HashMap<String, String>();
		listData2.put("TIMESCAPEUSEROID", "TIMESCAPEOID00002");
		listData2.put("NAME", "Sachin Mehta");
		listData2.put("EMAILID", "sachin.mehta@timesgroup.com");
		listData2.put("COMPANYNAME", "COMPANY00002");
		listData2.put("BRANCHNAME", "BRANCH00002");
		listData2.put("LOCATIONNAME", "LOCATION00002");
		listData2.put("DEPARTMENT", "DEPARTMENT00002");
		listData2.put("DESIGNATION", "DESIGNATION00002");
		listData2.put("DIRECTPHONENO", "PHONENO00002");
		listData2.put("DIRECTPHONENO1", "PHONENO100002");
		listData2.put("DIRECTPHONENO2", "DIRECTPHONENO200002");
		listData2.put("EXTENSION", "EXTENSION00002");
		listData2.put("HOTLINE", "HOTLINE00002");
		listData2.put("MOBILE", "MOBILE00002");
		listData2.put("FAX", "FAX00002");
		listData2.put("RESIDENCEPHONENO", "RESIDENCE00002");
		listData2.put("PAYROLLTYPETEXT", "PAYROLLTYPE00002");

		searchlist.add(listData2);

		HashMap<String, String> listData3 = new HashMap<String, String>();
		listData3.put("TIMESCAPEUSEROID", "TIMESCAPEOID00003");
		listData3.put("NAME", "Sachin Kale");
		listData3.put("EMAILID", "sachin.kale@timesgroup.com");
		listData3.put("COMPANYNAME", "COMPANY00003");
		listData3.put("BRANCHNAME", "BRANCH00003");
		listData3.put("LOCATIONNAME", "LOCATION00003");
		listData3.put("DEPARTMENT", "DEPARTMENT00003");
		listData3.put("DESIGNATION", "DESIGNATION00003");
		listData3.put("DIRECTPHONENO", "PHONENO00003");
		listData3.put("DIRECTPHONENO1", "PHONENO100003");
		listData3.put("DIRECTPHONENO2", "DIRECTPHONENO200003");
		listData3.put("EXTENSION", "EXTENSION00003");
		listData3.put("HOTLINE", "HOTLINE00003");
		listData3.put("MOBILE", "MOBILE00003");
		listData3.put("FAX", "FAX00003");
		listData3.put("RESIDENCEPHONENO", "RESIDENCE00003");
		listData3.put("PAYROLLTYPETEXT", "PAYROLLTYPE00003");

		searchlist.add(listData3);

		HashMap<String, String> listData4 = new HashMap<String, String>();
		listData4.put("TIMESCAPEUSEROID", "TIMESCAPEOID00004");
		listData4.put("NAME", "Amit Deshpande");
		listData4.put("EMAILID", "amit.deshpande@timesgroup.com");
		listData4.put("COMPANYNAME", "COMPANY00004");
		listData4.put("BRANCHNAME", "BRANCH00004");
		listData4.put("LOCATIONNAME", "LOCATION00004");
		listData4.put("DEPARTMENT", "DEPARTMENT00004");
		listData4.put("DESIGNATION", "DESIGNATION00004");
		listData4.put("DIRECTPHONENO", "PHONENO00004");
		listData4.put("DIRECTPHONENO1", "PHONENO100004");
		listData4.put("DIRECTPHONENO2", "DIRECTPHONENO200004");
		listData4.put("EXTENSION", "EXTENSION00004");
		listData4.put("HOTLINE", "HOTLINE00004");
		listData4.put("MOBILE", "MOBILE00004");
		listData4.put("FAX", "FAX00004");
		listData4.put("RESIDENCEPHONENO", "RESIDENCE00004");
		listData4.put("PAYROLLTYPETEXT", "PAYROLLTYPE00004");

		searchlist.add(listData4);

		HashMap<String, String> listData5 = new HashMap<String, String>();
		listData5.put("TIMESCAPEUSEROID", "TIMESCAPEOID00005");
		listData5.put("NAME", "Shreedhar Godbole");
		listData5.put("EMAILID", "shreedhar.godbole@timesgroup.com");
		listData5.put("COMPANYNAME", "COMPANY00005");
		listData5.put("BRANCHNAME", "BRANCH00005");
		listData5.put("LOCATIONNAME", "LOCATION00005");
		listData5.put("DEPARTMENT", "DEPARTMENT00005");
		listData5.put("DESIGNATION", "DESIGNATION00005");
		listData5.put("DIRECTPHONENO", "PHONENO00005");
		listData5.put("DIRECTPHONENO1", "PHONENO100005");
		listData5.put("DIRECTPHONENO2", "DIRECTPHONENO200005");
		listData5.put("EXTENSION", "EXTENSION00005");
		listData5.put("HOTLINE", "HOTLINE00005");
		listData5.put("MOBILE", "MOBILE00005");
		listData5.put("FAX", "FAX00005");
		listData5.put("RESIDENCEPHONENO", "RESIDENCE00005");
		listData5.put("PAYROLLTYPETEXT", "PAYROLLTYPE00005");

		searchlist.add(listData5);

		if (searchlist.size() > 0) {
			responsejson.put("searchdetailslist", searchlist);
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray getmanagerslist(String emailid, String actionby, String plant, String material, String userid)
			throws SQLException {

		boolean result = Validation.StringChecknull(emailid);
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

		String column = "";
		String sql = "";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int inventorycount = 0;
		try {
			con = DBConnection.getConnection();
			//if (plant.equalsIgnoreCase("NA")) {
				if (actionby.equalsIgnoreCase("true")) {
					/*
					 sql = "select PERSONNELNUMBER,PORTALID,EMAILID,MANAGERPERSONNELNUMBER,"
					 	+ "MANAGEREMAILID,MANAGER9,MANAGERCOMPANYCODE,STATUS from EMPLOYEEDETAILS where EMAILID = ? ";
					*/
					sql = "SELECT PERSONNELNUMBER,PORTALID,EMAILID,MANAGERPERSONNELNUMBER,"
						 	+ "MANAGEREMAILID,MANAGER9,MANAGERCOMPANYCODE,STATUS from EMPLOYEEDETAILS WHERE STATUS = ? ";

				} else {
					/*
					sql = "select PERSONNELNUMBER,PORTALID,EMAILID,MANAGERPERSONNELNUMBER,"
							+ "MANAGEREMAILID,MANAGER9,MANAGERCOMPANYCODE,STATUS from LIKEDETAILSGM where EMAILID= ? ";
					*/
					sql = "SELECT PERSONNELNUMBER,PORTALID,EMAILID,MANAGERPERSONNELNUMBER,"
							+ "MANAGEREMAILID,MANAGER9,MANAGERCOMPANYCODE,STATUS from LIKEDETAILSGM WHERE STATUS = ?";
				}
			//} 
			/*
			else {
				if (actionby.equalsIgnoreCase("true")) {
					String inventorychecksql = "select count(*) as counter from INVENTORYUSERLIST where PLANT=? AND MTYP=? AND USERID=?";

					ps = con.prepareStatement(inventorychecksql);
					ps.setString(1, plant);
					ps.setString(2, material);
					ps.setString(3, userid);
					rs = ps.executeQuery();

					if (rs.next()) {
						inventorycount = rs.getInt("counter");
					}

					if (inventorycount > 0) {
						sql = "select PERSONNELNUMBER,PORTALID,EMAILID,MANAGERPERSONNELNUMBER,"
								+ "MANAGEREMAILID,MANAGER9,MANAGERCOMPANYCODE,STATUS from EMPLOYEEDETAILS where EMAILID = ? ";

					} else {
						sql = "select PERSONNELNUMBER,PORTALID,EMAILID,MANAGERPERSONNELNUMBER,"
								+ "MANAGEREMAILID,MANAGER9,MANAGERCOMPANYCODE,STATUS from LIKEDETAILSGM where EMAILID= ? ";

					}

				} else {
					sql = "select PERSONNELNUMBER,PORTALID,EMAILID,MANAGERPERSONNELNUMBER,"
							+ "MANAGEREMAILID,MANAGER9,MANAGERCOMPANYCODE,STATUS from LIKEDETAILSGM where EMAILID= ? ";
				}
			}
			*/	
			ps = con.prepareStatement(sql);
			ps.setString(1, "A");
			rs = ps.executeQuery();
			ArrayList<HashMap<String, String>> detailslist = new ArrayList<HashMap<String, String>>();

			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("SAPNUMBER", rs.getString("PERSONNELNUMBER"));
				poData.put("PORTALID", rs.getString("PORTALID"));
				poData.put("EMAILID", rs.getString("EMAILID"));
				poData.put("MANAGERPERSONNELNUMBER", rs.getString("MANAGERPERSONNELNUMBER"));
				poData.put("MANAGEREMAILID", rs.getString("MANAGEREMAILID"));
				poData.put("MANAGER9", rs.getString("MANAGER9"));
				poData.put("MANAGERCOMPANYCODE", rs.getString("MANAGERCOMPANYCODE"));
				poData.put("STATUS", rs.getString("STATUS"));

				detailslist.add(poData);
			}
			rs.close();
			ps.close();
			if (detailslist.size() > 0) {
				responsejson.put("managerdetails", detailslist);
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
			}

		} catch (SQLException e) {
			log.error("getmanagerslist() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getmanagerslistfordev(String emailid) {

		boolean result = Validation.StringChecknull(emailid);
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

		ArrayList<HashMap<String, String>> detailslist = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> poData = new HashMap<String, String>();
		poData.put("SAPNUMBER", "SAP00000");
		poData.put("PORTALID", "PORT000000");
		poData.put("EMAILID", emailid);
		poData.put("MANAGERPERSONNELNUMBER", "personalnumber00000");
		poData.put("MANAGEREMAILID", "vikrant.desai@timesgroup.com");
		poData.put("MANAGER9", "Vikrant Desai");
		poData.put("MANAGERCOMPANYCODE", "Company00000");
		poData.put("STATUS", "A");

		detailslist.add(poData);

		HashMap<String, String> poData1 = new HashMap<String, String>();
		poData1.put("SAPNUMBER", "SAP00001");
		poData1.put("PORTALID", "PORT000001");
		poData1.put("EMAILID", emailid);
		poData1.put("MANAGERPERSONNELNUMBER", "personalnumber00001");
		poData1.put("MANAGEREMAILID", "dilip.kukreja@timesgroup.com");
		poData1.put("MANAGER9", "Dilip Kukreja");
		poData1.put("MANAGERCOMPANYCODE", "Company00001");
		poData1.put("STATUS", "A");
		detailslist.add(poData1);

		HashMap<String, String> poData2 = new HashMap<String, String>();
		poData2.put("SAPNUMBER", "SAP00002");
		poData2.put("PORTALID", "PORT000002");
		poData2.put("EMAILID", emailid);
		poData2.put("MANAGERPERSONNELNUMBER", "personalnumber00002");
		poData2.put("MANAGEREMAILID", "sachin.mehta@timesgroup.com");
		poData2.put("MANAGER9", "Sachin Mehta");
		poData2.put("MANAGERCOMPANYCODE", "Company00002");
		poData2.put("STATUS", "A");
		detailslist.add(poData2);

		HashMap<String, String> poData3 = new HashMap<String, String>();
		poData3.put("SAPNUMBER", "SAP00003");
		poData3.put("PORTALID", "PORT000003");
		poData3.put("EMAILID", emailid);
		poData3.put("MANAGERPERSONNELNUMBER", "personalnumber00003");
		poData3.put("MANAGEREMAILID", "sachin.kale@timesgroup.com");
		poData3.put("MANAGER9", "Sachin Kale");
		poData3.put("MANAGERCOMPANYCODE", "Company00003");
		poData3.put("STATUS", "A");
		detailslist.add(poData3);

		HashMap<String, String> poData4 = new HashMap<String, String>();
		poData4.put("SAPNUMBER", "SAP00004");
		poData4.put("PORTALID", "PORT000004");
		poData4.put("EMAILID", emailid);
		poData4.put("MANAGERPERSONNELNUMBER", "personalnumber00004");
		poData4.put("MANAGEREMAILID", "amit.deshpande@timesgroup.com");
		poData4.put("MANAGER9", "Amit Deshpande");
		poData4.put("MANAGERCOMPANYCODE", "Company000004");
		poData4.put("STATUS", "A");
		detailslist.add(poData4);

		HashMap<String, String> poData5 = new HashMap<String, String>();
		poData5.put("SAPNUMBER", "SAP00005");
		poData5.put("PORTALID", "PORT000005");
		poData5.put("EMAILID", emailid);
		poData5.put("MANAGERPERSONNELNUMBER", "personalnumber00005");
		poData5.put("MANAGEREMAILID", "shreedhar.godbole@timesgroup.com");
		poData5.put("MANAGER9", "Shreedhar Godbole");
		poData5.put("MANAGERCOMPANYCODE", "Company00005");
		poData5.put("STATUS", "A");
		detailslist.add(poData5);

		if (detailslist.size() > 0) {
			responsejson.put("managerdetails", detailslist);
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		}

		return jsonArray;
	}

	public JSONArray getenduserPODetails(String email1) throws SQLException {

		String po_data = "select * from ("
				+ "select PONUMBER,PODATE,COMPANY,PLANT,DEPARTMENT,COSTCENTRE,CATEGORY,BUSINESSPARTNEROID,"
				+ "BUSINESSPARTNERTEXT,VENDORID,CREATEDDATE,STARTDATE,ENDDATE,QUANTITY,LINEITEMNUMBER,LINEITEMTEXT,"
				+ "UNITOFMEASURE,POAMOUNT,IGSTAMOUNT,CGSTAMOUNT,SGSTAMOUNT,CONTACTPERSONEMAILID,CONTACTPERSONPHONE,DELIVERYADDRESS1, "
				+ "DELIVERYADDRESS2,DELIVERYADDRESS3,CITY,STATE,COUNTRY,PINCODE,BUYER,REQUSITIONER,CREATEDBY, "
				+ "CREATEDON,MODIFIEDBY,MODIFIEDON,STATUS,PURCHASINGORGANISATION,PURCHASINGGROUP, "
				+ "COMPANYCODE,QUOTATIONNO,QUOTATIONDATE,MATERIAL_TYPE from podetails where REQUSITIONER=? and Status=? "
				+ "Union " + "select PONUMBER,PODATE,COMPANY,PLANT,DEPARTMENT,COSTCENTRE,CATEGORY,BUSINESSPARTNEROID, "
				+ "BUSINESSPARTNERTEXT,VENDORID,CREATEDDATE,STARTDATE,ENDDATE,QUANTITY,LINEITEMNUMBER,LINEITEMTEXT, "
				+ "UNITOFMEASURE,POAMOUNT,IGSTAMOUNT,CGSTAMOUNT,SGSTAMOUNT,CONTACTPERSONEMAILID,CONTACTPERSONPHONE,DELIVERYADDRESS1, "
				+ "DELIVERYADDRESS2,DELIVERYADDRESS3,CITY,STATE,COUNTRY,PINCODE,BUYER,REQUSITIONER,CREATEDBY, "
				+ "CREATEDON,MODIFIEDBY,MODIFIEDON,STATUS,PURCHASINGORGANISATION,PURCHASINGGROUP, "
				+ "COMPANYCODE,QUOTATIONNO,QUOTATIONDATE,MATERIAL_TYPE from podetails where REQUSITIONER=? and Status <> ? "
				+ ") order by MODIFIEDON desc";

		String po_query = "Select * from CHATMESSAGE where CreatedOn in(select max(CreatedOn) from CHATMESSAGE where PONUMBER=?)"
				+ " having  status =? and SENDER=?";
		String po_Number = "Select PONUMBER from CHATMESSAGE where SENDER=? and status =? and INVOICENUMBER is null ";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> QueryList = new HashMap<String, String>();
		ArrayList<String> topic = new ArrayList<String>();

		String queryLists[] = { "payment amount is incorrect", "payment amount is incorrect",
				"payment amount is incorrect", "payment amount is incorrect" };
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			try {

				ps = con.prepareStatement(po_Number);
				ps.setString(1, email1);
				ps.setString(2, "A");
				rs = ps.executeQuery();
				while (rs.next()) {
					topic.add(rs.getString("PONUMBER"));
				}
				rs.close();
				ps.close();
			} catch (Exception e) {
				topic.add("0");
			}
			for (int i = 0; i < topic.size(); i++) {
				try {
					ps = con.prepareStatement(po_query);
					ps.setString(1, topic.get(i));
					rs = ps.executeQuery();
					while (rs.next()) {
						QueryList.put(rs.getString("PONUMBER"), rs.getString("MessageText"));
					}
					rs.close();
					ps.close();
				} catch (Exception e) {
					QueryList.put("PONUMBER", "No data Found");
				}
			}

			ps = con.prepareStatement(po_data);
			ps.setString(1, email1);
			ps.setString(2, "N");
			ps.setString(3, email1);
			ps.setString(4, "N");
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("PO_NUMBER", rs.getString("PONumber"));
				poData.put("DATE", rs.getString("PODate"));
				poData.put("AMOUNT", rs.getString("POAmount"));
				poData.put("STATUS", rs.getString("Status"));
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
		} catch (SQLException e) {
			log.error("getenduserPODetails() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0) {
			responsejson.put("poData", POList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	// changes are required for location code added in locationmaster

	public JSONArray getinternalPODetails(String email1, String mode, String status, int nPage, String ponumber,
			String fromdateofduration, String todateofduration, String fromdateofpo, String todateofpo, String plant,
			String vendor) throws SQLException {

		boolean twocondition = true;
		boolean nodata = false;
		String po_data = "";

		if (mode.equalsIgnoreCase("buyer")) {
			/*
			if (status.equalsIgnoreCase("SP")) {
				po_data = "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd."
						+ "CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,"
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,"
						+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,"
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,"
						+ "pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,"
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE,"
						+ "pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.POTYPE =? order By PODATE desc";
			} else 
			*/	
			if (status.equalsIgnoreCase("ALL")) {
				po_data = "select * from (select DISTINCT pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
						+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
						+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
						+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
						+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
						+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where " + "pd.CONTACTPERSONEMAILID=? and pd.Status=? "
						+ "Union "
						+ "select DISTINCT pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
						+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
						+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
						+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
						+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,"
						+ "pd.PURCHASINGGROUP,pd. COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from "
						+ "podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.Status <> ? ) order by PODATE desc";
			} else {
				po_data = "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd."
						+ "VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,"
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,"
						+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,"
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,"
						+ "pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,"
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE,"
						+ "pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.Status = ? order By PODATE desc";
			}

		} else if (mode.equalsIgnoreCase("enduser")) {
			/*
			if (status.equalsIgnoreCase("SP")) {
				po_data = "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd."
						+ "CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,"
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,"
						+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,"
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,"
						+ "pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,"
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE,"
						+ "pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? and pd.POTYPE =? order By PODATE desc";

			} else 
			*/
			if (status.equalsIgnoreCase("ALL")) {
				po_data = "select * from (select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
						+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
						+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
						+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
						+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
						+ "pd.COMPANYCODE,pd.quotationno,pd.quotationdate,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where " + "pd.REQUSITIONER=? and pd.Status=? " + " Union "
						+ "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
						+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
						+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
						+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
						+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,"
						+ "pd.PURCHASINGGROUP,pd.COMPANYCODE,pd.quotationno,pd.quotationdate,pd.MATERIAL_TYPE,pd.POTYPE, poe.PLANT from "
						+ "podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? and pd.Status <> ? "
						+ " Union "
						+ "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY, "
						+ "pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE, "
						+ "pd.ENDDATE,pd.QUANTITY, "
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT, "
						+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE, "
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY, "
						+ "pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY, "
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE, "
						+ "pd.quotationno,pd.quotationdate,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd, "
						+ "poeventdetails poe,locationmaster lm where pd.PONUMBER = poe.PONUMBER and lm.LOCATIONCODE = poe.DELVPLANT "
						+ "and lm.STOREKEEPEREMILID = ? ) order by PODATE desc";
			} else {
				po_data = "select * from ( select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd."
						+ "VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,"
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,"
						+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,"
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,"
						+ "pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,"
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE,"
						+ "pd.quotationno,pd.quotationdate,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? and pd.Status = ?  "
						+ " Union "
						+ "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY, "
						+ "pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE, "
						+ "pd.ENDDATE,pd.QUANTITY, "
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT, "
						+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE, "
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY, "
						+ "pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY, "
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE, "
						+ "pd.quotationno,pd.quotationdate,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd, "
						+ "poeventdetails poe,locationmaster lm where pd.PONUMBER = poe.PONUMBER and lm.LOCATIONCODE = poe.DELVPLANT "
						+ "and lm.STOREKEEPEREMILID = ? and pd.Status = ? ) order by PODATE desc ";
			}
		} else if (mode.equalsIgnoreCase("payer")) {
			twocondition = false;
			/*
			if (status.equalsIgnoreCase("SP")) {
				po_data = "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd."
						+ "CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,"
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,"
						+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,"
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,"
						+ "pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,"
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE,"
						+ "pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where  pd.POTYPE =? order by PODATE desc";

			} else 
			*/
			if (status.equalsIgnoreCase("ALL")) {

				po_data = "select * from (select DISTINCT pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
						+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
						+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
						+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
						+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
						+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where pd.Status=? " + "Union "
						+ "select DISTINCT pd.POTYPE,pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
						+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
						+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
						+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
						+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,"
						+ "pd.PURCHASINGGROUP,pd. COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from "
						+ "podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where pd.Status <> ? ) order by PODATE desc";
			} else {

				po_data = "select DISTINCT pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,pd.BUSINESSPARTNERTEXT,pd."
						+ "VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,"
						+ "pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,"
						+ "pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,"
						+ "pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,"
						+ "pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,pd.CREATEDON,pd.MODIFIEDBY,"
						+ "pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,pd.COMPANYCODE,"
						+ "pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,pd.POTYPE,poe.PLANT from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.Status = ?";

			}
		} else if (mode.equalsIgnoreCase("internalbcclportal")) {
			nodata = true;
		}

		if (nodata == false) {

			String po_query = "Select * from CHATMESSAGE where CreatedOn in(select max(CreatedOn) from CHATMESSAGE where PONUMBER=? AND Status = ?)"
					+ " having  status =? and SENDER=?";
			String po_Number = "Select PONUMBER from CHATMESSAGE where SENDER=? and status =? and INVOICENUMBER is null ";

			ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> QueryList = new HashMap<String, String>();
			ArrayList<String> topic = new ArrayList<String>();

			String queryLists[] = { "payment amount is incorrect", "payment amount is incorrect",
					"payment amount is incorrect", "payment amount is incorrect" };
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int pages = 0;
			try {
				con = DBConnection.getConnection();

				try {
					log.info("po_Number --" + po_Number);
					ps = con.prepareStatement(po_Number);
					ps.setString(1, email1);
					ps.setString(2, "A");
					rs = ps.executeQuery();
					while (rs.next()) {
						topic.add(rs.getString("PONUMBER"));
					}
					rs.close();
					ps.close();
				} catch (Exception e) {
					topic.add("0");
				}

				for (int i = 0; i < topic.size(); i++) {
					try {
						log.info("po_query --"+po_query);
						ps = con.prepareStatement(po_query);
						ps.setString(1, topic.get(i));
						ps.setString(2, "A");
						rs = ps.executeQuery();
						while (rs.next()) {
							QueryList.put(rs.getString("PONUMBER"), rs.getString("MessageText"));
						}
						rs.close();
						ps.close();
					} catch (Exception e) {
						QueryList.put("PONUMBER", "No data Found");
					}
				}
				Pagination pg = null;
				if (!status.equalsIgnoreCase("AS")) {

					ArrayList<String> param = new ArrayList<String>();

					if (status.equalsIgnoreCase("ALL")) {
						if (twocondition == true) {
							param.add(email1);
							param.add("N");
							param.add(email1);
							param.add("N");
							param.add(email1);
							

						} else if ("SP".equalsIgnoreCase(status)) {

							param.add(email1);
							param.add("S");

						} else {
							param.add("N");
							param.add("N");
						}
					} else {
						if (twocondition == true) {
							param.add(email1);
							if ("SP".equalsIgnoreCase(status)) {
								param.add("S");
							} else {
								param.add(status);
								param.add(email1);
								param.add(status);
							}
							
						} else {
							if ("SP".equalsIgnoreCase(status)) {
								param.add("S");
							} else {
								param.add(status);
							}
						}
					}
					
					log.info(" po_data =="+po_data);
					pg = new Pagination(po_data, nPage);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				} else {
					String subquery = "";
					String podata1 = "";
					ArrayList<String> param = new ArrayList<String>();
				
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(email1);
					}

					param.add("N");
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND poe.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}

					if (!vendor.equalsIgnoreCase("NA")) {
						String po = " AND pd.VENDORID=?";
						subquery = subquery + po;
						param.add(vendor);
					}

					if (!ponumber.equalsIgnoreCase("NA")) {
						String po = " AND pd.PONUMBER=?";
						subquery = subquery + po;
						param.add(ponumber);

					}
					if ((!fromdateofduration.equalsIgnoreCase("NA")) && (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
						String in = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + in;
						param.add(fromdateofduration);
						param.add(todateofduration);
					}
					if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
						String dt = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
						subquery = subquery + dt;
						param.add(fromdateofpo);
						param.add(todateofpo);
					}
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(email1);
					}
					param.add("N");
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!ponumber.equalsIgnoreCase("NA")) {
						param.add(ponumber);
					}
					if ((!fromdateofduration.equalsIgnoreCase("NA")) && (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
						param.add(fromdateofduration);
						param.add(todateofduration);
					}
					if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
						param.add(fromdateofpo);
						param.add(todateofpo);
					}
					/*	
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(email1);
					}
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);
					}
					if (!ponumber.equalsIgnoreCase("NA")) {
						param.add(ponumber);
					}
					if ((!fromdateofduration.equalsIgnoreCase("NA")) && (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
						param.add(fromdateofduration);
						param.add(todateofduration);
					}					
					if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
						param.add(fromdateofpo);
						param.add(todateofpo);
					}
					*/	
					if (mode.equalsIgnoreCase("buyer")) {

						podata1 = "select * from ("
								+ "select pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
								+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
								+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
								+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
								+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
								+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=?  and pd.Status=? "
								+ subquery + " " + " Union "
								+ "select pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
								+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
								+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
								+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
								+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
								+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.Status <> ? "
								+ subquery + ") " + "order by PODATE desc";

					} else if (mode.equalsIgnoreCase("enduser")) {

						podata1 = "select * from ("
								+ "select pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
								+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
								+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
								+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
								+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
								+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=?  and pd.Status=? " + subquery
								+ " Union "
								+ "select pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
								+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
								+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
								+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
								+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
								+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? and pd.Status <> ? " + subquery
								/*
								+ " Union "
								+ "select pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID, "
								+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT, "
								+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1, "
								+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY, "
								+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP, "
								+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd,poeventdetails poe, "
								+ "locationmaster lm where pd.PONUMBER = poe.PONUMBER and lm.LOCATIONCODE = poe.DELVPLANT "
								+ "and lm.STOREKEEPEREMILID = ?  " + subquery
								*/
								+ " ) " + " order by PODATE desc ";
					} else if (mode.equalsIgnoreCase("payer")) {
						twocondition = false;
						podata1 = "select * from ("
								+ "select pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
								+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
								+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
								+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
								+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
								+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.Status=? " + subquery + " " + "Union "
								+ "select pd.POTYPE, pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
								+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
								+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1,"
								+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY,"
								+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP,"
								+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,pd.MATERIAL_TYPE,poe.PLANT from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.Status <> ? " + subquery + ") "
								+ "order by PODATE desc";

					}
					log.info("podata1==="+podata1);
					pg = new Pagination(podata1, nPage);
					pages = pg.getPages(con, param);
					rs = pg.execute(con, param);
				}
 
				while (rs.next()) {

					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PO_NUMBER", rs.getString("PONumber"));
					poData.put("DATE", rs.getString("PODate"));
					poData.put("AMOUNT", rs.getString("POAmount"));
					poData.put("STATUS", rs.getString("Status"));
					poData.put("Quantity", rs.getString("Quantity"));
					poData.put("COMPANY", rs.getString("Company"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", getVendorName(rs.getString("VENDORID")));
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

				try {
					getinternalPODetailsCountAsPerStatus(email1, mode, status, nPage, ponumber, fromdateofduration,
							todateofduration, fromdateofpo, todateofpo, plant, vendor, con, ps, rs);
				} catch (Exception e) {
					log.error("getinternalPODetailsCountAsPerStatus() :", e.fillInStackTrace());
				}

			} catch (SQLException e1) {
				log.error("getinternalPODetails() :", e1.fillInStackTrace());

				responsejson.put("message", "Network Issue");
				jsonArray.add(responsejson);
			} finally {
				DBConnection.closeConnection(rs, ps, con);
			}
			if (POList.size() > 0) {
				responsejson.put("poData", POList);
				responsejson.put("popages", pages);
				responsejson.put("message", "Success");

				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}
		} else {
			responsejson.put("message", "No Data Found for given Email Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray getinvoicebasedonbuyerid(String emailId, HttpSession session) throws SQLException {

		String sql = "Select Distinct invoiceapproval.PONUMBER,invoiceapproval.INVOICENUMBER,"
				+ "invoiceapproval.INVOICEDATE,max(invoiceapproval.ENDUSEID) ENDUSEID,"
				+ "max(invoiceapproval.ENDUSERSTATUS) ENDUSERSTATUS,max(invoiceapproval.EUMANAGER) EUMANAGER,max(invoiceapproval.BUYERID) BUYERID,"
				+ "max(invoiceapproval.STATUS) STATUS,max(invoiceapproval.STAGE) STAGE,"
				+ "max( poninvoicesummery.overallstatus) OVERALLSTATUS,max(poninvoicesummery.amount) amount, max(invoiceapproval.proxy) proxyB from"
				+ " invoiceapproval join  poninvoicesummery on "
				+ "invoiceapproval.InvoiceNumber=poninvoicesummery.InvoiceNumber and "
				+ "invoiceapproval.PONumber=poninvoicesummery.PONumber where " + "BUYERID = ? group by "
				+ "invoiceapproval.PONUMBER,invoiceapproval.INVOICENUMBER,invoiceapproval.INVOICEDATE order by "
				+ "invoiceapproval.INVOICEDATE desc";

		String storeKepeerQuery = "Select Distinct A.PONUMBER,A.INVOICENUMBER, A.INVOICEDATE,max(A.ENDUSEID) ENDUSEID,"
				+ " max(A.ENDUSERSTATUS) ENDUSERSTATUS,max(A.EUMANAGER) EUMANAGER, max(A.STATUS) STATUS, max(A.BUYERID) BUYERID, "
				+ " max(A.STAGE) STAGE, max(B.overallstatus) OVERALLSTATUS,max(B.amount) amount,	max(A.proxy) proxyB "
				+ " from invoiceapproval A ,poninvoicesummery B where A.InvoiceNumber=B.InvoiceNumber and "
				+ " A.PONumber=B.PONumber and ( BUYERID = ? or A.proxy ='X') group by A.PONUMBER,A.INVOICENUMBER,A.INVOICEDATE order by "
				+ " A.INVOICEDATE desc ";

		String storeKepeer = (String) session.getAttribute("shopkepeer");
		ArrayList<HashMap<String, String>> POEvent = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> POListEvent = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			if ("true".equalsIgnoreCase(storeKepeer)) {
				ps = con.prepareStatement(storeKepeerQuery);
			} else {
				ps = con.prepareStatement(sql);
			}

			ps.setString(1, emailId);
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> poEvent = new HashMap<String, String>();
				poEvent.put("PONUMBER", rs.getString("PONUMBER"));
				poEvent.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				poEvent.put("INVOICEDATE", rs.getString("INVOICEDATE"));
				poEvent.put("ENDUSERID", rs.getString("ENDUSEID"));
				poEvent.put("ENDUSERSTATUS", rs.getString("ENDUSERSTATUS"));
				poEvent.put("EUMANAGER", rs.getString("EUMANAGER"));
				poEvent.put("STATUS", rs.getString("STATUS"));
				poEvent.put("TOTALAMOUNT", rs.getString("amount"));
				poEvent.put("STAGE", rs.getString("STAGE"));
				poEvent.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
				poEvent.put("PROXY", rs.getString("proxyB"));
				poEvent.put("BUYERID", rs.getString("BUYERID"));

				POListEvent.add(poEvent);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			log.error("getinvoicebasedonbuyerid() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POListEvent.size() > 0) {
			responsejson.put("invoicedetails", POListEvent);
			responsejson.put("message", "Success");
		} else {
			responsejson.put("message", "No Data Found");
		}
		jsonArray.add(responsejson);
		return jsonArray;

	}

	public JSONArray getinternalquerydetails(String po_num, String invoice_num) throws SQLException {

		String po_data = "SELECT * FROM CHATMESSAGE where PONUMBER = ? AND INVOICENUMBER = ? and STATUS=? "
				+ " order by createdon desc";
		ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, po_num);
			ps.setString(2, invoice_num);
			ps.setString(3, "A");
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

		} catch (Exception e) {
			log.error("getinternalquerydetails() :", e.fillInStackTrace());

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

	public JSONArray internalquerydetailsofapproval(String po_num, String invoice_num) throws SQLException {

		String po_data = "SELECT * FROM CHATMESSAGE where PONUMBER = ? AND INVOICENUMBER = ? and STATUS=? "
				+ " order by createdon desc";
		ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> POChatList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, po_num);
			ps.setString(2, invoice_num);
			ps.setString(3, "S");
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
				String dateqw = rs.getString("CREATEDON");
				DateFormat formatter = new SimpleDateFormat();
				SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
				Date date;
				date = inputFormat.parse(dateqw);
				SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
				poQuery.put("CREATEDON", outputFormat.format(date).toString());
				POQueryList.add(poQuery);
			}
			Set<String> s = new HashSet<String>();
			for (int a = 0; a < POQueryList.size(); a++) {
				s.add(POQueryList.get(a).get("SENDER"));
			}
			for (String emails : s) {
				for (int b = 0; b < POQueryList.size(); b++) {
					if (emails.equalsIgnoreCase(POQueryList.get(b).get("SENDER"))) {
						POChatList.add(POQueryList.get(b));
						break;
					}
				}
			}

			rs.close();
			ps.close();

		} catch (Exception e) {
			log.error("internalquerydetailsofapproval() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POChatList.size() > 0) {
			responsejson.put("poQueryList", POChatList);
			responsejson.put("message", "Success");

			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray getPayerPoninvoiceSummery(int nPage, String status, String invno, String pono, String fdate,
			String tdate, String plant, String vendor) throws SQLException {

		// For SHORT QUANTITY
		String sdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,"
				+ "ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,"
				+ "ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,ps.MATERIAL_TYPE,"
				+ "ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
				+ "ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from "
				+ "PONINVOICESUMMERY ps  where ps.invoicenumber is not null and "
				+ " ps.CREDITADVICENO IS NOT NULL order by ps.CREATEDON desc";

		String pendingsql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES," + "ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID "
				+ "from PONINVOICESUMMERY ps where  (OVERALLSTATUS=? OR OVERALLSTATUS=?) AND ps.invoicenumber is not null"
				+ " order by ps.CREATEDON desc";

		String sql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES," + "ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID "
				+ "from PONINVOICESUMMERY ps where  OVERALLSTATUS=? AND ps.invoicenumber is not null"
				+ " order by ps.CREATEDON desc";

		String alldata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES," + "ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID "
				+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null" + " order by ps.CREATEDON desc";

		String hdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
				+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES," + "ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID "
				+ "from PONINVOICESUMMERY ps where ONEXSTATUS=? AND ps.invoicenumber is not null"
				+ " order by ps.CREATEDON desc";
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
					pg = new Pagination(hdata, nPage);
				} else if (status.equalsIgnoreCase("ALL")) {
					pg = new Pagination(alldata, nPage);
				} else if (status.equalsIgnoreCase("C")) {
					pg = new Pagination(sdata, nPage);
				} else if (status.equalsIgnoreCase("P")) {
					param.add("P");
					param.add("M");
					pg = new Pagination(pendingsql, nPage);
				} else {
					param.add(status);
					pg = new Pagination(sql, nPage);

				}
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				int count = 0;
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
					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,"
							+ "ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,"
							+ "ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,"
							+ "ps.TAXAMOUNT,ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps where ps.invoicenumber is not null "
							+ subquery + "" + " order by ps.CREATEDON desc";
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

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,"
							+ "ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,ps.CREDITNOTENO,"
							+ "ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,"
							+ "ps.TAXAMOUNT,ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps where ps.invoicenumber is not null"
							+ " " + subquery + " " + " order by ps.CREATEDON desc";

				}
				pg = new Pagination(advqdata, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				while (rs.next()) {
					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("PONUMBER", rs.getString("PONUMBER"));
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

		// For SHORT QUANTITY
		String sdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,"
				+ "ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,"
				+ "ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,ps.MATERIAL_TYPE,"
				+ "ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
				+ "ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from "
				+ "PONINVOICESUMMERY ps  where ps.invoicenumber is not null and "
				+ "BUYER =? AND ps.CREDITADVICENO IS NOT NULL order by ps.CREATEDON desc";

		// For OFFLINE INVOICES
		String hdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,"
				+ "ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,"
				+ "ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,ps.MATERIAL_TYPE,"
				+ "ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
				+ "ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from "
				+ "PONINVOICESUMMERY ps  where ps.invoicenumber is not null and "
				+ "BUYER =? AND ONEXSTATUS=? order by ps.CREATEDON desc";
		// All Filter
		String sql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,"
				+ "ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,"
				+ "ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,ps.MATERIAL_TYPE,"
				+ "ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
				+ "ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from "
				+ "PONINVOICESUMMERY ps  where ps.invoicenumber is not null and "
				+ "BUYER =? AND OVERALLSTATUS=? order by ps.CREATEDON desc";

		String pendingsql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,"
				+ "ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,"
				+ "ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,ps.MATERIAL_TYPE,"
				+ "ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
				+ "ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from "
				+ "PONINVOICESUMMERY ps  where ps.invoicenumber is not null and "
				+ "BUYER =? AND (OVERALLSTATUS=? OR  OVERALLSTATUS=?) order by ps.CREATEDON desc";

		// All Status
		String alldata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,"
				+ "ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,"
				+ "ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,ps.MATERIAL_TYPE,"
				+ "ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
				+ "ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from "
				+ "PONINVOICESUMMERY ps  where ps.invoicenumber is not null and "
				+ "BUYER =? order by ps.CREATEDON desc";

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
					pg = new Pagination(hdata, nPage);
				} else if (status.equalsIgnoreCase("ALL")) {
					pg = new Pagination(alldata, nPage);
				} else if (status.equalsIgnoreCase("C")) {
					pg = new Pagination(sdata, nPage);
				} else if (status.equalsIgnoreCase("P")) {
					param.add("P");
					param.add("M");
					pg = new Pagination(pendingsql, nPage);
				} else {
					param.add(status);
					pg = new Pagination(sql, nPage);

				}
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				int count = 0;

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
					poData.put("HOLDCOUNT", rs.getString("HOLDCOUNT"));
					poData.put("PLANT", rs.getString("PLANT"));
					POImpl po = new POImpl();
					poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
					poData.put("VENDORID", rs.getString("VENDORID"));
					poData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
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
					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,ps.REQUSITIONER,"
							+ "ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,"
							+ "ps.TOTALAMOUNT,ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
							+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,"
							+ "ps.TAXAMOUNT,ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID" + " from PONINVOICESUMMERY ps  "
							+ "where ps.invoicenumber is not null and ps.BUYER =? " + subquery
							+ " order by ps.CREATEDON desc";

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

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,"
							+ "ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
							+ "ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps "
							+ " where ps.invoicenumber is not null and BUYER =? " + subquery
							+ " order by ps.CREATEDON desc";

				}
				pg = new Pagination(advqdata, nPage);
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				while (rs.next()) {
					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
					poData.put("PONUMBER", rs.getString("PONUMBER"));
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
					invoiceList.add(poData);
				}

				pg.close();
				rs.close();
				pg = null;

			} // end of else

			try {
				getInternalPonInvoiceSummeryCountsAsPerStatus(emailid, nPage, status, invno, pono, fdate, tdate, plant,
						vendor, "buyer", con, ps, rs);
			} catch (Exception e) {

			}

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

		// For SHORT QUANTITY
		String sdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,ps.MESSAGE,"
				+ "ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,ps.HOLDCOUNT,"
				+ "ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,ps.MATERIAL_TYPE,"
				+ "ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
				+ "ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from "
				+ "PONINVOICESUMMERY ps  where ps.invoicenumber is not null and "
				+ "REQUSITIONER = ? AND ps.CREDITADVICENO IS NOT NULL order by ps.CREATEDON desc";

		String hdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID, "
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT, "
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT, "
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME, "
				+ "ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO, "
				+ "ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps where "
				+ "ps.invoicenumber is not null and REQUSITIONER = ? AND ONEXSTATUS=? " + "order by ps.CREATEDON desc";

		String sql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID, "
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT, "
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT, "
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME, "
				+ "ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO, "
				+ "ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps where "
				+ "ps.invoicenumber is not null and REQUSITIONER = ? AND OVERALLSTATUS=? "
				+ "order by ps.CREATEDON desc";

		String pendingdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID, "
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT, "
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT, "
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME, "
				+ "ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO, "
				+ "ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps where "
				+ "ps.invoicenumber is not null and REQUSITIONER = ? AND (OVERALLSTATUS=? OR OVERALLSTATUS=?) "
				+ "order by ps.CREATEDON desc";

		String alldata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID, "
				+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT, "
				+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT, "
				+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME, "
				+ "ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO, "
				+ "ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
				+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps where "
				+ "ps.invoicenumber is not null and REQUSITIONER = ?  " + "order by ps.CREATEDON desc";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
		try {
			con = DBConnection.getConnection();

			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ"))) {
				ArrayList<String> param = new ArrayList<String>();
				param.add(emailid);
				Pagination pg = null;
				if (status.equalsIgnoreCase("H")) {
					param.add(status);
					pg = new Pagination(hdata, nPage);
				} else if (status.equalsIgnoreCase("ALL")) {
					pg = new Pagination(alldata, nPage);
				} else if (status.equalsIgnoreCase("P")) {
					param.add("P");
					param.add("M");
					pg = new Pagination(pendingdata, nPage);
				} else if (status.equalsIgnoreCase("C")) {
					pg = new Pagination(sdata, nPage);
				} else {
					param.add(status);
					pg = new Pagination(sql, nPage);

				}
				pages = pg.getPages(con, param);
				rs = pg.execute(con, param);

				int count = 0;
				while (rs.next()) {
					count++;
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
					poData.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");

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
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND ps.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
					}
					if (!pono.equalsIgnoreCase("NA")) {
						String po = " AND ps.PONUMBER=?";
						subquery = subquery + po;
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
					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID, ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,"
							+ "ps.CREATEDON,ps.MACOUNT, ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT, ps.MATERIAL_TYPE,ps.PGQ,"
							+ "ps.ONEXSTATUS,ps.ACTUALFILENAME, ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO, "
							+ "ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps where ps.invoicenumber is not null and "
							+ "REQUSITIONER = ? " + subquery + " " + "order by ps.CREATEDON desc";

				} else if (status.equalsIgnoreCase("ASSQ")) {
					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND ps.PLANT=?";
						subquery = subquery + po;
						param.add(plant);
					}
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
						subquery = subquery + po;
						param.add(vendor);
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

					advqdata = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID, ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,"
							+ "ps.AMOUNT,ps.CREATEDON,ps.MACOUNT, ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT, "
							+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME, ps.SAVEDFILENAME,ps.PAYMENTAMOUNT,"
							+ "ps.CREDITNOTENO,ps.CREDITADVICENO, ps.TOTALAMTINCTAXES,ps.TAXAMOUNT,"
							+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID from PONINVOICESUMMERY ps"
							+ " where ps.invoicenumber is not null and REQUSITIONER = ? " + subquery + " "
							+ "order by ps.CREATEDON desc";

				}
				pg = new Pagination(advqdata, nPage);
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
					poData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
					poData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
					poData.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
					poData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
					poData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
					poData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
					poData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
					poData.put("EXPENSESHEETID",
							rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");

					invoiceList.add(poData);
				}

				pg.close();
				rs.close();
				pg = null;

			} // end of else

			try {
				getInternalPonInvoiceSummeryCountsAsPerStatus(emailid, nPage, status, invno, pono, fdate, tdate, plant,
						vendor, "enduser", con, ps, rs);
			} catch (Exception e) {

			}

		} catch (Exception e) {
			log.error("getRequsitionerPoninvoiceSummery() :", e.fillInStackTrace());

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

	public JSONArray setInternalChatStatus(String invoiceNumber, String poNumber, String userEmailId, String topic,
			String message, String subject) throws SQLException {

		String userStatus = null;
		String bid = null;
		boolean flag = false;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		try {
			String queryFindPoDetails1 = "Select BUSINESSPARTNEROID from PODETAILS where PONUMBER = ? ";
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(queryFindPoDetails1);
			ps.setString(1, poNumber);
			rs = ps.executeQuery();
			if (rs.next()) {
				bid = rs.getString("BUSINESSPARTNEROID") == null ? "" : rs.getString("BUSINESSPARTNEROID");
			}
			rs.close();
			ps.close();

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
						ps = con.prepareStatement(queryUpdate);
						ps.setString(1, "Y");
						ps.setString(2, poNumber);
						ps.setString(3, invoiceNumber);
						ps.executeUpdate();
						ps.close();
						flag = true;
					}
				} else {
					flag = true;
				}
				if (flag) {
					String queryFindPoDetails = "Select CONTACTPERSONEMAILID,REQUSITIONER,POAMOUNT,BUSINESSPARTNEROID from PODETAILS where PONUMBER = ? ";
					ps = con.prepareStatement(queryFindPoDetails);
					ps.setString(1, poNumber);
					rs = ps.executeQuery();
					String buyerId = null;
					String endUserId = null;
					String amount = null;
					String msg = null;
					String primaryEmailid = null;
					String secondaryEmailid = null;
					String tertiaryEmailid = null;
					bid = null;
					ArrayList<String> emailArrayList = new ArrayList<String>();
					if (rs.next()) {
						buyerId = rs.getString("CONTACTPERSONEMAILID") == null ? "-"
								: rs.getString("CONTACTPERSONEMAILID");
						endUserId = rs.getString("REQUSITIONER") == null ? "-" : rs.getString("REQUSITIONER");
						amount = rs.getString("POAMOUNT") == null ? "0" : rs.getString("POAMOUNT");
						bid = rs.getString("BUSINESSPARTNEROID") == null ? "0" : rs.getString("BUSINESSPARTNEROID");
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
								ps = con.prepareStatement(queryChat);
								ps.setString(1, bid);
								ps.setString(2, invoiceNumber);
								ps.setString(3, poNumber);
								ps.setString(4, emailArrayList.get(counter));
								ps.setString(5, "A");
								ps.executeUpdate();
								ps.close();
							}
							rs1.close();
							ps1.close();
						} else {
							String querySubmitSummary = "insert into PONINVOICESUMMERY (INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,"
									+ "MESSAGE,REQUSITIONER,BUYER,AMOUNT) values (?,?,?,?,?,?,?)";
							ps = con.prepareStatement(querySubmitSummary);
							ps.setString(1, invoiceNumber);
							ps.setString(2, poNumber);
							ps.setString(3, bid);
							ps.setString(4, "Y");
							ps.setString(5, buyerId);
							ps.setString(6, endUserId);
							ps.setString(7, amount);
							ps.executeUpdate();
							ps.close();
							String queryBusinessPartner = "Select BUSINESSPARTNEROID,PRIMARYEMAILID,SECONDARYEMAILID,TERTIARYEMAILID"
									+ " from BUSINESSPARTNER where BUSINESSPARTNEROID = ? and STATUS =? ";
							ps = con.prepareStatement(queryBusinessPartner);
							ps.setString(1, bid);
							ps.setString(2, "A");
							rs = ps.executeQuery();
							if (rs.next()) {

								primaryEmailid = rs.getString("PRIMARYEMAILID") == null ? "-"
										: rs.getString("PRIMARYEMAILID");
								secondaryEmailid = rs.getString("SECONDARYEMAILID") == null ? "-"
										: rs.getString("SECONDARYEMAILID");
								tertiaryEmailid = rs.getString("TERTIARYEMAILID") == null ? "-"
										: rs.getString("TERTIARYEMAILID");

								emailArrayList.add(primaryEmailid);
								emailArrayList.add(secondaryEmailid);
								emailArrayList.add(tertiaryEmailid);

							}
							rs.close();
							ps.close();
							for (int counter = 0; counter <= emailArrayList.size() - 1; counter++) {

								String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
										+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
								ps = con.prepareStatement(queryChat);
								ps.setString(1, bid);
								ps.setString(2, invoiceNumber);
								ps.setString(3, poNumber);
								ps.setString(4, emailArrayList.get(counter));
								ps.setString(5, "A");
								ps.executeUpdate();
								ps.close();
							}
						}
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
					ps = con.prepareStatement(queryUpdate);
					ps.setString(1, "A");
					ps.setString(2, poNumber);
					ps.setString(3, invoiceNumber);
					ps.setString(4, userEmailId);
					ps.executeUpdate();
					ps.close();
					String queryUpdateChat = "update CHATSTATUS set status =? where PONUMBER = ? and INVOICENUMBER = ?"
							+ "and LOGGEDIN = ? ";
					ps = con.prepareStatement(queryUpdateChat);
					ps.setString(1, "R");
					ps.setString(2, poNumber);
					ps.setString(3, invoiceNumber);
					ps.setString(4, userEmailId);
					ps.executeUpdate();
					ps.close();
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
							ps = con.prepareStatement(queryUpdate);
							ps.setString(1, "Y");
							ps.setString(2, poNumber);
							ps.executeUpdate();
							poFlag = true;
						}
						rs.close();
						ps.close();
					} else {
						poFlag = true;
					}
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
							PreparedStatement ps1 = con.prepareStatement(queryFindPoSummery);
							ps1.setString(1, poNumber);
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
									ps = con.prepareStatement(queryChat);
									ps.setString(1, bid);
									ps.setString(2, invoiceNumber);
									ps.setString(3, poNumber);
									ps.setString(4, emailArrayList.get(counter));
									ps.setString(5, "A");
									ps.executeUpdate();
									ps.close();
								}

							} else {
								String querySubmitSummary = "insert into PONINVOICESUMMERY (INVOICENUMBER,PONUMBER,BUSINESSPARTNEROID,"
										+ "MESSAGE,REQUSITIONER,BUYER,AMOUNT) values (?,?,?,?,?,?,?)";
								ps = con.prepareStatement(querySubmitSummary);
								ps.setString(1, null);
								ps.setString(2, poNumber);
								ps.setString(3, bid);
								ps.setString(4, "Y");
								ps.setString(5, buyerId);
								ps.setString(6, endUserId);
								ps.setString(7, amount);
								ps.executeUpdate();
								ps.close();
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
				}
			}
			con.commit();
		} catch (Exception e) {
			log.error("setInternalChatStatus() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);

		}
		return jsonArray;
	}

	public JSONArray internalInvoiceStatusSubmit(String invoiceNumber, String poNumber, String userEmailId,
			String topic, String message, String subject) throws SQLException {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		String bid = "";
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String queryFindPoDetails1 = "Select BUSINESSPARTNEROID from PODETAILS where PONUMBER = ? ";

			ps = con.prepareStatement(queryFindPoDetails1);
			ps.setString(1, poNumber);
			rs = ps.executeQuery();
			if (rs.next()) {
				bid = rs.getString("BUSINESSPARTNEROID") == null ? "" : rs.getString("BUSINESSPARTNEROID");
			}
			rs.close();
			ps.close();
			String queryNotification = "insert into CHATMESSAGE (BUSINESSPARTNEROID,SENDER,PONUMBER,"
					+ "INVOICENUMBER,MESSAGETEXT,SUBJECT,STATUS,CREATEDON) values (?,?,?,?,?,?,?,?)";
			ps = con.prepareStatement(queryNotification);
			ps.setString(1, bid);
			ps.setString(2, userEmailId);
			ps.setString(3, poNumber);
			ps.setString(4, invoiceNumber);
			ps.setString(5, message);
			ps.setString(6, subject);
			ps.setString(7, "S");
			ps.setTimestamp(8, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.executeUpdate();
			ps.close();
			con.commit();
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);
		} catch (Exception e) {
			log.error("internalInvoiceStatusSubmit() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);

		}

		return jsonArray;
	}

	public JSONArray getInternalMessages(String po_num) throws SQLException {
		String po_data = "SELECT * FROM CHATMESSAGE where PONUMBER = ? "
				+ "and INVOICENUMBER is null AND STATUS = ? order by createdon desc";
		ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, po_num);
			ps.setString(2, "A");
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
		} catch (Exception e) {
			log.error("getInternalMessages() :", e.fillInStackTrace());

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

	public JSONArray getPODetailEvent(String po_num, String emailid) throws SQLException {

		String po_data = "SELECT * FROM poeventdetails where PONumber =?";
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, po_num);
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
				poData.put("BUSINESSPARTNERTEXT", rs.getString("BUSINESSPARTNERTEXT"));
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
				POList.add(poData);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			log.error("getPODetailEvent() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0) {
			responsejson.put("poData", POList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray getPODetailEventbuyer(String po_num, String emailid) throws SQLException {

		String po_data = "SELECT pd.PONumber,pd.BUSINESSPARTNEROID,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.DELIVERYDATE,pd.COMPANY,"
				+ " pd.PLANT,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.UNITOFMEASURE,pd.CONTACTPERSONEMAILID,"
				+ "pd.CONTACTPERSONPHONE,pd.BUSINESSPARTNERTEXT,pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,"
				+ "pd.COUNTRY,pd.PINCODE,pd.STATUS,pd.CREATEDON,pd.MODIFIEDON,pd.MATERIAL_TYPE,pd.ORDERNUMBER,pd.RATEPERQTY,pd.REMARK"
				+ ",pd.BALANCE_QTY,pd.QUANTITY,pd.CURRENCY FROM poeventdetails pd join podetails po  on pd.PONumber =po.PONumber where pd.PONumber =? and po.CONTACTPERSONEMAILID=?";
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, po_num);
			ps.setString(2, emailid);
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
				poData.put("BUSINESSPARTNERTEXT", rs.getString("BUSINESSPARTNERTEXT"));
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
				POList.add(poData);
			}

		} catch (SQLException e) {
			log.error("getPODetailEventbuyer() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0) {
			responsejson.put("poData", POList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}

		return jsonArray;

	}

	public JSONArray getPODetailEventPayer(String po_num) throws SQLException {

		String po_data = "SELECT pd.PONumber,pd.BUSINESSPARTNEROID,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,pd.DELIVERYDATE,pd.COMPANY,"
				+ " pd.PLANT,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.UNITOFMEASURE,pd.CONTACTPERSONEMAILID,"
				+ "pd.CONTACTPERSONPHONE,pd.BUSINESSPARTNERTEXT,pd.DELIVERYADDRESS1,pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,"
				+ "pd.COUNTRY,pd.PINCODE,pd.STATUS,pd.CREATEDON,pd.MODIFIEDON,pd.MATERIAL_TYPE,pd.ORDERNUMBER,pd.RATEPERQTY,pd.REMARK"
				+ ",pd.BALANCE_QTY,pd.QUANTITY,pd.CURRENCY FROM poeventdetails pd " + "where pd.PONumber =? ";
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, po_num);
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
				poData.put("BUSINESSPARTNERTEXT", rs.getString("BUSINESSPARTNERTEXT"));
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
				POList.add(poData);
			}

		} catch (SQLException e) {
			log.error("getPODetailEventPayer() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0) {
			responsejson.put("poData", POList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray getinternalPOSubmitQuery(String po_num, String emailid, String invoicenumber, String message,
			String subject, String status) throws SQLException {

		String po_data = "";
		String bid = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			String queryFindPoDetails1 = "Select BUSINESSPARTNEROID from PODETAILS where PONUMBER = ? ";
			ps = con.prepareStatement(queryFindPoDetails1);
			ps.setString(1, po_num);
			rs = ps.executeQuery();
			if (rs.next()) {
				bid = rs.getString("BUSINESSPARTNEROID") == null ? "" : rs.getString("BUSINESSPARTNEROID");
			}
			rs.close();
			ps.close();
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
						endUserId = rs.getString("REQUSITIONER") == null ? "-" : rs.getString("REQUSITIONER");

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
				ps1 = con.prepareStatement(queryBusinessPartner);
				ps1.setString(1, bid);
				ps1.setString(2, "A");
				rs1 = ps1.executeQuery();

				if (rs1.next()) {
					primaryEmailid = rs1.getString("PRIMARYEMAILID") == null ? "-" : rs1.getString("PRIMARYEMAILID");
					secondaryEmailid = rs1.getString("SECONDARYEMAILID") == null ? "-"
							: rs1.getString("SECONDARYEMAILID");
					tertiaryEmailid = rs1.getString("TERTIARYEMAILID") == null ? "-" : rs1.getString("TERTIARYEMAILID");
					emailArrayList.add(primaryEmailid);
					emailArrayList.add(secondaryEmailid);
					emailArrayList.add(tertiaryEmailid);
				}
				rs1.close();
				ps1.close();
				for (int counter = 0; counter <= emailArrayList.size() - 1; counter++) {

					String queryChat = "insert into CHATSTATUS (BUSINESSPARTNEROID,INVOICENUMBER,PONUMBER,"
							+ "LOGGEDIN,STATUS) values (?,?,?,?,?)";
					ps1 = con.prepareStatement(queryChat);
					ps1.setString(1, bid);
					ps1.setString(2, invoicenumber);
					ps1.setString(3, po_num);
					ps1.setString(4, emailArrayList.get(counter));
					ps1.setString(5, "A");
					ps1.executeUpdate();
					ps1.close();
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
			log.error("getinternalPOSubmitQuery() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getinternalPOReadStatus(String userMailId) throws SQLException {

		String po_data = "Select PONUMBER,STATUS from CHATSTATUS where" + " loggedin = ? and INVOICENUMBER is null ";
		String poninvoices = "Select PONUMBER from poninvoicesummery where MESSAGE= ? " + "and INVOICENUMBER is null ";

		ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> mapQueryList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, userMailId);
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
			ps.setString(1, "Y");
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
		} catch (Exception e) {
			log.error("getinternalPOReadStatus() :", e.fillInStackTrace());

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

	public JSONArray setInternalChatStatusUpdate(String bid, String userMailId, String invoiceNumber, String poNumber)
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
						PreparedStatement ps1 = con.prepareStatement(queryChat);
						ps1.setString(1, bid);
						ps1.setString(2, invoiceNumber);
						ps1.setString(3, poNumber);
						ps1.setString(4, userMailId);
						ps1.setString(5, "R");
						ps1.executeUpdate();
						ps1.close();
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
			log.error("setInternalChatStatusUpdate() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			con.rollback();
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getInternalInvoiceReadStatus(String userMailId) throws SQLException {

		String po_data = "Select PONUMBER,INVOICENUMBER,STATUS from CHATSTATUS where "
				+ "loggedin =?  and INVOICENUMBER is not null";
		String poninvoices = "Select PONUMBER,INVOICENUMBER from poninvoicesummery where"
				+ " MESSAGE=? and INVOICENUMBER is not null";

		ArrayList<HashMap<String, String>> POQueryList = new ArrayList<HashMap<String, String>>();
		ArrayList<HashMap<String, String>> mapQueryList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, userMailId);
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
			ps.setString(1, "Y");
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
			log.error("getInternalInvoiceReadStatus() :", e.fillInStackTrace());

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

	public JSONArray getVendorDetailsbuyer(String email) throws SQLException {

		ArrayList<HashMap<String, String>> vendorbuyerList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> typeindustry = new HashMap<String, String>();
		String sql = "select DISTINCT pd.ponumber,pd.poamount,pd.podate,pd.businesspartneroid,pd.buyer,bp.contactname,bp.contactemailid,"
				+ " bp.contactmobilenumber,ba.attributevalue from PODETAILS pd join BUSINESSPARTNERCONTACTS bp on pd.businesspartneroid=bp.businesspartneroid"
				+ " join BUSINESSPARTNERATTRIBUTES ba on pd.businesspartneroid=ba.businesspartneroid and bp.businesspartneroid=ba.businesspartneroid "
				+ "where pd.BUSINESSPARTNEROID in(select BUSINESSPARTNEROID from PODETAILS where buyer= ? ) and pd.buyer "
				+ "is not null and ba.attributetext='PURCH_GROUP'";

		String sqlin = "select DISTINCT pd.ponumber,ba.attributevalue from PODETAILS pd join BUSINESSPARTNERCONTACTS bp on "
				+ "pd.businesspartneroid=bp.businesspartneroid  join BUSINESSPARTNERATTRIBUTES ba on "
				+ "pd.businesspartneroid=ba.businesspartneroid and bp.businesspartneroid=ba.businesspartneroid"
				+ " where pd.BUSINESSPARTNEROID in(select BUSINESSPARTNEROID from PODETAILS where buyer= ? ) and pd.buyer is not null "
				+ "and ba.attributetext='TYPE_OF_INDUSTRY'";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(sqlin);
			ps.setString(1, email);
			rs = ps.executeQuery();
			while (rs.next()) {
				typeindustry.put(rs.getString("PONUMBER"), rs.getString("attributevalue"));
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement(sql);
			ps.setString(1, email);
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> vendorData = new HashMap<String, String>();
				vendorData.put("PO_NUMBER", rs.getString("ponumber"));
				vendorData.put("POAMOUNT", rs.getString("poamount"));
				vendorData.put("PODATE", rs.getString("podate"));
				vendorData.put("BUSINESSPARTNEROID", rs.getString("businesspartneroid"));
				vendorData.put("BUYER", rs.getString("buyer"));
				vendorData.put("CONTACTNAME", rs.getString("contactname"));
				vendorData.put("CONTACTEMAILID", rs.getString("contactemailid"));
				vendorData.put("CONTACTMOBILENUMBER", rs.getString("contactmobilenumber"));
				vendorData.put("PURCH_GROUP", rs.getString("attributevalue"));

				for (Entry<String, String> entry : typeindustry.entrySet()) {
					if (entry.getKey().equals(rs.getString("PONumber"))) {
						vendorData.put("TYPE_OF_INDUSTRY", entry.getValue());
					}
				}
				vendorbuyerList.add(vendorData);
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getVendorDetailsbuyer() :", e.fillInStackTrace());

			typeindustry.put("PONUMBER", "No data Found");
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		if (vendorbuyerList.size() > 0) {
			responsejson.put("poData", vendorbuyerList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray getInternalPoninvoiceSummery(String emailid) {
		responsejson.put("message", "No Data Found for given Email Id");
		jsonArray.add(responsejson);
		return jsonArray;
	}

	/*
	 * public JSONArray downloadapprovalinvoicelist(List<String> invoicedata,
	 * List<String> podata, String email, String page) throws SQLException {
	 * 
	 * String downloadquery = " SELECT DISTINCT " +
	 * " inv.ponumber                                             ponumber, " +
	 * " inv.invoicenumber                                        invoicenumber, " +
	 * " po.expensesheetid                                        expensesheetid, "
	 * +
	 * " TRIM(to_char((po.paymentamount), '999,999,999,999,999')) AS paymentamount, "
	 * + " to_char((inv.invoicedate), 'DD-MON-RRRR')                invoicedate, " +
	 * " bp.businesspartnertext                                   businesspartnertext, "
	 * + " inv.enduseid                                             enduseid, " +
	 * " inv.enduserstatus                                        enduserstatus, " +
	 * " po.overallstatus                                         overallstatus, " +
	 * " to_char(po.createdon, 'DD-MON-RRRR')                   createdon, " +
	 * " TRIM(to_char(po.amount, '999,999,999,999,999'))        AS amount, " +
	 * " inv.proxy                                                proxyb, " +
	 * " po.businesspartneroid                                    businesspartneroid, "
	 * + " po.grnnumber                                             grnnumber, " +
	 * " po.utrchequenumber                                       utrchequenumber, "
	 * + " purchasinggroup                                       purchasinggroup, "
	 * + " to_char(ds.grndate, 'DD-MON-RRRR')                     grndate, " +
	 * " to_char(po.utrdate, 'DD-MON-RRRR')                     utrdate, " +
	 * " po.plant                                              plantcode " +
	 * " FROM " + " invoiceapproval   inv, " + " poninvoicesummery po, " +
	 * " businesspartner   bp, " + " podetails         pd, " +
	 * " deliverysummary   ds " + " WHERE " +
	 * " inv.invoicenumber = po.invoicenumber " +
	 * " AND po.invoicenumber = ds.invoicenumber " +
	 * " AND inv.invoicenumber = ds.invoicenumber " +
	 * " AND pd.ponumber = po.ponumber " + " AND pd.ponumber = ds.ponumber " +
	 * " AND inv.ponumber = po.ponumber " + " AND pd.ponumber = inv.ponumber " +
	 * " AND po.businesspartneroid = bp.businesspartneroid " +
	 * " AND po.invoicenumber = ? " + " AND po.ponumber = ? ";
	 * 
	 * List<List<String>> POListEvent = new ArrayList<List<String>>(); Connection
	 * con = null; PreparedStatement ps = null; ResultSet rs = null; try { con =
	 * DBConnection.getConnection();
	 * 
	 * for (int z = 0; z < invoicedata.size(); z++) { ps =
	 * con.prepareStatement(downloadquery); ps.setString(1, invoicedata.get(z));
	 * ps.setString(2, podata.get(z)); rs = ps.executeQuery();
	 * 
	 * while (rs.next()) { List<String> poEvent = new ArrayList<String>();
	 * 
	 * poEvent.add(rs.getString("INVOICENUMBER")); poEvent.add(podata.get(z));
	 * poEvent.add(rs.getString("INVOICEDATE"));
	 * poEvent.add(rs.getString("CREATEDON") == null ? "" :
	 * rs.getString("CREATEDON")); poEvent.add(rs.getString("BUSINESSPARTNERTEXT"));
	 * poEvent.add(rs.getString("ENDUSEID")); String confirmers = ""; confirmers =
	 * getConfirmerlist(rs.getString("INVOICENUMBER"), rs.getString("PONUMBER"),
	 * con) .replace("_", ","); if (confirmers == null ||
	 * confirmers.equalsIgnoreCase("null")) { poEvent.add(""); } else {
	 * poEvent.add(confirmers); } String managers = ""; managers =
	 * getMGRlist(rs.getString("INVOICENUMBER"), rs.getString("PONUMBER"),
	 * con).replace("_", ","); if (managers == null ||
	 * managers.equalsIgnoreCase("null")) { poEvent.add(""); } else {
	 * poEvent.add(managers); } if (rs.getString("OVERALLSTATUS") == null) {
	 * poEvent.add("-"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("A")) {
	 * poEvent.add("Approved"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("M") ||
	 * rs.getString("OVERALLSTATUS").equalsIgnoreCase("P")) {
	 * poEvent.add("Pending"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("O")) {
	 * poEvent.add("On Hold"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("R")) {
	 * poEvent.add("Rejected"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("V")) {
	 * poEvent.add("Returned"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PRO")) {
	 * poEvent.add("Processed"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PD")) { poEvent.add("Paid");
	 * } else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PP")) {
	 * poEvent.add("Partially Paid"); } else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("RO")) {
	 * poEvent.add("Reopened"); }else if
	 * (rs.getString("OVERALLSTATUS").equalsIgnoreCase("INV")) {
	 * poEvent.add("Invalid Invoice"); } else { poEvent.add("-"); }
	 * poEvent.add(rs.getString("amount"));
	 * poEvent.add(rs.getString("PAYMENTAMOUNT"));
	 * poEvent.add(rs.getString("EXPENSESHEETID"));
	 * poEvent.add(rs.getString("UTRCHEQUENUMBER"));
	 * poEvent.add(rs.getString("UTRDATE") == null ? "" : rs.getString("UTRDATE"));
	 * poEvent.add(rs.getString("PURCHASINGGROUP"));
	 * poEvent.add(rs.getString("GRNNUMBER")); poEvent.add(rs.getString("GRNDATE")
	 * == null ? "" : rs.getString("GRNDATE")); String plantCode =
	 * rs.getString("PLANTCODE") == null ? "" : rs.getString("PLANTCODE");
	 * poEvent.add(plantCode); if ("".equalsIgnoreCase(plantCode)) {
	 * poEvent.add(""); } else { String plantName =
	 * getPlantName(rs.getString("PLANTCODE"), con); if (plantName == null ||
	 * plantName.equalsIgnoreCase("null")) { poEvent.add(""); } else {
	 * poEvent.add(plantName); } } POListEvent.add(poEvent); } rs.close();
	 * ps.close();
	 * 
	 * } if (POListEvent.size() > 0) { String encodedfile =
	 * downloadinernalfile(POListEvent); if (encodedfile.equalsIgnoreCase("")) {
	 * responsejson.put("message", "Fail"); } else { responsejson.put("message",
	 * "Success"); responsejson.put("data", encodedfile); } } else {
	 * responsejson.put("message", "Fail"); }
	 * 
	 * jsonArray.add(responsejson); } catch (Exception e) {
	 * log.error("downloadapprovalinvoicelist() :", e.fillInStackTrace());
	 * 
	 * } finally { DBConnection.closeConnection(rs, ps, con); } return jsonArray; }
	 */
	private boolean checkgrnwithmanager(Connection con, String inv, String po, String email) {

		String findusersDetails = " SELECT count(*) as counter" + " FROM INVOICEAPPROVAL I"
				+ " WHERE  I.INVOICENUMBER = ? AND I.PONUMBER = ? AND I.STATUS in ('CA','CM','CO') "
				+ "AND I.EUMANAGER =? ";

		boolean flag = true;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = con.prepareStatement(findusersDetails);
			ps.setString(1, inv);
			ps.setString(2, po);
			ps.setString(3, email);
			rs = ps.executeQuery();
			int count = 0;
			if (rs.next()) {
				count = rs.getInt("counter");
			}
			if (count > 0) {
				flag = false;
			}

		} catch (Exception e) {
			log.error("checkgrnwithmanager() :", e.fillInStackTrace());

		}
		return flag;
	}

	/*
	 * private String downloadinernalfile(List<List<String>> totallist) { String
	 * encodedfile = ""; XSSFWorkbook workbook = new XSSFWorkbook(); try {
	 * FileOutputStream out = new FileOutputStream(new File("demo.xlsx")); XSSFSheet
	 * sheet = workbook.createSheet("Invoice Data"); List<String> heading = new
	 * ArrayList<String>(); heading.add("INVOICE NUMBER"); heading.add("PO NUMBER");
	 * heading.add("INVOICE DATE"); heading.add("CREATED DATE");
	 * heading.add("VENDOR NAME"); heading.add("REQUISITIONER/STOREKEEPER");
	 * heading.add("GRN CONFIRMERS"); heading.add("INVOICE APPROVERS");
	 * heading.add("OVERALL STATUS"); heading.add("INVOICE AMOUNT");
	 * 
	 * heading.add("PAID AMOUNT"); heading.add("EXPENSE SHEET ID");
	 * heading.add("UTR NUMBER"); heading.add("UTR DATE");
	 * heading.add("PURCHASE GROUP"); heading.add("GRN NUMBER");
	 * heading.add("GRN DATE"); heading.add("PLANT CODE");
	 * heading.add("PLANT NAME");
	 * 
	 * Iterator<String> tempIterator1 = heading.iterator(); Iterator<List<String>> i
	 * = totallist.iterator(); int rownum = 0; int cellnum = 0; Row row1 =
	 * sheet.createRow(rownum++); while (tempIterator1.hasNext()) { String temp =
	 * (String) tempIterator1.next(); Cell cell = row1.createCell(cellnum++);
	 * 
	 * XSSFFont fontBold = workbook.createFont(); fontBold.setBold(true); CellStyle
	 * cellStyle1 = workbook.createCellStyle();
	 * cellStyle1.setAlignment(HorizontalAlignment.CENTER); XSSFRichTextString
	 * cellValue = new XSSFRichTextString(); cellValue.append(temp, fontBold);
	 * cell.setCellValue(cellValue); cell.setCellStyle(cellStyle1); } rownum++;
	 * while (i.hasNext()) { List<String> templist = (List<String>) i.next();
	 * Iterator<String> tempIterator = templist.iterator(); Row row =
	 * sheet.createRow(rownum++); cellnum = 0; int k = 0; while
	 * (tempIterator.hasNext()) { String temp = (String) tempIterator.next(); Cell
	 * cell = row.createCell(cellnum++); sheet.autoSizeColumn(cellnum); CellStyle
	 * cellStyle1 = workbook.createCellStyle(); if (k == 0) {
	 * cellStyle1.setAlignment(HorizontalAlignment.LEFT); } else if (k == 1) {
	 * cellStyle1.setAlignment(HorizontalAlignment.CENTER); } else if (k == 2) {
	 * cellStyle1.setAlignment(HorizontalAlignment.CENTER); } else if (k == 3) {
	 * cellStyle1.setAlignment(HorizontalAlignment.CENTER); } else if (k == 4) {
	 * cellStyle1.setAlignment(HorizontalAlignment.LEFT); } else if (k == 5) {
	 * cellStyle1.setAlignment(HorizontalAlignment.LEFT); } else if (k == 6) {
	 * cellStyle1.setAlignment(HorizontalAlignment.LEFT); } else if (k == 7) {
	 * cellStyle1.setAlignment(HorizontalAlignment.LEFT); } else if (k == 8) {
	 * cellStyle1.setAlignment(HorizontalAlignment.RIGHT); } else if (k == 9) {
	 * cellStyle1.setAlignment(HorizontalAlignment.RIGHT); } else if (k == 10) {
	 * cellStyle1.setAlignment(HorizontalAlignment.RIGHT); } else if (k == 11) {
	 * cellStyle1.setAlignment(HorizontalAlignment.RIGHT); } else if (k == 12) {
	 * cellStyle1.setAlignment(HorizontalAlignment.RIGHT); } else if (k == 13) {
	 * cellStyle1.setAlignment(HorizontalAlignment.CENTER); } else if (k == 14) {
	 * cellStyle1.setAlignment(HorizontalAlignment.RIGHT); } else if (k == 15) {
	 * cellStyle1.setAlignment(HorizontalAlignment.RIGHT); } else if (k == 16) {
	 * cellStyle1.setAlignment(HorizontalAlignment.CENTER); } else if (k == 17) {
	 * cellStyle1.setAlignment(HorizontalAlignment.RIGHT); } else if (k == 18) {
	 * cellStyle1.setAlignment(HorizontalAlignment.LEFT); }
	 * 
	 * cell.setCellValue(temp); cell.setCellStyle(cellStyle1); k++; } }
	 * workbook.write(out);
	 * 
	 * out.close(); workbook.close(); POImpl poimpl = new POImpl();
	 * 
	 * byte[] temp = poimpl.convert(sheet); File file = new File("demo.xlsx"); try {
	 * InputStream inputStream; inputStream = new FileInputStream(file); byte[]
	 * bytes1 = new byte[(int) file.length()]; inputStream.read(bytes1); encodedfile
	 * = new String(Base64.encodeBase64(bytes1), "UTF-8"); } catch (IOException e) {
	 * log.error("downloadinernalfile() 1 :", e.fillInStackTrace());
	 * 
	 * }
	 * 
	 * } catch (Exception e) { log.error("downloadinernalfile() 2: ",
	 * e.fillInStackTrace());
	 * 
	 * } return encodedfile; }
	 */
	boolean sendMailtoManager(String invoiceno, String pono, String emailid, Connection con) throws SQLException {
		String poNumber = null;
		String invoiceNumber = null;
		String vendorName = null;
		String invoiceDetails = null;
		String amount = null;
		String primaryEmailId = null;
		String secondaryEmailId = null;
		String tertiaryEmailId = null;
		String requsitionerEmailId = null;
		String buyerEmailId = null;
		String endUserId = null;
		String euManager = null;
		String mailingList = "";
		String status = null;
		String overallStatus = null;
		String endUserStatus = null;
		String managerStatus = null;
		ArrayList managerList = new ArrayList();

		String findusersDetails = " SELECT B.BUSINESSPARTNERTEXT AS VENDOR_NAME, P.PONUMBER AS PONUMBER, P.INVOICENUMBER AS INVOICENUMBER,"
				+ " P.AMOUNT AS AMOUNT, P.OVERALLSTATUS AS OVERALLSTATUS, P.REQUSITIONER AS REQUSITIONER, P.BUYER AS BUYER,"
				+ " I.ENDUSEID AS ENDUSEID, I.ENDUSERSTATUS AS ENDUSERSTATUS,"
				+ " I.EUMANAGER AS EUMANAGER ,I.STATUS AS MANAGERSTATUS"
				+ " FROM PONINVOICESUMMERY P ,BUSINESSPARTNER B ,INVOICEAPPROVAL I"
				+ " WHERE B.BUSINESSPARTNEROID = P.BUSINESSPARTNEROID AND I.INVOICENUMBER = P.INVOICENUMBER AND"
				+ " I.PONUMBER = P.PONUMBER  AND P.INVOICENUMBER = ? AND P.PONUMBER = ? "
				+ " ORDER BY DECODE(I.STATUS,'CM','X','M','Y','A'), I.MODIFIEDDATE ASC";

		boolean flag = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(findusersDetails);
			ps.setString(1, invoiceno);
			ps.setString(2, pono);
			rs = ps.executeQuery();
			int count = 0;
			while (rs.next()) {

				if (count == 0) {

					vendorName = rs.getString("VENDOR_NAME") == null ? "" : rs.getString("VENDOR_NAME");
					poNumber = rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER");
					invoiceNumber = rs.getString("INVOICENUMBER") == null ? "" : rs.getString("INVOICENUMBER");
					amount = rs.getString("AMOUNT") == null ? "" : rs.getString("AMOUNT");
					status = rs.getString("OVERALLSTATUS") == null ? "" : rs.getString("OVERALLSTATUS");
					requsitionerEmailId = rs.getString("REQUSITIONER") == null ? "" : rs.getString("REQUSITIONER");
					buyerEmailId = rs.getString("BUYER") == null ? "" : rs.getString("BUYER");
					endUserId = rs.getString("ENDUSEID") == null ? "" : rs.getString("ENDUSEID");
					endUserStatus = rs.getString("ENDUSERSTATUS") == null ? "" : rs.getString("ENDUSERSTATUS");
					if (!("".equalsIgnoreCase(endUserId)) && !("NA".equalsIgnoreCase(endUserId))) {
						managerList.add(endUserId + "-" + endUserStatus);
					}

					count++;

				}
				euManager = rs.getString("EUMANAGER") == null ? "" : rs.getString("EUMANAGER");
				managerStatus = rs.getString("MANAGERSTATUS") == null ? "" : rs.getString("MANAGERSTATUS");
				managerList.add(euManager + "-" + managerStatus);
				if (!mailingList.contains(euManager)) {
					if (!emailid.equalsIgnoreCase(euManager)) {
						if (!("".equalsIgnoreCase(euManager)) && !("NA".equalsIgnoreCase(euManager))) {
							mailingList += euManager + ",";
						}

					}
				}

			}
			rs.close();
			ps.close();
			managerStatus = "";
			if (!mailingList.contains(requsitionerEmailId)) {
				if (!emailid.equalsIgnoreCase(requsitionerEmailId)) {

					if (!("".equalsIgnoreCase(requsitionerEmailId)) && !("NA".equalsIgnoreCase(requsitionerEmailId))) {
						mailingList += requsitionerEmailId + ",";
					}
				}
			}
			if (!mailingList.contains(buyerEmailId)) {
				if (!emailid.equalsIgnoreCase(buyerEmailId)) {
					if (!("".equalsIgnoreCase(buyerEmailId)) && !("NA".equalsIgnoreCase(buyerEmailId))) {
						mailingList += buyerEmailId + ",";
					}

				}
			}
			if (!mailingList.contains(endUserId)) {
				if (!emailid.equalsIgnoreCase(endUserId)) {
					if (!("".equalsIgnoreCase(endUserId)) && !("NA".equalsIgnoreCase(endUserId))) {
						mailingList += endUserId + ",";
					}

				}
			}

			mailingList = mailingList.substring(0, mailingList.length() - 1);
			log.info("mailingList  :(" + mailingList + ")");
			StringBuilder buf = new StringBuilder();
			buf.append("Dear Colleague,<br><br> Below are the details with status for invoice number : " + invoiceNumber
					+ ". " + "<br><br><b>Vendor Name: " + vendorName + "</b><br><br>"
					+ "<!DOCTYPE html> <html><head><style> table { font-family: arial, sans-serif; border-collapse: collapse; width: 100%; }td, "
					+ "th { border: 1px solid #dddddd; text-align: left; padding: 8px; }tr:nth-child(even) { background-color: #dddddd; }"
					+ " </style> </head> <body> <table>" + " <tr>" + " <th>Sr.No</th>" + " <th>Invoice No.</th>"
					+ " <th>Invoice Amount.</th>  <th>Invoice Status </th>" + " </tr>");

			if ("A".equals(status)) {
				overallStatus = "Approved";
			} else if ("R".equals(status)) {
				overallStatus = "Rejected";
			} else if ("O".equals(status)) {
				overallStatus = "On Hold";
			} else if ("M".equals(status)) {
				overallStatus = "Pending";
			} else if ("V".equals(status)) {
				overallStatus = "Returned";
			}
			buf.append("<tr><td>1.</td>").append("<td>").append(invoiceNumber).append("</td><td>").append(amount)
					.append("</td><td>").append(overallStatus).append("</td></tr>");

			buf.append("</table> </body><br><br><b>Approval Status :</b><br><br>");

			buf.append("<table><tr><td>Approver Email ID</td><td>Status</td></tr>");

			for (int countM = 0; countM < managerList.size(); countM++) {
				String managerData = (String) managerList.get(countM);
				String managerListStatus[] = managerData.split("-");
				managerStatus = "";

				if ("A".equals(managerListStatus[1])) {
					if (endUserId.equalsIgnoreCase(managerListStatus[0])) {
						managerStatus = "GRN Creator";
					} else {
						managerStatus = "Invoice Approver";
					}
				} else if ("R".equals(managerListStatus[1])) {
					managerStatus = "Rejected";
				} else if ("O".equals(managerListStatus[1])) {
					managerStatus = "On Hold";
				} else if ("M".equals(managerListStatus[1])) {
					managerStatus = "Pending";
				} else if ("V".equals(managerListStatus[1])) {
					managerStatus = "Returned";
				} else if ("CO".equals(managerListStatus[1])) {
					managerStatus = "On Hold";
				} else if ("CM".equals(managerListStatus[1])) {
					managerStatus = "Pending";
				} else if ("CA".equals(managerListStatus[1])) {
					managerStatus = "GRN Confirmed";
				}

				buf.append("<tr><td>" + managerListStatus[0] + "</td><td>" + managerStatus + "</td>");
			}
			buf.append("</table><br><br>");
			buf.append(
					"</html> <br>Click <a href='https://timescape.timesgroup.com/irj/portal?NavigationTarget=navurl://d97ea9098661623a123464628538661c'>here</a> to check/approve invoice details."
							+ " <br><br>Regards,<br><br> BCCL PartnerDx Team <br>");
			String content = buf.toString();

			String Subject = "BCCL PartnerDx : " + overallStatus + " - " + invoiceNumber;
			log.info("content : " + content);
			String toAddr = mailingList;
			String ccAddr = emailid;

			log.info("toAddr :" + toAddr + "ccAddr : " + ccAddr);

			Hashtable mailDetails = new Hashtable();
			mailDetails.put("fromAddr", "noreply.partnerdx@timesgroup.com");
			mailDetails.put("toAddr", toAddr);
			mailDetails.put("ccAddr", ccAddr);
			mailDetails.put("subject", Subject);
			mailDetails.put("content", content);
			log.info("Mailing list " + mailingList);
			SendImapMessage myMail = new SendImapMessage();
			Properties prop = new Properties();
			InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
			prop.load(input);
			String server = prop.getProperty("servertype");
			if ("dev".equalsIgnoreCase(server)) {
				flag = true;
			} else if ("uat".equalsIgnoreCase(server)) {
				// flag = myMail.sendHtmlMail(mailDetails);
				flag = true;
			}

		} catch (Exception e) {
			log.error("sendMailtoManager() :", e.fillInStackTrace());

		} finally {
		}
		return flag;

	}

	public JSONArray reassigninvoice(String invoicenumber, String ponumber, String emailtobereassigned,
			String useremailid) throws SQLException {

		String reassignquery = "update INVOICEAPPROVAL set ENDUSEID=?, REASSIGNSTATUS=?,PROXY=?"
				+ "where INVOICENUMBER=? AND PONUMBER=?";
		int value = 0;
		int value1 = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(reassignquery);
			ps.setString(1, emailtobereassigned);
			ps.setString(2, "Z");
			ps.setString(3, null);
			ps.setString(4, invoicenumber);
			ps.setString(5, ponumber);
			value = ps.executeUpdate();
			ps.close();
			if (value > 0) {

				String insertaudit = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
						+ "MODIFIEDTIME,MODIFIEDBY,REASSIGNEDTO)" + " values(?,?,?,?,?,?,?)";

				ps = con.prepareStatement(insertaudit);
				ps.setString(1, invoicenumber);
				ps.setString(2, ponumber);
				ps.setString(3, "");
				ps.setString(4, "Z");
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(6, useremailid);
				ps.setString(7, emailtobereassigned);
//				ps.executeUpdate();
				value1 = ps.executeUpdate();
				ps.close();
				if (value1 > 0) {
					String updateoverallstatus = "update PONINVOICESUMMERY set OVERALLSTATUS=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";

					ps = con.prepareStatement(updateoverallstatus);
					ps.setString(1, "P");
					ps.setString(2, invoicenumber);
					ps.setString(3, ponumber);
					value = ps.executeUpdate();
					ps.close();
					String approvalstatusupdate = "update INVOICEAPPROVAL set ENDUSERSTATUS=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";

					ps = con.prepareStatement(approvalstatusupdate);
					ps.setString(1, "P");
					ps.setString(2, invoicenumber);
					ps.setString(3, ponumber);
					value = ps.executeUpdate();
					ps.close();
				}
			}

			con.commit();
			if (value > 0) {
				responsejson.put("message", "Success");
			} else {
				responsejson.put("message", "Fail");
			}
		} catch (SQLException e) {
			log.error("reassigninvoice() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getstoragelocation(String plant) {

		String getstoragelocation = "Select PLANT,STORAGELOCATION,LOCATIONDESC,ATTRIBUTEVALUE,STATUS "
				+ "from STORAGELOCATIONMASTER where PLANT=? AND STATUS=? ORDER BY LOCATIONDESC asc";
		ArrayList<HashMap<String, String>> locationlist = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			int bCount = 0;
			con = DBConnection.getConnection();

			ps = con.prepareStatement(getstoragelocation);
			ps.setString(1, plant);
			ps.setString(2, "A");

			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("PLANT", rs.getString("PLANT"));
				poData.put("STORAGELOCATION", rs.getString("STORAGELOCATION"));
				poData.put("LOCATIONDESC", rs.getString("LOCATIONDESC"));
				poData.put("ATTRIBUTEVALUE", rs.getString("ATTRIBUTEVALUE"));
				poData.put("STATUS", rs.getString("STATUS"));
				locationlist.add(poData);
			}
			rs.close();
			ps.close();
			if (locationlist.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("storagelist", locationlist);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getstoragelocation() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);

		}
		return jsonArray;

	}

	public JSONArray updateenduserspecialdetails(String multipleactualfilename, String multiplesavedfilename,
			String enduserremark, String invoicenumber, String ponumber) throws SQLException {
		int value = 0;
		String updateponinvoicesummery = "update PONINVOICESUMMERY set ENDUSERSUPPACTUALFILE=?,"
				+ "ENDUSERSUPPSAVEDFILE=?, ENDUSERREMARKS=? " + "where INVOICENUMBER=? AND PONUMBER=?";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			ps = con.prepareStatement(updateponinvoicesummery);
			ps.setString(1, multipleactualfilename);
			ps.setString(2, multiplesavedfilename);
			ps.setString(3, enduserremark);
			ps.setString(4, invoicenumber);
			ps.setString(5, ponumber);
			value = ps.executeUpdate();
			con.commit();
			if (value > 0) {
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("updateenduserspecialdetails() :", e.fillInStackTrace());

			con.rollback();

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
				String invoicenumber = temp[3];
				invoice = temp[3];
				String lineitemnumber = temp[1];
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
				ps.setString(3, null);
				ps.setString(4, invoicenumber);
				ps.setString(5, ponumber);
				ps.setString(6, lineitemnumber);
				value = ps.executeUpdate();
				ps.close();
			}
			String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
					+ "where INVOICENUMBER=? AND PONUMBER=?";

			ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
			ps.setString(1, enduser);
			ps.setString(2, null);
			ps.setString(3, invoice);
			ps.setString(4, po);
			value = ps.executeUpdate();
			ps.close();

			String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY_BEHALF (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

			ps = con.prepareStatement(insertauditacceptqty);
			ps.setString(1, po);
			ps.setString(2, invoice);
			ps.setString(3, email);
			ps.setString(4, "Y");
			ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
			value = ps.executeUpdate();
			ps.close();

			con.commit();
			if (value > 0) {
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("updateacceptedquantitywithoutgrn() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	// uat
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

			for (int k = 0; k < values.size(); k++) {

				String[] temp = billDatearr[k].split(",");
				String ponumber = temp[0];
				po = temp[0];
				String invoicenumber = temp[3];
				invoice = temp[3];
				String lineitemnumber = temp[1];
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
				ps.setString(3, null);
				ps.setString(4, invoicenumber);
				ps.setString(5, ponumber);
				ps.setString(6, lineitemnumber);
				value = ps.executeUpdate();
				ps.close();
			}
			String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
					+ "where INVOICENUMBER=? AND PONUMBER=?";

			ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
			ps.setString(1, enduser);
			ps.setString(2, null);
			ps.setString(3, invoice);
			ps.setString(4, po);
			value = ps.executeUpdate();
			ps.close();

			String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY_BEHALF (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

			ps = con.prepareStatement(insertauditacceptqty);
			ps.setString(1, po);
			ps.setString(2, invoice);
			ps.setString(3, email);
			ps.setString(4, "Y");
			ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
			value = ps.executeUpdate();
			ps.close();

			con.commit();
			if (value > 0) {
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			log.error("updateacceptedquantitywithoutgrnforprod() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

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

				comboPoLineItem = fatchedValues[1];
				poLineitem = fatchedValues[1];
				quantity = fatchedValues[5];
				poNumber = fatchedValues[0]; // 00010-10
				invoiceNumber = fatchedValues[3];
				storageLocation = fatchedValues[6];
				enduser = fatchedValues[7];
				portalid = fatchedValues[8];
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

			String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=? "
					+ "where INVOICENUMBER=? AND PONUMBER=?";

			ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
			ps.setString(1, enduser);
			ps.setString(2, invoiceNumber);
			ps.setString(3, poNumber);
			value = ps.executeUpdate();
			ps.close();

			String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY_BEHALF (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

			ps = con.prepareStatement(insertauditacceptqty);
			ps.setString(1, poNumber);
			ps.setString(2, invoiceNumber);
			ps.setString(3, email);
			ps.setString(4, "Y");
			ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
			value = ps.executeUpdate();
			ps.close();
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
			log.error("updateacceptedservicequantitywithoutgrn() :", e.fillInStackTrace());

			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	// for dev start -----
	public JSONArray updateacceptedquantity(List<String> values, String email) {
		log.info("billDatearr[0] ==> " + values);
		String[] billDatearr = values.toArray(new String[values.size()]);
		int value = 0;
		String grn = "";
		StringBuilder generatedToken = new StringBuilder();
		try {
			SecureRandom number = SecureRandom.getInstance("SHA1PRNG");
			for (int i = 0; i < 7; i++) {
				generatedToken.append(number.nextInt(7));
			}
		} catch (NoSuchAlgorithmException e) {
			log.error("getVendorDetails() :", e.fillInStackTrace());

		}
		log.info("generatedToken " + generatedToken.toString());
		grn = "GRN" + generatedToken.toString();
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
				log.info("billDatearr[0] ==> " + billDatearr[k]);
				log.info("temp============>" + temp);

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
				log.info(" The portal id is here ==> " + portalid);
				String actualquantityupdate = "update DELIVERYSUMMARY set ACCEPTEDQTY=? ,STORAGELOCATION=?,GRNNUMBER=? "
						+ "where INVOICENUMBER=? AND PONUMBER=? AND LINEITEMNUMBER=?";

				ps = con.prepareStatement(actualquantityupdate);
				ps.setString(1, actualquantity);
				ps.setString(2, storagelocation);
				ps.setString(3, grn);
				ps.setString(4, invoicenumber);
				ps.setString(5, ponumber);
				ps.setString(6, lineitemnumber);
				value = ps.executeUpdate();
				ps.close();
			}

			String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ? "
					+ " where INVOICENUMBER = ? AND PONUMBER = ? ";

			ps = con.prepareStatement(updateGRN);
			ps.setString(1, grn);
			ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(3, invoice);
			ps.setString(4, po);
			ps.executeUpdate();
			ps.close();

			if (("Y").equalsIgnoreCase(status)) {

				String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
						+ "where INVOICENUMBER=? AND PONUMBER=?";

				ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
				ps.setString(1, enduser);
				ps.setString(2, null);
				ps.setString(3, invoice);
				ps.setString(4, po);
				value = ps.executeUpdate();
				ps.close();

				String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";
				ps = con.prepareStatement(insertauditacceptqty);
				ps.setString(1, po);
				ps.setString(2, invoice);
				ps.setString(3, email);
				ps.setString(4, "Y");
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
				value = ps.executeUpdate();
				ps.close();
			}
			con.commit();

			if (value > 0) {
				responsejson.put("grn", grn);
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
			if (grn.length() == 0) {
				con.rollback();
			}
		} catch (Exception e) {
			log.error("updateacceptedquantity() :", e.fillInStackTrace());

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
		HashMap<Integer, String> grnHashMapping = new HashMap<Integer, String>();
		ArrayList lineItemlist = new ArrayList();
		ArrayList SAPReturnValues = new ArrayList();
		HashMap poLineGrnValue = new HashMap();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		int poCounter = 1;
		String showGrnList = null;
		String showScrnList = null;
		String comboPoLineitem = null;
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
				grnNumber = null;
				grnyear = null;
				grnqty = null;
				serviceNo = null;
				serviceLineitem = null;
				comboPoLineitem = null;
				String[] lineItemNo = null;

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

				if (!poLineGrnValue.containsKey(poLineitem)) { // 00010-10, 00010-20 ,00020-10
					grnHashMapping.put(poCounter, poLineitem); // {(1,"00010"),(2,"00020")}
					poLineGrnValue.put(poLineitem, ""); // 00010, 00020
					poCounter = poCounter + 1;
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
				String mainList[] = { poNumber, poLineitem, serviceLineitem, serviceNo, invoiceNumber, invoiceDate,
						quantity, storageLocation, portalid };
				lineItemlist.add(mainList);
				log.info("#" + poNumber + "#" + poLineitem + "#" + serviceLineitem + "#" + serviceNo + "#"
						+ invoiceNumber + "#" + invoiceDate + "#" + quantity + "#" + storageLocation + "#" + portalid);
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

			log.info("poLineGrnValue.size() ==> " + poLineGrnValue.size());
			for (int ii = 0; ii < poLineGrnValue.size(); ii++) {
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
					log.error("updateacceptedservicequantity() 1 :", e.fillInStackTrace());

				}
				returnGRN = generatedToken.toString();
				// grnyear = "2022";
				// grnqty = "1";
				serviceLineitem = servicelineitem.toString();
				String putGRNNSCRNValues = returnGRN + "-" + serviceLineitem;
				int key = ii + 1;
				log.info(" returnGRN ==> " + returnGRN);
				log.info(" serviceLineitem ==> " + serviceLineitem);
				if (grnHashMapping.containsKey(key)) { // {1,"00010"},{2,"00020"}}
					String mapValue = grnHashMapping.get(key); // 00010

					poLineGrnValue.put(mapValue, putGRNNSCRNValues);// {"00010","grn-srcn"},
					log.info("mapValue ==> " + mapValue);
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

					String getGrnScrnVal = (String) poLineGrnValue.get(poLineitem);
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
				showScrnList = showScrnList.substring(0, showScrnList.length() - 1);
				showGrnList = showGrnList.substring(0, showGrnList.length() - 1);
				log.info("show scrn list " + showScrnList);
				log.info("show grn list " + showGrnList);
				log.info("INVOICENUMBER " + invoiceNumber);
				log.info("PONUMBER " + poNumber);
				String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ?, "
						+ " SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? ";

				ps = con.prepareStatement(updateGRN);
				ps.setString(1, showGrnList);
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, showScrnList);
				ps.setString(4, invoiceNumber);
				ps.setString(5, poNumber);
				ps.executeUpdate();
				ps.close();
				if (("Y").equalsIgnoreCase(status)) {
					String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";

					ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
					ps.setString(1, enduser);
					ps.setString(2, null);
					ps.setString(3, invoiceNumber);
					ps.setString(4, poNumber);
					value = ps.executeUpdate();
					ps.close();

					String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

					ps = con.prepareStatement(insertauditacceptqty);
					ps.setString(1, poNumber);
					ps.setString(2, invoiceNumber);
					ps.setString(3, email);
					ps.setString(4, "Y");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					value = ps.executeUpdate();
					ps.close();
				}
			}

			if (returnGRN != null && returnGRN != "") {

				responsejson.put("grnlist", showGrnList);
				responsejson.put("scrnlist", showScrnList);
				responsejson.put("message", "Success");
				con.commit();
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
			log.error("updateacceptedservicequantity() 2 : ", e.fillInStackTrace());

			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	// for dev end -----

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
		String warningMessage = null;
		String orderNumber = null;
		String enduser = null;
		String timescapeid = null;
		String status = null;

		String portalid = null;
		ArrayList sendingList = new ArrayList();

		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		ArrayList lineItemlist = new ArrayList();
		ArrayList SAPReturnValues = new ArrayList();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			prop.load(input);

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
			SAPValues.put("IMPORTLINEITEMLIST1", sendingList);

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
						log.info("returnGRN-arrayData[0] :  " + arrayData[0] + " grnyear-arrayData[1] : " + arrayData[1]
								+ " grnqty-arrayData[2] : " + arrayData[2] + " Invoice Number : " + invoiceNumber);
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
						log.info("error :" + error + "-message -" + message + " Invoice Number : " + invoiceNumber);

						if ("W".equalsIgnoreCase(error) || "S".equalsIgnoreCase(error) || "I".equalsIgnoreCase(error)
								|| "E".equalsIgnoreCase(error)) {
							warningMessage = warningMessage + message + ",";
						}
					}
					if (warningMessage.length() > 2) {
						warningMessage = warningMessage.substring(0, warningMessage.length() - 1);
					}
				}
			}
			if (returnGRN != null && returnGRN != "") {
				con = DBConnection.getConnection();
				con.setAutoCommit(false);

				for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

					String[] fatchedValues = acceptedList[counterValue].split(",");

					poNumber = null;
					invoiceNumber = null;
					poLineitem = null;
					quantity = null;
					storageLocation = null;
					orderNumber = null;

					poNumber = fatchedValues[0]; // 00010-10
					poLineitem = fatchedValues[1];
					invoiceNumber = fatchedValues[3];
					quantity = fatchedValues[4];
					storageLocation = fatchedValues[5];
					enduser = fatchedValues[6];
					status = fatchedValues[8];

					String actualquantityupdate = "update DELIVERYSUMMARY set ACCEPTEDQTY=? ,STORAGELOCATION=?,GRNNUMBER=? "
							+ "where INVOICENUMBER=? AND PONUMBER=? AND LINEITEMNUMBER=?";

					ps = con.prepareStatement(actualquantityupdate);
					ps.setString(1, quantity);
					ps.setString(2, storageLocation);
					ps.setString(3, returnGRN);
					ps.setString(4, invoiceNumber);
					ps.setString(5, poNumber);
					ps.setString(6, poLineitem);
					value = ps.executeUpdate();
					ps.close();
				}
				String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ? "
						+ " where INVOICENUMBER = ? AND PONUMBER = ? ";

				ps = con.prepareStatement(updateGRN);
				ps.setString(1, returnGRN);
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, invoiceNumber);
				ps.setString(4, poNumber);
				ps.executeUpdate();
				ps.close();

				if (("Y").equalsIgnoreCase(status)) {
					String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";

					ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
					ps.setString(1, enduser);
					ps.setString(2, null);
					ps.setString(3, invoiceNumber);
					ps.setString(4, poNumber);
					value = ps.executeUpdate();
					ps.close();

					String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

					ps = con.prepareStatement(insertauditacceptqty);
					ps.setString(1, poNumber);
					ps.setString(2, invoiceNumber);
					ps.setString(3, email);
					ps.setString(4, "Y");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					value = ps.executeUpdate();
					ps.close();

				}
				con.commit();
			}

			if (returnGRN != null && returnGRN != "") {
				responsejson.put("grn", returnGRN);
				responsejson.put("message", "Success");
				responsejson.put("warningMessage", warningMessage == null ? "" : warningMessage);

				jsonArray.add(responsejson);
			} else {
				if (!"E".equalsIgnoreCase(error)) {
					message = warningMessage == null ? "" : warningMessage;
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

		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		ArrayList lineItemlist = new ArrayList();
		ArrayList SAPReturnValues = new ArrayList();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			prop.load(input);

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
			SAPValues.put("IMPORTLINEITEMLIST1", sendingList);
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

						log.info("error :" + error + "-message -" + message + " Invoice Number : " + invoiceNumber);

						if ("W".equalsIgnoreCase(error) || "S".equalsIgnoreCase(error) || "I".equalsIgnoreCase(error)
								|| "E".equalsIgnoreCase(error)) {
							warningMessage = warningMessage + message + ",";
						}
					}
					if (warningMessage.length() > 2) {
						warningMessage = warningMessage.substring(0, warningMessage.length() - 1);
					}
				}
			}

			if (returnGRN != null && returnGRN != "") {

				for (int counterValue = 0; counterValue < acceptedValues.size(); counterValue++) {

					String[] fatchedValues = acceptedList[counterValue].split(",");
					poNumber = null;
					invoiceNumber = null;
					poLineitem = null;
					quantity = null;
					storageLocation = null;
					orderNumber = null;

					poNumber = fatchedValues[0]; // 00010-10
					poLineitem = fatchedValues[1];
					invoiceNumber = fatchedValues[3];
					quantity = fatchedValues[4];
					storageLocation = fatchedValues[5];
					enduser = fatchedValues[6];
					status = fatchedValues[8];

					String actualquantityupdate = "update DELIVERYSUMMARY set ACCEPTEDQTY=? ,STORAGELOCATION=?,GRNNUMBER=? "
							+ "where INVOICENUMBER=? AND PONUMBER=? AND LINEITEMNUMBER=?";

					ps = con.prepareStatement(actualquantityupdate);
					ps.setString(1, quantity);
					ps.setString(2, storageLocation);
					ps.setString(3, returnGRN);
					ps.setString(4, invoiceNumber);
					ps.setString(5, poNumber);
					ps.setString(6, poLineitem);
					value = ps.executeUpdate();
					ps.close();
				}
				String updateGRN = "update PONINVOICESUMMERY set GRNNUMBER = ?,MODIFIEDON = ? "
						+ " where INVOICENUMBER = ? AND PONUMBER = ? ";

				ps = con.prepareStatement(updateGRN);
				ps.setString(1, returnGRN);
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, invoiceNumber);
				ps.setString(4, poNumber);
				ps.executeUpdate();
				ps.close();

				if (("Y").equalsIgnoreCase(status)) {
					String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";

					ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
					ps.setString(1, enduser);
					ps.setString(2, null);
					ps.setString(3, invoiceNumber);
					ps.setString(4, poNumber);
					value = ps.executeUpdate();
					ps.close();

					String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

					ps = con.prepareStatement(insertauditacceptqty);
					ps.setString(1, poNumber);
					ps.setString(2, invoiceNumber);
					ps.setString(3, email);
					ps.setString(4, "Y");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					value = ps.executeUpdate();
					ps.close();

				}
			}

			if (returnGRN != null && returnGRN != "") {
				responsejson.put("grn", returnGRN);
				responsejson.put("message", "Success");
				responsejson.put("warningMessage", warningMessage == null ? "" : warningMessage);
				jsonArray.add(responsejson);
			} else {
				if (!"E".equalsIgnoreCase(error)) {
					message = warningMessage == null ? "" : warningMessage;
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
		String warningMessage = null;
		String orderNumber = null;
		String enduser = null;
		String portalid = null;
		String status = null;

		ArrayList sendingList = new ArrayList();

		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		HashMap<Integer, String> grnHashMapping = new HashMap<Integer, String>();
		ArrayList SAPReturnValues = new ArrayList();
		HashMap poLineGrnValue = new HashMap();
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

				if (!poLineGrnValue.containsKey(poLineitem)) { // 00010-10, 00010-20 ,00020-10
					grnHashMapping.put(poCounter, poLineitem); // {(1,"00010"),(2,"00020")}
					poLineGrnValue.put(poLineitem, ""); // 00010, 00020
					poCounter = poCounter + 1;
				}

				serviceNo = fatchedValues[2];
				invoiceNumber = fatchedValues[3];
				Date date = (Date) formatter.parseObject(fatchedValues[4]);
				invoiceDate = sm.format(date);
				quantity = fatchedValues[5];
				storageLocation = fatchedValues[6];
				enduser = fatchedValues[7];
				portalid = fatchedValues[8];
				status = fatchedValues[9];

				log.info("#" + poNumber + "#" + poLineitem + "-" + serviceLineitem + "#" + invoiceDate + "#"
						+ invoiceNumber + "#" + quantity + "#" + storageLocation + "#" + portalid);

				String mainList[] = { poNumber, poLineitem, serviceLineitem, serviceNo, invoiceNumber, invoiceDate,
						quantity, storageLocation, portalid };
				sendingList.add(mainList);
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
			SAPValues.put("IMPORTLINEITEMLIST1", sendingList);

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
						String putGRNNSCRNValues = returnGRN + "-" + serviceLineitem;
						int key = ii + 1;
						if (grnHashMapping.containsKey(key)) { // {1,"00010"},{2,"00020"}}
							String mapValue = grnHashMapping.get(key); // 00010
							poLineGrnValue.put(mapValue, putGRNNSCRNValues);// {"00010","grn-srcn"},
						}
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
						error = arrayData[0];
						message = arrayData[2];
						log.info("arrayData[] 2 :" + arrayData.length + " : error : " + error + " : message : "
								+ message + " Invoice Number : " + invoiceNumber);

						if ("W".equalsIgnoreCase(error) || "S".equalsIgnoreCase(error) || "I".equalsIgnoreCase(error)
								|| "E".equalsIgnoreCase(error)) {
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
				String[] grnNscrnValue = null;

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

					String getGrnScrnVal = (String) poLineGrnValue.get(poLineitem);

					if (!("".equalsIgnoreCase(getGrnScrnVal)) && (!(getGrnScrnVal == null))) {
						grnNscrnValue = getGrnScrnVal.split("-");

						if (!showGrnList.contains(grnNscrnValue[0]) == true) {
							showGrnList = showGrnList + grnNscrnValue[0] + ",";
						}

						if (!showScrnList.contains(grnNscrnValue[1]) == true) {
							showScrnList = showScrnList + grnNscrnValue[1] + ",";
						}
					}

					log.info("showGrnList ==> " + showGrnList.toString() + "showScrnList ==> " + showScrnList.toString()
							+ " Invoice Number : " + invoiceNumber);
					log.info("grnNscrnValue[1] ==> " + grnNscrnValue[1] + "grnNscrnValue[0] ==> " + grnNscrnValue[0]
							+ " Invoice Number : " + invoiceNumber);

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

					String actualQuantityUpdate1 = "update DELIVERYSUMMARY set GRNNUMBER = ?, "
							+ "SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER like ? ";

					ps = con.prepareStatement(actualQuantityUpdate1);
					ps.setString(1, grnNscrnValue[0]);
					ps.setString(2, grnNscrnValue[1]);
					ps.setString(3, invoiceNumber);
					ps.setString(4, poNumber);
					ps.setString(5, poLineitem + "%");
					value = ps.executeUpdate();
					ps.close();

				}

				if (("Y").equalsIgnoreCase(status)) {
					String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";

					ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
					ps.setString(1, enduser);
					ps.setString(2, null);
					ps.setString(3, invoiceNumber);
					ps.setString(4, poNumber);
					value = ps.executeUpdate();
					ps.close();

					String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

					ps = con.prepareStatement(insertauditacceptqty);
					ps.setString(1, poNumber);
					ps.setString(2, invoiceNumber);
					ps.setString(3, email);
					ps.setString(4, "Y");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					value = ps.executeUpdate();
					ps.close();
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
				ps.setString(5, poNumber);
				ps.executeUpdate();
				ps.close();
			}
			con.commit();
			if (returnGRN != null && returnGRN != "") {
				responsejson.put("grnlist", showGrnList);
				responsejson.put("scrnlist", showScrnList);
				responsejson.put("message", "Success");
				responsejson.put("warningMessage", warningMessage == null ? "" : warningMessage);

				jsonArray.add(responsejson);
			} else {
				if ("E".equalsIgnoreCase(error)) {

				} else {
					message = warningMessage == null ? "" : warningMessage;
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

		ArrayList sendingList = new ArrayList();

		SimpleDateFormat sm = new SimpleDateFormat("yyyyMMdd");
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		Date now = new Date();
		Hashtable SAPConnectionDetails = new Hashtable();
		Hashtable SAPColumnHeads = new Hashtable();
		Hashtable SAPValues = new Hashtable();
		Hashtable SAPReturnData = new Hashtable();
		HashMap<Integer, String> grnHashMapping = new HashMap<Integer, String>();
		ArrayList SAPReturnValues = new ArrayList();
		HashMap poLineGrnValue = new HashMap();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		Properties prop = new Properties();
		int poCounter = 1;
		String showGrnList = null;
		String showScrnList = null;
		String comboPoLineitem = null;
		portalid = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			prop.load(input);
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

				if (!poLineGrnValue.containsKey(poLineitem)) { // 00010-10, 00010-20 ,00020-10
					grnHashMapping.put(poCounter, poLineitem); // {(1,"00010"),(2,"00020")}
					poLineGrnValue.put(poLineitem, ""); // 00010, 00020
					poCounter = poCounter + 1;
				}

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

				log.info("#" + poNumber + "#" + poLineitem + "-" + serviceLineitem + "#" + invoiceDate + "#"
						+ invoiceNumber + "#" + quantity + "#" + storageLocation + "#" + portalid);

				log.info(mainList.toString());
				sendingList.add(mainList);
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
			SAPValues.put("IMPORTLINEITEMLIST1", sendingList);

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
						String putGRNNSCRNValues = returnGRN + "-" + serviceLineitem;
						int key = ii + 1;
						if (grnHashMapping.containsKey(key)) { // {1,"00010"},{2,"00020"}}
							String mapValue = grnHashMapping.get(key); // 00010
							poLineGrnValue.put(mapValue, putGRNNSCRNValues);// {"00010","grn-srcn"},
						}

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
						error = arrayData[0];
						message = arrayData[2];
						log.info("arrayData[] 2 :" + arrayData.length + " : error : " + error + " : message : "
								+ message + " Invoice Number : " + invoiceNumber);

						if ("W".equalsIgnoreCase(error) || "S".equalsIgnoreCase(error) || "I".equalsIgnoreCase(error)
								|| "E".equalsIgnoreCase(error)) {
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

					String getGrnScrnVal = (String) poLineGrnValue.get(poLineitem);
					String[] grnNscrnValue = getGrnScrnVal.split("-");

					if (!showGrnList.contains(grnNscrnValue[0]) == true) {
						showGrnList = showGrnList + grnNscrnValue[0] + ",";
					}

					if (!showScrnList.contains(grnNscrnValue[1]) == true) {
						showScrnList = showScrnList + grnNscrnValue[1] + ",";
					}
					log.info("showGrnList ==> " + showGrnList.toString() + "showScrnList ==> " + showScrnList.toString()
							+ " Invoice Number : " + invoiceNumber);

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

					String actualQuantityUpdate1 = "update DELIVERYSUMMARY set GRNNUMBER = ?, "
							+ "SCRNNUMBER= ? where INVOICENUMBER = ? AND PONUMBER = ? AND LINEITEMNUMBER like ? ";

					ps = con.prepareStatement(actualQuantityUpdate1);
					ps.setString(1, grnNscrnValue[0]);
					ps.setString(2, grnNscrnValue[1]);
					ps.setString(3, invoiceNumber);
					ps.setString(4, poNumber);
					ps.setString(5, poLineitem + "%");
					value = ps.executeUpdate();
					ps.close();

				}
				if (("Y").equalsIgnoreCase(status)) {
					String updatedinvoiceapprovalwithenduser = "update INVOICEAPPROVAL set ENDUSEID=?,PROXY=? "
							+ "where INVOICENUMBER=? AND PONUMBER=?";
					ps = con.prepareStatement(updatedinvoiceapprovalwithenduser);
					ps.setString(1, enduser);
					ps.setString(2, null);
					ps.setString(3, invoiceNumber);
					ps.setString(4, poNumber);
					value = ps.executeUpdate();
					ps.close();

					String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

					ps = con.prepareStatement(insertauditacceptqty);
					ps.setString(1, poNumber);
					ps.setString(2, invoiceNumber);
					ps.setString(3, email);
					ps.setString(4, "Y");
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					value = ps.executeUpdate();
					ps.close();
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
				ps.setString(5, poNumber);
				ps.executeUpdate();
				ps.close();
			}
			if (returnGRN != null && returnGRN != "") {
				responsejson.put("grnlist", showGrnList);
				responsejson.put("scrnlist", showScrnList);
				responsejson.put("message", "Success");
				responsejson.put("warningMessage", warningMessage == null ? "" : warningMessage);
				jsonArray.add(responsejson);
			} else {
				if ("E".equalsIgnoreCase(error)) {
				} else {
					message = warningMessage == null ? "" : warningMessage;
				}
				responsejson.put("message", message);
				jsonArray.add(responsejson);
			}

		} catch (Exception e) {
			con.rollback();
			log.error("getAcceptQtynServiceGRN() :", e.fillInStackTrace());

		}
		return jsonArray;
	}

	public JSONArray getcreditadvice(String invoice, String po_num) throws SQLException {
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();

		boolean result;
		result = Validation.StringChecknull(invoice);
		if (result == false) {
			responsejson.put("validation", "invoice number is should be present");
			responsejson.put("message", "invoice number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "ponumber number is should be present");
			responsejson.put("message", "ponumber number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String po_query = "Select DELIVERYUNIQUENO,RATEPERQTY,LINEITEMNUMBER,SHORTQTY,SHORTAMOUNT,"
				+ "CREDITADVICENO,ACCEPTEDQTY,LINEITEMTOTALQUANTITY,LINEITEMTEXT from DELIVERYSUMMARY where "
				+ "INVOICENUMBER=? AND PONUMBER=? AND SHORTAMOUNT>0";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_query);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			rs = ps.executeQuery();
			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("DELIVERYUNIQUENO", rs.getString("DELIVERYUNIQUENO"));
				poData.put("RATEPERQTY", rs.getString("RATEPERQTY"));
				poData.put("LINEITEMNUMBER", rs.getString("LINEITEMNUMBER"));
				poData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
				poData.put("SHORTQTY", rs.getString("SHORTQTY"));
				poData.put("SHORTAMOUNT", rs.getString("SHORTAMOUNT"));
				poData.put("ACCEPTEDQTY", rs.getString("ACCEPTEDQTY"));
				poData.put("LINEITEMTOTALQUANTITY", rs.getString("LINEITEMTOTALQUANTITY"));
				poData.put("LINEITEMTEXT", rs.getString("LINEITEMTEXT"));
				POList.add(poData);
			}
			rs.close();
			ps.close();

		} catch (SQLException e) {
			log.error("getcreditadvice() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0) {
			responsejson.put("poData", POList);
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray shortfallCreditadvice(List<CreditAdviceDetails> shortfall) throws SQLException {

		boolean result;
		result = shortfall.isEmpty();
		if (result == true) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		for (int i = 0; i < shortfall.size(); i++) {
			result = Validation.StringChecknull(shortfall.get(i).getPonumber());

			if (result == false) {
				responsejson.put("validation", "PoNumber must be present");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(shortfall.get(i).getBid());

			if (result == false) {
				responsejson.put("validation", "Bid must be present");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			} else {
				result = Validation.numberCheck(shortfall.get(i).getBid());

				if (result == false) {
					responsejson.put("validation", "bid should be number");
					responsejson.put("message", "bid should be number");
					jsonArray.add(responsejson);
					return jsonArray;
				}
			}
			result = Validation.StringChecknull(shortfall.get(i).getInvoicenumber());

			if (result == false) {
				responsejson.put("validation", "invoice must be present");
				responsejson.put("message", "invoice number must be present");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(shortfall.get(i).getLineitemnumber());

			if (result == false) {
				responsejson.put("validation", "Lineitemnumber must be present");
				responsejson.put("message", "Lineitemnumber must be present");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(shortfall.get(i).getSortqty());

			if (result == false) {
				responsejson.put("validation", "SortQty must be present");
				responsejson.put("message", "SortQty must be present");
				jsonArray.add(responsejson);
				return jsonArray;
			}
			result = Validation.StringChecknull(shortfall.get(i).getRateperqty());

			if (result == false) {
				responsejson.put("validation", "RateQty must be present");
				responsejson.put("message", "RateQty must be present");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		String sqlIdentifier = "select CREDITADVICESUMMARY_SEQ.NEXTVAL from dual";
		String poninvoiceupdate = "update PONINVOICESUMMERY set CREDITADVICENO=? where INVOICENUMBER=? AND PONUMBER=? AND BUSINESSPARTNEROID=?";
		String creditAdviceSummary = "insert into CREDITADVICESUMMARY (CREDITADVICENO,BUSSINESSPARTNEROID,INVOICENUMBER,PONUMBER) values (?,?,?,?)";
		String poeventdetailsbalanceqty = " select balance_qty from poeventdetails where "
				+ "PONumber=? and LineItemNumber=? and BusinessPartnerOID=?";
		String shortFallQty = "UPDATE poeventdetails set balance_qty=? where BusinessPartnerOID =? "
				+ "and PONumber= ? and LineItemNumber= ? and OrderNumber is null";
		String creditadvicetotal = "update CREDITADVICESUMMARY set TOTALAMT=? where INVOICENUMBER=? AND PONUMBER=? AND BUSSINESSPARTNEROID=? AND CREDITADVICENO=?";
		String deliverySummaryUpdate = "UPDATE DELIVERYSUMMARY set CREDITADVICENO=?,SHORTQTY=?,SHORTAMOUNT=? where BUSSINESSPARTNEROID =? "
				+ "and PONumber= ? and LineItemNumber= ? and INVOICENUMBER =?";
		long myId = 0;
		int value = 0;
		int value1 = 0;
		int value2 = 0;
		int value3 = 0;
		int value4 = 0;
		double actualbalanceQty = 0.0;
		double shortTotalAmount = 0.0;
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

			for (int k = 0; k < shortfall.size(); k++) {
				ps = con.prepareStatement(poeventdetailsbalanceqty);
				ps.setString(1, shortfall.get(k).getPonumber());
				ps.setString(2, shortfall.get(k).getLineitemnumber());
				ps.setString(3, shortfall.get(k).getBid());
				rs = ps.executeQuery();
				while (rs.next()) {
					actualbalanceQty = Double.parseDouble(rs.getString("balance_qty"));
				}
				rs.close();
				ps.close();

				ps = con.prepareStatement(shortFallQty);
				ps.setDouble(1, actualbalanceQty + Double.parseDouble(shortfall.get(k).getSortqty()));
				ps.setString(2, shortfall.get(k).getBid());
				ps.setString(3, shortfall.get(k).getPonumber());
				ps.setString(4, shortfall.get(k).getLineitemnumber());
				ps.executeUpdate();
				ps.close();
			}

			ps = con.prepareStatement(creditAdviceSummary);
			ps.setLong(1, myId);
			ps.setString(2, shortfall.get(0).getBid());
			ps.setString(3, shortfall.get(0).getInvoicenumber());
			ps.setString(4, shortfall.get(0).getPonumber());
			value = ps.executeUpdate();
			ps.close();
			if (value > 0) {
				ps = con.prepareStatement(poninvoiceupdate);
				ps.setLong(1, myId);
				ps.setString(2, shortfall.get(0).getInvoicenumber());
				ps.setString(3, shortfall.get(0).getPonumber());
				ps.setString(4, shortfall.get(0).getBid());
				value2 = ps.executeUpdate();
				ps.close();
			}

			for (int z = 0; z < shortfall.size(); z++) {
				ps = con.prepareStatement(deliverySummaryUpdate);

				log.info("shortfall.size()" + shortfall.size() + "myId +" + myId + "shortfall.get(z).getSortqty() "
						+ shortfall.get(z).getSortqty());
				log.info((Double.parseDouble(shortfall.get(z).getSortqty())
						* Double.parseDouble(shortfall.get(z).getRateperqty())));
				ps.setLong(1, myId);
				ps.setString(2, shortfall.get(z).getSortqty());
				ps.setDouble(3, Double.parseDouble(shortfall.get(z).getSortqty())
						* Double.parseDouble(shortfall.get(z).getRateperqty()));
				ps.setString(4, shortfall.get(z).getBid());
				ps.setString(5, shortfall.get(z).getPonumber());
				ps.setString(6, shortfall.get(z).getLineitemnumber());
				ps.setString(7, shortfall.get(z).getInvoicenumber());
				shortTotalAmount = shortTotalAmount + (Double.parseDouble(shortfall.get(z).getSortqty())
						* Double.parseDouble(shortfall.get(z).getRateperqty()));
				value4 = ps.executeUpdate();
				ps.close();
			} // for Loop
			if (value4 > 0) {
				ps = con.prepareStatement(creditadvicetotal);
				ps.setDouble(1, shortTotalAmount);
				ps.setString(2, shortfall.get(0).getInvoicenumber());
				ps.setString(3, shortfall.get(0).getPonumber());
				ps.setString(4, shortfall.get(0).getBid());
				ps.setLong(5, myId);
				value = ps.executeUpdate();
				ps.close();
			}
			con.commit();
			if (value > 0) {
				responsejson.put("message", "shortfall Creditadvice created Sucess");
			} else {
				responsejson.put("message", "shortfall Creditadvice created Fail");
			}
		} catch (Exception e) {
			log.error("shortfallCreditadvice() :", e.fillInStackTrace());

			con.rollback();
			responsejson.put("message", e.getLocalizedMessage());
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getcreditNoteDetails(String invoice, String po_num, String creditadviceno) throws SQLException {
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> poData = new HashMap<String, String>();

		boolean result;
		result = Validation.StringChecknull(invoice);
		if (result == false) {
			responsejson.put("validation", "invoice number is should be present");
			responsejson.put("message", "invoice number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "ponumber number is should be present");
			responsejson.put("message", "ponumber number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(creditadviceno);
		if (result == false) {
			responsejson.put("validation", "creditadviceno number is should be present");
			responsejson.put("message", "creditadviceno number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String po_query = "Select * from CREDITNOTE where INVOICENUMBER=? AND PONUMBER=? AND CREDITADVICENO=?";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_query);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			ps.setString(3, creditadviceno);
			rs = ps.executeQuery();
			while (rs.next()) {
				poData.put("BUSSINESSPARTNEROID", rs.getString("BUSSINESSPARTNEROID"));
				poData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
				poData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
				poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				poData.put("PONUMBER", rs.getString("PONUMBER"));
				poData.put("AMOUNT", rs.getString("AMOUNT"));
				poData.put("TAX", rs.getString("TAX"));
				poData.put("TOTALAMT", rs.getString("TOTALAMT"));
				poData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
				poData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
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
			log.error("getcreditNoteDetails() :", e.fillInStackTrace());

			responsejson.put("message", e.getLocalizedMessage());
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	private int getUniqueNote(String po_num, String invoice, String creditadviceno) throws SQLException {

		String uniquePoInNoteCount = "Select count(*) as counter from CREDITNOTE where INVOICENUMBER=? AND PONUMBER=? AND CREDITADVICENO=?";

		int count = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {

			con = DBConnection.getConnection();
			ps = con.prepareStatement(uniquePoInNoteCount);
			ps.setString(1, invoice);
			ps.setString(2, po_num);
			ps.setString(3, creditadviceno);
			rs = ps.executeQuery();
			while (rs.next()) {
				count = rs.getInt("counter");
			}
			rs.close();
			ps.close();
			responsejson.put("count", count);
			responsejson.put("message", "Success");

		} catch (Exception e) {
			log.error("getUniqueNote() :", e.fillInStackTrace());

			count = 0;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return count;
	}

	public JSONArray addcreditNote(String invoice, String po_num, String bid, String creditadviceno, String amount,
			String tax, String totalamt, String actualfilename, String savedfilename) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(invoice);
		if (result == false) {
			responsejson.put("validation", "invoice number is should be present");
			responsejson.put("message", "invoice number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(po_num);
		if (result == false) {
			responsejson.put("validation", "ponumber number is should be present");
			responsejson.put("message", "ponumber number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(creditadviceno);
		if (result == false) {
			responsejson.put("validation", "creditadviceno number is should be present");
			responsejson.put("message", "creditadviceno number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(amount);
		if (result == false) {
			responsejson.put("validation", "amount number is should be present");
			responsejson.put("message", "amount number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		result = Validation.StringChecknull(actualfilename);
		if (result == false) {
			responsejson.put("validation", "actualfilename number is should be present");
			responsejson.put("message", "actualfilename number is should be present");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		int poincount = getUniqueNote(po_num, invoice, creditadviceno);

		if (poincount > 0) {
			responsejson.put("validation", "Duplicate po_num,invoice,creditadviceno number ");
			responsejson.put("message", "Duplicate po_num,invoice,creditadviceno number");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		String creditNoteAssign = "insert into CREDITNOTE (BUSSINESSPARTNEROID,CREDITADVICENO,CREDITNOTENO,INVOICENUMBER,PONUMBER,AMOUNT,TAX,TOTALAMT,ACTUALFILENAME,SAVEDFILENAME) values (?,?,?,?,?,?,?,?,?,?)";
		String sqlIdentifier = "select CREDITNOTE_SEQ.NEXTVAL from dual";
		String poninvoiceupdate = "update PONINVOICESUMMERY set CREDITNOTENO=?,MODIFIEDON=? where INVOICENUMBER=? AND PONUMBER=? AND BUSINESSPARTNEROID=? AND CREDITADVICENO=?";
		long myId = 0;
		int value = 0;
		int value1 = 0;
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
			try {
				ps = con.prepareStatement(poninvoiceupdate);
				ps.setString(1, String.valueOf(myId));
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, invoice);
				ps.setString(4, po_num);
				ps.setString(5, bid);
				ps.setString(6, creditadviceno);
				value1 = ps.executeUpdate();
				ps.close();
			} catch (Exception e) {
				log.error("addcreditNote() 1 :", e.fillInStackTrace());

			}
			ps = con.prepareStatement(creditNoteAssign);
			ps.setString(1, bid);
			ps.setString(2, creditadviceno);
			ps.setLong(3, myId);
			ps.setString(4, invoice);
			ps.setString(5, po_num);
			ps.setString(6, amount);
			ps.setString(7, tax);
			ps.setString(8, totalamt);
			ps.setString(9, actualfilename);
			ps.setString(10, savedfilename);
			value = ps.executeUpdate();
			ps.close();
			con.commit();
			if (value > 0) {
				responsejson.put("message", "CreditNote created Sucess");
			} else {
				responsejson.put("message", "CreditNote created Fail");
			}
		} catch (Exception e) {
			log.error("addcreditNote() 2 :", e.fillInStackTrace());

			responsejson.put("message", e.getLocalizedMessage());
			con.rollback();
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getportalid(String useremail) {

		String gettimescapeid = "select portalid from FINDEMPLOYEE  where emailid = ?";
		String portalid = "";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(gettimescapeid);
			ps.setString(1, useremail);
			rs = ps.executeQuery();

			if (rs.next()) {
				portalid = rs.getString("portalid");
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getportalid() : ", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);

		}

		if ("".equalsIgnoreCase(portalid) || portalid == null) {
			portalid = "";
			responsejson.put("message", "Portal id not found");
		} else {
			responsejson.put("message", "Success");
		}

		responsejson.put("portalid", portalid);
		jsonArray.add(responsejson);
		responsejson.put("message", "Success");
		return jsonArray;

	}

	public JSONArray getportalidfordev(String useremail) {

		responsejson.put("portalid", "dsubra1007");
		responsejson.put("message", "Success");
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getEnderUserReturn(List<EndUserReturn> enduserList, String email) throws SQLException {

		String shortFallQty = "UPDATE poeventdetails set balance_qty=? where BusinessPartnerOID =? and PONumber= ? and LineItemNumber= ? and OrderNumber is null";
		String overAllStatus = "UPDATE PONINVOICESUMMERY set OVERALLSTATUS=?,MODIFIEDON=? where INVOICENUMBER =? and PONUMBER=? ";
		String poeventdetailsbalanceqty = " select balance_qty from poeventdetails where "
				+ "PONumber=? and LineItemNumber=? and BusinessPartnerOID=?";
		String insertintotracker = "insert into INVOICETRACKER (INVOICENUMBER,PONUMBER,BUSSINESSPARTNEROID,STATUS,"
				+ "MODIFIEDTIME,MODIFIEDBY)" + " values(?,?,?,?,?,?)";
		String deleteresubmitstatus = "DELETE FROM INVOICETRACKER WHERE INVOICENUMBER =? and PONUMBER=? and STATUS=?";

		String checkInvStatus = "SELECT OVERALLSTATUS FROM PONINVOICESUMMERY where INVOICENUMBER =? and PONUMBER=? ";

		String insertauditacceptqty = "insert into AUDIT_ACCEPTQTY (PONUMBER,INVOICENUMBER,USEREMAILID,FLAG,CREATEDON) values (?,?,?,?,?)";

		double actualbalanceQty = 0.0;
		int value = 0;
		String status = null;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);

			ps = con.prepareStatement(checkInvStatus);
			ps.setString(1, enduserList.get(0).getInvoicenumber());
			ps.setString(2, enduserList.get(0).getPonumber());
			rs = ps.executeQuery();
			if (rs.next()) {
				status = rs.getString("OVERALLSTATUS");
			}
			rs.close();
			ps.close();

			if ("V".equalsIgnoreCase(status)) {
				throw new DXPortalException("Invoice has been already returned !!", "Invoice already returned");

			}
			executeUpdateBalance(con, enduserList);

			ps = con.prepareStatement(overAllStatus);
			ps.setString(1, "V");
			ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(3, enduserList.get(0).getInvoicenumber());
			ps.setString(4, enduserList.get(0).getPonumber());
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(insertintotracker);
			ps.setString(1, enduserList.get(0).getInvoicenumber());
			ps.setString(2, enduserList.get(0).getPonumber());
			ps.setString(3, enduserList.get(0).getBid());
			ps.setString(4, "V");
			ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.setString(6, email);
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(deleteresubmitstatus);
			ps.setString(1, enduserList.get(0).getInvoicenumber());
			ps.setString(2, enduserList.get(0).getPonumber());
			ps.setString(3, "S");
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement(insertauditacceptqty);
			ps.setString(1, enduserList.get(0).getPonumber());
			ps.setString(2, enduserList.get(0).getInvoicenumber());
			ps.setString(3, email);
			ps.setString(4, enduserList.get(0).getStatus());
			ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
			value = ps.executeUpdate();
			ps.close();

			con.commit();
			responsejson.put("message", "Invoice returned successfully");
			jsonArray.add(responsejson);
		} catch (DXPortalException dxp) {
			log.error("getEnderUserReturn() 1 :", dxp.fillInStackTrace());
			log.error("Get Cause", dxp.getCause());
			responsejson.put("message", dxp.reason);
			log.info(dxp.reason + " - " + dxp.subReason);
			jsonArray.add(responsejson);
			con.rollback();
		} catch (Exception e) {
			log.error("getEnderUserReturn() 2 :", e.fillInStackTrace());

			responsejson.put("message", e.getLocalizedMessage());
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public int getInvoiceBankvendorIdDetails(String vendorId) throws SQLException {

		String businessId = "select BusinessPartnerOID from businesspartner where VendorID= ?";
		int Bid = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(businessId);
			ps.setString(1, vendorId);
			rs = ps.executeQuery();
			while (rs.next()) {
				Bid = rs.getInt("BusinessPartnerOID");
			}

		} catch (Exception e) {
			log.error("getInvoiceBankvendorIdDetails() :", e.fillInStackTrace());

			return 0;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return Bid;
	}

	public String getemailidbasedonmaterial(String material, String plant) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		String qdata = "Select USERID from INVENTORYUSERLIST where MTYP=? AND PLANT=?";
		StringBuffer sb = new StringBuffer();
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(qdata);
			ps.setString(1, material);
			ps.setString(2, plant);
			rs = ps.executeQuery();
			List<String> invoiceids = new ArrayList<String>();
			while (rs.next()) {
				sb.append(rs.getString("USERID"));
				sb.append("_");
			}
			rs.close();
			ps.close();
			if (sb != null && !(sb.length() == 0)) {
				return sb.toString().substring(0, sb.toString().length() - 1);
			}

		} catch (Exception e) {
			log.error("getemailidbasedonmaterial() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return sb.toString();
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
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
							+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
							+ "AND (CREDITADVICENO IS NOT NULL) " + "UNION"
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(CREDITADVICENO IS NOT NULL AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
							+ "UNION" + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND"
							+ " (CREDITADVICENO IS NOT NULL AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

					storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND CREDITADVICENO IS NOT NULL "
							+ " UNION" + " SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
							+ " CREDITADVICENO IS NOT NULL " + "UNION "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' AND "
							+ "CREDITADVICENO IS NOT NULL " + "UNION" + " SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND CREDITADVICENO IS NOT NULL) JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "ORDER BY CREATEDON DESC) c )";
				} else if ("ALL".equalsIgnoreCase(status)) {

					sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
							+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
							+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
							+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
							+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
							+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "( A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)" + " UNION "
							+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
							+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND"
							+ " (A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

					storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
							+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
							+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
							+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
							+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
							+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
							+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
							+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
							+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + " UNION"
							+ " SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL  "
							+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
							+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
							+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
							+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
							+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' " + "UNION"
							+ " SELECT DISTINCT B.PONUMBER,"
							+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
							+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
							+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
							+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
							+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "ORDER BY CREATEDON DESC) c )";

				} else {
					if ("P".equalsIgnoreCase(status)) {
						sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE, "
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER, "
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS, "
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS, "
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER, "
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO, "
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? OR B.OVERALLSTATUS=?) " + " UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
								+ "(B1.OVERALLSTATUS=?  OR B1.OVERALLSTATUS=? AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
								+ "(B1.OVERALLSTATUS=?  OR B1.OVERALLSTATUS=? AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

						storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ " A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=? OR B.OVERALLSTATUS=? )"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
								+ "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=?) " + "UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  AND "
								+ "(B1.OVERALLSTATUS=? OR B1.OVERALLSTATUS=?)" + " UNION "
								+ "SELECT DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' AND (B.OVERALLSTATUS IN (?,?)) JOIN "
								+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "ORDER BY CREATEDON DESC) c )";
					} else {
						sql = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE, "
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER, "
								+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS, "
								+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS, "
								+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER, "
								+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO, "
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
								+ "AND (B.OVERALLSTATUS=? ) " + " UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
								+ "(B1.OVERALLSTATUS=?  AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL)"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,B1.TAXAMOUNT,"
								+ "B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
								+ "(B1.OVERALLSTATUS=?  AND A1.STATUS LIKE 'C%' ) ORDER BY CREATEDON DESC) c )";

						storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
								+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
								+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
								+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
								+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
								+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
								+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
								+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
								+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
								+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ " A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL AND (B.OVERALLSTATUS=?  )"
								+ " UNION " + "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL AND "
								+ "(B1.OVERALLSTATUS=? ) " + "UNION "
								+ "SELECT  B1.PONUMBER,B1.INVOICENUMBER,B1.INVOICEDATE,"
								+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B1.OVERALLSTATUS,B1.amount,A1.proxy,"
								+ "B1.ACTUALFILENAME,B1.SAVEDFILENAME,B1.GRNNUMBER,B1.SCRNNUMBER,B1.BUSINESSPARTNEROID,B1.MATERIAL_TYPE,B1.PLANT,B1.VENDORID,"
								+ "B1.CREDITNOTENO,B1.CREDITADVICENO,B1.TOTALAMTINCTAXES,"
								+ "B1.TAXAMOUNT,B1.CREATEDON,B1.PAYMENTAMOUNT,B1.BUSINESSPARTNERTEXT,B1.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  AND "
								+ "(B1.OVERALLSTATUS=? )" + " UNION " + "SELECT DISTINCT B.PONUMBER,"
								+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
								+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
								+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
								+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
								+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
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

				while (rs.next()) {
					invoicedata.add(rs.getString("INVOICENUMBER"));
					podata.add(rs.getString("PONUMBER"));
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
				String subquery = "";
				ArrayList<String> param = new ArrayList<String>();
				if ("true".equalsIgnoreCase(storeKepeer)) {

					param.add(emailId);

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
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,"
						+ "A.ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,"
						+ "B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,"
						+ "B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
						+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? " + " "
						+ subquery + " " + " UNION " + "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ "B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL "
						+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ? )  AND"
						+ "(A1.STATUS NOT LIKE 'C%' AND B.GRNNUMBER IS NOT NULL " + subquery + ") " + "UNION "
						+ "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,"
						+ " B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL "
						+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ? ) AND " + "(A1.STATUS LIKE 'C%' " + subquery
						+ ") ORDER BY CREATEDON DESC) c )";

				storeKepeerQuery = "SELECT * FROM ( SELECT PONUMBER,INVOICENUMBER,INVOICEDATE,ENDUSEID,STATUS,ENDUSERSTATUS,"
						+ "ACTIONBY,EUMANAGER,STAGE,OVERALLSTATUS,amount,proxy,ACTUALFILENAME,"
						+ "SAVEDFILENAME,GRNNUMBER,SCRNNUMBER,BUSINESSPARTNEROID,MATERIAL_TYPE,"
						+ "PLANT,VENDORID,CREDITNOTENO,CREDITADVICENO,TOTALAMTINCTAXES,TAXAMOUNT,CREATEDON,"
						+ "PAYMENTAMOUNT,BUSINESSPARTNERTEXT,EXPENSESHEETID, ROWNUM rnum FROM (SELECT DISTINCT B.PONUMBER,B.INVOICENUMBER,"
						+ "B.INVOICEDATE,A.ENDUSEID,B.OVERALLSTATUS AS STATUS,A.ENDUSERSTATUS,"
						+ "A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,"
						+ "B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,"
						+ "B.CREDITADVICENO,B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
						+ "A.InvoiceNumber=B.InvoiceNumber AND "
						+ "A.PONumber=B.PONumber   AND A.ENDUSEID = ? AND A.PROXY IS NULL  " + subquery + " " + "UNION "
						+ "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,"
						+ "B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' "
						+ "AND B.GRNNUMBER IS NOT NULL  " + subquery + " " + " UNION "
						+ "SELECT  B.PONUMBER,B.INVOICENUMBER,B.INVOICEDATE,"
						+ "A1.ENDUSEID,A1.STATUS,A1.ENDUSERSTATUS,A1.ACTIONBY,A1.EUMANAGER,A1.STAGE,B.OVERALLSTATUS,B.amount,A1.proxy,"
						+ "B.ACTUALFILENAME,B.SAVEDFILENAME,B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,"
						+ "B.CREDITNOTENO,B.CREDITADVICENO,B.TOTALAMTINCTAXES,"
						+ "B.TAXAMOUNT,B.CREATEDON,B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
						+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ?) " + "AND A1.STATUS LIKE 'C%'  " + subquery
						+ " " + "UNION " + "SELECT DISTINCT B.PONUMBER,"
						+ "B.INVOICENUMBER,B.INVOICEDATE,('') AS ENDUSEID,(B.OVERALLSTATUS) AS "
						+ "STATUS,('P') AS ENDUSERSTATUS,A.ACTIONBY,('') AS EUMANAGER,A.STAGE,B.OVERALLSTATUS,B.amount,A.proxy,B.ACTUALFILENAME,B.SAVEDFILENAME,"
						+ "B.GRNNUMBER,B.SCRNNUMBER,B.BUSINESSPARTNEROID,B.MATERIAL_TYPE,B.PLANT,B.VENDORID,B.CREDITNOTENO,B.CREDITADVICENO,"
						+ "B.TOTALAMTINCTAXES,B.TAXAMOUNT,B.CREATEDON,"
						+ "B.PAYMENTAMOUNT,B.BUSINESSPARTNERTEXT,B.EXPENSESHEETID FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
						+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' " + subquery + " JOIN "
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
				while (rs.next()) {
					invoicedata.add(rs.getString("INVOICENUMBER"));
					podata.add(rs.getString("PONUMBER"));
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

	public JSONArray updatesupportingfiles(String po, String invo, List<String> filenames, String sampletimestamp)
			throws SQLException {

		String invoice_data1 = "select ENDUSERSUPPACTUALFILE,ENDUSERSUPPSAVEDFILE from PONINVOICESUMMERY where INVOICENUMBER =? and PONUMBER =?";
		String invoice_data2 = "Update PONINVOICESUMMERY set  ENDUSERSUPPACTUALFILE =? , ENDUSERSUPPSAVEDFILE=? where INVOICENUMBER =? and PONUMBER =?";
		ArrayList<HashMap<String, String>> InvoiceQueryList = new ArrayList<HashMap<String, String>>();
		String enduseractualfilename = "";
		String endusersavedfilename = "";
		int cound = 0;
		int add = 0;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(invoice_data1);
			ps.setString(1, invo);
			ps.setString(2, po);
			rs = ps.executeQuery();
			while (rs.next()) {
				enduseractualfilename = rs.getString("ENDUSERSUPPACTUALFILE");
				endusersavedfilename = rs.getString("ENDUSERSUPPSAVEDFILE");
			}
			rs.close();
			ps.close();

			StringBuffer sb = new StringBuffer();
			StringBuffer sb1 = new StringBuffer();
			String addsuppenduseractualfilename = "";
			String addsuppendusersavedfilename = "";
			if (!(enduseractualfilename == null)) {
				String suppsaved[] = endusersavedfilename.split(",");
				String supp = suppsaved[0];
				String supp1[] = supp.split("_");
				String ponumber = supp1[0];
				String type = supp1[1];
				String timestamp = supp1[supp1.length - 1];
				int iend = timestamp.indexOf(".");
				if (iend != -1) {
					timestamp = timestamp.substring(0, iend);
				}

				for (int k = 0; k < filenames.size(); k++) {
					sb.append(filenames.get(k));
					sb.append(",");
					String name = "";
					int iend1 = filenames.get(k).indexOf(".");
					if (iend1 != -1) {
						name = filenames.get(k).substring(0, iend1);
					}
					String extension = filenames.get(k).substring(filenames.get(k).lastIndexOf(".") + 1);
					String savedfilename = ponumber + "_" + type + "_" + name + "_" + timestamp + "." + extension;
					sb1.append(savedfilename);
					sb1.append(",");
				}
				sb1.append(endusersavedfilename);
				sb.append(enduseractualfilename);
				addsuppenduseractualfilename = sb.toString();
				addsuppendusersavedfilename = sb1.toString();
			} else {
				for (int k = 0; k < filenames.size(); k++) {
					sb.append(filenames.get(k));
					sb.append(",");
					String name = "";
					int iend1 = filenames.get(k).indexOf(".");
					if (iend1 != -1) {
						name = filenames.get(k).substring(0, iend1);
					}
					String extension = filenames.get(k).substring(filenames.get(k).lastIndexOf(".") + 1);
					String savedfilename = po + "_enduserinvoice_" + name + "_" + sampletimestamp + "." + extension;
					sb1.append(savedfilename);
					sb1.append(",");
				}
				addsuppendusersavedfilename = sb1.substring(0, sb1.length() - 1);
				addsuppenduseractualfilename = sb.substring(0, sb.length() - 1);
			}
			ps = con.prepareStatement(invoice_data2);
			ps.setString(1, addsuppenduseractualfilename);
			ps.setString(2, addsuppendusersavedfilename);
			ps.setString(3, invo);
			ps.setString(4, po);
			cound = ps.executeUpdate();
			ps.close();

		} catch (Exception e) {
			log.error("updatesupportingfiles() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (cound > 0) {
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		}
		return jsonArray;
	}

	public JSONArray invoiceinternaldashboardsearch(String invoicenumber, String ponumber, String status,
			String fromdurationdate, String todurationdate, String plant, String purchasegroup,
			String requisitioneremailid, String frominvamount, String toinvamount, String ageingfrom, String ageingto,
			HttpSession session, String emailId, int nPage, String vendor) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String storeKepeer = (String) session.getAttribute("shopkepeer");
		String sql = "";
		ArrayList<String> param = new ArrayList<String>();
		String subquery = "";
		int pages = 0;
		ArrayList<HashMap<String, String>> POListEvent = new ArrayList<HashMap<String, String>>();
		if (!status.equalsIgnoreCase("NA")) {
			if ("C".equalsIgnoreCase(status)) {
				String po = " AND PS.CREDITADVICENO IS NOT NULL";
				subquery = subquery + po;
			} else if ("ALL".equalsIgnoreCase(status)) {
				String po = " ";
				subquery = subquery + po;
			} else if ("P".equalsIgnoreCase(status)) {
				String po = " AND (PS.OVERALLSTATUS=? OR  PS.OVERALLSTATUS=?)";
				subquery = subquery + po;
				param.add("P");
				param.add("M");
			} else {
				String po = " AND PS.OVERALLSTATUS=?";
				subquery = subquery + po;
				param.add(status);
			}
		}
		if (!ponumber.equalsIgnoreCase("NA")) {
			String po = " AND PS.PONUMBER=?";
			subquery = subquery + po;
			param.add(ponumber);
		}
		if (!vendor.equalsIgnoreCase("NA")) {
			String po = " AND PS.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
			subquery = subquery + po;
			param.add(vendor);
		}
		if (!invoicenumber.equalsIgnoreCase("NA")) {
			String in = " AND PS.INVOICENUMBER=?";
			subquery = subquery + in;
			param.add(invoicenumber);
		}
		if ((!fromdurationdate.equalsIgnoreCase("NA")) && (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
			String dt = " AND PS.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
			subquery = subquery + dt;
			param.add(fromdurationdate);
			param.add(todurationdate);
		}
		if (!plant.equalsIgnoreCase("NA")) {
			String pt = " AND PS.PLANT=?";
			subquery = subquery + pt;
			param.add(plant);
		}
		if (!purchasegroup.equalsIgnoreCase("NA")) {
			String pg = " AND PS.MATERIAL_TYPE=?";
			subquery = subquery + pg;
			param.add(purchasegroup);
		}
		if (!requisitioneremailid.equalsIgnoreCase("NA")) {
			String re = " AND PS.REQUSITIONER=LOWER(?) ";
			subquery = subquery + re;
			param.add(requisitioneremailid);
		}
		if (!frominvamount.equalsIgnoreCase("NA")) {
			String ia = "  AND PS.AMOUNT BETWEEN ? AND ? ";
			subquery = subquery + ia;
			param.add(frominvamount);
			param.add(toinvamount);
		}
		if (!ageingfrom.equalsIgnoreCase("NA")) {
			String ia = "";
			if (!status.equalsIgnoreCase("NA")) {
				param.add(ageingfrom);
				if ("P".equalsIgnoreCase(status)) {
					ia = "  AND sysdate - TO_DATE(PS.MODIFIEDON, 'dd-mm-yy') > ?";
					if (!ageingto.equalsIgnoreCase("NA")) {
						ia = ia + "  AND sysdate - TO_DATE(PS.MODIFIEDON, 'dd-mm-yy') < ?";
						param.add(ageingto);
					}
				} else {
					ia = "  AND sysdate - TO_DATE(PS.MODIFIEDON, 'dd-mm-yy') > ?";
					if (!ageingto.equalsIgnoreCase("NA")) {
						ia = ia + "  AND sysdate - TO_DATE(PS.MODIFIEDON, 'dd-mm-yy') < ?";
						param.add(ageingto);
					}
				}
			}
			subquery = subquery + ia;
		}
		if (!status.equalsIgnoreCase("NA")) {
			if ("C".equalsIgnoreCase(status)) {
			} else if ("ALL".equalsIgnoreCase(status)) {
			} else if ("P".equalsIgnoreCase(status)) {
				param.add("P");
				param.add("M");
			} else {
				param.add(status);
			}
		}
		if (!ponumber.equalsIgnoreCase("NA")) {
			param.add(ponumber);
		}
		if (!vendor.equalsIgnoreCase("NA")) {
			param.add(vendor);
		}
		if (!invoicenumber.equalsIgnoreCase("NA")) {
			param.add(invoicenumber);
		}
		if ((!fromdurationdate.equalsIgnoreCase("NA")) && (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
			param.add(fromdurationdate);
			param.add(todurationdate);
		}
		if (!plant.equalsIgnoreCase("NA")) {
			param.add(plant);
		}
		if (!purchasegroup.equalsIgnoreCase("NA")) {
			param.add(purchasegroup);
		}
		if (!requisitioneremailid.equalsIgnoreCase("NA")) {
			param.add(requisitioneremailid);
		}
		if (!frominvamount.equalsIgnoreCase("NA")) {
			param.add(frominvamount);
			param.add(toinvamount);
		}
		if (!ageingfrom.equalsIgnoreCase("NA")) {
			String ia = "";
			if (!status.equalsIgnoreCase("NA")) {
				param.add(ageingfrom);
				if ("P".equalsIgnoreCase(status)) {
					if (!ageingto.equalsIgnoreCase("NA")) {
						param.add(ageingto);
					}
				} else {
					if (!ageingto.equalsIgnoreCase("NA")) {
						param.add(ageingto);
					}
				}
			}
			subquery = subquery + ia;
		}

		try {
			con = DBConnection.getConnection();
			sql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
					+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
					+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
					+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
					+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
					+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.MODIFIEDON "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null " + subquery
					+ "  AND ps.MPO IS NULL " + "UNION "
					+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
					+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
					+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
					+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
					+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
					+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO,ps.MODIFIEDON "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber " + "where ps.invoicenumber is not null " + subquery
					+ " AND ps.MPO = 'Y' " + " order by CREATEDON desc";

			log.info("sql--"+sql);
			
			Pagination pg = null;
			pg = new Pagination(sql, nPage);
			pages = pg.getPages(con, param);
			rs = pg.execute(con, param);
			String invNumber = null;
			String invDate = null;
			String mPO = null;
			String bpid = null;
			while (rs.next()) {
				HashMap<String, String> poData = new HashMap<String, String>();
				poData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				poData.put("PONUMBER", rs.getString("PONUMBER"));
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
				String dateqw = rs.getString("MODIFIEDON");
				DateFormat formatter = new SimpleDateFormat();
				SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
				Date date;
				date = inputFormat.parse(dateqw);
				SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
				poData.put("MODIFIEDON", outputFormat.format(date).toString());
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
				POListEvent.add(poData);
			}
			pg.close();
			rs.close();
			pg = null;
			if (POListEvent.size() > 0) {
				responsejson.put("invoicedetails", POListEvent);
				responsejson.put("invoicedetailsrecords", pages);
				responsejson.put("message", "Success");
			} else {
				responsejson.put("message", "No Data Found");
			}

			try {
				getinvoicepofordashboardCountAsPerStatus(invoicenumber, ponumber, status, fromdurationdate,
						todurationdate, plant, purchasegroup, requisitioneremailid, frominvamount, toinvamount,
						ageingfrom, ageingto, emailId, vendor, con, ps, rs, "invoice");
			} catch (Exception e) {
				log.error("invoiceinternaldashboardsearch() 1 :", e.fillInStackTrace());

			}

		} catch (Exception e) {
			log.error("invoiceinternaldashboardsearch() 2 :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		jsonArray.add(responsejson);
		return jsonArray;

	}

	public JSONArray pointernaldashboardsearch(String ponumber, String status, String fromdurationdate,
			String todurationdate, String plant, String purchasegroup, String requisitioneremailid, String frompoamount,
			String topoamount, String ageingfrom, String ageingto, HttpSession session, String email, int nPage,
			String mode, String vendor) {

		boolean twocondition = true;
		boolean nodata = false;
		String po_data = "";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		Pagination pg = null;
		HashMap<String, String> QueryList = new HashMap<String, String>();
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		ArrayList<String> topic = new ArrayList<String>();
		String po_query = "Select * from CHATMESSAGE where CreatedOn in(select max(CreatedOn) from CHATMESSAGE where PONUMBER=?)"
				+ " having  status =? and SENDER=?";
		String po_Number = "Select PONUMBER from CHATMESSAGE where SENDER=? and status =? and INVOICENUMBER is null ";

		String queryLists[] = { "payment amount is incorrect", "payment amount is incorrect",
				"payment amount is incorrect", "payment amount is incorrect" };
		try {

			con = DBConnection.getConnection();
			try {

				ps = con.prepareStatement(po_Number);
				ps.setString(1, email);
				ps.setString(2, "A");
				rs = ps.executeQuery();
				while (rs.next()) {
					topic.add(rs.getString("PONUMBER"));
				}
				rs.close();
				ps.close();
			} catch (Exception e) {
				topic.add("0");
			}

			for (int i = 0; i < topic.size(); i++) {
				try {
					ps = con.prepareStatement(po_query);
					ps.setString(1, topic.get(i));
					rs = ps.executeQuery();
					while (rs.next()) {
						QueryList.put(rs.getString("PONUMBER"), rs.getString("MessageText"));
					}
					rs.close();
					ps.close();
				} catch (Exception e) {
					QueryList.put("PONUMBER", "No data Found");
				}
			}

			String subquery = "";
			String podata1 = "";
			ArrayList<String> param = new ArrayList<String>();

			if (!vendor.equalsIgnoreCase("NA")) {
				String po = " AND pd.VENDORID=?";
				subquery = subquery + po;
				param.add(vendor);

			}
			if (!ponumber.equalsIgnoreCase("NA")) {
				String po = " AND pd.PONUMBER=?";
				subquery = subquery + po;
				param.add(ponumber);

			}
			if ((!fromdurationdate.equalsIgnoreCase("NA")) && (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
				String in = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + " AND TO_DATE(?, 'DD/MM/YYYY')";
				subquery = subquery + in;
				param.add(fromdurationdate);
				param.add(todurationdate);
			}
			if (!status.equalsIgnoreCase("NA") && !status.equalsIgnoreCase("ALL")) {
				String dt = " AND pd.STATUS=?";
				subquery = subquery + dt;
				param.add(status);
			}
			if (!plant.equalsIgnoreCase("NA")) {
				String dt = " AND poe.PLANT =?";
				subquery = subquery + dt;
				param.add(plant);
			}
			if (!purchasegroup.equalsIgnoreCase("NA")) {
				String dt = " AND poe.MATERIAL_TYPE=?";
				subquery = subquery + dt;
				param.add(purchasegroup);
			}
			if (!requisitioneremailid.equalsIgnoreCase("NA")) {
				String dt = " AND pd.REQUSITIONER = LOWER(?)";
				subquery = subquery + dt;
				param.add(requisitioneremailid);
			}
			if (!frompoamount.equalsIgnoreCase("NA")) {
				String ia = "  AND pd.POAMOUNT BETWEEN ? AND ? ";
				subquery = subquery + ia;
				param.add(frompoamount);
				param.add(topoamount);
			}
			if (!ageingfrom.equalsIgnoreCase("NA")) {
				param.add(ageingfrom);
				String ia = "  AND sysdate - TO_DATE(pd.MODIFIEDON, 'dd-mm-yy') > ?";
				if (!ageingto.equalsIgnoreCase("NA")) {
					ia = ia + "  AND sysdate - TO_DATE(pd.MODIFIEDON, 'dd-mm-yy') < ?";
					param.add(ageingto);
				}
				subquery = subquery + ia;
			}

			if (!mode.equalsIgnoreCase("payer")) {
				param.add(email);
			}

			if (mode.equalsIgnoreCase("payer")) {
				twocondition = false;

				podata1 = "select * from ("
						+ "select distinct pd.PONUMBER,pd.PODATE,pd.COMPANY,pd.DEPARTMENT,pd.COSTCENTRE,pd.CATEGORY,pd.BUSINESSPARTNEROID,"
						+ "pd.BUSINESSPARTNERTEXT,pd.VENDORID,pd.CREATEDDATE,pd.STARTDATE,pd.ENDDATE,pd.QUANTITY,pd.LINEITEMNUMBER,pd.LINEITEMTEXT,"
						+ "pd.UNITOFMEASURE,pd.POAMOUNT,pd.IGSTAMOUNT,pd.CGSTAMOUNT,pd.SGSTAMOUNT,pd.CONTACTPERSONEMAILID,pd.CONTACTPERSONPHONE,pd.DELIVERYADDRESS1, "
						+ "pd.DELIVERYADDRESS2,pd.DELIVERYADDRESS3,pd.CITY,pd.STATE,pd.COUNTRY,pd.PINCODE,pd.BUYER,pd.REQUSITIONER,pd.CREATEDBY, "
						+ "pd.CREATEDON,pd.MODIFIEDBY,pd.MODIFIEDON,pd.STATUS,pd.PURCHASINGORGANISATION,pd.PURCHASINGGROUP, "
						+ "pd.COMPANYCODE,pd.QUOTATIONNO,pd.QUOTATIONDATE,poe.PLANT"
						+ " from podetails  pd join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.PONUMBER IS NOT NULL "
						+ "" + subquery + ") order by CREATEDON desc";

			}
			
			log.info("podata1 : "+podata1);
			pg = new Pagination(podata1, nPage);
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
				poData.put("PLANT", rs.getString("PLANT"));
				POImpl po = new POImpl();
				poData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT"), con));
				poData.put("VENDORID", rs.getString("VENDORID"));
				poData.put("VENDORNAME", getVendorName(rs.getString("VENDORID")));
				poData.put("DEPARTMENT", rs.getString("Department"));
				poData.put("COSTCENTRE", rs.getString("CostCentre"));
				poData.put("CATEGORY", rs.getString("Category"));
				poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
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

			getinvoicepofordashboardCountAsPerStatus("NA", ponumber, status, fromdurationdate, todurationdate, plant,
					purchasegroup, requisitioneremailid, frompoamount, topoamount, ageingfrom, ageingto,
					(String) session.getAttribute("email"), vendor, con, ps, rs, "po");
		} catch (Exception e) {
			log.error("pointernaldashboardsearch() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		if (POList.size() > 0) {
			responsejson.put("poData", POList);
			responsejson.put("popages", pages);
			responsejson.put("message", "Success");

			jsonArray.add(responsejson);
		} else {
			responsejson.put("message", "No Data Found for given Vendor Id");
			jsonArray.add(responsejson);
		}

		return jsonArray;
	}

	public JSONArray getplantcodeordescc(String text) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<HashMap<String, String>> PlantList = new ArrayList<HashMap<String, String>>();
		boolean plantcode = false;
		String sql = "";

		sql = "Select PLANTCODE,PLANTNAME from PLANTMASTER where (PLANTNAME LIKE ? OR PLANTCODE LIKE ?)";

		try {
			int bCount = 0;
			con = DBConnection.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, "%" + text + "%");
			ps.setString(2, "%" + text + "%");

			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> plantdata = new HashMap<String, String>();
				plantdata.put("PLANTCODE", rs.getString("PLANTCODE"));
				plantdata.put("PLANTNAME", rs.getString("PLANTNAME"));
				PlantList.add(plantdata);
			}
			rs.close();
			ps.close();
			if (PlantList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("grnbasedonpo", PlantList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getplantcodeordescc() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getvendorname(String text) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<HashMap<String, String>> PlantList = new ArrayList<HashMap<String, String>>();
		boolean vendorid = false;
		String sql = "";
		sql = "Select BUSINESSPARTNERTEXT,VENDORID from BUSINESSPARTNER where (BUSINESSPARTNERTEXT LIKE ? OR VENDORID LIKE ?)";
		try {
			int bCount = 0;
			con = DBConnection.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, text + "%");
			ps.setString(2, text + "%");

			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> plantdata = new HashMap<String, String>();
				plantdata.put("BUSINESSPARTNERTEXT", rs.getString("BUSINESSPARTNERTEXT"));
				plantdata.put("VENDORID", rs.getString("VENDORID"));
				PlantList.add(plantdata);
			}
			rs.close();
			ps.close();
			if (PlantList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("vendordetail", PlantList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getvendorname() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getmatcodeordescc(String text) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<HashMap<String, String>> MaterialList = new ArrayList<HashMap<String, String>>();

		String sql = "Select distinct MTYP, MTYPDESC from INVENTORYUSERLIST where MTYPDESC LIKE  ? ";

		try {
			con = DBConnection.getConnection();

			ps = con.prepareStatement(sql);
			ps.setString(1, "%" + text + "%");
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> materaildata = new HashMap<String, String>();
				materaildata.put("MATERIALTYPE", rs.getString("MTYP"));
				materaildata.put("MATERIALDESCRIPTION", rs.getString("MTYPDESC"));
				MaterialList.add(materaildata);
			}
			rs.close();
			ps.close();
			if (MaterialList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("grnbasedonpo", MaterialList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Empty");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getmatcodeordescc() :", e.fillInStackTrace());

			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public String getVendorName(String vendorid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		String qdata = "Select BUSINESSPARTNERTEXT from BUSINESSPARTNER where VENDORID=?";
		String sb = "";
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(qdata);
			ps.setString(1, vendorid);
			rs = ps.executeQuery();
			while (rs.next()) {
				sb = rs.getString("BUSINESSPARTNERTEXT");
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
			log.error("getVendorName() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return sb;
	}

	public JSONArray downloaddashboardinvoicelist(String invoicenumber, String ponumber, String status,
			String fromdurationdate, String todurationdate, String plant, String purchasegroup,
			String requisitioneremailid, String frominvamount, String toinvamount, String ageingfrom, String ageingto,
			HttpSession session, String emailId, String vendor) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String storeKepeer = (String) session.getAttribute("shopkepeer");
		String sql = "";
		ArrayList<String> param = new ArrayList<String>();
		List<String> invoicedata = new ArrayList<>();
		List<String> podata = new ArrayList<>();
		String subquery = "";
		int pages = 0;
		ArrayList<HashMap<String, String>> POListEvent = new ArrayList<HashMap<String, String>>();

		if (!status.equalsIgnoreCase("NA")) {
			if ("C".equalsIgnoreCase(status)) {
				String po = " AND ps.CREDITADVICENO IS NOT NULL";
				subquery = subquery + po;
			} else if ("ALL".equalsIgnoreCase(status)) {
				String po = " ";
				subquery = subquery + po;
			} else if ("P".equalsIgnoreCase(status)) {
				String po = " AND (ps.OVERALLSTATUS=? OR  ps.OVERALLSTATUS=?)";
				subquery = subquery + po;
				param.add("P");
				param.add("M");
			} else {
				String po = " AND ps.OVERALLSTATUS=?";
				subquery = subquery + po;
				param.add(status);
			}

		}
		if (!ponumber.equalsIgnoreCase("NA")) {
			String po = " AND ps.PONUMBER=?";
			subquery = subquery + po;
			param.add(ponumber);
		}
		if (!vendor.equalsIgnoreCase("NA")) {
			String po = " AND ps.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
			subquery = subquery + po;
			param.add(vendor);
		}
		if (!invoicenumber.equalsIgnoreCase("NA")) {
			String in = " AND ps.INVOICENUMBER=?";
			subquery = subquery + in;
			param.add(invoicenumber);
		}
		if ((!fromdurationdate.equalsIgnoreCase("NA")) && (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
			String dt = " AND ps.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
			subquery = subquery + dt;
			param.add(fromdurationdate);
			param.add(todurationdate);
		}
		if (!plant.equalsIgnoreCase("NA")) {
			String pt = " AND ps.PLANT=?";
			subquery = subquery + pt;
			param.add(plant);
		}
		if (!purchasegroup.equalsIgnoreCase("NA")) {
			String pg = " AND ps.MATERIAL_TYPE=?";
			subquery = subquery + pg;
			param.add(purchasegroup);
		}

		if (!requisitioneremailid.equalsIgnoreCase("NA")) {
			String re = " AND PS.REQUSITIONER=LOWER(?) ";
			subquery = subquery + re;
			param.add(requisitioneremailid);
		}

		if (!frominvamount.equalsIgnoreCase("NA")) {
			String ia = "  AND ps.AMOUNT BETWEEN ? AND ? ";
			subquery = subquery + ia;
			param.add(frominvamount);
			param.add(toinvamount);
		}
		if (!ageingfrom.equalsIgnoreCase("NA")) {
			String ia = "";
			if (!status.equalsIgnoreCase("NA")) {
				param.add(ageingfrom);
				if ("P".equalsIgnoreCase(status)) {
					ia = "  AND sysdate - TO_DATE(ps.MODIFIEDON, 'dd-mm-yy') > ?";
					if (!ageingto.equalsIgnoreCase("NA")) {
						ia = ia + "  AND sysdate - TO_DATE(ps.MODIFIEDON, 'dd-mm-yy') < ?";
						param.add(ageingto);
					}
				} else {
					ia = "  AND sysdate - TO_DATE(ps.MODIFIEDON, 'dd-mm-yy') > ?";
					if (!ageingto.equalsIgnoreCase("NA")) {
						ia = ia + "  AND sysdate - TO_DATE(ps.MODIFIEDON, 'dd-mm-yy') < ?";
						param.add(ageingto);
					}
				}
			}
			subquery = subquery + ia;
		}

		if (!status.equalsIgnoreCase("NA")) {
			if ("C".equalsIgnoreCase(status)) {

			} else if ("ALL".equalsIgnoreCase(status)) {

			} else if ("P".equalsIgnoreCase(status)) {
				param.add("P");
				param.add("M");
			} else {
				param.add(status);
			}

		}
		if (!ponumber.equalsIgnoreCase("NA")) {
			param.add(ponumber);
		}
		if (!vendor.equalsIgnoreCase("NA")) {
			param.add(vendor);
		}
		if (!invoicenumber.equalsIgnoreCase("NA")) {
			param.add(invoicenumber);
		}
		if ((!fromdurationdate.equalsIgnoreCase("NA")) && (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
			param.add(fromdurationdate);
			param.add(todurationdate);
		}
		if (!plant.equalsIgnoreCase("NA")) {
			param.add(plant);
		}
		if (!purchasegroup.equalsIgnoreCase("NA")) {
			param.add(purchasegroup);
		}
		if (!requisitioneremailid.equalsIgnoreCase("NA")) {
			param.add(requisitioneremailid);
		}
		if (!frominvamount.equalsIgnoreCase("NA")) {
			param.add(frominvamount);
			param.add(toinvamount);
		}
		if (!ageingfrom.equalsIgnoreCase("NA")) {
			String ia = "";
			if (!status.equalsIgnoreCase("NA")) {
				param.add(ageingfrom);
				if ("P".equalsIgnoreCase(status)) {
					if (!ageingto.equalsIgnoreCase("NA")) {
						param.add(ageingto);
					}
				} else {
					if (!ageingto.equalsIgnoreCase("NA")) {
						param.add(ageingto);
					}
				}
			}
		}

		try {
			con = DBConnection.getConnection();
			sql = "select ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ps.PONUMBER,ps.BUSINESSPARTNEROID,"
					+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
					+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
					+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
					+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
					+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND sysdate - TO_DATE(ps.CREATEDON, 'dd-mm-yy') < 90 "
					+ " " + subquery + "  AND ps.MPO IS NULL " + "UNION "
					+ "select distinct ps.INVOICENUMBER,ps.PLANT,ps.VENDORID,ia.PONUMBER,ps.BUSINESSPARTNEROID,"
					+ "ps.MESSAGE,ps.REQUSITIONER,ps.BUYER,ps.AMOUNT,ps.CREATEDON,ps.MACOUNT,"
					+ "ps.HOLDCOUNT,ps.OVERALLSTATUS,ps.INVOICEDATE,ps.TOTALAMOUNT,"
					+ "ps.MATERIAL_TYPE,ps.PGQ,ps.ONEXSTATUS,ps.ACTUALFILENAME,ps.SAVEDFILENAME,"
					+ "ps.PAYMENTAMOUNT,ps.CREDITNOTENO,ps.CREDITADVICENO,ps.TOTALAMTINCTAXES, ps.TAXAMOUNT,"
					+ "ps.BUSINESSPARTNERTEXT,ps.EXPENSESHEETID,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber "
					+ "where ps.invoicenumber is not null AND sysdate - TO_DATE(ps.CREATEDON, 'dd-mm-yy') < 90 " + " "
					+ subquery + " AND ps.MPO = 'Y' " + " order by CREATEDON desc";

			Pagination pg = null;

			pg = new Pagination(sql, 0);
			pages = pg.getPages(con, param);
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

			pg.close();
			rs.close();
			pg = null;

		} catch (Exception e) {
			log.error("downloaddashboardinvoicelist() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getinternalPOS(String email1, String mode, String status, int nPage, String ponumber,
			String fromdateofduration, String todateofduration, String fromdateofpo, String todateofpo, String plant,
			String vendor) {
		boolean twocondition = true;
		boolean nodata = false;
		String po_data = "";

		if (mode.equalsIgnoreCase("buyer")) {
			if (status.equalsIgnoreCase("SP")) {
				po_data = "select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where "
						+ "pd.CONTACTPERSONEMAILID=? and pd.POTYPE =? order by CREATEDON desc";
			} else if (status.equalsIgnoreCase("ALL")) {
				po_data = "select * from (select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where " + "pd.CONTACTPERSONEMAILID=? and pd.Status=? "
						+ "Union " + "select DISTINCT pd.PONUMBER,pd.CREATEDON from "
						+ "podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.Status <> ? ) order "
						+ "by CREATEDON desc";
			} else {
				po_data = "select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe on "
						+ "pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.Status = ? order by CREATEDON desc";
			}

		} else if (mode.equalsIgnoreCase("enduser")) {
			if (status.equalsIgnoreCase("SP")) {
				po_data = "select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? "
						+ "and pd.POTYPE =? order by CREATEDON desc";

			} else if (status.equalsIgnoreCase("ALL")) {
				po_data = "select * from (select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where " + "pd.REQUSITIONER=? and pd.Status=? " + "Union "
						+ "select DISTINCT pd.PONUMBER,pd.CREATEDON from " + "podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? and pd.Status <> ? ) order "
						+ "by CREATEDON desc";
			} else {
				po_data = "select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? "
						+ "and pd.Status = ? order by CREATEDON desc";

			}
		} else if (mode.equalsIgnoreCase("payer")) {
			twocondition = false;
			if (status.equalsIgnoreCase("SP")) {
				po_data = "select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where  pd.POTYPE =? order by CREATEDON desc";

			} else if (status.equalsIgnoreCase("ALL")) {

				po_data = "select * from (select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where pd.Status=? " + "Union "
						+ "select DISTINCT pd.PONUMBER,pd.CREATEDON from " + "podetails pd join poeventdetails poe "
						+ "on pd.PONUMBER = poe.PONUMBER where pd.Status <> ? ) order " + "by CREATEDON desc";
			} else {

				po_data = "select DISTINCT pd.PONUMBER,pd.CREATEDON from podetails pd "
						+ "join poeventdetails poe on pd.PONUMBER = poe.PONUMBER where pd.Status = ? order by CREATEDON desc";

			}
		} else if (mode.equalsIgnoreCase("internalbcclportal")) {
			nodata = true;
		}

		if (nodata == false) {

			ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
			ArrayList<String> topic = new ArrayList<String>();
			List<String> podata = new ArrayList<>();

			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			int pages = 0;

			try {
				con = DBConnection.getConnection();

				Pagination pg = null;
				if (!status.equalsIgnoreCase("AS")) {

					ArrayList<String> param = new ArrayList<String>();

					if (status.equalsIgnoreCase("ALL")) {
						if (twocondition == true) {
							param.add(email1);
							param.add("N");
							param.add(email1);
							param.add("N");

						} else {
							param.add("N");
							param.add("N");
						}
					} else {
						if (twocondition == true) {
							param.add(email1);
							param.add(status);
						} else {
							param.add(status);
						}
					}

					pg = new Pagination(po_data, nPage);
					rs = pg.execute(con, param);
				} else {
					String subquery = "";
					String podata1 = "";
					ArrayList<String> param = new ArrayList<String>();
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(email1);
					}
					param.add("N");

					if (!plant.equalsIgnoreCase("NA")) {
						String po = " AND poe.PLANT=?";
						subquery = subquery + po;
						param.add(plant);

					}

					if (!vendor.equalsIgnoreCase("NA")) {
						String po = " AND pd.VENDORID=?";
						subquery = subquery + po;
						param.add(vendor);

					}

					if (!ponumber.equalsIgnoreCase("NA")) {
						String po = " AND pd.PONUMBER=?";
						subquery = subquery + po;
						param.add(ponumber);

					}
					if ((!fromdateofduration.equalsIgnoreCase("NA"))
							&& (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
						String in = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') "
								+ " AND TO_DATE(?, 'DD/MM/YYYY')";
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
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(email1);
					}
					param.add("N");
					if (!plant.equalsIgnoreCase("NA")) {
						param.add(plant);

					}

					if (!vendor.equalsIgnoreCase("NA")) {
						param.add(vendor);

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

					if (mode.equalsIgnoreCase("buyer")) {

						podata1 = "select * from ("
								+ "select pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=?  " + "and pd.Status=? "
								+ subquery + " " + "Union "
								+ "select pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? "
								+ "and pd.Status <> ? " + subquery + ") " + "order by CREATEDON desc";

					} else if (mode.equalsIgnoreCase("enduser")) {

						podata1 = "select * from ("
								+ "select pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=?  " + "and pd.Status=? "
								+ subquery + " " + "Union "
								+ "select pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? and pd.Status <> ? " + subquery
								+ ") " + "order by CREATEDON desc";
					} else if (mode.equalsIgnoreCase("payer")) {
						twocondition = false;
						podata1 = "select * from ("
								+ "select pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.Status=? " + subquery + " " + "Union "
								+ "select pd.PONUMBER,pd.CREATEDON from podetails pd join poeventdetails poe "
								+ "on pd.PONUMBER = poe.PONUMBER where pd.Status <> ? " + subquery + ") "
								+ "order by CREATEDON desc";

					}
					pg = new Pagination(podata1, nPage);
					rs = pg.execute(con, param);
				}

				while (rs.next()) {

					HashMap<String, String> poData = new HashMap<String, String>();
					poData.put("PO_NUMBER", rs.getString("PONumber"));

					podata.add(rs.getString("PONumber"));
				}
				pg.close();
				rs.close();
				pg = null;

			} catch (SQLException e) {
				log.error("getinternalPOS() :", e.fillInStackTrace());

				responsejson.put("message", "Network Issue");
				jsonArray.add(responsejson);
			} finally {
				DBConnection.closeConnection(rs, ps, con);
			}
			if (podata.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("podata", podata);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}
		} else {
			responsejson.put("message", "No Data Found for given Email Id");
			jsonArray.add(responsejson);
		}
		return jsonArray;

	}

	public JSONArray getinernaldownloadInvoceidIdDetail(String emailid, int nPage, String status, String invno,
			String pono, String fdate, String tdate, String plant, String vendor, String mode) {

		String sdata = "";
		String hdata = "";
		String sql = "";
		String pendingsql = "";
		String alldata = "";
		List<String> invoicedata = new ArrayList<>();
		List<String> podata = new ArrayList<>();

		String basePoQuery = " and ps.ponumber=ia.ponumber ";

		if (mode.equalsIgnoreCase("buyer")) {

			// For Short quantity

			sdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and "
					+ "BUYER =? AND ps.CREDITADVICENO IS NOT NULL AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
					+ "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null and "
					+ "BUYER =? AND ps.CREDITADVICENO IS NOT NULL AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' "
					+ "order by CREATEDON desc";

			// All Status
			alldata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where "
					+ "ps.invoicenumber is not null and BUYER =? AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
					+ "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where"
					+ " ps.invoicenumber is not null and BUYER =? AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' "
					+ "order by CREATEDON desc";

			// For pending QUANTITY
			pendingsql = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where  BUYER =? "
					+ "AND ps.invoicenumber is not null AND  ps.ALLPO IS NULL AND ps.MPO IS NULL "
					+ "AND (OVERALLSTATUS=? OR OVERALLSTATUS=?)  " + "UNION "
					+ "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where BUYER =? "
					+ "AND ps.invoicenumber is not null AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' AND "
					+ " (OVERALLSTATUS=? OR OVERALLSTATUS=?) " + "order by CREATEDON desc";

			// For OFFLINE INVOICES
			hdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where"
					+ " ps.invoicenumber is not null AND BUYER =? AND ONEXSTATUS=?  ps.ALLPO IS NULL AND ps.MPO IS NULL "
					+ " UNION " + "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where "
					+ "ps.invoicenumber is not null and BUYER =? AND ONEXSTATUS=? "
					+ "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";
			// All Filter
			sql = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where  "
					+ " ps.invoicenumber is not null AND BUYER =? AND OVERALLSTATUS=? AND ps.ALLPO IS NULL AND ps.MPO IS NULL "
					+ "UNION " + "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND BUYER =? AND OVERALLSTATUS=? "
					+ "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";

		} else if (mode.equalsIgnoreCase("enduser")) {

			// For pending QUANTITY
			pendingsql = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where  REQUSITIONER =? "
					+ "AND ps.invoicenumber is not null AND  ps.ALLPO IS NULL AND ps.MPO IS NULL "
					+ "AND (OVERALLSTATUS=? OR OVERALLSTATUS=?)  " + "UNION "
					+ "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where REQUSITIONER =? "
					+ "AND ps.invoicenumber is not null AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' AND "
					+ " (OVERALLSTATUS=? OR OVERALLSTATUS=?) " + "order by CREATEDON desc";

			// For OFFLINE INVOICES
			hdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where"
					+ " ps.invoicenumber is not null AND REQUSITIONER =? AND ONEXSTATUS=?  ps.ALLPO IS NULL AND ps.MPO IS NULL "
					+ " UNION " + "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where "
					+ "ps.invoicenumber is not null and REQUSITIONER =? AND ONEXSTATUS=? "
					+ "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";
			// All Filter
			sql = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where  "
					+ " ps.invoicenumber is not null AND REQUSITIONER =? AND OVERALLSTATUS=? AND ps.ALLPO IS NULL AND ps.MPO IS NULL "
					+ "UNION " + "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND REQUSITIONER =? AND OVERALLSTATUS=? "
					+ "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";

			// For Short quantity

			sdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and "
					+ "REQUSITIONER =? AND ps.CREDITADVICENO IS NOT NULL AND ps.ALLPO IS NULL AND ps.MPO IS NULL "
					+ "UNION " + "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null and "
					+ "REQUSITIONER =? AND ps.CREDITADVICENO IS NOT NULL AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' "
					+ "order by CREATEDON desc";

			// All Status
			alldata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where "
					+ "ps.invoicenumber is not null and REQUSITIONER =? AND ps.ALLPO IS NULL AND ps.MPO IS NULL "
					+ "UNION " + "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where"
					+ " ps.invoicenumber is not null and REQUSITIONER =? AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' "
					+ "order by CREATEDON desc";

		} else if (mode.equalsIgnoreCase("payer")) {

			// For pending QUANTITY
			pendingsql = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL "
					+ "AND (OVERALLSTATUS=? or OVERALLSTATUS=?) " + "UNION "
					+ "select distinct ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
					+ "(OVERALLSTATUS=? or OVERALLSTATUS=?)" + " order by CREATEDON desc";

			// For OFFLINE INVOICES
			hdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL "
					+ "AND ONEXSTATUS=? " + "UNION "
					+ "select distinct ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
					+ "AND ONEXSTATUS=?" + " order by CREATEDON desc";

			// All Filter
			sql = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL "
					+ "AND OVERALLSTATUS=? " + "UNION "
					+ "select distinct ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ " from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber "
					+ "and ps.ponumber=ia.ponumber where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
					+ "OVERALLSTATUS=?" + " order by CREATEDON desc";

			// For Short quantity
			sdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL "
					+ "AND ps.CREDITADVICENO IS NOT NULL " + "UNION "
					+ "select distinct ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
					+ "ps.CREDITADVICENO IS NOT NULL" + " order by CREATEDON desc";

			// All Status
			alldata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO IS NULL " + "UNION "
					+ "select distinct ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
					+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null AND ps.MPO = 'Y' AND  "
					+ "ps.CREDITADVICENO IS NOT NULL" + " order by CREATEDON desc";

		} else if (mode.equalsIgnoreCase("internalbcclportal")) {
			boolean nodata = true;
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ"))) {

				ArrayList<String> param = new ArrayList<String>();
				if (!mode.equalsIgnoreCase("payer")) {
					param.add(emailid);
				}

				Pagination pg = null;
				if (status.equalsIgnoreCase("H")) {
					param.add(status);
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}
					param.add(status);
					pg = new Pagination(hdata, nPage);
				} else if (status.equalsIgnoreCase("ALL")) {
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}
					pg = new Pagination(alldata, nPage);
				} else if (status.equalsIgnoreCase("C")) {
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}
					pg = new Pagination(sdata, nPage);
				} else if (status.equalsIgnoreCase("P")) {
					param.add("P");
					param.add("M");
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}
					param.add("P");
					param.add("M");
					pg = new Pagination(pendingsql, nPage);
				} else if (status.equalsIgnoreCase("V")) {
					param.add("V");
					param.add("RO");
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}
					param.add("V");
					param.add("RO");
					pg = new Pagination(pendingsql, nPage);
				} else {
					param.add(status);
					if (!mode.equalsIgnoreCase("payer")) {
						param.add(emailid);
					}
					param.add(status);
					pg = new Pagination(sql, nPage);

				}
				rs = pg.execute(con, param);

				int count = 0;
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
						String in = " AND ps.INVOICENUMBER=?";
						param.add(invno);
					}
					if ((!fdate.equalsIgnoreCase("NA")) && (!fdate.equalsIgnoreCase("Invalid date"))) {
						param.add(fdate);
						param.add(tdate);
					}
					if (mode.equalsIgnoreCase("buyer")) {

						advqdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and BUYER =? "
								+ subquery + " " + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
								+ "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
								+ basePoQuery + " where ps.invoicenumber is not null and BUYER =? " + subquery + " "
								+ "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";
					} else if (mode.equalsIgnoreCase("enduser")) {

						advqdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and REQUSITIONER =? "
								+ subquery + " " + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
								+ "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
								+ basePoQuery + "  where ps.invoicenumber is not null and REQUSITIONER =? " + subquery
								+ " " + "AND ps.ALLPO IS NOT NULL AND ps.MPO ='Y' " + "order by CREATEDON desc";

					} else if (mode.equalsIgnoreCase("payer")) {

						advqdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null " + subquery
								+ "  AND ps.MPO IS NULL " + "UNION "
								+ "select distinct ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps  where ps.invoicenumber is not null " + subquery
								+ " AND ps.MPO = 'Y' order by CREATEDON desc";

					}

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

					if (mode.equalsIgnoreCase("buyer")) {
						advqdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and BUYER =? "
								+ subquery + " " + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
								+ "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
								+ " and ps.ponumber=ia.ponumber where ps.invoicenumber is not null and BUYER =? "
								+ subquery + " " + "AND ps.ALLPO IS NOT NULL " + "AND ps.MPO ='Y' "
								+ "order by CREATEDON desc";

					} else if (mode.equalsIgnoreCase("enduser")) {

						advqdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null and REQUSITIONER =? "
								+ subquery + " " + "AND ps.ALLPO IS NULL AND ps.MPO IS NULL " + "UNION "
								+ "select ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps join invoiceapproval ia ON ps.invoicenumber = ia.invoicenumber"
								+ " and ps.ponumber=ia.ponumber where ps.invoicenumber is not null and REQUSITIONER =? "
								+ subquery + " " + "AND ps.ALLPO IS NOT NULL " + "AND ps.MPO ='Y' "
								+ "order by CREATEDON desc";
					} else if (mode.equalsIgnoreCase("payer")) {
						advqdata = "select ps.INVOICENUMBER,ps.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null " + subquery + " AND  "
								+ "ps.CREDITADVICENO IS NOT NULL AND ps.MPO IS NULL " + "UNION "
								+ "select distinct ps.INVOICENUMBER,ia.PONUMBER,ps.CREATEDON,ps.MPO,ps.ALLPO "
								+ "from PONINVOICESUMMERY ps where ps.invoicenumber is not null " + subquery
								+ " AND ps.MPO = 'Y' AND  " + "ps.CREDITADVICENO IS NOT NULL"
								+ " order by CREATEDON desc";

					}

				}
				pg = new Pagination(advqdata, nPage);
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

				pg.close();
				rs.close();
				pg = null;

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
		} catch (SQLException e) {
			log.error("getinernaldownloadInvoceidIdDetail() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;

	}

	public JSONArray downloaddashboardpolist(String ponumber, String status, String fromdurationdate,
			String todurationdate, String plant, String purchasegroup, String requisitioneremailid, String frompoamount,
			String topoamount, String ageingfrom, String ageingto, HttpSession session, String email, int nPage,
			String mode, String vendor) {
		boolean twocondition = true;
		boolean nodata = false;
		String po_data = "";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		Pagination pg = null;
		HashMap<String, String> QueryList = new HashMap<String, String>();
		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		ArrayList<String> topic = new ArrayList<String>();

		try {

			con = DBConnection.getConnection();
			String subquery = "";
			String podata1 = "";
			ArrayList<String> param = new ArrayList<String>();
			List<String> podata = new ArrayList<>();
			if (!mode.equalsIgnoreCase("payer")) {
				param.add(email);
			}

			param.add("N");
			if (!vendor.equalsIgnoreCase("NA")) {
				String po = " AND VENDORID=?";
				subquery = subquery + po;
				param.add(vendor);

			}
			if (!ponumber.equalsIgnoreCase("NA")) {
				String po = " AND PONUMBER=?";
				subquery = subquery + po;
				param.add(ponumber);

			}
			if ((!fromdurationdate.equalsIgnoreCase("NA")) && (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
				String in = " AND PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + " AND TO_DATE(?, 'DD/MM/YYYY')";
				subquery = subquery + in;
				param.add(fromdurationdate);
				param.add(todurationdate);
			}
			if (!status.equalsIgnoreCase("NA")) {
				String dt = " AND STATUS=?";
				subquery = subquery + dt;
				param.add(status);
			}
			if (!plant.equalsIgnoreCase("NA")) {
				String dt = " AND PLANT =?";
				subquery = subquery + dt;
				param.add(plant);
			}
			if (!purchasegroup.equalsIgnoreCase("NA")) {
				String dt = " AND MATERIAL_TYPE=?";
				subquery = subquery + dt;
				param.add(purchasegroup);
			}
			if (!requisitioneremailid.equalsIgnoreCase("NA")) {
				String dt = " AND REQUSITIONER = LOWER(?)";
				subquery = subquery + dt;
				param.add(requisitioneremailid);
			}
			if (!frompoamount.equalsIgnoreCase("NA")) {
				String ia = "  AND POAMOUNT BETWEEN ? AND ? ";
				subquery = subquery + ia;
				param.add(frompoamount);
				param.add(topoamount);
			}
			if (!ageingfrom.equalsIgnoreCase("NA")) {
				param.add(ageingfrom);
				String ia = "  AND sysdate - TO_DATE(MODIFIEDON, 'dd-mm-yy') > ?";
				if (!ageingto.equalsIgnoreCase("NA")) {
					ia = ia + "  AND sysdate - TO_DATE(MODIFIEDON, 'dd-mm-yy') < ?";
					param.add(ageingto);
				}

				subquery = subquery + ia;
			}

			if (!mode.equalsIgnoreCase("payer")) {
				param.add(email);
			}
			param.add("N");
			if (!vendor.equalsIgnoreCase("NA")) {
				param.add(vendor);

			}
			if (!ponumber.equalsIgnoreCase("NA")) {
				param.add(ponumber);

			}
			if ((!fromdurationdate.equalsIgnoreCase("NA")) && (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
				param.add(fromdurationdate);
				param.add(todurationdate);
			}
			if (!status.equalsIgnoreCase("NA")) {
				param.add(status);
			}
			if (!plant.equalsIgnoreCase("NA")) {
				param.add(plant);
			}
			if (!purchasegroup.equalsIgnoreCase("NA")) {
				param.add(purchasegroup);
			}
			if (!requisitioneremailid.equalsIgnoreCase("NA")) {
				param.add(requisitioneremailid);
			}
			if (!frompoamount.equalsIgnoreCase("NA")) {
				param.add(frompoamount);
				param.add(topoamount);
			}
			if (!ageingfrom.equalsIgnoreCase("NA")) {
				param.add(ageingfrom);
				if (!ageingto.equalsIgnoreCase("NA")) {
					param.add(ageingto);
				}
			}

			if (mode.equalsIgnoreCase("buyer")) {

				podata1 = "select * from ("
						+ "select PONUMBER,CREATEDON from podetails where CONTACTPERSONEMAILID=?  and Status=? AND sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') < 90 "
						+ subquery + " " + "Union "
						+ "select PONUMBER,CREATEDON from podetails where CONTACTPERSONEMAILID=? and Status <> ? AND sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') < 90 "
						+ subquery + " " + ") order by CREATEDON desc";

			} else if (mode.equalsIgnoreCase("enduser")) {

				podata1 = "select * from ("
						+ "select PONUMBER,CREATEDON from podetails where REQUSITIONER=?  and Status=? AND sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') < 90 "
						+ subquery + " " + "Union "
						+ "select PONUMBER,CREATEDON from podetails where REQUSITIONER=?  and Status <> ? AND sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') < 90 "
						+ subquery + " " + ") order by CREATEDON desc";

			} else if (mode.equalsIgnoreCase("payer")) {
				twocondition = false;
				podata1 = "select * from (select PONUMBER,CREATEDON from "
						+ "podetails where Status=? AND sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') < 90 " + "" + subquery
						+ " " + "Union " + "select PONUMBER,CREATEDON from podetails where Status <> ? "
						+ "AND sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') < 90 " + subquery + ") "
						+ "order by CREATEDON desc";

			}
			
			log.info("podata1 : "+podata1);
			pg = new Pagination(podata1, nPage);
			rs = pg.execute(con, param);
			while (rs.next()) {
				podata.add(rs.getString("PONumber"));
			}

			if (podata.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("podata", podata);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}
			rs.close();
			pg.close();
			pg = null;
		} catch (Exception e) {
			log.error("downloaddashboardpolist() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	private static boolean sendMailtoCentralOps(String invoiceno, String pono, String emailid, Connection con)
			throws SQLException {
		String poNumber = null;
		String invoiceNumber = null;
		String vendorName = null;
		String invoiceDetails = null;
		String amount = null;
		String primaryEmailId = null;
		String secondaryEmailId = null;
		String tertiaryEmailId = null;
		String requsitionerEmailId = null;
		String buyerEmailId = null;
		String endUserId = null;
		String euManager = null;
		String mailingList = "";
		String status = null;
		String overallStatus = null;
		String endUserStatus = null;
		String managerStatus = null;
		ArrayList managerList = new ArrayList();

		String findusersDetails = " SELECT B.BUSINESSPARTNERTEXT AS VENDOR_NAME, P.PONUMBER AS PONUMBER, P.INVOICENUMBER AS INVOICENUMBER,"
				+ " P.AMOUNT AS AMOUNT, P.OVERALLSTATUS AS OVERALLSTATUS, P.REQUSITIONER AS REQUSITIONER, P.BUYER AS BUYER,"
				+ " I.ENDUSEID AS ENDUSEID, I.ENDUSERSTATUS AS ENDUSERSTATUS,"
				+ " I.EUMANAGER AS EUMANAGER ,I.STATUS AS MANAGERSTATUS"
				+ " FROM PONINVOICESUMMERY P ,BUSINESSPARTNER B ,INVOICEAPPROVAL I"
				+ " WHERE B.BUSINESSPARTNEROID = P.BUSINESSPARTNEROID AND I.INVOICENUMBER = P.INVOICENUMBER AND"
				+ " I.PONUMBER = P.PONUMBER  AND P.INVOICENUMBER = ? AND P.PONUMBER = ? AND I.STATUS in ('CA','CM','CO') ORDER BY "
				+ " DECODE(I.STATUS,'CM','X','M','Y','A'), I.MODIFIEDDATE ASC ";

		boolean flag = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(findusersDetails);
			ps.setString(1, invoiceno);
			ps.setString(2, pono);
			rs = ps.executeQuery();
			int count = 0;
			while (rs.next()) {

				if (count == 0) {

					vendorName = rs.getString("VENDOR_NAME") == null ? "" : rs.getString("VENDOR_NAME");
					poNumber = rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER");
					invoiceNumber = rs.getString("INVOICENUMBER") == null ? "" : rs.getString("INVOICENUMBER");
					amount = rs.getString("AMOUNT") == null ? "" : rs.getString("AMOUNT");
					status = rs.getString("OVERALLSTATUS") == null ? "" : rs.getString("OVERALLSTATUS");
					requsitionerEmailId = rs.getString("REQUSITIONER") == null ? "" : rs.getString("REQUSITIONER");
					buyerEmailId = rs.getString("BUYER") == null ? "" : rs.getString("BUYER");
					endUserId = rs.getString("ENDUSEID") == null ? "" : rs.getString("ENDUSEID");
					endUserStatus = rs.getString("ENDUSERSTATUS") == null ? "" : rs.getString("ENDUSERSTATUS");
					if (!("".equalsIgnoreCase(endUserId)) && !("NA".equalsIgnoreCase(endUserId))) {
						managerList.add(endUserId + "-" + endUserStatus);
					}
					count++;
				}
				euManager = rs.getString("EUMANAGER") == null ? "" : rs.getString("EUMANAGER");
				managerStatus = rs.getString("MANAGERSTATUS") == null ? "" : rs.getString("MANAGERSTATUS");
				if (!("".equalsIgnoreCase(euManager)) && !("NA".equalsIgnoreCase(euManager))) {
					managerList.add(euManager + "-" + managerStatus);
				}

				if (!mailingList.contains(euManager)) {
					if (!emailid.equalsIgnoreCase(euManager)) {
						if (!("".equalsIgnoreCase(euManager)) && !("NA".equalsIgnoreCase(euManager))) {
							mailingList += euManager + ",";
						}

					}
				}

			}
			rs.close();
			ps.close();
			managerStatus = "";
			if (!mailingList.contains(requsitionerEmailId)) {
				if (!emailid.equalsIgnoreCase(requsitionerEmailId)) {
					if (!("".equalsIgnoreCase(requsitionerEmailId)) && !("NA".equalsIgnoreCase(requsitionerEmailId))) {
						mailingList += requsitionerEmailId + ",";
					}

				}
			}
			if (!mailingList.contains(buyerEmailId)) {
				if (!emailid.equalsIgnoreCase(buyerEmailId)) {
					if (!("".equalsIgnoreCase(buyerEmailId)) && !("NA".equalsIgnoreCase(buyerEmailId))) {
						mailingList += buyerEmailId + ",";
					}

				}
			}
			if (!mailingList.contains(endUserId)) {
				if (!emailid.equalsIgnoreCase(endUserId)) {
					if (!("".equalsIgnoreCase(endUserId)) && !("NA".equalsIgnoreCase(endUserId))) {
						mailingList += endUserId + ",";
					}

				}
			}

			mailingList = mailingList.substring(0, mailingList.length() - 1);
			StringBuilder buf = new StringBuilder();
			buf.append("Dear Colleague,<br><br> Below are the details with status for invoice number : " + invoiceNumber
					+ ". The Goods / Services have been stated as accepted on your behalf, please click <a href='https://timescape.timesgroup.com/irj/portal?NavigationTarget=navurl://d97ea9098661623a123464628538661c'>here</a> to confirm authorisation "
					+ "for goods/services acceptance on your behalf." + "<br><br><b>Vendor Name : " + vendorName
					+ "</b><br><br>"
					+ "<!DOCTYPE html> <html><head><style> table { font-family: arial, sans-serif; border-collapse: collapse; width: 100%; }td, "
					+ "th { border: 1px solid #dddddd; text-align: left; padding: 8px; }tr:nth-child(even) { background-color: #dddddd; }"
					+ " </style> </head> <body> <table>" + " <tr>" + " <th>Sr.No</th>" + " <th>Invoice No.</th>"
					+ " <th>Invoice Amount </th>  <th>Invoice Status </th>" + " </tr>");

			if ("A".equals(status)) {
				overallStatus = "Approved";
			} else if ("R".equals(status)) {
				overallStatus = "Rejected";
			} else if ("O".equals(status)) {
				overallStatus = "On Hold";
			} else if ("M".equals(status)) {
				overallStatus = "Pending";
			} else if ("V".equals(status)) {
				overallStatus = "Returned";
			}
			buf.append("<tr><td>1.</td>").append("<td>").append(invoiceNumber).append("</td><td>").append(amount)
					.append("</td><td>").append(overallStatus).append("</td></tr>");

			buf.append("</table> </body><br><br><b>Approval Status :</b><br><br>");

			buf.append("<table><tr><td>Approver Email ID</td><td>Status</td></tr>");

			for (int countM = 0; countM < managerList.size(); countM++) {
				String managerData = (String) managerList.get(countM);
				String managerListStatus[] = managerData.split("-");
				managerStatus = "";
				if ("A".equals(managerListStatus[1])) {
					if (endUserId.equalsIgnoreCase(managerListStatus[0])) {
						managerStatus = "GRN Creator";
					} else {
						managerStatus = "Invoice Approver";
					}
				} else if ("R".equals(managerListStatus[1])) {
					managerStatus = "Rejected";
				} else if ("O".equals(managerListStatus[1])) {
					managerStatus = "On Hold";
				} else if ("M".equals(managerListStatus[1])) {
					managerStatus = "Pending";
				} else if ("V".equals(managerListStatus[1])) {
					managerStatus = "Returned";
				} else if ("CO".equals(managerListStatus[1])) {
					managerStatus = "On Hold";
				} else if ("CM".equals(managerListStatus[1])) {
					managerStatus = "Pending";
				} else if ("CA".equals(managerListStatus[1])) {
					managerStatus = "GRN Confirmed";
				}

				buf.append("<tr><td>" + managerListStatus[0] + "</td><td>" + managerStatus + "</td>");
			}
			buf.append("</table>");
			buf.append("</html>" + " <br>Regards,<br><br> BCCL PartnerDx Team <br>");
			String content = buf.toString();

			String Subject = "BCCL PartnerDx : Pending Goods/Services Acceptance Confirmation - " + invoiceNumber;
			log.info("content : " + content);
			String toAddr = mailingList;
			String ccAddr = emailid;
			Hashtable mailDetails = new Hashtable();
			mailDetails.put("fromAddr", "noreply.partnerdx@timesgroup.com");
			mailDetails.put("toAddr", toAddr);
			mailDetails.put("ccAddr", ccAddr);
			mailDetails.put("subject", Subject);
			mailDetails.put("content", content);
			SendImapMessage myMail = new SendImapMessage();
			Properties prop = new Properties();
			InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
			prop.load(input);
			String server = prop.getProperty("servertype");
			if ("dev".equalsIgnoreCase(server)) {
				flag = true;
			} else if ("uat".equalsIgnoreCase(server)) {
				// flag = myMail.sendHtmlMail(mailDetails);
				flag = true;
			}

		} catch (Exception e) {
			log.error("sendMailtoCentralOps() :", e.fillInStackTrace());

		} finally {
		}
		return flag;

	}

	public JSONArray getinternalPODetailsCountAsPerStatus(String email1, String mode, String status, int nPage,
			String ponumber, String fromdateofduration, String todateofduration, String fromdateofpo, String todateofpo,
			String plant, String vendor, Connection con, PreparedStatement ps, ResultSet rs) throws SQLException {

		try {

			HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
			int allCounter = 0;

			if (!status.equalsIgnoreCase("AS")) {
				String po_data = "";

				if (mode.equalsIgnoreCase("buyer")) {
					po_data = "select pd.status ,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? group by pd.status";
				} else if (mode.equalsIgnoreCase("enduser")) {
					po_data = "select pd.status ,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? group by pd.status " 
							+" Union "
							+ "select pd.status ,count(distinct pd.ponumber) as count from podetails pd, poeventdetails poe, "
							+"locationmaster lm where pd.PONUMBER = poe.PONUMBER and lm.LOCATIONCODE = poe.DELVPLANT "
							+ "and lm.STOREKEEPEREMILID=? group by pd.status ";
				} else if (mode.equalsIgnoreCase("payer")) {
					po_data = "select pd.status ,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER group by pd.status";
				}
				log.info("counts po_data --"+po_data);
				ps = con.prepareStatement(po_data);
				if (!mode.equalsIgnoreCase("payer")) {
					ps.setString(1, email1);
					ps.setString(2, email1);
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

				String po_data1 = "";

				if (mode.equalsIgnoreCase("buyer")) {
					po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.potype=?";
				} else if (mode.equalsIgnoreCase("enduser")) {
					po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? and pd.potype=?"
							+" Union "
							+ "select count(distinct pd.ponumber) as count from podetails pd,poeventdetails poe,locationmaster lm "
							+ "where lm.LOCATIONCODE = poe.DELVPLANT and pd.PONUMBER = poe.PONUMBER and "
							+" lm.STOREKEEPEREMILID=? and pd.potype=? ";
				} else if (mode.equalsIgnoreCase("payer")) {
					po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where  pd.potype=?";
				}

				log.info("counts po_data1 --" + po_data1);
				ps = con.prepareStatement(po_data1);
				if (!mode.equalsIgnoreCase("payer")) {
					ps.setString(1, email1);
					ps.setString(2, "S");
					ps.setString(3, email1);
					ps.setString(4, "S");
				} else {
					ps.setString(1, "S");
				}

				rs = ps.executeQuery();
				while (rs.next()) {
					String count = rs.getString("count");
					countAsPerStatus.put("SP", count);
				}
				rs.close();
				ps.close();

			} else {
				String subquery = "";
				if (!plant.equalsIgnoreCase("NA")) {
					String po = " AND poe.PLANT=?";
					subquery = subquery + po;
				}
				if (!vendor.equalsIgnoreCase("NA")) {
					String po = " AND pd.VENDORID=?";
					subquery = subquery + po;
				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND pd.PONUMBER=?";
					subquery = subquery + po;

				}
				if ((!fromdateofduration.equalsIgnoreCase("NA"))
						&& (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
					String in = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + in;
				}
				if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
					String dt = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
				}

				String po_data = "";

				if (mode.equalsIgnoreCase("buyer")) {
					po_data = "select pd.status ,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? " + subquery
							+ "  group by pd.status";
				} else if (mode.equalsIgnoreCase("enduser")) {
					po_data = "Select pd.status ,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=?  " + subquery
							+ " group by pd.status "
							+" Union "
							+ "select pd.status ,count(distinct pd.ponumber) as count from podetails pd, poeventdetails poe, "
							+ "locationmaster lm "
							+ "where lm.LOCATIONCODE = poe.DELVPLANT and pd.PONUMBER = poe.PONUMBER and "
							+" lm.STOREKEEPEREMILID = ? " + subquery
							+ " group by pd.status";
				} else if (mode.equalsIgnoreCase("payer")) {
					po_data = "select pd.status ,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER  " + subquery + " group by pd.status";
				}

				log.info("counts po_data -- :"+po_data);
				
				ps = con.prepareStatement(po_data);
				int queryCount = 0;
				if (!mode.equalsIgnoreCase("payer")) {
					queryCount++;
					ps.setString(queryCount, email1);
				//	ps.setString(queryCount, email1);
					
				}
				if (!plant.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, plant);
				}
				if (!vendor.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, vendor);
				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, ponumber);
				}
				if ((!fromdateofduration.equalsIgnoreCase("NA")) && (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
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
				if (!mode.equalsIgnoreCase("payer")) {
					queryCount++;
					ps.setString(queryCount, email1);
				}
				if (!plant.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, plant);
				}
				if (!vendor.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, vendor);
				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, ponumber);
				}				
				if ((!fromdateofduration.equalsIgnoreCase("NA")) && (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
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
					String sts = rs.getString("status");
					String count = rs.getString("count");
					countAsPerStatus.put(sts, count);
					allCounter += Integer.parseInt(count);
				}
				countAsPerStatus.put("ALL", allCounter + "");
				countAsPerStatus.put("SP", "0");
				rs.close();
				ps.close();

				/*
				String po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
						+ " on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.potype=?" + subquery;

				if (mode.equalsIgnoreCase("buyer")) {
					po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where pd.CONTACTPERSONEMAILID=? and pd.potype=?"
							+ subquery;
				} else if (mode.equalsIgnoreCase("enduser")) {
					po_data1 = "Select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where pd.REQUSITIONER=? and pd.potype=? " + subquery
							+" Union "
							+ "select pd.status ,count(distinct pd.ponumber) as count from podetails pd, poeventdetails poe, "
							+ "locationmaster lm "
							+ "where lm.LOCATIONCODE = poe.DELVPLANT and pd.PONUMBER = poe.PONUMBER and "
							+" lm.STOREKEEPEREMILID = ? and pd.potype = ? " + subquery;
							
				} else if (mode.equalsIgnoreCase("payer")) {
					po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
							+ " on pd.PONUMBER = poe.PONUMBER where  pd.potype=?" + subquery;
				}

				log.info("counts po_data1--"+po_data1);
				
				ps = con.prepareStatement(po_data1);
				queryCount = 0;
				if (!mode.equalsIgnoreCase("payer")) {
					queryCount++;
					ps.setString(queryCount, email1);
					queryCount++;
					ps.setString(queryCount, "S");
				} else {
					queryCount++;
					ps.setString(queryCount, "S");
				}

				if (!plant.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, plant);
				}
				if (!vendor.equalsIgnoreCase("NA")) {
					queryCount++;
					ps.setString(queryCount, vendor);
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
				if (!mode.equalsIgnoreCase("payer")) {
					queryCount++;
					ps.setString(queryCount, email1);
					queryCount++;
					ps.setString(queryCount, "S");
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
				*/
			}

			if (!countAsPerStatus.isEmpty()) {
				responsejson.put("poCountAsPerStatus", countAsPerStatus);
			}
		} catch (Exception e) {
			log.error("getinternalPODetailsCountAsPerStatus() :", e.fillInStackTrace());

		}
		return null;

	}

	public JSONArray getInternalPonInvoiceSummeryCountsAsPerStatus(String emailid, int nPage, String status,
			String invno, String pono, String fdate, String tdate, String plant, String vendor, String mode,
			Connection con, PreparedStatement ps, ResultSet rs) throws SQLException {

		log.info("Inside getInternalPonInvoiceSummeryCountsAsPerStatus method " + status + " email - " + emailid
				+ " mode - " + mode);
		try {

			HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
			int allCounter = 0;

			if ((!status.equalsIgnoreCase("AS")) && (!status.equalsIgnoreCase("ASWP"))
					&& (!status.equalsIgnoreCase("ASSQ")) && (!status.equalsIgnoreCase("Invalid Invoices"))) {

				String invoice_data = "";

				if (mode.equalsIgnoreCase("buyer")) {
					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND " + "PIS.BUYER = ? "
							+ " and pis.overallstatus is not null " + "Group by pis.overallstatus";
				} else if (mode.equalsIgnoreCase("enduser")) {
					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND " + "PIS.REQUSITIONER = ? "
							+ " and pis.overallstatus is not null " + "Group by pis.overallstatus";
				} else if (mode.equalsIgnoreCase("payer")) {
					invoice_data = "SELECT pis.overallstatus ,count(pis.invoicenumber)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL " + " and pis.overallstatus is not null "
							+ " Group by pis.overallstatus";
				}

				ps = con.prepareStatement(invoice_data);
				if (!mode.equalsIgnoreCase("payer")) {
					ps.setString(1, emailid);
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

			} else {
				String subquery = "";
				if ((!status.equalsIgnoreCase("ASWP")) && (!status.equalsIgnoreCase("ASSQ"))
						&& (!status.equalsIgnoreCase("Invalid Invoices"))) {
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND PIS.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid = ?)";
						subquery = subquery + po;
					}
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
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND PIS.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid = ?)";
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
				} else if (status.equalsIgnoreCase("ASSQ")) {
					if (!vendor.equalsIgnoreCase("NA")) {
						String po = "AND PIS.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid = ?)";
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
				}

				else if (status.equalsIgnoreCase("Invalid Invoices")) {

					String in = " AND PIS.overallstatus = ? ";
					subquery = subquery + in;
				}

				String invoice_data = "";

				if (mode.equalsIgnoreCase("buyer")) {
					invoice_data = "SELECT pis.overallstatus ,count( pis.invoicenumber)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND " + "PIS.BUYER = ? "
							+ " and pis.overallstatus is not null " + subquery + "  Group by pis.overallstatus";
				} else if (mode.equalsIgnoreCase("enduser")) {
					invoice_data = "SELECT pis.overallstatus ,count( pis.invoicenumber)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL AND " + "PIS.REQUSITIONER = ?  "
							+ " and pis.overallstatus is not null " + subquery + " Group by pis.overallstatus";
				} else if (mode.equalsIgnoreCase("payer")) {
					invoice_data = "SELECT pis.overallstatus ,count( pis.invoicenumber)as count FROM PONINVOICESUMMERY pis WHERE "
							+ "PIS.INVOICENUMBER IS NOT NULL " + " and pis.overallstatus is not null " + subquery
							+ " Group by pis.overallstatus";
				}

				ps = con.prepareStatement(invoice_data);
				int queryCounter = 0;
				if (!mode.equalsIgnoreCase("payer")) {
					queryCounter++;
					ps.setString(queryCounter, emailid);
				}

				if ((!status.equalsIgnoreCase("ASWP")) && (!status.equalsIgnoreCase("ASSQ"))
						&& (!status.equalsIgnoreCase("Invalid Invoices"))) {
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
				} else if (status.equalsIgnoreCase("Invalid Invoices")) {

					queryCounter++;
					ps.setString(queryCounter, "INV");

				}

				rs = ps.executeQuery();
				while (rs.next()) {
					String sts = rs.getString("overallstatus");
					String count = rs.getString("count");
					countAsPerStatus.put(sts, count);
					allCounter += Integer.parseInt(count);
				}

				if (status.equalsIgnoreCase("Invalid Invoices") && countAsPerStatus.isEmpty()) {
					log.info("No Invalid Invoices Found!!");
					countAsPerStatus.put("INV", "0");
				}

				if (!status.equalsIgnoreCase("Invalid Invoices")) {
					countAsPerStatus.put("ALL", allCounter + "");
				}

				rs.close();
				ps.close();

			}

			if (!status.equalsIgnoreCase("Invalid Invoices")) {

				String invoice_data = "select count(*) as count from poninvoicesummery where onexstatus = ? AND BUSINESSPARTNEROID = ? "
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
			e.printStackTrace();
			log.error("getInternalPonInvoiceSummeryCountsAsPerStatus() :", e.fillInStackTrace());
		}

		if (status.equalsIgnoreCase("Invalid Invoices")) {
			return jsonArray;
		}

		return null;
	}

	public JSONArray getinvoicebasedonemailidCountAsPerStatus(String emailId, HttpSession session, int nPage,
			String status, String pono, String invno, String fdate, String tdate, String plant, String vendor,
			Connection con, PreparedStatement ps, ResultSet rs) throws SQLException {
		String storeKepeer = (String) session.getAttribute("shopkepeer");

		try {

			HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
			int allCounter = 0;
			List<HashMap<String, String>> hashlist = new ArrayList<HashMap<String, String>>();
			if (!status.equalsIgnoreCase("AS")) {

				String invoice_data = "";

				if (storeKepeer.equalsIgnoreCase("true")) {
					invoice_data = "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber   AND A.ENDUSEID = ? "
							+ "AND A.PROXY IS NULL Group by B.overallstatus " + "UNION "
							+ "SELECT  B1.OVERALLSTATUS, count(DISTINCT B1.INVOICENUMBER) as count FROM INVOICEAPPROVAL A1 "
							+ "JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS NOT LIKE 'C%' "
							+ "AND B1.GRNNUMBER IS NOT NULL Group by B1.overallstatus " + "UNION "
							+ "SELECT  B1.OVERALLSTATUS, count(DISTINCT B1.INVOICENUMBER) as count FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%' Group by B1.overallstatus "
							+ "UNION "
							+ "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  "
							+ "ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X'  JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
							+ "Group by B.overallstatus";

					if ("C".equalsIgnoreCase(status)) {
						invoice_data = "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ "A.InvoiceNumber=B.InvoiceNumber AND " + "A.PONumber=B.PONumber   AND A.ENDUSEID = ? "
								+ "AND A.PROXY IS NULL AND CREDITADVICENO IS NOT NULL and B.overallstatus is not null "
								+ "and B.invoicenumber is not null " + "Group by B.overallstatus " + "UNION "
								+ "SELECT B1.OVERALLSTATUS,count(B1.INVOICENUMBER) as count "
								+ "FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) "
								+ "AND A1.STATUS NOT LIKE 'C%' And B1.invoicenumber is not null "
								+ "AND B1.GRNNUMBER IS NOT NULL AND B1.overallstatus is not null and "
								+ "CREDITADVICENO IS NOT NULL Group by B1.overallstatus " + "UNION "
								+ "SELECT B1.OVERALLSTATUS,count(B1.INVOICENUMBER) as count "
								+ "FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ?) AND A1.STATUS LIKE 'C%'  "
								+ "And B1.invoicenumber is not null AND B1.overallstatus is not null AND "
								+ "CREDITADVICENO IS NOT NULL Group by B1.overallstatus " + "UNION"
								+ " SELECT B.OVERALLSTATUS,count(B.INVOICENUMBER) as count FROM INVOICEAPPROVAL A "
								+ "JOIN PONINVOICESUMMERY B  ON  A.InvoiceNumber=B.InvoiceNumber AND "
								+ "A.PONumber=B.PONumber  AND A.PROXY = 'X' And B.invoicenumber is not null AND B.overallstatus is not null "
								+ "AND  CREDITADVICENO IS NOT NULL JOIN "
								+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT AND USERID=? "
								+ "Group by B.overallstatus";
					}
				} else {
					invoice_data = "SELECT B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER) as count "
							+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
							+ "AND B.overallstatus is not null and B.invoicenumber is not null "
							+ "Group by B.overallstatus " + "UNION "
							+ "SELECT  B1.OVERALLSTATUS,count(DISTINCT B1.INVOICENUMBER) as count FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL) "
							+ "AND B1.overallstatus is not null and B1.invoicenumber is not null "
							+ "Group by B1.overallstatus " + "UNION "
							+ "SELECT  B1.OVERALLSTATUS,count(DISTINCT B1.INVOICENUMBER) as count FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
							+ "A1.PONumber=B1.PONumber  AND (A1.EUMANAGER = ? ) AND "
							+ "B1.overallstatus is not null and B1.invoicenumber is not null AND "
							+ "(A1.STATUS LIKE 'C%' ) Group by B1.overallstatus";
					if ("C".equalsIgnoreCase(status)) {
						invoice_data = "SELECT  B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER) as count  "
								+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
								+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ? "
								+ "AND B.INVOICENUMBER IS NOT NULL AND B.CREDITADVICENO IS NOT NULL and B.overallstatus is not null "
								+ "AND A.PROXY IS NULL Group by B.overallstatus  " + "UNION "
								+ "SELECT  B1.OVERALLSTATUS,count(B1.INVOICENUMBER) as count "
								+ "FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B1  "
								+ "ON  A1.InvoiceNumber=B1.InvoiceNumber AND B1.INVOICENUMBER IS NOT NULL "
								+ "and B1.overallstatus is not null "
								+ "AND A1.PONumber=B1.PONumber  AND A1.EUMANAGER = ?   AND "
								+ "(B1.CREDITADVICENO IS NOT NULL AND A1.STATUS NOT LIKE 'C%' AND B1.GRNNUMBER IS NOT NULL) "
								+ "Group by B1.overallstatus " + "UNION "
								+ "SELECT  B1.OVERALLSTATUS,count(B1.INVOICENUMBER) as count " + "FROM INVOICEAPPROVAL "
								+ "A1 JOIN PONINVOICESUMMERY B1  ON  A1.InvoiceNumber=B1.InvoiceNumber AND "
								+ "A1.PONumber=B1.PONumber  AND A1.EUMANAGER = ? AND B1.INVOICENUMBER IS NOT NULL  "
								+ "and B1.overallstatus is not null AND "
								+ "(B1.CREDITADVICENO IS NOT NULL  AND A1.STATUS LIKE 'C%') "
								+ "Group by B1.overallstatus ";
					}
				}

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
					String po = " AND B.PONUMBER=(select DISTINCT C.BASEPO from poninvoicesummery C where A.ponumber=C.ponumber and A.invoicenumber=C.invoicenumber and A.invoicedate=C.invoicedate and A.businessparteroid=C.businessparteroid and C.ponumber=?)";
					subquery = subquery + po;
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
					invoice_data = "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber   AND A.ENDUSEID = ? "
							+ "AND A.PROXY IS NULL " + subquery + " Group by B.overallstatus " + "UNION "
							+ "SELECT  B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL A1 "
							+ "JOIN PONINVOICESUMMERY B ON  A1.InvoiceNumber=B.InvoiceNumber AND "
							+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ?) " + subquery
							+ " AND A1.STATUS NOT LIKE 'C%' " + "AND B.GRNNUMBER IS NOT NULL Group by B.overallstatus "
							+ "UNION "
							+ "SELECT  B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
							+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ?) " + subquery
							+ " AND A1.STATUS LIKE 'C%' Group by B.overallstatus " + "UNION "
							+ "SELECT B.OVERALLSTATUS, count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  "
							+ "ON  A.InvoiceNumber=B.InvoiceNumber AND "
							+ "A.PONumber=B.PONumber  AND A.PROXY = 'X'  JOIN "
							+ "INVENTORYUSERLIST inv ON inv.MTYP = B.MATERIAL_TYPE AND inv.PLANT = B.PLANT " + subquery
							+ " AND USERID=? " + "Group by B.overallstatus";

				} else {
					invoice_data = "SELECT B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER) as count "
							+ "FROM INVOICEAPPROVAL A JOIN PONINVOICESUMMERY B  ON  "
							+ "A.InvoiceNumber=B.InvoiceNumber AND A.PONumber=B.PONumber  AND A.ENDUSEID = ?  "
							+ "AND B.overallstatus is not null and B.invoicenumber is not null " + subquery + " "
							+ "Group by B.overallstatus " + "UNION "
							+ "SELECT  B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
							+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ? )  AND "
							+ "(A1.STATUS NOT LIKE 'C%' AND B.GRNNUMBER IS NOT NULL) "
							+ "AND B.overallstatus is not null and B.invoicenumber is not null " + subquery + " "
							+ "Group by B.overallstatus " + "UNION "
							+ "SELECT  B.OVERALLSTATUS,count(DISTINCT B.INVOICENUMBER) as count FROM INVOICEAPPROVAL "
							+ "A1 JOIN PONINVOICESUMMERY B  ON  A1.InvoiceNumber=B.InvoiceNumber AND "
							+ "A1.PONumber=B.PONumber  AND (A1.EUMANAGER = ? ) AND "
							+ "B.overallstatus is not null and B.invoicenumber is not null " + subquery + " AND"
							+ "(A1.STATUS LIKE 'C%' ) Group by B.overallstatus ";
				}

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
			responsejson.put("invoiceCountAsPerStatus", countAsPerStatus);
		} catch (Exception e) {
			log.error("getinvoicebasedonemailidCountAsPerStatus() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return null;
	}

	private JSONArray getinvoicepofordashboardCountAsPerStatus(String invoicenumber, String ponumber, String status,
			String fromdurationdate, String todurationdate, String plant, String purchasegroup,
			String requisitioneremailid, String frominvpoamount, String toinvpoamount, String ageingfrom,
			String ageingto, String emailId, String vendor, Connection con, PreparedStatement ps, ResultSet rs,
			String type) {

		ArrayList<String> param = new ArrayList<String>();
		String subquery = "";
		int pages = 0;
		ArrayList<HashMap<String, String>> POListEvent = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> countAsPerStatus = new HashMap<String, String>();
		int allCounter = 0;
		List<HashMap<String, String>> hashlist = new ArrayList<HashMap<String, String>>();

		String inv = "";
		String po_data = "";
		int queryCounter = 0;

		try {
			if (type.equalsIgnoreCase("invoice")) {
				if (!status.equalsIgnoreCase("NA")) {
					if ("C".equalsIgnoreCase(status)) {
						String po = " AND pis.CREDITADVICENO IS NOT NULL";
						subquery = subquery + po;
					} else if ("ALL".equalsIgnoreCase(status)) {
						String po = " ";
						subquery = subquery + po;
					} else if ("P".equalsIgnoreCase(status)) {
						String po = " AND (pis.OVERALLSTATUS=? OR  pis.OVERALLSTATUS=?)";
						subquery = subquery + po;
					} else {
						String po = " AND pis.OVERALLSTATUS=?";
						subquery = subquery + po;
					}

				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND pis.PONUMBER=?";
					subquery = subquery + po;
				}
				if (!vendor.equalsIgnoreCase("NA")) {
					String po = " AND pis.BUSINESSPARTNEROID IN (SELECT BUSINESSPARTNEROID FROM businesspartner where vendorid=?)";
					subquery = subquery + po;
				}
				if (!invoicenumber.equalsIgnoreCase("NA")) {
					String in = " AND pis.INVOICENUMBER=?";
					subquery = subquery + in;
				}
				if ((!fromdurationdate.equalsIgnoreCase("NA"))
						&& (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
					String dt = " AND pis.INVOICEDATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + dt;
				}
				if (!plant.equalsIgnoreCase("NA")) {
					String pt = " AND pis.PLANT=?";
					subquery = subquery + pt;
				}
				if (!purchasegroup.equalsIgnoreCase("NA")) {
					String pg = " AND pis.MATERIAL_TYPE=?";
					subquery = subquery + pg;
				}
				if (!requisitioneremailid.equalsIgnoreCase("NA")) {
					String re = " AND pis.REQUSITIONER=LOWER(?) ";
					subquery = subquery + re;
				}
				if (!frominvpoamount.equalsIgnoreCase("NA")) {
					String ia = "  AND pis.AMOUNT BETWEEN ? AND ? ";
					subquery = subquery + ia;
				}
				if (!ageingfrom.equalsIgnoreCase("NA")) {
					String ia = "";
					if (!status.equalsIgnoreCase("NA")) {
						if ("P".equalsIgnoreCase(status)) {
							ia = "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') > ?";
							if (!ageingto.equalsIgnoreCase("NA")) {
								ia = ia + "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') < ?";
							}
						}

						else {
							ia = "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') > ?";
							if (!ageingto.equalsIgnoreCase("NA")) {
								ia = ia + "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') < ?";
							}
						}
					}
					subquery = subquery + ia;
				}

				inv = "SELECT pis.overallstatus ,count(pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis WHERE "
						+ "PIS.INVOICENUMBER IS NOT NULL " + subquery
						+ " and pis.overallstatus is not null and  pis.mpo is null " + "Group by pis.overallstatus "
						+ "union all "
						+ "SELECT pis.overallstatus ,count(Distinct pis.invoicenumber || '-' || pis.invoicedate||'-'||pis.businesspartneroid)as count FROM PONINVOICESUMMERY pis WHERE "
						+ "PIS.INVOICENUMBER IS NOT NULL  " + subquery
						+ "  and pis.overallstatus is not null and  pis.mpo ='Y' " + "Group by pis.overallstatus";

				ps = con.prepareStatement(inv);

				if (!status.equalsIgnoreCase("NA")) {
					if ("C".equalsIgnoreCase(status)) {

					} else if ("ALL".equalsIgnoreCase(status)) {

					} else if ("P".equalsIgnoreCase(status)) {
						String po = " AND (pis.OVERALLSTATUS=? OR  pis.OVERALLSTATUS=?)";
						queryCounter++;
						ps.setString(queryCounter, "P");
						queryCounter++;
						ps.setString(queryCounter, "M");
					} else {
						String po = " AND pis.OVERALLSTATUS=?";
						queryCounter++;
						ps.setString(queryCounter, status);
					}

				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND pis.PONUMBER=?";
					queryCounter++;
					ps.setString(queryCounter, ponumber);
				}
				if (!vendor.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, vendor);
				}
				if (!invoicenumber.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, invoicenumber);
				}
				if ((!fromdurationdate.equalsIgnoreCase("NA"))
						&& (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
					queryCounter++;
					ps.setString(queryCounter, fromdurationdate);
					queryCounter++;
					ps.setString(queryCounter, todurationdate);
				}
				if (!plant.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, plant);
				}
				if (!purchasegroup.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, purchasegroup);
				}
				if (!requisitioneremailid.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, requisitioneremailid);
				}
				if (!frominvpoamount.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, frominvpoamount);
					queryCounter++;
					ps.setString(queryCounter, toinvpoamount);
				}
				if (!ageingfrom.equalsIgnoreCase("NA")) {
					String ia = "";
					if (!status.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, ageingfrom);
						if ("P".equalsIgnoreCase(status)) {
							ia = "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') > ?";
							if (!ageingto.equalsIgnoreCase("NA")) {
								queryCounter++;
								ps.setString(queryCounter, ageingto);
							}
						}

						else {
							ia = "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') > ?";
							if (!ageingto.equalsIgnoreCase("NA")) {
								ia = ia + "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') < ?";
								queryCounter++;
								ps.setString(queryCounter, ageingto);
							}
						}
					}
				}
				if (!status.equalsIgnoreCase("NA")) {
					if ("C".equalsIgnoreCase(status)) {

					} else if ("ALL".equalsIgnoreCase(status)) {

					} else if ("P".equalsIgnoreCase(status)) {
						String po = " AND (pis.OVERALLSTATUS=? OR  pis.OVERALLSTATUS=?)";
						queryCounter++;
						ps.setString(queryCounter, "P");
						queryCounter++;
						ps.setString(queryCounter, "M");
					} else {
						String po = " AND pis.OVERALLSTATUS=?";
						queryCounter++;
						ps.setString(queryCounter, status);
					}

				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND pis.PONUMBER=?";
					queryCounter++;
					ps.setString(queryCounter, ponumber);
				}
				if (!vendor.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, vendor);
				}
				if (!invoicenumber.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, invoicenumber);
				}
				if ((!fromdurationdate.equalsIgnoreCase("NA"))
						&& (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
					queryCounter++;
					ps.setString(queryCounter, fromdurationdate);
					queryCounter++;
					ps.setString(queryCounter, todurationdate);
				}
				if (!plant.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, plant);
				}
				if (!purchasegroup.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, purchasegroup);
				}
				if (!requisitioneremailid.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, requisitioneremailid);
				}
				if (!frominvpoamount.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, frominvpoamount);
					queryCounter++;
					ps.setString(queryCounter, toinvpoamount);
				}
				if (!ageingfrom.equalsIgnoreCase("NA")) {
					String ia = "";
					if (!status.equalsIgnoreCase("NA")) {
						queryCounter++;
						ps.setString(queryCounter, ageingfrom);
						if ("P".equalsIgnoreCase(status)) {
							ia = "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') > ?";
							if (!ageingto.equalsIgnoreCase("NA")) {
								queryCounter++;
								ps.setString(queryCounter, ageingto);
							}
						}

						else {
							ia = "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') > ?";
							if (!ageingto.equalsIgnoreCase("NA")) {
								ia = ia + "  AND sysdate - TO_DATE(pis.MODIFIEDON, 'dd-mm-yy') < ?";
								queryCounter++;
								ps.setString(queryCounter, ageingto);
							}
						}
					}
				}

			} else if (type.equalsIgnoreCase("po")) {
				if (!vendor.equalsIgnoreCase("NA")) {
					String po = " AND pd.VENDORID=?";
					subquery = subquery + po;
				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND pd.PONUMBER=?";
					subquery = subquery + po;
				}
				if ((!fromdurationdate.equalsIgnoreCase("NA"))
						&& (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
					String in = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + " AND TO_DATE(?, 'DD/MM/YYYY')";
					subquery = subquery + in;
				}
				if (!status.equalsIgnoreCase("NA") && !status.equalsIgnoreCase("ALL")) {
					String dt = " AND pd.STATUS=?";
					subquery = subquery + dt;
				}
				if (!plant.equalsIgnoreCase("NA")) {
					String dt = " AND poe.PLANT =?";
					subquery = subquery + dt;
				}
				if (!purchasegroup.equalsIgnoreCase("NA")) {
					String dt = " AND poe.MATERIAL_TYPE=?";
					subquery = subquery + dt;
				}
				if (!requisitioneremailid.equalsIgnoreCase("NA")) {
					String dt = " AND pd.REQUSITIONER = LOWER(?)";
					subquery = subquery + dt;
				}
				if (!frominvpoamount.equalsIgnoreCase("NA")) {
					String ia = "  AND pd.POAMOUNT BETWEEN ? AND ? ";
					subquery = subquery + ia;
				}
				if (!ageingfrom.equalsIgnoreCase("NA")) {
					String ia = "  AND sysdate - TO_DATE(pd.MODIFIEDON, 'dd-mm-yy') > ?";
					if (!ageingto.equalsIgnoreCase("NA")) {
						ia = ia + "  AND sysdate - TO_DATE(pd.MODIFIEDON, 'dd-mm-yy') < ?";
					}
					subquery = subquery + ia;
				}

				po_data = "select pd.status as overallstatus,count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
						+ " on pd.PONUMBER = poe.PONUMBER  " + subquery + " group by pd.status";

				ps = con.prepareStatement(po_data);

				if (!vendor.equalsIgnoreCase("NA")) {
					String po = " AND pd.VENDORID=?";
					queryCounter++;
					ps.setString(queryCounter, vendor);

				}
				if (!ponumber.equalsIgnoreCase("NA")) {
					String po = " AND pd.PONUMBER=?";
					queryCounter++;
					ps.setString(queryCounter, ponumber);

				}
				if ((!fromdurationdate.equalsIgnoreCase("NA"))
						&& (!fromdurationdate.equalsIgnoreCase("Invalid date"))) {
					String in = " AND pd.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') " + " AND TO_DATE(?, 'DD/MM/YYYY')";
					queryCounter++;
					ps.setString(queryCounter, fromdurationdate);
					queryCounter++;
					ps.setString(queryCounter, todurationdate);
				}
				if (!status.equalsIgnoreCase("NA") && !status.equalsIgnoreCase("ALL")) {
					String dt = " AND pd.STATUS=?";
					queryCounter++;
					ps.setString(queryCounter, status);
				}
				if (!plant.equalsIgnoreCase("NA")) {
					String dt = " AND poe.PLANT =?";
					queryCounter++;
					ps.setString(queryCounter, plant);
				}
				if (!purchasegroup.equalsIgnoreCase("NA")) {
					String dt = " AND poe.MATERIAL_TYPE=?";
					queryCounter++;
					ps.setString(queryCounter, purchasegroup);
				}
				if (!requisitioneremailid.equalsIgnoreCase("NA")) {
					String dt = " AND pd.REQUSITIONER = LOWER(?)";
					queryCounter++;
					ps.setString(queryCounter, requisitioneremailid);
				}
				if (!frominvpoamount.equalsIgnoreCase("NA")) {
					String ia = "  AND pd.POAMOUNT BETWEEN ? AND ? ";
					queryCounter++;
					ps.setString(queryCounter, frominvpoamount);
					queryCounter++;
					ps.setString(queryCounter, toinvpoamount);
				}
				if (!ageingfrom.equalsIgnoreCase("NA")) {
					queryCounter++;
					ps.setString(queryCounter, ageingfrom);
					String ia = "  AND sysdate - TO_DATE(pd.MODIFIEDON, 'dd-mm-yy') > ?";
					if (!ageingto.equalsIgnoreCase("NA")) {
						ia = ia + "  AND sysdate - TO_DATE(pd.MODIFIEDON, 'dd-mm-yy') < ?";
						queryCounter++;
						ps.setString(queryCounter, ageingto);
					}
				}

			}
			log.info("counts po_data--"+po_data);
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
		} catch (Exception e) {
			log.error("getinvoicepofordashboardCountAsPerStatus() 1 :", e.fillInStackTrace());

		}

		String po_data1 = "select count(distinct pd.ponumber) as count from podetails pd join poeventdetails poe"
				+ " on pd.PONUMBER = poe.PONUMBER where  pd.potype=?";

		try {
			log.info("counts po_data1--"+po_data1);
			ps = con.prepareStatement(po_data1);
			ps.setString(1, "S");
			rs = ps.executeQuery();
			while (rs.next()) {
				String count = rs.getString("count");
				countAsPerStatus.put("SP", count);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			log.error("getinvoicepofordashboardCountAsPerStatus() 2 :", e.fillInStackTrace());

		}

		if (!countAsPerStatus.isEmpty()) {
			responsejson.put("invoiceCountAsPerStatus", countAsPerStatus);
		}

		return jsonArray;

	}

	private HashMap<String, String> getactualcountafterremovinggrn(HashMap<String, String> countAsPerStatus,
			String emailId, List<HashMap<String, String>> hashlist, Connection con) {

		String sql = "Select count(*)  as counter from invoiceapproval where status IN ('CM','CO') "
				+ "and invoicenumber =? and ponumber = ?";

		String sql1 = "Select STATUS,EUMANAGER  from invoiceapproval where status IN (?,?) "
				+ "and invoicenumber = ? and ponul̥mber = ? group by STATUS,EUMANAGER";
		HashMap<String, String> countAsPerStatusnew = new HashMap<String, String>();
		hashlist.forEach(item -> {
			String invnumb = item.get("INVOICENUMBER");
			String ponum = item.get("PONUMBER");
			String ovstatus = item.get("OVERALLSTATUS");
			String grnnum = item.get("GRNNUMBER") == null ? "" : item.get("GRNNUMBER");
			int count = 0;
			String EUMANAGER = "";
			String STATUS = "";
			String counterofstatus = "1";
			if (grnnum.equalsIgnoreCase(null) || "".equalsIgnoreCase(grnnum) || "null".equalsIgnoreCase(grnnum)
					|| grnnum == null) {
				try {
					PreparedStatement ps = null;
					ResultSet rs = null;
					ps = con.prepareStatement(sql);
					ps.setString(1, invnumb);
					ps.setString(2, ponum);
					rs = ps.executeQuery();
					while (rs.next()) {
						count = Integer.parseInt(rs.getString("counter"));
					}
					rs.close();
					ps.close();
					if (count > 0) {
						PreparedStatement ps1 = null;
						ResultSet rs1 = null;
						ps = con.prepareStatement(sql1);
						ps.setString(1, invnumb);
						ps.setString(2, ponum);
						ps.setString(3, emailId);
						rs = ps.executeQuery();
						while (rs.next()) {
							EUMANAGER = rs.getString("EUMANAGER");
							STATUS = rs.getString("STATUS");
						}
						if ("O".equalsIgnoreCase(STATUS) || "P".equalsIgnoreCase(STATUS)
								|| "M".equalsIgnoreCase(STATUS)) {
							String sts = ovstatus;
							if (countAsPerStatusnew.containsKey(sts)) {
								int sum = Integer.parseInt(countAsPerStatusnew.get(sts))
										+ Integer.parseInt(counterofstatus);
								countAsPerStatusnew.put(sts, sum + "");
							} else {
								countAsPerStatusnew.put(sts, counterofstatus);
							}
						}
						rs.close();
						ps.close();
					}

					countAsPerStatusnew.entrySet().stream()
							.forEach(value -> log.info(value.getKey() + ":" + value.getValue()));
				} catch (Exception e) {
					log.error("getactualcountafterremovinggrn() :", e.fillInStackTrace());

				}
			}
		});

		return null;
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

	/*
	 * public JSONArray getPODetails(String email1, String mode, String status, int
	 * nPage, String ponumber, String fromdateofduration, String todateofduration,
	 * String fromdateofpo, String todateofpo, String plant, String vendor) throws
	 * SQLException {
	 * 
	 * String poUserQuery = ""; String poDataQuery = null; String poDateSubquery =
	 * "";
	 * 
	 * log.info("getPODetails() MODE : " + mode); if
	 * (mode.equalsIgnoreCase("buyer")) { poUserQuery = poUserQuery +
	 * "A.CONTACTPERSONEMAILID = ? AND "; } else if
	 * (mode.equalsIgnoreCase("enduser")) { poUserQuery = poUserQuery +
	 * "A.REQUSITIONER = ? AND "; } else if (mode.equalsIgnoreCase("payer")) {
	 * poUserQuery = ""; }
	 * 
	 * ArrayList<String> param = new ArrayList<String>();
	 * 
	 * if (status.equalsIgnoreCase("ALL")) {
	 * 
	 * if (!mode.equalsIgnoreCase("payer")) { param.add(email1); param.add("N"); }
	 * else { param.add("N"); }
	 * 
	 * if (!plant.equalsIgnoreCase("NA")) { String po = " AND B.PLANT=?";
	 * poDateSubquery = poDateSubquery + po; param.add(plant); }
	 * 
	 * if (!vendor.equalsIgnoreCase("NA")) { String po = " AND A.VENDORID=?";
	 * poDateSubquery = poDateSubquery + po; param.add(vendor); }
	 * 
	 * if (!ponumber.equalsIgnoreCase("NA")) { String po = " AND A.PONUMBER=?";
	 * poDateSubquery = poDateSubquery + po; param.add(ponumber);
	 * 
	 * } if ((!fromdateofduration.equalsIgnoreCase("NA")) &&
	 * (!fromdateofduration.equalsIgnoreCase("Invalid date"))) { String in =
	 * " AND A.CREATEDON BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')"
	 * ; poDateSubquery = poDateSubquery + in; param.add(fromdateofduration);
	 * param.add(todateofduration); } if ((!fromdateofpo.equalsIgnoreCase("NA")) &&
	 * (!fromdateofpo.equalsIgnoreCase("Invalid date"))) { String dt =
	 * " AND A.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')"
	 * ; poDateSubquery = poDateSubquery + dt; param.add(fromdateofpo);
	 * param.add(todateofpo); }
	 * 
	 * if (!mode.equalsIgnoreCase("payer")) { param.add(email1); param.add("N"); }
	 * else { param.add("N"); }
	 * 
	 * if (!plant.equalsIgnoreCase("NA")) { param.add(plant); }
	 * 
	 * if (!vendor.equalsIgnoreCase("NA")) { param.add(vendor); } if
	 * (!ponumber.equalsIgnoreCase("NA")) { param.add(ponumber); } if
	 * ((!fromdateofduration.equalsIgnoreCase("NA")) &&
	 * (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
	 * param.add(fromdateofduration); param.add(todateofduration); } if
	 * ((!fromdateofpo.equalsIgnoreCase("NA")) &&
	 * (!fromdateofpo.equalsIgnoreCase("Invalid date"))) { param.add(fromdateofpo);
	 * param.add(todateofpo); }
	 * 
	 * poDataQuery = "select * from ( " +
	 * "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
	 * +
	 * "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
	 * + "FROM podetails A,poeventdetails B " + "WHERE " + poUserQuery +
	 * " A.PONUMBER=B.PONUMBER and A.Status = ? " + poDateSubquery + " " + "Union "
	 * +
	 * "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
	 * +
	 * "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
	 * + "FROM podetails A,poeventdetails B " + "WHERE " + poUserQuery +
	 * " A.PONUMBER=B.PONUMBER and A.Status <> ? " + poDateSubquery + " " +
	 * ") ORDER BY 1,4";
	 * 
	 * } else {
	 * 
	 * if (status.equalsIgnoreCase("SP")) { String po = "and A.POTYPE =? ";
	 * poDateSubquery = poDateSubquery + po;
	 * 
	 * if (!mode.equalsIgnoreCase("payer")) { param.add(email1); param.add("S"); }
	 * else { param.add("S"); }
	 * 
	 * } else { String po = "and A.Status =? "; poDateSubquery = poDateSubquery +
	 * po; if (!mode.equalsIgnoreCase("payer")) { param.add(email1);
	 * param.add(status); } else { param.add(status); } }
	 * 
	 * if (!plant.equalsIgnoreCase("NA")) { String po = " AND B.PLANT=?";
	 * poDateSubquery = poDateSubquery + po; param.add(plant); }
	 * 
	 * if (!vendor.equalsIgnoreCase("NA")) { String po = " AND A.VENDORID=?";
	 * poDateSubquery = poDateSubquery + po; param.add(vendor); }
	 * 
	 * if (!ponumber.equalsIgnoreCase("NA")) { String po = " AND A.PONUMBER=?";
	 * poDateSubquery = poDateSubquery + po; param.add(ponumber);
	 * 
	 * } if ((!fromdateofduration.equalsIgnoreCase("NA")) &&
	 * (!fromdateofduration.equalsIgnoreCase("Invalid date"))) { String in =
	 * " AND A.CREATEDON BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')"
	 * ; poDateSubquery = poDateSubquery + in; param.add(fromdateofduration);
	 * param.add(todateofduration); } if ((!fromdateofpo.equalsIgnoreCase("NA")) &&
	 * (!fromdateofpo.equalsIgnoreCase("Invalid date"))) { String dt =
	 * " AND A.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')"
	 * ; poDateSubquery = poDateSubquery + dt; param.add(fromdateofpo);
	 * param.add(todateofpo); }
	 * 
	 * poDataQuery =
	 * "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
	 * +
	 * "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
	 * + "FROM podetails A,poeventdetails B " + "WHERE " + poUserQuery +
	 * " A.PONUMBER=B.PONUMBER " + poDateSubquery + " " + "ORDER BY 1,4";
	 * 
	 * }
	 * 
	 * log.info("poDataQuery = " +poDataQuery);
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
	 * POImpl poimpl = new POImpl(); String encodedfile =
	 * poimpl.writeintoexcelfile(poDataList);
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
	public JSONArray downloadapprovalinvoicelist(String emailId, String status, String invno, String pono, String fdate,
			String tdate, String plant, String vendor, String page, String mode) throws SQLException {

		String downloadquery = " SELECT DISTINCT "
				+ " po.ponumber                                             ponumber, "
				+ " po.invoicenumber                                        invoicenumber, "
				+ " po.expensesheetid                                        expensesheetid, "
				+ " TRIM(to_char((po.paymentamount), '999,999,999,999,999')) AS paymentamount, "
				+ " to_char((po.invoicedate), 'DD-MON-RRRR')                invoicedate, "
				+ " bp.businesspartnertext                                   businesspartnertext, "
				+ " po.requsitioner                                             enduseid, "
				+ " (SELECT max(ip.enduserstatus) FROM invoiceapproval ip  WHERE "
				+"  ip.invoicenumber = po.invoicenumber AND ip.ponumber =po.ponumber) enduserstatus, "
				+ " po.overallstatus                                         overallstatus, "
				+ " to_char(po.createdon, 'DD-MON-RRRR')                   createdon, "
				+ " TRIM(to_char(po.amount, '999,999,999,999,999'))        AS amount, "
				+ " (SELECT max(ip.proxy) FROM invoiceapproval ip  WHERE ip.invoicenumber = po.invoicenumber AND ip.ponumber =po.ponumber) proxyb, "
				+ " po.businesspartneroid                                    businesspartneroid, "
				+ " po.grnnumber                                             grnnumber, "
				+ " po.utrchequenumber                                       utrchequenumber, "
				+ " purchasinggroup                                       purchasinggroup, "
				+ " (SELECT MAX(to_char(gm.createdon, 'DD-MON-RRRR')) FROM grnmapping gm "
				+ "	 WHERE gm.invoicenumber = po.invoicenumber AND gm.ponumber = po.ponumber) grndate, "
				+ " to_char(po.paymentdate, 'DD-MON-RRRR')                     paymentdate, "
				+ " po.plant                                              plantcode, "
				+ " (SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = po.plant) plantname, "
				+ " (select ip.EUMANAGER  from CONFIRMERLIST ip where ip.invoicenumber = po.invoicenumber and ip.ponumber = po.ponumber ) confirmers,"
				+ " (select ip.EUMANAGER  from MANAGERLIST ip where ip.invoicenumber = po.invoicenumber and ip.ponumber = po.ponumber ) managers "
				+ " FROM  invoiceapproval   inv, " + " poninvoicesummery po, " + " businesspartner   bp, "
				+ " podetails pd  WHERE "
				+ " inv.invoicenumber = po.invoicenumber "
				+ " AND pd.ponumber = po.ponumber "
				+ " AND inv.ponumber = po.ponumber "
				+ " AND pd.ponumber = inv.ponumber "
				+ " AND po.businesspartneroid = bp.businesspartneroid "
				+ " AND inv.invoicenumber is not null ";
		
		List<List<String>> POListEvent = new ArrayList<List<String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			if (!"NA".equalsIgnoreCase(invno)) {
				downloadquery += " AND po.invoicenumber =  '" + invno + "'";
			}
			if (!"NA".equalsIgnoreCase(pono)) {
				downloadquery += " AND po.ponumber =  '" + pono + "'";
			}

			if ((!"NA".equalsIgnoreCase(fdate) && !"NA".equalsIgnoreCase(tdate))
					&& (!"Invalid date".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(tdate))) {
				String dt = " AND po.INVOICEDATE BETWEEN TO_DATE('" + fdate + "', 'DD/MM/YYYY') AND TO_DATE('" + tdate
						+ "', 'DD/MM/YYYY')";
				downloadquery += dt;
			}

			if ("C".equalsIgnoreCase(status) || "ALL".equalsIgnoreCase(status) || "AS".equalsIgnoreCase(status)) {

			} else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {

				if ("P".equalsIgnoreCase(status)) {

					downloadquery += " AND (po.OVERALLSTATUS = 'P' OR po.OVERALLSTATUS = 'M') ";

				} else if ("V".equalsIgnoreCase(status)) {

					downloadquery += " AND (po.OVERALLSTATUS = 'RO' OR po.OVERALLSTATUS = 'V') ";
				} else {
					downloadquery += " AND po.OVERALLSTATUS = '" + status + "' ";
				}
			}

			if (!"payer".equalsIgnoreCase(mode)) {
				if (emailId != null) {
					if("buyer".equalsIgnoreCase(mode)) {
						downloadquery += " AND po.BUYER = '"+emailId+ "' ";
					}else if ("enduser".equalsIgnoreCase(mode) && "view".equalsIgnoreCase(page)) {
						//downloadquery += " AND (po.REQUSITIONER = '" + emailId + "' or inv.EUMANAGER = '" + emailId + "') ";
						downloadquery += " AND po.REQUSITIONER = '" + emailId + "' ";
					}else if ("enduser".equalsIgnoreCase(mode) && "invalid".equalsIgnoreCase(page)) {
						//downloadquery += " AND (po.REQUSITIONER = '" + emailId + "' or inv.EUMANAGER = '" + emailId + "') ";
						downloadquery += " AND po.REQUSITIONER = '" + emailId + "' ";
					}
					else if ("Approval".equalsIgnoreCase(page)) {
						if (emailId != null) {

							downloadquery += " AND (inv.enduseid = '" + emailId + "' or inv.EUMANAGER = '" + emailId + "') ";
						}
					}

				}
			}

			if (!"NA".equalsIgnoreCase(plant)) {

				downloadquery += " AND (po.plant = '" + plant + "') ";
			}
			if (!"NA".equalsIgnoreCase(vendor)) {
				downloadquery += " AND (bp.VENDORID = '" + vendor + "') ";
			}

			log.info("download query :" + downloadquery);
			DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
			LocalDateTime myDateObj = LocalDateTime.now();
			String formattedDate = myDateObj.format(myFormatObj);
			log.info("Start Time: " + formattedDate);

			ps = con.prepareStatement(downloadquery);
			rs = ps.executeQuery();

			while (rs.next()) {
				List<String> poEvent = new ArrayList<String>();

				poEvent.add(rs.getString("INVOICENUMBER") == null ? "" : rs.getString("INVOICENUMBER"));
				poEvent.add(rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER"));
				poEvent.add(rs.getString("INVOICEDATE") == null ? "" : rs.getString("INVOICEDATE"));
				poEvent.add(rs.getString("CREATEDON") == null ? "" : rs.getString("CREATEDON"));
				poEvent.add(rs.getString("BUSINESSPARTNERTEXT") == null ? "" : rs.getString("BUSINESSPARTNERTEXT"));
				poEvent.add(rs.getString("ENDUSEID") == null ? "" : rs.getString("ENDUSEID"));

				String confirmers = "";
				// confirmers = getConfirmerlist(rs.getString("INVOICENUMBER"),
				// rs.getString("PONUMBER"), con).replace("_",",");
				confirmers = rs.getString("confirmers") == null ? "" : rs.getString("confirmers");
				if (confirmers == null || confirmers.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(confirmers);
				}
				String managers = "";
				// managers = getMGRlist(rs.getString("INVOICENUMBER"),
				// rs.getString("PONUMBER"), con).replace("_", ",");
				managers = rs.getString("managers") == null ? "" : rs.getString("managers");
				if (managers == null || managers.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(managers);
				}

				if (rs.getString("OVERALLSTATUS") == null) {
					poEvent.add("-");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("A")) {
					poEvent.add("Approved");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("M")
						|| rs.getString("OVERALLSTATUS").equalsIgnoreCase("P")) {
					poEvent.add("Pending");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("O")) {
					poEvent.add("On Hold");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("R")) {
					poEvent.add("Rejected");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("V")) {
					poEvent.add("Returned");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PRO")) {
					poEvent.add("Processed");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PD")) {
					poEvent.add("Paid");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PP")) {
					poEvent.add("Partially Paid");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("RO")) {
					poEvent.add("Reopened");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("INV")) {
					poEvent.add("Invalid Invoice");
				} else {
					poEvent.add("-");
				}
				poEvent.add(rs.getString("amount") == null ? "" : rs.getString("amount"));
				poEvent.add(rs.getString("PAYMENTAMOUNT") == null ? "" : rs.getString("PAYMENTAMOUNT"));
				poEvent.add(rs.getString("EXPENSESHEETID") == null ? "" : rs.getString("EXPENSESHEETID"));
				poEvent.add(rs.getString("UTRCHEQUENUMBER") == null ? "" : rs.getString("UTRCHEQUENUMBER"));
				poEvent.add(rs.getString("PAYMENTDATE") == null ? "" : rs.getString("PAYMENTDATE"));
				poEvent.add(rs.getString("PURCHASINGGROUP") == null ? "" : rs.getString("PURCHASINGGROUP"));
				poEvent.add(rs.getString("GRNNUMBER") == null ? "" : rs.getString("GRNNUMBER"));
				poEvent.add(rs.getString("GRNDATE") == null ? "" : rs.getString("GRNDATE"));
				String plantCode = rs.getString("PLANTCODE") == null ? "" : rs.getString("PLANTCODE");
				poEvent.add(plantCode);

				/*
				 * if ("".equalsIgnoreCase(plantCode)) { poEvent.add(""); } else { String
				 * plantName = getPlantName(rs.getString("PLANTNAME"), con); if (plantName ==
				 * null || plantName.equalsIgnoreCase("null")) { poEvent.add(""); } else {
				 * poEvent.add(plantName); } }
				 */
				String plantName = rs.getString("PLANTNAME");
				if (plantName == null || plantName.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(plantName);
				}

				POListEvent.add(poEvent);
			}
			rs.close();
			ps.close();
			//LocalDateTime myDateObj2 = LocalDateTime.now();
			//String formattedDate2 = myDateObj2.format(myFormatObj);
			//log.info("Mid Time: " + formattedDate2);

			if (POListEvent.size() > 0) {
				String encodedfile = downloadinernalfile(POListEvent);
				//String encodedfile = downloadInernalExcelfile(POListEvent);
				//LocalDateTime myDateObj3 = LocalDateTime.now();
				//String formattedDate3 = myDateObj3.format(myFormatObj);
				//log.info("End Time: " + formattedDate3);

				if (encodedfile.equalsIgnoreCase("")) {
					responsejson.put("message", "Fail");
				} else {
					responsejson.put("message", "Success");
					responsejson.put("data", encodedfile);
				}
			} else {
				responsejson.put("message", "Fail");
			}

			jsonArray.add(responsejson);
		} catch (Exception e) {
			log.info("downloadapprovalinvoicelist() :" + e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	private String downloadinernalfile(List<List<String>> totallist) {
		String encodedfile = "";
		try {

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
			CSVWriter writer = new CSVWriter(streamWriter);

			List<String[]> heading = new ArrayList<String[]>();

			String header = "INVOICE NUMBER" + "|" + "PO NUMBER" + "|" + "INVOICE DATE" + "|" + "CREATED DATE" + "|"
					+ "VENDOR NAME" + "|" + "REQUISITIONER/STOREKEEPER" + "|" + "GRN CONFIRMERS" + "|"
					+ "INVOICE APPROVERS" + "|" + "OVERALL STATUS" + "|" + "INVOICE AMOUNT" + "|" + "PAID AMOUNT" + "|"
					+ "EXPENSE SHEET ID" + "|" + "UTR NUMBER" + "|" + "PAYMENT DATE" + "|" + "PURCHASE GROUP" + "|"
					+ "GRN NUMBER" + "|" + "GRN DATE" + "|" + "PLANT CODE" + "|" + "PLANT NAME";

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
				log.error("downloadinernalfile() 1: ", e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("downloadinernalfile() 2: ", e.fillInStackTrace());

		}
		return encodedfile;
	}

	public JSONArray getPODetails(String email1, String mode, String status, int nPage, String ponumber,
			String fromdateofduration, String todateofduration, String fromdateofpo, String todateofpo, String plant,
			String vendor) throws SQLException {

		String poUserQuery = "";
		String poDataQuery = null;
		String poDateSubquery = "";

		log.info("getPODetails() MODE : " + mode);
		if (mode.equalsIgnoreCase("buyer")) {
			poUserQuery = poUserQuery + "A.CONTACTPERSONEMAILID = ? AND ";
		} else if (mode.equalsIgnoreCase("enduser")) {
			poUserQuery = poUserQuery + "A.REQUSITIONER = ? AND ";
		} else if (mode.equalsIgnoreCase("payer")) {
			poUserQuery = "";
		}

		ArrayList<String> param = new ArrayList<String>();

		if (status.equalsIgnoreCase("ALL")) {

			if (!mode.equalsIgnoreCase("payer")) {
				param.add(email1);
				param.add("N");
			} else {
				param.add("N");
			}

			if (!plant.equalsIgnoreCase("NA")) {
				String po = " AND B.PLANT=?";
				poDateSubquery = poDateSubquery + po;
				param.add(plant);
			}

			if (!vendor.equalsIgnoreCase("NA")) {
				String po = " AND A.VENDORID=?";
				poDateSubquery = poDateSubquery + po;
				param.add(vendor);
			}

			if (!ponumber.equalsIgnoreCase("NA")) {
				String po = " AND A.PONUMBER=?";
				poDateSubquery = poDateSubquery + po;
				param.add(ponumber);

			}
			if ((!fromdateofduration.equalsIgnoreCase("NA"))
					&& (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
				String in = " AND A.CREATEDON BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
				poDateSubquery = poDateSubquery + in;
				param.add(fromdateofduration);
				param.add(todateofduration);
			}
			if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
				String dt = " AND A.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY')";
				poDateSubquery = poDateSubquery + dt;
				param.add(fromdateofpo);
				param.add(todateofpo);
			}

			if (!mode.equalsIgnoreCase("payer")) {
				param.add(email1);
				param.add("N");
			} else {
				param.add("N");
			}

			if (!plant.equalsIgnoreCase("NA")) {
				param.add(plant);
			}

			if (!vendor.equalsIgnoreCase("NA")) {
				param.add(vendor);
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
			if ("enduser".equalsIgnoreCase(mode)) {
				param.add(email1);
			}
			if (!plant.equalsIgnoreCase("NA")) {
				param.add(plant);
			}

			if (!vendor.equalsIgnoreCase("NA")) {
				param.add(vendor);
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

			poDataQuery = "select * from ( "
					+ "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
					+ "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
					+ "FROM podetails A,poeventdetails B " + "WHERE " + poUserQuery
					+ " A.PONUMBER=B.PONUMBER and A.Status = ? " + poDateSubquery + " Union "
					+ "SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
					+ "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
					+ "FROM podetails A,poeventdetails B " + "WHERE " + poUserQuery
					+ " A.PONUMBER=B.PONUMBER and A.Status <> ? " + poDateSubquery + " " 
					/*
					+ " Union "
					+ " SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
					+ " B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
					+ " FROM podetails A,poeventdetails B, LOCATIONMASTER C " + "WHERE " 
					+ " A.PONUMBER=B.PONUMBER and B.DELVPLANT = C.LOCATIONCODE AND C.STOREKEEPEREMILID = ? "+ poDateSubquery 
					*/
					+ " ) ORDER BY 1,4";

		} else {

			if (status.equalsIgnoreCase("SP")) {
				String po = "and A.POTYPE =? ";
				poDateSubquery = poDateSubquery + po;

				if (!mode.equalsIgnoreCase("payer")) {
					param.add(email1);
					param.add("N");
				} else {
					param.add("N");
				}

			} else {
			
				poDateSubquery = poDateSubquery ;//+ po;
				if (!mode.equalsIgnoreCase("payer")) {
					param.add(email1);
					//param.add("N");				
				} else {
					param.add(status);
				}
				if("N".equalsIgnoreCase(status)) {
					String po = "and A.Status = ? ";
					poDateSubquery = poDateSubquery + po;
					param.add(status);
				}else if("A".equalsIgnoreCase(status)) {
					String po = "and A.Status = ? ";
					poDateSubquery = poDateSubquery + po;
					param.add(status);
				}else if("P".equalsIgnoreCase(status)) {
					String po = "and A.Status = ? ";
					poDateSubquery = poDateSubquery + po;
					param.add(status);
				}else if("C".equalsIgnoreCase(status)) {
					String po = "and A.Status = ? ";
					poDateSubquery = poDateSubquery + po;
					param.add(status);
				}	
			}

			if (!plant.equalsIgnoreCase("NA")) {
				String po = " AND B.PLANT = ? ";
				poDateSubquery = poDateSubquery + po;
				param.add(plant);
			}

			if (!vendor.equalsIgnoreCase("NA")) {
				String po = " AND A.VENDORID = ? ";
				poDateSubquery = poDateSubquery + po;
				param.add(vendor);
			}

			if (!ponumber.equalsIgnoreCase("NA")) {
				String po = " AND A.PONUMBER = ? ";
				poDateSubquery = poDateSubquery + po;
				param.add(ponumber);
			}
			if ((!fromdateofduration.equalsIgnoreCase("NA"))
					&& (!fromdateofduration.equalsIgnoreCase("Invalid date"))) {
				String in = " AND A.CREATEDON BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY') ";
				poDateSubquery = poDateSubquery + in;
				param.add(fromdateofduration);
				param.add(todateofduration);
			}
			if ((!fromdateofpo.equalsIgnoreCase("NA")) && (!fromdateofpo.equalsIgnoreCase("Invalid date"))) {
				String dt = " AND A.PODATE BETWEEN TO_DATE(?, 'DD/MM/YYYY') AND TO_DATE(?, 'DD/MM/YYYY') ";
				poDateSubquery = poDateSubquery + dt;
				param.add(fromdateofpo);
				param.add(todateofpo);
			}
			if ("enduser".equalsIgnoreCase(mode)) {
				param.add(email1);
			}	
			if("N".equalsIgnoreCase(status)) {
				param.add(status);
			}else if("A".equalsIgnoreCase(status)) {
				param.add(status);
			}else if("P".equalsIgnoreCase(status)) {
				param.add(status);
			}else if("C".equalsIgnoreCase(status)) {
				param.add(status);
			}	
			if (!plant.equalsIgnoreCase("NA")) {
				param.add(plant);
			}
			if (!vendor.equalsIgnoreCase("NA")) {
				param.add(vendor);
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
			
			String locationQuery = " AND C.STOREKEEPEREMILID = ? ";
			
			poDataQuery = "SELECT * FROM (SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
					+ "B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
					+ "FROM podetails A,poeventdetails B " + "WHERE " + poUserQuery + " A.PONUMBER=B.PONUMBER "
					+ poDateSubquery+ " UNION "
					+ " SELECT A.PONUMBER,A.CREATEDON,TO_CHAR(A.PODATE,'DD-MON-RRRR') AS PODATE,B.LINEITEMNUMBER,B.LINEITEMTEXT,B.RATEPERQTY,B.QUANTITY, "
					+ " B.BALANCE_QTY,(B.RATEPERQTY*B.QUANTITY) AS NETVALUE,A.POAMOUNT,A.STATUS,B.PLANT,(SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = B.PLANT) as PLANTNAME,(B.RATEPERQTY*B.BALANCE_QTY) AS BALANCENETVALUE  "
					+ " FROM podetails A,poeventdetails B,LOCATIONMASTER C WHERE A.PONUMBER=B.PONUMBER  AND B.DELVPLANT = C.LOCATIONCODE "
					+ locationQuery +" "+ poDateSubquery +" ) ORDER BY 1,4";

		}

		log.info("poDataQuery = " + poDataQuery);

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			Pagination pg = new Pagination(poDataQuery, 0);
			DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
			LocalDateTime myDateObj = LocalDateTime.now();
			String formattedDate = myDateObj.format(myFormatObj);
			log.info("Start Time: " + formattedDate);

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
					podata.add("Shipped");
				} //else if (rs.getString("Status").equalsIgnoreCase("S")) {
				  //	podata.add("Shipped");
				//}
				else if (rs.getString("Status").equalsIgnoreCase("C")) {
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

			// POImpl poimpl = new POImpl();
			// String encodedfile = poimpl.writeintoexcelfile(poDataList);

			LocalDateTime myDateObj2 = LocalDateTime.now();
			String formattedDate2 = myDateObj2.format(myFormatObj);
			log.info("Mid Time: " + formattedDate2);

			String encodedfile = downloadCSVfile(poDataList);

			LocalDateTime myDateObj3 = LocalDateTime.now();
			String formattedDate3 = myDateObj3.format(myFormatObj);
			log.info("End Time: " + formattedDate2);

			if (encodedfile.equalsIgnoreCase("")) {
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

	private String downloadCSVfile(List<List<String>> totallist) {
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
				log.error("downloadCSVfile() 1: " + e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("downloadCSVfile() 2: " + e.fillInStackTrace());

		}
		return encodedfile;
	}

	public JSONArray downloadAdvanceSearchlist(String invoicenumber, String ponumber, String status,
			String fromdurationdate, String todurationdate, String plant, String purchasegroup,
			String requisitioneremailid, String frominvamount, String toinvamount, String ageingfrom, String ageingto,
			String email, String vendor) throws SQLException {

		String downloadquery = " SELECT DISTINCT "
				+ " inv.ponumber                                             ponumber, "
				+ " inv.invoicenumber                                        invoicenumber, "
				+ " to_char(po.expensesheetid,'999999999999999')             expensesheetid, "
				+ " TRIM(to_char((po.paymentamount), '999,999,999,999,999')) AS paymentamount, "
				+ " to_char((inv.invoicedate), 'DD-MON-RRRR')                invoicedate, "
				+ " bp.businesspartnertext                                   businesspartnertext, "
				+ " inv.enduseid                                             enduseid, "
				+ " inv.enduserstatus                                        enduserstatus, "
				+ " po.overallstatus                                         overallstatus, "
				+ " to_char(po.createdon, 'DD-MON-RRRR')                   createdon, "
				+ " TRIM(to_char(po.amount, '999,999,999,999,999'))        AS amount, "
				+ " inv.proxy                                                proxyb, "
				+ " po.businesspartneroid                                    businesspartneroid, "
				+ " po.grnnumber                                             grnnumber, "
				+ " to_char(po.utrchequenumber,'999999999999999')         utrchequenumber, "
				+ " purchasinggroup                                       purchasinggroup, "
				+ " to_char(ds.grndate, 'DD-MON-RRRR')                     grndate, "
				+ " to_char(po.utrdate, 'DD-MON-RRRR')                     utrdate, "
				+ " po.plant                                              plantcode, "
				+ " (SELECT PLANTNAME FROM PLANTMASTER WHERE PLANTCODE = po.plant) plantname, "
				+ " (select ip.EUMANAGER  from CONFIRMERLIST ip where ip.invoicenumber = po.invoicenumber and ip.ponumber = po.ponumber ) confirmers,"
				+ " (select ip.EUMANAGER  from MANAGERLIST ip where ip.invoicenumber = po.invoicenumber and ip.ponumber = po.ponumber ) managers "
				+ " FROM " + " invoiceapproval   inv, " + " poninvoicesummery po, " + " businesspartner   bp, "
				+ " podetails         pd, " + " deliverysummary   ds " + " WHERE "
				+ " inv.invoicenumber = po.invoicenumber " + " AND po.invoicenumber = ds.invoicenumber "
				+ " AND inv.invoicenumber = ds.invoicenumber " + " AND pd.ponumber = po.ponumber "
				+ " AND pd.ponumber = ds.ponumber " + " AND inv.ponumber = po.ponumber "
				+ " AND pd.ponumber = inv.ponumber " + " AND po.businesspartneroid = bp.businesspartneroid ";

		List<List<String>> POListEvent = new ArrayList<List<String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			if (!"NA".equalsIgnoreCase(invoicenumber)) {
				downloadquery += " AND po.invoicenumber =  '" + invoicenumber + "'";
			}
			if (!"NA".equalsIgnoreCase(ponumber)) {
				downloadquery += " AND po.ponumber =  '" + ponumber + "'";
			}

			if ((!"NA".equalsIgnoreCase(fromdurationdate) && !"NA".equalsIgnoreCase(todurationdate))
					&& (!"Invalid date".equalsIgnoreCase(fromdurationdate)
							&& !"Invalid date".equalsIgnoreCase(todurationdate))) {
				String dt = " AND po.INVOICEDATE BETWEEN TO_DATE('" + fromdurationdate
						+ "', 'DD/MM/YYYY') AND TO_DATE('" + todurationdate + "', 'DD/MM/YYYY')";
				downloadquery += dt;
			}

			if ("C".equalsIgnoreCase(status) || "ALL".equalsIgnoreCase(status)) {

			} else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {

				if ("P".equalsIgnoreCase(status)) {

					downloadquery += " AND (po.OVERALLSTATUS = 'P' OR po.OVERALLSTATUS = 'M') ";

				} else if ("V".equalsIgnoreCase(status)) {

					downloadquery += " AND (po.OVERALLSTATUS = 'RO' OR po.OVERALLSTATUS = 'V') ";
				}
			} else {
				downloadquery += " AND po.OVERALLSTATUS = '" + status + "' ";
			}
			/*
			 * if (!"payer".equalsIgnoreCase(mode)) { if (emailId != null) {
			 * 
			 * downloadquery += " AND (inv.ENDUSEID = '" + emailId +
			 * "' or inv.EUMANAGER = '" + emailId + "') "; } } if
			 * ("Approval".equalsIgnoreCase(page)) { if (emailId != null) {
			 * 
			 * downloadquery += " AND (inv.ENDUSEID = '" + emailId +
			 * "' or inv.EUMANAGER = '" + emailId + "') "; } }
			 * 
			 */
			if (!"NA".equalsIgnoreCase(plant)) {

				downloadquery += " AND (po.plant = '" + plant + "') ";
			}
			if (!"NA".equalsIgnoreCase(vendor)) {
				downloadquery += " AND (bp.VENDORID = '" + vendor + "') ";
			}

			if (!"NA".equalsIgnoreCase(purchasegroup)) {
				downloadquery += " AND po.MATERIAL_TYPE= '" + purchasegroup + "'";

			}

			if (!"NA".equalsIgnoreCase(requisitioneremailid)) {
				downloadquery += " AND inv.enduseid=LOWER('" + requisitioneremailid + "') ";

			}

			if ((!"NA".equalsIgnoreCase(frominvamount) && !"NA".equalsIgnoreCase(toinvamount))) {
				downloadquery += " AND po.AMOUNT BETWEEN '" + frominvamount + "' AND '" + toinvamount + "' ";

			}
			if ((!"NA".equalsIgnoreCase(ageingfrom) && (!"NA".equalsIgnoreCase(ageingto)))) {

				downloadquery += "  AND sysdate - TO_DATE(po.MODIFIEDON, 'dd-mm-yy') > " + ageingfrom
						+ " AND sysdate - TO_DATE(po.MODIFIEDON, 'dd-mm-yy') < " + ageingto + " ";

			}

			log.info("download query :" + downloadquery);
			DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
			LocalDateTime myDateObj = LocalDateTime.now();
			String formattedDate = myDateObj.format(myFormatObj);
			log.info("Start Time: " + formattedDate);

			ps = con.prepareStatement(downloadquery);
			rs = ps.executeQuery();

			while (rs.next()) {
				List<String> poEvent = new ArrayList<String>();

				poEvent.add(rs.getString("INVOICENUMBER") == null ? "" : rs.getString("INVOICENUMBER"));
				poEvent.add(rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER"));
				poEvent.add(rs.getString("INVOICEDATE") == null ? "" : rs.getString("INVOICEDATE"));
				poEvent.add(rs.getString("CREATEDON") == null ? "" : rs.getString("CREATEDON"));
				poEvent.add(rs.getString("BUSINESSPARTNERTEXT") == null ? "" : rs.getString("BUSINESSPARTNERTEXT"));
				poEvent.add(rs.getString("ENDUSEID") == null ? "" : rs.getString("ENDUSEID"));

				String confirmers = "";
				// confirmers = getConfirmerlist(rs.getString("INVOICENUMBER"),
				// rs.getString("PONUMBER"), con).replace("_",",");
				confirmers = rs.getString("confirmers") == null ? "" : rs.getString("confirmers");
				if (confirmers == null || confirmers.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(confirmers);
				}
				String managers = "";
				// managers = getMGRlist(rs.getString("INVOICENUMBER"),
				// rs.getString("PONUMBER"), con).replace("_", ",");
				managers = rs.getString("managers") == null ? "" : rs.getString("managers");
				if (managers == null || managers.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(managers);
				}

				if (rs.getString("OVERALLSTATUS") == null) {
					poEvent.add("-");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("A")) {
					poEvent.add("Approved");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("M")
						|| rs.getString("OVERALLSTATUS").equalsIgnoreCase("P")) {
					poEvent.add("Pending");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("O")) {
					poEvent.add("On Hold");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("R")) {
					poEvent.add("Rejected");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("V")) {
					poEvent.add("Returned");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PRO")) {
					poEvent.add("Processed");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PD")) {
					poEvent.add("Paid");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PP")) {
					poEvent.add("Partially Paid");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("RO")) {
					poEvent.add("Reopened");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("INV")) {
					poEvent.add("Invalid Invoice");
				} else {
					poEvent.add("-");
				}
				poEvent.add(rs.getString("amount") == null ? "" : rs.getString("amount"));
				poEvent.add(rs.getString("PAYMENTAMOUNT") == null ? "" : rs.getString("PAYMENTAMOUNT"));
				poEvent.add(rs.getString("EXPENSESHEETID") == null ? "" : rs.getString("EXPENSESHEETID"));
				poEvent.add(rs.getString("UTRCHEQUENUMBER") == null ? "" : rs.getString("UTRCHEQUENUMBER"));
				poEvent.add(rs.getString("UTRDATE") == null ? "" : rs.getString("UTRDATE"));
				poEvent.add(rs.getString("PURCHASINGGROUP") == null ? "" : rs.getString("PURCHASINGGROUP"));
				poEvent.add(rs.getString("GRNNUMBER") == null ? "" : rs.getString("GRNNUMBER"));
				poEvent.add(rs.getString("GRNDATE") == null ? "" : rs.getString("GRNDATE"));
				String plantCode = rs.getString("PLANTCODE") == null ? "" : rs.getString("PLANTCODE");
				poEvent.add(plantCode);

				/*
				 * if ("".equalsIgnoreCase(plantCode)) { poEvent.add(""); } else { String
				 * plantName = getPlantName(rs.getString("PLANTNAME"), con); if (plantName ==
				 * null || plantName.equalsIgnoreCase("null")) { poEvent.add(""); } else {
				 * poEvent.add(plantName); } }
				 */
				String plantName = rs.getString("PLANTNAME");
				if (plantName == null || plantName.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(plantName);
				}

				POListEvent.add(poEvent);
			}
			rs.close();
			ps.close();
			LocalDateTime myDateObj2 = LocalDateTime.now();
			String formattedDate2 = myDateObj2.format(myFormatObj);
			log.info("Mid Time: " + formattedDate2);

			if (POListEvent.size() > 0) {
				String encodedfile = downloadinernalfile(POListEvent);

				LocalDateTime myDateObj3 = LocalDateTime.now();
				String formattedDate3 = myDateObj3.format(myFormatObj);
				log.info("End Time: " + formattedDate3);

				if (encodedfile.equalsIgnoreCase("")) {
					responsejson.put("message", "Fail");
				} else {
					responsejson.put("message", "Success");
					responsejson.put("data", encodedfile);
				}
			} else {
				responsejson.put("message", "Fail");
			}

			jsonArray.add(responsejson);
		} catch (Exception e) {
			log.error("downloadapprovalinvoicelist() :" + e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPODetails(String ponumber, String status, String fromdurationdate, String todurationdate,
			String plant, String purchasegroup, String requisitioneremailid, String frompoamount, String topoamount,
			String ageingfrom, String ageingto, HttpSession session, String email, int nPage, String mode,
			String vendor) {
		boolean twocondition = true;
		boolean nodata = false;
		String po_data = "";

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int pages = 0;
		Pagination pg = null;
		String subquery = "";
		String podata1 = "";

		try {

			podata1 = "select ped.plant,(select plantname from plantmaster where plantcode= ped.plant) PLANTNAME,to_char(pd.podate,'dd-MON-yyyy') PODATE,pd.ponumber, "
					+ "ped.lineitemnumber,ped.lineitemtext,pd.poamount,ped.rateperqty,ped.quantity,(ped.rateperqty * ped.quantity) LINEITEMAMT, "
					+ "ped.balance_qty,(ped.balance_qty * ped.rateperqty ) LINEITEMBALAMT,pd.status   from podetails pd, poeventdetails ped "
					+ "where ped.ponumber = pd.ponumber AND sysdate - TO_DATE(pd.CREATEDON, 'dd-mm-yy') < 90 ";

			con = DBConnection.getConnection();
			List<List<String>> poDataList = new ArrayList<List<String>>();

			if (!"NA".equalsIgnoreCase(vendor)) {
				String po = " AND pd.VENDORID =  '" + vendor + "' ";
				podata1 += po;
			}
			if (!"NA".equalsIgnoreCase(ponumber)) {
				String po = " AND pd.PONUMBER = '" + ponumber + "' ";
				podata1 += po;
			}
			if ((!"NA".equalsIgnoreCase(fromdurationdate) && !"NA".equalsIgnoreCase(todurationdate))
					&& (!"Invalid date".equalsIgnoreCase(fromdurationdate) && !"Invalid date".equalsIgnoreCase(fromdurationdate))) {
				String in = " AND pd.PODATE BETWEEN TO_DATE('" + fromdurationdate + "' , 'DD/MM/YYYY') "+ " AND TO_DATE('" + todurationdate + "', 'DD/MM/YYYY')";
				podata1 += in;
			}
			if (!"NA".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status) || !"AS".equalsIgnoreCase(status)) {
				String dt = " AND pd.STATUS = '" + status + "' ";
				podata1 += dt;
			}
			if (!"NA".equalsIgnoreCase(plant)) {
				String dt = " AND ped.PLANT = '" + plant + "' ";
				podata1 += dt;
			}
			if (!"NA".equalsIgnoreCase(purchasegroup)) {
				String dt = " AND ped.MATERIAL_TYPE = '" + purchasegroup + "'";
				podata1 += dt;
			}
			if (!"NA".equalsIgnoreCase(requisitioneremailid)) {
				String dt = " AND pd.REQUSITIONER = LOWER('" + requisitioneremailid + "') ";
				podata1 += dt;
			}
			if (!"NA".equalsIgnoreCase(frompoamount)) {
				String ia = "  AND pd.POAMOUNT BETWEEN '" + frompoamount + "' AND '" + topoamount + "' ";
				podata1 += ia;
			}
			if (!"NA".equalsIgnoreCase(ageingfrom)) {

				String ia = "  AND sysdate - TO_DATE(pd.MODIFIEDON, 'dd-mm-yy') > '" + ageingfrom + "' ";
				if (!"NA".equalsIgnoreCase(ageingto)) {
					ia = ia + "  AND sysdate - TO_DATE(pd.MODIFIEDON, 'dd-mm-yy') < '" + ageingto + "' ";
				}
				podata1 += ia;
			}

			if ("buyer".equalsIgnoreCase(mode)) {
				podata1 += " and pd.CONTACTPERSONEMAILID = '" + email + "' ";

			} else if ("enduser".equalsIgnoreCase(mode) || !"NA".equalsIgnoreCase(requisitioneremailid)) {

				podata1 += " and pd.REQUSITIONER = '" + requisitioneremailid + "' ";

			} else if ("payer".equalsIgnoreCase(mode)) {

			}

			log.info("podata1 : " + podata1);

			ps = con.prepareStatement(podata1);
			rs = ps.executeQuery();

			while (rs.next()) {
				List<String> poData = new ArrayList<String>();

				poData.add(rs.getString("PLANT"));
				poData.add(rs.getString("PLANTNAME"));
				poData.add(rs.getString("PODATE"));
				poData.add(rs.getString("PONUMBER"));
				poData.add(rs.getString("LINEITEMNUMBER"));
				poData.add(rs.getString("LINEITEMTEXT"));
				poData.add(rs.getString("POAMOUNT"));
				poData.add(rs.getString("RATEPERQTY"));
				poData.add(rs.getString("QUANTITY"));
				poData.add(rs.getString("LINEITEMAMT"));
				poData.add(rs.getString("BALANCE_QTY"));
				poData.add(rs.getString("LINEITEMBALAMT"));
				// poData.add(rs.getString("STATUS"));

				if ("N".equalsIgnoreCase(rs.getString("Status"))) {
					poData.add("New");
				} else if ("A".equalsIgnoreCase(rs.getString("Status"))) {
					poData.add("Accepted");
				} else if ("P".equalsIgnoreCase(rs.getString("Status"))) {
					poData.add("Pending");
				} else if ("W".equalsIgnoreCase(rs.getString("Status"))) {
					poData.add("Work In Progress");
				} else if ("S".equalsIgnoreCase(rs.getString("Status"))) {
					poData.add("Shipped");
				} else if ("C".equalsIgnoreCase(rs.getString("Status"))) {
					poData.add("Complete");
				}
				poDataList.add(poData);
			}

			rs.close();
			ps.close();

			log.info("poDataList.size() :" + poDataList.size());

			String encodedfile = downloadCSVfile(poDataList);

			if (encodedfile.equalsIgnoreCase("")) {
				responsejson.put("message", "Fail");

			} else {
				responsejson.put("message", "Success");
				responsejson.put("data", encodedfile);
			}
			jsonArray.add(responsejson);
		} catch (Exception e) {
			log.error("downloaddashboardpolist() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	private String downloadExcelfile(List<List<String>> totallist) {
		String encodedfile = "";
		try {
			String filename = "D:\\POReport.xlsx";
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			OutputStreamWriter streamWriter = new OutputStreamWriter(stream);

			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("PO Report");
			int rowIndex = 0;
			int cellIndex = 0;
			XSSFRow row = sheet.createRow(rowIndex++);

			row.createCell(cellIndex++).setCellValue("PLANT CODE");
			row.createCell(cellIndex++).setCellValue("PLANT NAME");
			row.createCell(cellIndex++).setCellValue("PO DATE");
			row.createCell(cellIndex++).setCellValue("PO NUMBER");
			row.createCell(cellIndex++).setCellValue("LINE ITEM NUMBER");
			row.createCell(cellIndex++).setCellValue("LINE ITEM DESCRIPTION");
			row.createCell(cellIndex++).setCellValue("PO AMOUNT");
			row.createCell(cellIndex++).setCellValue("LINE ITEM RATE");
			row.createCell(cellIndex++).setCellValue("LINE ITEM QUANTITY");
			row.createCell(cellIndex++).setCellValue("LINE ITEM AMOUNT");
			row.createCell(cellIndex++).setCellValue("LINE ITEM BALANCE QUANTITY");
			row.createCell(cellIndex++).setCellValue("LINE ITEM BALANCE AMOUNT");
			row.createCell(cellIndex++).setCellValue("STATUS");

			Iterator<List<String>> i = totallist.iterator();

			while (i.hasNext()) {

				List<String> templist = (List<String>) i.next();
				Iterator<String> tempIterator = templist.iterator();
				// String data = "";
				cellIndex = 0;
				row = sheet.createRow(rowIndex++);

				while (tempIterator.hasNext()) {
					String data = (String) tempIterator.next();
					row.createCell(cellIndex++).setCellValue(data);
				}
			}
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
			streamWriter.flush();
			byte[] byteArrayOutputStream = stream.toByteArray();

			try {
				encodedfile = new String(Base64.encodeBase64(byteArrayOutputStream), "UTF-8");
			} catch (IOException e) {
				log.error("downloadExcelfile() 1: " + e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("downloadExcelfile() 2: " + e.fillInStackTrace());

		}
		return encodedfile;
	}

	private String downloadInernalExcelfile(List<List<String>> totallist) {
		String encodedfile = "";
		try {
			String filename = "InvoiceReport.xlsx";

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
			// CSVWriter writer = new CSVWriter(streamWriter);

			// List<String[]> heading = new ArrayList<String[]>();
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("Invoice Report");
			int rowIndex = 0;
			int cellIndex = 0;
			XSSFRow row = sheet.createRow(rowIndex++);

			row.createCell(cellIndex++).setCellValue("INVOICE NUMBER");
			row.createCell(cellIndex++).setCellValue("PO NUMBER");
			row.createCell(cellIndex++).setCellValue("INVOICE DATE");
			row.createCell(cellIndex++).setCellValue("CREATED DATE");
			row.createCell(cellIndex++).setCellValue("VENDOR NAME");
			row.createCell(cellIndex++).setCellValue("REQUISITIONER/STOREKEEPER");
			row.createCell(cellIndex++).setCellValue("GRN CONFIRMERS");
			row.createCell(cellIndex++).setCellValue("INVOICE APPROVERS");
			row.createCell(cellIndex++).setCellValue("OVERALL STATUS");
			row.createCell(cellIndex++).setCellValue("INVOICE AMOUNT");
			row.createCell(cellIndex++).setCellValue("PAID AMOUNT");
			row.createCell(cellIndex++).setCellValue("EXPENSE SHEET ID");
			row.createCell(cellIndex++).setCellValue("UTR NUMBER");
			row.createCell(cellIndex++).setCellValue("UTR DATE");
			row.createCell(cellIndex++).setCellValue("PURCHASE GROUP");
			row.createCell(cellIndex++).setCellValue("GRN NUMBER");
			row.createCell(cellIndex++).setCellValue("GRN DATE");
			row.createCell(cellIndex++).setCellValue("PLANT CODE");
			row.createCell(cellIndex++).setCellValue("PLANT NAME");

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
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
			streamWriter.flush();
			byte[] byteArrayOutputStream = stream.toByteArray();

			try {
				encodedfile = new String(Base64.encodeBase64(byteArrayOutputStream), "UTF-8");
			} catch (IOException e) {
				log.error("downloadinernalfile() 1: ", e.fillInStackTrace());
			}

		} catch (Exception e) {
			log.error("downloadinernalfile() 2: ", e.fillInStackTrace());

		}
		return encodedfile;
	}

	public JSONArray downloadAppInvoicelist(String emailId, String status, String invno, String pono, String fdate,
			String tdate, String plant, String vendor, String page, String mode) throws SQLException {

		
		String addOne = "";

		String addTwo = "";

		String addThree = "";

		List<List<String>> POListEvent = new ArrayList<List<String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();

			String poInvoiceQuery = "";

			if (!"NA".equalsIgnoreCase(invno)) {
				poInvoiceQuery += " AND po.invoicenumber =  '" + invno + "'";
			}
			if (!"NA".equalsIgnoreCase(pono)) {
				poInvoiceQuery += " AND po.ponumber =  '" + pono + "'";
			}
			if (!"NA".equalsIgnoreCase(plant)) {

				poInvoiceQuery += " AND (po.plant = '" + plant + "') ";
			}
			if (!"NA".equalsIgnoreCase(vendor)) {
				poInvoiceQuery += " AND (bp.VENDORID = '" + vendor + "') ";
			}

			if ((!"NA".equalsIgnoreCase(fdate) && !"NA".equalsIgnoreCase(tdate))
					&& (!"Invalid date".equalsIgnoreCase(fdate) && !"Invalid date".equalsIgnoreCase(tdate))) {
				String dt = " AND po.INVOICEDATE BETWEEN TO_DATE('" + fdate + "', 'DD/MM/YYYY') AND TO_DATE('" + tdate
						+ "', 'DD/MM/YYYY')";
				poInvoiceQuery += dt;
			}

			if ("C".equalsIgnoreCase(status) || "ALL".equalsIgnoreCase(status) || "AS".equalsIgnoreCase(status)) {

				addOne = "    AND inv.enduseid = '" + emailId + "' ";

				addTwo = "    AND inv.eumanager = '" + emailId + "'  "
						+ "    AND inv.status NOT LIKE 'C%' AND po.grnnumber IS NOT NULL  ";

				addThree = "    AND ( inv.eumanager = '" + emailId + "' ) " + "    AND ( inv.status LIKE 'C%' )";

			} else if (!"C".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {

				if ("P".equalsIgnoreCase(status)) {

					// downloadquery += " AND (po.OVERALLSTATUS = 'P' OR po.OVERALLSTATUS = 'M') ";

					addOne = "    AND inv.enduseid = '" + emailId + "' "
							+ "    AND ( po.overallstatus = 'P' OR po.overallstatus = 'M' ) ";

					addTwo = "    AND inv.eumanager = '" + emailId + "'  "
							+ "    AND ( po.overallstatus = 'P' OR po.overallstatus = 'M' "
							+ "    AND inv.status NOT LIKE 'C%' AND po.grnnumber IS NOT NULL ) ";

					addThree = "    AND ( inv.eumanager = '" + emailId + "' ) "
							+ "    AND ( po.overallstatus = 'P' OR po.overallstatus = 'M'  AND inv.status LIKE 'C%' )";

				} else if ("V".equalsIgnoreCase(status)) {

					// downloadquery += " AND (po.OVERALLSTATUS = 'RO' OR po.OVERALLSTATUS = 'V') ";
					addOne = "    AND inv.enduseid = '" + emailId + "' "
							+ "    AND ( po.overallstatus = 'V' OR po.overallstatus = 'RO' ) ";

					addTwo = "    AND inv.eumanager = '" + emailId + "'  "
							+ "    AND ( po.overallstatus = 'V' OR po.overallstatus = 'RO' "
							+ "    AND inv.status NOT LIKE 'C%' AND po.grnnumber IS NOT NULL ) ";

					addThree = "    AND ( inv.eumanager = '" + emailId + "' ) "
							+ "    AND ( po.overallstatus = 'V' OR po.overallstatus = 'RO'  AND inv.status LIKE 'C%' )";

				} else {
					// downloadquery += " AND po.OVERALLSTATUS = '" + status + "' ";

					addOne = "  AND inv.enduseid = '" + emailId + "'  AND ( po.overallstatus = '" + status + "' ) ";

					addTwo = " AND ( inv.eumanager = '" + emailId + "' ) " + " AND ( po.overallstatus = '" + status	+ "' AND inv.status NOT LIKE 'C%' "
					+ " AND po.grnnumber IS NOT NULL ) ";

					addThree = " AND ( inv.eumanager = '" + emailId + "' ) " + "  AND ( po.overallstatus = '" + status+ "' AND inv.status LIKE 'C%' ) ";
				}
			}

		
			addOne = addOne + poInvoiceQuery;
			addTwo = addTwo + poInvoiceQuery;
			addThree = addThree + poInvoiceQuery;

			String downloadquery = "SELECT DISTINCT "
					+ "    po.ponumber                                              ponumber, "
					+ "    po.invoicenumber                                         invoicenumber, "
					+ "    po.expensesheetid                                        expensesheetid, "
					+ "    TRIM(to_char((po.paymentamount), '999,999,999,999,999')) AS paymentamount, "
					+ "    to_char((po.invoicedate), 'DD-MON-RRRR')                 invoicedate, "
					+ "    bp.businesspartnertext                                   businesspartnertext, "
					+ "    po.requsitioner                                          enduseid, "
					+ "    (SELECT  MAX(ip.enduserstatus) FROM invoiceapproval ip "
					+ "     WHERE ip.invoicenumber = po.invoicenumber AND ip.ponumber = po.ponumber "
					+ "    )                                                        enduserstatus, "
					+ "    po.overallstatus                                         overallstatus, "
					+ "    to_char(po.createdon, 'DD-MON-RRRR')                     createdon, "
					+ "    TRIM(to_char(po.amount, '999,999,999,999,999'))          AS amount, "
					+ "    (SELECT MAX(ip.proxy) FROM invoiceapproval ip WHERE "
					+ "    ip.invoicenumber = po.invoicenumber  AND ip.ponumber = po.ponumber "
					+ "    )                                                        proxyb, "
					+ "    po.businesspartneroid                                    businesspartneroid, "
					+ "    po.grnnumber                                             grnnumber, "
					+ "    po.utrchequenumber                                       utrchequenumber, "
					+ "    purchasinggroup                                          purchasinggroup, "
					+ "    (SELECT MAX(to_char(gm.createdon, 'DD-MON-RRRR')) FROM grnmapping gm "
					+ "        WHERE gm.invoicenumber = po.invoicenumber AND gm.ponumber = po.ponumber "
					+ "    )                                                        grndate, "
					+ "    to_char(po.paymentdate, 'DD-MON-RRRR')                       paymentdate, "
					+ "    po.plant                                                 plantcode, "
					+ "    ( SELECT plantname FROM plantmaster WHERE plantcode = po.plant) plantname, "
					+ "    ( SELECT ip.eumanager FROM confirmerlist ip WHERE ip.invoicenumber = po.invoicenumber "
					+ "      AND ip.ponumber = po.ponumber ) confirmers, "
					+ "    (  SELECT ip.eumanager FROM managerlist ip WHERE ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        managers " + "FROM "
					+ "    invoiceapproval   inv, " + "    poninvoicesummery po, " + "    businesspartner   bp, "
					+ "    podetails         pd " + "WHERE " + "        inv.invoicenumber = po.invoicenumber "
					+ "    AND pd.ponumber = po.ponumber " + "    AND inv.ponumber = po.ponumber "
					+ "    AND pd.ponumber = inv.ponumber " + "    AND po.businesspartneroid = bp.businesspartneroid "
					+ "    AND inv.invoicenumber IS NOT NULL " + addOne + "  union " + "  SELECT DISTINCT "
					+ "    po.ponumber                                              ponumber, "
					+ "    po.invoicenumber                                         invoicenumber, "
					+ "    po.expensesheetid                                        expensesheetid, "
					+ "    TRIM(to_char((po.paymentamount), '999,999,999,999,999')) AS paymentamount, "
					+ "    to_char((po.invoicedate), 'DD-MON-RRRR')                 invoicedate, "
					+ "    bp.businesspartnertext                                   businesspartnertext, "
					+ "    po.requsitioner                                          enduseid, " + "    ( "
					+ "        SELECT " + "            MAX(ip.enduserstatus) " + "        FROM "
					+ "            invoiceapproval ip " + "        WHERE "
					+ "                ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        enduserstatus, "
					+ "    po.overallstatus                                         overallstatus, "
					+ "    to_char(po.createdon, 'DD-MON-RRRR')                     createdon, "
					+ "    TRIM(to_char(po.amount, '999,999,999,999,999'))          AS amount, " + "    ( "
					+ "        SELECT " + "            MAX(ip.proxy) " + "        FROM "
					+ "            invoiceapproval ip " + "        WHERE "
					+ "                ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        proxyb, "
					+ "    po.businesspartneroid                                    businesspartneroid, "
					+ "    po.grnnumber                                             grnnumber, "
					+ "    po.utrchequenumber                                       utrchequenumber, "
					+ "    purchasinggroup                                          purchasinggroup, " 
					+ "    ( SELECT MAX(to_char(gm.createdon, 'DD-MON-RRRR')) FROM grnmapping gm "
					+ "        WHERE gm.invoicenumber = po.invoicenumber AND gm.ponumber = po.ponumber "
					+ "    )                                                        grndate, "
					+ "    to_char(po.paymentdate, 'DD-MON-RRRR')                       paymentdate, "
					+ "    po.plant                                                 plantcode, " + "    ( "
					+ "        SELECT " + "            plantname " + "        FROM " + "            plantmaster "
					+ "        WHERE " + "            plantcode = po.plant "
					+ "    )                                                        plantname, " + "    ( "
					+ "        SELECT " + "            ip.eumanager " + "        FROM "
					+ "            confirmerlist ip " + "        WHERE "
					+ "                ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        confirmers, " + "    ( "
					+ "        SELECT " + "            ip.eumanager " + "        FROM " + "            managerlist ip "
					+ "        WHERE " + "                ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        managers " + "FROM "
					+ "    invoiceapproval   inv, " + "    poninvoicesummery po, " + "    businesspartner   bp, "
					+ "    podetails         pd " + "WHERE " + "        inv.invoicenumber = po.invoicenumber "
					+ "    AND pd.ponumber = po.ponumber " + "    AND inv.ponumber = po.ponumber "
					+ "    AND pd.ponumber = inv.ponumber " + "    AND po.businesspartneroid = bp.businesspartneroid "
					+ "    AND inv.invoicenumber IS NOT NULL " + addTwo + "    union " + "    SELECT DISTINCT "
					+ "    po.ponumber                                              ponumber, "
					+ "    po.invoicenumber                                         invoicenumber, "
					+ "    po.expensesheetid                                        expensesheetid, "
					+ "    TRIM(to_char((po.paymentamount), '999,999,999,999,999')) AS paymentamount, "
					+ "    to_char((po.invoicedate), 'DD-MON-RRRR')                 invoicedate, "
					+ "    bp.businesspartnertext                                   businesspartnertext, "
					+ "    po.requsitioner                                          enduseid, " + "    ( "
					+ "        SELECT " + "            MAX(ip.enduserstatus) " + "        FROM "
					+ "            invoiceapproval ip " + "        WHERE "
					+ "                ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        enduserstatus, "
					+ "    po.overallstatus                                         overallstatus, "
					+ "    to_char(po.createdon, 'DD-MON-RRRR')                     createdon, "
					+ "    TRIM(to_char(po.amount, '999,999,999,999,999'))          AS amount, " + "    ( "
					+ "        SELECT " + "            MAX(ip.proxy) " + "        FROM "
					+ "            invoiceapproval ip " + "        WHERE "
					+ "                ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        proxyb, "
					+ "    po.businesspartneroid                                    businesspartneroid, "
					+ "    po.grnnumber                                             grnnumber, "
					+ "    po.utrchequenumber                                       utrchequenumber, "
					+ "    purchasinggroup                                          purchasinggroup, " 
					+ "    (SELECT MAX(to_char(gm.createdon, 'DD-MON-RRRR')) FROM grnmapping gm "
					+ "        WHERE gm.invoicenumber = po.invoicenumber AND gm.ponumber = po.ponumber "
					+ "    )                                                        grndate, "
					+ "    to_char(po.paymentdate, 'DD-MON-RRRR')                       paymentdate, "
					+ "    po.plant                                                 plantcode, " + "    ( "
					+ "        SELECT " + "            plantname " + "        FROM " + "            plantmaster "
					+ "        WHERE " + "            plantcode = po.plant "
					+ "    )                                                        plantname, " + "    ( "
					+ "        SELECT " + "            ip.eumanager " + "        FROM "
					+ "            confirmerlist ip " + "        WHERE "
					+ "                ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        confirmers, " + "    ( "
					+ "        SELECT " + "            ip.eumanager " + "        FROM " + "            managerlist ip "
					+ "        WHERE " + "                ip.invoicenumber = po.invoicenumber "
					+ "            AND ip.ponumber = po.ponumber "
					+ "    )                                                        managers " + "FROM "
					+ "    invoiceapproval   inv, " + "    poninvoicesummery po, " + "    businesspartner   bp, "
					+ "    podetails         pd " + "WHERE " + "        inv.invoicenumber = po.invoicenumber "
					+ "    AND pd.ponumber = po.ponumber " + "    AND inv.ponumber = po.ponumber "
					+ "    AND pd.ponumber = inv.ponumber " + "    AND po.businesspartneroid = bp.businesspartneroid "
					+ "    AND inv.invoicenumber IS NOT NULL " + addThree;

			log.info("download query :" + downloadquery);

			DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

			LocalDateTime myDateObj = LocalDateTime.now();

			String formattedDate = myDateObj.format(myFormatObj);

			log.info("Start Time: " + formattedDate);

			ps = con.prepareStatement(downloadquery);
			rs = ps.executeQuery();

			while (rs.next()) {
				List<String> poEvent = new ArrayList<String>();

				poEvent.add(rs.getString("INVOICENUMBER") == null ? "" : rs.getString("INVOICENUMBER"));
				poEvent.add(rs.getString("PONUMBER") == null ? "" : rs.getString("PONUMBER"));
				poEvent.add(rs.getString("INVOICEDATE") == null ? "" : rs.getString("INVOICEDATE"));
				poEvent.add(rs.getString("CREATEDON") == null ? "" : rs.getString("CREATEDON"));
				poEvent.add(rs.getString("BUSINESSPARTNERTEXT") == null ? "" : rs.getString("BUSINESSPARTNERTEXT"));
				poEvent.add(rs.getString("ENDUSEID") == null ? "" : rs.getString("ENDUSEID"));

				String confirmers = "";
				// confirmers = getConfirmerlist(rs.getString("INVOICENUMBER"),
				// rs.getString("PONUMBER"), con).replace("_",",");
				confirmers = rs.getString("confirmers") == null ? "" : rs.getString("confirmers");
				if (confirmers == null || confirmers.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(confirmers);
				}
				String managers = "";

				// managers = getMGRlist(rs.getString("INVOICENUMBER"),
				// rs.getString("PONUMBER"), con).replace("_", ",");
				managers = rs.getString("managers") == null ? "" : rs.getString("managers");
				if (managers == null || managers.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(managers);
				}

				if (rs.getString("OVERALLSTATUS") == null) {
					poEvent.add("-");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("A")) {
					poEvent.add("Approved");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("M")
						|| rs.getString("OVERALLSTATUS").equalsIgnoreCase("P")) {
					poEvent.add("Pending");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("O")) {
					poEvent.add("On Hold");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("R")) {
					poEvent.add("Rejected");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("V")) {
					poEvent.add("Returned");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PRO")) {
					poEvent.add("Processed");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PD")) {
					poEvent.add("Paid");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("PP")) {
					poEvent.add("Partially Paid");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("RO")) {
					poEvent.add("Reopened");
				} else if (rs.getString("OVERALLSTATUS").equalsIgnoreCase("INV")) {
					poEvent.add("Invalid Invoice");
				} else {
					poEvent.add("-");
				}
				poEvent.add(rs.getString("amount") == null ? "" : rs.getString("amount"));
				poEvent.add(rs.getString("PAYMENTAMOUNT") == null ? "" : rs.getString("PAYMENTAMOUNT"));
				poEvent.add(rs.getString("EXPENSESHEETID") == null ? "" : rs.getString("EXPENSESHEETID"));
				poEvent.add(rs.getString("UTRCHEQUENUMBER") == null ? "" : rs.getString("UTRCHEQUENUMBER"));
				poEvent.add(rs.getString("PAYMENTDATE") == null ? "" : rs.getString("PAYMENTDATE"));
				poEvent.add(rs.getString("PURCHASINGGROUP") == null ? "" : rs.getString("PURCHASINGGROUP"));
				poEvent.add(rs.getString("GRNNUMBER") == null ? "" : rs.getString("GRNNUMBER"));
				poEvent.add(rs.getString("GRNDATE") == null ? "" : rs.getString("GRNDATE"));
				String plantCode = rs.getString("PLANTCODE") == null ? "" : rs.getString("PLANTCODE");
				poEvent.add(plantCode);

				/*
				 * if ("".equalsIgnoreCase(plantCode)) { poEvent.add(""); } else { String
				 * plantName = getPlantName(rs.getString("PLANTNAME"), con); if (plantName ==
				 * null || plantName.equalsIgnoreCase("null")) { poEvent.add(""); } else {
				 * poEvent.add(plantName); } }
				 */
				String plantName = rs.getString("PLANTNAME");
				if (plantName == null || plantName.equalsIgnoreCase("null")) {
					poEvent.add("");
				} else {
					poEvent.add(plantName);
				}

				POListEvent.add(poEvent);
			}
			rs.close();
			ps.close();
			// LocalDateTime myDateObj2 = LocalDateTime.now();
			// String formattedDate2 = myDateObj2.format(myFormatObj);
			// System.out.println("Mid Time: " + formattedDate2);

			if (POListEvent.size() > 0) {
				String encodedfile = downloadinernalfile(POListEvent);
				// String encodedfile = downloadInernalExcelfile(POListEvent);

				// LocalDateTime myDateObj3 = LocalDateTime.now();
				// String formattedDate3 = myDateObj3.format(myFormatObj);
				// System.out.println("End Time: " + formattedDate3);

				if (encodedfile.equalsIgnoreCase("")) {
					responsejson.put("message", "Fail");
				} else {
					responsejson.put("message", "Success");
					responsejson.put("data", encodedfile);
				}
			} else {
				responsejson.put("message", "Fail");
			}

			jsonArray.add(responsejson);
		} catch (Exception e) {
			log.error("downloadapprovalinvoicelist() :", e.fillInStackTrace());

		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getemailidofpersontobesearched(String text, String actionby, String plant, String material,
			String userid) throws SQLException {

		boolean result;
		result = Validation.StringChecknull(text);
		if (result == false) {
			responsejson.put("validation", "validation Fail");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}
		String sql = "";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int inventorycount = 0;
		try {
			con = DBConnection.getConnection();
			ArrayList<HashMap<String, String>> searchlist = new ArrayList<HashMap<String, String>>();

			if (plant.equalsIgnoreCase("NA")) {
				if (actionby.equalsIgnoreCase("true")) {
					sql = "select * from LIKEDETAILS WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID";
				} else {
					sql = "select * from LIKEDETAILSGMNABOVE WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID";
				}
			} else {
				if (actionby.equalsIgnoreCase("true")) {

					String inventorychecksql = "select count(*) as counter from INVENTORYUSERLIST where PLANT=? AND MTYP=? AND USERID=?";
					ps = con.prepareStatement(inventorychecksql);
					ps.setString(1, plant);
					ps.setString(2, material);
					ps.setString(3, userid);
					rs = ps.executeQuery();

					if (rs.next()) {
						inventorycount = rs.getInt("counter");
					}
					rs.close();
					ps.close();
					if (inventorycount > 0) {
						sql = "select * from LIKEDETAILS WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID";
					} else {
						sql = "select * from LIKEDETAILSGMNABOVE WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID";
					}

				} else {
					sql = "select * from LIKEDETAILSGMNABOVE WHERE LOWER(EMAILID) LIKE LOWER(?) order by EMAILID";
				}
			}

			ps = con.prepareStatement(sql);
			ps.setString(1, text + "%");
			rs = ps.executeQuery();

			while (rs.next()) {
				HashMap<String, String> listData = new HashMap<String, String>();
				listData.put("ID", rs.getString("ID"));
				listData.put("EMAILID", rs.getString("EMAILID"));
				listData.put("NAME", rs.getString("NAME"));
				listData.put("DESIGNATION", rs.getString("DESIGNATION"));
				searchlist.add(listData);
			}
			rs.close();
			ps.close();
			if (searchlist.size() > 0) {
				responsejson.put("searchdetailslist", searchlist);
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
			}

		} catch (SQLException e) {
			log.error("getemailidofpersontobesearched() :", e.fillInStackTrace());

			responsejson.put("message", "Network Issue");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}
}
