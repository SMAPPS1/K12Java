package com.bccl.dxapi.security;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.bccl.dxapi.apiimpl.InternalportalImpl;
import com.bccl.dxapi.apiimpl.LoginImpl;

public class RestAuthenticationFilter implements Filter {

	static Logger log = Logger.getLogger(RestAuthenticationFilter.class.getName());

	public static final String AUTHENTICATION_HEADER = "Authorization";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filter)
			throws IOException, ServletException {
		if (request instanceof javax.servlet.http.HttpServletRequest) {

			String credvalue = "";
			javax.servlet.http.HttpServletRequest httpServletRequest = (javax.servlet.http.HttpServletRequest) request;
			String authCredentials = httpServletRequest.getHeader(AUTHENTICATION_HEADER);
			javax.servlet.http.HttpSession session = httpServletRequest.getSession(false);

			// log.info("Request received in Authentication Filter node: "+httpServletRequest.getLocalAddr());
			// log.info("Session id in Authentication Filter node: "+(session!= null?session.getId():0));
			// log.info(httpServletRequest.getRequestURI());
			if (httpServletRequest.getRequestURI().equals("/DxProject/api/file/download")
					|| httpServletRequest.getRequestURI().equals("/DxProject/api/file/downloadPo")
					||  httpServletRequest.getRequestURI().equals("/DxProject/api/file/vendorProfile")
					||  httpServletRequest.getRequestURI().equals("/DxProject/api/file/submitVendorReg")
					||  httpServletRequest.getRequestURI().equals("/DxProject/api/file/downloadPopdf")
					||  httpServletRequest.getRequestURI().equals("/DxProject/api/file/fileuploads")
					|| httpServletRequest.getRequestURI().equals("/DxProject/api/internalportal")
					|| httpServletRequest.getRequestURI().equals("/DxProject/api/podetails/asnhistorydownload")) {
				filter.doFilter(request, response);
//				log.info("Calling API.. Returning Authorization");
				return;
			}

			if (session == null) {
				javax.servlet.http.HttpServletResponse httpServletResponse = (javax.servlet.http.HttpServletResponse) response;
				httpServletResponse.setStatus(httpServletResponse.SC_REQUEST_TIMEOUT);
				return;
			}

			String type = "";
			if (session.getAttribute("type") != null) {

				if (session.getAttribute("type").equals("vendor")) {
					type = "vendor";
					if (session.getAttribute("pan") != null) {
						credvalue = (String) session.getAttribute("pan");
					}
				}else if (session.getAttribute("type").equals("internal")) {
					type = "internal";
					if (session.getAttribute("email") != null) {
						credvalue = (String) session.getAttribute("email");
					}
				}
			}

			boolean authenticationStatus = isUserAuthenticated(authCredentials, credvalue, session, type,
					httpServletRequest);

//			log.info(" In RestAuthentication filter : authenticationStatus = " + authenticationStatus);

			if (authenticationStatus) {
				filter.doFilter(request, response);
			} else {
				if (response instanceof javax.servlet.http.HttpServletResponse) {
					log.info("In RestAuthenticationFilter : servlet response SC_UNAUTHORIZED: credvalue = " + credvalue);
					javax.servlet.http.HttpServletResponse httpServletResponse = (javax.servlet.http.HttpServletResponse) response;
					httpServletResponse.setStatus(httpServletResponse.SC_UNAUTHORIZED);
				}
			}
		}
	}

	private boolean setInternalUser(String tokenemailid, HttpSession session) {
		LoginImpl objlogin = new LoginImpl();
		InternalportalImpl internalport = new InternalportalImpl();
		boolean status = false;
		try {
			JSONArray jsonArray = objlogin.getChecktheStoreKepeer(tokenemailid, session);
			JSONObject jo = (JSONObject) jsonArray.get(0);
			if (jo.get("status").toString().equalsIgnoreCase("Success")) {
				JSONArray jsonArray2 = internalport.getportaltype(tokenemailid, session);
				JSONObject jo2 = (JSONObject) jsonArray2.get(0);
				String sessionstatus = setsessionvalues(session, jo2.get("mode").toString(), tokenemailid);
				if (sessionstatus.equalsIgnoreCase("Success")) {
					status = true;
				}
			} else {
				status = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return status;
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}

	@Override
	public void destroy() {
	}

	private boolean isUserAuthenticated(String authString, String credvalue, javax.servlet.http.HttpSession session,
			String type, HttpServletRequest httpServletRequest) {
		boolean authenticationStatus = false;
		try {
			if (authString != null && !authString.equals("") && credvalue != null && !credvalue.equals("")) {
				byte[] decodeToken = authString.getBytes();
				String decrupttoken = GenrateToken.decrypt(decodeToken);
				String decodedAuth = new String(decrupttoken);
				String[] parts = decodedAuth.split(":");
				String tockenEmailId = parts[0];

				if (credvalue.equals(tockenEmailId)) {
					authenticationStatus = true;
				}
				if (type.equalsIgnoreCase("internal") && authenticationStatus == false) {
					session.invalidate();
					session = null;
					session = httpServletRequest.getSession();
					authenticationStatus = setInternalUser(tockenEmailId, session);
				}
			}
		} catch (Exception e) {
			log.error("Exception in isUserAuthenticated :",e.fillInStackTrace());
		}
		return authenticationStatus;
	}

	synchronized String setsessionvalues(HttpSession session, String mode, String tokenemailid) {

		session.setAttribute("email", tokenemailid);
		session.setAttribute("type", "internal");
		session.setAttribute("mode", mode);
		return "Success";
	}

}
