/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.core.test.CollectionFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, CollectionFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.rendition.api")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
public class BulkDownloadTest extends BaseTest {

    // NXP-30401
    @Test
    public void testAuditEventOnSelectionDownload() {
        String body = "{\"input\":\"docs:/folder_1/note_1,/folder_1/note_3\",\"params\":{\"filename\":\"selection.zip\"}}";
        try (CapturingEventListener listener = new CapturingEventListener(DownloadService.EVENT_NAME);
                CloseableClientResponse response = getResponse(RequestType.POST, "automation/Blob.BulkDownload",
                        body)) {
            assertEquals(200, response.getStatus());

            List<String> downloadedDocumentTitles = listener.streamCapturedEventContexts(DocumentEventContext.class)
                                                            .map(DocumentEventContext::getSourceDocument)
                                                            .map(DocumentModel::getTitle)
                                                            .sorted()
                                                            .collect(toList());
            List<String> expectedDocumentTitles = Arrays.asList("Note 1", "Note 3");
            // no event for selection.zip because BlobWriter doesn't call the download service in tests
            assertEquals(expectedDocumentTitles, downloadedDocumentTitles);
        }
    }

    // NXP-30401
    // Blob.BulkDownload of a container (folder/collection) will trigger a zip rendition which triggers a call to
    // Document.GetContainerRendition (check this operation to see the call to DownloadService)
    @Test
    public void testAuditEventOnFolderDownload() {
        String body = "{\"input\":\"docs:/folder_1\",\"params\":{\"filename\":\"Folder1.zip\"}}";
        try (CapturingEventListener listener = new CapturingEventListener(DownloadService.EVENT_NAME);
                CloseableClientResponse response = getResponse(RequestType.POST, "automation/Blob.BulkDownload",
                        body)) {
            assertEquals(200, response.getStatus());

            List<String> downloadedDocumentTitles = listener.streamCapturedEventContexts(DocumentEventContext.class)
                                                            .map(DocumentEventContext::getSourceDocument)
                                                            .map(DocumentModel::getTitle)
                                                            .sorted()
                                                            .collect(toList());
            List<String> expectedDocumentTitles = Arrays.asList("Folder 1", "Note 0", "Note 1", "Note 2", "Note 3",
                    "Note 4");
            // no event for Folder1.zip because BlobWriter doesn't call the download service in tests
            assertEquals(expectedDocumentTitles, downloadedDocumentTitles);
        }
    }
}
