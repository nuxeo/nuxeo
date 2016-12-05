/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.automation.scripting.internals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.DocumentType;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.objects.NativeArray;

/**
 * Wrap a {@link DocumentModel} to expose in a pretty way more information to automation scripts.
 *
 * @since 8.4
 */
public class DocumentScriptingWrapper extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    protected final AutomationMapper mapper;

    protected final DocumentModel doc;

    public static Object wrap(Object object, AutomationMapper mapper) {
        if (object == null) {
            return null;
        }
        if (object instanceof DocumentModel) {
            return new DocumentScriptingWrapper(mapper, (DocumentModel) object);
        } else if (object instanceof DocumentModelList) {
            List<DocumentScriptingWrapper> docs = new ArrayList<>();
            for (DocumentModel doc : (DocumentModelList) object) {
                docs.add(new DocumentScriptingWrapper(mapper, doc));
            }
            return docs;
        } else if (object instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) object;
            return wrap(m, mapper);
        }
        return object;
    }

    public static Map<String, Object> wrap(Map<String, Object> source, AutomationMapper mapper) {
        return source.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> wrap(e.getValue(), mapper)));
    }

    public static Object unwrap(Object object) {
        // First unwrap object if it's a nashorn object
        Object result = object;
        if (result instanceof ScriptObjectMirror) {
            result = ScriptObjectMirrors.unwrap((ScriptObjectMirror) result);
        }
        // TODO: not sure if this code is used, but we shouldn't use NativeArray as it's an internal class of nashorn
        if (result instanceof NativeArray) {
            result = Arrays.asList(((NativeArray) result).asObjectArray());
        }
        // Second unwrap object
        if (result instanceof DocumentScriptingWrapper) {
            result = ((DocumentScriptingWrapper) result).getDoc();
        } else if (result instanceof List<?>) {
            List<?> l = (List<?>) result;
            // Several possible cases here:
            // - l is of type DocumentModelList or BlobList -> already in right type
            // - l is a list of DocumentScriptingWrapper -> elements need to be unwrapped into a DocumentModelList
            // - l is a list of DocumentWrapper -> l needs to be converted to DocumentModelList
            // - l is a list of Blob -> l needs to be converted to BlobList
            // - l is a list -> do nothing
            if (l.size() > 0 && !(result instanceof DocumentModelList || result instanceof BlobList)) {
                Object first = l.get(0);
                if (first instanceof DocumentModel) {
                    result = l.stream().map(DocumentModel.class::cast)
                            .collect(Collectors.toCollection(DocumentModelListImpl::new));
                } else if (first instanceof Blob) {
                    result = l.stream().map(Blob.class::cast).collect(Collectors.toCollection(BlobList::new));
                } else if (first instanceof DocumentScriptingWrapper) {
                    result = l.stream().map(DocumentScriptingWrapper.class::cast).map(DocumentScriptingWrapper::getDoc)
                            .collect(Collectors.toCollection(DocumentModelListImpl::new));
                }
            }
        } else if (result instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            result = computeProperties(unwrap(map));
        }
        return result;
    }

    protected static Properties computeProperties(Map<?, ?> result) {
        DataModelProperties props = new DataModelProperties();
        for (Entry<?, ?> entry : result.entrySet()) {
            props.getMap().put(entry.getKey().toString(), (Serializable) entry.getValue());
        }
        return props;
    }

    public static Map<String, Object> unwrap(Map<String, Object> source) {
        return source.entrySet().stream().filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> unwrap(e.getValue())));
    }

    public DocumentScriptingWrapper(AutomationMapper mapper, DocumentModel doc) {
        this.mapper = mapper;
        this.doc = doc;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public CoreSession getSession() {
        return mapper.ctx.getCoreSession();
    }

    public DocumentScriptingWrapper getParent() {
        DocumentModel parent = getSession().getParentDocument(doc.getRef());
        return parent != null ? new DocumentScriptingWrapper(mapper, parent) : null;
    }

    public DocumentScriptingWrapper getParent(String type) {
        DocumentModel parent = getSession().getParentDocument(doc.getRef());
        while (parent != null && !type.equals(parent.getType())) {
            parent = getSession().getParentDocument(parent.getRef());
        }
        if (parent == null) {
            return null;
        }
        return new DocumentScriptingWrapper(mapper, parent);
    }

    public DocumentScriptingWrapper getWorkspace() {
        return getParent("Workspace");
    }

    public DocumentScriptingWrapper getDomain() {
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
     * Alias for #getProperty.
     */
    public Serializable getPropertyValue(String key) {
        return doc.getPropertyValue(key);
    }

    public void setProperty(String key, Serializable value) {
        doc.setPropertyValue(key, value);
    }

    /**
     * Alias for #setProperty.
     */
    public void setPropertyValue(String key, Serializable value) {
        doc.setPropertyValue(key, value);
    }

    /**
     * Used by nashorn for native javascript array/date.
     */
    public void setPropertyValue(String key, ScriptObjectMirror value) {
        doc.setPropertyValue(key, (Serializable) ScriptObjectMirrors.unwrap(value));
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
     * The behavior of this method was changed -> it is checking if an xpath has a value attached.
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
        return Stream.of(doc.getParts()).collect(Collectors.summingInt(part -> part.size()));
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(Stream.of(doc.getSchemas())
                .map(name -> doc.getProperties(name).keySet().stream()).flatMap(s -> s).collect(Collectors.toSet()));
    }

    @Override
    public Collection<Object> values() {
        return Collections.unmodifiableCollection(Stream.of(doc.getSchemas())
                .map(name -> doc.getProperties(name).values().stream()).flatMap(s -> s).collect(Collectors.toSet()));
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return Collections.unmodifiableSet(Stream.of(doc.getSchemas())
                .flatMap(name -> doc.getProperties(name).entrySet().stream()).collect(Collectors.toSet()));
    }

    /**
     * As we need to handle {@link ScriptObjectMirror} for array type from nashorn.
     */
    @Override
    public Object put(String key, Object value) {
        if (value instanceof ScriptObjectMirror) {
            return put(key, (Serializable) ScriptObjectMirrors.unwrap((ScriptObjectMirror) value));
        }
        return put(key, (Serializable) value);
    }

    public Serializable put(String key, Serializable value) {
        Property p = doc.getProperty(key);
        Serializable v = p.getValue();
        p.setValue(value);
        return v;
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
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

    @Override
    public String toString() {
        return doc.toString();
    }

}
