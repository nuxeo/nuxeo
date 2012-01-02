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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.PropertyException;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
        DocumentPart part;
        try {
            part = doc.getPart(key);
        } catch (ClientException e1) {
            throw new TemplateModelException(e1);
        }
        if (part != null) {
            // TODO it is easier for now to export the part as a map
            // may be in future we may want to implement a property template model
            try {
                Map<String, Serializable> map = (Map<String, Serializable>) part.getValue();
                return wrapper.wrap(unPrefixedMap(map));
            } catch (PropertyException e) {
                throw new TemplateModelException(
                        "Failed to get value for schema root property: "+key, e);
            }
        }
        return wrapper.wrap(null);
    }

    private static Map<String, Serializable> unPrefixedMap(
            Map<String, Serializable> map) {
        Map<String, Serializable> res = new HashMap<String, Serializable>();
        for (Entry<String, Serializable> e : map.entrySet()) {
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
            List<String> keysCol = new ArrayList<String>();
            keysCol.addAll(DocumentFieldAccessor.getFieldNames());
            String[] schemas = doc.getSchemas();
            keysCol.addAll(Arrays.asList(schemas));
            size = keysCol.size();
            keys = (TemplateCollectionModel)wrapper.wrap(keysCol);
        }
        return keys;
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        List<Object> values = new ArrayList<Object>();
        for (DocumentFieldAccessor accessor : DocumentFieldAccessor.getAcessors()) {
            values.add(accessor.getValue(doc));
        }
        try {
            for (DocumentPart part : doc.getParts()) {
                values.add(unPrefixedMap((Map<String, Serializable>) part.getValue()));
            }
        } catch (PropertyException e) {
            throw new TemplateModelException("failed to fetch a document", e);
        } catch (ClientException e) {
            throw new TemplateModelException(e);
        }
        return (TemplateCollectionModel)wrapper.wrap(values);
    }

    @Override
    public int size() throws TemplateModelException {
        if (size == -1) {
            size = DocumentFieldAccessor.getAcessorsCount() + doc.getSchemas().length;
        }
        return size;
    }

}
