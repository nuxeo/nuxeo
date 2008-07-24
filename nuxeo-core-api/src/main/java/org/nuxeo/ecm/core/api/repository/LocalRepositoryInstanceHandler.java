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

package org.nuxeo.ecm.core.api.repository;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LocalRepositoryInstanceHandler extends RepositoryInstanceHandler {

    protected Principal principal;

    public LocalRepositoryInstanceHandler(Repository repository, NuxeoPrincipal principal) {
        super (repository);
        this.principal = principal;
    }

    public LocalRepositoryInstanceHandler(Repository repository, String username) {
        super (repository);
        this.principal = new UserPrincipal(username);
    }

    @Override
    protected void open(Repository repository) throws Exception {
        session = Framework.getLocalService(CoreSession.class);
        String repositoryUri = repository.getRepositoryUri();
        if (repositoryUri == null) {
            repositoryUri = repository.getName();
        }
        HashMap<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("principal", (NuxeoPrincipal)principal);
        String sid = session.connect(repositoryUri, ctx);
        // register session on local JVM so it can be used later by doc models
        CoreInstance.getInstance().registerSession(sid, proxy);
    }

}
