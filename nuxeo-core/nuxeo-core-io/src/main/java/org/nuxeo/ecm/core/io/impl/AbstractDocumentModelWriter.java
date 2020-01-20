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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.io.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: improve it ->
// modify core session to add a batch create method and use it
public abstract class AbstractDocumentModelWriter extends AbstractDocumentWriter {

    private static final Log log = LogFactory.getLog(AbstractDocumentModelWriter.class);

    protected CoreSession session;

    protected Path root;

    private int saveInterval;

    protected int unsavedDocuments = 0;

    private final Map<DocumentLocation, DocumentLocation> translationMap = new HashMap<>();

    /**
     * @param session the session to the repository where to write
     * @param parentPath where to write the tree. this document will be used as the parent of all top level documents
     *            passed as input. Note that you may have
     */
    protected AbstractDocumentModelWriter(CoreSession session, String parentPath) {
        this(session, parentPath, 10);
    }

    protected AbstractDocumentModelWriter(CoreSession session, String parentPath, int saveInterval) {
        if (session == null) {
            throw new IllegalArgumentException("null session");
        }
        this.session = session;
        this.saveInterval = saveInterval;
        root = new Path(parentPath);
    }

    public Map<DocumentLocation, DocumentLocation> getTranslationMap() {
        return translationMap;
    }

    protected void saveIfNeeded() {
        if (unsavedDocuments >= saveInterval) {
            session.save();
            unsavedDocuments = 0;
        }
    }

    @Override
    public void close() {
        if (unsavedDocuments > 0) {
            session.save();
        }
        session = null;
        root = null;
    }

    /**
     * Creates a new document given its path.
     * <p>
     * The parent of this document is assumed to exist.
     *
     * @param xdoc the document containing
     * @param toPath the path of the doc to create
     */
    protected DocumentModel createDocument(ExportedDocument xdoc, Path toPath) {
        Path parentPath = toPath.removeLastSegments(1);
        String name = toPath.lastSegment();

        DocumentModel doc = session.createDocumentModel(parentPath.toString(), name, xdoc.getType());

        // set lifecycle state at creation
        Element system = xdoc.getDocument().getRootElement().element(ExportConstants.SYSTEM_TAG);
        String lifeCycleState = system.element(ExportConstants.LIFECYCLE_STATE_TAG).getText();
        doc.putContextData("initialLifecycleState", lifeCycleState);

        // loadFacets before schemas so that additional schemas are not skipped
        loadFacetsInfo(doc, xdoc.getDocument());

        // then load schemas data
        loadSchemas(xdoc, doc, xdoc.getDocument());

        if (doc.hasSchema("uid")) {
            doc.putContextData(VersioningService.SKIP_VERSIONING, true);
        }

        beforeCreateDocument(doc);
        doc = session.createDocument(doc);

        // load into the document the system properties, document needs to exist
        loadSystemInfo(doc, xdoc.getDocument());

        unsavedDocuments += 1;
        saveIfNeeded();

        return doc;
    }

    /**
     * @since 8.4
     */
    protected void beforeCreateDocument(DocumentModel doc) {
        // Empty default implementation
    }

    /**
     * Updates an existing document.
     */
    protected DocumentModel updateDocument(ExportedDocument xdoc, DocumentModel doc) {
        // load schemas data
        loadSchemas(xdoc, doc, xdoc.getDocument());

        loadFacetsInfo(doc, xdoc.getDocument());

        beforeSaveDocument(doc);
        doc = session.saveDocument(doc);

        unsavedDocuments += 1;
        saveIfNeeded();

        return doc;
    }

