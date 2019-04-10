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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

package org.nuxeo.drive.service.adapter;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.AbstractBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderFactory;

/**
 * Test {@link BlobHolderFactory} building a {@link BlobHolder} always throwing an exception.
 *
 * @since 9.1
 */
public class ExceptionBlobHolderFactory implements BlobHolderFactory {

    public static final String EXCEPTION_MESSAGE = "This is a deliberate exception thrown for test purpose";

    @Override
    public BlobHolder getBlobHolder(DocumentModel doc) {
        return new ExceptionBlobHolder();
    }

    protected class ExceptionBlobHolder extends AbstractBlobHolder {

        @Override
        public Serializable getProperty(String name) {
            throw new NuxeoException(EXCEPTION_MESSAGE);
        }

        @Override
        public Map<String, Serializable> getProperties() {
            throw new NuxeoException(EXCEPTION_MESSAGE);
        }

        @Override
        public Blob getBlob() {
            throw new NuxeoException(EXCEPTION_MESSAGE);
        }

        @Override
        protected String getBasePath() {
            throw new NuxeoException(EXCEPTION_MESSAGE);
        }

        @Override
        public Calendar getModificationDate() {
            throw new NuxeoException(EXCEPTION_MESSAGE);
        }

    }

}
