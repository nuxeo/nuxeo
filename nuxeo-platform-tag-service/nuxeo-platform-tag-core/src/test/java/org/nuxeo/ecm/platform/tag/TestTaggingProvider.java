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
 * $Id: TestEventConfService.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.tag;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.tag.entity.DublincoreEntity;
import org.nuxeo.ecm.platform.tag.entity.TagEntity;
import org.nuxeo.ecm.platform.tag.entity.TaggingEntity;
import org.nuxeo.ecm.platform.tag.persistence.TaggingProvider;
import org.nuxeo.runtime.api.Framework;

public class TestTaggingProvider extends SQLRepositoryTestCase {

    protected PersistenceProvider persistenceProvider;

    public TestTaggingProvider() {
        super(TestTaggingProvider.class.getName());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.tag");
        deployBundle("org.nuxeo.ecm.platform.tag.tests");

        openSession();

        TagServiceImpl service = (TagServiceImpl) Framework.getLocalService(TagService.class);
        service.updateSchema();

        createDataWarehouse();

        PersistenceProviderFactory factory = Framework.getService(PersistenceProviderFactory.class);
        persistenceProvider = factory.newProvider("nxtags");
        persistenceProvider.openPersistenceUnit();
        entityManager = persistenceProvider.acquireEntityManagerWithActiveTransaction();


        taggingProvider = TaggingProvider.createProvider(entityManager);
    }

    protected TaggingEntity doCreateTaggingEntry(
            DublincoreEntity targetDocument, TagEntity tag, String author,
            Boolean isPrivate) {
        TaggingEntity taggingEntry = new TaggingEntity();
        taggingEntry.setId(UUID.randomUUID().toString());
        taggingEntry.setTargetDocument(targetDocument);
        taggingEntry.setTag(tag);
        taggingEntry.setAuthor(author);
        taggingEntry.setCreationDate(new Date());
        taggingEntry.setIsPrivate(isPrivate);
        return taggingEntry;
    }

    DocumentModel folder;

    DocumentModel file1;

    DocumentModel file2;

    DocumentModel file3;

    DocumentModel tagRoot;

    DocumentModel tag1;

    DocumentModel tag2;

    DocumentModel tag3;

    TaggingProvider taggingProvider;

    EntityManager entityManager;

    /**
     * Creates the warehouse structure for allowing meaningfull tests. The
     * document space:
     * <p>
     * ROOT/folder/file1
     * <p>
     * ROOT/folder/file2
     * <p>
     * ROOT/file3
     * <p>
     * ROOT/TAGROOT/tag1/tag2
     * <p>
     * ROOT/TAGROOT/tag3
     */
    protected void createDataWarehouse() throws Exception {
        folder = session.createDocumentModel("/", "0001", "Folder");
        folder.setPropertyValue("dc:title", "folder");
        folder = session.createDocument(folder);
        folder = session.saveDocument(folder);
        tagRoot = session.createDocumentModel("/", "0002", "Tag");
        tagRoot.setPropertyValue("tag:label", "tag root");
        tagRoot.setPropertyValue("dc:title", "TAGROOT");
        tagRoot = session.createDocument(tagRoot);
        tagRoot = session.saveDocument(tagRoot);
        session.save();
        tag1 = session.createDocumentModel(tagRoot.getPathAsString(), "0003",
                "Tag");
        tag1.setPropertyValue("tag:label", "label1");
        tag1.setPropertyValue("dc:title", "Label1");
        tag1 = session.createDocument(tag1);
        tag1 = session.saveDocument(tag1);
        session.save();
        tag2 = session.createDocumentModel(tag1.getPathAsString(), "0004",
                "Tag");
        tag2.setPropertyValue("tag:label", "label2");
        tag2.setPropertyValue("dc:title", "Label2");
        tag2 = session.createDocument(tag2);
        tag2 = session.saveDocument(tag2);
        tag3 = session.createDocumentModel(tagRoot.getPathAsString(), "0005",
                "Tag");
        tag3.setPropertyValue("tag:label", "label3");
        tag3.setPropertyValue("dc:title", "Label3");
        tag3 = session.createDocument(tag3);
        tag3 = session.saveDocument(tag3);
        file1 = session.createDocumentModel(folder.getPathAsString(), "0006",
                "File");
        file1.setPropertyValue("dc:title", "File1");
        file1 = session.createDocument(file1);
        file1 = session.saveDocument(file1);
        file2 = session.createDocumentModel(folder.getPathAsString(), "0007",
                "File");
        file2.setPropertyValue("dc:title", "File2");
        file2 = session.createDocument(file2);
        file2 = session.saveDocument(file2);
        file3 = session.createDocumentModel("/", "0008", "File");
        file3.setPropertyValue("dc:title", "File3");
        file3 = session.createDocument(file3);
        file3 = session.saveDocument(file3);
        session.save();
    }

