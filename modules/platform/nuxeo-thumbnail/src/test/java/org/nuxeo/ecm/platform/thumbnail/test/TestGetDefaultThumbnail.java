/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     gildas
 */
package org.nuxeo.ecm.platform.thumbnail.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Test class to verify the fetch of the default thumbnail for a document type.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.thumbnail")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.url.core")
@Deploy("org.nuxeo.ecm.platform.thumbnail:test-thumbnail-document-contrib.xml")
public class TestGetDefaultThumbnail {

    @Inject
    CoreSession session;

    @Test
    public void thumbnail_service_returns_default_thumbnail_outside_servlet_context() {
        // When I create a new File in the repository
        DocumentModel root = session.getRootDocument();
        DocumentModel newDoc = session.createDocumentModel(root.getPathAsString(), "MyDoc", "MyDocType");
        session.createDocument(newDoc);
        session.save();

        // Then, the thumbnail service returns the big icon as default thumbnail
        ThumbnailService service = Framework.getService(ThumbnailService.class);
        newDoc = session.getDocument(newDoc.getRef());
        Blob thumbnail = service.getThumbnail(newDoc, session);

        assertThat(thumbnail.getFilename()).isEqualTo("mydoc_100.png");
    }
}
