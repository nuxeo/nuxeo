/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core.api.filter;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml",
        "org.nuxeo.ecm.core.test.tests:test-char-filtering-contrib.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestCharacterFiltering {

    @Inject
    protected CoreSession session;

    @Test
    public void testSimplePropertyFiltering() {

        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:title", "My\u000cTitle");
        doc.setPropertyValue("dc:description", "My\u200b\tDescription");
        doc.setPropertyValue("dc:creator", "John Doe");

        doc = session.createDocument(doc);
        assertEquals("MyTitle", doc.getProperty("dc:title").getValue());
        assertEquals("MyDescription", doc.getProperty("dc:description").getValue());
        assertEquals("John Doe", doc.getProperty("dc:creator").getValue());

        doc.setPropertyValue("dc:source", "The\0Source");
        doc = session.saveDocument(doc);
        assertEquals("TheSource", doc.getProperty("dc:source").getValue());

    }

    @Test
    public void testComplexPropertyFiltering() {
        DocumentModel doc = session.createDocumentModel("/", "complexDoc", "ComplexDoc");

        Map<String, Object> attachedFile = new HashMap<>();
        List<Map<String, Object>> vignettes = new ArrayList<>();
        Map<String, Object> vignette = new HashMap<>();
        vignette.put("label", "My\fLabel");
        vignettes.add(vignette);
        attachedFile.put("name", "AttachedFile");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);

        doc = session.createDocument(doc);
        assertEquals("MyLabel", doc.getProperty("cmpf:attachedFile/vignettes/vignette[0]/label").getValue());
    }

    @Test
    public void testArrayPropertyFiltering() {

        DocumentModel doc = session.createDocumentModel("/", "arrayDoc", "TestDocument");
        doc.setPropertyValue("tp:stringArray", new String[] { "My\u200bElement1", "My\u200bElement2" });
        doc = session.createDocument(doc);
        assertEquals("MyElement1", ((String[]) doc.getProperty("tp:stringArray").getValue())[0]);
        assertEquals("MyElement2", ((String[]) doc.getProperty("tp:stringArray").getValue())[1]);
    }

}
