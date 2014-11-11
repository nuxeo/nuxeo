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
 * $Id: TestProcessDocumentAdapter.java 28925 2008-01-10 14:39:42Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.platform.workflow.web.adapter.ProcessDocument;

/**
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestProcessDocumentAdapter extends RepositoryTestCase {

    private Repository repository;

    private CoreSession coreSession;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.workflow.web.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.workflow.web.tests",
                "DocumentAdapterService.xml");

        deployContrib("org.nuxeo.ecm.platform.workflow.web",
                "OSGI-INF/document-adapter-service-contrib.xml");
    }

    @Override
    protected void tearDown() throws Exception {
        releaseRepository();
        super.tearDown();
    }

    @Override
    public Repository getRepository() throws Exception {
        if (repository == null) {
            // the repository should be deployed the last
            // after any other bundle that is deploying doctypes
            deployContrib("org.nuxeo.ecm.platform.workflow.web.tests",
                    "DemoRepository.xml");
            repository = NXCore.getRepositoryService().getRepositoryManager().getRepository(
                    "demo");
        }
        return repository;
    }

    @Override
    public void releaseRepository() {
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
    }

    protected void openCoreSession() throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", "Administrator");
        coreSession = CoreInstance.getInstance().open("demo", context);
        assertNotNull(coreSession);
    }

    @Override
    protected Session getSession() throws Exception {
        return getRepository().getSession(null);
    }

    public void testAdapter() throws Exception {
        openCoreSession();

        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel childFile = coreSession.createDocumentModel(
                rootDM.getPathAsString(), "testfile1", "File");

        // should fill datamodel
        childFile = coreSession.createDocument(childFile);

        DocumentModel doc = childFile;

        ProcessDocument pdoc = doc.getAdapter(ProcessDocument.class);
        assertNotNull(pdoc);
        assertNotNull(pdoc.getProcessInfo());

        // Because the test env doesn't expose mandatory workflow EJB for now.
        assertEquals(0, pdoc.getProcessInfo().length);
    }

}
