/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.core.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 *
 * Initialize a {@code Properties} object from one or more {@link DataModel}s.
 * <p>
 * This object can then be passed to any operation accepting {@link Properties}.
 *
 * @since 5.7
 */
public class DataModelProperties extends Properties {

    protected boolean loaded = false;

    protected Map<String, Serializable> properties = new HashMap<>();

    public DataModelProperties() {
    }

    public DataModelProperties(DataModel dm) throws PropertyException {
        addDataModel(dm);
    }

    public DataModelProperties(List<DataModel> dms) throws PropertyException {
        for (DataModel dm : dms) {
            addDataModel(dm);
        }
    }

    public void addDataModel(DataModel dm) throws PropertyException {
        for (Map.Entry<String, Object> entry : dm.getMap().entrySet()) {
            String key = entry.getKey();
            if (!key.contains(":")) {
                key = dm.getSchema() + ":" + key;
            }
            properties.put(key, (Serializable) entry.getValue());
        }
    }

    public Map<String, Serializable> getMap() {
        return properties;
    }

}
