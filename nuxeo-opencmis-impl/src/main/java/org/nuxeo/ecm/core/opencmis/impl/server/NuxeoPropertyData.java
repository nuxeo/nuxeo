/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

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
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.DateTimeFormat;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisStreamNotSupportedException;
import org.apache.chemistry.opencmis.commons.impl.Constants;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamHashImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.shared.HttpUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.ComplexPropertyJSONEncoder;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

import com.google.common.collect.Iterators;

/**
 * Nuxeo implementation of an object's property, backed by a property of a {@link DocumentModel}.
 */
public abstract class NuxeoPropertyData<T> extends NuxeoPropertyDataBase<T> {

    protected final String name;

    protected final boolean readOnly;

    protected final CallContext callContext;

    public NuxeoPropertyData(PropertyDefinition<T> propertyDefinition, DocumentModel doc, String name,
            boolean readOnly, CallContext callContext) {
        super(propertyDefinition, doc);
        this.name = name;
        this.readOnly = readOnly;
        this.callContext = callContext;
    }

    /**
     * Factory for a new Property.
     */
    @SuppressWarnings("unchecked")
    public static <U> PropertyData<U> construct(NuxeoObjectData data, PropertyDefinition<U> pd, CallContext callContext) {
        DocumentModel doc = data.doc;
        String name = pd.getId();
        if (PropertyIds.OBJECT_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed((PropertyDefinition<String>) pd, doc.getId());
        } else if (PropertyIds.OBJECT_TYPE_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed((PropertyDefinition<String>) pd,
                    NuxeoTypeHelper.mappedId(doc.getType()));
        } else if (PropertyIds.BASE_TYPE_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed((PropertyDefinition<String>) pd,
                    NuxeoTypeHelper.getBaseTypeId(doc).value());
        } else if (PropertyIds.DESCRIPTION.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringData((PropertyDefinition<String>) pd, doc,
                    NuxeoTypeHelper.NX_DC_DESCRIPTION, false, callContext);
        } else if (PropertyIds.CREATED_BY.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringData((PropertyDefinition<String>) pd, doc,
                    NuxeoTypeHelper.NX_DC_CREATOR, true, callContext);
        } else if (PropertyIds.CREATION_DATE.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDateTimeData((PropertyDefinition<GregorianCalendar>) pd, doc,
                    NuxeoTypeHelper.NX_DC_CREATED, true, callContext);
        } else if (PropertyIds.LAST_MODIFIED_BY.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringData((PropertyDefinition<String>) pd, doc,
                    NuxeoTypeHelper.NX_DC_LAST_CONTRIBUTOR, true, callContext);
        } else if (PropertyIds.LAST_MODIFICATION_DATE.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDateTimeData((PropertyDefinition<GregorianCalendar>) pd, doc,
                    NuxeoTypeHelper.NX_DC_MODIFIED, true, callContext);
        } else if (PropertyIds.CHANGE_TOKEN.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringDataFixed((PropertyDefinition<String>) pd,
                    doc.getChangeToken());
        } else if (PropertyIds.NAME.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataName((PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.IS_IMMUTABLE.equals(name)) {
            // TODO check write
            return (PropertyData<U>) new NuxeoPropertyBooleanDataFixed((PropertyDefinition<Boolean>) pd, Boolean.FALSE);
        } else if (PropertyIds.IS_LATEST_VERSION.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataIsLatestVersion((PropertyDefinition<Boolean>) pd, doc);
        } else if (PropertyIds.IS_LATEST_MAJOR_VERSION.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataIsLatestMajorVersion((PropertyDefinition<Boolean>) pd, doc);
        } else if (PropertyIds.IS_MAJOR_VERSION.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataIsMajorVersion((PropertyDefinition<Boolean>) pd, doc);
        } else if (PropertyIds.VERSION_LABEL.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataVersionLabel((PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.VERSION_SERIES_ID.equals(name)) {
            // doesn't change once computed, no need to have a dynamic prop
            String versionSeriesId = doc.getVersionSeriesId();
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed((PropertyDefinition<String>) pd, versionSeriesId);
        } else if (PropertyIds.IS_VERSION_SERIES_CHECKED_OUT.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataIsVersionSeriesCheckedOut((PropertyDefinition<Boolean>) pd,
                    doc);
        } else if (PropertyIds.VERSION_SERIES_CHECKED_OUT_BY.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataVersionSeriesCheckedOutBy((PropertyDefinition<String>) pd,
                    doc, callContext);
        } else if (PropertyIds.VERSION_SERIES_CHECKED_OUT_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataVersionSeriesCheckedOutId((PropertyDefinition<String>) pd,
                    doc);
        } else if (NuxeoTypeHelper.NX_ISVERSION.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyBooleanDataFixed((PropertyDefinition<Boolean>) pd,
                    Boolean.valueOf(doc.isVersion()));
        } else if (NuxeoTypeHelper.NX_ISCHECKEDIN.equals(name)) {
            boolean co = doc.isCheckedOut();
            return (PropertyData<U>) new NuxeoPropertyBooleanDataFixed((PropertyDefinition<Boolean>) pd,
                    Boolean.valueOf(!co));
        } else if (PropertyIds.IS_PRIVATE_WORKING_COPY.equals(name)) {
            boolean co = doc.isCheckedOut();
            return (PropertyData<U>) new NuxeoPropertyBooleanDataFixed((PropertyDefinition<Boolean>) pd,
                    Boolean.valueOf(co));
        } else if (PropertyIds.CHECKIN_COMMENT.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataCheckInComment((PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_LENGTH.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataContentStreamLength((PropertyDefinition<BigInteger>) pd, doc);
        } else if (NuxeoTypeHelper.NX_DIGEST.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataContentStreamDigest((PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_HASH.equals(name)) {
            String digest = new NuxeoPropertyDataContentStreamDigest((PropertyDefinition<String>) pd, doc).getFirstValue();
            List<String> hashes;
            if (digest == null) {
                hashes = new ArrayList<String>();
            } else {
                hashes = Arrays.asList(new ContentStreamHashImpl(ContentStreamHashImpl.ALGORITHM_MD5, digest).getPropertyValue());
            }
            return (PropertyData<U>) new NuxeoPropertyDataContentStreamHash((PropertyDefinition<String>) pd, hashes);
        } else if (PropertyIds.CONTENT_STREAM_MIME_TYPE.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataContentStreamMimeType((PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_FILE_NAME.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataContentStreamFileName((PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.CONTENT_STREAM_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdDataFixed((PropertyDefinition<String>) pd, null);
        } else if (PropertyIds.PARENT_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataParentId((PropertyDefinition<String>) pd, doc);
        } else if (NuxeoTypeHelper.NX_PARENT_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataParentId((PropertyDefinition<String>) pd, doc);
        } else if (NuxeoTypeHelper.NX_PATH_SEGMENT.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringDataFixed((PropertyDefinition<String>) pd, doc.getName());
        } else if (NuxeoTypeHelper.NX_POS.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIntegerDataFixed((PropertyDefinition<BigInteger>) pd,
                    doc.getPos());
        } else if (PropertyIds.PATH.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyDataPath((PropertyDefinition<String>) pd, doc);
        } else if (PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdMultiDataFixed((PropertyDefinition<String>) pd,
                    Collections.<String> emptyList());
        } else if (PropertyIds.SOURCE_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdData((PropertyDefinition<String>) pd, doc,
                    NuxeoTypeHelper.NX_REL_SOURCE, false, callContext);
        } else if (PropertyIds.TARGET_ID.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyIdData((PropertyDefinition<String>) pd, doc,
                    NuxeoTypeHelper.NX_REL_TARGET, false, callContext);
        } else if (PropertyIds.POLICY_TEXT.equals(name)) {
            return (PropertyData<U>) new NuxeoPropertyStringDataFixed((PropertyDefinition<String>) pd, null);
        } else if (PropertyIds.SECONDARY_OBJECT_TYPE_IDS.equals(name)) {
            List<String> facets = getSecondaryTypeIds(doc);
            return (PropertyData<U>) new NuxeoPropertyIdMultiDataFixed((PropertyDefinition<String>) pd, facets);
        } else if (NuxeoTypeHelper.NX_FACETS.equals(name)) {
            List<String> facets = getFacets(doc);
            return (PropertyData<U>) new NuxeoPropertyIdMultiDataFixed((PropertyDefinition<String>) pd, facets);
        } else if (NuxeoTypeHelper.NX_LIFECYCLE_STATE.equals(name)) {
            String state = doc.getCurrentLifeCycleState();
            return (PropertyData<U>) new NuxeoPropertyStringDataFixed((PropertyDefinition<String>) pd, state);
        } else {
            boolean readOnly = pd.getUpdatability() != Updatability.READWRITE;
            // TODO WHEN_CHECKED_OUT, ON_CREATE

            switch (pd.getPropertyType()) {
            case BOOLEAN:
                return (PropertyData<U>) new NuxeoPropertyBooleanData((PropertyDefinition<Boolean>) pd, doc, name,
                        readOnly, callContext);
            case DATETIME:
                return (PropertyData<U>) new NuxeoPropertyDateTimeData((PropertyDefinition<GregorianCalendar>) pd, doc,
                        name, readOnly, callContext);
            case DECIMAL:
                return (PropertyData<U>) new NuxeoPropertyDecimalData((PropertyDefinition<BigDecimal>) pd, doc, name,
                        readOnly, callContext);
            case HTML:
                return (PropertyData<U>) new NuxeoPropertyHtmlData((PropertyDefinition<String>) pd, doc, name,
                        readOnly, callContext);
            case ID:
                return (PropertyData<U>) new NuxeoPropertyIdData((PropertyDefinition<String>) pd, doc, name, readOnly,
                        callContext);
            case INTEGER:
                return (PropertyData<U>) new NuxeoPropertyIntegerData((PropertyDefinition<BigInteger>) pd, doc, name,
                        readOnly, callContext);
            case STRING:
                return (PropertyData<U>) new NuxeoPropertyStringData((PropertyDefinition<String>) pd, doc, name,
                        readOnly, callContext);
            case URI:
                return (PropertyData<U>) new NuxeoPropertyUriData((PropertyDefinition<String>) pd, doc, name, readOnly,
                        callContext);
            default:
                throw new AssertionError(pd.getPropertyType().toString());
            }
        }
    }

    /** Gets the doc's relevant facets. */
    public static List<String> getFacets(DocumentModel doc) {
        List<String> facets = new ArrayList<String>(doc.getFacets());
        facets.remove(FacetNames.IMMUTABLE); // not actually stored or registered
        Collections.sort(facets);
        return facets;
    }

    /** Gets the doc's secondary type ids. */
    public static List<String> getSecondaryTypeIds(DocumentModel doc) {
        List<String> facets = getFacets(doc);
        DocumentType type = doc.getDocumentType();
        for (ListIterator<String> it = facets.listIterator(); it.hasNext();) {
            // remove those already in the doc type
            String facet = it.next();
            if (type.hasFacet(facet)) {
                it.remove();
                continue;
            }
            // add prefix
            it.set(NuxeoTypeHelper.FACET_TYPE_PREFIX + facet);
        }
        return facets;
    }

    public static ContentStream getContentStream(DocumentModel doc, HttpServletRequest request)
            throws CmisRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            throw new CmisStreamNotSupportedException();
        }
        Blob blob = blobHolder.getBlob();
        if (blob == null) {
            return null;
        }
        GregorianCalendar lastModified = (GregorianCalendar) doc.getPropertyValue("dc:modified");
        return NuxeoContentStream.create(doc, DownloadService.BLOBHOLDER_0, blob, "cmis", null, lastModified, request);
    }

    public static void setContentStream(DocumentModel doc, ContentStream contentStream, boolean overwrite)
            throws IOException, CmisContentAlreadyExistsException, CmisRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            throw new CmisContentAlreadyExistsException();
        }
        Blob oldBlob = blobHolder.getBlob();
        if (!overwrite && oldBlob != null) {
            throw new CmisContentAlreadyExistsException();
        }
        Blob blob;
        if (contentStream == null) {
            blob = null;
        } else {
            // default filename if none provided
            String filename = contentStream.getFileName();
            if (filename == null && oldBlob != null) {
                filename = oldBlob.getFilename();
            }
            if (filename == null) {
                filename = doc.getTitle();
            }
            blob = getPersistentBlob(contentStream, filename);
        }
        blobHolder.setBlob(blob);
    }

    /** Returns a Blob whose stream can be used several times. */
    public static Blob getPersistentBlob(ContentStream contentStream, String filename) throws IOException {
        if (filename == null) {
            filename = contentStream.getFileName();
        }
        File file = Framework.createTempFile("NuxeoCMIS-", null);
        try (InputStream in = contentStream.getStream(); OutputStream out = new FileOutputStream(file)){
            IOUtils.copy(in, out);
            Framework.trackFile(file, in);
        }
        return Blobs.createBlob(file, contentStream.getMimeType(), null, filename);
    }

    public static void validateBlobDigest(DocumentModel doc, CallContext callContext) {
        Blob blob = doc.getAdapter(BlobHolder.class).getBlob();
        if (blob == null) {
            return;
        }
        String blobDigestAlgorithm = blob.getDigestAlgorithm();
        if (blobDigestAlgorithm == null) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) callContext.get(CallContext.HTTP_SERVLET_REQUEST);
        String reqDigest = NuxeoPropertyData.extractDigestFromRequestHeaders(request, blobDigestAlgorithm);
        if (reqDigest == null) {
            return;
        }
        String blobDigest = blob.getDigest();
        if (!blobDigest.equals(reqDigest)) {
            throw new CmisInvalidArgumentException(String.format(
                    "Content Stream Hex-encoded Digest: '%s' must equal Request Header Hex-encoded Digest: '%s'",
                    blobDigest, reqDigest));
        }
    }

    protected static String extractDigestFromRequestHeaders(HttpServletRequest request, String digestAlgorithm) {
        if (request == null) {
            return null;
        }
        Enumeration<String> digests = request.getHeaders(NuxeoContentStream.DIGEST_HEADER_NAME);
        if (digests == null) {
            return null;
        }
        Iterator<String> it = Iterators.forEnumeration(digests);
        while (it.hasNext()) {
            String value = it.next();
            int equals = value.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String reqDigestAlgorithm = value.substring(0, equals);
            if (reqDigestAlgorithm.equalsIgnoreCase(digestAlgorithm)) {
                String digest = value.substring(equals + 1);
                digest = transcodeBase64ToHex(digest);
                return digest;
            }
        }
        return null;
    }

    public static String transcodeBase64ToHex(String base64String){
        byte[] bytes = Base64.decodeBase64(base64String);
        String hexString = Hex.encodeHexString(bytes);
        return hexString;
    }

    public static String transcodeHexToBase64(String hexString) {
        byte[] bytes;
        try {
            bytes = Hex.decodeHex(hexString.toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
        String base64String = Base64.encodeBase64String(bytes);
        return base64String;
    }

    /**
     * Conversion from Nuxeo values to CMIS ones.
     *
     * @return either a primitive type or a List of them, or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <U> U getValue() {
        Property prop = doc.getProperty(name);
        Serializable value = prop.getValue();
        if (value == null) {
            return null;
        }
        Type type = prop.getType();
        if (type.isListType()) {
            // array/list
            type = ((ListType) type).getFieldType();
            Collection<Object> values;
            if (type.isComplexType()) {
                values = (Collection) prop.getChildren();
            } else if (value instanceof Object[]) {
                values = Arrays.asList((Object[]) value);
            } else if (value instanceof List<?>) {
                values = (List<Object>) value;
            } else {
                throw new CmisRuntimeException("Unknown value type: " + value.getClass().getName());
            }
            List<Object> list = new ArrayList<>(values);
            for (int i = 0; i < list.size(); i++) {
                if (type.isComplexType()) {
                    value = (Serializable) convertComplexPropertyToCMIS((ComplexProperty) list.get(i), callContext);
                } else {
                    value = (Serializable) convertToCMIS(list.get(i));
                }
                list.set(i, value);
            }
            return (U) list;
        } else {
            // primitive type or complex type
            if (type.isComplexType()) {
                value = (Serializable) convertComplexPropertyToCMIS((ComplexProperty) prop, callContext);
            } else {
                value = (Serializable) convertToCMIS(value);
            }
            return (U) convertToCMIS(value);
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

    protected static Object convertComplexPropertyToCMIS(ComplexProperty prop, CallContext callContext) {
        DateTimeFormat cmisDateTimeFormat = getCMISDateTimeFormat(callContext);
        org.nuxeo.ecm.automation.core.util.DateTimeFormat nuxeoDateTimeFormat = cmisDateTimeFormat == DateTimeFormat.SIMPLE
                ? org.nuxeo.ecm.automation.core.util.DateTimeFormat.TIME_IN_MILLIS
                : org.nuxeo.ecm.automation.core.util.DateTimeFormat.W3C;
        try {
            return ComplexPropertyJSONEncoder.encode(prop, nuxeoDateTimeFormat);
        } catch (IOException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    protected static DateTimeFormat getCMISDateTimeFormat(CallContext callContext) {
        if (callContext != null && CallContext.BINDING_BROWSER.equals(callContext.getBinding())) {
            HttpServletRequest request = (HttpServletRequest) callContext.get(CallContext.HTTP_SERVLET_REQUEST);
            if (request != null) {
                String s = HttpUtils.getStringParameter(request, Constants.PARAM_DATETIME_FORMAT);
                if (s != null) {
                    try {
                        return DateTimeFormat.fromValue(s.trim().toLowerCase(Locale.ENGLISH));
                    } catch (IllegalArgumentException e) {
                        throw new CmisInvalidArgumentException("Invalid value for parameter "
                                + Constants.PARAM_DATETIME_FORMAT + "!");
                    }
                }
            }
            return DateTimeFormat.SIMPLE;
        }
        return DateTimeFormat.EXTENDED;
    }

    // conversion from CMIS value types to Nuxeo ones
    protected static Object convertToNuxeo(Object value, Type type) {
        if (value instanceof BigDecimal) {
            return Double.valueOf(((BigDecimal) value).doubleValue());
        } else if (value instanceof BigInteger) {
            return Long.valueOf(((BigInteger) value).longValue());
        } else if (type.isComplexType()) {
            try {
                return ComplexTypeJSONDecoder.decode((ComplexType) type, value.toString());
            } catch (IOException e) {
                throw new CmisRuntimeException(e.toString(), e);
            }
        } else {
            return value;
        }
    }

    /**
     * Validates a CMIS value according to a property definition.
     */
    @SuppressWarnings("unchecked")
    public static <T> void validateCMISValue(Object value, PropertyDefinition<T> pd) {
        if (value == null) {
            return;
        }
        List<T> values;
        if (value instanceof List<?>) {
            if (pd.getCardinality() != Cardinality.MULTI) {
                throw new CmisInvalidArgumentException("Property is single-valued: " + pd.getId());
            }
            values = (List<T>) value;
            if (values.isEmpty()) {
                return;
            }
        } else {
            if (pd.getCardinality() != Cardinality.SINGLE) {
                throw new CmisInvalidArgumentException("Property is multi-valued: " + pd.getId());
            }
            values = Collections.singletonList((T) value);
        }
        PropertyType type = pd.getPropertyType();
        for (Object v : values) {
            if (v == null) {
                throw new CmisInvalidArgumentException("Null values not allowed: " + values);
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
                ok = v instanceof BigInteger || v instanceof Byte || v instanceof Short || v instanceof Integer
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
                throw new CmisInvalidArgumentException("Value does not match property type " + type + ":  " + v);
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
        if (readOnly) {
            super.setValue(value);
        } else {
            Type type = doc.getProperty(name).getType();
            if (type.isListType()) {
                type = ((ListType) type).getFieldType();
            }
            Object propValue;
            if (value instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> list = new ArrayList<>((List<Object>) value);
                for (int i = 0; i < list.size(); i++) {
                    list.set(i, convertToNuxeo(list.get(i), type));
                }
                if (list.isEmpty()) {
                    list = null;
                }
                propValue = list;
            } else {
                propValue = convertToNuxeo(value, type);
            }
            doc.setPropertyValue(name, (Serializable) propValue);
        }
    }

    protected static Blob getBlob(DocumentModel doc) throws CmisRuntimeException {
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            return null;
        }
        return blobHolder.getBlob();
    }

    public static class NuxeoPropertyStringData extends NuxeoPropertyData<String> implements PropertyString {
        public NuxeoPropertyStringData(PropertyDefinition<String> propertyDefinition, DocumentModel doc, String name,
                boolean readOnly, CallContext callContext) {
            super(propertyDefinition, doc, name, readOnly, callContext);
        }
    }

    public static class NuxeoPropertyIdData extends NuxeoPropertyData<String> implements PropertyId {
        public NuxeoPropertyIdData(PropertyDefinition<String> propertyDefinition, DocumentModel doc, String name,
                boolean readOnly, CallContext callContext) {
            super(propertyDefinition, doc, name, readOnly, callContext);
        }
    }

    public static class NuxeoPropertyBooleanData extends NuxeoPropertyData<Boolean> implements PropertyBoolean {
        public NuxeoPropertyBooleanData(PropertyDefinition<Boolean> propertyDefinition, DocumentModel doc, String name,
                boolean readOnly, CallContext callContext) {
            super(propertyDefinition, doc, name, readOnly, callContext);
        }
    }

    public static class NuxeoPropertyIntegerData extends NuxeoPropertyData<BigInteger> implements PropertyInteger {
        public NuxeoPropertyIntegerData(PropertyDefinition<BigInteger> propertyDefinition, DocumentModel doc,
                String name, boolean readOnly, CallContext callContext) {
            super(propertyDefinition, doc, name, readOnly, callContext);
        }
    }

    public static class NuxeoPropertyDecimalData extends NuxeoPropertyData<BigDecimal> implements PropertyDecimal {
        public NuxeoPropertyDecimalData(PropertyDefinition<BigDecimal> propertyDefinition, DocumentModel doc,
                String name, boolean readOnly, CallContext callContext) {
            super(propertyDefinition, doc, name, readOnly, callContext);
        }
    }

    public static class NuxeoPropertyDateTimeData extends NuxeoPropertyData<GregorianCalendar> implements
            PropertyDateTime {
        public NuxeoPropertyDateTimeData(PropertyDefinition<GregorianCalendar> propertyDefinition, DocumentModel doc,
                String name, boolean readOnly, CallContext callContext) {
            super(propertyDefinition, doc, name, readOnly, callContext);
        }
    }

    public static class NuxeoPropertyHtmlData extends NuxeoPropertyData<String> implements PropertyHtml {
        public NuxeoPropertyHtmlData(PropertyDefinition<String> propertyDefinition, DocumentModel doc, String name,
                boolean readOnly, CallContext callContext) {
            super(propertyDefinition, doc, name, readOnly, callContext);
        }
    }

    public static class NuxeoPropertyUriData extends NuxeoPropertyData<String> implements PropertyUri {
        public NuxeoPropertyUriData(PropertyDefinition<String> propertyDefinition, DocumentModel doc, String name,
                boolean readOnly, CallContext callContext) {
            super(propertyDefinition, doc, name, readOnly, callContext);
        }
    }

    /**
     * Property for cmis:contentStreamFileName.
     */
    public static class NuxeoPropertyDataContentStreamFileName extends NuxeoPropertyDataBase<String> implements
            PropertyString {

        protected NuxeoPropertyDataContentStreamFileName(PropertyDefinition<String> propertyDefinition,
                DocumentModel doc) {
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
        // blob = blobHolder.getBlob();
        // if (blob != null) {
        // blob.setFilename((String) value);
        // }
        // }
    }

    /**
     * Property for cmis:contentStreamLength.
     */
    public static class NuxeoPropertyDataContentStreamLength extends NuxeoPropertyDataBase<BigInteger> implements
            PropertyInteger {

        protected NuxeoPropertyDataContentStreamLength(PropertyDefinition<BigInteger> propertyDefinition,
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
     * Property for nuxeo:contentStreamDigest.
     */
    public static class NuxeoPropertyDataContentStreamDigest extends NuxeoPropertyDataBase<String> implements
            PropertyString {

        protected NuxeoPropertyDataContentStreamDigest(PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            Blob blob = getBlob(doc);
            return blob == null ? null : blob.getDigest();
        }
    }

    /**
     * Property for cmis:contentStreamHash.
     */
    public static class NuxeoPropertyDataContentStreamHash extends NuxeoPropertyMultiDataFixed<String> implements
            PropertyString {

        protected NuxeoPropertyDataContentStreamHash(PropertyDefinition<String> propertyDefinition, List<String> hashes) {
            super(propertyDefinition, hashes);
        }
    }

    /**
     * Property for cmis:contentMimeTypeLength.
     */
    public static class NuxeoPropertyDataContentStreamMimeType extends NuxeoPropertyDataBase<String> implements
            PropertyString {

        protected NuxeoPropertyDataContentStreamMimeType(PropertyDefinition<String> propertyDefinition,
                DocumentModel doc) {
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
    public static class NuxeoPropertyDataName extends NuxeoPropertyDataBase<String> implements PropertyString {

        private static final Log log = LogFactory.getLog(NuxeoPropertyDataName.class);

        protected NuxeoPropertyDataName(PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
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
            return doc.getTitle();
        }

        @Override
        public String getFirstValue() {
            return getValue(doc);
        }

        @Override
        public void setValue(Object value) {
            try {
                doc.setPropertyValue(NuxeoTypeHelper.NX_DC_TITLE, (String) value);
            } catch (PropertyNotFoundException e) {
                // trying to set the name of a type with no dublincore
                // ignore
                log.debug("Cannot set CMIS name on type: " + doc.getType());
            }
        }
    }

    /**
     * Property for cmis:parentId and nuxeo:parentId.
     */
    public static class NuxeoPropertyDataParentId extends NuxeoPropertyDataBase<String> implements PropertyId {

        protected NuxeoPropertyDataParentId(PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            if (doc.getName() == null) {
                return null;
            } else {
                DocumentRef parentRef = doc.getParentRef();
                if (parentRef == null) {
                    return null; // unfiled document
                } else if (parentRef instanceof IdRef) {
                    return ((IdRef) parentRef).value;
                } else {
                    return doc.getCoreSession().getDocument(parentRef).getId();
                }
            }
        }
    }

    /**
     * Property for cmis:path.
     */
    public static class NuxeoPropertyDataPath extends NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataPath(PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            String path = doc.getPathAsString();
            return path == null ? "" : path;
        }
    }

    protected static boolean isVersionOrProxyToVersion(DocumentModel doc) {
        return doc.isVersion() || (doc.isProxy() && doc.getCoreSession().getSourceDocument(doc.getRef()).isVersion());
    }

    /**
     * Property for cmis:isMajorVersion.
     */
    public static class NuxeoPropertyDataIsMajorVersion extends NuxeoPropertyDataBase<Boolean> implements
            PropertyBoolean {

        protected NuxeoPropertyDataIsMajorVersion(PropertyDefinition<Boolean> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public Boolean getFirstValue() {
            if (isVersionOrProxyToVersion(doc)) {
                return Boolean.valueOf(doc.isMajorVersion());
            }
            // checked in doc considered latest version
            return Boolean.valueOf(!doc.isCheckedOut() && doc.getVersionLabel().endsWith(".0"));
        }
    }

    /**
     * Property for cmis:isLatestVersion.
     */
    public static class NuxeoPropertyDataIsLatestVersion extends NuxeoPropertyDataBase<Boolean> implements
            PropertyBoolean {

        protected NuxeoPropertyDataIsLatestVersion(PropertyDefinition<Boolean> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public Boolean getFirstValue() {
            if (isVersionOrProxyToVersion(doc)) {
                return Boolean.valueOf(doc.isLatestVersion());
            }
            // checked in doc considered latest version
            return Boolean.valueOf(!doc.isCheckedOut());
        }
    }

    /**
     * Property for cmis:isLatestMajorVersion.
     */
    public static class NuxeoPropertyDataIsLatestMajorVersion extends NuxeoPropertyDataBase<Boolean> implements
            PropertyBoolean {

        protected NuxeoPropertyDataIsLatestMajorVersion(PropertyDefinition<Boolean> propertyDefinition,
                DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public Boolean getFirstValue() {
            if (isVersionOrProxyToVersion(doc)) {
                return Boolean.valueOf(doc.isLatestMajorVersion());
            }
            // checked in doc considered latest version
            return Boolean.valueOf(!doc.isCheckedOut() && doc.getVersionLabel().endsWith(".0"));
        }
    }

    /**
     * Property for cmis:isVersionSeriesCheckedOut.
     */
    public static class NuxeoPropertyDataIsVersionSeriesCheckedOut extends NuxeoPropertyDataBase<Boolean> implements
            PropertyBoolean {

        protected NuxeoPropertyDataIsVersionSeriesCheckedOut(PropertyDefinition<Boolean> propertyDefinition,
                DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public Boolean getFirstValue() {
            return Boolean.valueOf(doc.isVersionSeriesCheckedOut());
        }
    }

    /**
     * Property for cmis:versionSeriesCheckedOutId.
     */
    public static class NuxeoPropertyDataVersionSeriesCheckedOutId extends NuxeoPropertyDataBase<String> implements
            PropertyId {

        protected NuxeoPropertyDataVersionSeriesCheckedOutId(PropertyDefinition<String> propertyDefinition,
                DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            if (!doc.isVersionSeriesCheckedOut()) {
                return null;
            }
            DocumentModel pwc = doc.getCoreSession().getWorkingCopy(doc.getRef());
            return pwc == null ? null : pwc.getId();
        }
    }

    /**
     * Property for cmis:versionSeriesCheckedOutBy.
     */
    public static class NuxeoPropertyDataVersionSeriesCheckedOutBy extends NuxeoPropertyDataBase<String> implements
            PropertyString {

        protected final CallContext callContext;

        protected NuxeoPropertyDataVersionSeriesCheckedOutBy(PropertyDefinition<String> propertyDefinition,
                DocumentModel doc, CallContext callContext) {
            super(propertyDefinition, doc);
            this.callContext = callContext;
        }

        @Override
        public String getFirstValue() {
            if (!doc.isVersionSeriesCheckedOut()) {
                return null;
            }
            DocumentModel pwc = doc.getCoreSession().getWorkingCopy(doc.getRef());
            // TODO not implemented
            return pwc == null ? null : callContext.getUsername();
        }
    }

    /**
     * Property for cmis:versionLabel.
     */
    public static class NuxeoPropertyDataVersionLabel extends NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataVersionLabel(PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            if (isVersionOrProxyToVersion(doc)) {
                return doc.getVersionLabel();
            }
            return doc.isCheckedOut() ? null : doc.getVersionLabel();
        }
    }

    /**
     * Property for cmis:checkinComment.
     */
    public static class NuxeoPropertyDataCheckInComment extends NuxeoPropertyDataBase<String> implements PropertyString {

        protected NuxeoPropertyDataCheckInComment(PropertyDefinition<String> propertyDefinition, DocumentModel doc) {
            super(propertyDefinition, doc);
        }

        @Override
        public String getFirstValue() {
            if (isVersionOrProxyToVersion(doc)) {
                return doc.getCheckinComment();
            }
            if (doc.isCheckedOut()) {
                return null;
            }
            CoreSession session = doc.getCoreSession();
            DocumentRef v = session.getBaseVersion(doc.getRef());
            DocumentModel ver = session.getDocument(v);
            return ver.getCheckinComment();
        }
    }

}
