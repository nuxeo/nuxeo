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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.internals.ScriptingOperationTypeImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.OperationTypeImpl;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.core.operations.login.Logout;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.test.AutomationServerFeature;
import org.nuxeo.ecm.automation.test.helpers.TestOperation;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

@RunWith(FeaturesRunner.class)
@Features({ AutomationServerFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.ecm.automation.test")
@Deploy("org.nuxeo.ecm.webengine.core")
public class TestOperationRegistration {

    @Inject
    AutomationService service;

    @Inject
    ObjectCodecService objectCodecService;

    @Inject
    protected HotDeployer hotDeployer;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

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

    @Test
    @Deploy("org.nuxeo.ecm.automation.test.test:test-scripted-operation-contrib.xml")
    public void testDisableScriptingOperationType() throws Exception {
        service.getOperation("testScript");
        hotDeployer.deploy("org.nuxeo.ecm.automation.test.test:test-scripted-operation-disable-contrib.xml");
        try {
            service.getOperation("testScript");
            fail("should not have found a disabled scripted operation");
        } catch (OperationNotFoundException e) {
            assertEquals("No operation was bound on ID: testScript", e.getMessage());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.automation.test.test:test-scripted-operation-contrib.xml")
    @LogCaptureFeature.FilterOn(logLevel = "ERROR", loggerClass = RegistrationInfoImpl.class)
    public void testMixingOperationTypes() throws Exception {
        // Trying to merge a ChainTypeImpl into a ScriptingOperationTypeImpl
        service.getOperation("testScript");
        hotDeployer.deploy("org.nuxeo.ecm.automation.test.test:test-chain-operation-disable-contrib.xml");
        logCaptureResult.assertHasEvent();
        var logs = logCaptureResult.getCaughtEvents();
        assertEquals(1, logs.size());
        assertEquals(
                "java.lang.UnsupportedOperationException: Can't merge operations with id: testScript. The type class org.nuxeo.ecm.automation.core.impl.ChainTypeImpl cannot be merged in class org.nuxeo.automation.scripting.internals.ScriptingOperationTypeImpl.",
                logs.get(0).getThrown().toString());
        assertNotNull(service.getOperation("testScript"));
        hotDeployer.undeploy("org.nuxeo.ecm.automation.test.test:test-chain-operation-disable-contrib.xml");
        logCaptureResult.clear();

        // Trying to merge a ChainTypeImpl into an OperationTypeImpl
        service.getOperation(Logout.ID);
        hotDeployer.deploy("org.nuxeo.ecm.automation.test.test:test-mix-operationType-contrib.xml");
        logs = logCaptureResult.getCaughtEvents();
        assertEquals(1, logs.size());
        assertEquals(
                "java.lang.UnsupportedOperationException: Can't merge operations with id: Auth.Logout. The type class org.nuxeo.ecm.automation.core.impl.ChainTypeImpl cannot be merged in class org.nuxeo.ecm.automation.core.impl.OperationTypeImpl.",
                logs.get(0).getThrown().toString());
        assertNotNull(service.getOperation(Logout.ID));
    }

}
