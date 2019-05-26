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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.action;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.FORCE_ANONYMOUS_LOGIN;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGOUT_PAGE;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * Logs the user in/out.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("loginLogoutAction")
@Scope(ScopeType.STATELESS)
public class LogoutAction extends InputController implements Serializable {

    private static final long serialVersionUID = 1L;

    public String login() {
        return navigationContext.goHome();
    }

    /**
     * Logs the user out. Invalidates the HTTP session so that it cannot be used anymore.
     *
     * @return the next page that is going to be displayed
     */
    public static String logout() throws IOException {
        Map<String, String> parameters = new HashMap<>();
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) eContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) eContext.getResponse();
        Principal principal = request.getUserPrincipal();
        if (principal instanceof NuxeoPrincipal) {
            NuxeoPrincipal nuxeoPrincipal = (NuxeoPrincipal) principal;
            if (nuxeoPrincipal.isAnonymous()) {
                parameters.put(FORCE_ANONYMOUS_LOGIN, "true");
            }
        }
        if (response != null && !context.getResponseComplete()) {
            String baseURL = BaseURL.getBaseURL(request) + LOGOUT_PAGE;
            request.setAttribute(DISABLE_REDIRECT_REQUEST_KEY, true);
            baseURL = URIUtils.addParametersToURIQuery(baseURL, parameters);
            response.sendRedirect(baseURL);
            context.responseComplete();
        }
        return null;
    }

}
