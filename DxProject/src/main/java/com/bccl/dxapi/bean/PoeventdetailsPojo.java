package com.bccl.dxapi.bean;

import javax.ws.rs.FormParam;

public class PoeventdetailsPojo {

	@FormParam("ponumber")
	private String ponumber;
	@FormParam("businesspartneroid")
	private String businesspartneroid;
	@FormParam("lineitemnumber")
	private String lineitemnumber;
	@FormParam("lineitemtext")
	private String lineitemtext;
	@FormParam("company")
	private String company;
	@FormParam("plant")
	private String plant;
	@FormParam("department")
	private String department;
	@FormParam("costcentre")
	private String costcentre;
	@FormParam("category")
	private String category;
	
	@FormParam("businesspartnertext")
	private String businesspartnertext;
	@FormParam("quantity")
	private String quantity;
	@FormParam("unitofmeasure")
	private String unitofmeasure;
	@FormParam("contactpersonemailid")
	private String contactpersonemailid;
	
	@FormParam("contactpersonphone")
	private String contactpersonphone;
	@FormParam("deliveryaddress1")
	private String deliveryaddress1;
	@FormParam("deliveryaddress2")
	private String deliveryaddress2;
	@FormParam("deliveryaddress3")
	private String deliveryaddress3;
	
	@FormParam("city")
	private String city;
	@FormParam("state")
	private String state;
	@FormParam("country")
	private String country;
	@FormParam("pincode")
	private String pincode;
	
	@FormParam("status")
	private String status;
	@FormParam("igstamount")
	private String igstamount;
	@FormParam("cgstamount")
	private String cgstamount;
	@FormParam("sgstamount")
	private String sgstamount;
	
	@FormParam("ordernumber")
	private String ordernumber;
	@FormParam("remark")
	private String remark;
	@FormParam("material_type")
	private String material_type;
	@FormParam("storagelocation")
	private String storagelocation;
	
	@FormParam("material")
	private String material;
	@FormParam("hsnno")
	private String hsnno;
	@FormParam("rateperqty")
	private String rateperqty;
	@FormParam("currency")
	private String currency;
	
	@FormParam("deliverydate")
	private String deliverydate;
	@FormParam("contractagreementno")
	private String contractagreementno;
	@FormParam("contractagreementitemno")
	private String contractagreementitemno;
	@FormParam("contractagreementdate")
	private String contractagreementdate;
	
	@FormParam("deladdresstitle")
	private String deladdresstitle;
	@FormParam("deladdressname")
	private String deladdressname;
	@FormParam("deladdressdistrict")
	private String deladdressdistrict;
	@FormParam("deladdressregioncode")
	private String deladdressregioncode;
	
