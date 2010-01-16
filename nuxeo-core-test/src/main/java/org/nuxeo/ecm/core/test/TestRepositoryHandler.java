/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
        CoreSession coreSession = new LocalSession();
        coreSession.connect(repositoryName, ctx);
        return coreSession;
    }

    public CoreSession changeUser(CoreSession session, String newUser)
            throws ClientException {
        releaseSession(session);
        return openSessionAs(newUser);
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
