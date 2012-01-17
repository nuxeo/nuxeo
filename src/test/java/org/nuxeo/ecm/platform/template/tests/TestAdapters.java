package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;

public class TestAdapters extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");

        deployContrib("org.nuxeo.ecm.platform.template.manager","OSGI-INF/core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.manager","OSGI-INF/life-cycle-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.manager","OSGI-INF/adapter-contrib.xml");
        openSession();
    }

    public void testAdapters() throws Exception {

         // create a template doc
         DocumentModel root = session.getRootDocument();
         DocumentModel template = session.createDocumentModel(
         root.getPathAsString(), "template", "TemplateSource");
         template.setProperty("dublincore", "title", "MyTemplate");
         File file = FileUtils.getResourceFileFromContext("data/hello.docx");
         assertNotNull(file);
         Blob fileBlob = new FileBlob(file);
         fileBlob.setFilename("hello.docx");
         template.setProperty("file", "content", fileBlob);
         template = session.createDocument(template);


         // test the adapter
         TemplateSourceDocument templateAdapter = template.getAdapter(TemplateSourceDocument.class);
         assertNotNull(templateAdapter);

         Blob blob = templateAdapter.getTemplateBlob();
         assertNotNull(blob);
         assertEquals("hello.docx", blob.getFilename());

         // add fields
         TemplateInput input1 = new TemplateInput("field1","Value1");
         input1.setDesciption("Some description");

         templateAdapter.addInput(input1);
         assertNotNull(templateAdapter.getAdaptedDoc().getPropertyValue("tmpl:templateData"));
         assertEquals(1,templateAdapter.getParams().size());
         assertNotNull(templateAdapter.getParamsAsString());

         template = templateAdapter.save();

         session.save();

         // Test template based doc

         DocumentModel testDoc = session.createDocumentModel(
         root.getPathAsString(), "templatedDoc", "TemplateBasedFile");
         testDoc.setProperty("dublincore", "title", "MyTestDoc");
         testDoc = session.createDocument(testDoc);

         TemplateBasedDocument adapter = testDoc.getAdapter(TemplateBasedDocument.class);
         assertNotNull(adapter);

         // associated to template
         testDoc = adapter.setTemplate(templateAdapter.getAdaptedDoc(), true);
         testDoc = adapter.initializeFromTemplate(true);

         // check fields
         List<TemplateInput> copiedParams = adapter.getParams();
         assertNotNull(copiedParams);
         assertEquals(1, copiedParams.size());
         assertEquals("field1", copiedParams.get(0).getName());
         assertEquals("Value1", copiedParams.get(0).getStringValue());

         // check update
         copiedParams.get(0).setStringValue("newValue");
         adapter.saveParams(copiedParams, true);
         copiedParams = adapter.getParams();
         assertEquals("newValue", copiedParams.get(0).getStringValue());

    }

}
