/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Radu Darlea
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_PRINCIPAL_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.DocumentSnapshot;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.Predicates;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.ws.NuxeoRemotingBean;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.runtime.datasource", "org.nuxeo.ecm.platform.tag", "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.ws", "org.nuxeo.ecm.platform.dublincore" })
@LocalDeploy({ "org.nuxeo.ecm.platform.tag:login-config.xml", "org.nuxeo.ecm.tag.tests:test-core-types-contrib.xml",
        "org.nuxeo.ecm.tag.tests:test-versioning-contrib.xml" })
public abstract class AbstractTestTagService {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected TagService tagService;

    @Inject
    protected HotDeployer deployer;

    protected boolean proxies;

    // Oracle fails if we do too many connections in a short time, sleep
    // here to prevent this.
    public void maybeSleep() throws Exception {
        StorageConfiguration storageConfiguration = coreFeature.getStorageConfiguration();
        if (storageConfiguration.isVCSOracle() || storageConfiguration.isVCSSQLServer()) {
            Thread.sleep(5 * 1000);
        }
    }

    @Test
    public void testTags() throws Exception {
        DocumentModel fold = session.createDocumentModel("/", "fold", "Folder");
        fold = session.createDocument(fold);
        DocumentModel file1 = session.createDocumentModel("/", "foo", "File");
        file1.setPropertyValue("dc:title", "File1");
        file1 = session.createDocument(file1);
        DocumentModel file2 = session.createDocumentModel("/fold", "bar", "File");
        file2.setPropertyValue("dc:title", "File2");
        file2 = session.createDocument(file2);
        session.save();
        String file1Id = file1.getId();
        String file2Id = file2.getId();

        Set<String> file1set = new HashSet<>(Collections.singleton(file1Id));
        Set<String> twofiles = new HashSet<>(Arrays.asList(file1Id, file2Id));

        // add tag
        tagService.tag(session, file1Id, "mytag");
        tagService.tag(session, file1Id, "othertag");
        tagService.tag(session, file2Id, "mytag");
        session.save();

        Set<String> mytag = new HashSet<>(Collections.singleton("mytag"));
        Set<String> twotags = new HashSet<>(Arrays.asList("mytag", "othertag"));

        // find tags for doc
        Set<String> tags;
        // file 1
        tags = tagService.getTags(session, file1Id);
        assertEquals(twotags, tags);
        // file 2
        tags = tagService.getTags(session, file2Id);
        assertEquals(mytag, tags);

        maybeSleep();

        // find docs for tag
        List<String> docIds;
        // tag 1
        docIds = tagService.getTagDocumentIds(session, "mytag");
        assertEquals(twofiles, new HashSet<>(docIds));
        // tag 2
        docIds = tagService.getTagDocumentIds(session, "othertag");
        assertEquals(file1set, new HashSet<>(docIds));

        maybeSleep();

        // suggestions
        Set<String> suggestions = tagService.getSuggestions(session, "my");
        assertEquals(mytag, suggestions);
        suggestions = tagService.getSuggestions(session, "%tag");
        assertEquals(twotags, suggestions);

        maybeSleep();

        // ws loader

        NuxeoRemotingBean remoting = new NuxeoRemotingBean();
        String sid = remoting.connect("Administrator", "Administrator");
        DocumentSnapshot snapshot = remoting.getDocumentSnapshot(sid, file1Id);
        DocumentProperty[] props = snapshot.getNoBlobProperties();
        Comparator<DocumentProperty> propsComparator = Comparator.comparing(DocumentProperty::getName);
        Arrays.sort(props, propsComparator);
        int ti = Arrays.binarySearch(props, new DocumentProperty("tags", null), propsComparator);
        assertTrue(ti > 0);
        String expected = "tags:othertag,mytag";
        String prop = props[ti].toString();
        if (!expected.equals(prop)) {
            // order depends on database
            expected = "tags:mytag,othertag";
        }
        assertEquals(expected, prop);
        // remove explicit tagging
        tagService.untag(session, file2Id, "mytag");
        tags = tagService.getTags(session, file2Id);
        assertTrue(tags.isEmpty());
        // remove all taggings on doc
        tagService.untag(session, file1Id, null);
        tags = tagService.getTags(session, file1Id);
        assertTrue(tags.isEmpty());

        // close remote session
        remoting.disconnect(sid);
    }

