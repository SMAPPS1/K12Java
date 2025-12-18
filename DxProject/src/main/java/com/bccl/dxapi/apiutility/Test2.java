package com.bccl.dxapi.apiutility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Test2 {

	private static final String baseAuthUrl = "http://localhost:3001/api/";
	private static final int BUFFER_SIZE = 5120;

	static {
		disableSslVerification();
	}

	private static void disableSslVerification() {
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

	public static String callAuthAPI(String apiName, String methodType, String requestType, String reqContentType,
			String data) throws RuntimeException {

		String apiUrl = baseAuthUrl + apiName;

		StringBuilder strBuf = new StringBuilder();
		HttpURLConnection con = null;
		BufferedReader reader = null;
		try {
			URL url = new URL(apiUrl);
			;
			System.out.println("URL : " + url);
			con = (HttpURLConnection) url.openConnection();

			if (methodType != null && !"".equals(methodType)) {
				con.setRequestMethod(methodType);
			}
			if (requestType != null && !"".equals(requestType)) {
				con.setRequestProperty("Accept", requestType);
			}
			if (methodType.equals("GET") && data != null) {
				System.out.println(
						"METHOD TYEP : " + methodType + " CONTENT TYPE : " + reqContentType + " DATA : " + data);
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
			System.out.println("Exception in callAPI 2: " + e);
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

	public static void main(String[] args) throws IOException {
		  
		  String response = null; 
		  JSONParser jsonParser=new JSONParser(); 
		  JSONObject reqJsonObject=new JSONObject(); 
		  JSONObject responseObject = null; 
		  JSONObject responseObject1 = null; 
		  JSONObject responseObject2 = null; 
		  JSONObject responseObject3 = null; 
		  try { 
			  response = callAuthAPI("auth", "GET", null, "application/json; charset=UTF-8", null); 
			  
		  try {
		  
			  System.out.println("Response : " + response);
			  // String jsonReplace = response.replaceAll("\\\\","");
			  
			  if(response.contains("PERMANENT ACCOUNT NUMBER")) {
				  
				  responseObject=(JSONObject) jsonParser.parse(response); 
				  String temp = responseObject.get("data").toString(); 
				  // temp = temp.replaceAll("\\\\","");
				 //System.out.println("temp : " + temp); 
				 responseObject1=(JSONObject) jsonParser.parse(temp); 
				 JSONArray ja_data = (JSONArray) responseObject1.get("responses");
				 
				  JSONObject fullText = null; 
				  Iterator<JSONObject> iterator = ja_data.iterator(); 
				 while (iterator.hasNext()) {
			    	  fullText = iterator.next(); 
				//	 System.out.println("fullText :"+fullText.toJSONString());
			 	 }
				 
				 responseObject2=(JSONObject) jsonParser.parse(fullText.toJSONString()); 
				// System.out.println("fullTextAnnotation :"+responseObject2.get("fullTextAnnotation"));
				 String pages =  responseObject2.get("fullTextAnnotation").toString();
				// System.out.println("pages :"+pages);
				 responseObject3=(JSONObject) jsonParser.parse(pages); 
				 
				 String text =  responseObject3.get("text").toString();
				 System.out.println("PanCard Details :"+text);
			
				  
			  }else if(response.contains("IFSC") || response.contains("IFS Code")) {
				  responseObject=(JSONObject) jsonParser.parse(response); 
				  String temp = responseObject.get("data").toString(); 
				  // temp = temp.replaceAll("\\\\","");
				 //System.out.println("temp : " + temp); 
				 responseObject1=(JSONObject) jsonParser.parse(temp); 
				 JSONArray ja_data = (JSONArray) responseObject1.get("responses");
				 
				  JSONObject fullText = null; 
				  Iterator<JSONObject> iterator = ja_data.iterator(); 
				 while (iterator.hasNext()) {
			    	  fullText = iterator.next(); 
				//	 System.out.println("fullText :"+fullText.toJSONString());
			 	 }
				 
				 responseObject2=(JSONObject) jsonParser.parse(fullText.toJSONString()); 
				// System.out.println("fullTextAnnotation :"+responseObject2.get("fullTextAnnotation"));
				 String pages =  responseObject2.get("fullTextAnnotation").toString();
				// System.out.println("pages :"+pages);
				 responseObject3=(JSONObject) jsonParser.parse(pages); 
				 
				 String text =  responseObject3.get("text").toString();
				 System.out.println("Cheque Details :"+text);
			  
			  }else {
				  responseObject=(JSONObject) jsonParser.parse(response); 
				  String temp = responseObject.get("data").toString(); 
				  // temp = temp.replaceAll("\\\\","");
				 //System.out.println("temp : " + temp); 
				 responseObject1=(JSONObject) jsonParser.parse(temp); 
				 JSONArray ja_data = (JSONArray) responseObject1.get("responses");
				 
				  JSONObject fullText = null; 
				  Iterator<JSONObject> iterator = ja_data.iterator(); 
				 while (iterator.hasNext()) {
			    	  fullText = iterator.next(); 
				//	 System.out.println("fullText :"+fullText.toJSONString());
			 	 }
				 
				 responseObject2=(JSONObject) jsonParser.parse(fullText.toJSONString()); 
				// System.out.println("fullTextAnnotation :"+responseObject2.get("fullTextAnnotation"));
				 String pages =  responseObject2.get("fullTextAnnotation").toString();
				// System.out.println("pages :"+pages);
				 responseObject3=(JSONObject) jsonParser.parse(pages); 
				 
				 String text =  responseObject3.get("text").toString();
				 System.out.println("Cheque Details :"+text);
			  }
			 	 
		 } catch (ParseException e) { e.printStackTrace();
		 System.out.println("Exception in main : " + e); }
		 
		 } catch (RuntimeException e) { e.printStackTrace();
		 System.out.println("Exception in call  : " + e); }
		 
		 }
		 
}
		 
	
	
	
/*
	public static void main(String[] args) {	
	
	  
	        String jsonData = null;
			JSONParser jsonParser=new JSONParser();
			JSONObject reqJsonObject=new JSONObject();
			JSONObject responseObject = null;
			try {
				jsonData = callAuthAPI("auth", "GET", null, "application/json; charset=UTF-8", null);
				System.out.println("response 0 : " + jsonData);
	        
	        
	            ObjectMapper objectMapper = new ObjectMapper();
	            JsonNode jsonNode = objectMapper.readTree(jsonData);

	            // Extracting specific fields from JSON
	            JsonNode dataNode = jsonNode.get("data");
	            JsonNode responsesNode = dataNode.get("responses");
	            
	            // Accessing textAnnotations array
	            for (JsonNode response : responsesNode) {
	                JsonNode textAnnotations = response.get("textAnnotations");
	                for (JsonNode annotation : textAnnotations) {
	                    String description = annotation.get("description").asText();
	                    System.out.println("Description: " + description);
	                }
	            }

	            // Accessing fullTextAnnotation text
	            JsonNode fullTextAnnotation = responsesNode.get(0).get("fullTextAnnotation");
	            String fullText = fullTextAnnotation.get("text").asText();
	            System.out.println("\nFull Text: " + fullText);

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    
    }
	*/

