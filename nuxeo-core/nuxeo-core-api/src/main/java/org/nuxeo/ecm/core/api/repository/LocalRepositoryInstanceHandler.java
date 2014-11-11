/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LocalRepositoryInstanceHandler extends RepositoryInstanceHandler {

    private static final long serialVersionUID = 1L;

    protected final Principal principal;

    public LocalRepositoryInstanceHandler(Repository repository,
            NuxeoPrincipal principal) {
        super(repository);
        this.principal = principal;
    }

    public LocalRepositoryInstanceHandler(Repository repository, String username) {
        this(repository, new UserPrincipal(username, new ArrayList<String>(),
                false, false));
    }

    @Override
    protected void open(Repository repository) throws Exception {
        session = Framework.getLocalService(CoreSession.class);
        String repositoryUri = repository.getRepositoryUri();
        if (repositoryUri == null) {
            repositoryUri = repository.getName();
        }
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("principal", (NuxeoPrincipal) principal);
        String sid = session.connect(repositoryUri, ctx);
        // register proxy on local JVM so it can be used later by doc models
        CoreInstance.getInstance().registerSession(sid, proxy);
    }

}
