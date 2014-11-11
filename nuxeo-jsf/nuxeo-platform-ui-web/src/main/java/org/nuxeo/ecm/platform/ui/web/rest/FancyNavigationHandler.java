/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FancyNavigationHandler.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest;

import java.io.IOException;
import java.util.Map;

import javax.faces.application.NavigationHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

/**
 * Navigation handler that keeps outcome information available so that it can be
 * used for a document view when redirecting to this context.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class FancyNavigationHandler extends NavigationHandler {

    private static final Log log = LogFactory.getLog(FancyNavigationHandler.class);

    private final NavigationHandler baseNavigationHandler;

    public FancyNavigationHandler(NavigationHandler navigationHandler) {
        baseNavigationHandler = navigationHandler;
    }

    @Override
    public void handleNavigation(FacesContext context, String fromAction,
            String outcome) {
        ExternalContext eContext = context.getExternalContext();
        HttpServletRequest httpRequest = (HttpServletRequest) eContext.getRequest();
        // put outcome in request params
        httpRequest.setAttribute(URLPolicyService.POST_OUTCOME_REQUEST_KEY,
                outcome);
        try {
            URLPolicyService pservice = Framework.getService(URLPolicyService.class);
            pservice.appendParametersToRequest(context);
        } catch (Exception e) {
            log.error("error occured while appending params to request: ", e);
        }
        baseNavigationHandler.handleNavigation(context, fromAction, outcome);
        // XXX AT: force redirect if outcome is null so that url can be
        // re-written except in an ajax request
        Map<String, String> map = eContext.getRequestParameterMap();
        boolean ajaxRequest = map != null && map.containsKey("AJAXREQUEST");
        if (outcome == null && !ajaxRequest && !context.getResponseComplete()) {
            String url = httpRequest.getRequestURL().toString();
            String localUrl = BaseURL.getServerURL(httpRequest, true);
            String baseUrl = BaseURL.getServerURL(httpRequest, false);
            if (localUrl != null && !localUrl.equals(baseUrl)) {
                url = url.replaceFirst(localUrl, baseUrl);
            }
            if (Contexts.isEventContextActive()) {
                // add conversation id before redirect
                url = RestHelper.addMainConversationParameters(url);
            }
            try {
                eContext.redirect(url);
            } catch (IOException e) {
                // do nothing...
                log.error(e, e);
            }
        }
    }
}
