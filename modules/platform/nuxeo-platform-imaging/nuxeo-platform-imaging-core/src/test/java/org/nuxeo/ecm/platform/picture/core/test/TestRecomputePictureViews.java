/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.operation.RecomputePictureViews;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the {@link RecomputePictureViews} operation.
 *
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
public class TestRecomputePictureViews {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected AutomationService automationService;

    @Test
    @SuppressWarnings("unchecked")
    public void testRecomputePictureViews() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "pictureDoc", "Picture");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"), "image/jpeg",
                StandardCharsets.UTF_8.name(), "test.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // wait for picture views generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        List<Serializable> pictureViews = (List<Serializable>) doc.getPropertyValue("picture:views");
        assertNotNull(pictureViews);
        assertFalse(pictureViews.isEmpty());

        // empty picture views
        doc.setPropertyValue("picture:views", new ArrayList<>());
        session.saveDocument(doc);
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        pictureViews = (List<Serializable>) doc.getPropertyValue("picture:views");
        assertNotNull(pictureViews);
        assertTrue(pictureViews.isEmpty());

        // call operation to recompute the picture views
        Map<String, String> parameters = Collections.singletonMap("query",
                "SELECT * FROM Document WHERE ecm:mixinType = 'Picture'");
        try (OperationContext ctx = new OperationContext(session)) {
            automationService.run(ctx, RecomputePictureViews.ID, parameters);
        }

        // wait for picture views generation
        txFeature.nextTransaction();
        doc = session.getDocument(doc.getRef());
        pictureViews = (List<Serializable>) doc.getPropertyValue("picture:views");
        assertNotNull(pictureViews);
        assertFalse(pictureViews.isEmpty());
    }

}