    /**
     * @since 8.4
     */
    protected void beforeSaveDocument(DocumentModel doc) {
        // Empty default implementation
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public void setSaveInterval(int saveInterval) {
        this.saveInterval = saveInterval;
    }

    @SuppressWarnings("unchecked")
    protected boolean loadFacetsInfo(DocumentModel docModel, Document doc) {
        boolean added = false;
        Element system = doc.getRootElement().element(ExportConstants.SYSTEM_TAG);
        if (system == null) {
            return false;
        }

        Iterator<Element> facets = system.elementIterator(ExportConstants.FACET_TAG);
        while (facets.hasNext()) {
            Element element = facets.next();
            String facet = element.getTextTrim();

            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            CompositeType facetType = schemaManager.getFacet(facet);

            if (facetType == null) {
                log.warn("The document " + docModel.getName() + " with id=" + docModel.getId() + " and type="
                        + docModel.getDocumentType().getName() + " contains the facet '" + facet
                        + "', which is not registered as available in the schemaManager. This facet will be ignored.");
                if (log.isDebugEnabled()) {
                    log.debug("Available facets: " + Arrays.toString(schemaManager.getFacets()));
                }
                continue;
            }

            if (!docModel.hasFacet(facet)) {
                docModel.addFacet(facet);
                added = true;
            }
        }

        return added;
    }

    @SuppressWarnings("unchecked")
    protected void loadSystemInfo(DocumentModel docModel, Document doc) {
        Element system = doc.getRootElement().element(ExportConstants.SYSTEM_TAG);

        Element accessControl = system.element(ExportConstants.ACCESS_CONTROL_TAG);
        if (accessControl == null) {
            return;
        }
        Iterator<Element> it = accessControl.elementIterator(ExportConstants.ACL_TAG);
        while (it.hasNext()) {
            Element element = it.next();
            // import only the local acl
            if (ACL.LOCAL_ACL.equals(element.attributeValue(ExportConstants.NAME_ATTR))) {
                // this is the local ACL - import it
                List<Element> entries = element.elements();
                int size = entries.size();
                if (size > 0) {
                    ACP acp = new ACPImpl();
                    ACL acl = new ACLImpl(ACL.LOCAL_ACL);
                    acp.addACL(acl);
                    for (Element el : entries) {
                        String username = el.attributeValue(ExportConstants.PRINCIPAL_ATTR);
                        String permission = el.attributeValue(ExportConstants.PERMISSION_ATTR);
                        String grant = el.attributeValue(ExportConstants.GRANT_ATTR);
                        String creator = el.attributeValue(ExportConstants.CREATOR_ATTR);
                        String beginStr = el.attributeValue(ExportConstants.BEGIN_ATTR);
                        Calendar begin = null;
                        if (beginStr != null) {
                            Date date = DateParser.parseW3CDateTime(beginStr);
                            begin = new GregorianCalendar();
                            begin.setTimeInMillis(date.getTime());
                        }
                        String endStr = el.attributeValue(ExportConstants.END_ATTR);
                        Calendar end = null;
                        if (endStr != null) {
                            Date date = DateParser.parseW3CDateTime(endStr);
                            end = new GregorianCalendar();
                            end.setTimeInMillis(date.getTime());
                        }
                        ACE ace = ACE.builder(username, permission)
                                     .isGranted(Boolean.parseBoolean(grant))
                                     .creator(creator)
                                     .begin(begin)
                                     .end(end)
                                     .build();
                        acl.add(ace);
                    }
                    acp.addACL(acl);
                    session.setACP(docModel.getRef(), acp, false);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadSchemas(ExportedDocument xdoc, DocumentModel docModel, Document doc) {
        SchemaManager schemaMgr = Framework.getService(SchemaManager.class);
        Iterator<Element> it = doc.getRootElement().elementIterator(ExportConstants.SCHEMA_TAG);
        while (it.hasNext()) {
            Element element = it.next();
            String schemaName = element.attributeValue(ExportConstants.NAME_ATTR);
            Schema schema = schemaMgr.getSchema(schemaName);
            if (schema == null) {
                log.warn("The document " + docModel.getName() + " with id=" + docModel.getId() + " and type="
                        + docModel.getDocumentType() + " contains the schema '" + schemaName
                        + "', which is not registered as available in the schemaManager. This schema will be ignored.");
                if (log.isDebugEnabled()) {
                    log.debug("Available schemas: " + Arrays.toString(schemaMgr.getSchemas()));
                }
                continue;
            }
            loadSchema(xdoc, schema, docModel, element);
        }
    }

    @SuppressWarnings("unchecked")
    protected static void loadSchema(ExportedDocument xdoc, Schema schema, DocumentModel doc, Element schemaElement) {
        String schemaName = schemaElement.attributeValue(ExportConstants.NAME_ATTR);
        Map<String, Object> data = new HashMap<>();
        Iterator<Element> it = schemaElement.elementIterator();
        while (it.hasNext()) {
            Element element = it.next();
            String name = element.getName();
            Field field = schema.getField(name);
            if (field == null) {
                throw new NuxeoException(
                        "Invalid input document. No such property was found " + name + " in schema " + schemaName);
            }
            Object value = getElementData(xdoc, element, field.getType());
            data.put(name, value);
        }
        Framework.doPrivileged(() -> doc.setProperties(schemaName, data));
    }

    protected static Class<?> getFieldClass(Type fieldType) {
        Class<?> klass = JavaTypes.getClass(fieldType);
        // for enumerated SimpleTypes we may need to lookup on the supertype
        // we do the recursion here and not in JavaTypes to avoid potential impacts
        if (klass == null) {
            assert fieldType.getSuperType() != null;
            return getFieldClass(fieldType.getSuperType());
        }
        return klass;
    }

    @SuppressWarnings("unchecked")
    private static Object getElementData(ExportedDocument xdoc, Element element, Type type) {
        // empty xml tag must be null value (not empty string)
        if (!element.hasContent()) {
            return null;
        }
        if (type.isSimpleType()) {
            return type.decode(element.getText());
        } else if (type.isListType()) {
            ListType ltype = (ListType) type;
            List<Object> list = new ArrayList<>();
            Iterator<Element> it = element.elementIterator();
            while (it.hasNext()) {
                Element el = it.next();
                list.add(getElementData(xdoc, el, ltype.getFieldType()));
            }
            Type ftype = ltype.getFieldType();
            if (ftype.isSimpleType()) { // these are stored as arrays
                Class<?> klass = getFieldClass(ftype);
                if (klass.isPrimitive()) {
                    return PrimitiveArrays.toPrimitiveArray(list, klass);
                } else {
                    return list.toArray((Object[]) Array.newInstance(klass, list.size()));
                }
            }
            return list;
        } else {
            ComplexType ctype = (ComplexType) type;
            if (TypeConstants.isContentType(ctype)) {
                String mimeType = element.elementText(ExportConstants.BLOB_MIME_TYPE);
                String encoding = element.elementText(ExportConstants.BLOB_ENCODING);
                String content = element.elementTextTrim(ExportConstants.BLOB_DATA);
                String filename = element.elementTextTrim(ExportConstants.BLOB_FILENAME);
                if ((content == null || content.length() == 0) && (mimeType == null || mimeType.length() == 0)) {
                    return null; // remove blob
                }
                Blob blob = null;
                if (xdoc.hasExternalBlobs()) {
                    blob = xdoc.getBlob(content);
                }
                if (blob == null) { // maybe the blob is embedded in Base64
                    // encoded data
                    byte[] bytes;
                    try {
                        bytes = Base64.decodeBase64(content);
                    } catch (IllegalArgumentException e) {
                        // example invalid base64: fd7b9e4.blob
                        if (log.isDebugEnabled()) {
                            log.warn("Invalid blob base64 in document: " + xdoc.getId() + ": " + StringUtils.abbreviate(content, 50));
                        } else {
                            log.warn("Invalid blob base64 in document: " + xdoc.getId());
                        }
                        bytes = new byte[0];
                    }
                    blob = Blobs.createBlob(bytes);
                }
                blob.setMimeType(mimeType);
                blob.setEncoding(encoding);
                blob.setFilename(filename);
                return blob;
            } else { // a complex type
                Map<String, Object> map = new HashMap<>();
                Iterator<Element> it = element.elementIterator();
                while (it.hasNext()) {
                    Element el = it.next();
                    String name = el.getName();
                    Object value = getElementData(xdoc, el, ctype.getField(el.getName()).getType());
                    map.put(name, value);
                }
                return map;
            }
        }
    }

}
