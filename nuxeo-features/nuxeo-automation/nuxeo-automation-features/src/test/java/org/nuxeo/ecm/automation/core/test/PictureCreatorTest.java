package org.nuxeo.ecm.automation.core.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.services.CreatePicture;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.automation.features",
        "org.nuxeo.ecm.automation.server",
        "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.convert" })
public class PictureCreatorTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    BatchManager batchManager;

    @Inject
    protected ImagingService imagingService;

    @Test
    public void testCreate() throws Exception {

        Blob source = new FileBlob(FileUtils.getResourceFileFromContext("test-data/sample.jpeg"));
        String fileName="MyTest.jpg";
        String mimeType="image/jpeg";

        batchManager.addStream("BID", "1", source.getStream(), fileName, mimeType);


        StringBuilder fakeJSON = new StringBuilder("{ ");
        fakeJSON.append(" \"type\" : \"blob\"");
        fakeJSON.append(", \"length\" : " + source.getLength());
        fakeJSON.append(", \"mime-type\" : \"" + mimeType +"\"");
        fakeJSON.append(", \"name\" : \"" + fileName + "\"");

        fakeJSON.append(", \"upload-batch\" : " + "\"BID\"");
        fakeJSON.append(", \"upload-fileId\" : \"1\" ");
        fakeJSON.append("}");

        // System.out.println("***************************************");
        // System.out.println(fakeJSON.toString());

        DocumentModel root = session.getRootDocument();

        OperationContext ctx = new OperationContext(session);

        ctx.setInput(root);

        Properties properties = new Properties();
        properties.put("dc:title", "MySuperPicture");
        properties.put(CreatePicture.PICTURE_FIELD, fakeJSON.toString());


        Properties templates = new Properties();

        templates.put("Original" , "{\"title\" : \"Original\"}");
        for (int i = 1; i<5; i++) {
            StringBuffer sb = new StringBuffer("{");
            sb.append("\"description\": \"Desc " + i +"\",");
            sb.append("\"title\": \"Title" + i +"\",");
            sb.append("\"maxsize\":"  + i*100 );
            sb.append("}");
            templates.put("thumb" + i, sb.toString());
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("properties",properties);
        params.put("pictureTemplates",templates);

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(CreatePicture.ID, params);
        chain.add(oparams);

        DocumentModel picture  = (DocumentModel) service.run(ctx, chain);

        assertNotNull(picture);

        MultiviewPicture mvp = picture.getAdapter(MultiviewPicture.class);

        assertNotNull(mvp);

        assertEquals(5, mvp.getViews().length);

        for (int i = 1; i<5; i++) {
            String title = "Title" + i;
            PictureView pv = mvp.getView(title);
            Blob content = (Blob) pv.getContent();
            ImageInfo ii = imagingService.getImageInfo(content);
            assertEquals(i*100, ii.getWidth());
        }
    }

}
