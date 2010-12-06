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

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * A test/sample class for remote copying of documents and resources.
 * <p>
 * Warning : this test is deactivated from autorun unit tests as it needs a particular configuration
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class TestIORemoteCopy extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(TestIORemoteCopy.class);

    protected static final String localRepositoryName = "demo";

    // used with 5.1.x
    // String STREAM_SERVER_URL = "socket://localhost:3233";
    // used with 5.2.x
    protected static final String STREAM_SERVER_URL = "socket://localhost:62474";

    protected CoreSession coreSession;

    protected DocumentModel parent;
    protected DocumentModel child;

    protected IOManager ioService;

    @Override
    public void setUp() throws Exception {

        System.setProperty("org.nuxeo.runtime.streaming.isServer", "false");
        System.setProperty("org.nuxeo.runtime.server.port", "62475");
        System.setProperty("org.nuxeo.runtime.server.host", "localhost");
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
                "test-CoreExtensions.xml");

        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "CoreEventListenerService.xml");

        // repo test case misc
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "DefaultPlatform.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "RepositoryManager.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "LifeCycleCoreExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "LifeCycleService.xml");
        // specific files
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "io-test-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "io-test-contrib.xml");

        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "RemotingService.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "JBossLoginConfig.xml");
        // donnot : deployContrib("StreamingServer.xml");

        deployContrib("org.nuxeo.ecm.platform.io.core.tests", "nxrelations.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "nxrelations-default-jena-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "nxrelations-jena-plugin.xml");

        InitialContext ctx1 = new InitialContext();
        System.err.println(ctx1.lookup("java:/comment-relations"));
        NamingEnumeration<NameClassPair> en = ctx1.list("/");
        while (en.hasMore()) {
            Object o = en.nextElement();
            System.err.println(o);
        }

        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "comment-schemas-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "CommentService.xml");
        deployContrib("org.nuxeo.ecm.platform.io.core.tests",
                "commentService-config-bundle.xml");

        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        coreSession = CoreInstance.getInstance().open(localRepositoryName, ctx);

        ioService = Framework.getService(IOManager.class);
    }

    private void createTestDocumentsAndResources(String parentName,
            String childName) throws ClientException {
        // adding a folder and a child doc

        parent = new DocumentModelImpl("/", parentName, "Folder");
        parent = coreSession.createDocument(parent);

        // create structure for comments infrastructure
        // -----
        DocumentModel comments = new DocumentModelImpl(
                parent.getPathAsString(), "Comments", "Folder");
        comments = coreSession.createDocument(comments);

        DateFormat df = new SimpleDateFormat("yyyy-MM");
        comments = new DocumentModelImpl(comments.getPathAsString(),
                df.format(new Date()), "Folder");
        comments = coreSession.createDocument(comments);
        // -----

        child = new DocumentModelImpl(parent.getPathAsString(), childName,
                "File");
        child = coreSession.createDocument(child);
        coreSession.save();

        // create a comment for the file
        //DocumentModel comment = RelationsTestHelper.createSampleComment(coreSession, child);

        // add dummy resources for each of them
        DummyIOResourceAdapter.backend.clear();
        DummyIOResourceAdapter.backend.put(parent.getId(), parent.getName()
                + "DummyValue");
        DummyIOResourceAdapter.backend.put(child.getId(), child.getName()
                + "DummyValue");
        coreSession.save();
    }

    public void testCopyDocumentsAndResources() throws Exception {
        createTestDocumentsAndResources("parent", "child");

        Collection<DocumentRef> sources = new ArrayList<DocumentRef>();
        sources.add(parent.getRef());
        Collection<String> adapters = new ArrayList<String>();
        adapters.add("dummy");

        // check docs before copy
        DocumentModelList children = coreSession.getChildren(coreSession.getRootDocument().getRef());
        printInfo("children", children);
        assertEquals(1, children.size());
        // check copied docs
        children = coreSession.getChildren(parent.getRef());
        assertEquals(1, children.size());

        // check resources before copy
        assertEquals(2, DummyIOResourceAdapter.backend.size());

        // remote copy
        //String serverAddress = "localhost";
        String serverAddress = "192.168.0.153";

        int jndiPort = 1099;
        String destRepoName = "default";

        //String remoteSessionUri = "jboss://" + serverAddress + ":" + jndiPort
        //        + "/nuxeo/DocumentManagerBean/remote";

        // need this to read target root document ref
        CoreSession remoteCoreSession = openToRep("default");

        DocumentModel remoteRootDoc = remoteCoreSession.getRootDocument();
        DocumentRef targetDocRef = remoteRootDoc.getRef();
        // DocumentRef targetDocRef = new PathRef("/");
        targetDocRef = new IdRef("de27d112-9b2e-469e-9a3c-8c3000a8b1f8");

        DocumentLocation targetLocation = new DocumentLocationImpl(
                destRepoName, targetDocRef);
        ioService.copyDocumentsAndResources(localRepositoryName, sources,
                serverAddress, jndiPort, targetLocation, adapters);

        // check copied docs
        DocumentModelList children2 = remoteCoreSession.getChildren(remoteRootDoc.getRef());
        boolean ok = false;
        for (DocumentModel child : children2) {
            if (child.getName().equals("parent")) {
                log.info("parent found");
                DocumentModelList children3 = remoteCoreSession.getChildren(child.getRef());
                log.info("childrens count " + children3.size());
                for (DocumentModel child3 : children3) {
                    if (child3.getName().equals("child")) {
                        log.info("child found");
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

    //private static RelationManager getRelationManager() throws Exception {
    //    return Framework.getService(RelationManager.class);
   // }

}
