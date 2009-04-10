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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 *
 * @author <a href="mailto:dms@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class TestVersioning extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(TestVersioning.class);

    CoreSession coreSession;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(Constants.CORE_BUNDLE,
                "OSGI-INF/CoreService.xml");
        deployContrib(Constants.CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        deployContrib(Constants.CORE_BUNDLE,
                "OSGI-INF/RepositoryService.xml");
        deployContrib(Constants.CORE_BUNDLE,
                "OSGI-INF/LifeCycleService.xml");

        deployContrib(Constants.CORE_FACADE_TESTS_BUNDLE,
                "TypeService.xml");
        deployContrib(Constants.CORE_FACADE_TESTS_BUNDLE,
                "test-CoreExtensions.xml");
        deployContrib(Constants.CORE_FACADE_TESTS_BUNDLE,
                "CoreTestExtensions.xml");
        deployContrib(Constants.CORE_FACADE_TESTS_BUNDLE,
                "LifeCycleServiceExtensions.xml");
        deployContrib(Constants.CORE_FACADE_TESTS_BUNDLE,
                "DemoRepository.xml");

        deployBundle("org.nuxeo.ecm.core.event");
        deployCustomVersioning();

        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        coreSession = CoreInstance.getInstance().open("default", ctx);
    }

    protected void deployCustomVersioning() throws Exception {
        // do nothing - will be overridden by a subclass that tests custom versioning
    }

    @Override
    public void tearDown() throws Exception {
        CoreInstance.getInstance().close(coreSession);
        super.tearDown();
    }

    public void testRemoveSingleDocVersion() throws ClientException {

        DocumentModel root = coreSession.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = coreSession.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = coreSession.createDocument(file);

        checkVersions(file, new String[0]);

        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        file = coreSession.saveDocument(file);

        checkVersions(file, new String[]{"1"});

        DocumentModel lastversion = coreSession.getLastDocumentVersion(file.getRef());
        assertNotNull(lastversion);

        log.info("removing version with label: " + lastversion.getVersionLabel());

        assertTrue(lastversion.isVersion());
        coreSession.removeDocument(lastversion.getRef());

        checkVersions(file, new String[0]);
    }

    /**
     * Creates 3 versions and removes the first.
     */
    public void testRemoveFirstDocVersion() throws ClientException {

        DocumentModel root = coreSession.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = coreSession.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = coreSession.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 0;
        DocumentModel firstversion = coreSession.getVersions(file.getRef()).get(
                VERSION_INDEX);
        assertNotNull(firstversion);

        log.info("removing version with label: " + firstversion.getVersionLabel());

        assertTrue(firstversion.isVersion());
        coreSession.removeDocument(firstversion.getRef());

        checkVersions(file, new String[] { "2", "3" });
    }

    /**
     * Creates 3 versions and removes the second.
     */
    public void testRemoveMiddleDocVersion() throws ClientException {

        DocumentModel root = coreSession.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = coreSession.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = coreSession.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 1;
        DocumentModel version = coreSession.getVersions(file.getRef()).get(VERSION_INDEX);
        assertNotNull(version);

        log.info("removing version with label: " + version.getVersionLabel());

        assertTrue(version.isVersion());
        coreSession.removeDocument(version.getRef());

        checkVersions(file, new String[] { "1", "3" });
    }

    /**
     * Creates 3 versions and removes the last.
     */
    public void testRemoveLastDocVersion() throws ClientException {

        DocumentModel root = coreSession.getRootDocument();

        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = coreSession.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = coreSession.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 2;
        DocumentModel lastversion = coreSession.getVersions(file.getRef()).get(VERSION_INDEX);
        assertNotNull(lastversion);

        log.info("removing version with label: " + lastversion.getVersionLabel());

        assertTrue(lastversion.isVersion());
        coreSession.removeDocument(lastversion.getRef());

        checkVersions(file, new String[]{"1", "2"});
    }

    private void createTrioVersions(DocumentModel file) throws ClientException {
        // create a first version
        file.setProperty("file", "filename", "A");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        file = coreSession.saveDocument(file);

        checkVersions(file, new String[] { "1" });

        // create a second version
        // make it dirty so it will be saved
        file.setProperty("file", "filename", "B");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        file = coreSession.saveDocument(file);

        checkVersions(file, new String[] { "1", "2" });

        // create a third version
        file.setProperty("file", "filename", "C");
        file.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        file = coreSession.saveDocument(file);

        checkVersions(file, new String[] { "1", "2", "3" });
    }

    private void checkVersions(DocumentModel doc, String[] labels) throws ClientException {
        List<DocumentModel> vers = coreSession.getVersions(doc.getRef());
        assertEquals(labels.length, vers.size());
        int i = 0;
        for (DocumentModel ver : vers) {
            assertTrue(ver.isVersion());
            assertEquals(labels[i], ver.getVersionLabel());
            i++;
        }
        List<DocumentRef> versionsRefs = coreSession.getVersionsRefs(doc.getRef());
        assertEquals(labels.length, versionsRefs.size());
    }

}
