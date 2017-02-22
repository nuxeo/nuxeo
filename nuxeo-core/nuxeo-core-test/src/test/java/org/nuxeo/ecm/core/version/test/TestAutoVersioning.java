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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.version.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestAutoVersioning {

    @Inject
    private CoreSession session;

    @Test
    public void testInitialVersion() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("2.0", doc.getVersionLabel());
    }

    @Test
    public void testAlwaysVersionMinor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.2", doc.getVersionLabel());
    }

    @Test
    public void testAlwaysVersionMajor() {
        // No initial state defined by policy
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("1.0", doc.getVersionLabel());

        // an edition should create a version
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertEquals("2.0", doc.getVersionLabel());
    }

}
