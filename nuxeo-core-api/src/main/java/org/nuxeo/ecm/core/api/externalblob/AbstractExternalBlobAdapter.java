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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api.externalblob;

import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * Abstract class for external blob adapters.
 * <p>
 * Provides generic methods
 * <p>
 * Extend this class if you want your contributions to be robust to interface
 * changes.
 *
 * @author Anahide Tchertchian
 */
public abstract class AbstractExternalBlobAdapter implements
        ExternalBlobAdapter {

    protected String prefix;

    protected Map<String, String> properties;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Map<String, String> getProperties() {
        // de-reference
        if (properties == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(properties);
    }

    public String getProperty(String name) {
        Map<String, String> props = getProperties();
        String prop = props.get(name);
        prop = prop.trim();
        return prop;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getLocalName(String uri) throws PropertyException {
        String prefix = getPrefix();
        if (prefix == null) {
            throw new PropertyException(String.format(
                    "Null prefix on external blob adapter with class '%s'",
                    getClass().getName()));
        }
        if (uri == null
                || !uri.startsWith(prefix
                        + ExternalBlobAdapter.PREFIX_SEPARATOR)) {
            throw new PropertyException(
                    String.format(
                            "Invalid uri '%s' for this adapter: expected to start with prefix '%s'",
                            uri, prefix));
        }
        return uri.substring(prefix.length() + 1);
    }
}
