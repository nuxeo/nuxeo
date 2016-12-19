/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDocumentModel {

    @Inject
    protected CoreSession session;

    /**
     * Tests on a DocumentModel that hasn't been created in the session yet.
     */
    @Test
    public void testDocumentModelNotYetCreated() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());
        doc.refresh();
    }

    @Test
    public void testContextDataOfCreatedDocument() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.putContextData("key", "value");
        doc = session.createDocument(doc);
        assertEquals(doc.getContextData("key"), "value");
    }

    @Test
    public void testDetachAttach() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        String sid = doc.getSessionId();
        assertNotNull(sid);
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertEquals("0.0", doc.getVersionLabel());

        doc.detach(false);
        doc.prefetchCurrentLifecycleState(null);
        assertNull(doc.getSessionId());
        assertNull(doc.getCurrentLifeCycleState());
        assertNull(doc.getVersionLabel());

        doc.attach(sid);
        session.saveDocument(doc);
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertEquals("0.0", doc.getVersionLabel());

        try {
            doc.attach("fakesid");
            fail("Should not allow attach");
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Cannot attach a document that is already attached"));
        }
    }

    /**
     * Verifies that checked out state, lifecycle state and lock info are stored on a detached document.
     */
    @Test
    public void testDetachedSystemInfo() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        doc.setLock();

        // refetch to clear lock info
        doc = session.getDocument(new IdRef(doc.getId()));
        // check in
        doc.checkIn(VersioningOption.MAJOR, null);
        // clear lifecycle info
        doc.prefetchCurrentLifecycleState(null);

        doc.detach(true);
        assertFalse(doc.isCheckedOut());
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertNotNull(doc.getLockInfo());

        // refetch to clear lock info
        doc = session.getDocument(new IdRef(doc.getId()));
        // checkout
        doc.checkOut();
        // clear lifecycle info
        doc.prefetchCurrentLifecycleState(null);

        doc.detach(true);
        assertTrue(doc.isCheckedOut());
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertNotNull(doc.getLockInfo());
    }

    @Test
    public void testDocumentLiveSerialization() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        doc.getProperty("common:icon").setValue("prefetched");
        doc.getProperty("dublincore:language").setValue("not-prefetch");
        doc = session.saveDocument(doc);

        Assertions.assertThat(doc.getCoreSession()).isNotNull();

        doc = SerializationUtils.clone(doc);

        assertThat(doc.getCoreSession()).isNull();
        assertThat(doc.getName()).isEqualTo("doc");
        assertThat(doc.getProperty("common:icon").getValue(String.class)).isEqualTo("prefetched");
        assertThat(doc.getProperty("dublincore:language").getValue(String.class)).isEqualTo("not-prefetch");
    }

    @Test
    public void testDocumentDirtySerialization() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        doc.getProperty("dublincore:source").setValue("Source");

        assertThat(doc.isDirty()).isTrue();

        doc = SerializationUtils.clone(doc);

        assertThat(doc.getCoreSession()).isNull();
        assertThat(doc.getProperty("dublincore:source").getValue(String.class)).isEqualTo("Source");
    }

    @Test
    public void testDocumentDeletedSerialization() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        doc.getProperty("dublincore:title").setValue("doc"); // prefetch
        doc.getProperty("dublincore:source").setValue("Source"); // not prefetch

        session.removeDocument(doc.getRef());

        assertThat(session.exists(doc.getRef())).isFalse();

        doc = SerializationUtils.clone(doc);

        assertThat(doc.getCoreSession()).isNull();
        assertThat(doc.getProperty("dublincore:title").getValue(String.class)).isEqualTo("doc");
        assertThat(doc.getProperty("dublincore:source").getValue(String.class)).isEqualTo("Source");
    }

    @Test
    public void testDetachedDocumentSerialization() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        doc.getProperty("dublincore:source").setValue("Source");
        doc.detach(false);

        assertThat(doc.getCoreSession()).isNull();

        doc = SerializationUtils.clone(doc);

        assertThat(doc.getCoreSession()).isNull();
        assertThat(doc.getName()).isEqualTo("doc");
        assertThat(doc.getProperty("dublincore:source").getValue(String.class)).isEqualTo("Source");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidSlashOnCreate() throws Exception {
        session.createDocumentModel("/", "doc/doc", "File");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidSlashOnMove() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.move(doc.getRef(), new PathRef("/"), "toto/tata");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidSlashOnCopy() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.copy(doc.getRef(), new PathRef("/"), "toto/tata");
    }

}
