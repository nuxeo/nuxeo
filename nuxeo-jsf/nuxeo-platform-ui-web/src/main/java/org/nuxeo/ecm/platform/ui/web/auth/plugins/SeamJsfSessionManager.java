/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jboss.seam.Seam;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.platform.ui.web.rest.FancyURLRequestWrapper;

public class SeamJsfSessionManager extends DefaultSessionManager {

    @Override
    public boolean canBypassRequest(ServletRequest request) {
        return request instanceof FancyURLRequestWrapper;
    }

    @Override
    public boolean invalidateSession(ServletRequest request) {
        try {
            Seam.invalidateSession();
            return true;
        } catch (RuntimeException e) {
            // TODO what is caught here?
            return super.invalidateSession(request);
        }
    }

    @Override
    public void onBeforeSessionReinit(ServletRequest request) {
        // destroy session
        // because of Seam Phase Listener we can't use Seam.invalidateSession()
        // because the session would be invalidated at the end of the request !
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                ExternalContext externalContext = facesContext.getExternalContext();
                // Make long-running conversation temporary
                Manager.instance().endConversation(true);
                Manager.instance().endRequest(externalContext.getSessionMap());
                ServletLifecycle.endRequest(httpRequest);
            }
        }
    }

    @Override
    public void onAfterSessionReinit(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // reinit Seam so the afterResponseComplete does not crash
        ServletLifecycle.beginRequest(httpRequest);
    }

}
