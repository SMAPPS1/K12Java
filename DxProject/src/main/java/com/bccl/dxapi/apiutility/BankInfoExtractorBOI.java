package com.bccl.dxapi.apiutility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankInfoExtractorBOI {
    public static void main(String[] args) {
        String text = "Pay\n" +
                "रुपये Rupees\n" +
                "खा. सं.\n" +
                "A/c No\n" +
                "KHJ\n" +
                "बैंक ऑफ़ इंडिया\n" +
                "Bank of India\n" +
                "BOI\n" +
                "880220110000765\n" +
                "★\n" +
                "साकेत नगर शाखा.. इंदौर (म.प्र.) - 452001\n" +
                "SAKET NAGAR BRANCH, INDORE MP - 452001\n" +
                "IFSC: BKID0008802\n" +
                "जारी करने की तारीख से 3 माह के लिए जै VALID FOR 3 MONTHS FROM DATE OF ISSUE.\n" +
                "अदा करें। ₹\n" +
                "चेक प्राप्तकर्ता की आधार संख्या (वैकल्पिक) Cheque receiver's AADHAAR number (optional)\n" +
                "SULOCHANA MARCOM PRIVATE LIMITED\n" +
                "हमारी सभी शाखाओं पर समाशोधन में देय PAYABLE AT ALL OUR BRANCHES IN I CLEARING\n" +
                "या धारक को Or Bearer\n" +
                "11 24180 21 452013009: 008880\"\n" +
                "Please sign above";

        // Regular expressions for extracting information
        String accountNumberRegex = "\\b(\\d{15,})\\b"; // Assuming account number is 15 digits or more
        String ifscRegex = "IFSC:\\s(\\w{4}\\d{7})"; // IFSC format example: ABCD0123456
        String bankNameRegex = "(Bank of India)"; // Bank name in English or Hindi
        String bankBranchRegex = "BRANCH [\\p{L}0-9\\s,-]+?\\d{6}"; // Branch information

        // Extract account number
        String accountNumber = extractInformation(text, accountNumberRegex);
        System.out.println("Account Number: " + accountNumber);

        // Extract IFSC code
        String ifscCode = extractInformation(text, ifscRegex);
        System.out.println("IFSC Code: " + ifscCode);

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
