package com.bccl.dxapi.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
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
import com.bccl.dxapi.bean.CreditAdviceDetails;
import com.bccl.dxapi.bean.EndUserReturn;
import com.bccl.dxapi.bean.Invoiceapproval;
import java.util.Properties;
import com.bccl.dxapi.security.GenrateToken;

@Path("/internalportal")
public class InternalportalController {

	JSONArray jsonArray = null;
	InvoiceDetailsImpl objPO = null;
	POImpl objPO1 = null;
	InternalportalImpl internalport = null;
	String token = "";

	@Context
	private HttpServletRequest httpRequest;
	@Context
	public javax.servlet.http.HttpServletResponse response;

	static Logger log = Logger.getLogger(InternalportalController.class.getName());

	@POST
	@Path("/getinvoicebasedonemailid")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getinvoicebasedonemailid(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String EmailId, @FormParam("page") String nPage, @FormParam("status") String status,
			@FormParam("invoicenumber") String invno, @FormParam("ponumber") String pono,
			@FormParam("fromdate") String fdate, @FormParam("todate") String tdate, @FormParam("plant") String plant,
			@FormParam("vendor") String vendor) {

		jsonArray = new JSONArray();
		internalport = new InternalportalImpl();

		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getinvoicebasedonemailid(EmailId, session, Integer.parseInt(nPage), status,
						pono, invno, fdate, tdate, plant, vendor);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getinvoicebasedonemailid() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/invoiceStatusDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceStatusDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("iemaild") != null) {

				jsonArray = internalport.getInvoiceStatusDetails(invoice);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceStatusDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/InvoiceApprovalDataStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBuyerApprvalStatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("INVOICENUMBER") String invoiceNumber, @FormParam("PONUMBER") String po_num,
			@FormParam("enduserstatus") String enduserstatus, @FormParam("managerstatus") String managerstatus) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);

			email = (String) session.getAttribute("email");
	
			if (session.getAttribute("email") != null) {
				jsonArray = internalport.getManagerApprvalStatus(invoiceNumber, po_num, enduserstatus, managerstatus);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getBuyerApprvalStatus() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/InvoiceApprovalEndUserStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEndUserApprvalStatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("INVOICENUMBER") String invoiceNumber, @FormParam("PONUMBER") String po_num,
			@FormParam("enduserstatus") String enduserstatus, @FormParam("enduserId") String enduserId,
			@FormParam("stage") String stage, @FormParam("storekeeperaction") String storekeeperaction) {

		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
	
			if (session.getAttribute("email") != null) {
				jsonArray = internalport.getEndUserApprvalStatus(invoiceNumber, po_num, enduserstatus, enduserId, stage,
						storekeeperaction);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getEndUserApprvalStatus() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	// BankDetails
	@POST
	@Path("/invoiceBankDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceBankDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("bid") String bid) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
	
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getInvoiceBankDetails(bid);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceBankDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/managerCountDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getmanagerCountDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getmanagerCountDetails(invoice, po_num);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getmanagerCountDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/managerDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getmanagerDetails(@CookieParam("Authorizationkey") String authkey) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getmanagerDetails();
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getmanagerDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	// Manager Approval 17-11-2021
	@POST
	@Path("/InvoiceApprovalManagerStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getManagerApprvalStatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("INVOICENUMBER") String invoiceNumber, @FormParam("PONUMBER") String po_num,
			@FormParam("managerstatus") String managerstatus, @FormParam("managerId") String managerId,
			@FormParam("stage") String stage) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {
				jsonArray = internalport.getManagerApprvalStatusall(invoiceNumber, po_num, managerstatus, managerId,
						stage);
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

	// Confirmer Approval 30-05-2022
	@POST
	@Path("/InvoiceApprovalConfirmerStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response InvoiceApprovalConfirmerStatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("INVOICENUMBER") String invoiceNumber, @FormParam("PONUMBER") String po_num,
			@FormParam("managerstatus") String managerstatus, @FormParam("managerId") String managerId,
			@FormParam("stage") String stage) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {
				jsonArray = internalport.InvoiceApprovalConfirmerStatus(invoiceNumber, po_num, managerstatus, managerId,
						stage, email);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("InvoiceApprovalConfirmerStatus() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/invoiceBankvendorIdDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceBankvendorIdDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("vendorId") String vendorId) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				int bid = internalport.getInvoiceBankvendorIdDetails(vendorId);
				if (0 != bid) {
					jsonArray = internalport.getInvoiceBankDetails(String.valueOf(bid));
				}
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInvoiceBankvendorIdDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/addMultipleManager")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getaddedMultipleManager(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<Invoiceapproval> manager) {

		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getaddedMultipleManager(manager, session);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getaddedMultipleManager() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/addMultipleManagerwithConfirmer")
	@Produces(MediaType.APPLICATION_JSON)
	public Response addMultipleManagerandConfirmer(@CookieParam("Authorizationkey") String authkey,
			@FormParam("confirmerslist") List<String> confirmers,
			@FormParam("managersapprovelist") List<String> manager) {

		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {
				jsonArray = internalport.addMultipleManagerwithConfirmer(manager, confirmers, session);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("addMultipleManagerandConfirmer() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getMultipleManagerList")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getaddedMultipleManagerList(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getaddedMultipleManagerList(invoice, po_num);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getaddedMultipleManagerList() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getadddetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getdetailsbasedonemail(@CookieParam("Authorizationkey") String authkey,
			@FormParam("portalid") String portalid) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getdetails(portalid);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getdetailsbasedonemail() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getportaltype")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getportaltype(@CookieParam("Authorizationkey") String authkey) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getportaltype(email, session);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getportaltype() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getmanagerslistfordev")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getmanagerslistfordev(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String emailid) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getmanagerslistfordev(emailid);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getmanagerslistfordev() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getmanagerslist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getmanagerslistofhighdesignation(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String emailid, @FormParam("actionby") String actionby,
			@FormParam("plant") String plant, @FormParam("material") String material) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getmanagerslist(emailid, actionby, plant, material, email);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getmanagerslistofhighdesignation() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/searchpeople")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchpeople(@CookieParam("Authorizationkey") String authkey, @FormParam("text") String text,
			@FormParam("actionby") String actionby, @FormParam("plant") String plant,
			@FormParam("material") String material) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getemailidofpersontobesearched(text, actionby, plant, material, email);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("searchpeople() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/searchpeoplefordev")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchpeoplefordev(@CookieParam("Authorizationkey") String authkey,
			@FormParam("text") String text) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getemailidofpersontobesearchedfordev(text);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("searchpeoplefordev() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/internalInvoiceReadChatQuery")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceReadChatStatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String emailid) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {
				email = (String) session.getAttribute("email");
				jsonArray = internalport.getInternalInvoiceReadStatus(emailid);
				String token = GenrateToken.issueToken(email, "1203");
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
	@Path("/trackinvoicestatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response trackinvoicestatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoicenumber") String invoicenumber, @FormParam("ponumber") String ponumber) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {
				String bid = "0";
				jsonArray = objPO.gettrackinvoicestatus(invoicenumber, ponumber, bid);
				String token = GenrateToken.issueToken(email, "1203");
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
	@Path("/invoiceLineItemDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceLineItemDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = objPO.getInvoiceLineItemDetails(invoice, po_num);
				String token = GenrateToken.issueToken(email, "1203");
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
	@Path("/internalpOData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response internalpOData(@CookieParam("Authorizationkey") String authkey, @FormParam("email") String email1,
			@FormParam("status") String status, @FormParam("nPage") String nPage,
			@FormParam("ponumber") String ponumber, @FormParam("fromdateofduration") String fromdateofduration,
			@FormParam("todateofduration") String todateofduration, @FormParam("fromdateofpo") String fromdateofpo,
			@FormParam("todateofpo") String todateofpo, @FormParam("vendor") String vendor,
			@FormParam("plant") String plant) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");
			log.info("mode --" +mode);

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getinternalPODetails(email1, mode, status, Integer.parseInt(nPage), ponumber,
						fromdateofduration, todateofduration, fromdateofpo, todateofpo, plant, vendor);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("internalpOData() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getinvoicebasedonbuyerid")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getinvoicebasedonbuyerid(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String EmailId) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getinvoicebasedonbuyerid(EmailId, session);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getinvoicebasedonbuyerid() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/internalpOEventDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPOEventDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("po_num") String po_num) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");
			if (session.getAttribute("email") != null) {
				String emailid = (String) session.getAttribute("email");

				if (mode.equalsIgnoreCase("buyer")) {
					jsonArray = internalport.getPODetailEventbuyer(po_num, emailid);
				} else if (mode.equalsIgnoreCase("enduser")) {
					jsonArray = internalport.getPODetailEvent(po_num, emailid);
				} else if (mode.equalsIgnoreCase("payer")) {
					jsonArray = internalport.getPODetailEventPayer(po_num);
				}
				String token = GenrateToken.issueToken(email, "1203");

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
	@Path("/internalquerydetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getinternalquerydetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("po_num") String po_num, @FormParam("invoicenumber") String invoiceno) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getinternalquerydetails(po_num, invoiceno);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getinternalquerydetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/internalquerydetailsofapproval")
	@Produces(MediaType.APPLICATION_JSON)
	public Response internalquerydetailsofapproval(@CookieParam("Authorizationkey") String authkey,
			@FormParam("po_num") String po_num, @FormParam("invoicenumber") String invoiceno) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.internalquerydetailsofapproval(po_num, invoiceno);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("internalquerydetailsofapproval() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/internalInvoiceChatSubmit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response setInvoiceChatQuery(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice_num") String invoice_num, @FormParam("po_num") String po_num,
			@FormParam("emailid") String emailid, @FormParam("topic") String topic,
			@FormParam("message") String message, @FormParam("subject") String subject) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {
				jsonArray = internalport.setInternalChatStatus(invoice_num, po_num, emailid, topic, message, subject);
				String token = GenrateToken.issueToken(email, "1203");

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
	@Path("/internalInvoiceStatusSubmit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response internalInvoiceStatusSubmit(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice_num") String invoice_num, @FormParam("po_num") String po_num,
			@FormParam("emailid") String emailid, @FormParam("topic") String topic,
			@FormParam("message") String message, @FormParam("subject") String subject) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {
				jsonArray = internalport.internalInvoiceStatusSubmit(invoice_num, po_num, emailid, topic, message,
						subject);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("internalInvoiceStatusSubmit() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/internalpOMessages")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInternalMessages(@CookieParam("Authorizationkey") String authkey,
			@FormParam("po_num") String po_num) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getInternalMessages(po_num);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getInternalMessages() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/internalpOSubmitQuery")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getinternalPOSubmitQuery(@CookieParam("Authorizationkey") String authkey,
			@FormParam("bid") String bid, @FormParam("po_num") String po_num, @FormParam("emailid") String emailid,
			@FormParam("invoiceNumber") String invoiceNumber, @FormParam("subject") String subject,
			@FormParam("message") String message, @FormParam("status") String status) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String id = "";
			String pan = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getinternalPOSubmitQuery(po_num, emailid, invoiceNumber, message, subject,
						status);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getinternalPOSubmitQuery() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/internalpOReadStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getinternalPoReadStatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String emailid) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getinternalPOReadStatus(emailid);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getinternalPoReadStatus() :", e.fillInStackTrace());
			
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
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String mode = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				if (mode.equalsIgnoreCase("buyer")) {
					jsonArray = internalport.getBuyerPoninvoiceSummery(emailid, Integer.parseInt(invoicepageno), status,
							invno, pono, fdate, tdate, plant, vendor);
				} else if (mode.equalsIgnoreCase("enduser")) {
					jsonArray = internalport.getRequsitionerPoninvoiceSummery(emailid, Integer.parseInt(invoicepageno),
							status, invno, pono, fdate, tdate, plant, vendor);
				} else if (mode.equalsIgnoreCase("payer")) {
					jsonArray = internalport.getPayerPoninvoiceSummery(Integer.parseInt(invoicepageno), status, invno,
							pono, fdate, tdate, plant, vendor);
				} else if (mode.equalsIgnoreCase("internalbcclportal")) {
					jsonArray = internalport.getInternalPoninvoiceSummery(emailid);
				}
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getRequsitionerBuyerPayerPoninvoiceSummery() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/internalChatUpdate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response setInternalChatStatusUpdate(@CookieParam("Authorizationkey") String authkey,
			@FormParam("bid") String bid, @FormParam("emailid") String emailid,
			@FormParam("invoice_num") String invoice_num, @FormParam("po_num") String po_num) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {
				jsonArray = internalport.setInternalChatStatusUpdate(bid, emailid, invoice_num, po_num);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("setInternalChatStatusUpdate() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/internaltrackinvoicestatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response internaltrackinvoicestatus(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoicenumber") String invoicenumber, @FormParam("ponumber") String ponumber) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {
				String bussinesspartneroid = "1";
				jsonArray = objPO.gettrackinvoicestatus(invoicenumber, ponumber, bussinesspartneroid);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("internaltrackinvoicestatus() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/profileDatabuyervendor")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVendorDetailsBuyer(@CookieParam("Authorizationkey") String authkey) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				if (mode.equalsIgnoreCase("buyer")) {
					jsonArray = internalport.getVendorDetailsbuyer(email);
				}
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getVendorDetailsBuyer() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getorderitems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getorderitems(@CookieParam("Authorizationkey") String authkey, @FormParam("po_num") String po_num,
			@FormParam("lineitemnumber") String lineitemnumber) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				String bid = "0";
				jsonArray = objPO1.getorderitems(bid, po_num, lineitemnumber);
				String token = GenrateToken.issueToken(email, "1203");

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
	@Path("/getinternalpofile")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getinternalpofile(@FormParam("email") String email1, @FormParam("status") String status,
			@FormParam("nPage") String nPage, @FormParam("ponumber") String ponumber,
			@FormParam("fromdateofduration") String fromdateofduration,
			@FormParam("todateofduration") String todateofduration, @FormParam("fromdateofpo") String fromdateofpo,
			@FormParam("todateofpo") String todateofpo, @FormParam("vendor") String vendor,
			@FormParam("plant") String plant, @CookieParam("Authorizationkey") String authkey) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				List<String> podata = new ArrayList<>();
				String token = GenrateToken.issueToken(email, "1203");
				
				log.info("email1 :" + email1 + " :mode: "+mode +" :status: "+status+ " :page: "+0
						+ " :ponumber: "+ponumber+ " :fromdateofduration: "+fromdateofduration
						+ " :todateofduration: "+todateofduration+ " :fromdateofpo: "
						+fromdateofpo+ " :todateofpo: "+todateofpo+ " :plant: "+plant+ " :vendor: "+vendor);
				
				jsonArray = internalport.getPODetails(email1, mode, status, 0, ponumber, fromdateofduration,
						todateofduration, fromdateofpo, todateofpo, plant, vendor);
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getinternalpofile() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/downloadinternalinvoiceData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadinvoiceData(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String emailid, @FormParam("page") String invoicepageno,
			@FormParam("status") String status, @FormParam("invoicenumber") String invno,
			@FormParam("ponumber") String pono, @FormParam("fromdate") String fdate, @FormParam("todate") String tdate,
			@FormParam("plant") String plant, @FormParam("vendor") String vendor) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
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
				jsonArray = internalport.getinernaldownloadInvoceidIdDetail(emailid, 0, status, invno, pono, fdate,
						tdate, plant, vendor, mode);
				JSONObject jsonobject = (JSONObject) jsonArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					invoicedata = (List<String>) jsonobject.get("invoicedata");
					podata = (List<String>) jsonobject.get("podata");
				}
				
				*/
			//	jsonArray = internalport.downloadapprovalinvoicelist(invoicedata, podata, email, "");
				
				jsonArray = internalport.downloadapprovalinvoicelist(emailid, status, invno, pono, fdate, tdate,plant,vendor, "View",mode);
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
	@Path("/downloadapprovalinvoicelist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadapprovalinvoicelist(@CookieParam("Authorizationkey") String authkey,
			@FormParam("emailid") String EmailId, @FormParam("status") String status, @FormParam("inv") String invno,
			@FormParam("po") String pono, @FormParam("fd") String fdate, @FormParam("td") String tdate) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
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
				jsonArray = internalport.getapprovalinvoicelistfordownload(EmailId, status, invno, pono, fdate, tdate,
						session);
				JSONObject jsonobject = (JSONObject) jsonArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					invoicedata = (List<String>) jsonobject.get("invoicedata");
					podata = (List<String>) jsonobject.get("podata");
				}
				//jsonArray = internalport.downloadapprovalinvoicelist(invoicedata, podata, email, "APPROVAL");
				*/
				jsonArray = internalport.downloadapprovalinvoicelist(EmailId, status, invno, pono, fdate, tdate, "NA", "NA", "NA", "APPROVAL");
				
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("downloadapprovalinvoicelist() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/reassigninvoice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response reassigninvoice(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoicenumber") String invoicenumber, @FormParam("ponumber") String ponumber,
			@FormParam("emailtobereassigned") String emailtobereassigned,
			@FormParam("useremailid") String useremailid) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				jsonArray = internalport.reassigninvoice(invoicenumber, ponumber, emailtobereassigned, useremailid);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("reassigninvoice() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/updateacceptedquantity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateacceptedquantity(@CookieParam("Authorizationkey") String authkey,
			@FormParam("updateaccepted") List<String> values) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
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
					jsonArray = internalport.updateacceptedquantity(values, email);
				} else if ("uat".equalsIgnoreCase(server)) {
					jsonArray = internalport.getAcceptQtynGRN(values, email);
				}

				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateacceptedquantity() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/updateacceptedquantitywithoutgrn")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateacceptedquantitywithoutgrn(@CookieParam("Authorizationkey") String authkey,
			@FormParam("updateacceptedwithoutgrn") List<String> values) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
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
					jsonArray = internalport.updateacceptedquantitywithoutgrn(values, email);
				} else if ("uat".equalsIgnoreCase(server)) {
					jsonArray = internalport.updateacceptedquantitywithoutgrnforprod(values, email);
				}

				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateacceptedquantitywithoutgrn() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/updateacceptedservicequantitywithoutgrn")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateacceptedservicequantitywithoutgrn(@CookieParam("Authorizationkey") String authkey,
			@FormParam("updateserviceaccepted") List<String> values) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				jsonArray = internalport.updateacceptedservicequantitywithoutgrn(values, email);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateacceptedservicequantitywithoutgrn() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/updateacceptedservicequantity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateacceptedservicequantity(@CookieParam("Authorizationkey") String authkey,
			@FormParam("updateaccepted") List<String> values) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
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
					jsonArray = internalport.updateacceptedservicequantity(values, email);
				} else if ("uat".equalsIgnoreCase(server)) {
					jsonArray = internalport.getAcceptQtynServiceGRN(values, email);
				}

				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateacceptedservicequantity() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getportalidfromemail")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getportalid(@CookieParam("Authorizationkey") String authkey,
			@FormParam("useremail") String useremail) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
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
					jsonArray = internalport.getportalidfordev(useremail);
				} else if ("uat".equalsIgnoreCase(server)) {
					jsonArray = internalport.getportalid(useremail);
				}

				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getportalid() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getstorage")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getstoragelocation(@CookieParam("Authorizationkey") String authkey,
			@FormParam("plant") String plant) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getstoragelocation(plant);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			}

		} catch (Exception e) {
			log.error("getstoragelocation() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/updateenduserspecialdetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateenduserspecialdetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("multipleactualfilename") String multipleactualfilename,
			@FormParam("multiplesavedfilename") String multiplesavedfilename,
			@FormParam("enduserremark") String enduserremark, @FormParam("invoicenumber") String invoicenumber,
			@FormParam("ponumber") String ponumber) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");
			if (session.getAttribute("email") != null) {
				jsonArray = internalport.updateenduserspecialdetails(multipleactualfilename, multiplesavedfilename,
						enduserremark, invoicenumber, ponumber);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("updateenduserspecialdetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}

	@POST
	@Path("/getcreditadvice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getcreditadvice(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoice") String invoice, @FormParam("po_num") String po_num) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {
			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				jsonArray = internalport.getcreditadvice(invoice, po_num);
				String token = GenrateToken.issueToken(email, "1203");

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
	@Path("/shortfallcreditadvice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getshortfallCreditadvice(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<CreditAdviceDetails> shortfall) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.shortfallCreditadvice(shortfall);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getshortfallCreditadvice() :", e.fillInStackTrace());
			
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
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.addcreditNote(invoice, po_num, bid, creditadviceno, amount, tax, totalamt,
						actualfilename, savedfilename);
				String token = GenrateToken.issueToken(email, "1203");
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
	@Path("/getintcreditNoteDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getintcreditNoteDetails(@CookieParam("Authorizationkey") String authkey,
			@FormParam("creditadviceno") String creditadviceno, @FormParam("invoice") String invoice,
			@FormParam("po_num") String po_num) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getcreditNoteDetails(invoice, po_num, creditadviceno);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getintcreditNoteDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getEnderUserReturn")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEnderUserReturn(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<EndUserReturn> enduserList) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getEnderUserReturn(enduserList, email);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getEnderUserReturn() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/insertenduserfiles")
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertenduserfiles(@CookieParam("Authorizationkey") String authkey,
			@FormParam("filenames") List<String> filenames, @FormParam("invoicenumber") String invoicenumber,
			@FormParam("purchasenumber") String ponumber, @FormParam("timestamp") String sampletimestamp) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.updatesupportingfiles(ponumber, invoicenumber, filenames, sampletimestamp);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("insertenduserfiles() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/invoiceinternaldashboardsearch")
	@Produces(MediaType.APPLICATION_JSON)
	public Response invoiceinternaldashboardsearch(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoicenumber") String invoicenumber, @FormParam("ponumber") String ponumber,
			@FormParam("status") String status, @FormParam("fromdurationdate") String fromdurationdate,
			@FormParam("todurationdate") String todurationdate, @FormParam("plant") String plant,
			@FormParam("purchasegroup") String purchasegroup,
			@FormParam("requisitioneremailid") String requisitioneremailid,
			@FormParam("frominvamount") String frominvamount, @FormParam("toinvamount") String toinvamount,
			@FormParam("ageingfrom") String ageingfrom, @FormParam("ageingto") String ageingto,
			@FormParam("npage") String nPage, @FormParam("vendor") String vendor) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.invoiceinternaldashboardsearch(invoicenumber, ponumber, status,
						fromdurationdate, todurationdate, plant, purchasegroup, requisitioneremailid, frominvamount,
						toinvamount, ageingfrom, ageingto, session, email, Integer.parseInt(nPage), vendor);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("invoiceinternaldashboardsearch() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/pointernaldashboardsearch")
	@Produces(MediaType.APPLICATION_JSON)
	public Response pointernaldashboardsearch(@CookieParam("Authorizationkey") String authkey,
			@FormParam("ponumber") String ponumber, @FormParam("status") String status,
			@FormParam("fromdurationdate") String fromdurationdate, @FormParam("todurationdate") String todurationdate,
			@FormParam("plant") String plant, @FormParam("purchasegroup") String purchasegroup,
			@FormParam("requisitioneremailid") String requisitioneremailid,
			@FormParam("frompoamount") String frompoamount, @FormParam("topoamount") String topoamount,
			@FormParam("ageingfrom") String ageingfrom, @FormParam("ageingto") String ageingto,
			@FormParam("npage") String nPage, @FormParam("vendor") String vendor) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");
			
			log.info("mode --"+ mode);
			log.info("ponumber: " +ponumber + " :status: " +status + " :fromdurationdate: " +fromdurationdate 
					+ " :todurationdate: " +todurationdate+ " :plant: " +plant+ " :purchasegroup: " +purchasegroup
					+ " :requisitioneremailid: " +requisitioneremailid+ " :frompoamount: " +frompoamount
					+ " :topoamount: " +topoamount+ " :ageingfrom: " +ageingfrom+ " :ageingto: " +ageingto
					+ " :session: " +session+ " :email: " +email+ " :nPage: " +nPage+ " :mode: " +mode
					+ " :vendor: " +vendor);
			
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.pointernaldashboardsearch(ponumber, status, fromdurationdate, todurationdate,
						plant, purchasegroup, requisitioneremailid, frompoamount, topoamount, ageingfrom, ageingto,
						session, email, Integer.parseInt(nPage), mode, vendor);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("pointernaldashboardsearch() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/creditnoteinternaldashboardsearch")
	@Produces(MediaType.APPLICATION_JSON)
	public Response creditnotedashboardsearch(@CookieParam("Authorizationkey") String authkey,
			@FormParam("filenames") List<String> filenames, @FormParam("invoicenumber") String invoicenumber,
			@FormParam("purchasenumber") String ponumber, @FormParam("timestamp") String sampletimestamp) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.updatesupportingfiles(ponumber, invoicenumber, filenames, sampletimestamp);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("creditnotedashboardsearch() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getplantcodeordescc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getplantcodeordescc(@CookieParam("Authorizationkey") String authkey,
			@FormParam("codeordesc") String text) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getplantcodeordescc(text);
				String token = GenrateToken.issueToken(email, "1203");
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
	@Path("/getmatcodeordescc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getmatcodeordescc(@CookieParam("Authorizationkey") String authkey,
			@FormParam("matcodeordesc") String text) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getmatcodeordescc(text);
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getmatcodeordescc() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/getvendorname")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getvendorname(@CookieParam("Authorizationkey") String authkey,
			@FormParam("vendordesc") String text) {
		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");

			if (session.getAttribute("email") != null) {

				jsonArray = internalport.getvendorname(text);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getvendorname() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/downloaddashboardinvoicelist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloaddashboardinvoicelist(@CookieParam("Authorizationkey") String authkey,
			@FormParam("invoicenumber") String invoicenumber, @FormParam("ponumber") String ponumber,
			@FormParam("status") String status, @FormParam("fromdurationdate") String fromdurationdate,
			@FormParam("todurationdate") String todurationdate, @FormParam("plant") String plant,
			@FormParam("purchasegroup") String purchasegroup,
			@FormParam("requisitioneremailid") String requisitioneremailid,
			@FormParam("frominvamount") String frominvamount, @FormParam("toinvamount") String toinvamount,
			@FormParam("ageingfrom") String ageingfrom, @FormParam("ageingto") String ageingto,
			@FormParam("vendor") String vendor) {

		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
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
				jsonArray = internalport.downloaddashboardinvoicelist(invoicenumber, ponumber, status, fromdurationdate,
						todurationdate, plant, purchasegroup, requisitioneremailid, frominvamount, toinvamount,
						ageingfrom, ageingto, session, email, vendor);

				JSONObject jsonobject = (JSONObject) jsonArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					invoicedata = (List<String>) jsonobject.get("invoicedata");
					podata = (List<String>) jsonobject.get("podata");
				}
				*/
				//jsonArray = internalport.downloadapprovalinvoicelist(invoicedata, podata, email, "DASHBOARD");
				//jsonArray = internalport.downloadapprovalinvoicelist(email, status, invoicenumber, ponumber,fromdurationdate, todurationdate, plant, vendor, "view",mode );
				
				jsonArray = internalport.downloadAdvanceSearchlist(invoicenumber, ponumber, status, fromdurationdate,
						todurationdate, plant, purchasegroup, requisitioneremailid, frominvamount, toinvamount,
						ageingfrom, ageingto, email, vendor);
				
				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("downloaddashboardinvoicelist() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/downloaddashboardpolist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloaddashboardpolist(@CookieParam("Authorizationkey") String authkey,
			@FormParam("ponumber") String ponumber, @FormParam("status") String status,
			@FormParam("fromdurationdate") String fromdurationdate, @FormParam("todurationdate") String todurationdate,
			@FormParam("plant") String plant, @FormParam("purchasegroup") String purchasegroup,
			@FormParam("requisitioneremailid") String requisitioneremailid,
			@FormParam("frompoamount") String frompoamount, @FormParam("topoamount") String topoamount,
			@FormParam("ageingfrom") String ageingfrom, @FormParam("ageingto") String ageingto,
			@FormParam("vendor") String vendor) {

		objPO = new InvoiceDetailsImpl();
		objPO1 = new POImpl();
		internalport = new InternalportalImpl();
		jsonArray = new JSONArray();
		try {

			String email = "";
			String mode = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			email = (String) session.getAttribute("email");
			mode = (String) session.getAttribute("mode");

			if (session.getAttribute("email") != null) {
				List<String> invoicedata = new ArrayList<>();
				List<String> podata = new ArrayList<>();
				log.info("downloaddashboardpolist ---");
				log.info("ponumber: "+ponumber+" :status: "+ status+" :fromdurationdate: "+ fromdurationdate
						+" :todurationdate: "+todurationdate +" :plant: "+plant +" :purchasegroup: "+ purchasegroup
						+" :requisitioneremailid: "+ requisitioneremailid+" :frompoamount: "+ frompoamount+" :topoamount: "+ 
						topoamount +" :ageingfrom: "+ ageingfrom+" :ageingto: "+ ageingto+" :session: "+session 
						+" :email: "+ email+" :0: "+0 +" :mode: "+ mode+" :vendor: "+ vendor);
				/*
				jsonArray = internalport.downloaddashboardpolist(ponumber, status, fromdurationdate, todurationdate,
						plant, purchasegroup, requisitioneremailid, frompoamount, topoamount, ageingfrom, ageingto,
						session, email, 0, mode, vendor);

				JSONObject jsonobject = (JSONObject) jsonArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					podata = (List<String>) jsonobject.get("podata");
				}
				String bid = "0";
				//jsonArray = objPO1.getPODetails(podata);
				 
				 */
				jsonArray = internalport.getPODetails(ponumber, status, fromdurationdate, todurationdate,
						plant, purchasegroup, requisitioneremailid, frompoamount, topoamount, ageingfrom, ageingto,
						session, email, 0, mode, vendor);

				String token = GenrateToken.issueToken(email, "1203");
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();

			}

		} catch (Exception e) {
			log.error("downloaddashboardpolist() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
}
