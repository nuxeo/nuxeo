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
 * $Id: FictiveDataModel.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.bulkupdate;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DataModel;

/**
 * A data model that is not tied to a particular schema, neither has anything to
 * do with a session (CoreSession).
 * <p>
 * Used just to hold data for a <code>FictiveDocumentModel</code> in a
 * non-constraining way.
 *
 * @author DM
 *
 */
public class FictiveDataModel implements DataModel {

    private static final Log log = LogFactory.getLog(FictiveDataModel.class);

    private static final long serialVersionUID = 1084408307307844294L;

    private Map<String, Object> data = new HashMap<String, Object>();

    public Object getData(String key) {
        return data.get(key);
    }

    public Collection<String> getDirtyFields() {
        // returns all fields
        return data.keySet();
    }

    public Map<String, Object> getMap() {
        return data;
    }

    public String getSchema() {
        // TODO Auto-generated method stub
        log.error("getSchema not implemented");
        throw new UnsupportedOperationException("not implemented");
    }

    public boolean isDirty() {
        // a fictiveDataModel is always dirty because it has no connection to JCR
        return true;
    }

    public boolean isDirty(String name) {
        // every field of a fictiveDataModel is dirty
        return true;
    }

    public void setData(String key, Object value) {
        data.put(key, value);
    }

    public void setMap(Map<String, Object> data) {
        if (null == data) {
            throw new IllegalArgumentException("null data map");
        }
        this.data = data;
    }

    public void setDirty(String name) {
        // every field of a fictiveDataModel is dirty
    }

    public Object getValue(String path) throws ParseException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Object setValue(String path, Object value) throws ParseException {
        throw new UnsupportedOperationException("Not implemented");
    }

}
