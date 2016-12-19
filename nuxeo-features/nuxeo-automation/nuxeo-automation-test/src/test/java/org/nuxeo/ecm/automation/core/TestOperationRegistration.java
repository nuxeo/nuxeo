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
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.test", "org.nuxeo.ecm.automation.io", "org.nuxeo.ecm.automation.server",
        "org.nuxeo.ecm.webengine.core" })
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

}
