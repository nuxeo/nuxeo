package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.template.InputType;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.XMLSerializer;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.processors.fm.JODReportTemplateProcessor;

public class TestJODProcessing extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");

        deployContrib("org.nuxeo.ecm.platform.template.managaner",
                "OSGI-INF/core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.managaner",
                "OSGI-INF/life-cycle-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.managaner",
                "OSGI-INF/adapter-contrib.xml");
        openSession();
    }

    protected List<TemplateInput> getTestParams() {

        List<TemplateInput> params = new ArrayList<TemplateInput>();
        TemplateInput input1 = new TemplateInput("StringVar","John Smith");
        TemplateInput input2 = new TemplateInput("DateVar", new Date());
        TemplateInput input3 = new TemplateInput("Description");
        input3.setType(InputType.StringValue);
        input3.setSource("dc:description");
        TemplateInput input4 = new TemplateInput("BooleanVar",
                new Boolean(false));
        TemplateInput input5 = new TemplateInput("picture");
        input5.setType(InputType.PictureProperty);
        input5.setSource("files:files/0/file");

        params.add(input1);
        params.add(input2);
        params.add(input3);
        params.add(input4);
        params.add(input5);

        return params;
    }

    protected TemplateBasedDocument setupTestDocs() throws Exception {

        DocumentModel root = session.getRootDocument();
        DocumentModel testDoc = session.createDocumentModel(root
                .getPathAsString(), "templatedDoc", "TemplateBasedFile");
        testDoc.setProperty("dublincore", "title", "MyTestDoc");

        File file = FileUtils
                .getResourceFileFromContext("data/testDoc.odt");

        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("testDoc.odt");
        testDoc.setProperty("file", "content", fileBlob);
        testDoc.setProperty("dublincore", "description", "some description");

        File imgFile = FileUtils.getResourceFileFromContext("data/android.jpg");
        Blob imgBlob = new FileBlob(imgFile);
        imgBlob.setFilename("android.jpg");
        List<Map<String, Serializable>> blobs = new ArrayList<Map<String,Serializable>>();
        Map<String, Serializable> blob1 = new HashMap<String, Serializable>();
        blob1.put("file", (Serializable) imgBlob);
        blob1.put("filename", "android.jpg");
        blobs.add(blob1);

        testDoc.setPropertyValue("files:files", (Serializable)blobs);

        testDoc = session.createDocument(testDoc);

        TemplateBasedDocument adapter = testDoc
                .getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        return adapter;
    }

    public void testFileUpdateFromParams() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        List<TemplateInput> params = getTestParams();

        testDoc = adapter.saveParams(params, true);
        session.save();

        JODReportTemplateProcessor processor = new JODReportTemplateProcessor();

        Blob newBlob = processor.renderTemplate(adapter);

        System.out.println(((FileBlob)newBlob).getFile().getAbsolutePath());

        String xmlContent = processor.readXMLContent(newBlob);

        assertTrue(xmlContent.contains("John Smith"));
        assertTrue(xmlContent.contains("some description"));
        assertTrue(xmlContent.contains("The boolean var is false"));

        System.out.println(xmlContent);

    }


    public void testParamExtraction() throws Exception {
        JODReportTemplateProcessor processor = new JODReportTemplateProcessor();
        File file = FileUtils
        .getResourceFileFromContext("data/testDoc.odt");

        List<TemplateInput> inputs = processor.getInitialParametersDefinition(new FileBlob(file));

        String[] expectedVars = new String[]{"StringVar","DateVar", "Description","picture", "BooleanVar"};

        assertEquals(expectedVars.length, inputs.size());
        for (String expected : expectedVars) {
            boolean found = false;
            for (TemplateInput input : inputs) {
                if (expected.equals(input.getName())) {
                    found=true;
                    break;
                }
            }
            assertTrue(found);
        }

        String xmlParams = XMLSerializer.serialize(inputs);

        for (TemplateInput input : inputs) {
            assertTrue(xmlParams.contains("name=\"" + input.getName() + "\""));
        }

        List<TemplateInput> inputs2 = XMLSerializer.readFromXml(xmlParams);

        assertEquals(inputs.size(), inputs2.size());
        for (TemplateInput input : inputs) {
            boolean found = false;
            for (TemplateInput input2 : inputs2) {
                if (input2.getName().equals(input.getName())) {
                    found=true;
                    break;
                }
            }
            assertTrue(found);
        }
    }


}
