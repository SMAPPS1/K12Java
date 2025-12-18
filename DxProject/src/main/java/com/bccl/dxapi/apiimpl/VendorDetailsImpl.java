package com.bccl.dxapi.apiimpl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.apiutility.MailOffice365;
import com.bccl.dxapi.bean.VendorAmendmentSubmit;
import com.bccl.dxapi.bean.VendorRegSubmission;

public class VendorDetailsImpl {

	static Logger log = Logger.getLogger(VendorDetailsImpl.class.getName());

	public VendorDetailsImpl() {
		responsejson = new JSONObject();
		jsonArray = new JSONArray();
		objImage = new ImageUploadImpl();
		random = new Random();
	}

	@Override
	protected void finalize() throws Throwable {
		responsejson = null;
		jsonArray = null;
		random = null;
		objImage = null;
		super.finalize();
	}

	JSONObject responsejson = null;
	JSONArray jsonArray = null;
	ImageUploadImpl objImage = null;
	Random random = null;

	public JSONArray getVendorDetails(String id) throws SQLException {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			
			/*
			String general_data = "SELECT BPC.BUSINESSPARTNEROID,BPD.PLANT,BPA.ADDRESSLINE1,BPA.ADDRESSLINE1, BPA.ADDRESSLINE2, BPA.ADDRESSLINE3, BPA.PINCODE,"
					+ "BPA.CITY, BPA.STATE, BPA.COUNTRY,BPC.CONTACTTITLE,BPC.CONTACTNAME,BPC.CONTACTMOBILENUMBER,BPC.CONTACTLANDLINENUMBER,"
					+ "BPC.CONTACTEMAILID FROM BUSINESSPARTNERCONTACTS BPC, BUSINSESSPARTNERDOCUMENTS BPD ,BUSINESSPARTNERADDRESS BPA"
					+ " WHERE BPD.BUSINESSPARTNEROID = BPC.BUSINESSPARTNEROID AND "
					+ " BPA.BUSINESSPARTNEROID = BPC.BUSINESSPARTNEROID AND BPC.BUSINESSPARTNEROID =?";
			*/
			
			String general_data = "SELECT BPC.BUSINESSPARTNEROID,BPD.PLANT,BPA.ADDRESSLINE1,BPA.ADDRESSLINE1, BPA.ADDRESSLINE2, BPA.ADDRESSLINE3, BPA.PINCODE, "
					+ "BPA.CITY, BPA.STATE, BPA.COUNTRY,BPC.CONTACTTITLE,BPC.CONTACTNAME,BPC.CONTACTMOBILENUMBER,BPC.CONTACTLANDLINENUMBER, "
					+ "BPC.CONTACTEMAILID, BP.PRIMARYEMAILID, BP.SECONDARYEMAILID, BP.TERTIARYEMAILID, "
					+ "(SELECT BPAT.ATTRIBUTEVALUE FROM  BUSINESSPARTNERATTRIBUTES BPAT WHERE BPAT.ATTRIBUTETEXT = 'EMAILID' "
					+ "AND BPAT.ATTRIBUTEOID ='004' AND BPAT.BUSINESSPARTNEROID = ? ) AS EMAILID4, "
					+ "(SELECT BPAT.ATTRIBUTEVALUE FROM  BUSINESSPARTNERATTRIBUTES BPAT WHERE BPAT.ATTRIBUTETEXT = 'EMAILID' "
					+ "AND BPAT.ATTRIBUTEOID ='005' AND BPAT.BUSINESSPARTNEROID = ? ) AS EMAILID5 "
					+ "FROM BUSINESSPARTNERCONTACTS BPC, BUSINSESSPARTNERDOCUMENTS BPD ,BUSINESSPARTNERADDRESS BPA, "
					+ "BUSINESSPARTNER BP WHERE BPD.BUSINESSPARTNEROID = BPC.BUSINESSPARTNEROID  AND BP.BUSINESSPARTNEROID = BPC.BUSINESSPARTNEROID "
					+ "AND BPA.BUSINESSPARTNEROID = BPC.BUSINESSPARTNEROID AND BPC.BUSINESSPARTNEROID = ? " ;
			
			String banking_data = "select bpb.BankCode, bpb.BankShortText, bpb.BranchText, bpb.AccountNumber, bp.PANNumber from businesspartnerbankdetails "
					+ "bpb ,businesspartner bp where bpb.BusinessPartnerOID = bp.BusinessPartnerOID and bpb.BusinessPartnerOID = ?";
			
			String business_data = "SELECT * FROM businsesspartnerdocuments where BusinessPartnerOID = ?";
			
			String business_partner_attributes = "SELECT AttributeText,AttributeValue FROM businesspartnerattributes where BusinessPartnerOID = ?";
			
			con = DBConnection.getConnection();

			ArrayList<HashMap<String, String>> businesspartnerattributesdetails = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> attributesDetails = new HashMap<String, String>();

			ps = con.prepareStatement(business_partner_attributes);
			ps.setString(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {
				attributesDetails.put(rs.getString("AttributeText"), rs.getString("AttributeValue"));
			}
			rs.close();
			ps.close();

			ArrayList<HashMap<String, String>> GeneralprofileList = new ArrayList<HashMap<String, String>>();
			ps = con.prepareStatement(general_data);
			ps.setString(1, id);
			ps.setString(2, id);
			ps.setString(3, id);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> generalData = new HashMap<String, String>();
				generalData.put("COMPANY_CODE", rs.getString("PLANT"));
				generalData.put("PURCH_GROUP", attributesDetails.get("PURCH_GROUP") != null ? attributesDetails.get("PURCH_GROUP").toString() : "");
				generalData.put("TITLE", rs.getString("CONTACTTITLE"));
				generalData.put("VENDOR_NAME_LINE1", rs.getString("CONTACTNAME"));
				generalData.put("VENDOR_NAME_LINE2", rs.getString("CONTACTNAME"));
				generalData.put("STREET_LINE1", rs.getString("ADDRESSLINE1"));
				generalData.put("STREET_LINE2", rs.getString("ADDRESSLINE2"));
				generalData.put("STREET_LINE3", rs.getString("ADDRESSLINE3"));
				generalData.put("STREET_LINE4", rs.getString("ADDRESSLINE3"));
				generalData.put("POSTAL_CODE", rs.getString("PINCODE"));
				generalData.put("CITY", rs.getString("CITY"));
				generalData.put("COUNTRY_KEY", rs.getString("COUNTRY"));
				generalData.put("REGION", rs.getString("STATE"));
				generalData.put("TELEPHONE", rs.getString("CONTACTLANDLINENUMBER"));
				generalData.put("MOBILE_NO", rs.getString("CONTACTMOBILENUMBER"));
				generalData.put("FAX", attributesDetails.get("FAX") != null ? attributesDetails.get("FAX").toString() : "");
				generalData.put("VENDOR_EMAIL_ID", rs.getString("CONTACTEMAILID"));
				generalData.put("PRIMARYEMAILID", rs.getString("PRIMARYEMAILID"));
				generalData.put("SECONDARYEMAILID", rs.getString("SECONDARYEMAILID"));
				generalData.put("TERTIARYEMAILID", rs.getString("TERTIARYEMAILID"));
				generalData.put("EMAILID4", rs.getString("EMAILID4"));
				generalData.put("EMAILID5", rs.getString("EMAILID5"));
				
				GeneralprofileList.add(generalData);

			}
			rs.close();
			ps.close();

			ArrayList<HashMap<String, String>> BankingprofileList = new ArrayList<HashMap<String, String>>();
			ps = con.prepareStatement(banking_data);
			ps.setString(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> bankingData = new HashMap<String, String>();
				bankingData.put("BANK_NAME", rs.getString("BankShortText"));
				bankingData.put("BRANCH", rs.getString("BranchText"));
				bankingData.put("BANK_ACCOUNT", rs.getString("AccountNumber"));
				bankingData.put("SWIFT_CODE", attributesDetails.get("SWIFT_CODE") != null ? attributesDetails.get("SWIFT_CODE").toString() : "");
				bankingData.put("IFSC_NUMBER", attributesDetails.get("IFSC_NUMBER") != null ? attributesDetails.get("IFSC_NUMBER").toString() : "");
				bankingData.put("PF_REGISTRATION_NUMBER", attributesDetails.get("PF_REGISTRATION_NUMBER") != null ? attributesDetails.get("PF_REGISTRATION_NUMBER").toString() : "");
				bankingData.put("ESIC_REGISTRATION_NUMBER", attributesDetails.get("ESIC_REGISTRATION_NUMBER") != null ? attributesDetails.get("ESIC_REGISTRATION_NUMBER").toString() : "");
				bankingData.put("VAT_REGISTRATION",	attributesDetails.get("VAT_REGISTRATION") != null ? attributesDetails.get("VAT_REGISTRATION").toString() : "");
				bankingData.put("PAN", rs.getString("PANNumber"));
				bankingData.put("SERVICE_TAX_REG_NO", attributesDetails.get("SERVICE_TAX_REG_NO") != null ? attributesDetails.get("SERVICE_TAX_REG_NO").toString() : "");
				bankingData.put("CST_NO", attributesDetails.get("CST_NO") != null ? attributesDetails.get("CST_NO").toString() : "");
				bankingData.put("LST_NO", attributesDetails.get("LST_NO") != null ? attributesDetails.get("LST_NO").toString() : "");
				bankingData.put("GST_REGN",	attributesDetails.get("GST_REGN") != null ? attributesDetails.get("GST_REGN").toString() : "");
				bankingData.put("TAX_CLASSSIFICATION", attributesDetails.get("TAX_CLASSSIFICATION") != null	? attributesDetails.get("TAX_CLASSSIFICATION").toString() : "");
				bankingData.put("TDS_EXEMP_NO",	attributesDetails.get("TDS_EXEMP_NO") != null ? attributesDetails.get("TDS_EXEMP_NO").toString() : "");
				bankingData.put("EXEMPT", attributesDetails.get("EXEMPT") != null ? attributesDetails.get("EXEMPT").toString() : "");
				bankingData.put("EXEMPT_FROM",attributesDetails.get("EXEMPT_FROM") != null ? attributesDetails.get("EXEMPT_FROM").toString() : "");
				bankingData.put("EXEMPT_TO", attributesDetails.get("EXEMPT_TO") != null ? attributesDetails.get("EXEMPT_TO").toString() : "");
				bankingData.put("MSME_STATUS",attributesDetails.get("MSME_STATUS") != null ? attributesDetails.get("MSME_STATUS").toString() : "");
				bankingData.put("MSME_NO", attributesDetails.get("MSME_NO") != null ? attributesDetails.get("MSME_NO").toString() : "");
				BankingprofileList.add(bankingData);
			}
			rs.close();
			ps.close();

			ArrayList<HashMap<String, String>> BusinessprofileList = new ArrayList<HashMap<String, String>>();
			ps = con.prepareStatement(business_data);
			ps.setString(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> businessData = new HashMap<String, String>();
				businessData.put("TYPE_OF_BUSINESS",
						attributesDetails.get("TYPE_OF_BUSINESS") != null ? attributesDetails.get("TYPE_OF_BUSINESS").toString() : "");
				businessData.put("TYPE_OF_INDUSTRY", attributesDetails.get("TYPE_OF_INDUSTRY") != null ? attributesDetails.get("TYPE_OF_INDUSTRY").toString() : "");
				businessData.put("PROMOTERS", attributesDetails.get("PROMOTERS") != null ? attributesDetails.get("PROMOTERS").toString() : "");
				businessData.put("TOP_5_CLIENTS", attributesDetails.get("TOP_5_CLIENTS") != null ? attributesDetails.get("TOP_5_CLIENTS").toString() : "");
				businessData.put("TURNOVER", attributesDetails.get("TURNOVER") != null ? attributesDetails.get("TURNOVER").toString() : "");
				businessData.put("CLIENT_REFERNCES", attributesDetails.get("CLIENT_REFERNCES") != null ? attributesDetails.get("CLIENT_REFERNCES").toString() : "");
				businessData.put("COMPLIANCE_CATEGORY",attributesDetails.get("COMPLIANCE_CATEGORY") != null ? attributesDetails.get("COMPLIANCE_CATEGORY").toString() : "");
				businessData.put("CONSITUTION_OF_BUS",attributesDetails.get("CONSITUTION_OF_BUS") != null ? attributesDetails.get("CONSITUTION_OF_BUS").toString() : "");
				businessData.put("UPLOAD_PRODUCT_PORTFOLIO",attributesDetails.get("UPLOAD_PRODUCT_PORTFOLIO") != null ? attributesDetails.get("UPLOAD_PRODUCT_PORTFOLIO").toString() : "");
				BusinessprofileList.add(businessData);
			}
			rs.close();
			ps.close();

			ArrayList<HashMap<String, String>> PersonalprofileList = new ArrayList<HashMap<String, String>>();
			ps = con.prepareStatement(business_data);
			ps.setString(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> personalData = new HashMap<String, String>();
				personalData.put("COMPANY_NAME", rs.getString("Company"));
				personalData.put("PARENT_COMPANY", attributesDetails.get("PARENT_COMPANY") != null ? attributesDetails.get("PARENT_COMPANY").toString() : "");
				personalData.put("GLOBAL_HEAD_OFFICE", attributesDetails.get("GLOBAL_HEAD_OFFICE") != null ? attributesDetails.get("GLOBAL_HEAD_OFFICE").toString(): "");
				personalData.put("INDIA_HEAD_OFFICE", attributesDetails.get("INDIA_HEAD_OFFICE") != null ? attributesDetails.get("INDIA_HEAD_OFFICE").toString() : "");
				personalData.put("UPLOAD_PRODUCT_PORTFOLIO", rs.getString("DocumentFileName"));
				personalData.put("WEBSITE", attributesDetails.get("WEBSITE") != null ? attributesDetails.get("WEBSITE").toString() : "");
				personalData.put("MANUFACTUREING_SITE",	attributesDetails.get("MANUFACTUREING_SITE") != null ? attributesDetails.get("MANUFACTUREING_SITE").toString() : "");
				personalData.put("MFG_CAPACITY", attributesDetails.get("MFG_CAPACITY") != null ? attributesDetails.get("MFG_CAPACITY").toString() : "");
				personalData.put("YEAR_OF_ESTABLISHMENT", attributesDetails.get("YEAR_OF_ESTABLISHMENT") != null ? attributesDetails.get("YEAR_OF_ESTABLISHMENT").toString() : "");
				personalData.put("BRIEF_DESC_OF_COMPANY", attributesDetails.get("BRIEF_DESC_OF_COMPANY") != null ? attributesDetails.get("BRIEF_DESC_OF_COMPANY").toString() : "");
				personalData.put("BNC_CATEGORY", attributesDetails.get("BNC_CATEGORY") != null ? attributesDetails.get("BNC_CATEGORY").toString() : "");
				personalData.put("MATERIAL_SERV_PROVIDER", attributesDetails.get("MATERIAL_SERV_PROVIDER") != null ? attributesDetails.get("MATERIAL_SERV_PROVIDER").toString()	: "");
				personalData.put("REVENUE",	attributesDetails.get("REVENUE") != null ? attributesDetails.get("REVENUE").toString() : ""); 
				personalData.put("PAY_LY", attributesDetails.get("PAY_LY") != null ? attributesDetails.get("PAY_LY").toString() : "");
				personalData.put("LY_SPEND",attributesDetails.get("LY_SPEND") != null ? attributesDetails.get("LY_SPEND").toString() : "");
				personalData.put("NO_OF_EMPLOYEES",	attributesDetails.get("NO_OF_EMPLOYEES") != null ? attributesDetails.get("NO_OF_EMPLOYEES").toString() : "");
				PersonalprofileList.add(personalData);
			}
			rs.close();
			ps.close();
			ArrayList<HashMap<String, String>> L1leadership = new ArrayList<HashMap<String, String>>();
			ps = con.prepareStatement(business_data);
			ps.setString(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> leadershipData = new HashMap<String, String>();
				leadershipData.put("L1_LEADER_NAME",attributesDetails.get("L1_LEADER_NAME") != null ? attributesDetails.get("L1_LEADER_NAME").toString(): "");
				leadershipData.put("L1_LEADER_EMAIL_ID", attributesDetails.get("L1_LEADER_EMAIL_ID") != null ? attributesDetails.get("L1_LEADER_EMAIL_ID").toString() : "");
				leadershipData.put("L1_LEADER_CONTACT_NO", attributesDetails.get("L1_LEADER_CONTACT_NO") != null ? attributesDetails.get("L1_LEADER_CONTACT_NO").toString()	: "");
				L1leadership.add(leadershipData);
			}
			rs.close();
			ps.close();

			ArrayList<HashMap<String, String>> L2leadership = new ArrayList<HashMap<String, String>>();
			ps = con.prepareStatement(business_data);
			ps.setString(1, id);
			rs = ps.executeQuery();
			while (rs.next()) {

				HashMap<String, String> leadershipData2 = new HashMap<String, String>();
				leadershipData2.put("L2_LEADER_NAME", attributesDetails.get("L2_LEADER_NAME") != null ? attributesDetails.get("L2_LEADER_NAME").toString() : "");
				leadershipData2.put("L2_LEADER_EMAIL_ID", attributesDetails.get("L2_LEADER_EMAIL_ID") != null ? attributesDetails.get("L2_LEADER_EMAIL_ID").toString() : "");
				leadershipData2.put("L2_LEADER_CONTACT_NO",attributesDetails.get("L2_LEADER_CONTACT_NO") != null ? attributesDetails.get("L2_LEADER_CONTACT_NO").toString()	: "");
				leadershipData2.put("L2_LEADER_ASSOCIATED",attributesDetails.get("L2_LEADER_ASSOCIATED") != null ? attributesDetails.get("L2_LEADER_ASSOCIATED").toString()	: "");
				leadershipData2.put("L2_LEADER_BCCL_ANNUAL", attributesDetails.get("L2_LEADER_BCCL_ANNUAL") != null	? attributesDetails.get("L2_LEADER_BCCL_ANNUAL").toString()	: "");
				leadershipData2.put("L2_LEADER_GROUP_CO", attributesDetails.get("L2_LEADER_GROUP_CO") != null ? attributesDetails.get("L2_LEADER_GROUP_CO").toString() : "");
				leadershipData2.put("L2_LEADER_MARKET",	attributesDetails.get("L2_LEADER_MARKET") != null ? attributesDetails.get("L2_LEADER_MARKET").toString() : "");
				L2leadership.add(leadershipData2);
			}
			rs.close();
			ps.close();

			if (BankingprofileList.size() > 0 && GeneralprofileList.size() > 0 && BusinessprofileList.size() > 0
					&& PersonalprofileList.size() > 0) {
				responsejson.put("generalData", GeneralprofileList);
				responsejson.put("bankingData", BankingprofileList);
				responsejson.put("businessData", BusinessprofileList);
				responsejson.put("personalData", PersonalprofileList);
				responsejson.put("leadershipData", L1leadership);
				responsejson.put("leadershipData2", L2leadership);
				jsonArray.add(responsejson);
			} else {
				responsejson.put("message", "No Data Found for given Vendor Id");
				jsonArray.add(responsejson);
			}

		} catch (SQLException e) {
			log.error("getVendorDetails() : ", e.fillInStackTrace());
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getVendorBussDetails(String id, List<String> bussinessdetailslist) throws SQLException {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			con.setAutoCommit(false);
			HashMap<String, String> personalDetails = new HashMap<String, String>();
			String sql = "UPDATE businesspartnerattributes set AttributeValue=? where BusinessPartnerOID =? AND AttributeText=? ";

			int value = 0;
			Hashtable<Integer, String> hash = new Hashtable<Integer, String>();
			hash.put(1, "COMPANY_NAME");
			hash.put(2, "PARENT_COMPANY");
			hash.put(3, "GLOBAL_HEAD_OFFICE");
			hash.put(4, "INDIA_HEAD_OFFICE");
			hash.put(5, "WEBSITE");
			hash.put(6, "MANUFACTUREING_SITE");
			hash.put(7, "MFG_CAPACITY");
			hash.put(8, "YEAR_OF_ESTABLISHMENT");
			hash.put(9, "BRIEF_DESC_OF_COMPANY");
			hash.put(10, "BNC_CATEGORY");
			hash.put(11, "MATERIAL_SERV_PROVIDER");
			hash.put(12, "REVENUE");
			hash.put(13, "PAY_LY");
			hash.put(14, "LY_SPEND");
			hash.put(15, "NO_OF_EMPLOYEES");
			hash.put(16, "L1_LEADER_NAME");
			hash.put(17, "L1_LEADER_EMAIL_ID");
			hash.put(18, "L1_LEADER_CONTACT_NO");
			hash.put(19, "L2_LEADER_NAME");
			hash.put(20, "L2_LEADER_EMAIL_ID");
			hash.put(21, "L2_LEADER_CONTACT_NO");
			hash.put(22, "L2_LEADER_ASSOCIATED");
			hash.put(23, "L2_LEADER_BCCL_ANNUAL");
			hash.put(24, "L2_LEADER_GROUP_CO");
			hash.put(25, "L2_LEADER_MARKET");
			hash.put(26, "UPLOAD_PRODUCT_PORTFOLIO");
			hash.put(27, "TOP_5_CLIENTS");
			hash.put(28, "BusinessPartnerOID");

			ps = con.prepareStatement(sql);
			int j = bussinessdetailslist.size();

			for (int i = 1; i <= (bussinessdetailslist.size() - 2); i++) {
				ps.setString(1, bussinessdetailslist.get(i + 1));
				ps.setString(2, bussinessdetailslist.get(0));
				ps.setString(3, hash.get(i + 1).toString());
				ps.addBatch();
			}
			int[] affectedRecords = ps.executeBatch();
			value = affectedRecords.length;
			ps.close();
			if (value > 0) {
				String sql1 = "UPDATE businsesspartnerdocuments set Company=? where BusinessPartnerOID =?";
				ps = con.prepareStatement(sql1);
				ps.setString(1, bussinessdetailslist.get(1));
				ps.setString(2, bussinessdetailslist.get(0));
				ps.executeUpdate();
				ps.close();
				responsejson.put("message", "success");
			} else {
				responsejson.put("message", "fail");
			}
			jsonArray.add(responsejson);
			con.commit();

		} catch (SQLException e) {
			log.error("getVendorBussDetails() : ", e.fillInStackTrace());
			responsejson.put("message", "fail");
			con.rollback();
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}
/*   commented for vendor registration 28-03-2024
 
	public JSONArray insertVendorAmendment(List<VendorAmendmentSubmit> vendors,String emailId) throws SQLException {

		String fromAddress = null;
		Connection con = null;
		PreparedStatement ps = null;
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		ResultSet rs = null;
		String requesterId = null;
		String partnerOID = null;
		int rCount = 0;
		
		con = DBConnection.getConnection();
		con.setAutoCommit(false);
		
		SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
		SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
		
		try {
			
			String dataCheck = " SELECT COUNT(*) vCount FROM VENDORGENERALDETAILS WHERE  STATUS in ('I','P') AND BUSINESSPARTNEROID = ?";
			
			
			String generalQuery= " Insert into VENDORGENERALDETAILS (BUSINESSPARTNEROID, PURCHASEGROUP, PLANTNAME, PANNUMBER, TITLE, " 
								+" VENDORNAMEL1, VENDORNAMEL2, STREET1, STREET2, STREET3, STREET4, PINCODE, CITY, COUNTRY, TELNO, MOBILENO, "
								+"FAXNO, EMAILID, CREATEDBY, MODIFIEDBY, EMAILID1, EMAILID2, EMAILID3, EMAILID4 ) " 
								+ "Values( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ";
			
			String bankingQuery = "INSERT INTO VENDORBANKDETAILS (BUSINESSPARTNEROID, BANKNAME, BRANCH, BANKACCNO, SWIFTCODE, IFSCCODE, " 
								+"PFREGISTRATIONNO, ESIREGISTRATIONNO, VATREGISTRATIONNO, PANNUMBER, SERVICETAXREGNNO, CSTNOCSTDATE, "
								+"LSTNOLSTDATE, GSTREGISTRATIONNO, TAXCLASSIFICATION, TDSEXEMPTION, TDSEXEMPTIONPERCENT, TDSEXEMPTIONFROM, "
								+"TDSEXEMPTIONTO, MSMESTATUS, MSMENO, CREATEDBY, MODIFIEDBY ) "
								+"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) " ;
			
			String businessQuery = "INSERT INTO VENDORBUSINESSDETAILS (BUSINESSPARTNEROID, TYPEOFBUSINESS, TYPEOFINDUSTRY, PROMOTERS, " 
								+ "TURNOVER, TOP5CLIENT, CLIENTREFERENCE, COMPLIANCECATEGORY, CONSITITUTIONOFBUSINESS, CREATEDBY, MODIFIEDBY ) " 
							    + " VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ";
			
			
			ps = con.prepareStatement(dataCheck);
			ps.setString(1, vendors.get(0).getBusinessPartnerOID());
			rs = ps.executeQuery();
			
			while(rs.next()) {				
				rCount = rs.getInt("vCount");				
			}
			ps.close();
			rs.close();
			
			if(rCount > 0) {
				responsejson.put("message", "Request Has Already Been Submitted.");
				responsejson.put("StatusCode", "203");
			}else { 
				
				if(vendors != null) {
				
					ps1 = con.prepareStatement(generalQuery);
					ps1.setString(1, vendors.get(0).getBusinessPartnerOID());
					ps1.setString(2, vendors.get(0).getPurchaseGroup());
					ps1.setString(3, vendors.get(0).getPlantName());
					ps1.setString(4, vendors.get(0).getPanNumber());
					ps1.setString(5, vendors.get(0).getTitle());
					ps1.setString(6, vendors.get(0).getVendorNameL1());
					ps1.setString(7, vendors.get(0).getVendorNameL2());
					ps1.setString(8, vendors.get(0).getStreet1());
					ps1.setString(9, vendors.get(0).getStreet2());
					ps1.setString(10, vendors.get(0).getStreet3());
					ps1.setString(11, vendors.get(0).getStreet4());
					ps1.setString(12, vendors.get(0).getPinCode());
					ps1.setString(13, vendors.get(0).getCity());
					ps1.setString(14, vendors.get(0).getCountry());
					ps1.setString(15, vendors.get(0).getTelNo());
					ps1.setString(16, vendors.get(0).getMobileNo());
					ps1.setString(17, vendors.get(0).getFaxNo());
					ps1.setString(18, vendors.get(0).getEmailId());
					ps1.setString(19, emailId);
					ps1.setString(20, emailId);
					ps1.setString(21, vendors.get(0).getEmailId1());
					ps1.setString(22, vendors.get(0).getEmailId2());
					ps1.setString(23, vendors.get(0).getEmailId3());
					ps1.setString(24, vendors.get(0).getEmailId4());
					rs = ps1.executeQuery();
					
					ps1.close();
					rs.close();
					
					ps = con.prepareStatement(bankingQuery);
					ps.setString(1, vendors.get(0).getBusinessPartnerOID());
					ps.setString(2, vendors.get(0).getBankName());
					ps.setString(3, vendors.get(0).getBranch());
					ps.setString(4, vendors.get(0).getBankAccNo());
					ps.setString(5, vendors.get(0).getSwiftCode());
					ps.setString(6, vendors.get(0).getIfscCode());
					ps.setString(7, vendors.get(0).getPfRegistrationNo());
					ps.setString(8, vendors.get(0).getEsiRegistrationNo());
					ps.setString(9, vendors.get(0).getVatRegistrationNo());
					ps.setString(10, vendors.get(0).getPanNumber());
					ps.setString(11, vendors.get(0).getServiceTaxRegnNo());					
					ps.setString(12, vendors.get(0).getCstNoCstDate());
					ps.setString(13, vendors.get(0).getLstNoLstDate());
					ps.setString(14, vendors.get(0).getGstRegistrationNo());
					ps.setString(15, vendors.get(0).getTaxClassification());
					ps.setString(16, vendors.get(0).getTdsExemption());
					ps.setString(17, vendors.get(0).getTdsExemptionPercent());
					ps.setString(18, vendors.get(0).getTdsExemptionFrom());
					ps.setString(19, vendors.get(0).getTdsExemptionTo());
					ps.setString(20, vendors.get(0).getMsmeStatus());
					ps.setString(21, vendors.get(0).getMsmeNo());
					ps.setString(22, emailId);
					ps.setString(23, emailId);
					rs = ps.executeQuery();
					
					ps.close();
					rs.close();
					
					ps1 = con.prepareStatement(businessQuery);
					ps1.setString(1, vendors.get(0).getBusinessPartnerOID());
					ps1.setString(2, vendors.get(0).getTypeOfBusiness());
					ps1.setString(3, vendors.get(0).getTypeOfIndustry());
					ps1.setString(4, vendors.get(0).getPromoters());
					ps1.setString(5, vendors.get(0).getTurnOver());
					ps1.setString(6, vendors.get(0).getTop5Client());
					ps1.setString(7, vendors.get(0).getClientReference());
					ps1.setString(8, vendors.get(0).getComplianceCategory());
					ps1.setString(9, vendors.get(0).getConsititutionOfBusiness());
					ps1.setString(10, emailId);
					ps1.setString(11, emailId);
				
					rs = ps1.executeQuery();
					
					ps1.close();
					rs.close();					
					con.commit();
								
					Properties myProp = new Properties();
					InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
					
					try {
						myProp.load(input);
						fromAddress = myProp.getProperty("FROM_ADDRESS");
					} catch (IOException e) {
						log.error("insertVendorAmendment properties error () :", e.fillInStackTrace());
					}
					
					String message = "<p>Dear Valued Partner,</p>" 
							+ "<p>Your amendment request has been generated.</p>"
							+ "<p>Regards,</p>" + "<p>BCCL PartnerDx Team</p>";
				
					Hashtable<String, String> hashTable = new Hashtable<String, String>();
					hashTable.put("fromAddr", fromAddress);
					hashTable.put("toAddr", emailId);
					hashTable.put("subject", "Vendor Amendment Request is Generated. ");
					hashTable.put("content", message);
					
					MailOffice365 myMail = new MailOffice365();
					boolean sendFlag = myMail.sendEmail(hashTable);
					log.info("Mail send :"+sendFlag);
				
					responsejson.put("message", "Success");
					responsejson.put("StatusCode", "200");
				
					
				}else {
					
					responsejson.put("message", "No Data Found");
					responsejson.put("StatusCode", "203");
				}
		     }
				jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("insertVendorAmendment() :", e.fillInStackTrace());
			//System.out.println("insertVendorAmendment() :"+ e.fillInStackTrace());			
			con.rollback();
			responsejson.put("StatusCode", "406");
			jsonArray.add(responsejson);
			return jsonArray;
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}
	*/
}
