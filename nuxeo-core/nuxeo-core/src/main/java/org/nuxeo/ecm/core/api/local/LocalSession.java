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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.local;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LocalSession extends AbstractSession {

    private static final long serialVersionUID = 1L;

    private Session session;

    private NuxeoPrincipal principal;


    // Locally we don't yet support NXCore.getRepository()
    protected Session createSession(String repoName,
            Map<String, Serializable> context) throws ClientException {
        try {
            if (context != null) {
                principal = (NuxeoPrincipal) context.get("principal");
                if (principal == null) {
                    String username = (String) context.get("username");
                    if (username != null) {
                        principal = new UserPrincipal(username);
                    }
                }
            } else {
                context = new HashMap<String, Serializable>();
            }
            // store the principal in the core session context so that other core tools may retrieve it
            context.put("principal", principal);

            Repository repo = NXCore.getRepositoryService()
                    .getRepositoryManager().getRepository(repoName);
            return repo.getSession(context);
        } catch (Exception e) {
            throw new ClientException("Failed to load repository " + repoName, e);
        }
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    protected Session getSession() throws ClientException {
        if (session == null) {
            session = createSession(repositoryName, sessionContext);
        }
        return session;
    }

    @Override
    public boolean isSessionAlive() {
        return session != null && session.isLive();
    }

}
