package com.bccl.dxapi.apiutility;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class PanCardExtractor2 {
		
		static JSONObject responseObject = null;
		static JSONObject responseObject1 = null;
		static JSONObject responseObject2 = null;
		static JSONObject responseObject3 = null;
		static String response = null;
		static JSONParser jsonParser = new JSONParser();
		JSONObject reqJsonObject = new JSONObject();

	    public static void main(String[] args) {
	    	 String jsonString = "{ \"rowData\": [\"( Amended )\",\"me t\",\"Government of India\",\"Form GST REG - 06\",\"[ See Rule 10 ( 1 ) ]\",\"Registration Certificate\",\"Registration Number : 07AAACJ2412Q1ZI\",\"1 . Legal Name JHAROKHA SOFT FURNISHINGS PVT LTD\",\"2 . Trade Name , if any JHAROKHA SOFT FURNISHING PVT LTD\",\"3 . Constitution of Business Private Limited Company\",\"4 . Address of Principal Place of 16 , COMMUNITY CENTER , EAST OF KAILASH , NEW DELHI , South\",\"Business Delhi , Delhi , 110065\",\"5 . Date of Liability 01/07/2017\",\"6 . Date of Validity From 23/09/2017 To NA\",\"7 . Type of Registration\",\"Regular\",\"8 . Particulars of Approving Authority\",\"Signature\",\"Name\",\"Designation\",\"Office\"]], "
     				+"\"result\": { \"responses\": [ { \"textAnnotations\": [ { \"locale\":\"en\",\"description\":\"1.\n2.\n3.\nRegistration Number :07AAACJ2412Q1ZI\n4.\n5.\n6.\n7.\n8.\nName\nLegal Name\nTrade Name, if any\nConstitution of Business\nAddress of Principal Place of\nBusiness\nDate of Liability\nDate of Validity\nType of Registration\nDesignation\nOffice\nGovernment of India\nForm GST REG-06\n[See Rule 10(1)]\nme t\nRegistration Certificate\nParticulars of Approving Authority\nJHAROKHA SOFT FURNISHINGS PVT LTD\nJHAROKHA SOFT FURNISHING PVT LTD\nRegular\nPrivate Limited Company\n16, COMMUNITY CENTER, EAST OF KAILASH, NEW DELHI, South\nDelhi, Delhi, 110065\n01/07/2017\nFrom\n23/09/2017\nSignature\nTo\n(Amended)\nNA\",\"boundingPoly\":{\"vertices\":[{\"x\":70,\"y\":15},{\"x\":594,\"y\":15},{\"x\":594,\"y\":610},{\"x\":70,\"y\":610}]}}, { \"description\": \"Pan\", \"boundingPoly\": "
     				+"{ \"vertices\": [ { \"x\": 141, \"y\": 47 }, { \"x\": 362, \"y\": 55 }, { \"x\": 361, \"y\": 86 }, { \"x\": 140, \"y\": 78 } ] } }, "
     				+"{ \"description\": \"/\", \"boundingPoly\": { \"vertices\": [ { \"x\": 377, \"y\": 56 }, { \"x\": 392, \"y\": 57 }, { \"x\": 391, \"y\": 87 },"
     				+" { \"x\": 376, \"y\": 86 } ] } }, { \"description\": \"PERMANENT\", \"boundingPoly\": { \"vertices\": [ { \"x\": 391, \"y\": 56 }, "
     				+"{ \"x\": 597, \"y\": 64 }, { \"x\": 596, \"y\": 95 }, { \"x\": 390, \"y\": 87 } ] } }, { \"description\": \"ACCOUNT\", \"boundingPoly\": "
     				+"{ \"vertices\": [ { \"x\": 608, \"y\": 64 }, { \"x\": 767, \"y\": 70 }, { \"x\": 766, \"y\": 101 }, { \"x\": 607, \"y\": 95 } ] } }, "
     				+"{ \"description\": \"NUMBER\", \"boundingPoly\": { \"vertices\": [ { \"x\": 779, \"y\": 70 }, { \"x\": 922, \"y\": 75 },"
     				+" { \"x\": 921, \"y\": 106 }, { \"x\": 778, \"y\": 101 } ] } }, { \"description\": \"AACCP8008Q\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 541, \"y\": 106 }, { \"x\": 798, \"y\": 114 }, { \"x\": 797, \"y\": 146 }, { \"x\": 540, \"y\": 138 } ] } }, "
     				+"{ \"description\": \"/\", \"boundingPoly\": { \"vertices\": [ { \"x\": 199, \"y\": 168 }, { \"x\": 211, \"y\": 168 }, "
     				+"{ \"x\": 210, \"y\": 190 }, { \"x\": 198, \"y\": 190 } ] } }, { \"description\": \"NAME\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 211, \"y\": 168 }, { \"x\": 291, \"y\": 171 }, { \"x\": 290, \"y\": 193 }, { \"x\": 210, \"y\": 190 } ] } }, "
     				+"{ \"description\": \"POLO\", \"boundingPoly\": { \"vertices\": [ { \"x\": 140, \"y\": 213 }, { \"x\": 230, \"y\": 216 }, "
     				+"{ \"x\": 229, \"y\": 242 }, { \"x\": 139, \"y\": 239 } ] } }, { \"description\": \"GIFTS\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 242, \"y\": 217 }, { \"x\": 335, \"y\": 220 }, { \"x\": 334, \"y\": 246 }, { \"x\": 241, \"y\": 243 } ] } }, "
     				+"{ \"description\": \"CREATIONS\", \"boundingPoly\": { \"vertices\": [ { \"x\": 346, \"y\": 221 }, { \"x\": 534, \"y\": 228 },"
     				+" { \"x\": 533, \"y\": 254 }, { \"x\": 345, \"y\": 247 } ] } }, { \"description\": \"PRIVATE\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 546, \"y\": 228 }, { \"x\": 679, \"y\": 233 }, { \"x\": 678, \"y\": 259 }, { \"x\": 545, \"y\": 254 } ] } }, "
     				+"{ \"description\": \"LIMITED\", \"boundingPoly\": { \"vertices\": [ { \"x\": 138, \"y\": 254 }, { \"x\": 271, \"y\": 258 }, "
     				+"{ \"x\": 270, \"y\": 283 }, { \"x\": 137, \"y\": 279 } ] } }, { \"description\": \"ANH\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 132, \"y\": 427 }, { \"x\": 198, \"y\": 430 }, { \"x\": 197, \"y\": 460 }, { \"x\": 131, \"y\": 457 } ] } }, "
     				+"{ \"description\": \"/\", \"boundingPoly\": { \"vertices\": [ { \"x\": 214, \"y\": 431 }, { \"x\": 235, \"y\": 432 }, "
     				+"{ \"x\": 234, \"y\": 461 }, { \"x\": 213, \"y\": 460 } ] } }, { \"description\": \"/\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 412, \"y\": 438 }, { \"x\": 426, \"y\": 439 }, { \"x\": 425, \"y\": 468 }, { \"x\": 411, \"y\": 467 } ] } }, "
     				+"{ \"description\": \"DATE\", \"boundingPoly\": { \"vertices\": [ { \"x\": 424, \"y\": 439 }, { \"x\": 497, \"y\": 442 }, "
     				+"{ \"x\": 496, \"y\": 471 }, { \"x\": 423, \"y\": 468 } ] } }, { \"description\": \"OF\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 507, \"y\": 442 }, { \"x\": 546, \"y\": 444 }, { \"x\": 545, \"y\": 472 }, { \"x\": 506, \"y\": 471 } ] } }, "
     				+"{ \"description\": \"INCORPORATION\", \"boundingPoly\": { \"vertices\": [ { \"x\": 556, \"y\": 444 }, { \"x\": 794, \"y\": 453 }, "
     				+"{ \"x\": 793, \"y\": 482 }, { \"x\": 555, \"y\": 473 } ] } }, { \"description\": \"/\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 795, \"y\": 453 }, { \"x\": 807, \"y\": 453 }, { \"x\": 806, \"y\": 482 }, { \"x\": 794, \"y\": 482 } ] } }, "
     				+"{ \"description\": \"FORMATION\", \"boundingPoly\": { \"vertices\": [ { \"x\": 804, \"y\": 453 }, { \"x\": 972, \"y\": 460 }, "
     				+"{ \"x\": 971, \"y\": 490 }, { \"x\": 803, \"y\": 483 } ] } }, { \"description\": \"06-08-2002\", \"boundingPoly\": { \"vertices\":"
     				+" [ { \"x\": 129, \"y\": 488 }, { \"x\": 304, \"y\": 494 }, { \"x\": 303, \"y\": 518 }, { \"x\": 128, \"y\": 512 } ] } }, "
     				+"{ \"description\": \"FE\", \"boundingPoly\": { \"vertices\": [ { \"x\": 984, \"y\": 168 }, { \"x\": 1030, \"y\": 169 }, "
     				+"{ \"x\": 1030, \"y\": 176 }, { \"x\": 984, \"y\": 175 } ] } }, { \"description\": \"Lea\", \"boundingPoly\": { \"vertices\":"
     				+" [ { \"x\": 763, \"y\": 571 }, { \"x\": 835, \"y\": 562 }, { \"x\": 840, \"y\": 599 }, { \"x\": 768, \"y\": 608 } ] } }, "
     				+"{ \"description\": \"Briche\", \"boundingPoly\": { \"vertices\": [ { \"x\": 595, \"y\": 631 }, { \"x\": 688, \"y\": 634 },"
     				+" { \"x\": 687, \"y\": 665 }, { \"x\": 594, \"y\": 662 } ] } }, { \"description\": \"34th\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 697, \"y\": 634 }, { \"x\": 778, \"y\": 636 }, { \"x\": 777, \"y\": 667 }, { \"x\": 696, \"y\": 665 } ] } }, "
     				+"{ \"description\": \"(\", \"boundingPoly\": { \"vertices\": [ { \"x\": 788, \"y\": 637 }, { \"x\": 805, \"y\": 637 }, "
     				+"{ \"x\": 804, \"y\": 667 }, { \"x\": 787, \"y\": 667 } ] } }, { \"description\": \"chole\", \"boundingPoly\": { \"vertices\":"
     				+" [ { \"x\": 803, \"y\": 637 }, { \"x\": 902, \"y\": 640 }, { \"x\": 901, \"y\": 671 }, { \"x\": 802, \"y\": 668 } ] } }, "
     				+"{ \"description\": \"cha\", \"boundingPoly\": { \"vertices\": [ { \"x\": 912, \"y\": 640 }, { \"x\": 965, \"y\": 641 }, "
     				+"{ \"x\": 964, \"y\": 672 }, { \"x\": 911, \"y\": 671 } ] } }, { \"description\": \")\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 970, \"y\": 642 }, { \"x\": 985, \"y\": 642 }, { \"x\": 984, \"y\": 672 }, { \"x\": 969, \"y\": 672 } ] } }, "
     				+"{ \"description\": \"Commissioner\", \"boundingPoly\": { \"vertices\": [ { \"x\": 498, \"y\": 670 }, { \"x\": 657, \"y\": 676 }, "
     				+"{ \"x\": 656, \"y\": 701 }, { \"x\": 497, \"y\": 695 } ] } }, { \"description\": \"of\", \"boundingPoly\": { \"vertices\": "
     				+" [ { \"x\": 660, \"y\": 676 }, { \"x\": 682, \"y\": 677 }, { \"x\": 681, \"y\": 702 }, { \"x\": 659, \"y\": 701 } ] } }, "
     				+"{ \"description\": \"Income\", \"boundingPoly\": { \"vertices\": [ { \"x\": 687, \"y\": 677 }, { \"x\": 764, \"y\": 680 },"
     				+" { \"x\": 763, \"y\": 705 }, { \"x\": 686, \"y\": 702 } ] } }, { \"description\": \"-\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 766, \"y\": 681 }, { \"x\": 773, \"y\": 681 }, { \"x\": 772, \"y\": 705 }, { \"x\": 765, \"y\": 705 } ] } }, "
     				+"{ \"description\": \"tax\", \"boundingPoly\": { \"vertices\": [ { \"x\": 772, \"y\": 681 }, { \"x\": 803, \"y\": 682 },"
     				+" { \"x\": 802, \"y\": 706 }, { \"x\": 771, \"y\": 705 } ] } }, { \"description\": \"(\", \"boundingPoly\": { \"vertices\": "
     				+"[ { \"x\": 812, \"y\": 682 }, { \"x\": 820, \"y\": 682 }, { \"x\": 819, \"y\": 706 }, { \"x\": 811, \"y\": 706 } ] } }, "
     				+"{ \"description\": \"Computer\", \"boundingPoly\": { \"vertices\": [ { \"x\": 819, \"y\": 682 }, { \"x\": 927, \"y\": 686 }, "
     				+"{ \"x\": 926, \"y\": 711 }, { \"x\": 818, \"y\": 707 } ] } }, { \"description\": \"Operations\", \"boundingPoly\": { \"vertices\":"
     				+" [ { \"x\": 932, \"y\": 687 }, { \"x\": 1047, \"y\": 691 }, { \"x\": 1046, \"y\": 716 }, { \"x\": 931, \"y\": 712 } ] } }, "
     				+" { \"description\": \")\", \"boundingPoly\": { \"vertices\": [ { \"x\": 1047, \"y\": 691 }, { \"x\": 1056, \"y\": 691 }, "
     				+"{ \"x\": 1055, \"y\": 715 }, { \"x\": 1046, \"y\": 715 } ] } } ], "
     				+"\"fullTextAnnotation\": { \"pages\": [ {} ], \"text\": \"1.\n2.\n3.\nRegistration Number :07AAACJ2412Q1ZI\n4.\n5.\n6.\n7.\n8.\nName\nLegal Name\nTrade Name, if any\nConstitution of Business\nAddress of Principal Place of\nBusiness\nDate of Liability\nDate of Validity\nType of Registration\nDesignation\nOffice\nGovernment of India\nForm GST REG-06\n[See Rule 10(1)]\nme t\nRegistration Certificate\nParticulars of Approving Authority\nJHAROKHA SOFT FURNISHINGS PVT LTD\nJHAROKHA SOFT FURNISHING PVT LTD\nRegular\nPrivate Limited Company\n16, COMMUNITY CENTER, EAST OF KAILASH, NEW DELHI, South\nDelhi, Delhi, 110065\n01/07/2017\nFrom\n23/09/2017\nSignature\nTo\n(Amended)\nNA\" } } ] } }";
     
	        try {
	        	responseObject = (JSONObject) jsonParser.parse(jsonString);
				String temp = responseObject.get("result").toString();
				// temp = temp.replaceAll("\\\\","");
				 System.out.println("temp : " + temp);
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
			
				// Extract INAME
				String iname = extractInformation(text, inameRegex);
				System.out.println("NAME: " + iname);
			
				// Extract Date of Incorporation/Formation
				String date = extractInformation(text, dateRegex);
				System.out.println("DATE: " + date);
				//jsonArray.add(responsejson);
	            

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


