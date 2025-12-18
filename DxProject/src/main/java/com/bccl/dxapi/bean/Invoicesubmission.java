package com.bccl.dxapi.bean;

import javax.ws.rs.FormParam;

public class Invoicesubmission {

	@FormParam("bid")
	private String bid;
	@FormParam("po_num")
	private String po_num;
	@FormParam("irnNumber")
	private String irnNumber;
	@FormParam("irnDate")
	private String irnDate;
	@FormParam("invoiceNumber")
	private String invoiceNumber;
	@FormParam("invoiceDate")
	private String invoiceDate;
	@FormParam("referenceNumber")
	private String referenceNumber;
	@FormParam("lineItemNumber")
	private String lineItemNumber;
	@FormParam("orderNumber")
	private String orderNumber;
	@FormParam("quantity")
	private String quantity;
	@FormParam("uOM")
	private String uOM;
	@FormParam("contactPerson")
	private String contactPerson;
	@FormParam("contactPersonPhone")
	private String contactPersonPhone;
	@FormParam("vendorID")
	private String vendorID;
	@FormParam("company")
	private String company;
	@FormParam("plant")
	private String plant;
	@FormParam("department")
	private String department;
	@FormParam("costCentre")
	private String costCentre;
	@FormParam("category")
	private String category;
	@FormParam("businessPartnerText")
	private String businessPartnerText;
	@FormParam("profileID")
	private String profileID;
	@FormParam("invoiceDocumentPath")
	private String invoiceDocumentPath;
	@FormParam("iGSTAmount")
	private String iGSTAmount;
	@FormParam("cGSTAmount")
	private String cGSTAmount;
	@FormParam("sgstAmount")
	private String sgstAmount;
	@FormParam("totalAmount")
	private String totalAmount;
	@FormParam("description")
	private String description;
	@FormParam("status")
	private String status;
	@FormParam("invoiceamount")
	private String invoiceamount;
	@FormParam("actualfilename")
	private String actualfilename;
	@FormParam("savedfilename")
	private String savedfilename;
	@FormParam("createdby")
	private String createdby;
	@FormParam("managerid")
	private String managerid;
	@FormParam("buyerid")
	private String buyerid;
	@FormParam("stage")
	private String stage;
	@FormParam("balance_qty")
	private String balance_qty;
	@FormParam("modified_by")
	private String modified_by;
	@FormParam("rawinvno")
	private String rawinvno;
	@FormParam("invoicetype")
	private String invoicetype;
	@FormParam("material")
	private String material;
	@FormParam("rateperquantity")
	private String rateperquantity;
	@FormParam("multipleactualfilename")
	private String multipleactualfilename;
	@FormParam("multiplesavedfilename")
	private String multiplesavedfilename;
	@FormParam("billofladingdate")
	private String billofladingdate;
	@FormParam("remark")
	private String remark;
	@FormParam("totalamtinctaxes")
	private String totalamtinctaxes;
	@FormParam("taxamount")
	private String taxamount;
	@FormParam("storagelocation")
	private String storagelocation;
	@FormParam("grnnumber")
	private String grnnumber;
	@FormParam("lineitemtext")
	private String lineitemtext;

	@FormParam("uniquereferencenumber")
	private String uniquereferencenumber;

	@FormParam("SAPLINEITEMNO")
	private String saplineitemnumber;

	@FormParam("servicenumber")
	private String servicenumber;

	@FormParam("srcnnumber")
	private String srcnnumber;

	@FormParam("beforesubmissioninvoicenumber")
	private String beforesubmissioninvoicenumber;

	@FormParam("dcnumber")
	private String dcnumber;

	@FormParam("type")
	private String type;

	@FormParam("previnvno")
	private String previnvno;

	@FormParam("prevponos")
	private String prevponos;

	@FormParam("previnvdate")
	private String previnvdate;

	@FormParam("notifyenduseremailiD")
	private String notifyenduseremailiD;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPrevinvno() {
		return previnvno;
	}

	public void setPrevinvno(String previnvno) {
		this.previnvno = previnvno;
	}

	public String getPrevponos() {
		return prevponos;
	}

	public void setPrevponos(String prevponos) {
		this.prevponos = prevponos;
	}

	public String getPrevinvdate() {
		return previnvdate;
	}

	public void setPrevinvdate(String previnvdate) {
		this.previnvdate = previnvdate;
	}

	public String getDcnumber() {
		return dcnumber;
	}

	public void setDcnumber(String dcnumber) {
		this.dcnumber = dcnumber;
	}

	public String getSrcnnumber() {
		return srcnnumber;
	}

	public void setSrcnnumber(String srcnnumber) {
		this.srcnnumber = srcnnumber;
	}

