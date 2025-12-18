package com.bccl.dxapi.security;

import java.io.IOException;
import java.util.Date;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class CORSFilter implements Filter {

	static Logger log = Logger.getLogger(CORSFilter.class.getName());
	
	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {

		javax.servlet.http.HttpServletRequest request = (javax.servlet.http.HttpServletRequest) servletRequest;
		javax.servlet.http.HttpServletResponse resp = (javax.servlet.http.HttpServletResponse) servletResponse;
		Date date = new Date();

		String origin = request.getHeader("Origin");
		String referrer = request.getHeader("referer");

		((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Origin", origin);
		((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Headers", "content-type,authorization,authorizationkey,Cookie,Set-Cookie");
		((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Credentials", "true");
		((HttpServletResponse) servletResponse).addHeader("Access-Control-Expose-Headers", "content-type,authorization,authorizationkey");
		((HttpServletResponse) servletResponse).addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		((HttpServletResponse) servletResponse).addHeader("X-XSS-Protection", "1");

		if (request.getMethod().equals("OPTIONS")) {
			resp.setStatus(HttpServletResponse.SC_ACCEPTED);
			return;
		}

		chain.doFilter(request, servletResponse);

	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}
}
