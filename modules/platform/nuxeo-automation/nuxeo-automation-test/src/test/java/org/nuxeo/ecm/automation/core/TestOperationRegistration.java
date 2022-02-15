/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */
package org.nuxeo.ecm.automation.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.internals.ScriptingOperationTypeImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.OperationTypeImpl;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.test.AutomationServerFeature;
import org.nuxeo.ecm.automation.test.helpers.TestOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationServerFeature.class)
@Deploy("org.nuxeo.ecm.automation.test")
@Deploy("org.nuxeo.ecm.webengine.core")
public class TestOperationRegistration {

    @Inject
    AutomationService service;

    @Inject
    ObjectCodecService objectCodecService;

    @Test
    public void testRegistration() throws Exception {
        OperationType op = service.getOperation(CreateDocument.ID);
        assertEquals(CreateDocument.class, op.getType());

        // register new operation to override existing one, but replace = false
        // (default value)
        try {
            service.putOperation(DummyCreateDocument.class);
        } catch (OperationException e) {
            assertTrue(e.getMessage().startsWith("An operation is already bound to: " + DummyCreateDocument.ID));
        }
        // check nothing has changed
        op = service.getOperation(CreateDocument.ID);
        assertEquals(CreateDocument.class, op.getType());

        // register new operation to override existing one with replace = true,
        service.putOperation(DummyCreateDocument.class, true);
        try {
            op = service.getOperation(CreateDocument.ID);
        } catch (OperationException e) {
            // should not happen
        }
        assertEquals(DummyCreateDocument.class, op.getType());
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.test.test:operation-contrib.xml")
    @Deploy("org.nuxeo.ecm.automation.test.test:chain-scripting-operation-contrib.xml")
    public void testContributingComponent() throws Exception {
        OperationType op = service.getOperation(SaveDocument.ID);
        assertEquals("service:org.nuxeo.ecm.core.automation.coreContrib", op.getContributingComponent());
        // check operation from another component
        op = service.getOperation(TestOperation.ID);
        assertTrue(op instanceof OperationTypeImpl);
        assertEquals("service:org.nuxeo.automation.rest.test.operationContrib", op.getContributingComponent());
        // check chains
        op = service.getOperation("FileManager.ImportWithMetaData");
        assertTrue(op instanceof ChainTypeImpl);
        assertEquals("service:org.nuxeo.ecm.core.automation.features.operations", op.getContributingComponent());
        // check chain from another component
        op = service.getOperation("testChain");
        assertTrue(op instanceof ChainTypeImpl);
        assertEquals("service:org.nuxeo.automation.rest.test.operationContrib", op.getContributingComponent());
        // check chain old-style
        op = service.getOperation("testChain2");
        assertTrue(op instanceof ChainTypeImpl);
        assertEquals("service:org.nuxeo.automation.rest.test.chainScriptingOperationContrib",
                op.getContributingComponent());
        // check scripting
        op = service.getOperation("javascript.RemoteScriptWithDoc");
        assertTrue(op instanceof ScriptingOperationTypeImpl);
        assertEquals("service:org.nuxeo.automation.rest.test.operationContrib", op.getContributingComponent());
    }

}
