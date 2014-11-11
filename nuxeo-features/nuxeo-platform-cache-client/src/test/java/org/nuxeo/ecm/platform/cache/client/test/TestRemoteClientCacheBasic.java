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
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.api.ECM;
import org.nuxeo.ecm.platform.api.login.UserSession;
import org.nuxeo.ecm.platform.api.test.NXClientTestCase;

/**
 *
 * @author <a href="mailto:dms@nuxeo.com">Dragos Mihalache</a>
 */
public class TestRemoteClientCacheBasic extends NXClientTestCase {

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
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {

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

    public void testRetrieveChildren() throws Exception {
        final String logPrefix = "<testRetrieveChildren> ";

        DocumentModel root = coreSession.getRootDocument();

        createDocuments(root, 20);

        log.info(logPrefix + "calling getChildrenIterator...");

        // DocumentModelList dmList = coreSession.getChildren(root.getRef());

        DocumentModelIterator dmIterator = coreSession.getChildrenIterator(root.getRef());

        for (DocumentModel model : dmIterator) {
            String title = (String) model.getProperty("dublincore", "title");
            log.info(logPrefix + "title: " + title);
        }
    }

}
