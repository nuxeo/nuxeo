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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

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

import com.google.inject.Inject;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class})
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:OSGI-INF/copy-schema-test-contrib.xml")
public class CopySchemaTest {

    @Inject
    private CoreSession session;

    @Inject
    private AutomationService service;

    private DocumentModel source;
    private DocumentModel target1;
    private DocumentModel target2;
    private DocumentModelList targets;

    @Before
    public void setUp() {
        source = session.createDocumentModel("/", "Source", "File");
        source = session.createDocument(source);
        session.save();

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
        session.saveDocument(target1);

        OperationContext context = new OperationContext(session);
        context.setInput(target1);
        OperationChain chain = new OperationChain("testThrowException");
        chain.add(CopySchema.ID).set("schema", schema);
        try {
            target1 = (DocumentModel)service.run(context, chain);
        } catch (OperationException e) {
            triggeredException = true;
        }
        assertTrue(triggeredException);
    }

    @Test
    public void testSingleTargetSinglePropertyById() throws OperationException {
        String schema = "dublincore";
        String property = "title";

        assertTrue(source.hasSchema(schema));
        source.setProperty(schema, property, source.getName());
        session.saveDocument(source);

        assertTrue(target1.hasSchema(schema));
        target1.setProperty(schema, property, target1.getName());
        session.saveDocument(target1);

        OperationContext context = new OperationContext(session);
        context.setInput(target1);
        OperationChain chain = new OperationChain("testSingleTargetSinglePropertyById");
        chain.add(CopySchema.ID).set("sourceId", source.getId()).set("schema", schema);
        target1 = (DocumentModel)service.run(context, chain);

        assertEquals(source.getProperty(schema, property), target1.getProperty(schema, property));
    }

    @Test
    public void testSingleTargetSinglePropertyByPath() throws OperationException {
        String schema = "dublincore";
        String property = "title";

        assertTrue(source.hasSchema(schema));
        source.setProperty(schema, property, source.getName());
        session.saveDocument(source);

        assertTrue(target1.hasSchema(schema));
        target1.setProperty(schema, property, target1.getName());
        session.saveDocument(target1);

        OperationContext context = new OperationContext(session);
        context.setInput(target1);
        OperationChain chain = new OperationChain("testSingleTargetSinglePropertyByPath");
        chain.add(CopySchema.ID).set("sourcePath", source.getPath().toString()).set("schema", schema);
        target1 = (DocumentModel)service.run(context, chain);

        assertEquals(source.getProperty(schema, property), target1.getProperty(schema, property));
    }

    @Test
    public void testSingleTargetFullSchema() throws OperationException {
        String schema = "common";
        assertTrue(source.hasSchema(schema));
        assertTrue(target1.hasSchema(schema));

        source.setProperty(schema, "icon-expanded", "icon-expanded1");
        source.setProperty(schema, "icon", "icon1");
        session.saveDocument(source);

        target1.setProperty(schema, "icon-expanded", "icon-expanded2");
        target1.setProperty(schema, "icon", "icon2");
        session.saveDocument(target1);

        OperationContext context = new OperationContext(session);
        context.setInput(target1);
        OperationChain chain = new OperationChain("testSingleTargetFullSchema");
        chain.add(CopySchema.ID).set("sourceId", source.getId()).set("schema", schema);
        target1 = (DocumentModel)service.run(context, chain);

        for (Map.Entry<String, Object> pair : source.getProperties(schema).entrySet()) {
            // ensure that the values of the properties for the two documents are now the same
            assertEquals(source.getProperty(schema, pair.getKey()), target1.getProperty(schema, pair.getKey()));
        }
    }

    @Test
    public void testMultipleTargetsSingleProperty() throws OperationException {
        String schema = "dublincore";
        String property = "title";

        assertTrue(source.hasSchema(schema));
        source.setProperty(schema, property, source.getName());
        session.saveDocument(source);

        assertTrue(target1.hasSchema(schema));
        target1.setProperty(schema, property, target1.getName());
        session.saveDocument(target1);

        assertTrue(target2.hasSchema(schema));
        target2.setProperty(schema, property, target2.getName());
        session.saveDocument(target2);

        OperationContext context = new OperationContext(session);
        context.setInput(targets);
        OperationChain chain = new OperationChain("testMultipleTargetsSingleProperty");
        chain.add(CopySchema.ID).set("sourceId", source.getId()).set("schema", schema);
        targets = (DocumentModelList)service.run(context, chain);

        assertEquals(target1, targets.get(0));
        assertEquals(target2, targets.get(1));
        assertEquals(source.getProperty(schema, property), target1.getProperty(schema, property));
        assertEquals(source.getProperty(schema, property), target2.getProperty(schema, property));
    }

    @Test
    public void testMultipleTargetsFullSchema() throws OperationException {
        String schema = "common";
        assertTrue(source.hasSchema(schema));
        assertTrue(target1.hasSchema(schema));
        assertTrue(target2.hasSchema(schema));

        source.setProperty(schema, "icon-expanded", "icon-expanded1");
        source.setProperty(schema, "icon", "icon1");
        session.saveDocument(source);

        target1.setProperty(schema, "icon-expanded", "icon-expanded2");
        target1.setProperty(schema, "icon", "icon2");
        session.saveDocument(target1);

        target2.setProperty(schema, "icon-expanded", "icon-expanded2");
        target2.setProperty(schema, "icon", "icon2");
        session.saveDocument(target2);

        OperationContext context = new OperationContext(session);
        context.setInput(targets);
        OperationChain chain = new OperationChain("testMultipleTargersFullSchema");
        chain.add(CopySchema.ID).set("sourceId", source.getId()).set("schema", schema);
        targets = (DocumentModelList)service.run(context, chain);

        assertEquals(target1, targets.get(0));
        assertEquals(target2, targets.get(1));
        for (Map.Entry<String, Object> pair : source.getProperties(schema).entrySet()) {
            // ensure that the values of the properties for the three documents are now the same
            assertEquals(source.getProperty(schema, pair.getKey()), target1.getProperty(schema, pair.getKey()));
            assertEquals(source.getProperty(schema, pair.getKey()), target2.getProperty(schema, pair.getKey()));
        }
    }

}
