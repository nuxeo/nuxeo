/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 9.10
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy("org.nuxeo.ecm.core.schema")
@LocalDeploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-documentmodel-removed-types-contrib.xml")
public class TestDocumentModelComparator {

    @Test
    public void testCompareShouldIgnoreCase() {
        String schemaName = "file";
        String field = "filename";
        String property = schemaName + ":" + field;

        DocumentModel doc1 = new DocumentModelImpl("/", "doc1", "File");
        doc1.setPropertyValue(property, "My doc1");
        DocumentModel doc2 = new DocumentModelImpl("/", "doc2", "File");
        doc1.setPropertyValue(property, "My doc2");

        Map<String, String> orderBy = new HashMap<>();
        DocumentModelComparator comp = new DocumentModelComparator(null, orderBy);
        assertEquals(0, comp.compare(doc1, doc2));

        comp = new DocumentModelComparator(schemaName, orderBy);
        assertEquals(0, comp.compare(doc1, doc2));

        orderBy.put(field, "asc");
        comp = new DocumentModelComparator(schemaName, orderBy);
        assertEquals(1, comp.compare(doc1, doc2));

        orderBy.put(field, "ASC");
        comp = new DocumentModelComparator(schemaName, orderBy);
        assertEquals(1, comp.compare(doc1, doc2));

        orderBy.put(field, "desc");
        comp = new DocumentModelComparator(schemaName, orderBy);
        assertEquals(-1, comp.compare(doc1, doc2));

        orderBy.put(field, "DESC");
        comp = new DocumentModelComparator(schemaName, orderBy);
        assertEquals(-1, comp.compare(doc1, doc2));
    }

}
