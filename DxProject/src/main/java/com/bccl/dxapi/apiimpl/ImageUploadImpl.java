package com.bccl.dxapi.apiimpl;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.bccl.dxapi.controller.VendorDetailsController;
import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.bccl.dxapi.apiutility.DBConnection;
import com.bccl.dxapi.apiutility.MailOffice365;
import com.bccl.dxapi.bean.Invoicesubmission;
import com.bccl.dxapi.bean.VendorRegSubmission;

public class ImageUploadImpl {

	static Logger log = Logger.getLogger(ImageUploadImpl.class.getName());

	private final String baseAuthUrl = "http://localhost:3001/api/";
	private final int BUFFER_SIZE = 5120;

	public ImageUploadImpl() {
		jsonObject = null;
		responsejson = new JSONObject();
		jsonArray = new JSONArray();
		input = ImageUploadImpl.class.getResourceAsStream("/dxproperties.properties");
		prop = new Properties();
	}

	@Override
	protected void finalize() throws Throwable {
		responsejson = null;
		jsonArray = null;
		jsonObject = null;
		input = null;
		prop = null;
		super.finalize();
	}

	JSONObject jsonObject = null;
	JSONObject responsejson = null;
	JSONArray jsonArray = null;
	InputStream input = null;
	Properties prop = null;

	@SuppressWarnings("unchecked")
	public JSONArray UploadPortfolio(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
			String id) {

		String extension = "";
		String msg = "";
		int MaxSize = 1024 * 5;
		boolean status = false;
		Random random = new Random();
		int no = random.nextInt(9 - 1 + 1) + 1;

		try {
			int i = fileDetail.getFileName().lastIndexOf('.');
			if (i > 0) {
				extension = fileDetail.getFileName().substring(i + 1);
			}
			prop.load(input);

			if (fileDetail.getSize() < MaxSize) {

				String[] fileName = id.split("@");
				String filename = id + "_" + no + fileDetail.getFileName();
				String Uploadfilepath = "";
				String path = "";

				Uploadfilepath = prop.getProperty("fileLocation") + fileName[0];
				path = Uploadfilepath + "/" + filename;

				File file = new File(Uploadfilepath);
				if (!file.exists()) {
					if (file.mkdirs()) {
					}
				}
				if (extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("jpg")
						|| extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("doc")
						|| extension.equalsIgnoreCase("docx") || extension.equalsIgnoreCase("xls")
						|| extension.equalsIgnoreCase("xlsx") || extension.equalsIgnoreCase("pdf")) {

					filename = fileDetail.getFileName();

					File filepath = new File(path);
					OutputStream out = new FileOutputStream(filepath);
					int read = 0;
					byte[] bytes = new byte[MaxSize];
					while ((read = uploadedInputStream.read(bytes)) != -1) {
						out.write(bytes, 0, read);
						status = true;
					}

					out.flush();
					out.close();
				} else {
					msg = "File Extension not allowed";
					status = false;
				}
			} else {
				msg = "Maximum File size limit 5 MB";
				status = false;
			}
			responsejson.put("status", "Success");
			responsejson.put("data", status);
			responsejson.put("message", msg);
			jsonArray.add(responsejson);

		} catch (IOException e) {
			log.error("UploadPortfolio() :", e.fillInStackTrace());

		}
		return jsonArray;

	}

	public JSONArray uploadvendorFile(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
			String id) {
		String extension = "";
		String msg = "";
		int MaxSize = 1024 * 5;
		boolean status = false;

		try {
			int i = fileDetail.getFileName().lastIndexOf('.');
			if (i > 0) {
				extension = fileDetail.getFileName().substring(i + 1);
			}
			prop.load(input);

			if (fileDetail.getSize() < MaxSize) {

				String[] fileName = fileDetail.getFileName().split("_");
				String ponumber = fileName[0];
				String type = fileName[1];
				String timestamp = fileName[fileName.length - 1];
				int iend = timestamp.indexOf(".");
				// String subString;
				if (iend != -1) {
					timestamp = timestamp.substring(0, iend);
				}

				String Uploadfilepath = "";

				String path = "";
				Uploadfilepath = prop.getProperty("fileLocation") + "/" + ponumber + "/" + type + "/" + timestamp;
				path = Uploadfilepath + "/" + fileDetail.getFileName();

				File file = new File(Uploadfilepath);
				if (!file.exists()) {
					if (file.mkdirs()) {
					}
				}
				if (extension.equalsIgnoreCase("pdf") || extension.equalsIgnoreCase("csv")) {

					File filepath = new File(path);
					OutputStream out = new FileOutputStream(filepath);
					int read = 0;
					byte[] bytes = new byte[MaxSize];
					while ((read = uploadedInputStream.read(bytes)) != -1) {
						out.write(bytes, 0, read);
						status = true;
					}

					out.flush();
					out.close();

				} else {
					msg = "File Extension not allowed";
					status = false;
				}
			} else {
				msg = "Maximum File size limit 5 MB";
				status = false;
			}
			responsejson.put("status", "Success");
			responsejson.put("data", status);
			responsejson.put("message", msg);
			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("uploadvendorFile() :", e.fillInStackTrace());

		}

		return jsonArray;
	}

