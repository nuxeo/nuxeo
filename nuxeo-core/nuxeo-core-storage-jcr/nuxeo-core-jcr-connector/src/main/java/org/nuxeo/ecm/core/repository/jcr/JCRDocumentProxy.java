/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentVersionProxy;
import org.nuxeo.ecm.core.model.EmptyDocumentIterator;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.repository.jcr.versioning.Versioning;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * An implementation of the IDocument over a JackRabbit node.
 * <p>
 * Please note that this is not a JCR implementation of the IDocument, but
 * rather a JackRabbit one - this way we can benefit from JackRabbit internal
 * API to optimize some operations.
 * <p>
 * Thus, the underlying node of the IDocument is of type {@link Node} and not
 * {@link Node}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JCRDocumentProxy extends JCRDocument implements
        DocumentVersionProxy {
    // private static Log log = LogFactory.getLog(JCRDocument.class);

    // referred doc
    final JCRDocument doc;

    /**
     * Constructs a document that wraps the given JCR node.
     *
     * @param session the current session
     * @param node the JCR node to wrap
     * @throws RepositoryException if any JCR exception occurs
     */
    public JCRDocumentProxy(JCRSession session, Node node)
            throws RepositoryException {
        super(session, node);
        javax.jcr.Property frozenNode = node.getProperty(NodeConstants.ECM_REF_FROZEN_NODE.rawname);
        doc = Versioning.getService().newDocumentVersion(session,
                frozenNode.getNode());
        type = doc.type;
    }

    public String getTargetDocumentUUID() throws DocumentException {
        try {
            return node.getProperty(NodeConstants.ECM_REF_UUID.rawname).getString();
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "Failed to get the ref doc uuid for proxy", e);
        }
    }

    // protected JCRDocument(JCRSession workspace, Node node, DocumentType type)
    // throws RepositoryException {
    // this.session = workspace;
    // this.node = node;
    // this.type = type;
    // }

    public DocumentVersion getTargetVersion() {
        return (DocumentVersion) doc;
    }

    public void updateToBaseVersion() throws DocumentException {
        try {
            JCRDocument doc = (JCRDocument) getTargetDocument();
            Node baseVersion = doc.getNode().getProperty("jcr:baseVersion").getNode();
            if (!baseVersion.getUUID().equals(node.getParent().getUUID())) {
                // not the same version update to base version
                Node frozenNode = baseVersion.getNode("jcr:frozenNode");
                node.setProperty(NodeConstants.ECM_REF_FROZEN_NODE.rawname,
                        frozenNode);
                // FIXME: review this line below.
                doc = new JCRDocument(session, frozenNode);
            }
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "Failed to update proxy to base version", e);
        }
    }

    public Document getTargetDocument() throws DocumentException {
        return session.getDocumentByUUID(getTargetDocumentUUID());
    }

    @Override
    public boolean isFolder() {
        return false; // proxy docs are not folders
    }

    @Override
    public DocumentType getType() {
        return doc.type;
    }

    @Override
    public JCRDocument getDocument() {
        return doc.getDocument();
    }

    @Override
    public void removeChild(String name) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are not folders");
    }

    @Override
    public void orderBefore(String src, String dest) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are not folders");
    }

    @Override
    public Document addChild(String name, String typeName)
            throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are not folders");
    }

    @Override
    public Document getChild(String name) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are not folders");
    }

    @Override
    public Iterator<Document> getChildren() throws DocumentException {
        return EmptyDocumentIterator.INSTANCE;
    }

    @Override
    public boolean hasChild(String name) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are not folders");
    }

    @Override
    public boolean hasChildren() throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are not folders");
    }

    // Version related functions

    @Override
    public void checkIn(String label) throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call checkin on proxy docs");
    }

    @Override
    public void checkIn(String label, String description)
            throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call checkin on proxy docs");
    }

    @Override
    public void checkOut() throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call checkout on proxy docs");
    }

    @Override
    public boolean isCheckedOut() throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call isCheckedOut on proxy docs");
    }

    public Version getVersionByLabel(String label) {
        throw new UnsupportedOperationException(
                "cannot call getVersionByLabel on proxy docs");
    }

    public void restore(DocumentVersion documentVersion) {
        throw new UnsupportedOperationException(
                "cannot call restore on proxy docs");
    }

    @Override
    public void restore(String label) throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call restore on proxy docs");
    }

    @Override
    public Document getVersion(String label) throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call getVersion on proxy docs");
    }

    public Document getVersion(DocumentVersion version) {
        throw new UnsupportedOperationException(
                "cannot call getVersion on proxy docs");
    }

    @Override
    public DocumentVersionIterator getVersions() throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call getVersions on proxy docs");
    }

    // END - Version related function

    // ------------- property management -------------------

    @Override
    public boolean getBoolean(String name) throws DocumentException {
        return doc.getBoolean(name);
    }

    @Override
    public Blob getContent(String name) throws DocumentException {
        return doc.getContent(name);
    }

    @Override
    public Calendar getDate(String name) throws DocumentException {
        return doc.getDate(name);
    }

    @Override
    public double getDouble(String name) throws DocumentException {
        return doc.getDouble(name);
    }

    @Override
    public long getLong(String name) throws DocumentException {
        return doc.getLong(name);
    }

    @Override
    public Property getProperty(String name) throws DocumentException {
        return doc.getProperty(name);
    }

    @Override
    public void setPropertyValue(String name, Object value)
            throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public Object getPropertyValue(String name) throws DocumentException {
        return doc.getPropertyValue(name);
    }

    @Override
    public Collection<Property> getProperties() throws DocumentException {
        return doc.getProperties();
    }

    @Override
    public Iterator<Property> getPropertyIterator() throws DocumentException {
        return doc.getPropertyIterator();
    }

    @Override
    public Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException {
        return doc.exportMap(schemas);
    }

    @Override
    public Map<String, Object> exportMap(String schemaName)
            throws DocumentException {
        return doc.exportMap(schemaName);
    }

    @Override
    public final Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException {
        return doc.exportFlatMap(schemas);
    }

    @Override
    public void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public void importFlatMap(Map<String, Object> map) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public String getString(String name) throws DocumentException {
        return doc.getString(name);
    }

    @Override
    public boolean isPropertySet(String path) throws DocumentException {
        return doc.isPropertySet(path);
    }

    @Override
    public void removeProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public void setBoolean(String name, boolean value) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public void setContent(String name, Blob value) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public void setDate(String name, Calendar value) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public void setDouble(String name, double value) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public void setLong(String name, long value) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public void setString(String name, String value) throws DocumentException {
        throw new UnsupportedOperationException("proxy docs are read only");
    }

    @Override
    public Field getField(String name) {
        return doc.getField(name);
    }

    @Override
    public ComplexType getSchema(String schema) {
        return doc.getSchema(schema);
    }

    @Override
    public Node connect() throws DocumentException {
        return doc.connect();
    }

    @Override
    public boolean isConnected() {
        return doc.isConnected();
    }

    public Node getTargetNode() {
        return doc.getNode();
    }

    @Override
    public Collection<Field> getFields() {
        return doc.getFields();
    }

    @Override
    public boolean hasVersions() throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call hasVersions on proxy docs");
    }

    @Override
    public DocumentVersion getLastVersion() throws DocumentException {
        throw new UnsupportedOperationException(
                "cannot call getLastVersions on proxy docs");
    }

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        return getTargetVersion();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof JCRDocumentProxy) {
            return node == ((JCRDocumentProxy) obj).node;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public String getCurrentLifeCycleState() throws LifeCycleException {
        // Life cycle state for a proxy is the life cycle state of the reference
        return doc.getCurrentLifeCycleState();
    }

    @Override
    public String getLifeCyclePolicy() throws LifeCycleException {
        // Life cycle policy for a proxy is the life cycle state of the
        // reference
        return doc.getLifeCyclePolicy();
    }

    @Override
    public boolean followTransition(String transition)
            throws LifeCycleException {
        throw new UnsupportedOperationException(
                "proxy docs cannot follow life cycle transition");
    }

    @Override
    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
        // No possible state transitions for proxy docs.
        return new ArrayList<String>();
    }

    public void setTargetDocument(Document document, String label) {
        throw new UnsupportedOperationException();
    }

}
