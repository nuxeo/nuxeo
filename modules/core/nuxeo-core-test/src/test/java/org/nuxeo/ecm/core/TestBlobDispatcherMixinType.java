/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests blob dispatch based on a combination of {@code blob:xpath} and {@code ecm:mixinType} clauses (AND), and that
 * blobs are automatically re-dispatched when a document's facets change.
 * <p>
 * The dispatcher under test models a multi-tier storage scenario:
 * <ul>
 * <li>{@code file:content} on an <em>Archive</em>-faceted document → {@code archive} provider</li>
 * <li>{@code file:content} on any other document → {@code tiered} provider</li>
 * <li>everything else (attachments, thumbnails, …) → {@code test} provider</li>
 * </ul>
 *
 * @since 2025.24
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-blob-dispatcher-mixin-type.xml")
public class TestBlobDispatcherMixinType {

    // MD5 of "foo" / "bar", consistent with TestBlobDispatcherRecord
    protected static final String FOO_DIGEST = "acbd18db4cc2f85cedef654fccc4a4d8";

    protected static final String BAR_DIGEST = "37b51d194a7513e45b56f6524f2d51f2";

    // Expected keys per provider (LocalBlobProvider uses "<providerId>:<digest>" as key)
    protected static final String FOO_TIERED_KEY = "tiered:" + FOO_DIGEST;

    protected static final String FOO_ARCHIVE_KEY = "archive:" + FOO_DIGEST;

    protected static final String BAR_TEST_KEY = "test:" + BAR_DIGEST;

    @Inject
    protected CoreSession session;

    /**
     * Rule: {@code blob:xpath=content} (no facet) → {@code tiered} provider.
     */
    @Test
    public void testMainContentDispatchedToTieredByDefault() throws Exception {
        var doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("foo", "text/plain"));
        doc = session.createDocument(doc);

        var blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("main content without archive facet should be in tiered provider", FOO_TIERED_KEY,
                ((ManagedBlob) blob).getKey());
    }

    /**
     * Rule: {@code blob:xpath=content,ecm:mixinType=Archive} → {@code archive} provider. The AND clause matches only
     * when both conditions hold simultaneously.
     */
    @Test
    public void testMainContentOfArchiveDocDispatchedToArchive() throws Exception {
        var doc = session.createDocumentModel("/", "doc", "File");
        doc.addFacet("Archive");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("foo", "text/plain"));
        doc = session.createDocument(doc);

        var blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("main content of archive-faceted doc should be in archive provider", FOO_ARCHIVE_KEY,
                ((ManagedBlob) blob).getKey());
    }

    /**
     * Attachments (blob:xpath != content) are never matched by the first two rules and always land in the default
     * {@code test} provider, regardless of the document's facets.
     */
    @Test
    public void testAttachmentAlwaysInDefaultProvider() throws Exception {
        // without archive facet
        var doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("files:files",
                (Serializable) List.of(Map.of("file", Blobs.createBlob("bar", "text/plain"))));
        doc = session.createDocument(doc);

        var blob = (Blob) doc.getPropertyValue("files:files/0/file");
        assertEquals("attachment without facet should be in default provider", BAR_TEST_KEY,
                ((ManagedBlob) blob).getKey());

        // also with archive facet: attachment must still be in the default provider
        doc.addFacet("Archive");
        doc = session.saveDocument(doc);

        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("files:files/0/file");
        assertEquals("attachment of archive-faceted doc should still be in default provider", BAR_TEST_KEY,
                ((ManagedBlob) blob).getKey());
    }

    /**
     * When the {@code Archive} facet is added to a document, the {@code file:content} blob must be re-dispatched from
     * {@code tiered} to {@code archive} (AND clause now satisfied).
     */
    @Test
    public void testMainContentRedispatchedToArchiveOnFacetAdd() throws Exception {
        // start without the facet: main content goes to tiered
        var doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("foo", "text/plain"));
        doc = session.createDocument(doc);

        var blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(FOO_TIERED_KEY, ((ManagedBlob) blob).getKey());

        // add the archive facet: AND clause now satisfied, blob must move to archive provider
        doc.addFacet("Archive");
        doc = session.saveDocument(doc);

        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("main content should move to archive provider after facet add", FOO_ARCHIVE_KEY,
                ((ManagedBlob) blob).getKey());
    }

    /**
     * When the {@code Archive} facet is removed, the {@code file:content} blob must be re-dispatched from
     * {@code archive} back to {@code tiered} (AND clause no longer satisfied, xpath-only rule applies).
     */
    @Test
    public void testMainContentRedispatchedToTieredOnFacetRemove() throws Exception {
        // start with the archive facet: main content goes to archive
        var doc = session.createDocumentModel("/", "doc", "File");
        doc.addFacet("Archive");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("foo", "text/plain"));
        doc = session.createDocument(doc);

        var blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals(FOO_ARCHIVE_KEY, ((ManagedBlob) blob).getKey());

        // remove the archive facet: AND clause no longer satisfied, blob must move back to tiered
        doc.removeFacet("Archive");
        doc = session.saveDocument(doc);

        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("main content should move back to tiered provider after facet remove", FOO_TIERED_KEY,
                ((ManagedBlob) blob).getKey());
    }

    /**
     * A save that changes only document metadata (no facet change) must not trigger any blob re-dispatch.
     */
    @Test
    public void testBlobNotRedispatchedWhenNoFacetChange() throws Exception {
        var doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("foo", "text/plain"));
        doc = session.createDocument(doc);

        doc.setPropertyValue("dc:title", "Updated title");
        doc = session.saveDocument(doc);

        doc = session.getDocument(doc.getRef());
        var blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("blob should stay in tiered provider when only metadata changes", FOO_TIERED_KEY,
                ((ManagedBlob) blob).getKey());
    }

}
