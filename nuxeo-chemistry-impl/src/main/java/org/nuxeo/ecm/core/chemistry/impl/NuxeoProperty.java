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
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * A live property of an object.
 *
 * @author Florent Guillaume
 */
public class NuxeoProperty implements Property {

    private final org.nuxeo.ecm.core.api.model.Property prop;

    private final PropertyDefinition propertyDefinition;

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

    public NuxeoProperty(org.nuxeo.ecm.core.api.model.Property prop,
            PropertyDefinition propertyDefinition) {
        this.prop = prop;
        this.propertyDefinition = propertyDefinition;
    }

    /**
     * Factory for a new Property.
     */
    public static Property getProperty(DocumentModel doc, Type type,
            String name, CoreSession session) throws ClientException {
        PropertyDefinition pd = type.getPropertyDefinition(name);
        if (pd == null) {
            throw new IllegalArgumentException(name);
        }
        Serializable value;
        org.nuxeo.ecm.core.api.model.Property prop = null;
        if (Property.ID.equals(name)) {
            value = doc.getId();
        } else if (Property.TYPE_ID.equals(name)) {
            value = doc.getType();
        } else if (Property.BASE_TYPE_ID.equals(name)) {
            if (doc.isFolder()) {
                value = BaseType.FOLDER.getId();
            } else {
                value = BaseType.DOCUMENT.getId();
            }
        } else if (Property.CREATED_BY.equals(name)) {
            value = doc.getPropertyValue(NuxeoType.NX_DC_CREATOR);
        } else if (Property.CREATION_DATE.equals(name)) {
            value = doc.getPropertyValue(NuxeoType.NX_DC_CREATED);
        } else if (Property.LAST_MODIFIED_BY.equals(name)) {
            value = doc.getPropertyValue("dc:contributors");
            if (value == null || ((String[]) value).length == 0) {
                value = null;
            } else {
                value = ((String[]) value)[0];
            }
        } else if (Property.LAST_MODIFICATION_DATE.equals(name)) {
            value = doc.getPropertyValue(NuxeoType.NX_DC_MODIFIED);
        } else if (Property.CHANGE_TOKEN.equals(name)) {
            value = null;
        } else if (Property.NAME.equals(name)) {
            return new NameProperty(doc, pd);
        } else if (Property.IS_IMMUTABLE.equals(name)) {
            value = Boolean.FALSE; // TODO check write permission
        } else if (Property.IS_LATEST_VERSION.equals(name)) {
            value = Boolean.TRUE;
        } else if (Property.IS_MAJOR_VERSION.equals(name)) {
            value = Boolean.FALSE;
        } else if (Property.IS_LATEST_MAJOR_VERSION.equals(name)) {
            value = Boolean.FALSE;
        } else if (Property.VERSION_LABEL.equals(name)) {
            // value = doc.getVersionLabel();
            value = null;
        } else if (Property.VERSION_SERIES_ID.equals(name)) {
            value = doc.getId();
        } else if (Property.IS_VERSION_SERIES_CHECKED_OUT.equals(name)) {
            value = Boolean.FALSE;
        } else if (Property.VERSION_SERIES_CHECKED_OUT_BY.equals(name)) {
            value = null;
        } else if (Property.VERSION_SERIES_CHECKED_OUT_ID.equals(name)) {
            value = null;
        } else if (Property.CHECK_IN_COMMENT.equals(name)) {
            value = null;
        } else if (Property.CONTENT_STREAM_LENGTH.equals(name)) {
            ContentStream contentStream = getContentStream(doc);
            value = contentStream == null ? null
                    : Integer.valueOf((int) contentStream.getLength());
        } else if (Property.CONTENT_STREAM_MIME_TYPE.equals(name)) {
            ContentStream contentStream = getContentStream(doc);
            value = contentStream == null ? null : contentStream.getMimeType();
        } else if (Property.CONTENT_STREAM_FILE_NAME.equals(name)) {
            ContentStream contentStream = getContentStream(doc);
            value = contentStream == null ? null : contentStream.getFileName();
        } else if (Property.CONTENT_STREAM_ID.equals(name)) {
            value = null;
        } else if (Property.PARENT_ID.equals(name)) {
            // TODO cache this
            DocumentRef parentRef = doc.getParentRef();
            if (parentRef == null) {
                value = null;
            } else {
                value = session.getDocument(parentRef).getId();
            }
        } else if (Property.PATH.equals(name)) {
            value = doc.getPathAsString();
        } else if (Property.ALLOWED_CHILD_OBJECT_TYPE_IDS.equals(name)) {
            value = null;
        } else if (Property.SOURCE_ID.equals(name)) {
            value = null;
        } else if (Property.TARGET_ID.equals(name)) {
            value = null;
        } else if (Property.POLICY_TEXT.equals(name)) {
            value = null;
        } else if (pd.getUpdatability() == Updatability.READ_WRITE) {
            // read/write property
            value = null;
            prop = doc.getProperty(name);
        } else {
            // read-only property
            value = doc.getPropertyValue(name);
        }

        if (prop == null) {
            // read-only
            return new ReadOnlyProperty(value, pd);
        } else {
            return new NuxeoProperty(prop, pd);
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

    public PropertyDefinition getDefinition() {
        return propertyDefinition;
    }

    public Serializable getValue() {
        try {
            return prop.getValue();
        } catch (PropertyException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
    }

    public void setValue(Serializable value) {
        try {
            prop.setValue(value);
        } catch (PropertyException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
    }

    /**
     * A read-only property.
     */
    public static class ReadOnlyProperty implements Property {

        private final Serializable value;

        private final PropertyDefinition propertyDefinition;

        public ReadOnlyProperty(Serializable value,
                PropertyDefinition propertyDefinition) {
            this.value = value;
            this.propertyDefinition = propertyDefinition;
        }

        public PropertyDefinition getDefinition() {
            return propertyDefinition;
        }

        public Serializable getValue() {
            return value;
        }

        public void setValue(Serializable value) {
            throw new UnsupportedOperationException("Read-only property: "
                    + propertyDefinition.getId());
        }
    }

    /**
     * Property for NAME. Allows writing before the document is saved.
     */

    public static class NameProperty implements Property {

        private final DocumentModel doc;

        private final PropertyDefinition propertyDefinition;

        public NameProperty(DocumentModel doc,
                PropertyDefinition propertyDefinition) {
            this.doc = doc;
            this.propertyDefinition = propertyDefinition;
        }

        public PropertyDefinition getDefinition() {
            return propertyDefinition;
        }

        public Serializable getValue() {
            return doc.getName();
        }

        public void setValue(Serializable value) {
            if (doc.getId() != null) {
                throw new UnsupportedOperationException("Read-only property: "
                        + propertyDefinition.getId());
            }
            if (value == null || "".equals(value)) {
                throw new IllegalArgumentException("Illegal empty name");
            }
            doc.setPathInfo(doc.getPath().removeLastSegments(1).toString(),
                    (String) value);
        }
    }

}
