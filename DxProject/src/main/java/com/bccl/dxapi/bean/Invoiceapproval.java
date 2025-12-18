package com.bccl.dxapi.bean;

import javax.ws.rs.FormParam;

public class Invoiceapproval {
	
	@FormParam("vendorid")
	private String vendorid;
	@FormParam("invoicenumber")
	private String invoicenumber;
	@FormParam("invoicedate")
	private String invoicedate;
	@FormParam("ponumber")
	private String ponumber;
	@FormParam("buyerid")
	private String buyerid;
	@FormParam("enduserid")
	private String enduserid;
	@FormParam("enduserstatus")
	private String enduserstatus;
	@FormParam("eumanager")
	private String eumanager;
	@FormParam("status")
	private String status;
	@FormParam("stage")
	private String stage;
	@FormParam("totalamount")
	private String totalamount;
	@FormParam("managercount")
	private String managercount;
	
	@FormParam("storekeeperaction")
	private String storekeeperaction;
	
	
	public String getStorekeeperaction() {
		return storekeeperaction;
	}
	public void setStorekeeperaction(String storekeeperaction) {
		this.storekeeperaction = storekeeperaction;
	}
	
	public String getVendorid() {
		return vendorid;
	}
	public void setVendorid(String vendorid) {
		this.vendorid = vendorid;
	}
	public String getInvoicenumber() {
		return invoicenumber;
	}
	public void setInvoicenumber(String invoicenumber) {
		this.invoicenumber = invoicenumber;
	}
	public String getInvoicedate() {
		return invoicedate;
	}
	public void setInvoicedate(String invoicedate) {
		this.invoicedate = invoicedate;
	}
	public String getPonumber() {
		return ponumber;
	}
	public void setPonumber(String ponumber) {
		this.ponumber = ponumber;
	}
	public String getBuyerid() {
		return buyerid;
	}
	public void setBuyerid(String buyerid) {
		this.buyerid = buyerid;
	}
	public String getEnduserid() {
		return enduserid;
	}
	public void setEnduserid(String enduserid) {
		this.enduserid = enduserid;
	}
	public String getEnduserstatus() {
		return enduserstatus;
	}
	public void setEnduserstatus(String enduserstatus) {
		this.enduserstatus = enduserstatus;
	}
	public String getEumanager() {
		return eumanager;
	}
	public void setEumanager(String eumanager) {
		this.eumanager = eumanager;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getStage() {
		return stage;
	}
	public void setStage(String stage) {
		this.stage = stage;
	}
	public String getTotalamount() {
		return totalamount;
	}
	public void setTotalamount(String totalamount) {
		this.totalamount = totalamount;
	}
	public String getManagercount() {
		return managercount;
	}
	public void setManagercount(String managercount) {
		this.managercount = managercount;
	}
	
	@Override
	public String toString() {
		return "Invoiceapproval [vendorid=" + vendorid + ", invoicenumber=" + invoicenumber + ", invoicedate="
				+ invoicedate + ", ponumber=" + ponumber + ", buyerid=" + buyerid + ", enduserid=" + enduserid
				+ ", enduserstatus=" + enduserstatus + ", eumanager=" + eumanager + ", status=" + status + ", stage="
				+ stage + ", totalamount=" + totalamount + ", managercount=" + managercount + ", storekeeperaction="
				+ storekeeperaction + "]";
	}
	
	
		
}

