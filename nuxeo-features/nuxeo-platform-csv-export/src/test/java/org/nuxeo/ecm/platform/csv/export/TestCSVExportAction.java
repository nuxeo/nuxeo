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
package org.nuxeo.ecm.platform.csv.export;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.bulk.action.computation.SortBlob.SORT_PARAMETER;
import static org.nuxeo.ecm.core.bulk.action.computation.ZipBlob.ZIP_PARAMETER;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.test.DocumentSetRepositoryInit.CREATED_TOTAL;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_XPATHS;
import static org.nuxeo.ecm.platform.csv.export.io.DocumentModelCSVHelper.SYSTEM_PROPERTIES_HEADER_FIELDS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkCommand.Builder;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DocumentSetRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.platform.csv.export.action.CSVExportAction;
import org.nuxeo.elasticsearch.test.RepositoryLightElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

@RunWith(FeaturesRunner.class)
@Features({ CoreBulkFeature.class, CoreFeature.class, DirectoryFeature.class,
        RepositoryLightElasticSearchFeature.class })
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.platform.csv.export")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class)
public class TestCSVExportAction {

    @Inject
    public CoreSession session;

    @Inject
    public BulkService bulkService;

    @Inject
    public DownloadService downloadService;

    @Inject
    protected TransactionalFeature txFeature;

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
        testCsvExport(createBuilder().build());
    }

    @Test
    public void testSimpleWithMultipleBuckets() throws Exception {
        BulkCommand command = createBuilder().build();
        command.setBucketSize(1);
        command.setBatchSize(1);
        testCsvExport(command);
    }

    @Test
    public void testSimpleWithFileUnsorted() throws Exception {
        testCsvExport(createBuilder(false, false).build());
    }

    @Test
    public void testSimpleWithFileZipped() throws Exception {
        testCsvExport(createBuilder(false, true).build());
    }

    protected void testCsvExport(BulkCommand command) throws Exception {
        bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(command.getId(), Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(CREATED_TOTAL, status.getProcessed());
        assertEquals(CREATED_TOTAL, status.getTotal());

        String url = Framework.getService(DownloadService.class).getDownloadUrl(command.getId());
        assertEquals(url, status.getResult().get("url"));

        Blob blob = getBlob(command.getId());
        // file is ziped
        boolean zipped = command.getParam(ZIP_PARAMETER);
        String extension = zipped ? "zip" : "csv";
        assertEquals(extension, FilenameUtils.getExtension(blob.getFilename()));
        File file;
        if (zipped) {
            try (InputStream is = new FileInputStream(blob.getFile())) {
                assertTrue(ZipUtils.isValid(is));
            }
            file = getUnzipFile(command, blob);
        } else {
            file = blob.getFile();
        }

        // file has the correct number of lines
        List<String> lines = Files.lines(file.toPath()).collect(Collectors.toList());
        // number of docs plus the header
        assertEquals(CREATED_TOTAL + 1, lines.size());

        // Check header
        assertArrayEquals(SYSTEM_PROPERTIES_HEADER_FIELDS, lines.get(0).split(","));
        long count = lines.stream().filter(Predicate.isEqual(lines.get(0))).count();
        assertEquals(1, count);

        // file is sorted
        boolean sorted = command.getParam(SORT_PARAMETER);
        if (sorted) {
            List<String> content = lines.subList(1, lines.size());
            List<String> sortedContent = new ArrayList<>(content);
            Collections.sort(sortedContent);
            assertEquals(content, sortedContent);
        }
    }

    @Test
    public void testSimpleWithMultiLine() throws Exception {
        session.getChildren(new PathRef("/default-domain/workspaces/test")).forEach(doc -> {
            doc.setPropertyValue("dc:description", "Some multiline content\nLast line");
            session.saveDocument(doc);
        });
        txFeature.nextTransaction();

        BulkCommand command = createBuilder(true, false).param( //
                PARAM_XPATHS, new ArrayList<>(Collections.singleton("dc:description"))).build();
        bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(command.getId(), Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(CREATED_TOTAL, status.getProcessed());
        assertEquals(CREATED_TOTAL, status.getTotal());

        Blob blob = getBlob(command.getId());
        assertTrue(blob.getString().contains("Some multiline content Last line"));
    }

    @Test
    public void testExportWithParams() throws Exception {
        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        List<DocumentModel> children = session.getChildren(model.getRef());
        List<String> testIds = new ArrayList<>();
        boolean addBlob = true;
        for (int i = 0; i < children.size(); i++) {
            DocumentModel child = children.get(i);
            child.setPropertyValue("dc:nature", "article");
            if (i % 2 == 0) {
                child.setPropertyValue("dc:subjects", new String[] { "art/architecture" });
                testIds.add(child.getId());
            }
            if (child.getType().equals("File") && addBlob) {
                child.setPropertyValue("dc:title", "FileWithContent" + i);
                Blob blob = Blobs.createBlob("the blob content");
                blob.setFilename("initial_name.txt");
                child.setPropertyValue("file:content", (Serializable) blob);
                addBlob = false;
            }

            session.saveDocument(child);
        }
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        ImmutableList<String> xpaths = ImmutableList.of("cpx:complex/foo", "file:content/name", "file:content/length");
        BulkCommand command = createBuilder().param("schemas", ImmutableList.of("dublincore"))
                                             .param("xpaths", xpaths)
                                             .param("lang", "fr")
                                             .build();
        bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(command.getId(), Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(CREATED_TOTAL, status.getProcessed());
        assertEquals(CREATED_TOTAL, status.getTotal());

        Blob blob = getBlob(command.getId());
        File file = blob.getFile();

        List<String> lines = Files.lines(file.toPath()).collect(Collectors.toList());
        // Check header
        List<String> header = Arrays.asList(lines.get(0).split(","));
        // Check that the given schemas and properties are present after the system properties

        int systemHeaderSize = SYSTEM_PROPERTIES_HEADER_FIELDS.length;
        String[] dcFields = new String[] { "dc:contributors", "dc:coverage", "dc:coverage[label]", "dc:created",
                "dc:creator", "dc:description", "dc:expired", "dc:format", "dc:issued", "dc:language",
                "dc:lastContributor", "dc:modified", "dc:nature", "dc:nature[label]", "dc:publisher", "dc:rights",
                "dc:source", "dc:subjects", "dc:subjects[label]", "dc:title", "dc:valid", "cpx:complex/foo",
                "file:content/length", "file:content/name" };

        int headerSize = systemHeaderSize + dcFields.length;
        assertArrayEquals(dcFields, header.subList(systemHeaderSize, headerSize).toArray());

        List<String> content = lines.subList(1, lines.size());
        for (String doc : content) {
            // There should be headerSize - 1 number of commas for headerSize number of properties
            assertEquals(headerSize - 1, StringUtils.countMatches(doc, ","));
            List<String> properties = Arrays.asList(doc.split(","));
            if (properties.contains("ComplexDoc")) {
                assertTrue(properties.contains("Article FR"));
            }
            if (testIds.contains(properties.get(1))) {
                assertTrue(properties.contains("art/architecture"));
            }
            // if the document is a File with content it must contain its content filename and length
            if (properties.contains("FileWithContent")) {
                assertTrue(properties.contains("initial_name.txt"));
                assertTrue(properties.contains("16"));
            }
        }

    }

    protected File getUnzipFile(BulkCommand command, Blob blob) throws IOException {
        Path dir = Files.createTempDirectory(CSVExportAction.ACTION_NAME + "test" + System.currentTimeMillis());
        ZipUtils.unzip(blob.getFile(), dir.toFile());
        return new File(dir.toFile(), command.getId() + ".csv");
    }

    @Test
    public void testMulti() throws Exception {
        BulkCommand command1 = createBuilder().build();
        BulkCommand command2 = createBuilder().build();
        bulkService.submit(command1);
        bulkService.submit(command2);

        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command1.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(CREATED_TOTAL, status.getProcessed());

        status = bulkService.getStatus(command2.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(CREATED_TOTAL, status.getProcessed());

        Blob blob1 = getBlob(command1.getId());
        Blob blob2 = getBlob(command2.getId());

        // this produce the exact same content
        HashCode hash1 = hash(blob1.getFile());
        HashCode hash2 = hash(blob2.getFile());
        assertEquals(hash1, hash2);
    }

    @Test
    public void testDownloadCSV() throws Exception {
        testDownloadCSV("default");
    }

    @Test
    public void testDownloadCSVWithElasticScroller() throws Exception {
        testDownloadCSV("elastic");
    }

    @Test
    public void testWithVeryLongLine() throws Exception {
        DocumentModel bigMeta = session.createDocumentModel(DocumentSetRepositoryInit.ROOT, "big-meta", "File");
        int size = 2_000_000;
        bigMeta.setPropertyValue("dc:title", new String(new char[size]).replace('\0', 'X'));
        session.createDocument(bigMeta);
        txFeature.nextTransaction();

        BulkCommand command = createBuilder().build();
        bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(command.getId(), Duration.ofSeconds(60)));

        BulkStatus status = bulkService.getStatus(command.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(CREATED_TOTAL + 1, status.getProcessed());
        assertEquals(CREATED_TOTAL + 1, status.getTotal());
    }

    protected void testDownloadCSV(String scroller) throws Exception {
        BulkCommand command = createBuilder().param("schemas", ImmutableList.of("dublincore"))
                                             .bucket(100)
                                             .batch(40)
                                             .scroller(scroller)
                                             .build();
        bulkService.submit(command);
        assertTrue("Bulk action didn't finish", bulkService.await(command.getId(), Duration.ofSeconds(60)));
        BulkStatus status = bulkService.getStatus(command.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(CREATED_TOTAL, status.getProcessed());
        assertEquals(CREATED_TOTAL, status.getTotal());

        Path dir = Files.createTempDirectory(CSVExportAction.ACTION_NAME + "test" + System.currentTimeMillis());
        File testCsv = new File(dir.toFile(), "test.csv");
        try (FileOutputStream out = new FileOutputStream(testCsv)) {
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
        }

        List<String> lines = Files.lines(testCsv.toPath()).collect(Collectors.toList());
        long headerCount = lines.stream().filter(line -> line.startsWith("repository,uid")).count();
        assertEquals(1, headerCount);
        // number of docs plus header
        assertEquals(CREATED_TOTAL + headerCount, lines.size());

    }

    protected HashCode hash(File file) throws IOException {
        return com.google.common.io.Files.asByteSource(file).hash(Hashing.sha256());
    }

    protected Builder createBuilder(boolean sorted, boolean zipped) {
        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * FROM Document WHERE ecm:ancestorId='%s' AND ecm:isVersion=0",
                model.getId());
        return new BulkCommand.Builder(CSVExportAction.ACTION_NAME, nxql).repository(session.getRepositoryName())
                                                                         .user(session.getPrincipal().getName())
                                                                         .param(SORT_PARAMETER, sorted)
                                                                         .param(ZIP_PARAMETER, zipped);
    }

    protected Builder createBuilder() {
        return createBuilder(true, false);
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
