package org.nuxeo.template.processors.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.docx.WordXMLRawTemplateProcessor;

public class TestWordXMLRawProcessing extends SQLRepositoryTestCase {

    protected static final String TEMPLATE_NAME = "mytestTemplate";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");

        deployBundle("org.nuxeo.template.manager.api");
        deployBundle("org.nuxeo.template.manager");
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

    protected List<TemplateInput> getTestParams() {

        List<TemplateInput> params = new ArrayList<TemplateInput>();
        TemplateInput input1 = new TemplateInput("sName_of_Licensee", "John Smith");
        TemplateInput input2 = new TemplateInput("s_effective_date", new Date());
        TemplateInput input3 = new TemplateInput("name_of_the_call");
        input3.setType(InputType.DocumentProperty);
        input3.setSource("dc:description");
        TemplateInput input4 = new TemplateInput("boolean_property", new Boolean(false));

        params.add(input1);
        params.add(input2);
        params.add(input3);
        params.add(input4);

        return params;
    }

    protected TemplateBasedDocument setupTestDocs() throws Exception {

        DocumentModel root = session.getRootDocument();

        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), "templatedDoc",
                "TemplateSource");
        templateDoc.setProperty("dublincore", "title", "MyTemplate");
        templateDoc.setPropertyValue("tmpl:templateName", TEMPLATE_NAME);
        File file = FileUtils.getResourceFileFromContext("data/sample templatet.docx");
        Blob fileBlob = Blobs.createBlob(file);
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc = session.createDocument(templateDoc);

        DocumentModel testDoc = session.createDocumentModel(root.getPathAsString(), "templatedDoc", "TemplateBasedFile");
        testDoc.setProperty("dublincore", "title", "MyTestDoc");
        fileBlob.setFilename("sample templatet.docx");
        // testDoc.setProperty("file", "content", fileBlob);
        testDoc.setProperty("dublincore", "description", "some description");
        testDoc = session.createDocument(testDoc);

        TemplateBasedDocument adapter = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        // associate doc and template
        adapter.setTemplate(templateDoc, true);

        return adapter;
    }

    @Test
    public void testFileUpdateFromParams() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);
        List<TemplateInput> params = getTestParams();

        testDoc = adapter.saveParams(TEMPLATE_NAME, params, true);
        session.save();

        WordXMLRawTemplateProcessor processor = new WordXMLRawTemplateProcessor();

        Blob newBlob = processor.renderTemplate(adapter, TEMPLATE_NAME);

        String xmlContent = processor.readPropertyFile(newBlob.getStream());

        // System.out.println(xmlContent);

        assertTrue(xmlContent.contains("name=\"sName_of_Licensee\"><vt:lpwstr>John Smith</vt:lpwstr>"));
        assertTrue(xmlContent.contains("name=\"name_of_the_call\"><vt:lpwstr>some description</vt:lpwstr>"));

    }

    /**
     * Broken for now public void testDocumentUpdateFromFile() throws Exception { TemplateBasedDocument adapter =
     * setupTestDocs(); DocumentModel testDoc = adapter.getAdaptedDoc(); List<TemplateInput> params = getTestParams();
     * testDoc = adapter.saveParams(params, true); session.save(); BidirectionalTemplateProcessor processor = new
     * WordXMLTemplateProcessor(); testDoc = processor.updateDocumentFromBlob(adapter); String updatedContent =
     * testDoc.getPropertyValue("dc:description") .toString(); assertEquals("name of the call", updatedContent); }
     **/

    @Test
    public void testParameterInit() throws Exception {

        File file = FileUtils.getResourceFileFromContext("data/sample templatet.docx");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("sample templatet.docx");

        TemplateProcessor processor = new WordXMLRawTemplateProcessor();

        List<TemplateInput> params = processor.getInitialParametersDefinition(fileBlob);

        assertNotNull(params);
        assertEquals(4, params.size());

        assertEquals("sName_of_Licensee", params.get(0).getName());
        assertEquals(InputType.StringValue, params.get(0).getType());

        assertEquals("s_effective_date", params.get(1).getName());
        assertEquals(InputType.DateValue, params.get(1).getType());

        assertEquals("name_of_the_call", params.get(2).getName());
        assertEquals(InputType.StringValue, params.get(2).getType());

        assertEquals("boolean_property", params.get(3).getName());
        assertEquals(InputType.BooleanValue, params.get(3).getType());

    }
}
