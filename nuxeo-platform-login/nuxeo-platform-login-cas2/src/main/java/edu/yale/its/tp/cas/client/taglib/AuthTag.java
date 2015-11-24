/*
 *  Copyright (c) 2000-2003 Yale University. All rights reserved.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, ARE EXPRESSLY
 *  DISCLAIMED. IN NO EVENT SHALL YALE UNIVERSITY OR ITS EMPLOYEES BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED, THE COSTS OF
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED IN ADVANCE OF THE POSSIBILITY OF SUCH
 *  DAMAGE.
 *
 *  Redistribution and use of this software in source or binary forms,
 *  with or without modification, are permitted, provided that the
 *  following conditions are met:
 *
 *  1. Any redistribution must include the above copyright notice and
 *  disclaimer and this list of conditions in any related documentation
 *  and, if feasible, in the redistributed software.
 *
 *  2. Any redistribution must include the acknowledgment, "This product
 *  includes software developed by Yale University," in any related
 *  documentation and, if feasible, in the redistributed software.
 *
 *  3. The names "Yale" and "Yale University" must not be used to endorse
 *  or promote products derived from this software.
 */

package edu.yale.its.tp.cas.client.taglib;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import edu.yale.its.tp.cas.client.*;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * <p>Authentication tag for use with the Yale Central Authentication
 * Service.</p>
 *
 * <p>Typical usage involves placing the tag at the top of the page.
 * The tag checks to determine if the attribute referenced by id/scope
 * exists; if it does, the tag has no runtime effect.  If the attribute
 * does not exist, however, a CAS authentication is necessary:
 * if no ticket is present, we redirect to CAS, and if a ticket is
 * present, we validate it.  Upon successful CAS authentication (either
 * by a pre-existing attribute or through CAS directly), we store the
 * NetID in the attribute referenced by id/scope.</p>
 *
 * @author Shawn Bayern
 * @author Drew Mazurek
 */
public class AuthTag extends TagSupport {

	//*********************************************************************
	// Internal state

	private String var; // tag attribute
	private int scope; // tag attribute
	private String casLogin, casValidate, service; // from children
	private List acceptedProxies; // from children
	private HttpServletRequest request;
	private HttpServletResponse response;

	//*********************************************************************
	// Tag logic

	public int doStartTag() throws JspException {
		// retrieve and save the request and response objects
		request = (HttpServletRequest) pageContext.getRequest();
		response = (HttpServletResponse) pageContext.getResponse();

		// reset invocation-specific state
		casLogin = null;
		casValidate = null;
		try {
			service =
				Util.getService(
					request,
					(String) pageContext.getServletContext().getInitParameter(
						"edu.yale.its.tp.cas.serverName"));
		} catch (ServletException ex) {
			throw new JspException(ex);
		}
		acceptedProxies = new ArrayList();
		return EVAL_BODY_INCLUDE;
	}

	public int doEndTag() throws JspTagException {
		try {
			// if our attribute's already present, don't do anything
			if (pageContext.getAttribute(var, scope) != null)
				return EVAL_PAGE;

			// otherwise, we need to authenticate via CAS
			String ticket = request.getParameter("ticket");

			// no ticket?  redirect...
			if (ticket == null || ticket.equals("")) {
				if (casLogin == null)
					throw new JspTagException(
						"for pages that expect to be called without 'ticket' parameter, "
							+ "cas:auth must have a cas:loginUrl subtag");
				response.sendRedirect(casLogin + "?service=" + service);
				return SKIP_PAGE;
			}

			// Yay, ticket!  Validate it.
			String netid = getAuthenticatedNetid(ticket);
			if (netid == null)
				throw new JspTagException("Unexpected CAS authentication error");

			// Store the authenticate user in the id/scope attribute
			pageContext.setAttribute(var, netid, scope);

			return EVAL_PAGE;

		} catch (IOException ex) {
			throw new JspTagException(ex.getMessage());
		} catch (SAXException ex) {
			throw new JspTagException(ex.getMessage());
		} catch (ParserConfigurationException ex) {
			throw new JspTagException(ex.getMessage());
		}
	}

	//*********************************************************************
	// Attribute accessors

	public void setVar(String var) {
		this.var = var;
	}

	public void setScope(String scope) {
		if (scope.equals("page"))
			this.scope = PageContext.PAGE_SCOPE;
		else if (scope.equals("request"))
			this.scope = PageContext.REQUEST_SCOPE;
		else if (scope.equals("session"))
			this.scope = PageContext.SESSION_SCOPE;
		else if (scope.equals("application"))
			this.scope = PageContext.APPLICATION_SCOPE;
		else
			throw new IllegalArgumentException("invalid scope");
	}

	//*********************************************************************
	// Accessors for child tags

	public void setCasLogin(String url) {
		casLogin = url;
	}

	public void setCasValidate(String url) {
		casValidate = url;
	}

	public void addAuthorizedProxy(String proxyId) {
		acceptedProxies.add(proxyId);
	}

	public void setService(String service) {
		this.service = service;
	}

	//*********************************************************************
	// Constructor and lifecycle management

	public AuthTag() {
		super();
		init();
	}

	// Releases any resources we may have (or inherit)
	public void release() {
		super.release();
		init();
	}

	// clears any internal state we might have
	private void init() {
		var = null;
		scope = PageContext.PAGE_SCOPE;
		casLogin = null;
		casValidate = null;
		acceptedProxies = null;
	}

	//*********************************************************************
	// Utility methods

	private String getAuthenticatedNetid(String ticket)
		throws ParserConfigurationException, SAXException, IOException, JspTagException {
		ProxyTicketValidator pv = new ProxyTicketValidator();
		pv.setCasValidateUrl(casValidate);
		pv.setServiceTicket(ticket);
		pv.setService(service);
		pv.validate();
		if (!pv.isAuthenticationSuccesful())
			throw new JspTagException(
				"CAS authentication error: " + pv.getErrorCode());
		if (pv.getProxyList().size() != 0) {
			// ticket was proxied
			if (acceptedProxies.size() == 0)
				throw new JspTagException("this page does not accept proxied tickets");
			else if (!acceptedProxies.contains(pv.getProxyList().get(0)))
				throw new JspTagException(
					"unauthorized top-level proxy: '" + pv.getProxyList().get(0) + "'");
		}
		return pv.getUser();
	}
}