	public JSONArray downloadfile(String filename, String typeofdownload) {
		boolean status = false;
		String encodedfile = "";

		try {
			prop.load(input);
			String path = null;
			if (typeofdownload.equalsIgnoreCase("singlepodownload")) {
				String[] fileName = filename.split("_");
				String timestamp = fileName[fileName.length - 1];
				int iend = timestamp.indexOf(".");

				if (iend != -1) {
					timestamp = timestamp.substring(0, iend);
				}
				StringBuffer downloadfilename = null;
				path = "grpbpofilesfordownload" + "//" + filename + ".pdf";
			} else if (typeofdownload.equalsIgnoreCase("listdownload")) {
				String[] fileName = filename.split("_");
				String timestamp = fileName[fileName.length - 1];
				int iend = timestamp.indexOf(".");
				if (iend != -1) {
					timestamp = timestamp.substring(0, iend);
				}
				StringBuffer downloadfilename = null;
				path = fileName[0] + "//" + fileName[1] + "//" + timestamp + "//" + filename;
				
			}	else if (typeofdownload.equalsIgnoreCase("ASN")) {
				String[] fileName = filename.split("_");
				String timestamp = fileName[fileName.length - 1];
				int iend = timestamp.indexOf(".");
				if (iend != -1) {
					timestamp = timestamp.substring(0, iend);
				}
				StringBuffer downloadfilename = null;
				path = "/ASN/" + fileName[0] + "//" + fileName[1] + "//" + timestamp + "//" + filename;
				//path = "/ASN/" + fileName[0] + "//" + timestamp + "//" + filename;
			} else if (typeofdownload.equalsIgnoreCase("multiplefiledownload")) {
				FileOutputStream fos = null;
				ZipOutputStream zipOut = null;
				FileInputStream fis = null;
				String savedfiles[] = filename.split(",");
				String firstname = savedfiles[0];
				String[] tempfileName = firstname.split("_");
				String temptimestamp = tempfileName[tempfileName.length - 1];
				int iend1 = temptimestamp.indexOf(".");
				if (iend1 != -1) {
					temptimestamp = temptimestamp.substring(0, iend1);
				}
				String zippath = tempfileName[0] + "//" + tempfileName[1] + "//" + temptimestamp + "//";
				String zipFolderName = prop.getProperty("fileLocation") + zippath + "supporting_documents.zip";
				path = zippath + "supporting_documents.zip";
				fos = new FileOutputStream(zipFolderName);
				zipOut = new ZipOutputStream(new BufferedOutputStream(fos));
				for (int a = 0; a < savedfiles.length; a++) {
					String tempname = savedfiles[a];
					String[] fileName = tempname.split("_");
					String timestamp = fileName[fileName.length - 1];
					int iend = timestamp.indexOf(".");
					if (iend != -1) {
						timestamp = timestamp.substring(0, iend);
					}
					StringBuffer downloadfilename = null;
					String fpath = fileName[0] + "//" + fileName[1] + "//" + timestamp + "//" + tempname;
					File input = new File(prop.getProperty("fileLocation") + fpath);
					fis = new FileInputStream(input);
					ZipEntry ze = new ZipEntry(input.getName());
					zipOut.putNextEntry(ze);
					byte[] tmp = new byte[4 * 1024];
					int size = 0;
					while ((size = fis.read(tmp)) != -1) {
						zipOut.write(tmp, 0, size);
					}
					zipOut.flush();
					fis.close();

				}
				zipOut.close();
			}

			try {
				File file = new File(prop.getProperty("fileLocation") + path);

				InputStream inputStream;
				inputStream = new FileInputStream(file);
				byte[] bytes1 = new byte[(int) file.length()];
				inputStream.read(bytes1);
				encodedfile = new String(Base64.encodeBase64(bytes1), "UTF-8");
				responsejson.put("reason", "none");
				responsejson.put("status", "Success");
				responsejson.put("message", "Success");
				responsejson.put("data", encodedfile);
				if (typeofdownload.equalsIgnoreCase("singlepodownload")) {
					responsejson.put("filename", filename + ".pdf");
				} else if (typeofdownload.equalsIgnoreCase("listdownload")) {
					responsejson.put("filename", filename);
				}
				inputStream.close();
			} catch (Exception e) {
				log.error("downloadfile()1 :", e.fillInStackTrace());

				responsejson.put("status", "Fail");
				responsejson.put("message", "Error while downloading");
				responsejson.put("reason", "File not present");

			}

		} catch (Exception e) {
			log.error("downloadfile()2 :", e.fillInStackTrace());

			responsejson.put("status", "Fail");
			responsejson.put("message", "Error while downloading");
			responsejson.put("reason", e.getLocalizedMessage());
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	public JSONArray Uploadmultiplefiles(List<FormDataBodyPart> files, List<FormDataContentDisposition> fileDetail,
			String id, String fileType) throws IOException, Exception {

		String msg = "";
		int maxSize = 0;
		int mb = 1024;
		int numberOfFileCount = 0;
		boolean status = false;
		OutputStream out = null;
		try {
			prop.load(input);
			numberOfFileCount = Integer.parseInt(prop.getProperty("uploadFileCountForSuppDoc"));
			maxSize = Integer.parseInt(prop.getProperty("uploadFileSizeForSuppDoc"));
			maxSize = maxSize * mb;

			FormDataBodyPart this_formDataBodyPartFile2 = files.get(0);
			ContentDisposition this_contentDispositionHeader2 = this_formDataBodyPartFile2.getContentDisposition();
			FormDataContentDisposition fileDetail4 = (FormDataContentDisposition) this_contentDispositionHeader2;
			String[] fileName2 = fileDetail4.getFileName().split("_");
			String variant = fileName2[0];
			int count = 0;
			if (files.size() <= numberOfFileCount || variant.equalsIgnoreCase("bulkupload")) {
				for (int j = 0; j < files.size(); j++) {

					String path = null;
					String Uploadfilepath = null;
					FormDataBodyPart this_formDataBodyPartFile = files.get(j);
					ContentDisposition this_contentDispositionHeader = this_formDataBodyPartFile
							.getContentDisposition();
					InputStream this_fileInputStream = this_formDataBodyPartFile.getValueAs(InputStream.class);
					FormDataContentDisposition fileDetail2 = (FormDataContentDisposition) this_contentDispositionHeader;
					String extension = "";
					if (fileDetail2.getSize() < maxSize) {

						int i = fileDetail2.getFileName().lastIndexOf('.');
						if (i > 0) {
							extension = fileDetail2.getFileName().substring(i + 1);
						}
						if (extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("jpg")
								|| extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("doc")
								|| extension.equalsIgnoreCase("docx") || extension.equalsIgnoreCase("xls")
								|| extension.equalsIgnoreCase("xlsx") || extension.equalsIgnoreCase("pdf")
								|| extension.equalsIgnoreCase("csv") || extension.equalsIgnoreCase("PDF")) {
							String[] fileName = fileDetail2.getFileName().split("_");
							String timestamp = fileName[fileName.length - 1];
							int iend = timestamp.indexOf(".");
							if (iend != -1) {
								timestamp = timestamp.substring(0, iend);
							}
							path = prop.getProperty("fileLocation") + fileName[0] + "/" + fileName[1] + "/" + timestamp;
							
							if(fileType.equalsIgnoreCase("ASN")) {
								path = prop.getProperty("fileLocation")+"/ASN/" + fileName[0] + "/" + fileName[1] + "/" + timestamp;	
								//path = prop.getProperty("fileLocation")+"/ASN/" + fileName[0] + "/" + timestamp;
							}
							
							Uploadfilepath = path + "/" + fileDetail2.getFileName();

							File file = new File(path);
							if (!file.exists()) {
								if (file.mkdirs()) {
								}
							}
							File filepath = new File(Uploadfilepath);
							out = new FileOutputStream(filepath);
							int read = 0;
							byte[] bytes = new byte[maxSize];
							while ((read = this_fileInputStream.read(bytes)) != -1) {
								out.write(bytes, 0, read);
								status = true;
							}
							out.flush();
							out.close();
							count++;
						} else {
							responsejson.put("status", "Fail");
							responsejson.put("message", "." + extension + " is not allowed.");
							break;
						}
					} else {
						responsejson.put("status", "Fail");
						responsejson.put("message", "Some File is greater than minimum size");
						out.flush();
						out.close();
						break;
					}
				}
			} else {
				responsejson.put("status", "Fail");
				responsejson.put("message", "The Maximum you can upload is " + numberOfFileCount + " Files.");
			}
			if (count == files.size()) {
				responsejson.put("status", "Success");
				responsejson.put("message", "Successfully uploaded");
			}
		} catch (Exception e) {
			log.error("Uploadmultiplefiles() :", e.fillInStackTrace());

			out.flush();
			out.close();
			responsejson.put("status", "Fail");
			responsejson.put("message", "Error while uploading");
		}
		jsonArray.add(responsejson);
		return jsonArray;
	}

	String finalPath = null;

	public StreamingOutput download(String filename) {
		finalPath = null;
		boolean status = false;
		String encodedfile = "";
		try {
			prop.load(input);
			String path = null;
			String[] fileName = filename.split("_");
			String timestamp = fileName[fileName.length - 1];
			int iend = timestamp.indexOf(".");
			if (iend != -1) {
				timestamp = timestamp.substring(0, iend);
			}
			StringBuffer downloadfilename = null;
			path = fileName[0] + "//" + fileName[1] + "//" + timestamp + "//" + filename;
			finalPath = path;
			File myFile = new File(prop.getProperty("fileLocation") + path);
			if (myFile.length() > 0) {
				log.info("FILE Length : " + myFile.length());
				StreamingOutput fileStream = new StreamingOutput() {
					@Override
					public void write(java.io.OutputStream output) {
						try {
							java.nio.file.Path path1 = Paths.get(prop.getProperty("fileLocation") + finalPath);
							byte[] data = Files.readAllBytes(path1);
							output.write(data);
							output.flush();
						} catch (Exception e) {
						}
					}
				};
				return fileStream;
			} else {
				log.info("FILE Length Zero : " + myFile.length());
				return null;
			}

		} catch (Exception e) {
			log.error("getVendorDetails() :", e.fillInStackTrace());

		}

		return null;
	}

	boolean fileNotFounFlag = false;

	public StreamingOutput downloadPo(String filename) {
		finalPath = null;
		try {
			prop.load(input);
			String path = null;
			String[] fileName = filename.split("_");
			String timestamp = fileName[fileName.length - 1];
			int iend = timestamp.indexOf(".");
			if (iend != -1) {
				timestamp = timestamp.substring(0, iend);
			}
			path = "grpbpofilesfordownload" + "//" + filename + ".pdf";
			finalPath = path;
			File myFile = new File(prop.getProperty("fileLocation") + path);
			if (myFile.length() > 0) {
				log.info("FILE Length : " + myFile.length());
				StreamingOutput fileStream = new StreamingOutput() {
					@Override
					public void write(java.io.OutputStream output) {
						try {
							java.nio.file.Path path1 = Paths.get(prop.getProperty("fileLocation") + finalPath);
							byte[] data = Files.readAllBytes(path1);
							output.write(data);
							output.flush();
						} catch (Exception e) {
							return;
						}
					}
				};
				return fileStream;
			} else {
				log.info("FILE Length Zero : " + myFile.length());
				return null;
			}

		} catch (Exception e) {
			log.error("downloadPo() :", e.fillInStackTrace());
			

			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public JSONArray UploadVendorFiles(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
			String id) {

		String extension = "";
		String msg = "";
		int MaxSize = 1024 * 5;
		boolean status = false;
		Random random = new Random();
		int no = random.nextInt(9 - 1 + 1) + 1;

		try {
			int i = fileDetail.getFileName().lastIndexOf('.');
			if (i > 0) {
				extension = fileDetail.getFileName().substring(i + 1);
			}
			prop.load(input);

			if (fileDetail.getSize() < MaxSize) {

				/*
				 * String[] fileName = id.split("@"); String filename = id + "_" + no +
				 * fileDetail.getFileName();
				 */
				String filename = fileDetail.getFileName();
				String Uploadfilepath = "";
				String path = "";

				Uploadfilepath = prop.getProperty("fileLocation") + filename;
				// path = Uploadfilepath + "/" + filename;

				File file = new File(path);
				if (!file.exists()) {
					if (file.mkdirs()) {
					}
				}
				if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("png")) {

					filename = fileDetail.getFileName();

					File filepath = new File(path);
					OutputStream out = new FileOutputStream(filepath);
					int read = 0;
					byte[] bytes = new byte[MaxSize];
					while ((read = uploadedInputStream.read(bytes)) != -1) {
						out.write(bytes, 0, read);
						status = true;
					}
					out.flush();
					out.close();
				} else {
					msg = "File Extension not allowed";
					status = false;
				}
			} else {
				msg = "Maximum File size limit 5 MB";
				status = false;
			}
			responsejson.put("status", "Success");
			responsejson.put("data", status);
			responsejson.put("message", msg);
			jsonArray.add(responsejson);

		} catch (IOException e) {
			log.error("UploadPortfolio() :", e.fillInStackTrace());

		}
		return jsonArray;

	}

	public InputStream downloadPopdf(String po) {
		InputStream is = null;
		try {
			prop.load(input);
			String GET_URL =  prop.getProperty("SAP_API_LINK")+"?PurchaseOrder='"+po+"'";
			String username =  prop.getProperty("SAP_USERNAME");
			String password =  prop.getProperty("SAP_PASSWORD");

			// Encode the credentials in base64
			String credentials = username + ":" + password;
			String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

			// Create the URL object
			URL url = new URL(GET_URL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			// Set up the request
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
			connection.setRequestProperty("Accept", "application/json");

			// Get the response
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					String inputLine;
					StringBuilder content = new StringBuilder();
					while ((inputLine = in.readLine()) != null) {
						content.append(inputLine);
					}
					org.json.JSONObject obj = new org.json.JSONObject(content.toString());
					String pdfbin = obj.getJSONObject("d").getJSONObject("GetPDF").getString("PurchaseOrderBinary").toString();
					String carg = pdfbin.replaceAll("\\n|\\r", "").trim();
					byte[] decodedBytes = java.util.Base64.getDecoder().decode(carg);
					is = new ByteArrayInputStream(decodedBytes);

				} catch (Exception e) {
					log.error("downloadPdf() :", e.fillInStackTrace());
				}

			}
			connection.disconnect();
		} catch (Exception e) {
			log.error("downloadPdf() :", e.fillInStackTrace());
		}
		return is;
	}
	
	public String downloadpdf(String po) {
		String carg = "";
		try {
			prop.load(input);
			String GET_URL =  prop.getProperty("SAP_API_LINK")+"?PurchaseOrder='"+po+"'";
			String username =  prop.getProperty("SAP_USERNAME");
			String password =  prop.getProperty("SAP_PASSWORD");

			// Encode the credentials in base64
			String credentials = username + ":" + password;
			String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

			// Create the URL object
			URL url = new URL(GET_URL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			// Set up the request
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
			connection.setRequestProperty("Accept", "application/json");

			// Get the response
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					String inputLine;
					StringBuilder content = new StringBuilder();
					while ((inputLine = in.readLine()) != null) {
						content.append(inputLine);
					}
					org.json.JSONObject obj = new org.json.JSONObject(content.toString());
					String pdfbin = obj.getJSONObject("d").getJSONObject("GetPDF").getString("PurchaseOrderBinary").toString();
					carg = pdfbin.replaceAll("\\n|\\r", "").trim();
				} catch (Exception e) {
					log.error("downloadPdf() :", e.fillInStackTrace());
				}
			}
			connection.disconnect();
		} catch (Exception e) {
			log.error("downloadPdf() :", e.fillInStackTrace());
		}
		return carg;
	}

	/*  commented on Vendor Registration on 28-03-2024
	
	@SuppressWarnings("unchecked")
	public JSONArray UploadVendorPortfolio(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
			String id) {

		String extension = "";
		String msg = "";
		int MaxSize = 1024 * 5;
		boolean status = false;
		Random random = new Random();
		int no = random.nextInt(9 - 1 + 1) + 1;

		try {
			int i = fileDetail.getFileName().lastIndexOf('.');
			if (i > 0) {
				extension = fileDetail.getFileName().substring(i + 1);
			}
			prop.load(input);

			if (fileDetail.getSize() < MaxSize) {
				
				 // String[] fileName = id.split("@"); String filename = id + "_" + no +fileDetail.getFileName();
				 
				String filename = fileDetail.getFileName();
				String Uploadfilepath = "";
				String path = "";

//				Uploadfilepath = prop.getProperty("vendorFileLoc") + fileName[0];
				Uploadfilepath = prop.getProperty("vendorFileLoc");// + filename;
				path = Uploadfilepath + "/" + filename;
				// path = Uploadfilepath;
				System.out.println("Uploadfilepath :" + Uploadfilepath);
				System.out.println("path :" + path);
				File file = new File(Uploadfilepath);
				System.out.println("file :" + file);
				if (!file.exists()) {
					if (file.mkdirs()) {
					}
				}
				if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("png")) {

					filename = fileDetail.getFileName();

					File filepath = new File(path);
					OutputStream out = new FileOutputStream(filepath);
					int read = 0;
					byte[] bytes = new byte[MaxSize];
					while ((read = uploadedInputStream.read(bytes)) != -1) {
						out.write(bytes, 0, read);
						status = true;
					}

					out.flush();
					out.close();
					// jsonArray = apiCall(filename);
					responsejson = apiCall(filename);
				} else {
					msg = "File Extension not allowed";
					status = false;
				}
			} else {
				msg = "Maximum File size limit 5 MB";
				status = false;
			}
			responsejson.put("status", "Success");
			responsejson.put("data", status);
			responsejson.put("message", msg);
			jsonArray.add(responsejson);

		} catch (IOException e) {
			log.error("UploadVendorPortfolio() :", e.fillInStackTrace());

		}
		return jsonArray;

	}

	public JSONArray uploadVendorProfile(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
			String id) {
		String extension = "";
		String msg = "";
		int MaxSize = 1024 * 5;
		boolean status = false;

		try {
			int i = fileDetail.getFileName().lastIndexOf('.');
			if (i > 0) {
				extension = fileDetail.getFileName().substring(i + 1);
			}
			prop.load(input);

			if (fileDetail.getSize() < MaxSize) {

				String[] fileName = fileDetail.getFileName().split("_");
				String ponumber = fileName[0];
				String type = fileName[1];
				String timestamp = fileName[fileName.length - 1];
				int iend = timestamp.indexOf(".");
				// String subString;
				if (iend != -1) {
					timestamp = timestamp.substring(0, iend);
				}

				String Uploadfilepath = "";

				String path = "";
				Uploadfilepath = prop.getProperty("vendorFileLoc") + "/" + ponumber + "/" + type + "/" + timestamp;
				path = Uploadfilepath + "/" + fileDetail.getFileName();

				File file = new File(Uploadfilepath);
				if (!file.exists()) {
					if (file.mkdirs()) {
					}
				}
				if (extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpg")) {

					File filepath = new File(path);
					OutputStream out = new FileOutputStream(filepath);
					int read = 0;
					byte[] bytes = new byte[MaxSize];
					while ((read = uploadedInputStream.read(bytes)) != -1) {
						out.write(bytes, 0, read);
						status = true;
					}

					out.flush();
					out.close();

					// jsonArray = apiCall(fileDetail.getFileName());
					responsejson = apiCall(fileDetail.getFileName());
				} else {
					msg = "File Extension not allowed";
					status = false;
				}
			} else {
				msg = "Maximum File size limit 5 MB";
				status = false;
			}
			responsejson.put("status", "Success");
			responsejson.put("data", status);
			responsejson.put("message", msg);
			jsonArray.add(responsejson);

		} catch (Exception e) {
			log.error("uploadVendorProfile() :", e.fillInStackTrace());
		}

		return jsonArray;
	}

	
	private void disableSslVerification() {
		try {

			X509TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			X509TrustManager[] trustAllCerts = new X509TrustManager[] { tm };

			// trustAllCerts;

			// };

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}

	public String callAuthAPI(String apiName, String methodType, String requestType, String reqContentType, String data,
			String fileName) throws RuntimeException {

		String apiUrl = baseAuthUrl + apiName + "?docName=" + fileName;

		StringBuilder strBuf = new StringBuilder();
		HttpURLConnection con = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(apiUrl);

			System.out.println("URL : " + url);
			con = (HttpURLConnection) url.openConnection();

			if (methodType != null && !"".equals(methodType)) {
				con.setRequestMethod(methodType);
			}
			if (requestType != null && !"".equals(requestType)) {
				con.setRequestProperty("Accept", requestType);
			}

			System.out.println("METHOD TYPE : " + methodType + " CONTENT TYPE : " + reqContentType + " DATA : " + data);
			if (methodType.equals("GET") && data != null) {
				System.out.println(
						"METHOD TYPE : " + methodType + " CONTENT TYPE : " + reqContentType + " DATA : " + data);
				con.setRequestProperty("Content-Type", reqContentType);
				con.setDoOutput(true);
				con.setDoInput(true);
				OutputStream os = con.getOutputStream();
				os.write(data.getBytes("UTF-8"));
				os.close();
			}
			if (con.getResponseCode() != 200) {
				System.out.println("Exception in callAuthAPI : HTTP " + methodType
						+ " Request Failed with Error code : " + con.getResponseCode());
				throw new RuntimeException(
						"HTTP " + methodType + " Request Failed with Error code : " + con.getResponseCode());
			}
			System.out.println("getResponseCode " + con.getResponseCode());

			reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
			System.out.println("reader " + reader);

			String output = null;
			while ((output = reader.readLine()) != null)
				strBuf.append(output);

		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println("Exception in callAPI 1: " + e);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Excep" + "tion in callAPI 2: " + e);
		} finally {

			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (con != null) {
				con.disconnect();
			}
		}

		return strBuf.toString();
	}

	public JSONObject apiCall(String fileName) {
		String response = null;
		JSONParser jsonParser = new JSONParser();
		JSONObject reqJsonObject = new JSONObject();
		JSONObject responseObject = null;
		JSONObject responseObject1 = null;
		JSONObject responseObject2 = null;
		JSONObject responseObject3 = null;
		String registrationNumber = "";
		String legalName = "";
		String tradeName = "";
		String constitutionOfBusiness = "";
		String addressOfPrincipalPlaceOf = "";
		String dateOfLiability = "";
		String dateOfValidityFrom = "";
		String dateOfValidityTo = "";
		String typeOfRegistration = "";
		try {

			response = callAuthAPI("auth", "GET", null, "application/json; charset=UTF-8", null, fileName);

			ArrayList<HashMap<String, String>> vendorList = new ArrayList<HashMap<String, String>>();

			HashMap<String, String> vendorDetails = new HashMap<String, String>();

			try {

				System.out.println("Response : " + response);
				log.info("Response : " + response);

				responseObject1 = (JSONObject) jsonParser.parse(response);
				String temp3 = responseObject1.get("result").toString();
				// JSONArray ja_data = (JSONArray) responseObject1.get("result");

				// System.out.println("temp3 : " + temp3);
				// log.info("temp3 : " + temp3);

				if (response.contains("PERMANENT ACCOUNT NUMBER")) {

					responseObject = (JSONObject) jsonParser.parse(response);
					String temp = responseObject.get("result").toString();
					responseObject1 = (JSONObject) jsonParser.parse(temp);
					JSONArray ja_data1 = (JSONArray) responseObject1.get("responses");

					JSONObject fullText = null;

					Iterator<JSONObject> iterator = ja_data1.iterator();
					while (iterator.hasNext()) {
						fullText = iterator.next();
						// System.out.println("fullText :"+fullText.toJSONString());
					}

					responseObject2 = (JSONObject) jsonParser.parse(fullText.toString());
					// System.out.println("fullTextAnnotation
					// :"+responseObject2.get("fullTextAnnotation"));
					String pages = responseObject2.get("fullTextAnnotation").toString();
					// System.out.println("pages :"+pages);
					responseObject3 = (JSONObject) jsonParser.parse(pages);

					String text = responseObject3.get("text").toString();
					System.out.println("PanCard Details :" + text);

					// Regular expressions for PAN, INAME, and Date of Incorporation/Formation
					String panRegex = "PERMANENT ACCOUNT NUMBER\\s*(\\w+)";
					String inameRegex = "NAME\\s*([\\p{L}0-9\\s]+)";
					String dateRegex = "DATE OF INCORPORATION/FORMATION\\s*(\\d{2}-\\d{2}-\\d{4})";

					// Extract PAN
					String pan = extractInformation(text, panRegex);
					System.out.println("PANNUMBER: " + pan);
					vendorDetails.put("PANNUMBER", pan);

					// Extract INAME
					String iname = extractInformation(text, inameRegex);
					System.out.println("NAME: " + iname);
					vendorDetails.put("NAME", iname);

					// Extract Date of Incorporation/Formation
					String date = extractInformation(text, dateRegex);
					System.out.println("DATE: " + date);
					vendorDetails.put("DATE", date);
					vendorList.add(vendorDetails);
					responsejson.put("PANCARD", vendorList);

					// jsonArray.add(responsejson);

				} else if (response.contains("IFSC") || response.contains("IFS Code")) {
					//responseObject = (JSONObject) jsonParser.parse(response);
					//String temp = responseObject.get("result").toString();
					// temp = temp.replaceAll("\\\\","");
					//System.out.println("temp : " + temp);
					responseObject1 = (JSONObject) jsonParser.parse(temp3);
					JSONArray ja_data3 = (JSONArray) responseObject1.get("responses");

					JSONObject fullText = null;
					Iterator<JSONObject> iterator = ja_data3.iterator();
					while (iterator.hasNext()) {
						fullText = iterator.next();
						// System.out.println("fullText :"+fullText.toJSONString());
					}

					responseObject2 = (JSONObject) jsonParser.parse(fullText.toJSONString());
					// System.out.println("fullTextAnnotation
					// :"+responseObject2.get("fullTextAnnotation"));
					String pages = responseObject2.get("fullTextAnnotation").toString();
					// System.out.println("pages :"+pages);
					responseObject3 = (JSONObject) jsonParser.parse(pages);

					String text = responseObject3.get("text").toString();
					System.out.println("Cheque Details :" + text);

					String chequeType = "";
					HashMap<String, String> bankHashMap = null;

					if (text.contains("ICICI Bank")) {
						bankHashMap = bankdetails("ICICI Bank", text);
					} else if (text.contains("Bank of India")) {
						bankHashMap = bankdetails("Bank of India", text);
					} else if (text.contains("Canara Bank")) {
						bankHashMap = bankdetails("Canara Bank", text);
					}

					vendorList.add(bankHashMap);
					responsejson.put("BankDetails", vendorList);
					// jsonArray.add(responsejson);

				} else {
					responseObject = (JSONObject) jsonParser.parse(response);
					String temp = responseObject.get("result").toString();
					// temp = temp.replaceAll("\\\\","");
					// System.out.println("temp : " + temp);
					responseObject1 = (JSONObject) jsonParser.parse(temp);
					JSONArray ja_data1 = (JSONArray) responseObject1.get("responses");

					JSONObject fullText = null;
					Iterator<JSONObject> iterator = ja_data1.iterator();
					while (iterator.hasNext()) {
						// fullText = iterator.next();
						// String row = (String) obj;
						fullText = iterator.next();
						System.out.println("-----------------------------------------------------------");
						// System.out.println(" jsonobject : "+ fullText.toString());

					}

					responseObject2 = (JSONObject) jsonParser.parse(fullText.toJSONString());
					// System.out.println("fullTextAnnotation
					// :"+responseObject2.get("fullTextAnnotation"));
					String pages = responseObject2.get("fullTextAnnotation").toString();
					//System.out.println("pages :" + pages);
					log.info("pages :" + pages);
					responseObject3 = (JSONObject) jsonParser.parse(pages);

					String row = responseObject3.get("text").toString();
					System.out.println("GST Details :" + row);
					log.info("GST Details :" + row);
					String col[] = row.split("\n");
					System.out.println("string array length :" + col.length);
					log.info("string array length :" + col.length);
					int i = 0;
					for (String lineItem : col) {
						i++;
						// System.out.println(" lineitem no:"+i++);
						if (i == 7) {
							System.out.println("GST Details inside :" + lineItem);
							registrationNumber = lineItem.substring(lineItem.indexOf("Registration Number :") + 21)
									.trim();
							System.out.println("registrationNumber :" + registrationNumber);
						} else if (i == 8) {
							legalName = lineItem.trim();
							System.out.println("legalName :" + legalName);
						} else if (i == 9) {
							tradeName = lineItem.trim();
							System.out.println("tradeName :" + tradeName);
						} else if (i == 10) {
							constitutionOfBusiness = lineItem.trim();
							System.out.println("constitutionOfBusiness :" + constitutionOfBusiness);
						} else if (i == 11) {
							addressOfPrincipalPlaceOf = lineItem.trim();
							System.out.println("addressOfPrincipalPlaceOf :" + addressOfPrincipalPlaceOf);
						} else if (i == 12) {
							addressOfPrincipalPlaceOf = addressOfPrincipalPlaceOf + " " + lineItem.trim();
							System.out.println("addressOfPrincipalPlaceOf2 :" + addressOfPrincipalPlaceOf);
						} else if (i == 24) {
							dateOfLiability = lineItem.trim();
							dateOfLiability = dateOfLiability.replaceAll("\\/", "-");
							System.out.println("dateOfLiability :" + dateOfLiability);
						} else if (i == 27) {
							dateOfValidityFrom = lineItem.trim();
							dateOfValidityFrom = dateOfValidityFrom.replaceAll("\\/", "-");
							System.out.println("dateOfValidityFrom :" + dateOfValidityFrom);
						} else if (i == 28) {
							dateOfValidityTo = lineItem.trim();
							dateOfValidityTo = dateOfValidityTo.replaceAll("\\/", "-");
							System.out.println("dateOfValidityTo :" + dateOfValidityTo);
						}else if (i == 33) {
							typeOfRegistration = lineItem.trim();
							System.out.println("typeOfRegistration :" + typeOfRegistration);
						} 
					}

					// Create a new JSONObject to store the extracted information
					// JSONObject extractedInfo = new JSONObject();
					vendorDetails.put("Registration_Number", registrationNumber);
					vendorDetails.put("Legal_Name", legalName);
					vendorDetails.put("Trade_Name", tradeName);
					vendorDetails.put("Constitution_of_Business", constitutionOfBusiness);
					vendorDetails.put("Address_of_Principal_Place_of", addressOfPrincipalPlaceOf);
					vendorDetails.put("Date_of_Liability", dateOfLiability);
					vendorDetails.put("Date_of_Validity_From", dateOfValidityFrom);
					vendorDetails.put("Date_of_Validity_To", dateOfValidityTo);
					vendorDetails.put("Type_of_Registration", typeOfRegistration);

					vendorList.add(vendorDetails);
					responsejson.put("GSTDetails", vendorList);
					// Print the extracted information
					System.out.println(responsejson.toString());

				}

			} catch (ParseException e) {
				e.printStackTrace();
				System.out.println("Exception in main : " + e);
			}

		} catch (RuntimeException e) {
			e.printStackTrace();
			System.out.println("Exception in call  : " + e);
		}

		// return jsonArray.add(responsejson);
		// return jsonArray;
		return responsejson;

	}

	public String extractInformation(String text, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return "Not Found";
		}
	}

	public HashMap<String, String> bankdetails(String chequeType, String text) {

		HashMap<String, String> bankDetails = new HashMap<String, String>();

		if ("Canara Bank".equalsIgnoreCase(chequeType)) {

			// Regular expressions for extracting information
			String accountNumberRegex = "\\b(\\d{14})\\b"; // Assuming account number is 15 digits
			String ifscRegex = "IFSC\\s(\\w{4}\\d{7})"; // IFSC format example: ABCD0123456
			String micrRegex = "\\b(\\d{10})\\b"; // Assuming MICR is 9 digits
			String bankNameRegex = "(Canara Bank)"; // Bank name in Hindi or English
			String bankBranchRegex = "Branch([\\p{L}0-9\\s,]+)\\n"; // Branch information

			// Extract account number
			String accountNumber = extractInformation(text, accountNumberRegex);
			System.out.println("AccountNumber: " + accountNumber);
			bankDetails.put("AccountNumber", accountNumber);

			// Extract IFSC code
			String ifscCode = extractInformation(text, ifscRegex);
			System.out.println("IFSCCode: " + ifscCode);
			bankDetails.put("IFSCCode", ifscCode);

			// Extract MICR
			String micr = extractInformation(text, micrRegex);
			System.out.println("MICR: " + micr);
			bankDetails.put("MICR", micr);

			// Extract bank name
			String bankName = extractInformation(text, bankNameRegex);
			System.out.println("BankName: " + bankName);
			bankDetails.put("BankName", bankName);

			// Extract bank branch
			String bankBranch = extractInformation(text, bankBranchRegex);
			System.out.println("BankBranch: " + bankBranch);
			bankDetails.put("BankBranch", bankBranch);

		} else if ("Bank of India".equalsIgnoreCase(chequeType)) {

			// Regular expressions for extracting information
			String accountNumberRegex = "\\b(\\d{15,})\\b"; // Assuming account number is 15 digits or more
			String ifscRegex = "IFSC:\\s(\\w{4}\\d{7})"; // IFSC format example: ABCD0123456
			String bankNameRegex = "(Bank of India)"; // Bank name in English or Hindi
			String bankBranchRegex = "BRANCH [\\p{L}0-9\\s,-]+?\\d{6}"; // Branch information

			// Extract account number
			String accountNumber = extractInformation(text, accountNumberRegex);
			System.out.println("AccountNumber: " + accountNumber);
			bankDetails.put("AccountNumber", accountNumber);
			// Extract IFSC code
			String ifscCode = extractInformation(text, ifscRegex);
			System.out.println("IFSCCode: " + ifscCode);
			bankDetails.put("IFSCCode", ifscCode);
			// Extract bank name
			String bankName = extractInformation(text, bankNameRegex);
			System.out.println("BankName: " + bankName);
			bankDetails.put("BankName", bankName);
			bankDetails.put("MICR", "");
			// Extract bank branch
			String bankBranch = extractInformation(text, bankBranchRegex);
			System.out.println("BankBranch: " + bankBranch);
			bankDetails.put("BankBranch", bankBranch);

		} else if ("ICICI Bank".equalsIgnoreCase(chequeType)) {

			// Regular expressions for extracting information
			String accountNumberRegex = "\\b(\\d{12,})\\b"; // Assuming account number is 15 digits or more
			String ifscRegex = "IFS Code:\\s(\\w{4}\\d{7})"; // IFSC format example: ABCD0123456
			String micrRegex = "\\b(\\d{9})\\b"; // Assuming MICR is 9 digits
			String bankNameRegex = "(ICICI Bank|आईसीआईसीआई बैंक)"; // Bank name in English or Hindi
			String bankBranchRegex = "Branch\\n([\\p{L}0-9\\s,-]+)\\n"; // Branch information

			// Extract account number
			String accountNumber = extractInformation(text, accountNumberRegex);
			System.out.println("AccountNumber: " + accountNumber);
			bankDetails.put("AccountNumber", accountNumber);
			// Extract IFSC code
			String ifscCode = extractInformation(text, ifscRegex);
			System.out.println("IFSCCode: " + ifscCode);
			bankDetails.put("IFSCCode", ifscCode);
			// Extract MICR
			String micr = extractInformation(text, micrRegex);
			System.out.println("MICR: " + micr);
			bankDetails.put("MICR", micr);
			// Extract bank name
			String bankName = extractInformation(text, bankNameRegex);
			System.out.println("BankName: " + bankName);
			bankDetails.put("BankName", bankName);

			// Extract bank branch
			String bankBranch = extractInformation(text, bankBranchRegex);
			System.out.println("BankBranch: " + bankBranch);
			bankDetails.put("BankBranch", bankBranch);

		} else {
			bankDetails.put("AccountNumber", "Not Found");
			bankDetails.put("IFSCCode", "Not Found");
			bankDetails.put("MICR", "Not Found");
			bankDetails.put("BankName", "Not Found");
			bankDetails.put("BankBranch", "Not Found");
		}
		return bankDetails;
	}

	public JSONArray insertVendorRegistration(List<VendorRegSubmission> vendors,String emailId) throws SQLException {

		Connection con = null;
		PreparedStatement ps = null;
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		ResultSet rs = null;
		String requesterId = null;
		String partnerOID = null;
		con = DBConnection.getConnection();
		con.setAutoCommit(false);
		String fromAddress =null;
		String buyerEmailId = null;

		try {
			
			String requesterQuery = " SELECT REQUESTEROID, BUYEREMAILID FROM REQUESTERDETAILS WHERE STATUS = ? and REQUESTEREMAILID = ? ";
			
			String requesterStr = " Update REQUESTERDETAILS SET GSTNO = ? , PINCODE = ? , PANNUMBER = ? , STATUS = ? WHERE REQUESTEROID = ? AND "
					+ " REQUESTEREMAILID = ? ";
			
			String generalQuery= " Insert into PARNTERGENERALDETAILS (PARTNEROID, REQUESTEROID, PANNUMBER, TITLE, PARTNERNAME, STREET1, STREET2, STREET3, " 
								+"STREET4, PINCODE, CITY, TELNO, MOBILENO, FAXNO, PRIMARYEMAILID, SECONDARYEMAILID, TERTIARYEMAILID, "
								+ "EMAILID4TH, EMAILID5TH, CREATEDBY, MODIFIEDBY ) Values( " 
								+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ";
			
			String partnerOidQuery = "SELECT PARTNER_SEQ.nextval as partnerOID From DUAL ";
			
			String bankingQuery = "INSERT INTO PARNTERBANKDETAILS (PARTNEROID, BANKACCNO, SWIFTCODE, IFSCCODE, GSTNO, MSMESTATUS, MSMENO, " 
								+ "TDSEXEMPTION, TDSEXEMPTIONPERCENT, TDSEXEMPTIONFROM, TDSEXEMPTIONTO, PFREGISTRATIONNO, ESIREGISTRATIONNO,  " 
								+" CREATEDBY, MODIFIEDBY ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) " ;
			
			String businessQuery = "INSERT INTO PARNTERBUSINESSDETAILS (PARTNEROID, TYPEOFBUSINESS, TYPEOFINDUSTRY, PROMOTERS, TURNOVER,  " 
							    + " TOP5CLIENT, CLIENTREFERENCE, CREATEDBY, MODIFIEDBY, TOP5CLIENT1, TOP5CLIENT2, TOP5CLIENT3, TOP5CLIENT4 ) " 
							    + " VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ";
			
			if(vendors != null) {
				
				ps = con.prepareStatement(requesterQuery);
				ps.setString(1, "I");
				ps.setString(2, emailId);
				rs = ps.executeQuery();
							
				while(rs.next()) {
					requesterId = rs.getString("REQUESTEROID");
					buyerEmailId = rs.getString("BUYEREMAILID");
				}
				rs.close();
				ps.close();
				if(requesterId !=null) {
					
					ps1 = con.prepareStatement(requesterStr);
					ps1.setString(1, vendors.get(0).getGstNumber());
					ps1.setString(2, vendors.get(0).getPinCode());
					ps1.setString(3, vendors.get(0).getPanNumber());
					ps1.setString(4, "P");
					ps1.setString(5, requesterId);
					ps1.setString(6, emailId);
					
					int updateFlag = ps1.executeUpdate();
					ps1.close();
					
					ps = con.prepareStatement(partnerOidQuery);
					rs = ps.executeQuery();
								
					while(rs.next()) {
						partnerOID = rs.getString("partnerOID");
					}
					rs.close();
					ps.close();
						
					ps1 = con.prepareStatement(generalQuery);
					ps1.setString(1, partnerOID);
					ps1.setString(2, requesterId);
					ps1.setString(3, vendors.get(0).getPanNumber());
					ps1.setString(4, vendors.get(0).getTitle());
					ps1.setString(5, vendors.get(0).getVendorNameL1() + " " + vendors.get(0).getVendorNameL2());
					ps1.setString(6, vendors.get(0).getStreetL1());
					ps1.setString(7, vendors.get(0).getStreetL2());
					ps1.setString(8, vendors.get(0).getStreetL3());
					ps1.setString(9, vendors.get(0).getStreetL4());
					ps1.setString(10, vendors.get(0).getPinCode());
					ps1.setString(11, vendors.get(0).getCity());
					ps1.setString(12, vendors.get(0).getTelePhoneNo());
					ps1.setString(13, vendors.get(0).getMobileNo());
					ps1.setString(14, vendors.get(0).getFaxNo());
					ps1.setString(15, vendors.get(0).getEmailId());
					ps1.setString(16, vendors.get(0).getEmailId1());
					ps1.setString(17, vendors.get(0).getEmailId2());
					ps1.setString(18, vendors.get(0).getEmailId3());
					ps1.setString(19, vendors.get(0).getEmailId4());
					ps1.setString(20, emailId);
					ps1.setString(21, emailId);
					rs = ps1.executeQuery();
					
					ps1.close();
					rs.close();
					
					ps = con.prepareStatement(bankingQuery);
					ps.setString(1, partnerOID);
					ps.setString(2, vendors.get(0).getBankAcNo());
					ps.setString(3, vendors.get(0).getSwiftCode());
					ps.setString(4, vendors.get(0).getIfscCode());
					ps.setString(5, vendors.get(0).getGstNumber());
					ps.setString(6, vendors.get(0).getMsmeStatus());
					ps.setString(7, vendors.get(0).getMsmeNo());
					ps.setString(8, vendors.get(0).getTdsExemptionNo());
					ps.setString(9, vendors.get(0).getTdsExemptionPercentage());
					ps.setString(10, vendors.get(0).getTdsExemptionFrom());
					ps.setString(11, vendors.get(0).getTdsExemptionTo());
					ps.setString(12, vendors.get(0).getPfRegistrationNo());
					ps.setString(13, vendors.get(0).getEsiRegistrationNo());
					ps.setString(14, emailId);
					ps.setString(15, emailId);				
					rs = ps.executeQuery();
					
					ps.close();
					rs.close();
					
					ps1 = con.prepareStatement(businessQuery);
					ps1.setString(1, partnerOID);
					ps1.setString(2, vendors.get(0).getTypeOfBusiness());
					ps1.setString(3, vendors.get(0).getTypeOfIndustory());
					ps1.setString(4, vendors.get(0).getPromoters());
					ps1.setString(5, vendors.get(0).getTurnOver());
					ps1.setString(6, vendors.get(0).getTop5Client());
					ps1.setString(7, vendors.get(0).getClientsReferences());
					ps1.setString(8, emailId);
					ps1.setString(9, emailId);
					ps1.setString(10, vendors.get(0).getTop5Client1());
					ps1.setString(11, vendors.get(0).getTop5Client2());
					ps1.setString(12, vendors.get(0).getTop5Client3());
					ps1.setString(13, vendors.get(0).getTop5Client4());
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
						log.error("insertVendorRegistration properties error () :", e.fillInStackTrace());
					}
					
					String message = "<p>Dear Valued Partner,</p>" 
							+ "<p>Your vendor registration request has been generated and your request number is "+requesterId+".</p>"
							+ "<p>Regards,</p>" + "<p>BCCL PartnerDx Team</p>";
				
					Hashtable<String, String> hashTable = new Hashtable<String, String>();
					hashTable.put("fromAddr", fromAddress);
					hashTable.put("toAddr", emailId);
					hashTable.put("ccAddr", buyerEmailId);					
					hashTable.put("subject", "Vendor Registration Request is Generated.");
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
				jsonArray.add(responsejson);
			}				

		} catch (Exception e) {
			log.error("insertVendorRegistration() :", e.fillInStackTrace());
			System.out.println("insertVendorRegistration() :"+ e.fillInStackTrace());			
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
