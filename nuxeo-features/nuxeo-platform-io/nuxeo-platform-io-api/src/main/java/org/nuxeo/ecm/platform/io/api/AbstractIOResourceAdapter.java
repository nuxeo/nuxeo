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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: AbstractIOResourceAdapter.java 25080 2007-09-18 14:52:20Z atchertchian $
 */

package org.nuxeo.ecm.platform.io.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;

/**
 * Abstract implementation for {@link IOResourceAdapter}.
 * <p>
 * Offers helper methods for properties.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public abstract class AbstractIOResourceAdapter implements IOResourceAdapter {

    private static final long serialVersionUID = 4399167777434048174L;

    private static final Log log = LogFactory.getLog(AbstractIOResourceAdapter.class);

    protected Map<String, Serializable> properties;

    protected AbstractIOResourceAdapter() {
        properties = new HashMap<>();
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

    @Override
    public abstract IOResources extractResources(String repo, Collection<DocumentRef> sources);

    @Override
    public abstract IOResources translateResources(String repo, IOResources resources, DocumentTranslationMap map);

    @Override
    public abstract void storeResources(IOResources newResources);

    @Override
    public abstract void getResourcesAsXML(OutputStream out, IOResources newResources);

    @Override
    public abstract IOResources loadResourcesFromXML(InputStream stream);

    // helper methods

    protected boolean getBooleanProperty(String propName) {
        Boolean value = (Boolean) properties.get(propName);
        if (value == null) {
            return false;
        }
        return value;
    }

    protected void setBooleanProperty(String propName, Serializable propValue) {
        if (propValue == null) {
            return;
        }
        if (propValue instanceof String) {
            properties.put(propName, Boolean.valueOf((String) propValue));
        } else {
            log.error(String.format("Property %s needs a string representing a boolean:" + " invalid value %s",
                    propName, propValue));
        }
    }

    protected String getStringProperty(String propName) {
        return (String) properties.get(propName);
    }

    protected void setStringProperty(String propName, Serializable propValue) {
        if (propValue == null) {
            return;
        }
        if (!(propValue instanceof String)) {
            log.error(String.format("Property %s needs a string value:" + " invalid value %s", propName, propValue));
        }
        properties.put(propName, propValue);
    }

    protected String[] getStringArrayProperty(String propName) {
        return (String[]) properties.get(propName);
    }

    protected void setStringArrayProperty(String propName, Serializable propValue) {
        if (propValue == null) {
            return;
        }
        if (!(propValue instanceof String[])) {
            log.error(String.format("Property %s needs a string array, invalid value %s", propName, propValue));
        }
        properties.put(propName, propValue);
    }

}