	public String getBeforesubmissioninvoicenumber() {
		return beforesubmissioninvoicenumber;
	}

	public void setBeforesubmissioninvoicenumber(String beforesubmissioninvoicenumber) {
		this.beforesubmissioninvoicenumber = beforesubmissioninvoicenumber;
	}

	public String getServicenumber() {
		return servicenumber;
	}

	public void setServicenumber(String servicenumber) {
		this.servicenumber = servicenumber;
	}

	public String getSaplineitemnumber() {
		return saplineitemnumber;
	}

	public void setSaplineitemnumber(String saplineitemnumber) {
		this.saplineitemnumber = saplineitemnumber;
	}

	public String getUniquereferencenumber() {
		return uniquereferencenumber;
	}

	public void setUniquereferencenumber(String uniquereferencenumber) {
		this.uniquereferencenumber = uniquereferencenumber;
	}

	public String getLineitemtext() {
		return lineitemtext;
	}

	public void setLineitemtext(String lineitemtext) {
		this.lineitemtext = lineitemtext;
	}

	public String getGrnnumber() {
		return grnnumber;
	}

	public void setGrnnumber(String grnnumber) {
		this.grnnumber = grnnumber;
	}

	public String getStoragelocation() {
		return storagelocation;
	}

	public void setStoragelocation(String storagelocation) {
		this.storagelocation = storagelocation;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getTotalamtinctaxes() {
		return totalamtinctaxes;
	}

	public void setTotalamtinctaxes(String totalamtinctaxes) {
		this.totalamtinctaxes = totalamtinctaxes;
	}

	public String getTaxamount() {
		return taxamount;
	}

	public void setTaxamount(String taxamount) {
		this.taxamount = taxamount;
	}

	public String getBillofladingdate() {
		return billofladingdate;
	}

	public void setBillofladingdate(String billofladingdate) {
		this.billofladingdate = billofladingdate;
	}

	public String getMultipleactualfilename() {
		return multipleactualfilename;
	}

	public void setMultipleactualfilename(String multipleactualfilename) {
		this.multipleactualfilename = multipleactualfilename;
	}

	public String getMultiplesavedfilename() {
		return multiplesavedfilename;
	}

	public void setMultiplesavedfilename(String multiplesavedfilename) {
		this.multiplesavedfilename = multiplesavedfilename;
	}

	public String getRateperquantity() {
		return rateperquantity;
	}

	public void setRateperquantity(String rateperquantity) {
		this.rateperquantity = rateperquantity;
	}

	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}

	public String getInvoicetype() {
		return invoicetype;
	}

	public void setInvoicetype(String invoicetype) {
		this.invoicetype = invoicetype;
	}

	public String getRawinvno() {
		return rawinvno;
	}

	public void setRawinvno(String rawinvno) {
		this.rawinvno = rawinvno;
	}

	public String getModified_by() {
		return modified_by;
	}

	public void setModified_by(String modified_by) {
		this.modified_by = modified_by;
	}

	public String getBalance_qty() {
		return balance_qty;
	}

