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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.CHANGE_TOKEN_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_CHECKED_OUT_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_PROXY_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_TRASHED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_VERSION_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LAST_MODIFIED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LOCK_CREATED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LOCK_OWNER_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PARENT_REF_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PATH_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PROXY_TARGET_ID_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.REPOSITORY_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.STATE_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.TITLE_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.TYPE_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.UID_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.VERSIONABLE_ID_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.VERSION_LABEL_FIELD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.action.CSVExportAction;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

@RunWith(FeaturesRunner.class)
@Features({ CoreBulkFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class)
public class TestCSVExportAction {

    @Inject
    public CoreSession session;

    @Inject
    public BulkService bulkService;

    @Inject
    public DownloadService downloadService;

    protected static abstract class DummyServletOutputStream extends ServletOutputStream {
        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }
    }

    @Test
    public void testSimple() throws Exception {
        BulkCommand command = createCommand();
        bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(command.getId(), Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(10, status.getProcessed());
        assertEquals(10, status.getTotal());

        String url = Framework.getService(DownloadService.class).getDownloadUrl(command.getId());
        assertEquals(url, status.getResult().get("url"));

        Blob blob = getBlob(command.getId());
        // file is ziped
        assertNotNull(blob);
        try (InputStream is = new FileInputStream(blob.getFile())) {
            assertTrue(ZipUtils.isValid(is));
        }

        // file has the correct number of lines
        File file = getUnzipFile(command, blob);

        List<String> lines = Files.lines(file.toPath()).collect(Collectors.toList());
        assertEquals(11, lines.size());

        // Check header
        assertArrayEquals(new String[] { REPOSITORY_FIELD, UID_FIELD, PATH_FIELD, TYPE_FIELD, STATE_FIELD,
                PARENT_REF_FIELD, IS_CHECKED_OUT_FIELD, IS_VERSION_FIELD, IS_PROXY_FIELD, PROXY_TARGET_ID_FIELD,
                VERSIONABLE_ID_FIELD, CHANGE_TOKEN_FIELD, IS_TRASHED_FIELD, TITLE_FIELD, VERSION_LABEL_FIELD,
                LOCK_OWNER_FIELD, LOCK_CREATED_FIELD, LAST_MODIFIED_FIELD }, lines.get(0).split(","));

        // file is sorted
        List<String> sortedLines = new ArrayList<>(lines);
        Collections.sort(sortedLines);
        assertEquals(lines, sortedLines);
    }

    @Test
    public void testExportWithParams() throws Exception {
        BulkCommand command = createCommandWithParams();
        bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(command.getId(), Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(10, status.getProcessed());
        assertEquals(10, status.getTotal());

        Blob blob = getBlob(command.getId());
        // file is ziped
        assertNotNull(blob);
        try (InputStream is = new FileInputStream(blob.getFile())) {
            assertTrue(ZipUtils.isValid(is));
        }

        // file has the correct number of lines
        File file = getUnzipFile(command, blob);

        List<String> lines = Files.lines(file.toPath()).collect(Collectors.toList());
        // Check header
        List<String> header = Arrays.asList(lines.get(0).split(","));
        assertArrayEquals(
                new String[] { "dc:contributors", "dc:coverage", "dc:created", "dc:creator", "dc:description",
                        "dc:expired", "dc:format", "dc:issued", "dc:language", "dc:lastContributor", "dc:modified",
                        "dc:nature", "dc:publisher", "dc:rights", "dc:source", "dc:subjects", "dc:title", "dc:valid",
                        "cpx:complex/foo" },
                header.subList(18, 37).toArray());

    }

    protected File getUnzipFile(BulkCommand command, Blob blob) throws IOException {
        Path dir = Files.createTempDirectory(CSVExportAction.ACTION_NAME + "test" + System.currentTimeMillis());
        ZipUtils.unzip(blob.getFile(), dir.toFile());
        return new File(dir.toFile(), command.getId() + ".csv");
    }

    @Test
    public void testMulti() throws Exception {
        BulkCommand command1 = createCommand();
        BulkCommand command2 = createCommand();
        bulkService.submit(command1);
        bulkService.submit(command2);

        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command1.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(10, status.getProcessed());

        status = bulkService.getStatus(command2.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(10, status.getProcessed());

        Blob blob1 = getBlob(command1.getId());
        Blob blob2 = getBlob(command2.getId());

        // this produce the exact same content
        HashCode hash1 = hash(getUnzipFile(command1, blob1));
        HashCode hash2 = hash(getUnzipFile(command2, blob2));
        assertEquals(hash1,  hash2);
    }

    @Test
    public void testDownloadCSV() throws Exception {

        BulkCommand command = createCommand();
        bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(command.getId(), Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(10, status.getProcessed());
        assertEquals(10, status.getTotal());

        Path dir = Files.createTempDirectory(CSVExportAction.ACTION_NAME + "test" + System.currentTimeMillis());
        File testZip = new File(dir.toFile(), "test.zip");
        FileOutputStream out = new FileOutputStream(testZip);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);

        String url = (String) status.getResult().get("url");
        downloadService.handleDownload(request, response, null, url);

        ZipUtils.unzip(testZip, dir.toFile());
        File csv = new File(dir.toFile(), command.getId() + ".csv");
        List<String> lines = Files.lines(csv.toPath()).collect(Collectors.toList());
        assertEquals(11, lines.size());

    }

    private HashCode hash(File file) throws IOException {
        return com.google.common.io.Files
                .asByteSource(file).hash(Hashing.sha256());
    }

    protected BulkCommand createCommand() {
        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from Document where ecm:parentId='%s'", model.getId());
        return new BulkCommand.Builder(CSVExportAction.ACTION_NAME, nxql).repository(session.getRepositoryName())
                                                                         .user(session.getPrincipal().getName())
                                                                         .build();
    }

    protected BulkCommand createCommandWithParams() {
        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from Document where ecm:parentId='%s'", model.getId());
        return new BulkCommand.Builder(CSVExportAction.ACTION_NAME, nxql).repository(session.getRepositoryName())
                                                                         .user(session.getPrincipal().getName())
                                                                         .param("schemas",
                                                                                 ImmutableList.of("dublincore"))
                                                                         .param("xpaths",
                                                                                 ImmutableList.of("cpx:complex/foo"))
                                                                         .build();
    }

    protected Blob getBlob(String commandId) {
        // the convention is that the blob is created in the download storage with the command id
        TransientStore download = Framework.getService(TransientStoreService.class)
                                           .getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);

        List<Blob> blobs = download.getBlobs(commandId);
        assertNotNull(blobs);
        assertEquals(1, blobs.size());
        return blobs.get(0);
    }
}
