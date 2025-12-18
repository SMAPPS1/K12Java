package com.bccl.dxapi.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
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
import com.bccl.dxapi.apiimpl.SimpoImpl;
import com.bccl.dxapi.security.GenrateToken;

@Path("/invoiceDetails")
public class InvoiceDetailsController {

	JSONArray jsonArray = null;
	InvoiceDetailsImpl objPO = null;
	InternalportalImpl internal = null;
	Response responsemessage = null;
	String token = "";

	@Context
	private HttpServletRequest httpRequest;
	@Context
	public javax.servlet.http.HttpServletResponse response;

	static Logger log = Logger.getLogger(InvoiceDetailsController.class.getName());

	@POST
	@Path("/invoiceData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceDetails(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("page") String invoicepageno, @FormParam("status") String status,
			@FormParam("invoicenumber") String invno, @FormParam("ponumber") String pono,
			@FormParam("fromdate") String fdate, @FormParam("todate") String tdate, @FormParam("plant") String plant) {

		jsonArray = new JSONArray();
		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
	
			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getInvoiceDetails(bid, Integer.parseInt(invoicepageno), status, invno, pono, fdate,
						tdate, plant);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/BidAndInvoiceIdData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response BidAndInvoiceIdData(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("Invoiceid") String Invoiceid) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getBussiIdInvoceidIdDetails(bid, Invoiceid);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("BidAndInvoiceIdData() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/invoiceSubmitQuery")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceSubmitQuery(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("po_num") String po_num, @FormParam("emailid") String emailid,
			@FormParam("RequsitionerEmail") String RequsitionerEmail, @FormParam("subject") String subject,
			@FormParam("message") String message, @FormParam("status") String status) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
	
			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getInvoiceSubmitQuery(po_num, emailid, RequsitionerEmail, message, bid, subject,
						status);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceSubmitQuery() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/invoiceReadStatusN")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPoDetailsStatusN(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
	
			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getPoDetailsStatusN(bid);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getPoDetailsStatusN() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
/*
	@POST
	@Path("/downloadinvoiceData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadinvoiceData(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("status") String status, @FormParam("po") String ponumber,
			@FormParam("inv") String invoicenumber, @FormParam("fd") String fromdate, @FormParam("td") String todate, @FormParam("plant") String plant) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("COMPANY CODE : "+companyCode);
	
			if (session.getAttribute("id") != null && companyCode!=null) {
				List<String> invoicedata = new ArrayList<>();
				List<String> podata = new ArrayList<>();
				jsonArray = objPO.getdownloadInvoceidIdDetails(bid, status, ponumber, invoicenumber, fromdate, todate,plant,companyCode);
				JSONObject jsonobject = (JSONObject) jsonArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					invoicedata = (List<String>) jsonobject.get("invoicedata");
					podata = (List<String>) jsonobject.get("podata");
				}

				jsonArray = objPO.downloadInvoceidIdDetails(bid, invoicedata, podata);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
				downloadInvoceidIdDetails(String bid, String status, String ponumber, String invoicenumber,
						String fromdate, String todate, String plant, String companyCode)
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			}

		} catch (Exception e) {
			log.error("downloadinvoiceData() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
*/
	@POST
	@Path("/invoiceChatQuery")
	@Produces(MediaType.APPLICATION_JSON)
	public Response setInvoiceChatQuery(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("invoice_num") String invoice_num, @FormParam("po_num") String po_num,
			@FormParam("emailid") String emailid, @FormParam("topic") String topic,
			@FormParam("message") String message, @FormParam("subject") String subject) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";

			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			if (session.getAttribute("id") != null) {
				pan = (String) session.getAttribute("pan");
				jsonArray = objPO.setChatStatus(bid, invoice_num, po_num, emailid, topic, message, subject);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("setInvoiceChatQuery() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/invoiceReadChatQuery")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceReadChatStatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("bid") String bid, @FormParam("emailid") String emailid) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";

			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			if (session.getAttribute("id") != null) {
				pan = (String) session.getAttribute("pan");
				jsonArray = objPO.getInvoiceReadStatus(bid, emailid);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceReadChatStatus() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/invoiceChatUpdate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response setChatStatusUpdate(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("emailid") String emailid, @FormParam("invoice_num") String invoice_num,
			@FormParam("po_num") String po_num) {

		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";

			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			if (session.getAttribute("id") != null) {
				pan = (String) session.getAttribute("pan");

				jsonArray = objPO.setChatStatusUpdate(bid, emailid, invoice_num, po_num);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("setChatStatusUpdate() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/invoiceLineItemDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceLineItemDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getInvoiceLineItemDetails(invoice, po_num);
				String token = GenrateToken.issueToken(id, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceLineItemDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/invoiceQueryDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSubmitQueryDetails(@CookieParam("Authorizationkey") String authkey, @FormParam("Bid") String bid,
			@FormParam("po_num") String po_num, @FormParam("invoicenumber") String invoiceno) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getinvoiceQueryDetails(bid, po_num, invoiceno);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getSubmitQueryDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/IRNDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGRNDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("actualfilename") String actualfilename, @FormParam("savedfilename") String savedfilename) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getIRNDetails(actualfilename, savedfilename);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getGRNDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/trackinvoicestatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response trackinvoicestatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoicenumber") String invoicenumber, @FormParam("ponumber") String ponumber,
			@FormParam("bussinesspartneroid") String bussinesspartneroid) {

		jsonArray = new JSONArray();
		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			if (session.getAttribute("id") != null) {

				jsonArray = objPO.gettrackinvoicestatus(invoicenumber, ponumber, bussinesspartneroid);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("trackinvoicestatus() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getcreditadvice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getcreditadvice(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {
		jsonArray = new JSONArray();

		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = internal.getcreditadvice(invoice, po_num);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getcreditadvice() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/addcreditNote")
	@Produces(MediaType.APPLICATION_JSON)
	public Response addcreditNote(@CookieParam("Authorizationkey") String authkey, @FormParam("invoice") String invoice,
			@FormParam("po_num") String po_num, @FormParam("bid") String bid,
			@FormParam("creditadviceno") String creditadviceno, @FormParam("amount") String amount,
			@FormParam("tax") String tax, @FormParam("totalamt") String totalamt,
			@FormParam("actualfilename") String actualfilename, @FormParam("savedfilename") String savedfilename) {

		jsonArray = new JSONArray();
		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = internal.addcreditNote(invoice, po_num, bid, creditadviceno, amount, tax, totalamt,
						actualfilename, savedfilename);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("addcreditNote() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getcreditNoteDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getcreditNoteDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num,
			@FormParam("creditadviceno") String creditadviceno) {
		jsonArray = new JSONArray();
		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = internal.getcreditNoteDetails(invoice, po_num, creditadviceno);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getcreditNoteDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getVendorReturn")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVendorReturn(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {

		jsonArray = new JSONArray();
		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

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
			log.error("getVendorReturn() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/gethistoricinvoice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response gethistoricinvoice(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid) {
		jsonArray = new JSONArray();
		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.gethistoricinvoice(bid);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("gethistoricinvoice() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getplantcodeordescc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getplantcodeordescc(@CookieParam("Authorizationkey") String authkey,
			@FormParam("codeordesc") String text) {
		jsonArray = new JSONArray();
		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = internal.getplantcodeordescc(text);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getplantcodeordescc() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/downloadinvoiceData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadinvoiceData(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("status") String status, @FormParam("po") String ponumber,
			@FormParam("inv") String invoicenumber, @FormParam("fd") String fromdate, 
			@FormParam("td") String todate, @FormParam("plant") String plant) {
	
		jsonArray = new JSONArray();
		objPO = new InvoiceDetailsImpl();
		internal = new InternalportalImpl();

		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("COMPANY CODE : "+companyCode);
	
			if (session.getAttribute("id") != null && companyCode!=null) {

				jsonArray = objPO.downloadInvoceidIdDetails(bid, status, ponumber, invoicenumber,
						fromdate, todate, plant, companyCode);
				
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
	@Path("/viewInvoiceTracker")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getviewInvoiceTracker(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoicenumber") String invoicenumber, @FormParam("ponumber") String ponumber,
			@FormParam("bussinesspartneroid") String bussinesspartneroid) {
		objPO = new InvoiceDetailsImpl();		
		jsonArray = new JSONArray();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);			
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getPendingInvoiceTracker(ponumber,invoicenumber,bussinesspartneroid);
				String token = GenrateToken.issueToken(pan, "1203");
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