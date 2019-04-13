/*
 *  (C) Copyright 2000-2003 Yale University. All rights reserved.
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
 * <p>
 * Filter protects resources such that only specified usernames, as authenticated with CAS, can access.
 * </p>
 * <p>
 * <code>edu.yale.its.tp.cas.client.filter.user</code> must be set before this filter in the filter chain.
 * </p>
 * <p>
 * This filter takes the init-param <code>edu.yale.its.tp.cas.client.filter.authorizedUsers</code>, a
 * whitespace-delimited list of users authorized to pass through this filter.
 * </p>
 *
 * @author Andrew Petro
 */
public class SimpleCASAuthorizationFilter implements Filter {

    // *********************************************************************
    // Constants

    public static final String AUTHORIZED_USER_STRING = "edu.yale.its.tp.cas.client.filter.authorizedUsers";

    public static final String FILTER_NAME = "SimpleCASAuthorizationFilter";

    // *********************************************************************
    // Configuration state

    private String authorizedUsersString;

    private List<String> authorizedUsers;

    // *********************************************************************
    // Initialization

    @Override
    public void init(FilterConfig config) throws ServletException {
        this.authorizedUsersString = config.getInitParameter(AUTHORIZED_USER_STRING);
        StringTokenizer tokenizer = new StringTokenizer(authorizedUsersString);
        this.authorizedUsers = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            this.authorizedUsers.add((String) tokenizer.nextElement());
        }
    }

    // *********************************************************************
    // Filter processing

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws ServletException,
            IOException {

        // make sure we've got an HTTP request
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException(FILTER_NAME + ": protects only HTTP resources");
        }

        HttpSession session = ((HttpServletRequest) request).getSession();

        if (this.authorizedUsers.isEmpty()) {
            // user cannot be authorized if no users are authorized
            // break the fiter chain by throwing exception
            throw new ServletException(FILTER_NAME + ": no authorized users set.");

        } else if (!this.authorizedUsers.contains((session.getAttribute(CASFilter.CAS_FILTER_USER)))) {
            // this user is not among the authorized users
            // break the filter chain by throwing exception
            throw new ServletException(FILTER_NAME + ": user " + session.getAttribute(CASFilter.CAS_FILTER_USER)
                    + " not authorized.");
        }

        // continue processing the request
        fc.doFilter(request, response);
    }

    // *********************************************************************
    // Destruction

    @Override
    public void destroy() {
    }

}
