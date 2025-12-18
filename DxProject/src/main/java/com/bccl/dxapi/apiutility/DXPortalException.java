package com.bccl.dxapi.apiutility;

public class DXPortalException extends Exception{

	public String reason;
	public String subReason;
	
	public DXPortalException(){
		
	}
	
	public DXPortalException(String error,String errorDesc){		
		reason= error;
		subReason = errorDesc;
		
	}
	
}
