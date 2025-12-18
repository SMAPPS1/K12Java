package com.bccl.dxapi.apiutility;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PanCardExtractor {
	
	static JSONObject responseObject = null;
	static JSONObject responseObject1 = null;
	static JSONObject responseObject2 = null;
	static JSONObject responseObject3 = null;
	static String response = null;
	static JSONParser jsonParser = new JSONParser();
	JSONObject reqJsonObject = new JSONObject();
	 

    public static void main(String[] args) {
    	
    	 String registrationNumber = "";
         String legalName = "";
         String tradeName = "";
         String constitutionOfBusiness = "";
         String addressOfPrincipalPlaceOf = "";
         String dateOfLiability = "";
         String dateOfValidityFrom = "";
         String dateOfValidityTo = "";
         String typeOfRegistration = "";
    	
        //String jsonString = "{\"rowData\":[\"Pan\",\"/ PERMANENT\",\"ACCOUNT\",\"NUMBER\",\"AACCP8008Q\",\"/ NAME FE\",\"POLO GIFTS\",\"CREATIONS\",\"PRIVATE\",\"LIMITED\",\"ANH /\",\"/ DATE OF\",\"INCORPORATION\",\"/ FORMATION\",\"06-08-2002\",\"Lea\",\"Briche 34th\",\"( chole cha)\",\"Commissioner\",\"of Income\",\"- tax ( Computer\",\"Operations )\"],\"result\":{\"responses\":[{\"textAnnotations\":[{\"description\":\"Pan/PERMANENT ACCOUNT NUMBER\",\"boundingPoly\":{\"vertices\":[{\"x\":128,\"y\":47},{\"x\":1061,\"y\":47},{\"x\":1061,\"y\":741},{\"x\":128,\"y\":741}]}}]}]}}";
        String jsonString = "{ "
        		+ "  \"rowData\": [ "
        		+ "    \"( Amended )\", "
        		+ "    \"me t\", "
        		+ "    \"Government of India\", "
        		+ "    \"Form GST REG - 06\", "
        		+ "    \"[ See Rule 10 ( 1 ) ]\", "
        		+ "    \"Registration Certificate\", "
        		+ "    \"Registration Number : 07AAACJ2412Q1ZI\", "
        		+ "    \"1 . Legal Name JHAROKHA SOFT FURNISHINGS PVT LTD\", "
        		+ "    \"2 . Trade Name , if any JHAROKHA SOFT FURNISHING PVT LTD\", "
        		+ "    \"3 . Constitution of Business Private Limited Company\", "
        		+ "    \"4 . Address of Principal Place of 16 , COMMUNITY CENTER , EAST OF KAILASH , NEW DELHI , South\", "
        		+ "    \"Business Delhi , Delhi , 110065\", "
        		+ "    \"5 . Date of Liability 01/07/2017\", "
        		+ "    \"6 . Date of Validity From 23/09/2017 To NA\", "
        		+ "    \"7 . Type of Registration\", "
        		+ "    \"Regular\", "
        		+ "    \"8 . Particulars of Approving Authority\", "
        		+ "    \"Signature\", "
        		+ "    \"Name\", "
        		+ "    \"Designation\", "
        		+ "    \"Office\" "
        		+ "  ], "
        		+ "  \"result\": { "
        		+ "    \"responses\": [ "
        		+ "      { "
        		+ "        \"textAnnotations\": [ "
        		+ "          { "
        		+ "            \"locale\": \"en\", "
        		+ "            \"description\": \"1.\\n2.\\n3.\\nRegistration Number :07AAACJ2412Q1ZI\\n4.\\n5.\\n6.\\n7.\\n8.\\nName\\nLegal Name\\nTrade Name, if any\\nConstitution of Business\\nAddress of Principal Place of\\nBusiness\\nDate of Liability\\nDate of Validity\\nType of Registration\\nDesignation\\nOffice\\nGovernment of India\\nForm GST REG-06\\n[See Rule 10(1)]\\nme t\\nRegistration Certificate\\nParticulars of Approving Authority\\nJHAROKHA SOFT FURNISHINGS PVT LTD\\nJHAROKHA SOFT FURNISHING PVT LTD\\nRegular\\nPrivate Limited Company\\n16, COMMUNITY CENTER, EAST OF KAILASH, NEW DELHI, South\\nDelhi, Delhi, 110065\\n01/07/2017\\nFrom\\n23/09/2017\\nSignature\\nTo\\n(Amended)\\nNA\", "
        		+ "            \"boundingPoly\": { "
        		+ "            } "
        		+ "          } "
        		+ "                    "
        		+ "           "
        		+ " "
        		+ "         ], "
        		+ "        \"fullTextAnnotation\": { "
        		+ "          \"pages\": [ "
        		+ "             "
        		+ "             "
        		+ "          ], "
        		+ "          \"text\": \"1.\\n2.\\n3.\\nRegistration Number :07AAACJ2412Q1ZI\\n4.\\n5.\\n6.\\n7.\\n8.\\nName\\nLegal Name\\nTrade Name, if any\\nConstitution of Business\\nAddress of Principal Place of\\nBusiness\\nDate of Liability\\nDate of Validity\\nType of Registration\\nDesignation\\nOffice\\nGovernment of India\\nForm GST REG-06\\n[See Rule 10(1)]\\nme t\\nRegistration Certificate\\nParticulars of Approving Authority\\nJHAROKHA SOFT FURNISHINGS PVT LTD\\nJHAROKHA SOFT FURNISHING PVT LTD\\nRegular\\nPrivate Limited Company\\n16, COMMUNITY CENTER, EAST OF KAILASH, NEW DELHI, South\\nDelhi, Delhi, 110065\\n01/07/2017\\nFrom\\n23/09/2017\\nSignature\\nTo\\n(Amended)\\nNA\" "
        		+ "        } "
        		+ "      } "
        		+ "    ] "
        		+ "  } "
        		+ "}";
        
        try {
        	responseObject = (JSONObject) jsonParser.parse(jsonString);
			String temp = responseObject.get("result").toString();
			// temp = temp.replaceAll("\\\\","");
			// System.out.println("temp : " + temp);
			responseObject1 = (JSONObject) jsonParser.parse(temp);
			JSONArray ja_data1 = (JSONArray) responseObject1.get("responses");
			
			JSONObject fullText = null;
			Iterator<JSONObject> iterator = ja_data1.iterator();
				while (iterator.hasNext()) {
					//fullText = iterator.next();
		            //String row = (String) obj;
					fullText = iterator.next();
					System.out.println("-----------------------------------------------------------");
				//	System.out.println(" jsonobject : "+ fullText.toString());
					
		        }
		        
				responseObject2 = (JSONObject) jsonParser.parse(fullText.toJSONString());
				// System.out.println("fullTextAnnotation :"+responseObject2.get("fullTextAnnotation"));
				String pages = responseObject2.get("fullTextAnnotation").toString();
				// System.out.println("pages :"+pages);
				responseObject3 = (JSONObject) jsonParser.parse(pages);

				String row = responseObject3.get("text").toString();
				//System.out.println("GST Details :" + row);
				String col[] = row.split("\n");
				//System.out.println("string array length :" + col.length);
				int i=0;
				for (String lineItem : col) {
										i++;
					//System.out.println(" lineitem no:"+i++);
					if (i==4) {
						System.out.println("GST Details :" + lineItem);
						registrationNumber = lineItem.substring(lineItem.indexOf("Registration Number :") + 21).trim();
		                System.out.println("registrationNumber :" + registrationNumber);
		            } else if (i==27) {
		                legalName = lineItem.trim();
		                System.out.println("legalName :" + legalName);
		            } else if (i==28) {
		                tradeName = lineItem.trim();
		                System.out.println("tradeName :" + tradeName);
		            } else if (i==30) {
		                constitutionOfBusiness = lineItem.trim();
		                System.out.println("constitutionOfBusiness :" + constitutionOfBusiness);
		            } else if (i==31) {
		                addressOfPrincipalPlaceOf = lineItem.trim();
		                System.out.println("addressOfPrincipalPlaceOf :" + addressOfPrincipalPlaceOf);
		            } else if (i==32) {
		            	addressOfPrincipalPlaceOf = addressOfPrincipalPlaceOf + " " +lineItem.trim();
		                System.out.println("addressOfPrincipalPlaceOf2 :" + addressOfPrincipalPlaceOf);
		            } else if (i==33) {
		            	dateOfLiability = lineItem.trim();
		            	dateOfLiability = dateOfLiability.replaceAll("\\/", "-");
		                System.out.println("dateOfLiability :" + dateOfLiability);
		            } else if (i==35) {
		                dateOfValidityFrom = lineItem.trim();
		                dateOfValidityFrom = dateOfValidityFrom.replaceAll("\\/", "-");
		                System.out.println("dateOfValidityFrom :" + dateOfValidityFrom);
		            } else if (i==39) {
		            	dateOfValidityTo = lineItem.trim();
		            	dateOfValidityTo = dateOfValidityTo.replaceAll("\\/", "-");
		                System.out.println("dateOfValidityTo :" + dateOfValidityTo);
		            } else if (i==29) {
		                typeOfRegistration = lineItem.trim();
		                System.out.println("typeOfRegistration :" + typeOfRegistration);
		            }
				}
				
		        // Create a new JSONObject to store the extracted information
		        JSONObject extractedInfo = new JSONObject();
		        extractedInfo.put("Registration Number", registrationNumber);
		        extractedInfo.put("Legal Name", legalName);
		        extractedInfo.put("Trade Name", tradeName);
		        extractedInfo.put("Constitution of Business", constitutionOfBusiness);
		        extractedInfo.put("Address of Principal Place of", addressOfPrincipalPlaceOf);
		        extractedInfo.put("Date of Liability", dateOfLiability);
		        extractedInfo.put("Date of Validity From", dateOfValidityFrom);
		        extractedInfo.put("Type of Registration", typeOfRegistration);
		        
		        // Print the extracted information
		        System.out.println(extractedInfo.toString());
		        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String extractInformation(String text, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return "Not Found";
		}
	}
}

