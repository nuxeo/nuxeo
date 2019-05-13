/*
 * (C) Copyright 2012-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.content.adapter.base.ContentDiffConversionType;
import org.nuxeo.ecm.diff.web.DiffActionsBean;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.ui:OSGI-INF/urlservice-framework.xml")
@Deploy("org.nuxeo.ecm.platform.ui:OSGI-INF/urlservice-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.url.core")
@Deploy("org.nuxeo.diff.content:OSGI-INF/content-diff-adapter-framework.xml")
@Deploy("org.nuxeo.diff.content:OSGI-INF/content-diff-adapter-contrib.xml")
public class TestContentDiffFancyBoxURL {

    @Inject
    protected CoreSession session;

    /**
     * Tests {@link DiffActionsBean#getContentDiffFancyBoxURL(DocumentModel, String, String, String)} .
     */
    @Test
    public void testGetContentDiffFancyBoxURL() {
        DocumentModel doc = createDoc(session, "testDoc", "File", "Test doc");
        String fancyBoxURL = DiffActionsBean.getContentDiffFancyBoxURL(doc, "my.property.label", "file:content",
                ContentDiffConversionType.html.name());
        StringBuilder sb = new StringBuilder("/nuxeo/nxdoc/test/");
        sb.append(doc.getId());
        sb.append("/content_diff_fancybox?label=my.property.label&xPath=file%3Acontent&conversionType=html");
        assertEquals(sb.toString(), fancyBoxURL);
    }

    /**
     * Creates a document given the specified id, type and title.
     *
     * @param session the session
     * @param id the document id
     * @param type the document type
     * @param title the document title
     * @return the document model
     */
    protected DocumentModel createDoc(CoreSession session, String id, String type, String title) {
        DocumentModel doc = session.createDocumentModel("/", id, type);
        doc.setPropertyValue("dc:title", title);
        return session.createDocument(doc);
    }

}
