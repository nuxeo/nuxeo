/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.ui.web.rest;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.api.Framework;

/**
 * Filter used to decode URLs and wrap requests to enable encoding. This filter is useful because Nuxeo support
 * pluggable URL patterns
 *
 * @author tiry
 */
public class FancyURLFilter implements Filter {

    private static final Log log = LogFactory.getLog(FancyURLFilter.class);

    protected URLPolicyService urlService;

    protected ServletContext servletContext;

    @Override
    public void init(FilterConfig conf) throws ServletException {
        log.debug("Nuxeo5 URLFilter started");
        servletContext = conf.getServletContext();
    }

    protected URLPolicyService getUrlService() {
        if (urlService == null) {
            urlService = Framework.getService(URLPolicyService.class);
        }
        return urlService;
    }

    @Override
    public void destroy() {
        urlService = null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {
            getUrlService();
            // initialize its view id manager if necessary
            urlService.initViewIdManager(servletContext, httpRequest, httpResponse);

            // check if this is an URL that needs to be parsed
            if (urlService.isCandidateForDecoding(httpRequest)) {
                DocumentView docView = urlService.getDocumentViewFromRequest(httpRequest);
                // if parse succeeded => process
                if (docView != null) {
                    // put it in request
                    urlService.setDocumentViewInRequest(httpRequest, docView);

                    // get the view id for navigation from the stored outcome
                    String jsfOutcome = docView.getViewId();

                    // get target page according to navigation rules
                    String target = urlService.getViewIdFromOutcome(jsfOutcome, httpRequest);

                    // dispatch
                    RequestDispatcher dispatcher;
                    if (target != null) {
                        dispatcher = httpRequest.getRequestDispatcher(target);
                    } else {
                        // Use a dummy dispatcher if the target is not needed.
                        // This comes handy for instance for nxfile url
                        dispatcher = httpRequest.getRequestDispatcher("/malformed_url_error_page.faces");
                    }
                    // set force encoding in case forward triggers a
                    // redirect (when a seam page is processed for instance).
                    request.setAttribute(URLPolicyService.FORCE_URL_ENCODING_REQUEST_KEY, Boolean.TRUE);
                    // forward request to the target viewId
                    dispatcher.forward(new FancyURLRequestWrapper(httpRequest, docView),
                            wrapResponse(httpRequest, httpResponse));
                    return;
                }
            }

            // do not filter if it's candidate for encoding so soon : document
            // view has not been set in the request yet => always wrap
            chain.doFilter(request, wrapResponse(httpRequest, httpResponse));

        } catch (IOException e) {
            String url = httpRequest.getRequestURL().toString();
            if (DownloadHelper.isClientAbortError(e)) {
                DownloadHelper.logClientAbort(e);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Client disconnected from URL %s : %s", url, e.getMessage()));
                }
            } else {
                throw new IOException("On requestURL: " + url, e);
            }
        } catch (ServletException e) {
            String url = httpRequest.getRequestURL().toString();
            if (DownloadHelper.isClientAbortError(e)) {
                DownloadHelper.logClientAbort(e);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Client disconnected from URL %s : %s", url, e.getMessage()));
                }
            } else {
                throw new ServletException("On requestURL: " + url, e);
            }
        }

    }

    protected ServletResponse wrapResponse(HttpServletRequest request, HttpServletResponse response) {
        return new FancyURLResponseWrapper(response, request);
    }

}
