package com.bccl.dxapi.security;

import javax.mail.*;
import javax.mail.internet.*;
import org.apache.log4j.Logger;
import java.util.*;
import javax.activation.*;
import java.io.*;

public class SendImapMessage {
	
	static Logger log = Logger.getLogger(SendImapMessage.class.getName());
	
	private String toAdd = null;
	private String ccAdd = null;
	private String bccAdd = null;
	private String msg = null;
	private String subj = null;
	private String priority = null;
	public static String footer = "";
	String list = null;
	String compMsg = null;
	String display = null;
	Session session = null;
	static Store store;
	MimeMessage message;
	MimeMessage smsMessage;
	String MAIL_PATH = "apprelay.timesgroup.com";

	/*
	 * Constructor to get the reqd session.
	 */

	public SendImapMessage() {
		// Do Nothing
	}

	/***********************************************************************************************************************************************
	 * @Method Name : sendMail( )
	 * @Arguments : Hashtable - details
	 * @Returns : boolean
	 * @Comments : Returns void
	 ***********************************************************************************************************************************************/
	public boolean sendMail(Hashtable details) {

		String subject = " ";
		String fromAddr = " ";
		String toAddr = " ";
		String content = " ";
		String ccAddr = null;
		boolean flag = false;

		try {
			fromAddr = (String) details.get("fromAddr");
			toAddr = (String) details.get("toAddr");
			subject = (String) details.get("subject");
			content = (String) details.get("content");

			log.info("Sent Email to "+toAddr+" from "+fromAddr+ " subject: "+subject);
			
			if (details.containsKey("ccAddr")) {
				ccAddr = (String) details.get("ccAddr");
			}

			if (details.containsKey("mailPath")) {
				MAIL_PATH = (String) details.get("mailPath");
			}

			// Code to send email using POP
			String gString_host = "mayavi.timesgroup.com";
			Properties lProperties_obj = new Properties();
			lProperties_obj.put("mail.smtp.host", MAIL_PATH);
			Session lSession = Session.getInstance(lProperties_obj, null);
			MimeMessage lMessage_obj = new MimeMessage(lSession);
			lMessage_obj.setFrom(new InternetAddress(fromAddr));

			// Send email to multiple Ids
			String newCCAddr = toAddr;
			while (newCCAddr.indexOf(",") != -1) {
				toAddr = newCCAddr.substring(0, newCCAddr.indexOf(",")).trim();
				lMessage_obj.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddr));
				newCCAddr = newCCAddr.substring(newCCAddr.indexOf(",") + 1).trim();
			}
			lMessage_obj.addRecipient(Message.RecipientType.TO, new InternetAddress(newCCAddr));

			if (ccAddr != null) {
				// CC to multiple Ids
				newCCAddr = ccAddr;
				while (newCCAddr.indexOf(",") != -1) {
					ccAddr = newCCAddr.substring(0, newCCAddr.indexOf(",")).trim();
					lMessage_obj.addRecipient(Message.RecipientType.CC, new InternetAddress(ccAddr));
					newCCAddr = newCCAddr.substring(newCCAddr.indexOf(",") + 1).trim();
				}
				lMessage_obj.addRecipient(Message.RecipientType.CC, new InternetAddress(newCCAddr));
			}

