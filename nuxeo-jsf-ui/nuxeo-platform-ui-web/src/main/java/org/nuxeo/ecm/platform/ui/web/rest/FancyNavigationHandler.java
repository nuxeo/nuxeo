/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FancyNavigationHandler.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.util.Util;

/**
 * Navigation handler that keeps outcome information available so that it can be used for a document view when
 * redirecting to this context.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class FancyNavigationHandler extends ConfigurableNavigationHandler {

    private static final Log log = LogFactory.getLog(FancyNavigationHandler.class);

    private final NavigationHandler parent;

    public static final String DISABLE_REDIRECT_FOR_URL_REWRITE = FancyNavigationHandler.class.getCanonicalName()
            + ".DISABLE_REDIRECT_FOR_URL_REWRITE";

    public FancyNavigationHandler(NavigationHandler navigationHandler) {
        parent = navigationHandler;
    }

    @Override
    public void handleNavigation(FacesContext context, String fromAction, String outcome) {
        ExternalContext eContext = context.getExternalContext();
        HttpServletRequest httpRequest = (HttpServletRequest) eContext.getRequest();
        // put outcome in request params
        httpRequest.setAttribute(URLPolicyService.POST_OUTCOME_REQUEST_KEY, outcome);
        URLPolicyService pservice = Framework.getService(URLPolicyService.class);
        pservice.appendParametersToRequest(context);
        // get old root to check if it's changed
        UIViewRoot oldRoot = context.getViewRoot();
        parent.handleNavigation(context, fromAction, outcome);
        UIViewRoot newRoot = context.getViewRoot();
        boolean rootChanged = !oldRoot.equals(newRoot);
        if (outcome != null && !context.getResponseComplete() && !rootChanged && Framework.isDevModeSet()) {
            // navigation was not done => maybe a hot reload issue: perform
            // navigation again using local code because it uses
            // information from the StaticNavigationHandler that is
            // hot-reloaded correctly
            // TODO: check if still relevant in JSF2
            handleHotReloadNavigation(pservice, context, fromAction, outcome);
        }
        Object disable = httpRequest.getAttribute(DISABLE_REDIRECT_FOR_URL_REWRITE);
        if (Boolean.TRUE.equals(disable)) {
            // avoid redirect
            return;
        }
        // force redirect if outcome is null so that url can be
        // re-written except in an ajax request
        boolean ajaxRequest = context.getPartialViewContext().isAjaxRequest();
        if (outcome == null && !ajaxRequest && !context.getResponseComplete()) {
            String url = httpRequest.getRequestURL().toString();
            String localUrl = BaseURL.getServerURL(httpRequest, true);
            String baseUrl = BaseURL.getServerURL(httpRequest, false);
            if (localUrl != null && !localUrl.equals(baseUrl)) {
                url = StringUtils.replaceOnce(url, localUrl, baseUrl);
            }
            if (Contexts.isEventContextActive()) {
                // strip any jsession id before redirect
                int jsessionidIndex = url.indexOf(";jsessionid");
                if (jsessionidIndex != -1) {
                    url = url.substring(0, jsessionidIndex);
                }
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

    protected void handleHotReloadNavigation(URLPolicyService pservice, FacesContext context, String fromAction,
            String outcome) {
        String viewId = pservice.getViewIdFromOutcome(outcome, null);
        ExternalContext extContext = context.getExternalContext();
        if (viewId != null) {
            ViewHandler viewHandler = Util.getViewHandler(context);
            // always redirect
            String newPath = viewHandler.getActionURL(context, viewId);
            try {
                extContext.redirect(extContext.encodeActionURL(newPath));
            } catch (java.io.IOException ioe) {
                throw new FacesException(ioe.getMessage(), ioe);
            }
            context.responseComplete();
        }
    }

    @Override
    public NavigationCase getNavigationCase(FacesContext context, String fromAction, String outcome) {
        if (parent instanceof ConfigurableNavigationHandler) {
            return ((ConfigurableNavigationHandler) parent).getNavigationCase(context, fromAction, outcome);
        }
        return null;
    }

    @Override
    public Map<String, Set<NavigationCase>> getNavigationCases() {
        if (parent instanceof ConfigurableNavigationHandler) {
            return ((ConfigurableNavigationHandler) parent).getNavigationCases();
        }
        return null;
    }
}
