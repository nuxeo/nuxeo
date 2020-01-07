/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.test.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.schema.FacetNames.COLD_STORAGE;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.io.DocumentModelReaders;
import org.nuxeo.elasticsearch.io.JsonESDocumentWriter;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 5.9.5
 */

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestDocumentModelReader {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Before
    public void setupIndex() {
        esa.initIndexes(true);
    }

    @Test
    public void ICanReadADocModelFromJson() {
        String json = "{\"ecm:versionLabel\":\"0.0\",\"common:icon-expanded\":null,"
                + "\"ecm:currentLifeCycleState\":\"project\",\"ecm:changeToken\":null,\"ecm:uuid\":\"56ca3935-c6c9-4cd4-ac23-d9df5ebf340a\","
                + "\"dc:nature\":\"Nature0\",\"dc:created\":null,\"relatedtext:relatedtextresources\":[],\"dc:description\":null,"
                + "\"dc:rights\":\"Rights0\",\"file:content\":null,\"uid:uid\":null,\"files:files\":[],"
                + "\"ecm:acl\":[\"administrators\",\"Administrator\",\"members\"],\"dc:subjects\":[],"
                + "\"dc:format\":null,\"dc:valid\":null,\"ecm:path\":\"/root/my/path/file0\","
                + "\"ecm:mixinType\":[\"Downloadable\",\"Commentable\",\"Versionable\",\"Publishable\",\"HasRelatedText\", \"ColdStorage\"],"
                + "\"ecm:isProxy\":false,\"ecm:isCheckedIn\":false,"
                + "\"dc:title\":\"File Title\",\"dc:lastContributor\":null,"
                + "\"ecm:repository\":\"test\",\"common:icon\":null,\"dc:creator\":null,"
                + "\"ecm:primaryType\":\"File\",\"dc:contributors\":[],\"dc:source\":null,"
                + "\"ecm:name\":\"file0\",\"dc:publisher\":null,\"uid:major_version\":\"0\",\"ecm:parentId\":\"35cef677-f721-47b8-ab6b-050dbe257d0d\","
                + "\"ecm:isVersion\":false,\"uid:minor_version\":\"0\",\"dc:issued\":null,"
                + "\"ecm:title\":\"File Title\",\"dc:modified\":null,\"dc:expired\":null,\"dc:coverage\":null,\"dc:language\":null}";
        DocumentModel doc = DocumentModelReaders.fromJson(json).getDocumentModel();
        assertNotNull(doc);
        assertEquals("56ca3935-c6c9-4cd4-ac23-d9df5ebf340a", doc.getId());
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertEquals("file0", doc.getName());
        assertEquals("/root/my/path/file0", doc.getPathAsString());
        assertEquals("test", doc.getRepositoryName());
        assertNull(doc.getSessionId());
        assertFalse(doc.isProxy());
        assertFalse(doc.isFolder());
        assertFalse(doc.isVersion());
        assertFalse(doc.isLocked());
        assertTrue(doc.hasFacet(COLD_STORAGE));
        assertEquals("File Title", doc.getTitle());
        assertNotNull(doc.getParentRef());
        assertTrue(doc.isImmutable());
        assertEquals("File", doc.getType());
    }

    @Test
    public void ICanReadADocModelFromSource() {
        Map<String, Object> source = new HashMap<>();
        source.put("ecm:uuid", "001");
        source.put("ecm:primaryType", "File");
        DocumentModel doc = DocumentModelReaders.fromSource(source).getDocumentModel();
        assertNotNull(doc);
        assertEquals(doc.getId(), "001");
        assertEquals("File", doc.getType());
        assertFalse(doc.isFolder());
    }

    @Test
    public void IGetTheSameDocAsVcs() throws Exception {
        // I create a document
        DocumentModel doc = session.createDocumentModel("/", "somefile", "File");
        doc.setPropertyValue("dc:title", "Some file");
        session.createDocument(doc);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        WorkManager wm = Framework.getService(WorkManager.class);
        assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        esa.refresh();

        // search and retrieve from ES
        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        DocumentModelList docs = ess.query(
                new NxQueryBuilder(session).nxql("SELECT * FROM File").fetchFromElasticsearch());
        assertEquals(1, docs.totalSize());
        DocumentModel esDoc = docs.get(0);
        assertNotNull(esDoc);

        // search from ES retrieve with VCS
        docs = ess.query(new NxQueryBuilder(session).nxql("SELECT * FROM File").fetchFromDatabase());
        DocumentModel vcsDoc = docs.get(0);

        // compare both docs
        assertNotNull(esDoc);
        assertEquals(vcsDoc, esDoc);

        JsonFactory factory = new JsonFactory();
        OutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator jsonGen = factory.createGenerator(out)) {
            new JsonESDocumentWriter().writeESDocument(jsonGen, esDoc, null, null);
        }
        String esJson = out.toString();

        out = new ByteArrayOutputStream();
        try (JsonGenerator jsonGen = factory.createGenerator(out)) {
            new JsonESDocumentWriter().writeESDocument(jsonGen, vcsDoc, null, null);
        }
        String vcsJson = out.toString();

        assertEquals(vcsJson, esJson);
    }
}
