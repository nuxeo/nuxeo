/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.DocumentIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentIndexableResourceImpl;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourcesFactory;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.transaction.Transactions;
import org.nuxeo.runtime.api.Framework;

/**
 * Integration tests for the search service with the default backend
 * configuration (i.e. Compass based).
 *
 * @author <a href="ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
public class TestSearchServiceIntegration extends RepositoryOSGITestCase {

    // reproducible pseudo-random generator
    private final Random random = new Random(0);

    SearchService sservice;

    @Override
    public void setUp() throws Exception {
        // deploy the default core configurations with schemas and types
        super.setUp();

        // transform plugins are needed for fulltext indexing
        deployBundle("nuxeo-platform-transform-api");
        deployBundle("nuxeo-platform-transform-core");
        deployBundle("nuxeo-platform-transform-plugins");

        // deploy the default search service
        deployBundle("nuxeo-platform-search-api");
        deployBundle("nuxeo-platform-search-core");
        deployBundle("nuxeo-platform-search-compass-plugin");

        // override the default compass configuration to instead use
        // a non-JXTA memory backend
        deployContrib("nxsearch-compass-test-integration-contrib.xml");

        deployBundle("nuxeo-platform-search-core-listener");

        openRepository();
        sservice = Framework.getService(SearchService.class);
        // set test mode for transactions management within search backend
        Transactions.setTest(true);
    }

    @Override
    protected void tearDown() throws Exception {
        // set test mode for transactions management within search backend
        Transactions.setTest(false);
        super.tearDown();
    }

    private SearchPageProvider query(String query) throws Exception {
        SQLQuery nxqlQuery = SQLQueryParser.parse(query);
        ResultSet rs = sservice.searchQuery(new ComposedNXQueryImpl(nxqlQuery),
                0, 100);
        return new SearchPageProvider(rs);
    }

    private static Blob getSampleTextBlob() {
        return getSampleTextBlob("some default content");
    }

    private static Blob getSampleTextBlob(String content) {
        return StreamingBlob.createFromString(content, "text/plain");
    }

    private static List<Map<String, Object>> getSampleAttachedFileMaps() {
        int length = 3;
        List<Map<String, Object>> files = new ArrayList<Map<String, Object>>(
                length);

        for (int i = 0; i < length; i++) {
            Map<String, Object> attachedFile = new HashMap<String, Object>();
            Integer fileId = Integer.valueOf(i);
            attachedFile.put("filename", String.format("sample_file_%d.txt",
                    fileId));
            attachedFile.put("file", getSampleTextBlob(String.format(
                    "some content%d", fileId)));
            files.add(attachedFile);
        }

        return files;
    }

    private DocumentModel createSampleFile() throws Exception {
        // Create a document model.
        DocumentModel root = coreSession.getRootDocument();
        DocumentModel file = coreSession.createDocumentModel(
                root.getPathAsString(), String.valueOf(random.nextLong()),
                "File");

        // fill in some default values
        file.setProperty("dublincore", "title", "Title of the file");
        file.setProperty("file", "filename", "sample-text-file.txt");
        file.setProperty("file", "content", getSampleTextBlob());
        file.setProperty("files", "files", getSampleAttachedFileMaps());

        // save it in the repository (and synchronous indexing of the pre-fetch)
        file = coreSession.createDocument(file);
        coreSession.save();

        // simulate asynchronous fulltext indexing
        // sservice.indexInThread(file, false, true);
        sservice.index(IndexableResourcesFactory.computeResourcesFor(file,
                file.getSessionId()), true);

        return file;
    }

    public void testFulltextIndexing() throws Exception {
        DocumentRef docRef = createSampleFile().getRef();

        // search for a word occurring in the title field
        SearchPageProvider spp = query("SELECT * FROM Document WHERE ecm:fulltext = 'title'");
        assertEquals(1, spp.getResultsCount());
        assertEquals(docRef, spp.getCurrentPage().get(0).getRef());

        // search for a word occurring in the main blob content
        spp = query("SELECT * FROM Document WHERE ecm:fulltext = 'some default content'");
        assertEquals(1, spp.getResultsCount());
        assertEquals(docRef, spp.getCurrentPage().get(0).getRef());

        // search for a word occurring in the dynamic blob list (XXX borked)
        // spp = query("SELECT * FROM Document WHERE ecm:fulltext = 'some
        // content1'");
        // assertEquals(1, spp.getResultsCount());
        // assertEquals(docRef, spp.getCurrentPage().get(0).getRef());
    }

    @SuppressWarnings("unchecked")
    public void testBlobReconstruction() throws Exception {
        DocumentRef docRef = createSampleFile().getRef();

        // search for a word occurring in the title field
        SearchPageProvider spp = query("SELECT * FROM Document WHERE ecm:fulltext = 'title'");
        assertEquals(1, spp.getResultsCount());

        DocumentModel result = spp.getCurrentPage().get(0);
        assertEquals(docRef, result.getRef());

        Object fileContent = result.getProperty("file", "content");

        // the search service has rebuilt a Blob instance out of the stored
        // indexed data
        assertNotNull(fileContent);
        assertTrue(fileContent instanceof Blob);
        Blob blob = (Blob) fileContent;
        assertEquals("text/plain", blob.getMimeType());

        // however by default the blob content is empty since it's data is not
        // stored by the search service
        assertEquals("", blob.getString());

        Object filesContent = result.getProperty("files", "files");
        assertNotNull(filesContent);
        assertTrue(filesContent instanceof List);
        List<Map<String, String>> fileMaps = (List<Map<String, String>>) filesContent;
        assertEquals(3, fileMaps.size());

        for (int i = 0; i < fileMaps.size(); i++) {
            Map<String, String> fileMap = fileMaps.get(i);

            // first check the filename subfield
            assertEquals(String.format("sample_file_%d.txt", i),
                    fileMap.get("filename"));

            // XXX: list of blobs are not (yet?) rebuilt since they are third
            // level nested complex types (list of map of map)
            // fileContent = fileMap.get("file");
            // assertNotNull(fileContent);
            // assertTrue(fileContent instanceof Blob);
            // blob = (Blob) fileContent;
            // assertEquals("text/plain", blob.getMimeType());
            // // here again the blob content should be empty
            // assertEquals("", blob.getString());
        }
    }

    public void testComplexFieldsIndexableRessource() throws Exception {
        DocumentModel file = createSampleFile();

        IndexableResourceConf conf = sservice.getIndexableResourceConfByName(
                "files", true);

        DocumentIndexableResource rs = new DocumentIndexableResourceImpl(file,
                conf, file.getSessionId());

        Serializable indexedValue = rs.getValueFor("files:files:filename");
        List<String> expectedValue = Arrays.asList("sample_file_0.txt",
                "sample_file_1.txt", "sample_file_2.txt");
        assertEquals(expectedValue, indexedValue);

        // XXX: complex types nested to the third level are not (yet?) indexed
        // indexedValue = rs.getValueFor("files:files:file:mime-type");
        // expectedValue = Arrays.asList("test/plain",
        // "test/plain", "test/plain");
        // assertEquals(expectedValue, indexedValue);
    }

}
