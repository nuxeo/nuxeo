/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.picture.core.ImagingFeature;
import org.nuxeo.ecm.platform.picture.operation.CreatePicture;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(ImagingFeature.class)
@Deploy("org.nuxeo.ecm.automation.server")
@Deploy("org.nuxeo.ecm.platform.query.api")
public class CreatePictureTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    BatchManager batchManager;

    protected OperationContext ctx;

    @Before
    public void createOperationContext() {
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    public void testCreate() throws Exception {

        Blob source = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"));
        String fileName = "MyTest.jpg";
        String mimeType = "image/jpeg";

        String batchId = batchManager.initBatch();
        batchManager.addBlob(batchId, "1", source, fileName, mimeType);

        StringBuilder fakeJSON = new StringBuilder("{ ");
        fakeJSON.append(" \"type\" : \"blob\"")
                .append(", \"length\" : ")
                .append(source.getLength())
                .append(", \"mime-type\" : \"")
                .append(mimeType)
                .append("\"")
                .append(", \"name\" : \"")
                .append(fileName)
                .append("\"")
                .append(", \"upload-batch\" : \"")
                .append(batchId)
                .append("\"")
                .append(", \"upload-fileId\" : \"1\" ")
                .append("}");

        DocumentModel root = session.getRootDocument();

        ctx.setInput(root);

        Properties properties = new Properties();
        properties.put("dc:title", "MySuperPicture");
        properties.put(CreatePicture.PICTURE_FIELD, fakeJSON.toString());

        Properties templates = new Properties();

        for (int i = 1; i < 5; i++) {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"description\": \"Desc ")
              .append(i)
              .append("\",")
              .append("\"title\": \"Title")
              .append(i)
              .append("\",")
              .append("\"maxsize\":")
              .append(i * 100)
              .append("}");
            templates.put("thumb" + i, sb.toString());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("properties", properties);
        params.put("pictureTemplates", templates);

        DocumentModel picture = (DocumentModel) service.run(ctx, CreatePicture.ID, params);
        assertNotNull(picture);

        MultiviewPicture mvp = picture.getAdapter(MultiviewPicture.class);
        assertNotNull(mvp);

        assertEquals(4, mvp.getViews().length);

        for (int i = 1; i < 4; i++) {
            String title = "Title" + i;

            PictureView pv = mvp.getView(title);
            assertNotNull(pv);

            Blob content = pv.getBlob();
            // Just test if we have a blob
            assertNotNull(content);

            // TODO: Check size ??
        }
    }

}
