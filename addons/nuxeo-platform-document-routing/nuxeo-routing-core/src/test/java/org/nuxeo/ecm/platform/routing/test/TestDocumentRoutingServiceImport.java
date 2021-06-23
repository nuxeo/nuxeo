/*
 * (C) Copyright 2009-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.routing.core.persistence.RouteModelsZipImporter.WORKFLOW_KEY_VALUE_STORE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.HotDeployer;

@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.routing.core")
@Deploy("org.nuxeo.ecm.platform.routing.core.test")
@RepositoryConfig(init = TestDocumentRoutingServiceImport.ImportRouteRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestDocumentRoutingServiceImport extends DocumentRoutingTestCase {

    protected static final String TMP_PATH_PROP = "nuxeo.routing.test.tmp.path";

    protected File tmp;

    @Inject
    protected HotDeployer hotDeployer;

    protected KeyValueStore workflowModelKV;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        File runtimeHome = Framework.getRuntime().getHome();
        Framework.getResourceLoader().addURL(runtimeHome.toURI().toURL());
        // create a ZIP for the contrib
        tmp = zipResource("/routes/myRoute");
        Path rpath = Paths.get(runtimeHome.getAbsolutePath()).relativize(Paths.get(tmp.getAbsolutePath()));
        Framework.getProperties().put(TMP_PATH_PROP, rpath.toString());

        hotDeployer.deploy("org.nuxeo.ecm.platform.routing.core.test:OSGI-INF/test-document-routing-model-contrib.xml");

        workflowModelKV = Framework.getService(KeyValueService.class).getKeyValueStore(WORKFLOW_KEY_VALUE_STORE);
    }

    protected File zipResource(String resource) throws Exception {
        File runtimeHome = Framework.getRuntime().getHome();
        File file = File.createTempFile("nuxeoRoutingTest", ".zip", runtimeHome);
        try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(file))) {
            URL url = getClass().getResource(resource);
            File dir = new File(url.toURI().getPath());
            zipTree("", dir, false, zout);
            zout.finish();
        }
        return file;
    }

    protected void zipTree(String prefix, File root, boolean includeRoot, ZipOutputStream zout) throws IOException {
        if (includeRoot) {
            prefix += root.getName() + '/';
            zipDirectory(prefix, zout);
        }
        for (String name : root.list()) {
            File file = new File(root, name);
            if (file.isDirectory()) {
                zipTree(prefix, file, true, zout);
            } else {
                if (name.endsWith("~") || name.endsWith("#") || name.endsWith(".bak")) {
                    continue;
                }
                name = prefix + name;
                zipFile(name, file, zout);
            }
        }
    }

    protected void zipDirectory(String entryName, ZipOutputStream zout) throws IOException {
        ZipEntry zentry = new ZipEntry(entryName);
        zout.putNextEntry(zentry);
        zout.closeEntry();
    }

    protected void zipFile(String entryName, File file, ZipOutputStream zout) throws IOException {
        ZipEntry zentry = new ZipEntry(entryName);
        zentry.setTime(file.lastModified());
        zout.putNextEntry(zentry);
        try (FileInputStream in = new FileInputStream(file)) {
            IOUtils.copy(in, zout);
        }
        zout.closeEntry();
    }

    @After
    public void tearDown() {
        if (tmp != null) {
            tmp.delete();
            tmp = null;
        }
        Framework.getProperties().remove(TMP_PATH_PROP);
    }

    public static class ImportRouteRepositoryInit implements RepositoryInit {

        @Override
        public void populate(CoreSession session) {
            // create an initial route to test that is override at import
            DocumentModel root = createDocumentModel(session, "document-route-models-root", "DocumentRouteModelsRoot",
                                                     "/");
            assertNotNull(root);
            DocumentModel route = createDocumentModel(session, "myRoute", "DocumentRoute",
                                                      "/document-route-models-root/");
            route.setPropertyValue("dc:coverage", "test");
            route = session.saveDocument(route);
            // set ACL to test that the ACLs are kept
            ACP acp = route.getACP();
            ACL acl = acp.getOrCreateACL("testrouting");
            acl.add(new ACE("testusername", "Write", true));
            acp.addACL(acl);
            route.setACP(acp, true);
            route = session.saveDocument(route);

            assertNotNull(route);
            assertEquals("test", route.getPropertyValue("dc:coverage"));

            DocumentModel node = createDocumentModel(session, "myNode", "RouteNode",
                                                     "/document-route-models-root/myRoute");
            assertNotNull(node);
        }

        protected DocumentModel createDocumentModel(CoreSession session, String name, String type, String path) {
            DocumentModel doc = session.createDocumentModel(path, name, type);
            doc.setPropertyValue(DocumentRoutingConstants.TITLE_PROPERTY_NAME, name);
            return session.createDocument(doc);
        }

    }

    @Test
    public void testImportRouteModel() {
        // re-import routes created by test repository init after initial import
        service.importAllRouteModels(session);

        DocumentModel modelsRoot = session.getDocument(new PathRef("/document-route-models-root/"));
        assertNotNull(modelsRoot);
        DocumentModel route = session.getDocument(new PathRef("/document-route-models-root/myRoute"));
        assertNotNull(route);

        String routeDocId = service.getRouteModelDocIdWithId(session, "myRoute");
        DocumentModel doc = session.getDocument(new IdRef(routeDocId));
        DocumentRoute model = doc.getAdapter(DocumentRoute.class);

        assertEquals(route.getId(), model.getDocument().getId());
        // test that document was overriden but the ACLs were kept
        ACL newAcl = route.getACP().getACL("testrouting");
        assertNotNull(newAcl);
        assertEquals(1, newAcl.getACEs().length);
        assertEquals("testusername", newAcl.getACEs()[0].getUsername());

        // Oracle makes no difference between null and blank
        assertTrue(StringUtils.isBlank((String) route.getPropertyValue("dc:coverage")));
        DocumentModel node;
        try {
            node = session.getDocument(new PathRef("/document-route-models-root/myRoute/myNode"));
        } catch (DocumentNotFoundException e) {
            node = null;
        }
        assertNull(node);
        assertEquals("DocumentRoute", route.getType());
        DocumentModel step1 = session.getDocument(new PathRef("/document-route-models-root/myRoute/Step1"));
        assertNotNull(step1);
        assertEquals("RouteNode", step1.getType());
        DocumentModel step2 = session.getDocument(new PathRef("/document-route-models-root/myRoute/Step2"));
        assertNotNull(step2);
        assertEquals("RouteNode", step2.getType());
    }

    // NXP-30170
    @Test
    public void testImportRouteModelUseKeyValueStore() throws Exception {
        // DocumentRoute has been imported by RouteModelsInitializator - check KV
        String zipDigest = getMD5Digest(tmp);
        String kvDigest = workflowModelKV.getString("digest-myRoute");
        assertEquals(zipDigest, kvDigest);

        DocumentModel myRoute = session.getDocument(new PathRef("/document-route-models-root/myRoute"));
        String myRouteId = myRoute.getId();

        // re-importing the same route doesn't overwrite the current document as digest hasn't changed
        service.importAllRouteModels(session);

        myRoute = session.getDocument(new PathRef("/document-route-models-root/myRoute"));
        assertEquals(myRouteId, myRoute.getId());
        assertEquals("myRoute", myRoute.getPropertyValue("dc:title"));

        // removing the digest and importing again will overwrite the current document
        workflowModelKV.put("digest-myRoute", (String) null);
        service.importAllRouteModels(session);

        myRoute = session.getDocument(new PathRef("/document-route-models-root/myRoute"));
        assertNotEquals(myRouteId, myRoute.getId());
        assertEquals("myRoute", myRoute.getPropertyValue("dc:title"));
        myRouteId = myRoute.getId();
        // check that KV has been filled again
        kvDigest = workflowModelKV.getString("digest-myRoute");
        assertEquals(zipDigest, kvDigest);

        // importing a workflow with a different digest will overwrite the current document
        // the key is _path_ in exported document, it is the same value for myRoute and myRouteBis
        File myRouteBisFile = zipResource("/routes/myRouteBis");
        Path rpath = Paths.get(Framework.getRuntime().getHome().getAbsolutePath())
                          .relativize(Paths.get(myRouteBisFile.getAbsolutePath()));
        Framework.getProperties().put(TMP_PATH_PROP, rpath.toString());

        hotDeployer.undeploy(
                "org.nuxeo.ecm.platform.routing.core.test:OSGI-INF/test-document-routing-model-contrib.xml");
        hotDeployer.deploy("org.nuxeo.ecm.platform.routing.core.test:OSGI-INF/test-document-routing-model-contrib.xml");
        workflowModelKV = Framework.getService(KeyValueService.class).getKeyValueStore(WORKFLOW_KEY_VALUE_STORE);

        myRoute = session.getDocument(new PathRef("/document-route-models-root/myRoute"));
        assertNotEquals(myRouteId, myRoute.getId());
        assertEquals("myRouteBis", myRoute.getPropertyValue("dc:title"));
        // check that KV has changed
        zipDigest = getMD5Digest(myRouteBisFile);
        kvDigest = workflowModelKV.getString("digest-myRoute");
        assertEquals(zipDigest, kvDigest);
    }

    protected String getMD5Digest(File file) {
        try (InputStream in = new FileInputStream(file)) {
            return DigestUtils.md5Hex(in);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

}
