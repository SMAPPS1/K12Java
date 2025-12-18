package com.bccl.dxapi.apiutility;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class Validation {
	
	static Logger log = Logger.getLogger(Validation.class.getName());

	// string Validation
	public static boolean StringChecknull(String string) {
		if (string == null || string.length() == 0 || string.isEmpty() || string.trim().isEmpty() || string.equals("")
				|| string.trim().equals("") || string.equalsIgnoreCase("null")) {
			return false;
		} else {
			return true;
		}
	}

	// number validation
	public static boolean numberCheck(String string) {
		double yourNumber;
		try {
			yourNumber = Double.parseDouble(string);
			return true;
		} catch (Exception ex) {
			log.error("Exception in numberCheck : ",ex.fillInStackTrace());
			return false;
		}

	}

	public static boolean emailCheck(String string) {
		String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." + "[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
				+ "A-Z]{2,7}$";
		Pattern pat = Pattern.compile(emailRegex);
		return pat.matcher(string).matches();
	}
	
	public static boolean validateDateFormat(String format,String date) {
		if (format == null || format.length() == 0 || format.isEmpty() || format.trim().isEmpty() || format.equals("")
				|| format.trim().equals("") || format.equalsIgnoreCase("null")) {
			
			return false;
		} 
		
		if (date == null || date.length() == 0 || date.isEmpty() || date.trim().isEmpty() || date.equals("")
				|| date.trim().equals("") || date.equalsIgnoreCase("null")) {
			return false;
		}
		
		Date pdate = null;
		DateFormat sdf = new SimpleDateFormat(format);
        sdf.setLenient(false);
        try {
        	pdate=sdf.parse(date);
        	
        	if(date.equals(sdf.format(pdate))) {
        		return true;
        	}else {
        		return false;
        	}
        } catch (ParseException e) {
            return false;
        }
	}
	
	public static boolean invoiceNumberCheck(String string) {
		//		~`!@#$%^&*_+={[}]|\:;��<>.,?()
		String specialChars = "~`!@#$%^&*_+={[}]|\\:;��<>.,?()\" ";
		boolean check = false;
		// Use for loop to check special characters
		for (int i = 0; i < string.length(); i++) {
			String strChar = Character.toString(string.charAt(i));
			// Check whether String contains special character or not
			if (specialChars.contains(strChar)) {
				check = true;
				break;
			}
		}
		
		return check;
	}
	
	public static boolean irnNumberCheck(String string) {
		String specialChars = "~`!@#$%^&*-_+={[}]|\\/:;��<>.,?()'\" ";
		boolean check = false;
		// Use for loop to check special characters
		for (int i = 0; i < string.length(); i++) {
			String strChar = Character.toString(string.charAt(i));
			// Check whether String contains special character or not
			if (specialChars.contains(strChar)) {
				check = true;
				break;
			}
		}
		
		return check;
	}
	
	public static boolean fileExtension(String fileExt) {
		boolean check = false;
		
		if(fileExt.indexOf(".")!=-1) {
			String [] strArray = fileExt.split("\\.");
			if(strArray.length >1) {
				if(!"pdf".equalsIgnoreCase(strArray[1])) {
					check= true;
			}		
		 }
		}		
		return check;
	}
	
	//".JPEG",".JPG",".PNG",".DOC",".DOCX",".XLS",".XLSX",".CSV",".PDF"
	public static boolean multiFilesExtension(String fileExt) {
		boolean check = false;
		
		if(fileExt.indexOf(",")!=-1) {
			String[] arrayFile = fileExt.split(",");
			for (String strFile : arrayFile) {		
				if(strFile.indexOf(".")!=-1) {
					String[] arrOfStr = strFile.split("\\.");					
					if(arrOfStr.length >1) {
						if(!"pdf".equalsIgnoreCase(arrOfStr[1]) && !"JPEG".equalsIgnoreCase(arrOfStr[1]) 
								&& !"JPG".equalsIgnoreCase(arrOfStr[1]) && !"PNG".equalsIgnoreCase(arrOfStr[1])
								&& !"DOC".equalsIgnoreCase(arrOfStr[1]) && !"DOCX".equalsIgnoreCase(arrOfStr[1]) 
								&& !"XLSX".equalsIgnoreCase(arrOfStr[1]) && !"CSV".equalsIgnoreCase(arrOfStr[1]) 
								&& !"XLS".equalsIgnoreCase(arrOfStr[1])) {
							check= true;
						}
					}					
				}
			}
		}	
		return check;
	}	
}	
