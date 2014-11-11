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
 * $Id$
 */

package org.nuxeo.ecm.platform.io.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.io.exceptions.ExportDocumentException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * A test/sample class for remote copying of documents and resources
 * from one remote server to another.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class TestIORemoteCopy2 extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(TestIORemoteCopy2.class);

    protected static final String repositoryName1 = "default";
    protected static final String repositoryName2 = "default";

    // used with 5.1.x
    // String STREAM_SERVER_URL = "socket://localhost:3233";
    // used with 5.2.x
    protected static final String STREAM_SERVER_URL = "socket://localhost:62474";

    protected CoreSession coreSession1;
    protected CoreSession coreSession2;

    protected DocumentModel testRoot;
    protected DocumentModel parent;
    protected DocumentModel child;
    protected DocumentModel child2;

    protected IOManager ioService;
    protected IOManager remoteIOManager;

    protected DocumentRef srcDocId;
    protected DocumentRef dstDocId;
    protected String destServerAddress;

    @Override
    public void setUp() throws Exception {

        System.setProperty("org.nuxeo.runtime.streaming.isServer", "false");
        //System.setProperty("org.nuxeo.runtime.server.port", "62475");
        //System.setProperty("org.nuxeo.runtime.server.host", "localhost");
        System.setProperty("org.nuxeo.runtime.streaming.serverLocator",
                STREAM_SERVER_URL);

        super.setUp();

        // the core bundle
        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreService.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests", "TypeService.xml");
        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/SecurityService.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "RepositoryService.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "RepositoryManager.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "test-CoreExtensions.xml");

        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "CoreEventListenerService.xml");

        // repo test case misc
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "DefaultPlatform2WayCopy.xml");

        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "RemotingService.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "JBossLoginConfig.xml");
        // donnot : deployContrib("StreamingServer.xml");

        acquireCoreSessions();
    }

    private void acquireCoreSessions() throws Exception {
        coreSession1 = openToRep("rep1");
        assertNotNull(coreSession1);
        coreSession2 = openToRep("rep2");
        assertNotNull(coreSession2);

        // get ioService from the first server
        ioService = Framework.getService(IOManager.class);
        assertNotNull(ioService);

        remoteIOManager = Framework.getService(IOManager.class, "remote");
        assertNotNull(remoteIOManager);
    }

    private void createTestDocumentsAndResources() throws Exception {

        String parentName = "parent";
        String childName = "child";
        CoreSession coreSession = coreSession1;

        // adding a folder and a child doc

        testRoot = new DocumentModelImpl("/", "test" + new Date().getTime(),
                "Folder");
        testRoot = coreSession.createDocument(testRoot);
        testRoot.setProperty("dublincore", "title", "TestRoot");
        testRoot = coreSession.saveDocument(testRoot);

        parent = new DocumentModelImpl(testRoot.getPathAsString(), parentName,
                "Folder");
        parent = coreSession.createDocument(parent);
        parent.setProperty("dublincore", "title", "Parent");
        parent = coreSession.saveDocument(parent);

        child = new DocumentModelImpl(parent.getPathAsString(), childName,
                "File");
        child = coreSession.createDocument(child);
        child.setProperty("dublincore", "title", "Child");
        child = coreSession.saveDocument(child);
        child.setProperty("dublincore", "description", "some description");
        child = coreSession.saveDocument(child);

        child2 = new DocumentModelImpl(parent.getPathAsString(), childName,
                "File");
        child2 = coreSession.createDocument(child2);
        child2.setProperty("dublincore", "title", "Child2");
        child2 = coreSession.saveDocument(child2);

        // create a relation between child & child2

        coreSession.save();

        // create a comment for the file
        //DocumentModel comment = RelationsTestHelper.createSampleComment(coreSession, child);
        DummyRelationsHelper.createRelation(child, child2);

        // add dummy resources for each of them
        DummyIOResourceAdapter.backend.clear();
        DummyIOResourceAdapter.backend.put(parent.getId(), parent.getName()
                + "DummyValue");
        DummyIOResourceAdapter.backend.put(child.getId(), child.getName()
                + "DummyValue");
        coreSession.save();
    }

    // this is using documents created by this test
    void prepareLocations1() {
        srcDocId = parent.getRef();
        dstDocId = new PathRef("/");

        // alternatively
        // DocumentModel remoteRootDoc = coreSession2.getRootDocument();
        // dstDocId = remoteRootDoc.getRef();

        // this will be transmitted to the source server thus it will know
        // where to connect to
        destServerAddress = "192.168.0.197";
    }

    // this is using existing documents in the repositories
    void prepareLocations2() throws ExportDocumentException {
        srcDocId = new IdRef("6a840040-ee01-4c67-b734-4df7696e5b46");
        dstDocId = new IdRef("7b027a65-808d-4958-b583-eff15a5b2071");
        // this will be transmitted to the source server thus it will know
        // where to connect to
        destServerAddress = "192.168.0.197";

        // check docs exist
        //ByteArrayOutputStream out = new ByteArrayOutputStream();
        //Collection<DocumentRef> sources = new ArrayList<DocumentRef>();
        //sources.add(dstDocId);
        //remoteIOManager.exportDocumentsAndResources(out, "default", sources, true, null, null);
    }

    public void testCopyDocumentsAndResources() throws Exception {
        prepareLocations2();

        createTestDocumentsAndResources();

        Collection<DocumentRef> sources = new ArrayList<DocumentRef>();
        sources.add(srcDocId);

        // test we have required adapters
        //IOResourceAdapter adapter = ioService.getAdapter("template_relations");
        //assertNotNull(adapter);
        //adapter = ioService.getAdapter("audit_logs");
        //assertNotNull(adapter);

        Collection<String> adapters = new ArrayList<String>();
        adapters.add("template_relations");
        adapters.add("audit_logs");
        //adapters.add("dummy");

        // check docs before copy
        DocumentModelList children = coreSession1.getChildren(testRoot.getRef());
        printInfo("children", children);
        //assertEquals(1, children.size());
        // check copied docs
        children = coreSession1.getChildren(parent.getRef());
        assertEquals(2, children.size());

        // check resources before copy
        //assertEquals(2, DummyIOResourceAdapter.backend.size());

        // remote copy
        //String serverAddress = "localhost";
        String serverAddress = destServerAddress;

        int jndiPort = 1099;
        String sourceRepoName = "default";
        String destRepoName = "default";

        //String remoteSessionUri = "jboss://" + serverAddress + ":" + jndiPort
        //        + "/nuxeo/DocumentManagerBean/remote";

        DocumentRef targetDocRef = dstDocId;

        DocumentLocation targetLocation = new DocumentLocationImpl(
                destRepoName, targetDocRef);
        //ioService.copyDocumentsAndResources(sourceRepoName, sources,
        //        serverAddress, jndiPort, targetLocation, adapters);
        ioService.copyDocumentsAndResources(sourceRepoName, sources,
                remoteIOManager, targetLocation, adapters);

        // check copied docs
        DocumentModelList children2 = coreSession2.getChildren(targetDocRef);
        boolean ok = false;
        for (DocumentModel child : children2) {
            if (child.getName().equals("parent")) {
                log.info("parent found");
                DocumentModelList children3 = coreSession2.getChildren(child.getRef());
                log.info("childrens count " + children3.size());
                for (DocumentModel child3 : children3) {
                    if (child3.getName().equals("child")) {
                        log.info("child found");
                        // check detailed info for child
                        Logs auditLogs = Framework.getService(Logs.class);
                        List<LogEntry> entries = auditLogs.getLogEntriesFor(child3.getId());
                        assertEquals(5, entries.size());

                        ok = true;
                        break;
                    }
                }
                break;
            }
        }
        assertTrue(ok);

        // check copied resources
        assertEquals(2, DummyIOResourceAdapter.backend.size());
    }

    public void testExportImport() throws Exception {

        log.info("--------------------------------------------- begin");

        prepareLocations2();

        createTestDocumentsAndResources();

        Collection<DocumentRef> sources = new ArrayList<DocumentRef>();
        sources.add(srcDocId);

        Collection<String> adapters = new ArrayList<String>();
        adapters.add("template_relations");
        adapters.add("audit_logs");

        // check docs before copy
        DocumentModelList children = coreSession1.getChildren(testRoot.getRef());
        printInfo("children", children);
        // assertEquals(1, children.size());
        // check copied docs
        children = coreSession1.getChildren(parent.getRef());
        assertEquals(2, children.size());

        // check resources before copy
        // assertEquals(2, DummyIOResourceAdapter.backend.size());

        // remote copy
        String serverAddress = destServerAddress;

        String sourceRepoName = "default";
        String destRepoName = "default";

        DocumentRef targetDocRef = dstDocId;

        DocumentLocation targetLocation = new DocumentLocationImpl(
                destRepoName, targetDocRef);

        String exportUri = ioService.externalizeExport(sourceRepoName, sources,
                adapters);

        log.info("export downloadable from: " + exportUri);

        // connect to the second server and specify to import from uri

        String newExportUri = exportUri.replaceAll("127.0.0.1", "192.168.0.103");

        log.info("import exported file from: " + newExportUri);

        remoteIOManager.importExportedFile(newExportUri, targetLocation);

        // upload done, remove source
        ioService.disposeExport(exportUri);

        log.info("--------------------------------------------- end");
    }

    private static void printInfo(String title, Object obj) {
        log.info("-------------------");
        log.info(title);
        log.info(obj);
        log.info("-------------------");
    }

    private static CoreSession openToRep(String repName) throws Exception {
        RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
        Repository repository = repositoryMgr.getRepository(repName);

        Framework.login();

        return repository.open();
    }

}
