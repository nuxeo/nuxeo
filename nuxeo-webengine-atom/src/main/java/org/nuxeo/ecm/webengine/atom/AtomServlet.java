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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.atom;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.protocol.server.Provider;
import org.apache.abdera.protocol.server.servlet.AbderaServlet;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.session.UserSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AtomServlet extends AbderaServlet {

    private static final long serialVersionUID = 1L;

    protected Provider createProvider() {
        NuxeoProvider provider = new NuxeoProvider("/");

        DocumentWorkspaceManager wsMgr = new DocumentWorkspaceManager();
        provider.setWorkspaceManager(wsMgr);


        provider.init(getAbdera(), null);
        return provider;
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        //TODO this code must go into UserSession
        UserSession us = UserSession.getCurrentSession(request.getSession(true));
        CoreSession session = us.getCoreSession();
        if (session == null) {
            try {
                session = AbderaHelper.openSession();
            } catch (Exception e) {
                throw new ServletException("Failed to open core session", e);
            }
            us.setCoreSession(session);
        }
        super.service(request, response);
    }


}
