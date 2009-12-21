/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * Base session class with helper methods common to all kinds of directory
 * sessions.
 *
 * @author Anahide Tchertchian
 * @since 5.2M4
 */
public abstract class BaseSession implements Session {

    /**
     * Returns a bare document model for this directory.
     * <p/>
     * Can be used for creation screen
     *
     * @since 5.2M4
     */
    public static DocumentModel createEntryModel(String sessionId, String schema, String id, Map<String, Object> values) throws PropertyException {
        DocumentModelImpl entry = new DocumentModelImpl(sessionId, schema, id, null, null, null, new String[]{schema}, null);
        DataModel dataModel;
        if (values == null) {
            dataModel = new DataModelImpl(schema, Collections.<String, Object>emptyMap());
        } else {
            dataModel = new DataModelImpl(schema);
            dataModel.setMap(values); // makes fields dirty
        }
        entry.addDataModel(dataModel);
        return entry;
    }


    protected static Map<String, Serializable> mkSerializableMap(Map<String, Object> map) {
        Map<String, Serializable> serializableMap = null;
        if (map != null) {
            serializableMap = new HashMap<String, Serializable>();
            for (String key : map.keySet()) {
                serializableMap.put(key, (Serializable) map.get(key));
            }
        }
        return serializableMap;
    }

    protected static Map<String, Object> mkObjectMap(Map<String, Serializable> map) {
        Map<String, Object> objectMap = null;
        if (map != null) {
            objectMap = new HashMap<String, Object>();
            for (String key : map.keySet()) {
                objectMap.put(key, map.get(key));
            }
        }
        return objectMap;
    }

}
