package com.bccl.dxapi.bean;

import java.sql.Date;

import javax.ws.rs.FormParam;

public class VendorAmendmentSubmit {

	
	@FormParam("businessPartnerOID")
	private String businessPartnerOID;
	@FormParam("purchaseGroup")
	private String purchaseGroup;
	@FormParam("plantName")
	private String plantName;
	@FormParam("title")
	private String title;
	@FormParam("vendorNameL1")
	private String vendorNameL1;
	@FormParam("vendorNameL2")
	private String vendorNameL2;
	@FormParam("street1")
	private String street1;
	@FormParam("street2")
	private String street2;
	@FormParam("street3")
	private String street3;
	@FormParam("street4")
	private String street4;
	@FormParam("pinCode")
	private String pinCode;
	@FormParam("city")
	private String city;
	@FormParam("country")
	private String country;
	@FormParam("telNo")
	private String telNo;
	@FormParam("mobileNo")
	private String mobileNo;
	@FormParam("faxNo")
	private String faxNo;
	@FormParam("emailId")
	private String emailId;
	@FormParam("emailId1")
	private String emailId1;
	@FormParam("emailId2")
	private String emailId2;
	@FormParam("emailId3")
	private String emailId3;
	@FormParam("emailId4")
	private String emailId4;
	@FormParam("typeOfBusiness")
	private String typeOfBusiness;
	@FormParam("typeOfIndustry")
	private String typeOfIndustry;
	@FormParam("promoters")
	private String promoters;
	@FormParam("turnOver")
	private String turnOver;
	@FormParam("top5Client")
	private String top5Client;
	@FormParam("clientReference")
	private String clientReference;
	@FormParam("complianceCategory")
	private String complianceCategory;
	@FormParam("consititutionOfBusiness")
	private String consititutionOfBusiness;
	@FormParam("bankName")
	private String bankName;
	@FormParam("branch")
	private String branch;
	@FormParam("bankAccNo")
	private String bankAccNo;
	@FormParam("swiftCode")
	private String swiftCode;
	@FormParam("ifscCode")
	private String ifscCode;
	@FormParam("pfRegistrationNo")
	private String pfRegistrationNo;
	@FormParam("esiRegistrationNo")
	private String esiRegistrationNo;
	@FormParam("vatRegistrationNo")
	private String vatRegistrationNo;
	@FormParam("panNumber")
	private String panNumber;
	@FormParam("serviceTaxRegnNo")
	private String serviceTaxRegnNo;
	@FormParam("cstNoCstDate")
	private String cstNoCstDate;
	@FormParam("lstNoLstDate")
	private String lstNoLstDate;
	@FormParam("gstRegistrationNo")
	private String gstRegistrationNo;
	@FormParam("taxClassification")
	private String taxClassification;
	@FormParam("tdsExemption")
	private String tdsExemption;
	@FormParam("tdsExemptionPercent")
	private String tdsExemptionPercent;
	@FormParam("tdsExemptionFrom")
	private String tdsExemptionFrom;
	@FormParam("tdsExemptionTo")
	private String tdsExemptionTo;
	@FormParam("msmeStatus")
	private String msmeStatus;
	@FormParam("msmeNo")
	private String msmeNo;
	
