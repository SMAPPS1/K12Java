package com.bccl.dxapi.apiutility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankInfoExtractor {
	
    public static void main(String[] args) {
    
    	String text = "8\n" +
                "2\n" +
                "केनरा बैंक\n" +
                "Pay\n" +
                "Rupees रुपये\n" +
                "Alc. No.\n" +
                "A\n" +
                "Canara Bank\n" +
                "SANTACRUZ WEST, MUMBAI Branch.\n" +
                "MUMBAI MAHARASHTRA 400054\n" +
                "IFSC CNRB0015060\n" +
                "50601250000359\n" +
                "CANCELLED\n" +
                "318151\n" +
                "Valid for three months only from the date of instrument\n" +
                "Payable at par at all our branches in India\n" +
                "MULTI-CITY OD\n" +
                "अदा करें ₹\n" +
                "\"\"318151\" 4000152381: 000351\" 30\n" +
                "DDMMYYYY\n" +
                "या धारक को Or Bearer\n" +
                "For POLO GIFTS CREATION PVT LIMITED\n" +
                "Authorised signatory\n" +
                "Please sign above";
		
    	// Regular expressions for extracting information
        String accountNumberRegex = "\\b(\\d{14})\\b"; // Assuming account number is 15 digits
        String ifscRegex = "IFSC\\s(\\w{4}\\d{7})"; // IFSC format example: ABCD0123456
        String micrRegex = "\\b(\\d{10})\\b"; // Assuming MICR is 9 digits
        String bankNameRegex = "(Canara Bank)"; // Bank name in Hindi or English
        String bankBranchRegex = "Branch([\\p{L}0-9\\s,]+)\\n"; // Branch information

        // Extract account number
        String accountNumber = extractInformation(text, accountNumberRegex);
        System.out.println("Account Number: " + accountNumber);

        // Extract IFSC code
        String ifscCode = extractInformation(text, ifscRegex);
        System.out.println("IFSC Code: " + ifscCode);

        // Extract MICR
        String micr = extractInformation(text, micrRegex);
        System.out.println("MICR: " + micr);

        // Extract bank name
        String bankName = extractInformation(text, bankNameRegex);
        System.out.println("Bank Name: " + bankName);

        // Extract bank branch
        String bankBranch = extractInformation(text, bankBranchRegex);
        System.out.println("Bank Branch: " + bankBranch);
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
