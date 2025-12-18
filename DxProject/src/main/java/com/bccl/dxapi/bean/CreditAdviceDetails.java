package com.bccl.dxapi.bean;
import javax.ws.rs.FormParam;

public class CreditAdviceDetails {

	@FormParam("bid")
	private String bid;
	@FormParam("invoicenumber")
	private String invoicenumber;
	@FormParam("ponumber")
	private String ponumber;
	@FormParam("lineItemDesc")
	private String lineItemDesc;
	@FormParam("sortqty")
	private String sortqty;
	@FormParam("rateperqty")
	private String rateperqty;
	@FormParam("amount")
	private String amount;
	@FormParam("totalamount")
	private String totalamount;
	@FormParam("lineitemnumber")
	private String lineitemnumber;
	@FormParam("emailid")
	private String emailid;
	
	
	
	
	public String getEmailid() {
		return emailid;
	}
	public void setEmailid(String emailid) {
		this.emailid = emailid;
	}
	public String getBid() {
		return bid;
	}
	public void setBid(String bid) {
		this.bid = bid;
	}
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
	public String getLineItemDesc() {
		return lineItemDesc;
	}
	public void setLineItemDesc(String lineItemDesc) {
		this.lineItemDesc = lineItemDesc;
	}
	public String getSortqty() {
		return sortqty;
	}
	public void setSortqty(String sortqty) {
		this.sortqty = sortqty;
	}
	public String getRateperqty() {
		return rateperqty;
	}
	public void setRateperqty(String rateperqty) {
		this.rateperqty = rateperqty;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public String getTotalamount() {
		return totalamount;
	}
	public void setTotalamount(String totalamount) {
		this.totalamount = totalamount;
	}
	public String getLineitemnumber() {
		return lineitemnumber;
	}
	public void setLineitemnumber(String lineitemnumber) {
		this.lineitemnumber = lineitemnumber;
	}
	@Override
	public String toString() {
		return "CreditAdviceDetails [bid=" + bid + ", invoicenumber=" + invoicenumber + ", ponumber=" + ponumber
				+ ", lineItemDesc=" + lineItemDesc + ", sortqty=" + sortqty + ", rateperqty=" + rateperqty + ", amount="
				+ amount + ", totalamount=" + totalamount + ", lineitemnumber=" + lineitemnumber + ", emailid="
				+ emailid + "]";
	}
	
}

