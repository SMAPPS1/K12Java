package com.bccl.dxapi.apiimpl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;
import java.util.Base64;

import javax.servlet.http.HttpSession;

import java.sql.Timestamp;


import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.bccl.dxapi.apiutility.AESencrp;
import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.apiutility.MailOffice365;
//import com.bccl.dxapi.apiutility.MailOffice365;
import com.bccl.dxapi.apiutility.Validation;
import com.bccl.dxapi.security.GenrateToken;
import com.bccl.dxapi.security.SendImapMessage;

public class LoginImpl {

	static Logger log = Logger.getLogger(LoginImpl.class.getName());

	public LoginImpl() {
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
	private static final int OTP_EXPIRY_TIME = 5 * 60 * 1000; // change 2 * 60 * 1000; to 10 * 60 * 1000

	private static String businesspartnerID = "";
	private static String bussinesspartnertext = "";
	private static String PrimaryEmailID = "";
	private static String SecondaryEmailID = "";
	private static String TertiaryEmailID = "";
	private static String profileID = "";
	String panCard = null;
	String enc_pan = "";
	String token = "";

	public JSONArray sendOTPinEmail(String email, String pan) throws SQLException {

		if (email.equalsIgnoreCase(PrimaryEmailID) || email.equalsIgnoreCase(SecondaryEmailID)
				|| email.equalsIgnoreCase(TertiaryEmailID)) {
			String otp = GenrateToken.random(6);
			int value = 0;
			String message = "<p>Dear Valued Partner,</p>" + "<p>Your OTP is <strong>" + otp + "</strong></p>"
					+ "<p>Please enter this OTP on the BCCL PartnerDx to complete the login process. Please note this OTP is valid for 1 minute only.</p>"
					+ "<p>Regards,</p>" + "<p>BCCL PartnerDx Team</p>";

			String sqlUpdate = "insert into otpfactory (BUSINESSPARTNEROID, PROFILEID, OTP, CREATEDBY, CREATEDON, MODIFIEDBY, MODIFIEDON, STATUS) values (?,?,?,?,?,?,?,?)";
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = DBConnection.getConnection();

				ps = con.prepareStatement(sqlUpdate);
				ps.setString(1, businesspartnerID);
				ps.setString(2, "-");
				ps.setInt(3, Integer.parseInt(otp));
				ps.setString(4, email);
				ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(6, email);
				ps.setTimestamp(7, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(8, "A");

				value = ps.executeUpdate();
				Hashtable<String, String> hashTable = new Hashtable<String, String>();
				hashTable.put("fromAddr", "donotreply.Procurement@orchids.edu.in");
				hashTable.put("toAddr", email);
				hashTable.put("subject", "OTP from  Partner Portal.");
				hashTable.put("content", message);
				Properties prop = new Properties();
				InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
				try {
					prop.load(input);
				} catch (IOException e) {
					log.error("sendOTPinEmail() 1 :", e.fillInStackTrace());					
				}
				String server = prop.getProperty("servertype");
				if ("uat".equalsIgnoreCase(server)) {
					//SendImapMessage myMail = new SendImapMessage();
					//myMail.sendHtmlMail(hashTable);
					//MailOffice365 myMail = new MailOffice365();
					//myMail.sendEmail(hashTable);
				}
				responsejson.put("message", "OTP send to email address ");
				responsejson.put("OTP", otp);
			} catch (SQLException e) {
				log.error("sendOTPinEmail() 2 :", e.fillInStackTrace());
				
				responsejson.put("message", "Fail");
			} finally {
				DBConnection.closeConnection(rs, ps, con);
			}
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public String getPanDetails(String bid) throws SQLException {

		String pan = null;
		String po_data = "Select BusinessPartnerText,PANNumber from businesspartner where BusinessPartnerOID=?";
		businesspartnerID = bid;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, bid);
			rs = ps.executeQuery();
			while (rs.next()) {
				bussinesspartnertext = rs.getString("BusinessPartnerText");
				pan = rs.getString("PANNumber");
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			log.error("getPanDetails() :", e.fillInStackTrace());
			
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return pan;
	}

	public JSONArray getOtpVerifyEmail(String email, String pan, Integer otp) throws SQLException {
// hello
		boolean result;
		result = Validation.StringChecknull(email);
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

		if (otp == null) {
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

			String sql = "select BusinessPartnerOID,OTP,CreatedOn,CreatedBy FROM otpfactory where CreatedBy = ? "
					+ "AND otp = ? AND Status = ?  order by CreatedOn desc";
			Timestamp modifyDate = null;
			String id = "";
			jsonArray = new JSONArray();
			JSONObject responsejson = new JSONObject();
			ps = con.prepareStatement(sql);
			ps.setString(1, email);
			ps.setInt(2, otp);
			ps.setString(3, "A");
			rs = ps.executeQuery();
			while (rs.next()) {
				id = rs.getString("BusinessPartnerOID");
				modifyDate = rs.getTimestamp("CreatedOn");
				break;
			}
			rs.close();
			ps.close();

			if (id != "") {

				long fiveAgo = modifyDate.getTime();
				long tenAgo = System.currentTimeMillis() - OTP_EXPIRY_TIME;
				if (fiveAgo > tenAgo) {
					responsejson.put("message", "Valid Otp");
					responsejson.put("bussinesspartnertext", bussinesspartnertext);
					responsejson.put("bussinesspartneroid", id);

					String sqlUpdate = "UPDATE otpfactory SET ModifiedBy = ?, ModifiedOn=? ,Status=? "
							+ " WHERE CreatedBy=? AND BusinessPartnerOID=?";
					ps = con.prepareStatement(sqlUpdate);
					ps.setString(1, email);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, "E");
					ps.setString(4, email);
					ps.setString(5, id);
					ps.executeUpdate();
					ps.close();
					int i = Integer.parseInt(id);
					String sqlUpdate1 = "insert into signonhistory (BusinessPartnerOID,ProfileID,SignOnDate) values (?,?,?)";
					ps = con.prepareStatement(sqlUpdate1);
					ps.setString(1, businesspartnerID);
					ps.setString(2, "-");
					ps.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.executeUpdate();
					ps.close();
				} else {
					String sqlUpdate = "UPDATE otpfactory SET ModifiedBy = ?,ModifiedOn=?,Status=?  WHERE OTP=? "
							+ "AND CreatedBy=?";
					ps = con.prepareStatement(sqlUpdate);
					ps.setString(1, email);
					ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(3, "E");
					ps.setInt(4, otp);
					ps.setString(5, email);
					ps.executeUpdate();
					responsejson.put("message", "OTP Expired");
				}
			} else {
				responsejson.put("message", "Invalid OTP");
			}
			jsonArray.add(responsejson);
		} catch (SQLException e) {
			log.error("getOtpVerifyEmail() :", e.fillInStackTrace());
			
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;

	}

	public String getEmailIDSendMail(String email, HashMap<String, String> hashMap, String loginType)
			throws SQLException {

		String emailmessage = null;
		String otp = GenrateToken.random(6);
		int value = 0;
		String message = "";
		log.info("otp " + otp);
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		if (loginType.equals("user")) {
			message = "<p>Dear User,</p>" + "<p>Your OTP is <strong>" + otp + "</strong></p>"
				+ "<p>Please enter this OTP on the Partner Portal to complete the login process. Please note this OTP is valid for 5 minutes only.</p>"
					+ "<p>Regards,</p>" + "<p>Admin Team</p>";
			String insertotp = "insert into OTPUSERFACTORY (ProfileID,OTP,CreatedBy,CreatedOn,Status,LoginType) values (?,?,?,?,?,?)";

			try {
				String userId = GenrateToken.random(6);
				con = DBConnection.getConnection();
				ps = con.prepareStatement(insertotp);
				ps.setString(1, userId);
				ps.setInt(2, Integer.parseInt(otp));
				ps.setString(3, email);
				ps.setTimestamp(4, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(5, "A");
				ps.setString(6, "USER");
				value = ps.executeUpdate();
				ps.close();
			} catch (SQLException e) {
				emailmessage = "Fail";
				log.error("getEmailIDSendMail() 1 : ", e.fillInStackTrace());
				return emailmessage;
			}

		} else {
			message = "<p>Dear Valued Partner,</p>" + "<p>Your OTP is <strong>" + otp + "</strong></p>"
					+ "<p>Please enter this OTP on the Partner Portal to complete the login process. Please note this OTP is valid for 5 minutes only.</p>"
				+ "<p>Regards,</p>" + "<p>Admin Team</p>";

		String sql = "select * from otpfactory where BusinessPartnerOID=? and CreatedBy=?";

		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, hashMap.get("BUSINESSPARTNEROID"));
			ps.setString(2, email);
			rs = ps.executeQuery();
			String tempvalue = "";
			while (rs.next()) {
				tempvalue = rs.getString("BUSINESSPARTNEROID");
				break;
			}
			rs.close();
			ps.close();
			if (tempvalue != "") {
				String updateotp = "update otpfactory set otp=?,createdon=?,status=? where  BusinessPartnerOID=? AND CreatedBy=?";
				ps = con.prepareStatement(updateotp);
				ps.setInt(1, Integer.parseInt(otp));
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, "A");
				ps.setString(4, hashMap.get("BUSINESSPARTNEROID"));
				ps.setString(5, email);
				value = ps.executeUpdate();
				ps.close();
			} else {
				String insertotp = "insert into otpfactory (BusinessPartnerOID,ProfileID,OTP,CreatedBy,CreatedOn,Status) values (?,?,?,?,?,?)";

				try {
					ps = con.prepareStatement(insertotp);
					ps.setString(1, hashMap.get("BUSINESSPARTNEROID"));
					ps.setString(2, "-");
					ps.setInt(3, Integer.parseInt(otp));
					ps.setString(4, email);
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(6, "A");
					value = ps.executeUpdate();
					ps.close();
				} catch (SQLException e) {
					emailmessage = "Fail";
					log.error("getEmailIDSendMail() 1 : ", e.fillInStackTrace());
					
					return emailmessage;
				}
			}
			} catch (SQLException e) {
				emailmessage = "Fail";
				log.error("getEmailIDSendMail() 3 : ", e.fillInStackTrace());

				return emailmessage;
			} finally {
				DBConnection.closeConnection(rs, ps, con);
			}
		}

			// commented 23-02-2024 dotone server
			Hashtable<String, String> hashTable = new Hashtable<String, String>();
			hashTable.put("fromAddr", "donotreply.Procurement@orchids.edu.in");
			hashTable.put("toAddr", email);
			hashTable.put("subject", "OTP from Partner Portal");
			hashTable.put("content", message);
			Properties prop = new Properties();
			InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
			try {
				prop.load(input);
			} catch (IOException e) {
				log.error("getEmailIDSendMail() 2 : ", e.fillInStackTrace());
				
			}
			String server = prop.getProperty("servertype");
			if ("uat".equalsIgnoreCase(server)) {
				//SendImapMessage myMail = new SendImapMessage();
				//myMail.sendHtmlMail(hashTable);
				//MailOffice365 myMail = new MailOffice365();
				//myMail.sendEmail(hashTable);
			}
			
			emailmessage = "OTP send to email address " + otp;
			return emailmessage;
	}

	public String getEmailIDSendMail(String email, HashMap<String, String> hashMap)
			throws SQLException {

		String emailmessage = null;
		String otp = GenrateToken.random(6);
		int value = 0;
		String message = "";
		log.info("otp " + otp);
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		message = "<p>Dear Valued Partner,</p>" + "<p>Your OTP is <strong>" + otp + "</strong></p>"
					+ "<p>Please enter this OTP on the Partner Portal to complete the login process. Please note this OTP is valid for 5 minutes only.</p>"
				+ "<p>Regards,</p>" + "<p>Admin Team</p>";

		String sql = "select * from otpfactory where BusinessPartnerOID=? and CreatedBy=?";

		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, hashMap.get("BUSINESSPARTNEROID"));
			ps.setString(2, email);
			rs = ps.executeQuery();
			String tempvalue = "";
			while (rs.next()) {
				tempvalue = rs.getString("BUSINESSPARTNEROID");
				break;
			}
			rs.close();
			ps.close();
			if (tempvalue != "") {
				String updateotp = "update otpfactory set otp=?,createdon=?,status=? where  BusinessPartnerOID=? AND CreatedBy=?";
				ps = con.prepareStatement(updateotp);
				ps.setInt(1, Integer.parseInt(otp));
				ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
				ps.setString(3, "A");
				ps.setString(4, hashMap.get("BUSINESSPARTNEROID"));
				ps.setString(5, email);
				value = ps.executeUpdate();
				ps.close();
			} else {
				String insertotp = "insert into otpfactory (BusinessPartnerOID,ProfileID,OTP,CreatedBy,CreatedOn,Status) values (?,?,?,?,?,?)";

				try {
					ps = con.prepareStatement(insertotp);
					ps.setString(1, hashMap.get("BUSINESSPARTNEROID"));
					ps.setString(2, "-");
					ps.setInt(3, Integer.parseInt(otp));
					ps.setString(4, email);
					ps.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
					ps.setString(6, "A");
					value = ps.executeUpdate();
					ps.close();
				} catch (SQLException e) {
					emailmessage = "Fail";
					log.error("getEmailIDSendMail() 1 : ", e.fillInStackTrace());
					
					return emailmessage;
				}
			}
			} catch (SQLException e) {
				emailmessage = "Fail";
				log.error("getEmailIDSendMail() 3 : ", e.fillInStackTrace());

				return emailmessage;
			} finally {
				DBConnection.closeConnection(rs, ps, con);
			}
		

