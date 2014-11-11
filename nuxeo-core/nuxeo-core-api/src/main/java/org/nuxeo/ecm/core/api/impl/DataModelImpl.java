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

package org.nuxeo.ecm.core.api.impl;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.AbstractProperty;
import org.nuxeo.ecm.core.api.model.impl.DefaultPropertyFactory;

/**
 * Data model implementation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DataModelImpl implements DataModel {

    private static final long serialVersionUID = -186670993439802490L;

    private static final Log log = LogFactory.getLog(DataModelImpl.class);

    private DocumentPart dp;

    /**
     * Builds an empty data model.
     *
     * @param schema a schema name.
     */
    public DataModelImpl(String schema) {
        this(schema, new HashMap<String, Object>());
    }

    /**
     * Builds a data model using the given data.
     *
     * @param schema a schema name.
     * @param data the data (map String&gt;Object) to put in the DataModel.
     */
    public DataModelImpl(String schema, Map<String, Object> data) {
        assert data != null;

        dp = DefaultPropertyFactory.newDocumentPart(schema);
        if (!data.isEmpty()) {
            try {
                dp.init((Serializable) data);
            } catch (PropertyException e) {
                log.error(e);
            }
        }
    }

    public DataModelImpl(DocumentPart part) {
        assert part != null;
        dp = part;
    }

    /**
     * Get the underlying document part
     */
    public DocumentPart getDocumentPart() {
        return dp;
    }

    public String getSchema() {
        return dp.getSchema().getName();
    }

    public Object getData(String key) {
        try {
            return dp.getValue(key);
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    public void setData(String key, Object value) {
        try {
            dp.setValue(key, value);
        } catch (Exception e) {
            log.error(e);
        }
    }

    public Map<String, Object> getMap() {
        try {
            return (Map<String, Object>) dp.getValue();
        } catch (PropertyException e) {
            log.error(e);
            return null;
        }
    }

    public void setMap(Map<String, Object> data) {
        try {
            dp.setValue(data);
        } catch (PropertyException e) {
            log.error(e);
        }
    }

    public boolean isDirty() {
        return dp.isDirty();
    }

    public boolean isDirty(String name) {
        try {
            return dp.get(name).isDirty();
        } catch (Exception e) {
            log.error(e);
            return false;
        }
    }

    public Collection<String> getDirtyFields() {
        Collection<String> dirtyFields = new ArrayList<String>();
        for (Property prop : dp.getChildren()) {
            if (prop.isDirty()) {
                dirtyFields.add(prop.getName());
            }
        }
        return dirtyFields;
    }

    @Override
    public String toString() {
        // to make it easier to introspect DocumentModel contents with the
        // debugger
        StringBuilder buf = new StringBuilder();
        buf.append(DocumentModelImpl.class.getSimpleName());
        buf.append(" {");
        buf.append(" schema: ");
        buf.append(getSchema());
        buf.append("- Details N/A yet }");
        // try {
        // Map<String,Object> map = (Map<String,Object>)dp.getValue();
        // for (String fieldName : map.keySet()) {
        // buf.append(", ");
        // if (isDirty(fieldName)) {
        // buf.append('*');
        // }
        // buf.append(fieldName);
        // buf.append('=');
        // buf.append(map.get(fieldName));
        // }
        // buf.append(" }");
        // } catch (Exception e) {
        // log.error(e);
        // }

        return buf.toString();
    }

    public void setDirty(String name) {
        try {
            ((AbstractProperty) dp.get(name)).setIsModified();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public Object getValue(String path) throws ParseException {
        try {
            return dp.getValue(path);
        } catch (PropertyException e) {
            ParseException ee = new ParseException("get value failed", 0);
            ee.initCause(e);
            throw ee;
        }
    }

    public Object setValue(String path, Object value) throws ParseException {
        try {
            Property prop = dp.resolvePath(path);
            Object oldValue = prop.getValue();
            prop.setValue(value);
            return oldValue;
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

}
