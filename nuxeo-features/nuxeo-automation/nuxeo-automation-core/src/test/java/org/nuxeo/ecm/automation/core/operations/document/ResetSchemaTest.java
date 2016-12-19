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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.inject.Inject;

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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class})
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:OSGI-INF/reset-schema-test-contrib.xml")
public class ResetSchemaTest {

    @Inject
    private CoreSession session;

    @Inject
    private AutomationService service;

    private DocumentModel target1;
    private DocumentModel target2;
    private DocumentModelList targets;

    @Before
    public void setUp() {
        target1 = session.createDocumentModel("/", "Target1", "File");
        target1 = session.createDocument(target1);
        session.save();

        target2 = session.createDocumentModel("/", "Target2", "File");
        target2 = session.createDocument(target2);
        session.save();

        targets = new DocumentModelListImpl();
        targets.add(target1);
        targets.add(target2);
    }

    @After
    public void tearDown() {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();
    }

    @Test
    public void testThrowException() {
        String schema = "dublincore";
        String property = "title";
        boolean triggeredException = false;

        assertTrue(target1.hasSchema(schema));
        target1.setProperty(schema, property, target1.getName());

        OperationContext context = new OperationContext(session);
        context.setInput(target1);
        OperationChain chain = new OperationChain("testThrowException");
        chain.add(ResetSchema.ID);
        try {
            target1 = (DocumentModel)service.run(context, chain);
        } catch (OperationException e) {
            triggeredException = true;
        }
        assertTrue(triggeredException);
    }

    @Test
    public void testSingleTargetSingleProperty() throws OperationException {
        String schema = "dublincore";
        String property = "title";

        assertTrue(target1.hasSchema(schema));
        target1.setProperty(schema, property, target1.getName());

        OperationContext context = new OperationContext(session);
        context.setInput(target1);
        OperationChain chain = new OperationChain("testSingleTargetSingleProperty");
        chain.add(ResetSchema.ID).set("xpath", schema + ":" + property);
        target1 = (DocumentModel)service.run(context, chain);

        assertNull(target1.getProperty(schema, property));
    }

    @Test
    public void testSingleTargetFullSchema() throws OperationException {
        String schema = "common";

        assertTrue(target1.hasSchema(schema));
        target1.setProperty(schema, "icon-expanded", "icon-expanded1");
        target1.setProperty(schema, "icon", "icon1");

        OperationContext context = new OperationContext(session);
        context.setInput(target1);
        OperationChain chain = new OperationChain("testSingleTargetFullSchema");
        chain.add(ResetSchema.ID).set("schema", schema);
        target1 = (DocumentModel)service.run(context, chain);

        for (Map.Entry<String, Object> entry : target1.getProperties(schema).entrySet()) {
            // ensure that the values of the properties are now null
            assertNull(target1.getProperty(schema, entry.getKey()));
        }
    }

    @Test
    public void testMultipleTargetsSingleProperty() throws OperationException {
        String schema = "dublincore";
        String property = "title";

        assertTrue(target1.hasSchema(schema));
        target1.setProperty(schema, property, target1.getName());

        assertTrue(target2.hasSchema(schema));
        target2.setProperty(schema, property, target2.getName());

        OperationContext context = new OperationContext(session);
        context.setInput(targets);
        OperationChain chain = new OperationChain("testMultipleTargetsSingleProperty");
        chain.add(ResetSchema.ID).set("xpath", schema + ":" + property);
        targets = (DocumentModelList)service.run(context, chain);

        assertNull(target1.getProperty(schema, property));
        assertNull(target2.getProperty(schema, property));
    }

    @Test
    public void testMultipleTargetsFullSchema() throws OperationException {
        String schema = "common";

        assertTrue(target1.hasSchema(schema));
        target1.setProperty(schema, "icon-expanded", "icon-expanded1");
        target1.setProperty(schema, "icon", "icon1");

        assertTrue(target2.hasSchema(schema));
        target2.setProperty(schema, "icon-expanded", "icon-expanded2");
        target2.setProperty(schema, "icon", "icon2");

        OperationContext context = new OperationContext(session);
        context.setInput(targets);
        OperationChain chain = new OperationChain("testMultipleTargetsFullSchema");
        chain.add(ResetSchema.ID).set("schema", schema);
        targets = (DocumentModelList)service.run(context, chain);

        for (Map.Entry<String, Object> entry : target1.getProperties(schema).entrySet()) {
            // ensure that the values of the properties of both documents are now null
            assertNull(target1.getProperty(schema, entry.getKey()));
            assertNull(target2.getProperty(schema, entry.getKey()));
        }
    }

}