	public void setBalance_qty(String balance_qty) {
		this.balance_qty = balance_qty;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	public String getBuyerid() {
		return buyerid;
	}

	public void setBuyerid(String buyerid) {
		this.buyerid = buyerid;
	}

	public String getManagerid() {
		return managerid;
	}

	public void setManagerid(String managerid) {
		this.managerid = managerid;
	}

	public String getCreatedby() {
		return createdby;
	}

	public void setCreatedby(String createdby) {
		this.createdby = createdby;
	}

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

	public String getIrnNumber() {
		return irnNumber;
	}

	public void setIrnNumber(String irnNumber) {
		this.irnNumber = irnNumber;
	}

	public String getIrnDate() {
		return irnDate;
	}

	public void setIrnDate(String irnDate) {
		this.irnDate = irnDate;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public String getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(String invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	public String getLineItemNumber() {
		return lineItemNumber;
	}

	public void setLineItemNumber(String lineItemNumber) {
		this.lineItemNumber = lineItemNumber;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public String getQuantity() {
		return quantity;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}

	public String getuOM() {
		return uOM;
	}

	public void setuOM(String uOM) {
		this.uOM = uOM;
	}

	public String getContactPerson() {
		return contactPerson;
	}

	public void setContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
	}

	public String getContactPersonPhone() {
		return contactPersonPhone;
	}

	public void setContactPersonPhone(String contactPersonPhone) {
		this.contactPersonPhone = contactPersonPhone;
	}

	public String getVendorID() {
		return vendorID;
	}

	public void setVendorID(String vendorID) {
		this.vendorID = vendorID;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getPlant() {
		return plant;
	}

	public void setPlant(String plant) {
		this.plant = plant;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getCostCentre() {
		return costCentre;
	}

	public void setCostCentre(String costCentre) {
		this.costCentre = costCentre;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getBusinessPartnerText() {
		return businessPartnerText;
	}

	public void setBusinessPartnerText(String businessPartnerText) {
		this.businessPartnerText = businessPartnerText;
	}

	public String getProfileID() {
		return profileID;
	}

	public void setProfileID(String profileID) {
		this.profileID = profileID;
	}

	public String getInvoiceDocumentPath() {
		return invoiceDocumentPath;
	}

	public void setInvoiceDocumentPath(String invoiceDocumentPath) {
		this.invoiceDocumentPath = invoiceDocumentPath;
	}

	public String getiGSTAmount() {
		return iGSTAmount;
	}

	public void setiGSTAmount(String iGSTAmount) {
		this.iGSTAmount = iGSTAmount;
	}

	public String getcGSTAmount() {
		return cGSTAmount;
	}

	public void setcGSTAmount(String cGSTAmount) {
		this.cGSTAmount = cGSTAmount;
	}

	public String getSgstAmount() {
		return sgstAmount;
	}

	public void setSgstAmount(String sgstAmount) {
		this.sgstAmount = sgstAmount;
	}

	public String getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(String totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getInvoiceamount() {
		return invoiceamount;
	}

	public void setInvoiceamount(String invoiceamount) {
		this.invoiceamount = invoiceamount;
	}

	public String getActualfilename() {
		return actualfilename;
	}

	public void setActualfilename(String actualfilename) {
		this.actualfilename = actualfilename;
	}

	public String getSavedfilename() {
		return savedfilename;
	}

	public void setSavedfilename(String savedfilename) {
		this.savedfilename = savedfilename;
	}
	
	public String getNotifyenduseremailiD() {
		return notifyenduseremailiD;
	}

	public void setNotifyenduseremailiD(String notifyenduseremailiD) {
		this.notifyenduseremailiD = notifyenduseremailiD;
	}

	@Override
	public String toString() {
		return "Invoicesubmission [bid=" + bid + ", po_num=" + po_num + ", irnNumber=" + irnNumber + ", irnDate="
				+ irnDate + ", invoiceNumber=" + invoiceNumber + ", invoiceDate=" + invoiceDate + ", referenceNumber="
				+ referenceNumber + ", lineItemNumber=" + lineItemNumber + ", orderNumber=" + orderNumber
				+ ", quantity=" + quantity + ", uOM=" + uOM + ", contactPerson=" + contactPerson
				+ ", contactPersonPhone=" + contactPersonPhone + ", vendorID=" + vendorID + ", company=" + company
				+ ", plant=" + plant + ", department=" + department + ", costCentre=" + costCentre + ", category="
				+ category + ", businessPartnerText=" + businessPartnerText + ", profileID=" + profileID
				+ ", invoiceDocumentPath=" + invoiceDocumentPath + ", iGSTAmount=" + iGSTAmount + ", cGSTAmount="
				+ cGSTAmount + ", sgstAmount=" + sgstAmount + ", totalAmount=" + totalAmount + ", description="
				+ description + ", status=" + status + ", invoiceamount=" + invoiceamount + ", actualfilename="
				+ actualfilename + ", savedfilename=" + savedfilename + ", createdby=" + createdby + ", managerid="
				+ managerid + ", buyerid=" + buyerid + ", stage=" + stage + ", balance_qty=" + balance_qty
				+ ", modified_by=" + modified_by + ", rawinvno=" + rawinvno + ", invoicetype=" + invoicetype
				+ ", material=" + material + ", rateperquantity=" + rateperquantity + ", multipleactualfilename="
				+ multipleactualfilename + ", multiplesavedfilename=" + multiplesavedfilename + ", billofladingdate="
				+ billofladingdate + ", remark=" + remark + ", totalamtinctaxes=" + totalamtinctaxes + ", taxamount="
				+ taxamount + ", storagelocation=" + storagelocation + ", grnnumber=" + grnnumber + ", lineitemtext="
				+ lineitemtext + ", uniquereferencenumber=" + uniquereferencenumber + ", saplineitemnumber="
				+ saplineitemnumber + ", servicenumber=" + servicenumber + ", srcnnumber=" + srcnnumber
				+ ", beforesubmissioninvoicenumber=" + beforesubmissioninvoicenumber + ", dcnumber=" + dcnumber 
				+ ", notifyenduseremailiD = "+ notifyenduseremailiD + "]";
	}

}
