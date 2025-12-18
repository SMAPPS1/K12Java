package com.bccl.dxapi.security;

import java.util.*;
import javax.mail.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.*; 
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.log4j.Logger;
import javax.mail.Message.RecipientType;

import javax.activation.*;  


import com.bccl.dxapi.apiimpl.POImpl;

public class EmailImpl {

	static InputStream input = EmailImpl.class.getResourceAsStream("/dxproperties.properties");
	static Properties prop = new Properties();
	static Logger log = Logger.getLogger(EmailImpl.class.getName());

	public static String sendEmail(String to, String message) throws IOException {

		prop.load(input);
		Properties pro = System.getProperties();
		
		prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "465");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); 
		prop.put("mail.smtp.ssl.enable", "true");
		prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		/*
		pro.put("mail.smtp.host", prop.getProperty("mailhost"));
		pro.put("mail.smtp.port", prop.getProperty("port"));
		pro.put("mail.smtp.starttls.enable", "true");
		pro.put("mail.smtp.auth", "true");
		*/
		Session session = Session.getInstance(pro, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(prop.getProperty("fromAddress"), prop.getProperty("password"));
			}
		});
		MimeMessage m = new MimeMessage(session);
		try {
			m.setFrom(new InternetAddress(prop.getProperty("fromAddress"), "Vendor Portal"));
			m.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			m.setSubject("OTP From Partner Portal");
			m.setContent(message, "text/html");
			Transport.send(m);

		} catch (Exception e) {
			log.info("Exception in EmailImpl..." + e.fillInStackTrace());
		}
		return to;
	}
	

	/***********************************************************************************************************************************************
	 * @param attachment 
	 * @param requestNo 
	 * @Method Name : sendHtmlMail( )
	 * @Arguments : Hashtable - details
	 * @Returns : boolean
	 * @Comments : Returns void
	 ***********************************************************************************************************************************************/

	public boolean sendHtmlMail(Hashtable details, ArrayList requestNo, ArrayList attachment) {

		String subject = "";
		String fromAddr = "";
		String toAddr = "";
		String content = "";
		String ccAddr = null;
		//String attachment = "";
		//String requestNO = "";
		String attachSub = "";
		boolean flag = false;

		try {
			// Getting the details from the Hashtable
			// fromAddr = (String) details.get("fromAddr");
			toAddr = (String) details.get("toAddr");
			subject = (String) details.get("subject");
			content = (String) details.get("content");
			//attachment = (String) details.get("attachment");
			//requestNO = (String) details.get("requestNO");
			
			 if(details.containsKey("ccAddr")) {
				 ccAddr = (String) details.get("ccAddr");
			 }
			 
			 
			final String username = "donotreply.Procurement@orchids.edu.in";
			final String password = "Purchase@2025";

			Properties prop = new Properties();
			prop.put("mail.smtp.host", "smtp.gmail.com");
	        prop.put("mail.smtp.port", "465");
	        prop.put("mail.smtp.auth", "true");
	        prop.put("mail.smtp.starttls.enable", "true"); 
			prop.put("mail.smtp.ssl.enable", "true");
			prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");
			/*
			 final String username = "smtptest@4cplus.com";
			 final String password = "Test#SMtomail";

				Properties prop = new Properties();
				prop.put("mail.smtp.host", "smtp3.4cplus.com");
				prop.put("mail.smtp.port", "587");
				prop.put("mail.smtp.auth", "true");
				prop.put("mail.smtp.starttls.enable", "true"); // TLS
			*/		
			Session session = Session.getInstance(prop, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});
			MimeMessage mimeMessage;
			mimeMessage = new MimeMessage(session);
			mimeMessage.setFrom(new InternetAddress("donotreply.Procurement@orchids.edu.in"));
			//mimeMessage.setFrom(new InternetAddress("Harendra@4cplus.com"));
			mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(toAddr));
			if(details.containsKey("ccAddr")) {
				mimeMessage.addRecipient(RecipientType.CC, new InternetAddress(ccAddr));
			}
			mimeMessage.setSubject(subject);
			
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(content, "text/html; charset=utf-8");
			
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			if(attachment != null){
				
				for (int i = 0; i < attachment.size(); i++) {
					if (attachment.get(i) != null && !"".equals(attachment.get(i))) {

						
						MimeBodyPart messageBodyPart1 = new MimeBodyPart();

						String filename = requestNo.get(i) + ".pdf";
						int iend = attachment.get(i).toString().indexOf(".");
						if (iend != -1) {
							attachSub = attachment.get(i).toString().substring(0, iend);
						}
						String filePath = attachSub.replaceAll("_", "/");
						DataSource source = new FileDataSource(
							"/k12share/dxprojectuploads/ASN/" + filePath + "/" + attachment.get(i));
				//		DataSource source = new FileDataSource(
				//				"/var/timescapeattachments/dxprojectuploads/ASN/" + filePath + "/" + attachment.get(i));
							
						messageBodyPart1.setDataHandler(new DataHandler(source));
						messageBodyPart1.setFileName(filename);

						multipart.addBodyPart(messageBodyPart1);
						mimeMessage.setContent(multipart);
					} else {
						mimeMessage.setText(content);
						mimeMessage.setContent(content, "text/html; charset=utf-8");
					}
				}
			}else{
						mimeMessage.setText(content);
						mimeMessage.setContent(content, "text/html; charset=utf-8");
			}		
			Transport.send(mimeMessage);
			flag = true;

		} catch (Exception e) {
			System.out.println("SendImapMessage .. sendMail(Hashtable) 2 " + e);
		}

		return flag;
	}

}