	public String getBusinessPartnerOID() {
		return businessPartnerOID;
	}
	public void setBusinessPartnerOID(String businessPartnerOID) {
		this.businessPartnerOID = businessPartnerOID;
	}
	public String getPurchaseGroup() {
		return purchaseGroup;
	}
	public void setPurchaseGroup(String purchaseGroup) {
		this.purchaseGroup = purchaseGroup;
	}
	public String getPlantName() {
		return plantName;
	}
	public void setPlantName(String plantName) {
		this.plantName = plantName;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getVendorNameL1() {
		return vendorNameL1;
	}
	public void setVendorNameL1(String vendorNameL1) {
		this.vendorNameL1 = vendorNameL1;
	}
	public String getVendorNameL2() {
		return vendorNameL2;
	}
	public void setVendorNameL2(String vendorNameL2) {
		this.vendorNameL2 = vendorNameL2;
	}
	public String getStreet1() {
		return street1;
	}
	public void setStreet1(String street1) {
		this.street1 = street1;
	}
	public String getStreet2() {
		return street2;
	}
	public void setStreet2(String street2) {
		this.street2 = street2;
	}
	public String getStreet3() {
		return street3;
	}
	public void setStreet3(String street3) {
		this.street3 = street3;
	}
	public String getStreet4() {
		return street4;
	}
	public void setStreet4(String street4) {
		this.street4 = street4;
	}
	public String getPinCode() {
		return pinCode;
	}
	public void setPinCode(String pinCode) {
		this.pinCode = pinCode;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getTelNo() {
		return telNo;
	}
	public void setTelNo(String telNo) {
		this.telNo = telNo;
	}
	public String getMobileNo() {
		return mobileNo;
	}
	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}
	public String getFaxNo() {
		return faxNo;
	}
	public void setFaxNo(String faxNo) {
		this.faxNo = faxNo;
	}
	public String getEmailId() {
		return emailId;
	}
	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}	
	public String getEmailId1() {
		return emailId1;
	}
	public void setEmailId1(String emailId1) {
		this.emailId1 = emailId1;
	}
	public String getEmailId2() {
		return emailId2;
	}
	public void setEmailId2(String emailId2) {
		this.emailId2 = emailId2;
	}
	public String getEmailId3() {
		return emailId3;
	}
	public void setEmailId3(String emailId3) {
		this.emailId3 = emailId3;
	}
	public String getEmailId4() {
		return emailId4;
	}
	public void setEmailId4(String emailId4) {
		this.emailId4 = emailId4;
	}
	public String getTypeOfBusiness() {
		return typeOfBusiness;
	}
	public void setTypeOfBusiness(String typeOfBusiness) {
		this.typeOfBusiness = typeOfBusiness;
	}
	public String getTypeOfIndustry() {
		return typeOfIndustry;
	}
	public void setTypeOfIndustry(String typeOfIndustry) {
		this.typeOfIndustry = typeOfIndustry;
	}
	public String getPromoters() {
		return promoters;
	}
	public void setPromoters(String promoters) {
		this.promoters = promoters;
	}
	public String getTurnOver() {
		return turnOver;
	}
	public void setTurnOver(String turnOver) {
		this.turnOver = turnOver;
	}
	public String getTop5Client() {
		return top5Client;
	}
	public void setTop5Client(String top5Client) {
		this.top5Client = top5Client;
	}
	public String getClientReference() {
		return clientReference;
	}
	public void setClientReference(String clientReference) {
		this.clientReference = clientReference;
	}
	public String getComplianceCategory() {
		return complianceCategory;
	}
	public void setComplianceCategory(String complianceCategory) {
		this.complianceCategory = complianceCategory;
	}
	public String getConsititutionOfBusiness() {
		return consititutionOfBusiness;
	}
	public void setConsititutionOfBusiness(String consititutionOfBusiness) {
		this.consititutionOfBusiness = consititutionOfBusiness;
	}
	public String getBankName() {
		return bankName;
	}
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	public String getBranch() {
		return branch;
	}
	public void setBranch(String branch) {
		this.branch = branch;
	}
	public String getBankAccNo() {
		return bankAccNo;
	}
	public void setBankAccNo(String bankAccNo) {
		this.bankAccNo = bankAccNo;
	}
	public String getSwiftCode() {
		return swiftCode;
	}
	public void setSwiftCode(String swiftCode) {
		this.swiftCode = swiftCode;
	}
	public String getIfscCode() {
		return ifscCode;
	}
	public void setIfscCode(String ifscCode) {
		this.ifscCode = ifscCode;
	}
	public String getPfRegistrationNo() {
		return pfRegistrationNo;
	}
	public void setPfRegistrationNo(String pfRegistrationNo) {
		this.pfRegistrationNo = pfRegistrationNo;
	}
	public String getEsiRegistrationNo() {
		return esiRegistrationNo;
	}
	public void setEsiRegistrationNo(String esiRegistrationNo) {
		this.esiRegistrationNo = esiRegistrationNo;
	}
	public String getVatRegistrationNo() {
		return vatRegistrationNo;
	}
	public void setVatRegistrationNo(String vatRegistrationNo) {
		this.vatRegistrationNo = vatRegistrationNo;
	}
	public String getPanNumber() {
		return panNumber;
	}
	public void setPanNumber(String panNumber) {
		this.panNumber = panNumber;
	}
	public String getServiceTaxRegnNo() {
		return serviceTaxRegnNo;
	}
	public void setServiceTaxRegnNo(String serviceTaxRegnNo) {
		this.serviceTaxRegnNo = serviceTaxRegnNo;
	}
	public String getCstNoCstDate() {
		return cstNoCstDate;
	}
	public void setCstNoCstDate(String cstNoCstDate) {
		this.cstNoCstDate = cstNoCstDate;
	}
	public String getLstNoLstDate() {
		return lstNoLstDate;
	}
	public void setLstNoLstDate(String lstNoLstDate) {
		this.lstNoLstDate = lstNoLstDate;
	}
	public String getGstRegistrationNo() {
		return gstRegistrationNo;
	}
	public void setGstRegistrationNo(String gstRegistrationNo) {
		this.gstRegistrationNo = gstRegistrationNo;
	}
	public String getTaxClassification() {
		return taxClassification;
	}
	public void setTaxClassification(String taxClassification) {
		this.taxClassification = taxClassification;
	}
	public String getTdsExemption() {
		return tdsExemption;
	}
	public void setTdsExemption(String tdsExemption) {
		this.tdsExemption = tdsExemption;
	}
	public String getTdsExemptionPercent() {
		return tdsExemptionPercent;
	}
	public void setTdsExemptionPercent(String tdsExemptionPercent) {
		this.tdsExemptionPercent = tdsExemptionPercent;
	}
	public String getTdsExemptionFrom() {
		return tdsExemptionFrom;
	}
	public void setTdsExemptionFrom(String tdsExemptionFrom) {
		this.tdsExemptionFrom = tdsExemptionFrom;
	}
	public String getTdsExemptionTo() {
		return tdsExemptionTo;
	}
	public void setTdsExemptionTo(String tdsExemptionTo) {
		this.tdsExemptionTo = tdsExemptionTo;
	}
	public String getMsmeStatus() {
		return msmeStatus;
	}
	public void setMsmeStatus(String msmeStatus) {
		this.msmeStatus = msmeStatus;
	}
	public String getMsmeNo() {
		return msmeNo;
	}
	public void setMsmeNo(String msmeNo) {
		this.msmeNo = msmeNo;
	}
	
