/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class TestBlobHolder extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-blobholder-contrib.xml");
    }

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

    public static class DocumentTypeBlobHolderFactory implements
            BlobHolderFactory {

        @Override
        public BlobHolder getBlobHolder(DocumentModel doc) {
            return new DocumentTypeBlobHolder();
        }

    }

    public static class DocumentTypeBlobHolder extends AbstractBlobHolder {

        @Override
        public Blob getBlob() throws ClientException {
            return null;
        }

        @Override
        protected String getBasePath() {
            return null;
        }

        @Override
        public Calendar getModificationDate() throws ClientException {
            return null;
        }

        @Override
        public Serializable getProperty(String name) throws ClientException {
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
        public Blob getBlob() throws ClientException {
            return null;
        }

        @Override
        protected String getBasePath() {
            return null;
        }

        @Override
        public Calendar getModificationDate() throws ClientException {
            return null;
        }

        @Override
        public Serializable getProperty(String name) throws ClientException {
            return null;
        }

        @Override
        public Map<String, Serializable> getProperties() {
            return null;
        }
    }
}
