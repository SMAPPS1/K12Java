package com.bccl.dxapi.apiimpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.apiutility.Validation;

public class DashboardImpl {
	static Logger log = Logger.getLogger(DashboardImpl.class.getName());
	private static final DecimalFormat df = new DecimalFormat("0.00");

	JSONObject responsejson = null;
	JSONArray jsonArray = null;

	public DashboardImpl() {
		responsejson = new JSONObject();
		jsonArray = new JSONArray();
	}

	@Override
	protected void finalize() throws Throwable {
		responsejson = null;
		jsonArray = null;
		super.finalize();
	}

	@SuppressWarnings("unchecked")
	public JSONArray getInvoiceSummeryFunctionWise(String deptcode, String fromDate, String toDate) {

		log.info("getInvoiceSummeryFunctionWise - DEPTCODE : " + deptcode + " FROM DATE : " + fromDate
				+ " TO DATE : " + toDate);
		boolean result;
		result = Validation.StringChecknull(deptcode);
		if (result == false) {
			responsejson.put("validation", "Dept. code is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		result = Validation.StringChecknull(fromDate);
		if (result == false) {
			responsejson.put("validation", "From date is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.validateDateFormat("dd-MM-yy", fromDate);
			if (result == false) {
				responsejson.put("validation", "From date should be in dd-MM-yy format.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(toDate);
		if (result == false) {
			responsejson.put("validation", "To date is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.validateDateFormat("dd-MM-yy", toDate);
			if (result == false) {
				responsejson.put("validation", "To date should be in dd-MM-yy format.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		log.info("getInvoiceSummeryFunctionWise - validation checked ");

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<String> endUserProcessList = new ArrayList<String>();
		endUserProcessList.add("P"); // Pending with GRN creator
		endUserProcessList.add("CM"); // Pending with GRN confirmer
		endUserProcessList.add("M"); // Pending with GRN confirmer/Invoice Approver
		endUserProcessList.add("V"); // Return to vendor
		endUserProcessList.add("RO"); // Reopen for correction
		// endUserProcessList.add("O"); // On Hold // date:04-03-2023 Sachin Sir said
		// remove the hold option and counts in the list
		endUserProcessList.add("INV"); // invalid Invoice
		endUserProcessList.add("A"); // Approved
		endUserProcessList.add("PRO"); // Processed
		endUserProcessList.add("PP"); // Partially Paid
		endUserProcessList.add("PD"); // Paid
		Double count = 0.00;
		Double percentage = 0.00;
		Double amountInLac = 0.00;
		try {

			String subQuery = "";
			if (!deptcode.equalsIgnoreCase("All")) {
				subQuery = subQuery + " deptcode = ? and ";
			}

			subQuery = subQuery + " overallstatus = ? and ";

			String percSubQuery = "";
			if (!deptcode.equalsIgnoreCase("All")) {
				percSubQuery = percSubQuery + " deptcode = ? and ";
			}
			String dateQuery = "";
			String dateQuery1 = "";
			String dateQuery2 = "";
			String dateQuery3 = "";
			String dateQuery4 = "";
			con = DBConnection.getConnection();

			ArrayList<Object> invSummList = new ArrayList<Object>();
			LinkedHashMap<String, Object> invData1 = new LinkedHashMap<String, Object>();
			
			for (String status : endUserProcessList) {

				dateQuery = "";
				dateQuery1 = "";
				dateQuery2 = "";
				dateQuery3 = "";
				dateQuery4 = "";

				if ("P".equalsIgnoreCase(status) || "CM".equalsIgnoreCase(status) || "M".equalsIgnoreCase(status)
						|| "V".equalsIgnoreCase(status) || "RO".equalsIgnoreCase(status)
						|| "INV".equalsIgnoreCase(status)) {

					dateQuery = " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') and overallstatus in( 'P','M','INV','V','RO')) as TOTALSUM, ";
					dateQuery1 = " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY')  and overallstatus in( 'P','M','INV','V','RO') )),2) as PERCENTAGE, ";
					dateQuery2 = " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') ";
					if("M".equalsIgnoreCase(status)) {
						dateQuery2 = dateQuery2 + " AND grnnumber is not null " ;
					}
				} else {

					dateQuery = " MODIFIEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY')  and overallstatus in( 'A','PRO','PP','PD')) as TOTALSUM, ";
					dateQuery1 = " MODIFIEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY')  and overallstatus in( 'A','PRO','PP','PD') )),2) as PERCENTAGE, ";
					dateQuery2 = " MODIFIEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') ";
					dateQuery3 = " AND onexstatus ='P' ";
				}

				String paymentProcessQuery = "SELECT OVERALLSTATUS,count(*) as ICOUNT, "
						+ "(select count(*) from GET_DASHBOARD_FOR_FUNCTION where " + percSubQuery + dateQuery
						+ "round(((count(*)*100)/(select count(*) from GET_DASHBOARD_FOR_FUNCTION where " + percSubQuery
						+ dateQuery1 + " TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') as AMOUNTInCR ";

				if ("CM".equalsIgnoreCase(status)) {

					dateQuery4 = "from GET_DASHBOARD_FOR_FUNCTION a " + "where " + subQuery + " grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber) and "
							+ dateQuery2 + dateQuery3 + "group by overallstatus " + "order by 1 ";

				} else {
					dateQuery4 = "from GET_DASHBOARD_FOR_FUNCTION " + "where " + subQuery + dateQuery2 + dateQuery3
							+ "group by overallstatus " + "order by 1 ";

				}
				paymentProcessQuery = paymentProcessQuery + dateQuery4;
				log.info("paymentProcessQuery - " + paymentProcessQuery);

				ps = con.prepareStatement(paymentProcessQuery);

				if (!deptcode.equalsIgnoreCase("All")) {
					ps.setString(1, deptcode);
					ps.setString(2, fromDate);
					ps.setString(3, toDate);

					ps.setString(4, deptcode);
					ps.setString(5, fromDate);
					ps.setString(6, toDate);

					ps.setString(7, deptcode);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(8, "M");
					} else {
						ps.setString(8, status);
					}
					ps.setString(9, fromDate);
					ps.setString(10, toDate);
				} else {
					ps.setString(1, fromDate);
					ps.setString(2, toDate);
					ps.setString(3, fromDate);
					ps.setString(4, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(5, "M");
					} else {
						ps.setString(5, status);
					}
					ps.setString(6, fromDate);
					ps.setString(7, toDate);
				}

				rs = ps.executeQuery();
				boolean dataflag = false;
				while (rs.next()) {

					dataflag = true;
					HashMap<String, String> invData = new HashMap<String, String>();

					log.info("status--" + status);

					if (!"RO".equalsIgnoreCase(status) && !"INV".equalsIgnoreCase(status)
							&& !"V".equalsIgnoreCase(status)) {

						if ("CM".equalsIgnoreCase(status)) {
							invData.put("COUNT", rs.getString("ICOUNT"));
							invData.put("PERCENTAGE", rs.getString("PERCENTAGE"));
							invData.put("AMOUNTInCR", rs.getString("AMOUNTInCR"));
							invData1.put("CM", invData);
						} else {
							invData.put("COUNT", rs.getString("ICOUNT"));
							invData.put("PERCENTAGE", rs.getString("PERCENTAGE"));
							invData.put("AMOUNTInCR", rs.getString("AMOUNTInCR"));
							invData1.put(rs.getString("OVERALLSTATUS"), invData);
						}
					} else {
						BigDecimal bdCount = new BigDecimal(Double.parseDouble(rs.getString("ICOUNT"))).setScale(2,RoundingMode.HALF_UP);
						BigDecimal bdPercentage = new BigDecimal(Double.parseDouble(rs.getString("PERCENTAGE"))).setScale(2, RoundingMode.HALF_UP);
						BigDecimal bdAmountInLac = new BigDecimal(Double.parseDouble(rs.getString("AMOUNTInCR"))).setScale(2, RoundingMode.HALF_UP);

						Double count1 = bdCount.doubleValue();
						Double percentage1 = bdPercentage.doubleValue();
						Double amountInLac1 = bdAmountInLac.doubleValue();
						count = Double.sum(count, count1);
						percentage = Double.sum(percentage, percentage1);
						amountInLac = Double.sum(amountInLac, amountInLac1);

						invData.put("COUNT", String.valueOf(String.valueOf(new BigDecimal(count).setScale(2, RoundingMode.HALF_UP))));
						invData.put("PERCENTAGE", String.valueOf(String.valueOf(new BigDecimal(percentage).setScale(2, RoundingMode.HALF_UP))));
						invData.put("AMOUNTInCR", String.valueOf(String.valueOf(new BigDecimal(amountInLac).setScale(2, RoundingMode.HALF_UP))));

						invData1.put("INVROV", invData);
						log.info(count + "-" + percentage + "-" + amountInLac);
					}
				}

				rs.close();
				ps.close();

				if (!dataflag) {
					log.info("getInvoiceSummeryFunctionWise - NO DATA " + status);
					HashMap<String, String> invData = new HashMap<String, String>();
					if ("V".equalsIgnoreCase(status) || "RO".equalsIgnoreCase(status)
							|| "INV".equalsIgnoreCase(status)) {
						if (!invData1.containsKey("INVROV")) {
							invData.put("COUNT", "0");
							invData.put("PERCENTAGE", "0.00");
							invData.put("AMOUNTInCR", "0.00");
							invData1.put("INVROV", invData);
						}
					} else {
						invData.put("COUNT", "0");
						invData.put("PERCENTAGE", "0.00");
						invData.put("AMOUNTInCR", "0.00");
						invData1.put(status, invData);
					}
				}
			}

			if (!invData1.isEmpty()) {
				invSummList.add(invData1);
			}

			if (invSummList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("invoiceSummery", invSummList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found.");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getInvoiceSummeryFunctionWise - error ");
			responsejson.put("message", "Error " + e.getMessage());
			jsonArray.add(responsejson);
			e.printStackTrace();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}


	@SuppressWarnings("unchecked")
	public JSONArray getInvoiceAgeing(String deptcode, String fromDate, String toDate, String myStatus) {

		log.info("getInvoiceAgeing -  FROM DATE : " + fromDate + " TO DATE : " + toDate + " DEPT CODE : "
				+ deptcode + " mystatus " + myStatus);

		boolean result;
		result = Validation.StringChecknull(deptcode);
		if (result == false) {
			responsejson.put("validation", "Dept. code is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		result = Validation.StringChecknull(fromDate);
		if (result == false) {
			responsejson.put("validation", "From date is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.validateDateFormat("dd-MM-yy", fromDate);
			if (result == false) {
				responsejson.put("validation", "From date should be in dd-MM-yy format.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(toDate);
		if (result == false) {
			responsejson.put("validation", "To date is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.validateDateFormat("dd-MM-yy", toDate);
			if (result == false) {
				responsejson.put("validation", "To date should be in dd-MM-yy format.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		log.info("getInvoiceAgeing - validation checked ");

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		List<String> statusList = new ArrayList<String>();
		/*
		 * statusList.add("P"); // Pending with GRN creator statusList.add("CM");
		 * //Pending with GRN confirmer statusList.add("M"); // Pending with GRN
		 * confirmer/Invoice Approver statusList.add("V"); // Return to vendor
		 * statusList.add("RO"); // Reopen for correction // statusList.add("O"); // On
		 * Hold // date:04-03-2023 Sachin Sir said removed // the hold option and counts
		 * in the list statusList.add("INV"); // invalid Invoice statusList.add("A"); //
		 * Approved statusList.add("PRO"); // Processed statusList.add("PP"); //
		 * Partially Paid statusList.add("PD"); // Paid
		 */

		if ("INVROV".equalsIgnoreCase(myStatus)) {
			statusList.add("INV");
			statusList.add("RO");
			statusList.add("V");
		} else {
			statusList.add(myStatus);
		}
		ArrayList<HashMap<String, HashMap<String, String>>> invAgeingList = new ArrayList<HashMap<String, HashMap<String, String>>>();

		try {

			String subQuery = "";

			if (!deptcode.equalsIgnoreCase("All")) {
				subQuery = subQuery + "deptcode = ? and";
			}

			con = DBConnection.getConnection();

			int invoiceCnt = 0;
			String inv_ageing = null;
			int pInv_lt_7days = 0;
			int pInv_bw_7_14days = 0;
			int pInv_bw_14_21days = 0;
			int pInv_GT_21days = 0;
			int pTotal_Inv = 0;
			double pPerc_LT_7Days = 0.00;
			double pPerc_BW_7_14Days = 0.00;
			double pPerc_BW_14_21Days = 0.00;
			double pPerc_GT_21Days = 0.00;
			double pAMT_LT7Days = 0.00;
			double pAMT_GT7_LT14Days = 0.00;
			double pAMT_GT14_LT21Days = 0.00;
			double pAMT_GT21Days = 0.00;
			LinkedHashMap<String, HashMap<String, String>> invAgeStatusWise = new LinkedHashMap<String, HashMap<String, String>>();

			for (String status : statusList) {

				log.info("getInvoiceAgeing - STATUS " + status);

				if ("P".equalsIgnoreCase(status) || "M".equalsIgnoreCase(status) || "V".equalsIgnoreCase(status)
						|| "RO".equalsIgnoreCase(status) || "INV".equalsIgnoreCase(status)) {

					if("M".equalsIgnoreCase(status)) {
						subQuery = subQuery + " grnnumber IS NOT NULL and ";
					}
					inv_ageing = "SELECT " + "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 7 AND overallstatus = ?  ) AS Inv_LT_7days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 AND overallstatus = ?  ) AS Inv_bw_7_14days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 AND overallstatus = ?  ) AS Inv_bw_14_21days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ?  ) AS Inv_GT_21days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND overallstatus = ?  ) AS Total_Inv, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 7 AND overallstatus = ? ) > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') <= 7 AND overallstatus = ? ) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ? ))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 7 AND overallstatus = ?  ) = 0  "
							+ "then 0 END AS Perc_LT_7Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 AND overallstatus = ? ) > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 AND overallstatus = ? ) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ? ))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 AND overallstatus = ?  ) = 0  "
							+ "then 0 END AS Perc_BW_7_14Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 AND overallstatus = ? ) > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 AND overallstatus = ? ) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ? ))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 AND overallstatus = ?  ) = 0  "
							+ "then 0 END AS Perc_BW_14_21Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ? ) > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ? ) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ? ))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ?  ) = 0 "
							+ " then 0 END AS Perc_GT_21Days, "
							+ " (SELECT case when (SUM(amount)/10000000) > 0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION WHERE  "
							+ subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 7 AND overallstatus = ? ) AS AMT_LT7Days ,  "
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION WHERE  "
							+ subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 "
							+ " AND overallstatus = ? ) AMT_GT7_LT14Days,"
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION "
							+ " WHERE  " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14  AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 "
							+ " AND overallstatus = ? ) AS AMT_GT14_LT21Days,"
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION WHERE  "
							+ subQuery + " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') "
							+ " AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ? ) AS AMT_GT21Days "
							+ "FROM DUAL";

				} else if ("CM".equalsIgnoreCase(status)) {

					inv_ageing = "SELECT " + "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 7 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AS Inv_LT_7days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AS Inv_bw_7_14days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AS Inv_bw_14_21days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ?  and grnnumber is null and "
							+ "  invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AS Inv_GT_21days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AS Total_Inv, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 7 AND overallstatus = ?  and grnnumber is null and "
							+ "  invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') <= 7 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 7 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) = 0  "
							+ "then 0 END AS Perc_LT_7Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) = 0  "
							+ "then 0 END AS Perc_BW_7_14Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) = 0  "
							+ "then 0 END AS Perc_BW_14_21Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION a WHERE " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) = 0 "
							+ " then 0 END AS Perc_GT_21Days, "
							+ " (SELECT case when (SUM(amount)/10000000) > 0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION a WHERE  "
							+ subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 7 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AS AMT_LT7Days ,  "
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION a WHERE  "
							+ subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 14 "
							+ " AND overallstatus = ?  and grnnumber is null and "
							+ "  invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AMT_GT7_LT14Days,"
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION a "
							+ " WHERE  " + subQuery
							+ " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 14  AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') < = 21 "
							+ " AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AS AMT_GT14_LT21Days,"
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION a WHERE  "
							+ subQuery + " CREATEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') "
							+ " AND to_date(sysdate,'DD-MM-YY')-to_date(CREATEDON,'DD-MM-YY') > 21 AND overallstatus = ?  and grnnumber is null and "
							+ " invoicenumber in (select invoicenumber from audit_acceptqty_behalf where ponumber=a.ponumber)) AS AMT_GT21Days "
							+ "FROM DUAL";

				} else {

					inv_ageing = "SELECT " + "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 7 AND overallstatus = ?  AND ONEXSTATUS='P' ) AS Inv_LT_7days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 14 AND overallstatus = ?  AND ONEXSTATUS='P' ) AS Inv_bw_7_14days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 21 AND overallstatus = ?  AND ONEXSTATUS='P' ) AS Inv_bw_14_21days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 21 AND overallstatus = ?  AND ONEXSTATUS='P' ) AS Inv_GT_21days, "
							+ "(SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND overallstatus = ?  AND ONEXSTATUS='P' ) AS Total_Inv, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 7 AND overallstatus = ?  AND ONEXSTATUS='P') > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') <= 7 AND overallstatus = ?  AND ONEXSTATUS='P') * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ?  AND ONEXSTATUS='P'))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 7 AND overallstatus = ?  AND ONEXSTATUS='P' ) = 0  "
							+ "then 0 END AS Perc_LT_7Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 14 AND overallstatus = ?  AND ONEXSTATUS='P') > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 14 AND overallstatus = ?  AND ONEXSTATUS='P') * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ?  AND ONEXSTATUS='P'))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 14 AND overallstatus = ?  AND ONEXSTATUS='P' ) = 0  "
							+ "then 0 END AS Perc_BW_7_14Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 21 AND overallstatus = ?  AND ONEXSTATUS='P') > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 21 AND overallstatus = ?  AND ONEXSTATUS='P') * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ?  AND ONEXSTATUS='P'))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 14 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 21 AND overallstatus = ?  AND ONEXSTATUS='P' ) = 0  "
							+ "then 0 END AS Perc_BW_14_21Days, "
							+ "Case When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 21 AND overallstatus = ?  AND ONEXSTATUS='P') > 0  "
							+ "then round((((SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 21 AND overallstatus = ?  AND ONEXSTATUS='P' ) * 100 ) / ( ( "
							+ "SELECT COUNT(*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon BETWEEN TO_DATE(?, 'DD-MM-YY') AND TO_DATE(?, 'DD-MM-YY') AND overallstatus = ?  AND ONEXSTATUS='P'))),2 ) "
							+ "When (SELECT COUNT (*) FROM GET_DASHBOARD_FOR_FUNCTION WHERE " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 21 AND overallstatus = ?  AND ONEXSTATUS='P' ) = 0 "
							+ " then 0 END AS Perc_GT_21Days, "
							+ " (SELECT case when (SUM(amount)/10000000) > 0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION WHERE  "
							+ subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 7 AND overallstatus = ?  AND ONEXSTATUS='P') AS AMT_LT7Days ,  "
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION WHERE  "
							+ subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 7 AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 14 "
							+ " AND overallstatus = ?  AND ONEXSTATUS='P') AMT_GT7_LT14Days,"
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION "
							+ " WHERE  " + subQuery
							+ " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 14  AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') < = 21 "
							+ " AND overallstatus = ?  AND ONEXSTATUS='P') AS AMT_GT14_LT21Days,"
							+ " (SELECT case when (SUM(amount)/10000000) >0 then TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') else TO_CHAR('00.00', 'fm9999999999D00') end FROM GET_DASHBOARD_FOR_FUNCTION WHERE  "
							+ subQuery + " modifiedon between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') "
							+ " AND to_date(sysdate,'DD-MM-YY')-to_date(modifiedon,'DD-MM-YY') > 21 AND overallstatus = ?  AND ONEXSTATUS='P') AS AMT_GT21Days "
							+ "FROM DUAL";

				}

				log.info("inv_ageing--" + inv_ageing);
				ps = con.prepareStatement(inv_ageing);

				if (!deptcode.equalsIgnoreCase("All")) {
					// Less than 15 days
					ps.setString(1, deptcode);
					ps.setString(2, fromDate);
					ps.setString(3, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(4, "M");
					} else {
						ps.setString(4, status);
					}

					// between 15 to 3o days
					ps.setString(5, deptcode);
					ps.setString(6, fromDate);
					ps.setString(7, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(8, "M");
					} else {
						ps.setString(8, status);
					}

					// between 31 to 6o days
					ps.setString(9, deptcode);
					ps.setString(10, fromDate);
					ps.setString(11, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(12, "M");
					} else {
						ps.setString(12, status);
					}

					// greater than 60 days
					ps.setString(13, deptcode);
					ps.setString(14, fromDate);
					ps.setString(15, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(16, "M");
					} else {
						ps.setString(16, status);
					}

					// Total invoices
					ps.setString(17, deptcode);
					ps.setString(18, fromDate);
					ps.setString(19, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(20, "M");
					} else {
						ps.setString(20, status);
					}

					// Calculating percentage for less than 15 days
					ps.setString(21, deptcode);
					ps.setString(22, fromDate);
					ps.setString(23, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(24, "M");
					} else {
						ps.setString(24, status);
					}
					ps.setString(25, deptcode);
					ps.setString(26, fromDate);
					ps.setString(27, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(28, "M");
					} else {
						ps.setString(28, status);
					}

					ps.setString(29, deptcode);
					ps.setString(30, fromDate);
					ps.setString(31, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(32, "M");
					} else {
						ps.setString(32, status);
					}
					ps.setString(33, deptcode);
					ps.setString(34, fromDate);
					ps.setString(35, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(36, "M");
					} else {
						ps.setString(36, status);
					}
					// Calculating percentage between 15 to 30 days
					ps.setString(37, deptcode);
					ps.setString(38, fromDate);
					ps.setString(39, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(40, "M");
					} else {
						ps.setString(40, status);
					}

					ps.setString(41, deptcode);
					ps.setString(42, fromDate);
					ps.setString(43, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(44, "M");
					} else {
						ps.setString(44, status);
					}

					ps.setString(45, deptcode);
					ps.setString(46, fromDate);
					ps.setString(47, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(48, "M");
					} else {
						ps.setString(48, status);
					}

					ps.setString(49, deptcode);
					ps.setString(50, fromDate);
					ps.setString(51, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(52, "M");
					} else {
						ps.setString(52, status);
					}

					// Calculating percentage between 31 to 60 days
					ps.setString(53, deptcode);
					ps.setString(54, fromDate);
					ps.setString(55, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(56, "M");
					} else {
						ps.setString(56, status);
					}

					ps.setString(57, deptcode);
					ps.setString(58, fromDate);
					ps.setString(59, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(60, "M");
					} else {
						ps.setString(60, status);
					}

					ps.setString(61, deptcode);
					ps.setString(62, fromDate);
					ps.setString(63, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(64, "M");
					} else {
						ps.setString(64, status);
					}

					ps.setString(65, deptcode);
					ps.setString(66, fromDate);
					ps.setString(67, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(68, "M");
					} else {
						ps.setString(68, status);
					}

					// Calculating percentage greater than 60 days
					ps.setString(69, deptcode);
					ps.setString(70, fromDate);
					ps.setString(71, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(72, "M");
					} else {
						ps.setString(72, status);
					}

					ps.setString(73, deptcode);
					ps.setString(74, fromDate);
					ps.setString(75, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(76, "M");
					} else {
						ps.setString(76, status);
					}

					ps.setString(77, deptcode);
					ps.setString(78, fromDate);
					ps.setString(79, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(80, "M");
					} else {
						ps.setString(80, status);
					}

					ps.setString(81, deptcode);
					ps.setString(82, fromDate);
					ps.setString(83, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(84, "M");
					} else {
						ps.setString(84, status);
					}

					ps.setString(85, deptcode);
					ps.setString(86, fromDate);
					ps.setString(87, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(88, "M");
					} else {
						ps.setString(88, status);
					}
					ps.setString(89, deptcode);
					ps.setString(90, fromDate);
					ps.setString(91, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(92, "M");
					} else {
						ps.setString(92, status);
					}

					ps.setString(93, deptcode);
					ps.setString(94, fromDate);
					ps.setString(95, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(96, "M");
					} else {
						ps.setString(96, status);
					}
					ps.setString(97, deptcode);
					ps.setString(98, fromDate);
					ps.setString(99, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(100, "M");
					} else {
						ps.setString(100, status);
					}

				} else {

					// Less than 15 days
					ps.setString(1, fromDate);
					ps.setString(2, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(3, "M");
					} else {
						ps.setString(3, status);
					}

					// between 15 to 3o days
					ps.setString(4, fromDate);
					ps.setString(5, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(6, "M");
					} else {
						ps.setString(6, status);
					}

					// between 31 to 6o days
					ps.setString(7, fromDate);
					ps.setString(8, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(9, "M");
					} else {
						ps.setString(9, status);
					}

					// greater than 60 days
					ps.setString(10, fromDate);
					ps.setString(11, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(12, "M");
					} else {
						ps.setString(12, status);
					}

					// Total invoices
					ps.setString(13, fromDate);
					ps.setString(14, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(15, "M");
					} else {
						ps.setString(15, status);
					}
					// Calculating percentage for less than 15 days
					ps.setString(16, fromDate);
					ps.setString(17, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(18, "M");
					} else {
						ps.setString(18, status);
					}
					ps.setString(19, fromDate);
					ps.setString(20, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(21, "M");
					} else {
						ps.setString(21, status);
					}
					ps.setString(22, fromDate);
					ps.setString(23, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(24, "M");
					} else {
						ps.setString(24, status);
					}
					ps.setString(25, fromDate);
					ps.setString(26, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(27, "M");
					} else {
						ps.setString(27, status);
					}
					// Calculating percentage between 15 to 30 days
					ps.setString(28, fromDate);
					ps.setString(29, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(30, "M");
					} else {
						ps.setString(30, status);
					}
					ps.setString(31, fromDate);
					ps.setString(32, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(33, "M");
					} else {
						ps.setString(33, status);
					}

					ps.setString(34, fromDate);
					ps.setString(35, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(36, "M");
					} else {
						ps.setString(36, status);
					}

					ps.setString(37, fromDate);
					ps.setString(38, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(39, "M");
					} else {
						ps.setString(39, status);
					}

					// Calculating percentage between 31 to 60 days
					ps.setString(40, fromDate);
					ps.setString(41, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(42, "M");
					} else {
						ps.setString(42, status);
					}
					ps.setString(43, fromDate);
					ps.setString(44, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(45, "M");
					} else {
						ps.setString(45, status);
					}

					ps.setString(46, fromDate);
					ps.setString(47, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(48, "M");
					} else {
						ps.setString(48, status);
					}
					ps.setString(49, fromDate);
					ps.setString(50, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(51, "M");
					} else {
						ps.setString(51, status);
					}

					// Calculating percentage greater than 60 days
					ps.setString(52, fromDate);
					ps.setString(53, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(54, "M");
					} else {
						ps.setString(54, status);
					}
					ps.setString(55, fromDate);
					ps.setString(56, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(57, "M");
					} else {
						ps.setString(57, status);
					}
					ps.setString(58, fromDate);
					ps.setString(59, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(60, "M");
					} else {
						ps.setString(60, status);
					}

					ps.setString(61, fromDate);
					ps.setString(62, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(63, "M");
					} else {
						ps.setString(63, status);
					}

					ps.setString(64, fromDate);
					ps.setString(65, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(66, "M");
					} else {
						ps.setString(66, status);
					}

					ps.setString(67, fromDate);
					ps.setString(68, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(69, "M");
					} else {
						ps.setString(69, status);
					}

					ps.setString(70, fromDate);
					ps.setString(71, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(72, "M");
					} else {
						ps.setString(72, status);
					}

					ps.setString(73, fromDate);
					ps.setString(74, toDate);
					if ("CM".equalsIgnoreCase(status)) {
						ps.setString(75, "M");
					} else {
						ps.setString(75, status);
					}

				}

				rs = ps.executeQuery();

				boolean dataFlag = false;

				while (rs.next()) {
					dataFlag = true;
					LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();

					if (!"RO".equalsIgnoreCase(status) && !"INV".equalsIgnoreCase(status)
							&& !"V".equalsIgnoreCase(status)) {

						if ("CM".equalsIgnoreCase(status)) {

							invData.put("Invoices less than 7 days", rs.getString("Inv_LT_7days"));
							invData.put("Invoices between 7 to 14 days", rs.getString("Inv_bw_7_14days"));
							invData.put("Invoices between 14 to 21 days", rs.getString("Inv_bw_14_21days"));
							invData.put("Invoices greater than 21 days", rs.getString("Inv_GT_21days"));
							invData.put("Total Invoices", rs.getString("Total_Inv"));
							invData.put("Percentage of Invoices less than 7 days", rs.getString("Perc_LT_7Days"));
							invData.put("Percentage of Invoices between 7 to 14 days",
									rs.getString("Perc_BW_7_14Days"));
							invData.put("Percentage of Invoices between 14 to 21 days",
									rs.getString("Perc_BW_14_21Days"));
							invData.put("Percentage of Invoices greater than 21 days", rs.getString("Perc_GT_21Days"));
							invData.put("Amount of Invoices less than 7 days", rs.getString("AMT_LT7Days"));
							invData.put("Amount of Invoices between 7 to 14 days", rs.getString("AMT_GT7_LT14Days"));
							invData.put("Amount of Invoices between 14 to 21 days", rs.getString("AMT_GT14_LT21Days"));
							invData.put("Amount of Invoices greater than 21 days", rs.getString("AMT_GT21Days"));
							invAgeStatusWise.put("CM", invData);
							// invAgeingList.add(invAgeStatusWise);

							invoiceCnt = invoiceCnt + Integer.parseInt(rs.getString("Total_Inv").trim());

						} else {

							invData.put("Invoices less than 7 days", rs.getString("Inv_LT_7days"));
							invData.put("Invoices between 7 to 14 days", rs.getString("Inv_bw_7_14days"));
							invData.put("Invoices between 14 to 21 days", rs.getString("Inv_bw_14_21days"));
							invData.put("Invoices greater than 21 days", rs.getString("Inv_GT_21days"));
							invData.put("Total Invoices", rs.getString("Total_Inv"));
							invData.put("Percentage of Invoices less than 7 days", rs.getString("Perc_LT_7Days"));
							invData.put("Percentage of Invoices between 7 to 14 days",
									rs.getString("Perc_BW_7_14Days"));
							invData.put("Percentage of Invoices between 14 to 21 days",
									rs.getString("Perc_BW_14_21Days"));
							invData.put("Percentage of Invoices greater than 21 days", rs.getString("Perc_GT_21Days"));
							invData.put("Amount of Invoices less than 7 days", rs.getString("AMT_LT7Days"));
							invData.put("Amount of Invoices between 7 to 14 days", rs.getString("AMT_GT7_LT14Days"));
							invData.put("Amount of Invoices between 14 to 21 days", rs.getString("AMT_GT14_LT21Days"));
							invData.put("Amount of Invoices greater than 21 days", rs.getString("AMT_GT21Days"));
							invAgeStatusWise.put(status, invData);
							// invAgeingList.add(invAgeStatusWise);

							invoiceCnt = invoiceCnt + Integer.parseInt(rs.getString("Total_Inv").trim());

						}

					} else {
						// LinkedHashMap<String, HashMap<String, String>> invAgeStatusWise = new
						// LinkedHashMap<String, HashMap<String, String>>();
						// LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();

						pInv_lt_7days = pInv_lt_7days + Integer.parseInt(rs.getString("Inv_LT_7days"));
						pInv_bw_7_14days = pInv_bw_7_14days + Integer.parseInt(rs.getString("Inv_bw_7_14days"));
						pInv_bw_14_21days = pInv_bw_14_21days + Integer.parseInt(rs.getString("Inv_bw_14_21days"));
						pInv_GT_21days = pInv_GT_21days + Integer.parseInt(rs.getString("Inv_GT_21days"));

						pTotal_Inv = pTotal_Inv + Integer.parseInt(rs.getString("Total_Inv"));
						invoiceCnt = invoiceCnt + Integer.parseInt(rs.getString("Total_Inv").trim());
						pPerc_LT_7Days = pPerc_LT_7Days + Double.parseDouble(rs.getString("Perc_LT_7Days"));
						pPerc_BW_7_14Days = pPerc_BW_7_14Days + Double.parseDouble(rs.getString("Perc_BW_7_14Days"));
						pPerc_BW_14_21Days = pPerc_BW_14_21Days + Double.parseDouble(rs.getString("Perc_BW_14_21Days"));
						pPerc_GT_21Days = pPerc_GT_21Days + Double.parseDouble(rs.getString("Perc_GT_21Days"));

						pAMT_LT7Days = pAMT_LT7Days + Double.parseDouble(rs.getString("AMT_LT7Days"));
						pAMT_GT7_LT14Days = pAMT_GT7_LT14Days + Double.parseDouble(rs.getString("AMT_GT7_LT14Days"));
						pAMT_GT14_LT21Days = pAMT_GT14_LT21Days + Double.parseDouble(rs.getString("AMT_GT14_LT21Days"));
						pAMT_GT21Days = pAMT_GT21Days + Double.parseDouble(rs.getString("AMT_GT21Days"));

						System.out.print(pInv_lt_7days + "--");
						System.out.print(pInv_bw_7_14days + "--");
						System.out.print(pInv_bw_14_21days + "--");
						System.out.print(pInv_GT_21days + "--");
						System.out.print(pTotal_Inv + "--");
						System.out.print(pPerc_LT_7Days + "--");
						System.out.print(pPerc_BW_7_14Days + "--");
						System.out.print(pPerc_BW_14_21Days + "--");
						System.out.print(pPerc_GT_21Days + "--");
						System.out.print(pAMT_LT7Days + "--");
						System.out.print(pAMT_GT7_LT14Days + "--");
						System.out.print(pAMT_GT14_LT21Days + "--");
						System.out.print(pAMT_GT21Days + "--");

						invData.put("Invoices less than 7 days", String.valueOf(pInv_lt_7days));
						invData.put("Invoices between 7 to 14 days", String.valueOf(pInv_bw_7_14days));
						invData.put("Invoices between 14 to 21 days", String.valueOf(pInv_bw_14_21days));
						invData.put("Invoices greater than 21 days", String.valueOf(pInv_GT_21days));

						invData.put("Total Invoices", String.valueOf(pTotal_Inv));

						invData.put("Percentage of Invoices less than 7 days", String.valueOf(pPerc_LT_7Days));
						invData.put("Percentage of Invoices between 7 to 14 days", String.valueOf(pPerc_BW_7_14Days));
						invData.put("Percentage of Invoices between 14 to 21 days", String.valueOf(pPerc_BW_14_21Days));
						invData.put("Percentage of Invoices greater than 21 days", String.valueOf(pPerc_GT_21Days));

						invData.put("Amount of Invoices less than 7 days", String.valueOf(pAMT_LT7Days));
						invData.put("Amount of Invoices between 7 to 14 days", String.valueOf(pAMT_GT7_LT14Days));
						invData.put("Amount of Invoices between 14 to 21 days", String.valueOf(pAMT_GT14_LT21Days));
						invData.put("Amount of Invoices greater than 21 days", String.valueOf(pAMT_GT21Days));

						invAgeStatusWise.put("INVROV", invData);
						// invAgeingList.add(invAgeStatusWise);
					}
				}

				rs.close();
				ps.close();

				if (!dataFlag) {
					log.info("getInvoiceAgeing - NO DATA " + dataFlag + " Status : " + status);
					// LinkedHashMap<String, HashMap<String, String>> invAgeStatusWise = new
					// LinkedHashMap<String, HashMap<String, String>>();
					LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();
					invData.put("Invoices less than 7 days", "0");
					invData.put("Invoices between 7 to 14 days", "0");
					invData.put("Invoices between 14 to 21 days", "0");
					invData.put("Invoices greater than 21 days", "0");
					invData.put("Total Invoices", "0");
					invData.put("Percentage of Invoices less than 7 days", "0");
					invData.put("Percentage of Invoices between 7 to 14 days", "0");
					invData.put("Percentage of Invoices between 14 to 21 days", "0");
					invData.put("Percentage of Invoices greater than 21 days", "0");
					invData.put("Amount of Invoices less than 7 days", "0.00");
					invData.put("Amount of Invoices between 7 to 14 days", "0.00");
					invData.put("Amount of Invoices between 14 to 21 days", "0.00");
					invData.put("Amount of Invoices greater than 21 days", "0.00");
					invAgeStatusWise.put(status, invData);
					// invAgeingList.add(invAgeStatusWise);
				}
				// invAgeingList.add(invAgeStatusWise);
				log.info("getInvoiceAgeing - END " + invAgeingList.size());
			}
			invAgeingList.add(invAgeStatusWise);

			log.info("getInvoiceAgeing - data size " + invAgeingList.size());
			if (invAgeingList.size() > 0) {
				responsejson.put("invoiceAgeingList", invAgeingList);
				//responsejson.put("Total Invoices", invoiceCnt);
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found.");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getInvoiceAgeing - error ",e.fillInStackTrace());
			responsejson.put("message", "Error " + e.getMessage());
			jsonArray.add(responsejson);
			e.printStackTrace();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}


	@SuppressWarnings("unchecked")
	public JSONArray getInvoiceDetails(String deptcode, String fromDate, String toDate, String status) {

		log.info("getInvoiceDetails - DEPTCODE : " + deptcode + " FROM DATE : " + fromDate + " TO DATE : " + toDate
				+ " STATUS : ");
		boolean result;
		result = Validation.StringChecknull(deptcode);
		if (result == false) {
			responsejson.put("validation", "Dept. Code is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		result = Validation.StringChecknull(fromDate);
		if (result == false) {
			responsejson.put("validation", "From date is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.validateDateFormat("dd-MM-yy", fromDate);
//			log.info("FRM DT "+result);
			if (result == false) {
				responsejson.put("validation", "From date should be in dd-MM-yy format.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(toDate);
		if (result == false) {
			responsejson.put("validation", "From date is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.validateDateFormat("dd-MM-yy", toDate);
			if (result == false) {
				responsejson.put("validation", "To date should be in dd-MM-yy format.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(status);
		if (result == false) {
			responsejson.put("validation", "Status is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		log.info("getInvoiceDetails - validation checked ");

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			String subQuery = "";

			if (!deptcode.equalsIgnoreCase("All")) {
				subQuery = subQuery + "deptcode = ? and ";
			}

			if (!status.equalsIgnoreCase("All")) {
				subQuery = subQuery + "OVERALLSTATUS = ? and";
			}

			String inv_details = "SELECT * from GET_DASHBOARD_FOR_FUNCTION " + "where " + subQuery
					+ " MODIFIEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') ";

			con = DBConnection.getConnection();
			ps = con.prepareStatement(inv_details);

			if (!deptcode.equalsIgnoreCase("All") && !status.equalsIgnoreCase("All")) {
				ps.setString(1, deptcode);
				ps.setString(2, status);
				ps.setString(3, fromDate);
				ps.setString(4, toDate);
			} else if (!deptcode.equalsIgnoreCase("All")) {
				ps.setString(1, deptcode);
				ps.setString(2, fromDate);
				ps.setString(3, toDate);
			} else if (!status.equalsIgnoreCase("All")) {
				ps.setString(1, status);
				ps.setString(2, fromDate);
				ps.setString(3, toDate);
			} else {
				ps.setString(1, fromDate);
				ps.setString(2, toDate);
			}

			rs = ps.executeQuery();

			ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
			while (rs.next()) {

				HashMap<String, String> invData = new HashMap<String, String>();
				invData.put("INVOICENUMBER", rs.getString("INVOICENUMBER"));
				invData.put("PONUMBER", rs.getString("PONUMBER"));
				invData.put("BUSINESSPARTNEROID", rs.getString("BUSINESSPARTNEROID"));
				invData.put("MESSAGE", rs.getString("MESSAGE"));
				invData.put("REQUSITIONER", rs.getString("REQUSITIONER"));
				invData.put("BUYER", rs.getString("BUYER"));
				invData.put("AMOUNT", rs.getString("AMOUNT"));
				invData.put("MACOUNT", rs.getString("MACOUNT"));
				invData.put("HOLDCOUNT", rs.getString("HOLDCOUNT"));
				invData.put("PLANT", rs.getString("PLANT"));
//				POImpl po = new POImpl();
//				invData.put("PLANTNAME", po.getPlantName(rs.getString("PLANT")));
				invData.put("VENDORID", rs.getString("VENDORID"));
				invData.put("VENDORNAME", rs.getString("BUSINESSPARTNERTEXT"));
				invData.put("OVERALLSTATUS", rs.getString("OVERALLSTATUS"));
				invData.put("INVOICEDATE", rs.getString("INVOICEDATE"));
				invData.put("TOTALAMOUNT", rs.getString("TOTALAMOUNT"));
				invData.put("MATERIAL_TYPE", rs.getString("MATERIAL_TYPE"));
				invData.put("PGQ", rs.getString("PGQ"));
				invData.put("ONEXSTATUS", rs.getString("ONEXSTATUS"));
				invData.put("ACTUALFILENAME", rs.getString("ACTUALFILENAME"));
				invData.put("SAVEDFILENAME", rs.getString("SAVEDFILENAME"));
				invData.put("PAYMENTAMOUNT", rs.getString("PAYMENTAMOUNT"));
				invData.put("CREDITNOTENO", rs.getString("CREDITNOTENO"));
				invData.put("CREDITADVICENO", rs.getString("CREDITADVICENO"));
				invData.put("TOTALAMTINCTAXES", rs.getString("TOTALAMTINCTAXES"));
				invData.put("TAXAMOUNT", rs.getString("TAXAMOUNT"));
				invData.put("EXPENSESHEETID",
						rs.getString("EXPENSESHEETID") != null ? rs.getString("EXPENSESHEETID").toString() : "NA");
				invData.put("MPO", rs.getString("MPO"));
				invData.put("ALLPO", rs.getString("ALLPO"));
				invData.put("DEPTCODE", rs.getString("DEPTCODE"));
				invoiceList.add(invData);

			}

			rs.close();
			ps.close();
			log.info("getInvoiceDetails - data size " + invoiceList.size());
			if (invoiceList.size() > 0) {
				responsejson.put("invoiceDetails", invoiceList);
				responsejson.put("Total Invoices", invoiceList.size());
				responsejson.put("message", "Success");
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found.");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getInvoiceDetails - error ", e.fillInStackTrace());
			responsejson.put("message", "Error " + e.getMessage());
			jsonArray.add(responsejson);
			e.printStackTrace();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public JSONArray getDeptDetails(String email, String mode) {

		log.info("getDeptDetails - email : " + email + " mode : " + mode);

		boolean result;
		result = Validation.StringChecknull(email);
		if (result == false) {
			responsejson.put("validation", "Email Id not found.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		result = Validation.StringChecknull(mode);
		if (result == false) {
			responsejson.put("validation", "Mode not found.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		log.info("getDeptDetails - validation checked ");

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {

			String subQuery = "";

			if (!mode.equalsIgnoreCase("Payer")) {
				subQuery = "where REF_SRNO IN (select COMPANYCODE from FUNCTIONALDIRECTORS where EMAILID = ? and STATUS='A') "
						+ "and REF_CODE IN (select DEPTCODE from FUNCTIONALDIRECTORS where EMAILID = ? and STATUS='A') ";
			}

			String deptQuery = "select * from DeptDetails " + subQuery;

			con = DBConnection.getConnection();
			ps = con.prepareStatement(deptQuery);

			if (!mode.equalsIgnoreCase("Payer")) {
				ps.setString(1, email);
				ps.setString(2, email);
			}

			rs = ps.executeQuery();

			ArrayList<Object> deptList = new ArrayList<Object>();

			if ("Payer".equalsIgnoreCase(mode)) {
				HashMap<String, String> deptData = new HashMap<String, String>();
				deptData.put("COMPCODE", "All");
				deptData.put("DEPTCODE", "All");
				deptData.put("DESCRIPTION", "All");
				deptList.add(deptData);
			}

			while (rs.next()) {
				HashMap<String, String> deptData = new HashMap<String, String>();
				deptData.put("COMPCODE", rs.getString("REF_SRNO"));
				deptData.put("DEPTCODE", rs.getString("REF_CODE"));
				deptData.put("DESCRIPTION", rs.getString("REF_DESCRIPTION"));

				deptList.add(deptData);

			}

			rs.close();
			ps.close();
			log.info("getDeptDetails - data size " + deptList.size());
			if (deptList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("deptdetails", deptList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found.");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getDeptDetails - error ", e.fillInStackTrace());
			responsejson.put("message", "Error " + e.getMessage());
			jsonArray.add(responsejson);
			e.printStackTrace();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	@SuppressWarnings("unchecked")
	public JSONArray getInvoiceSummeryFunctionWise_Chart(String deptcode, String fromDate, String toDate) {

		log.info("getInvoiceSummeryFunctionWise_Chart - DEPTCODE : " + deptcode + " FROM DATE : " + fromDate
				+ " TO DATE : " + toDate);
		boolean result;
		result = Validation.StringChecknull(deptcode);
		if (result == false) {
			responsejson.put("validation", "Dept. code is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		}

		result = Validation.StringChecknull(fromDate);
		if (result == false) {
			responsejson.put("validation", "From date is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.validateDateFormat("dd-MM-yy", fromDate);
			if (result == false) {
				responsejson.put("validation", "From date should be in dd-MM-yy format.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		result = Validation.StringChecknull(toDate);
		if (result == false) {
			responsejson.put("validation", "To date is mandatory.");
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
			return jsonArray;
		} else {
			result = Validation.validateDateFormat("dd-MM-yy", toDate);
			if (result == false) {
				responsejson.put("validation", "To date should be in dd-MM-yy format.");
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
				return jsonArray;
			}
		}

		log.info("getInvoiceSummeryFunctionWise_Chart - validation checked ");

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {

			String subQuery = "";

			if (!deptcode.equalsIgnoreCase("All")) {
				subQuery = subQuery + "deptcode = ? and";
			}

			String inv_summery = "SELECT OVERALLSTATUS,count(*) as ICOUNT, "
					+ "(select count(*) from GET_DASHBOARD_FOR_FUNCTION where " + subQuery
					+ " MODIFIEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') ) as TOTALSUM, "
					+ "round(((count(*)*100)/(select count(*) from GET_DASHBOARD_FOR_FUNCTION where " + subQuery
					+ " MODIFIEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') )),2) as PERCENTAGE "
					+ "from GET_DASHBOARD_FOR_FUNCTION " + "where " + subQuery
					+ " MODIFIEDON between TO_DATE(?,'DD-MM-YY') AND TO_DATE(?,'DD-MM-YY') " + "group by overallstatus "
					+ "order by 1 ";

			con = DBConnection.getConnection();
			ps = con.prepareStatement(inv_summery);

			if (!deptcode.equalsIgnoreCase("All")) {
				ps.setString(1, deptcode);
				ps.setString(2, fromDate);
				ps.setString(3, toDate);

				ps.setString(4, deptcode);
				ps.setString(5, fromDate);
				ps.setString(6, toDate);

				ps.setString(7, deptcode);
				ps.setString(8, fromDate);
				ps.setString(9, toDate);
			} else {
				ps.setString(1, fromDate);
				ps.setString(2, toDate);

				ps.setString(3, fromDate);
				ps.setString(4, toDate);

				ps.setString(5, fromDate);
				ps.setString(6, toDate);
			}

			rs = ps.executeQuery();

//			ArrayList<Object> invSummList = new ArrayList<Object>();
//			LinkedHashMap<String, Object> invData1 = new LinkedHashMap<String,Object>();

			List<String> statusList = new ArrayList<String>();
			statusList.add("P");
			statusList.add("M");
			statusList.add("O");
			statusList.add("A");
			statusList.add("V");
			statusList.add("RO");
			statusList.add("PRO");
			statusList.add("PP");
			statusList.add("PD");

			HashMap<String, String> statusMap = new HashMap<String, String>();
			statusMap.put("P", "Pending with GRN Creator");

			statusMap.put("M", "Pending with GRN Confirmer / Invoice Approver");
			statusMap.put("CM", "Pending with GRN Confirmer / Invoice Approver");

			statusMap.put("V", "Return to Vendor");
			statusMap.put("RO", "Return to Vendor");

			statusMap.put("O", "On Hold(with GRN Creator /  GRN Confirmer / Invoice Approver)");
			statusMap.put("CO", "On Hold(with GRN Creator /  GRN Confirmer / Invoice Approver)");

			statusMap.put("A", "Approved");
			statusMap.put("PRO", "Processed");
			statusMap.put("PP", "Partially Paid");
			statusMap.put("PD", "Paid");

			ArrayList<String> labels = new ArrayList<String>();
			ArrayList<String> data = new ArrayList<String>();
			ArrayList<String> data_Perc = new ArrayList<String>();

			ArrayList<String> color = new ArrayList<String>();

			ArrayList<String> bg_color = new ArrayList<String>();
			bg_color.add("rgb(255, 99, 132)");
			bg_color.add("rgb(54, 162, 235)");
			bg_color.add("rgb(255, 205, 86)");

			bg_color.add("rgb(255, 99, 112)");
			bg_color.add("rgb(54, 162, 215)");
			bg_color.add("rgb(255, 205, 66)");

			bg_color.add("rgb(255, 99, 152)");
			bg_color.add("rgb(54, 162, 255)");
			bg_color.add("rgb(255, 205, 96)");

			bg_color.add("rgb(255, 79, 132)");
			bg_color.add("rgb(54, 142, 235)");
			bg_color.add("rgb(255, 185, 86)");

			int i = 0;

			LinkedHashMap<String, Object> map1 = new LinkedHashMap<String, Object>();

			while (rs.next()) {
//				HashMap<String, String> invData = new HashMap<String, String>();
//				invData.put("COUNT", rs.getString("ICOUNT"));
//				invData.put("PERCENTAGE", rs.getString("PERCENTAGE"));	
//				invData1.put("Total Invoices", rs.getString("TOTALSUM"));
//				invData1.put(rs.getString("OVERALLSTATUS"), invData);

//				String val=statusMap.get(rs.getString("OVERALLSTATUS"));

//				labels.add(rs.getString("OVERALLSTATUS"));
//				data.add(rs.getString("ICOUNT"));
//				data_Perc.add(rs.getString("PERCENTAGE"));
//				color.add(bg_color.get(i));
//				i++;

				if (map1.containsKey(statusMap.get(rs.getString("OVERALLSTATUS")))) {
					String val = (String) map1.get(statusMap.get(rs.getString("OVERALLSTATUS")));

					Double percentage = Double.parseDouble(val) + Double.parseDouble(rs.getString("PERCENTAGE"));

					map1.put(statusMap.get(rs.getString("OVERALLSTATUS")), percentage.toString());
				} else {
					map1.put(statusMap.get(rs.getString("OVERALLSTATUS")), rs.getString("PERCENTAGE"));
				}

			}

			for (Map.Entry<String, Object> entry : map1.entrySet()) {
				log.info("Key = " + entry.getKey() + ", Value = " + entry.getValue());

				labels.add(entry.getKey());
				data.add((String) entry.getValue());
				color.add(bg_color.get(i));
				i++;
			}

			LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
			if (!labels.isEmpty()) {
				responseMap.put("type", "pie");

				LinkedHashMap<String, Object> outermap = new LinkedHashMap<String, Object>();
				outermap.put("labels", labels);

				ArrayList<Object> innerList = new ArrayList<Object>();
				LinkedHashMap<String, Object> innermap = new LinkedHashMap<String, Object>();
				innermap.put("label", "Invoice Summery");
				innermap.put("data", data);
				innermap.put("backgroundColor", color);
				innermap.put("hoverOffset", 4);
				innerList.add(innermap);

				outermap.put("datasets", innerList);

				responseMap.put("data", outermap);

				LinkedHashMap<String, Object> optionmap = new LinkedHashMap<String, Object>();

				LinkedHashMap<String, Object> optioninnermap = new LinkedHashMap<String, Object>();
				optioninnermap.put("display", true);
				optionmap.put("legend", optioninnermap);

				LinkedHashMap<String, Object> optioninnermap1 = new LinkedHashMap<String, Object>();
				optioninnermap1.put("display", true);
				optioninnermap1.put("text", "Invoice Summery Function Wise");
				optionmap.put("title", optioninnermap1);

				responseMap.put("options", optionmap);
			}

			rs.close();
			ps.close();
			log.info("getInvoiceSummeryFunctionWise_Chart - data size " + responseMap.size());
			if (responseMap.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("invoiceSummery", responseMap);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found.");
				jsonArray.add(responsejson);
			}
		} catch (Exception e) {
			log.error("getInvoiceSummeryFunctionWise - error ", e.fillInStackTrace());
			responsejson.put("message", "Error " + e.getMessage());
			jsonArray.add(responsejson);
			e.printStackTrace();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}
	
	@SuppressWarnings("unchecked")
	public JSONArray getBigFiveFunctionData(String email, String mode) {

		log.info("getBigFiveFunctionData - email : " + email + " mode : " + mode);

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		ArrayList<Object> bigFiveFunctionList = new ArrayList<Object>();
		
		List<String> endUserProcessList = new ArrayList<String>();
		endUserProcessList.add("P");  // Pending with GRN creator
		endUserProcessList.add("CM"); // Pending with GRN confirmer
		endUserProcessList.add("M");  // Pending with GRN confirmer/Invoice Approver
		
				
		try {
			con = DBConnection.getConnection();
						
			LinkedHashMap<String, Object> functionWiseInvData = new LinkedHashMap<String, Object>();
			LinkedHashMap<String, Object> otherFunctionsInvData = new LinkedHashMap<String, Object>();
			
			Double p_count = 0.00;
			Double p_percentage = 0.00;
			Double p_amountInLac = 0.00;
			Double p_total = 0.00;
			Double p_G21L45 = 0.00;
			Double p_G45L90 = 0.00;
			Double p_G90 = 0.00;
			
			Double cm_count = 0.00;
			Double cm_percentage = 0.00;
			Double cm_amountInLac = 0.00;
			Double cm_G21L45 = 0.00;
			Double cm_G45L90 = 0.00;
			Double cm_G90 = 0.00;
			
			Double m_count = 0.00;
			Double m_percentage = 0.00;
			Double m_amountInLac = 0.00;
			Double m_G21L45 = 0.00;
			Double m_G45L90 = 0.00;
			Double m_G90 = 0.00;
			
			for(String status:endUserProcessList) {
				
				log.info("status - " + status);
				
				String filterQuery="";
				String filterQuery1 = "";
				
				if("CM".equals(status)) {
					filterQuery= " and f.grnnumber is null and invoicenumber in ( select invoicenumber from audit_acceptqty_behalf where ponumber = f.ponumber ) ";
					filterQuery1 = " and grnnumber is null ";
				}else if("M".equals(status)) {
					filterQuery= " and f.grnnumber is not null ";
					filterQuery1 = " and grnnumber is not null ";
				}
				/*
				else {
					filterQuery1 = " and grnnumber is null ";
				}
		*/
				String dateQuery1=" and sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') > 21  ";
				String dateQuery2=" and sysdate - TO_DATE(f.CREATEDON, 'dd-mm-yy') > 21 ";
				
				String deptCodeQuery = "select deptcode from ( select f.deptcode,(select count(*) from GET_DASHBOARD_FOR_FUNCTION  where overallstatus in('P','M') and f.deptcode=deptcode ) AS TOTALSUM "
						+ "from GET_DASHBOARD_FOR_FUNCTION f where f.overallstatus in('P','M')  group by f.deptcode order by 2 desc) "
						+ "where ROWNUM <= 5 ";
				
				String dateRangeQuery =  " ( SELECT COUNT(*) FROM get_dashboard_for_function WHERE overallstatus = ? AND f.deptcode = deptcode " + filterQuery1
						+ " AND sysdate - to_date(createdon, 'dd-mm-yy') > 21 AND sysdate - to_date(createdon, 'dd-mm-yy') <= 45 ) AS G21L45, "
						+ " ( SELECT COUNT(*) FROM get_dashboard_for_function WHERE overallstatus = ? AND f.deptcode = deptcode " + filterQuery1 
						+ " AND sysdate - to_date(createdon, 'dd-mm-yy') > 45 AND sysdate - to_date(createdon, 'dd-mm-yy') <= 90 ) AS G45L90 , "
						+ " (SELECT COUNT(*) FROM get_dashboard_for_function WHERE overallstatus = ? AND f.deptcode = deptcode "+filterQuery1
						+ " AND sysdate - to_date(createdon, 'dd-mm-yy') > 90 ) AS G90 " ;
				
				
				String bigFiveFunctionQuery="SELECT f.deptcode,d.ref_description as function,f.overallstatus,count(*) as ICOUNT, "
						+ "(select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus = ? and f.deptcode=deptcode " + filterQuery1 + dateQuery1 + " ) as TOTALSUM, "
						+ "round(((((select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus = ? and f.deptcode=deptcode " + filterQuery1 + dateQuery1
						+ " ))*100)/(select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus in ('P','M') and f.deptcode=deptcode " +  dateQuery1 +" )),2) as PERCENTAGE, "
						+ "TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') as AMOUNTInCR, "
						+ dateRangeQuery
						+ "from GET_DASHBOARD_FOR_FUNCTION f "
						+ "join deptdetails d on d.ref_code = f.deptcode "
						+ "where f.overallstatus = ? "
						+ "and f.deptcode IN ("+ deptCodeQuery +") "
						+  dateQuery2 + filterQuery + dateQuery1
						+ "group by f.deptcode,d.ref_description,f.overallstatus "
						+ "order by 4 desc,1,3 ";
				
				String otherFunctionQuery="SELECT f.deptcode,d.ref_description as function,f.overallstatus,count(*) as ICOUNT, "
						+ "(select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus = ? and f.deptcode=deptcode "+ filterQuery1 + dateQuery1 + " ) as TOTALSUM, "
						+ "round(((((select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus =? and f.deptcode=deptcode " + filterQuery1 + dateQuery1 
						+ " ))*100)/(select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus in ('P','M') and f.deptcode=deptcode " +dateQuery1 + " )),2) as PERCENTAGE, "
						+ "TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') as AMOUNTInCR, "
						+ dateRangeQuery
						+ "from GET_DASHBOARD_FOR_FUNCTION f "
						+ "join deptdetails d on d.ref_code = f.deptcode "
						+ "where f.overallstatus = ? "
						+ "and f.deptcode NOT IN ("+ deptCodeQuery +") "
						+  dateQuery2 + filterQuery +dateQuery1
						+ "group by f.deptcode,d.ref_description,f.overallstatus "
						+ "order by 4 desc,1,3 ";
				
				log.info("bigFiveFunctionQuery - " + bigFiveFunctionQuery);
				
				ps = con.prepareStatement(bigFiveFunctionQuery);
				if ("CM".equalsIgnoreCase(status)) {
					ps.setString(1, "M");
					ps.setString(2, "M");
					ps.setString(3, "M");
					ps.setString(4, "M");
					ps.setString(5, "M");
					ps.setString(6, "M");
					//ps.setString(7, "M");
					//ps.setString(8, "M");
					
				} else {
					ps.setString(1, status);
					ps.setString(2, status);
					ps.setString(3, status);
					ps.setString(4, status);
					ps.setString(5, status);
					ps.setString(6, status);
				//	ps.setString(7, status);
				//	ps.setString(8, status);
					
				}
				rs = ps.executeQuery();
				
				boolean funcAddFlag=false;
				while (rs.next()) {
					funcAddFlag=false;
					LinkedHashMap<String, Object> statusWiseInvData = new LinkedHashMap<String, Object>();
					
					LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();
					invData.put("DEPTCODE", rs.getString("DEPTCODE"));
					/*
					invData.put("COUNT", rs.getString("ICOUNT"));
					invData.put("TOTALSUM",rs.getString("TOTALSUM"));
					*/
					invData.put("COUNT", rs.getString("TOTALSUM"));
					invData.put("PERCENTAGE", rs.getString("PERCENTAGE"));
					invData.put("AMOUNTInCR", Double.parseDouble(rs.getString("AMOUNTInCR"))+"");
					invData.put("G21L45", rs.getString("G21L45"));
					invData.put("G45L90", rs.getString("G45L90"));
					invData.put("G90", rs.getString("G90"));					
					   
					if(!functionWiseInvData.isEmpty()) {
						if(functionWiseInvData.containsKey(rs.getString("FUNCTION"))) {
							funcAddFlag=true;
							statusWiseInvData = (LinkedHashMap<String, Object>) functionWiseInvData.get(rs.getString("FUNCTION"));
						}
					}
					
					if(!funcAddFlag) {
						LinkedHashMap<String, String> invData1 = new LinkedHashMap<String, String>();
						invData1.put("DEPTCODE", rs.getString("DEPTCODE"));
						invData1.put("COUNT", "0");
						//invData.put("TOTALSUM","0");
						invData1.put("PERCENTAGE", "0.00");
						invData1.put("AMOUNTInCR", "0.00");
						invData1.put("G21L45", "0");
						invData1.put("G45L90", "0");
						invData1.put("G90", "0");					
												
						statusWiseInvData.put("P", invData1);
						statusWiseInvData.put("CM", invData1);
						statusWiseInvData.put("M", invData1);
					}				
					
					if ("CM".equalsIgnoreCase(status)) {
						statusWiseInvData.put("CM", invData);
					}else {
						statusWiseInvData.put(rs.getString("OVERALLSTATUS"), invData);
					}
					functionWiseInvData.put(rs.getString("FUNCTION"), statusWiseInvData);
					//log.info(" functionWiseInvData --"+ functionWiseInvData);					
				}

				rs.close();
				ps.close();
								
				// OTHER FUNCTIONS DATA
				log.info("otherFunctionQuery - " + otherFunctionQuery);
								
				ps = con.prepareStatement(otherFunctionQuery);
				if ("CM".equalsIgnoreCase(status)) {
					ps.setString(1, "M");
					ps.setString(2, "M");
					ps.setString(3, "M");
					ps.setString(4, "M");
					ps.setString(5, "M");
					ps.setString(6, "M");
					//ps.setString(7, "M");
					//ps.setString(8, "M");
					
				} else {
					ps.setString(1, status);
					ps.setString(2, status);
					ps.setString(3, status);
					ps.setString(4, status);
					ps.setString(5, status);
					ps.setString(6, status);
					//ps.setString(7, status);
					//ps.setString(8, status);
					
				}
				rs = ps.executeQuery();
				
				while (rs.next()) {
					funcAddFlag=false;
					LinkedHashMap<String, Object> statusWiseInvData = new LinkedHashMap<String, Object>();
					
					LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();
					invData.put("DEPTCODE", rs.getString("DEPTCODE"));
					/*
					invData.put("COUNT", rs.getString("ICOUNT"));
					invData.put("TOTALSUM",rs.getString("TOTALSUM"));
					*/
					invData.put("COUNT", rs.getString("TOTALSUM"));
					invData.put("PERCENTAGE", rs.getString("PERCENTAGE"));
					invData.put("AMOUNTInCR", Double.parseDouble(rs.getString("AMOUNTInCR"))+"");
					invData.put("G21L45", rs.getString("G21L45"));
					invData.put("G45L90", rs.getString("G45L90"));
					invData.put("G90", rs.getString("G90"));					
					
					if(!otherFunctionsInvData.isEmpty()) {
						if(otherFunctionsInvData.containsKey(rs.getString("FUNCTION"))) {
							funcAddFlag=true;
							statusWiseInvData = (LinkedHashMap<String, Object>) otherFunctionsInvData.get(rs.getString("FUNCTION"));
						}
					}
					
					if(!funcAddFlag) {
						LinkedHashMap<String, String> invData1 = new LinkedHashMap<String, String>();
						invData1.put("DEPTCODE", rs.getString("DEPTCODE"));
						invData1.put("COUNT", "0");
						//invData1.put("TOTALSUM","0");
						invData1.put("PERCENTAGE", "0.00");
						invData1.put("AMOUNTInCR", "0.00");
						invData1.put("G21L45", "0");
						invData1.put("G45L90", "0");
						invData1.put("G90", "0");	
						
						statusWiseInvData.put("P", invData1);
						statusWiseInvData.put("CM", invData1);
						statusWiseInvData.put("M", invData1);
					}
					
					if ("CM".equalsIgnoreCase(status)) {
						statusWiseInvData.put("CM", invData);
					}else {
						statusWiseInvData.put(rs.getString("OVERALLSTATUS"), invData);
					}
					otherFunctionsInvData.put(rs.getString("FUNCTION"), statusWiseInvData);
										
					//BigDecimal bdCount = new BigDecimal(Double.parseDouble(rs.getString("ICOUNT"))).setScale(2,RoundingMode.HALF_UP);
					BigDecimal bdCount = new BigDecimal(Double.parseDouble(rs.getString("TOTALSUM"))).setScale(2,RoundingMode.HALF_UP);
					BigDecimal bdPercentage = new BigDecimal(Double.parseDouble(rs.getString("PERCENTAGE"))).setScale(2, RoundingMode.HALF_UP);
					BigDecimal bdAmountInLac = new BigDecimal(Double.parseDouble(rs.getString("AMOUNTInCR"))).setScale(2, RoundingMode.HALF_UP);
					
					BigDecimal bdG21L45 = new BigDecimal(Double.parseDouble(rs.getString("G21L45"))).setScale(2,RoundingMode.HALF_UP);
					BigDecimal bdG45L90 = new BigDecimal(Double.parseDouble(rs.getString("G45L90"))).setScale(2,RoundingMode.HALF_UP);
					BigDecimal bdG90 = new BigDecimal(Double.parseDouble(rs.getString("G90"))).setScale(2,RoundingMode.HALF_UP);

					Double count1 = bdCount.doubleValue();
					Double percentage1 = bdPercentage.doubleValue();

					Double amountInLac1 = bdAmountInLac.doubleValue();
					Double countG21L45 = bdG21L45.doubleValue();
					Double countG45L90 = bdG45L90.doubleValue();
					Double countG90 = bdG90.doubleValue();
										
					p_total = Double.sum(p_total, count1);
					
					if("P".equals(status)) {
						p_count = Double.sum(p_count, count1);
//						p_percentage = Double.sum(p_percentage, percentage1);
						p_amountInLac = Double.sum(p_amountInLac, amountInLac1);
						p_G21L45 = Double.sum(p_G21L45, countG21L45);
						p_G45L90 = Double.sum(p_G45L90, countG45L90);
						p_G90 = Double.sum(p_G90, countG90);
						
					}else if("M".equals(status)) {
						m_count = Double.sum(m_count, count1);
//						m_percentage = Double.sum(m_percentage, percentage1);
						m_amountInLac = Double.sum(m_amountInLac, amountInLac1);
						m_G21L45 = Double.sum(m_G21L45, countG21L45);
						m_G45L90 = Double.sum(m_G45L90, countG45L90);
						m_G90 = Double.sum(m_G90, countG90);
						
					}else {
						cm_count = Double.sum(cm_count, count1);
//						cm_percentage = Double.sum(cm_percentage, percentage1);
						cm_amountInLac = Double.sum(cm_amountInLac, amountInLac1);
						cm_G21L45 = Double.sum(cm_G21L45, countG21L45);
						cm_G45L90 = Double.sum(cm_G45L90, countG45L90);
						cm_G90 = Double.sum(cm_G90, countG90);
					}
				}
				rs.close();
				ps.close();
								
			}
			
				if(p_total!=0) {
					p_percentage=(p_count/p_total)*100;
					cm_percentage=(cm_count/p_total)*100;
					m_percentage=(m_count/p_total)*100;
				}
			
			LinkedHashMap<String, Object> statusWiseInvData = new LinkedHashMap<String, Object>();
			LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();
			invData.put("DEPTCODE", "OTHER");
			invData.put("COUNT", String.valueOf(String.valueOf(new BigDecimal(p_count).setScale(2, RoundingMode.HALF_UP))));
			invData.put("PERCENTAGE", String.valueOf(String.valueOf(new BigDecimal(p_percentage).setScale(2, RoundingMode.HALF_UP))));
			invData.put("AMOUNTInCR", String.valueOf(String.valueOf(new BigDecimal(p_amountInLac).setScale(2, RoundingMode.HALF_UP))));
			invData.put("G21L45", String.valueOf(String.valueOf(new BigDecimal(p_G21L45).setScale(0, RoundingMode.HALF_UP))));
			invData.put("G45L90", String.valueOf(String.valueOf(new BigDecimal(p_G45L90).setScale(0, RoundingMode.HALF_UP))));
			invData.put("G90", String.valueOf(String.valueOf(new BigDecimal(p_G90).setScale(0, RoundingMode.HALF_UP))));			
			statusWiseInvData.put("P", invData);
			
			LinkedHashMap<String, String> invData1 = new LinkedHashMap<String, String>();
			invData1.put("DEPTCODE", "OTHER");
			invData1.put("COUNT", String.valueOf(String.valueOf(new BigDecimal(cm_count).setScale(2, RoundingMode.HALF_UP))));
			invData1.put("PERCENTAGE", String.valueOf(String.valueOf(new BigDecimal(cm_percentage).setScale(2, RoundingMode.HALF_UP))));
			invData1.put("AMOUNTInCR", String.valueOf(String.valueOf(new BigDecimal(cm_amountInLac).setScale(2, RoundingMode.HALF_UP))));
			invData1.put("G21L45", String.valueOf(String.valueOf(new BigDecimal(cm_G21L45).setScale(0, RoundingMode.HALF_UP))));
			invData1.put("G45L90", String.valueOf(String.valueOf(new BigDecimal(cm_G45L90).setScale(0, RoundingMode.HALF_UP))));
			invData1.put("G90", String.valueOf(String.valueOf(new BigDecimal(cm_G90).setScale(0, RoundingMode.HALF_UP))));			
			statusWiseInvData.put("P", invData);		
			statusWiseInvData.put("CM", invData1);
			
			LinkedHashMap<String, String> invData2 = new LinkedHashMap<String, String>();
			invData2.put("DEPTCODE", "OTHER");
			invData2.put("COUNT", String.valueOf(String.valueOf(new BigDecimal(m_count).setScale(2, RoundingMode.HALF_UP))));
			invData2.put("PERCENTAGE", String.valueOf(String.valueOf(new BigDecimal(m_percentage).setScale(2, RoundingMode.HALF_UP))));
			invData2.put("AMOUNTInCR", String.valueOf(String.valueOf(new BigDecimal(m_amountInLac).setScale(2, RoundingMode.HALF_UP))));
			invData2.put("G21L45", String.valueOf(String.valueOf(new BigDecimal(m_G21L45).setScale(0, RoundingMode.HALF_UP))));
			invData2.put("G45L90", String.valueOf(String.valueOf(new BigDecimal(m_G45L90).setScale(0, RoundingMode.HALF_UP))));
			invData2.put("G90", String.valueOf(String.valueOf(new BigDecimal(m_G90).setScale(0, RoundingMode.HALF_UP))));			
			statusWiseInvData.put("P", invData);
			statusWiseInvData.put("M", invData2);
			
			statusWiseInvData.put("data", otherFunctionsInvData);
			functionWiseInvData.put("OTHER", statusWiseInvData);
			bigFiveFunctionList.add(functionWiseInvData);
			log.info("getBigFiveFunctionData - data size " + bigFiveFunctionList.size());
			
			if (bigFiveFunctionList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("data", bigFiveFunctionList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found.");
				jsonArray.add(responsejson);
			}
			
		} catch (Exception e) {
			
			log.error("getBigFiveFunctionData - Exception "+ e);
			responsejson.put("message", "Error " + e.getMessage());
			jsonArray.add(responsejson);
			
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}	


	//  old method 
/*
	@SuppressWarnings("unchecked")
	public JSONArray getBigFiveFunctionData(String email, String mode) {

		log.info("getBigFiveFunctionData - email : " + email + " mode : " + mode);

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		ArrayList<Object> bigFiveFunctionList = new ArrayList<Object>();
		
		List<String> endUserProcessList = new ArrayList<String>();
		endUserProcessList.add("P");  // Pending with GRN creator
		endUserProcessList.add("CM"); // Pending with GRN confirmer
		endUserProcessList.add("M");  // Pending with GRN confirmer/Invoice Approver
		
		String dateQuery1=" and sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') > 21 and sysdate - TO_DATE(CREATEDON, 'dd-mm-yy') < 90 ";
		String dateQuery2=" and sysdate - TO_DATE(f.CREATEDON, 'dd-mm-yy') > 21 and sysdate - TO_DATE(f.CREATEDON, 'dd-mm-yy') < 90 ";
		
		String deptCodeQuery = "select deptcode from ( select f.deptcode,(select count(*) from GET_DASHBOARD_FOR_FUNCTION  where overallstatus in( 'P','M') and f.deptcode=deptcode"+dateQuery1+") AS TOTALSUM "
				+ "from GET_DASHBOARD_FOR_FUNCTION f where f.overallstatus in( 'P','M') "+dateQuery2+" group by f.deptcode order by 2 desc) "
				+ "where ROWNUM <= 5 ";
		
		try {
			con = DBConnection.getConnection();
			
			
			LinkedHashMap<String, Object> functionWiseInvData = new LinkedHashMap<String, Object>();
			LinkedHashMap<String, Object> otherFunctionsInvData = new LinkedHashMap<String, Object>();
			
			Double p_count = 0.00;
			Double p_percentage = 0.00;
			Double p_amountInLac = 0.00;
			Double p_total = 0.00;
			
			Double cm_count = 0.00;
			Double cm_percentage = 0.00;
			Double cm_amountInLac = 0.00;
			
			Double m_count = 0.00;
			Double m_percentage = 0.00;
			Double m_amountInLac = 0.00;
			
			for(String status:endUserProcessList) {
				
				log.info("status - " + status);
				
				String filterQuery="";
				if("CM".equals(status)) {
					filterQuery= " and f.grnnumber is null and invoicenumber in ( select invoicenumber from audit_acceptqty_behalf where ponumber = f.ponumber ) ";
				}else if("M".equals(status)) {
					filterQuery= " and f.grnnumber is not null ";
				}
				
				String bigFiveFunctionQuery="SELECT f.deptcode,d.ref_description as function,f.overallstatus,count(*) as ICOUNT, "
						+ "(select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus in( 'P','M') and f.deptcode=deptcode "+dateQuery1+" ) as TOTALSUM, "
						+ "round(((count(*)*100)/(select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus in( 'P','M') and f.deptcode=deptcode "+dateQuery1+" )),2) as PERCENTAGE, "
						+ "TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') as AMOUNTInCR "
						+ "from GET_DASHBOARD_FOR_FUNCTION f "
						+ "join deptdetails d on d.ref_code = f.deptcode "
						+ "where f.overallstatus = ? "
						+ "and f.deptcode IN ("+deptCodeQuery+") "
						+  dateQuery2+filterQuery
						+ "group by f.deptcode,d.ref_description,f.overallstatus "
						+ "order by 5 desc,1,3 ";
				
				String otherFunctionQuery="SELECT f.deptcode,d.ref_description as function,f.overallstatus,count(*) as ICOUNT, "
						+ "(select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus in( 'P','M') and f.deptcode=deptcode "+dateQuery1+" ) as TOTALSUM, "
						+ "round(((count(*)*100)/(select count(*) from GET_DASHBOARD_FOR_FUNCTION where overallstatus in( 'P','M') and f.deptcode=deptcode "+dateQuery1+" )),2) as PERCENTAGE, "
						+ "TO_CHAR(SUM(amount)/10000000, 'fm9999999999D00') as AMOUNTInCR "
						+ "from GET_DASHBOARD_FOR_FUNCTION f "
						+ "join deptdetails d on d.ref_code = f.deptcode "
						+ "where f.overallstatus = ? "
						+ "and f.deptcode NOT IN ("+deptCodeQuery+") "
						+  dateQuery2+filterQuery
						+ "group by f.deptcode,d.ref_description,f.overallstatus "
						+ "order by 5 desc,1,3 ";
				
				log.info("bigFiveFunctionQuery - " + bigFiveFunctionQuery);
				
				ps = con.prepareStatement(bigFiveFunctionQuery);
				if ("CM".equalsIgnoreCase(status)) {
					ps.setString(1, "M");
				} else {
					ps.setString(1, status);
				}
				rs = ps.executeQuery();
				
				boolean funcAddFlag=false;
				while (rs.next()) {
					funcAddFlag=false;
					LinkedHashMap<String, Object> statusWiseInvData = new LinkedHashMap<String, Object>();
					
					LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();
					invData.put("DEPTCODE", rs.getString("DEPTCODE"));
					invData.put("COUNT", rs.getString("ICOUNT"));
					invData.put("PERCENTAGE", rs.getString("PERCENTAGE"));
					invData.put("AMOUNTInCR", Double.parseDouble(rs.getString("AMOUNTInCR"))+"");
					
					if(!functionWiseInvData.isEmpty()) {
						if(functionWiseInvData.containsKey(rs.getString("FUNCTION"))) {
							funcAddFlag=true;
							statusWiseInvData = (LinkedHashMap<String, Object>) functionWiseInvData.get(rs.getString("FUNCTION"));
						}
					}
					
					if(!funcAddFlag) {
						LinkedHashMap<String, String> invData1 = new LinkedHashMap<String, String>();
						invData1.put("DEPTCODE", rs.getString("DEPTCODE"));
						invData1.put("COUNT", "0");
						invData1.put("PERCENTAGE", "0.00");
						invData1.put("AMOUNTInCR", "0.00");
						
						statusWiseInvData.put("P", invData1);
						statusWiseInvData.put("CM", invData1);
						statusWiseInvData.put("M", invData1);
					}				
					
					if ("CM".equalsIgnoreCase(status)) {
						statusWiseInvData.put("CM", invData);
					}else {
						statusWiseInvData.put(rs.getString("OVERALLSTATUS"), invData);
					}
					functionWiseInvData.put(rs.getString("FUNCTION"), statusWiseInvData);
										
				}

				rs.close();
				ps.close();
								
				// OTHER FUNCTIONS DATA
				
				ps = con.prepareStatement(otherFunctionQuery);
				if ("CM".equalsIgnoreCase(status)) {
					ps.setString(1, "M");
				} else {
					ps.setString(1, status);
				}
				rs = ps.executeQuery();
				
				while (rs.next()) {
					funcAddFlag=false;
					LinkedHashMap<String, Object> statusWiseInvData = new LinkedHashMap<String, Object>();
					
					LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();
					invData.put("DEPTCODE", rs.getString("DEPTCODE"));
					invData.put("COUNT", rs.getString("ICOUNT"));
					invData.put("PERCENTAGE", rs.getString("PERCENTAGE"));
					invData.put("AMOUNTInCR", Double.parseDouble(rs.getString("AMOUNTInCR"))+"");
					
					if(!otherFunctionsInvData.isEmpty()) {
						if(otherFunctionsInvData.containsKey(rs.getString("FUNCTION"))) {
							funcAddFlag=true;
							statusWiseInvData = (LinkedHashMap<String, Object>) otherFunctionsInvData.get(rs.getString("FUNCTION"));
						}
					}
					
					if(!funcAddFlag) {
						LinkedHashMap<String, String> invData1 = new LinkedHashMap<String, String>();
						invData1.put("DEPTCODE", rs.getString("DEPTCODE"));
						invData1.put("COUNT", "0");
						invData1.put("PERCENTAGE", "0.00");
						invData1.put("AMOUNTInCR", "0.00");
						
						statusWiseInvData.put("P", invData1);
						statusWiseInvData.put("CM", invData1);
						statusWiseInvData.put("M", invData1);
					}
					
					if ("CM".equalsIgnoreCase(status)) {
						statusWiseInvData.put("CM", invData);
					}else {
						statusWiseInvData.put(rs.getString("OVERALLSTATUS"), invData);
					}
					otherFunctionsInvData.put(rs.getString("FUNCTION"), statusWiseInvData);
										
					BigDecimal bdCount = new BigDecimal(Double.parseDouble(rs.getString("ICOUNT"))).setScale(2,RoundingMode.HALF_UP);
					BigDecimal bdPercentage = new BigDecimal(Double.parseDouble(rs.getString("PERCENTAGE"))).setScale(2, RoundingMode.HALF_UP);
					BigDecimal bdAmountInLac = new BigDecimal(Double.parseDouble(rs.getString("AMOUNTInCR"))).setScale(2, RoundingMode.HALF_UP);
					
//					BigDecimal bdTotal = new BigDecimal(Double.parseDouble(rs.getString("TOTALSUM"))).setScale(2, RoundingMode.HALF_UP);

					Double count1 = bdCount.doubleValue();
					Double percentage1 = bdPercentage.doubleValue();
					Double amountInLac1 = bdAmountInLac.doubleValue();
//					Double totalSum = bdTotal.doubleValue();
					
					p_total = Double.sum(p_total, count1);
					
					if("P".equals(status)) {
						p_count = Double.sum(p_count, count1);
//						p_percentage = Double.sum(p_percentage, percentage1);
						p_amountInLac = Double.sum(p_amountInLac, amountInLac1);
						
					}else if("M".equals(status)) {
						m_count = Double.sum(m_count, count1);
//						m_percentage = Double.sum(m_percentage, percentage1);
						m_amountInLac = Double.sum(m_amountInLac, amountInLac1);
					}else {
						cm_count = Double.sum(cm_count, count1);
//						cm_percentage = Double.sum(cm_percentage, percentage1);
						cm_amountInLac = Double.sum(cm_amountInLac, amountInLac1);
					}
				}
				rs.close();
				ps.close();
								
			}
			
//			try {
				
				if(p_total!=0) {
					p_percentage=(p_count/p_total)*100;
					cm_percentage=(cm_count/p_total)*100;
					m_percentage=(m_count/p_total)*100;
				}
//			}catch(Exception e) {
//				p_percentage=0.00;
//				cm_percentage=0.00;
//				m_percentage=0.00;
//			}
			
			
			LinkedHashMap<String, Object> statusWiseInvData = new LinkedHashMap<String, Object>();
			LinkedHashMap<String, String> invData = new LinkedHashMap<String, String>();
			invData.put("DEPTCODE", "OTHER");
			invData.put("COUNT", String.valueOf(String.valueOf(new BigDecimal(p_count).setScale(2, RoundingMode.HALF_UP))));
			invData.put("PERCENTAGE", String.valueOf(String.valueOf(new BigDecimal(p_percentage).setScale(2, RoundingMode.HALF_UP))));
			invData.put("AMOUNTInCR", String.valueOf(String.valueOf(new BigDecimal(p_amountInLac).setScale(2, RoundingMode.HALF_UP))));
			statusWiseInvData.put("P", invData);
			
			LinkedHashMap<String, String> invData1 = new LinkedHashMap<String, String>();
			invData1.put("DEPTCODE", "OTHER");
			invData1.put("COUNT", String.valueOf(String.valueOf(new BigDecimal(cm_count).setScale(2, RoundingMode.HALF_UP))));
			invData1.put("PERCENTAGE", String.valueOf(String.valueOf(new BigDecimal(cm_percentage).setScale(2, RoundingMode.HALF_UP))));
			invData1.put("AMOUNTInCR", String.valueOf(String.valueOf(new BigDecimal(cm_amountInLac).setScale(2, RoundingMode.HALF_UP))));
			statusWiseInvData.put("CM", invData1);
			
			LinkedHashMap<String, String> invData2 = new LinkedHashMap<String, String>();
			invData2.put("DEPTCODE", "OTHER");
			invData2.put("COUNT", String.valueOf(String.valueOf(new BigDecimal(m_count).setScale(2, RoundingMode.HALF_UP))));
			invData2.put("PERCENTAGE", String.valueOf(String.valueOf(new BigDecimal(m_percentage).setScale(2, RoundingMode.HALF_UP))));
			invData2.put("AMOUNTInCR", String.valueOf(String.valueOf(new BigDecimal(m_amountInLac).setScale(2, RoundingMode.HALF_UP))));
			statusWiseInvData.put("M", invData2);
			
			statusWiseInvData.put("data", otherFunctionsInvData);
			
			functionWiseInvData.put("OTHER", statusWiseInvData);

			bigFiveFunctionList.add(functionWiseInvData);
			
			log.info("getBigFiveFunctionData - data size " + bigFiveFunctionList.size());
			
			if (bigFiveFunctionList.size() > 0) {
				responsejson.put("message", "Success");
				responsejson.put("data", bigFiveFunctionList);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found.");
				jsonArray.add(responsejson);
			}
			
		} catch (Exception e) {
			
			log.error("getBigFiveFunctionData - Exception ", e);
			responsejson.put("message", "Error " + e.getMessage());
			jsonArray.add(responsejson);
			
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}
	
*/
}
