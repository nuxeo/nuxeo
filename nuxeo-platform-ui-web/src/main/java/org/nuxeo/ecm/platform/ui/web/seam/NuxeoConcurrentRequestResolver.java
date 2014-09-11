/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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

package org.nuxeo.ecm.platform.ui.web.seam;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.ConversationEntry;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.jsf.concurrency.AbstractResolver;
import org.jboss.seam.jsf.concurrency.ConcurrentRequestResolver;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Default implementation to handle concurrent requests against the same Seam
 * Conversation. This component can be overridden using the standard Seam
 * override system.
 *
 * @since 5.7.3
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Scope(ScopeType.STATELESS)
@Name(ConcurrentRequestResolver.NAME)
@Install(precedence = FRAMEWORK)
@BypassInterceptors
public class NuxeoConcurrentRequestResolver extends AbstractResolver implements
        ConcurrentRequestResolver {

    public boolean handleConcurrentRequest(ConversationEntry ce,
            HttpServletRequest request, HttpServletResponse response) {

        if (request.getMethod().equalsIgnoreCase("get")) {
            // flag request to skip apply method bindings
            request.setAttribute(URLPolicyService.DISABLE_ACTION_BINDING_KEY,
                    Boolean.TRUE);
            // let's try to continue
            addTransientMessage(
                    Severity.WARN,
                    "org.nuxeo.seam.concurrent.unsaferun",
                    "This page may be not up to date, an other concurrent requests is still running");
            return true;
        } else if (request.getMethod().equalsIgnoreCase("post")) {

            if (FacesContext.getCurrentInstance() == null) {
                // we are not inside JSF
                // may be an Automation call inside the Seam context
                // continuing is not safe !
                return false;
            }

            String url = request.getHeader("referer");
            if (url == null || url.length() == 0) {
                url = VirtualHostHelper.getServerURL(request)
                        + request.getRequestURI();
            }
            addTransientMessage(
                    Severity.WARN,
                    "org.nuxeo.seam.concurrent.skip",
                    "Your request was not processed because you already have a requests in processing.");
            // XXX should Mark Request for No Tx Commit !
            return handleRedirect(ce, response, url);
        } else {
            return handleNoResponse(response);
        }
    }

}
