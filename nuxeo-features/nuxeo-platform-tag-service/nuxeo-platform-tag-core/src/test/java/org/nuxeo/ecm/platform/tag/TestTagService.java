/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Radu Darlea
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.tag;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.DocumentSnapshot;
import org.nuxeo.ecm.platform.ws.NuxeoRemotingBean;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.runtime.datasource", "org.nuxeo.ecm.platform.tag", "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.ws" })
@LocalDeploy("org.nuxeo.ecm.platform.tag:login-config.xml")
public class TestTagService {

    protected static final Log log = LogFactory.getLog(TestTagService.class);

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected TagService tagService;

    @Inject
    protected TrashService trashService;

    @Before
    public void checkTagsSupported() {
        assumeTrue("DBS does not support tags", !coreFeature.getStorageConfiguration().isDBS());
    }

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

        Set<String> file1set = new HashSet<String>(Arrays.asList(file1Id));
        Set<String> twofiles = new HashSet<String>(Arrays.asList(file1Id, file2Id));

        // add tag
        tagService.tag(session, file1Id, "mytag", "Administrator");
        tagService.tag(session, file1Id, "othertag", "Administrator");
        tagService.tag(session, file2Id, "mytag", "Administrator");
        session.save();

        Set<String> mytag = new HashSet<String>(Arrays.asList("mytag"));
        Set<String> twotags = new HashSet<String>(Arrays.asList("mytag", "othertag"));

        // find tags for doc
        List<Tag> tags;
        // file 1
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(twotags, labels(tags));
        tags = tagService.getDocumentTags(session, file1Id, "Administrator");
        assertEquals(twotags, labels(tags));
        tags = tagService.getDocumentTags(session, file1Id, "bob");
        assertTrue(tags.isEmpty());
        // file 2
        tags = tagService.getDocumentTags(session, file2Id, null);
        assertEquals(mytag, labels(tags));
        tags = tagService.getDocumentTags(session, file2Id, "Administrator");
        assertEquals(mytag, labels(tags));
        tags = tagService.getDocumentTags(session, file2Id, "bob");
        assertTrue(tags.isEmpty());

        maybeSleep();

        // find docs for tag
        List<String> docIds;
        // tag 1
        docIds = tagService.getTagDocumentIds(session, "mytag", null);
        assertEquals(twofiles, new HashSet<String>(docIds));
        docIds = tagService.getTagDocumentIds(session, "mytag", "Administrator");
        assertEquals(twofiles, new HashSet<String>(docIds));
        docIds = tagService.getTagDocumentIds(session, "mytag", "bob");
        assertTrue(docIds.isEmpty());
        // tag 2
        docIds = tagService.getTagDocumentIds(session, "othertag", null);
        assertEquals(file1set, new HashSet<String>(docIds));
        docIds = tagService.getTagDocumentIds(session, "othertag", "Administrator");
        assertEquals(file1set, new HashSet<String>(docIds));
        docIds = tagService.getTagDocumentIds(session, "othertag", "bob");
        assertTrue(docIds.isEmpty());

        maybeSleep();

        // global cloud
        List<Tag> cloud = tagService.getTagCloud(session, null, null, null);
        assertEquals(cloud.toString(), 2, cloud.size());
        Collections.sort(cloud, Tag.LABEL_COMPARATOR);
        Tag tag1 = cloud.get(0);
        assertEquals("mytag", tag1.getLabel());
        assertEquals(2, tag1.getWeight());
        Tag tag2 = cloud.get(1);
        assertEquals("othertag", tag2.getLabel());
        assertEquals(1, tag2.getWeight());
        // specific tagging user
        cloud = tagService.getTagCloud(session, null, "bob", null);
        assertEquals(0, cloud.size());

        // cloud per folder
        cloud = tagService.getTagCloud(session, fold.getId(), null, null);
        assertEquals(1, cloud.size()); // only file2 under fold
        Collections.sort(cloud, Tag.LABEL_COMPARATOR);
        tag1 = cloud.get(0);
        assertEquals("mytag", tag1.getLabel());
        assertEquals(1, tag1.getWeight());

