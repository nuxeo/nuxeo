/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
    public void onBeforeSessionInvalidate(ServletRequest request) {
        try {
            Seam.invalidateSession();
        }
        catch (Exception e) {
            super.onBeforeSessionInvalidate(request);
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