	@FormParam("prnumber")
	private String prnumber;
	@FormParam("foreclosestatuscheck")
	private String foreclosestatuscheck;
	@FormParam("balance_qty")
	private String balance_qty;
	public String getPonumber() {
		return ponumber;
	}
	public void setPonumber(String ponumber) {
		this.ponumber = ponumber;
	}
	public String getBusinesspartneroid() {
		return businesspartneroid;
	}
	public void setBusinesspartneroid(String businesspartneroid) {
		this.businesspartneroid = businesspartneroid;
	}
	public String getLineitemnumber() {
		return lineitemnumber;
	}
	public void setLineitemnumber(String lineitemnumber) {
		this.lineitemnumber = lineitemnumber;
	}
	public String getLineitemtext() {
		return lineitemtext;
	}
	public void setLineitemtext(String lineitemtext) {
		this.lineitemtext = lineitemtext;
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
	public String getCostcentre() {
		return costcentre;
	}
	public void setCostcentre(String costcentre) {
		this.costcentre = costcentre;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getBusinesspartnertext() {
		return businesspartnertext;
	}
	public void setBusinesspartnertext(String businesspartnertext) {
		this.businesspartnertext = businesspartnertext;
	}
	public String getQuantity() {
		return quantity;
	}
	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}
	public String getUnitofmeasure() {
		return unitofmeasure;
	}
	public void setUnitofmeasure(String unitofmeasure) {
		this.unitofmeasure = unitofmeasure;
	}
	public String getContactpersonemailid() {
		return contactpersonemailid;
	}
	public void setContactpersonemailid(String contactpersonemailid) {
		this.contactpersonemailid = contactpersonemailid;
	}
	public String getContactpersonphone() {
		return contactpersonphone;
	}
	public void setContactpersonphone(String contactpersonphone) {
		this.contactpersonphone = contactpersonphone;
	}
	public String getDeliveryaddress1() {
		return deliveryaddress1;
	}
	public void setDeliveryaddress1(String deliveryaddress1) {
		this.deliveryaddress1 = deliveryaddress1;
	}
	public String getDeliveryaddress2() {
		return deliveryaddress2;
	}
	public void setDeliveryaddress2(String deliveryaddress2) {
		this.deliveryaddress2 = deliveryaddress2;
	}
	public String getDeliveryaddress3() {
		return deliveryaddress3;
	}
	public void setDeliveryaddress3(String deliveryaddress3) {
		this.deliveryaddress3 = deliveryaddress3;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getPincode() {
		return pincode;
	}
	public void setPincode(String pincode) {
		this.pincode = pincode;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getIgstamount() {
		return igstamount;
	}
	public void setIgstamount(String igstamount) {
		this.igstamount = igstamount;
	}
	public String getCgstamount() {
		return cgstamount;
	}
	public void setCgstamount(String cgstamount) {
		this.cgstamount = cgstamount;
	}
	public String getSgstamount() {
		return sgstamount;
	}
	public void setSgstamount(String sgstamount) {
		this.sgstamount = sgstamount;
	}
	public String getOrdernumber() {
		return ordernumber;
	}
	public void setOrdernumber(String ordernumber) {
		this.ordernumber = ordernumber;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public String getMaterial_type() {
		return material_type;
	}
	public void setMaterial_type(String material_type) {
		this.material_type = material_type;
	}
	public String getStoragelocation() {
		return storagelocation;
	}
	public void setStoragelocation(String storagelocation) {
		this.storagelocation = storagelocation;
	}
	public String getMaterial() {
		return material;
	}
	public void setMaterial(String material) {
		this.material = material;
	}
	public String getHsnno() {
		return hsnno;
	}
	public void setHsnno(String hsnno) {
		this.hsnno = hsnno;
	}
	public String getRateperqty() {
		return rateperqty;
	}
	public void setRateperqty(String rateperqty) {
		this.rateperqty = rateperqty;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getDeliverydate() {
		return deliverydate;
	}
	public void setDeliverydate(String deliverydate) {
		this.deliverydate = deliverydate;
	}
	public String getContractagreementno() {
		return contractagreementno;
	}
	public void setContractagreementno(String contractagreementno) {
		this.contractagreementno = contractagreementno;
	}
	public String getContractagreementitemno() {
		return contractagreementitemno;
	}
	public void setContractagreementitemno(String contractagreementitemno) {
		this.contractagreementitemno = contractagreementitemno;
	}
	public String getContractagreementdate() {
		return contractagreementdate;
	}
	public void setContractagreementdate(String contractagreementdate) {
		this.contractagreementdate = contractagreementdate;
	}
	public String getDeladdresstitle() {
		return deladdresstitle;
	}
	public void setDeladdresstitle(String deladdresstitle) {
		this.deladdresstitle = deladdresstitle;
	}
	public String getDeladdressname() {
		return deladdressname;
	}
	public void setDeladdressname(String deladdressname) {
		this.deladdressname = deladdressname;
	}
	public String getDeladdressdistrict() {
		return deladdressdistrict;
	}
	public void setDeladdressdistrict(String deladdressdistrict) {
		this.deladdressdistrict = deladdressdistrict;
	}
	public String getDeladdressregioncode() {
		return deladdressregioncode;
	}
	public void setDeladdressregioncode(String deladdressregioncode) {
		this.deladdressregioncode = deladdressregioncode;
	}
	public String getPrnumber() {
		return prnumber;
	}
	public void setPrnumber(String prnumber) {
		this.prnumber = prnumber;
	}
	public String getForeclosestatuscheck() {
		return foreclosestatuscheck;
	}
	public void setForeclosestatuscheck(String foreclosestatuscheck) {
		this.foreclosestatuscheck = foreclosestatuscheck;
	}
	public String getBalance_qty() {
		return balance_qty;
	}
	public void setBalance_qty(String balance_qty) {
		this.balance_qty = balance_qty;
	}
	@Override
	public String toString() {
		return "PoeventdetailsPojo [ponumber=" + ponumber + ", businesspartneroid=" + businesspartneroid
				+ ", lineitemnumber=" + lineitemnumber + ", lineitemtext=" + lineitemtext + ", company=" + company
				+ ", plant=" + plant + ", department=" + department + ", costcentre=" + costcentre + ", category="
				+ category + ", businesspartnertext=" + businesspartnertext + ", quantity=" + quantity
				+ ", unitofmeasure=" + unitofmeasure + ", contactpersonemailid=" + contactpersonemailid
				+ ", contactpersonphone=" + contactpersonphone + ", deliveryaddress1=" + deliveryaddress1
				+ ", deliveryaddress2=" + deliveryaddress2 + ", deliveryaddress3=" + deliveryaddress3 + ", city=" + city
				+ ", state=" + state + ", country=" + country + ", pincode=" + pincode + ", status=" + status
				+ ", igstamount=" + igstamount + ", cgstamount=" + cgstamount + ", sgstamount=" + sgstamount
				+ ", ordernumber=" + ordernumber + ", remark=" + remark + ", material_type=" + material_type
				+ ", storagelocation=" + storagelocation + ", material=" + material + ", hsnno=" + hsnno
				+ ", rateperqty=" + rateperqty + ", currency=" + currency + ", deliverydate=" + deliverydate
				+ ", contractagreementno=" + contractagreementno + ", contractagreementitemno="
				+ contractagreementitemno + ", contractagreementdate=" + contractagreementdate + ", deladdresstitle="
				+ deladdresstitle + ", deladdressname=" + deladdressname + ", deladdressdistrict=" + deladdressdistrict
				+ ", deladdressregioncode=" + deladdressregioncode + ", prnumber=" + prnumber
				+ ", foreclosestatuscheck=" + foreclosestatuscheck + ", balance_qty=" + balance_qty + "]";
	}
	
	
	

}
