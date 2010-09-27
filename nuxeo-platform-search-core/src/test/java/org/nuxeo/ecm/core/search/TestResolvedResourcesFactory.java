/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id:TestSearchEnginePluginRegistration.java 13121 2007-03-01 18:07:58Z janguenot $
 */

package org.nuxeo.ecm.core.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.LazyBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.ResolvedResourcesFactory;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.DocumentIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourcesFactory;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

/**
 * Test factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestResolvedResourcesFactory extends SQLRepositoryTestCase {

    protected CoreSession remote;

    private SearchService service;

    private final Random random = new Random(new Date().getTime());

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxmimetype-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxtransform-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxtransform-platform-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxtransform-plugins-bundle.xml");

        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxsearch-test-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxsearch-test-contrib.xml");

        service = Framework.getLocalService(SearchService.class);
        assertNotNull(service);

        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        remote = mgr.getDefaultRepository().open();

        assertNotNull(remote);
    }

    @Override
    public void tearDown() throws Exception {
        ResolvedResourcesFactory.reInitSearchService();
        super.tearDown();
    }

    private String generateUnique() {
        return String.valueOf(random.nextLong());
    }

    private DocumentModel createChildDocument(DocumentModel childFolder)
            throws ClientException {

        DocumentModel ret = remote.createDocument(childFolder);

        assertNotNull(ret);
        assertNotNull(ret.getName());
        assertNotNull(ret.getId());
        assertNotNull(ret.getRef());
        assertNotNull(ret.getPathAsString());

        return ret;
    }

    private DocumentModel createSampleFile() throws Exception {

        // Create a document model.
        DocumentModel root = remote.getRootDocument();
        DocumentModel dm = new DocumentModelImpl(root.getPathAsString(),
                "file#" + generateUnique(), "File");
        dm = createChildDocument(dm);
        remote.save();

        assertEquals("project", remote.getCurrentLifeCycleState(dm.getRef()));
        assertEquals("File", dm.getType());

        dm.setProperty("dublincore", "title", "Indexable data");
        dm.setProperty("dublincore", "description", "Indexable description");
        dm.setProperty("file", "filename", "foo.pdf");
        String[] contributors = { "a", "b" };
        dm.setProperty("dublincore", "contributors", contributors);

        // add a blob
        StringBlob sb = new StringBlob("<doc>Indexing baby</doc>");
        byte[] bytes = sb.getByteArray();
        Blob blob = new ByteArrayBlob(bytes, "text/html", "ISO-8859-15");
        dm.setProperty("file", "content", blob);

        dm.setProperty("dublincore", "created", Calendar.getInstance());

        dm = remote.saveDocument(dm);
        remote.save();

        // remote.disconnect();
        return dm;
    }

    private DocumentModel createSampleFileTextPlain() throws Exception {

        // Create a document model.
        DocumentModel root = remote.getRootDocument();
        DocumentModel dm = new DocumentModelImpl(root.getPathAsString(),
                "txtfile#" + generateUnique(), "File");
        dm = createChildDocument(dm);
        remote.save();

        // Life c ycle disabled for now.
        //assertEquals("project", remote.getCurrentLifeCycleState(dm.getRef()));
        assertEquals("File", dm.getType());

        dm.setProperty("dublincore", "title", "Indexable data");
        dm.setProperty("dublincore", "description", "Indexable description");
        dm.setProperty("file", "filename", "foo.txt");
        String[] contributors = { "a", "b" };
        dm.setProperty("dublincore", "contributors", contributors);

        // add a blob
        StringBlob sb = new StringBlob("Indexing baby");
        byte[] bytes = sb.getByteArray();
        Blob blob = new ByteArrayBlob(bytes, "text/plain", "ISO-8859-15");
        dm.setProperty("file", "content", blob);

        dm.setProperty("dublincore", "created", Calendar.getInstance());

        dm = remote.saveDocument(dm);
        remote.save();

        // remote.disconnect();
        return dm;
    }

    private DocumentModel createSampleFileWithPdfBlob() throws Exception {

        // Create a document model.
        DocumentModel root = remote.getRootDocument();
        DocumentModel dm = new DocumentModelImpl(root.getPathAsString(),
                "file#" + generateUnique(), "File");
        dm = createChildDocument(dm);
        remote.save();

        assertEquals("project", remote.getCurrentLifeCycleState(dm.getRef()));
        assertEquals("File", dm.getType());

        dm.setProperty("dublincore", "title", "Indexable data");
        dm.setProperty("dublincore", "description", "Indexable description");
        dm.setProperty("file", "filename", "foo.pdf");
        String[] contributors = { "a", "b" };
        dm.setProperty("dublincore", "contributors", contributors);

        // add a blob
        File pdfFile = FileUtils.getResourceFileFromContext("test-data/hello.pdf");
        InputStream stream = new FileInputStream(pdfFile);
        Blob blob = new LazyBlob(stream, "UTF-8", "application/pdf",
                remote.getSessionId(), null, dm.getRepositoryName());
        dm.setProperty("file", "content", blob);

        dm.setProperty("dublincore", "created", Calendar.getInstance());

        dm = remote.saveDocument(dm);
        remote.save();

        // remote.disconnect();
        return dm;
    }

    private DocumentModel createSampleDomain() throws Exception {
        // Create a document model.
        DocumentModel root = remote.getRootDocument();
        DocumentModel dm = new DocumentModelImpl(root.getPathAsString(),
                "domain#" + generateUnique(), "Domain");
        dm = createChildDocument(dm);
        remote.save();

        assertEquals("project", remote.getCurrentLifeCycleState(dm.getRef()));
        assertEquals("Domain", dm.getType());

        dm.setProperty("dublincore", "title", "Indexable domain");
        dm.setProperty("dublincore", "description",
                "Indexable domain description");
        String[] contributors = { "a", "b" };
        dm.setProperty("dublincore", "contributors", contributors);

        dm.setProperty("dublincore", "created", Calendar.getInstance());

        dm = remote.saveDocument(dm);
        remote.save();

        // remote.disconnect();
        return dm;
    }

    private DocumentModel createSampleMyDocument() throws Exception {
        // Create a document model.
        DocumentModel root = remote.getRootDocument();
        DocumentModel dm = new DocumentModelImpl(root.getPathAsString(),
                "medoc#" + generateUnique(), "MyDocType");
        dm = createChildDocument(dm);
        remote.save();

        assertEquals("MyDocType", dm.getType());

        Map<String, String> name = new HashMap<String, String>();
        name.put("FirstName", "Georges");
        name.put("LastName", "Racinet");
        dm.setProperty("MySchema", "name", name);

        dm.setProperty("dublincore", "title", "A document of my type");

        HashMap<String, Serializable> task;
        List<HashMap<String, Serializable>> tasks = new ArrayList<HashMap<String, Serializable>>(2);
        task = new HashMap<String, Serializable>();
        task.put("what", "eat");
        tasks.add(task);
        task = new HashMap<String, Serializable>();
        task.put("what", "digest");
        tasks.add(task);
        dm.setProperty("task", "tasks", tasks);

        dm = remote.saveDocument(dm);
        remote.save();

        // remote.disconnect();
        return dm;
    }

    private static ResolvedData getDataFrom(String name, List<ResolvedData> data) {
        for (ResolvedData each : data) {
            if (each.getName().equals(name)) {
                return each;
            }
        }
        return null;
    }

    public void testWithEmptyDM() {
        // Ensure no exception
        DocumentModel dm = null;
        IndexableResources ir = IndexableResourcesFactory.computeResourcesFor(dm);
        assertNull(ir);
    }

    public void testIndexableResourcesGeneration() throws Exception {
        DocumentModel dm = createSampleFile();

        // Generate aggregated indexable resources and test out.
        IndexableResources res = IndexableResourcesFactory.computeResourcesFor(dm);
        assertEquals(dm.getId(), res.getId());

        // Test fetching data from the resources.
        List<IndexableResource> resources = res.getIndexableResources();
        assertEquals(5, resources.size());

        DocumentIndexableResource oneRes = (DocumentIndexableResource) resources.get(0);
        assertEquals("dublincore", oneRes.getName());

        assertEquals("Indexable data", oneRes.getValueFor("dublincore:title"));
        assertEquals("Indexable description",
                oneRes.getValueFor("dublincore:description"));

        // Test configuration including data field configuration
        IndexableResourceConf conf = oneRes.getConfiguration();
        assertEquals("dublincore", conf.getName());

        Map<String, IndexableResourceDataConf> fields = conf.getIndexableFields();
        assertEquals(5, fields.size());

        // This configuration includes 3 fields
        assertTrue(fields.keySet().contains("title"));
        assertTrue(fields.keySet().contains("description"));
        assertTrue(fields.keySet().contains("contributors"));
        assertTrue(fields.keySet().contains("created"));

        // Test the title configuration
        IndexableResourceDataConf fconf = fields.get("title");
        assertNotNull(fconf);
        assertEquals("title", fconf.getIndexingName());
        assertEquals("standard", fconf.getIndexingAnalyzer());
        assertEquals("Text", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertFalse(fconf.isBinary());
        assertEquals(0, fconf.getTermVector().size());

        // Test the description configuration
        fconf = fields.get("description");
        assertNotNull(fconf);
        assertEquals("description", fconf.getIndexingName());
        assertEquals("standard", fconf.getIndexingAnalyzer());
        assertEquals("Text", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertFalse(fconf.isBinary());
        assertEquals(0, fconf.getTermVector().size());

        fconf = fields.get("contributors");
        assertNotNull(fconf);
        assertEquals("contributors", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("keyword", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertTrue(fconf.isMultiple());
        assertFalse(fconf.isBinary());

        fconf = fields.get("created");
        assertNotNull(fconf);
        assertEquals("created", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("date", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertFalse(fconf.isMultiple());
        assertFalse(fconf.isBinary());

        // Test doc resources metadata
        DocumentIndexableResource docRes = oneRes;
        assertEquals(dm.getRef(), docRes.getDocRef());
        assertEquals(dm.getParentRef(), docRes.getDocParentRef());
        assertEquals(dm.getType(), docRes.getDocType());
        assertEquals(dm.getPath(), docRes.getDocPath());
        assertEquals(dm.getRepositoryName(), docRes.getDocRepositoryName());

        // :XXX:
        // assertEquals(dm.getRepositoryName(), docRes.getDocURL());

        // Test second resources
        oneRes.closeCoreSession();

        oneRes = (DocumentIndexableResource) resources.get(3);
        assertEquals("file", oneRes.getName());
        assertEquals("foo.pdf", oneRes.getValueFor("file:filename"));

        // Test configuration including data field configuration
        conf = oneRes.getConfiguration();

        assertEquals("file", conf.getName());

        fields = conf.getIndexableFields();
        // do no more assume static fields count - schemas are dynamic and may change over time
        //assertEquals(7, fields.size());

        fconf = fields.get("filename");
        assertNotNull(fconf);
        assertEquals("filename", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("keyword", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertFalse(fconf.isBinary());
        assertEquals(0, fconf.getTermVector().size());

        fconf = fields.get("content:data");
        assertNotNull(fconf);
        assertEquals("content:data", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("text", fconf.getIndexingType());
        assertFalse(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertTrue(fconf.isBinary());
        assertEquals(0, fconf.getTermVector().size());

        fconf = fields.get("content:data");
        assertNotNull(fconf);
        assertEquals("content:data", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("text", fconf.getIndexingType());
        assertFalse(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertTrue(fconf.isBinary());
        assertEquals(0, fconf.getTermVector().size());

        fconf = fields.get("content:encoding");
        assertNotNull(fconf);
        assertEquals("content:encoding", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("keyword", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertFalse(fconf.isBinary());
        assertEquals(0, fconf.getTermVector().size());

        fconf = fields.get("content:mime-type");
        assertNotNull(fconf);
        assertEquals("content:mime-type", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("keyword", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertFalse(fconf.isBinary());
        assertEquals(0, fconf.getTermVector().size());

        // Test doc resources metadata
        docRes = oneRes;
        assertEquals(dm.getRef(), docRes.getDocRef());
        assertEquals(dm.getParentRef(), docRes.getDocParentRef());
        assertEquals(dm.getType(), docRes.getDocType());
        assertEquals(dm.getPath(), docRes.getDocPath());
        assertEquals(dm.getRepositoryName(), docRes.getDocRepositoryName());

        // :XXX:
        // assertEquals(dm.getRepositoryName(), docRes.getDocURL());

        oneRes.closeCoreSession();
    }

    public void testResolvedResourcesGeneration() throws Exception {

        DocumentModel dm = createSampleFile();

        // Generate aggregated indexable resources
        IndexableResources resources = IndexableResourcesFactory.computeResourcesFor(dm);

        // Generate aggregated resolved resources and test out.
        ResolvedResources aggregated = ResolvedResourcesFactory.computeAggregatedResolvedResourcesFrom(resources, true);

        assertEquals(dm.getId(), aggregated.getId());

        // Test ACP generation
        ACP acp = aggregated.getACP();
        assertNotNull(acp);

        // Test resources
        List<ResolvedResource> resolvedResources = aggregated.getIndexableResolvedResources();
        assertEquals(4, resolvedResources.size());

        // Test has been taken from the user configuration
        ResolvedResource resource = aggregated.getIndexableResolvedResourceByConfName("dublincore");
        assertNotNull(resource);
        assertEquals(resource.getId(), dm.getId());
        assertNotNull(resource.getIndexableResource());
        assertEquals(5, resource.getIndexableData().size());
        assertEquals("dc", resource.getConfiguration().getPrefix());

        ResolvedResource fileResource = aggregated.getIndexableResolvedResourceByConfName("file");
        assertNotNull(fileResource);
        assertEquals(fileResource.getId(), dm.getId());
        assertNotNull(fileResource.getIndexableResource());
        // do no more assume static fields count - schemas are dynamic and may change over time
        //assertEquals(7, fileResource.getIndexableData().size());

        assertEquals("file", fileResource.getConfiguration().getPrefix());

        // Test the title configuration
        ResolvedData data = resource.getIndexableDataByName("title");
        assertNotNull(data);
        assertEquals("title", data.getName());
        assertEquals("standard", data.getAnalyzerName());
        assertEquals("Text", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertEquals(0, data.getTermVector().size());
        assertEquals("Indexable data", data.getValue());

        // Test the description configuration
        data = resource.getIndexableDataByName("description");
        assertNotNull(data);
        assertEquals("description", data.getName());
        assertEquals("standard", data.getAnalyzerName());
        assertEquals("Text", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertEquals(0, data.getTermVector().size());
        assertEquals("Indexable description", data.getValue());

        ResolvedResource fConf = aggregated.getIndexableResolvedResourceByConfName("file");
        assertNotNull(fConf);
        assertEquals(fConf.getId(), dm.getId());
        assertNotNull(fConf.getIndexableResource());
        // do no more assume static fields count - schemas are dynamic and may change over time
        assertEquals(7, fConf.getIndexableData().size());

        // Test the contributorList configuration
        data = resource.getIndexableDataByName("contributors");
        assertNotNull(data);
        assertEquals("contributors", data.getName());
        assertEquals("default", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.isMultiple());
        assertEquals(0, data.getTermVector().size());
        List<String> value = Arrays.asList((String[]) data.getValue());
        assertTrue(value.contains("a"));
        assertTrue(value.contains("b"));

        data = resource.getIndexableDataByName("created");
        assertNotNull(data);
        assertEquals("created", data.getName());
        assertEquals("default", data.getAnalyzerName());
        assertEquals("date", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertFalse(data.isMultiple());
        assertEquals(0, data.getTermVector().size());
        assertTrue(data.getValue() instanceof GregorianCalendar);

        data = fConf.getIndexableDataByName("filename");

        assertNotNull(data);
        assertEquals("filename", data.getName());
        assertEquals("default", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertEquals(0, data.getTermVector().size());
        assertEquals("foo.pdf", data.getValue());

        data = fConf.getIndexableDataByName("content:data");
        assertNotNull(data);
        assertEquals("content:data", data.getName());
        assertEquals("default", data.getAnalyzerName());
        assertEquals("text", data.getTypeName());
        assertFalse(data.isStored());
        assertTrue(data.isIndexed());
        assertTrue(data.isBinary());
        assertEquals(0, data.getTermVector().size());
        Object ob = data.getValue();
        assertNotNull(ob);
        assertTrue(ob instanceof Blob);
        Blob blob = (Blob) ob;
        assertEquals("text/html", blob.getMimeType());
        assertEquals("ISO-8859-15", blob.getEncoding());
        assertEquals("<doc>Indexing baby</doc>", blob.getString());

        // other content subfields
        data = fConf.getIndexableDataByName("content:mime-type");
        assertNotNull(data);
        assertEquals("content:mime-type", data.getName());
        assertEquals("default", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertEquals(0, data.getTermVector().size());
        String v = (String) data.getValue();
        assertEquals("text/html", v);

        data = fConf.getIndexableDataByName("content:encoding");
        assertNotNull(data);
        assertEquals("content:encoding", data.getName());
        assertEquals("default", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertEquals(0, data.getTermVector().size());
        v = (String) data.getValue();
        assertEquals("ISO-8859-15", v);

        // Test merged data
        List<ResolvedData> merged = aggregated.getMergedIndexableData();
        //assertEquals(7, merged.size());

        // Test builtins
        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_PARENT_REF, merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_PARENT_REF, data.getName());
        assertEquals("keyword", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals(dm.getParentRef(), data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_UUID, merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_UUID, data.getName());
        assertEquals("keyword", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals(dm.getId(), data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_REF, merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_REF, data.getName());
        assertEquals("keyword", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals(dm.getRef(), data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_PATH, merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_PATH, data.getName());
        assertNull(data.getAnalyzerName());
        assertEquals("Path", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals(dm.getPath(), data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_TYPE, merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_TYPE, data.getName());
        assertEquals("keyword", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals(dm.getType(), data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_REPOSITORY_NAME,
                merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_REPOSITORY_NAME,
                data.getName());
        assertEquals("keyword", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals(dm.getRepositoryName(), data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_URL, merged);
        assertNotNull(data);
        assertEquals("keyword", data.getAnalyzerName());
        assertEquals(BuiltinDocumentFields.FIELD_DOC_URL, data.getName());

        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        // :XXX:
        assertEquals("foo/bar", data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE, merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE, data.getName());
        assertEquals("keyword", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals("project", data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_FULLTEXT, merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_FULLTEXT, data.getName());
        assertEquals("french", data.getAnalyzerName());
        assertEquals("text", data.getTypeName());
        assertFalse(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals("Indexable data Indexable description",
                normalizeWhiteSpace(data.getValue()));

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_VERSION_LABEL,
                merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_VERSION_LABEL,
                data.getName());
        assertEquals("keyword", data.getAnalyzerName());
        assertEquals("keyword", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        // can't set versionLabel on DocumentModelImpl
        // No time to subclass a fake
        assertNull(data.getValue());

        data = getDataFrom(BuiltinDocumentFields.FIELD_DOC_IS_PROXY,
                merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_DOC_IS_PROXY,
                data.getName());
        assertEquals("boolean", data.getAnalyzerName());
        assertEquals("boolean", data.getTypeName());
        assertTrue(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        // can't set versionLabel on DocumentModelImpl
        // No time to subclass a fake
        assertFalse((Boolean)data.getValue());
    }

    private static String normalizeWhiteSpace(Object s) {
        return ((String) s).trim().replaceAll("\\s+", " ");
    }

    public void assertTextEquals(String expected, String actual) {
        assertEquals(normalizeTokens(expected), normalizeTokens(actual));
    }

    private static List<String> normalizeTokens(String txt) {
        txt = txt.trim();
        String[] tokens = txt.split("\\s+");
        Arrays.sort(tokens);
        return Arrays.asList(tokens);
    }

    public void TODOtestFullTextAll() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxsearch-test-fulltext-all.xml");
        DocumentModel dm = createSampleMyDocument();

        // Generate aggregated indexable resources
        IndexableResources resources = IndexableResourcesFactory.computeResourcesFor(dm);

        // Generate aggregated resolved resources and test out.
        ResolvedResources aggregated = ResolvedResourcesFactory.computeAggregatedResolvedResourcesFrom(resources, true);

        List<ResolvedData> merged = aggregated.getMergedIndexableData();
        ResolvedData data;

        data = getDataFrom(BuiltinDocumentFields.FIELD_FULLTEXT, merged);

        assertEquals("text", data.getTypeName());
        assertFalse(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertTextEquals("A document of my type eat digest Racinet Georges the default value",
                (String)data.getValue());

        // Now with blobs
        dm = createSampleFile();
        resources = IndexableResourcesFactory.computeResourcesFor(dm);
        aggregated = ResolvedResourcesFactory.computeAggregatedResolvedResourcesFrom(resources, true);
        merged = aggregated.getMergedIndexableData();
        data = getDataFrom(BuiltinDocumentFields.FIELD_FULLTEXT, merged);

        assertEquals("text", data.getTypeName());
        assertFalse(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        // note: no conversion available for text/html from here
        assertTextEquals("a b Indexable data Indexable description text/html ISO-8859-15 foo.pdf",
                (String) data.getValue());
    }

    public void TODOtestFullTextAllOneResource() throws Exception {
        DocumentModel dm = createSampleMyDocument();

        FulltextFieldDescriptor desc = service.getFullTextDescriptorByName("ecm:fulltext");

        // Generate aggregated indexable resources
        IndexableResources resources = IndexableResourcesFactory.computeResourcesFor(dm);

        assertTextEquals(
                "eat digest",
                ResolvedResourcesFactory.extractAllForFullText(
                        "task", desc, resources));
        assertTextEquals(
                "Racinet Georges the default value",
                ResolvedResourcesFactory.extractAllForFullText(
                        "MySchema", desc, resources));
    }

    // Disabled because faild when ran after testResolvedResourcesGeneration
    // individual run is ok though
    public void testResolvedResourcesGenerationTextPlainBlob()
            throws Exception {
        DocumentModel dm = createSampleFileTextPlain();

        // Generate aggregated indexable resources
        IndexableResources resources = IndexableResourcesFactory.computeResourcesFor(dm);

        // Generate aggregated resolved resources and test out.
        ResolvedResources aggregated = ResolvedResourcesFactory.computeAggregatedResolvedResourcesFrom(resources, true);

        assertEquals(dm.getId(), aggregated.getId());

        // Test ACP generation
        ACP acp = aggregated.getACP();
        assertNotNull(acp);

        // Test resources
        List<ResolvedResource> resolvedResources = aggregated.getIndexableResolvedResources();
        assertEquals(4, resolvedResources.size());

        ResolvedResource fileResource = aggregated.getIndexableResolvedResourceByConfName("file");
        assertNotNull(fileResource);
        assertEquals(fileResource.getId(), dm.getId());
        assertNotNull(fileResource.getIndexableResource());
        // do no more assume static fields count - schemas are dynamic and may change over time
        //assertEquals(7, fileResource.getIndexableData().size());

        assertEquals("file", fileResource.getConfiguration().getPrefix());

        ResolvedResource fConf = aggregated.getIndexableResolvedResourceByConfName("file");
        assertNotNull(fConf);
        assertEquals(fConf.getId(), dm.getId());
        assertNotNull(fConf.getIndexableResource());
        // do no more assume static fields count - schemas are dynamic and may change over time
        assertEquals(7, fConf.getIndexableData().size());

        ResolvedData data = fConf.getIndexableDataByName("content:data");
        assertNotNull(data);
        assertEquals("content:data", data.getName());
        assertEquals("default", data.getAnalyzerName());
        assertEquals("text", data.getTypeName());
        assertFalse(data.isStored());
        assertTrue(data.isIndexed());
        assertTrue(data.isBinary());
        assertEquals(0, data.getTermVector().size());
        Object ob = data.getValue();
        assertNotNull(ob);
        assertTrue(ob instanceof Blob);
        Blob blob = (Blob) ob;
        assertEquals("text/plain", blob.getMimeType());
        assertEquals("ISO-8859-15", blob.getEncoding());
        assertEquals("Indexing baby", blob.getString());

        // Test fulltext
        List<ResolvedData> merged = aggregated.getMergedIndexableData();
        data = getDataFrom(BuiltinDocumentFields.FIELD_FULLTEXT, merged);
        assertNotNull(data);
        assertEquals(BuiltinDocumentFields.FIELD_FULLTEXT, data.getName());
        // assertion fails if test is not run individually !
        //assertEquals("french", data.getAnalyzerName());
        assertEquals("text", data.getTypeName());
        assertFalse(data.isStored());
        assertTrue(data.isIndexed());
        assertFalse(data.isBinary());
        assertTrue(data.getTermVector().isEmpty());
        assertEquals("Indexable data Indexable description Indexing baby",
                normalizeWhiteSpace(data.getValue()));
    }

    public void TODOtestComplexProperty() throws Exception {
        DocumentModel dm = createSampleMyDocument();

        // Generate aggregated indexable resources
        IndexableResources resources = IndexableResourcesFactory.computeResourcesFor(dm);

        // Generate aggregated resolved resources and test out.
        ResolvedResources aggregated = ResolvedResourcesFactory.computeAggregatedResolvedResourcesFrom(
                resources, true);

        assertEquals(dm.getId(), aggregated.getId());

        // Test ACP generation
        ACP acp = aggregated.getACP();
        assertNotNull(acp);

        // Test resources
        List<ResolvedResource> resolvedResources = aggregated.getIndexableResolvedResources();
        assertEquals(4, resolvedResources.size());

        // Test has been taken from the user configuration
        ResolvedResource resource = aggregated.getIndexableResolvedResourceByConfName("MySchema");
        assertNotNull(resource);
        assertEquals(resource.getId(), dm.getId());
        assertNotNull(resource.getIndexableResource());
        // do no more assume static fields count - schemas are dynamic and may change over time
        assertEquals(15, resource.getIndexableData().size());
        assertEquals("my", resource.getConfiguration().getPrefix());

        ResolvedData data;
        data = resource.getIndexableDataByName("name:FirstName");
        assertNotNull(data);
        assertEquals("Georges", data.getValue());
        data = resource.getIndexableDataByName("name:LastName");
        assertNotNull(data);
        assertEquals("Racinet", data.getValue());
    }

    private static IndexableResources computeResourcesFor(DocumentModel doc) {
        return IndexableResourcesFactory.computeResourcesFor(doc);
    }

    public void TODOtestUpdateModification() throws Exception {
        DocumentModel doc = createSampleDomain();
        service.index(computeResourcesFor(doc), true);
    }

    public void testPdfIndexation() throws Exception {
        DocumentModel doc = createSampleFileWithPdfBlob();
        service.index(computeResourcesFor(doc), true);
    }

}
