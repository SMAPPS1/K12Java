package com.bccl.dxapi.apiutility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractInfo {
	
    public static void main(String[] args) {
        /*
    	String text = "स्थाई लेखा संख्या /PERMANENT ACCOUNT NUMBER\n" +
                "AAACJ2412Q\n" +
                "नाम INAME\n" +
                "JHAROKHA SOFT FURNISHINGS PVT LTD\n" +
                "निगमन बनने की तिथि /DATE OF INCORPORATION/FORMATION\n" +
                "13-06-1995\n" +
                "Rhedingh\n" +
                "आयकर निदेशक (पद्धति)\n" +
                "DIRECTOR OF INCOME TAX (SYSTEMS)\n" +
                "For Jharokha Soft Furnishings Pvt. Ltd.\n" +
                "Ushakuiha\n" +
                "Director";
        */
        
        String text = "स्थाई लेखा संख्या / PERMANENT ACCOUNT NUMBER\r\n"
        		+ "AACCP8008Q\r\n"
        		+ "नाम / NAME\r\n"
        		+ "POLO GIFTS CREATIONS PRIVATE\r\n"
        		+ "LIMITED\r\n"
        		+ "निगमन/बनने की तिथि /DATE OF INCORPORATION/FORMATION\r\n"
        		+ "06-08-2002\r\n"
        		+ "FE\r\n"
        		+ "किश\r\n"
        		+ "आयकर आयुक्त (कम्प्यूटर केन्द्र)\r\n"
        		+ "Commissioner of Income-tax (Computer Operations)\r\n";

        // Regular expressions for PAN, INAME, and Date of Incorporation/Formation
        String panRegex = "PERMANENT ACCOUNT NUMBER\\s*(\\w+)";
        String inameRegex = "NAME\\s*([\\p{L}0-9\\s]+)";
        String dateRegex = "DATE OF INCORPORATION/FORMATION\\s*(\\d{2}-\\d{2}-\\d{4})";

        // Extract PAN
        String pan = extractInformation(text, panRegex);
        System.out.println("PERMANENT ACCOUNT NUMBER: " + pan);

        // Extract INAME
        String iname = extractInformation(text, inameRegex);
        System.out.println("NAME: " + iname);

        // Extract Date of Incorporation/Formation
        String date = extractInformation(text, dateRegex);
        System.out.println("DATE OF INCORPORATION/FORMATION: " + date);
    }

    private static String extractInformation(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "Not Found";
        }
    }
}
