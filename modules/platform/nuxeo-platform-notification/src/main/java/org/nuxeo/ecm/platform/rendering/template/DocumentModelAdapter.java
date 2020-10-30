/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.DocumentModel;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentModelAdapter implements TemplateHashModelEx, AdapterTemplateModel {

    protected final DocumentModel doc;

    protected final ObjectWrapper wrapper;

    private TemplateCollectionModel keys;

    private int size = -1;

    // id, name, path, type, schemas, facets, system, schema1, schema2, ...

    public DocumentModelAdapter(DocumentModel doc, ObjectWrapper wrapper) {
        this.doc = doc;
        this.wrapper = wrapper;
    }

    public DocumentModelAdapter(DocumentModel doc) {
        this(doc, ObjectWrapper.DEFAULT_WRAPPER);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdaptedObject(Class hint) {
        return doc;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        DocumentFieldAccessor accessor = DocumentFieldAccessor.get(key);
        if (accessor != null) {
            return wrapper.wrap(accessor.getValue(doc));
        }
        // may be a schema name (doc.dublincore.title)
        Map<String, Object> properties = doc.getProperties(key);
        if (properties != null) {
            return wrapper.wrap(unPrefixedMap(properties));
        }
        return wrapper.wrap(null);
    }

    private static Map<String, Object> unPrefixedMap(Map<String, Object> props) {
        Map<String, Object> res = new HashMap<String, Object>();
        for (Entry<String, Object> e : props.entrySet()) {
            String key = e.getKey();
            int pos = key.indexOf(':');
            if (pos > -1) {
                key = key.substring(pos + 1);
            }
            res.put(key, e.getValue());
        }
        return res;
    }

    /**
     * a doc model is never empty
     */
    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateModelException {
        if (keys == null) {
            List<String> keysCol = new ArrayList<>();
            keysCol.addAll(DocumentFieldAccessor.getFieldNames());
            String[] schemas = doc.getSchemas();
            keysCol.addAll(Arrays.asList(schemas));
            size = keysCol.size();
            keys = (TemplateCollectionModel) wrapper.wrap(keysCol);
        }
        return keys;
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        List<Object> values = new ArrayList<>();
        for (DocumentFieldAccessor accessor : DocumentFieldAccessor.getAcessors()) {
            values.add(accessor.getValue(doc));
        }
        for (String schema : doc.getSchemas()) {
            Map<String, Object> properties = doc.getProperties(schema);
            if (properties != null) {
                values.add(properties);
            }
        }
        return (TemplateCollectionModel) wrapper.wrap(values);
    }

    @Override
    public int size() throws TemplateModelException {
        if (size == -1) {
            size = DocumentFieldAccessor.getAcessorsCount() + doc.getSchemas().length;
        }
        return size;
    }

}
