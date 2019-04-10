package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public abstract class SimpleTemplateDocTestCase extends SQLRepositoryTestCase {

    protected abstract Blob getTemplateBlob();

    protected static final String TEMPLATE_NAME = "mytestTemplate";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployBundle("org.nuxeo.template.manager.api");
        deployBundle("org.nuxeo.template.manager");
        openSession();
    }

    protected TemplateBasedDocument setupTestDocs() throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(
                root.getPathAsString(), "templatedDoc", "TemplateSource");
        templateDoc.setProperty("dublincore", "title", "MyTemplate");
        templateDoc.setPropertyValue("tmpl:templateName", TEMPLATE_NAME);
        Blob fileBlob = getTemplateBlob();
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc = session.createDocument(templateDoc);

        TemplateSourceDocument templateSource = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(templateSource);
        assertEquals(TEMPLATE_NAME, templateSource.getName());

        // now create a template based doc
        DocumentModel testDoc = session.createDocumentModel(
                root.getPathAsString(), "templatedBasedDoc",
                "TemplateBasedFile");
        testDoc.setProperty("dublincore", "title", "MyTestDoc");
        testDoc.setProperty("dublincore", "description", "some description");

        // set dc:subjects
        List<String> subjects = new ArrayList<String>();
        subjects.add("Subject 1");
        subjects.add("Subject 2");
        subjects.add("Subject 3");
        testDoc.setPropertyValue("dc:subjects", (Serializable) subjects);

        // add an image as first entry of files
        File imgFile = FileUtils.getResourceFileFromContext("data/android.jpg");
        Blob imgBlob = new FileBlob(imgFile);
        imgBlob.setFilename("android.jpg");
        List<Map<String, Serializable>> blobs = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> blob1 = new HashMap<String, Serializable>();
        blob1.put("file", (Serializable) imgBlob);
        blob1.put("filename", "android.jpg");
        blobs.add(blob1);
        testDoc.setPropertyValue("files:files", (Serializable) blobs);

        testDoc = session.createDocument(testDoc);

        // associate doc and template
        TemplateBasedDocument adapter = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        adapter.setTemplate(templateDoc, true);

        return adapter;
    }

    protected List<TemplateInput> getTestParams() {

        List<TemplateInput> params = new ArrayList<TemplateInput>();
        TemplateInput input1 = new TemplateInput("StringVar", "John Smith");
        TemplateInput input2 = new TemplateInput("DateVar", new Date());
        TemplateInput input3 = new TemplateInput("Description");
        input3.setType(InputType.PictureProperty);
        input3.setSource("dc:description");
        TemplateInput input4 = new TemplateInput("BooleanVar", new Boolean(
                false));
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

    @Override
    public void tearDown() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

}
