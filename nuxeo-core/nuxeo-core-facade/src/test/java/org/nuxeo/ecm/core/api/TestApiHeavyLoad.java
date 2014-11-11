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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import static org.nuxeo.ecm.core.api.Constants.CORE_BUNDLE;
import static org.nuxeo.ecm.core.api.Constants.CORE_FACADE_TESTS_BUNDLE;

/**
 * @author <a href="mailto:dms@nuxeo.com">Dragos Mihalache</a>
 */
public class TestApiHeavyLoad extends BaseTestCase {

    private static final Log log = LogFactory.getLog(TestApiHeavyLoad.class);

    @BeforeClass
    public static void startRuntime() throws Exception {
        runtime = new NXRuntimeTestCase() {};
        runtime.setUp();

        runtime.deployContrib(CORE_BUNDLE,
                "OSGI-INF/CoreService.xml");
        runtime.deployContrib(CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        runtime.deployContrib(CORE_BUNDLE,
                "OSGI-INF/RepositoryService.xml");

        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE,
                "TypeService.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE,
                "permissions-contrib.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE,
                "test-CoreExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE,
                "CoreTestExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE,
                "DemoRepository.xml");

        runtime.deployBundle("org.nuxeo.ecm.core.event");
    }

    // Tests

    protected void createChildDocuments(DocumentModel folder, int childrenCount)
            throws ClientException {

        for (int i = 0; i < childrenCount; i++) {
            DocumentModel file = new DocumentModelImpl(
                    folder.getPathAsString(), "file#_" + i, "File");
            file = session.createDocument(file);
            file.setProperty("dublincore", "title", "file_" + i);
            session.saveDocument(file);
        }

        session.save();
    }

    @Test
    public void testRetrieveChildren() throws ClientException {
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        createChildDocuments(folder, 50);

        DocumentModelIterator docsIt = session.getChildrenIterator(folder.getRef());

        int count = 0;
        while (docsIt.hasNext()) {
            DocumentModel dm = docsIt.next();
            assertEquals("file#_" + count, dm.getName());
            assertEquals("file_" + count, dm.getProperty("dublincore", "title"));
            count++;
        }
        assertEquals(50, count);
    }

    @Test
    public void testNXQuery() throws ClientException {
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        createChildDocuments(folder, 50);

        DocumentModelIterator docsIt = session.queryIt("SELECT * FROM Document", null, 10000);

        int count = 0;
        while (docsIt.hasNext()) {
            DocumentModel dm = docsIt.next();
            String name = dm.getName();
            assertTrue(name == null || name.startsWith("file#_") || name.equals("folder#1"));
            Object title = dm.getProperty("dublincore", "title");
            assertTrue(title == null || title.toString().startsWith("file_") || title.equals(""));
            count++;
        }

        // total docs = root + folder + children(50)
        assertEquals(52, count);
    }

    @Test
    public void testFtsQuery() throws ClientException {
        log.info("<testFtsQuery>... ");
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        createChildDocuments(folder, 50);

        int pageSize = 10;
        DocumentModelIterator docsIt = session.querySimpleFtsIt("file", null, pageSize);

        assertEquals(DocumentModelIterator.UNKNOWN_SIZE, docsIt.size());
    }

    public void OBSOLETEtestFtsQueryWithinPath() throws ClientException {
        log.info("<testFtsQueryWithinPath>... ");
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);
        createChildDocuments(folder, 10);

        DocumentModel folder2 = new DocumentModelImpl(folder.getPathAsString(),
                "folder#2", "Folder");
        folder2 = session.createDocument(folder2);

        createChildDocuments(folder2, 25);

        DocumentModel folder3 = new DocumentModelImpl(folder2.getPathAsString(),
                "folder#3", "Folder");
        folder3 = session.createDocument(folder3);

        createChildDocuments(folder3, 25);

        DocumentModelIterator docsIt = session.querySimpleFtsIt(
                "file", folder2.getPathAsString(), null, 10000);

        int count = 0;
        while (docsIt.hasNext()) {
            DocumentModel dm = docsIt.next();
            assertTrue(dm.getName().startsWith("file#_"));
            assertTrue(dm.getProperty("dublincore", "title").toString().startsWith("file_"));
            count++;
        }

        // total docs = root + folder + children(50)
        assertEquals(50, count);
    }

}
