/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.DefaultPropertyFactory;

/**
 * A data model that is not tied to a particular schema, neither has anything to
 * do with a session (CoreSession).
 * <p>
 * Used just to hold data for a <code>FictiveDocumentModel</code> in a
 * non-constraining way.
 *
 * @author DM
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class FictiveDataModel implements DataModel {

    private static final long serialVersionUID = 1L;

    protected Map<String, Object> data = new HashMap<String, Object>();

    protected final DocumentPart dp;

    public FictiveDataModel(String schema) {
        dp = DefaultPropertyFactory.newDocumentPart(schema);
    }

    public void setData(String key, Object value) throws PropertyException {
        data.put(key, value);
    }

    public Object getData(String key) throws PropertyException {
        return data.get(key);
    }

    public String getSchema() {
        return dp.getSchema().getSchemaName();
    }

    public Map<String, Object> getMap() throws PropertyException {
        return data;
    }

    public void setMap(Map<String, Object> data) throws PropertyException {
        this.data = new HashMap<String, Object>(data);
    }

    public boolean isDirty() {
        return true;
    }

    public boolean isDirty(String name) throws PropertyNotFoundException {
        return true;
    }

    public void setDirty(String name) throws PropertyNotFoundException {
    }

    public Collection<String> getDirtyFields() {
        return data.keySet();
    }

    public Object getValue(String path) throws PropertyException {
        throw new UnsupportedOperationException();
    }

    public Object setValue(String path, Object value) throws PropertyException {
        throw new UnsupportedOperationException();
    }

}
