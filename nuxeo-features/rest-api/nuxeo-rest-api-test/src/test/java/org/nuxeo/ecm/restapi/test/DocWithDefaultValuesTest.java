/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

/**
 * Test doc creation with default values
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-defaultvalue-docTypes.xml")
public class DocWithDefaultValuesTest extends BaseTest {

    private static String createDocumentJSON(String properties) {
        String doc = "{";
        doc += "\"entity-type\":\"document\" ,";
        doc += "\"name\":\"doc1\" ,";
        doc += "\"type\":\"DocDefaultValue\"";
        if (properties != null) {
            doc += ", \"properties\": {";
            doc += properties;
            doc += "}";
        }
        doc += "}";
        return doc;
    }

    @Test
    public void testScalarCreatedWithDefaultValue() throws Exception {
        // given a doc saved with a property with a default value not modified
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/", createDocumentJSON(null))) {

            // when I get it
            fetchInvalidations();
            DocumentModel doc = session.getDocument(new PathRef("/doc1"));

            // then the default value must be set
            assertNull(doc.getPropertyValue("dv:simpleWithoutDefault"));
            assertEquals("value", doc.getPropertyValue("dv:simpleWithDefault"));
        }
    }

    @Test
    public void testScalarSetOnNullSetsDefaultValueAgain() throws Exception {
        // given a doc saved with a property with a default value set to null
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/",
                createDocumentJSON("\"dv:simpleWithDefault\": null"))) {

            // when I get it
            fetchInvalidations();
            DocumentModel doc = session.getDocument(new PathRef("/doc1"));

            // then the default value must be set
            doc = session.getDocument(doc.getRef());
            assertEquals("value", doc.getPropertyValue("dv:simpleWithDefault"));
        }
    }

    @Test
    public void testMultiCreatedWithDefaultValue() throws Exception {
        // given a doc saved with a property with a default value not modified
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/", createDocumentJSON(null))) {

            // when I get it
            fetchInvalidations();
            DocumentModel doc = session.getDocument(new PathRef("/doc1"));

            // then the default value must be set
            assertNull(doc.getPropertyValue("dv:multiWithoutDefault"));
            assertArrayEquals(new String[] { "value1", "value2" },
                    (String[]) doc.getPropertyValue("dv:multiWithDefault"));
        }
    }

    @Test
    public void testMultiSetOnNullSetsDefaultValueAgain() throws Exception {
        // given a doc saved with a property with a default value set to null
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path/",
                createDocumentJSON("\"dv:multiWithDefault\": null"))) {

            // when I get it
            fetchInvalidations();
            DocumentModel doc = session.getDocument(new PathRef("/doc1"));

            // then the default value must be set
            doc = session.getDocument(doc.getRef());
            assertArrayEquals(new String[] { "value1", "value2" },
                    (String[]) doc.getPropertyValue("dv:multiWithDefault"));
        }
    }

}
