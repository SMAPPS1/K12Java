package com.bccl.dxapi.controller;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
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
import com.bccl.dxapi.apiimpl.POImpl;
import com.bccl.dxapi.bean.AdvanceShippingNotification;
import com.bccl.dxapi.bean.Invoicesubmission;
import com.bccl.dxapi.bean.PoeventdetailsPojo;
import com.bccl.dxapi.security.GenrateToken;

@Path("/podetails")
public class POController {

	JSONArray jsonArray = null;
	JSONObject responsejson = null;
	POImpl objPO = null;
	String token = "";

	@Context
	private HttpServletRequest httpRequest;
	@Context
	public javax.servlet.http.HttpServletResponse response;

	static Logger log = Logger.getLogger(POController.class.getName());

	@POST
	@Path("/pOData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVendorDetails(@CookieParam("Authorizationkey") String authkey, @FormParam("Bid") String bid,
			@FormParam("status") String status, @FormParam("nPage") String nPage,
			@FormParam("ponumber") String ponumber, @FormParam("fromdateofduration") String fromdateofduration,
			@FormParam("todateofduration") String todateofduration, @FormParam("fromdateofpo") String fromdateofpo,
			@FormParam("todateofpo") String todateofpo, @FormParam("plant") String plant) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
		//	log.info("BID | COMPANY CODE : "+ id+" | "+companyCode);

