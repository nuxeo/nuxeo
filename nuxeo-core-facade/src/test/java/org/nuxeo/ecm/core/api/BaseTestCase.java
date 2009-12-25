package org.nuxeo.ecm.core.api;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public abstract class BaseTestCase extends Assert {

    protected static NXRuntimeTestCase runtime;

    protected static boolean usingCustomVersioning = false;

    protected final Random random = new Random(new Date().getTime());

    protected CoreSession session;

    protected DocumentModel root;

    @AfterClass
    public static void stopRuntime() throws Exception {
        runtime.tearDown();
    }

    @Before
    public void setUp() throws Exception {
        openSession();
        root = getRootDocument();
    }

    @After
    public void tearDown() throws Exception {
        cleanUp(getRootDocument().getRef());
        closeSession();
    }

    public void openSession() throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        session = CoreInstance.getInstance().open("default", ctx);
        assertNotNull(session);
    }

    public void closeSession() {
        CoreInstance.getInstance().close(session);
    }

    // Convenience methods

    protected String generateUnique() {
        return String.valueOf(random.nextLong());
    }

    protected DocumentModel getRootDocument() throws ClientException {
        DocumentModel root = session.getRootDocument();

        assertNotNull(root);
        assertNotNull(root.getId());
        assertNotNull(root.getRef());
        assertNotNull(root.getPathAsString());

        return root;
    }

    protected DocumentModel createChildDocument(DocumentModel childFolder)
            throws ClientException {

        DocumentModel ret = session.createDocument(childFolder);

        assertNotNull(ret);
        assertNotNull(ret.getName());
        assertNotNull(ret.getId());
        assertNotNull(ret.getRef());
        assertNotNull(ret.getPathAsString());

        return ret;
    }

    protected List<DocumentModel> createChildDocuments(
            List<DocumentModel> childFolders) throws ClientException {
        List<DocumentModel> rets = new ArrayList<DocumentModel>();
        Collections.addAll(
                rets,
                session.createDocument(childFolders.toArray(new DocumentModel[childFolders.size()])));

        assertNotNull(rets);
        assertEquals(childFolders.size(), rets.size());

        for (DocumentModel createdChild : rets) {
            assertNotNull(createdChild);
            assertNotNull(createdChild.getName());
            assertNotNull(createdChild.getRef());
            assertNotNull(createdChild.getPathAsString());
            assertNotNull(createdChild.getId());
        }

        return rets;
    }

    protected void cleanUp(DocumentRef ref) throws ClientException {
        session.removeChildren(ref);
        session.save();
    }

}
