package com.bccl.dxapi.controller;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.bccl.dxapi.apiimpl.DashboardImpl;
import com.bccl.dxapi.apiimpl.InternalportalImpl;
import com.bccl.dxapi.apiimpl.LoginImpl;
import com.bccl.dxapi.apiimpl.SimpoImpl;
import com.bccl.dxapi.security.GenrateToken;

@Path("/Login")
public class LoginController {

	@Context
	private HttpServletRequest httpRequest;
	@Context
	public javax.servlet.http.HttpServletResponse response;

	static Logger log = Logger.getLogger(LoginController.class.getName());

	@POST
	@Path("/chklogin")
	@Produces(MediaType.APPLICATION_JSON)
	public Response displayUserEmail(@FormParam("email") String email) throws SQLException {

		JSONArray jsonArray = new JSONArray();
		LoginImpl objlogin = new LoginImpl();

		jsonArray = objlogin.getEmailDetails(email);
		return Response.status(200).entity(jsonArray).type(MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	@Path("/user/chklogin")
	@Produces(MediaType.APPLICATION_JSON)
	public Response displayEmail(@FormParam("email") String email) throws SQLException {

		JSONArray jsonArray = new JSONArray();
		LoginImpl objlogin = new LoginImpl();

		jsonArray = objlogin.getUserEmailDetails(email);
		return Response.status(200).entity(jsonArray).type(MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	@Path("/user/chklogin/otp")
	@Produces(MediaType.APPLICATION_JSON)
	public Response verifyUserOtp(@FormParam("email") String email, @FormParam("otp") String onetimepassword,
			@FormParam("Bid") String bid) {

		JSONArray jsonArray = new JSONArray();
		LoginImpl objlogin = new LoginImpl();
		String token = "";
		Response responsemessage = null;
		try {
			String enc_email = "";
			String deviceId = "AR12EBD6RT57WDU9";
			Integer otp = Integer.parseInt(onetimepassword);
			javax.servlet.http.HttpSession session = httpRequest.getSession(true);
			if (!email.equals("") && !otp.equals("")) {

					jsonArray = objlogin.getOtpUserVerifyEmail(email, "", otp);
					//session.setAttribute("id", bid);
					session.setAttribute("type", "internal");
					
					JSONObject jsonObj = (JSONObject) jsonArray.get(0);
					if (jsonObj.get("message").toString().equalsIgnoreCase("Valid Otp")) {
						session.setAttribute("email", email);
					//	chkemail(email);
						enc_email = GenrateToken.encrypt(email);
						token = GenrateToken.issueToken(email, deviceId);
						return Response.status(200).entity(jsonArray).header("Authorization", token)
								.header("Authorizationkey", enc_email).type(MediaType.APPLICATION_JSON).build();
						
					}else {
						return Response.status(200).entity(jsonArray).header("Authorization", token)
								.header("Authorizationkey", enc_email).type(MediaType.APPLICATION_JSON).build();
						
					}
			}

		} catch (Exception e) {
			log.error("verifyOtp() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
		return responsemessage;
	}

	@POST
	@Path("/chklogin/otp")
	@Produces(MediaType.APPLICATION_JSON)
	public Response verifyOtp(@FormParam("email") String email, @FormParam("otp") String onetimepassword,
			@FormParam("Bid") String bid) {

		JSONArray jsonArray = new JSONArray();
		LoginImpl objlogin = new LoginImpl();
		String token = "";
		Response responsemessage = null;
		try {
			String enc_pan = "";
			String deviceId = "AR12EBD6RT57WDU9";
			Integer otp = Integer.parseInt(onetimepassword);
			javax.servlet.http.HttpSession session = httpRequest.getSession(true);
			String pan = objlogin.getPanDetails(bid);
			if (!email.equals("") && !otp.equals("") && !pan.equals("")) {
			//if (!email.equals("") && !otp.equals("")) {

				if (pan != null && !pan.equals("")) {
				//if (email != null && !email.equals("")) {
					jsonArray = objlogin.getOtpVerifyEmail(email, pan, otp);
					session.setAttribute("id", bid);
					session.setAttribute("pan", pan);
					session.setAttribute("type", "vendor");

					JSONObject jsonObj = (JSONObject) jsonArray.get(0);
					if (jsonObj.get("message").toString().equalsIgnoreCase("Valid Otp")) {

						session.setAttribute("vemail", email);

						/**
						 * Setting company code in session attribute
						 */
						String compCode = "";
						String compName = "";
						Properties prop = new Properties();
						InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
						prop.load(input);
						String server = prop.getProperty("servertype");
						SimpoImpl simp = new SimpoImpl();

//						JSONArray jArray = simp.getPartnerGroupCompanyDetails(bid, server);
						JSONArray jArray = simp.getPartnerGroupCompanyList(email, server, bid);
						JSONObject jsonobject = (JSONObject) jArray.get(0);
						if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
							ArrayList<HashMap<String, String>> pgList = new ArrayList<HashMap<String, String>>();
							log.info("company code success ");

							if (jsonobject.get("partnerGroups") != null) {
								pgList = (ArrayList<HashMap<String, String>>) jsonobject.get("partnerGroups");
								log.info("company code SIZE " + pgList.size());

								if (pgList != null && !pgList.isEmpty()) {
									compCode = pgList.get(0).get("COMPANYCODE");
									compName = pgList.get(0).get("COMPANYNAME");
									log.info("company code  " + compCode + " | company name " + compName);

									session.setAttribute("COMPANYCODE", compCode);
								}
							} else {
								log.info("company code null");
								session.setAttribute("COMPANYCODE", compCode);

								JSONObject responsejson = new JSONObject();
								jsonArray = new JSONArray();
								responsejson.put("validation", "Company code not found");
								responsejson.put("message", "Fail");
								jsonArray.add(responsejson);
							}

//							JSONObject responsejson = new JSONObject();
							jsonObj.put("COMPANYCODE", compCode);
							jsonObj.put("COMPANYNAME", compName);
//							jsonArray.add(responsejson);
						}
						//log.info("Setting up vendor's email id and company code : " + email + " | " + compCode);

						

					}

					if (pan != null && !pan.equals("")) {
					//if (email != null && !email.equals("")) {	
						enc_pan = GenrateToken.encrypt(pan);
						token = GenrateToken.issueToken(pan, deviceId);
						//enc_pan = GenrateToken.encrypt(email);
						//token = GenrateToken.issueToken(email, deviceId);
					}
					return Response.status(200).entity(jsonArray).header("Authorization", token)
							.header("Authorizationkey", enc_pan).type(MediaType.APPLICATION_JSON).build();
				}
			}

		} catch (Exception e) {
			log.error("verifyOtp() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
		return responsemessage;
	}

	@POST
	@Path("/pOSignonhistory")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOSignonhistory(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("companyCode") String companyCode) {

		JSONArray jsonArray = new JSONArray();
		LoginImpl objlogin = new LoginImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null && companyCode != null && !"".equalsIgnoreCase(companyCode)) {
				session.setAttribute("id", bid);
				session.setAttribute("COMPANYCODE", companyCode);
				jsonArray = objlogin.getPOSignonhistory(bid);
				String token = GenrateToken.issueToken(pan, "1203");

				JSONObject jsonObj = (JSONObject) jsonArray.get(0);
				/**
				 * Setting company code in session attribute
				 */
				String compCode = "";
				String compName = "";
				Properties prop = new Properties();
				InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
				prop.load(input);
				String server = prop.getProperty("servertype");
				SimpoImpl simp = new SimpoImpl();

				JSONArray jArray = simp.getPartnerGroupCompanyDetails(bid, server);

				JSONObject jsonobject = (JSONObject) jArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					ArrayList<HashMap<String, String>> pgList = new ArrayList<HashMap<String, String>>();
					log.info("company code success ");

					if (jsonobject.get("partnerGroups") != null) {
						pgList = (ArrayList<HashMap<String, String>>) jsonobject.get("partnerGroups");
						log.info("company code SIZE " + pgList.size());

						if (pgList != null && !pgList.isEmpty()) {
							compCode = pgList.get(0).get("COMPANYCODE");
							compName = pgList.get(0).get("COMPANYNAME");
							log.info("company code  " + compCode + " | company name " + compName);

							session.setAttribute("COMPANYCODE", compCode);
						}
					} else {
						log.info("company code null");
						session.setAttribute("COMPANYCODE", compCode);
					}

					jsonObj.put("COMPANYCODE", compCode);
					jsonObj.put("COMPANYNAME", compName);
				}
				log.info("Setting up company code : " + compCode);
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOSignonhistory() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/chkemail")
	@Produces(MediaType.APPLICATION_JSON)
	public Response chkemail(@FormParam("email") String email) {

		JSONArray jsonArray = new JSONArray();
		LoginImpl objlogin = new LoginImpl();

		try {
			String enc_email = "";
			String token = "";
			String deviceId = "AR12EBD6RT57WDU9";
			javax.servlet.http.HttpSession session = httpRequest.getSession();
			if (!email.equals("")) {
				session.setAttribute("email", email);
				session.setAttribute("type", "internal");
				jsonArray = objlogin.getChecktheStoreKepeer(email, session);
				enc_email = GenrateToken.encrypt(email);
				token = GenrateToken.issueToken(email, deviceId);
				enc_email = GenrateToken.encrypt(email);

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", enc_email).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", enc_email).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("chkemail() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@GET
	@Path("/getInvoiceSummeryFunctionWise")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceSummeryFunctionWise(@QueryParam("deptcode") String deptcode,
			@QueryParam("fromDate") String fromDate, @QueryParam("toDate") String toDate) {

		JSONArray jsonArray = new JSONArray();
		JSONObject responsejson = new JSONObject();
		DashboardImpl objPO = new DashboardImpl();

		try {
			String mode = "";
			String email = "";
			log.info("getInvoiceSummeryFunctionWise API called for userid: " + email + " mode : " + mode);

			jsonArray = objPO.getInvoiceSummeryFunctionWise_Chart(deptcode, fromDate, toDate);
			String token = GenrateToken.issueToken(email, "1203");

			return Response.status(200).entity(jsonArray).header("Authorization", token)
					.header("Authorizationkey", null).type(MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			log.error("getInvoiceSummeryFunctionWise() :", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}


}
