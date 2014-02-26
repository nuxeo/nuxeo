/**
 * License Agreement.
 *
 *  JBoss RichFaces - Ajax4jsf Component Library
 *
 * Copyright (C) 2007  Exadel, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package org.nuxeo.ecm.platform.ui.web.multipart;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.DEPLOYMENT;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;

/**
 * A filter for decoding multipart requests, for use with the file upload
 * control.
 * <p>
 * Nuxeo Filter to override buggy seam implementation, taken from RichFaces
 * (see NXP-5138), FRAMEWORK precedence is not enough (seam filter is
 * registered with APPLICATION precedence instead of BUILT_IN for some reason)
 *
 * @author Shane Bryzak
 */
// FIXME: disable override, maybe not needed anymore + MultiPartRequest should
// be adapted to JSF2
//@Scope(APPLICATION)
//@Name("org.jboss.seam.web.multipartFilter")
//@Install(precedence = DEPLOYMENT)
//@BypassInterceptors
//@Filter(within = { "org.jboss.seam.web.ajax4jsfFilter",
//        "org.jboss.seam.web.exceptionFilter" })
public class MultipartFilter extends org.jboss.seam.web.MultipartFilter {

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (!(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (isMultipartRequest(httpRequest)) {
            chain.doFilter(new MultipartRequest(httpRequest,
                    getCreateTempFiles(), getMaxRequestSize()), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isMultipartRequest(HttpServletRequest request) {
        if (!"post".equals(request.getMethod().toLowerCase())) {
            return false;
        }

        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }

        if (contentType.toLowerCase().startsWith(MULTIPART)) {
            return true;
        }

        return false;
    }
}
