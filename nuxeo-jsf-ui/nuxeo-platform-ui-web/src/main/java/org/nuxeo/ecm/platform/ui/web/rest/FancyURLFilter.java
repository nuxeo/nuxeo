/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    /**
     * @deprecated since 11.1. Use {@link Framework#getService(Class)} with {@link URLPolicyService} instead.
     */
    @Deprecated
    protected URLPolicyService urlService;

    protected ServletContext servletContext;

    @Override
    public void init(FilterConfig conf) throws ServletException {
        log.debug("Nuxeo5 URLFilter started");
        servletContext = conf.getServletContext();
    }


    /**
     * @deprecated since 11.1. Use {@link Framework#getService(Class)} with {@link URLPolicyService} instead.
     */
    @Deprecated
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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        URLPolicyService urlService = Framework.getService(URLPolicyService.class);
        try {
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