	@Override
	public String toString() {
		return "VendorAmendmentSubmit [businessPartnerOID=" + businessPartnerOID + ", purchaseGroup=" + purchaseGroup
				+ ", plantName=" + plantName + ", title=" + title + ", vendorNameL1=" + vendorNameL1 + ", vendorNameL2="
				+ vendorNameL2 + ", street1=" + street1 + ", street2=" + street2 + ", street3=" + street3 + ", street4="
				+ street4 + ", pinCode=" + pinCode + ", city=" + city + ", country=" + country + ", telNo=" + telNo
				+ ", mobileNo=" + mobileNo + ", faxNo=" + faxNo + ", emailId=" + emailId + ", emailId1=" + emailId1
				+ ", emailId2=" + emailId2 + ", emailId3=" + emailId3 + ", emailId4=" + emailId4 + ", typeOfBusiness="
				+ typeOfBusiness + ", typeOfIndustry=" + typeOfIndustry + ", promoters=" + promoters + ", turnOver="
				+ turnOver + ", top5Client=" + top5Client + ", clientReference=" + clientReference
				+ ", complianceCategory=" + complianceCategory + ", consititutionOfBusiness=" + consititutionOfBusiness
				+ ", bankName=" + bankName + ", branch=" + branch + ", bankAccNo=" + bankAccNo + ", swiftCode="
				+ swiftCode + ", ifscCode=" + ifscCode + ", pfRegistrationNo=" + pfRegistrationNo
				+ ", esiRegistrationNo=" + esiRegistrationNo + ", vatRegistrationNo=" + vatRegistrationNo
				+ ", panNumber=" + panNumber + ", serviceTaxRegnNo=" + serviceTaxRegnNo + ", cstNoCstDate="
				+ cstNoCstDate + ", lstNoLstDate=" + lstNoLstDate + ", gstRegistrationNo=" + gstRegistrationNo
				+ ", taxClassification=" + taxClassification + ", tdsExemption=" + tdsExemption
				+ ", tdsExemptionPercent=" + tdsExemptionPercent + ", tdsExemptionFrom=" + tdsExemptionFrom
				+ ", tdsExemptionTo=" + tdsExemptionTo + ", msmeStatus=" + msmeStatus + ", msmeNo=" + msmeNo + "]";
	}		
}
