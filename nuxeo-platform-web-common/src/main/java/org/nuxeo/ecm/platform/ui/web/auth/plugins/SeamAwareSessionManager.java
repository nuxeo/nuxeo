/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
//import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationSessionManager;

public class SeamAwareSessionManager extends DefaultSessionManager implements
        NuxeoAuthenticationSessionManager {

    /*

    @Override
    public boolean bypassRequest(ServletRequest request)
    {
        if (request instanceof FancyURLRequestWrapper)
            return true;
        return false;
    }

    @Override
    public void invalidateSession(ServletRequest request) {
        try {
            Seam.invalidateSession();
        }
        catch (Exception e) {
            super.invalidateSession(request);
        }
    }

    @Override
    public boolean isAvalaible(ServletRequest request) {
        if (FacesContext.getCurrentInstance()!=null)
            return true;
        return false;
    }

    @Override
    public HttpSession reinitSession(ServletRequest request) {
        // destroy session
        // because of Seam Phase Listener we can't use Seam.invalidateSession()
        // because the session would be invalidated at the end of the request !
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            // Make long-running conversation temporary
            Manager.instance().endConversation(true);
            Manager.instance().endRequest(externalContext.getSessionMap());
            ServletLifecycle.endRequest(httpRequest);
            session.invalidate();

            session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
            }

        }
        session = httpRequest.getSession(true);

        // reinit Seam so the afterResponseComplete does not crash
        ServletLifecycle.beginRequest(httpRequest);
    }


    @Override
    public String getBaseURL(ServletRequest request) {
        return BaseURL.getBaseURL(httpRequest);
    }

    */
}
