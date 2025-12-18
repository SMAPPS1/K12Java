package com.bccl.dxapi.apiutility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankInfoExtractorICICI {
	
    public static void main(String[] args) {
        String text = "SESHAASAI (D) / CTS - 2010\n" +
                "11\n" +
                "East Of Kailash Branch\n" +
                "D-138, East Of Kailash,,New Delhi - 110019\n" +
                "RTGS/NEFT IFS Code: ICIC0000719\n" +
                "ICICI Bank\n" +
                "A/c No.\n" +
                "Rupees\n" +
                "OF MAN LABACOBA COBA COLA COCC BARCOBAM CO BANCO BAM COBAR CO BAMBAWAWCOMBA COBAKOCSAYBANK CC BANK COBAWIO BACC BANCO BAMIDEA COBAVA.COBADA CO BABAK CD BANK DANDDO BANCO ACCEASSA COBA COBAR CC SAVRICC. BAIK DO GARCOBANKIC BANKCO\n" +
                "OR ORDER\n" +
                "Pay\n" +
                "COM COMXCOMM MOMEDCOMCCOMODAM COCOBA DOBACODAWACABAW BACO BANK BANK BANK COBACOBAROCBAW COBAW BAW CDA CDAND OF FACEBEASO BUDOWEDEN SAK COBAR CO SANNIDOSA.COBANK OC SANKCIO ACEA CO BADO BABACO BAK.DOBA GO FAK DO BANK COBA OG SAW CC BX CC. SACO BARCO BANK AD SAW OC AMEO SAN.20\n" +
                "CO BANCOBOLAMCO AMCC ECC.COM COCOCCA COCA COA COBADO BAW DOAM COBAW CO EAW CO BANKOOBANK DO SA COBANK CO AWCO BANCOBA COBAMCOBAW DO BANK CC BADO BANCO BANK CC BANK OCBAC BANK CC BANKICO BANCO BANK CD BANK DC BANK CC BANK ICO BANK CC BAN CICAK CICIBANK COBANK CC BANKO BANKIDIO\n" +
                "DRITARITE.CCOMCONDMwscow.cOMNIGAN\n" +
                "071905001564\n" +
                "SAKSI\n" +
                "02/9/19\n" +
                "CABUS CBS\n" +
                "BUSINESS BANKING: CURRENT ACCOUNT\n" +
                "Payable at par at all branches of ICICI Bank Limited in India\n" +
                "0\n" +
                "1\n" +
                "5\n" +
                "6\n" +
                "05 0 91\n" +
                "4000\n" +
                "7\n" +
                "ank i\n" +
                "0\n" +
                "07\n" +
                "8\n" +
                "5\n" +
                "VALID FOR THREE MONTHS ONLY\n" +
                "F\n" +
                "₹\n" +
                "\"000758\" 110 2290801 001564\" 29\n" +
                "DDM MYYYY\n" +
                "FOR JHAROKHA SOFT FURNISHINGS PRIVATE\n" +
                "LIMITED\n" +
                "2\n" +
                "AUTHORISED SIGNATORIES\n" +
                "Please sign above";

        // Regular expressions for extracting information
        String accountNumberRegex = "\\b(\\d{12,})\\b"; // Assuming account number is 15 digits or more
        String ifscRegex = "IFS Code:\\s(\\w{4}\\d{7})"; // IFSC format example: ABCD0123456
        String bankNameRegex = "(ICICI Bank|आईसीआईसीआई बैंक)"; // Bank name in English or Hindi
        String bankBranchRegex = "Branch\\n([\\p{L}0-9\\s,-]+)\\n"; // Branch information

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
