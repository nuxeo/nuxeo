package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
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
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;

public class TestJODProcessingWithFileNote extends SQLRepositoryTestCase {

    private DocumentModel templateDoc;

    private DocumentModel testDoc;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployContrib("org.nuxeo.ecm.platform.template.manager",
                "OSGI-INF/core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.manager",
                "OSGI-INF/life-cycle-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.manager",
                "OSGI-INF/adapter-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.manager",
                "OSGI-INF/templateprocessor-service.xml");
        deployContrib("org.nuxeo.ecm.platform.template.manager",
                "OSGI-INF/templateprocessor-contrib.xml");
        openSession();
    }

    protected void setupTestDocs() throws Exception {

        DocumentModel root = session.getRootDocument();


        // create the template
        templateDoc = session.createDocumentModel(root
                .getPathAsString(), "templatedDoc", "TemplateSource");
        templateDoc.setProperty("dublincore", "title", "MyTemplate");
        File file = FileUtils
                .getResourceFileFromContext("data/master2.odt");
        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("master.odt");
        templateDoc.setProperty("file", "content", fileBlob);

        templateDoc = session.createDocument(templateDoc);


        // create the note
        testDoc = session.createDocumentModel(root
                .getPathAsString(), "testDoc", "TemplateBasedNote");
        testDoc.setProperty("dublincore", "title", "MyTestNote");
        testDoc.setPropertyValue("note:note", "<h1>Title 1</h1> Some text <br/><h1>Title2 </h1> Some more text<br/>");

        File imgFile = FileUtils.getResourceFileFromContext("data/android.jpg");
        Blob imgBlob = new FileBlob(imgFile);
        imgBlob.setFilename("android.jpg");
        imgBlob.setMimeType("image/jpeg");

        List<Map<String, Serializable>> blobs = new ArrayList<Map<String,Serializable>>();
        Map<String, Serializable> blob1 = new HashMap<String, Serializable>();
        blob1.put("file", (Serializable) imgBlob);
        blob1.put("filename", "android.jpg");
        blobs.add(blob1);

        testDoc.setPropertyValue("files:files", (Serializable)blobs);

        testDoc = session.createDocument(testDoc);
    }


    public void testNothing() throws Exception {
        // Shut up for now
    }

    public void XXXtestNoteWithMasterTemplateAndPicture() throws Exception {

        setupTestDocs();

        // check the template

        TemplateSourceDocument source = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(source);

        // init params
        source.initTemplate(true);

        List<TemplateInput> params = source.getParams();
        System.out.println(params);
        assertEquals(2, params.size());
        //assertEquals(InputType.PictureProperty, params.get(0).getType());
        assertEquals(InputType.Include, params.get(1).getType());

        // Set params value
        params.get(0).setType(InputType.PictureProperty);
        params.get(0).setSource("files:files/0/file");
        params.get(1).setSource("note:note");

        templateDoc = source.saveParams(params, true);

        TemplateBasedDocument templateBased = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(templateBased);

        // associate to template
        templateBased.setTemplate(templateDoc, true);

        // render
        testDoc = templateBased.initializeFromTemplate(true);
        templateBased.renderAndStoreAsAttachment(true);

        // check result
        Blob blob = (Blob)testDoc.getPropertyValue("file:content");
        assertNotNull(blob);

        File testFile = new File ("/tmp/testOOo.odt");

        blob.transferTo(testFile);

    }
}
