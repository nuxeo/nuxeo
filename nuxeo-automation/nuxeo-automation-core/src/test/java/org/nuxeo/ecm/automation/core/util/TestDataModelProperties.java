/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.core.util;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.Serializable;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

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
        doc.setPropertyValue("dc:subjects", new String[] { "subject1",
                "subject2" });
        doc = session.createDocument(doc);
        session.save();
    }

    @Test
    public void shouldWorkWithDocumentModel() throws ClientException {
        DataModelProperties properties = new DataModelProperties(
                doc.getDataModel("dublincore"));
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
    public void shouldWorkWithSimpleDocumentModel() throws ClientException {
        SimpleDocumentModel documentModel = new SimpleDocumentModel();
        documentModel.setPropertyValue("dc:title", "Doc2");
        documentModel.setPropertyValue("dc:description", "desc2");
        documentModel.setPropertyValue("dc:subjects", new String[] {
                "subject1", "subject2" });

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