    @Test
    public void testUntagOnTrash() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File1");
        file = session.createDocument(file);
        String file1Id = file.getId();

        tagService.tag(session, file1Id, "mytag");

        // check tag present
        Set<String> tags = tagService.getTags(session, file1Id);
        assertEquals(Collections.singleton("mytag"), tags);

        // trash doc
        file.followTransition(LifeCycleConstants.DELETE_TRANSITION);
        TransactionHelper.commitOrRollbackTransaction();

        // wait for async tag removal
        Framework.getService(EventService.class).waitForAsyncCompletion();

        // check no more tag
        TransactionHelper.startTransaction();
        tags = tagService.getTags(session, file1Id);
        assertEquals(Collections.emptySet(), tags);
    }

    @Test
    public void testRemoveTags() {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File1");
        file = session.createDocument(file);
        session.save();

        String fileId = file.getId();

        tagService.tag(session, fileId, "foo");
        tagService.tag(session, fileId, "bar");

        assertEquals(2, tagService.getTags(session, fileId).size());

        tagService.removeTags(session, fileId);

        assertEquals(0, tagService.getTags(session, fileId).size());
    }

    @Test
    public void testCopyTags() {
        DocumentModel srcFile = session.createDocumentModel("/", "srcFile", "File");
        srcFile.setPropertyValue("dc:title", "File1");
        srcFile = session.createDocument(srcFile);
        session.save();
        String srcDocId = srcFile.getId();

        DocumentModel dstFile = session.createDocumentModel("/", "dstFile", "File");
        dstFile.setPropertyValue("dc:title", "File1");
        dstFile = session.createDocument(dstFile);
        session.save();
        String dstDocId = dstFile.getId();

        tagService.tag(session, srcDocId, "foo");
        tagService.tag(session, srcDocId, "bar");
        tagService.tag(session, srcDocId, "baz");

        assertEquals(3, tagService.getTags(session, srcDocId).size());
        assertEquals(0, tagService.getTags(session, dstDocId).size());

        tagService.copyTags(session, srcDocId, dstDocId);
        session.save();

        assertEquals(3, tagService.getTags(session, srcDocId).size());
        Set<String> tags = tagService.getTags(session, dstDocId);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("foo"));
        assertTrue(tags.contains("bar"));
        assertTrue(tags.contains("baz"));

    }

    @Test
    public void testReplaceTags() {
        DocumentModel srcFile = session.createDocumentModel("/", "srcFile", "File");
        srcFile.setPropertyValue("dc:title", "File1");
        srcFile = session.createDocument(srcFile);
        String srcDocId = srcFile.getId();

        DocumentModel dstFile = session.createDocumentModel("/", "dstFile", "File");
        dstFile.setPropertyValue("dc:title", "File1");
        dstFile = session.createDocument(dstFile);
        String dstDocId = dstFile.getId();
        session.save();

        tagService.tag(session, srcDocId, "foo");
        tagService.tag(session, srcDocId, "bar");
        tagService.tag(session, srcDocId, "baz");

        tagService.tag(session, dstDocId, "tag1");
        tagService.tag(session, dstDocId, "tag2");

        assertEquals(3, tagService.getTags(session, srcDocId).size());
        assertEquals(2, tagService.getTags(session, dstDocId).size());

        tagService.replaceTags(session, srcDocId, dstDocId);
        session.save();

        assertEquals(3, tagService.getTags(session, srcDocId).size());
        Set<String> tags = tagService.getTags(session, dstDocId);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("foo"));
        assertTrue(tags.contains("bar"));
        assertTrue(tags.contains("baz"));
    }

    @Test
    public void testCopyTagsOnVersion() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "File1");
        doc = session.createDocument(doc);
        String docId = doc.getId();

        tagService.tag(session, docId, "foo");
        tagService.tag(session, docId, "bar");
        tagService.tag(session, docId, "baz");
        assertEquals(3, tagService.getTags(session, docId).size());

        DocumentRef versionRef = checkIn(doc.getRef());
        DocumentModel version = session.getDocument(versionRef);
        String versionId = version.getId();

        Set<String> tags = tagService.getTags(session, versionId);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("foo"));
        assertTrue(tags.contains("bar"));
        assertTrue(tags.contains("baz"));

    }

    @Test
    public void testCopyTagsOnProxy() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "File1");
        doc = session.createDocument(doc);
        String docId = doc.getId();

        tagService.tag(session, docId, "foo");
        tagService.tag(session, docId, "bar");
        tagService.tag(session, docId, "baz");
        assertEquals(3, tagService.getTags(session, docId).size());

        DocumentModel proxy = publishDocument(doc);
        String proxyId = proxy.getId();

        Set<String> tags = tagService.getTags(session, proxyId);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("foo"));
        assertTrue(tags.contains("bar"));
        assertTrue(tags.contains("baz"));

    }

    @Test
    public void testTagsOnProxy() {

        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "File1");
        doc = session.createDocument(doc);
        String docId = doc.getId();

        tagService.tag(session, docId, "foo");
        tagService.tag(session, docId, "bar");
        tagService.tag(session, docId, "baz");
        assertEquals(3, tagService.getTags(session, docId).size());

        DocumentModel proxy = publishDocument(doc);
        String proxyId = proxy.getId();

        try {
            tagService.tag(session, proxyId, "toto");
            fail("Tags should not be allowed on proxies");
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testCopyTagsOnRestoredVersion() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "File1");
        doc = session.createDocument(doc);
        String docId = doc.getId();

        tagService.tag(session, docId, "foo");
        tagService.tag(session, docId, "bar");
        tagService.tag(session, docId, "baz");

        DocumentRef versionRef = checkIn(doc.getRef());
        DocumentModel version = session.getDocument(versionRef);
        String versionId = version.getId();

        assertEquals(3, tagService.getTags(session, docId).size());
        assertEquals(3, tagService.getTags(session, versionId).size());

        // put new tags on the version
        tagService.removeTags(session, versionId);
        tagService.tag(session, versionId, "tag1");
        tagService.tag(session, versionId, "tag2");

        restoreToVersion(doc.getRef(), version.getRef());

        Set<String> tags = tagService.getTags(session, docId);
        assertEquals(2, tags.size());
        assertTrue(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));
    }

    protected DocumentRef checkIn(DocumentRef ref) {
        DocumentRef versionRef = session.checkIn(ref, null, null);
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        return versionRef;
    }

    protected DocumentModel publishDocument(DocumentModel doc) {
        DocumentModel proxy = session.publishDocument(doc, session.getRootDocument());
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        return proxy;
    }

    protected DocumentModel restoreToVersion(DocumentRef docRef, DocumentRef versionRef) {
        DocumentModel docModel = session.restoreToVersion(docRef, versionRef);
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        return docModel;
    }

    @Test
    public void testCloudNormalization() throws Exception {
        List<Tag> cloud = new ArrayList<>();
        RelationTagService.normalizeCloud(cloud, 0, 0, true);

        // linear
        cloud.add(new Tag("a", 3));
        RelationTagService.normalizeCloud(cloud, 3, 3, true);
        assertEquals(100, cloud.get(0).getWeight());

        // logarithmic
        cloud.add(new Tag("a", 3));
        RelationTagService.normalizeCloud(cloud, 3, 3, false);
        assertEquals(100, cloud.get(0).getWeight());

        // linear
        cloud = new ArrayList<>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 5));
        RelationTagService.normalizeCloud(cloud, 1, 5, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(100, cloud.get(1).getWeight());

        // logarithmic
        cloud = new ArrayList<>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 5));
        RelationTagService.normalizeCloud(cloud, 1, 5, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(100, cloud.get(1).getWeight());

        // linear
        cloud = new ArrayList<>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        RelationTagService.normalizeCloud(cloud, 1, 5, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(25, cloud.get(1).getWeight());
        assertEquals(100, cloud.get(2).getWeight());

        // logarithmic
        cloud = new ArrayList<>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        RelationTagService.normalizeCloud(cloud, 1, 5, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(43, cloud.get(1).getWeight());
        assertEquals(100, cloud.get(2).getWeight());

        // linear
        cloud = new ArrayList<>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        cloud.add(new Tag("d", 12));
        RelationTagService.normalizeCloud(cloud, 1, 12, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(9, cloud.get(1).getWeight());
        assertEquals(36, cloud.get(2).getWeight());
        assertEquals(100, cloud.get(3).getWeight());

        // logarithmic
        cloud = new ArrayList<>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        cloud.add(new Tag("d", 12));
        RelationTagService.normalizeCloud(cloud, 1, 12, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(28, cloud.get(1).getWeight());
        assertEquals(65, cloud.get(2).getWeight());
        assertEquals(100, cloud.get(3).getWeight());
    }

    /*
     * NXP-19047
     */
    @Test
    public void testUntagAllowed() {
        DocumentModel file1 = session.createDocumentModel("/", "foo", "File");
        file1.setPropertyValue("dc:title", "File1");
        file1 = session.createDocument(file1);
        session.save();
        String file1Id = file1.getId();

        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("bob", SecurityConstants.WRITE, true));
        acl.add(new ACE("bender", SecurityConstants.READ, true));
        session.setACP(file1.getRef(), acp, false);
        session.save();

        // Test untag for user with write permission
        try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {

            // Tag document
            tagService.tag(bobSession, file1Id, "mytag");

            // Test tag present
            Set<String> tags = tagService.getTags(bobSession, file1Id);
            assertEquals(1, tags.size());
            assertEquals("mytag", new ArrayList<>(tags).get(0));

            // Untag
            tagService.untag(bobSession, file1Id, "mytag");

            // Test tag absent
            tags = tagService.getTags(bobSession, file1Id);
            assertTrue(tags.isEmpty());
        }

        // Test untag for user which created tag
        try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bender")) {

            // Tag document
            tagService.tag(bobSession, file1Id, "othertag");

            // Test tag present
            Set<String> tags = tagService.getTags(bobSession, file1Id);
            assertEquals(1, tags.size());
            assertEquals("othertag", tags.toArray()[0]);

            // Untag
            tagService.untag(bobSession, file1Id, "othertag");

            // Test tag absent
            tags = tagService.getTags(bobSession, file1Id);
            assertTrue(tags.isEmpty());
        }
    }

    /*
     * NXP-19047
     */
    @Test
    public void testUntagForbidden() {
        DocumentModel file1 = session.createDocumentModel("/", "foo", "File");
        file1.setPropertyValue("dc:title", "File1");
        file1 = session.createDocument(file1);
        session.save();
        String file1Id = file1.getId();

        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("bob", SecurityConstants.READ, true));
        acl.add(new ACE("bender", SecurityConstants.WRITE, true));
        session.setACP(file1.getRef(), acp, false);
        session.save();

        try (CoreSession benderSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bender")) {

            // Tag document
            tagService.tag(benderSession, file1Id, "mytag");

            // Test tag present
            Set<String> tags = tagService.getTags(benderSession, file1Id);
            assertEquals(1, tags.size());
            assertEquals("mytag", tags.toArray()[0]);

            try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
                // Untag with bob user
                tagService.untag(bobSession, file1Id, "mytag");
                fail("bob is not allowed to untag document file1 tagged by bender");
            } catch (DocumentSecurityException e) {
                assertEquals("User 'bob' is not allowed to remove tag 'mytag' on document '" + file1Id + "'",
                        e.getMessage());
            }

            // Test tag present
            tags = tagService.getTags(benderSession, file1Id);
            assertEquals(1, tags.size());
            assertEquals("mytag", tags.toArray()[0]);
        }
    }

    @Test
    public void testSanitize() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File");
        file = session.createDocument(file);
        session.save();
        String fileId = file.getId();

        List<String> charToTests = Arrays.asList(" ", "\\", "/", "'", "%");
        String sanitizedLabel = "mytag";
        Set<String> expectedTagLabels = singleton(sanitizedLabel);
        for (String charToTest : charToTests) {
            String tagLabel = "my" + charToTest + "tag";
            String message = "Character '" + charToTest + "' is not well sanitized";

            // add tag
            tagService.tag(session, fileId, tagLabel);
            session.save();
            // find tag for doc
            Set<String> tags = tagService.getTags(session, fileId);
            assertEquals(message, expectedTagLabels, tags);
            // find suggestion
            Set<String> suggestions = tagService.getSuggestions(session, "%" + charToTest + "tag");
            assertEquals(message, expectedTagLabels, suggestions);
            // find documents with this tag
            List<String> taggedDocIds = tagService.getTagDocumentIds(session, tagLabel);
            assertEquals(message, singletonList(fileId), taggedDocIds);
            // remove tag
            tagService.untag(session, fileId, tagLabel);
            session.save();
            tags = tagService.getTags(session, fileId);
            assertTrue(message, tags.isEmpty());

            maybeSleep();

            // add tag again to test sanitize version
            tagService.tag(session, fileId, tagLabel);
            session.save();
            // find documents with this tag
            taggedDocIds = tagService.getTagDocumentIds(session, sanitizedLabel);
            assertEquals(message, singletonList(fileId), taggedDocIds);
            // remove tag
            tagService.untag(session, fileId, sanitizedLabel);
            session.save();
            tags = tagService.getTags(session, fileId);
            assertTrue(message, tags.isEmpty());

            maybeSleep();
        }
    }

    @Test
    public void testQueriesOnTagsWithProxies() throws Exception {
        proxies = true;
        testQueriesOnTags();
    }

    @Test
    public void testQueriesOnTagsWithoutProxies() throws Exception {
        proxies = false;
        testQueriesOnTags();
    }

    protected void testQueriesOnTags() throws Exception {
        String nxql;
        DocumentModelList dml;
        IterableQueryResult res;

        DocumentModel file1 = session.createDocumentModel("/", "file1", "File");
        file1.setPropertyValue("dc:title", "file1");
        session.createDocument(file1);
        DocumentModel file2 = session.createDocumentModel("/", "file2", "File");
        file2.setPropertyValue("dc:title", "file2");
        session.createDocument(file2);
        DocumentModel file3 = session.createDocumentModel("/", "file3", "File"); // without tags
        file3.setPropertyValue("dc:title", "file3");
        file3 = session.createDocument(file3);
        session.save();

        createTags();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        file1 = session.getDocument(new PathRef("/file1"));

        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag0'");
        assertEquals(0, session.query(nxql).size());

        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag1'");
        assertEquals(2, session.query(nxql).size());

        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag2'");
        dml = session.query(nxql);
        assertEquals(1, dml.size());
        assertEquals(file1.getId(), dml.get(0).getId());

        nxql = nxql("SELECT * FROM File WHERE ecm:tag IN ('tag1', 'tag2')");
        assertEquals(2, session.query(nxql).size());

        // unqualified name refers to the same tag
        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag1' AND ecm:tag = 'tag2'");
        assertEquals(0, session.query(nxql).size());

        // unqualified name refers to the same tag
        nxql = nxql("SELECT * FROM File WHERE ecm:tag = 'tag1' OR ecm:tag = 'tag2'");
        assertEquals(2, session.query(nxql).size());

        // any tag instance
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/* = 'tag1'");
        assertEquals(2, session.query(nxql).size());

        // any tag instance
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/* = 'tag1' AND ecm:tag/* = 'tag2'");
        dml = session.query(nxql);
        assertEquals(1, dml.size());
        assertEquals(file1.getId(), dml.get(0).getId());

        // any tag instance
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/* = 'tag1' OR ecm:tag/* = 'tag2'");
        dml = session.query(nxql);
        assertEquals(2, dml.size());

        if (supportsIsNull()) {
            // no tags
            nxql = nxql("SELECT * FROM File WHERE ecm:tag IS NULL'");
            dml = session.query(nxql);
            assertEquals(1, dml.size());
            assertEquals(file3.getId(), dml.get(0).getId());

            nxql = nxql("SELECT * FROM File WHERE ecm:tag/* IS NULL'");
            dml = session.query(nxql);
            assertEquals(1, dml.size());
            assertEquals(file3.getId(), dml.get(0).getId());
        }

        // numbered tag instance
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/*1 = 'tag1'");
        assertEquals(2, session.query(nxql).size());

        // numbered tag instance are the same tag
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/*1 = 'tag1' AND ecm:tag/*1 = 'tag2'");
        assertEquals(0, session.query(nxql).size());

        // different numbered tags
        nxql = nxql("SELECT * FROM File WHERE ecm:tag/*1 = 'tag1' AND ecm:tag/*2 = 'tag2'");
        assertEquals(1, session.query(nxql).size());

        // needs DISTINCT
        nxql = nxql("SELECT * FROM File WHERE ecm:tag IN ('tag1', 'tag2')");
        assertEquals(2, session.query(nxql).size());

        // needs DISTINCT
        nxql = nxql("SELECT * FROM File WHERE ecm:tag IN ('tag1', 'tag2') AND dc:title = 'file1'");
        dml = session.query(nxql);
        assertEquals(1, dml.size());
        assertEquals(file1.getId(), dml.get(0).getId());

        // ----- queryAndFetch -----

        nxql = nxql("SELECT ecm:tag FROM File");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        // file1: tag1, tag2; file2: tag1
        assertIterableQueryResult(res, "ecm:tag", "tag1", "tag2");
        res.close();

        nxql = nxql("SELECT ecm:tag FROM File WHERE ecm:tag LIKE '%1'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, "ecm:tag", "tag1");
        res.close();

        // explicit DISTINCT
        if (!coreFeature.getStorageConfiguration().isDBS()) {
            nxql = nxql("SELECT DISTINCT ecm:tag FROM File");
            res = session.queryAndFetch(nxql, NXQL.NXQL);
            assertIterableQueryResult(res, "ecm:tag", "tag1", "tag2");
            res.close();
        }

        nxql = nxql("SELECT ecm:tag FROM File WHERE dc:title = 'file1'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, "ecm:tag", "tag1", "tag2");
        res.close();

        // unqualified name refers to the same tag
        nxql = nxql("SELECT ecm:tag FROM File WHERE ecm:tag = 'tag1'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, "ecm:tag", "tag1");
        res.close();

        // unqualified name refers to the same tag
        nxql = nxql("SELECT ecm:tag FROM File WHERE ecm:tag = 'tag2'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, "ecm:tag", "tag2");
        res.close();

        // numbered tag
        nxql = nxql("SELECT ecm:tag/*1 FROM File WHERE ecm:tag/*1 = 'tag1'");
        res = session.queryAndFetch(nxql, NXQL.NXQL);
        assertIterableQueryResult(res, "ecm:tag/*1", "tag1");
        res.close();
    }

    protected String nxql(String nxql) {
        if (proxies) {
            return nxql;
        } else if (nxql.contains(" WHERE ")) {
            return nxql.replace(" WHERE ", " WHERE ecm:isProxy = 0 AND ");
        } else {
            return nxql + " WHERE ecm:isProxy = 0";
        }
    }

    protected static void assertIterableQueryResult(IterableQueryResult actual, String prop, String... expected) {
        Collection<String> set = new HashSet<>();
        for (Map<String, Serializable> map : actual) {
            String tag = (String) map.get(prop);
            if (tag != null) {
                set.add(tag);
            }
        }
        assertEquals(new HashSet<>(Arrays.asList(expected)), set);
    }

    @Test
    public void testTagDoesNotTriggerAutomaticVersioning() {

        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        String fileId = file.getId();
        DocumentRef fileRef = file.getRef();
        session.save();
        assertEquals("0.0", file.getVersionLabel());
        assertEquals(0, tagService.getTags(session, fileId).size());

        // Test tagging
        tagService.tag(session, fileId, "tag1");
        file = session.getDocument(fileRef);
        assertEquals("0.0", file.getVersionLabel());
        assertEquals(1, tagService.getTags(session, fileId).size());

        DocumentModel note = session.createDocumentModel("/", "note", "TestNote");
        note = session.createDocument(note);
        String noteId = note.getId();
        DocumentRef noteRef = note.getRef();
        session.save();
        assertEquals("0.1", note.getVersionLabel());
        assertEquals(0, tagService.getTags(session, noteId).size());

        // Test tagging
        tagService.tag(session, noteId, "tag1");
        tagService.tag(session, noteId, "tag2");
        note = session.getDocument(noteRef);
        assertEquals("0.1", note.getVersionLabel());
        assertEquals(2, tagService.getTags(session, noteId).size());

        // Test untagging
        tagService.untag(session, noteId, "tag2");
        note = session.getDocument(noteRef);
        assertEquals("0.1", note.getVersionLabel());
        assertEquals(1, tagService.getTags(session, noteId).size());

        // Test copying tags
        DocumentModel otherNote = session.createDocumentModel("/", "otherNote", "TestNote");
        otherNote = session.createDocument(otherNote);
        String otherId = otherNote.getId();
        tagService.tag(session, otherId, "othertag1");
        tagService.tag(session, otherId, "othertag2");
        tagService.tag(session, otherId, "othertag3");

        tagService.copyTags(session, otherId, noteId);
        note = session.getDocument(noteRef);
        assertEquals("0.1", note.getVersionLabel());
        assertEquals(4, tagService.getTags(session, noteId).size());
    }

    /*
     * NXP-24176
     */
    @Test
    public void testTagDoesNotChangeLastContributor() {
        DocumentModel file1 = session.createDocumentModel("/", "foo", "File");
        file1.setPropertyValue("dc:title", "File1");
        file1 = session.createDocument(file1);
        session.save();
        assertEquals("Administrator", file1.getPropertyValue("dc:lastContributor"));

        String file1Id = file1.getId();
        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("bob", SecurityConstants.READ, true));
        session.setACP(file1.getRef(), acp, false);
        session.save();

        try (CoreSession bobSession = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            // Tag with bob user does not chang the last contributor on the document
            tagService.tag(bobSession, file1Id, "tag");
            file1 = bobSession.getDocument(file1.getRef());
            assertEquals("Administrator", file1.getPropertyValue("dc:lastContributor"));
        }

        // Also check that the event was not logged in the audit
        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        AuditBackend backend = audit.getBackend();
        assertEquals(0, backend.queryLogs(new AuditQueryBuilder().predicates(Predicates.eq(LOG_PRINCIPAL_NAME, "bob"),
                Predicates.eq(LOG_EVENT_ID, DOCUMENT_UPDATED))).size());

    }

    protected abstract void createTags();

    protected boolean supportsIsNull() {
        return true;
    }

}
