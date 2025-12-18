package com.bccl.dxapi.bean;

import javax.ws.rs.FormParam;

public class AdvanceShippingNotification {


		@FormParam("bid")
		private String bid;
		@FormParam("po_num")
		private String po_num;
		@FormParam("lineItemNumber")
		private String lineItemNumber;
		@FormParam("lineItemText")
		private String lineItemText;
		@FormParam("balancQTY")
		private String balancQTY;
		@FormParam("asnQuantity")
		private String asnQuantity;
		@FormParam("asnDate")
		private String asnDate;
		@FormParam("asnLocation")
		private String asnLocation;
		@FormParam ("asnNumber")
		private String asnNumber;
		@FormParam ("reqNo")
		private String reqNo;	
		@FormParam ("supportingDocName")
		private String supportingDocName;	
		
		public String getBid() {
			return bid;
		}
		public void setBid(String bid) {
			this.bid = bid;
		}
		public String getPo_num() {
			return po_num;
		}
		public void setPo_num(String po_num) {
			this.po_num = po_num;
		}
		public String getLineItemNumber() {
			return lineItemNumber;
		}
		public void setLineItemNumber(String lineItemNumber) {
			this.lineItemNumber = lineItemNumber;
		}
		public String getLineItemText() {
			return lineItemText;
		}
		public void setLineItemText(String lineItemText) {
			this.lineItemText = lineItemText;
		}
		public String getBalancQTY() {
			return balancQTY;
		}
		public void setBalancQTY(String balancQTY) {
			this.balancQTY = balancQTY;
		}
		public String getAsnQuantity() {
			return asnQuantity;
		}
		public void setAsnQuantity(String asnQuantity) {
			this.asnQuantity = asnQuantity;
		}
		public String getAsnDate() {
			return asnDate;
		}		
		public void setAsnDate(String asnDate) {
			this.asnDate = asnDate;
		}
		public String getAsnLocation() {
			return asnLocation;
		}
		public void setAsnLocation(String asnLocation) {
			this.asnLocation = asnLocation;
		}
		public String getAsnNumber() {
			return asnNumber;
		}
		public void setAsnNumber(String asnNumber) {
			this.asnNumber = asnNumber;
		}
		public String getReqNo() {
			return reqNo;
		}
		public void setReqNo(String reqNo) {
			this.reqNo = reqNo;
		}
		
		public String getSupportingDocName() {
			return supportingDocName;
		}
		public void setSupportingDocName(String supportingDocName) {
			this.supportingDocName = supportingDocName;
		}
		@Override
		public String toString() {
			return "AdvanceShippingNotification [bid=" + bid + ", po_num=" + po_num + ", lineItemNumber="
					+ lineItemNumber + ", lineItemText=" + lineItemText + ", balancQTY=" + balancQTY + ", asnQuantity="
					+ asnQuantity + ", asnDate=" + asnDate + ", asnLocation=" + asnLocation + ", asnNumber=" + asnNumber
					+ ", reqNo=" + reqNo + "]";
		}
		
		
}
