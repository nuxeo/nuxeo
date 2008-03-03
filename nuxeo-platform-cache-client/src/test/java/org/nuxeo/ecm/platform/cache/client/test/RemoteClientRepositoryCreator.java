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
 * $Id: LogEntryCallbackListener.java 16046 2007-04-12 14:34:58Z fguillaume $
 */
package org.nuxeo.ecm.platform.cache.client.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.api.ECM;
import org.nuxeo.ecm.platform.api.login.UserSession;
import org.nuxeo.ecm.platform.api.test.NXClientTestCase;

/**
 *
 * @author <a href="mailto:dms@nuxeo.com">Dragos Mihalache</a>
 */
public class RemoteClientRepositoryCreator extends NXClientTestCase {

    private static final Log log = LogFactory.getLog(NXClientTestCase.class);

    UserSession us;

    CoreSession coreSession;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // we deploy this first to test pending bindings
        deploy("OSGI-INF/CacheBinding.xml");
        deploy("OSGI-INF/PlatformService.xml");
        deploy("DefaultPlatform.xml");

        us = new UserSession("Administrator", "Administrator");
        us.login();
        // ------------ user session started -----------

        coreSession = ECM.getPlatform().openRepository("demo");

        assertNotNull("CoreSession not found", coreSession);

        log.info("--------------------------------------------------------");
        log.info("");
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        log.info("");
        log.info("--------------------------------------------------------");

        CoreInstance.getInstance().close(coreSession);
        // ---------------------------------------------------
        us.logout();

        super.tearDown();
    }

    private void createDocuments(DocumentModel folder, int childrenCount)
            throws ClientException {

        final String logPrefix = "<createDocuments> ";

        for (int i = 0; i < childrenCount; i++) {

            log.info(logPrefix + "#: " + i);

            DocumentModel file = new DocumentModelImpl(
                    folder.getPathAsString(), "file#_" + i, "File");
            file = coreSession.createDocument(file);
            file.setProperty("dublincore", "title", "file_" + i);
            coreSession.saveDocument(file);
        }

        coreSession.save();
    }

    private DocumentModel getWorkspacesDM() throws ClientException {
        final String logPrefix = "<getWorkspacesDM> ";

        DocumentModel root = coreSession.getRootDocument();
        DocumentModelList dmlist = coreSession.getChildren(root.getRef());
        DocumentModel domain = dmlist.get(0);
        if (domain == null) {
            return null;
        }
        dmlist = coreSession.getChildren(domain.getRef());

        printDocuments(dmlist, logPrefix);

        DocumentModel workspacesDM = dmlist.get(0);

        return workspacesDM;
    }

    private DocumentModel createWorkspace(DocumentModel workspacesDM)
            throws ClientException {

        DocumentModelList dmlist = coreSession.getChildren(workspacesDM.getRef());

        // create a new workspace
        String name = "wsp_" + dmlist.size();
        DocumentModel workspaceDM = new DocumentModelImpl(
                workspacesDM.getPathAsString(), name, "Workspace");
        workspaceDM = coreSession.createDocument(workspaceDM);
        workspaceDM.setProperty("dublincore", "title", name);
        coreSession.saveDocument(workspaceDM);

        return workspaceDM;
    }

    public void testCreateWSChildren() throws Exception {
        DocumentModel workspacesDM = getWorkspacesDM();

        DocumentModel workspace = createWorkspace(workspacesDM);

        createDocuments(workspace, 12);
    }

    public void _testRetrieveChildren() throws Exception {
        final String logPrefix = "<testRetrieveChildren> ";

        DocumentModel parent = getWorkspacesDM();

        log.info(logPrefix + "calling getChildrenIterator...");

        // DocumentModelList dmList = coreSession.getChildren(root.getRef());

        DocumentModelIterator dmIterator = coreSession.getChildrenIterator(parent.getRef());

        printDocuments(dmIterator, logPrefix);
    }

    public void testRetrieveBySearch() throws Exception {
        final String logPrefix = "<testRetrieveBySearch> ";

        DocumentModel parent = getWorkspacesDM();

        log.info(logPrefix + "calling search...");

        String startingPath = "//"; //parent.getPathAsString();
        log.info(logPrefix + "startingPath: " + startingPath);
        DocumentModelIterator dmIterator = coreSession.querySimpleFtsIt("file", startingPath, null, 10);
        //DocumentModelList dmIterator = coreSession.query("SELECT * FROM Document");// WHERE ecm:path STARTSWITH '" + startingPath + "'");

        printDocuments(dmIterator, logPrefix);
    }

    private void printDocuments(DocumentModelList dmList, String logPrefix) {
        int i = 0;
        for (DocumentModel model : dmList) {
            String title = (String) model.getProperty("dublincore", "title");
            log.info(logPrefix + "title: " + title + "; path: "
                    + model.getPathAsString());
            i++;
        }

        log.info(logPrefix + "count: " + i);
    }

    private void printDocuments(DocumentModelIterator dmIterator,
            String logPrefix) {
        int i = 0;
        for (DocumentModel model : dmIterator) {
            String title = (String) model.getProperty("dublincore", "title");
            log.info(logPrefix + "title: " + title + "; path: "
                    + model.getPathAsString());
            i++;
        }

        log.info(logPrefix + "count: " + i);
    }

}
