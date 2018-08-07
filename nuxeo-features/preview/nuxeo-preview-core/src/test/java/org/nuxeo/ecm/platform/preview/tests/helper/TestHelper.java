/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.preview.tests.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests url generation/resolution via the static helper
 *
 * @author tiry
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
public class TestHelper {

    @Inject
    protected CoreSession session;

    private static final String uuid = "f53fc32e-21b3-4640-9917-05e873aa1e53";

    private static final String targetURL1 = "site/api/v1/repo/default/id/f53fc32e-21b3-4640-9917-05e873aa1e53/@preview/";

    private static final String targetURL2 = "site/api/v1/repo/default/id/f53fc32e-21b3-4640-9917-05e873aa1e53/@blob/file:content/@preview/";

    private static final String targetURL3 = "site/api/v1/repo/test/id/%s/@blob/file:content/@preview/?changeToken=0-0";

    @Test
    public void testPreviewURLDefault() {
        DocumentModel doc = new DocumentModelImpl("", "File", uuid, new Path("/"), null, null, null, null, null, null,
                "default");

        String previewURL = PreviewHelper.getPreviewURL(doc);
        assertNotNull(previewURL);
        assertEquals(targetURL1, previewURL);
    }

    @Test
    public void testPreviewURL() {
        DocumentModel doc = new DocumentModelImpl("", "File", uuid, new Path("/"), null, null, null, null, null, null,
                "default");

        String previewURL = PreviewHelper.getPreviewURL(doc, "file:content");
        assertNotNull(previewURL);
        assertEquals(targetURL2, previewURL);
    }

    /**
     * @since 10.3
     */
    @Test
    public void testPreviewURLWithChangeToken() {
        DocumentModel doc = session.createDocumentModel("/", "test", "File");
        doc = session.createDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());
        String previewURL = PreviewHelper.getPreviewURL(doc, "file:content");
        assertNotNull(previewURL);
        assertEquals(String.format(targetURL3, doc.getId()), previewURL);
    }

}
