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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that hit directly the server APIs.
 */
public class TestNuxeoBinding extends NuxeoBindingTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
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
        return binding.getObjectService().createDocument(repositoryId,
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
        return binding.getObjectService().getObject(repositoryId, id, "*",
                Boolean.FALSE, IncludeRelationships.NONE, null, Boolean.FALSE,
                Boolean.FALSE, null);
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

}
