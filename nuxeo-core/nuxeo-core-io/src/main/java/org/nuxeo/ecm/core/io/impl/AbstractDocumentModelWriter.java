/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.io.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.versioning.VersioningService;
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

    private final Map<DocumentLocation, DocumentLocation> translationMap = new HashMap<DocumentLocation, DocumentLocation>();

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

    protected void saveIfNeeded() throws ClientException {
        if (unsavedDocuments >= saveInterval) {
            session.save();
            unsavedDocuments = 0;
        }
    }

    @Override
    public void close() {
        if (unsavedDocuments > 0) {
            try {
                session.save();
            } catch (ClientException e) {
                log.error(e, e);
            }
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
    protected DocumentModel createDocument(ExportedDocument xdoc, Path toPath) throws ClientException {
        Path parentPath = toPath.removeLastSegments(1);
        String name = toPath.lastSegment();

        DocumentModel doc = new DocumentModelImpl(parentPath.toString(), name, xdoc.getType());

        // set lifecycle state at creation
        Element system = xdoc.getDocument().getRootElement().element(ExportConstants.SYSTEM_TAG);
        String lifeCycleState = system.element(ExportConstants.LIFECYCLE_STATE_TAG).getText();
        doc.putContextData("initialLifecycleState", lifeCycleState);

        // loadFacets before schemas so that additional schemas are not skipped
        loadFacetsInfo(doc, xdoc.getDocument());

        // then load schemas data
        loadSchemas(xdoc, doc, xdoc.getDocument());

        if (doc.hasSchema("uid")) {
            doc.putContextData(ScopeType.REQUEST, VersioningService.SKIP_VERSIONING, true);
        }

        doc = session.createDocument(doc);

        // load into the document the system properties, document needs to exist
        loadSystemInfo(doc, xdoc.getDocument());

        unsavedDocuments += 1;
        saveIfNeeded();

        return doc;
    }

    /**
     * Updates an existing document.
     */
    protected DocumentModel updateDocument(ExportedDocument xdoc, DocumentModel doc) throws ClientException {
        // load schemas data
        loadSchemas(xdoc, doc, xdoc.getDocument());

        loadFacetsInfo(doc, xdoc.getDocument());

        doc = session.saveDocument(doc);

        unsavedDocuments += 1;
        saveIfNeeded();

        return doc;
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public void setSaveInterval(int saveInterval) {
        this.saveInterval = saveInterval;
    }

    @SuppressWarnings("unchecked")
    protected boolean loadFacetsInfo(DocumentModel docModel, Document doc) throws ClientException {
        boolean added = false;
        Element system = doc.getRootElement().element(ExportConstants.SYSTEM_TAG);
        if (system == null) {
            return false;
        }

        Iterator<Element> facets = system.elementIterator(ExportConstants.FACET_TAG);
        while (facets.hasNext()) {
            Element element = facets.next();
            String facet = element.getTextTrim();
            if (!docModel.hasFacet(facet)) {
                docModel.addFacet(facet);
                added = true;
            }
        }

        return added;
    }

    @SuppressWarnings("unchecked")
    protected void loadSystemInfo(DocumentModel docModel, Document doc) throws ClientException {
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
                    for (int i = 0; i < size; i++) {
                        Element el = entries.get(i);
                        String username = el.attributeValue(ExportConstants.PRINCIPAL_ATTR);
                        String permission = el.attributeValue(ExportConstants.PERMISSION_ATTR);
                        String grant = el.attributeValue(ExportConstants.GRANT_ATTR);
                        ACE ace = new ACE(username, permission, Boolean.parseBoolean(grant));
                        acl.add(ace);
                    }
                    acp.addACL(acl);
                    session.setACP(docModel.getRef(), acp, false);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadSchemas(ExportedDocument xdoc, DocumentModel docModel, Document doc) throws ClientException {
        SchemaManager schemaMgr = Framework.getLocalService(SchemaManager.class);
        Iterator<Element> it = doc.getRootElement().elementIterator(ExportConstants.SCHEMA_TAG);
        while (it.hasNext()) {
            Element element = it.next();
            String schemaName = element.attributeValue(ExportConstants.NAME_ATTR);
            Schema schema = schemaMgr.getSchema(schemaName);
            if (schema == null) {
                throw new ClientException("Schema not found: " + schemaName);
            }
            loadSchema(xdoc, schema, docModel, element);
        }
    }

    @SuppressWarnings("unchecked")
    protected static void loadSchema(ExportedDocument xdoc, Schema schema, DocumentModel doc, Element schemaElement)
            throws ClientException {
        String schemaName = schemaElement.attributeValue(ExportConstants.NAME_ATTR);
        Map<String, Object> data = new HashMap<String, Object>();
        Iterator<Element> it = schemaElement.elementIterator();
        while (it.hasNext()) {
            Element element = it.next();
            String name = element.getName();
            Field field = schema.getField(name);
            if (field == null) {
                throw new ClientException("Invalid input document. No such property was found " + name + " in schema "
                        + schemaName);
            }
            Object value = getElementData(xdoc, element, field.getType());
            data.put(name, value);
        }
        doc.setProperties(schemaName, data);
    }

    protected static Class getFieldClass(Type fieldType) {
        Class klass = JavaTypes.getClass(fieldType);
        // for enumerated SimpleTypes we may need to lookup on the supertype
        // we do the recursion here and not in JavaTypes to avoid potential impacts
        if (klass == null && fieldType.getSuperType() != null) {
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
            List<Object> list = new ArrayList<Object>();
            Iterator<Element> it = element.elementIterator();
            while (it.hasNext()) {
                Element el = it.next();
                list.add(getElementData(xdoc, el, ltype.getFieldType()));
            }
            Type ftype = ltype.getFieldType();
            if (ftype.isSimpleType()) { // these are stored as arrays
                Class klass = getFieldClass(ftype);
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
                    byte[] bytes = Base64.decode(content);
                    blob = Blobs.createBlob(bytes);
                }
                blob.setMimeType(mimeType);
                blob.setEncoding(encoding);
                blob.setFilename(filename);
                return blob;
            } else { // a complex type
                Map<String, Object> map = new HashMap<String, Object>();
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
