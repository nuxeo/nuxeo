/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.core.util;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestDataModelProperties {

    @Inject
    CoreSession session;

    private DocumentModel doc;

    @Before
    public void setup() throws Exception {
        doc = session.createDocumentModel("/", "my-doc", "File");
        doc.setPropertyValue("dc:title", "Doc1");
        doc.setPropertyValue("dc:description", "desc1");
        doc.setPropertyValue("dc:source", "source1");
        doc.setPropertyValue("dc:subjects", new String[] { "subject1", "subject2" });
        doc = session.createDocument(doc);
        session.save();
    }

    @Test
    public void shouldWorkWithDocumentModel() {
        DataModelProperties properties = new DataModelProperties(doc.getDataModel("dublincore"));
        Map<String, Serializable> map = properties.getMap();
        assertFalse(map.isEmpty());

        // the underlying map should contains String => Serializable
        assertEquals("Doc1", map.get("dc:title"));
        assertEquals("desc1", map.get("dc:description"));
        assertEquals("source1", map.get("dc:source"));
        String[] subjects = (String[]) map.get("dc:subjects");
        assertEquals(2, subjects.length);
        assertEquals("subject1", subjects[0]);
        assertEquals("subject2", subjects[1]);
    }

    @Test
    public void shouldWorkWithSimpleDocumentModel() {
        SimpleDocumentModel documentModel = SimpleDocumentModel.empty();
        documentModel.setPropertyValue("dc:title", "Doc2");
        documentModel.setPropertyValue("dc:description", "desc2");
        documentModel.setPropertyValue("dc:subjects", new String[] { "subject1", "subject2" });

        DataModelProperties properties = new DataModelProperties();
        for (String schema : documentModel.getSchemas()) {
            properties.addDataModel(documentModel.getDataModel(schema));
        }
        Map<String, Serializable> map = properties.getMap();
        assertFalse(map.isEmpty());

        // the underlying map should contains String => Serializable
        assertEquals("Doc2", map.get("dc:title"));
        assertEquals("desc2", map.get("dc:description"));
        String[] subjects = (String[]) map.get("dc:subjects");
        assertEquals(2, subjects.length);
        assertEquals("subject1", subjects[0]);
        assertEquals("subject2", subjects[1]);
    }

}
