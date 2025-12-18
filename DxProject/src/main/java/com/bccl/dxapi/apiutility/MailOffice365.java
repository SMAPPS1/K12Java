package com.bccl.dxapi.apiutility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;

import javax.mail.Message.RecipientType;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.bccl.dxapi.apiimpl.InternalportalImpl;
import com.bccl.dxapi.apiimpl.LoginImpl;

public class MailOffice365 {

	Properties properties;
	Properties prop;
	Session session;
	MimeMessage mimeMessage;

	String userName = null;
	String password = null;
	String hostName = null;
	String startTlsPort = null;
	String fromAddress = null;
	
	boolean STARTTLS = true;
	boolean AUTH = true;
	
	static Logger log = Logger.getLogger(MailOffice365.class.getName());
	
	public MailOffice365(){
		
		prop = new Properties();
		InputStream input = InternalportalImpl.class.getResourceAsStream("/dxproperties.properties");
		try {
			prop.load(input);
			userName = prop.getProperty("USERNAME");
			password = prop.getProperty("PASSWORD");
			hostName = prop.getProperty("HOSTNAME");
			startTlsPort = prop.getProperty("STARTTLS_PORT");
			fromAddress = prop.getProperty("FROM_ADDRESS");
		
		} catch (IOException e) {
			log.error("MailOffice365() 1 :", e.fillInStackTrace());			
		}
		
	}
	public static void main(String args[]) throws MessagingException {
		
		String subject = "Subject:Text Subject";
		String body = "Text Message Body: Hello World";
		String toAddress = "--";
		String ccAddress = "";
		String fromAddrss = "";
	
		MailOffice365 office365 = new MailOffice365();
		Hashtable mailDetails = new Hashtable();
		mailDetails.put("subject", subject);
		mailDetails.put("content", body);
		mailDetails.put("toAddr", toAddress);
		mailDetails.put("ccAddr",ccAddress);
		mailDetails.put("fromAddr",fromAddrss);
		boolean flag = office365.sendEmail(mailDetails);
	
	}

	public boolean sendEmail(Hashtable mailDetails) {
	
		boolean flag = false;
		String subject =  (String) mailDetails.get("subject");
		String body = (String) mailDetails.get("content");
		String toAddress = (String) mailDetails.get("toAddr");
		String ccAddress =(String) mailDetails.get("ccAddr");
		String fromAddress = (String) mailDetails.get("fromAddr");
		try {
			properties = new Properties();
			properties.put("mail.smtp.host", hostName);
			properties.put("mail.smtp.port", startTlsPort);
			properties.put("mail.smtp.auth", AUTH);
			properties.put("mail.smtp.starttls.enable", STARTTLS);
			//properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
			//properties.put("mail.smtp.host", "smtp.gmail.com");
			//properties.put("mail.smtp.port", "465");
			//properties.put("mail.smtp.auth", "true");
			//properties.put("mail.smtp.starttls.enable", "true"); 
			properties.put("mail.smtp.ssl.enable", "true");
			properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");

			Authenticator auth = new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(userName, password);
				}
			};

			session = Session.getInstance(properties, auth);
			mimeMessage = new MimeMessage(session);

			mimeMessage.setFrom(new InternetAddress(fromAddress));
			mimeMessage.addRecipient(RecipientType.TO, new InternetAddress(toAddress));
			
			if(ccAddress != null) {
				mimeMessage.addRecipient(RecipientType.CC, new InternetAddress(ccAddress));	
			}
			
			mimeMessage.setSubject(subject);
			mimeMessage.setText(body);
			mimeMessage.setContent(body,"text/html; charset=utf-8");

			Transport.send(mimeMessage);
			log.info("Mail Send Successfully");
			flag = true;
		} catch (Exception e) {			
			log.error("MailOffice365() sendEmail :", e.fillInStackTrace());
		}
		return flag;
	}

}