    /**
     * Creates complex tagging: file1 with tags: tag1, tag2 twice; file2 with
     * tags: tag3 3 times and tag1; file3 with tags: tag1, tag2 twice; folder
     * with tags: tag1 3 times and tag3.
     *
     * @throws Exception
     */
    protected void createTaggings() throws Exception {
        TagEntity tagEntity1 = taggingProvider.getTagById(tag1.getId());
        TagEntity tagEntity2 = taggingProvider.getTagById(tag2.getId());
        TagEntity tagEntity3 = taggingProvider.getTagById(tag3.getId());
        DublincoreEntity dcEntity1 = taggingProvider.getDcById(file1.getId());
        DublincoreEntity dcEntity2 = taggingProvider.getDcById(file2.getId());
        DublincoreEntity dcEntity3 = taggingProvider.getDcById(file3.getId());
        DublincoreEntity folderEntity = taggingProvider.getDcById(folder.getId());
        TaggingEntity tg = doCreateTaggingEntry(dcEntity1, tagEntity1, "hunus",
                Boolean.FALSE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity1, tagEntity2, "hunus", Boolean.FALSE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity1, tagEntity2, "gigi", Boolean.FALSE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity2, tagEntity1, "hunus", Boolean.FALSE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity2, tagEntity3, "hunus", Boolean.FALSE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity2, tagEntity3, "gigi", Boolean.FALSE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity2, tagEntity3, "private",
                Boolean.TRUE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity3, tagEntity1, "hunus", Boolean.TRUE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity3, tagEntity2, "private",
                Boolean.TRUE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(dcEntity3, tagEntity2, "gigi", Boolean.TRUE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(folderEntity, tagEntity1, "private",
                Boolean.TRUE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(folderEntity, tagEntity1, "hunus",
                Boolean.FALSE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(folderEntity, tagEntity1, "gigi",
                Boolean.FALSE);
        taggingProvider.addTagging(tg);
        tg = doCreateTaggingEntry(folderEntity, tagEntity3, "gigi",
                Boolean.FALSE);
        taggingProvider.addTagging(tg);
    }

    public static final Log log = LogFactory.getLog(TestTaggingProvider.class);

    public void testGetById() throws Exception {
        DublincoreEntity dcEntity = taggingProvider.getDcById(file2.getId());
        TagEntity tagEntity = taggingProvider.getTagById(tag2.getId());
        assertNotNull("Failed to get document", dcEntity);
        assertNotNull("Failed to get tag", tagEntity);
        assertEquals(dcEntity.getTitle(), file2.getTitle());
        assertEquals(tagEntity.getLabel(), tag2.getPropertyValue("tag:label"));
    }

    public void testAddTagging() throws Exception {
        DublincoreEntity dcEntity = taggingProvider.getDcById(file2.getId());
        TagEntity tagEntity = taggingProvider.getTagById(tag2.getId());
        TaggingEntity entry = doCreateTaggingEntry(dcEntity, tagEntity,
                "hunus", Boolean.FALSE);
        taggingProvider.addTagging(entry);
        assertNotNull("No tagging created?", entry.getId());
        TaggingEntity tgEntry = entityManager.find(TaggingEntity.class,
                entry.getId());
        assertEquals(tgEntry.getTargetDocument().getId(), file2.getId());
        assertEquals(tgEntry.getTag().getId(), tag2.getId());
    }

    public void testListTagsForDocumentPublic() throws Exception {
        createTaggings();
        List<Tag> listTag = taggingProvider.listTagsForDocument(file1.getId(),
                "hunus");
        assertEquals(2, listTag.size());
        for (Tag simpleTag : listTag) {
            assertTrue("Found " + simpleTag.tagLabel + " unknown",
                    "label1".equals(simpleTag.tagLabel)
                            || "label2".equals(simpleTag.tagLabel));
        }
    }

    public void testGetAuthor() throws Exception {
        createTaggings();
        TagEntity tagEntity2 = taggingProvider.getTagById(tag2.getId());
        String author = taggingProvider.getTaggingId(file1.getId(),
                tagEntity2.getLabel(), "hunus");
        assertNotNull(author);
    }

    public void testListTagsForDocumentPrivate() throws Exception {
        createTaggings();
        List<Tag> listTag = taggingProvider.listTagsForDocument(file3.getId(),
                "hunus");
        assertEquals(1, listTag.size());
        String label = listTag.get(0).tagLabel;
        assertEquals("label1", label);
        listTag = taggingProvider.listTagsForDocument(file3.getId(), "private");
        assertEquals(1, listTag.size());
        label = listTag.get(0).tagLabel;
        assertEquals("label2", label);
    }

    public void testListDocumentsForTag() throws Exception {
        createTaggings();
        // tag2 was applied on file1 (and file3 by gigi or private)
        List<String> result = taggingProvider.getDocumentsForTag(tag2.getId(),
                "gigi");
        assertEquals(2, result.size());
        assertTrue("File1 not found", result.contains(file1.getId()));
        assertTrue("File3 not found", result.contains(file3.getId()));

        result = taggingProvider.getDocumentsForTag(tag2.getId(), "another");
        assertEquals(1, result.size());
        assertTrue("File1 not found", result.contains(file1.getId()));
    }

    public void testGetVoteTag() throws Exception {
        createTaggings();
        // file1: tag1 - 1, tag2 - 2
        Long result = taggingProvider.getVoteTag(file1.getId(), tag1.getId(),
                "hunus");
        assertTrue(result + " vote for file1 / tag1 instead 1", result == 1);
        result = taggingProvider.getVoteTag(file1.getId(), tag2.getId(),
                "hunus");
        assertTrue(result + " vote for file1 / tag2 instead 2", result == 2);
        // file2 / tag3 - 2 public votes and extra 1 for private user
        result = taggingProvider.getVoteTag(file2.getId(), tag3.getId(),
                "hunus");
        assertTrue(result + " vote for file2 / tag3 instead 2", result == 2);
        result = taggingProvider.getVoteTag(file2.getId(), tag3.getId(),
                "private");
        assertTrue(result + " vote for file2 / tag3 instead 3", result == 3);
    }

    public void testRemoveTagging() throws Exception {
        createTaggings();
        // check folder has tag3 applied
        List<Tag> listTag = taggingProvider.listTagsForDocument(folder.getId(),
                "hunus");
        assertEquals(2, listTag.size());

        // remove the only one instance tag3 was applied
        boolean result = taggingProvider.removeTagging(folder.getId(),
                tag3.getId(), "gigi");
        assertTrue("Failed to remove it", result);

        // now check there is only one
        listTag = taggingProvider.listTagsForDocument(folder.getId(), "hunus");
        assertEquals(1, listTag.size());
    }

    public void testPopularCloudGeneration() throws Exception {
        createTaggings();
        // popular cloud for folder / any user: tag1 - 3, tag2 - 1, tag3 - 2
        DocumentModelList documents = new DocumentModelListImpl();
        documents.add(file1);
        documents.add(file2);
        documents.add(folder);
        List<WeightedTag> cloud = taggingProvider.getPopularCloud(documents,
                "hunus");
        assertEquals(3, cloud.size());
        for (WeightedTag weightedTag : cloud) {
            String label = weightedTag.tagLabel;
            if (label.equals("label1")) {
                assertEquals(3, weightedTag.weight);
            } else if (label.equals("label2")) {
                assertEquals(1, weightedTag.weight);
            } else if (label.equals("label3")) {
                assertEquals(2, weightedTag.weight);
            } else {
                fail("Unexpected label: " + label);
            }
        }
        // popular cloud for root / private: tag1 - 3, tag2 - 2, tag3 - 2
        documents.add(file3);
        cloud = taggingProvider.getPopularCloud(documents, "private");
        assertEquals(3, cloud.size());
        for (WeightedTag weightedTag : cloud) {
            String label = weightedTag.tagLabel;
            if (label.equals("label1")) {
                assertEquals(3, weightedTag.weight);
            } else if (label.equals("label2")) {
                assertEquals(2, weightedTag.weight);
            } else if (label.equals("label3")) {
                assertEquals(2, weightedTag.weight);
            } else {
                fail("Unexpected label: " + label);
            }
        }
    }

}
