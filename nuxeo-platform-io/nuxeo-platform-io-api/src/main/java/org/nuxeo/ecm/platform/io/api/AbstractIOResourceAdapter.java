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
        properties = new HashMap<String, Serializable>();
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

    public abstract IOResources extractResources(String repo,
            Collection<DocumentRef> sources);

    public abstract IOResources translateResources(String repo,
            IOResources resources, DocumentTranslationMap map);

    public abstract void storeResources(IOResources newResources);

    public abstract void getResourcesAsXML(OutputStream out,
            IOResources newResources);

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
            log.error(String.format(
                    "Property %s needs a string representing a boolean:"
                            + " invalid value %s", propName, propValue));
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
            log.error(String.format("Property %s needs a string value:"
                    + " invalid value %s", propName, propValue));
        }
        properties.put(propName, propValue);
    }

    protected String[] getStringArrayProperty(String propName) {
        return (String[]) properties.get(propName);
    }

    protected void setStringArrayProperty(String propName,
            Serializable propValue) {
        if (propValue == null) {
            return;
        }
        if (!(propValue instanceof String[])) {
            log.error(String.format(
                    "Property %s needs a string array, invalid value %s",
                    propName, propValue));
        }
        properties.put(propName, propValue);
    }

}