			// commented 23-02-2024 dotone server
			Hashtable<String, String> hashTable = new Hashtable<String, String>();
			hashTable.put("fromAddr", "donotreply.Procurement@orchids.edu.in");
			hashTable.put("toAddr", email);
			hashTable.put("subject", "OTP from Partner Portal");
			hashTable.put("content", message);
			Properties prop = new Properties();
			InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
			try {
				prop.load(input);
			} catch (IOException e) {
				log.error("getEmailIDSendMail() 2 : ", e.fillInStackTrace());
				
			}
			String server = prop.getProperty("servertype");
			if ("uat".equalsIgnoreCase(server)) {
				//SendImapMessage myMail = new SendImapMessage();
				//myMail.sendHtmlMail(hashTable);
				//MailOffice365 myMail = new MailOffice365();
				//myMail.sendEmail(hashTable);
			}
			
			emailmessage = "OTP send to email address " + otp;
			return emailmessage;
	}

	public JSONArray getEmailDetails(String email) throws SQLException {

		boolean result = Validation.StringChecknull(email);
		boolean flag = false;
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

		Properties prop = new Properties();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		try {
			prop.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String serverType = prop.getProperty("servertype");
		
		String compCodeJoinQuery=" join COMP_COMPANY v on v.COMP_COMPANYCODE = pd.companycode ";
		if("uat".equalsIgnoreCase(serverType)) {
			compCodeJoinQuery=" join VW_GET_COMPANYLIST v on v.COMP_COMPANYCODE = pd.companycode ";			
		}
		
		String po_data = "select DISTINCT b.BusinessPartnerOID, b.status "
				+ "from businesspartner b  "
				+ "join podetails pd on pd.businesspartneroid = b.BusinessPartnerOID  "
				+ compCodeJoinQuery
				+ "where (b.PrimaryEmailID= ? or  "
				+ "b.SecondaryEmailID= ? or b.TertiaryEmailID= ?) "
				+ "UNION "
				+ "select b.BusinessPartnerOID, b.status  "
				+ "from businesspartnerattributes b "
				+ "join podetails pd on pd.businesspartneroid = b.BusinessPartnerOID  "
				+ compCodeJoinQuery
				+ "where b.AttributeValue= ? "
				+ "UNION "
				+ "select b.BusinessPartnerOID, b.status  "
				+ "from businesspartnercontacts b "
				+ "join podetails pd on pd.businesspartneroid = b.BusinessPartnerOID  "
				+ compCodeJoinQuery
				+ "where b.ContactEmailID= ? "
				+ "ORDER BY Status ASC,BusinessPartnerOID ASC";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		PreparedStatement ps1 = null;
		ResultSet rs = null;
		ResultSet rs1 = null;
		boolean flagValidEmailid = false;
		boolean flagValidCompany = false;
		
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
				String status = null;
				status = rs.getString("status");
				if ("A".equalsIgnoreCase(status)) {
					poData.put("BUSINESSPARTNEROID", rs.getString("BusinessPartnerOID"));
					POList.add(poData);
				} else if ("C".equalsIgnoreCase(status)) {
					responsejson.put("message", "deactivated");
					flag = true;
				} else {
					responsejson.put("message", "No Data Found for given Email Id");
					flag = true;
				}
				break;
			}
			rs.close();
			ps.close();

			if (POList.size() > 0) {
				responsejson.put("poData", POList.get(0));
				String message = getEmailIDSendMail(email, POList.get(0));
				System.out.println("before message : "+message);
				//  commented for encreption testing 05-01-2024
				
				//commented encription code on 23-02-2024 for dotoneserver
				//AESencrp  messageEncrp = new AESencrp();
				try {
					//message = messageEncrp.encrypt(message);
					System.out.println("after message : "+message);
				} catch (Exception e) {
					log.error("getEmailDetails() encrption :", e.fillInStackTrace());
				} 
				
				responsejson.put("message", message);
				responsejson.put("Return_Code","200");
			
				jsonArray.add(responsejson);
			} else {
				
				
				String vaildEmailid = " Select DISTINCT b.BusinessPartnerOID, b.status "
						+ "from businesspartner b "
						+ "where (b.PrimaryEmailID = ? or "
						+ "b.SecondaryEmailID = ?  or b.TertiaryEmailID = ? ) "
						+ "UNION "
						+ "select b.BusinessPartnerOID, b.status "
						+ "from businesspartnerattributes b "
						+ "where b.AttributeValue= ? "
						+ "UNION "
						+ "select b.BusinessPartnerOID, b.status "
						+ "from businesspartnercontacts b "
						+ "where b.ContactEmailID = ?  "
						+ "ORDER BY Status ASC,BusinessPartnerOID ASC ";
				
				ps = con.prepareStatement(vaildEmailid);
				ps.setString(1, email);
				ps.setString(2, email);
				ps.setString(3, email);
				ps.setString(4, email);
				ps.setString(5, email);
				rs = ps.executeQuery();
				while (rs.next()) {
					flagValidEmailid = true;				
				
					String vaildCompany = "select DISTINCT b.BusinessPartnerOID, b.status "
							+ "from businesspartner b  "
							+ "join podetails pd on pd.businesspartneroid = b.BusinessPartnerOID  "
							+ "join VW_GET_COMPANYLIST v on v.COMP_COMPANYCODE = pd.companycode "
							+ "where (b.PrimaryEmailID = ? or "
							+ "b.SecondaryEmailID = ?  or b.TertiaryEmailID = ? ) ";
					
					ps1 = con.prepareStatement(vaildCompany);
					ps1.setString(1, email);
					ps1.setString(2, email);
					ps1.setString(3, email);
					rs1 = ps1.executeQuery();
					while (rs1.next()) {
						flagValidCompany = true;	
					}						
					ps1.close();
					rs1.close();					
				}
				ps.close();
				rs.close();
				
				if (flag == false && flagValidEmailid == false) {
					responsejson.put("message", "No Data Found for given Email Id");
					responsejson.put("Return_Code","401");
					
				}else if(flagValidEmailid == true && flagValidCompany == false) {
					responsejson.put("message", "No PO is assigned for given Email Id");
					responsejson.put("Return_Code","401");						
				}
				jsonArray.add(responsejson);
			}
		} catch (SQLException e) {
			log.error("getEmailDetails() :", e.fillInStackTrace());
			
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
	}

	public JSONArray getPOSignonhistory(String bid) throws SQLException {

		String ProfileID = null;
		String po_data = "Select ProfileID from businesspartnerprofile where BusinessPartnerOID=?";

		String po_data1 = "Select BusinessPartnerText from businesspartner where BusinessPartnerOID=?";

		String sqlUpdate = "insert into signonhistory (BusinessPartnerOID,ProfileID,SignOnDate) values (?,?,?)";
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(po_data);
			ps.setString(1, bid);
			rs = ps.executeQuery();
			while (rs.next()) {
				ProfileID = rs.getString("ProfileID");
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement(po_data1);
			ps.setString(1, bid);
			rs = ps.executeQuery();
			while (rs.next()) {
				bussinesspartnertext = rs.getString("BusinessPartnerText");
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement(sqlUpdate);
			ps.setString(1, bid);
			ps.setString(2, ProfileID);
			ps.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
			ps.executeUpdate();

			responsejson.put("Bussinesspartnertext", bussinesspartnertext);
			responsejson.put("message", "Success");
			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("getPOSignonhistory() :", e.fillInStackTrace());
			
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}

	public JSONArray internalsignon(String email) {

		responsejson.put("status", "Success");
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray getChecktheStoreKepeer(String email, HttpSession session) throws SQLException {

		int countUser = 0;
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

		String queryData = "select count(*) as count1 from INVENTORYUSERLIST where userid = ? and status ='A' ";

		ArrayList<HashMap<String, String>> POList = new ArrayList<HashMap<String, String>>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			ps = con.prepareStatement(queryData);
			ps.setString(1, email);
			rs = ps.executeQuery();
			while (rs.next()) {
				countUser = Integer.parseInt(rs.getString("count1"));
			}
			rs.close();
			ps.close();
			if (countUser > 0) {
				session.setAttribute("shopkepeer", "true");
				responsejson.put("message", true);
			} else {
				session.setAttribute("shopkepeer", "false");
				responsejson.put("message", "No Data Found"); // will check in front end.
			}
			responsejson.put("status", "Success");
			jsonArray.add(responsejson);

		} catch (SQLException e) {
			log.error("getChecktheStoreKepeer() :", e.fillInStackTrace());
			
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}

		return jsonArray;
	}
	
	public JSONArray getUserEmailDetails(String email) {
		try {
			boolean result = Validation.StringChecknull(email);
			boolean flag = false;
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

			Properties prop = new Properties();
			InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
			try {
				prop.load(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String serverType = prop.getProperty("servertype");
			String message = getEmailIDSendMail(email, null, "user");
			System.out.println("before message : " + message);
			// commented for encreption testing 05-01-2024

			// commented encription code on 23-02-2024 for dotoneserver
			//AESencrp messageEncrp = new AESencrp();
			try {
				//message = messageEncrp.encrypt(message);
				// System.out.println("after message : "+message);
			} catch (Exception e) {
				log.error("getEmailDetails() encrption :", e.fillInStackTrace());
			}

			responsejson.put("message", message);
			responsejson.put("Return_Code", "200");

			jsonArray.add(responsejson);

		} catch (SQLException e) {
				log.error("getEmailDetails() :", e.fillInStackTrace());
				responsejson.put("message", "Fail");
				jsonArray.add(responsejson);
			} 
			return jsonArray;
	}
	
	public JSONArray getOtpUserVerifyEmail(String email, String pan, Integer otp) throws SQLException {
		// hello
				boolean result;
				result = Validation.StringChecknull(email);
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

				if (otp == null) {
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

					String sql = "select ProfileID,OTP,CreatedOn,CreatedBy FROM OTPUSERFACTORY where CreatedBy = ? "
							+ "AND otp = ? AND Status = ?  order by CreatedOn desc";
					Timestamp modifyDate = null;
					String id = "";
					jsonArray = new JSONArray();
					JSONObject responsejson = new JSONObject();
					ps = con.prepareStatement(sql);
					ps.setString(1, email);
					ps.setInt(2, otp);
					ps.setString(3, "A");
					rs = ps.executeQuery();
					while (rs.next()) {
						id = rs.getString("ProfileID");
						modifyDate = rs.getTimestamp("CreatedOn");
						break;
					}
					rs.close();
					ps.close();

					if (id != "") {

						long fiveAgo = modifyDate.getTime();
						long tenAgo = System.currentTimeMillis() - OTP_EXPIRY_TIME;
						if (fiveAgo > tenAgo) {
							responsejson.put("message", "Valid Otp");
					//		responsejson.put("bussinesspartnertext", bussinesspartnertext);
					//		responsejson.put("bussinesspartneroid", id);

							String sqlUpdate = "UPDATE OTPUSERFACTORY SET ModifiedBy = ?, ModifiedOn=? ,Status=? "
									+ " WHERE CreatedBy=? AND ProfileID=?";
							ps = con.prepareStatement(sqlUpdate);
							ps.setString(1, email);
							ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
							ps.setString(3, "E");
							ps.setString(4, email);
							ps.setString(5, id);
							ps.executeUpdate();
							ps.close();
							/*
							int i = Integer.parseInt(id);
							String sqlUpdate1 = "insert into signonhistory (BusinessPartnerOID,ProfileID,SignOnDate) values (?,?,?)";
							ps = con.prepareStatement(sqlUpdate1);
							ps.setString(1, "-");
							ps.setString(2, id);
							ps.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
							ps.executeUpdate();
							ps.close();
							*/
						} else {
							String sqlUpdate = "UPDATE OTPUSERFACTORY SET ModifiedBy = ?,ModifiedOn=?,Status=?  WHERE OTP=? "
									+ "AND CreatedBy=?";
							ps = con.prepareStatement(sqlUpdate);
							ps.setString(1, email);
							ps.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
							ps.setString(3, "E");
							ps.setInt(4, otp);
							ps.setString(5, email);
							ps.executeUpdate();
							responsejson.put("message", "OTP Expired");
						}
					} else {
						responsejson.put("message", "Invalid OTP");
					}
					
					jsonArray.add(responsejson);
					
				} catch (SQLException e) {
					log.error("getOtpVerifyEmail() :", e.fillInStackTrace());
					
					responsejson.put("message", "Fail");
					jsonArray.add(responsejson);
				} finally {
					DBConnection.closeConnection(rs, ps, con);
				}
				return jsonArray;

			}

	/* commented on vendor registration on 28-03-2024
	 
	public JSONArray vandorRegistration(String buyerEmailId, String emailId, String mobileNo) throws SQLException {

			int value = 0;
			String countValue= "";
			String fromAddress = null;
			String urlLink = null;
			
			String sqlSequence = " Select count(*) seq from requesterDetails where REQUESTEREMAILID = ?  " 
								+" and status in ('P','I') ";
			
			String sqlUpdate ="Insert into requesterDetails "
			+ "(REQUESTEROID,REQUESTEREMAILID,BUYEREMAILID,MOBILENO,REQUESTSTATUS,CREATEDBY,MODIFIEDBY,STATUS) "
			+ "values (REQUESTER_SEQ.nextval,?,?,?,?,?,?,?)" ;
			
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				con = DBConnection.getConnection();
				
				ps = con.prepareStatement(sqlSequence);
				ps.setString(1, emailId);
				rs = ps.executeQuery();
								
				while(rs.next()) {
					value = rs.getInt("seq");
				}
				rs.close();
				ps.close();
							
				if(value > 0) {
					responsejson.put("Email", "Your Request is Already Registered.");
					responsejson.put("message", "Fail");
					responsejson.put("StatusCode", "203");					
				}else {
					
					ps = con.prepareStatement(sqlUpdate);
					ps.setString(1, emailId);
					ps.setString(2, buyerEmailId);
					ps.setString(3, mobileNo);
					ps.setString(4, "I");
					ps.setString(5, buyerEmailId);
					ps.setString(6, buyerEmailId);
					ps.setString(7, "I");
					value = ps.executeUpdate();
										
					Properties myProp = new Properties();
					InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
					
					try {
						myProp.load(input);
						fromAddress = myProp.getProperty("FROM_ADDRESS");
						urlLink = myProp.getProperty("URL_LINK");
					} catch (IOException e) {
						log.error("vandorRegistration properties error () :", e.fillInStackTrace());												
					}							
					
					String message = "<p>Dear Valued Partner,</p>" 
							+"Partner registration link is: "+urlLink
							+ "<p>Regards,</p>" + "<p>PartnerDx Team</p>";
					
					Hashtable<String, String> hashTable = new Hashtable<String, String>();
					hashTable.put("fromAddr", fromAddress);
					hashTable.put("toAddr", emailId);
					hashTable.put("subject", "Partner Registration Link");
					hashTable.put("content", message);
					
					MailOffice365 myMail = new MailOffice365();
					boolean sendFlag = myMail.sendEmail(hashTable);
					log.info("Mail send :"+sendFlag);
					
					responsejson.put("Email", "Mail is sent to partner email id");
					responsejson.put("message", "Success");
					responsejson.put("StatusCode", "200");
					
				}
					
			} catch (SQLException e) {
				log.error("vandorRegistration() :", e.fillInStackTrace());
				responsejson.put("StatusCode", "401");
				responsejson.put("message", "Fail");
			} finally {
				DBConnection.closeConnection(rs, ps, con);
			}
		
		jsonArray.add(responsejson);
		return jsonArray;
	}

	@SuppressWarnings("resource")
	public JSONArray getVandorEmailID(String emailId) throws SQLException {

		int value = 0;
		String rOID = null;
		String pemailid = null;
		String mobileNo = null;
		String fromAddress = null;
		String status = null;
		String sqlQuery = "Select REQUESTEROID, REQUESTEREMAILID, MOBILENO, STATUS from REQUESTERDETAILS WHERE "
				+ "REQUESTEREMAILID = ?  ";
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			
			ps = con.prepareStatement(sqlQuery);
			ps.setString(1, emailId);			
			rs = ps.executeQuery();
			
			while(rs.next()) {
				rOID = rs.getString("REQUESTEROID")==null?"NA":rs.getString("REQUESTEROID");
				pemailid = rs.getString("REQUESTEREMAILID")==null?"NA":rs.getString("REQUESTEREMAILID");
				mobileNo = rs.getString("MOBILENO")==null?"NA":rs.getString("MOBILENO");
				status =  rs.getString("STATUS")==null?"NA":rs.getString("STATUS");
			}
			rs.close();
			ps.close();
			
			if("I".equalsIgnoreCase(status)) {
				
				String otp = GenrateToken.random(6);
				
				if(rOID !=null ) {
					
					String message = "<p>Dear Valued Partner,</p>" + "<p>Your OTP is <strong>" + otp + "</strong></p>"
							+ "<p>Please enter this OTP on the PartnerDx to complete the vendor registration process. Please note this OTP is valid for 5 minute only.</p>"
							+ "<p>Regards,</p>" + "<p>BCCL PartnerDx Team</p>";

					String sqlUpdate = " Insert into PARTNEROTPFACTORY (PARTNEROID, OTP, CREATEDBY, MODIFIEDBY, STATUS) values (?,?,?,?,?) ";
					
					try {
						con = DBConnection.getConnection();
						ps = con.prepareStatement(sqlUpdate);
						ps.setString(1, rOID);
						ps.setString(2, otp);
						ps.setString(3, emailId);
						ps.setString(4, emailId);
						ps.setString(5, "A");
						
						value = ps.executeUpdate();
											
						Properties myProp = new Properties();
						InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
						
						try {
							myProp.load(input);
							fromAddress = myProp.getProperty("FROM_ADDRESS");
						} catch (IOException e) {
							log.error("getVandorEmailID properties error () :", e.fillInStackTrace());
						}
					
						Hashtable<String, String> hashTable = new Hashtable<String, String>();
						hashTable.put("fromAddr", fromAddress);
						hashTable.put("toAddr", emailId);
						hashTable.put("subject", "OTP from Partner registration ");
						hashTable.put("content", message);
						
						MailOffice365 myMail = new MailOffice365();
						boolean sendFlag = myMail.sendEmail(hashTable);
						log.info("Mail send :"+sendFlag);
						
						responsejson.put("message", "OTP send to email address ");
						responsejson.put("OTP", otp);
						System.out.println("OTP is "+otp);
						
					} catch (SQLException e) {
						log.error("getVandorEmailID() 2 :", e.fillInStackTrace());				
						responsejson.put("message", "Fail");
					} finally {
						DBConnection.closeConnection(rs, ps, con);
					}
				
				}
				
					if(!"NA".equalsIgnoreCase(rOID)) {
						responsejson.put("StatusCode", "200");
						responsejson.put("message", "Success");
					}
								
			}else {
				
				if(status == null) {
					responsejson.put("message", "EmailId is not registered.");
					responsejson.put("StatusCode", "203");
					responsejson.put("Status", "Fail");
				}else {
					responsejson.put("message", "EmailId is already registered.");
					responsejson.put("StatusCode", "203");
					responsejson.put("Status", "Fail");
				}
			}
			
			
					
		} catch (SQLException e) {
			log.error("getVandorEmailID() :", e.fillInStackTrace());	
			responsejson.put("StatusCode", "401");
			responsejson.put("message", "Fail");
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
	
	jsonArray.add(responsejson);
	return jsonArray;
}
		
	@SuppressWarnings("resource")
	public JSONArray getVandorOTPCheck(String emailId,String otp) throws SQLException {

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

		if (otp == null) {
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

			String sql = "select PARTNEROID,OTP,CREATEDON  FROM PARTNEROTPFACTORY where CREATEDBY = ? "
					+ "AND OTP = ? AND STATUS = ?  order by CREATEDON desc";
			Timestamp modifyDate = null;
			String id = "";
			jsonArray = new JSONArray();
			JSONObject responsejson = new JSONObject();
			ps = con.prepareStatement(sql);
			ps.setString(1, emailId);
			ps.setString(2, otp);
			ps.setString(3, "A");
			rs = ps.executeQuery();
			while (rs.next()) {
				id = rs.getString("PARTNEROID");
				modifyDate = rs.getTimestamp("CreatedOn");
				break;
			}
			rs.close();
			ps.close();

			if (id != "") {

				long fiveAgo = modifyDate.getTime();
				long tenAgo = System.currentTimeMillis() - OTP_EXPIRY_TIME;
				if (fiveAgo > tenAgo) {
					enc_pan = GenrateToken.encrypt(emailId);
					token = GenrateToken.issueToken(emailId, "1234");
					responsejson.put("message", "Valid Otp");
					responsejson.put("PARTNEROID", id);

					String sqlUpdate = "UPDATE PARTNEROTPFACTORY SET MODIFIEDBY = ?, STATUS = ?, MODIFIEDON = sysdate "
							+ " WHERE CREATEDBY = ? AND PARTNEROID = ? ";
					ps = con.prepareStatement(sqlUpdate);
					ps.setString(1, emailId);
					ps.setString(2, "E");
					ps.setString(3, emailId);
					ps.setString(4, id);
					ps.executeUpdate();
					ps.close();
					
				} else {
					String sqlUpdate = "UPDATE PARTNEROTPFACTORY SET MODIFIEDBY = ?, STATUS = ?, MODIFIEDON = sysdate "
							+ " WHERE CREATEDBY = ? AND PARTNEROID = ? ";
					ps = con.prepareStatement(sqlUpdate);
					ps.setString(1, emailId);					
					ps.setString(2, "E");
					ps.setString(3, emailId);
					ps.setString(4, id);
					ps.executeUpdate();
					responsejson.put("message", "OTP Expired");
				}
			} else {
				responsejson.put("message", "Invalid OTP");
			}
			jsonArray.add(responsejson);
		} catch (SQLException e) {
			log.error("getOtpVerifyEmail() :", e.fillInStackTrace());
			
			responsejson.put("message", "Fail");
			jsonArray.add(responsejson);
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}
		return jsonArray;
}

	@SuppressWarnings("resource")
	public String getMobileNo(String emailId) throws SQLException {

		int value = 0;
		String rOID = null;
		String pemailid = null;
		String mobileNo = null;
		String sqlQuery = "Select REQUESTEROID, REQUESTEREMAILID, MOBILENO from REQUESTERDETAILS WHERE  REQUESTEREMAILID = ? and Status = ? ";
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DBConnection.getConnection();
			
			ps = con.prepareStatement(sqlQuery);
			ps.setString(1, emailId);
			ps.setString(2, "I");
			rs = ps.executeQuery();
			
			while(rs.next()) {
				rOID = rs.getString("REQUESTEROID")==null?"NA":rs.getString("REQUESTEROID");
				pemailid = rs.getString("REQUESTEREMAILID")==null?"NA":rs.getString("REQUESTEREMAILID");
				mobileNo = rs.getString("MOBILENO")==null?"NA":rs.getString("MOBILENO");
			}
			rs.close();
			ps.close();
			
		} catch (SQLException e) {
			log.error("getVandorEmailID() :", e.fillInStackTrace());			
		} finally {
			DBConnection.closeConnection(rs, ps, con);
		}	
	
	return mobileNo;	
	}
	*/
}
