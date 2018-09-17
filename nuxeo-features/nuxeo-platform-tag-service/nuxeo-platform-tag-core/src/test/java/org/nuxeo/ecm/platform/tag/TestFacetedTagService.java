/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.tag;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_LIST;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.Deploy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test class for tag service based on facet
 *
 * @since 9.3
 */
public class TestFacetedTagService extends AbstractTestTagService {

    @Override
    protected void createTags() {
        DocumentModel file1 = session.getDocument(new PathRef("/file1"));
        DocumentModel file2 = session.getDocument(new PathRef("/file2"));

        Map<String, Serializable> tag1 = new HashMap<>();
        tag1.put("label", "tag1");
        tag1.put("username", "Administrator");

        Map<String, Serializable> tag2 = new HashMap<>();
        tag2.put("label", "tag2");
        tag2.put("username", "Administrator");

        file1.setPropertyValue(TAG_LIST, (Serializable) Arrays.asList(tag1, tag2));
        file2.setPropertyValue(TAG_LIST, (Serializable) Arrays.asList(tag1));

        session.saveDocument(file1);
        session.saveDocument(file2);
        session.save();
    }

    @Test
    public void testTagOnDocumentWithMissingFacet() {

        DocumentModel relation = session.createDocumentModel("/", "foo", "Relation");
        relation = session.createDocument(relation);
        session.save();

        tagService.tag(session, relation.getId(), "tag");

        assertEquals(0, tagService.getTags(session, relation.getId()).size());
    }

    @Test
    public void testNoVersioningFacetedTagFilter() {

        DocumentModel note = session.createDocumentModel("/", "note", "TestNote");
        note.setPropertyValue("test:stringArray", new String[] { "test1" });
        note.setPropertyValue("test:stringList", (Serializable) Arrays.asList("test1"));
        Map<String, Serializable> complex = new HashMap<>();
        complex.put("foo", "test");
        complex.put("bar", (Serializable) Arrays.asList("test1"));
        note.setPropertyValue("test:complex", (Serializable) complex);
        note = session.createDocument(note);
        session.save();

        assertEquals("0.1", note.getVersionLabel());

        Map<String, Serializable> tag = new HashMap<>();
        tag.put("label", "tag");
        tag.put("username", "Administrator");

        note.setPropertyValue(TAG_LIST, (Serializable) Arrays.asList(tag));
        // Disable auto checkout as we are only editing tags
        note.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
        note = session.saveDocument(note);

        assertEquals("0.1", note.getVersionLabel());
        assertEquals(1, tagService.getTags(session, note.getId()).size());

        Map<String, Serializable> otherTag = new HashMap<>();
        otherTag.put("label", "otherTag");
        otherTag.put("username", "Administrator");
        note.setPropertyValue(TAG_LIST, (Serializable) Arrays.asList(tag, otherTag));
        // Edit an other simple property of the document to trigger auto versioning
        note.setPropertyValue("dc:title", "testNote");
        note = session.saveDocument(note);

        assertEquals("0.2", note.getVersionLabel());
        assertEquals(2, tagService.getTags(session, note.getId()).size());

        note.setPropertyValue(TAG_LIST, new ArrayList<>());
        // Edit an array property of the document to trigger auto versioning
        note.setPropertyValue("test:stringArray", new String[] { "test1", "test2" });
        note = session.saveDocument(note);

        assertEquals("0.3", note.getVersionLabel());
        assertEquals(0, tagService.getTags(session, note.getId()).size());

        note.setPropertyValue(TAG_LIST, (Serializable) Arrays.asList(tag));
        // Edit a list property of the document to trigger auto versioning
        note.setPropertyValue("test:stringList", (Serializable) Arrays.asList("test1", "test2", "test3"));
        note = session.saveDocument(note);

        assertEquals("0.4", note.getVersionLabel());
        assertEquals(1, tagService.getTags(session, note.getId()).size());

        note.setPropertyValue(TAG_LIST, (Serializable) Arrays.asList(tag, otherTag));
        // Edit a complex property of the document to trigger auto versioning
        Map<String, Serializable> othercomplex = new HashMap<>();
        othercomplex.put("foo", "othertest");
        othercomplex.put("bar", (Serializable) Arrays.asList("test1", "test2"));
        note.setPropertyValue("test:complex", (Serializable) othercomplex);
        note = session.saveDocument(note);

        assertEquals("0.5", note.getVersionLabel());
        assertEquals(2, tagService.getTags(session, note.getId()).size());
    }

    // start with a checked out doc
    @Test
    public void testNoVersioningFacetedTagFilterWithDifferentContributor() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.save();

        assertEquals("0.0", doc.getVersionLabel());
        assertEquals("Administrator", doc.getPropertyValue("dc:lastContributor"));

        // add a tag with a different user
        // we don't want a versioning policy like "collaborative-save" to be triggered and version the doc
        try (CloseableCoreSession joeSession = CoreInstance.openCoreSession(session.getRepositoryName(), "joe")) {
            tagService.tag(joeSession, doc.getId(), "mytag");
        }

        doc.refresh();
        assertEquals("0.0", doc.getVersionLabel()); // version unchanged
    }

    // start with a checked in doc
    @Test
    public void testNoVersioningFacetedTagFilterWithDifferentContributorCheckedIn() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.save();
        session.checkIn(doc.getRef(), VersioningOption.MAJOR, null);
        doc.refresh();

        assertEquals("1.0", doc.getVersionLabel());
        assertEquals("Administrator", doc.getPropertyValue("dc:lastContributor"));

        // add a tag with a different user
        // we don't want a versioning policy like "collaborative-save" to be triggered and version the doc
        try (CloseableCoreSession joeSession = CoreInstance.openCoreSession(session.getRepositoryName(), "joe")) {
            tagService.tag(joeSession, doc.getId(), "mytag");
        }

        doc.refresh();
        assertEquals("1.0", doc.getVersionLabel()); // version unchanged
    }

    @Test
    @Deploy("org.nuxeo.ecm.tag.tests:test-no-tag-sanitization-contrib.xml")
    public void testNoSanitization() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "foo", "File");
        file.setPropertyValue("dc:title", "File");
        file = session.createDocument(file);
        session.save();
        String fileId = file.getId();
        List<String> charToTests = Arrays.asList(" ", "\\", "/", "'");
        for (String charToTest : charToTests) {
            String tagLabel = charToTest + "my" + charToTest + "tag" + charToTest;
            String message = "tag " + tagLabel + " is invalid";
            // add tag
            tagService.tag(session, fileId, tagLabel);
            session.save();
            // find tag for doc
            Set<String> tags = tagService.getTags(session, fileId);
            assertTrue(message, tags.contains(tagLabel));
            // find suggestion
            Set<String> suggestions = tagService.getSuggestions(session, "%tag" + charToTest);
            assertTrue(message, suggestions.contains(tagLabel));
            suggestions = tagService.getSuggestions(session, charToTest + "my");
            assertTrue(message, suggestions.contains(tagLabel));
            // find documents with this tag
            List<String> taggedDocIds = tagService.getTagDocumentIds(session, tagLabel);
            assertEquals(message, singletonList(fileId), taggedDocIds);
            // remove tag
            tagService.untag(session, fileId, tagLabel);
            session.save();
            tags = tagService.getTags(session, fileId);
            assertTrue(message, tags.isEmpty());

            maybeSleep();
        }
    }

}
