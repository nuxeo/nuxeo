package org.nuxeo.ecm.automation.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodec;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.automation.server.test.MyObject;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.test", "org.nuxeo.ecm.automation.io",
        "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.webengine.core" })
@LocalDeploy("org.nuxeo.ecm.automation.test:test-bindings-compat.xml")
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
            assertTrue(e.getMessage().startsWith(
                    "An operation is already bound to: "
                            + DummyCreateDocument.ID));
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
    public void testCompatibilityRegistration() throws Exception {
        ObjectCodec codec = objectCodecService.getCodec(MyObject.class);
        assertNotNull(codec);
    }
}
