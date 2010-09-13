/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that hit directly the server APIs.
 */
public class TestNuxeoBinding extends NuxeoBindingTestCase {

    protected ObjectService objService;

    protected NavigationService navService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Helper.makeNuxeoRepository(nuxeotc.getSession());
        objService = binding.getObjectService();
        navService = binding.getNavigationService();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected String createDocument(String name, String folderId, String typeId) {
        ContentStream contentStream = null;
        VersioningState versioningState = VersioningState.NONE;
        List<String> policies = null;
        Acl addACEs = null;
        Acl removeACEs = null;
        return objService.createDocument(repositoryId,
                createDocumentProperties(name, typeId), folderId,
                contentStream, versioningState, policies, addACEs, removeACEs,
                null);
    }

    protected Properties createDocumentProperties(String name, String typeId) {
        BindingsObjectFactory factory = binding.getObjectFactory();
        List<PropertyData<?>> props = new ArrayList<PropertyData<?>>();
        props.add(factory.createPropertyIdData(PropertyIds.NAME, name));
        props.add(factory.createPropertyIdData(PropertyIds.OBJECT_TYPE_ID,
                typeId));
        return factory.createPropertiesData(props);
    }

    protected ObjectData getDocument(String id) {
        return objService.getObject(repositoryId, id, "*", Boolean.FALSE,
                IncludeRelationships.NONE, null, Boolean.FALSE, Boolean.FALSE,
                null);
    }

    @Test
    public void testCreateDocument() {
        String id = createDocument("doc1", rootFolderId, "File");
        assertNotNull(id);
        ObjectData data = getDocument(id);
        assertEquals(id, data.getId());
        Map<String, PropertyData<?>> properties = data.getProperties().getProperties();
        assertEquals("doc1", properties.get("cmis:name").getFirstValue());
    }

    // flatten and order children
    protected static List<String> flatTree(List<ObjectInFolderContainer> tree)
            throws Exception {
        if (tree == null) {
            return null;
        }
        List<String> r = new LinkedList<String>();
        for (Iterator<ObjectInFolderContainer> it = tree.iterator(); it.hasNext();) {
            ObjectInFolderContainer child = it.next();
            String name = (String) child.getObject().getObject().getProperties().getProperties().get(
                    PropertyIds.NAME).getFirstValue();
            String elem = name;
            List<String> sub = flatTree(child.getChildren());
            if (sub != null) {
                elem += "[" + StringUtils.join(sub, ", ") + "]";
            }
            r.add(elem);
        }
        Collections.sort(r);
        return r;
    }

    protected static String flat(List<ObjectInFolderContainer> tree)
            throws Exception {
        return StringUtils.join(flatTree(tree), ", ");
    }

    @Test
    public void testDescendants() throws Exception {
        List<ObjectInFolderContainer> tree;

        try {
            navService.getDescendants(repositoryId, rootFolderId,
                    BigInteger.valueOf(0), null, null, null, null, null, null);
            fail("Depth 0 should be forbidden");
        } catch (CmisInvalidArgumentException e) {
            // ok
        }

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(1), null, null, null, null, null, null);
        assertEquals("testfolder1_Title, testfolder2", flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(2), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[testfile1_Title, "
                + /* */"testfile2_Title, " //
                + /* */"testfile3_Title], " //
                + "testfolder2[testfolder3]", flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(3), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[testfile1_Title[], "
                + /* */"testfile2_Title[], " //
                + /* */"testfile3_Title[]], " //
                + "testfolder2[testfolder3[testfile4_Title]]", flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(4), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[testfile1_Title[], "
                + /* */"testfile2_Title[], " //
                + /* */"testfile3_Title[]], " //
                + "testfolder2[testfolder3[testfile4_Title[]]]", flat(tree));

        tree = navService.getDescendants(repositoryId, rootFolderId,
                BigInteger.valueOf(-1), null, null, null, null, null, null);
        assertEquals("testfolder1_Title[testfile1_Title[], "
                + /* */"testfile2_Title[], " //
                + /* */"testfile3_Title[]], " //
                + "testfolder2[testfolder3[testfile4_Title[]]]", flat(tree));

        ObjectData ob = objService.getObjectByPath(repositoryId,
                "/testfolder2", null, null, null, null, null, null, null);
        String folder2Id = ob.getId();

        tree = navService.getDescendants(repositoryId, folder2Id,
                BigInteger.valueOf(1), null, null, null, null, null, null);
        assertEquals("testfolder3", flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id,
                BigInteger.valueOf(2), null, null, null, null, null, null);
        assertEquals("testfolder3[testfile4_Title]", flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id,
                BigInteger.valueOf(3), null, null, null, null, null, null);
        assertEquals("testfolder3[testfile4_Title[]]", flat(tree));

        tree = navService.getDescendants(repositoryId, folder2Id,
                BigInteger.valueOf(-1), null, null, null, null, null, null);
        assertEquals("testfolder3[testfile4_Title[]]", flat(tree));
    }

    @Test
    public void testCopy() throws Exception {
        ObjectData ob = objService.getObjectByPath(repositoryId,
                "/testfolder1/testfile1", null, null, null, null, null, null,
                null);
        PropertiesImpl props = new PropertiesImpl();
        props.addProperty(new PropertyStringImpl("dc:title", "new title"));
        try {
            String id = objService.createDocumentFromSource(repositoryId,
                    ob.getId(), props, rootFolderId, null, null, null, null,
                    null);
            assertNotNull(id);
            assertNotSame(id, ob.getId());
        } catch (CmisRuntimeException e) {
            throw e;
            // assertTrue(e.getMessage().contains(
            // "AtomPub bindings do not support"));
            // return;
        }
        // fetch
        ObjectData copy = objService.getObjectByPath(repositoryId,
                "/testfile1", null, null, null, null, null, null, null);
        assertNotNull(copy);
        assertEquals(
                "new title",
                copy.getProperties().getProperties().get("dc:title").getFirstValue());
    }

}
