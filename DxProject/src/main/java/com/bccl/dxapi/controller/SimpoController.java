package com.bccl.dxapi.controller;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.bccl.dxapi.apiimpl.InternalportalImpl;
import com.bccl.dxapi.apiimpl.InvoiceDetailsImpl;
import com.bccl.dxapi.apiimpl.POImpl;
import com.bccl.dxapi.apiimpl.SimpoImpl;
import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.bean.EndUserReturn;
import com.bccl.dxapi.bean.Invoicesubmission;
import com.bccl.dxapi.security.GenrateToken;

@Path("/simpodetails")
public class SimpoController {
	JSONArray jsonArray = null;
	JSONObject responsejson = null;
	SimpoImpl objPO = null;
	String token = "";

	@Context
	private HttpServletRequest httpRequest;
	@Context
	public javax.servlet.http.HttpServletResponse response;

	static Logger log = Logger.getLogger(SimpoController.class.getName());

	@POST
	@Path("/simPoEventDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSimPoEventDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("po_num_list") List<String> po_num_list) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				String bid = (String) session.getAttribute("id");
				jsonArray = objPO.getSimPoDetailEvent(po_num_list, bid);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getSimPoEventDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/createcustomdeliveryitemsforsimpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createcustomdeliveryitemsforsimpo(@CookieParam("Authorizationkey") String authkey,
			@FormParam("items") List<String> items) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.createcustomdeliveryitemsforsimpo(items, id);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("createcustomdeliveryitemsforsimpo() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/createcustomdeliveryitemsforsimpowithdc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createcustomdeliveryitemsforsimpowithdc(@CookieParam("Authorizationkey") String authkey,
			@FormParam("items") List<String> items) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.createcustomdeliveryitemsforsimpoWithDC(items, id);

				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("createcustomdeliveryitemsforsimpo() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	
	@POST
	@Path("/simpoSubmitInvoice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSimpoSubmitInvoice(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<Invoicesubmission> persons) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
		
			
			String vendorEmail=(String) session.getAttribute("vemail");
			
			
			if (session.getAttribute("id") != null) {
				System.out.println("Session ID not null");
				
				log.info("Vendor Email ID "+vendorEmail+" ID : "+id);

//				jsonArray = objPO.insertSimpoInvoice(persons,vendorEmail);
				jsonArray = objPO.insertSimpoInvoice(persons, id, vendorEmail);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getSimpoSubmitInvoice() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/deleteemptydeliveriessimpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteemptydeliveriessimpo(@CookieParam("Authorizationkey") String authkey,
			@FormParam("items") List<String> items) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.deleteemptydeliveries(items);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("deleteemptydeliveriessimpo() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/simpoInvoiceLineItemDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSimpoInvoiceLineItemDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num_list") List<String> po_num_list) {
		jsonArray = new JSONArray();
		objPO = new SimpoImpl();

		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = objPO.getSimpoInvoiceLineItemDetails(invoice, po_num_list);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getSimpoInvoiceLineItemDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/simpoTrackInvoiceLineItemDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSimpoTrackInvoiceLineItemDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num_list") List<String> po_num_list) {
		jsonArray = new JSONArray();
		objPO = new SimpoImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getSimpoInvoiceLineItemDetails(invoice, po_num_list);
				String token = GenrateToken.issueToken(id, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getSimpoTrackInvoiceLineItemDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/simpoManagerCountDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSimpoManagerCountDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num_list") List<String> po_num_list) {

		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = objPO.getSimpoManagerCountDetails(invoice, po_num_list);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getSimpoManagerCountDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getMultipleManagerListForSimpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getaddedMultipleManagerListForSimpo(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num_list") List<String> po_num_list) {

		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = objPO.getaddedMultipleManagerListForSimpo(invoice, po_num_list);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getaddedMultipleManagerListForSimpo() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/simPoProcessedPos")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSimPoProcessedPos(@CookieParam("Authorizationkey") String authkey,
			@FormParam("plantCode") String plantCode) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("BID | COMPANY CODE : "+ id+" | "+companyCode);

			if (session.getAttribute("id") != null&&companyCode!=null) {
				String bid = (String) session.getAttribute("id");
				jsonArray = objPO.getSimPoProcessedPos(bid, plantCode,companyCode);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getSimPoProcessedPos() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getorderhavingdcnforsimpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getorderhavingdcnforsimpo(@CookieParam("Authorizationkey") String authkey,
			@FormParam("items") List<String> items) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getorderhavingdcnforsimpo(id, items);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getorderhavingdcnforsimpo() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getsimpoinvoicebasedonemailid")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getsimpoinvoicebasedonemailid(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String EmailId, @FormParam("page") String nPage, @FormParam("status") String status,
			@FormParam("invoicenumber") String invno, @FormParam("ponumber") String pono,
			@FormParam("fromdate") String fdate, @FormParam("todate") String tdate, @FormParam("plant") String plant,
			@FormParam("vendor") String vendor) {

		jsonArray = new JSONArray();
		objPO = new SimpoImpl();

		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = objPO.getSimpoinvoicebasedonemailid(EmailId, session, Integer.parseInt(nPage), status, pono,
						invno, fdate, tdate, plant, vendor);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getsimpoinvoicebasedonemailid() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/SimpoInvoiceApprovalManagerStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getManagerApprvalStatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("INVOICENUMBER") String invoiceNumber, @FormParam("PONUMBER") String po_num,
			@FormParam("managerstatus") String managerstatus, @FormParam("managerId") String managerId,
			@FormParam("stage") String stage, @FormParam("INVOICEDATE") String invoiceDate,
			@FormParam("Bid") String bid) {

		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {
				jsonArray = objPO.getManagerApprvalStatusall(invoiceNumber, po_num, managerstatus, managerId, stage,
						invoiceDate, bid);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getManagerApprvalStatus() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/invoiceData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceDetails(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("page") String invoicepageno, @FormParam("status") String status,
			@FormParam("invoicenumber") String invno, @FormParam("ponumber") String pono,
			@FormParam("fromdate") String fdate, @FormParam("todate") String tdate, @FormParam("plant") String plant) {

		jsonArray = new JSONArray();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("BID | COMPANY CODE : "+ id+" | "+companyCode);

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getInvoiceDetails(bid, Integer.parseInt(invoicepageno), status, invno, pono, fdate,
						tdate, plant,companyCode);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceDetails() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/internalponinvoicesummery")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRequsitionerBuyerPayerPoninvoiceSummery(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String emailid, @FormParam("page") String invoicepageno,
			@FormParam("status") String status, @FormParam("invoicenumber") String invno,
			@FormParam("ponumber") String pono, @FormParam("fromdate") String fdate, @FormParam("todate") String tdate,
			@FormParam("plant") String plant, @FormParam("vendor") String vendor) {
		objPO = new SimpoImpl();
		InternalportalImpl internalImp = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String mode = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");
			
			log.info("mode :"+mode);
			
			if (session.getAttribute("email") != null) {

				if (mode.equalsIgnoreCase("buyer")) {
					jsonArray = objPO.getBuyerPoninvoiceSummery(emailid, Integer.parseInt(invoicepageno), status, invno,
							pono, fdate, tdate, plant, vendor);
				} else if (mode.equalsIgnoreCase("enduser")) {
					jsonArray = objPO.getRequsitionerPoninvoiceSummery(emailid, Integer.parseInt(invoicepageno), status,
							invno, pono, fdate, tdate, plant, vendor);
				} else if (mode.equalsIgnoreCase("payer")) {
					jsonArray = objPO.getPayerPoninvoiceSummery(Integer.parseInt(invoicepageno), status, invno, pono,
							fdate, tdate, plant, vendor);
				} else if (mode.equalsIgnoreCase("internalbcclportal")) {
					jsonArray = internalImp.getInternalPoninvoiceSummery(emailid);
				}
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getRequsitionerBuyerPayerPoninvoiceSummery() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/updateacceptedquantity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateacceptedquantity(@CookieParam("Authorizationkey") String authkey,
			@FormParam("updateaccepted") List<String> values) {
		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				Properties prop = new Properties();
				InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
				prop.load(input);
				String server = prop.getProperty("servertype");

				if ("dev".equalsIgnoreCase(server)) {
					jsonArray = objPO.updateacceptedquantity(values, email);
				} else if ("uat".equalsIgnoreCase(server)) {
					jsonArray = objPO.getAcceptQtynGRN(values, email);
				}

				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			}

		} catch (Exception e) {
			log.error("updateacceptedquantity() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/updateacceptedquantitywithoutgrn")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateacceptedquantitywithoutgrn(@CookieParam("Authorizationkey") String authkey,
			@FormParam("updateacceptedwithoutgrn") List<String> values) {
		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				Properties prop = new Properties();
				InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
				prop.load(input);
				String server = prop.getProperty("servertype");

				if ("dev".equalsIgnoreCase(server)) {
					jsonArray = objPO.updateacceptedquantitywithoutgrn(values, email);
				} else if ("uat".equalsIgnoreCase(server)) {
					jsonArray = objPO.updateacceptedquantitywithoutgrnforprod(values, email);
				}

				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateacceptedquantitywithoutgrn() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getVendorReturn")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVendorReturn(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {

		jsonArray = new JSONArray();
		objPO = new SimpoImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getVendorReturn(invoice, po_num);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getVendorReturn() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/updateacceptedservicequantitywithoutgrnforsimpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateacceptedservicequantitywithoutgrn(@CookieParam("Authorizationkey") String authkey,
			@FormParam("updateserviceaccepted") List<String> values) {
		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				jsonArray = objPO.updateacceptedservicequantitywithoutgrn(values, email);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateacceptedservicequantitywithoutgrn() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/updateacceptedservicequantity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateacceptedservicequantity(@CookieParam("Authorizationkey") String authkey,
			@FormParam("updateaccepted") List<String> values) {
		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				Properties prop = new Properties();
				InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
				prop.load(input);
				String server = prop.getProperty("servertype");
				if ("dev".equalsIgnoreCase(server)) {
					jsonArray = objPO.updateacceptedservicequantity(values, email);
				} else if ("uat".equalsIgnoreCase(server)) {
					jsonArray = objPO.getAcceptQtynServiceGRN(values, email);
				}

				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateacceptedservicequantity() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getEnderUserReturnforsimpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEnderUserReturn(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<EndUserReturn> enduserList) {

		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = objPO.getEnderUserReturn(enduserList, email);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getEnderUserReturn() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/downloadapprovalinvoicelist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadapprovalinvoicelist(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String EmailId, @FormParam("status") String status, @FormParam("inv") String invno,
			@FormParam("po") String pono, @FormParam("fd") String fdate, @FormParam("td") String tdate) {

		objPO = new SimpoImpl();
		InternalportalImpl internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				/*
				List<String> invoicedata = new ArrayList<>();
				List<String> podata = new ArrayList<>();
				jsonArray = objPO.getapprovalinvoicelistfordownload(EmailId, status, invno, pono, fdate, tdate,
						session);

				JSONObject jsonobject = (JSONObject) jsonArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					invoicedata = (List<String>) jsonobject.get("invoicedata");
					podata = (List<String>) jsonobject.get("podata");
				}
				//jsonArray = internalport.downloadapprovalinvoicelist(invoicedata, podata, email, "APPROVAL");
				*/
				//emailid, status, invno, pono, fdate, tdate,plant,vendor, "APPROVAL",mode
				jsonArray = internalport.downloadAppInvoicelist(EmailId, status, invno, pono, fdate, tdate,"NA","NA", "APPROVAL",mode);
				//downloadAppInvoicelist
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("downloadapprovalinvoicelist() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/updatebasepo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateBasePO(@CookieParam("Authorizationkey") String authkey) {
		objPO = new SimpoImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			jsonArray = objPO.updateBasePO();
			String token = GenrateToken.issueToken(email, "1203");
			return Response.status(200).entity(jsonArray).header("Authorization", token)
					.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			log.error("updateBasePO() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/updatereopenedinvoice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReopenedInvoice(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<Invoicesubmission> persons) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.updateReopenedInvoice(persons);

				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateReopenedInvoice() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getgrnbasedonpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getgrnbasedonpo(@CookieParam("Authorizationkey") String authkey,@FormParam("ponumber") String ponumber) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getgrnbasedonpo(id, ponumber);

				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getgrnbasedonpo() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getgrnbasedoninvoiceandpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getgrnbasedoninvoiceandpo(@CookieParam("Authorizationkey") String authkey, @FormParam("dcnumber") List<String> dcnumber, @FormParam("basepo") String basepo) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getgrnbasedoninvoiceandpo(id,dcnumber,basepo);

				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getgrnbasedoninvoiceandpo() :", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	
	
	@POST
	@Path("/getInvalidInvoiceCount")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvalidInvoiceCount(@CookieParam("Authorizationkey") String authkey) {

		jsonArray = new JSONArray();		
		objPO = new SimpoImpl();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {

			String bid = "";
			String pan = "";
			String vendorEmail = "";
			
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			bid = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			vendorEmail = (String) session.getAttribute("vemail");
			
			log.debug("getInvalidInvoiceCount : "+bid +" Email "+vendorEmail);
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("COMPANY CODE : "+companyCode);
			
			if (session.getAttribute("id") != null) {
				
				con = DBConnection.getConnection();

				jsonArray = objPO.getInvoiceDetailsCountAsPerStatus(bid,0,"Invalid Invoices","NA","NA","NA","NA","NA",con,ps,rs,companyCode);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvalidInvoiceCount() ", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
			
		}
		finally {
			DBConnection.closeConnection(rs, ps, con);
		}

	}
	
	
	@POST
	@Path("/getInvalidInvoiceCountForInternalUser")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvalidInvoiceCountForInternalUser(@CookieParam("Authorizationkey") String authkey) {
		
		objPO = new SimpoImpl();
		InternalportalImpl internalImp=new InternalportalImpl();
		jsonArray = new JSONArray();
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {

			String mode = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");
			
			log.info("getInvalidInvoiceCountForInternalUser API called for userid: " + email + " mode : "+mode);
			
			if (session.getAttribute("email") != null) {
				
				
				con = DBConnection.getConnection();
				
				jsonArray = objPO.getInternalPonInvoiceSummeryCountsAsPerStatus(email, 0, "Invalid Invoices", "NA", "NA", "NA", "NA", "NA","NA", mode, con, ps, rs);
				
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvalidInvoiceCountForInternalUser() ", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}finally {
			DBConnection.closeConnection(rs, ps, con);
		}
	}
	
	
	@POST
	@Path("/getInvalidInvoiceDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvalidInvoiceDetails(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("page") String invoicepageno, @FormParam("status") String status,
			@FormParam("invoicenumber") String invno, @FormParam("ponumber") String pono,
			@FormParam("fromdate") String fdate, @FormParam("todate") String tdate, @FormParam("plant") String plant) {

		jsonArray = new JSONArray();
		objPO = new SimpoImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("COMPANY CODE : "+companyCode);

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getInvalidInvoiceDetails(bid, Integer.parseInt(invoicepageno), status, invno, pono, fdate,tdate, plant,companyCode);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceDetails() : ", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	
	@POST
	@Path("/getInvalidInvoiceDetailsForInternalUser")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvalidInvoiceDetailsForInternalUser(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String emailid, @FormParam("page") String invoicepageno,
			@FormParam("status") String status, @FormParam("invoicenumber") String invno,
			@FormParam("ponumber") String pono, @FormParam("fromdate") String fdate, @FormParam("todate") String tdate,
			@FormParam("plant") String plant, @FormParam("vendor") String vendor) {
		objPO = new SimpoImpl();
		InternalportalImpl internalImp = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String mode = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");
			
			log.info("getInvalidInvoiceDetailsForInternalUser MODE : "+mode+" EMAIL : "+email+" emailid : "+emailid);

			if (session.getAttribute("email") != null) {
				
				if(!mode.equalsIgnoreCase("internalbcclportal")) {
					jsonArray = objPO.getInvalidInvoiceDetailsForInternalUser(emailid, Integer.parseInt(invoicepageno), status, invno,
							pono, fdate, tdate, plant, vendor,mode);
				}else {
					jsonArray = internalImp.getInternalPoninvoiceSummery(emailid);
				}
				
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getRequsitionerBuyerPayerPoninvoiceSummery() : ", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	
	
	@POST
	@Path("/downloadinvalidinvoiceData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadinvoiceData(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("status") String status, @FormParam("po") String ponumber,
			@FormParam("inv") String invoicenumber, @FormParam("fd") String fromdate, @FormParam("td") String todate, @FormParam("plant") String plant) {
		
		jsonArray = new JSONArray();
		objPO = new SimpoImpl();
		InvoiceDetailsImpl objPO1 = new InvoiceDetailsImpl();
		
		JSONArray jsonArray1 = new JSONArray();
		
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("COMPANY CODE : "+companyCode);
	
			if (session.getAttribute("id") != null) {
				
				/*
				List<String> invoicedata = new ArrayList<>();
				List<String> podata = new ArrayList<>();
				
				ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
				
//				jsonArray = objPO.getdownloadInvoceidIdDetails(bid, status, ponumber, invoicenumber, fromdate, todate);
				
				jsonArray1 = objPO.getInvalidInvoiceDetails(bid, 0, status, invoicenumber, ponumber, fromdate,todate, plant,companyCode);
				
				
				JSONObject jsonobject = (JSONObject) jsonArray1.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Sucessinvlist")) {
					log.debug("Inside success 1");
					invoiceList = (ArrayList<HashMap<String, String>>) jsonobject.get("invoiceData");
					
					log.debug("Inside success 1 size "+invoiceList.size());
					
					if(invoiceList!=null&&!invoiceList.isEmpty()) {
						for(int i=0;i<invoiceList.size();i++) {
							invoicedata.add(invoiceList.get(i).get("INVOICENUMBER"));
							podata.add(invoiceList.get(i).get("PO_NUMBER"));
						}
					}
					
					log.debug("Inside success 1 size "+invoicedata.size() +" and "+podata.size());
				}
				
				if (jsonobject.get("message1").toString().equalsIgnoreCase("Sucessinvlist1")) {
					log.debug("Inside success 2");
					invoiceList = (ArrayList<HashMap<String, String>>) jsonobject.get("invoiceDataWOPO");
					
					log.debug("Inside success 2 size "+invoiceList.size());
					
					if(invoiceList!=null&&!invoiceList.isEmpty()) {
						for(int i=0;i<invoiceList.size();i++) {
							invoicedata.add(invoiceList.get(i).get("INVOICENUMBER"));
							podata.add(invoiceList.get(i).get("PO_NUMBER"));
						}
					}
					
					log.debug("Inside success 2 size "+invoicedata.size() +" and "+podata.size());
				}
				*/
				//jsonArray = objPO1.downloadInvoceidIdDetails(bid, invoicedata, podata);
				jsonArray = objPO1.downloadInvoceidIdDetails(bid, status, ponumber, invoicenumber, fromdate, todate, plant, companyCode);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			}

		} catch (Exception e) {
			log.error("downloadinvoiceData() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	
	@POST
	@Path("/downloadinternalinvalidinvoiceData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadinvoiceData(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String emailid, @FormParam("page") String invoicepageno,
			@FormParam("status") String status, @FormParam("invoicenumber") String invno,
			@FormParam("ponumber") String pono, @FormParam("fromdate") String fdate, @FormParam("todate") String tdate,
			@FormParam("plant") String plant, @FormParam("vendor") String vendor) {
		objPO = new SimpoImpl();
		InternalportalImpl internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		JSONArray jsonArray1 = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				
				/*
				List<String> invoicedata = new ArrayList<>();
				List<String> podata = new ArrayList<>();
				
				ArrayList<HashMap<String, String>> invoiceList = new ArrayList<HashMap<String, String>>();
				
				jsonArray1 = objPO.getInvalidInvoiceDetailsForInternalUser(emailid, 0, status, invno,
						pono, fdate, tdate, plant, vendor,mode);
				
				log.info("Invalid invoice download ");
				
				JSONObject jsonobject = (JSONObject) jsonArray1.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					log.info("Invalid invoice download success ");
					invoiceList = (ArrayList<HashMap<String, String>>) jsonobject.get("invoiceData");
					log.info("Invalid invoice download success "+invoiceList.size());
					if(invoiceList!=null&&!invoiceList.isEmpty()) {
						for(int i=0;i<invoiceList.size();i++) {
							invoicedata.add(invoiceList.get(i).get("INVOICENUMBER"));
							podata.add(invoiceList.get(i).get("PONUMBER"));
						}
					}
					
					log.info("Invalid invoice download success "+invoicedata.size() +" & "+podata.size());
				}
				*/
			//	jsonArray = internalport.downloadapprovalinvoicelist(invoicedata, podata, email, "");
				jsonArray = internalport.downloadapprovalinvoicelist(email, status, invno, pono,
						fdate, tdate, plant, vendor, "invalid", mode); 
				
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("downloadinvoiceData() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	

	@POST
	@Path("/getPartnerGroupCompanyDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPartnerGroupCompanyDetails(@CookieParam("Authorizationkey") String authkey,@FormParam("email") String emailId) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			pan =  (String) session.getAttribute("pan");;
					
			Properties prop = new Properties();
			InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
			prop.load(input);
			String server = prop.getProperty("servertype");


			if (session.getAttribute("id") != null && emailId != null && !"".equalsIgnoreCase(emailId)) {
				
				String bid = (String) session.getAttribute("id");
				//jsonArray = objPO.getPartnerGroupCompanyDetails(emailId, server);
				jsonArray = objPO.getPartnerGroupCompanyList(emailId, server,null);				
				String token = GenrateToken.issueToken(pan, "1203");
				
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPartnerGroupCompanyDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	
	@POST
	@Path("/updatePartnerGroup")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOSignonhistory(@CookieParam("Authorizationkey") String authkey, @FormParam("companyCode") String companyCode) {

		JSONArray jsonArray = new JSONArray();
		responsejson = new JSONObject();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null&&companyCode!=null) {
				
				session.setAttribute("COMPANYCODE", companyCode);
				responsejson.put("message", "Success");
				responsejson.put("COMPANYCODE", companyCode);
				jsonArray.add(responsejson);

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
/*	
	@POST
	@Path("/getGSTBaseOnCompanyCode")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGSTBaseOnCompanyCode(@CookieParam("Authorizationkey") String authkey,@FormParam("vendorEmailid") String vendorEmailId,
			@FormParam("companyCode") String companyCode) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new SimpoImpl();
		try {

			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			pan =  (String) session.getAttribute("pan");;
					
			Properties prop = new Properties();
			InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
			prop.load(input);
			String server = prop.getProperty("servertype");


			if (session.getAttribute("id") != null) {
				
				String bid = (String) session.getAttribute("id");
				jsonArray = objPO.getGSTBaseOnCompanyCode(vendorEmailId, companyCode);
				
				String token = GenrateToken.issueToken(pan, "1203");
				
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPartnerGroupCompanyDetails() :", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	*/

	@POST
	@Path("/viewInvoiceTracker")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getviewInvoiceTracker(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {
		objPO = new SimpoImpl();		
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = objPO.getPendingInvoiceTracker(po_num,invoice);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getviewInvoiceTracker() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/faqDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response faqDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("userType") String userType) {
		POImpl objPO = new POImpl();		
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = objPO.faqDetails(userType);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getviewInvoiceTracker() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	
	
}