        // cloud under root folder
        cloud = tagService.getTagCloud(session, session.getRootDocument().getId(), null, null);
        assertEquals(2, cloud.size());
        Collections.sort(cloud, Tag.LABEL_COMPARATOR);
        tag1 = cloud.get(0);
        assertEquals("mytag", tag1.getLabel());
        assertEquals(2, tag1.getWeight());
        tag2 = cloud.get(1);
        assertEquals("othertag", tag2.getLabel());
        assertEquals(1, tag2.getWeight());

        // suggestions
        List<Tag> suggestions = tagService.getSuggestions(session, "my", null);
        assertEquals(mytag, labels(suggestions));
        suggestions = tagService.getSuggestions(session, "%tag", null);
        assertEquals(twotags, labels(suggestions));

        maybeSleep();

        // ws loader

        NuxeoRemotingBean remoting = new NuxeoRemotingBean();
        String sid = remoting.connect("Administrator", "Administrator");
        DocumentSnapshot snapshot = remoting.getDocumentSnapshot(sid, file1Id);
        DocumentProperty[] props = snapshot.getNoBlobProperties();
        Comparator<DocumentProperty> propsComparator = new Comparator<DocumentProperty>() {

            @Override
            public int compare(DocumentProperty o1, DocumentProperty o2) {
                return o1.getName().compareTo(o2.getName());
            }

        };
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
        tagService.untag(session, file2Id, "mytag", null);
        tags = tagService.getDocumentTags(session, file2Id, null);
        assertTrue(tags.isEmpty());
        docIds = tagService.getTagDocumentIds(session, "mytag", "Administrator");
        assertEquals(file1set, new HashSet<String>(docIds));
        // remove all taggings on doc
        tagService.untag(session, file1Id, null, null);
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertTrue(tags.isEmpty());

