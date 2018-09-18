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
 *     pierre
 */
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.COMPLETED;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.actions.CSVExportAction;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreBulkFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class)
public class TestCSVExportAction {

    @Inject
    public CoreSession session;

    @Inject
    public BulkService bulkService;

    @Test
    public void test() throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from Document where ecm:parentId='%s'", model.getId());

        String commandId = bulkService.submit(new BulkCommand().withRepository(session.getRepositoryName())
                                                               .withUsername(session.getPrincipal().getName())
                                                               .withQuery(nxql)
                                                               .withAction(CSVExportAction.ACTION_NAME));

        assertTrue("Bulk action didn't finish", bulkService.await(commandId, Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        assertEquals(10, status.getProcessed());

        TransientStore download = Framework.getService(TransientStoreService.class)
                                           .getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);

        List<Blob> blobs = download.getBlobs(commandId);
        Blob blob = blobs == null || blobs.isEmpty() ? null : blobs.get(0);

        // file is ziped
        assertNotNull(blob);
        try (InputStream is = new FileInputStream(blob.getFile())) {
            assertTrue(ZipUtils.isValid(is));
        }

        // file has the correct number of lines
        Path dir = Files.createTempDirectory(CSVExportAction.ACTION_NAME + "test" + System.currentTimeMillis());
        ZipUtils.unzip(blob.getFile(), dir.toFile());
        File file = new File(dir.toFile(), commandId + ".csv");
        List<String> lines = Files.lines(file.toPath()).collect(Collectors.toList());
        assertEquals(10, lines.size());

        // file is sorted
        List<String> sortedLines = new ArrayList<>(lines);
        Collections.sort(sortedLines);
        assertEquals(lines, sortedLines);
    }
}
