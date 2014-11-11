/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.api.propertiesmapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Deploy({ "org.nuxeo.ecm.core.api",
        "org.nuxeo.ecm.core.test.tests:test-CoreExtensions.xml",
        "org.nuxeo.ecm.core.test.tests:test-propertiesmapping-contrib.xml" })
@Features(CoreFeature.class)
public class TestMappingPropertiesService {

    @Inject
    PropertiesMappingService mappingService;

    @Inject
    CoreSession session;

    @Test
    public void testMapping() throws ClientException {

        assertNotNull(mappingService.getMapping("testMapping"));
        DocumentModel doc1 = createDocument("/", "doc1", "MappingDoc");
        DocumentModel doc2 = createDocument("/", "doc2", "MappingDoc");
        doc1.setPropertyValue("dc:title", "testTitle");
        doc1.setPropertyValue("dc:source", "testSource");
        List<String> contributors = new ArrayList<String>();
        contributors.add("contrib1");
        contributors.add("contrib2");
        doc1.setPropertyValue("dc:contributors",
                contributors.toArray(new String[1]));
        List<Map<String, Serializable>> comments = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> comment = new HashMap<String, Serializable>();
        comment.put("author", "testAuthor");
        comment.put("text", "testText");
        comment.put("creationDate", new Date());
        comments.add(comment);
        doc1.setPropertyValue("mp:comments", (Serializable) comments);

        doc1 = session.saveDocument(doc1);

        // test a valid mapping
        mappingService.mapProperties(session, doc1, doc2, "testMapping");
        assertEquals("testTitle", doc2.getTitle());
        assertEquals("testSource", doc2.getPropertyValue("dc:source"));

        String[] contrib = (String[]) doc2.getPropertyValue("dc:contributors");
        assertEquals("contrib1", contrib[0]);
        assertEquals("contrib2", contrib[1]);
        List<Map<String, Serializable>> cmnts = (List<Map<String, Serializable>>) doc2.getPropertyValue("mp:comments");
        assertEquals(1, cmnts.size());
        assertEquals("testAuthor", cmnts.get(0).get("author"));

        // test mapping on a invalid xpath
        Exception e = null;
        try {
            mappingService.mapProperties(session, doc1, doc2,
                    "testMappingInvalidXpath");
        } catch (ClientException e1) {
            e = e1;
        }
        assertNotNull(e);

        // test mapping wrong types
        e = null;
        try {
            mappingService.mapProperties(session, doc1, doc2,
                    "testMappingWrongTypes");

        } catch (ClientException e1) {
            e = e1;
        }
        assertNotNull(e);
    }

    protected DocumentModel createDocument(String parentPath, String id,
            String type) throws ClientException {
        DocumentModel doc = session.createDocumentModel(parentPath, id, type);
        doc = session.createDocument(doc);
        return session.saveDocument(doc);
    }

}
