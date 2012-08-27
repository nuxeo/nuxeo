/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.LogOperation;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentInputFromScript;
import org.nuxeo.ecm.automation.core.operations.SetInputAsVar;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import com.google.inject.Inject;

/**
 * Testing RestoreDocumentInputFromScript and LogOperation operations.
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, LogCaptureFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.core" })
@LogCaptureFeature.FilterWith(TestRestoreInputFromScriptAndLogOperation.MyLogFilter.class)
public class TestRestoreInputFromScriptAndLogOperation {
    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    LogCaptureFeature.Result logCaptureResult;

    public static class MyLogFilter implements LogCaptureFeature.Filter {

        @Override
        public boolean accept(LoggingEvent event) {
            if (!event.getLevel().equals(Level.ERROR)) {
                return false;
            }
            if (!event.getLoggerName().equals("loggerName")) {
                return false;
            }
            return true;
        }

    }

    @Test
    public void testRestoreInput() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "test", "File");
        doc.setPropertyValue("dc:title", "test");
        doc = session.createDocument(doc);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);

        OperationChain chain = new OperationChain("testSetObjectInput");

        chain.add(FetchContextDocument.ID);
        // put the document in the context
        chain.add(SetInputAsVar.ID).set("name", "test");
        // set the input with fetch /
        chain.add(FetchDocument.ID).set("value", "/");
        // use the new operation to restore the input
        chain.add(RestoreDocumentInputFromScript.ID).set("script",
                "Context[\"test\"]");
        chain.add(LogOperation.ID).set("category", "loggerName").set("message",
                "expr:Input title @{This.title}. next id : @{Fn.getNextId(\"pouet\")}").set(
                "level", "error");

        // assert that the output is "/test" is the one retrieved from the
        // context variable
        DocumentModel returnedDoc = (DocumentModel) service.run(ctx, chain);
        Assert.assertEquals("/test", returnedDoc.getPathAsString());
        // making sure that
        logCaptureResult.assertHasEvent();
    }
}
