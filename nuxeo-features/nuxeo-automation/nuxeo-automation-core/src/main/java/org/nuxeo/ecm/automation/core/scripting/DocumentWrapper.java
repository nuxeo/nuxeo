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
 */
package org.nuxeo.ecm.automation.core.scripting;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.DocumentType;

/**
 * Wrap a {@link DocumentModel} to expose in a pretty way more information to
 * mvel scripts.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentWrapper extends HashMap<String, Serializable> {

    private static final long serialVersionUID = 1L;

    protected final CoreSession session;

    protected final DocumentModel doc;

    public DocumentWrapper(CoreSession session, DocumentModel doc) {
        this.session = session;
        this.doc = doc;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public CoreSession getSession() {
        return session;
    }

    public DocumentWrapper getParent() throws Exception {
        DocumentModel parent = session.getParentDocument(doc.getRef());
        return parent != null ? new DocumentWrapper(session, parent) : null;
    }

    public DocumentWrapper getParent(String type) throws Exception {
        DocumentModel parent = doc;
        while (parent != null && !type.equals(parent.getType())) {
            parent = session.getParentDocument(doc.getRef());
        }
        return parent != null ? new DocumentWrapper(session, parent) : null;
    }

    public DocumentWrapper getWorkspace() throws Exception {
        return getParent("Workspace");
    }

    public DocumentWrapper getDomain() throws Exception {
        return getParent("Domain");
    }

    public String getTitle() throws Exception {
        return doc.getTitle();
    }

    public String getPath() {
        return doc.getPathAsString();
    }

    public String resolvePath(String relative) {
        return doc.getPath().append(relative).toString();
    }

    public DocumentRef resolvePathAsRef(String relative) {
        return new PathRef(doc.getPath().append(relative).toString());
    }

    public String getDescription() throws Exception {
        return (String) doc.getPropertyValue("dc:description");
    }

    public boolean hasFacet(String facet) {
        return doc.hasFacet(facet);
    }

    public boolean hasSchema(String schema) {
        return doc.hasSchema(schema);
    }

    public boolean addFacet(String facet) {
        return doc.addFacet(facet);
    }

    public boolean removeFacet(String facet) {
        return doc.removeFacet(facet);
    }

    public String getType() {
        return doc.getType();
    }

    public DocumentType getDocumentType() {
        return doc.getDocumentType();
    }

    public String getLifeCycle() throws Exception {
        return doc.getCurrentLifeCycleState();
    }

    public boolean isLocked() {
        return doc.isLocked();
    }

    public boolean isFolder() {
        return doc.isFolder();
    }

    public boolean isImmutable() {
        return doc.isImmutable();
    }

    public boolean isProxy() {
        return doc.isProxy();
    }

    public boolean isVersion() {
        return doc.isVersion();
    }

    public boolean isDownloadable() throws Exception {
        return doc.isDownloadable();
    }

    public boolean isVersionable() {
        return doc.isVersionable();
    }

    public String getId() {
        return doc.getId();
    }

    public String getName() {
        return doc.getName();
    }

    public String[] getSchemas() {
        return doc.getSchemas();
    }

    public Set<String> getFacets() {
        return doc.getFacets();
    }

    public Serializable getProperty(String key) throws Exception {
        return doc.getPropertyValue(key);
    }

    public void setProperty(String key, Serializable value) throws Exception {
        doc.setPropertyValue(key, value);
    }

    public String getVersionLabel() {
        return doc.getVersionLabel();
    }

    /** property map implementation */

    @Override
    public boolean containsKey(Object key) {
        try {
            doc.getProperty(key.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * The behavior of this method was changed -> it is checking if an xpath
     * has a value attached.
     */
    @Override
    public boolean containsValue(Object value) {
        try {
            return doc.getProperty(value.toString()).getValue() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Serializable get(Object key) {
        try {
            return doc.getProperty(key.toString()).getValue();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Collection<Serializable> values() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Set<Map.Entry<String, Serializable>> entrySet() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Serializable put(String key, Serializable value) {
        try {
            Property p = doc.getProperty(key);
            Serializable v = p.getValue();
            p.setValue(value);
            return v;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Serializable> m) {
        throw new UnsupportedOperationException("Read Only Map.");
    }

    @Override
    public Serializable remove(Object key) {
        throw new UnsupportedOperationException("Read Only Map.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Read Only Map.");
    }

}
