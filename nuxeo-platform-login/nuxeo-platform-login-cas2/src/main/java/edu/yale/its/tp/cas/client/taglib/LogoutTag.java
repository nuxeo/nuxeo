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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * <p>Logout tag for use with the Yale Central Authentication
 * Service.  Clears the indicated attribute and, if 'scope' is 'session',
 * also invalidates the session.  Finally, redirects to CAS's
 * logout URL.</p>
 *
 * @author Shawn Bayern
 */
public class LogoutTag extends TagSupport {

  //*********************************************************************
  // Internal state

  private String var;					// tag attribute
  private String logoutUrl;				// tag attribute
  private int scope;					// tag attribute

  //*********************************************************************
  // Tag logic

  public int doStartTag() throws JspException {
    try {

      // retrieve the response object
      HttpServletResponse response =
        (HttpServletResponse) pageContext.getResponse();

      // kill the authentication information
      pageContext.removeAttribute(var, scope);

      // if scope is SESSION_SCOPE, invalidate the session
      if (scope == PageContext.SESSION_SCOPE)
        pageContext.getSession().invalidate();

      // send the redirect
      response.sendRedirect(logoutUrl);

      return SKIP_BODY;
	
    } catch (IOException ex) {
      throw new JspTagException(ex.getMessage());
    }
  }

  public int doEndTag() {
    return SKIP_PAGE;
  }

  //*********************************************************************
  // Accessors

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

  public void setLogoutUrl(String logoutUrl) {
    this.logoutUrl = logoutUrl;
  }


  //*********************************************************************
  // Constructor and lifecycle management

  public LogoutTag() {
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
    var = logoutUrl = null;
    scope = PageContext.PAGE_SCOPE;
  }
}
