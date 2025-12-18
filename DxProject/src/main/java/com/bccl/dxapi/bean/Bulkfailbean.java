package com.bccl.dxapi.bean;

public class Bulkfailbean {
	
	private String invoicenumber;
	private String ponumber;
	private String bussinesspartneroid;
	private String message;
	public String getInvoicenumber() {
		return invoicenumber;
	}
	public void setInvoicenumber(String invoicenumber) {
		this.invoicenumber = invoicenumber;
	}
	public String getPonumber() {
		return ponumber;
	}
	public void setPonumber(String ponumber) {
		this.ponumber = ponumber;
	}
	public String getBussinesspartneroid() {
		return bussinesspartneroid;
	}
	public void setBussinesspartneroid(String bussinesspartneroid) {
		this.bussinesspartneroid = bussinesspartneroid;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public String toString() {
		return "Bulkfailbean [invoicenumber=" + invoicenumber + ", ponumber=" + ponumber + ", bussinesspartneroid="
				+ bussinesspartneroid + ", message=" + message + "]";
	}
	
	

}
