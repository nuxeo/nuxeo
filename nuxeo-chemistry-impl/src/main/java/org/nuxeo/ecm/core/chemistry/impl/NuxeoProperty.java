/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.BaseType;
import org.apache.chemistry.CMISRuntimeException;
import org.apache.chemistry.ContentAlreadyExistsException;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Property;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.StreamNotSupportedException;
import org.apache.chemistry.Type;
import org.apache.chemistry.Updatability;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * A live property of an object.
 *
 * @author Florent Guillaume
 */
public class NuxeoProperty extends NuxeoPropertyBase {

    protected final String name;

    protected final boolean readOnly;

    public static final Map<String, String> propertyNameToNXQL;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put(Property.ID, NXQL.ECM_UUID);
        map.put(Property.TYPE_ID, NXQL.ECM_PRIMARYTYPE);
        map.put(Property.PARENT_ID, NXQL.ECM_PARENTID);
        map.put(Property.NAME, NXQL.ECM_NAME);
        map.put(Property.CREATED_BY, NuxeoType.NX_DC_CREATOR);
        map.put(Property.CREATION_DATE, NuxeoType.NX_DC_CREATED);
        map.put(Property.LAST_MODIFIED_BY, "dc:contributors");
        map.put(Property.LAST_MODIFICATION_DATE, NuxeoType.NX_DC_MODIFIED);
        propertyNameToNXQL = Collections.unmodifiableMap(map);
    }

    public NuxeoProperty(PropertyDefinition propertyDefinition,
            DocumentModelHolder docHolder, String name, boolean readOnly) {
        super(propertyDefinition, docHolder);
        this.name = name;
        this.readOnly = readOnly;
    }

    public Serializable getValue() {
        try {
            Serializable value = docHolder.getDocumentModel().getPropertyValue(name);
            if (value instanceof Double) {
                value = BigDecimal.valueOf(((Double) value).doubleValue());
            }
            return value;
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void setValue(Serializable value) {
        try {
            if (readOnly) {
                super.setValue(value);
            } else {
                if (value instanceof BigDecimal) {
                    value = Double.valueOf(((BigDecimal) value).doubleValue());
                }
                docHolder.getDocumentModel().setPropertyValue(name, value);
            }
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    /**
     * Factory for a new Property.
     */
    protected static Property construct(String name, Type type,
            DocumentModelHolder docHolder) {
        return construct(name, type.getPropertyDefinition(name), docHolder);
    }

    protected static Property construct(String name, PropertyDefinition pd,
            DocumentModelHolder docHolder) {
        if (pd == null) {
            throw new IllegalArgumentException(name);
        }
        DocumentModel doc = docHolder.getDocumentModel();
        if (Property.ID.equals(name)) {
            return new FixedProperty(pd, doc.getId());
        } else if (Property.TYPE_ID.equals(name)) {
            return new FixedProperty(pd, NuxeoType.mappedId(doc.getType()));
        } else if (Property.BASE_TYPE_ID.equals(name)) {
            return new FixedProperty(pd,
                    doc.isFolder() ? BaseType.FOLDER.getId()
                            : BaseType.DOCUMENT.getId());
        } else if (Property.CREATED_BY.equals(name)) {
            return new NuxeoProperty(pd, docHolder, NuxeoType.NX_DC_CREATOR,
                    true);
        } else if (Property.CREATION_DATE.equals(name)) {
            return new NuxeoProperty(pd, docHolder, NuxeoType.NX_DC_CREATED,
                    true);
        } else if (Property.LAST_MODIFIED_BY.equals(name)) {
            return new LastModifiedByProperty(pd, docHolder);
        } else if (Property.LAST_MODIFICATION_DATE.equals(name)) {
            return new NuxeoProperty(pd, docHolder, NuxeoType.NX_DC_MODIFIED,
                    true);
        } else if (Property.CHANGE_TOKEN.equals(name)) {
            return new FixedProperty(pd, null);
        } else if (Property.NAME.equals(name)) {
            return new NameProperty(pd, docHolder);
        } else if (Property.IS_IMMUTABLE.equals(name)) {
            return new FixedProperty(pd, Boolean.FALSE); // TODO check write
        } else if (Property.IS_LATEST_VERSION.equals(name)) {
            return new FixedProperty(pd, Boolean.TRUE);
        } else if (Property.IS_MAJOR_VERSION.equals(name)) {
            return new FixedProperty(pd, Boolean.FALSE);
        } else if (Property.IS_LATEST_MAJOR_VERSION.equals(name)) {
            return new FixedProperty(pd, Boolean.FALSE);
        } else if (Property.VERSION_LABEL.equals(name)) {
            // value = doc.getVersionLabel();
            return new FixedProperty(pd, null);
        } else if (Property.VERSION_SERIES_ID.equals(name)) {
            return new FixedProperty(pd, doc.getId());
        } else if (Property.IS_VERSION_SERIES_CHECKED_OUT.equals(name)) {
            return new FixedProperty(pd, Boolean.FALSE);
        } else if (Property.VERSION_SERIES_CHECKED_OUT_BY.equals(name)) {
            return new FixedProperty(pd, null);
        } else if (Property.VERSION_SERIES_CHECKED_OUT_ID.equals(name)) {
            return new FixedProperty(pd, null);
        } else if (Property.CHECK_IN_COMMENT.equals(name)) {
            return new FixedProperty(pd, null);
        } else if (Property.CONTENT_STREAM_LENGTH.equals(name)) {
            return new ContentStreamLengthProperty(pd, docHolder);
        } else if (Property.CONTENT_STREAM_MIME_TYPE.equals(name)) {
            return new ContentStreamMimeTypeProperty(pd, docHolder);
        } else if (Property.CONTENT_STREAM_FILE_NAME.equals(name)) {
            return new ContentStreamFileNameProperty(pd, docHolder);
        } else if (Property.CONTENT_STREAM_ID.equals(name)) {
            return new FixedProperty(pd, null);
        } else if (Property.PARENT_ID.equals(name)) {
            return new ParentIdProperty(pd, docHolder);
        } else if (Property.PATH.equals(name)) {
            return new PathProperty(pd, docHolder);
        } else if (Property.ALLOWED_CHILD_OBJECT_TYPE_IDS.equals(name)) {
            return new FixedProperty(pd, null);
        } else if (Property.SOURCE_ID.equals(name)) {
            return new FixedProperty(pd, null);
        } else if (Property.TARGET_ID.equals(name)) {
            return new FixedProperty(pd, null);
        } else if (Property.POLICY_TEXT.equals(name)) {
            return new FixedProperty(pd, null);
        } else {
            boolean readOnly = pd.getUpdatability() != Updatability.READ_WRITE;
            // TODO WHEN_CHECKED_OUT, ON_CREATE
            return new NuxeoProperty(pd, docHolder, name, readOnly);
        }
    }

    protected static ContentStream getContentStream(DocumentModel doc)
            throws CMISRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            throw new StreamNotSupportedException();
        }
        Blob blob;
        try {
            blob = blobHolder.getBlob();
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
        return blob == null ? null : new NuxeoContentStream(blob);
    }

    protected static void setContentStream(DocumentModel doc,
            ContentStream contentStream, boolean overwrite) throws IOException,
            ContentAlreadyExistsException, CMISRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            throw new StreamNotSupportedException();
        }
        if (!overwrite) {
            Blob blob;
            try {
                blob = blobHolder.getBlob();
            } catch (ClientException e) {
                throw new CMISRuntimeException(e.toString(), e);
            }
            if (blob != null) {
                throw new ContentAlreadyExistsException();
            }
        }
        Blob blob = contentStream == null ? null : new InputStreamBlob(
                contentStream.getStream(), contentStream.getMimeType(), null,
                contentStream.getFileName(), null);
        try {
            blobHolder.setBlob(blob);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

    /**
     * A fixed property.
     */
    protected static class FixedProperty implements Property {

        protected final PropertyDefinition propertyDefinition;

        protected final Serializable value;

        protected FixedProperty(PropertyDefinition propertyDefinition,
                Serializable value) {
            this.propertyDefinition = propertyDefinition;
            this.value = value;
        }

        public PropertyDefinition getDefinition() {
            return propertyDefinition;
        }

        public Serializable getValue() {
            return value;
        }

        public void setValue(Serializable v) {
            if (value == null && v == null) {
                return;
            }
            if (value != null && value.equals(v)) {
                return;
            }
            throw new UnsupportedOperationException("Read-only property: "
                    + propertyDefinition.getId());
        }
    }

    /**
     * Property for cmis:path.
     */
    protected static class PathProperty extends NuxeoPropertyBase {

        protected PathProperty(PropertyDefinition propertyDefinition,
                DocumentModelHolder docHolder) {
            super(propertyDefinition, docHolder);
        }

        public Serializable getValue() {
            String path = docHolder.getDocumentModel().getPathAsString();
            return path == null ? "" : path;
        }
    }

    /**
     * Property for cmis:parentId.
     */
    protected static class ParentIdProperty extends NuxeoPropertyBase {

        protected ParentIdProperty(PropertyDefinition propertyDefinition,
                DocumentModelHolder docHolder) {
            super(propertyDefinition, docHolder);
        }

        public Serializable getValue() {
            DocumentModel doc = docHolder.getDocumentModel();
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
                        throw new CMISRuntimeException(e.toString(), e);
                    }
                }
            }
        }
    }

    /**
     * Property for cmis:lastModifiedBy.
     */
    protected static class LastModifiedByProperty extends NuxeoPropertyBase {

        protected LastModifiedByProperty(PropertyDefinition propertyDefinition,
                DocumentModelHolder docHolder) {
            super(propertyDefinition, docHolder);
        }

        public Serializable getValue() {
            DocumentModel doc = docHolder.getDocumentModel();
            try {
                String[] value = (String[]) doc.getPropertyValue("dc:contributors");
                if (value == null || value.length == 0) {
                    return null;
                } else {
                    return value[0];
                }
            } catch (ClientException e) {
                throw new CMISRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:contentStreamFileName.
     */
    protected static class ContentStreamFileNameProperty extends
            NuxeoPropertyBase {

        protected ContentStreamFileNameProperty(
                PropertyDefinition propertyDefinition,
                DocumentModelHolder docHolder) {
            super(propertyDefinition, docHolder);
        }

        public Serializable getValue() {
            ContentStream cs = getContentStream(docHolder.getDocumentModel());
            return cs == null ? null : cs.getFileName();
        }

        @Override
        public void setValue(Serializable value) {
            BlobHolder blobHolder = docHolder.getDocumentModel().getAdapter(
                    BlobHolder.class);
            if (blobHolder == null) {
                throw new StreamNotSupportedException();
            }
            Blob blob;
            try {
                blob = blobHolder.getBlob();
            } catch (ClientException e) {
                throw new CMISRuntimeException(e.toString(), e);
            }
            if (blob != null) {
                blob.setFilename((String) value);
            }
        }
    }

    /**
     * Property for cmis:contentStreamLength.
     */
    protected static class ContentStreamLengthProperty extends
            NuxeoPropertyBase {

        protected ContentStreamLengthProperty(
                PropertyDefinition propertyDefinition,
                DocumentModelHolder docHolder) {
            super(propertyDefinition, docHolder);
        }

        public Serializable getValue() {
            ContentStream cs = getContentStream(docHolder.getDocumentModel());
            return cs == null ? null : Integer.valueOf((int) cs.getLength());
        }
    }

    /**
     * Property for cmis:contentMimeTypeLength.
     */
    protected static class ContentStreamMimeTypeProperty extends
            NuxeoPropertyBase {

        protected ContentStreamMimeTypeProperty(
                PropertyDefinition propertyDefinition,
                DocumentModelHolder docHolder) {
            super(propertyDefinition, docHolder);
        }

        public Serializable getValue() {
            ContentStream cs = getContentStream(docHolder.getDocumentModel());
            return cs == null ? null : cs.getMimeType();
        }
    }

    /**
     * Property for cmis:name. Allows writing before the document is saved,
     * otherwise does a move.
     */
    protected static class NameProperty extends NuxeoPropertyBase {

        protected NameProperty(PropertyDefinition propertyDefinition,
                DocumentModelHolder docHolder) {
            super(propertyDefinition, docHolder);
        }

        public Serializable getValue() {
            String name = docHolder.getDocumentModel().getName();
            return name == null ? "" : name; // Nuxeo root has null name
        }

        @Override
        public void setValue(Serializable value) {
            Serializable name = getValue();
            if (name.equals(value)) {
                return;
            }
            if (value == null || "".equals(value)) {
                throw new IllegalArgumentException("Illegal empty name");
            }
            DocumentModel doc = docHolder.getDocumentModel();
            if (doc.getId() == null) {
                // not saved yet
                doc.setPathInfo(doc.getPath().removeLastSegments(1).toString(),
                        (String) value);
            } else {
                // do a move
                CoreSession session = doc.getCoreSession();
                DocumentModel newDoc;
                try {
                    newDoc = session.move(doc.getRef(), doc.getParentRef(),
                            (String) value);
                } catch (ClientException e) {
                    throw new CMISRuntimeException(e.toString(), e);
                }
                // set the new document
                docHolder.setDocumentModel(newDoc);
            }
        }
    }

}
