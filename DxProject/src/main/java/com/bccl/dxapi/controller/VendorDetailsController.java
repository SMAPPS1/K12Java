package com.bccl.dxapi.controller;

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

import com.bccl.dxapi.apiimpl.ImageUploadImpl;
import com.bccl.dxapi.apiimpl.VendorDetailsImpl;
import com.bccl.dxapi.bean.VendorAmendmentSubmit;
import com.bccl.dxapi.bean.VendorRegSubmission;
import com.bccl.dxapi.security.GenrateToken;

@Path("/vendordetails")
public class VendorDetailsController {

	JSONArray jsonArray = null;
	VendorDetailsImpl objvendor = null;
	String token = "";

	@Context
	private HttpServletRequest httpRequest;
	@Context
	public javax.servlet.http.HttpServletResponse response;

	static Logger log = Logger.getLogger(VendorDetailsController.class.getName());

	@POST
	@Path("/profileData")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVendorDetails(@CookieParam("Authorizationkey") String authkey, @FormParam("Bid") String bid) {
		jsonArray = new JSONArray();
		objvendor = new VendorDetailsImpl();

		try {

			String id = bid;
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				jsonArray = objvendor.getVendorDetails(bid);
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
	@Path("/updatebussinessdetails")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getVendorBussDetails(@FormParam("bussinesslist") List<String> aList,
			@CookieParam("Authorizationkey") String authkey) {

		jsonArray = new JSONArray();
		objvendor = new VendorDetailsImpl();

		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {

				String token = GenrateToken.issueToken(pan, "1203");
				jsonArray = objvendor.getVendorBussDetails(id, aList);
				JSONObject jsonobject = (JSONObject) jsonArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					return Response.status(200).entity(jsonArray).header("Authorization", token)
							.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();					
				} else {
				return Response.status(203).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
				}
			}else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getVendorBussDetails() :", e.fillInStackTrace());
			
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	
	/*  commented for vendor registration on 28-03-2024
	@POST
	@Path("/submitVendorAmendment")
	@Produces(MediaType.APPLICATION_JSON)
	public Response submitVendorReg(@CookieParam("Authorizationkey") String authkey,
			@BeanParam List<VendorAmendmentSubmit> vendor) {
			
		jsonArray = new JSONArray();
		objvendor = new VendorDetailsImpl();
		
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			String sessEmailId= (String) session.getAttribute("vemail");
			System.out.println("Vendor's Email Id : " + sessEmailId);

			if (sessEmailId != null) {

				jsonArray = objvendor.insertVendorAmendment(vendor,sessEmailId);
				String token = GenrateToken.issueToken(sessEmailId, "1203");
				JSONObject jsonobject = (JSONObject) jsonArray.get(0);
				if (jsonobject.get("message").toString().equalsIgnoreCase("Success")) {
					return Response.status(200).entity(jsonArray).header("Authorization", token)
							.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
				}else {
					return Response.status(203).entity(jsonArray).header("Authorization", token)
							.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
				}
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("getPOSubmitInvoice1() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}

	}
	*/
	
}
