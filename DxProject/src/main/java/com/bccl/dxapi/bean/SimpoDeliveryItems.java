package com.bccl.dxapi.bean;

import java.util.List;

import javax.ws.rs.FormParam;

public class SimpoDeliveryItems {
	@FormParam("ponumber") 
	String ponumber;
	
	@FormParam("lineitemnumbers")
	String lineitemnumber;
	
	@FormParam("quantity") 
	String quantity;
	
	@FormParam("dcn")
	String dcn;

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

	public String getDcn() {
		return dcn;
	}

	public void setDcn(String dcn) {
		this.dcn = dcn;
	}

	@Override
	public String toString() {
		return "SimpoDeliveryItems [ponumber=" + ponumber + ", lineitemnumber=" + lineitemnumber + ", quantity="+ quantity + ", dcn=" + dcn + "]";
	}

	
	
	
	
}