        // close remote session
        remoting.disconnect(sid);
    }

    protected static Set<String> labels(List<Tag> tags) {
        Set<String> list = new HashSet<String>();
        for (Tag tag : tags) {
            list.add(tag.getLabel());
        }
        return list;
    }

    @Test
    public void testUntagOnDelete() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File1");
        file = session.createDocument(file);
        String file1Id = file.getId();
        List<Tag> tags;

        tagService.tag(session, file1Id, "mytag", "Administrator");

        // check tag present
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(Collections.singleton("mytag"), labels(tags));

        // delete doc
        session.removeDocument(file.getRef());
        TransactionHelper.commitOrRollbackTransaction();

        // wait for async tag removal
        Framework.getService(EventService.class).waitForAsyncCompletion();

        // check no more tag
        TransactionHelper.startTransaction();
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(Collections.emptySet(), labels(tags));
    }

    @Test
    public void testUntagOnTrash() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File1");
        file = session.createDocument(file);
        String file1Id = file.getId();
        List<Tag> tags;

        tagService.tag(session, file1Id, "mytag", "Administrator");

        // check tag present
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(Collections.singleton("mytag"), labels(tags));

        // trash doc
        file.followTransition(LifeCycleConstants.DELETE_TRANSITION);
        TransactionHelper.commitOrRollbackTransaction();

        // wait for async tag removal
        Framework.getService(EventService.class).waitForAsyncCompletion();

        // check no more tag
        TransactionHelper.startTransaction();
        tags = tagService.getDocumentTags(session, file1Id, null);
        assertEquals(Collections.emptySet(), labels(tags));
    }

    @Test
    public void testRemoveTags() {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File1");
        file = session.createDocument(file);
        session.save();

        String fileId = file.getId();

        tagService.tag(session, fileId, "foo", "Administrator");
        tagService.tag(session, fileId, "bar", "leela");
        tagService.tag(session, fileId, "foo", "bender");

        assertEquals(2, tagService.getDocumentTags(session, fileId, null).size());

        tagService.removeTags(session, fileId);

        assertEquals(0, tagService.getDocumentTags(session, fileId, null).size());
    }

    @Test
    public void testRemoveDoc() {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File1");
        file = session.createDocument(file);
        session.save();

        String fileId = file.getId();

        tagService.tag(session, fileId, "foo", "Administrator");
        tagService.tag(session, fileId, "bar", "leela");
        tagService.tag(session, fileId, "foo", "bender");

        assertEquals(2, tagService.getDocumentTags(session, fileId, null).size());

        session.removeDocument(file.getRef());

        // wait for async tag removal
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();

        // check no more tag
        assertEquals(0, tagService.getDocumentTags(session, fileId, null).size());
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

        tagService.tag(session, srcDocId, "foo", "Administrator");
        tagService.tag(session, srcDocId, "foo", "leela");
        tagService.tag(session, srcDocId, "foo", "bender");
        tagService.tag(session, srcDocId, "bar", "fry");
        tagService.tag(session, srcDocId, "bar", "leela");
        tagService.tag(session, srcDocId, "baz", "bender");

        assertEquals(3, tagService.getDocumentTags(session, srcDocId, null).size());
        assertEquals(0, tagService.getDocumentTags(session, dstDocId, null).size());

        tagService.copyTags(session, srcDocId, dstDocId);
        session.save();

        assertEquals(3, tagService.getDocumentTags(session, srcDocId, null).size());
        List<Tag> tags = tagService.getDocumentTags(session, dstDocId, null);
        assertEquals(3, tags.size());
        assertTrue(tags.contains(new Tag("foo", 0)));
        assertTrue(tags.contains(new Tag("bar", 0)));
        assertTrue(tags.contains(new Tag("baz", 0)));

        assertEquals(1, tagService.getDocumentTags(session, dstDocId, "Administrator").size());
        assertEquals(2, tagService.getDocumentTags(session, dstDocId, "leela").size());
        assertEquals(2, tagService.getDocumentTags(session, dstDocId, "bender").size());
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

        tagService.tag(session, srcDocId, "foo", "Administrator");
        tagService.tag(session, srcDocId, "foo", "leela");
        tagService.tag(session, srcDocId, "foo", "bender");
        tagService.tag(session, srcDocId, "bar", "fry");
        tagService.tag(session, srcDocId, "bar", "leela");
        tagService.tag(session, srcDocId, "baz", "bender");

        tagService.tag(session, dstDocId, "tag1", "Administrator");
        tagService.tag(session, dstDocId, "tag1", "fry");
        tagService.tag(session, dstDocId, "tag2", "leela");
        tagService.tag(session, dstDocId, "tag2", "bender");

        assertEquals(3, tagService.getDocumentTags(session, srcDocId, null).size());
        assertEquals(2, tagService.getDocumentTags(session, dstDocId, null).size());

        tagService.replaceTags(session, srcDocId, dstDocId);
        session.save();

        assertEquals(3, tagService.getDocumentTags(session, srcDocId, null).size());
        List<Tag> tags = tagService.getDocumentTags(session, dstDocId, null);
        assertEquals(3, tags.size());
        assertTrue(tags.contains(new Tag("foo", 0)));
        assertTrue(tags.contains(new Tag("bar", 0)));
        assertTrue(tags.contains(new Tag("baz", 0)));

        assertEquals(1, tagService.getDocumentTags(session, dstDocId, "Administrator").size());
        assertEquals(2, tagService.getDocumentTags(session, dstDocId, "leela").size());
        assertEquals(2, tagService.getDocumentTags(session, dstDocId, "bender").size());
    }

    @Test
    public void testCopyTagsOnVersion() {
        DocumentModel doc;
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "File1");
        doc = session.createDocument(doc);
        String docId = doc.getId();

        tagService.tag(session, docId, "foo", "Administrator");
        tagService.tag(session, docId, "foo", "leela");
        tagService.tag(session, docId, "foo", "bender");
        tagService.tag(session, docId, "bar", "fry");
        tagService.tag(session, docId, "bar", "leela");
        tagService.tag(session, docId, "baz", "bender");
        assertEquals(3, tagService.getDocumentTags(session, docId, null).size());

        DocumentRef versionRef = checkIn(doc.getRef());
        DocumentModel version = session.getDocument(versionRef);
        String versionId = version.getId();

        List<Tag> tags = tagService.getDocumentTags(session, versionId, null);
        assertEquals(3, tags.size());
        assertTrue(tags.contains(new Tag("foo", 0)));
        assertTrue(tags.contains(new Tag("bar", 0)));
        assertTrue(tags.contains(new Tag("baz", 0)));

        assertEquals(1, tagService.getDocumentTags(session, versionId, "Administrator").size());
        assertEquals(2, tagService.getDocumentTags(session, versionId, "leela").size());
        assertEquals(2, tagService.getDocumentTags(session, versionId, "bender").size());
        // prevents NXP-14608 NXP-14441
        tagService.removeTags(session, versionId);
        tagService.removeTags(session, docId);
    }

    @Test
    public void testCopyTagsOnProxy() {
        DocumentModel doc;
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "File1");
        doc = session.createDocument(doc);
        String docId = doc.getId();

        tagService.tag(session, docId, "foo", "Administrator");
        tagService.tag(session, docId, "foo", "leela");
        tagService.tag(session, docId, "foo", "bender");
        tagService.tag(session, docId, "bar", "fry");
        tagService.tag(session, docId, "bar", "leela");
        tagService.tag(session, docId, "baz", "bender");
        assertEquals(3, tagService.getDocumentTags(session, docId, null).size());

        DocumentModel proxy = publishDocument(doc);
        String proxyId = proxy.getId();

        List<Tag> tags = tagService.getDocumentTags(session, proxyId, null);
        assertEquals(3, tags.size());
        assertTrue(tags.contains(new Tag("foo", 0)));
        assertTrue(tags.contains(new Tag("bar", 0)));
        assertTrue(tags.contains(new Tag("baz", 0)));

        assertEquals(1, tagService.getDocumentTags(session, proxyId, "Administrator").size());
        assertEquals(2, tagService.getDocumentTags(session, proxyId, "leela").size());
        assertEquals(2, tagService.getDocumentTags(session, proxyId, "bender").size());
        // prevents NXP-14608 NXP-14441
        tagService.removeTags(session, proxy.getId());
        tagService.removeTags(session, docId);
    }

    @Test
    public void testCopyTagsOnRestoredVersion() {
        DocumentModel doc;
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "File1");
        doc = session.createDocument(doc);
        String docId = doc.getId();

        tagService.tag(session, docId, "foo", "Administrator");
        tagService.tag(session, docId, "foo", "leela");
        tagService.tag(session, docId, "foo", "bender");
        tagService.tag(session, docId, "bar", "fry");
        tagService.tag(session, docId, "bar", "leela");
        tagService.tag(session, docId, "baz", "bender");

        DocumentRef versionRef = checkIn(doc.getRef());
        DocumentModel version = session.getDocument(versionRef);
        String versionId = version.getId();

        assertEquals(3, tagService.getDocumentTags(session, docId, null).size());
        assertEquals(3, tagService.getDocumentTags(session, versionId, null).size());

        // put new tags on the version
        tagService.removeTags(session, versionId);
        tagService.tag(session, versionId, "tag1", "leela");
        tagService.tag(session, versionId, "tag2", "fry");

        restoreToVersion(doc.getRef(), version.getRef());

        List<Tag> tags = tagService.getDocumentTags(session, docId, null);
        assertEquals(2, tags.size());
        assertTrue(tags.contains(new Tag("tag1", 0)));
        assertTrue(tags.contains(new Tag("tag2", 0)));
        // prevents NXP-14608 NXP-14441
        tagService.removeTags(session, versionId);
        tagService.removeTags(session, docId);
    }

    protected DocumentRef checkIn(DocumentRef ref) {
        DocumentRef versionRef = session.checkIn(ref, null, null);
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        return versionRef;
    }

    protected DocumentModel publishDocument(DocumentModel doc) {
        DocumentModel proxy = session.publishDocument(doc, session.getRootDocument());
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        return proxy;
    }

    protected DocumentModel restoreToVersion(DocumentRef docRef, DocumentRef versionRef) {
        DocumentModel docModel = session.restoreToVersion(docRef, versionRef);
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        return docModel;
    }

    @Test
    public void testCloudNormalization() throws Exception {
        List<Tag> cloud;
        cloud = new ArrayList<Tag>();
        TagServiceImpl.normalizeCloud(cloud, 0, 0, true);

        // linear
        cloud.add(new Tag("a", 3));
        TagServiceImpl.normalizeCloud(cloud, 3, 3, true);
        assertEquals(100, cloud.get(0).getWeight());

        // logarithmic
        cloud.add(new Tag("a", 3));
        TagServiceImpl.normalizeCloud(cloud, 3, 3, false);
        assertEquals(100, cloud.get(0).getWeight());

        // linear
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 5));
        TagServiceImpl.normalizeCloud(cloud, 1, 5, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(100, cloud.get(1).getWeight());

        // logarithmic
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 5));
        TagServiceImpl.normalizeCloud(cloud, 1, 5, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(100, cloud.get(1).getWeight());

        // linear
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        TagServiceImpl.normalizeCloud(cloud, 1, 5, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(25, cloud.get(1).getWeight());
        assertEquals(100, cloud.get(2).getWeight());

        // logarithmic
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        TagServiceImpl.normalizeCloud(cloud, 1, 5, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(43, cloud.get(1).getWeight());
        assertEquals(100, cloud.get(2).getWeight());

        // linear
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        cloud.add(new Tag("d", 12));
        TagServiceImpl.normalizeCloud(cloud, 1, 12, true);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(9, cloud.get(1).getWeight());
        assertEquals(36, cloud.get(2).getWeight());
        assertEquals(100, cloud.get(3).getWeight());

        // logarithmic
        cloud = new ArrayList<Tag>();
        cloud.add(new Tag("a", 1));
        cloud.add(new Tag("b", 2));
        cloud.add(new Tag("c", 5));
        cloud.add(new Tag("d", 12));
        TagServiceImpl.normalizeCloud(cloud, 1, 12, false);
        assertEquals(0, cloud.get(0).getWeight());
        assertEquals(28, cloud.get(1).getWeight());
        assertEquals(65, cloud.get(2).getWeight());
        assertEquals(100, cloud.get(3).getWeight());
    }

    @Test
    public void testTag() throws Exception {
        int count = buildDocWithProxiesAndTags();

        log.debug("before commit");
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();

        String nxql = "SELECT * FROM Document, Relation order by ecm:uuid";
        DocumentModelList docs = session.query(nxql);

        // Due NXP-16154 this gives a random number of docs
        String digest = getDigest(docs);
        assertEquals(digest, count, docs.totalSize());
    }

    private int buildDocWithProxiesAndTags() {
        int count = 0;
        DocumentModel folder = session.createDocumentModel("/", "section", "Folder");
        session.createDocument(folder);
        folder = session.saveDocument(folder);
        count += 1; // folder
        for (int i = 0; i < 5; i++) {
            DocumentModel doc = session.createDocumentModel("/", "testDoc" + i, "File");
            doc.setPropertyValue("dc:title", "TestMe" + i);
            doc = session.createDocument(doc);
            session.saveDocument(doc);
            count += 1; // doc
            tagService.tag(session, doc.getId(), "mytag" + i, "Administrator");
            count += 2; // tagging + tag
            session.publishDocument(doc, folder);
            count += 2; // proxy + tagging
            count += 2; // version + tagging
            trashService.trashDocuments(Arrays.asList(doc));
            count -= 1; // tagging
        }
        return count;
    }

    protected String getDigest(DocumentModelList docs) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (DocumentModel doc : docs) {
            String nameOrTitle = doc.getName();
            if (nameOrTitle == null || nameOrTitle.isEmpty()) {
                nameOrTitle = doc.getTitle();
            }
            sb.append(String.format("%s %s proxy:%s %s", doc.getId(), doc.getType(), doc.isProxy(), nameOrTitle));
            sb.append("\n");
        }
        return sb.toString();
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
            tagService.tag(session, fileId, tagLabel, "Administrator");
            session.save();
            // find tag for doc
            List<Tag> tags = tagService.getDocumentTags(session, fileId, "Administrator");
            assertEquals(message, expectedTagLabels, labels(tags));
            // find suggestion
            List<Tag> suggestions = tagService.getSuggestions(session, "%" + charToTest + "tag", null);
            assertEquals(message, expectedTagLabels, labels(suggestions));
            // find documents with this tag
            List<String> taggedDocIds = tagService.getTagDocumentIds(session, tagLabel, null);
            assertEquals(message, singletonList(fileId), taggedDocIds);
            // remove tag
            tagService.untag(session, fileId, tagLabel, "Administrator");
            session.save();
            tags = tagService.getDocumentTags(session, fileId, "Administrator");
            assertTrue(message, tags.isEmpty());

            maybeSleep();

            // add tag again to test sanitize version
            tagService.tag(session, fileId, tagLabel, "Administrator");
            session.save();
            // find documents with this tag
            taggedDocIds = tagService.getTagDocumentIds(session, sanitizedLabel, null);
            assertEquals(message, singletonList(fileId), taggedDocIds);
            // remove tag
            tagService.untag(session, fileId, sanitizedLabel, "Administrator");
            session.save();
            tags = tagService.getDocumentTags(session, fileId, "Administrator");
            assertTrue(message, tags.isEmpty());

            maybeSleep();
        }
    }
}
