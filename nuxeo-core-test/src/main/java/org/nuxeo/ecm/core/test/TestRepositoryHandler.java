/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.model.Repository;

public class TestRepositoryHandler {

    private Repository repository;

    private final String repositoryName;

    public TestRepositoryHandler(String name) {
        repositoryName = name;
    }

    public void openRepository() throws Exception {
        repository = NXCore.getRepositoryService().getRepositoryManager().getRepository(
                repositoryName);
    }

    public CoreSession openSessionAs(String userName) throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", userName);
        CoreSession session = LocalSession.createInstance();
        session.connect(repositoryName, ctx);
        return session;
    }

    public CoreSession openSession(Map<String, Serializable> ctx)
            throws ClientException {
        if (ctx == null) {
            throw new IllegalArgumentException(
                    "The session context cannot be null");
        }
        CoreSession session = LocalSession.createInstance();
        session.connect(repositoryName, ctx);
        return session;
    }

    public void releaseRepository() {
        if (repository != null) {
            repository.shutdown();
        }
    }

    public void releaseSession(CoreSession session) {
        CoreInstance.getInstance().close(session);
    }
}
