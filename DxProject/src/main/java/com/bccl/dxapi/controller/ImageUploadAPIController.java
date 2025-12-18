package com.bccl.dxapi.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.core.StreamingOutput;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.bccl.dxapi.apiimpl.ImageUploadImpl;
import com.bccl.dxapi.apiimpl.POImpl;
import com.bccl.dxapi.bean.Invoicesubmission;
import com.bccl.dxapi.bean.VendorRegSubmission;
import com.bccl.dxapi.security.GenrateToken;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;

@Path("/file")
public class ImageUploadAPIController {

	JSONArray jsonArray = null;
	JSONObject responsejson = null;

	ImageUploadImpl objImage = null;
	String token = "";

	@Context
	private HttpServletRequest httpRequest;
	@Context
	public javax.servlet.http.HttpServletResponse response;

	static Logger log = Logger.getLogger(ImageUploadAPIController.class.getName());

	@POST
	@Path("/productProfile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response UploadFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@CookieParam("Authorizationkey") String authkey) {
		objImage = new ImageUploadImpl();
		jsonArray = new JSONArray();
		try {

			String id = "";
			String pan = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");

			if (session.getAttribute("id") != null) {
				int count = 0;

				for (int i = 0; i < fileDetail.getFileName().length(); i++) {
					if (fileDetail.getFileName().charAt(i) == '_')
						count++;
				}
				String token = GenrateToken.issueToken(pan, "1203");
				if (count > 2) {
					jsonArray = objImage.uploadvendorFile(uploadedInputStream, fileDetail, id);

				} else {
					jsonArray = objImage.UploadPortfolio(uploadedInputStream, fileDetail, id);
				}

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("UploadFile() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
/*
	@POST
	@Path("/multiplefileuploads")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response UploadMultipleFiles(@FormDataParam("files") List<FormDataBodyPart> file,
			@FormDataParam("files") List<FormDataContentDisposition> fileDetail,
			@CookieParam("Authorizationkey") String authkey) {
		objImage = new ImageUploadImpl();
		jsonArray = new JSONArray();
		try {

			String id = "";
			String pan = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			email = (String) session.getAttribute("email");

			if (session.getAttribute("id") != null) {
				jsonArray = objImage.Uploadmultiplefiles(file, fileDetail, id, "");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else if (session.getAttribute("email") != null) {

				String token = GenrateToken.issueToken(email, "1203");
				jsonArray = objImage.Uploadmultiplefiles(file, fileDetail, id, "");

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("UploadMultipleFiles() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
*/	
	/*
	 * @POST
	@Path("/vendorProfile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response UploadVendorFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@CookieParam("Authorizationkey") String authkey) 
	 */
	
//	@POST
//	@Path("/fileuploads")
	@POST
	@Path("/multiplefileuploads")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response fileuploads(@FormDataParam("files") List<FormDataBodyPart> file,
			@FormDataParam("files") List<FormDataContentDisposition> fileDetail,
			@CookieParam("Authorizationkey") String authkey,@QueryParam(value = "fileType") String fileType) {
		objImage = new ImageUploadImpl();
		jsonArray = new JSONArray();
		try {

			String id = "";
			String pan = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			email = (String) session.getAttribute("email");

			if (session.getAttribute("id") != null) {
				jsonArray = objImage.Uploadmultiplefiles(file, fileDetail, id, fileType);

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else if (session.getAttribute("email") != null) {

				String token = GenrateToken.issueToken(email, "1203");
				jsonArray = objImage.Uploadmultiplefiles(file, fileDetail, id, fileType);

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("UploadMultipleFiles() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/downloadfile")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadFile(@FormParam("fileName") String fileName,
			@CookieParam("Authorizationkey") String authkey, @FormParam("typeofdownload") String typeofdownload) {

		objImage = new ImageUploadImpl();
		jsonArray = new JSONArray();
		try {

			String id = "";
			String pan = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			email = (String) session.getAttribute("email");

			if (session.getAttribute("id") != null) {
				String token = GenrateToken.issueToken(pan, "1203");
				jsonArray = objImage.downloadfile(fileName, typeofdownload);

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else if (session.getAttribute("email") != null) {

				String token = GenrateToken.issueToken(email, "1203");
				jsonArray = objImage.downloadfile(fileName, typeofdownload);

				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("downloadFile() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@POST
	@Path("/downloadinternalFile")
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadinternalFile(@FormParam("fileName") String fileName,
			@CookieParam("Authorizationkey") String authkey, @FormParam("typeofdownload") String typeofdownload) {

		objImage = new ImageUploadImpl();
		jsonArray = new JSONArray();
		try {

			String id = "";
			String pan = "";
			String email = "";
			javax.servlet.http.HttpSession session = httpRequest.getSession(false);
			id = (String) session.getAttribute("id");
			pan = (String) session.getAttribute("pan");
			email = (String) session.getAttribute("email");

			if (session.getAttribute("id") != null) {

				String token = GenrateToken.issueToken(pan, "1203");
				jsonArray = objImage.downloadfile(fileName, typeofdownload);
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else if (session.getAttribute("email") != null) {

				String token = GenrateToken.issueToken(email, "1203");
				jsonArray = objImage.downloadfile(fileName, typeofdownload);
				return Response.status(200).entity(jsonArray).header("Authorization", token)
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			} else {
				return Response.status(406).entity(jsonArray).header("Authorization", "")
						.header("Authorizationkey", authkey).type(MediaType.APPLICATION_JSON).build();
			}

		} catch (Exception e) {
			log.error("downloadinternalFile() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@GET
	@Path("/download")
	public Response downloadFile(@QueryParam("fileName") String fileName) {

		objImage = new ImageUploadImpl();
		jsonArray = new JSONArray();
		try {
			StreamingOutput in = objImage.download(fileName);

			if (in != null) {
				return Response.ok(in, MediaType.APPLICATION_OCTET_STREAM)
						.header("Content-Disposition", "attachment; filename=" + fileName).build();
			} else {
				return Response.status(Response.Status.OK).entity("File not found.").build();
			}
		} catch (Exception e) {
			log.error("downloadFile() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	@GET
	@Path("/downloadPo")
	public Response downloadPoFile(@QueryParam("fileName") String fileName) {

		objImage = new ImageUploadImpl();
		jsonArray = new JSONArray();
		try {

			StreamingOutput in = objImage.downloadPo(fileName);
			if (in != null) {
				return Response.ok(in, MediaType.APPLICATION_OCTET_STREAM)
						.header("Content-Disposition", "attachment; filename=" + fileName + ".pdf").build();
			} else {
				return Response.status(Response.Status.OK).entity("File not found.").build();
			}

		} catch (Exception e) {
			log.error("downloadPoFile() :", e.fillInStackTrace());

			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
	
	@GET
	@Path("/downloadPopdf")
	public Response downloadPopdf(@QueryParam("po") String po) {
		try {
			objImage = new ImageUploadImpl();
			InputStream in = objImage.downloadPopdf(po);
			String filename = po + "" + ".pdf";
			if (in != null) {
				return Response.ok(in).header("Content-Disposition", "attachment;filename=\"" + filename).build();
			} else {
				return Response.status(Response.Status.OK).entity("File not found.").build();
			}
	        
		} catch (Exception e) {
			log.error("downloadFile() :", e.fillInStackTrace());
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}
}