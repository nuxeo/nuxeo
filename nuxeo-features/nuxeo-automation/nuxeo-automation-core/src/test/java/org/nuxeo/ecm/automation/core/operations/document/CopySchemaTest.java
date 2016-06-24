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
 *
 */
package org.nuxeo.ecm.automation.core.operations.document;

import com.google.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import java.util.Map;

import static org.junit.Assert.*;

@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class})
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:OSGI-INF/copy-schema-test-contrib.xml")
public class CopySchemaTest {

    @Inject
    private CoreSession session;

    @Inject
    private AutomationService service;

    private DocumentModel documentSource;
    private DocumentModel documentTarget;

    @Before
    public void setUp() {
        documentSource = session.createDocumentModel("/", "Source", "File");
        documentSource = session.createDocument(documentSource);
        session.save();
        assertNotNull(documentSource);
        documentTarget = session.createDocumentModel("/", "Target", "File");
        documentSource = session.createDocument(documentTarget);
        session.save();
        assertNotNull(documentTarget);
    }

    @After
    public void tearDown() {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();
    }

    @Test
    public void testCopySingleProperty() throws OperationException {
        String schema = "dublincore";
        String property = "title";

        assertTrue(documentSource.hasSchema(schema));
        documentSource.setProperty(schema, property, "Source");

        assertTrue(documentTarget.hasSchema(schema));
        documentTarget.setProperty(schema, property, "Target");

        assertNotEquals(documentSource.getProperty(schema, property), documentTarget.getProperty(schema, property));

        OperationContext context = new OperationContext(session);
        context.setInput(documentTarget);
        OperationChain chain = new OperationChain("testCopySingleProperty");
        chain.add(CopySchema.ID).set("source", documentSource).set("schema", schema);
        documentTarget = (DocumentModel)service.run(context, chain);

        assertEquals(documentSource.getProperty(schema, property), documentTarget.getProperty(schema, property));
    }

    @Test
    public void testCopyFullSchema() throws OperationException {
        String schema = "common";
        assertTrue(documentSource.hasSchema(schema));
        assertTrue(documentTarget.hasSchema(schema));

        documentSource.setProperty(schema, "size", 1);
        documentSource.setProperty(schema, "icon-expanded", "icon-expanded1");
        documentSource.setProperty(schema, "icon", "icon1");

        documentTarget.setProperty(schema, "size", 2);
        documentTarget.setProperty(schema, "icon-expanded", "icon-expanded2");
        documentTarget.setProperty(schema, "icon", "icon2");

        OperationContext context = new OperationContext(session);
        context.setInput(documentTarget);
        OperationChain chain = new OperationChain("testCopyFullSchema");
        chain.add(CopySchema.ID).set("source", documentSource).set("schema", schema);
        documentTarget = (DocumentModel)service.run(context, chain);

        Map<String, Object> propertyMap = documentSource.getProperties(schema);
        for (Map.Entry<String, Object> pair : propertyMap.entrySet()) {
            // ensure that the values of the properties for the two documents are now the same
            assertEquals(documentSource.getProperty(schema, pair.getKey()), documentTarget.getProperty(schema, pair.getKey()));
        }
    }

}