			lMessage_obj.setSubject(subject);
			lMessage_obj.setText(content);
			javax.mail.Transport.send(lMessage_obj);
			flag = true;

		} catch (Exception e) {
			log.info("SendImapMessage .. sendMail " + e.fillInStackTrace());
		}

		return flag;
	}

	/***********************************************************************************************************************************************
	 * @Method Name : sendMailMultiple(Map<String, ArrayList<String>> detailsList)
	 * @Arguments : HashMap - detailsList
	 * @Returns : boolean
	 * @Comments : Returns void
	 ***********************************************************************************************************************************************/

	public boolean sendMailMultiple(Map<String, ArrayList<String>> detailsList) {

		ArrayList<String> subjectList = new ArrayList<String>();
		ArrayList<String> fromAddrList = new ArrayList<String>();
		ArrayList<String> toAddrList = new ArrayList<String>();
		ArrayList<String> contentList = new ArrayList<String>();
		String ccAddr = null;
		boolean flag = false;

		try {
			toAddrList = detailsList.get("toAddrList");
			int sizeList = toAddrList.size();
			InternetAddress addMail = new InternetAddress();

			for (int ls = 0; ls <= sizeList; ls++) {

				// Code to get fromAddr from Map
				fromAddrList = detailsList.get("fromAddrList");
				String frmAddr = (String) fromAddrList.get(0);
				String gString_host = "mayavi.timesgroup.com";
				Properties lProperties_obj = new Properties();
				lProperties_obj.put("mail.smtp.host", MAIL_PATH);
				Session lSession = Session.getInstance(lProperties_obj, null);
				MimeMessage lMessage_obj = new MimeMessage(lSession);
				lMessage_obj.setFrom(new InternetAddress(frmAddr));
				subjectList = detailsList.get("subjectList");
				String subj = (String) subjectList.get(0);
				lMessage_obj.setSubject(subj);

				// Get content from map
				contentList = detailsList.get("contentList");
				String cont = "";
				String contentArray[] = contentList.toArray(new String[contentList.size()]);
				contentArray = new String[contentList.size()];
				contentArray[ls] = contentList.get(ls);
				cont = " ";
				cont = contentArray[ls];
				lMessage_obj.setText(cont);

				// Code to get toAddr from Map and convert to Arraylist
				toAddrList = detailsList.get("toAddrList");
				String mailAddTo[] = toAddrList.toArray(new String[toAddrList.size()]);
				mailAddTo = new String[toAddrList.size()];
				mailAddTo[ls] = toAddrList.get(ls);
				String addEmail = "";
				ArrayList<String> addrList = new ArrayList<String>();
				// Get the email Ids in an array from Arraylist
				InternetAddress[] mailAdd_To = new InternetAddress[mailAddTo.length];
				mailAdd_To[ls] = new InternetAddress(mailAddTo[ls].toString());
				addMail = null;
				addMail = mailAdd_To[ls];
				lMessage_obj.addRecipient(Message.RecipientType.TO, addMail);
				javax.mail.Transport.send(lMessage_obj);
				lMessage_obj = null;
				flag = true;
			}
		}

		catch (Exception e) {
			log.error("SendImapMessage .. sendMailMultiple :  " + e.fillInStackTrace());
		}
		return flag;
	}

	/***********************************************************************************************************************************************
	 * @Method Name : sendHtmlMail( )
	 * @Arguments : Hashtable - details
	 * @Returns : boolean
	 * @Comments : Returns void
	 ***********************************************************************************************************************************************/
	public boolean sendHtmlMail(Hashtable details) {

		String subject = "";
		String fromAddr = "";
		String toAddr = "";
		String content = "";
		String ccAddr = null;
		String attachment = "";
		boolean flag = false;

		try {
			// Getting the details from the Hashtable
			fromAddr = (String) details.get("fromAddr");
			toAddr = (String) details.get("toAddr");
			subject = (String) details.get("subject");
			content = (String) details.get("content");
			if (details.containsKey("ccAddr")) {
				ccAddr = (String) details.get("ccAddr");
			}

			// Code to send email using POP
			String gString_host = "mayavi.timesgroup.com";
			Properties lProperties_obj = new Properties();

			lProperties_obj.put("mail.smtp.host", MAIL_PATH);

			Session lSession = Session.getInstance(lProperties_obj, null);
			MimeMessage lMessage_obj = new MimeMessage(lSession);
			lMessage_obj.setFrom(new InternetAddress(fromAddr));

			// Send email to multiple Ids
			String newCCAddr = toAddr;
			while (newCCAddr.indexOf(",") != -1) {
				toAddr = newCCAddr.substring(0, newCCAddr.indexOf(",")).trim();
				lMessage_obj.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddr));
				newCCAddr = newCCAddr.substring(newCCAddr.indexOf(",") + 1).trim();
			}
			lMessage_obj.addRecipient(Message.RecipientType.TO, new InternetAddress(newCCAddr));

			if (ccAddr != null) {
				// CC to multiple Ids
				newCCAddr = ccAddr;
				while (newCCAddr.indexOf(",") != -1) {
					ccAddr = newCCAddr.substring(0, newCCAddr.indexOf(",")).trim();
					lMessage_obj.addRecipient(Message.RecipientType.CC, new InternetAddress(ccAddr));
					newCCAddr = newCCAddr.substring(newCCAddr.indexOf(",") + 1).trim();
				}
				lMessage_obj.addRecipient(Message.RecipientType.CC, new InternetAddress(newCCAddr));
			}

			lMessage_obj.setSubject(subject);

			// create and fill the first message part
			MimeBodyPart mbp1 = new MimeBodyPart();
			mbp1.setContent(content, "text/html");

			// create the Multipart and add its parts to it
			Multipart mp = new MimeMultipart();
			mp.addBodyPart(mbp1);

			if (details.containsKey("attachment")) {
				attachment = (String) details.get("attachment");

				// create and fill the message attachment part
				File attachFile = new File(attachment);
				FileDataSource fds = new FileDataSource(attachFile);

				MimeBodyPart mbp2 = new MimeBodyPart();
				mbp2.setDisposition(Part.ATTACHMENT);
				mbp2.setDataHandler(new DataHandler(fds));
				mbp2.setFileName(fds.getName());
				mp.addBodyPart(mbp2);
			}

			if (details.containsKey("noofattachments")) {
				String noofattachments = (String) details.get("noofattachments");
				int noOfAttachments = Integer.parseInt(noofattachments);

				for (int ii = 0; ii < noOfAttachments; ii++) {

					String attachmentFile = (String) details.get("attachment" + (ii + 1));
					// create and fill the message attachment part
					File attachFile = new File(attachmentFile);
					FileDataSource fds = new FileDataSource(attachFile);

					MimeBodyPart mbp2 = new MimeBodyPart();
					mbp2.setDisposition(Part.ATTACHMENT);
					mbp2.setDataHandler(new DataHandler(fds));
					mbp2.setFileName(fds.getName());
					mp.addBodyPart(mbp2);
				}
			}
			// add the Multipart to the message
			lMessage_obj.setContent(mp);
			// set the Date: header
			lMessage_obj.setSentDate(new java.util.Date());

			javax.mail.Transport.send(lMessage_obj);

			flag = true;

		} catch (Exception e) {
			log.info("SendImapMessage .. sendHtmlMail " + e.fillInStackTrace());
		}

		return flag;
	}

	/***********************************************************************************************************************************************
	 * @Method Name : sendMail( )
	 * @Arguments : Hashtable - details
	 * @Returns : boolean
	 * @Comments : Returns void---Using Attachment
	 ***********************************************************************************************************************************************/
	public boolean sendMail(Hashtable details, String attachment) {
		String subject = "";
		String fromAddr = "";
		String toAddr = "";
		String content = "";
		String ccAddr = null;
		boolean flag = false;

		try {
			Properties lProperties_obj = new Properties();
			Session lSession = Session.getInstance(lProperties_obj, null);
			MimeMessage lMessage_obj = new MimeMessage(lSession);

			File attachFile = new File(attachment);
			FileDataSource fds = new FileDataSource(attachFile);

			fromAddr = (String) details.get("fromAddr");
			toAddr = (String) details.get("toAddr");
			subject = (String) details.get("subject");
			content = (String) details.get("content");

			if (details.containsKey("ccAddr")) {

				ccAddr = (String) details.get("ccAddr");
			}

			// Code to send email using POP
			String gString_host = "mayavi.timesgroup.com";
			lProperties_obj.put("mail.smtp.host", "apprelay.timesgroup.com");

			lMessage_obj.setFrom(new InternetAddress(fromAddr));
			lMessage_obj.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddr));
			if (ccAddr != null) {
				lMessage_obj.addRecipient(Message.RecipientType.CC, new InternetAddress(ccAddr));
			}

			lMessage_obj.setSubject(subject);

			Multipart mp = new MimeMultipart();

			MimeBodyPart text = new MimeBodyPart();
			text.setDisposition(Part.INLINE);
			text.setContent(content, "text/plain");
			mp.addBodyPart(text);

			MimeBodyPart mbp1 = new MimeBodyPart();
			mbp1.setDisposition(Part.ATTACHMENT);
			mbp1.setDataHandler(new DataHandler(fds));
			mbp1.setFileName(fds.getName());
			mp.addBodyPart(mbp1);

			lMessage_obj.setContent(mp);
			javax.mail.Transport.send(lMessage_obj);

			flag = true;
		} catch (Exception e) {
			log.info("SendImapMessage .. sendMail " + e.fillInStackTrace());
		}

		return flag;
	}
}
