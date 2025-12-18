package com.bccl.dxapi.bean;

import java.util.List;

public class onexbean {

	private String ExpenseSheetid;
	private String InvoiceType;
	private String SupplierGSTIN;
	private String PoNo;
	private String POUserId;
	private String Department;
	private String VendorName;
	private String BillofLadingDate;
	private String InvoiceDate;
	private String InvoiceNo;
	private String InvoiceAmount;
	private String IRNDate;
	private String IRNNO;

	private String Remark;
	private String REF3;
	private List<grnbean> DeliveryNoteList;
	public String getExpenseSheetid() {
		return ExpenseSheetid;
	}
	public void setExpenseSheetid(String expenseSheetid) {
		ExpenseSheetid = expenseSheetid;
	}
	public String getInvoiceType() {
		return InvoiceType;
	}
	public void setInvoiceType(String invoiceType) {
		InvoiceType = invoiceType;
	}
	public String getSupplierGSTIN() {
		return SupplierGSTIN;
	}
	public void setSupplierGSTIN(String supplierGSTIN) {
		SupplierGSTIN = supplierGSTIN;
	}
	public String getPoNo() {
		return PoNo;
	}
	public void setPoNo(String poNo) {
		PoNo = poNo;
	}
	public String getPOUserId() {
		return POUserId;
	}
	public void setPOUserId(String pOUserId) {
		POUserId = pOUserId;
	}
	public String getDepartment() {
		return Department;
	}
	public void setDepartment(String department) {
		Department = department;
	}
	public String getVendorName() {
		return VendorName;
	}
	public void setVendorName(String vendorName) {
		VendorName = vendorName;
	}
	public String getBillofLadingDate() {
		return BillofLadingDate;
	}
	public void setBillofLadingDate(String billofLadingDate) {
		BillofLadingDate = billofLadingDate;
	}
	public String getInvoiceDate() {
		return InvoiceDate;
	}
	public void setInvoiceDate(String invoiceDate) {
		InvoiceDate = invoiceDate;
	}
	public String getInvoiceNo() {
		return InvoiceNo;
	}
	public void setInvoiceNo(String invoiceNo) {
		InvoiceNo = invoiceNo;
	}
	public String getInvoiceAmount() {
		return InvoiceAmount;
	}
	public void setInvoiceAmount(String invoiceAmount) {
		InvoiceAmount = invoiceAmount;
	}
	public String getIRNDate() {
		return IRNDate;
	}
	public void setIRNDate(String iRNDate) {
		IRNDate = iRNDate;
	}
	public String getIRNNO() {
		return IRNNO;
	}
	public void setIRNNO(String iRNNO) {
		IRNNO = iRNNO;
	}
	public String getRemark() {
		return Remark;
	}
	public void setRemark(String remark) {
		Remark = remark;
	}
	public String getREF3() {
		return REF3;
	}
	public void setREF3(String rEF3) {
		REF3 = rEF3;
	}
	public List<grnbean> getDeliveryNoteList() {
		return DeliveryNoteList;
	}
	public void setDeliveryNoteList(List<grnbean> deliveryNoteList) {
		DeliveryNoteList = deliveryNoteList;
	}
	@Override
	public String toString() {
		return "onexbean [ExpenseSheetid=" + ExpenseSheetid + ", InvoiceType=" + InvoiceType + ", SupplierGSTIN="
				+ SupplierGSTIN + ", PoNo=" + PoNo + ", POUserId=" + POUserId + ", Department=" + Department
				+ ", VendorName=" + VendorName + ", BillofLadingDate=" + BillofLadingDate + ", InvoiceDate="
				+ InvoiceDate + ", InvoiceNo=" + InvoiceNo + ", InvoiceAmount=" + InvoiceAmount + ", IRNDate=" + IRNDate
				+ ", IRNNO=" + IRNNO + ", Remark=" + Remark + ", REF3=" + REF3 + ", DeliveryNoteList="
				+ DeliveryNoteList + "]";
	}

}
