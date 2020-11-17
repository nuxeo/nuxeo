/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.DocumentType;

/**
 * Wrap a {@link DocumentModel} to expose in a pretty way more information to mvel scripts.
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

    public DocumentWrapper getParent() {
        DocumentModel parent = session.getParentDocument(doc.getRef());
        return parent != null ? new DocumentWrapper(session, parent) : null;
    }

    public DocumentWrapper getParent(String type) {
        DocumentModel parent = session.getParentDocument(doc.getRef());
        while (parent != null && !type.equals(parent.getType())) {
            parent = session.getParentDocument(parent.getRef());
        }
        if (parent == null) {
            return null;
        }
        return new DocumentWrapper(session, parent);
    }

    public DocumentWrapper getWorkspace() {
        return getParent("Workspace");
    }

    public DocumentWrapper getDomain() {
        return getParent("Domain");
    }

    public String getTitle() {
        return doc.getTitle();
    }

    public String getPath() {
        return doc.getPathAsString();
    }

    public String resolvePath(String relative) {
        return doc.getPath().append(relative).toString();
    }

    /**
     * @return the document ref
     * @since 5.6
     */
    public DocumentRef getRef() {
        return doc.getRef();
    }

    public DocumentRef resolvePathAsRef(String relative) {
        return new PathRef(doc.getPath().append(relative).toString());
    }

    public String getDescription() {
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

    public String getLifeCycle() {
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

    public boolean isDownloadable() {
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

    public Serializable getProperty(String key) {
        return doc.getPropertyValue(key);
    }

    /**
     * @since 5.7.3 Alias for #getProperty.
     */
    public Serializable getPropertyValue(String key) {
        return doc.getPropertyValue(key);
    }

    public void setProperty(String key, Serializable value) {
        doc.setPropertyValue(key, value);
    }

    /**
     * @since 5.7.3 Alias for #setProperty.
     */
    public void setPropertyValue(String key, Serializable value) {
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
        } catch (PropertyException e) {
            return false;
        }
    }

    /**
     * The behavior of this method was changed -&gt; it is checking if an xpath has a value attached.
     */
    @Override
    public boolean containsValue(Object value) {
        try {
            return doc.getProperty(value.toString()).getValue() != null;
        } catch (PropertyException e) {
            return false;
        }
    }

    @Override
    public Serializable get(Object key) {
        try {
            return doc.getProperty(key.toString()).getValue();
        } catch (PropertyException e) {
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
        Property p = doc.getProperty(key);
        Serializable v = p.getValue();
        p.setValue(value);
        return v;
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
