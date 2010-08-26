/*
 * Copyright 2009-2010 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyId;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisStreamNotSupportedException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * A live property of an object.
 */
public class NuxeoPropertyData<T> extends NuxeoPropertyDataBase<T> {

    protected final String name;

    protected final boolean readOnly;

    // TODO unused
    public static final Map<String, String> propertyNameToNXQL;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put(PropertyIds.OBJECT_ID, NXQL.ECM_UUID);
        map.put(PropertyIds.OBJECT_TYPE_ID, NXQL.ECM_PRIMARYTYPE);
        map.put(PropertyIds.PARENT_ID, NXQL.ECM_PARENTID);
        map.put(PropertyIds.NAME, NXQL.ECM_NAME);
        map.put(PropertyIds.CREATED_BY, NuxeoTypeHelper.NX_DC_CREATOR);
        map.put(PropertyIds.CREATION_DATE, NuxeoTypeHelper.NX_DC_CREATED);
        map.put(PropertyIds.LAST_MODIFIED_BY, "dc:contributors");
        map.put(PropertyIds.LAST_MODIFICATION_DATE,
                NuxeoTypeHelper.NX_DC_MODIFIED);
        propertyNameToNXQL = Collections.unmodifiableMap(map);
    }

    public NuxeoPropertyData(PropertyDefinition<T> propertyDefinition,
            DocumentModel doc, String name, boolean readOnly) {
        super(propertyDefinition, doc);
        this.name = name;
        this.readOnly = readOnly;
    }

    /**
     * Factory for a new Property.
     */
    protected static <U> PropertyData<U> construct(NuxeoObjectData data,
            PropertyDefinition<U> pd) {
        DocumentModel doc = data.doc;
        String name = pd.getId();
        if (PropertyIds.OBJECT_ID.equals(name)) {
            return (PropertyData<U>) new FixedPropertyIdData(
                    (PropertyDefinition<String>) pd, doc.getId());
        } else if (PropertyIds.OBJECT_TYPE_ID.equals(name)) {
            return (PropertyData<U>) new FixedPropertyIdData(
                    (PropertyDefinition<String>) pd,
                    NuxeoTypeHelper.mappedId(doc.getType()));
        } else if (PropertyIds.BASE_TYPE_ID.equals(name)) {
            return (PropertyData<U>) new FixedPropertyIdData(
                    (PropertyDefinition<String>) pd,
                    doc.isFolder() ? BaseTypeId.CMIS_FOLDER.value()
                            : BaseTypeId.CMIS_DOCUMENT.value());
        } else if (PropertyIds.CREATED_BY.equals(name)) {
            return new NuxeoPropertyData(pd, doc,
                    NuxeoTypeHelper.NX_DC_CREATOR, true);
        } else if (PropertyIds.CREATION_DATE.equals(name)) {
            return new NuxeoPropertyData(pd, doc,
                    NuxeoTypeHelper.NX_DC_CREATED, true);
        } else if (PropertyIds.LAST_MODIFIED_BY.equals(name)) {
            return (PropertyData<U>) new LastModifiedByPropertyData(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.LAST_MODIFICATION_DATE.equals(name)) {
            return new NuxeoPropertyData(pd, doc,
                    NuxeoTypeHelper.NX_DC_MODIFIED, true);
        } else if (PropertyIds.CHANGE_TOKEN.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.NAME.equals(name)) {
            return (PropertyData<U>) new NamePropertyData(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.IS_IMMUTABLE.equals(name)) {
            return new FixedPropertyData(pd, Boolean.FALSE); // TODO check write
        } else if (PropertyIds.IS_LATEST_VERSION.equals(name)) {
            return new FixedPropertyData(pd, Boolean.TRUE);
        } else if (PropertyIds.IS_MAJOR_VERSION.equals(name)) {
            return new FixedPropertyData(pd, Boolean.FALSE);
        } else if (PropertyIds.IS_LATEST_MAJOR_VERSION.equals(name)) {
            return new FixedPropertyData(pd, Boolean.FALSE);
        } else if (PropertyIds.VERSION_LABEL.equals(name)) {
            // value = doc.getVersionLabel();
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.VERSION_SERIES_ID.equals(name)) {
            return new FixedPropertyData(pd, doc.getId());
        } else if (PropertyIds.IS_VERSION_SERIES_CHECKED_OUT.equals(name)) {
            return new FixedPropertyData(pd, Boolean.FALSE);
        } else if (PropertyIds.VERSION_SERIES_CHECKED_OUT_BY.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.VERSION_SERIES_CHECKED_OUT_ID.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.CHECKIN_COMMENT.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.CONTENT_STREAM_LENGTH.equals(name)) {
            return (PropertyData<U>) new ContentStreamLengthPropertyData(
                    (PropertyDefinition<BigInteger>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_MIME_TYPE.equals(name)) {
            return (PropertyData<U>) new ContentStreamMimeTypePropertyData(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_FILE_NAME.equals(name)) {
            return (PropertyData<U>) new ContentStreamFileNamePropertyData(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_ID.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.PARENT_ID.equals(name)) {
            return (PropertyData<U>) new ParentIdPropertyData(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.PATH.equals(name)) {
            return (PropertyData<U>) new PathPropertyData(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.SOURCE_ID.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.TARGET_ID.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else if (PropertyIds.POLICY_TEXT.equals(name)) {
            return new FixedPropertyData(pd, null);
        } else {
            boolean readOnly = pd.getUpdatability() != Updatability.READWRITE;
            // TODO WHEN_CHECKED_OUT, ON_CREATE
            return new NuxeoPropertyData(pd, doc, name, readOnly);
        }
    }

    protected static ContentStream getContentStream(DocumentModel doc)
            throws CmisRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            throw new CmisStreamNotSupportedException();
        }
        Blob blob;
        try {
            blob = blobHolder.getBlob();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
        return blob == null ? null : new NuxeoContentStream(blob);
    }

    protected static void setContentStream(DocumentModel doc,
            ContentStream contentStream, boolean overwrite) throws IOException,
            CmisContentAlreadyExistsException, CmisRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            throw new CmisContentAlreadyExistsException();
        }
        if (!overwrite) {
            Blob blob;
            try {
                blob = blobHolder.getBlob();
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
            if (blob != null) {
                throw new CmisContentAlreadyExistsException();
            }
        }
        Blob blob = contentStream == null ? null : new InputStreamBlob(
                contentStream.getStream(), contentStream.getMimeType(), null,
                contentStream.getFileName(), null);
        try {
            blobHolder.setBlob(blob);
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public T getFirstValue() {
        try {
            Serializable value = doc.getPropertyValue(name);
            // conversion from Nuxeo types to CMIS ones
            if (value instanceof Double) {
                value = BigDecimal.valueOf(((Double) value).doubleValue());
            } else if (value instanceof Integer) {
                value = BigDecimal.valueOf(((Integer) value).intValue());
            }
            return (T) value;
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void setValue(Object value) {
        try {
            if (readOnly) {
                super.setValue(value);
            } else {
                if (value instanceof BigDecimal) {
                    value = Double.valueOf(((BigDecimal) value).doubleValue());
                }
                // TODO many more checks (multi, etc.)
                doc.setPropertyValue(name, (Serializable) value);
            }
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    protected static Blob getBlob(DocumentModel doc)
            throws CmisRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            throw new CmisStreamNotSupportedException();
        }
        try {
            return blobHolder.getBlob();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    // protected static void setContentStream(DocumentModel doc,
    // ContentStream contentStream, boolean overwrite) throws IOException,
    // CmisContentAlreadyExistsException, CmisRuntimeException {
    // BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
    // if (blobHolder == null) {
    // throw new CmisStreamNotSupportedException();
    // }
    // if (!overwrite) {
    // Blob blob;
    // try {
    // blob = blobHolder.getBlob();
    // } catch (ClientException e) {
    // throw new CmisRuntimeException(e.toString(), e);
    // }
    // if (blob != null) {
    // throw new CmisContentAlreadyExistsException();
    // }
    // }
    // Blob blob = contentStream == null ? null : new InputStreamBlob(
    // contentStream.getStream(), contentStream.getMimeType(), null,
    // contentStream.getFileName(), null);
    // try {
    // blobHolder.setBlob(blob);
    // } catch (ClientException e) {
    // throw new CmisRuntimeException(e.toString(), e);
    // }
    // }

    /**
     * A fixed property.
     */
    protected static class FixedPropertyData<T> extends
            NuxeoPropertyDataBase<T> {

        protected final T value;

        protected FixedPropertyData(PropertyDefinition<T> propertyDefinition,
                T value) {
            super(propertyDefinition, null);
            this.value = value;
        }

        @Override
        public T getFirstValue() {
            return value;
        }
    }

    /**
     * A fixed ID property.
     */
    protected static class FixedPropertyIdData extends
            FixedPropertyData<String> implements PropertyId {

        protected final String value;

        protected FixedPropertyIdData(
                PropertyDefinition<String> propertyDefinition, String value) {
            super(propertyDefinition, null);
            this.value = value;
        }

        @Override
        public String getFirstValue() {
            return value;
        }
    }

    /**
     * Property for cmis:path.
     */
    protected static class PathPropertyData extends
            NuxeoPropertyDataBase<String> {

        protected PathPropertyData(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            String path = doc.getPathAsString();
            return path == null ? "" : path;
        }
    }

    /**
     * Property for cmis:parentId.
     */
    protected static class ParentIdPropertyData extends
            NuxeoPropertyDataBase<String> {

        protected ParentIdPropertyData(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            if (doc.getName() == null) {
                return null;
            } else {
                DocumentRef parentRef = doc.getParentRef();
                if (parentRef instanceof IdRef) {
                    return ((IdRef) parentRef).value;
                } else {
                    try {
                        return doc.getCoreSession().getDocument(parentRef).getId();
                    } catch (ClientException e) {
                        throw new CmisRuntimeException(e.toString(), e);
                    }
                }
            }
        }
    }

    /**
     * Property for cmis:lastModifiedBy.
     */
    protected static class LastModifiedByPropertyData extends
            NuxeoPropertyDataBase<String> {

        protected LastModifiedByPropertyData(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            try {
                String[] value = (String[]) doc.getPropertyValue("dc:contributors");
                if (value == null || value.length == 0) {
                    return null;
                } else {
                    return value[0];
                }
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:contentStreamFileName.
     */
    protected static class ContentStreamFileNamePropertyData extends
            NuxeoPropertyDataBase<String> {

        protected ContentStreamFileNamePropertyData(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            Blob blob = getBlob(doc);
            return blob == null ? null : blob.getFilename();
        }

        // @Override
        // public void setValue(Serializable value) {
        // BlobHolder blobHolder = docHolder.getDocumentModel().getAdapter(
        // BlobHolder.class);
        // if (blobHolder == null) {
        // throw new StreamNotSupportedException();
        // }
        // Blob blob;
        // try {
        // blob = blobHolder.getBlob();
        // } catch (ClientException e) {
        // throw new CmisRuntimeException(e.toString(), e);
        // }
        // if (blob != null) {
        // blob.setFilename((String) value);
        // }
        // }
    }

    /**
     * Property for cmis:contentStreamLength.
     */
    protected static class ContentStreamLengthPropertyData extends
            NuxeoPropertyDataBase<BigInteger> {

        protected ContentStreamLengthPropertyData(
                PropertyDefinition<BigInteger> propertyDefinition,
                DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public BigInteger getFirstValue() {
            Blob blob = getBlob(doc);
            return blob == null ? null : BigInteger.valueOf(blob.getLength());
        }
    }

    /**
     * Property for cmis:contentMimeTypeLength.
     */
    protected static class ContentStreamMimeTypePropertyData extends
            NuxeoPropertyDataBase<String> {

        protected ContentStreamMimeTypePropertyData(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            Blob blob = getBlob(doc);
            return blob == null ? null : blob.getMimeType();
        }
    }

    /**
     * Property for cmis:name.
     */
    protected static class NamePropertyData extends
            NuxeoPropertyDataBase<String> {

        protected NamePropertyData(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            String name;
            try {
                name = doc.getTitle();
            } catch (ClientException e) {
                name = null;
            }
            return name == null ? "" : name; // Nuxeo root has null name
        }

        @Override
        public void setValue(Object value) {
            try {
                doc.setPropertyValue(NuxeoTypeHelper.NX_DC_TITLE,
                        (String) value);
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }
}
