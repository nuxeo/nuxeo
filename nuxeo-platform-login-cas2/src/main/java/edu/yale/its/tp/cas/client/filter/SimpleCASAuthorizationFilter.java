package edu.yale.its.tp.cas.client.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>Filter protects resources such that only specified usernames, as 
 * authenticated with CAS, can access.</p>
 * 
 * <p><code>edu.yale.its.tp.cas.client.filter.user</code> must be set before 
 * this filter in the filter chain.</p>
 * 
 * <p>This filter takes the init-param 
 * <code>edu.yale.its.tp.cas.client.filter.authorizedUsers</code>, a 
 * whitespace-delimited list of users authorized to pass through this 
 * filter.</p>
 *
 * @author Andrew Petro
 */
public class SimpleCASAuthorizationFilter implements Filter {

	//*********************************************************************
	// Constants

	public static final String AUTHORIZED_USER_STRING =
		"edu.yale.its.tp.cas.client.filter.authorizedUsers";
	public static final String FILTER_NAME = "SimpleCASAuthorizationFilter";

	//*********************************************************************
	// Configuration state

	private String authorizedUsersString;
	private List authorizedUsers;

	//*********************************************************************
	// Initialization 

	public void init(FilterConfig config) throws ServletException {
		this.authorizedUsersString =
			config.getInitParameter(AUTHORIZED_USER_STRING);
		StringTokenizer tokenizer = new StringTokenizer(authorizedUsersString);
		this.authorizedUsers = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			this.authorizedUsers.add(tokenizer.nextElement());
		}
	}

	//*********************************************************************
	// Filter processing

	public void doFilter(
		ServletRequest request,
		ServletResponse response,
		FilterChain fc)
		throws ServletException, IOException {

		// make sure we've got an HTTP request
		if (!(request instanceof HttpServletRequest)
			|| !(response instanceof HttpServletResponse)) {
			throw new ServletException(
				FILTER_NAME + ": protects only HTTP resources");
		}

		HttpSession session = ((HttpServletRequest) request).getSession();

		if (this.authorizedUsers.isEmpty()) {
			// user cannot be authorized if no users are authorized
			// break the fiter chain by throwing exception
			throw new ServletException(FILTER_NAME + ": no authorized users set.");

		} else if (
			!this.authorizedUsers.contains(
				((String) session.getAttribute(CASFilter.CAS_FILTER_USER)))) {
			// this user is not among the authorized users
			// break the filter chain by throwing exception
			throw new ServletException(
				FILTER_NAME
					+ ": user "
					+ session.getAttribute(CASFilter.CAS_FILTER_USER)
					+ " not authorized.");
		}

		// continue processing the request
		fc.doFilter(request, response);
	}

	//*********************************************************************
	// Destruction

	public void destroy() {
	}

}
