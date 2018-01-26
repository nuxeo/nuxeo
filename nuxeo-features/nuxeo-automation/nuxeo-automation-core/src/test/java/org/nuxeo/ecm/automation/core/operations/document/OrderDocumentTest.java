/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class OrderDocumentTest {

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    protected DocumentModel folder1;

    protected DocumentModel folder2;

    protected DocumentModel file11;

    protected DocumentModel file12;

    protected DocumentModel file13;

    protected DocumentModel file14;

    protected DocumentModel file15;

    protected DocumentModel file21;

    @Before
    public void initRepo() throws Exception {

        folder1 = session.createDocumentModel("/", "Folder1", "OrderedFolder");
        folder1.setPropertyValue("dc:title", "Folder1");
        folder1 = session.createDocument(folder1);
        session.save();

        folder2 = session.createDocumentModel("/", "Folder2", "OrderedFolder");
        folder2.setPropertyValue("dc:title", "Folder1");
        folder2 = session.createDocument(folder2);
        session.save();

        file11 = session.createDocumentModel("/Folder1", "File11", "File");
        file11.setPropertyValue("dc:title", "File11");
        file11 = session.createDocument(file11);
        session.save();

        file12 = session.createDocumentModel("/Folder1", "File12", "File");
        file12.setPropertyValue("dc:title", "File11");
        file12 = session.createDocument(file12);
        session.save();

        file13 = session.createDocumentModel("/Folder1", "File13", "File");
        file13.setPropertyValue("dc:title", "File13");
        file13 = session.createDocument(file13);
        session.save();

        file14 = session.createDocumentModel("/Folder1", "File14", "File");
        file14.setPropertyValue("dc:title", "File14");
        file14 = session.createDocument(file14);
        session.save();

        file15 = session.createDocumentModel("/Folder1", "File15", "File");
        file15.setPropertyValue("dc:title", "File15");
        file15 = session.createDocument(file15);
        session.save();

        file21 = session.createDocumentModel("/Folder2", "File21", "File");
        file21.setPropertyValue("dc:title", "File21");
        file21 = session.createDocument(file21);
        session.save();
    }

    @Test
    public void testIllegalOrderDocument() throws OperationException {
        try {
            moveBefore(file12, file21);
            fail("Should not be able to order in different folder");
        } catch (NuxeoException e) {
            assertTrue(e.getMessage().contains(OrderDocument.NOT_SAME_FOLDER_ERROR_MSG));
        }
    }

    @Test
    public void testOrderDocument() throws OperationException {
        assertEquals(Long.valueOf(0), file11.getPos());
        assertEquals(Long.valueOf(1), file12.getPos());
        assertEquals(Long.valueOf(2), file13.getPos());
        assertEquals(Long.valueOf(3), file14.getPos());
        assertEquals(Long.valueOf(4), file15.getPos());

        // Effective
        moveBefore(file12, file11);

        assertEquals(Long.valueOf(1), file11.getPos());
        assertEquals(Long.valueOf(0), file12.getPos());
        assertEquals(Long.valueOf(2), file13.getPos());
        assertEquals(Long.valueOf(3), file14.getPos());
        assertEquals(Long.valueOf(4), file15.getPos());

        // Ineffective i.e. unchanged
        moveBefore(file11, file13);

        assertEquals(Long.valueOf(1), file11.getPos());
        assertEquals(Long.valueOf(0), file12.getPos());
        assertEquals(Long.valueOf(2), file13.getPos());
        assertEquals(Long.valueOf(3), file14.getPos());
        assertEquals(Long.valueOf(4), file15.getPos());

        // Effective
        moveBefore(file13, file11);

        assertEquals(Long.valueOf(2), file11.getPos());
        assertEquals(Long.valueOf(0), file12.getPos());
        assertEquals(Long.valueOf(1), file13.getPos());
        assertEquals(Long.valueOf(3), file14.getPos());
        assertEquals(Long.valueOf(4), file15.getPos());

        // Move last
        moveBefore(file12, null);

        assertEquals(Long.valueOf(1), file11.getPos());
        assertEquals(Long.valueOf(4), file12.getPos());
        assertEquals(Long.valueOf(0), file13.getPos());
        assertEquals(Long.valueOf(2), file14.getPos());
        assertEquals(Long.valueOf(3), file15.getPos());
    }

    @Test
    public void testOrderDocumentMultipleSimple() throws OperationException {
        assertEquals(Long.valueOf(0), file11.getPos());
        assertEquals(Long.valueOf(1), file12.getPos());
        assertEquals(Long.valueOf(2), file13.getPos());

        // Effective
        moveBeforeMultiple(new DocumentModel[] { file12, file13 }, file11);

        assertEquals(Long.valueOf(2), file11.getPos());
        assertEquals(Long.valueOf(0), file12.getPos());
        assertEquals(Long.valueOf(1), file13.getPos());

        // Ineffective
        moveBeforeMultiple(new DocumentModel[] { file12, file13 }, file11);

        assertEquals(Long.valueOf(2), file11.getPos());
        assertEquals(Long.valueOf(0), file12.getPos());
        assertEquals(Long.valueOf(1), file13.getPos());
    }

    @Test
    public void testOrderDocumentMultipleComlex() throws OperationException {
        assertEquals(Long.valueOf(0), file11.getPos());
        assertEquals(Long.valueOf(1), file12.getPos());
        assertEquals(Long.valueOf(2), file13.getPos());
        assertEquals(Long.valueOf(3), file14.getPos());
        assertEquals(Long.valueOf(4), file15.getPos());

        // Effective
        moveBeforeMultiple(new DocumentModel[] { file13, file15 }, file12);

        assertEquals(Long.valueOf(0), file11.getPos());
        assertEquals(Long.valueOf(3), file12.getPos());
        assertEquals(Long.valueOf(1), file13.getPos());
        assertEquals(Long.valueOf(4), file14.getPos());
        assertEquals(Long.valueOf(2), file15.getPos());
    }

    @Test
    public void testOrderDocumentMultipleLast() throws OperationException {
        assertEquals(Long.valueOf(0), file11.getPos());
        assertEquals(Long.valueOf(1), file12.getPos());
        assertEquals(Long.valueOf(2), file13.getPos());
        assertEquals(Long.valueOf(3), file14.getPos());
        assertEquals(Long.valueOf(4), file15.getPos());

        // Effective
        moveBeforeMultiple(new DocumentModel[] { file13, file15 }, null);

        assertEquals(Long.valueOf(0), file11.getPos());
        assertEquals(Long.valueOf(1), file12.getPos());
        assertEquals(Long.valueOf(3), file13.getPos());
        assertEquals(Long.valueOf(2), file14.getPos());
        assertEquals(Long.valueOf(4), file15.getPos());
    }

    protected void moveBefore(DocumentModel src, DocumentModel dest) throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        Map<String, Object> params = new HashMap<>();
        if (dest != null) {
            params.put("before", dest);
        }
        service.run(ctx, OrderDocument.ID, params);
        file11 = session.getDocument(file11.getRef());
        file12 = session.getDocument(file12.getRef());
        file13 = session.getDocument(file13.getRef());
        file14 = session.getDocument(file14.getRef());
        file15 = session.getDocument(file15.getRef());
    }

    protected void moveBeforeMultiple(DocumentModel[] srcList, DocumentModel dest) throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(Arrays.asList(srcList));
        Map<String, Object> params = new HashMap<>();
        if (dest != null) {
            params.put("before", dest);
        }
        service.run(ctx, OrderDocument.ID, params);
        file11 = session.getDocument(file11.getRef());
        file12 = session.getDocument(file12.getRef());
        file13 = session.getDocument(file13.getRef());
        file14 = session.getDocument(file14.getRef());
        file15 = session.getDocument(file15.getRef());
    }

}
