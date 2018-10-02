/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;
import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.runtime.api.Framework;

/**
 * A representation for an exported document.
 * <p>
 * It contains all the information needed to restore document data and state.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ExportedDocumentImpl implements ExportedDocument {

    private static final Random RANDOM = new SecureRandom();

    protected DocumentLocation srcLocation;

    // document unique ID
    protected String id;

    // document path
    protected Path path;

    // the main document
    protected Document document;

    // the external blobs if any
    protected final Map<String, Blob> blobs = new HashMap<>(4);

    // the optional attached documents
    protected final Map<String, Document> documents = new HashMap<>(4);

    public ExportedDocumentImpl() {
    }

    /**
     * @param path the path to use for this document this is used to remove full paths
     */
    public ExportedDocumentImpl(DocumentModel doc, Path path, boolean inlineBlobs) throws IOException {
        id = doc.getId();
        if (path == null) {
            this.path = new Path("");
        } else {
            this.path = path.makeRelative();
        }
        readDocument(doc, inlineBlobs);
        srcLocation = new DocumentLocationImpl(doc);
    }

    public ExportedDocumentImpl(DocumentModel doc) throws IOException {
        this(doc, false);
    }

    public ExportedDocumentImpl(DocumentModel doc, boolean inlineBlobs) throws IOException {
        this(doc, doc.getPath(), inlineBlobs);
    }

    /**
     * @return the source DocumentLocation
     */
    @Override
    public DocumentLocation getSourceLocation() {
        return srcLocation;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void setPath(Path path) {
        this.path = path;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return document.getRootElement().element(ExportConstants.SYSTEM_TAG).elementText("type");
    }

    @Override
    public Document getDocument() {
        return document;
    }

    @Override
    public void setDocument(Document document) {
        this.document = document;
        id = document.getRootElement().attributeValue(ExportConstants.ID_ATTR);
        String repName = document.getRootElement().attributeValue(ExportConstants.REP_NAME);
        srcLocation = new DocumentLocationImpl(repName, new IdRef(id));
    }

    @Override
    public Map<String, Blob> getBlobs() {
        return blobs;
    }

    @Override
    public void putBlob(String blobId, Blob blob) {
        blobs.put(blobId, blob);
    }

    @Override
    public Blob removeBlob(String blobId) {
        return blobs.remove(blobId);
    }

    @Override
    public Blob getBlob(String blobId) {
        return blobs.get(blobId);
    }

    @Override
    public boolean hasExternalBlobs() {
        return !blobs.isEmpty();
    }

    @Override
    public Map<String, Document> getDocuments() {
        return documents;
    }

    @Override
    public Document getDocument(String docId) {
        return documents.get(docId);
    }

    @Override
    public void putDocument(String docId, Document doc) {
        documents.put(docId, doc);
    }

    @Override
    public Document removeDocument(String docId) {
        return documents.remove(docId);
    }

    /**
     * @return the number of files describing the document.
     */
    @Override
    public int getFilesCount() {
        return 1 + documents.size() + blobs.size();
    }

    protected void readDocument(DocumentModel doc, boolean inlineBlobs) throws IOException {
        document = DocumentFactory.getInstance().createDocument();
        document.setName(doc.getName());
        Element rootElement = document.addElement(ExportConstants.DOCUMENT_TAG);
        rootElement.addAttribute(ExportConstants.REP_NAME, doc.getRepositoryName());
        rootElement.addAttribute(ExportConstants.ID_ATTR, doc.getRef().toString());
        Element systemElement = rootElement.addElement(ExportConstants.SYSTEM_TAG);
        systemElement.addElement(ExportConstants.TYPE_TAG).addText(doc.getType());
        systemElement.addElement(ExportConstants.PATH_TAG).addText(path.toString());
        // lifecycle
        readLifeCycleInfo(systemElement, doc);

        // facets
        readFacets(systemElement, doc);
        // write security
        Element acpElement = systemElement.addElement(ExportConstants.ACCESS_CONTROL_TAG);
        ACP acp = doc.getACP();
        if (acp != null) {
            readACP(acpElement, acp);
        }
        // write schemas
        readDocumentSchemas(rootElement, doc, inlineBlobs);
    }

    protected void readLifeCycleInfo(Element element, DocumentModel doc) {
        String lifeCycleState = doc.getCurrentLifeCycleState();
        if (lifeCycleState != null && lifeCycleState.length() > 0) {
            element.addElement(ExportConstants.LIFECYCLE_STATE_TAG).addText(lifeCycleState);
        }
        String lifeCyclePolicy = doc.getLifeCyclePolicy();
        if (lifeCyclePolicy != null && lifeCyclePolicy.length() > 0) {
            element.addElement(ExportConstants.LIFECYCLE_POLICY_TAG).addText(lifeCyclePolicy);
        }
    }

    protected void readFacets(Element element, DocumentModel doc) {
        // facets
        for (String facet : doc.getFacets()) {
            element.addElement(ExportConstants.FACET_TAG).addText(facet);
        }
    }

    protected void readDocumentSchemas(Element element, DocumentModel doc, boolean inlineBlobs) throws IOException {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        String[] schemaNames = doc.getSchemas();
        for (String schemaName : schemaNames) {
            Element schemaElement = element.addElement(ExportConstants.SCHEMA_TAG).addAttribute("name", schemaName);
            Schema schema = schemaManager.getSchema(schemaName);
            Namespace targetNs = schema.getNamespace();
            // If namespace prefix is empty, use schema name
            if (StringUtils.isEmpty(targetNs.prefix)) {
                targetNs = new Namespace(targetNs.uri, schema.getName());
            }
            schemaElement.addNamespace(targetNs.prefix, targetNs.uri);
            DataModel dataModel = doc.getDataModel(schemaName);
            for (Field field : schema.getFields()) {
                Object value = dataModel.getData(field.getName().getLocalName());
                readProperty(schemaElement, targetNs, field, value, inlineBlobs);
            }
        }

    }

    protected void readProperty(Element parent, Namespace targetNs, Field field, Object value, boolean inlineBlobs)
            throws IOException {
        if (value == null) {
            return; // have no content
        }
        Type type = field.getType();
        QName name = QName.get(field.getName().getLocalName(), targetNs.prefix, targetNs.uri);
        Element element = parent.addElement(name);

        // extract the element content
        if (type.isSimpleType()) {
            // use CDATA to avoid any bad interaction between content and envelope
            String encodedValue = type.encode(value);
            if (encodedValue != null) {
                // workaround embedded CDATA
                encodedValue = encodedValue.replaceAll("]]>", "]]]]><![CDATA[>");
            }
            element.addCDATA(encodedValue);
        } else if (type.isComplexType()) {
            ComplexType ctype = (ComplexType) type;
            if (TypeConstants.isContentType(ctype)) {
                readBlob(element, ctype, (Blob) value, inlineBlobs);
            } else {
                readComplex(element, ctype, (Map) value, inlineBlobs);
            }
        } else if (type.isListType()) {
            if (value instanceof List) {
                readList(element, (ListType) type, (List) value, inlineBlobs);
            } else if (value.getClass().getComponentType() != null) {
                readList(element, (ListType) type, PrimitiveArrays.toList(value), inlineBlobs);
            } else {
                throw new IllegalArgumentException("A value of list type is neither list neither array: " + value);
            }
        }
    }

    protected final void readBlob(Element element, ComplexType ctype, Blob blob, boolean inlineBlobs)
            throws IOException {
        String blobPath = Integer.toHexString(RANDOM.nextInt()) + ".blob";
        element.addElement(ExportConstants.BLOB_ENCODING).addText(blob.getEncoding() != null ? blob.getEncoding() : "");
        element.addElement(ExportConstants.BLOB_MIME_TYPE)
               .addText(blob.getMimeType() != null ? blob.getMimeType() : "");
        element.addElement(ExportConstants.BLOB_FILENAME).addText(blob.getFilename() != null ? blob.getFilename() : "");
        Element data = element.addElement(ExportConstants.BLOB_DATA);
        if (inlineBlobs) {
            String content = Base64.encodeBase64String(blob.getByteArray());
            data.setText(content);
        } else {
            data.setText(blobPath);
            blobs.put(blobPath, blob);
        }
        element.addElement(ExportConstants.BLOB_DIGEST).addText(blob.getDigest() != null ? blob.getDigest() : "");
    }

    protected final void readComplex(Element element, ComplexType ctype, Map map, boolean inlineBlobs)
            throws IOException {
        Iterator<Map.Entry> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = it.next();
            readProperty(element, ctype.getNamespace(), ctype.getField(entry.getKey().toString()), entry.getValue(),
                    inlineBlobs);
        }
    }

    protected final void readList(Element element, ListType ltype, List list, boolean inlineBlobs) throws IOException {
        Field field = ltype.getField();
        for (Object obj : list) {
            readProperty(element, Namespace.DEFAULT_NS, field, obj, inlineBlobs);
        }
    }

    protected static void readACP(Element element, ACP acp) {
        ACL[] acls = acp.getACLs();
        for (ACL acl : acls) {
            Element aclElement = element.addElement(ExportConstants.ACL_TAG);
            aclElement.addAttribute(ExportConstants.NAME_ATTR, acl.getName());
            ACE[] aces = acl.getACEs();
            for (ACE ace : aces) {
                Element aceElement = aclElement.addElement(ExportConstants.ACE_TAG);
                aceElement.addAttribute(ExportConstants.PRINCIPAL_ATTR, ace.getUsername());
                aceElement.addAttribute(ExportConstants.PERMISSION_ATTR, ace.getPermission());
                aceElement.addAttribute(ExportConstants.GRANT_ATTR, String.valueOf(ace.isGranted()));
                aceElement.addAttribute(ExportConstants.CREATOR_ATTR, ace.getCreator());
                Calendar begin = ace.getBegin();
                if (begin != null) {
                    aceElement.addAttribute(ExportConstants.BEGIN_ATTR,
                            DateParser.formatW3CDateTime((begin).getTime()));
                }
                Calendar end = ace.getEnd();
                if (end != null) {
                    aceElement.addAttribute(ExportConstants.END_ATTR, DateParser.formatW3CDateTime((end).getTime()));
                }
            }
        }
    }

}
