/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Default implementation to handle concurrent requests against the same Seam Conversation. This component can be
 * overridden using the standard Seam override system.
 *
 * @since 5.7.3
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Scope(ScopeType.STATELESS)
@Name(ConcurrentRequestResolver.NAME)
@Install(precedence = FRAMEWORK)
@BypassInterceptors
public class NuxeoConcurrentRequestResolver extends AbstractResolver implements ConcurrentRequestResolver {

    @Override
    public boolean handleConcurrentRequest(ConversationEntry ce, HttpServletRequest request,
            HttpServletResponse response) {

        if (request.getMethod().equalsIgnoreCase("get")) {
            // flag request to skip apply method bindings
            request.setAttribute(URLPolicyService.DISABLE_ACTION_BINDING_KEY, Boolean.TRUE);
            // let's try to continue
            addTransientMessage(Severity.WARN, "org.nuxeo.seam.concurrent.unsaferun",
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
                url = VirtualHostHelper.getServerURL(request) + request.getRequestURI();
            }
            addTransientMessage(Severity.WARN, "org.nuxeo.seam.concurrent.skip",
                    "Your request was not processed because you already have a requests in processing.");
            // XXX should Mark Request for No Tx Commit !
            return handleRedirect(ce, response, url);
        } else {
            return handleNoResponse(response);
        }
    }

}
