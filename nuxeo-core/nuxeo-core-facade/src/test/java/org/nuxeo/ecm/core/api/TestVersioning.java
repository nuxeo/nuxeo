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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import static org.nuxeo.common.collections.ScopeType.*;
import static org.nuxeo.ecm.core.api.Constants.CORE_BUNDLE;
import static org.nuxeo.ecm.core.api.Constants.CORE_FACADE_TESTS_BUNDLE;
import static org.nuxeo.ecm.core.api.facet.VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY;

/**
 * @author <a href="mailto:dms@nuxeo.com">Dragos Mihalache</a>
 */
public class TestVersioning extends BaseTestCase {

    private static final Log log = LogFactory.getLog(TestVersioning.class);

    @BeforeClass
    public static void startRuntime() throws Exception {
        runtime = new NXRuntimeTestCase() {};
        runtime.setUp();

        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/CoreService.xml");
        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/SecurityService.xml");
        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/RepositoryService.xml");
        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/LifeCycleService.xml");

        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "TypeService.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "test-CoreExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "CoreTestExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "LifeCycleServiceExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "DemoRepository.xml");

        runtime.deployBundle("org.nuxeo.ecm.core.event");

        //deployCustomVersioning();
    }

    protected void deployCustomVersioning() throws Exception {
        // do nothing - will be overridden by a subclass that tests custom versioning
    }

    // Tests

    @Test
    public void testRemoveSingleDocVersion() throws ClientException {
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        checkVersions(file, new String[0]);

        file.putContextData(REQUEST,
                CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        file = session.saveDocument(file);

        checkVersions(file, new String[]{"1"});

        DocumentModel lastVersion = session.getLastDocumentVersion(file.getRef());
        assertNotNull(lastVersion);

        log.info("removing version with label: " + lastVersion.getVersionLabel());

        assertTrue(lastVersion.isVersion());
        session.removeDocument(lastVersion.getRef());

        checkVersions(file, new String[0]);
    }

    /**
     * Creates 3 versions and removes the first.
     */
    @Test
    public void testRemoveFirstDocVersion() throws ClientException {
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 0;
        DocumentModel firstVersion = session.getVersions(file.getRef()).get(
                VERSION_INDEX);
        assertNotNull(firstVersion);

        log.info("removing version with label: " + firstVersion.getVersionLabel());

        assertTrue(firstVersion.isVersion());
        session.removeDocument(firstVersion.getRef());

        checkVersions(file, new String[] { "2", "3" });
    }

    /**
     * Creates 3 versions and removes the second.
     */
    @Test
    public void testRemoveMiddleDocVersion() throws ClientException {
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 1;
        DocumentModel version = session.getVersions(file.getRef()).get(VERSION_INDEX);
        assertNotNull(version);

        log.info("removing version with label: " + version.getVersionLabel());

        assertTrue(version.isVersion());
        session.removeDocument(version.getRef());

        checkVersions(file, new String[] { "1", "3" });
    }

    /**
     * Creates 3 versions and removes the last.
     */
    @Test
    public void testRemoveLastDocVersion() throws ClientException {
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "folder#1", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = new DocumentModelImpl(folder.getPathAsString(),
                "file#1", "File");
        file = session.createDocument(file);

        createTrioVersions(file);

        final int VERSION_INDEX = 2;
        DocumentModel lastversion = session.getVersions(file.getRef()).get(VERSION_INDEX);
        assertNotNull(lastversion);

        log.info("removing version with label: " + lastversion.getVersionLabel());

        assertTrue(lastversion.isVersion());
        session.removeDocument(lastversion.getRef());

        checkVersions(file, new String[]{"1", "2"});
    }

    private void createTrioVersions(DocumentModel file) throws ClientException {
        // create a first version
        file.setProperty("file", "filename", "A");
        file.putContextData(REQUEST, CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        file = session.saveDocument(file);

        checkVersions(file, new String[] { "1" });

        // create a second version
        // make it dirty so it will be saved
        file.setProperty("file", "filename", "B");
        file.putContextData(REQUEST, CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        file = session.saveDocument(file);

        checkVersions(file, new String[] { "1", "2" });

        // create a third version
        file.setProperty("file", "filename", "C");
        file.putContextData(REQUEST, CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        file = session.saveDocument(file);

        checkVersions(file, new String[] { "1", "2", "3" });
    }

    private void checkVersions(DocumentModel doc, String[] labels) throws ClientException {
        List<DocumentModel> vers = session.getVersions(doc.getRef());
        assertEquals(labels.length, vers.size());
        int i = 0;
        for (DocumentModel ver : vers) {
            assertTrue(ver.isVersion());
            assertEquals(labels[i], ver.getVersionLabel());
            i++;
        }
        List<DocumentRef> versionsRefs = session.getVersionsRefs(doc.getRef());
        assertEquals(labels.length, versionsRefs.size());
    }

}
