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
 *     Thierry Delprat
 *
 * $Id: AnonymousLoginManager.java 26986 2007-11-08 23:39:15Z fguillaume $
 */

package org.nuxeo.ecm.webapp.security;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Thierry Delprat
 */
@Name("anonymousLoginManager")
@Scope(ScopeType.SESSION)
@Startup
public class AnonymousLoginManager implements Serializable {

    private static final long serialVersionUID = 1L;

    @Factory(autoCreate = true, value = "anonymousLoginEnabled", scope = ScopeType.APPLICATION)
    public boolean computeAnonymousLoginIsEnabled() throws Exception {
        UserManager um = Framework.getService(UserManager.class);
        String anonymous = um.getAnonymousUserId();
        return anonymous == null;
    }

    public String login() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        Object req = eContext.getRequest();
        Object resp = eContext.getResponse();
        if (req instanceof HttpServletRequest
                && resp instanceof HttpServletResponse && !context.getResponseComplete()) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;
            request.setAttribute(URLPolicyService.DISABLE_REDIRECT_REQUEST_KEY, Boolean.TRUE);
            response.sendRedirect(BaseURL.getBaseURL(request) + NXAuthConstants.LOGOUT_PAGE);
            context.responseComplete();
        }
        return null;
    }

}