			if (session.getAttribute("id") != null&&companyCode!=null) {
				jsonArray = objPO.getPODetails(bid, status, Integer.parseInt(nPage), ponumber, fromdateofduration,
						todateofduration, fromdateofpo, todateofpo, plant,companyCode);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getVendorDetails() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/pODatawopo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVendorDetailswopo(@CookieParam("Authorizationkey") String authkey, @FormParam("Bid") String bid,
			@FormParam("pageno") String pageno, @FormParam("status") String status,
			@FormParam("ponumber") String ponumber, @FormParam("fromdateofduration") String fromdateofduration,
			@FormParam("todateofduration") String todateofduration, @FormParam("fromdateofpo") String fromdateofpo,
			@FormParam("todateofpo") String todateofpo) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("BID | COMPANY CODE : "+ id+" | "+companyCode);

			if (session.getAttribute("id") != null&&companyCode!=null) {

				jsonArray = objPO.getPODetailswithoutpo(bid, Integer.parseInt(pageno), status, ponumber,
						fromdateofduration, todateofduration, fromdateofpo, todateofpo,companyCode);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getVendorDetailswopo() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/pOSelect")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOStatus(@CookieParam("Authorizationkey") String authkey, @FormParam("select") String select,
			@FormParam("remark") String remark, @FormParam("po_num") String po_num) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getPOStatus(id, select, remark, po_num);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOStatus() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/pOEventDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOEventDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("po_num") String po_num) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				String bid = (String) session.getAttribute("id");
				jsonArray = objPO.getPODetailEvent(po_num, bid);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOEventDetails() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getorderitems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getorderitems(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("po_num") String po_num, @FormParam("lineitemnumber") String lineitemnumber) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getorderitems(bid, po_num, lineitemnumber);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getorderitems() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/pOSubmitQuery")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOSubmitQuery(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("po_num") String po_num, @FormParam("emailid") String emailid,
			@FormParam("invoiceNumber") String invoiceNumber, @FormParam("subject") String subject,
			@FormParam("message") String message, @FormParam("status") String status) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getPOSubmitQuery(po_num, emailid, invoiceNumber, message, bid, subject, status);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOSubmitQuery() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/pOQueryDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSubmitQueryDetails(@CookieParam("Authorizationkey") String authkey, @FormParam("Bid") String bid,
			@FormParam("po_num") String po_num) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getSubmitQueryDetails(bid, po_num);
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
	@Path("/getpofile")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getpolistfilefordownload(@FormParam("mode") String mode,
			@CookieParam("Authorizationkey") String authkey, @FormParam("ponumber") String ponumber,
			@FormParam("fromdateofduration") String fromdateofduration,
			@FormParam("todateofduration") String todateofduration, @FormParam("fromdateofpo") String fromdateofpo,
			@FormParam("todateofpo") String todateofpo) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			
			String companyCode=(String) session.getAttribute("COMPANYCODE");
			log.info("BID:"+ id+" :COMPANY CODE: "+companyCode + " :mode: "+mode+ " :ponumber: "+ponumber+" :fromdateofduration : "+ fromdateofduration
					+ " :todateofduration: "+todateofduration + " :fromdateofpo: "+fromdateofpo+ " :todateofpo: "+todateofpo);
			
			if (session.getAttribute("id") != null) {
				String token = GenrateToken.issueToken(pan, "1203");
				jsonArray = objPO.getPODetails(id, mode, ponumber, fromdateofduration, todateofduration, fromdateofpo,
						todateofpo,companyCode);
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getpolistfilefordownload() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/pOReadStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPoReadStatus(@CookieParam("Authorizationkey") String authkey, @FormParam("Bid") String bid,
			@FormParam("emailid") String emailid) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getPOReadStatus(bid, emailid);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPoReadStatus() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/pOReadStatusUpdate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPoReadStatusUpdate(@CookieParam("Authorizationkey") String authkey, @FormParam("Bid") String bid,
			@FormParam("po_num") String po_num) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getPOReadStatusUpdate(bid, po_num);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPoReadStatusUpdate() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/pOGSTPincode")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOGSTPincode(@CookieParam("Authorizationkey") String authkey, @FormParam("email") String email) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getpOGSTPincode(email);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOGSTPincode() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	/*
	@POST
	@Path("/pOGST")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOGST(@CookieParam("Authorizationkey") String authkey, @FormParam("email") String email) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getpOGST(email);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOGST() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
*/
	
	@POST
	@Path("/pOGST")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOGST(@CookieParam("Authorizationkey") String authkey, @FormParam("email") String email,
			@FormParam("companyCode") String companyCode) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getpOGST(email,companyCode,id);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOGST() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	
	@POST
	@Path("/pOPincode")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOPincode(@CookieParam("Authorizationkey") String authkey, @FormParam("gst") String gst) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getpOPincode(gst, session);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOPincode() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/invoiceReadStatusN")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPoDetailsStatusN(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("Status") String Status) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getPoDetailsStatusN(bid, Status);
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

	@POST
	@Path("/POProfileupdate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPoDetailsProfileUpdate(@CookieParam("Authorizationkey") String authkey,
			@FormParam("Bid") String Bid) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getModifiedOndDetails(Bid);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getPoDetailsProfileUpdate() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/InvoiceProfileupdate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceDetailsProfileUpdate(@CookieParam("Authorizationkey") String authkey,
			@FormParam("Bid") String Bid) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getInvoiceModifiedAndDetailsForInvoice(Bid);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getInvoiceDetailsProfileUpdate() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/InvoiceProfileupdateNumber")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceNumberDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("Bid") String Bid) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getInvoiceModifiedAndDetailsForInvoice(Bid);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getInvoiceNumberDetails() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/pOSubmitInvoice1")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOSubmitInvoice1(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<Invoicesubmission> persons) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			String vendorEmail = (String) session.getAttribute("vemail");
			System.out.println("Vendor's Email Id : " + vendorEmail);

			if (session.getAttribute("id") != null) {

//				jsonArray = objPO.insertinvoice(persons, vendorEmail);
				
				jsonArray = objPO.insertinvoiceWithDC(persons, vendorEmail);
				
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOSubmitInvoice1() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/nonposubmitinvoice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response nonposubmitinvoice(@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
			@FormParam("irnNumber") String irnNumber, @FormParam("irnDate") String irnDate,
			@FormParam("invoiceNumber") String invoiceNumber, @FormParam("invoicedate") String invoiceDate,
			@FormParam("totalAmount") String totalAmount, @FormParam("description") String description,
			@FormParam("status") String status, @FormParam("invoiceamount") String invoiceamount,
			@FormParam("stage") String stage, @FormParam("useremail") String useremail,
			@FormParam("billofladingdate") String billofladingdate, @FormParam("actualfilename") String actualfilename,
			@FormParam("savedfilename") String savedfilename,
			@FormParam("multipleactualfilename") String multipleactualfilename,
			@FormParam("multiplesavedfilename") String multiplesavedfilename) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.nonposubmitinvoice(invoiceNumber, invoiceDate, bid, totalAmount, description, status,
						invoiceamount, stage, useremail, billofladingdate, actualfilename, savedfilename,
						multipleactualfilename, multiplesavedfilename, irnNumber, irnDate);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("nonposubmitinvoice() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/logout")
	@Produces(MediaType.APPLICATION_JSON)
	public Response logout() {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();

		try {
			jsonArray = objPO.logout(response, httpRequest);
			return Response.status(200).entity(jsonArray).type(MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			log.error("logout() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/insertbulkuploadfile")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getbulkuploadfile(@CookieParam("Authorizationkey") String authkey,
			@FormParam("fileName") String fileName) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.insertbulkinvoice(fileName);
				String token = "";
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getbulkuploadfile() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/pOCreateDeliverynew")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOCreateDeliverynew(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<PoeventdetailsPojo> createdelivery) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getPOCreateDeliverynew(createdelivery);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOCreateDeliverynew() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/createcustomdeliveryitems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createcustomdeliveryitems(@CookieParam("Authorizationkey") String authkey,
			@FormParam("ponumber") String ponumber, @FormParam("lineitemnumbers") List<String> lineitemnumber,
			@FormParam("quantity") List<String> quantity) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				String invoicenumber = null;
				String invoicedate = "";
				jsonArray = objPO.createcustomdeliveryitems(ponumber, lineitemnumber, invoicenumber, quantity, id,
						invoicedate);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("createcustomdeliveryitems() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getorderforfullpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getorderforfullpo(@CookieParam("Authorizationkey") String authkey,
			@FormParam("ponumber") String ponumber, @FormParam("dcn") List<String> dcnvalues) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				String lineitemnumber = null;
				jsonArray = objPO.getorderitems(id, ponumber, lineitemnumber);

				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getorderforfullpo() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getorderhavingdcn")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getorderhavingdcn(@CookieParam("Authorizationkey") String authkey,
			@FormParam("ponumber") String ponumber, @FormParam("dcn") List<String> dcnvalues) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				String lineitemnumber = null;
				jsonArray = objPO.getorderhavingdcn(id, ponumber, dcnvalues);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getorderhavingdcn() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getwithoutpodetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getwithoutpodetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoicenumber") String invoicenumber) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				String lineitemnumber = null;
				jsonArray = objPO.getwithoutpodetails(id, invoicenumber);

				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getwithoutpodetails() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getgrnbasedonpo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getgrnbasedonpo(@CookieParam("Authorizationkey") String authkey,
			@FormParam("ponumber") String ponumber) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
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
	public Response getgrnbasedoninvoiceandpo(@CookieParam("Authorizationkey") String authkey,
			@FormParam("ponumber") String ponumber, @FormParam("dcnumber") List<String> dcnumber) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.getgrnbasedoninvoiceandpo(id, ponumber, dcnumber);

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
	@Path("/insertbulk")
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertbulk(@CookieParam("Authorizationkey") String authkey, @FormParam("filename") String filename,
			@FormParam("username") String username) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {
			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				jsonArray = objPO.insertbulk(filename, username);

				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("insertbulk() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/pOInvoiceSearchData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOInvoiceSearchData(@CookieParam("Authorizationkey") String authkey,
			@FormParam("poInvNumber") String poInvNumber) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getPOInvoiceSearchData(id, poInvNumber);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOInvoiceSearchData() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/deleteemptydeliveries")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteemptydeliveries(@CookieParam("Authorizationkey") String authkey,
			@FormParam("ponumber") String ponumber, @FormParam("dcnumber") List<String> dcnumber) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.deleteemptydeliveries(dcnumber, ponumber);
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("deleteemptydeliveries() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	
	@POST
	@Path("/faqDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response faqDetails(@CookieParam("Authorizationkey") String authkey,	@FormParam("userType") String userType) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.faqDetails(userType);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("deleteemptydeliveries() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	
	@POST
	@Path("/locDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response locationDetails(@CookieParam("Authorizationkey") String authkey) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.locationDetails();
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("deleteemptydeliveries() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	
	@POST
	@Path("/asnDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response putASNDetails(@CookieParam("Authorizationkey") String authkey,	@BeanParam List<AdvanceShippingNotification> persons) {
		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			String vendorEmail = (String) session.getAttribute("vemail");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.insertASNDetails(persons, vendorEmail);
				
				String token = GenrateToken.issueToken(pan, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("putASNDetails() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	
	@POST
	@Path("/asnList")
	@Produces(MediaType.APPLICATION_JSON)
	public Response asnList(@CookieParam("Authorizationkey") String authkey, @FormParam("ponumber") String ponumber,
			@FormParam("bid") String bid) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.getASNList(bid,ponumber);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("asnList() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	
	@POST
	@Path("/poListDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response poListDetails (@CookieParam("Authorizationkey") String authkey, 
	@FormParam("ponumber") String ponumber) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.poListDetails(id,ponumber);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("poListDetails() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/putASN")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateASNDetails (@CookieParam("Authorizationkey") String authkey, @FormParam("bid") String bid,
	@FormParam("asnNumber") String asnNumber, @FormParam("reqNo") String reqNo,@FormParam("docName") String docName,
	@FormParam("status") String status) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new POImpl();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objPO.putAsnNo(bid,asnNumber,reqNo,docName,status);
				String token = GenrateToken.issueToken(pan, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("insertASNDetails() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	
	@GET
	  @Path("/asnhistorydownload")
	  @Produces({"application/json"})
	  public Response asnHistoryDownload(@CookieParam("Authorizationkey") String authkey, @QueryParam("email") String email, @QueryParam("po") String po) {
	    this.jsonArray = new JSONArray();
	    this.responsejson = new JSONObject();
	    this.objPO = new POImpl();
	    try {
	      this.jsonArray = this.objPO.getASNHistory(po, email);
	      String token = GenrateToken.issueToken("", "1203");
	      return Response.status(200).entity(this.jsonArray).header("Authorization", token)
	        .header("Authorizationkey", authkey).type("application/json").build();
	    } catch (Exception e) {
	      log.error("asnhistorydownload() :", e.fillInStackTrace());
	      throw new WebApplicationException(Response.Status.BAD_REQUEST);
	    } 
	  }

}
