/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.api;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
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

    protected CoreSession rootSession;
    protected CoreSession session;

    protected DocumentModel root;

    @AfterClass
    public static void stopRuntime() throws Exception {
        runtime.tearDown();
    }

    @Before
    public void setUp() throws Exception {
        session = getRootSession();
        root = getRootDocument();
    }

    @After
    public void tearDown() throws Exception {
        cleanUp(getRootDocument().getRef());
        closeSession();
    }

    public CoreSession getRootSession() throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        CoreSession session = CoreInstance.getInstance().open("default", ctx);
        assertNotNull(session);
        return session;
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
        CoreSession session = getRootSession();
        session.removeChildren(ref);
        session.save();
    }

}
