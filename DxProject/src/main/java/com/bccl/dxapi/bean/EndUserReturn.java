package com.bccl.dxapi.bean;

import javax.ws.rs.FormParam;

public class EndUserReturn {
	
	@FormParam("ponumber")
	private String ponumber;
	
	@FormParam("lineitemnumber")
	private String lineitemnumber;
	
	@FormParam("quantity")
	private String quantity;
	
	@FormParam("invoicenumber")
	private String invoicenumber;
	
	@FormParam("bid")
	private String bid;
	
	@FormParam("userid")
	private String userid;
	
	@FormParam("status")
	private String status;
	
	
	

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPonumber() {
		return ponumber;
	}

	public void setPonumber(String ponumber) {
		this.ponumber = ponumber;
	}

	public String getLineitemnumber() {
		return lineitemnumber;
	}

	public void setLineitemnumber(String lineitemnumber) {
		this.lineitemnumber = lineitemnumber;
	}

	public String getQuantity() {
		return quantity;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}

	public String getInvoicenumber() {
		return invoicenumber;
	}

	public void setInvoicenumber(String invoicenumber) {
		this.invoicenumber = invoicenumber;
	}

	public String getBid() {
		return bid;
	}

	public void setBid(String bid) {
		this.bid = bid;
	}

	@Override
	public String toString() {
		return "EndUserReturn [ponumber=" + ponumber + ", lineitemnumber=" + lineitemnumber + ", quantity=" + quantity
				+ ", invoicenumber=" + invoicenumber + ", bid=" + bid + ", userid=" + userid + ", status=" + status
				+ "]";
	}

	

	
}