/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.template.manager.api")
@Deploy("org.nuxeo.template.manager")
public class TestTemplateSourceTypeBindings {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected TemplateProcessorService tps;

    protected void waitForAsyncCompletion() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        eventService.waitForAsyncCompletion();
    }

    protected TemplateSourceDocument createTemplateDoc(String name) throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), name, "TemplateSource");
        templateDoc.setProperty("dublincore", "title", name);
        File file = FileUtils.getResourceFileFromContext("data/testDoc.odt");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("testDoc.odt");
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc = session.createDocument(templateDoc);
        session.save();

        TemplateSourceDocument result = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(result);
        return result;
    }

    protected TemplateSourceDocument createWebTemplateDoc(String name) throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), name, "WebTemplateSource");
        templateDoc.setProperty("dublincore", "title", name);
        templateDoc.setProperty("note", "note", "Template ${doc.title}");
        templateDoc = session.createDocument(templateDoc);
        session.save();

        TemplateSourceDocument result = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(result);
        return result;
    }

    @Test
    public void testTypeBindingAndOverride() throws Exception {

        // test simple mapping
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        t1.setForcedTypes(new String[] { "File", "Note" }, true);

        assertTrue(t1.getForcedTypes().contains("File"));
        assertTrue(t1.getForcedTypes().contains("Note"));

        session.save();

        // wait for Async listener to run !
        waitForAsyncCompletion();

        Map<String, List<String>> mapping = tps.getTypeMapping();

        assertTrue(mapping.get("File").contains(t1.getAdaptedDoc().getId()));
        assertTrue(mapping.get("Note").contains(t1.getAdaptedDoc().getId()));

        // wait for Async listener to run !
        waitForAsyncCompletion();

        // test override
        TemplateSourceDocument t2 = createTemplateDoc("t2");
        t2.setForcedTypes(new String[] { "Note" }, true);

        assertFalse(t2.getForcedTypes().contains("File"));
        assertTrue(t2.getForcedTypes().contains("Note"));

        session.save();

        // wait for Async listener to run !
        waitForAsyncCompletion();

        session.save();

        mapping = tps.getTypeMapping();

        assertTrue(mapping.get("File").contains(t1.getAdaptedDoc().getId()));
        assertTrue(mapping.get("Note").contains(t1.getAdaptedDoc().getId()));
        assertTrue(mapping.get("Note").contains(t2.getAdaptedDoc().getId()));

        // check update on initial template
        // re-fetch staled DocumentModel
        t1 = session.getDocument(new IdRef(t1.getAdaptedDoc().getId())).getAdapter(TemplateSourceDocument.class);
        assertTrue(t1.getForcedTypes().contains("File"));
        assertTrue(t1.getForcedTypes().contains("Note"));
    }

    @Test
    public void testAutomaticTemplateBinding() throws Exception {

        // create a template and a simple mapping
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        t1.setForcedTypes(new String[] { "File" }, true);
        assertTrue(t1.getForcedTypes().contains("File"));
        session.save();

        // wait for Async listener to run !
        waitForAsyncCompletion();

        // now create a simple file
        DocumentModel root = session.getRootDocument();
        DocumentModel simpleFile = session.createDocumentModel(root.getPathAsString(), "myTestFile", "File");
        simpleFile = session.createDocument(simpleFile);

        session.save();

        // verify that template has been associated
        TemplateBasedDocument templatizedFile = simpleFile.getAdapter(TemplateBasedDocument.class);
        assertNotNull(templatizedFile);

        // remove binding
        t1.setForcedTypes(new String[] {}, true);
        session.save();
        waitForAsyncCompletion();

        // now create a simple file
        DocumentModel simpleFile2 = session.createDocumentModel(root.getPathAsString(), "myTestFile2", "File");
        simpleFile2 = session.createDocument(simpleFile2);

        // verify that template has NOT been associated
        assertNull(simpleFile2.getAdapter(TemplateBasedDocument.class));

        // restore binding
        t1.setForcedTypes(new String[] { "File" }, true);
        assertTrue(t1.getForcedTypes().contains("File"));
        session.save();
        waitForAsyncCompletion();
        // change template's state to 'deleted'
        session.followTransition(t1.getAdaptedDoc(), LifeCycleConstants.DELETE_TRANSITION);
        assertEquals(LifeCycleConstants.DELETED_STATE, session.getDocument(t1.getAdaptedDoc().getRef()).getCurrentLifeCycleState());
        // now create a simple file
        DocumentModel simpleFile3 = session.createDocumentModel(root.getPathAsString(), "myTestFile3", "File");
        simpleFile3 = session.createDocument(simpleFile3);
        // verify that template has NOT been associated
        assertNull(simpleFile3.getAdapter(TemplateBasedDocument.class));
    }

    @Test
    public void testManualTemplateBinding() throws Exception {

        // create a template and no mapping
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        session.save();

        // now create a simple Note
        DocumentModel root = session.getRootDocument();
        DocumentModel simpleNote = session.createDocumentModel(root.getPathAsString(), "myTestFile", "Note");
        simpleNote = session.createDocument(simpleNote);

        session.save();

        // verify that not template is associated
        assertNull(simpleNote.getAdapter(TemplateBasedDocument.class));

        simpleNote = tps.makeTemplateBasedDocument(simpleNote, t1.getAdaptedDoc(), true);

        // verify that template has been associated
        assertNotNull(simpleNote.getAdapter(TemplateBasedDocument.class));

    }

    @Test
    public void testAutomaticTemplateMultiBinding() throws Exception {

        // create a template and a simple mapping
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        t1.setForcedTypes(new String[] { "File" }, true);
        assertTrue(t1.getForcedTypes().contains("File"));
        session.save();

        // create a second template and a simple mapping
        TemplateSourceDocument t2 = createTemplateDoc("t2");
        t2.setForcedTypes(new String[] { "File" }, true);
        assertTrue(t2.getForcedTypes().contains("File"));
        session.save();

        // wait for Async listener to run !
        waitForAsyncCompletion();

        // now create a simple file
        DocumentModel root = session.getRootDocument();
        DocumentModel simpleFile = session.createDocumentModel(root.getPathAsString(), "myTestFile", "File");
        simpleFile = session.createDocument(simpleFile);

        session.save();

        // verify that template has been associated
        TemplateBasedDocument templatizedFile = simpleFile.getAdapter(TemplateBasedDocument.class);
        assertNotNull(templatizedFile);

        List<String> templateNames = templatizedFile.getTemplateNames();

        assertTrue(templateNames.contains("t1"));
        assertTrue(templateNames.contains("t2"));

    }

}
