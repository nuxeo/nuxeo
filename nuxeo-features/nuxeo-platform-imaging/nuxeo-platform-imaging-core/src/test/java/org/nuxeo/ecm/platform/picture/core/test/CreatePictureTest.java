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
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.picture.operation.CreatePicture;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.transientstore.test.TransientStoreFeature;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, TransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.automation.server")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.collections.core:OSGI-INF/collection-core-types-contrib.xml")
public class CreatePictureTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    BatchManager batchManager;

    @Test
    public void testCreate() throws Exception {

        Blob source = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"));
        String fileName = "MyTest.jpg";
        String mimeType = "image/jpeg";

        String batchId = batchManager.initBatch();
        batchManager.addBlob(batchId, "1", source, fileName, mimeType);

        StringBuilder fakeJSON = new StringBuilder("{ ");
        fakeJSON.append(" \"type\" : \"blob\"");
        fakeJSON.append(", \"length\" : " + source.getLength());
        fakeJSON.append(", \"mime-type\" : \"" + mimeType + "\"");
        fakeJSON.append(", \"name\" : \"" + fileName + "\"");

        fakeJSON.append(", \"upload-batch\" : \"" + batchId + "\"");
        fakeJSON.append(", \"upload-fileId\" : \"1\" ");
        fakeJSON.append("}");

        DocumentModel root = session.getRootDocument();

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(root);

        Properties properties = new Properties();
        properties.put("dc:title", "MySuperPicture");
        properties.put(CreatePicture.PICTURE_FIELD, fakeJSON.toString());

        Properties templates = new Properties();

        for (int i = 1; i < 5; i++) {
            StringBuffer sb = new StringBuffer("{");
            sb.append("\"description\": \"Desc " + i + "\",");
            sb.append("\"title\": \"Title" + i + "\",");
            sb.append("\"maxsize\":" + i * 100);
            sb.append("}");
            templates.put("thumb" + i, sb.toString());
        }

        Map<String, Object> params = new HashMap<String, Object>();
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
