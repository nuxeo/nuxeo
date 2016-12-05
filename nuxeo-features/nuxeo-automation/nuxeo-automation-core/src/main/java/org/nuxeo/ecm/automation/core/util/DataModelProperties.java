/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.core.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.PropertyException;

/**
 * Initialize a {@code Properties} object from one or more {@link DataModel}s.
 * <p>
 * This object can then be passed to any operation accepting {@link Properties}.
 *
 * @since 5.7
 */
public class DataModelProperties extends Properties {

    private static final long serialVersionUID = 1L;

    protected boolean onlyDirtyProperties = false;

    protected Map<String, Serializable> properties = new HashMap<>();

    public DataModelProperties() {
    }

    public DataModelProperties(DataModel dm, boolean onlyDirtyProperties) throws PropertyException {
        this.onlyDirtyProperties = onlyDirtyProperties;
        addDataModel(dm);

    }

    public DataModelProperties(List<DataModel> dms, boolean onlyDirtyProperties) throws PropertyException {
        this.onlyDirtyProperties = onlyDirtyProperties;
        for (DataModel dm : dms) {
            addDataModel(dm);
        }
    }

    public DataModelProperties(DataModel dm) throws PropertyException {
        this(dm, false);
    }

    public DataModelProperties(List<DataModel> dms) throws PropertyException {
        this(dms, false);
    }

    public void addDataModel(DataModel dm) throws PropertyException {
        for (Map.Entry<String, Object> entry : dm.getMap().entrySet()) {
            String key = entry.getKey();
            if ((onlyDirtyProperties && dm.isDirty(key)) || !onlyDirtyProperties) {
                if (!key.contains(":")) {
                    key = dm.getSchema() + ":" + key;
                }
                properties.put(key, (Serializable) entry.getValue());
            }
        }
    }

    public Map<String, Serializable> getMap() {
        return properties;
    }

    @Override
    public String toString() {
        Map<String,Serializable> merged = new HashMap<String, Serializable>();
        merged.putAll(properties);
        merged.putAll(this);
        return merged.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + properties.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DataModelProperties)) {
            return false;
        }
        DataModelProperties other = (DataModelProperties) obj;
        if (!properties.equals(other.properties)) {
            return false;
        }
        return true;
    }


}
