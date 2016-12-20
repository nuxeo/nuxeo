/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins <tmartins@nuxeo.com>
 */

package org.nuxeo.ecm.platform.preview.tests.adapter;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.preview" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.platform.preview:doctype-contrib-test.xml" })
public class TestPreviewAdapter {

    @Inject
    CoreSession session;
    
    @Test
    public void testFileDocument() {
        DocumentModel document = session.createDocumentModel("File");
        HtmlPreviewAdapter adapter = document.getAdapter(HtmlPreviewAdapter.class);
        assertEquals(false, adapter.hasBlobToPreview());
        
        Blob blob = new StringBlob("test");
        Map<String, Serializable> file = new HashMap<>();
        // Attach one file to the list
        file.put("file", (Serializable) blob);
        document.setPropertyValue("file:content", (Serializable) blob);
        assertEquals(true, adapter.hasBlobToPreview());
    }
    
    @Test
    public void testCustomDocumentWithFilesSchema() {
        DocumentModel document = session.createDocumentModel("CustomDoc");
        
        // no preview adapter should be found in this case (empty files property)
        assertEquals(null, document.getAdapter(HtmlPreviewAdapter.class));
        
        Blob blob = new StringBlob("test");
        Map<String, Serializable> file = new HashMap<>();
        // Attach one file to the list
        file.put("file", (Serializable) blob);
        ArrayList<Map<String, Serializable>> files = new ArrayList<>();
        files.add(file);
        document.setPropertyValue("files:files", files);
        HtmlPreviewAdapter adapter = document.getAdapter(HtmlPreviewAdapter.class);
        assertEquals(true, adapter.hasBlobToPreview());
    }
    
}
