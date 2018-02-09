/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.blobholder;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class TestBlobHolder extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployContrib("org.nuxeo.ecm.core.api", "OSGI-INF/test-blobholder-contrib.xml");
    }

    @Test
    public void testBlobHolderFactoryContribution() {
        DocumentModel doc = new DocumentModelImpl("TestType");
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        assertTrue(bh instanceof DocumentTypeBlobHolder);

        doc = new DocumentModelImpl("TestType");
        doc.addFacet("TestFacet");
        bh = doc.getAdapter(BlobHolder.class);
        assertTrue(bh instanceof DocumentTypeBlobHolder);

        doc = new DocumentModelImpl("AnotherType");
        doc.addFacet("TestFacet");
        bh = doc.getAdapter(BlobHolder.class);
        assertTrue(bh instanceof FacetBlobHolder);
    }

    public static class DocumentTypeBlobHolderFactory implements BlobHolderFactory {

        @Override
        public BlobHolder getBlobHolder(DocumentModel doc) {
            return new DocumentTypeBlobHolder();
        }

    }

    public static class DocumentTypeBlobHolder extends AbstractBlobHolder {

        @Override
        public Blob getBlob() {
            return null;
        }

        @Override
        protected String getBasePath() {
            return null;
        }

        @Override
        public Calendar getModificationDate() {
            return null;
        }

        @Override
        public Serializable getProperty(String name) {
            return null;
        }

        @Override
        public Map<String, Serializable> getProperties() {
            return null;
        }
    }

    public static class FacetBlobHolderFactory implements BlobHolderFactory {

        @Override
        public BlobHolder getBlobHolder(DocumentModel doc) {
            return new FacetBlobHolder();
        }

    }

    public static class FacetBlobHolder extends AbstractBlobHolder {

        @Override
        public Blob getBlob() {
            return null;
        }

        @Override
        protected String getBasePath() {
            return null;
        }

        @Override
        public Calendar getModificationDate() {
            return null;
        }

        @Override
        public Serializable getProperty(String name) {
            return null;
        }

        @Override
        public Map<String, Serializable> getProperties() {
            return null;
        }
    }
}
