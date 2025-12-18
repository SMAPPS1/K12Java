package com.bccl.dxapi.bean;

public class grnbean {

	private String ExpenseSheetid;
	private String DeliveryNote;
	private String TaxCode;
	public String getExpenseSheetid() {
		return ExpenseSheetid;
	}
	public void setExpenseSheetid(String expenseSheetid) {
		ExpenseSheetid = expenseSheetid;
	}
	public String getDeliveryNote() {
		return DeliveryNote;
	}
	public void setDeliveryNote(String deliveryNote) {
		DeliveryNote = deliveryNote;
	}
	public String getTaxCode() {
		return TaxCode;
	}
	public void setTaxCode(String taxCode) {
		TaxCode = taxCode;
	}
	@Override
	public String toString() {
		return "grnbean [ExpenseSheetid=" + ExpenseSheetid + ", DeliveryNote=" + DeliveryNote + ", TaxCode=" + TaxCode
				+ "]";
	}
	
	
}
