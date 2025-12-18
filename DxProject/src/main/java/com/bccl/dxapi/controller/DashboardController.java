package com.bccl.dxapi.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

import com.bccl.dxapi.apiimpl.DashboardImpl;
import com.bccl.dxapi.security.GenrateToken;

@Path("/dashboard")
public class DashboardController {
	
	static Logger log = Logger.getLogger(DashboardController.class.getName());
	
	JSONArray jsonArray = null;
	JSONObject responsejson = null;
	DashboardImpl objPO = null;

	@Context
	private HttpServletRequest httpRequest;
	
	
	@POST
	@Path("/getInvoiceSummeryFunctionWise")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceSummeryFunctionWise(@CookieParam("Authorizationkey") String authkey,
			@FormParam("deptcode") String deptcode,@FormParam("fromDate") String fromDate,@FormParam("toDate") String toDate) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new DashboardImpl();
		
		try {
			String mode = "";
			String email = "";
			HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");
			
			log.info("getInvoiceSummeryFunctionWise API called for userid: " + email + " mode : "+mode);
			
			if (session.getAttribute("email") != null) {
				
				jsonArray = objPO.getInvoiceSummeryFunctionWise(deptcode,fromDate,toDate);
//				jsonArray = objPO.getInvoiceSummeryFunctionWise_Chart(deptcode,fromDate,toDate);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getInvoiceSummeryFunctionWise() :", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	
	
	@POST
	@Path("/getInvoiceAgeing")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceAgeing(@CookieParam("Authorizationkey") String authkey,
			@FormParam("deptcode") String deptcode,@FormParam("fromDate") String fromDate,
			@FormParam("toDate") String toDate, @FormParam("status") String status) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new DashboardImpl();
		
		try {
			String mode = "";
			String email = "";
			HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");
			
			log.info("getInvoiceAgeing API called for userid: " + email + " mode : "+mode);
			
			if (session.getAttribute("email") != null) {
				
				jsonArray = objPO.getInvoiceAgeing(deptcode,fromDate,toDate,status);
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getInvoiceAgeing() :", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	
	
	@POST
	@Path("/getInvoiceDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInvoiceDetails(@CookieParam("Authorizationkey") String authkey,@FormParam("deptcode") String deptcode,
			@FormParam("fromDate") String fromDate,@FormParam("toDate") String toDate,@FormParam("status") String status) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new DashboardImpl();
		
		try {
			String mode = "";
			String email = "";
			HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");
			
			log.info("getInvoiceAgeing API called for userid: " + email + " mode : "+mode);
			
			if (session.getAttribute("email") != null) {
				
				jsonArray = objPO.getInvoiceDetails(deptcode,fromDate,toDate,status);
				String token = GenrateToken.issueToken(email, "1203");

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
	@Path("/getDeptDetails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDeptDetails(@CookieParam("Authorizationkey") String authkey) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new DashboardImpl();
		
		try {
			String mode = "";
			String email = "";
			HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");
			
			log.info("getDeptDetails API called for userid: " + email + " mode : "+mode);
			
			if (session.getAttribute("email") != null) {
				
				jsonArray = objPO.getDeptDetails(email,mode);
				
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getDeptDetails() :", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	
	
	@POST
	@Path("/getBigFiveFunctionData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBigFiveFunctionData(@CookieParam("Authorizationkey") String authkey) {

		jsonArray = new JSONArray();
		responsejson = new JSONObject();
		objPO = new DashboardImpl();
		
		try {
			String mode = "";
			String email = "";
			HttpSession session = httpRequest.getSession(false);
			mode = (String) session.getAttribute("mode");
			email = (String) session.getAttribute("email");
			
			log.info("getBigFiveFunctionData API called for userid: " + email + " mode : "+mode);
			
			if (session.getAttribute("email") != null) {
				
				jsonArray = objPO.getBigFiveFunctionData(email,mode);
				
				String token = GenrateToken.issueToken(email, "1203");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}
		} catch (Exception e) {
			log.error("getBigFiveFunctionData() :", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
}
