package org.nuxeo.ecm.core.io.marshallers.csv.document;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCsvWriterTest;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class DocumentModelCsvWriterTest extends AbstractCsvWriterTest.Local<DocumentModelCsvWriter, DocumentModel> {

    public DocumentModelCsvWriterTest() {
        super(DocumentModelCsvWriter.class, DocumentModel.class);
    }

    private DocumentModel document;

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        document = session.createDocumentModel("/", "myDoc", "RefDoc");
        document = session.createDocument(document);
        document.setPropertyValue("dc:title", "coucou");
        document = session.saveDocument(document);
    }

    @Test
    public void test() throws Exception {
        System.out.println(asCsv(document));
    }

    @Test
    public void testWithProperties() throws Exception {
        System.out.println(asCsv(document, CtxBuilder.properties("dublincore").get()));
    }

}
