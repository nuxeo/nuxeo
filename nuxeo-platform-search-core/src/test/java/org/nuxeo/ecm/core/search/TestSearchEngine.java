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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.blobs.BlobExtractor;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentIndexableResourceImpl;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.events.IndexingEventConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocType;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.ecm.core.search.blobs.NXTransformBlobExtractor;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

/**
 * Test search engine plugins registration.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestSearchEngine extends SQLRepositoryTestCase {

    private static final String BACKEND_NAME = "fake";
    private static final String CONF_NAME = "fake.xml";
    private static final String DOC_SCHEMA_CONF_NAME = "dublincore";

    private SearchService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.search.tests", "nxsearch-test-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests", "nxsearch-test-contrib.xml");

        service = Framework.getLocalService(SearchService.class);
        assertNotNull(service);
    }

    private SearchServiceInternals getSearchServiceInternals() {
        return (SearchServiceInternals) service;
    }

    public void testBackendProperties() {
        SearchEngineBackend backend = getSearchServiceInternals().getSearchEngineBackendByName(
                BACKEND_NAME);
        assertEquals(BACKEND_NAME, backend.getName());
        assertEquals(CONF_NAME, backend.getConfigurationFileName());
    }

    public void testBackendRegistration() {
        assertEquals(1,
                getSearchServiceInternals().getSearchEngineBackends().size());
        assertNotNull(getSearchServiceInternals().getSearchEngineBackendByName(
                BACKEND_NAME));
        assertEquals(BACKEND_NAME,
                getSearchServiceInternals().getDefaultSearchEngineBakendName());
        assertEquals(BACKEND_NAME,
                getSearchServiceInternals().getPreferedBackendNameFor(null));
    }

    public void testIndexableSchemaRegistration() {
        assertEquals(3,
                getSearchServiceInternals().getIndexableResourceConfs().size());
        assertNotNull(getSearchServiceInternals().getIndexableResourceConfByName(
                DOC_SCHEMA_CONF_NAME, false));
        assertNotNull(getSearchServiceInternals().getIndexableResourceConfByName(
                BuiltinDocumentFields.DOC_BUILTINS_RESOURCE_NAME, false));
    }

    public void testDocType2IndexableResourcesRegistration() {
        Map<String, IndexableDocType> mapping = getSearchServiceInternals().getIndexableDocTypes();
        assertEquals(3, mapping.size());

        IndexableDocType docType = getSearchServiceInternals().getIndexableDocTypeFor(
                "File");
        assertEquals("File", docType.getType());

        assertTrue(docType.areAllSchemasIndexable());

        List<String> resources = docType.getResources();
        assertEquals(1, resources.size());
        assertTrue(resources.contains("dublincore"));

        List<String> excluded = docType.getExcludedSchemas();
        assertEquals(3, excluded.size());
        assertTrue(excluded.contains("common"));
        assertTrue(excluded.contains("Downloadable"));
        assertTrue(excluded.contains("Versionable"));

        docType = getSearchServiceInternals().getIndexableDocTypeFor("Domain");
        assertEquals("Domain", docType.getType());

        assertTrue(docType.areAllSchemasIndexable());

        resources = docType.getResources();
        assertEquals(0, resources.size());

        excluded = docType.getExcludedSchemas();
        assertEquals(0, excluded.size());
    }

    public void testIndexableSchemaXMAPGeneration() {
        IndexableResourceConf dcResourceConf = getSearchServiceInternals().getIndexableResourceConfByName(
                DOC_SCHEMA_CONF_NAME, false);

        assertEquals(DOC_SCHEMA_CONF_NAME, dcResourceConf.getName());

        assertEquals(DOC_SCHEMA_CONF_NAME, dcResourceConf.getName());
        assertEquals("dc", dcResourceConf.getPrefix());
        assertTrue(dcResourceConf.areAllFieldsIndexable());
        assertEquals(10, dcResourceConf.getExcludedFields().size());

        Map<String, IndexableResourceDataConf> fields = dcResourceConf.getIndexableFields();
        assertEquals(3, fields.size());

        // This configuration includes 3 fields

        assertTrue(fields.keySet().contains("title"));
        assertTrue(fields.keySet().contains("description"));
        assertTrue(fields.keySet().contains("contributors"));

        // Test the title configuration

        IndexableResourceDataConf fconf = fields.get("title");
        assertNotNull(fconf);
        assertEquals("title", fconf.getIndexingName());
        assertEquals("standard", fconf.getIndexingAnalyzer());
        assertEquals("Text", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isSortable());
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
        assertFalse(fconf.isSortable());
        assertEquals(0, fconf.getTermVector().size());

        // Test the contributorList configuration

        fconf = fields.get("contributors");
        assertNotNull(fconf);
        assertEquals("contributors", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("keyword", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertTrue(fconf.isMultiple());
        assertFalse(fconf.isBinary());

        // Test fake conf with reindex all fields flag.

        dcResourceConf = getSearchServiceInternals().getIndexableResourceConfByName(
                "fake", false);
        assertTrue(dcResourceConf.areAllFieldsIndexable());
        fields = dcResourceConf.getIndexableFields();

        // Test the fake field with Term vectors.

        fconf = fields.get("fake");
        assertNotNull(fconf);
        assertEquals("fake", fconf.getIndexingName());
        assertEquals("standard", fconf.getIndexingAnalyzer());
        assertEquals("Text", fconf.getIndexingType());
        assertFalse(fconf.isStored());
        assertFalse(fconf.isIndexed());
        assertTrue(fconf.isBinary());
        assertEquals(2, fconf.getTermVector().size());
    }

    public void testCapabilities() {
        String[] names = getSearchServiceInternals().getAvailableBackendNames();
        assertEquals(1, names.length);
        assertEquals(BACKEND_NAME, names[0]);
    }

    public void testReverseTranslation() {
        IndexableResourceDataConf conf = getSearchServiceInternals().getIndexableDataConfFor(
            "dc:title");
        assertEquals("title", conf.getIndexingName());

        // accept sub prop too
        conf = getSearchServiceInternals().getIndexableDataConfFor(
            "file:content:encoding");
        assertEquals("content:encoding", conf.getIndexingName());
    }

    public void testgetIndexableDataConfByName() {
        IndexableResourceDataConf conf = getSearchServiceInternals().getIndexableDataConfByName(
                "title");
        assertEquals("title", conf.getIndexingName());
        assertEquals("text", conf.getIndexingType().toLowerCase());
    }

    public void testSearchPrincipal() {
        SearchService service = getSearchServiceInternals();

        // Test using a NuxeoPrincipal
        SearchPrincipal sPrincipal = service.getSearchPrincipal(new FakeNuxeoPrincipal());
        assertEquals("foobar", sPrincipal.getName());
        assertEquals(3, sPrincipal.getGroups().length);
        assertEquals("foo", sPrincipal.getGroups()[0]);
        assertEquals(SecurityConstants.EVERYONE, sPrincipal.getGroups()[2]);

        // Test using a regular principal
        sPrincipal = service.getSearchPrincipal(new FakePrincipal());
        assertEquals("foobar", sPrincipal.getName());
        assertEquals(0, sPrincipal.getGroups().length);
    }

    public void testSchemaConfXmap() {
        IndexableResourceConf conf = getSearchServiceInternals().getIndexableResourceConfByName(
                "fake", false);

        assertEquals("fake", conf.getName());

        assertEquals("fake", conf.getName());
        assertTrue(conf.areAllFieldsIndexable());

        Set<String> excludedFields = conf.getExcludedFields();
        assertEquals(2, excludedFields.size());
        assertTrue(excludedFields.contains("ef1"));
        assertTrue(excludedFields.contains("ef2"));
    }

    public void testFullTextDescriptor() {
        FulltextFieldDescriptor desc = getSearchServiceInternals().getFullTextDescriptorByName(
                BuiltinDocumentFields.FIELD_FULLTEXT);
        assertNotNull(desc);

        assertEquals(BuiltinDocumentFields.FIELD_FULLTEXT, desc.getName());
        List<String> resourceFields = desc.getResourceFields();
        assertEquals(4, resourceFields.size());
        assertTrue(resourceFields.contains("dublincore:title"));
        assertTrue(resourceFields.contains("dublincore:description"));
        assertTrue(resourceFields.contains("file:content"));
        assertTrue(resourceFields.contains("fake:fake"));

        assertEquals("french", desc.getAnalyzer());

        assertEquals("nuxeoTransform", desc.getBlobExtractorName());

        String transformer = desc.lookupTransformer("application/pdf");
        assertEquals("pdf2text", transformer);

        transformer = desc.lookupTransformer("application/ms-word");
        assertEquals("word2text", transformer);

        transformer = desc.lookupTransformer("application/x-test-app");
        assertEquals("any2text", transformer);
    }

    public void testIndexingEventDescriptor() {
        IndexingEventConf desc = getSearchServiceInternals().getIndexingEventConfByName(
                "modify");
        assertNotNull(desc);
        assertEquals(IndexingEventConf.RE_INDEX, desc.getAction());
        assertNull(desc.getRelevantResources());
        assertFalse(desc.isRecursive());

        desc = getSearchServiceInternals().getIndexingEventConfByName("secu");
        assertNotNull(desc);
        assertEquals(IndexingEventConf.UN_INDEX, desc.getAction());
        Set<String> resources = desc.getRelevantResources();
        assertEquals(1, resources.size());
        assertTrue(resources.contains("security"));
        assertTrue(desc.isRecursive());
    }

    public void testReversedResolutionWithRegisteredResourceConfiguration() {

        // Indexable resource registered as a contribution.

        IndexableResourceConf conf = service.getIndexableResourceConfByName(
                "dublincore", true);
        assertNotNull(conf);

        conf = service.getIndexableResourceConfByPrefix("dc", true);
        assertNotNull(conf);

        assertEquals("dublincore", conf.getName());

        Map<String, IndexableResourceDataConf> fields = conf.getIndexableFields();
        // assertEquals(4, fields.size());

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
    }

    public void testReversedResolutionWithUnRegisteredResourceConfiguration() {

        // Indexable resource registered as a contribution.

        IndexableResourceConf conf = service.getIndexableResourceConfByName(
                "file", true);
        assertNotNull(conf);

        // Note here the prefix doesn't exist.
        conf = service.getIndexableResourceConfByPrefix("file", true);
        assertNotNull(conf);

        assertEquals("file", conf.getName());

        Map<String, IndexableResourceDataConf> fields = conf.getIndexableFields();
        // do no more assume static fields count - schemas are dynamic and may change over time
        // log.error("fields content : "+fields);
        assertEquals(7, fields.size());

        IndexableResourceDataConf fconf = fields.get("filename");
        assertNotNull(fconf);
        assertEquals("filename", fconf.getIndexingName());
        assertEquals("default", fconf.getIndexingAnalyzer());
        assertEquals("keyword", fconf.getIndexingType());
        assertTrue(fconf.isStored());
        assertTrue(fconf.isIndexed());
        assertFalse(fconf.isBinary());
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
    }

    public void testBlobFullTextExtractor() {
        BlobExtractor extractor = service.getBlobExtractorByName("nuxeoTransform");
        assertNotNull(extractor);
        assertTrue(extractor instanceof NXTransformBlobExtractor);

        // Check if doesn't exist.
        extractor = service.getBlobExtractorByName("fake");
        assertNull(extractor);
    }

    /*
     * TODO add a test for facets related methods. Would need to fake type
     * service
     */

    public void testSchemaResourceTypeRegistration() {

        // SCHEMA

        ResourceTypeDescriptor rtype = service.getResourceTypeDescriptorByName(ResourceType.SCHEMA);
        assertNotNull(rtype);
        assertEquals(ResourceType.SCHEMA, rtype.getName());

        IndexableResourceFactory factory = rtype.getFactory();
        assertTrue(factory.createEmptyIndexableResource() instanceof DocumentIndexableResourceImpl);

        // DOC BUILTINS

        rtype = service.getResourceTypeDescriptorByName(ResourceType.DOC_BUILTINS);
        assertNotNull(rtype);
        assertEquals(ResourceType.DOC_BUILTINS, rtype.getName());

        factory = rtype.getFactory();
        assertTrue(factory.createEmptyIndexableResource() instanceof DocumentIndexableResourceImpl);

        // DOES NOT EXIST

        rtype = service.getResourceTypeDescriptorByName("doesnotexist");
        assertNull(rtype);
    }

    public void testIndexingThreadPoolPT() {
        assertEquals(16, service.getNumberOfIndexingThreads());
    }

}
