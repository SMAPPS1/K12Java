package com.bccl.dxapi.apiutility;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONExtractor {
    public static void main(String[] args) {
        String jsonString = "{\"rowData\":[\"( Amended )\",\"me t\",\"Government of India\",\"Form GST REG - 06\",\" See Rule 10 ( 1 ) \",\"Registration Certificate\",\"Registration Number : 07AAACJ2412Q1ZI\",\"1 . Legal Name JHAROKHA SOFT FURNISHINGS PVT LTD\",\"2 . Trade Name , if any JHAROKHA SOFT FURNISHING PVT LTD\",\"3 . Constitution of Business Private Limited Company\",\"4 . Address of Principal Place of 16 , COMMUNITY CENTER , EAST OF KAILASH , NEW DELHI , South\",\"Business Delhi , Delhi , 110065\",\"5 . Date of Liability 01/07/2017\",\"6 . Date of Validity From 23/09/2017 To NA\",\"7 . Type of Registration\",\"Regular\",\"8 . Particulars of Approving Authority\",\"Signature\",\"Name\",\"Designation\",\"Office\"],\"result\":{\"responses\":[{\"textAnnotations\":[{\"locale\":\"en\",\"description\":\"1.\\n2.\\n3.\\nRegistration Number :07AAACJ2412Q1ZI\\n4.\\n5.\\n6.\\n7.\\n8.\\nName\\nLegal Name\\nTrade Name, if any\\nConstitution of Business\\nAddress of Principal Place of\\nBusiness\\nDate of Liability\\nDate of Validity\\nType of Registration\\nDesignation\\nOffice\\nGovernment of India\\nForm GST REG-06\\n[See Rule 10(1)]\\nme t\\nRegistration Certificate\\nParticulars of Approving Authority\\nJHAROKHA SOFT FURNISHINGS PVT LTD\\nJHAROKHA SOFT FURNISHING PVT LTD\\nRegular\\nPrivate Limited Company\\n16, COMMUNITY CENTER, EAST OF KAILASH, NEW DELHI, South\\nDelhi, Delhi, 110065\\n01/07/2017\\nFrom\\n23/09/2017\\nSignature\\nTo\\n(Amended)\\nNA\",\"boundingPoly\":{}}],\"fullTextAnnotation\":{\"pages\":[],\"text\":\"1.\\n2.\\n3.\\nRegistration Number :07AAACJ2412Q1ZI\\n4.\\n5.\\n6.\\n7.\\n8.\\nName\\nLegal Name\\nTrade Name, if any\\nConstitution of Business\\nAddress of Principal Place of\\nBusiness\\nDate of Liability\\nDate of Validity\\nType of Registration\\nDesignation\\nOffice\\nGovernment of India\\nForm GST REG-06\\nSee Rule 10(1)\\nme t\\nRegistration Certificate\\nParticulars of Approving Authority\\nJHAROKHA SOFT FURNISHINGS PVT LTD\\nJHAROKHA SOFT FURNISHING PVT LTD\\nRegular\\nPrivate Limited Company\\n16, COMMUNITY CENTER, EAST OF KAILASH, NEW DELHI, South\\nDelhi, Delhi, 110065\\n01/07/2017\\nFrom\\n23/09/2017\\nSignature\\nTo\\n(Amended)\\nNA\"}}}]}}";
        
        // Parse JSON string to JSONObject
        JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(jsonString);
		
        // Get the "rowData" array
        JSONArray rowDataArray = jsonObject.getJSONArray("rowData");
        
        // Initialize variables to store extracted information
        String registrationNumber = "";
        String legalName = "";
        String tradeName = "";
        String constitutionOfBusiness = "";
        String addressOfPrincipalPlaceOf = "";
        String dateOfLiability = "";
        String dateOfValidityFrom = "";
        String typeOfRegistration = "";
        
        // Iterate through the "rowData" array to extract information
        //for (Object obj : rowDataArray) {
        int len = rowDataArray.length();
        System.out.println("len-----------"+len);
        /*
        Iterator<JSONObject> iterator = rowDataArray.iterator();
		while (iterator.hasNext()) {
			//fullText = iterator.next();
            //String row = (String) obj;
			String row = iterator.next();
            if (row.startsWith("Registration Number")) {
                registrationNumber = row.split(":")[1].trim();
            } else if (row.startsWith("1 . Legal Name")) {
                legalName = row.substring(row.indexOf("Legal Name") + 10).trim();
            } else if (row.startsWith("2 . Trade Name")) {
                tradeName = row.substring(row.indexOf("Trade Name") + 10, row.indexOf(", if any")).trim();
            } else if (row.startsWith("3 . Constitution of Business")) {
                constitutionOfBusiness = row.substring(row.indexOf("Constitution of Business") + 25).trim();
            } else if (row.startsWith("4 . Address of Principal Place of")) {
                addressOfPrincipalPlaceOf = row.substring(row.indexOf("Address of Principal Place of") + 31).trim();
            } else if (row.startsWith("5 . Date of Liability")) {
                dateOfLiability = row.substring(row.indexOf("Date of Liability") + 20).trim();
            } else if (row.startsWith("6 . Date of Validity From")) {
                dateOfValidityFrom = row.substring(row.indexOf("Date of Validity From") + 23, row.indexOf("To")).trim();
            } else if (row.startsWith("7 . Type of Registration")) {
                typeOfRegistration = row.substring(row.indexOf("Type of Registration") + 23).trim();
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
        */
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
}

