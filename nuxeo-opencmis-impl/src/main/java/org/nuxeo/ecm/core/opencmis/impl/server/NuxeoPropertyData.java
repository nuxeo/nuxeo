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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyBoolean;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyDateTime;
import org.apache.chemistry.opencmis.commons.data.PropertyDecimal;
import org.apache.chemistry.opencmis.commons.data.PropertyHtml;
import org.apache.chemistry.opencmis.commons.data.PropertyId;
import org.apache.chemistry.opencmis.commons.data.PropertyInteger;
import org.apache.chemistry.opencmis.commons.data.PropertyString;
import org.apache.chemistry.opencmis.commons.data.PropertyUri;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisStreamNotSupportedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * Nuxeo implementation of an object's property, backed by a property of a
 * {@link DocumentModel}.
 */
public abstract class NuxeoPropertyData<T> extends NuxeoPropertyDataBase<T> {

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
    @SuppressWarnings("unchecked")
    public static <U> PropertyData<U> construct(NuxeoObjectData data,
            PropertyDefinition<U> pd) {
        DocumentModel doc = data.doc;
        String name = pd.getId();
        if (PropertyIds.OBJECT_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed(
                    (PropertyDefinition<String>) pd, doc.getId());
        } else if (PropertyIds.OBJECT_TYPE_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed(
                    (PropertyDefinition<String>) pd,
                    NuxeoTypeHelper.mappedId(doc.getType()));
        } else if (PropertyIds.BASE_TYPE_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed(
                    (PropertyDefinition<String>) pd,
                    doc.isFolder() ? BaseTypeId.CMIS_FOLDER.value()
                            : BaseTypeId.CMIS_DOCUMENT.value());
        } else if (PropertyIds.CREATED_BY.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringData(
                    (PropertyDefinition<String>) pd, doc,
                    NuxeoTypeHelper.NX_DC_CREATOR, true);
        } else if (PropertyIds.CREATION_DATE.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDateTimeData(
                    (PropertyDefinition<GregorianCalendar>) pd, doc,
                    NuxeoTypeHelper.NX_DC_CREATED, true);
        } else if (PropertyIds.LAST_MODIFIED_BY.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataLastModifiedBy(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.LAST_MODIFICATION_DATE.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDateTimeData(
                    (PropertyDefinition<GregorianCalendar>) pd, doc,
                    NuxeoTypeHelper.NX_DC_MODIFIED, true);
        } else if (PropertyIds.CHANGE_TOKEN.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringDataFixed(
                    (PropertyDefinition<String>) pd, null);
        } else if (PropertyIds.NAME.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataName(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.IS_IMMUTABLE.equals(name)) {
            // TODO check write
            return (PropertyData<U>) new NuxeoPropertyBooleanDataFixed(
                    (PropertyDefinition<Boolean>) pd, Boolean.FALSE);
        } else if (PropertyIds.IS_LATEST_VERSION.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataIsLatestVersion(
                    (PropertyDefinition<Boolean>) pd, doc);
        } else if (PropertyIds.IS_LATEST_MAJOR_VERSION.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataIsLatestMajorVersion(
                    (PropertyDefinition<Boolean>) pd, doc);
        } else if (PropertyIds.IS_MAJOR_VERSION.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataIsMajorVersion(
                    (PropertyDefinition<Boolean>) pd, doc);
        } else if (PropertyIds.VERSION_LABEL.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataVersionLabel(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.VERSION_SERIES_ID.equals(name)) {
            // doesn't change once computed, no need to have a dynamic prop
            String versionSeriesId;
            try {
                versionSeriesId = doc.getVersionSeriesId();
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed(
                    (PropertyDefinition<String>) pd, versionSeriesId);
        } else if (PropertyIds.IS_VERSION_SERIES_CHECKED_OUT.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataIsVersionSeriesCheckedOut(
                    (PropertyDefinition<Boolean>) pd, doc);
        } else if (PropertyIds.VERSION_SERIES_CHECKED_OUT_BY.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataVersionSeriesCheckedOutBy(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.VERSION_SERIES_CHECKED_OUT_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataVersionSeriesCheckedOutId(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CHECKIN_COMMENT.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataCheckInComment(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_LENGTH.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataContentStreamLength(
                    (PropertyDefinition<BigInteger>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_MIME_TYPE.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataContentStreamMimeType(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_FILE_NAME.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataContentStreamFileName(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed(
                    (PropertyDefinition<String>) pd, null);
        } else if (PropertyIds.PARENT_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataParentId(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.PATH.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataPath(
                    (PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdMultiDataFixed(
                    (PropertyDefinition<String>) pd,
                    Collections.<String> emptyList());
        } else if (PropertyIds.SOURCE_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed(
                    (PropertyDefinition<String>) pd, null);
        } else if (PropertyIds.TARGET_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed(
                    (PropertyDefinition<String>) pd, null);
        } else if (PropertyIds.POLICY_TEXT.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringDataFixed(
                    (PropertyDefinition<String>) pd, null);
        } else {
            boolean readOnly = pd.getUpdatability() != Updatability.READWRITE;
            // TODO WHEN_CHECKED_OUT, ON_CREATE

            switch (pd.getPropertyType()) {
            case BOOLEAN:
                return (PropertyData<U>) new NuxeoPropertyBooleanData(
                        (PropertyDefinition<Boolean>) pd, doc, name, readOnly);
            case DATETIME:
                return (PropertyData<U>) new NuxeoPropertyDateTimeData(
                        (PropertyDefinition<GregorianCalendar>) pd, doc, name,
                        readOnly);
            case DECIMAL:
                return (PropertyData<U>) new NuxeoPropertyDecimalData(
                        (PropertyDefinition<BigDecimal>) pd, doc, name,
                        readOnly);
            case HTML:
                return (PropertyData<U>) new NuxeoPropertyHtmlData(
                        (PropertyDefinition<String>) pd, doc, name, readOnly);
            case ID:
                return (PropertyData<U>) new NuxeoPropertyIdData(
                        (PropertyDefinition<String>) pd, doc, name, readOnly);
            case INTEGER:
                return (PropertyData<U>) new NuxeoPropertyIntegerData(
                        (PropertyDefinition<BigInteger>) pd, doc, name,
                        readOnly);
            case STRING:
                return (PropertyData<U>) new NuxeoPropertyStringData(
                        (PropertyDefinition<String>) pd, doc, name, readOnly);
            case URI:
                return (PropertyData<U>) new NuxeoPropertyUriData(
                        (PropertyDefinition<String>) pd, doc, name, readOnly);
            default:
                throw new AssertionError(pd.getPropertyType().toString());
            }
        }
    }

    public static ContentStream getContentStream(DocumentModel doc)
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

    public static void setContentStream(DocumentModel doc,
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

    /**
     * Conversion from Nuxeo values to CMIS ones.
     *
     * @return either a primitive type or a List of them, or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <U> U getValue() {
        try {
            Property prop = doc.getProperty(name);
            Serializable value = prop.getValue();
            if (value == null) {
                return null;
            }
            Type type = prop.getType();
            if (type.isListType()) {
                // array/list
                List<Object> values;
                if (value instanceof Object[]) {
                    values = Arrays.asList((Object[]) value);
                } else if (value instanceof List<?>) {
                    values = (List<Object>) value;
                } else {
                    throw new CmisRuntimeException("Unknown value type: "
                            + value.getClass().getName());
                }
                List<Object> list = new ArrayList<Object>(values);
                for (int i = 0; i < list.size(); i++) {
                    list.set(i, convertToCMIS(list.get(i)));
                }
                return (U) list;
            } else {
                // primitive type
                return (U) convertToCMIS(value);
            }
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    // conversion from Nuxeo value types to CMIS ones
    protected static Object convertToCMIS(Object value) {
        if (value instanceof Double) {
            return BigDecimal.valueOf(((Double) value).doubleValue());
        } else if (value instanceof Integer) {
            return BigInteger.valueOf(((Integer) value).intValue());
        } else if (value instanceof Long) {
            return BigInteger.valueOf(((Long) value).longValue());
        } else {
            return value;
        }
    }

    // conversion from CMIS value types to Nuxeo ones
    protected static Object convertToNuxeo(Object value) {
        if (value instanceof BigDecimal) {
            return Double.valueOf(((BigDecimal) value).doubleValue());
        } else if (value instanceof BigInteger) {
            return Long.valueOf(((BigInteger) value).longValue());
        } else {
            return value;
        }
    }

    /**
     * Validates a CMIS value according to a property definition.
     */
    @SuppressWarnings("unchecked")
    public static <T> void validateCMISValue(Object value,
            PropertyDefinition<T> pd) {
        if (value == null) {
            return;
        }
        List<T> values;
        if (value instanceof List<?>) {
            if (pd.getCardinality() != Cardinality.MULTI) {
                throw new CmisInvalidArgumentException(
                        "Property is single-valued: " + pd.getId());
            }
            values = (List<T>) value;
            if (values.isEmpty()) {
                return;
            }
        } else {
            if (pd.getCardinality() != Cardinality.SINGLE) {
                throw new CmisInvalidArgumentException(
                        "Property is multi-valued: " + pd.getId());
            }
            values = Collections.singletonList((T) value);
        }
        PropertyType type = pd.getPropertyType();
        for (Object v : values) {
            if (v == null) {
                throw new CmisInvalidArgumentException(
                        "Null values not allowed: " + values);
            }
            boolean ok;
            switch (type) {
            case STRING:
            case ID:
            case URI:
            case HTML:
                ok = v instanceof String;
                break;
            case INTEGER:
                ok = v instanceof BigInteger || v instanceof Byte
                        || v instanceof Short || v instanceof Integer
                        || v instanceof Long;
                break;
            case DECIMAL:
                ok = v instanceof BigDecimal;
                break;
            case BOOLEAN:
                ok = v instanceof Boolean;
                break;
            case DATETIME:
                ok = v instanceof GregorianCalendar;
                break;
            default:
                throw new RuntimeException(type.toString());
            }
            if (!ok) {
                throw new CmisInvalidArgumentException(
                        "Value does not match property type " + type + ":  "
                                + v);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getFirstValue() {
        Object value = getValue();
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return null;
            }
            return (T) list.get(0);
        } else {
            return (T) value;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> getValues() {
        Object value = getValue();
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            return (List<T>) value;
        } else {
            return (List<T>) Collections.singletonList(value);
        }
    }

    @Override
    public void setValue(Object value) {
        try {
            if (readOnly) {
                super.setValue(value);
            } else {
                Object propValue;
                if (value instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = new ArrayList<Object>(
                            (List<Object>) value);
                    for (int i = 0; i < list.size(); i++) {
                        list.set(i, convertToNuxeo(list.get(i)));
                    }
                    if (list.isEmpty()) {
                        list = null;
                    }
                    propValue = list;
                } else {
                    propValue = convertToNuxeo(value);
                }
                doc.setPropertyValue(name, (Serializable) propValue);
            }
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    protected static Blob getBlob(DocumentModel doc)
            throws CmisRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            return null;
        }
        try {
            return blobHolder.getBlob();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    public static class NuxeoPropertyStringData extends
            NuxeoPropertyData<String> implements PropertyString {
        public NuxeoPropertyStringData(
                PropertyDefinition<String> propertyDefinition,
                DocumentModel doc, String name, boolean readOnly) {
            super(propertyDefinition, doc, name, readOnly);
        }
    }

    public static class NuxeoPropertyIdData extends NuxeoPropertyData<String>
            implements PropertyId {
        public NuxeoPropertyIdData(
                PropertyDefinition<String> propertyDefinition,
                DocumentModel doc, String name, boolean readOnly) {
            super(propertyDefinition, doc, name, readOnly);
        }
    }

    public static class NuxeoPropertyBooleanData extends
            NuxeoPropertyData<Boolean> implements PropertyBoolean {
        public NuxeoPropertyBooleanData(
                PropertyDefinition<Boolean> propertyDefinition,
                DocumentModel doc, String name, boolean readOnly) {
            super(propertyDefinition, doc, name, readOnly);
        }
    }

    public static class NuxeoPropertyIntegerData extends
            NuxeoPropertyData<BigInteger> implements PropertyInteger {
        public NuxeoPropertyIntegerData(
                PropertyDefinition<BigInteger> propertyDefinition,
                DocumentModel doc, String name, boolean readOnly) {
            super(propertyDefinition, doc, name, readOnly);
        }
    }

    public static class NuxeoPropertyDecimalData extends
            NuxeoPropertyData<BigDecimal> implements PropertyDecimal {
        public NuxeoPropertyDecimalData(
                PropertyDefinition<BigDecimal> propertyDefinition,
                DocumentModel doc, String name, boolean readOnly) {
            super(propertyDefinition, doc, name, readOnly);
        }
    }

    public static class NuxeoPropertyDateTimeData extends
            NuxeoPropertyData<GregorianCalendar> implements PropertyDateTime {
        public NuxeoPropertyDateTimeData(
                PropertyDefinition<GregorianCalendar> propertyDefinition,
                DocumentModel doc, String name, boolean readOnly) {
            super(propertyDefinition, doc, name, readOnly);
        }
    }

    public static class NuxeoPropertyHtmlData extends NuxeoPropertyData<String>
            implements PropertyHtml {
        public NuxeoPropertyHtmlData(
                PropertyDefinition<String> propertyDefinition,
                DocumentModel doc, String name, boolean readOnly) {
            super(propertyDefinition, doc, name, readOnly);
        }
    }

    public static class NuxeoPropertyUriData extends NuxeoPropertyData<String>
            implements PropertyUri {
        public NuxeoPropertyUriData(
                PropertyDefinition<String> propertyDefinition,
                DocumentModel doc, String name, boolean readOnly) {
            super(propertyDefinition, doc, name, readOnly);
        }
    }

    /**
     * Property for cmis:contentStreamFileName.
     */
    public static class NuxeoPropertyDataContentStreamFileName extends
            NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataContentStreamFileName(
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
    public static class NuxeoPropertyDataContentStreamLength extends
            NuxeoPropertyDataBase<BigInteger> implements PropertyInteger {

        protected NuxeoPropertyDataContentStreamLength(
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
    public static class NuxeoPropertyDataContentStreamMimeType extends
            NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataContentStreamMimeType(
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
     * Property for cmis:lastModifiedBy.
     */
    public static class NuxeoPropertyDataLastModifiedBy extends
            NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataLastModifiedBy(
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
     * Property for cmis:name.
     */
    public static class NuxeoPropertyDataName extends
            NuxeoPropertyDataBase<String> implements PropertyString {

        private static final Log log = LogFactory.getLog(NuxeoPropertyDataName.class);

        protected NuxeoPropertyDataName(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        /**
         * Gets the value for the cmis:name property.
         */
        public static String getValue(DocumentModel doc) {
            if (doc.getPath() == null) {
                // not a real doc (content changes)
                return "";
            }
            if (doc.getPath().isRoot()) {
                return ""; // Nuxeo root
            }
            String name;
            try {
                name = doc.getTitle();
            } catch (ClientException e) {
                name = "";
            }
            return name;
        }

        @Override
        public String getFirstValue() {
            return getValue(doc);
        }

        @Override
        public void setValue(Object value) {
            try {
                doc.setPropertyValue(NuxeoTypeHelper.NX_DC_TITLE,
                        (String) value);
            } catch (PropertyNotFoundException e) {
                // trying to set the name of a type with no dublincore
                // ignore
                log.debug("Cannot set CMIS name on type: " + doc.getType());
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:parentId.
     */
    public static class NuxeoPropertyDataParentId extends
            NuxeoPropertyDataBase<String> implements PropertyId {

        protected NuxeoPropertyDataParentId(
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
     * Property for cmis:path.
     */
    public static class NuxeoPropertyDataPath extends
            NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataPath(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            String path = doc.getPathAsString();
            return path == null ? "" : path;
        }
    }

    protected static boolean isLiveDocumentMajorVersion(DocumentModel doc)
            throws ClientException {
        return !doc.isCheckedOut() && doc.getVersionLabel().endsWith(".0");
    }

    /**
     * Property for cmis:isMajorVersion.
     */
    public static class NuxeoPropertyDataIsMajorVersion extends
            NuxeoPropertyDataBase<Boolean> implements PropertyBoolean {

        protected NuxeoPropertyDataIsMajorVersion(
                PropertyDefinition<Boolean> propertyDefinition,
                DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public Boolean getFirstValue() {
            try {
                if (doc.isVersion() || doc.isProxy()) {
                    return Boolean.valueOf(doc.isMajorVersion());
                }
                // checked in doc considered latest version
                return Boolean.valueOf(isLiveDocumentMajorVersion(doc));
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:isLatestVersion.
     */
    public static class NuxeoPropertyDataIsLatestVersion extends
            NuxeoPropertyDataBase<Boolean> implements PropertyBoolean {

        protected NuxeoPropertyDataIsLatestVersion(
                PropertyDefinition<Boolean> propertyDefinition,
                DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public Boolean getFirstValue() {
            try {
                if (doc.isVersion() || doc.isProxy()) {
                    return Boolean.valueOf(doc.isLatestVersion());
                }
                // checked in doc considered latest version
                return Boolean.valueOf(!doc.isCheckedOut());
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:isLatestMajorVersion.
     */
    public static class NuxeoPropertyDataIsLatestMajorVersion extends
            NuxeoPropertyDataBase<Boolean> implements PropertyBoolean {

        protected NuxeoPropertyDataIsLatestMajorVersion(
                PropertyDefinition<Boolean> propertyDefinition,
                DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public Boolean getFirstValue() {
            try {
                if (doc.isVersion() || doc.isProxy()) {
                    return Boolean.valueOf(doc.isLatestMajorVersion());
                }
                // checked in doc considered latest version
                return Boolean.valueOf(isLiveDocumentMajorVersion(doc));
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:isVersionSeriesCheckedOut.
     */
    public static class NuxeoPropertyDataIsVersionSeriesCheckedOut extends
            NuxeoPropertyDataBase<Boolean> implements PropertyBoolean {

        protected NuxeoPropertyDataIsVersionSeriesCheckedOut(
                PropertyDefinition<Boolean> propertyDefinition,
                DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public Boolean getFirstValue() {
            try {
                return Boolean.valueOf(doc.isVersionSeriesCheckedOut());
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:versionSeriesCheckedOutId.
     */
    public static class NuxeoPropertyDataVersionSeriesCheckedOutId extends
            NuxeoPropertyDataBase<String> implements PropertyId {

        protected NuxeoPropertyDataVersionSeriesCheckedOutId(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            try {
                if (!doc.isVersionSeriesCheckedOut()) {
                    return null;
                }
                DocumentModel pwc = doc.getCoreSession().getWorkingCopy(
                        doc.getRef());
                return pwc == null ? null : pwc.getId();
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:versionSeriesCheckedOutBy.
     */
    public static class NuxeoPropertyDataVersionSeriesCheckedOutBy extends
            NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataVersionSeriesCheckedOutBy(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            try {
                if (!doc.isVersionSeriesCheckedOut()) {
                    return null;
                }
                DocumentModel pwc = doc.getCoreSession().getWorkingCopy(
                        doc.getRef());
                return pwc == null ? null : "system"; // TODO not implemented
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:versionLabel.
     */
    public static class NuxeoPropertyDataVersionLabel extends
            NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataVersionLabel(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            try {
                if (doc.isVersion() || doc.isProxy()) {
                    return doc.getVersionLabel();
                }
                return doc.isCheckedOut() ? null : doc.getVersionLabel();
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

    /**
     * Property for cmis:checkinComment.
     */
    public static class NuxeoPropertyDataCheckInComment extends
            NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataCheckInComment(
                PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            try {
                if (doc.isVersion() || doc.isProxy()) {
                    return doc.getCheckinComment();
                }
                if (doc.isCheckedOut()) {
                    return null;
                }
                CoreSession session = doc.getCoreSession();
                DocumentRef v = session.getBaseVersion(doc.getRef());
                DocumentModel ver = session.getDocument(v);
                return ver.getCheckinComment();
            } catch (ClientException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        }
    }

}
