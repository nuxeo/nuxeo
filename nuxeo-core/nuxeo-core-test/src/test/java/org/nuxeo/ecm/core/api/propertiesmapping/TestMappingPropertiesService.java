/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.core.api.propertiesmapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.core.test.tests:test-CoreExtensions.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:test-propertiesmapping-contrib.xml")
@Features(CoreFeature.class)
public class TestMappingPropertiesService {

    @Inject
    PropertiesMappingService mappingService;

    @Inject
    CoreSession session;

    @Test
    public void testMapping() {

        assertNotNull(mappingService.getMapping("testMapping"));
        DocumentModel doc1 = createDocument("/", "doc1", "MappingDoc");
        DocumentModel doc2 = createDocument("/", "doc2", "MappingDoc");
        doc1.setPropertyValue("dc:title", "testTitle");
        doc1.setPropertyValue("dc:source", "testSource");
        List<String> contributors = new ArrayList<String>();
        contributors.add("contrib1");
        contributors.add("contrib2");
        doc1.setPropertyValue("dc:contributors", contributors.toArray(new String[1]));
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
        try {
            mappingService.mapProperties(session, doc1, doc2, "testMappingInvalidXpath");
            fail();
        } catch (PropertyNotFoundException e) {
            // ok
        }

        // test mapping wrong types
        try {
            mappingService.mapProperties(session, doc1, doc2, "testMappingWrongTypes");
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Invalid mapping"));
        }
    }

    protected DocumentModel createDocument(String parentPath, String id, String type) {
        DocumentModel doc = session.createDocumentModel(parentPath, id, type);
        doc = session.createDocument(doc);
        return session.saveDocument(doc);
    }

}
